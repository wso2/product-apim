/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

import beans.RawDataBean;
import org.wso2.carbon.apimgt.samples.utils.SampleUtils;
import org.wso2.carbon.apimgt.samples.utils.TenantUtils;
import org.wso2.carbon.apimgt.samples.utils.beans.ApiBean;
import org.wso2.carbon.apimgt.samples.utils.beans.TenantBean;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.ApiException;
import org.wso2.carbon.apimgt.samples.utils.publisher.rest.client.model.API;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CreateSampleTwoRawData {

    private static final String hostname = "localhost";
    private static final String port = "9443";
    private static final String serviceEndpoint = "https://" + hostname + ":" + port + "/services/";
    private static List<RawDataBean> rawDataList = createRawDataList();

    public static void main(String[] args) throws IOException, ApiException, InterruptedException {

        createTenants(rawDataList);
        createAPIsForTenants(rawDataList);
        publishAPIs(rawDataList);

        ArrayList<String> apiFourVisibleTenants = new ArrayList<>();
        apiFourVisibleTenants.add("finance.abc.com");
        apiFourVisibleTenants.add("core.abc.com");
        ArrayList<String> apiFourTags = new ArrayList<>();
        apiFourTags.add("employee");
        String apiId = SampleUtils
                .createApi("Employee_info_API", "1.0.0", "/empInfo", new ArrayList<>(), apiFourVisibleTenants,
                        API.SubscriptionAvailabilityEnum.SPECIFIC_TENANTS, hostname, port, apiFourTags);
        SampleUtils.publishAPI(apiId);
    }

    /**
     * This method is used to create tenants
     */
    private static void createTenants(List<RawDataBean> rawDataBeans) {
        for (RawDataBean rawDataBean : rawDataBeans) {
            TenantBean tenant = rawDataBean.getTenant();
            TenantUtils.createTenant(tenant.getAdminUser(), tenant.getAdminPwd(), tenant.getDomain(),
                    tenant.getAdminFirstName(), tenant.getAdminLastName(), serviceEndpoint);
            System.out.println(tenant.getDomain() + " added successfully");
        }
    }

    /**
     * This method is used to create API's for tenants.
     *
     * @param rawDataBeans  API related raw data.
     * @return A list of API Id's that was created.
     * @throws ApiException throws if an exception is thrown when API creation.
     * @throws IOException  throws when if an error occurred when reading the api definition file.
     */
    private static void createAPIsForTenants(List<RawDataBean> rawDataBeans)
            throws ApiException, IOException, InterruptedException {

        for (RawDataBean rawDataBean : rawDataBeans) {
            ApiBean api = rawDataBean.getApi();
            TenantBean tenant = rawDataBean.getTenant();
            String apiId = SampleUtils.createApiForTenant(api.getApiName(), api.getApiVersion(), api.getApiContext(),
                    API.VisibilityEnum.PUBLIC, new ArrayList<>(), new ArrayList<>(),
                    API.SubscriptionAvailabilityEnum.CURRENT_TENANT, hostname, port, api.getTagList(),
                    tenant.getDomain(), tenant.getAdminUser(), tenant.getAdminPwd());
            api.setApiId(apiId);
            rawDataBean.setApi(api);
            rawDataBeans.set(rawDataBean.getId(), rawDataBean);
            System.out.println("Waiting the tenant " + tenant.getDomain() + " to be loaded...");
            Thread.sleep(20000);
            System.out.println("API " + api.getApiName() + "-" + api.getApiVersion() + "created successfully.");
        }
        rawDataList = rawDataBeans;
    }

    /**
     * This method is used to publish created API's.
     *
     * @throws ApiException throws if an exception is thrown when publishing an API.
     */
    private static void publishAPIs(List<RawDataBean> rawDataBeans) throws ApiException {

        for (RawDataBean rawDataBean : rawDataBeans) {
            ApiBean api = rawDataBean.getApi();
            TenantBean tenant = rawDataBean.getTenant();
            SampleUtils.publishAPI(api.getApiId(), tenant.getDomain(), tenant.getAdminUser(), tenant.getAdminPwd());
            System.out.println("API " + api.getApiName() + "-" + api.getApiVersion() + " published to store.");
        }

    }

    private static List<RawDataBean> createRawDataList() {
        TenantBean tenant1 = new TenantBean();
        tenant1.setDomain("finance.abc.com");
        tenant1.setAdminUser("john");
        tenant1.setAdminPwd("123123");
        tenant1.setAdminFirstName("John");
        tenant1.setAdminLastName("Smith");

        TenantBean tenant2 = new TenantBean();
        tenant2.setDomain("core.abc.com");
        tenant2.setAdminUser("tom");
        tenant2.setAdminPwd("123123");
        tenant2.setAdminFirstName("Tom");
        tenant2.setAdminLastName("Smith");

        TenantBean tenant3 = new TenantBean();
        tenant3.setDomain("operations.abc.com");
        tenant3.setAdminUser("bob");
        tenant3.setAdminPwd("123123");
        tenant3.setAdminFirstName("Bob");
        tenant3.setAdminLastName("Len");

        ApiBean api1 = new ApiBean();
        api1.setApiName("Salary_details_API");
        api1.setApiVersion("1.0.0");
        api1.setApiContext("/t/" + tenant1.getDomain() + "/salaries");
        ArrayList tagList1 = new ArrayList();
        tagList1.add("finance");
        api1.setTagList(tagList1);

        ApiBean api2 = new ApiBean();
        api2.setApiName("Mobile_stock_API");
        api2.setApiVersion("1.0.0");
        api2.setApiContext("/t/" + tenant2.getDomain() + "/stocks");
        ArrayList tagList2 = new ArrayList();
        tagList2.add("stock");
        api2.setTagList(tagList2);

        ApiBean api3 = new ApiBean();
        api3.setApiName("Maintenance_ask_API");
        api3.setApiVersion("1.0.0");
        api3.setApiContext("/t/" + tenant3.getDomain() + "/tasks");
        ArrayList tagList3 = new ArrayList();
        tagList3.add("maintenance");
        api3.setTagList(tagList3);

        RawDataBean rawDataBean1 = new RawDataBean();
        rawDataBean1.setId(0);
        rawDataBean1.setApi(api1);
        rawDataBean1.setTenant(tenant1);

        RawDataBean rawDataBean2 = new RawDataBean();
        rawDataBean2.setId(1);
        rawDataBean2.setApi(api2);
        rawDataBean2.setTenant(tenant2);

        RawDataBean rawDataBean3 = new RawDataBean();
        rawDataBean3.setId(2);
        rawDataBean3.setApi(api3);
        rawDataBean3.setTenant(tenant3);

        List<RawDataBean> rawDataBeanList = new ArrayList<>();
        rawDataBeanList.add(rawDataBean1);
        rawDataBeanList.add(rawDataBean2);
        rawDataBeanList.add(rawDataBean3);
        return rawDataBeanList;
    }
}
