# WSO2 API Manager v3 Design and Architecture

API Manager consists of 5 main runtimes

1. API Core
1. API Gateway
1. Key Manager
1. Traffic Manager
1. Analytics

## API Core

API Core consists of several microservices. The following diagram depicts the aggregation of the subcomponents of the API Manager Core.

<img src="https://github.com/lakwarus/product-apim/raw/master/docs/design/images/API-Core.png" width="600">

- The top layer or the web interface of the component is where the users interact with the API Management solution and consume the core API Management features shifted with the product. This layer comprises of a set of single page web applications developed on top of the React UI Framework. The web interface consists of the following web applications:
  - API Publisher - Provides an end-user, collaborative web interface for API providers to compose, publish and manage APIs and share documentation.
  - API Store - Provides an end-user, collaborative web interface for consumers to self-register, discover API functionality, subscribe to APIs, evaluate them and interact with API publishers.
  - API Admin Portal - Provides an end-user, collaborative web interface for API Manager administrator users to manage the deployment, users, configurations, view statistics, etc.

- The web applications consume the RESTful API layer underneath, which is written using swagger 2.0.0. The REST API layer provides a distinct set of REST APIs for the API Publisher, API Store and Admin portal.  
- The business logic is implemented in the API Manager implementation layer. All the API Management related backend service implementations reside in this layer and they are exposed as microservices.  
- Inbuilt lightweight Message Broker is provided reliable messaging platform from API Core to other API Management components. It's allowed to scale individual components independently.
- The bottom most layer is the persistence layer where the API management related data is persisted. The data layer includes the API Manager database, which is dedicated to persisting API related data, and the Stat database, which is dedicated to persisting API analytics data.


## API Geteway

APIM v3 API Gateway is written by using Ballerina. Ballerina is a general purpose, concurrent and strongly typed programming language with both textual and graphical syntaxes, optimized for integration.

<img src="https://github.com/lakwarus/product-apim/raw/master/docs/design/images/API-GW.png" width="600">

## Key Manager

The Key Manager component handles security and key related operations like user authentications, API key generation and key validations. Key management operations are based on OAuth 2.0.0 protocol specifications.

## Traffic Manager

The Traffic Manager helps users to regulate API traffic, make APIs and applications available to consumers at different service levels, and secure APIs against security attacks. The Traffic Manager features a dynamic throttling engine to process throttling policies in real-time, including rate limiting of API requests.

## Analytics

Additionally, monitoring and analytics are provided by the analytics component, WSO2 API Manager Analytics. This component provides a host of statistical graphs, an alerting mechanism on pre-determined events.
 
