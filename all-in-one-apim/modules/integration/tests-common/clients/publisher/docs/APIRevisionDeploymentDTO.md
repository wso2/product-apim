

# APIRevisionDeploymentDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**revisionUuid** | **String** |  |  [optional]
**name** | **String** |  |  [optional]
**status** | [**StatusEnum**](#StatusEnum) |  |  [optional]
**vhost** | **String** |  |  [optional]
**displayOnDevportal** | **Boolean** |  |  [optional]
**deployedTime** | **String** |  |  [optional] [readonly]
**successDeployedTime** | **String** |  |  [optional] [readonly]
**liveGatewayCount** | **Integer** | The number of gateways that are currently live in the gateway environment  |  [optional] [readonly]
**deployedGatewayCount** | **Integer** | The number of gateways in which the API revision is deployed successfully  |  [optional] [readonly]
**failedGatewayCount** | **Integer** | The number of gateways where the API revision deployment has failed  |  [optional] [readonly]



## Enum: StatusEnum

Name | Value
---- | -----
CREATED | &quot;CREATED&quot;
APPROVED | &quot;APPROVED&quot;
REJECTED | &quot;REJECTED&quot;



