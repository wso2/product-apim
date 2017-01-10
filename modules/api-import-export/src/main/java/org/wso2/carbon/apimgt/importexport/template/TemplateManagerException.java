package org.wso2.carbon.apimgt.importexport.template;

public class TemplateManagerException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -4542002388160480140L;

    private String errorDescription;

    public TemplateManagerException(String errorMessage) {
        this.errorDescription = errorMessage;
    }

    public TemplateManagerException(String msg, Throwable e) {
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
