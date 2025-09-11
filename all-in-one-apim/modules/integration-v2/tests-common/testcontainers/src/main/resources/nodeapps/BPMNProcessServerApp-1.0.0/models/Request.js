class Request {
    constructor(tenantId, processDefinitionKey, businessKey, variables = []) {
        this.tenantId = tenantId;
        this.processDefinitionKey = processDefinitionKey;
        this.businessKey = businessKey;
        this.variables = variables;
    }
}

module.exports = Request;
