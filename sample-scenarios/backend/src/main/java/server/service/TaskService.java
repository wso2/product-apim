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

import server.obj.Task;

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

@Path("/taskservice/") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON) public class TaskService {

    Map<String, Task> taskMap = new HashMap<>();

    public void init() {

        Task taskOne = new Task();
        taskOne.setId(1);
        taskOne.setName("Clean office floor area");
        taskOne.setDuration("One day");

        Task taskTwo = new Task();
        taskTwo.setId(2);
        taskOne.setName("Fill the printer cartridges");
        taskOne.setDuration("Ten minutes");

        taskMap.put("1", taskOne);
        taskMap.put("2", taskTwo);

    }

    public TaskService() {
        init();
    }

    @GET @Path("/task/{id}/") public Task getEmployee(@PathParam("id") String id, @Context HttpHeaders headers) {
        return taskMap.get(id);
    }
}