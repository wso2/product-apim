# WSO2 API Manager v3 Design and Architecture

API Manager consists of 5 main runtimes

1. APIM Core
1. API Gateway
1. Key Manager
1. Traffic Manager
1. Analytics

## APIM Core

API Core consists of several microservices. The following diagram depicts the aggregation of the subcomponents of the API Manager Core.

<img src="https://github.com/lakwarus/product-apim/raw/master/docs/design/images/API-Core.png" width="600">

- The top layer or the web interface of the component is where the users interact with the API Management solution and consume the core API Management features shifted with the product. This layer comprises of a set of single page web applications developed on top of the React UI Framework. The web interface consists of the following web applications:
  - API Publisher - Provides an end-user, collaborative web interface for API providers to compose, publish and manage APIs and share documentation.
  - API Store - Provides an end-user, collaborative web interface for consumers to self-register, discover API functionality, subscribe to APIs, evaluate them and interact with API publishers.
  - API Admin Portal - Provides an end-user, collaborative web interface for API Manager administrator users to manage the deployment, users, configurations, view statistics, etc.

- The web applications consume the RESTful API layer underneath, which is written using swagger 2.0.0. The REST API layer provides a distinct set of REST APIs for the API Publisher, API Store and Admin portal.  
- The business logic is implemented in the API Manager implementation layer. All the API Management related backend service implementations reside in this layer and they are exposed as microservices.  
- The bottom most layer is the persistence layer where the API management related data is persisted. The data layer includes the API Manager database, which is dedicated to persisting API related data, and the Stat database, which is dedicated to persisting API analytics data.


## API Geteway




 
