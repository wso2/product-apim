/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.wso2.carbon.registry.core.Comment;

import java.util.Date;

public class APIComment extends Comment {
    int apiId ;
    long commentId;
    String commentText;
    String commentedUser;
    Date createdTime;


    public APIComment (int apiId, long commentId, Comment comment) {
        this.apiId = apiId;
        this.commentId = commentId;
        this.commentText = comment.getDescription();
        this.commentedUser = comment.getUser();
        this.createdTime = comment.getCreatedTime();
    }

    public long getCommentId(){
        return this.commentId;
    }

    public int getApiId() {
        return  this.apiId;
    }

    public String getCommentText() {
        return this.commentText;
    }

    public String getCommentedUser() {
        return this.commentedUser;
    }

    public Date getCreatedDate() {
        return this.createdTime;
    }

}
