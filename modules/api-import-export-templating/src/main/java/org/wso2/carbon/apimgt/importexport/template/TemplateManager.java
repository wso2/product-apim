package org.wso2.carbon.apimgt.importexport.template;

import java.util.Map;

public interface TemplateManager {
    
    public static final String WSO_HEADER_PREFIX = "X-WSO2-";
    
    public static final String WSO_HEADER_TEMPLATE_MANAGER = "X-WSO2-TEMPLATECLASS";

    public Map<String, Object> getProperties(Map<String, String> headers) throws TemplateManagerException;

}
