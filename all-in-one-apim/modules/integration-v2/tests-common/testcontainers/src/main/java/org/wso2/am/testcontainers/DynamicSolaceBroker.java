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

package org.wso2.am.testcontainers;

import com.github.dockerjava.api.model.Ulimit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

/**
 * Solace under test, as ONE abstraction over TWO containers: a faked CONTROL plane and a real DATA plane.
 *
 * <p><b>Why the control plane is faked.</b> APIM's Solace integration is written against Solace Cloud,
 * which cannot be reached from integration tests, and there is no public Solace API-Management-Connector
 * image (Docker Hub's {@code solace/*} namespace ships the broker, operator, exporter, event-management
 * agent and ~12 protocol bridges — no connector). So that API is served by the {@code solaceshim} image.
 *
 * <p>The surface it serves is the v2 EVENT PORTAL one APIM actually calls — {@code /eventApiProducts},
 * {@code /eventApiProducts/{id}/plans}, that plan's {@code /eventApis/{id}} AsyncAPI, and the
 * {@code /appRegistrations} family (registration, {@code /credentials}, {@code /accessRequests}) — taken from
 * the Feign interface {@code SolaceV2ApimApisClient} in {@code org.wso2.carbon.apimgt.solace} and
 * cross-checked against Solace's published API walkthrough. NOT the v1 connector surface
 * ({@code /{org}/environments}, {@code /apis}, {@code /apiProducts}, {@code /developers}) that the legacy
 * {@code SolaceTestCase} WireMock stubs described: that mechanism is gone on this product, which is also why
 * the legacy deploy-to-Solace-environment rows are not ported (see solace_event_api.feature).
 *
 * <p><b>Why the data plane is REAL.</b> The broker is the only component in the arc we did not author, so
 * it is the only one that can contradict us: it REJECTS malformed topic syntax and unsupported protocol
 * mappings, which a fake would silently accept. APIM performs topic/protocol translation (it emits
 * {@code ?topicSyntax=mqtt} and carries an smf/mqtt/amqp/ws/jms/kafka vocabulary), and a real broker is
 * what makes that translation falsifiable.
 *
 * <p><b>Assertion rule (non-negotiable).</b> A shim built to satisfy APIM passes BY CONSTRUCTION. Scenarios
 * MUST assert broker state through SEMP ({@link #getSempUrl()}), never the shim's own responses. The shim's
 * {@code GET /_mock/state} and {@code POST /_mock/reset} endpoints exist for debugging only and are not
 * assertion targets.
 *
 * <p><b>Why two GenericContainers rather than {@code DockerComposeContainer}.</b> Compose support creates
 * its OWN network, so APIM — which lives on the block's private network — could not resolve the shim by network
 * alias, and the shim could not resolve the broker. Wrapping two containers here lets the caller put both on
 * the SAME block network with stable aliases while still presenting a single object, which is the property that
 * actually mattered.
 *
 * <p><b>Scope (CLAUDE.md 14).</b> Infrastructure only. This class boots containers and publishes URLs. It
 * performs NO product operation — registering the Solace environment, importing, deploying, subscribing and
 * key generation are all feature steps run as an {@code Identity} actor. Doing any of that here would repeat
 * the mistakes that got {@code KeyManagerRegistration} and {@code DefaultKmProvisioner} removed.
 */
public class DynamicSolaceBroker {

    private static final Log logger = LogFactory.getLog(DynamicSolaceBroker.class);

    /** Alias APIM uses to reach the faked connector on the block network. */
    public static final String SHIM_ALIAS = "solaceshim";
    /** Alias the shim (and any messaging client) uses to reach the broker. */
    public static final String BROKER_ALIAS = "solacebroker";
    /**
     * Alias THE BROKER uses to reach APIM, for fetching APIM's JWKS. Bound onto the block's APIM container by
     * {@link DynamicApimContainer#withSolaceJwksAlias()} — see there for why this is not the {@code wso2am}
     * alias. Lives here because the shim's environment (built in this class) has to name it.
     */
    public static final String APIM_JWKS_ALIAS = "apimforsolace";

    /** Connector API port served by the shim. */
    public static final int SHIM_PORT = 8081;
    /** SEMP / management port — the independent validator tests assert against. */
    public static final int SEMP_PORT = 8080;
    /** Messaging ports surfaced so scenarios can invoke once a product flow needs it. */
    public static final int SMF_PORT = 55555;
    public static final int MQTT_PORT = 1883;
    public static final int WS_MQTT_PORT = 8000;
    public static final int AMQP_PORT = 5672;
    /**
     * REST messaging (publish) port. Chosen over smf/mqtt for the invocation scenarios because publishing is
     * a plain authenticated HTTP POST, so it rides the suite's existing {@code Requests}/{@code SimpleHTTPClient}
     * funnels with no messaging client dependency.
     *
     * <p>MEASURED on this image: {@code POST /TOPIC/<topic>} with {@code Authorization: Bearer <APIM access
     * token>} returns 200, and 403 for a tampered, malformed or absent token.
     *
     * <p>The credential form is REST-SPECIFIC and cost a wrong turn worth recording. Solace's tutorials show
     * an OAuth token being passed as the PASSWORD in the form {@code OAUTH~<profile>~<token>} with the username
     * ignored — that is for smf/mqtt, where a client has only username/password fields. Over REST it is
     * rejected: the broker reads an {@code Authorization: Basic} header as basic auth, full stop, and answers
     * 403 "Basic Authentication Is Shutdown" (basic auth being disabled by the shim). REST clients present the
     * token as a Bearer header instead, and the broker selects the OAuth profile from the token's {@code iss}.
     */
    public static final int REST_PORT = 9000;

    /** SEMP admin credentials. Fixed, because the broker is disposable and nothing secret lives in it. */
    public static final String SEMP_USER = "admin";
    public static final String SEMP_PASSWORD = "admin";
    /** The message VPN everything is provisioned into — the one the broker image ships. */
    public static final String SEMP_VPN = "default";
    /**
     * Name of the OAuth profile the shim configures on the broker. Shared with the shim through the
     * {@code OAUTH_PROFILE} env below so the two cannot drift, and needed by TESTS because protocols with only
     * a username/password field (smf, mqtt) carry the token as the password in the form
     * {@code OAUTH~<profile>~<token>} — the profile name is part of the credential there. (Over REST the
     * profile is not named: the broker selects it from the token's {@code iss}. See {@link #REST_PORT}.)
     */
    public static final String OAUTH_PROFILE_NAME = "wso2apim";

    private static final String DEFAULT_BROKER_IMAGE = "solace/solace-pubsub-standard:latest";
    /**
     * Must match the tag the {@code build-solace-shim-docker-image} exec produces (the
     * {@code solace.shim.docker.image.name} property in the integration-v2 pom, which surefire also passes in as
     * a system property). Nothing else builds this image, so a tag that disagrees with the pom fails at
     * container start with an image-not-found — which is how it behaved before the exec existed.
     */
    private static final String DEFAULT_SHIM_IMAGE = "solace-apim-shim:latest";

    private final GenericContainer<?> broker;
    private final GenericContainer<?> shim;

    /**
     * PER-BLOCK, and deliberately NOT a shared singleton multi-homed across block networks like
     * {@link NodeAppServer}. The difference is direction of traffic: blocks only CALL the node backend, so one
     * stateless instance can serve every block's network at once; this pair CALLS BACK INTO APIM — the shim's
     * {@code APIM_JWKS_URL} resolves {@link #APIM_JWKS_ALIAS} to fetch APIM's JWKS. A container attached to two
     * block networks would resolve that alias on both, so which APIM answers would be arbitrary. Giving each
     * block its own broker on its own private network makes the alias unambiguous by construction, which is what
     * removed the JVM-wide alias permit these blocks previously serialized on.
     *
     * <p>Per-block also isolates broker STATE (VPN client-usernames, OAuth profile) that scenarios provision
     * over SEMP, which a shared broker could not offer. The cost is one broker boot per opting-in block (~1-2
     * min); only blocks that set {@code initSolaceBroker} pay it.
     *
     * @param blockLabel owning block's label — log prefix and diagnostics only
     * @param network    the block's private docker network both containers join (the caller owns its lifecycle;
     *                   {@link #stop()} must run before it is closed)
     */
    public DynamicSolaceBroker(String blockLabel, Network network) {

        logger.info("Initializing DynamicSolaceBroker for block '" + blockLabel
                + "' (faked control plane + real PubSub+ data plane)...");

        // ---- DATA plane: real PubSub+ standard ------------------------------------------------------
        // shm_size and the nofile ulimit are REQUIRED by the broker, not tuning: with the docker default
        // 64MB /dev/shm it fails during startup, and it refuses to start below its file-descriptor floor.
        broker = new GenericContainer<>(
                System.getProperty("solace.broker.docker.image.name", DEFAULT_BROKER_IMAGE))
                .withExposedPorts(SEMP_PORT, SMF_PORT, MQTT_PORT, WS_MQTT_PORT, AMQP_PORT, REST_PORT)
                .withNetwork(network)
                .withNetworkAliases(BROKER_ALIAS)
                // MEASURED: these two names are exact. An invented single
                // "username_admin_globalaccess_password" makes SolOS abort during config render with
                // ERROR "Either password, passwordfilepath, or encryptedpassword must be specified."
                // followed by FATAL Config check failed, and the container exits 0 -- which reads as a
                // clean shutdown rather than a misconfiguration.
                .withEnv("username_admin_password", SEMP_PASSWORD)
                .withEnv("username_admin_globalaccesslevel", "admin")
                .withEnv("system_scaling_maxconnectioncount", "100")
                .withCreateContainerCmdModifier(cmd -> {
                    cmd.getHostConfig().withShmSize(1024L * 1024L * 1024L);
                    cmd.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 2448, 1048576)});
                })
                // Wait on SEMP answering, NOT on a log line. MEASURED: this image (SolOS 10.26.0) never
                // logs "Primary Virtual Router is now active" -- greps for ready|active|started|Virtual
                // Router return nothing -- while SEMP /config/about was already returning 200. A
                // log-message wait therefore hangs the full timeout and fails a working broker. SEMP is
                // also precisely what scenarios assert against, so this waits on the real dependency.
                .waitingFor(Wait.forHttp("/SEMP/v2/config/about")
                        .forPort(SEMP_PORT)
                        .withBasicCredentials(SEMP_USER, SEMP_PASSWORD)
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofMinutes(6)));
        broker.withLogConsumer(new JclLogConsumer(logger));

        // ---- CONTROL plane: the shim ----------------------------------------------------------------
        shim = new GenericContainer<>(
                System.getProperty("solace.shim.docker.image.name", DEFAULT_SHIM_IMAGE))
                .withExposedPorts(SHIM_PORT)
                .withNetwork(network)
                .withNetworkAliases(SHIM_ALIAS)
                // The shim advertises broker endpoint URIs to APIM using the in-network alias, so the URIs
                // APIM stores are reachable from inside the network rather than only from the host.
                .withEnv("SEMP_URL", "http://" + BROKER_ALIAS + ":" + SEMP_PORT)
                .withEnv("SEMP_USER", SEMP_USER)
                .withEnv("SEMP_PASSWORD", SEMP_PASSWORD)
                // The shim reads SEMP_VPN and otherwise falls back to its own "default" literal, so without this
                // the two sides hardcode the VPN independently and changing the constant above would silently
                // leave the shim provisioning into the old one. Same reason OAUTH_PROFILE is passed.
                .withEnv("SEMP_VPN", SEMP_VPN)
                /*
                 * What the shim needs to make the broker trust APIM as an OAuth authorization server, so an
                 * application can publish with the access token APIM issued it.
                 *
                 * The JWKS is addressed by NETWORK ALIAS on APIM's PLAIN-HTTP port: the broker fetches it
                 * itself, from inside the network, so a mapped host port is wrong here — and 9763 avoids
                 * having to provision a certificate authority into the broker for APIM's self-signed 9443
                 * certificate. Verified reachable from the broker container, with a `kid` matching the token's.
                 * The alias only exists because the block binds it (withSolaceJwksAlias) — APIM has none by
                 * default, and the first run of this scenario failed 403 for exactly that reason.
                 *
                 * The issuer must equal the `iss` claim VERBATIM, because that is how the broker selects which
                 * OAuth profile validates a token (see the long note in the shim's configureApimOAuth). APIM's
                 * default token issuer is localhost:9443 regardless of the container's network identity — it
                 * comes from APIM's own config, not from how it is reached. If APIM's issuer is ever changed,
                 * this must change with it; the failure is a 403 on publish whose reason appears only in the
                 * broker's event.log ("did not match the issuer of any active profile").
                 */
                .withEnv("OAUTH_PROFILE", OAUTH_PROFILE_NAME)
                .withEnv("APIM_JWKS_URL", "http://" + APIM_JWKS_ALIAS + ":9763/oauth2/jwks")
                .withEnv("APIM_TOKEN_ISSUER", "https://localhost:9443/oauth2/token")
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));
        shim.withLogConsumer(new JclLogConsumer(logger));
    }

    /**
     * Boots the broker first, then the shim — the shim PROVISIONS the broker, so ordering matters.
     *
     * <p>The broker is not decorative and is not optional. The shim configures it as an OAuth RESOURCE SERVER
     * that trusts APIM as the authorization server, and forwards the credentials APIM pushes
     * ({@code POST /appRegistrations/{id}/credentials}) into a real SEMP {@code clientUsername}, removing it
     * again on delete. A publish therefore only succeeds if the broker independently verified an APIM-issued
     * token against APIM's own JWKS AND found the client id it carries provisioned. That is what makes the
     * broker the one component in this arc that can CONTRADICT us — it rejects credentials the control-plane
     * double would happily accept — and what makes invoking a Solace event API testable at all.
     */
    public void start() {
        logger.info("Starting Solace broker (this takes ~1-2 min; it is a full event broker)...");
        broker.start();
        logger.info("Solace broker up. SEMP at " + getSempUrl());
        try {
            shim.start();
        } catch (RuntimeException | Error startupFailure) {
            try {
                stop();
            } catch (RuntimeException | Error cleanupFailure) {
                startupFailure.addSuppressed(cleanupFailure);
            }
            throw startupFailure;
        }
        logger.info("Solace connector shim up at " + getConnectorBaseUrl());
    }

    public void stop() {
        if (shim.isRunning()) {
            shim.stop();
        }
        if (broker.isRunning()) {
            broker.stop();
        }
    }

    public boolean isRunning() {
        return broker.isRunning() && shim.isRunning();
    }

    // ---- URLs callers need --------------------------------------------------------------------------

    /**
     * Base URL for APIM's Solace environment config (v1 {@code SolaceAdminApis} baseUrl). The organization
     * is a PATH PREFIX appended by APIM, so this deliberately ends without one.
     */
    public String getConnectorBaseUrl() {
        return "http://" + SHIM_ALIAS + ":" + SHIM_PORT;
    }

    /** Host-side connector URL, for a test that needs to inspect the shim directly (debugging only). */
    public String getConnectorHostUrl() {
        return "http://" + shim.getHost() + ":" + shim.getMappedPort(SHIM_PORT);
    }

    /** SEMP base URL from the HOST — this is what scenarios assert broker state against. */
    public String getSempUrl() {
        return "http://" + broker.getHost() + ":" + broker.getMappedPort(SEMP_PORT) + "/SEMP/v2";
    }

    /** SMF messaging URI reachable from inside the block network. */
    public String getSmfUri() {
        return "tcp://" + BROKER_ALIAS + ":" + SMF_PORT;
    }

    /** MQTT messaging URI reachable from inside the block network. */
    public String getMqttUri() {
        return "tcp://" + BROKER_ALIAS + ":" + MQTT_PORT;
    }

    /**
     * REST messaging base URL as seen FROM THE HOST — this is what invocation scenarios publish to, since the
     * test JVM runs on the host rather than inside the network. Publish with
     * {@code POST {thisUrl}/TOPIC/<topic>} and {@code Authorization: Bearer <APIM access token>} (see
     * {@link #REST_PORT} for why a Bearer header rather than the {@code OAUTH~} password form).
     */
    public String getRestMessagingUrl() {
        return "http://" + broker.getHost() + ":" + broker.getMappedPort(REST_PORT);
    }

    /**
     * MQTT port as seen FROM THE HOST, for the subscribe-side scenarios: the test JVM runs on the host, and
     * unlike publishing (a plain HTTP POST over REST) receiving needs a real messaging protocol, so a scenario
     * connects here directly.
     */
    public int getMappedMqttPort() {
        return broker.getMappedPort(MQTT_PORT);
    }

    /**
     * Broker host as seen FROM THE HOST, to pair with {@link #getMappedMqttPort()} — the testcontainers-resolved
     * host, not a {@code localhost} literal, for the same reason the URL getters above use it: under a remote or
     * rootless daemon (or some Colima setups) the mapped port is not published on loopback.
     */
    public String getBrokerHost() {
        return broker.getHost();
    }

    public int getMappedSempPort() {
        return broker.getMappedPort(SEMP_PORT);
    }

    public int getMappedShimPort() {
        return shim.getMappedPort(SHIM_PORT);
    }
}
