

# SettingsDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**grantTypes** | **List&lt;String&gt;** |  |  [optional]
**scopes** | **List&lt;String&gt;** |  |  [optional]
**applicationSharingEnabled** | **Boolean** |  |  [optional]
**mapExistingAuthApps** | **Boolean** |  |  [optional]
**apiGatewayEndpoint** | **String** |  |  [optional]
**monetizationEnabled** | **Boolean** |  |  [optional]
**recommendationEnabled** | **Boolean** |  |  [optional]
**isUnlimitedTierPaid** | **Boolean** |  |  [optional]
**identityProvider** | [**SettingsIdentityProviderDTO**](SettingsIdentityProviderDTO.md) |  |  [optional]
**isAnonymousModeEnabled** | **Boolean** |  |  [optional]
**isPasswordChangeEnabled** | **Boolean** |  |  [optional]
**userStorePasswordPattern** | **String** | The &#39;PasswordJavaRegEx&#39; cofigured in the UserStoreManager |  [optional]
**passwordPolicyPattern** | **String** | The regex configured in the Password Policy property &#39;passwordPolicy.pattern&#39; |  [optional]
**passwordPolicyMinLength** | **Integer** | If Password Policy Feature is enabled, the property &#39;passwordPolicy.min.length&#39; is returned as the &#39;passwordPolicyMinLength&#39;. If password policy is not enabled, default value -1 will be returned. And it should be noted that the regex pattern(s) returned in &#39;passwordPolicyPattern&#39; and &#39;userStorePasswordPattern&#39; properties too will affect the minimum password length allowed and an intersection of all conditions will be considered finally to validate the password. |  [optional]
**passwordPolicyMaxLength** | **Integer** | If Password Policy Feature is enabled, the property &#39;passwordPolicy.max.length&#39; is returned as the &#39;passwordPolicyMaxLength&#39;. If password policy is not enabled, default value -1 will be returned. And it should be noted that the regex pattern(s) returned in &#39;passwordPolicyPattern&#39; and &#39;userStorePasswordPattern&#39; properties too will affect the maximum password length allowed and an intersection of all conditions will be considered finally to validate the password. |  [optional]



