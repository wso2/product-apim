package org.wso2.carbon.apimgt.rest.integration.tests.admin.api;

import org.wso2.carbon.apimgt.rest.integration.tests.admin.ApiClient;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.EncodingUtils;

import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.Error;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.Workflow;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.WorkflowRequest;
import org.wso2.carbon.apimgt.rest.integration.tests.admin.model.WorkflowResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-03-27T17:24:45.778+05:30")
public interface WorkflowsIndividualApi extends ApiClient.Api {


  /**
   * Retrieve workflow information
   * This operation can be used to retrieve a workflow task.  
    * @param workflowReferenceId Workflow reference id  (required)
   * @return Workflow
   */
  @RequestLine("GET /workflows/{workflowReferenceId}")
  @Headers({
    "Accept: application/json",
  })
  Workflow workflowsWorkflowReferenceIdGet(@Param("workflowReferenceId") String workflowReferenceId);

  /**
   * Update workflow status
   * This operation can be used to approve or reject a workflow task. . 
    * @param workflowReferenceId Workflow reference id  (required)
    * @param body Workflow event that need to be updated  (required)
   * @return WorkflowResponse
   */
  @RequestLine("PUT /workflows/{workflowReferenceId}")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  WorkflowResponse workflowsWorkflowReferenceIdPut(@Param("workflowReferenceId") String workflowReferenceId,
                                                   WorkflowRequest body);
}
