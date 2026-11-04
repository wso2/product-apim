'use strict';
const { test } = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const { spawn } = require('node:child_process');
const path = require('node:path');
const { once } = require('node:events');
const { aclProfileNameFor, reconcileAclProfile } = require('../acl-reconciliation');

class Broker {
    profiles = new Map();
    users = new Map();
    calls = [];
    pageSize = 100;
    failDelete = false;
    request = async (method, raw, body) => {
        this.calls.push({ method, raw, body });
        const url = new URL(raw, 'http://broker');
        const seg = url.pathname.split('/').filter(Boolean);
        const name = decodeURIComponent(seg[1] || '');
        const ok = (data) => ({ status: 200, body: { data } });
        const missing = () => ({ status: 400, body: { meta: { error: { status: 'NOT_FOUND', code: 6 } } } });
        if (seg[0] === 'clientUsernames') {
            if (method === 'POST') this.users.set(body.clientUsername, body);
            if (method === 'DELETE') this.users.delete(name);
            return ok(body || {});
        }
        assert.equal(seg[0], 'aclProfiles');
        if (seg.length === 1 && method === 'POST') {
            assert.equal(this.profiles.has(body.aclProfileName), false, 'duplicate profile creation');
            this.profiles.set(body.aclProfileName, { ...body, publish: new Map(), subscribe: new Map() });
            return ok(body);
        }
        const profile = this.profiles.get(name);
        if (!profile) return missing();
        if (seg.length === 2) {
            if (method === 'DELETE') {
                assert.equal([...this.users.values()].some((u) => u.aclProfileName === name), false,
                    'profile must be unreferenced before deletion');
                this.profiles.delete(name);
            }
            return ok({ aclProfileName: name });
        }
        const direction = seg[2].startsWith('publish') ? 'publish' : 'subscribe';
        const topicField = `${direction}TopicException`;
        const syntaxField = `${topicField}Syntax`;
        const entries = profile[direction];
        if (method === 'GET') {
            const offset = Number(url.searchParams.get('cursor') || 0);
            const all = [...entries.values()];
            const data = all.slice(offset, offset + this.pageSize);
            const more = offset + data.length < all.length;
            return { status: 200, body: { data, meta: more ? { paging: { cursorQuery: String(offset + data.length) } } : {} } };
        }
        if (method === 'POST') {
            const key = JSON.stringify([body[syntaxField], body[topicField]]);
            assert.equal(entries.has(key), false, 'duplicate exception creation');
            entries.set(key, body);
            return ok(body);
        }
        assert.equal(method, 'DELETE');
        if (this.failDelete) return { status: 500, body: {} };
        const comma = seg[3].indexOf(',');
        const key = JSON.stringify([decodeURIComponent(seg[3].slice(0, comma)), decodeURIComponent(seg[3].slice(comma + 1))]);
        assert.equal(entries.has(key), true, 'deleting an absent/wrongly encoded exception');
        entries.delete(key);
        return ok({});
    };
    topics(id, direction) {
        return [...this.profiles.get(aclProfileNameFor(id))[direction].values()]
            .map((x) => x[`${direction}TopicException`]).sort();
    }
}
const grant = (id, publish, subscribe = publish, state = 'APPROVED') =>
    ({ accessRequestId: id, state, permissions: { publish, subscribe } });

test('creates restrictive defaults and exact independent publish/subscribe grants', async () => {
    const b = new Broker();
    await reconcileAclProfile('app-one', [grant('one', ['orders/created'], ['orders/received'])], b.request);
    const profile = b.profiles.get(aclProfileNameFor('app-one'));
    assert.equal(profile.clientConnectDefaultAction, 'allow');
    assert.equal(profile.publishTopicDefaultAction, 'disallow');
    assert.equal(profile.subscribeTopicDefaultAction, 'disallow');
    assert.deepEqual(b.topics('app-one', 'publish'), ['orders/created']);
    assert.deepEqual(b.topics('app-one', 'subscribe'), ['orders/received']);
});

test('revoking one request retains shared and disjoint grants from remaining requests', async () => {
    const b = new Broker();
    const a = grant('a', ['shared', 'only-a'], ['received/shared', 'received/a']);
    const c = grant('c', ['shared', 'only-c'], ['received/shared', 'received/c']);
    await reconcileAclProfile('app', [a, c], b.request);
    await reconcileAclProfile('app', [c], b.request);
    assert.deepEqual(b.topics('app', 'publish'), ['only-c', 'shared']);
    assert.deepEqual(b.topics('app', 'subscribe'), ['received/c', 'received/shared']);
});

test('revoking the last request removes both topic grants and preserves connection permission', async () => {
    const b = new Broker();
    await reconcileAclProfile('app', [grant('a', ['orders/created'])], b.request);
    await reconcileAclProfile('app', [], b.request);
    assert.deepEqual(b.topics('app', 'publish'), []);
    assert.deepEqual(b.topics('app', 'subscribe'), []);
    assert.equal(b.profiles.get('app').clientConnectDefaultAction, 'allow');
});

test('revocation does not modify another application profile', async () => {
    const b = new Broker();
    const a = grant('a', ['orders/created']);
    await reconcileAclProfile('first', [a], b.request);
    await reconcileAclProfile('control', [a], b.request);
    await reconcileAclProfile('first', [], b.request);
    assert.deepEqual(b.topics('control', 'publish'), ['orders/created']);
    assert.deepEqual(b.topics('control', 'subscribe'), ['orders/created']);
});

test('compound SEMP keys preserve topic slashes, commas and Unicode on removal', async () => {
    const b = new Broker();
    await reconcileAclProfile('app', [grant('a', ['orders/a,b/Δ'])], b.request);
    await reconcileAclProfile('app', [], b.request);
    assert.ok(b.calls.some((x) => x.raw.endsWith('/smf,orders%2Fa%2Cb%2F%CE%94')));
    assert.deepEqual(b.topics('app', 'publish'), []);
});

test('repeating reconciliation performs no duplicate mutations', async () => {
    const b = new Broker();
    const requests = [grant('a', ['shared']), grant('b', ['shared'])];
    await reconcileAclProfile('app', requests, b.request);
    b.calls = [];
    await reconcileAclProfile('app', requests, b.request);
    assert.ok(b.calls.every((x) => x.method === 'GET'));
});

test('all SEMP pages are reconciled, including stale grants on later pages', async () => {
    const b = new Broker();
    b.pageSize = 1;
    await reconcileAclProfile('app', [grant('a', ['first', 'second', 'third'])], b.request);
    await reconcileAclProfile('app', [grant('b', ['second'])], b.request);
    assert.deepEqual(b.topics('app', 'publish'), ['second']);
    assert.deepEqual(b.topics('app', 'subscribe'), ['second']);
    assert.ok(b.calls.some((x) => x.raw.includes('cursor=2')));
});

test('a failed broker delete cannot be acknowledged as successful reconciliation', async () => {
    const b = new Broker();
    await reconcileAclProfile('app', [grant('a', ['orders/created'])], b.request);
    b.failDelete = true;
    await assert.rejects(reconcileAclProfile('app', [], b.request), /remove publish grant.*500/);
});

test('unavailable broker or unexpected profile errors fail without pretending a profile is absent', async () => {
    await assert.rejects(reconcileAclProfile('app', [], async () => null), /no response/);
    await assert.rejects(reconcileAclProfile('app', [], async () => ({ status: 400, body: {} })), /read ACL profile.*400/);
});

test('pending requests grant nothing; LIVE requests retain their permissions', async () => {
    const b = new Broker();
    await reconcileAclProfile('app', [grant('pending', ['blocked'], ['blocked'], 'AWAITING_APPROVAL'),
        grant('live', ['allowed'], ['allowed'], 'LIVE')], b.request);
    assert.deepEqual(b.topics('app', 'publish'), ['allowed']);
    assert.deepEqual(b.topics('app', 'subscribe'), ['allowed']);
});

// Every external wait is bounded; timers are cleared when the corresponding signal arrives.
async function withDeadline(promise, milliseconds, description) {
    let timer;
    try {
        return await Promise.race([promise, new Promise((_, reject) => {
            timer = setTimeout(() => reject(new Error(`${description} exceeded ${milliseconds}ms`)), milliseconds);
        })]);
    } finally {
        clearTimeout(timer);
    }
}

async function stopChild(child) {
    if (!child || child.exitCode !== null || child.signalCode !== null) return;
    const exited = once(child, 'exit');
    child.kill('SIGTERM');
    try {
        await withDeadline(exited, 2000, 'mock SIGTERM shutdown');
    } catch (error) {
        child.kill('SIGKILL');
        await withDeadline(exited, 2000, 'mock SIGKILL shutdown');
    }
}

function waitForPort(child) {
    return new Promise((resolve, reject) => {
        let output = '';
        let stderr = '';
        const finish = (error, port) => {
            clearTimeout(timer);
            child.stdout.off('data', onData);
            child.stderr.off('data', onStderr);
            child.off('error', onError);
            child.off('exit', onExit);
            child.stdout.resume();
            child.stderr.resume();
            if (error) reject(error); else resolve(port);
        };
        const onData = (chunk) => {
            output = (output + chunk).slice(-65536);
            const match = /listening on http:\/\/localhost:(\d+)/.exec(output);
            if (match) finish(null, match[1]);
        };
        const onStderr = (chunk) => { stderr = (stderr + chunk).slice(-4096); };
        const onError = (error) => finish(error);
        const onExit = (code, signal) => finish(new Error(`mock exited before readiness: code=${code}, signal=${signal}; ${stderr}`));
        const timer = setTimeout(() => finish(new Error(`mock readiness exceeded 10000ms; ${stderr}`)), 10000);
        child.stdout.on('data', onData);
        child.stderr.on('data', onStderr);
        child.once('error', onError);
        child.once('exit', onExit);
    });
}

test('HTTP routes preserve supplied IDs, serialize changes and retain state on broker failure', { timeout: 30000 }, async (t) => {
    const b = new Broker();
    const semp = http.createServer(async (req, res) => {
        let raw = '';
        for await (const chunk of req) raw += chunk;
        const result = await b.request(req.method, req.url.replace('/SEMP/v2/config/msgVpns/default', ''), raw ? JSON.parse(raw) : undefined);
        res.writeHead(result.status, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(result.body));
    });
    let child;
    t.after(async () => {
        try {
            await stopChild(child);
        } finally {
            semp.closeAllConnections();
            await withDeadline(new Promise((resolve, reject) => semp.close((error) => error ? reject(error) : resolve())),
                2000, 'SEMP stub shutdown');
        }
    });
    await withDeadline(new Promise((resolve, reject) => {
        semp.once('error', reject);
        semp.listen(0, '127.0.0.1', resolve);
    }), 2000, 'SEMP stub listen');
    child = spawn(process.execPath, [path.join(__dirname, '..', 'server.js')], {
        env: { ...process.env, PORT: '0', SEMP_URL: `http://127.0.0.1:${semp.address().port}`,
            APIM_JWKS_URL: '', SEMP_USER: 'admin', SEMP_PASSWORD: 'admin', SEMP_VPN: 'default' },
        stdio: ['ignore', 'pipe', 'pipe'],
    });
    const port = await waitForPort(child);
    const call = async (method, route, data) => {
        const response = await fetch(`http://127.0.0.1:${port}${route}`, {
            method, headers: { 'Content-Type': 'application/json' }, body: data ? JSON.stringify(data) : undefined,
            signal: AbortSignal.timeout(5000),
        });
        return { status: response.status, body: await response.json() };
    };
    const route = '/appRegistrations/registration-one/accessRequests';
    assert.equal((await call('GET', route)).status, 404);
    for (const id of ['first-id', 'second-id']) {
        const response = await call('POST', route, { accessRequestId: id, eventApiProductId: 'product' });
        assert.equal(response.status, 201);
        assert.equal(response.body.data.accessRequestId, id);
    }
    assert.equal((await call('POST', route, { accessRequestId: 'first-id' })).status, 409);
    assert.equal((await call('DELETE', `${route}/unknown-id`)).status, 404);
    assert.deepEqual(b.topics('registration-one', 'publish'), ['orders/created']);
    const results = await Promise.all([call('DELETE', `${route}/first-id`),
        call('POST', route, { accessRequestId: 'third-id' })]);
    assert.deepEqual(results.map((r) => r.status), [200, 201]);
    assert.deepEqual((await call('GET', route)).body.data.map((x) => x.accessRequestId).sort(), ['second-id', 'third-id']);
    assert.equal((await call('POST', '/appRegistrations/registration-one/credentials',
        { secret: { consumerKey: 'client-one', consumerSecret: 'fixture-secret' } })).status, 201);
    assert.deepEqual(b.topics('registration-one', 'publish'), ['orders/created']);
    b.failDelete = true;
    assert.equal((await call('DELETE', `${route}/second-id`)).status, 200, 'shared grant still present');
    assert.equal((await call('DELETE', `${route}/third-id`)).status, 500);
    assert.deepEqual((await call('GET', route)).body.data.map((x) => x.accessRequestId), ['third-id']);
    b.failDelete = false;
    assert.equal((await call('DELETE', `${route}/third-id`)).status, 200);
    assert.deepEqual(b.topics('registration-one', 'publish'), []);
    assert.deepEqual(b.topics('registration-one', 'subscribe'), []);
    assert.equal((await call('DELETE', '/appRegistrations/registration-one')).status, 200);
    assert.equal(b.users.size, 0);
    assert.equal(b.profiles.size, 0);
});

test('a mock that exits before readiness fails immediately with its exit code', { timeout: 5000 }, async (t) => {
    const child = spawn(process.execPath, ['-e', 'process.exit(23)'], { stdio: ['ignore', 'pipe', 'pipe'] });
    t.after(() => stopChild(child));
    await assert.rejects(waitForPort(child), /mock exited before readiness: code=23/);
});
