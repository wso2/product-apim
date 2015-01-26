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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;


public class ApiDAO {
	
	public static HashMap<String,String> getURITemplatesPerAPIAsString(APIIdentifier identifier, Connection conn)
            throws APIManagementException {
        ResultSet resultSet = null;
        PreparedStatement ps = null;
        int apiId = -1;
        HashMap<String,String> urlMappings = new LinkedHashMap<String, String>();

        try {
            apiId = getAPIID(identifier,conn);
            conn = DBUtils.getConnection();
            String sqlQuery =
                    "SELECT " +
                            "URL_PATTERN" +
                            ",HTTP_METHOD" +
                            ",AUTH_SCHEME " +
                    "FROM " +
                            "AM_API_URL_MAPPING " +
                    "WHERE " +
                            "API_ID = ? " +
                    "ORDER BY " +
                            "URL_MAPPING_ID ASC ";


            ps = conn.prepareStatement(sqlQuery);
            ps.setInt(1, apiId);
            resultSet = ps.executeQuery();
            while (resultSet.next()) {
                String uriPattern = resultSet.getString("URL_PATTERN");
                String httpMethod = resultSet.getString("HTTP_METHOD");
                String authScheme = resultSet.getString("AUTH_SCHEME");
                urlMappings.put(uriPattern + "::" + httpMethod + "::" + authScheme,null);
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
			e.printStackTrace();
                	throw new APIManagementException(e1);
                }
            }
            e.printStackTrace();
        } finally {
        	DBUtils.closeAllConnections(ps, conn, resultSet);
        }
        return urlMappings;
    }

    public static int getAPIID(APIIdentifier apiId, Connection connection)
            throws APIManagementException, SQLException {
        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        int id = -1;
        String getAPIQuery = "SELECT API_ID " +
                " FROM AM_API" +
                " WHERE API_PROVIDER = ?" +
                " AND API_NAME = ?" +
                " AND API_VERSION = ?";

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
            throw e;
        } finally {
            DBUtils.closeAllConnections(prepStmt, connection, rs);
        }
        return id;
    }

    /*public static void addComment(Connection connection, long commentId, String commentText, String user, Date createdTime, int apiId) {
        if (apiId == -1) {
            //application addition has failed
            return;
        }
        PreparedStatement prepStmt = null;
        String addCommentQuery = "INSERT " +
                " INTO AM_API_COMMENTS (COMMENT_ID,COMMENT_TEXT,COMMENTED_USER,DATE_COMMENTED,API_ID)" +
                " VALUES (?,?,?,?,?)";
        try {
            Adding data to the AM_API_COMMENTS table
            prepStmt = connection.prepareStatement(addCommentQuery);
            prepStmt.setLong(1, commentId);
            prepStmt.setString(2, commentText);
            prepStmt.setString(3, user);
            Calendar c1 = Calendar.getInstance();
            c1.setTime(createdTime);
            prepStmt.setTimestamp(4, new Timestamp(c1.getTimeInMillis()), Calendar.getInstance());
            prepStmt.setInt(5, apiId);

            prepStmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error while adding comment to the database");
            e.printStackTrace();
        } finally {
            DBUtils.closeAllConnections(prepStmt, connection, null);
        }
    }

    public static void addRating(int apiId, Connection connection, int rating, int subscriberId) {
        if (apiId == -1) {
            //application addition has failed
            return;
        }
        PreparedStatement prepStmtSelect = null;
        PreparedStatement prepStmtAdd = null;
        ResultSet rs = null;

        boolean userRatingExists = false;
        //This query to check the ratings already exists for the user in the AM_API_RATINGS table
        String selectRatingQuery = "SELECT " +
                "RATING FROM AM_API_RATINGS " +
                " WHERE API_ID= ? AND SUBSCRIBER_ID=? ";

        String addRatingQuery = "INSERT " +
                " INTO AM_API_RATINGS (API_ID, RATING, SUBSCRIBER_ID)" +
                " VALUES (?,?,?)";
        try {
            prepStmtSelect = connection.prepareStatement(selectRatingQuery);
            prepStmtSelect.setInt(1, apiId);
            prepStmtSelect.setInt(2, subscriberId);
            rs = prepStmtSelect.executeQuery();
            while (rs.next()) {
                userRatingExists = true;
            }

            Adding data to the AM_API_COMMENTS table
            if (!userRatingExists) {
                prepStmtAdd = connection.prepareStatement(addRatingQuery);
                prepStmtAdd.setInt(1, apiId);
                prepStmtAdd.setInt(2, rating);
                prepStmtAdd.setInt(3, subscriberId);

                prepStmtAdd.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Error while adding rating to the database");
            e.printStackTrace();
        } finally {
            DBUtils.closeAllConnections(prepStmtSelect, connection, rs);
            DBUtils.closeAllConnections(prepStmtAdd, connection, null);
        }
    }

    public static Set<Subscriber> getAllSubscribers(Connection connection) {
        Set<Subscriber> subscribers = new HashSet<Subscriber>();
        PreparedStatement prepStmt = null;
        ResultSet result = null;/TestAPI-1.0.0/api-doc.json

        try {
            String sqlQuery = "SELECT DISTINCT " +
                    " SUBSCRIBER_ID, USER_ID, DATE_SUBSCRIBED " +
                    " FROM AM_SUBSCRIBER";

            prepStmt = connection.prepareStatement(sqlQuery);
            result = prepStmt.executeQuery();
            if (result == null) {
                return subscribers;
            }
            while (result.next()) {
                Subscriber subscriber =
                        new Subscriber(result.getString(APIConstants.SUBSCRIBER_FIELD_USER_ID));
                subscriber.setSubscribedDate(
                        result.getTimestamp(APIConstants.SUBSCRIBER_FIELD_DATE_SUBSCRIBED));
                subscriber.setId(result.getInt(APIConstants.APPLICATION_SUBSCRIBER_ID));
                subscribers.add(subscriber);
            }
        } catch (SQLException e) {
            System.out.println("Error while getting subscribers from database");
            e.printStackTrace();
        } finally {
            DBUtils.closeAllConnections(prepStmt, connection, result);
        }
        return subscribers;
    }*/
}

