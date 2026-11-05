# Zero-dependency mock of the Solace Event Portal APIM/DevPortal v2 API used by APIM's notifiers.
# The required Node 20 test stage checks ID handling, serialized ACL changes and failure behavior before
# runtime files can be copied into the final image. No npm packages or test files are needed at runtime.
FROM node:20-alpine AS tested
WORKDIR /app
COPY server.js acl-reconciliation.js jwks.js ./
COPY test ./test
RUN node --test test/*.test.js

FROM node:20-alpine
WORKDIR /app
COPY --from=tested /app/server.js /app/acl-reconciliation.js /app/jwks.js ./
# server.js defaults to 9081 so it can be run standalone on a host; inside the container it must serve
# 8081 to match DynamicSolaceBroker.SHIM_PORT and the wait-for-listening-port readiness probe.
ENV PORT=8081
EXPOSE 8081
CMD ["node", "server.js"]
