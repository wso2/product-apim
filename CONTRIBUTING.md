# Contributing to WSO2 API Manager

## Overview

## Project Resources

- **Home page:**           https://wso2.com/api-manager/
- **Docs:**                https://apim.docs.wso2.com/en/latest/
- **Issue Tracker:**       https://github.com/wso2/api-manager/issues

## Building the Distribution from Source

To build the WSO2 API Manager distribution from source, follow these steps:

1. Install **Java SE Development Kit 11**.
2. Install **Apache Maven 3.x.x** ([Download Maven](https://maven.apache.org/download.cgi#)).
3. Clone the repository: `https://github.com/wso2/product-apim.git` or download the source.
4. From the `product-apim` directory, run one of the following Maven commands:
    - `mvn clean install`  
      _(Builds the binary and source distributions with tests)_
    - `mvn clean install -Dmaven.test.skip=true`  
      _(Builds the binary and source distributions without running any unit/integration tests)_
5. The binary distribution will be available in the `product-apim/all-in-one-apim/modules/distribution/product/target` directory.


## Bug Fixing

WSO2 API Manager is built with many components. The source code for each component resides in its own repository. The two main components used within the product are:

- Core API Manager component: https://github.com/wso2/carbon-apimgt
- UI components: https://github.com/wso2/apim-apps

Most product-related issues need to be fixed in these two components. Instructions for building each component can be found in the README files of their respective repositories.

Once you implement a bug fix, build the component locally.

To apply the fix to the product, you need to update the component version in the product.

For backend-related fixes, update the carbon-apimgt component version (e.g., <carbon.apimgt.version> to x.x.x-SNAPSHOT) in https://github.com/wso2/product-apim/blob/master/all-in-one-apim/pom.xml 

For UI-related fixes, update the apim-apps component version in <carbon.apimgt.ui.version> in https://github.com/wso2/product-apim/blob/master/all-in-one-apim/pom.xml 

After updating the version, rebuild the product-apim repository.

## Debug the Product

1. Unzip the product distribution (e.g., wso2am-4.5.0.zip).
2. Identify the component version used in the product. You can find this by:
    - Checking the component version in the `repository/components/plugins/` folder, or
    - Viewing the component version in the corresponding product release tag (ex: https://github.com/wso2/product-apim/blob/v4.5.0/all-in-one-apim/pom.xml for APIM 4.5.0).
3. Clone the relevant component repository and check out the tag that matches the component version. Example: The carbon-apimgt component version used in WSO2 API Manager 4.5.0 is 9.31.86. The source code for this version is available at:
https://github.com/wso2/carbon-apimgt/tree/v9.31.86
4. Load the source code into your IDE.
5. Start the product in debug mode:
   - Navigate to `wso2am-4.5.0/bin/`
   - Run:
   - On Linux/macOS: `./api-manager.sh -debug 5005`
   - On Windows: `./api-manager.bat -debug 5005` 
6. Connect to the server using your IDE’s remote debug option.

## Contributing Code

All the bug fixes are added to the master branch and will be included in an upcoming product release. To include the fix,

1. Fork the repository on GitHub.
2. Create a new branch for your changes.
3. Make your changes and commit them with a descriptive message.
4. Push your changes to your forked repository.
5. Create a pull request against the `master` branch of the main repository.
6. Ensure your code adheres to the project's coding standards and passes all tests.
7. Provide a clear description of your changes and the reason for them in the pull request.
8. For more information, refer to [https://wso2.github.io/](https://wso2.github.io/)

