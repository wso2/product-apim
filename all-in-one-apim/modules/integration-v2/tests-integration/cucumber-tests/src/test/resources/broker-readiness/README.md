# Live broker readiness

`KeyManagerReadiness` waits for an active `keyManager` AMQP topic subscription belonging to the current Gateway.
It queries `AndesAdminService` on the runtime's control-plane servlet endpoint. This serves CP's broker in the
distributed topology and the embedded broker in all-in-one. It uses the block's supertenant management identity,
independently of the scenario's tenant or acting user.

The shared `BrokerSubscriptionReadiness` helper is also used for distributed `throttleData` subscriptions on CP
and TM. It checks the returned `active` flag explicitly because the bundled broker can return inactive entries
when asked for active subscriptions. It also requires the requested topic, AMQP protocol, TOPIC destination,
and a matching source address. A CP subscriber cannot satisfy distributed Gateway readiness. Failed HTTP,
SOAP faults, malformed XML, missing subscriptions, and inactive subscriptions remain unready.

`ApimRuntime.getGatewayBrokerClientAddresses()` provides the topology facts. Distributed uses the Gateway's
address on its block network. All-in-one uses its own container interfaces and loopback because the broker and
Gateway share a container. Host test-client/bridge addresses are not used as Gateway identities.

This check does not depend on startup log markers. `JMSListener.start()` checks for its consumers for only three
seconds; after a slow-start warning it does not emit the success marker when consumers start later. Logs cannot
serve as current subscription state. The live check does not create subscribers, restart components, change
policies, or change any product assertion.

## Regression verification

After building the reactor dependencies, run from `cucumber-tests`:

```bash
mvn test -Dsurefire.suite.xml=testng-fv-broker-readiness.xml
```

The suite checks subscription matching, rejection of inactive/wrong-origin/wrong-topic subscriptions, HTTP/SOAP
failures, and secure XML parsing with the integration classpath.

## Runtime verification

Use focused suites copied from each topology's existing eventing block, with the original `KeyManagerApiKeyRunner`
and the relevant scenario-name filter. `BrokerSubscriptionRuntimeVerificationTest` can be selected as the **last**
class in a dedicated verification block. It proves the live check works without reading startup logs and rejects
an unrelated source address/topic. Its final method intentionally stops only that block's Gateway and verifies
readiness becomes false. Do not put this verification class in a product suite: it must run after all product
scenarios have completed. Its artifacts are written to `target/logs/broker-readiness/`.

Keep the existing lifecycle, block-scope and clock listeners in the focused suite. Ubuntu clock preparation is
documented in `../host-clock/README.md`. Restore persistent development hosts after verification.
