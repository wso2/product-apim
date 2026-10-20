# Test-only listener certificates

`default-listener.crt` and `default-listener.key` are disposable, self-signed
`localhost` TLS fixtures used only by the integration-v2 TestContainer harness
for the platform gateway listener.

These credentials are not trusted production material. Do not reuse, distribute,
or deploy them outside the integration test harness. Generate a new certificate
and private key for any other environment.
