/*
 *
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package apim.restful.importexport;

/**
 * This is the class to represent APIImportException. This exception is used to indicate the
 * exceptions that might be occurred during API import process.
 */
public class APIImportException extends Exception {

    String errorDescription;

    public APIImportException(String errorMessage) {
        this.errorDescription = errorMessage;
    }

    public APIImportException(String msg, Throwable e) {
        super(msg, e);
    }

    /**
     * This method returns the error description to the caller.
     *
     * @return errorDescription a string which contains the error
     */
    public String getErrorDescription() {
        return this.errorDescription;
    }
}
