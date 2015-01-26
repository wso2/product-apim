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


import javax.servlet.*;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import java.util.Enumeration;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.descriptor.JspConfigDescriptor;


public class SimpleServletContext implements ServletContext {

    @Override
    public Object getAttribute(String arg0) {
        return null;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return null;
    }

    @Override
    public ServletContext getContext(String arg0) {
        return null;
    }

    @Override
    public String getContextPath() {
        return null;
    }

    @Override
    public String getInitParameter(String arg0) {
        return null;
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return null;
    }

    @Override
    public boolean setInitParameter(String s, String s2) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public String getMimeType(String arg0) {
        return null;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getEffectiveMinorVersion() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        return null;
    }

    @Override
    public String getRealPath(String arg0) {
        return null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String url) {
        return new SimpleRequestDispatcher(url);
    }

    @Override
    public URL getResource(String arg0) throws MalformedURLException {
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String arg0) {
        return null;
    }

    @Override
    public Set<String> getResourcePaths(String arg0) {
        return null;
    }

    @Override
    public String getServerInfo() {
        return null;
    }

    @Override
    public Servlet getServlet(String arg0) throws ServletException {
        return null;
    }

    @Override
    public String getServletContextName() {
        return null;
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String s, String s2) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String s, Servlet servlet) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String s, Class<? extends Servlet> aClass) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> tClass) throws ServletException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ServletRegistration getServletRegistration(String s) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String s, String s2) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String s, Filter filter) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String s, Class<? extends Filter> aClass) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> tClass) throws ServletException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public FilterRegistration getFilterRegistration(String s) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addListener(String s) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addListener(Class<? extends EventListener> aClass) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> tClass) throws ServletException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ClassLoader getClassLoader() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void declareRoles(String... strings) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Enumeration<String> getServletNames() {
        return null;
    }

    @Override
    public Enumeration<Servlet> getServlets() {
        return null;
    }

    @Override
    public void log(String arg0) {

    }

    @Override
    public void log(Exception arg0, String arg1) {

    }

    @Override
    public void log(String arg0, Throwable arg1) {

    }

    @Override
    public void removeAttribute(String arg0) {

    }

    @Override
    public void setAttribute(String arg0, Object arg1) {

    }
}
