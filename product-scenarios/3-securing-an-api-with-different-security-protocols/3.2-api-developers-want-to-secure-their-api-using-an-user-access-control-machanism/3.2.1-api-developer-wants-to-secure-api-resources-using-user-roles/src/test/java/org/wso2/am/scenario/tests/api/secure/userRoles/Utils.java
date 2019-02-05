/*
 *Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.scenario.tests.api.secure.userRoles;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Utils {

    /*
     * This method is used to read a file (OAS doc)
     * @param fileName name of the file
     * @Return file content as a string
     * */

    public static String readFromFile(String file_name) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file_name));
        StringBuilder sb = new StringBuilder();
        int x;
        while ((x = br.read()) != -1) {
            sb.append((char) x);
        }
        String payloadText = sb.toString();
        return payloadText;
    }
}
