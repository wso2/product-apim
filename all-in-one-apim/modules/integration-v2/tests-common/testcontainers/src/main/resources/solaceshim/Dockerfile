# Mock of the Solace Cloud / Event-Portal "APIM DevPortal" API (real base path /api/v2/apim) -- the
# surface APIM's v2 Solace client actually calls. Contract derived from the Feign interface
# SolaceV2ApimApisClient (org.wso2.carbon.apimgt.solace) and cross-checked against Solace's published
# APIM/DevPortal API walkthrough.
#
# An earlier revision of this image served the v1 CONNECTOR contract (/{org}/apis, /{org}/apiProducts,
# /{org}/developers/{dev}/apps) recovered from legacy SolaceTestCase's WireMock stubs. That surface is
# not reachable on this product: the v1 path needs a provider="solace" gateway environment, whose
# deployer (ExternalGatewayDeployer/SolaceBrokerDeployer) is bound by NOTHING in the distribution --
# verified across all 772 plugin jars. Solace now integrates through notifiers on the v2 API instead.
#
# NO npm install: server.js is zero-dependency (pure node:http). The previous revision needed express;
# dropping it removes the install layer, so this image builds offline in about a second.
FROM node:20-alpine
WORKDIR /app
COPY server.js ./
# server.js defaults to 9081 so it can be run standalone on a host; inside the container it must serve
# 8081 to match DynamicSolaceBroker.SHIM_PORT and the wait-for-listening-port readiness probe.
ENV PORT=8081
EXPOSE 8081
CMD ["node", "server.js"]
