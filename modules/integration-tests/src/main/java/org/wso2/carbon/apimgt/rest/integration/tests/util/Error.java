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

import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * DTO class for Error
 */
@ApiModel(description = "")
public class Error {
    @ApiModelProperty(required = true, value = "")
    @SerializedName("code")
    private Long code = null;

    @ApiModelProperty(required = true, value = "Error message.")
    @SerializedName("message")
    private String message = null;

    @ApiModelProperty(value = "A detail description about the error message.")
    @SerializedName("description")
    private String description = null;

    @ApiModelProperty(value = "Preferably an url with more details about the error.")
    @SerializedName("moreInfo")
    private Map<String, String> paramList = null;

    @ApiModelProperty(value = "If there are more than one error list them out. Ex. list out validation errors "
            + "by each field.")
    @SerializedName("error")
    private List<ErrorListItem> error = new ArrayList<ErrorListItem>();

    /**
     * Method to get the error code
     *
     * @return error code.
     **/
    public Long getCode() {
        return code;
    }

    public void setCode(Long code) {
        this.code = code;
    }

    /**
     * Method th get the error message.
     *
     * @return error message.
     */
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * A detail description about the error message.
     *
     * @return error description.
     */

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Preferably an url with more details about the error.
     *
     * @return map of parameters specific to the error.
     */

    public Map<String, String> getMoreInfo() {
        return paramList;
    }

    public void setMoreInfo(Map<String, String> moreInfo) {
        this.paramList = moreInfo;
    }

    /**
     * If there are more than one error list them out. Ex. list out validation errors by each field.
     *
     * @return {@code List<ErrorListItemDTO>}   List of error objects.
     */
    public List<ErrorListItem> getError() {
        return error;
    }

    public void setError(List<ErrorListItem> error) {
        this.error = error;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ErrorDTO {\n");

        sb.append("  code: ").append(code).append("\n");
        sb.append("  message: ").append(message).append("\n");
        sb.append("  description: ").append(description).append("\n");
        sb.append("  moreInfo: ").append(paramList).append("\n");
        sb.append("  error: ").append(error).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}