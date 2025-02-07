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

import server.obj.Pouch;

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

@Path("/pouchpriceservice/") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON) public class PouchPriceService {

    Map<String, Pouch> pouchMap = new HashMap<>();

    public void init() {

        Pouch pouchOne = new Pouch();
        pouchOne.setId(1);
        pouchOne.setBrand("iPhone");
        pouchOne.setModel("6");
        pouchOne.setPrice(90);

        Pouch pouchTwo = new Pouch();
        pouchTwo.setId(2);
        pouchTwo.setBrand("iPhone");
        pouchTwo.setModel("6s");
        pouchTwo.setPrice(100);

        Pouch pouchThree = new Pouch();
        pouchThree.setId(2);
        pouchThree.setBrand("iPhone");
        pouchThree.setModel("6s plus");
        pouchThree.setPrice(100);

        pouchMap.put("1", pouchOne);
        pouchMap.put("2", pouchTwo);
        pouchMap.put("3", pouchThree);

    }

    public PouchPriceService() {
        init();
    }

    @GET @Path("/pouch/{id}/") public Pouch getPouch(@PathParam("id") String id, @Context HttpHeaders headers) {
        Pouch pouch = pouchMap.get(id);
        return pouch;
    }
}