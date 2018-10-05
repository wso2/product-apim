### Instructions to build IS as Key Manager

- Build product-apim (clean repo mode)
`mvn clean install -Dmaven.repo.local=path/to/new/local/m2`

- Make sure that path of `<p2_repo_folder>` under `<properties>` section in product-apim/modules/is-km/pom.xml, is pointing to the correct p2 location. The default should be "../p2-profile/product/target/p2-repo"

- Set the `<keymanager.feature.version>` under `<properties>` section in product-apim/modules/is-km/pom.xml to the current carbon-apimgt version.

- Copy the respective WSO2 IS distribution to product-apim/modules/is-km directory. Unzip it.

- Set the `<version>` under `<parent>` in product-apim/modules/is-km/pom.xml, to the current API Manager version.

- Set the `<version>` under `<project>` in product-apim/modules/is-km/pom.xml, to the current Identity Server version.

- Put the name of the extracted folder as `<is_folder_name>` under `<properties>` in product-apim/modules/is-km/pom.xml.

- Open `${is_folder_name}/repository/conf/identity/identity.xml`

- Uncomment `<IdentityOAuthTokenGenerator>` under `<OAuth>` section and save the file.

- Open `${is_folder_name}/repository/conf/user-mgt.xml`

- Uncomment `<UserStoreManager class="org.wso2.carbon.user.core.jdbc.JDBCUserStoreManager">` section in user-mgt.xml.

- Comment `<UserStoreManager class="org.wso2.carbon.user.core.ldap.ReadWriteLDAPUserStoreManager">` section in user-mgt.xml.

- Goto product-apim/modules/is-km and run the following command.
`mvn clean install`

- After that, folder pointed in `<is_folder_name>` will be the IS as Key Manager.

- Rename the folder accordingly (Eg - wso2is-km-5.6.0), then zip it.
