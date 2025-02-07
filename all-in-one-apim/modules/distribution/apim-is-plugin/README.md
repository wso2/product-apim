# WSO2 APIM IS Plugin - 4.3.0

## Prerequisites:
1. Download WSO2 IS product
2. Download WSO2 APIM IS Plugin product

## Installation Steps:
1. Extract WSO2 IS product. Let's call it `<WSO2_IS_HOME>`.
2. Extract WSO2 APIM IS Plugin. Lets call it `<WSO2_APIM_IS_PLUGIN_HOME>`.
3. Navigate to `<WSO2_APIM_IS_PLUGIN_HOME>` directory and execute `./bin/merge.sh` command by providing `<WSO2_IS_HOME>` as argument. This will copy the key manager artifacts to the WSO2 IS.

## Audit Logs:
- Running `merge.sh` script creates an audit log folder in the product home. Structure of it looks like below;

    ``` sh
    apim-is-plugin
    ├── backup
    │   ├── dropins
    │   └── webapps
    └── merge_audit.log
    ```
- `backup` folder contains the files that were originally there in the IS product before running the plugin. Please note that only the last state will be there.