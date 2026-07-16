# 1. Use official Node.js runtime as a parent image
# alpine variant: smallest base (~50 MB). Uses musl libc, which is fine here because every
# service depends only on pure-JS packages (express/body-parser/uuid) — no native modules to compile.
FROM node:18-alpine

# 2. Set working directory
WORKDIR /usr/src/app

# 3. Copy package.json and package-lock.json for caching
COPY node-coffee-service/package*.json ./node-coffee-service/
COPY node-customer-service/package*.json ./node-customer-service/
COPY am-auditApi-sample/package*.json  ./am-auditApi-sample/
COPY am-graphQL-sample/package*.json ./am-graphQL-sample/
COPY BPMNProcessServerApp-1.0.0/package*.json ./BPMNProcessServerApp-1.0.0/
COPY duplicate-header-backend/package*.json ./duplicate-header-backend/
COPY etcdmock/package*.json ./etcdmock/
COPY jaxrs_basic/package*.json ./jaxrs_basic/
COPY name-check1/package*.json ./name-check1/
COPY name-check1_SB/package.*json ./name-check1_SB/
COPY name-check2/package*.json ./name-check2/
COPY name-check2_SB/package*.json ./name-check2_SB/
COPY name-check3/package*.json ./name-check3/
COPY name-check3_SB/package*.json ./name-check3_SB/
COPY name-checkOne/package*.json ./name-checkOne/
COPY name-checkTwo/package*.json ./name-checkTwo/
COPY name-checkThree/package*.json ./name-checkThree/
COPY wildcard/package*.json ./wildcard/
COPY node-people-service/package*.json ./node-people-service/
COPY soap-stub/package*.json ./soap-stub/
COPY mcp-server/package*.json ./mcp-server/

# 4. Install dependencies + pm2 in a single layer, then drop the npm cache so it
#    is not baked into the image.
RUN for d in node-coffee-service node-customer-service am-auditApi-sample \
        am-graphQL-sample BPMNProcessServerApp-1.0.0 duplicate-header-backend \
        etcdmock jaxrs_basic name-check1 name-check1_SB name-check2 name-check2_SB \
        name-check3 name-check3_SB name-checkOne name-checkTwo name-checkThree \
        wildcard node-people-service soap-stub mcp-server; do \
        (cd "$d" && npm install) || exit 1; \
    done \
    && npm install -g pm2 \
    && npm cache clean --force

# 5. Copy app source code folders
COPY node-coffee-service ./node-coffee-service
COPY node-customer-service ./node-customer-service
COPY am-auditApi-sample ./am-auditApi-sample
COPY am-graphQL-sample ./am-graphQL-sample
COPY BPMNProcessServerApp-1.0.0 ./BPMNProcessServerApp-1.0.0
COPY duplicate-header-backend ./duplicate-header-backend
COPY etcdmock ./etcdmock
COPY jaxrs_basic ./jaxrs_basic
COPY name-check1 ./name-check1
COPY name-check1_SB ./name-check1_SB
COPY name-check2 ./name-check2
COPY name-check2_SB ./name-check2_SB
COPY name-check3 ./name-check3
COPY name-check3_SB ./name-check3_SB
COPY name-checkOne ./name-checkOne
COPY name-checkTwo ./name-checkTwo
COPY name-checkThree ./name-checkThree
COPY wildcard ./wildcard
COPY node-people-service ./node-people-service
COPY soap-stub ./soap-stub
COPY mcp-server ./mcp-server
# network-access-control-fixtures is dependency-free (pure Node http), so it needs no package.json COPY / npm install above.
COPY network-access-control-fixtures ./network-access-control-fixtures

# 6. Copy PM2 ecosystem config file
COPY ecosystem.config.js .

# 7. Expose ports used by the apps
EXPOSE 3000 3001 3002 3003 3004 3005 3006 3007 3008 3009 3010 3011 3012 3013 3014 3015 3016 3017 3018 3019

# 8. Start  apps using pm2-runtime
CMD ["pm2-runtime", "ecosystem.config.js"]
