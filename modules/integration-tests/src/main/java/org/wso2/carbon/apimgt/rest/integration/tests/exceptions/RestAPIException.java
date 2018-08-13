/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.apimgt.rest.integration.tests.exceptions;

import org.wso2.carbon.apimgt.rest.integration.tests.util.Error;

/**
 * This Class For Handle Feign Errors from Feign Client
 */
public class RestAPIException extends Exception {

    private int code;
    private String reason;
    private Error error;

    public RestAPIException(int code, String reason, Error error) {
        this.code = code;
        this.reason = reason;
        this.error = error;
    }

    public RestAPIException(String message, int code, String reason) {
        super(message);
        this.code = code;
        this.reason = reason;
    }

    public RestAPIException(String message, Throwable cause, int code, String reason) {
        super(message, cause);
        this.code = code;
        this.reason = reason;
    }

    public RestAPIException(Throwable cause, int code, String reason) {
        super(cause);
        this.code = code;
        this.reason = reason;
    }

    public RestAPIException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace,
                            int code, String reason) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.code = code;
        this.reason = reason;
    }

    public int getCode() {
        return code;
    }

    public String getReason() {
        return reason;
    }

    public Error getError() {
        return error;
    }
}

