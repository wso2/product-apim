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

// SMTP capture sink — the replacement for the legacy NotificationTestCase's in-JVM GreenMail server.
//
// WHY IT LIVES IN THIS IMAGE AND NOT IN THE TEST JVM: APIM's new-API-version notifier sends the mail from INSIDE
// the APIM container through the carbon `email` output-event adapter, so the SMTP server has to be reachable on
// the shared docker network (nodebackend:3025). For the same reason the ASSERTIONS cannot live here — they run in
// the test JVM, so captured messages are read back over the HOST-published HTTP port this app already owns (3005).
// Piggy-backing on duplicate-header-backend means only an SMTP listener is added: no new HTTP port, no
// Dockerfile/ecosystem/NodeAppServer churn.
//
// 3025 is deliberately NOT in NodeAppServer#exposedPorts: container-to-container traffic works over the shared
// network without publishing (mcp-server on 3020 is the precedent), and every published port also joins the
// startup liveness set, so listing one buys nothing here and risks stalling every block's boot.
//
// MAILBOXES ARE KEYED BY ENVELOPE RECIPIENT, lower-cased. Tests run in parallel against this one shared backend,
// so each scenario mints its own unique recipient address (utils/Names.unique) — a global inbox would let two
// concurrent scenarios read each other's mail, and a unique address also makes a stale message impossible by
// construction (nothing can have been delivered to an address that did not exist before the scenario).
//
// WRITES CREATE, READS DO NOT. Delivery creates the mailbox on first touch; the introspection GET 404s an address
// it has never seen. That is the websub-receiver's hard-won rule: a read that allocates makes a typo'd address
// answer 200 with count 0, so a wrong-address assertion fails as "0 != 1" instead of "no such mailbox" — or worse,
// passes vacuously against a mailbox that never existed.
//
// RETENTION IS BOUNDED. This container lives for the whole suite run, so bodies are truncated and each mailbox
// keeps only the most recent MAX_RETAINED messages. `count` is the exact number retained for that address.
const { SMTPServer } = require('smtp-server');
const { simpleParser } = require('mailparser');

const MAX_RETAINED = 50;
const MAX_BODY_CHARS = 8192;

/** lower-cased recipient address -> { messages: [...] } */
const mailboxes = new Map();

function normalize(address) {
    return String(address || '').trim().toLowerCase();
}

function truncate(value) {
    const text = typeof value === 'string' ? value : '';
    return text.length > MAX_BODY_CHARS ? text.substring(0, MAX_BODY_CHARS) : text;
}

/** Delivery path only: creates on first touch (see the header note). */
function mailboxFor(address) {
    const key = normalize(address);
    if (!mailboxes.has(key)) {
        mailboxes.set(key, { messages: [] });
    }
    return mailboxes.get(key);
}

function record(session, parsed) {
    const recipients = (session.envelope.rcptTo || []).map((rcpt) => rcpt.address);
    const message = {
        // Envelope (MAIL FROM / RCPT TO) and headers are reported separately: the notifier addresses several
        // subscribers in ONE message, so the To header can list addresses this mailbox is only one of.
        envelopeFrom: session.envelope.mailFrom ? session.envelope.mailFrom.address : '',
        recipients: recipients,
        from: parsed.from ? parsed.from.text : '',
        to: parsed.to ? parsed.to.text : '',
        subject: parsed.subject || '',
        contentType: parsed.headers.get('content-type')
            ? String(parsed.headers.get('content-type').value || '')
            : '',
        text: truncate(parsed.text),
        html: truncate(typeof parsed.html === 'string' ? parsed.html : ''),
        receivedAt: new Date().toISOString()
    };
    recipients.forEach((recipient) => {
        const mailbox = mailboxFor(recipient);
        mailbox.messages.push(message);
        while (mailbox.messages.length > MAX_RETAINED) {
            mailbox.messages.shift();
        }
    });
    console.log(`mail-sink: captured "${message.subject}" for [${recipients.join(', ')}]`);
}

/** Starts the SMTP listener. Plain (no TLS, no mandatory auth) — it is a capture sink, never a real relay. */
function startSmtpSink(port) {
    const server = new SMTPServer({
        // A test sink terminates plaintext only. STARTTLS is hidden rather than merely unused so a client
        // configured with starttls.enable=true fails loudly instead of negotiating a certificate that does not
        // exist. AUTH is accepted-and-ignored so a mis-set enable_authentication does not present as "mail was
        // never sent" (the hardest failure of this arc to diagnose).
        hideSTARTTLS: true,
        authOptional: true,
        onAuth(auth, session, callback) {
            callback(null, { user: auth.username });
        },
        onData(stream, session, callback) {
            simpleParser(stream)
                .then((parsed) => {
                    record(session, parsed);
                    callback();
                })
                .catch((error) => {
                    console.error('mail-sink: failed to parse message', error);
                    callback(error);
                });
        }
    });
    server.on('error', (error) => console.error('mail-sink: SMTP server error', error));
    server.listen(port, '0.0.0.0', () => {
        console.log(`Mail sink SMTP listener running at nodebackend:${port}`);
    });
    return server;
}

/** GET /mail/inbox?recipient=<address> — 404 for an address nothing was ever delivered to. */
function handleGetInbox(req, res) {
    const recipient = normalize(req.query.recipient);
    if (!recipient) {
        return res.status(400).json({ error: 'query parameter "recipient" is required' });
    }
    if (!mailboxes.has(recipient)) {
        return res.status(404).json({ error: `no mailbox for ${recipient}` });
    }
    const messages = mailboxes.get(recipient).messages;
    return res.status(200).json({ recipient: recipient, count: messages.length, messages: messages });
}

/**
 * DELETE /mail/inbox?recipient=<address> — empties the mailbox, keeping it registered so a later read still
 * distinguishes "cleared" (200, count 0) from "never existed" (404). Not needed by a scenario that mints a unique
 * recipient (nothing can be inherited), but it is what makes the sink reusable by one that reuses an address.
 */
function handleClearInbox(req, res) {
    const recipient = normalize(req.query.recipient);
    if (!recipient) {
        return res.status(400).json({ error: 'query parameter "recipient" is required' });
    }
    if (!mailboxes.has(recipient)) {
        return res.status(404).json({ error: `no mailbox for ${recipient}` });
    }
    mailboxes.get(recipient).messages = [];
    return res.status(200).json({ recipient: recipient, count: 0, messages: [] });
}

module.exports = { startSmtpSink, handleGetInbox, handleClearInbox };
