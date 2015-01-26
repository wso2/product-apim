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

import org.apache.catalina.connector.Request;

import javax.servlet.http.HttpSession;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;


public class SimpleHttpRequest extends Request {

    private static int _remotePort_s = 0;

    private String _requestURI = null;
    private String _queryString = null;
    private String _remoteUser = null;
    private String _method = "GET";
    private String _remoteHost = null;
    private String _remoteAddr = null;
    private int _remotePort = -1;
    private Map<String, String> _headers = new HashMap<String, String>();
    private Map<String, String> _parameters = new HashMap<String, String>();
    private byte[] _content = null;
    private HttpSession _session = new SimpleHttpSession();
    private Principal _principal = null;

    public SimpleHttpRequest() {
        super();
        _remotePort = nextRemotePort();
    }

    public synchronized static int nextRemotePort() {
        return ++_remotePort_s;
    }

    public synchronized static void resetRemotePort() {
        _remotePort_s = 0;
    }

    //@Override
    public void addHeader(String headerName, String headerValue) {
        _headers.put(headerName, headerValue);
    }

    @Override
    public String getHeader(String headerName) {
        return _headers.get(headerName);
    }

    @Override
    public String getMethod() {
        return _method;
    }

    @Override
    public int getContentLength() {
        return _content == null ? -1 : _content.length;
    }

    @Override
    public int getRemotePort() {
        return _remotePort;
    }

   // @Override
    public void setMethod(String methodName) {
        _method = methodName;
    }

   // @Override
    public void setContentLength(int length) {
        _content = new byte[length];
    }

    public void setRemoteUser(String username) {
        _remoteUser = username;
    }

    @Override
    public String getRemoteUser() {
        return _remoteUser;
    }

    @Override
    public HttpSession getSession() {
        return _session;
    }

    @Override
    public HttpSession getSession(boolean create) {
        if (_session == null && create) {
            _session = new SimpleHttpSession();
        }
        return _session;
    }

    @Override
    public String getQueryString() {
        return _queryString;
    }

    //@Override
    public void setQueryString(String queryString) {
        _queryString = queryString;
        if (_queryString != null) {
            for (String eachParameter : _queryString.split("[&]")) {
                String[] pair = eachParameter.split("=");
                String value = (pair.length == 2) ? pair[1] : "";
                addParameter(pair[0], value);
            }
        }
    }

    //@Override
    public void setRequestURI(String uri) {
        _requestURI = uri;
    }

    @Override
    public String getRequestURI() {
        return _requestURI;
    }

    @Override
    public String getParameter(String parameterName) {
        return _parameters.get(parameterName);
    }

    public void addParameter(String parameterName, String parameterValue) {
        _parameters.put(parameterName, parameterValue);
    }

    @Override
    public String getRemoteHost() {
        return _remoteHost;
    }

    @Override
    public void setRemoteHost(String remoteHost) {
        _remoteHost = remoteHost;
    }

    @Override
    public String getRemoteAddr() {
        return _remoteAddr;
    }

    @Override
    public void setRemoteAddr(String remoteAddr) {
        _remoteAddr = remoteAddr;
    }

    @Override
    public Principal getUserPrincipal() {
        return _principal;
    }

    @Override
    public void setUserPrincipal(Principal principal) {
        _principal = principal;
    }
}
