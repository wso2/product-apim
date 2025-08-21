module.exports = {
  apps: [
    {
      name: "node-coffee-service",
      script: "./node-coffee-service/server.js",
      cwd: "./",
      env: {
        PORT: 3000
      }
    },
    {
      name: "node-customer-service",
      script: "./node-customer-service/server.js",
      cwd: "./",
      env: {
        PORT: 3001
      }
    },
    {
      name: "am-auditApi-sample",
      script: "./am-auditApi-sample/server.js",
      cwd: "./",
      env: {
        PORT: 3002
      }
    },
    {
      name: "am-graphQL-sample",
      script: "./am-graphQL-sample/server.js",
      cwd: "./",
      env: {
        PORT: 3003
      }
    },
    {
      name: "BPMNProcessServerApp-1.0.0",
      script: "./BPMNProcessServerApp-1.0.0/server.js",
      cwd: "./",
      env: {
        PORT: 3004
      }
    },
    {
      name: "duplicate-header-backend",
      script: "./duplicate-header-backend/server.js",
      cwd: "./",
      env: {
        PORT: 3005
      }
    },
    {
      name: "etcdmock",
      script: "./etcdmock/server.js",
      cwd: "./",
      env: {
        PORT: 3006
      }
    },
    {
      name: "jaxrs_basic",
      script: "./jaxrs_basic/server.js",
      cwd: "./",
      env: {
        PORT: 3007
      }
    },
    {
      name: "name-check1",
      script: "./name-check1/server.js",
      cwd: "./",
      env: {
        PORT: 3008
      }
    },
    {
      name: "name-check1_SB",
      script: "./name-check1_SB/server.js",
      cwd: "./",
      env: {
        PORT: 3009
      }
    },
    {
      name: "name-check2",
      script: "./name-check2/server.js",
      cwd: "./",
      env: {
        PORT: 3010
      }
    },
    {
      name: "name-check2_SB",
      script: "./name-check2_SB/server.js",
      cwd: "./",
      env: {
        PORT: 3011
      }
    },
    {
      name: "name-check3",
      script: "./name-check3/server.js",
      cwd: "./",
      env: {
        PORT: 3012
      }
    },
    {
      name: "name-check3_SB",
      script: "./name-check3_SB/server.js",
      cwd: "./",
      env: {
        PORT: 3013
      }
    },
    {
      name: "name-checkOne",
      script: "./name-checkOne/server.js",
      cwd: "./",
      env: {
        PORT: 3014
      }
    },
    {
      name: "name-checkTwo",
      script: "./name-checkTwo/server.js",
      cwd: "./",
      env: {
        PORT: 3015
      }
    },
   {
     name: "name-checkThree",
     script: "./name-checkThree/server.js",
     cwd: "./",
     env: {
       PORT: 3016
     }
   },
   {
    name: "wildcard",
    script: "./wildcard/server.js",
    cwd: "./",
    env: {
      PORT: 3017
    }
  },
  {
    name: "node-people-service",
    script: "./node-people-service/server.js",
        cwd: "./",
        env: {
          PORT: 3018
    }
  }
  ]
};
