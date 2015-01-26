/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.integration.services.jaxrs.peoplesample.bean;

public class Person {
    private String email;
    private String firstName;
    private String lastName;

    public Person() {
    }

    public Person( final String email ) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail( final String email ) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setFirstName( final String firstName ) {
        this.firstName = firstName;
    }

    public void setLastName( final String lastName ) {
        this.lastName = lastName;
    }
}