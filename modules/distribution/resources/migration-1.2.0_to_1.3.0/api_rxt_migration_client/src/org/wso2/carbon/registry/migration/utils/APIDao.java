/*
*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.registry.migration.utils;

import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.api.model.URITemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Set;


public class APIDao {

        public static int getAPIID(APIIdentifier apiId, Connection connection)
            throws APIManagementException {
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        int id = -1;
        String getAPIQuery = "SELECT " +
                             "API.API_ID FROM AM_API API" +
                             " WHERE " +
                             "API.API_PROVIDER = ?" +
                             "AND API.API_NAME = ?" +
                             "AND API.API_VERSION = ?";

        try {
            prepStmt = connection.prepareStatement(getAPIQuery);
            prepStmt.setString(1, apiId.getProviderName());
            prepStmt.setString(2, apiId.getApiName());
            prepStmt.setString(3, apiId.getVersion());
            rs = prepStmt.executeQuery();
            if (rs.next()) {
                id = rs.getInt("API_ID");
            }
            if (id == -1) {
                String msg = "Unable to find the API: " + apiId + " in the database";
                System.out.println(msg);
                throw new APIManagementException(msg);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtils.closeAllConnections(prepStmt, null, rs);
        }
        return id;
    }


    public static void addURLTemplates(int apiId, Set<URITemplate> uriTemps, Connection connection) throws APIManagementException {
        if(apiId == -1){
            //application addition has failed
            return;
        }

        PreparedStatement prepStmt = null;
        String query = "INSERT INTO AM_API_URL_MAPPING (API_ID,HTTP_METHOD,AUTH_SCHEME,URL_PATTERN) VALUES (?,?,?,?)";
        try {
            //connection = APIMgtDBUtil.getConnection();
            prepStmt = connection.prepareStatement(query);

            Iterator<URITemplate> uriTemplateIterator = uriTemps.iterator();
            URITemplate uriTemplate;
            for(;uriTemplateIterator.hasNext();){
                uriTemplate = uriTemplateIterator.next();
                prepStmt.setInt(1,apiId);
                prepStmt.setString(2,uriTemplate.getHTTPVerb());
                prepStmt.setString(3,uriTemplate.getAuthType());
                prepStmt.setString(4,uriTemplate.getUriTemplate());
                prepStmt.addBatch();
            }
            prepStmt.executeBatch();
            prepStmt.clearBatch();


        } catch (SQLException e) {
            System.out.println("Error while adding URL template(s) to the database "+ e);
        }
    }


    public static boolean isURLMappingsExists(int apiId, Connection connection)
            throws APIManagementException {
        boolean isExists = false;

        if (apiId == -1) {
            //Avoid data migrating
            return true;
        }
        ResultSet rs = null;
        PreparedStatement prepStmt = null;

        String query = "SELECT API_ID FROM AM_API_URL_MAPPING WHERE API_ID = ?";
        try {
            prepStmt = connection.prepareStatement(query);
            prepStmt.setInt(1, apiId);

            rs = prepStmt.executeQuery();

            while (rs.next()) {
                isExists = true;
            }

        } catch (SQLException e) {
            System.out.println("Error when executing the SQL query to check the API URI Mappings has stored to database"+e);
        } finally {
            DBUtils.closeAllConnections(prepStmt, connection, rs);
        }
        return isExists;
    }
}
