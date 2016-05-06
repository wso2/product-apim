/*
 * Copyright (c) WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

/**
 * The api namespace exposes methods to retrieve information individual states of the lifecycles
 * deployed to the Governance Registry
 * @namespace
 * @example
 *     var api = require('lifecycle').api;
 *     var superTenantId=-1234;
 *
 *     api.getLifecycleList(superTenantId);
 * @requires store
 * @requires event
 * @requires utils
 * @requires Packages.org.wso2.carbon.governance.lcm.util.CommonUtil
 */
var api = {};
(function (api, core) {
    var log = new Log('lifecycle');    

    /**
     * Represents a class which models a lifecycle
     * @constructor
     * @param {Object} definiton The JSON definition of a lifecycle
     * @memberOf api
     */
    function Lifecycle(definiton) {
        this.definition = definiton;
    }

    /**
     * Returns the JSON definition for the lifecycle managed by the instance
     * @return {Object} Lifecycle definition
     */
    Lifecycle.prototype.getDefinition = function () {
        return this.definition;
    };

    /**
     * Returns the name of the lifecycle
     * @return {String} The name of the lifecycle
     */
    Lifecycle.prototype.getName = function () {
        if (!this.definition.name) {
            throw 'Unable to locate name attribute in the lifecycle definition ';
        }
        return this.definition.name;
    };

   
    /**
     * Returns an instance of the Lifecycle class
     * @example
     *     var lc = api.getLifecycle('SimpleLifeCycle',-1234);
     *     lc.nextStates('initial');
     * @param  {String} lifecycleName The name of the lifecycle
     * @param  {Number} tenantDomain       The tenant ID
     * @return {Object}                An instance of the Lifecycle class
     * @throws Unable to locate lifecycle without a tenant ID
     */
    api.getLifecycle = function (tenantDomain, APIProvider) {
        if (!tenantDomain) {
            throw 'Unable to locate lifecycle without a tenantDomain';
        }
        var lcJSON = core.getJSONDef(tenantDomain,APIProvider);

        if (!lcJSON) {
            log.warn('Unable to locate lifecycle  for the tenant: ' + tenantDomain);
            return null; //TODO: This should throw an exception
        }
        return new Lifecycle(lcJSON);
    };

    
}(api, core));
