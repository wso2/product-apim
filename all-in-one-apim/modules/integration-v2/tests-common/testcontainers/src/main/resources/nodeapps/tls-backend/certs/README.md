# tls-backend TLS fixture

`server.key` / `server.crt` are the self-signed key pair `tls-backend/server.js` presents on port 3023.

They are **committed fixtures on purpose.** The endpoint-certificate invocation arc uploads this exact
certificate through the publisher `/endpoint-certificates` API and then expects the gateway to trust the
backend. If the pair were generated at image-build or container-start time it would drift from the uploaded
`.cer`, and the resulting handshake failure (HTTP 500 at the gateway) is indistinguishable from the
untrusted-certificate behaviour the test is asserting — a silent, very confusing false failure.

The same certificate is committed for upload at
`tests-integration/cucumber-tests/src/test/resources/artifacts/certs/endpoint/nodebackend.cer`
(identical PEM bytes; that is the encoding the existing `endpoint.cer` / `endpoint2.cer` fixtures use and the
form the upload API parses). **If you regenerate the pair, re-copy `server.crt` over that `.cer` too.**

Regenerate with:

```bash
openssl req -x509 -newkey rsa:2048 -sha256 -days 7300 -nodes \
  -keyout server.key -out server.crt \
  -subj "/C=LK/ST=Colombo/O=WSO2/OU=integration-v2/CN=nodebackend" \
  -addext "subjectAltName=DNS:nodebackend"
```

Two properties are load-bearing:

- **`CN` and the `subjectAltName` DNS entry must be `nodebackend`.** The gateway reaches the backend by its
  Docker network alias (`https://nodebackend:3023`) and its HTTP sender verifies the hostname against the
  presented certificate. Any other name and the post-upload 200 leg can never pass.
- **Long validity (7300 days).** A short-lived fixture would start failing the suite on an unrelated day.
