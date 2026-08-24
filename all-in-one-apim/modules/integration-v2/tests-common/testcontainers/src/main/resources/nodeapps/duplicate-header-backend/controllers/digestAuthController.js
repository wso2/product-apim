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

// HTTP Digest (RFC 2617, MD5 + qop=auth) protected backend, the upstream for DIGEST endpoint security tests.
// Unauthenticated -> 401 + WWW-Authenticate challenge; a correct Authorization: Digest response hash -> 200
// carrying the authenticated principal, so a test can assert WHICH user completed the handshake.
const crypto = require('crypto');

const REALM = 'wso2-digest-backend';
const OPAQUE = '5ccc069c403ebaf9f0171e9517f40e41';

// Two distinct principals so a test can prove the gateway injected THAT api's configured credential rather
// than a credential the backend would accept from anyone.
const USERS = {
    digestUser: 'digestPass',
    digestTenantUser: 'digestTenantPass'
};

const md5 = (value) => crypto.createHash('md5').update(value, 'utf8').digest('hex');

// Pulls `k="v"` and `k=v` pairs out of a Digest Authorization header value — the gateway sends nc/qop/algorithm
// unquoted and username/realm/nonce/cnonce/uri/response quoted.
function parseDigestParams(headerValue) {
    const params = {};
    const pair = /(\w+)\s*=\s*(?:"([^"]*)"|([^,\s]+))/g;
    let match;
    while ((match = pair.exec(headerValue)) !== null) {
        params[match[1]] = match[2] !== undefined ? match[2] : match[3];
    }
    return params;
}

// The gateway's DigestAuthMediator splits this header on ", " and strips exactly one pair of quotes from
// realm/nonce/qop/algorithm — so every value is quoted and the separator is exactly comma+space.
function challenge(res, reason) {
    const nonce = crypto.randomBytes(16).toString('hex');
    res.setHeader('WWW-Authenticate', `Digest realm="${REALM}", qop="auth", nonce="${nonce}", `
        + `opaque="${OPAQUE}", algorithm="MD5"`);
    console.log(`----digest challenge issued: ${reason}`);
    res.status(401).type('text/plain').send(`digest authentication required (${reason})`);
}

const handleDigestRequest = (req, res) => {
    const authHeader = req.header('Authorization') || '';
    if (!authHeader.startsWith('Digest ')) {
        return challenge(res, 'no digest credentials');
    }

    const params = parseDigestParams(authHeader.substring('Digest '.length));
    const password = USERS[params.username];
    if (password === undefined) {
        return challenge(res, `unknown user ${params.username}`);
    }

    // HA2 hashes the digest-uri the CLIENT signed (params.uri), which is what RFC 2617 prescribes; the nonce is
    // likewise taken as presented. Nonce freshness/replay is deliberately not tracked — this stub proves
    // possession of the password, and a stateful nonce store would only add flakiness under parallel lanes.
    const ha1 = md5(`${params.username}:${REALM}:${password}`);
    const ha2 = md5(`${req.method}:${params.uri}`);
    const expected = params.qop
        ? md5(`${ha1}:${params.nonce}:${params.nc}:${params.cnonce}:${params.qop}:${ha2}`)
        : md5(`${ha1}:${params.nonce}:${ha2}`);

    if (expected !== params.response) {
        return challenge(res, `response hash mismatch for ${params.username}`);
    }

    console.log(`----digest authenticated: ${params.username} on ${params.uri}`);
    res.status(200).json({
        authenticatedUser: params.username,
        scheme: 'Digest',
        realm: REALM,
        digestUri: params.uri
    });
};

module.exports = { handleDigestRequest };
