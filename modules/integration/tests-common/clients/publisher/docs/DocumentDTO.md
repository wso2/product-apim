
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
**fileName** | **String** |  |  [optional]
**inlineContent** | **String** |  |  [optional]
**otherTypeName** | **String** |  |  [optional]
**visibility** | [**VisibilityEnum**](#VisibilityEnum) |  | 
**createdTime** | **String** |  |  [optional]
**createdBy** | **String** |  |  [optional]
**lastUpdatedTime** | **String** |  |  [optional]
**lastUpdatedBy** | **String** |  |  [optional]


<a name="TypeEnum"></a>
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


<a name="SourceTypeEnum"></a>
## Enum: SourceTypeEnum
Name | Value
---- | -----
INLINE | &quot;INLINE&quot;
MARKDOWN | &quot;MARKDOWN&quot;
URL | &quot;URL&quot;
FILE | &quot;FILE&quot;


<a name="VisibilityEnum"></a>
## Enum: VisibilityEnum
Name | Value
---- | -----
OWNER_ONLY | &quot;OWNER_ONLY&quot;
PRIVATE | &quot;PRIVATE&quot;
API_LEVEL | &quot;API_LEVEL&quot;



