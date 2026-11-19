'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const { fetchPublicKeys } = require('../jwks');

const root = { kid: 'root', kty: 'RSA', n: 'root-public-modulus', e: 'AQAB' };
const tenant = { kid: 'tenant', kty: 'RSA', n: 'tenant-public-modulus', e: 'AQAB' };
const source = keys => ({ ok: true, json: async () => ({ keys }) });

test('retains distinct tenant public keys and deduplicates identical key IDs', async () => {
    const calls = [];
    const result = await fetchPublicKeys(['root', 'tenant'], { fetchImpl: async (url, options) => {
        calls.push(url);
        assert.ok(options.signal instanceof AbortSignal);
        return source(url === 'root' ? [root] : [tenant, { ...root }]);
    } });
    assert.deepEqual(calls, ['root', 'tenant']);
    assert.deepEqual(result, { keys: [root, tenant] });
});

test('rejects conflicting key material sharing a key ID', async () => {
    await assert.rejects(fetchPublicKeys(['a', 'b'], { fetchImpl: async url =>
        source([url === 'a' ? root : { ...root, n: 'different-modulus' }]) }), /Conflicting/);
});

test('fails closed if any configured source fails or returns no usable key set', async () => {
    for (const response of [{ ok: false, status: 503 }, source([]), source([{ kty: 'RSA' }]),
        { ok: true, json: async () => ({ unexpected: [] }) }]) {
        await assert.rejects(fetchPublicKeys(['root', 'tenant'], { fetchImpl: async url =>
            url === 'root' ? source([root]) : response }), /JWKS/);
    }
    await assert.rejects(fetchPublicKeys([]), /No APIM JWKS sources/);
});

test('bounds an unresponsive JWKS source', { timeout: 2000 }, async t => {
    // Keep the event loop alive while AbortSignal.timeout's unreferenced timer drives the request deadline.
    const keepAlive = setInterval(() => {}, 1000);
    t.after(() => clearInterval(keepAlive));
    try {
        await assert.rejects(fetchPublicKeys(['root'], { timeoutMs: 20,
            fetchImpl: async (url, { signal }) => new Promise((resolve, reject) => {
                signal.addEventListener('abort', () => reject(signal.reason), { once: true });
            }) }), { name: 'TimeoutError' });
    } finally { clearInterval(keepAlive); }
});
