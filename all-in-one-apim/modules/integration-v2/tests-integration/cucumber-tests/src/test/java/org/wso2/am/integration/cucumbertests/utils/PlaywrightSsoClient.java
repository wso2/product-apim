/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.am.integration.cucumbertests.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.Proxy;
import com.microsoft.playwright.options.RequestOptions;
import com.microsoft.playwright.options.WaitUntilState;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Drives the APIM console single-sign-on journey through APIM's resident IS to an external federated Identity
 * Server in a REAL browser (Playwright Chromium, headless). Unlike the earlier hand-rolled HTTP walk, a real
 * browser scopes cookies per path, runs the console SPA's JavaScript, and preserves the PKCE
 * {@code code_verifier} across the federated round-trip, so the console's {@code login_callback.jsp} server-side
 * token exchange succeeds and the {@code AM_ACC_TOKEN} session cookie is genuinely issued.
 *
 * <p>The browser reaches the mapped container ports through an in-process {@link ConnectProxy}: it navigates to
 * the servers' INTERNAL hostnames ({@code https://localhost:9443} for APIM, {@code https://wso2is:9443} for IS)
 * and the proxy tunnels those to the host-mapped ports. Because the browser uses the same internal hostnames the
 * servers emit in their redirects and {@code redirect_uri}s, nothing has to be rewritten and the code-to-token
 * exchange's {@code redirect_uri} stays consistent end to end. {@code --proxy-bypass-list=<-loopback>} forces
 * even {@code localhost} through the proxy (Chromium bypasses loopback by default).
 *
 * <p>Realistic browser headers are inherent (a real Chromium), so the #17744 condition — the multi-option
 * {@code /commonauth} request whose expanded scope header overruns a too-small {@code maxHttpHeaderSize} — is
 * exercised naturally: on a broken build the federated click lands on Tomcat's 400 and the login never
 * completes; on a fixed build the whole journey lands.
 */
public final class PlaywrightSsoClient implements AutoCloseable {

    private static final Log log = LogFactory.getLog(PlaywrightSsoClient.class);

    /** Internal hostnames the servers emit; the proxy tunnels these to the mapped container ports. */
    private static final String APIM_INTERNAL = "https://localhost:9443";

    /** APIM resident-IS authentication-framework session cookie; its reuse across consoles is single sign-on. */
    private static final String IS_SESSION_COOKIE = "commonAuthId";

    /** Path of the authentication endpoint's credential page; being served one means a login was demanded. */
    private static final String LOGIN_PAGE_PATH = "/authenticationendpoint/login";

    /** {@code docTrail} size when the current SSO console open began; later hops belong to that open. */
    private int trailMarkAtConsoleOpen;
    private java.util.List<String> lastLogoutHops = java.util.Collections.emptyList();

    /**
     * The resident-IS session the first console landing established. Every later console must present the same
     * value — see {@link #assertSameIsSessionAcrossConsoles(String)}.
     */
    private String firstIsSessionId;

    private final ConnectProxy proxy;
    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext context;
    private final Page page;
    /** Whether the browser proxy has a route to the gateway's internal localhost:8243 endpoint. */
    private boolean gatewayRouted;
    /** Document-navigation trail ("status url") recorded from responses — the browser equivalent of walk hops. */
    private final java.util.List<String> docTrail = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
    /** Management-plane XHR/fetch calls (status method path) — diagnostic for stuck SPA form validation. */
    private final java.util.List<String> apiCalls = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
    /** Browser console errors/warnings — a JS error can wedge a React form's async validation. */
    private final java.util.List<String> consoleMsgs = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

    /**
     * @param apimMappedBase    the APIM host-mapped base URL (e.g. {@code https://localhost:32912/})
     * @param isMappedBase      the IS host-mapped base URL (e.g. {@code https://localhost:32914/})
     * @param gatewayMappedBase the APIM gateway host-mapped HTTPS base URL (e.g. {@code https://localhost:32913/}),
     *                          so the DevPortal Try-Out / API invocation reaches the gateway at its internal
     *                          {@code localhost:8243}; may be null when no gateway invocation is needed.
     */
    public PlaywrightSsoClient(String apimMappedBase, String isMappedBase, String gatewayMappedBase) {
        InetSocketAddress apim = addressOf(apimMappedBase);
        InetSocketAddress is = addressOf(isMappedBase);
        Map<String, InetSocketAddress> routes = new HashMap<>();
        routes.put("localhost:9443", apim);   // APIM self-emitted URLs + console
        routes.put("wso2am:9443", apim);       // APIM network alias (if ever emitted)
        routes.put("wso2is:9443", is);         // federated IdP authorize/token host
        if (gatewayMappedBase != null && !gatewayMappedBase.isBlank()) {
            // The API's deployed gateway URL is https://localhost:8243/<context> (VHost "localhost"); tunnel it to
            // the mapped gateway port so the browser can invoke the API end to end.
            routes.put("localhost:8243", addressOf(gatewayMappedBase));
            gatewayRouted = true;
        }
        this.proxy = new ConnectProxy(routes);

        // Only Chromium is installed (via `playwright install chromium`); tell the driver NOT to download any
        // browser at runtime, so create() never blocks on a slow Firefox/WebKit fetch from the CDN.
        this.playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        this.browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setProxy(new Proxy("http://127.0.0.1:" + proxy.getPort()))
                // Chromium bypasses the proxy for loopback by default; force localhost through it so the
                // servers' localhost:9443 URLs are tunnelled to the mapped ports.
                .setArgs(List.of("--proxy-bypass-list=<-loopback>")));
        this.context = browser.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(true));
        this.page = context.newPage();
        // Record every top-level document navigation (url + status) — safe to read even while the page is mid
        // navigation, unlike page.content(). This is the diagnostic trail and the #17744 detector.
        // Capture management-plane XHR/fetch calls + browser console errors, so a stuck SPA form (e.g. the create
        // API form whose async context-uniqueness validation wedges the submit button) can be diagnosed.
        page.onResponse(resp -> {
            try {
                String rt = resp.request().resourceType();
                if (("xhr".equals(rt) || "fetch".equals(rt)) && resp.url().contains("/api/am/")) {
                    apiCalls.add(resp.status() + " " + resp.request().method() + " " + shortUrl(resp.url()));
                }
            } catch (RuntimeException ignored) {
                // response object may be gone; ignore
            }
        });
        page.onRequestFailed(req -> {
            try {
                String rt = req.resourceType();
                if ("xhr".equals(rt) || "fetch".equals(rt)) {
                    apiCalls.add("FAILED " + req.method() + " " + shortUrl(req.url()) + " (" + req.failure() + ")");
                }
            } catch (RuntimeException ignored) {
                // request object may be gone; ignore
            }
        });
        page.onConsoleMessage(m -> {
            try {
                if ("error".equals(m.type()) || "warning".equals(m.type())) {
                    consoleMsgs.add(m.type() + ": " + (m.text().length() > 220 ? m.text().substring(0, 220) : m.text()));
                }
            } catch (RuntimeException ignored) {
                // ignore
            }
        });
        page.onResponse(resp -> {
            try {
                if ("document".equals(resp.request().resourceType())) {
                    docTrail.add(resp.status() + " " + resp.url());
                }
            } catch (RuntimeException ignored) {
                // response object may be gone; ignore
            }
        });
    }

    private static InetSocketAddress addressOf(String url) {
        URI u = URI.create(url);
        int port = u.getPort() == -1 ? 443 : u.getPort();
        return new InetSocketAddress(u.getHost(), port);
    }

    // ─────────────────────────── flow methods ───────────────────────────

    /** Navigates to the console's login endpoint, which redirects to the (multi-option) login page. */
    public void openConsoleLoginPage(String consoleContext) {
        page.navigate(consoleEntryUrl(consoleContext),
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        page.waitForLoadState();
        if ("devportal".equals(consoleContext)) {
            page.waitForURL("**/authenticationendpoint/login.do**",
                    new Page.WaitForURLOptions().setTimeout(30000));
            settle();
        }
    }

    /**
     * The URL that makes a console begin authentication.
     *
     * <p>Not uniform across the consoles. The Publisher and Admin webapps map a server-side
     * {@code /services/auth/login} entry that starts the OIDC flow. The DevPortal does NOT — that path is a 404
     * there, and its {@code /login} route is served by the SPA index servlet — so it is entered through an
     * authenticated SPA route, which makes the application initiate the login itself.
     *
     * <p>Using the Publisher's path for the DevPortal lands on a 404 whose page has no credential form, which
     * silently satisfies a "was I re-prompted?" check and reports success for a console that never authenticated.
     */
    private String consoleEntryUrl(String consoleContext) {
        if ("devportal".equals(consoleContext)) {
            return APIM_INTERNAL + "/devportal/applications?tenant=" + devportalTenant;
        }
        return APIM_INTERNAL + "/" + consoleContext + "/services/auth/login";
    }

    /** Whether the login page is a multi-option step (carries the {@code multiOptionURI} parameter). */
    public boolean pageHasMultiOptionUri() {
        return page.content().contains("multiOptionURI");
    }

    /** Whether the login page offers BOTH the local BasicAuthenticator and the federated OIDC authenticator. */
    public boolean pageOffersBothAuthenticators(String idpName) {
        String html = page.content();
        return html.contains("BasicAuthenticator")
                && (html.contains("OpenIDConnectAuthenticator%3A" + idpName)
                    || html.contains("OpenIDConnectAuthenticator:" + idpName));
    }

    /**
     * Clicks the federated IdP option on the multi-option page — the #17744 {@code /commonauth} navigation. On a
     * build with too small a {@code maxHttpHeaderSize} the resulting request overruns the limit and Tomcat
     * answers 400, surfaced here as a clear AssertionError; on a fixed build the browser reaches the external IS
     * login page.
     */
    public void selectFederatedIdp(String idpName) {
        // The multi-option page renders each federated authenticator as a clickable element whose onclick calls
        // handleNoDomain(this, '<idp>', '<authenticator>'). Prefer an element referencing this IdP; fall back to
        // the IdP display text.
        String[] candidates = {
                "[onclick*='handleNoDomain'][onclick*=\"" + idpName + "\"]",
                "a[onclick*='handleNoDomain'][onclick*=\"" + idpName + "\"]",
                "[onclick*=\"" + idpName + "\"]",
                "a:has-text(\"" + idpName + "\")",
                "text=" + idpName
        };
        boolean clicked = false;
        for (String sel : candidates) {
            try {
                if (page.locator(sel).count() > 0) {
                    page.locator(sel).first().click();
                    clicked = true;
                    break;
                }
            } catch (RuntimeException ignored) {
                // try the next selector
            }
        }
        if (!clicked) {
            throw new AssertionError("could not find the '" + idpName + "' federated option on the multi-option "
                    + "page. " + pageDiagnostic());
        }
        settle();
        assertNot17744();
    }

    /**
     * Fills and submits credentials against the LOCAL authenticator on the console's own multi-option login page
     * — the other half of that page, alongside the federated option.
     *
     * <p>Tenant-dimensioned in a way the federated option is not: the console service provider is a super-tenant
     * SaaS app, and {@code BasicAuthenticator} resolves a qualified username against that tenant's user store, so
     * {@code admin@tenant1.com} authenticates as the tenant's admin. (A federated login resolves through the
     * identity provider instead, and always lands in the provider's own tenant.)
     */
    public void authenticateWithLocalAuthenticator(String username, String password) {
        try {
            page.waitForSelector("input[type=password]",
                    new Page.WaitForSelectorOptions().setTimeout(25000));
        } catch (com.microsoft.playwright.TimeoutError e) {
            assertNot17744();
            throw new AssertionError("the console's local credential form never appeared on the multi-option "
                    + "login page. " + pageDiagnostic());
        }
        fillFirst(username, "input[name=username]", "#username", "input#usernameUserInput", "input[type=text]");
        fillFirst(password, "input[name=password]", "#password", "input[type=password]");
        clickFirst("button[type=submit]", "input[type=submit]", "#loginForm button", "button:has-text(\"Sign In\")",
                "button:has-text(\"Continue\")");
        settle();
        assertNot17744();
    }

    /**
     * The username the console's session actually belongs to, read from the console's own introspect endpoint.
     *
     * <p>Used to pin WHICH principal a login produced. The console token is opaque, so this asks the console
     * itself rather than trying to decode anything.
     */
    public String sessionPrincipal(String consoleContext) {
        APIResponse resp = page.request().get(APIM_INTERNAL + "/" + consoleContext + "/services/auth/introspect");
        String body;
        try {
            body = resp.text();
        } catch (RuntimeException e) {
            body = null;
        }
        log.info("[SSO] introspect(" + consoleContext + ") status=" + resp.status() + " body="
                + (body == null ? null : (body.length() > 300 ? body.substring(0, 300) : body)));
        if (body == null) {
            throw new AssertionError("the '" + consoleContext + "' console introspect returned no body, so the "
                    + "session's principal cannot be read.");
        }
        String user = firstJson(body, "username");
        if (user == null) {
            user = firstJson(body, "user");
        }
        if (user == null) {
            user = firstJson(body, "sub");
        }
        if (user == null) {
            throw new AssertionError("could not read a username from the '" + consoleContext + "' console "
                    + "introspect response: " + body);
        }
        return user;
    }

    private static String firstJson(String body, String field) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"" + field + "\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
        return m.find() ? m.group(1) : null;
    }

    /**
     * Chooses the tenant on the multi-tenant broker's tenant-selection page.
     *
     * <p>This page has no counterpart in the single-tenant journey: the broker authenticator serves it before any
     * credentials are asked for, so that it knows which tenant's service provider to hand the flow to. Selecting
     * here is what makes the rest of the login resolve inside that tenant.
     */
    public void selectTenant(String tenantDomain) {
        try {
            page.waitForURL("**/select-tenant/**", new Page.WaitForURLOptions().setTimeout(30000));
        } catch (com.microsoft.playwright.TimeoutError e) {
            assertNot17744();
            throw new AssertionError("the multi-tenant broker never served its tenant-selection page, so no "
                    + "tenant could be chosen. " + pageDiagnostic());
        }
        settle();
        // The tenant list is a dropdown filled ASYNCHRONOUSLY — it renders as "Loading tenants..." first — so
        // wait for this tenant's own option. That wait doubles as a real check: a tenant that never synchronized
        // to API Manager is simply not offered, and this fails naming it rather than selecting nothing.
        try {
            // ATTACHED, not the default VISIBLE: an <option> inside a <select> is never "visible", so waiting on
            // visibility times out even once the tenant is listed.
            page.waitForSelector("#tenantIdentifier option[value='" + tenantDomain + "']",
                    new Page.WaitForSelectorOptions().setTimeout(30000)
                            .setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED));
        } catch (com.microsoft.playwright.TimeoutError e) {
            throw new AssertionError("the broker's tenant-selection page never offered tenant '" + tenantDomain
                    + "'. Its dropdown loads the tenants API Manager knows about, so this usually means the "
                    + "tenant was not synchronized. Page controls: " + pageMarkupExcerpt());
        }
        page.selectOption("#tenantIdentifier", tenantDomain);
        clickFirst("#submitBtn", "button[type=submit]", "input[type=submit]",
                "button:has-text(\"Continue\")");
        settle();
        assertNot17744();
        log.info("[SSO] selected tenant '" + tenantDomain + "' on the broker's tenant-selection page");
    }

    /**
     * The interactive controls on the current page — inputs, selects and links — so an unexpected page shape is
     * diagnosable from the log instead of by guessing selectors.
     */
    private String pageMarkupExcerpt() {
        try {
            return (String) page.evaluate(
                    "() => Array.from(document.querySelectorAll('input,select,option,button,a'))"
                    + ".slice(0,25).map(e => e.tagName"
                    + " + (e.id ? '#' + e.id : '')"
                    + " + (e.name ? '[name=' + e.name + ']' : '')"
                    + " + (e.type ? '[type=' + e.type + ']' : '')"
                    + " + (e.value ? '[value=' + e.value + ']' : '')"
                    + " + (e.textContent ? '{' + e.textContent.trim().slice(0,30) + '}' : '')).join(' | ')");
        } catch (RuntimeException e) {
            return "<could not read: " + e.getClass().getSimpleName() + ">";
        }
    }

    /** Fills and submits the federated user's credentials on the external IS login form. */
    public void authenticateAtExternalIs(String username, String password) {
        // The click above redirected through /commonauth to the external IS; wait for its login form to appear.
        try {
            page.waitForSelector("input[type=password]",
                    new Page.WaitForSelectorOptions().setTimeout(25000));
        } catch (com.microsoft.playwright.TimeoutError e) {
            assertNot17744(); // if the redirect hit a /commonauth 400, surface #17744 with a clear message
            throw new AssertionError("the external IS login form never appeared after selecting the IdP. "
                    + pageDiagnostic());
        }
        fillFirst(username, "input[name=username]", "#username", "input#usernameUserInput", "input[type=text]");
        fillFirst(password, "input[name=password]", "#password", "input[type=password]");
        clickFirst("button[type=submit]", "input[type=submit]", "#loginForm button", "button:has-text(\"Sign In\")",
                "button:has-text(\"Continue\")");
        // The submit kicks off the federated return, JIT provisioning and the console callback exchange;
        // assertLandedInConsole polls the cookie store for the resulting session cookie.
        assertNot17744();
    }

    /** Lenient wait for the current navigation to reach LOAD — never fails the step on a redirect-chain timeout. */
    private void settle() {
        try {
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.LOAD,
                    new Page.WaitForLoadStateOptions().setTimeout(15000));
        } catch (RuntimeException ignored) {
            // mid-redirect / slow chain — the explicit selector/cookie waits downstream are the real gates
        }
    }

    /**
     * Asserts the journey landed authenticated in the console: the console back-end completed the code-to-token
     * exchange and issued the session cookie, and the browser is on the console app (not a login/consent/error
     * page).
     */
    public void assertLandedInConsole(String consoleContext) {
        // The federated redirect chain + SPA callback are asynchronous; poll the cookie store (safe to read
        // while the page navigates, unlike page.content()) until the console back-end issues the session cookie.
        if (!waitForAccessTokenCookie(consoleContext, 30000)) {
            throw new AssertionError("did not land authenticated in the '" + consoleContext + "' console — it "
                    + "issued no session token cookie of its own within 30s (the federated auth / JIT / token "
                    + "exchange did not complete). " + pageDiagnostic());
        }
    }

    /** Polls the cookie store until an {@code AM_ACC_TOKEN…} cookie appears or the timeout elapses. */
    private boolean waitForAccessTokenCookie(String consoleContext, int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        // CHECKSTYLE:OFF deadlineLoop - browser cookie-store poll (Playwright); Utils.retryUntil is HTTP-only, N/A here
        while (System.currentTimeMillis() < deadline) {
            // CHECKSTYLE:ON
            if (hasAccessTokenCookie(consoleContext)) {
                return true;
            }
            try {
                page.waitForTimeout(500);
            } catch (RuntimeException e) {
                return hasAccessTokenCookie(consoleContext);
            }
        }
        return hasAccessTokenCookie(consoleContext);
    }

    /**
     * Whether THIS console has issued its own session token, i.e. its split access-token part-1 cookie exists at
     * the console's own cookie path.
     *
     * <p>Console-scoped on purpose. A check that accepts any {@code AM_ACC_TOKEN…} in the jar is satisfied by a
     * DIFFERENT console's cookie, so it would report a console as authenticated when that console never completed
     * a login of its own.
     */
    public boolean hasAccessTokenCookie(String consoleContext) {
        return consoleTokenPart1("/" + consoleContext) != null;
    }

    /**
     * Opens a console reusing the SAME browser context (shared SSO session), marking the navigation trail so
     * {@link #assertNotPromptedToLoginAgain()} can tell which hops belong to this open.
     */
    public void openConsoleExpectingSso(String consoleContext) {
        trailMarkAtConsoleOpen = docTrail.size();
        try {
            page.navigate(consoleEntryUrl(consoleContext),
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
        } catch (PlaywrightException e) {
            // A post-logout console open is expected to redirect through the authentication endpoint. Chromium
            // can report ERR_ABORTED when that redirect replaces the document while navigate() is waiting for
            // DOMContentLoaded. The document response trail still records the redirect and is what the fresh-login
            // assertion evaluates; unrelated navigation failures must remain visible to the test.
            if (!e.getMessage().contains("net::ERR_ABORTED")) {
                throw e;
            }
            log.info("[SSO] console '" + consoleContext
                    + "' navigation was interrupted by the expected login redirect; continuing with trail checks");
        }
    }

    /** Document-navigation hops recorded since the most recent {@link #openConsoleExpectingSso(String)}. */
    private java.util.List<String> hopsSinceConsoleOpen() {
        java.util.List<String> all = new java.util.ArrayList<>(docTrail);
        int from = Math.min(trailMarkAtConsoleOpen, all.size());
        return all.subList(from, all.size());
    }

    /**
     * Asserts the console was entered under the SAME resident-IS session the first federated login established,
     * recording that session on the first call.
     *
     * <p>This is what single sign-on IS: the console redirects to APIM's resident IS, which recognises its
     * {@code commonAuthId} session and returns without authenticating again. Reusing that session and
     * re-authenticating are indistinguishable from the rest of the journey — every later step would pass either
     * way, since they only prove the user could do the work. Comparing the session identity is the one signal
     * that separates them.
     *
     * <p>Scoped to APIM's origin on purpose: the external Identity Server sets a {@code commonAuthId} of its own,
     * and it is APIM's resident-IS session that decides whether a console re-prompts.
     */
    public void assertSameIsSessionAcrossConsoles(String consoleContext) {
        String current = isSessionCookieValue();
        if (current == null || current.isBlank()) {
            throw new AssertionError("no " + IS_SESSION_COOKIE + " cookie for " + APIM_INTERNAL + " after landing "
                    + "in the '" + consoleContext + "' console — there is no resident-IS session for a later "
                    + "console to single-sign-on with. cookies=" + cookieNames());
        }
        if (firstIsSessionId == null) {
            firstIsSessionId = current;
            log.info("[SSO] resident-IS session established at the '" + consoleContext + "' console");
            return;
        }
        if (!firstIsSessionId.equals(current)) {
            throw new AssertionError("the '" + consoleContext + "' console was entered under a DIFFERENT "
                    + "resident-IS session than the first federated login established, so it re-authenticated "
                    + "instead of reusing the session — that is not single sign-on. " + pageDiagnostic());
        }
        log.info("[SSO] the '" + consoleContext + "' console reused the same resident-IS session");
    }

    /**
     * Value of APIM's resident-IS session cookie, or {@code null} when the browser holds none.
     *
     * <p>The value is a session secret, so it is compared but never logged.
     */
    private String isSessionCookieValue() {
        for (Cookie c : context.cookies(APIM_INTERNAL)) {
            if (IS_SESSION_COOKIE.equals(c.name)) {
                return c.value;
            }
        }
        return null;
    }

    /**
     * Logs out of a console, following the logout chain to completion: the console's logout endpoint drives the
     * resident IS's OIDC logout, which in turn calls the federated IdP's logout endpoint before returning to the
     * console's logout callback. Both service providers skip logout consent, so no confirmation is expected.
     *
     * <p>The endpoint is {@code /services/logout} — the console webapps map that, plus
     * {@code /services/auth/callback/logout} for the return leg. It does NOT mirror the login endpoint's
     * {@code /services/auth/login} shape, and the symmetrical-looking {@code /services/auth/logout} is a 404,
     * which leaves the session intact and makes a logout assertion look like a product failure.
     */
    public void logoutFromConsole(String consoleContext) {
        int mark = docTrail.size();
        page.navigate(APIM_INTERNAL + "/" + consoleContext + "/services/logout",
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
        settle();
        // Fail HERE if the logout endpoint did not answer. Otherwise the session simply survives and the residue
        // assertions below report a global-logout failure, which reads as a product defect rather than a bad URL.
        java.util.List<String> all = new java.util.ArrayList<>(docTrail);
        lastLogoutHops = new java.util.ArrayList<>(all.subList(Math.min(mark, all.size()), all.size()));
        for (String hop : lastLogoutHops) {
            if (hop.contains("/services/logout") && !hop.startsWith("2") && !hop.startsWith("3")) {
                throw new AssertionError("the '" + consoleContext + "' console logout endpoint did not answer: "
                        + hop + " — no logout was performed, so nothing about single logout can be concluded.");
            }
        }
        log.info("[SSO] logged out of the '" + consoleContext + "' console");
    }

    /** Requires the logout flow to return through the console's registered logout callback. */
    public void assertLogoutRedirect(String consoleContext) {
        String callback = "/" + consoleContext + "/services/auth/callback/logout";
        boolean returnedThroughCallback = lastLogoutHops.stream().anyMatch(hop -> hop.contains(callback));
        if (!returnedThroughCallback) {
            throw new AssertionError("the '" + consoleContext + "' logout did not return through its callback '"
                    + callback + "': " + pageDiagnostic());
        }
        log.info("[SSO] logout returned through '" + callback + "'");
    }

    /**
     * Asserts a single logout left NO usable session for {@code consoleContext} — opening it demands a fresh
     * login.
     *
     * <p>The behavioural half of the residue check, and the inverse of
     * {@link #assertNotPromptedToLoginAgain()}: a login page served for this open is exactly what must now
     * happen. Asserted for EVERY console, not only the one logged out from, because a logout that clears one
     * console while leaving another usable is not single logout.
     */
    public void assertConsoleDemandsFreshLogin(String consoleContext) {
        openConsoleExpectingSso(consoleContext);
        boolean loginServed = false;
        for (String hop : hopsSinceConsoleOpen()) {
            if (hop.contains(LOGIN_PAGE_PATH)) {
                loginServed = true;
                break;
            }
        }
        boolean credentialFormPresent = page.locator("input[type=password]").count() > 0;
        if (!loginServed && !credentialFormPresent) {
            throw new AssertionError("after single logout the '" + consoleContext + "' console did NOT demand a "
                    + "fresh login — a usable session survived there, so the logout was not global. trail:"
                    + trail());
        }
        log.info("[SSO] the '" + consoleContext + "' console demands a fresh login after logout");
    }

    /**
     * Asserts a console's session is no longer USABLE after logout, by attempting an authenticated read that
     * succeeded while logged in. Anything but a rejection means the session survived.
     *
     * <p>This is the residue signal for the DevPortal, which cannot use
     * {@link #assertConsoleDemandsFreshLogin(String)}: it has no server-side login entry point to redirect to
     * (its {@code /login/*} route is served by the SPA index servlet, and {@code /services/auth/login} — which the
     * Publisher and Admin consoles do map — returns 404 there), and it permits anonymous browsing, so "a login
     * page appeared" would be neither expected nor meaningful.
     */
    public void assertConsoleSessionUnusable(String consoleContext, String resourcePath, String pathHint) {
        String part1 = consoleTokenPart1(pathHint);
        if (part1 == null) {
            log.info("[SSO] the '" + consoleContext + "' console kept no token cookie at all after logout");
            return;
        }
        APIResponse resp = page.request().get(APIM_INTERNAL + resourcePath,
                RequestOptions.create().setHeader("Authorization", "Bearer " + part1));
        if (resp.status() == 200) {
            throw new AssertionError("after single logout the '" + consoleContext + "' console's token STILL "
                    + "authorizes " + shortUrl(resourcePath) + " (HTTP 200) — a usable session survived there, so "
                    + "the logout was not global.");
        }
        log.info("[SSO] the '" + consoleContext + "' console's surviving token no longer authorizes (HTTP "
                + resp.status() + ")");
    }

    /**
     * Asserts a single logout left NO session cookie residue: neither the resident-IS session nor any console's
     * access-token cookies survive.
     *
     * <p>The cookie half of the residue check. A console whose token cookies outlive the logout can keep making
     * authenticated calls even though the interactive session appears gone.
     */
    public void assertNoSessionCookieResidue() {
        java.util.List<String> residue = new java.util.ArrayList<>();
        for (Cookie c : context.cookies(APIM_INTERNAL)) {
            boolean isSessionCookie = IS_SESSION_COOKIE.equals(c.name)
                    || c.name.startsWith("AM_ACC_TOKEN")
                    || c.name.startsWith("WSO2_AM_TOKEN_1");
            if (isSessionCookie && c.value != null && !c.value.isBlank()) {
                residue.add(c.name + (c.path == null ? "" : " (path " + c.path + ")"));
            }
        }
        if (!residue.isEmpty()) {
            throw new AssertionError("single logout left session cookie residue for " + APIM_INTERNAL + ": "
                    + residue + " — these still authorize calls after logout.");
        }
        log.info("[SSO] no session cookie residue remains after logout");
    }

    /**
     * Asserts the SSO open did NOT re-prompt for credentials.
     *
     * <p>Checked two ways, because the end state alone is not enough. A login page served part-way through the
     * redirect chain and then navigated away from leaves no trace in the final DOM, so the navigation trail for
     * this open is scanned for it first; only then is the settled page inspected for a credential form.
     */
    public void assertNotPromptedToLoginAgain() {
        for (String hop : hopsSinceConsoleOpen()) {
            if (hop.contains(LOGIN_PAGE_PATH)) {
                throw new AssertionError("SSO failed: the authentication endpoint served a login page while "
                        + "opening the console, so the session was not reused (even if the flow later moved on). "
                        + "trail:" + trail());
            }
        }
        boolean prompted = page.locator("input[type=password]").count() > 0
                || page.content().contains("sessionDataKey") && page.content().contains("login.do");
        if (prompted) {
            throw new AssertionError("SSO failed: a second credential prompt appeared when opening the console — "
                    + "the shared session was not reused. " + pageDiagnostic());
        }
    }

    // ─────────────────────────── publisher SPA work ───────────────────────────

    /** The API id of the last API created through the publisher UI (captured from the overview URL). */
    private String currentApiId;

    public String currentApiId() {
        return currentApiId;
    }

    /**
     * Drives the publisher "Create an API" (REST, design-first) form under the SSO session: fills name / context /
     * version / backend endpoint and clicks <b>Create</b> (NOT "Create &amp; Publish" — deploy and publish are
     * separate SSO-continuation steps). Waits until the app routes to the new API's overview and captures its id.
     */
    public String createRestApi(String name, String context, String version, String endpoint) {
        // The create form's mandatory-field validation POSTs two async uniqueness checks
        // (/api/am/publisher/v4/apis/validate?query=name:… and ?query=context:…). While the publisher REST
        // webapp is still COLD under boot load those return 404, which the form treats as "not validated" and
        // keeps the Create button disabled; once the webapp is warm they return 200 and Create enables. So retry
        // the WHOLE form (fresh navigation re-fires the checks; the webapp warms across attempts) until Create
        // enables and the app routes to the new API's overview. The Backend Endpoint is left empty here (a
        // provided endpoint triggers a browser-side reachability test the proxy can't satisfy); it is set on the
        // Endpoints config page afterwards.
        for (int attempt = 1; attempt <= 6; attempt++) {
            page.navigate(APIM_INTERNAL + "/publisher/apis/create/rest",
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            try {
                page.waitForSelector("[data-testid=default-api-form]",
                        new Page.WaitForSelectorOptions().setTimeout(30000));
            } catch (RuntimeException e) {
                continue; // form did not render this attempt; reload
            }
            fillAndBlur("input[name=name]", name);
            fillAndBlur("input[name=context]", context);
            fillAndBlur("input[name=version]", version);
            // Provide the backend endpoint so the created API has a production endpoint (required for Deploy to
            // enable and for the gateway to route). We only TYPE it — we never click the "TestEndpoint" button,
            // whose browser-side reachability probe the CONNECT proxy can't satisfy (it reaches only APIM/IS); the
            // URL is still saved on the API, and the gateway reaches nodebackend in-network at invoke time.
            if (endpoint != null && !endpoint.isBlank()) {
                fillAndBlur("input[name=endpoint]", endpoint);
            }
            Locator create = page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Create").setExact(true));
            try {
                create.waitFor(new Locator.WaitForOptions().setTimeout(10000));
            } catch (RuntimeException e) {
                continue;
            }
            if (!waitForEnabled(create, 20000)) {
                log.info("[SSO-WORK] create attempt " + attempt
                        + ": Create stayed disabled (validate 404 — webapp cold)");
                // Last-ditch on the final attempts: the name/context ARE unique (server-side), so the disable is
                // only the client's failed uniqueness pre-check. Force-clear the disabled attribute and submit;
                // the server-side create still enforces real uniqueness.
                if (attempt >= 4) {
                    try {
                        create.evaluate("el => { el.disabled = false; el.classList.remove('Mui-disabled'); }");
                        create.dispatchEvent("click");
                        page.waitForURL("**/publisher/apis/*/overview**",
                                new Page.WaitForURLOptions().setTimeout(6000));
                        currentApiId = apiIdFromUrl(page.url());
                        if (currentApiId != null) {
                            log.info("[SSO-WORK] created API id=" + currentApiId
                                    + " (force-submit, attempt " + attempt + ")");
                            return currentApiId;
                        }
                    } catch (RuntimeException forceFailed) {
                        // fall through to reload
                    }
                }
                continue;
            }
            // Enabled — click until the app routes to the overview (the button can still flicker mid-validation).
            long deadline = System.currentTimeMillis() + 20000;
            boolean navigated = false;
            // CHECKSTYLE:OFF deadlineLoop - retry the Create click until the SPA routes to the API; not an HTTP retry
            while (System.currentTimeMillis() < deadline) {
                // CHECKSTYLE:ON
                waitForEnabled(create, 4000);
                try {
                    create.dispatchEvent("click");
                } catch (RuntimeException ignored) {
                    // momentarily detached/disabled; retry
                }
                try {
                    page.waitForURL("**/publisher/apis/*/overview**",
                            new Page.WaitForURLOptions().setTimeout(4000));
                    navigated = true;
                    break;
                } catch (RuntimeException stillOnForm) {
                    // not yet — loop
                }
            }
            if (navigated) {
                currentApiId = apiIdFromUrl(page.url());
                if (currentApiId == null) {
                    throw new AssertionError("could not extract the created API id from " + page.url());
                }
                log.info("[SSO-WORK] created API id=" + currentApiId + " at " + page.url()
                        + " (attempt " + attempt + ")");
                return currentApiId;
            }
            log.info("[SSO-WORK] create attempt " + attempt + ": enabled but no navigation; reloading");
        }
        snap("create-no-nav");
        dumpDiagnostics("create-no-nav");
        throw new AssertionError("publisher create-API never completed across retries (Create stayed disabled / "
                + "no navigation — publisher /apis/validate likely still returning 404). " + pageDiagnostic());
    }

    /** Extracts the API UUID from a publisher URL like {@code .../publisher/apis/<uuid>/overview}. */
    private static String apiIdFromUrl(String url) {
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("/publisher/apis/([0-9a-fA-F-]{16,})").matcher(url);
        return m.find() ? m.group(1) : null;
    }

    /**
     * Deploys the current API's first revision to the Default gateway environment via the publisher Deployments
     * page (the {@code deploy-btn} onboarding action). Waits until a deployed revision is shown (the
     * {@code Undeploy} control appears), which is the deploy-succeeded signal.
     */
    public void deployApi() {
        page.navigate(APIM_INTERNAL + "/publisher/apis/" + currentApiId + "/deployments",
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        Locator deploy = page.locator("#deploy-btn");
        deploy.waitFor(new Locator.WaitForOptions().setTimeout(30000));
        if (!waitForEnabled(deploy, 30000)) {
            snap("deploy-btn-disabled");
            dumpDiagnostics("deploy-btn-disabled");
            throw new AssertionError("the Deploy button never enabled (the API likely has no production "
                    + "endpoint). " + pageDiagnostic());
        }
        // Use the deploy REST call as the deterministic success signal: clicking Deploy creates a revision and
        // POSTs .../deploy-revision. Wait for that response rather than guessing a post-deploy UI label.
        com.microsoft.playwright.Response resp;
        try {
            resp = page.waitForResponse(
                    r -> r.url().contains("/deploy-revision") && "POST".equalsIgnoreCase(r.request().method()),
                    new Page.WaitForResponseOptions().setTimeout(45000),
                    () -> clickWhenReady(deploy, "Deploy"));
        } catch (RuntimeException e) {
            snap("deploy-no-call");
            dumpDiagnostics("deploy-no-call");
            throw new AssertionError("the deploy-revision call was never made after clicking Deploy. "
                    + pageDiagnostic(), e);
        }
        if (resp.status() >= 300) {
            snap("deploy-http-fail");
            throw new AssertionError("deploy-revision returned HTTP " + resp.status() + ". " + pageDiagnostic());
        }
        log.info("[SSO-WORK] deployed API id=" + currentApiId + " (deploy-revision " + resp.status() + ")");
    }

    /**
     * Publishes the current API via the publisher Lifecycle page ({@code Publish-btn}). Waits until the API state
     * reads PUBLISHED. Requires a deployed revision (call {@link #deployApi()} first).
     */
    public void publishApi() {
        page.navigate(APIM_INTERNAL + "/publisher/apis/" + currentApiId + "/lifecycle",
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        Locator publish = page.locator("[data-testid=Publish-btn]");
        publish.waitFor(new Locator.WaitForOptions().setTimeout(30000));
        if (!waitForEnabled(publish, 20000)) {
            snap("publish-btn-disabled");
            throw new AssertionError("the Publish button never enabled. " + pageDiagnostic());
        }
        // The publish POSTs .../change-lifecycle?action=Publish — wait for that as the deterministic success signal.
        com.microsoft.playwright.Response resp;
        try {
            resp = page.waitForResponse(
                    r -> r.url().contains("/change-lifecycle") && "POST".equalsIgnoreCase(r.request().method()),
                    new Page.WaitForResponseOptions().setTimeout(45000),
                    () -> clickWhenReady(publish, "Publish"));
        } catch (RuntimeException e) {
            snap("publish-no-call");
            dumpDiagnostics("publish-no-call");
            throw new AssertionError("the change-lifecycle call was never made after clicking Publish. "
                    + pageDiagnostic(), e);
        }
        if (resp.status() >= 300) {
            snap("publish-http-fail");
            throw new AssertionError("change-lifecycle returned HTTP " + resp.status() + ". " + pageDiagnostic());
        }
        log.info("[SSO-WORK] published API id=" + currentApiId + " (change-lifecycle " + resp.status() + ")");
    }

    /**
     * Redeploys the current API by creating and deploying a NEW revision via the deployed-state Deployments page
     * ("Deploy New Revision"). Called after the admin provider change to prove the admin→publisher round-trip
     * needs no second login. Waits for the deploy-revision REST call as the success signal.
     */
    public void redeployApi() {
        page.navigate(APIM_INTERNAL + "/publisher/apis/" + currentApiId + "/deployments",
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        Locator dnr = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Deploy New Revision"));
        try {
            dnr.waitFor(new Locator.WaitForOptions().setTimeout(30000));
        } catch (RuntimeException e) {
            snap("redeploy-no-button");
            throw new AssertionError("'Deploy New Revision' not found on the deployments page (did the admin→"
                    + "publisher round-trip land unauthenticated?). " + pageDiagnostic(), e);
        }
        waitForEnabled(dnr, 20000);
        com.microsoft.playwright.Response resp;
        try {
            resp = page.waitForResponse(
                    r -> r.url().contains("/deploy-revision") && "POST".equalsIgnoreCase(r.request().method()),
                    new Page.WaitForResponseOptions().setTimeout(45000),
                    () -> {
                        clickWhenReady(dnr, "Deploy New Revision");
                        // "Deploy New Revision" opens a confirm dialog (revision description + gateway, Default
                        // pre-selected); click its Deploy. If the build deploys directly with no dialog, this is a
                        // best-effort no-op.
                        try {
                            Locator confirm = page.getByRole(AriaRole.DIALOG).getByRole(AriaRole.BUTTON,
                                    new Locator.GetByRoleOptions().setName("Deploy").setExact(true));
                            confirm.waitFor(new Locator.WaitForOptions().setTimeout(6000));
                            waitForEnabled(confirm, 6000);
                            clickWhenReady(confirm, "Deploy (dialog)");
                        } catch (RuntimeException noDialog) {
                            // no confirm dialog — the click above already triggered the deploy
                        }
                    });
        } catch (RuntimeException e) {
            snap("redeploy-no-call");
            dumpDiagnostics("redeploy-no-call");
            throw new AssertionError("the redeploy (deploy-revision) call was never made. " + pageDiagnostic(), e);
        }
        if (resp.status() >= 300) {
            snap("redeploy-http-fail");
            throw new AssertionError("redeploy deploy-revision returned HTTP " + resp.status() + ". "
                    + pageDiagnostic());
        }
        log.info("[SSO-WORK] redeployed API id=" + currentApiId + " (deploy-revision " + resp.status() + ")");
    }

    // ─────────────────────────── admin SPA-session REST work ───────────────────────────

    /**
     * Clicks an (already-enabled) locator robustly: a normal actionability-checked click first, and if that times
     * out — a MUI button can re-render/animate enough that Playwright never sees a "stable, unobscured" instant —
     * a direct {@code dispatchEvent("click")} that bypasses the actionability wait (React's delegated handler
     * still fires). Throws only if both fail.
     */
    private void clickWhenReady(Locator locator, String what) {
        try {
            locator.scrollIntoViewIfNeeded(new Locator.ScrollIntoViewIfNeededOptions().setTimeout(5000));
        } catch (RuntimeException ignored) {
            // not fatal; the click attempts below are the real gate
        }
        try {
            locator.click(new Locator.ClickOptions().setTimeout(8000));
            return;
        } catch (RuntimeException normalClickFailed) {
            // Fall through to a forced DOM click for a re-rendering/obscured-but-enabled button.
        }
        try {
            locator.dispatchEvent("click");
        } catch (RuntimeException e) {
            snap("click-fail-" + what.replaceAll("\\s+", "-"));
            throw new AssertionError("could not click " + what + " (normal + dispatched click both failed). "
                    + pageDiagnostic(), e);
        }
    }

    /** Fills an input and blurs it (Tab) so a formik/react-hook-form field is marked touched and validates. */
    private void fillAndBlur(String selector, String value) {
        Locator field = page.locator(selector);
        field.waitFor(new Locator.WaitForOptions().setTimeout(15000));
        field.fill(value);
        field.press("Tab");
    }

    /** Polls until the locator is enabled (actionable) or the timeout elapses; returns whether it enabled. */
    private boolean waitForEnabled(Locator locator, int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        // CHECKSTYLE:OFF deadlineLoop - poll a MUI control's enabled-state (Playwright); not an HTTP retry
        while (System.currentTimeMillis() < deadline) {
            // CHECKSTYLE:ON
            try {
                if (locator.isEnabled()) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                // element may be re-rendering; retry
            }
            try {
                page.waitForTimeout(400);
            } catch (RuntimeException e) {
                break;
            }
        }
        try {
            return locator.isEnabled();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Performs the admin operation {@code POST /api/am/admin/v4/apis/{apiId}/change-provider?provider=…} using the
     * SSO session itself — proving the federated admin session yields a token that authorizes a real admin API
     * call (there is no Admin-console UI for provider change). The APIM consoles use an OPAQUE access token split
     * into two path-scoped halves: {@code WSO2_AM_TOKEN_1_<env>} (part 1, JS-readable) and
     * {@code AM_ACC_TOKEN_<env>_P2} (part 2, httpOnly). APIM's split-token filter rebuilds the full token by
     * APPENDING the httpOnly part-2 cookie to the {@code Authorization} header — so we send ONLY the admin part 1
     * as the bearer and let Playwright's shared request context carry the part-2 cookie (scoped to
     * {@code /api/am/admin}) automatically, exactly as an Admin-console XHR does.
     *
     * @return the HTTP status of the change-provider call
     */
    public int changeApiProviderViaAdminSession(String apiId, String newProvider) {
        // Make the Admin console the active context so its split-token cookies are present/fresh.
        page.navigate(APIM_INTERNAL + "/admin/",
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        settle();
        String part1 = consoleTokenPart1("/admin");
        if (part1 == null) {
            throw new AssertionError("could not find the admin SSO access-token part 1 cookie "
                    + "(WSO2_AM_TOKEN_1_* at /admin). cookies=" + cookieNames());
        }
        String url = APIM_INTERNAL + "/api/am/admin/v4/apis/" + apiId + "/change-provider?provider="
                + java.net.URLEncoder.encode(newProvider, java.nio.charset.StandardCharsets.UTF_8);
        APIResponse resp = page.request().post(url,
                RequestOptions.create().setHeader("Authorization", "Bearer " + part1));
        String body;
        try {
            body = resp.text();
        } catch (RuntimeException e) {
            body = "<no body>";
        }
        log.info("[SSO-WORK] change-provider status=" + resp.status() + " provider=" + newProvider
                + " body=" + (body.length() > 300 ? body.substring(0, 300) : body));
        return resp.status();
    }

    /**
     * The principal a console session actually acts as, read from a resource it just created: the Publisher
     * records an API's {@code provider} and the DevPortal an application's {@code owner} as the authenticated
     * user.
     *
     * <p>Used to pin the identity behind the SSO session. The console token is an OPAQUE split token, so there is
     * no {@code sub} claim to decode; resource ownership is the product's own statement of who acted, which is a
     * stronger check than anything derivable from the cookie jar.
     *
     * @param resourcePath the resource's APIM REST path, e.g. {@code /api/am/publisher/v4/apis/<id>}. Resolved
     *                     against the servers' internal host, since this request rides the browser context and so
     *                     goes through the same CONNECT proxy as the console traffic
     * @param pathHint     which console's split-token cookie to authenticate with ({@code /publisher}, {@code
     *                     /devportal})
     * @param ownerField   the field naming the principal ({@code provider} for an API, {@code owner} for an app)
     * @return the recorded principal, never {@code null}
     */
    public String recordedPrincipal(String resourcePath, String pathHint, String ownerField) {
        String part1 = consoleTokenPart1(pathHint);
        if (part1 == null) {
            throw new AssertionError("no " + pathHint + " split-token cookie, so the session's principal cannot "
                    + "be read. cookies=" + cookieNames());
        }
        String resourceUrl = APIM_INTERNAL + resourcePath;
        APIResponse resp = page.request().get(resourceUrl,
                RequestOptions.create().setHeader("Authorization", "Bearer " + part1));
        if (resp.status() != 200) {
            throw new AssertionError("reading " + shortUrl(resourceUrl) + " as the SSO session returned HTTP "
                    + resp.status() + ", so its " + ownerField + " cannot be checked.");
        }
        String body;
        try {
            body = resp.text();
        } catch (RuntimeException e) {
            body = null;
        }
        String bodyForMessage = body == null ? "<no body>"
                : (body.length() > 300 ? body.substring(0, 300) : body);
        if (body == null || body.isBlank()) {
            throw new AssertionError("reading " + shortUrl(resourceUrl) + " as the SSO session returned HTTP "
                    + resp.status() + " with a blank response body: " + bodyForMessage);
        }
        String principal = new org.json.JSONObject(body).optString(ownerField, null);
        if (principal == null || principal.isBlank()) {
            throw new AssertionError("the resource at " + shortUrl(resourceUrl) + " carries no '" + ownerField
                    + "', so the session's principal cannot be verified.");
        }
        return principal;
    }

    /**
     * The JS-readable "part 1" of a console's OPAQUE split access token ({@code WSO2_AM_TOKEN_1_<env>}), selecting
     * the console whose cookie path contains {@code pathHint} ({@code /admin}, {@code /publisher}, {@code
     * /devportal}). Sent as the bearer; APIM's filter appends the httpOnly part-2 cookie to form the full token.
     */
    private String consoleTokenPart1(String pathHint) {
        for (Cookie c : context.cookies()) {
            if (c.path != null && c.path.contains(pathHint) && c.value != null
                    && c.name.startsWith("WSO2_AM_TOKEN_1")) {
                return c.value;
            }
        }
        return null;
    }

    // ─────────────────────────── devportal SPA work ───────────────────────────

    /** The application id of the last app created through the devportal UI (captured from the overview URL). */
    private String currentAppId;
    private String devportalTenant = "carbon.super";

    public String currentAppId() {
        return currentAppId;
    }

    /** Selects the tenant query parameter used by DevPortal application flows in the shared browser session. */
    public void setDevportalTenant(String tenantDomain) {
        this.devportalTenant = tenantDomain;
    }

    /**
     * Creates a DevPortal application (default token quota) under the SSO session and returns its id. Entering the
     * protected create page triggers the devportal's SSO login (reusing the shared IS session — no relogin).
     */
    public String createDevportalApp(String appName) {
        page.navigate(APIM_INTERNAL + "/devportal/applications/create?tenant=" + devportalTenant,
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        page.waitForSelector("input[name=name]", new Page.WaitForSelectorOptions().setTimeout(30000));
        fillAndBlur("input[name=name]", appName);
        Locator save = page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("SAVE").setExact(true));
        save.waitFor(new Locator.WaitForOptions().setTimeout(15000));
        if (!waitForEnabled(save, 20000)) {
            snap("dp-app-save-disabled");
            throw new AssertionError("the DevPortal application SAVE button never enabled. " + pageDiagnostic());
        }
        // The SAVE POSTs /api/am/devportal/v3/applications; wait for that as the success signal and read the id.
        com.microsoft.playwright.Response resp;
        try {
            resp = page.waitForResponse(
                    r -> r.url().contains("/devportal/v3/applications") && "POST".equalsIgnoreCase(r.request().method()),
                    new Page.WaitForResponseOptions().setTimeout(30000),
                    () -> clickWhenReady(save, "SAVE"));
        } catch (RuntimeException e) {
            snap("dp-app-no-call");
            dumpDiagnostics("dp-app-no-call");
            throw new AssertionError("the DevPortal application create call was never made. " + pageDiagnostic(), e);
        }
        if (resp.status() >= 300) {
            snap("dp-app-http-fail");
            throw new AssertionError("DevPortal application create returned HTTP " + resp.status() + ". "
                    + pageDiagnostic());
        }
        try {
            currentAppId = new org.json.JSONObject(resp.text()).optString("applicationId", null);
        } catch (RuntimeException ignored) {
            currentAppId = null;
        }
        if (currentAppId == null) {
            // Fall back to the overview URL the app routes to after save.
            try {
                page.waitForURL("**/devportal/applications/*/overview**",
                        new Page.WaitForURLOptions().setTimeout(15000));
                currentAppId = appIdFromUrl(page.url());
            } catch (RuntimeException ignored) {
                // leave null; asserted below
            }
        }
        if (currentAppId == null) {
            throw new AssertionError("could not determine the created DevPortal application id. " + pageDiagnostic());
        }
        log.info("[SSO-WORK] created DevPortal app id=" + currentAppId);
        return currentAppId;
    }

    /** Extracts the application UUID from a devportal URL like {@code .../devportal/applications/<uuid>/overview}. */
    private static String appIdFromUrl(String url) {
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("/devportal/applications/([0-9a-fA-F-]{16,})").matcher(url);
        return m.find() ? m.group(1) : null;
    }

    /**
     * Subscribes the current DevPortal application to the given published API via the API's Credentials page
     * (pick application + business plan, click Subscribe). Waits for the subscriptions REST call as the signal.
     */
    public void subscribeAppToApi(String apiId, String appName) {
        page.navigate(APIM_INTERNAL + "/devportal/apis/" + apiId + "/credentials?tenant=" + devportalTenant,
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        page.waitForSelector("#subscribe-to-api-btn", new Page.WaitForSelectorOptions().setTimeout(30000));
        // The "Application" select defaults to DefaultApplication — switch it to OUR app. (The "Business Plan"
        // select already defaults to a valid tier, e.g. Unlimited, so we leave it.) MUI Select is opened by
        // clicking the label-associated combobox, then the matching option is clicked in the popup listbox.
        try {
            Locator appCombo = page.getByLabel("Application", new Page.GetByLabelOptions().setExact(true));
            appCombo.waitFor(new Locator.WaitForOptions().setTimeout(15000));
            clickWhenReady(appCombo, "Application select");
            Locator opt = page.getByRole(AriaRole.OPTION, new Page.GetByRoleOptions().setName(appName));
            opt.waitFor(new Locator.WaitForOptions().setTimeout(8000));
            clickWhenReady(opt, "app option");
        } catch (RuntimeException e) {
            snap("dp-subscribe-select-fail");
            throw new AssertionError("could not select application '" + appName + "' on the API credentials "
                    + "page. " + pageDiagnostic(), e);
        }
        Locator subscribe = page.locator("#subscribe-to-api-btn");
        waitForEnabled(subscribe, 15000);
        com.microsoft.playwright.Response resp;
        try {
            resp = page.waitForResponse(
                    r -> r.url().contains("/devportal/v3/subscriptions")
                            && "POST".equalsIgnoreCase(r.request().method()),
                    new Page.WaitForResponseOptions().setTimeout(30000),
                    () -> clickWhenReady(subscribe, "Subscribe"));
        } catch (RuntimeException e) {
            snap("dp-subscribe-no-call");
            dumpDiagnostics("dp-subscribe-no-call");
            throw new AssertionError("the subscribe call was never made. " + pageDiagnostic(), e);
        }
        if (resp.status() >= 300) {
            snap("dp-subscribe-http-fail");
            throw new AssertionError("subscribe returned HTTP " + resp.status() + ". " + pageDiagnostic());
        }
        log.info("[SSO-WORK] subscribed app '" + appName + "' to API " + apiId
                + " (subscriptions " + resp.status() + ")");
    }

    /**
     * Generates production OAuth keys for the current DevPortal application (default grant types) via the
     * production-keys page's Generate Keys action. Waits for the generate-keys REST call as the signal.
     */
    public void generateAppKeys() {
        page.navigate(APIM_INTERNAL + "/devportal/applications/" + currentAppId
                        + "/productionkeys/oauth?tenant=" + devportalTenant,
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        Locator gen = page.locator("#generate-keys");
        gen.waitFor(new Locator.WaitForOptions().setTimeout(30000));
        waitForEnabled(gen, 20000);
        com.microsoft.playwright.Response resp;
        try {
            resp = page.waitForResponse(
                    r -> r.url().contains("/generate-keys") && "POST".equalsIgnoreCase(r.request().method()),
                    new Page.WaitForResponseOptions().setTimeout(45000),
                    () -> {
                        // "GENERATE KEYS" opens a "Create Secret" dialog (description + expiry); its CREATE button
                        // performs the actual generate-keys POST.
                        clickWhenReady(gen, "Generate Keys");
                        Locator create = page.getByRole(AriaRole.DIALOG).getByRole(AriaRole.BUTTON,
                                new Locator.GetByRoleOptions().setName("Create"));
                        create.waitFor(new Locator.WaitForOptions().setTimeout(10000));
                        waitForEnabled(create, 10000);
                        clickWhenReady(create, "Create (keys dialog)");
                    });
        } catch (RuntimeException e) {
            snap("dp-keys-no-call");
            dumpDiagnostics("dp-keys-no-call");
            throw new AssertionError("the generate-keys call was never made. " + pageDiagnostic(), e);
        }
        if (resp.status() >= 300) {
            snap("dp-keys-http-fail");
            throw new AssertionError("generate-keys returned HTTP " + resp.status() + ". " + pageDiagnostic());
        }
        // The generate-keys response carries the app's consumer key/secret — capture them to mint an app token.
        try {
            org.json.JSONObject b = new org.json.JSONObject(resp.text());
            appConsumerKey = b.optString("consumerKey", null);
            appConsumerSecret = b.optString("consumerSecret", null);
        } catch (RuntimeException ignored) {
            // captured null; invoke step asserts presence
        }
        log.info("[SSO-WORK] generated app keys (generate-keys " + resp.status()
                + ", consumerKey " + (appConsumerKey == null ? "MISSING" : "captured") + ")");
    }

    /** App OAuth credentials captured from generate-keys, used to mint an app access token for invocation. */
    private String appConsumerKey;
    private String appConsumerSecret;

    /**
     * Invokes the published API through the gateway using an application access token minted (client_credentials)
     * from the keys generated in {@link #generateAppKeys()} — the end-to-end consumer proof that the SSO-created,
     * subscribed application can actually call the API. Mints the token at the resident KM token endpoint (reached
     * through the proxy) and calls {@code https://localhost:8243/<context>/<version><resourcePath>} (tunnelled to
     * the mapped gateway). Returns the gateway HTTP status.
     */
    public int invokeApi(String context, String version, String resourcePath) {
        if (!gatewayRouted) {
            throw new AssertionError("cannot invoke API: the gateway base URL is missing or blank; "
                    + "gatewayMappedBase is required to route localhost:8243 through the browser proxy.");
        }
        if (appConsumerKey == null || appConsumerSecret == null) {
            throw new AssertionError("no app consumer key/secret captured — generate keys before invoking.");
        }
        String basic = java.util.Base64.getEncoder().encodeToString(
                (appConsumerKey + ":" + appConsumerSecret).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        APIResponse tokenResp = page.request().post(APIM_INTERNAL + "/oauth2/token",
                RequestOptions.create()
                        .setHeader("Authorization", "Basic " + basic)
                        .setForm(com.microsoft.playwright.options.FormData.create()
                                .set("grant_type", "client_credentials")));
        String tokenBody = null;
        try {
            tokenBody = tokenResp.text();
        } catch (RuntimeException ignored) {
            // Keep the response status and the unavailable-body diagnostic in the assertion below.
        }
        String tokenBodyForMessage = tokenBody == null ? "<no body>"
                : (tokenBody.length() > 200 ? tokenBody.substring(0, 200) : tokenBody);
        if (tokenResp.status() != 200) {
            throw new AssertionError("could not mint an app access token (client_credentials) — HTTP "
                    + tokenResp.status() + " body=" + tokenBodyForMessage);
        }
        if (tokenBody == null || tokenBody.isBlank()) {
            throw new AssertionError("could not mint an app access token (client_credentials) — HTTP "
                    + tokenResp.status() + " returned a blank response body: " + tokenBodyForMessage);
        }
        String accessToken = new org.json.JSONObject(tokenBody).getString("access_token");
        String url = "https://localhost:8243" + context + "/" + version + resourcePath;
        // A freshly (re)deployed API takes a moment to become routable at the gateway — retry until 200 or timeout.
        int status = 0;
        String body = "";
        long deadline = System.currentTimeMillis() + 40000;
        // CHECKSTYLE:OFF deadlineLoop - poll the gateway for invoke-propagation via page.request; not an HTTP retry loop
        while (System.currentTimeMillis() < deadline) {
            // CHECKSTYLE:ON
            APIResponse inv = page.request().get(url,
                    RequestOptions.create().setHeader("Authorization", "Bearer " + accessToken));
            status = inv.status();
            body = safeText(inv).replaceAll("\\s+", " ").trim();
            if (status == 200) {
                break;
            }
            try {
                page.waitForTimeout(2500);
            } catch (RuntimeException e) {
                break;
            }
        }
        log.info("[SSO-WORK] invoke " + url + " -> status=" + status + " body=" + body);
        return status;
    }

    private static String safeText(APIResponse r) {
        try {
            String t = r.text();
            return t.length() > 200 ? t.substring(0, 200) : t;
        } catch (RuntimeException e) {
            return "<no body>";
        }
    }

    /** Best-effort debug screenshot to {@code /tmp/snap-<label>.png} (never throws). */
    private void snap(String label) {
        try {
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(java.nio.file.Paths.get("/tmp", "snap-" + label + ".png")).setFullPage(true));
        } catch (RuntimeException ignored) {
        }
    }

    // ─────────────────────────── helpers ───────────────────────────

    private void assertNot17744() {
        // A document navigation to /commonauth that returned 400 IS the #17744 header-overrun regression.
        for (String hop : new java.util.ArrayList<>(docTrail)) {
            if (hop.startsWith("400 ") && hop.contains("/commonauth")) {
                throw new AssertionError("HTTP 400 on /commonauth — the #17744 Tomcat maxHttpHeaderSize "
                        + "regression (multi-option scope header overrun); the federated login cannot proceed. "
                        + "trail:" + trail());
            }
        }
    }

    /** The recorded document-navigation trail (status + url), newest last. */
    private String trail() {
        StringBuilder b = new StringBuilder();
        for (String hop : new java.util.ArrayList<>(docTrail)) {
            b.append("\n  ").append(hop);
        }
        return b.toString();
    }

    private void fillFirst(String value, String... selectors) {
        for (String sel : selectors) {
            try {
                if (page.locator(sel).count() > 0) {
                    page.locator(sel).first().fill(value);
                    return;
                }
            } catch (RuntimeException ignored) {
            }
        }
        throw new AssertionError("could not find an input for one of " + String.join(", ", selectors)
                + " on the IS login page. " + pageDiagnostic());
    }

    private void clickFirst(String... selectors) {
        for (String sel : selectors) {
            try {
                if (page.locator(sel).count() > 0) {
                    page.locator(sel).first().click();
                    return;
                }
            } catch (RuntimeException ignored) {
            }
        }
        throw new AssertionError("could not find a submit control for one of " + String.join(", ", selectors)
                + " on the IS login page. " + pageDiagnostic());
    }

    /** Compact diagnostic: current URL, title, and a body snippet — for failure messages. */
    public String pageDiagnostic() {
        String url;
        String title;
        String snippet;
        try {
            url = page.url();
            title = page.title();
            String html = page.content();
            snippet = html.length() > 2000 ? html.substring(0, 2000) : html;
        } catch (RuntimeException e) {
            url = "<unavailable>";
            title = "<unavailable>";
            snippet = "<content unavailable: " + e.getMessage() + ">";
        }
        return "url=" + url + " title=" + title + " cookies=" + cookieNames() + " trail:" + trail()
                + "\n---- page snippet ----\n" + snippet + "\n---- end snippet ----";
    }

    /** Path + trimmed query of a URL (host stripped) for compact API-call logging. */
    private static String shortUrl(String url) {
        try {
            URI u = URI.create(url);
            String q = u.getQuery();
            return u.getPath() + (q == null ? "" : "?" + (q.length() > 120 ? q.substring(0, 120) + "…" : q));
        } catch (RuntimeException e) {
            return url;
        }
    }

    /** Prints the captured management-plane XHRs and console errors — the diagnostic for a wedged SPA form. */
    private void dumpDiagnostics(String label) {
        java.util.List<String> calls = new java.util.ArrayList<>(apiCalls);
        int from = Math.max(0, calls.size() - 30);
        log.info("[SSO-DIAG] " + label + " recent /api/am calls (" + calls.size() + " total):");
        for (String c : calls.subList(from, calls.size())) {
            log.info("[SSO-DIAG]   " + c);
        }
        java.util.List<String> msgs = new java.util.ArrayList<>(consoleMsgs);
        int mfrom = Math.max(0, msgs.size() - 20);
        log.info("[SSO-DIAG] " + label + " console errors/warnings (" + msgs.size() + " total):");
        for (String m : msgs.subList(mfrom, msgs.size())) {
            log.info("[SSO-DIAG]   " + m);
        }
    }

    private String cookieNames() {
        StringBuilder b = new StringBuilder("[");
        for (Cookie c : context.cookies()) {
            b.append(c.name).append(' ');
        }
        return b.toString().trim() + "]";
    }


    @Override
    public void close() {
        try {
            if (context != null) {
                context.close();
            }
        } catch (RuntimeException ignored) {
        }
        try {
            if (browser != null) {
                browser.close();
            }
        } catch (RuntimeException ignored) {
        }
        try {
            if (playwright != null) {
                playwright.close();
            }
        } catch (RuntimeException ignored) {
        }
        proxy.close();
    }
}
