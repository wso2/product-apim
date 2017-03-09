#WSO2 API Management Server



Running Samples
---------------

Sample Integration Flow configurations are available at samples/SequenceDiagramDSLSamples directory.

Configuration can be deployed to server by dropping the file to <CARBON_HOME>/deployment/integration-flows/ directory.


####Sample Configuration

```sh
@Path ("/stock")
@Source (protocol="http", host="localhost", port=8080)
@Api (tags = {"stock_info","stock_update"}, description = "Rest api for do operations on admin", produces = MediaType.APPLICATION_JSON)
package com.sample;

constant endpoint stockEP = new HTTPEndPoint("http://localhost:8081/stockquote/WSO2");

@GET
@PUT
@POST
@Path ("/passthrough")
resource passthrough (message m) {
   reply invoke(stockEP, m);
}
```
