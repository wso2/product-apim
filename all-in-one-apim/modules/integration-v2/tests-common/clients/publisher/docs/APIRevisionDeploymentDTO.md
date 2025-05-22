

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



## Enum: StatusEnum

Name | Value
---- | -----
CREATED | &quot;CREATED&quot;
APPROVED | &quot;APPROVED&quot;
REJECTED | &quot;REJECTED&quot;



