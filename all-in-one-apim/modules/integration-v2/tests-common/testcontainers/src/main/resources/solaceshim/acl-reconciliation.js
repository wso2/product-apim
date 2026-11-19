/* Broker ACL reconciliation for the Solace control-plane double. No dependencies. */
'use strict';

function aclProfileNameFor(registrationId) {
    return String(registrationId).replace(/[^A-Za-z0-9_]/g, '').slice(0, 32);
}

function requireSuccess(result, operation) {
    if (!result || result.status < 200 || result.status >= 300) {
        throw new Error(`${operation} failed: ${result ? result.status : 'no response'}`);
    }
    return result.body;
}

async function readExceptions(path, request) {
    const entries = [];
    const cursors = new Set();
    let cursor;
    do {
        const query = '?count=100' + (cursor ? '&cursor=' + encodeURIComponent(cursor) : '');
        const body = requireSuccess(await request('GET', path + query), `read ${path}`);
        if (!Array.isArray(body && body.data)) throw new Error(`Missing SEMP data array for ${path}`);
        entries.push(...body.data);
        cursor = body.meta && body.meta.paging && body.meta.paging.cursorQuery;
        if (cursor && cursors.has(cursor)) throw new Error(`Repeated SEMP cursor for ${path}`);
        if (cursor) cursors.add(cursor);
    } while (cursor);
    return entries;
}

/** Replace the owned profile's topic exceptions with the union of its remaining approved requests. */
async function reconcileAclProfile(registrationId, accessRequests, request) {
    const name = aclProfileNameFor(registrationId);
    const path = `/aclProfiles/${encodeURIComponent(name)}`;
    const existing = await request('GET', path);
    const missing = existing && (existing.status === 404 || (existing.status === 400
        && existing.body && existing.body.meta && existing.body.meta.error
        && existing.body.meta.error.status === 'NOT_FOUND'));
    if (missing) {
        requireSuccess(await request('POST', '/aclProfiles', {
            aclProfileName: name,
            clientConnectDefaultAction: 'allow',
            publishTopicDefaultAction: 'disallow',
            subscribeTopicDefaultAction: 'disallow',
        }), `create ACL profile ${name}`);
    } else {
        requireSuccess(existing, `read ACL profile ${name}`);
    }

    for (const direction of ['publish', 'subscribe']) {
        const topicField = `${direction}TopicException`;
        const syntaxField = `${topicField}Syntax`;
        const collection = `${path}/${direction}TopicExceptions`;
        const desired = new Set();
        for (const accessRequest of accessRequests) {
            if (!['APPROVED', 'LIVE'].includes(accessRequest.state)) continue;
            for (const topic of (accessRequest.permissions && accessRequest.permissions[direction]) || []) {
                desired.add(topic);
            }
        }
        const current = await readExceptions(collection, request);
        const retained = new Set();
        for (const entry of current) {
            const topic = entry[topicField];
            const syntax = entry[syntaxField];
            if (syntax === 'smf' && desired.has(topic)) {
                retained.add(topic);
            } else {
                // SEMP's compound key is syntax,topic; encode each field, including slashes/commas in topics.
                const key = `${encodeURIComponent(syntax)},${encodeURIComponent(topic)}`;
                requireSuccess(await request('DELETE', `${collection}/${key}`), `remove ${direction} grant ${topic}`);
            }
        }
        for (const topic of desired) {
            if (!retained.has(topic)) {
                requireSuccess(await request('POST', collection, {
                    [topicField]: topic, [syntaxField]: 'smf',
                }), `add ${direction} grant ${topic}`);
            }
        }
    }
    return name;
}

module.exports = { aclProfileNameFor, reconcileAclProfile };
