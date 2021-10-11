/*
 * WSO2 API Manager - Gateway
 * This document specifies a **RESTful API** for WSO2 **API Manager** - Gateway. Please see [full swagger definition](https://raw.githubusercontent.com/wso2/carbon-apimgt/v6.7.206/components/apimgt/org.wso2.carbon.apimgt.rest.api.gateway.v1/src/main/resources/gateway-api.yaml) of the API which is written using [swagger 2.0](http://swagger.io/) specification. 
 *
 * OpenAPI spec version: v1
 * Contact: architecture@wso2.com
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package org.wso2.am.integration.clients.gateway.api.v1.dto;

import java.util.Objects;
import java.util.Arrays;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * EndpointsDTO
 */

public class EndpointsDTO {
  @SerializedName("deployedEndpoints")
  private List<String> deployedEndpoints = null;

  @SerializedName("notdeployedEndpoints")
  private List<String> notdeployedEndpoints = null;

  public EndpointsDTO deployedEndpoints(List<String> deployedEndpoints) {
    this.deployedEndpoints = deployedEndpoints;
    return this;
  }

  public EndpointsDTO addDeployedEndpointsItem(String deployedEndpointsItem) {
    if (this.deployedEndpoints == null) {
      this.deployedEndpoints = new ArrayList<>();
    }
    this.deployedEndpoints.add(deployedEndpointsItem);
    return this;
  }

   /**
   * The end points which has been deployed in the gateway 
   * @return deployedEndpoints
  **/
  @ApiModelProperty(value = "The end points which has been deployed in the gateway ")
  public List<String> getDeployedEndpoints() {
    return deployedEndpoints;
  }

  public void setDeployedEndpoints(List<String> deployedEndpoints) {
    this.deployedEndpoints = deployedEndpoints;
  }

  public EndpointsDTO notdeployedEndpoints(List<String> notdeployedEndpoints) {
    this.notdeployedEndpoints = notdeployedEndpoints;
    return this;
  }

  public EndpointsDTO addNotdeployedEndpointsItem(String notdeployedEndpointsItem) {
    if (this.notdeployedEndpoints == null) {
      this.notdeployedEndpoints = new ArrayList<>();
    }
    this.notdeployedEndpoints.add(notdeployedEndpointsItem);
    return this;
  }

   /**
   * The end points which has not been deployed in the gateway 
   * @return notdeployedEndpoints
  **/
  @ApiModelProperty(value = "The end points which has not been deployed in the gateway ")
  public List<String> getNotdeployedEndpoints() {
    return notdeployedEndpoints;
  }

  public void setNotdeployedEndpoints(List<String> notdeployedEndpoints) {
    this.notdeployedEndpoints = notdeployedEndpoints;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EndpointsDTO endpoints = (EndpointsDTO) o;
    return Objects.equals(this.deployedEndpoints, endpoints.deployedEndpoints) &&
        Objects.equals(this.notdeployedEndpoints, endpoints.notdeployedEndpoints);
  }

  @Override
  public int hashCode() {
    return Objects.hash(deployedEndpoints, notdeployedEndpoints);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EndpointsDTO {\n");
    
    sb.append("    deployedEndpoints: ").append(toIndentedString(deployedEndpoints)).append("\n");
    sb.append("    notdeployedEndpoints: ").append(toIndentedString(notdeployedEndpoints)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}
