package org.wso2.carbon.apimgt.importexport.template;

import java.util.Map;

public interface TemplateManager {

    public Map<String, Object> getProperties(Map<String, String> headers) throws TemplateManagerException;

}
