/*
*Copyright (c) 2005-2013, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.carbon.grant.ntlm.utils;

import org.apache.catalina.connector.Response;

import java.util.*;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author dblock[at]dblock[dot]org
 */
public class SimpleHttpResponse extends Response {
    private int _status = 500;
    private Map<String, List<String>> _headers = new HashMap<String, List<String>>();

    @Override
    public int getStatus() {
        return _status;
    }

    @Override
    public void addHeader(String headerName, String headerValue) {
        List<String> current = _headers.get(headerName);
        if (current == null)
            current = new ArrayList<String>();
        current.add(headerValue);
        _headers.put(headerName, current);
    }

    @Override
    public void setHeader(String headerName, String headerValue) {
        List<String> current = _headers.get(headerName);
        if (current == null) {
            current = new ArrayList<String>();
        } else {
            current.clear();
        }
        current.add(headerValue);
        _headers.put(headerName, current);
    }

    @Override
    public void setStatus(int value) {
        _status = value;
    }

    public String getStatusString() {
        if (_status == 401) {
            return "Unauthorized";
        }
        return "Unknown";
    }

    @Override
    public void flushBuffer() {
        System.out.println(_status + " " + getStatusString());
        for (String header : _headers.keySet()) {
            for (String headerValue : _headers.get(header)) {
                System.out.println(header + ": " + headerValue);
            }
        }
    }

    //@Override
    public String[] getHeaderValues(String headerName) {
        List<String> headerValues = _headers.get(headerName);
        return headerValues == null ? null : headerValues
                .toArray(new String[0]);
    }

    @Override
    public String getHeader(String headerName) {
        List<String> headerValues = _headers.get(headerName);
        if (headerValues == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String headerValue : headerValues) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(headerValue);
        }
        return sb.toString();
    }

    @Override
    public Collection<String> getHeaderNames() {
        return _headers.keySet();
    }

    @Override
    public void sendError(int rc, String message) {
        _status = rc;
    }

    @Override
    public void sendError(int rc) {
        _status = rc;
    }
}
