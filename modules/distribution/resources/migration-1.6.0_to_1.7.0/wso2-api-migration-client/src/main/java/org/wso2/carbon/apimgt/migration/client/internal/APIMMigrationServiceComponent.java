/*
* Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.wso2.carbon.apimgt.migration.client.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wso2.carbon.apimgt.impl.APIManagerConfigurationService;
import org.wso2.carbon.apimgt.impl.utils.APIMgtDBUtil;
import org.wso2.carbon.apimgt.migration.APIMigrationException;
import org.wso2.carbon.apimgt.migration.client.MigrateFrom16to17;
import org.wso2.carbon.apimgt.migration.util.Constants;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.service.TenantRegistryLoader;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.CarbonUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;


/**
 * @scr.component name="org.wso2.carbon.apimgt.migration.client" immediate="true"
 * @scr.reference name="realm.service"
 * interface="org.wso2.carbon.user.core.service.RealmService" cardinality="1..1"
 * policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService" cardinality="1..1"
 * policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="registry.core.dscomponent"
 * interface="org.wso2.carbon.registry.core.service.RegistryService" cardinality="1..1"
 * policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="tenant.registryloader" interface="org.wso2.carbon.registry.core.service.TenantRegistryLoader" cardinality="1..1"
 * policy="dynamic" bind="setTenantRegistryLoader" unbind="unsetTenantRegistryLoader"
 * @scr.reference name="apim.configuration" interface="org.wso2.carbon.apimgt.impl.APIManagerConfigurationService" cardinality="1..1"
 * policy="dynamic" bind="setApiManagerConfig" unbind="unsetApiManagerConfig"
 */

@SuppressWarnings("unused")
public class APIMMigrationServiceComponent {

    private static final Log log = LogFactory.getLog(APIMMigrationServiceComponent.class);

    /**
     * Method to activate bundle.
     *
     * @param context OSGi component context.
     */
    protected void activate(ComponentContext context) {
        boolean isCorrectProductVersion = false;
        try {
            APIMgtDBUtil.initialize();
        } catch (Exception e) {
            //APIMgtDBUtil.initialize() throws generic exception
            log.error("Error occurred while initializing DB Util ", e);
        }

        // Product and version validation
        File carbonXmlConfig = new File(CarbonUtils.getServerXml());

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(carbonXmlConfig);

            doc.getDocumentElement().normalize();

            NodeList nameNodes = doc.getElementsByTagName("Name");

            if (nameNodes.getLength() > 0) {
                Element name = (Element) nameNodes.item(0);
                if (Constants.APIM_PRODUCT_NAME.equals(name.getTextContent())) {
                    NodeList versionNodes = doc.getElementsByTagName("Version");

                    if (versionNodes.getLength() > 0) {
                        Element version = (Element) versionNodes.item(0);
                        if (Constants.VERSION_1_7.equals(version.getTextContent())) {
                            isCorrectProductVersion = true;
                        }
                    }
                }
            }
        } catch (ParserConfigurationException e) {
            log.error("ParserConfigurationException when processing carbon.xml", e);
        } catch (SAXException e) {
            log.error("SAXException when processing carbon.xml", e);
        } catch (IOException e) {
            log.error("IOException when processing carbon.xml", e);
        }

        String tenants = System.getProperty(Constants.ARG_MIGRATE_TENANTS);
        boolean migrateAll = Boolean.parseBoolean(System.getProperty(Constants.ARG_MIGRATE_ALL));
        boolean isRegistryMigration = Boolean.parseBoolean(System.getProperty(Constants.ARG_MIGRATE_REG));
        boolean isFileSystemMigration = Boolean.parseBoolean(System.getProperty(Constants.ARG_MIGRATE_FILE_SYSTEM));

        try {
            if (isCorrectProductVersion) {
                log.info("Migrating WSO2 API Manager " + Constants.PREVIOUS_VERSION + " to WSO2 API Manager " + Constants.VERSION_1_7);

                // Create a thread and wait till the APIManager DBUtils is initialized

                MigrateFrom16to17 migrateFrom16to17 = new MigrateFrom16to17(tenants);

                boolean isArgumentValid = false;

                //Default operation will migrate all three types of resources
                if (migrateAll) {
                    log.info("Migrating WSO2 API Manager  " + Constants.PREVIOUS_VERSION + " resources to WSO2 API Manager " + Constants.VERSION_1_7);
                    migrateFrom16to17.registryResourceMigration();
                    migrateFrom16to17.fileSystemMigration();
                    isArgumentValid = true;
                } else {
                    //Only performs registry migration
                    if (isRegistryMigration) {
                        log.info("Migrating WSO2 API Manager " + Constants.PREVIOUS_VERSION + "  registry resources to WSO2 API Manager " + Constants.VERSION_1_7);
                        migrateFrom16to17.registryResourceMigration();
                        isArgumentValid = true;
                    }
                    //Only performs file system migration
                    if (isFileSystemMigration) {
                        log.info("Migrating WSO2 API Manager " + Constants.PREVIOUS_VERSION + "  file system resources to WSO2 API Manager " + Constants.VERSION_1_7);
                        migrateFrom16to17.fileSystemMigration();
                        isArgumentValid = true;
                    }
                }

                if (isArgumentValid) {
                    log.info("API Manager " + Constants.PREVIOUS_VERSION + "  to  " + Constants.VERSION_1_7 + " migration successfully completed");
                }
            } else {
                log.error("Migration client installed in incompatible product version. This migration client is only compatible with " +
                        Constants.APIM_PRODUCT_NAME + " " + Constants.VERSION_1_7 + ". Please verify the product/version in use.");
            }
        } catch (APIMigrationException e) {
            log.error("API Management  exception occurred while migrating", e);
        } catch (UserStoreException e) {
            log.error("User store  exception occurred while migrating", e);
        } catch (Exception e) {
            log.error("Generic exception occurred while migrating", e);
        } catch (Throwable t) {
            log.error("Throwable error", t);
        }
        log.info("WSO2 API Manager migration component successfully activated.");
    }

    /**
     * Method to deactivate bundle.
     *
     * @param context OSGi component context.
     */
    protected void deactivate(ComponentContext context) {
        log.info("WSO2 API Manager migration bundle is deactivated");
    }

    /**
     * Method to set registry service.
     *
     * @param registryService service to get tenant data.
     */
    protected void setRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting RegistryService for WSO2 API Manager migration");
        }
        ServiceHolder.setRegistryService(registryService);
    }

    /**
     * Method to unset registry service.
     *
     * @param registryService service to get registry data.
     */
    protected void unsetRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("Unset Registry service");
        }
        ServiceHolder.setRegistryService(null);
    }

    /**
     * Method to set realm service.
     *
     * @param realmService service to get tenant data.
     */
    protected void setRealmService(RealmService realmService) {
        log.debug("Setting RealmService for WSO2 API Manager migration");
        ServiceHolder.setRealmService(realmService);
    }

    /**
     * Method to unset realm service.
     *
     * @param realmService service to get tenant data.
     */
    protected void unsetRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.debug("Unset Realm service");
        }
        ServiceHolder.setRealmService(null);
    }

    /**
     * Method to set tenant registry loader
     *
     * @param tenantRegLoader tenant registry loader
     */
    protected void setTenantRegistryLoader(TenantRegistryLoader tenantRegLoader) {
        log.debug("Setting TenantRegistryLoader for WSO2 API Manager migration");
        ServiceHolder.setTenantRegLoader(tenantRegLoader);
    }

    /**
     * Method to unset tenant registry loader
     *
     * @param tenantRegLoader tenant registry loader
     */
    protected void unsetTenantRegistryLoader(TenantRegistryLoader tenantRegLoader) {
        log.debug("Unset Tenant Registry Loader");
        ServiceHolder.setTenantRegLoader(null);
    }

    /**
     * Method to set API Manager configuration
     *
     * @param apiManagerConfig api manager configuration
     */
    protected void setApiManagerConfig(APIManagerConfigurationService apiManagerConfig) {
        log.info("Setting APIManager configuration");
    }

    /**
     * Method to unset API manager configuration
     *
     * @param apiManagerConfig api manager configuration
     */
    protected void unsetApiManagerConfig(APIManagerConfigurationService apiManagerConfig) {
        log.info("Un-setting APIManager configuration");
    }

}
