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


import org.apache.catalina.*;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.ApplicationServletRegistration;
import org.apache.catalina.util.CharsetMapper;
import org.apache.juli.logging.Log;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.util.http.mapper.Mapper;

import javax.management.ObjectName;
import javax.naming.directory.DirContext;
import javax.servlet.*;
import java.beans.PropertyChangeListener;
import java.io.IOException;


import java.net.URL;
import java.util.Locale;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.descriptor.JspConfigDescriptor;

import org.apache.catalina.AccessLog;
import org.apache.catalina.Cluster;
import org.apache.catalina.Container;
import org.apache.catalina.ContainerListener;
import org.apache.catalina.Context;
import org.apache.catalina.Loader;
import org.apache.catalina.Manager;
import org.apache.catalina.Pipeline;
import org.apache.catalina.Realm;
import org.apache.catalina.Wrapper;
import org.apache.catalina.deploy.ApplicationParameter;
import org.apache.catalina.deploy.ErrorPage;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.NamingResources;
import org.apache.catalina.deploy.SecurityConstraint;


public class SimpleContext implements Context {

    private Realm _realm = null;
    private ServletContext _servletContext = new SimpleServletContext();

    @Override
    public void addApplicationListener(String arg0) {

    }

    @Override
    public void addApplicationParameter(ApplicationParameter arg0) {

    }

    @Override
    public void addConstraint(SecurityConstraint arg0) {

    }

    @Override
    public void addErrorPage(ErrorPage arg0) {

    }

    @Override
    public void addFilterDef(FilterDef arg0) {

    }

    @Override
    public void addFilterMap(FilterMap arg0) {

    }

    @Override
    public void addFilterMapBefore(FilterMap filterMap) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addInstanceListener(String arg0) {

    }

    //@Override
    public void addJspMapping(String arg0) {

    }

    @Override
    public void addLocaleEncodingMappingParameter(String arg0, String arg1) {

    }

    @Override
    public void addMimeMapping(String arg0, String arg1) {

    }

    @Override
    public void addParameter(String arg0, String arg1) {

    }

    @Override
    public void addRoleMapping(String arg0, String arg1) {

    }

    @Override
    public void addSecurityRole(String arg0) {

    }

    @Override
    public void addServletMapping(String arg0, String arg1) {

    }

    @Override
    public void addServletMapping(String s, String s2, boolean b) {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    public void addTaglib(String arg0, String arg1) {

    }

    @Override
    public void addWatchedResource(String arg0) {

    }

    @Override
    public void addWelcomeFile(String arg0) {

    }

    @Override
    public void addWrapperLifecycle(String arg0) {

    }

    @Override
    public void addWrapperListener(String arg0) {

    }

    @Override
    public Wrapper createWrapper() {
        return null;
    }

    @Override
    public String[] findApplicationListeners() {
        return new String[0];
    }

    @Override
    public ApplicationParameter[] findApplicationParameters() {
        return new ApplicationParameter[0];
    }

    @Override
    public SecurityConstraint[] findConstraints() {
        return new SecurityConstraint[0];
    }

    @Override
    public ErrorPage findErrorPage(int arg0) {
        return null;
    }

    @Override
    public ErrorPage findErrorPage(String arg0) {
        return null;
    }

    @Override
    public ErrorPage[] findErrorPages() {
        return new ErrorPage[0];
    }

    @Override
    public FilterDef findFilterDef(String arg0) {
        return null;
    }

    @Override
    public FilterDef[] findFilterDefs() {
        return new FilterDef[0];
    }

    @Override
    public FilterMap[] findFilterMaps() {
        return new FilterMap[0];
    }

    @Override
    public String[] findInstanceListeners() {
        return new String[0];
    }

    @Override
    public String findMimeMapping(String arg0) {
        return null;
    }

    @Override
    public String[] findMimeMappings() {
        return new String[0];
    }

    @Override
    public String findParameter(String arg0) {
        return null;
    }

    @Override
    public String[] findParameters() {
        return new String[0];
    }

    @Override
    public String findRoleMapping(String arg0) {
        return null;
    }

    @Override
    public boolean findSecurityRole(String arg0) {
        return false;
    }

    @Override
    public String[] findSecurityRoles() {
        return new String[0];
    }

    @Override
    public String findServletMapping(String arg0) {
        return null;
    }

    @Override
    public String[] findServletMappings() {
        return new String[0];
    }

    @Override
    public String findStatusPage(int arg0) {
        return null;
    }

    @Override
    public int[] findStatusPages() {
        return new int[0];
    }

    //@Override
    public String findTaglib(String arg0) {
        return null;
    }

   // @Override
    public String[] findTaglibs() {
        return new String[0];
    }

    @Override
    public String[] findWatchedResources() {
        return new String[0];
    }

    @Override
    public boolean findWelcomeFile(String arg0) {
        return false;
    }

    @Override
    public String[] findWelcomeFiles() {
        return new String[0];
    }

    @Override
    public String[] findWrapperLifecycles() {
        return new String[0];
    }

    @Override
    public String[] findWrapperListeners() {
        return new String[0];
    }

    @Override
    public boolean fireRequestInitEvent(ServletRequest servletRequest) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean fireRequestDestroyEvent(ServletRequest servletRequest) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getAltDDName() {
        return null;
    }

    @Override
    public boolean getAllowCasualMultipartParsing() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setAllowCasualMultipartParsing(boolean b) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object[] getApplicationEventListeners() {
        return new Object[0];
    }

    @Override
    public Object[] getApplicationLifecycleListeners() {
        return new Object[0];
    }

    @Override
    public boolean getAvailable() {
        return false;
    }

    @Override
    public CharsetMapper getCharsetMapper() {
        return null;
    }

    @Override
    public URL getConfigFile() {
        return null;
    }

    @Override
    public void setConfigFile(URL url) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean getConfigured() {
        return false;
    }

    @Override
    public boolean getCookies() {
        return false;
    }

    @Override
    public boolean getCrossContext() {
        return false;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public boolean getDistributable() {
        return false;
    }

    @Override
    public String getDocBase() {
        return null;
    }

    @Override
    public String getEncodedPath() {
        return null;
    }

    @Override
    public boolean getIgnoreAnnotations() {
        return false;
    }

    @Override
    public LoginConfig getLoginConfig() {
        return null;
    }

    @Override
    public Mapper getMapper() {
        return null;
    }

    @Override
    public NamingResources getNamingResources() {
        return null;
    }

    @Override
    public boolean getOverride() {
        return false;
    }

    @Override
    public String getPath() {
        return null;
    }

    @Override
    public boolean getPrivileged() {
        return false;
    }

    @Override
    public String getPublicId() {
        return null;
    }

    @Override
    public boolean getReloadable() {
        return false;
    }

    @Override
    public ServletContext getServletContext() {
        return _servletContext;
    }

    @Override
    public int getSessionTimeout() {
        return 0;
    }

    @Override
    public boolean getSwallowOutput() {
        return false;
    }

    @Override
    public boolean getTldNamespaceAware() {
        return false;
    }

    @Override
    public boolean getTldValidation() {
        return false;
    }

    @Override
    public boolean getUseHttpOnly() {
        return false;
    }

    @Override
    public String getWrapperClass() {
        return null;
    }

    @Override
    public boolean getXmlNamespaceAware() {
        return false;
    }

    @Override
    public boolean getXmlValidation() {
        return false;
    }

    @Override
    public void reload() {

    }

    @Override
    public void removeApplicationListener(String arg0) {

    }

    @Override
    public void removeApplicationParameter(String arg0) {

    }

    @Override
    public void removeConstraint(SecurityConstraint arg0) {

    }

    @Override
    public void removeErrorPage(ErrorPage arg0) {

    }

    @Override
    public void removeFilterDef(FilterDef arg0) {

    }

    @Override
    public void removeFilterMap(FilterMap arg0) {

    }

    @Override
    public void removeInstanceListener(String arg0) {

    }

    @Override
    public void removeMimeMapping(String arg0) {

    }

    @Override
    public void removeParameter(String arg0) {

    }

    @Override
    public void removeRoleMapping(String arg0) {

    }

    @Override
    public void removeSecurityRole(String arg0) {

    }

    @Override
    public void removeServletMapping(String arg0) {

    }

    //@Override
    public void removeTaglib(String arg0) {

    }

    @Override
    public void removeWatchedResource(String arg0) {

    }

    @Override
    public void removeWelcomeFile(String arg0) {

    }

    @Override
    public void removeWrapperLifecycle(String arg0) {

    }

    @Override
    public void removeWrapperListener(String arg0) {

    }

    @Override
    public String getRealPath(String s) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getEffectiveMajorVersion() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setEffectiveMajorVersion(int i) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getEffectiveMinorVersion() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setEffectiveMinorVersion(int i) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addResourceJarUrl(URL url) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addServletContainerInitializer(ServletContainerInitializer servletContainerInitializer, Set<Class<?>> classes) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean getPaused() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isServlet22() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set<String> addServletSecurity(ApplicationServletRegistration applicationServletRegistration, ServletSecurityElement servletSecurityElement) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setResourceOnlyServlets(String s) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getResourceOnlyServlets() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isResourceOnlyServlet(String s) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getBaseName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setWebappVersion(String s) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getWebappVersion() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setFireRequestListenersOnForwards(boolean b) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean getFireRequestListenersOnForwards() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setPreemptiveAuthentication(boolean b) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean getPreemptiveAuthentication() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setSendRedirectBody(boolean b) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean getSendRedirectBody() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setAltDDName(String arg0) {

    }

    @Override
    public void setApplicationEventListeners(Object[] arg0) {

    }

    @Override
    public void setApplicationLifecycleListeners(Object[] arg0) {

    }

    //@Override
    public void setAvailable(boolean arg0) {

    }

    @Override
    public void setCharsetMapper(CharsetMapper arg0) {

    }

    @Override
    public String getCharset(Locale locale) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    //@Override
    public void setConfigFile(String arg0) {

    }

    @Override
    public void setConfigured(boolean arg0) {

    }

    @Override
    public void setCookies(boolean arg0) {

    }

    @Override
    public void setCrossContext(boolean arg0) {

    }

    @Override
    public void setDisplayName(String arg0) {

    }

    @Override
    public void setDistributable(boolean arg0) {

    }

    @Override
    public void setDocBase(String arg0) {

    }

    @Override
    public void setIgnoreAnnotations(boolean arg0) {

    }

    @Override
    public void setLoginConfig(LoginConfig arg0) {

    }

    @Override
    public void setNamingResources(NamingResources arg0) {

    }

    @Override
    public void setOverride(boolean arg0) {

    }

    @Override
    public void setPath(String arg0) {

    }

    @Override
    public void setPrivileged(boolean arg0) {

    }

    @Override
    public void setPublicId(String arg0) {

    }

    @Override
    public void setReloadable(boolean arg0) {

    }

    @Override
    public void setSessionTimeout(int arg0) {

    }

    @Override
    public boolean getSwallowAbortedUploads() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setSwallowAbortedUploads(boolean b) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setSwallowOutput(boolean arg0) {

    }

    @Override
    public void setTldNamespaceAware(boolean arg0) {

    }

    @Override
    public JarScanner getJarScanner() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setJarScanner(JarScanner jarScanner) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Authenticator getAuthenticator() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setLogEffectiveWebXml(boolean b) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean getLogEffectiveWebXml() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setTldValidation(boolean arg0) {

    }

    @Override
    public void setUseHttpOnly(boolean arg0) {

    }

    @Override
    public void setWrapperClass(String arg0) {

    }

    @Override
    public void setXmlNamespaceAware(boolean arg0) {

    }

    @Override
    public void setXmlValidation(boolean arg0) {

    }

    @Override
    public void addChild(Container arg0) {

    }

    @Override
    public void addContainerListener(ContainerListener arg0) {

    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener arg0) {

    }

    @Override
    public void backgroundProcess() {

    }

    @Override
    public Container findChild(String arg0) {
        return null;
    }

    @Override
    public Container[] findChildren() {
        return new Container[0];
    }

    @Override
    public ContainerListener[] findContainerListeners() {
        return new ContainerListener[0];
    }

    @Override
    public int getBackgroundProcessorDelay() {
        return 0;
    }

    @Override
    public Cluster getCluster() {
        return null;
    }

    @Override
    public String getInfo() {
        return null;
    }

    @Override
    public Loader getLoader() {
        return null;
    }

    @Override
    public Log getLogger() {
        return null;
    }

    @Override
    public Manager getManager() {
        return null;
    }

    @Override
    public Object getMappingObject() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public ObjectName getObjectName() {
        return null;
    }

    @Override
    public Container getParent() {
        return null;
    }

    @Override
    public ClassLoader getParentClassLoader() {
        return null;
    }

    @Override
    public Pipeline getPipeline() {
        return null;
    }

    @Override
    public Realm getRealm() {
        return _realm;
    }

    @Override
    public DirContext getResources() {
        return null;
    }

    @Override
    public void invoke(Request arg0, Response arg1) throws IOException,
            ServletException {

    }

    @Override
    public void removeChild(Container arg0) {

    }

    @Override
    public void removeContainerListener(ContainerListener arg0) {

    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener arg0) {

    }

    @Override
    public void fireContainerEvent(String s, Object o) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setBackgroundProcessorDelay(int arg0) {

    }

    @Override
    public void setCluster(Cluster arg0) {

    }

    @Override
    public void setLoader(Loader arg0) {

    }

    @Override
    public void setManager(Manager arg0) {

    }

    @Override
    public void setName(String arg0) {

    }

    @Override
    public void setParent(Container arg0) {

    }

    @Override
    public void setParentClassLoader(ClassLoader arg0) {

    }

    @Override
    public void setRealm(Realm realm) {
        _realm = realm;
    }

    @Override
    public void setResources(DirContext arg0) {

    }

    @Override
    public String getSessionCookieDomain() {
        return null;
    }

    @Override
    public String getSessionCookieName() {
        return null;
    }

    @Override
    public String getSessionCookiePath() {
        return null;
    }

    @Override
    public void setSessionCookieDomain(String arg0) {

    }

    @Override
    public void setSessionCookieName(String arg0) {

    }

    @Override
    public void setSessionCookiePath(String arg0) {

    }

    @Override
    public boolean getSessionCookiePathUsesTrailingSlash() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setSessionCookiePathUsesTrailingSlash(boolean b) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public AccessLog getAccessLog() {
        return null;
    }

    @Override
    public int getStartStopThreads() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setStartStopThreads(int i) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void logAccess(Request arg0, Response arg1, long arg2, boolean arg3) {

    }

    //@Override
    public boolean isDisableURLRewriting() {
        return false;
    }

    //@Override
    public void setDisableURLRewriting(boolean arg0) {

    }

    @Override
    public void addLifecycleListener(LifecycleListener lifecycleListener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return new LifecycleListener[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeLifecycleListener(LifecycleListener lifecycleListener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void init() throws LifecycleException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void start() throws LifecycleException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void stop() throws LifecycleException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void destroy() throws LifecycleException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public LifecycleState getState() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getStateName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
