
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
 * The core namespace contains methods that load the lifecycle definitions from the registry
 * @namespace
 * @example
 *     var core = require('lifecycle').core;
 *     core.init(); //Should only be called once in the lifecycle of an app.Ideally in an init script
 * @requires store
 * @requires event
 * @requires utils
 * @requires Packages.org.wso2.carbon.governance.lcm.util.CommonUtil
 */
var core = {};
(function(core) {
    var CommonUtil = Packages.org.wso2.carbon.governance.lcm.util.CommonUtil;
    var LC_MAP = 'lc.map';
    var EMPTY = '';
    var log = new Log('lifecycle');
    var addRawLifecycle = function(lifecycleName, content, tenantId) {
        var lcMap = core.configs(tenantId);
        if (!lcMap.raw) {
            lcMap.raw = {};
        }
        lcMap.raw[lifecycleName] = new String(content);
    };
    var addJsonLifecycle = function(lifecycleName, definition, tenantId) {
        var lcMap = core.configs(tenantId);
        if (!lcMap.json) {
            lcMap.json = {};
        }
        lcMap.json[lifecycleName] = definition;
    };

    /**
     * Converts array references to properties.The JSON conversion produces some properties which need to be accessed
     * using array indexes.
     * @param  {Object} obj  Unaltered JSON object
     * @return {Object}      JSON object with resolved array references
     */
    var transformJSONLifecycle = function(obj) {
        obj.configuration = obj.configuration[0];
        obj.configuration.lifecycle = obj.configuration.lifecycle[0];
        obj.configuration.lifecycle.scxml = obj.configuration.lifecycle.scxml[0];
        var states = obj.configuration.lifecycle.scxml.state;
        var stateObj = {};
        var state;
        for (var index = 0; index < states.length; index++) {
            state = states[index];
            stateObj[state.id.toLowerCase()] = state;
            if (stateObj[state.id.toLowerCase()].datamodel) {
                stateObj[state.id.toLowerCase()].datamodel = stateObj[state.id.toLowerCase()].datamodel[0];
            }
        }
        obj.configuration.lifecycle.scxml.state = stateObj;
        return obj;
    };
    /*
     Creates an xml file from the contents of an Rxt file
     @rxtFile: An rxt file
     @return: An xml file
     */
    var createXml = function(content) {
        var fixedContent = content.replace('<xml version="1.0"?>', EMPTY).replace('</xml>', EMPTY);
        return new XML(fixedContent);
    };
    var parseLifeycle = function(content) {
        var ref = require('utils').xml;
        var obj = ref.convertE4XtoJSON(createXml(content));
        return obj;
    };
    var loadLifecycles = function(tenantDomain,APIProvider) {

        //Obtain the definition
        content = APIProvider.getLifecycleConfiguration(tenantDomain);
        //Store the raw lifecycle
        addRawLifecycle("APILifeCycle", content, tenantDomain);
        //Parse the raw lifecycle definition into a json
        var jsonLifecycle = parseLifeycle(new String(content));
        //Correct any array references
        jsonLifecycle = transformJSONLifecycle(jsonLifecycle);
        //Store the json lifecycle definition
        addJsonLifecycle("APILifeCycle", jsonLifecycle, tenantDomain);
        if(log.isDebugEnabled()){
            log.debug('Found lifecycle: ' + jsonLifecycle.name + ' tenant: ' + tenantDomain);
        }

    };
    var init = function(tenantDomain,APIProvider) {
        loadLifecycles(tenantDomain,APIProvider);
    };
    core.force = function(tenantDomain,APIProvider) {
        init(tenantDomain,APIProvider);
    };

    /**
     * Returns the lifecycle map which is stored in the application context
     * The map is maintained on a per user basis
     * @param  {Number} tenantId  The tenant ID
     * @return {Object}           The lifecycle map
     */
    core.configs = function(tenantDomain) {        
        var lcMap = application.get(LC_MAP);
        if (!lcMap) {
            log.debug('Creating lcMap in the application context');
            lcMap = {};
            application.put(LC_MAP, lcMap);            
        }
        if (!lcMap[tenantDomain]) {
            log.debug('Creating lcMap for the tenant: ' + tenantDomain + ' in application context');
            lcMap[tenantDomain] = {};
        }
        return lcMap[tenantDomain];
    };

    /**
     * Returns the JSON definition of the provided lifecycle for the given tenant
     * @param  {Number} tenantDomain      The tenant Domain
     * @return {Object}                The JSON definitin of the lifecycle
     * @throws There is no lifcycle information for the tenant
     * @throws There is no json lifecycle information for the lifecycle of the tenant
     */
    core.getJSONDef = function(tenantDomain,APIProvider) {
        var lifecycleName='APILifeCycle';
        var lcMap = core.configs(tenantDomain);
        if (!lcMap) {
            throw 'There is no lifecycle information for the tenant: ' + tenantDomain;
        }
        //if (!lcMap.json) {
          //  throw 'There is no json lifecycle information for the lifecycle of tenant: ' + tenantDomain;
        //}
        if (!lcMap.json) {
            core.force(tenantDomain,APIProvider);
            lcMap = core.configs(tenantDomain);
            if (!lcMap.json[lifecycleName]){
                throw 'There is no lifecycle information for ';
            }
        }
        return lcMap.json[lifecycleName];
    };


}(core));
