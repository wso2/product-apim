package org.wso2.am.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.ModulePathResolver;

import java.time.Duration;


public abstract class BaseAPIMContainer extends GenericContainer<BaseAPIMContainer> {

    private static final int HTTPS_PORT = 9443 ;
    private static final int HTTP_PORT = 9763 ;
    private static final int GATEWAY_HTTPS_PORT = 8243 ;
    private static final int GATEWAY_HTTP_PORT = 8280 ;

    String callerModuleDir = ModulePathResolver.getModuleDir(BaseAPIMContainer.class);

    public BaseAPIMContainer() {
        super(Constants.DOCKER_IMAGE);
        withExposedPorts(HTTPS_PORT, HTTP_PORT,GATEWAY_HTTP_PORT,GATEWAY_HTTPS_PORT);
        withCopyFileToContainer(MountableFile.forHostPath(callerModuleDir + Constants.API_MANAGER_DIR), "/opt/");
        withCommand("sh /opt/bin/api-manager.sh");
        withNetwork(ContainerNetwork.SHARED_NETWORK);

        // Env vars for APIMGT_DB
        withEnv("API_MANAGER_DATABASE_TYPE", System.getenv("API_MANAGER_DATABASE_TYPE"));
        withEnv("API_MANAGER_DATABASE_TYPE", System.getenv("API_MANAGER_DATABASE_TYPE"));
        withEnv("API_MANAGER_DATABASE_DRIVER", System.getenv("API_MANAGER_DATABASE_DRIVER"));
        withEnv("API_MANAGER_DATABASE_URL", System.getenv("API_MANAGER_DATABASE_URL").replace("&", "&amp;"));
        withEnv("API_MANAGER_DATABASE_USERNAME", System.getenv("API_MANAGER_DATABASE_USERNAME"));
        withEnv("API_MANAGER_DATABASE_PASSWORD", System.getenv("API_MANAGER_DATABASE_PASSWORD"));
        withEnv("API_MANAGER_DATABASE_VALIDATION_QUERY", System.getenv("API_MANAGER_DATABASE_VALIDATION_QUERY"));

        // Env vars for SHARED_DB
        withEnv("SHARED_DATABASE_TYPE", System.getenv("SHARED_DATABASE_TYPE"));
        withEnv("SHARED_DATABASE_DRIVER", System.getenv("SHARED_DATABASE_DRIVER"));
        withEnv("SHARED_DATABASE_URL", System.getenv("SHARED_DATABASE_URL").replace("&", "&amp;"));
        withEnv("SHARED_DATABASE_USERNAME", System.getenv("SHARED_DATABASE_USERNAME"));
        withEnv("SHARED_DATABASE_PASSWORD", System.getenv("SHARED_DATABASE_PASSWORD"));
        withEnv("SHARED_DATABASE_VALIDATION_QUERY", System.getenv("SHARED_DATABASE_VALIDATION_QUERY"));

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



