/**
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.rest.integration.tests.util;

import com.google.gson.Gson;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.wso2.carbon.apimgt.rest.integration.tests.exceptions.RestAPIException;

import java.io.IOException;

/**
 * Error Decoder for Feign API Client to handle Errors
 */
public class RestAPIErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String s, Response response) {
        Error error = null;
        if (response.status() >= 400 && response.status() <= 499) {
            try {
                error = new Gson().fromJson(response.body().asReader(), Error.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new RestAPIException(response.status(), response.reason(), error);
        }
        if (response.status() >= 500 && response.status() <= 599) {
            try {
                error = new Gson().fromJson(response.body().asReader(), Error.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new RestAPIException(response.status(), response.reason(), error);

        }
        return new ErrorDecoder.Default().decode(s, response);
    }
}

