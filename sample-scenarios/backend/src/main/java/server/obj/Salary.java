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

package server.obj;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Salary")
public class Salary {

    private long id;
    private long fixed;
    private long allowance;
    private String empId;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getFixed() {
        return fixed;
    }

    public void setFexed(long fixed) {
        this.fixed = fixed;
    }

    public long getAllowance() {
        return allowance;
    }

    public void setAllowance(long allowance) {
        this.allowance = allowance;
    }

    public String getEmpId() {
        return empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId;
    }


}