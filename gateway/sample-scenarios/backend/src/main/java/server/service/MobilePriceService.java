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

import server.obj.Mobile;

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

@Path("/mobilepriceservice/") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON) public class MobilePriceService {

    Map<String, Mobile> mobileMap = new HashMap<>();

    public void init() {

        Mobile mobileOne = new Mobile();
        mobileOne.setId(1);
        mobileOne.setBrand("iPhone");
        mobileOne.setModel("6");
        mobileOne.setPrice(900);

        Mobile mobileTwo = new Mobile();
        mobileTwo.setId(2);
        mobileTwo.setBrand("iPhone");
        mobileTwo.setModel("6s");
        mobileTwo.setPrice(1000);

        Mobile mobileThree = new Mobile();
        mobileThree.setId(2);
        mobileThree.setBrand("iPhone");
        mobileThree.setModel("6s plus");
        mobileThree.setPrice(1500);

        mobileMap.put("1", mobileOne);
        mobileMap.put("2", mobileTwo);
        mobileMap.put("3", mobileThree);

    }

    public MobilePriceService() {
        init();
    }

    @GET @Path("/mobile/{id}/") public Mobile getMobile(@PathParam("id") String id, @Context HttpHeaders headers) {
        Mobile mobile = mobileMap.get(id);
        return mobile;
    }
}