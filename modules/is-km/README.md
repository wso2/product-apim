### Instructions to build IS as Key Manager

- Build product-apim (clean repo mode)
`mvn clean install -Dmaven.repo.local=path/to/new/local/m2`

- Make sure that path of `<p2_repo_folder>` under `<properties>` section in product-apim/modules/is-km/pom.xml, is pointing to the correct p2 location. The default should be "../p2-profile/product/target/p2-repo"

- Set the `<keymanager.feature.version>` under `<properties>` section in product-apim/modules/is-km/pom.xml to the current carbon-apimgt version.

- Copy the respective WSO2 IS distribution to product-apim/modules/is-km directory. Unzip it.

- Set the `<version>` under `<parent>` in product-apim/modules/is-km/pom.xml, to the current API Manager version.

- Set the `<version>` under `<project>` in product-apim/modules/is-km/pom.xml, to the current Identity Server version.

- Put the name of the extracted folder as `<is_folder_name>` under `<properties>` in product-apim/modules/is-km/pom.xml.

- Goto product-apim/modules/is-km and run the following command.
`mvn clean install -Dmaven.repo.local=path/to/new/local/m2`

- It's required to change a element in identity.xml.j2 located in 
  {IS-KM-HOME}/repository/resources/conf/templates/repository/conf/identity/. 
  This is not required when that element is configurable.
  
  Change the following element as follow.
  
  Existing element -
  
  <EventListener id="{{listener.id}}"
                         type="{{listener.type}}"
                         name="{{listener.name}}"
                         orderId="{{listener.order}}"
                         enable="true">
                         
   Element after the change -
                        
  <EventListener id="{{listener.id}}"
                         type="{{listener.type}}"
                         name="{{listener.name}}"
                         orderId="{{listener.order}}"
                         enable="{{listener.enable}}">                       


- After that, folder pointed in `<is_folder_name>` will be the IS as Key Manager.

- Rename the folder accordingly (Eg - wso2is-km-5.9.0), then zip it.
