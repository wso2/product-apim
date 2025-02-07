

# DocumentDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**documentId** | **String** |  |  [optional] [readonly]
**name** | **String** |  | 
**type** | [**TypeEnum**](#TypeEnum) |  | 
**summary** | **String** |  |  [optional]
**sourceType** | [**SourceTypeEnum**](#SourceTypeEnum) |  | 
**sourceUrl** | **String** |  |  [optional] [readonly]
**fileName** | **String** |  |  [optional] [readonly]
**inlineContent** | **String** |  |  [optional]
**otherTypeName** | **String** |  |  [optional] [readonly]
**visibility** | [**VisibilityEnum**](#VisibilityEnum) |  | 
**createdTime** | **String** |  |  [optional] [readonly]
**createdBy** | **String** |  |  [optional]
**lastUpdatedTime** | **String** |  |  [optional] [readonly]
**lastUpdatedBy** | **String** |  |  [optional] [readonly]



## Enum: TypeEnum

Name | Value
---- | -----
HOWTO | &quot;HOWTO&quot;
SAMPLES | &quot;SAMPLES&quot;
PUBLIC_FORUM | &quot;PUBLIC_FORUM&quot;
SUPPORT_FORUM | &quot;SUPPORT_FORUM&quot;
API_MESSAGE_FORMAT | &quot;API_MESSAGE_FORMAT&quot;
SWAGGER_DOC | &quot;SWAGGER_DOC&quot;
OTHER | &quot;OTHER&quot;



## Enum: SourceTypeEnum

Name | Value
---- | -----
INLINE | &quot;INLINE&quot;
MARKDOWN | &quot;MARKDOWN&quot;
URL | &quot;URL&quot;
FILE | &quot;FILE&quot;



## Enum: VisibilityEnum

Name | Value
---- | -----
OWNER_ONLY | &quot;OWNER_ONLY&quot;
PRIVATE | &quot;PRIVATE&quot;
API_LEVEL | &quot;API_LEVEL&quot;



