# 1. Use official Node.js runtime as a parent image
FROM node:18

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

# 4. Install dependencies
RUN cd node-coffee-service && npm install
RUN cd node-customer-service && npm install
RUN cd am-auditApi-sample && npm install
RUN cd am-graphQL-sample && npm install
RUN cd BPMNProcessServerApp-1.0.0 && npm install
RUN cd duplicate-header-backend && npm install
RUN cd etcdmock && npm install
RUN cd jaxrs_basic && npm install
RUN cd name-check1 && npm install
RUN cd name-check1_SB && npm install
RUN cd name-check2 && npm install
RUN cd name-check2_SB && npm install
RUN cd name-check3 && npm install
RUN cd name-check3_SB && npm install
RUN cd name-checkOne && npm install
RUN cd name-checkTwo && npm install
RUN cd name-checkThree && npm install
RUN cd wildcard && npm install
RUN cd node-people-service && npm install

# 5. Install pm2 globally
RUN npm install -g pm2

# 6. Copy app source code folders
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

# 7. Copy PM2 ecosystem config file
COPY ecosystem.config.js .

# 8. Expose ports used by the apps
EXPOSE 3000 3001 3002 3003 3004 3005 3006 3007 3008 3009 3010 3011 3012 3013 3014 3015 3016 3017 3018

# 9. Start  apps using pm2-runtime
CMD ["pm2-runtime", "ecosystem.config.js"]
