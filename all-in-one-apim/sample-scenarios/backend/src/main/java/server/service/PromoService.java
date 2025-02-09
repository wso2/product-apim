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

package server.service;

import server.obj.Promo;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

@Path("/promoservice/") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON) public class PromoService {

    Map<String, Promo> promoMap = new HashMap<>();

    public void init() {

        Promo promoOne = new Promo();
        promoOne.setId(1);
        promoOne.setName("10 % discount from 24th Nov to 30th Nov.");

        Promo promoTwo = new Promo();
        promoTwo.setId(2);
        promoTwo.setName("But a Nokia phone and get a pouch free.");

        promoMap.put("1", promoOne);
        promoMap.put("2", promoTwo);

    }

    public PromoService() {
        init();
    }

    @GET @Path("/promo/{id}/") public Promo getPromo(@PathParam("id") String id, @Context HttpHeaders headers) {
        Promo promo = promoMap.get(id);
        return promo;
    }
}