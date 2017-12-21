# API Rate Monitization

### Sample use case

#### API Monetization

* Defining, enabling and monitoring revenue for APIs 

* Defining monetization tiers and throttling traffic based on these tiers 

* Support for different API subscription models 

    * By call volume 

    * By Bandwidth Consumption

    * By time of day usage 

* For back-ends that are also monetized, create reports and compare against invoices from back-end vendors.

* Offer free trial version and ask to pay if they wish to continue the service for a long time 

### Business Story

* Let ABC organization has an API to expose there mobile phone prices. For the time bean let's assume this organization exposes prices to the sales agents and they have big number of third party sales agents.

* Further they need to gain revenue from third party agents based on the there subscription models. As an example let's use the following subscription models

	They need to charge for the,

* Number of request counts

* Bandwidth consumption

### How this Business Scenario can be achieved Using WSO2 API Manager ?

We have the following subscription tires that have been defined already.

 ![](images/image_0.png)

You can add any custom subscription tires as well.

We can connect to a billing engine as mentioned in the following document[1] and connect the API manager node to the billing engine and charge according to the API usage and bandwidth consumption.

By using connecting the API Manager to our API MAnager analytics server we can identify the API usages along with the billing engine outcomes.

Hence using the above mentioned subscription approach we can have a free subscription tier for free trail version users. Once they reach they can be offered the prices subscriptions which will gain the revenue. 

### References

[1] https://docs.wso2.com/display/AM210/Enabling+Monetization+of+APIs