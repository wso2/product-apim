# 1. API Development

1.1 Managing Public, Partner vs Private APIs
- Enable the internal use of APIs
- Help pick and choose subset of those to be used with partners
- Enabling building API ecosystems with partners that can unlock partnerships that cross industries.
  - Connect with APIs from partners
  - Enable partners to connect with ours
- A further subset to be exposed as public APIs. Many of the same APIs used internally and with partners can be used as public APIs to drive additional business and help obtain new customers.

1.2 Ownership, permission and collaborative API development
- Each business units are responsible for their APIs and data. Some of them having sensitive data. They need to consider security and controllability while providing value to the other business units.
- Able to restrict API (and its development) by business units
- Eg. APIs developed by financial business unit should be able to restrict (view/edit) to other business units
- Selected APIs should be able to share with different business units (edit and view)

1.3 Developer Optimized APIs Development
- Business APIs can be access different parties via different devices. Providing optimized and personalized experience is the key success of digital transformation.
- Same API can be access by different clients.
- Eg, mobile device, PC, TV etc.
- API developer should be able to optimize API output by identifying its aclient
- Optimization can be a composition of the multiple backend or stripdown.
- Client based prioritization of the APIs

# 2.0 API security
- Prevent misuse or abuse of information or of any application resources exposed by an API
- Have the ability to distinguish between internal, partner and public use of APIs via security controls and audits
- Ability to trace back which apps are using what APIs (hence data or resources) with which user credentials, permissions and roles
- Ability to support multiple security standards and protocols
  - Embed custom security algorithms globally or for selected services
- Ability to enforce both authentication (are you a valid user) and authorization (are you permitted to perform this action) on APIs

2.1 Protecting Businesses by Anomaly Detection

- Identifying anomalies, abnormalities and frauds are key responsibilities of a business organisation. Not knowing them leads to business complications.
- Should be able to add different policies to identify anomalies
- Eg. Hijacking access tokens, application malfunction etc
- System should be able to detect and able to act on it

# 3. App Development with APIs
- Use cases for APIs used in Mobile App development, Web App development and other forms of App development
- Ability to facilitate different app channels and be able to track and monitor them
- Support for custom APIs that deal with specific apps that require additional security
- Location, territory or locale based support for APIs for specific Apps
- Traffic shaping for different tiers of Apps calling APIs
- Assist App developers via API Documentation and Try It options
- Fast track App development via API SDKs
- Support users to consume services, APIs using different platforms (i.e - ability to be consumed by various clients)

# 4. API Lifecycle Management
- Ability to run full lifecycle API Management from the inception stage of an API until retirement
- Notification mechanisms for informing developers on API changes
- App lifecycle management mechanisms in sync with API lifecycle management
- Introduce and execute organization specific lifecycle states

# 5. [API Versioning](https://github.com/sanjeewa-malalgoda/product-scenarios/tree/master/api-versioning)
- Ability to retire old APIs and introduce new versions of APIs to enhance its functionality
- Ensure that API updates don&#39;t break when upgraded/versioned or moved between environments, geographies, data centers and the cloud
- A/B testing with old vs new APIs
- Ability to notify consumers of the old version about the availability of the new API version
- Enforcing a grace period to upgrade to the new version of the API

Transferring contracts with app developers to newer versions.

# 6. API Governance
- Control and track the broader operational character of how APIs get exposed
- Manage and maintain policy characteristics such as metering, SLAs, availability and performance
- Different partners and developers specific policy management
- People &amp; persona driven governance models (who can do what when?)
- Dependency analysis. Track which services fuel which APIs and which APIs fuel which Apps.

# 7. API Rate Limiting
- For monetization purposes, to enforce limits on an Application based on its subscriptions
- Enforce fair usage policy among an application&#39;s users
- Allow privileged rate limits based on location, device type, user credentials, etc.
- Enforce a peak limit on back-end services to prevent total outages.

# 8. API Monetization

- Defining, enabling and monitoring revenue for APIs
- Defining monetization tiers and throttling traffic based on these tiers
- Support for different API subscription models
  - By call volume
  - By Bandwidth Consumption
  - By time of day usage
- For back-ends that are also monetized, create reports and compare against invoices from back-end vendors.
- Offer free trial version and ask to pay if they wish to continue the service for a long time (something similar to WSO2 API cloud)

# 9. Manage the Value Chain for APIs
- Defining, measuring, monitoring &amp; observing value of your API to the developer who uses it (The value for the app developer)
- Defining, measuring, monitoring &amp; observing the value that developers provide to the end user of the App that they create (Value to the end users of the app that the developer create)
- Value driven API management – maximize value, keep or kill decisions (Maximize the value chain for each participant in the API usage)
- Combining APIs and facilitating composition to value add in novel ways around APIs
- Identifying potential problems in the API, data to help make decisions to scale, prevent downtime, retrospect on failures, etc.

9.1 Business Insights by API analytics
- Business need to understand how they are performing. What are the new business opportunity etc.  API solution should be helped the by giving clear insights.
- System should be able to provide use full analytics of API usage
- These analytics should be help to business personal to analyse current business and able to get an insight of how to improve the business growth
- eg. Which APIs (business units) giving more revenue
- eg.  Which APIs (business units) need to improve or retired
- Eg. which region giving more business, do we need more campaign etc

# 10. Developer Enablement and Community Building
- Bring developers on board
- Manage developers and assist them in making the most of the exposed APIs
- Community building around APIs provided
  - Social and community characteristics
- Forums for collaboration
- Rating, comments and recommendations on APIs for other developers.
- Integrate with social media accounts (login with well known SM sites like FB, G+) / Single sign on with google/facebook/twitter accounts
- API mar

# 11. Support for API Types
- Data APIs – Providing access to data assets
  - Concerns with data security, availability and role based access controls to be addressed
- Resource APIs – Providing access to REST and Micro Services style APIs
- Legacy (SOAP) APIs -  Providing access to traditional APIs in a uniform and secure manner
- Cloud APIs – those that are external to the organization access via Internet
- Streaming APIs and Push Notifications for reducing server load and to minimize client to server connection overhead.

# 12. Geographical Distributed wide API Management
- Multi geographical based business need to maintain quality of services across multi regions.
- Application can be access from multi geographical locations
- Optimizing backend routing based on the geographic
- Global view of the APIs utilization.
- Geographical wide analysis
- Geographical based rate limiting

# 13. API SLA
- Mission critical API providers should be maintained high level of SLAs to their consumers. Need to mitigate risk to maximum level.
- Able to deploy solution in multi datacenter
- Increase the availability of the solution (getting more 9&#39;s)
- Backup and recovery plan
- Monitoring and traffic partitioning

# 14. Micro API Gateway
### 14.1 Deploy API Microgateway and use microgateway toolkit.
### 14.2 Create Microgateway and Deploy - VM mode(Selected API or Labled group of API).
### 14.3 Deploy Microgateway in Docker Envirionment.
### 14.4 Invoke APIs Deployed in API Microgateway with OAuth 2.0 Security.
### 14.5 Invoke APIs Deployed in API Microgateway with JWT Security Token.
### 14.5 API Microgateway Throttling

# 15. Hybrid Cloud Pattern Support
- Protecting business APIs and data will be a key requirement of business organizations. Ability to keep private APIs on promise will be more comfortable to the organizations.
- Should be able to handle API development and management over cloud solution
- Runtime traffic of the APIs should not go outside of the on-promise.
- Able to handle API routing near to its backend to get more security and performance
- Should be able to handle API traffic routing in lock-down environments
