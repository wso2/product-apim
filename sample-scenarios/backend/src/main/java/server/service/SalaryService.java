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

import server.obj.Salary;

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

@Path("/salaryservice/") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON) public class SalaryService {

    Map<String, Salary> salaryMap = new HashMap<>();

    public void init() {

        Salary employeeSalaryOne = new Salary();
        employeeSalaryOne.setId(1);
        employeeSalaryOne.setFexed(8000);
        employeeSalaryOne.setAllowance(3000);
        employeeSalaryOne.setEmpId("1");

        Salary employeeSalaryTwo = new Salary();
        employeeSalaryTwo.setId(2);
        employeeSalaryTwo.setFexed(10000);
        employeeSalaryTwo.setAllowance(5000);
        employeeSalaryTwo.setEmpId("2");

        salaryMap.put("1", employeeSalaryTwo);
        salaryMap.put("2", employeeSalaryTwo);

    }

    public SalaryService() {
        init();
    }

    @GET @Path("/salary/{id}/") public Salary getEmployeeSalary(@PathParam("id") String id,
            @Context HttpHeaders headers) {
        Salary salary = salaryMap.get(id);
        return salary;
    }
}