package org.wso2.am.integration.tests.restapi;

import org.apache.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.am.integration.clients.publisher.api.v1.dto.*;
import org.wso2.am.integration.clients.service.catalog.api.ApiException;
import org.wso2.am.integration.clients.service.catalog.api.ApiResponse;
import org.wso2.am.integration.clients.service.catalog.api.v1.dto.APIListDTO;
import org.wso2.am.integration.clients.service.catalog.api.v1.dto.ServiceDTO;
import org.wso2.am.integration.clients.service.catalog.api.v1.dto.ServiceInfoListDTO;
import org.wso2.am.integration.clients.service.catalog.api.v1.dto.ServiceListDTO;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.generic.TestConfigurationProvider;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.carbon.automation.engine.context.TestUserMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE})
public class ServiceCatalogRestAPITestCase extends APIMIntegrationBaseTest {

    private File definitionFileSampleOne;

    private String serviceIdOne = "";

    private String serviceIdTwo = "";

    private final String invalidServiceId = "01234567-0123-0123-0123";

    private final String emptyServiceId = null;

    @Factory(dataProvider = "userModeDataProvider")
    public ServiceCatalogRestAPITestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_ADMIN},
                new Object[]{TestUserMode.TENANT_ADMIN},
                new Object[]{TestUserMode.SUPER_TENANT_USER_STORE_USER},
                new Object[]{TestUserMode.SUPER_TENANT_EMAIL_USER},
                new Object[]{TestUserMode.TENANT_EMAIL_USER},
        };
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init(userMode);
    }

    @Test(groups = {"wso2.am"}, description = "Create a Service through the Service Catalog Rest API")
    public void testCreateAService() throws Exception {

        ServiceDTO serviceMetadataSampleOne = new ServiceDTO();
        serviceMetadataSampleOne.setName("Pizzashack-Endpoint");
        serviceMetadataSampleOne.setDescription("A Catalog Entry that exposes a Pizza REST endpoint");
        serviceMetadataSampleOne.setVersion("v1");
        serviceMetadataSampleOne.serviceKey("Pizzashack-Endpoint-1.0.0");
        serviceMetadataSampleOne.serviceUrl("http://localhost/pizzashack");
        serviceMetadataSampleOne.definitionType(ServiceDTO.DefinitionTypeEnum.OAS3);
        serviceMetadataSampleOne.setSecurityType(ServiceDTO.SecurityTypeEnum.BASIC);
        serviceMetadataSampleOne.setMutualSSLEnabled(false);
        serviceMetadataSampleOne.setDefinitionUrl("https://petstore.swagger.io/v2/swagger.json");

        String filePath = TestConfigurationProvider.getResourceLocation() + File.separator + "service-catalog" + File.separator + "definition1.yaml";
        definitionFileSampleOne = new File(filePath);

        // Create service
        ServiceDTO createServiceResOne = restAPIServiceCatalog.createService(serviceMetadataSampleOne, definitionFileSampleOne, null);
        Assert.assertNotNull(createServiceResOne);
        Assert.assertNotNull(createServiceResOne.getName());
        Assert.assertEquals(createServiceResOne.getName(), "Pizzashack-Endpoint");
        Assert.assertEquals(createServiceResOne.getVersion(), "v1");
        Assert.assertEquals(createServiceResOne.getServiceKey(), "Pizzashack-Endpoint-1.0.0");
        Assert.assertNotNull(createServiceResOne.getId());
        serviceIdOne = createServiceResOne.getId();

        ServiceDTO serviceMetadataSampleTwo = new ServiceDTO();
        serviceMetadataSampleTwo.setName("Petstore-Endpoint-1");
        serviceMetadataSampleTwo.setDescription("This is a sample server Petstore server");
        serviceMetadataSampleTwo.setVersion("1.0.0");
        serviceMetadataSampleTwo.serviceKey("Petstore-Endpoint-1");
        serviceMetadataSampleTwo.serviceUrl("https://localhost/api/am/service/catalog/services");
        serviceMetadataSampleTwo.definitionType(ServiceDTO.DefinitionTypeEnum.OAS3);
        serviceMetadataSampleTwo.setSecurityType(ServiceDTO.SecurityTypeEnum.BASIC);
        serviceMetadataSampleTwo.setMutualSSLEnabled(false);

        String filePath1 = TestConfigurationProvider.getResourceLocation() + File.separator + "service-catalog" + File.separator + "definition2.yaml";
        File definitionFileSampleTwo = new File(filePath1);

        // Create service without definition file
        try {
            restAPIServiceCatalog.createService(serviceMetadataSampleTwo, null, null);
        } catch (ApiException e) {
            Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getCode());
        }

        //Create second service
        ServiceDTO createServiceResTwo = restAPIServiceCatalog.createService(serviceMetadataSampleTwo, definitionFileSampleTwo, null);
        Assert.assertNotNull(createServiceResTwo);
        Assert.assertNotNull(createServiceResTwo.getName());
        Assert.assertEquals(createServiceResTwo.getName(), "Petstore-Endpoint-1");
        Assert.assertEquals(createServiceResTwo.getVersion(), "1.0.0");
        Assert.assertEquals(createServiceResTwo.getServiceKey(), "Petstore-Endpoint-1");
        Assert.assertNotNull(createServiceResTwo.getId());
        serviceIdTwo = createServiceResTwo.getId();

        // Create service without serviceMetaData file
        try {
            restAPIServiceCatalog.createService(null, definitionFileSampleOne, null);
        } catch (ApiException e) {
            Assert.assertEquals("Missing the required parameter 'serviceMetadata' when calling addService(Async)", e.getMessage());
        }

        /*
          Create a service with the same key as an existing one
          Use the above serviceMetaData file and definition file
         */
        try {
            restAPIServiceCatalog.createService(serviceMetadataSampleOne, definitionFileSampleOne, null);
        } catch (ApiException e) {
            Assert.assertEquals(HttpStatus.SC_CONFLICT, e.getCode());
        }
    }

    @Test(groups = {"wso2.am"}, description = "Get Service by UUID through the Service Catalog Rest API", dependsOnMethods = "testCreateAService")
    public void testGetServiceByUUID() throws Exception {

        // Retrieve Service by UUID
        if (!serviceIdOne.equals("")) {
            ServiceDTO getServiceByIDRes = restAPIServiceCatalog.retrieveServiceById(serviceIdOne);
            Assert.assertNotNull(getServiceByIDRes);
            Assert.assertEquals(getServiceByIDRes.getName(), "Pizzashack-Endpoint");
            Assert.assertEquals(getServiceByIDRes.getVersion(), "v1");
        }

        // Retrieve Service with invalid service id
        try {
            restAPIServiceCatalog.retrieveServiceById(invalidServiceId);
        } catch (ApiException e) {
            Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getCode());
        }

        // Retrieve Service with empty service id
        try {
            restAPIServiceCatalog.retrieveServiceById(emptyServiceId);
        } catch (ApiException e) {
            Assert.assertEquals("Missing the required parameter 'serviceId' when calling getServiceById(Async)", e.getMessage());
        }
    }

    @Test(groups = {"wso2.am"}, description = "Search Services through the Service Catalog Rest API", dependsOnMethods = "testGetServiceByUUID")
    public void testSearchService() throws Exception {

        // Search by Name
        ServiceListDTO getServiceByNameRes = restAPIServiceCatalog.retrieveServices("Pizzashack-Endpoint", null, null, null, null, null, null, 25, 0);
        Assert.assertNotNull(getServiceByNameRes);
        Assert.assertNotNull(getServiceByNameRes.getList());
        Assert.assertNotNull(getServiceByNameRes.getList().get(0));
        Assert.assertEquals(getServiceByNameRes.getList().get(0).getName(), "Pizzashack-Endpoint");

        // Search by Version
        ServiceListDTO getServiceByVersionRes = restAPIServiceCatalog.retrieveServices(null, "v1", null, null, null, null, null, 25, 0);
        Assert.assertNotNull(getServiceByVersionRes);
        Assert.assertNotNull(getServiceByVersionRes.getList());
        Assert.assertNotNull(getServiceByVersionRes.getList().get(0));
        Assert.assertEquals(getServiceByVersionRes.getList().get(0).getVersion(), "v1");

        // Search by Definition Type
        ServiceListDTO getServiceByDefTypeRes = restAPIServiceCatalog.retrieveServices(null, null, "OAS3", null, null, null, null, 25, 0);
        Assert.assertNotNull(getServiceByDefTypeRes);
        Assert.assertNotNull(getServiceByDefTypeRes.getList());
        Assert.assertNotNull(getServiceByDefTypeRes.getList().get(0));
        Assert.assertEquals(getServiceByDefTypeRes.getList().get(0).getDefinitionType(), ServiceDTO.DefinitionTypeEnum.OAS3);

        // Search by Service Key
        ServiceListDTO getServiceByKeyRes = restAPIServiceCatalog.retrieveServices(null, null, null, "Pizzashack-Endpoint-1.0.0", null, null, null, 25, 0);
        Assert.assertNotNull(getServiceByKeyRes);
        Assert.assertNotNull(getServiceByKeyRes.getList());
        Assert.assertNotNull(getServiceByKeyRes.getList().get(0));
        Assert.assertEquals(getServiceByKeyRes.getList().get(0).getServiceKey(), "Pizzashack-Endpoint-1.0.0");

        // Search by name in Asc order
        ServiceListDTO getServiceAscOrderRes = restAPIServiceCatalog.retrieveServices(null, null, null, null, null, "name", "asc", 25, 0);
        Assert.assertNotNull(getServiceAscOrderRes);
        Assert.assertNotNull(getServiceAscOrderRes.getList());
        Assert.assertNotNull(getServiceAscOrderRes.getList().get(0));
        Assert.assertEquals(getServiceAscOrderRes.getList().get(0).getName(), "Petstore-Endpoint-1");
        Assert.assertNotNull(getServiceAscOrderRes.getList().get(1));
        Assert.assertEquals(getServiceAscOrderRes.getList().get(1).getName(), "Pizzashack-Endpoint");

        // Search by name in Desc order
        ServiceListDTO getServiceDescOrderRes = restAPIServiceCatalog.retrieveServices(null, null, null, null, null, "name", "desc", 25, 0);
        Assert.assertNotNull(getServiceDescOrderRes);
        Assert.assertNotNull(getServiceDescOrderRes.getList());
        Assert.assertNotNull(getServiceDescOrderRes.getList().get(0));
        Assert.assertEquals(getServiceDescOrderRes.getList().get(0).getName(), "Pizzashack-Endpoint");
        Assert.assertNotNull(getServiceDescOrderRes.getList().get(1));
        Assert.assertEquals(getServiceDescOrderRes.getList().get(1).getName(), "Petstore-Endpoint-1");

        //Retrieve services in N limit
        ServiceListDTO getServiceByLimitRes = restAPIServiceCatalog.retrieveServices(null, null, null, null, null, "name", null, 1, 0);
        Assert.assertNotNull(getServiceByLimitRes);
        Assert.assertNotNull(getServiceByLimitRes.getList());
        Assert.assertEquals(getServiceByLimitRes.getList().size(), 1);
        Assert.assertNotNull(getServiceByLimitRes.getList().get(0));
        Assert.assertEquals(getServiceByLimitRes.getList().get(0).getName(), "Petstore-Endpoint-1");

        //Retrieve services after N offset
        ServiceListDTO getServiceByOffsetRes = restAPIServiceCatalog.retrieveServices(null, null, null, null, null, "name", null, 1, 1);
        Assert.assertNotNull(getServiceByOffsetRes);
        Assert.assertNotNull(getServiceByOffsetRes.getList());
        Assert.assertEquals(getServiceByOffsetRes.getList().size(), 1);
        Assert.assertNotNull(getServiceByOffsetRes.getList().get(0));
        Assert.assertEquals(getServiceByOffsetRes.getList().get(0).getName(), "Pizzashack-Endpoint");

        //Search by invalid definitionType
        try {
            restAPIServiceCatalog.retrieveServices(null, null, "OS3", null, null, null, null, 25, 0);
        } catch (ApiException e) {
            Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getCode());
        }

        //Search by invalid sortBy value
        try {
            restAPIServiceCatalog.retrieveServices(null, null, null, null, null, "defType", "asc", 25, 0);
        } catch (ApiException e) {
            Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getCode());
        }

        //Search by invalid sortOrder value
        try {
            restAPIServiceCatalog.retrieveServices(null, null, null, null, null, "name", "acs", 25, 0);
        } catch (ApiException e) {
            Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getCode());
        }
    }

    @Test(groups = {"wso2.am"}, description = "Get Service Definition by UUID through the Service Catalog Rest API", dependsOnMethods = "testSearchService")
    public void testGetServiceDefinition() throws Exception {

        // Get service definition
        if (!serviceIdOne.equals("")) {
            String serviceDefinitionRes = restAPIServiceCatalog.retrieveServiceDefinition(serviceIdOne);
            Assert.assertNotNull(serviceDefinitionRes);
        }

        // Get Service Definition by Invalid UUID
        try {
            restAPIServiceCatalog.retrieveServiceDefinition(invalidServiceId);
        } catch (ApiException e) {
            Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getCode());
        }
    }

    @Test(groups = {"wso2.am"}, description = "Update Service through the Service Catalog Rest API", dependsOnMethods = "testGetServiceDefinition")
    public void testUpdateService() throws Exception {

        // Update service
        ServiceDTO serviceMetadataSampleThree = new ServiceDTO();
        serviceMetadataSampleThree.setName("Pizzashack-Endpoint");
        serviceMetadataSampleThree.setDescription("Updated Catalog Entry that exposes a Pizza REST endpoint");
        serviceMetadataSampleThree.setVersion("v1");
        serviceMetadataSampleThree.serviceKey("Pizzashack-Endpoint-1.0.0");
        serviceMetadataSampleThree.serviceUrl("http://localhost/pizzashack");
        serviceMetadataSampleThree.definitionType(ServiceDTO.DefinitionTypeEnum.OAS3);
        serviceMetadataSampleThree.setSecurityType(ServiceDTO.SecurityTypeEnum.BASIC);
        serviceMetadataSampleThree.setMutualSSLEnabled(false);
        serviceMetadataSampleThree.setDefinitionUrl("https://petstore.swagger.io/v2/swagger.json");
        if (!serviceIdOne.equals("")) {
            ServiceDTO updateServiceRes = restAPIServiceCatalog.updateService(serviceIdOne, serviceMetadataSampleThree, definitionFileSampleOne, null);
            Assert.assertNotNull(updateServiceRes);
            Assert.assertEquals(updateServiceRes.getId(), serviceIdOne);
            Assert.assertEquals(updateServiceRes.getName(), "Pizzashack-Endpoint");
            Assert.assertEquals(updateServiceRes.getVersion(), "v1");
            Assert.assertEquals(updateServiceRes.getServiceKey(), "Pizzashack-Endpoint-1.0.0");
            Assert.assertEquals(updateServiceRes.getDescription(), "Updated Catalog Entry that exposes a Pizza REST endpoint");
        }

        // Update service with invalid UUID
        try {
            restAPIServiceCatalog.updateService(invalidServiceId, serviceMetadataSampleThree, definitionFileSampleOne, null);
        } catch (ApiException e) {
            Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getCode());
        }

        // Update service without definition file
        try {
            restAPIServiceCatalog.updateService(serviceIdOne, serviceMetadataSampleThree, null, null);
        } catch (ApiException e) {
            Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getCode());
        }

        // Update a service without serviceMetaData file
        try {
            restAPIServiceCatalog.updateService(serviceIdOne, null, definitionFileSampleOne, null);
        } catch (ApiException e) {
            Assert.assertEquals("Missing the required parameter 'serviceMetadata' when calling updateService(Async)", e.getMessage());
        }
    }

    @Test(groups = {"wso2.am"}, description = "Import Service through the Service Catalog Rest API", dependsOnMethods = "testUpdateService")
    public void testImportService() throws Exception {

        // Import Service
        String zipFilePathOne = TestConfigurationProvider.getResourceLocation() + File.separator + "service-catalog" + File.separator + "service1.zip";
        File servicesFileOne = new File(zipFilePathOne);

        ServiceInfoListDTO importServiceInfoListRes = restAPIServiceCatalog.importService(servicesFileOne, true, null);
        Assert.assertNotNull(importServiceInfoListRes);
        Assert.assertNotNull(importServiceInfoListRes.getList());
        Assert.assertNotNull(importServiceInfoListRes.getList().get(0).getName());
        Assert.assertEquals(importServiceInfoListRes.getList().get(0).getName(), "Pizzashack-Endpoint-v2");

        // Import a service without a zip file
        try {
            restAPIServiceCatalog.importService(null, true, null);
        } catch (ApiException e) {
            Assert.assertEquals("Missing the required parameter 'file' when calling importService(Async)", e.getMessage());
        }

        String zipFilePathTwo = TestConfigurationProvider.getResourceLocation() + File.separator + "service-catalog" + File.separator + "service2.zip";
        File servicesFileTwo = new File(zipFilePathTwo);

        // Import service with existing name and version with overwrite = false
        try {
            restAPIServiceCatalog.importService(servicesFileTwo, false, null);
        } catch (ApiException e) {
            Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getCode());
        }

        // Import service with existing name and version with overwrite = true
        ServiceInfoListDTO importServiceOverwriteRes = restAPIServiceCatalog.importService(servicesFileTwo, true, null);
        Assert.assertNotNull(importServiceOverwriteRes);
        Assert.assertNotNull(importServiceOverwriteRes.getList());
        Assert.assertNotNull(importServiceOverwriteRes.getList().get(0).getName());
        Assert.assertEquals(importServiceOverwriteRes.getList().get(0).getName(), "Pizzashack-Endpoint");
        Assert.assertEquals(importServiceOverwriteRes.getList().get(0).getKey(), "Pizzashack-Endpoint-1.0.0");
    }

    @Test(groups = {"wso2.am"}, description = "Export Service through the Service Catalog Rest API", dependsOnMethods = "testImportService")
    public void testExportService() throws Exception {

        // Export Services
        File exportServiceRes = restAPIServiceCatalog.exportService("Pizzashack-Endpoint", "v1");
        Assert.assertNotNull(exportServiceRes);

        // Export services with wrong name or version
        try {
            restAPIServiceCatalog.exportService("Pizzashack", "v1");
        } catch (ApiException e) {
            Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getCode());
        }
    }

    @Test(groups = {"wso2.am"}, description = "Create an API Through the Publisher Rest API", dependsOnMethods = "testExportService")
    public void testCreateAnAPIThroughPublisher() throws Exception {

        APIDTO apiCreationDTO = new APIDTO();
        apiCreationDTO.setName("PizzaShackAPI");
        apiCreationDTO.setDescription("This is a simple API for Pizza Shack online pizza delivery store.");
        apiCreationDTO.setContext("pizza");
        apiCreationDTO.setVersion("1.0.0");
        apiCreationDTO.setProvider("admin");
        apiCreationDTO.setLifeCycleStatus("CREATED");

        apiCreationDTO.setType(APIDTO.TypeEnum.HTTP);
        apiCreationDTO.setAudience(APIDTO.AudienceEnum.PUBLIC);
        apiCreationDTO.setIsDefaultVersion(false);

        apiCreationDTO.setAccessControl(APIDTO.AccessControlEnum.NONE);

        APIBusinessInformationDTO apiBusinessInformationDTO = new APIBusinessInformationDTO();
        apiBusinessInformationDTO.setBusinessOwner("businessowner");
        apiBusinessInformationDTO.setBusinessOwnerEmail("businessowner@wso2.com");
        apiBusinessInformationDTO.setTechnicalOwner("technicalowner");
        apiBusinessInformationDTO.setTechnicalOwnerEmail("technicalowner@wso2.com");
        apiCreationDTO.setBusinessInformation(apiBusinessInformationDTO);

        APIOperationsDTO apiOperationsDTOSampleOne = new APIOperationsDTO();
        apiOperationsDTOSampleOne.setVerb("GET");
        apiOperationsDTOSampleOne.setTarget("/menu");

        APIOperationsDTO apiOperationsDTOSampleTwo = new APIOperationsDTO();
        apiOperationsDTOSampleTwo.setVerb("POST");
        apiOperationsDTOSampleTwo.setTarget("/order/{orderId}");

        List<APIOperationsDTO> operationsDTOS = new ArrayList<>();
        operationsDTOS.add(apiOperationsDTOSampleOne);
        operationsDTOS.add(apiOperationsDTOSampleTwo);
        apiCreationDTO.setOperations(operationsDTOS);

        APIServiceInfoDTO apiServiceInfoDTO = new APIServiceInfoDTO();
        apiServiceInfoDTO.setKey("Pizzashack-Endpoint-1.0.0");
        apiServiceInfoDTO.setName("Pizzashack-Endpoint");
        apiServiceInfoDTO.setVersion("v1");
        apiServiceInfoDTO.setOutdated(false);
        apiCreationDTO.setServiceInfo(apiServiceInfoDTO);

        APIDTO apidto = restAPIPublisher.addAPI(apiCreationDTO, "v3");
        Assert.assertNotNull(apidto);
        Assert.assertNotNull(apidto.getServiceInfo());
        Assert.assertEquals(apidto.getServiceInfo().getName(), "Pizzashack-Endpoint");
        Assert.assertEquals(apidto.getServiceInfo().getKey(), "Pizzashack-Endpoint-1.0.0");
    }

    @Test(groups = {"wso2.am"}, description = "Get Service Usage by UUID through the Service Catalog Rest API", dependsOnMethods = "testCreateAnAPIThroughPublisher")
    public void testGetServiceUsage() throws Exception {

        // Get service usage
        if (!serviceIdOne.equals("")) {
            APIListDTO serviceUsageRes = restAPIServiceCatalog.retrieveServiceUsage(serviceIdOne);
            Assert.assertNotNull(serviceUsageRes);
            Assert.assertNotNull(serviceUsageRes.getList());
            Assert.assertEquals(serviceUsageRes.getList().size(), 1);
            Assert.assertEquals(serviceUsageRes.getList().get(0).getName(), "PizzaShackAPI");
        }

        // Get Service Usage by Invalid UUID
        try {
            restAPIServiceCatalog.retrieveServiceUsage(invalidServiceId);
        } catch (ApiException e) {
            Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getCode());
        }
    }

    @Test(groups = {"wso2.am"}, description = "Delete Service through the Service Catalog Rest API", dependsOnMethods = "testGetServiceUsage")
    public void testDeleteService() throws Exception {

        // Try to delete a service used by an API
        if (!serviceIdOne.equals("")) {
            try {
                restAPIServiceCatalog.deleteService(serviceIdOne);
            } catch (ApiException e) {
                Assert.assertEquals(HttpStatus.SC_CONFLICT, e.getCode());
            }
        }

        // Delete Service
        if (!serviceIdTwo.equals("")) {
          ApiResponse deleteServiceRes =  restAPIServiceCatalog.deleteService(serviceIdTwo);
          Assert.assertEquals(HttpStatus.SC_NO_CONTENT, deleteServiceRes.getStatusCode());
        }

        // Delete Service By Invalid UUID
        try {
            restAPIServiceCatalog.deleteService(invalidServiceId);
        } catch (ApiException e) {
            Assert.assertEquals(HttpStatus.SC_NOT_FOUND, e.getCode());
        }
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        super.cleanUp();
    }
}
