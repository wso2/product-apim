

# KeyManagerInfoDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **String** |  |  [optional]
**name** | **String** |  | 
**type** | **String** |  | 
**displayName** | **String** | display name of Keymanager  |  [optional]
**description** | **String** |  |  [optional]
**enabled** | **Boolean** |  |  [optional]
**availableGrantTypes** | **List&lt;String&gt;** |  |  [optional]
**tokenEndpoint** | **String** |  |  [optional]
**revokeEndpoint** | **String** |  |  [optional]
**userInfoEndpoint** | **String** |  |  [optional]
**enableTokenGeneration** | **Boolean** |  |  [optional]
**enableTokenEncryption** | **Boolean** |  |  [optional]
**enableTokenHashing** | **Boolean** |  |  [optional]
**enableOAuthAppCreation** | **Boolean** |  |  [optional]
**enableMapOAuthConsumerApps** | **Boolean** |  |  [optional]
**applicationConfiguration** | [**List&lt;KeyManagerApplicationConfigurationDTO&gt;**](KeyManagerApplicationConfigurationDTO.md) |  |  [optional]
**alias** | **String** | The alias of Identity Provider. If the tokenType is EXCHANGED, the alias value should be inclusive in the audience values of the JWT token  |  [optional]
**additionalProperties** | [**Object**](.md) |  |  [optional]



