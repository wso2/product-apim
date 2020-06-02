
# DocumentSearchResultDTO

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**docType** | [**DocTypeEnum**](#DocTypeEnum) |  |  [optional]
**summary** | **String** |  |  [optional]
**sourceType** | [**SourceTypeEnum**](#SourceTypeEnum) |  |  [optional]
**sourceUrl** | **String** |  |  [optional]
**otherTypeName** | **String** |  |  [optional]
**visibility** | [**VisibilityEnum**](#VisibilityEnum) |  |  [optional]
**apiName** | **String** | The name of the associated API |  [optional]
**apiVersion** | **String** | The version of the associated API |  [optional]
**apiProvider** | **String** |  |  [optional]
**apiUUID** | **String** |  |  [optional]


<a name="DocTypeEnum"></a>
## Enum: DocTypeEnum
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
URL | &quot;URL&quot;
FILE | &quot;FILE&quot;
MARKDOWN | &quot;MARKDOWN&quot;


<a name="VisibilityEnum"></a>
## Enum: VisibilityEnum
Name | Value
---- | -----
OWNER_ONLY | &quot;OWNER_ONLY&quot;
PRIVATE | &quot;PRIVATE&quot;
API_LEVEL | &quot;API_LEVEL&quot;



