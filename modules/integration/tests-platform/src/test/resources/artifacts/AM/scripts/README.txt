The script will execute the following tests

1. Creation of tenants
2. Creation of User roles
4. Creation of users for said tenant
5. Creation of APIs
6. Publishing APIs
7. Get default app ID
8. Subscribing users to APIs with default app
9. Get user toke for API
10. Invoke API

Load test features
1. Get All APIs
2. Get All Recently added APIs
3. Get All Tags


Please note that in in the script attached here, both the functionality flow and the load test flow will be executed (in a single thread) since the objective of running the script at build time is to check all the functionality of the pack at hand.

However, you may increase the thread count and run for load test purposes.

**** by default the script will create 1 tenant, for that tenant 1 user and for that user 1 api. these parameters can be changed via the user defined variable section. the variable names are 
	numberOfTenants - Number of tenants to be created
	numberOfUsers - Number of users to be created for each tenant
	numberOfAPIs - Number of APIs to be added

Please edit the same and add parameters of your preference.
	

