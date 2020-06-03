/*
 *Copyright (c) 2005-2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */
package org.wso2.am.apiMonitorService;

import org.wso2.carbon.apimgt.api.model.KeyManager;
import org.wso2.carbon.apimgt.impl.factory.KeyManagerHolder;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/keyManagerInformation/")
public class KeyManagerInformationService {

    public KeyManagerInformationService() {

    }

    @Path("/{tenantDomain}/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    /**
     * Provide the  status of key manager in tenant.
     */
    public Response get(@PathParam("tenantDomain") String tenantDomain, @PathParam("name") String name) {

        KeyManager keyManagerInstance = KeyManagerHolder.getKeyManagerInstance(tenantDomain, name);
        if (keyManagerInstance != null) {
            return Response.ok().entity("ok").build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity("not_found").build();
        }
    }

}
