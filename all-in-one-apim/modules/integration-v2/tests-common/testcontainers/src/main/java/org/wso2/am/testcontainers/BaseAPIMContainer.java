package org.wso2.am.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;
import org.wso2.am.integration.test.utils.ModulePathResolver;

import java.time.Duration;

public abstract class BaseAPIMContainer extends GenericContainer<BaseAPIMContainer> {

    private static final String DOCKER_IMAGE = "openjdk:11-jre-slim";
    private static final String API_MANAGER_DIR = "/src/main/resources/apim/wso2am-4.5.0-SNAPSHOT";
    protected static final String TOML_PATH = "/opt/repository/conf/deployment.toml";
    private static final int HTTPS_PORT = 9443;
    private static final int HTTP_PORT = 9763;
    private static final int GATEWAY_HTTPS_PORT = 8243;
    private static final int GATEWAY_HTTP_PORT = 8280;
    String callerModuleDir = ModulePathResolver.getModuleDir(BaseAPIMContainer.class);

    public BaseAPIMContainer() {
        super(DOCKER_IMAGE);
        withExposedPorts(HTTPS_PORT, HTTP_PORT,GATEWAY_HTTP_PORT,GATEWAY_HTTPS_PORT);
        withCopyFileToContainer(MountableFile.forHostPath(callerModuleDir+API_MANAGER_DIR), "/opt/");
        withCommand("sh /opt/bin/api-manager.sh");
        withNetwork(ContainerNetwork.SHARED_NETWORK);
        waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(20)));
    }

    public String getAPIManagerUrl() {
        return String.format("https://%s:%d/", getHost(), getMappedPort(HTTPS_PORT));
    }

    public Integer getHttpsPort() {
        return getMappedPort(HTTPS_PORT);
    }

    public Integer getHttpPort() {
        return getMappedPort(HTTP_PORT);
    }
}
