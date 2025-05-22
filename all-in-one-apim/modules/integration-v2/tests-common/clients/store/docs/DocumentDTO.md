

# DocumentDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**documentId** | **String** |  |  [optional]
**name** | **String** |  | 
**type** | [**TypeEnum**](#TypeEnum) |  | 
**summary** | **String** |  |  [optional]
**sourceType** | [**SourceTypeEnum**](#SourceTypeEnum) |  | 
**sourceUrl** | **String** |  |  [optional]
**otherTypeName** | **String** |  |  [optional]



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



