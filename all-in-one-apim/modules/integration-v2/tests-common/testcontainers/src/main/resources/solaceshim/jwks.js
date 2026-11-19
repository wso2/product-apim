'use strict';

/** Combines the configured APIM public key sets; a failed source or conflicting key ID fails closed. */
async function fetchPublicKeys(urls, { fetchImpl = fetch, timeoutMs = 5000 } = {}) {
    if (!urls.length) throw new Error('No APIM JWKS sources configured');
    const sets = await Promise.all(urls.map(async url => {
        const response = await fetchImpl(url, { signal: AbortSignal.timeout(timeoutMs) });
        if (!response.ok) throw new Error(`APIM JWKS source returned HTTP ${response.status}`);
        const body = await response.json();
        if (!Array.isArray(body.keys) || !body.keys.length) throw new Error('APIM JWKS source has no keys');
        return body.keys;
    }));
    const keys = new Map();
    for (const key of sets.flat()) {
        if (typeof key.kid !== 'string' || !key.kid.trim() || typeof key.kty !== 'string' || !key.kty.trim()) {
            throw new Error('APIM JWKS key lacks kid or kty');
        }
        const canonical = value => JSON.stringify(Object.fromEntries(Object.keys(value).sort().map(k => [k, value[k]])));
        if (keys.has(key.kid) && canonical(keys.get(key.kid)) !== canonical(key)) {
            throw new Error('Conflicting APIM JWKS key IDs');
        }
        keys.set(key.kid, key);
    }
    return { keys: [...keys.values()] };
}

module.exports = { fetchPublicKeys };
