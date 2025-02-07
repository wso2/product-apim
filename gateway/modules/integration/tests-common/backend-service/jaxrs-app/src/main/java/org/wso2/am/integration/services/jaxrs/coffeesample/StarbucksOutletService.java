/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.am.integration.services.jaxrs.coffeesample;

import org.wso2.am.integration.services.jaxrs.coffeesample.bean.Order;
import org.wso2.am.integration.services.jaxrs.coffeesample.bean.Payment;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public interface StarbucksOutletService {

    @POST
    @Path("/orders/")
//    @Produces(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)   // produces application/json
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})   // consumes text/xml
    public Response addOrder(Order orderBean);

    /**
     * Read the following article on ATOM data binding in CXF
     * http://goo.gl/UKJdM
     * @param id order id
     * @return the order
     */
    @GET
    @Path("/orders/{orderId}")
    @Produces( { "application/json", "application/xml" } )  // produces atom and json as relevant
    public Order getOrder(@PathParam("orderId") String id);

    @PUT
    @Path("/orders/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateOrder(Order orderBean);

    @GET
    @Path("/orders/pending/")
    @Produces( { "application/json", "application/atom+xml;type=feed", "application/xml" } )   // application/atom+xml and json
    public Response getPendingOrders(); //todo add a atom feader
    
    @PUT
    @Path("/orders/lock/{orderId}/")
    @Produces({MediaType.APPLICATION_XML})     // application/xml
        public Response lockOrder(@PathParam("orderId") String id);
    
    @DELETE
    @Path("/orders/{orderId}/")
    @Produces({MediaType.TEXT_PLAIN})
    public Response removeOrder(@PathParam("orderId") String id);

    @POST
    @Path("/payment/{orderId}/")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_HTML} )
    public Response doPayment(@PathParam("orderId") String id, Payment payment);
    
    @GET    
    @Path("/payment/{orderId}/")
    @Produces(MediaType.APPLICATION_JSON)
    public Payment getPayment(@PathParam("orderId") String id);

}
