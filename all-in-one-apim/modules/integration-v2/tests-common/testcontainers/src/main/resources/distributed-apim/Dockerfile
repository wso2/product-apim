# Manual distributed APIM component image recipe.
# The build context must contain component.zip and mysql-connector-j.jar.
ARG BASE_IMAGE=eclipse-temurin:21-jdk
FROM ${BASE_IMAGE}

ARG START_SCRIPT
ENV WSO2_HOME=/opt/wso2
ENV WSO2_START_SCRIPT=${START_SCRIPT}

RUN apt-get update \
    && apt-get install --no-install-recommends -y unzip ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --create-home --shell /bin/bash wso2carbon \
    && mkdir -p /opt/wso2-stage

COPY component.zip /tmp/component.zip
COPY mysql-connector-j.jar /tmp/mysql-connector-j.jar
COPY start-component.sh /usr/local/bin/start-component.sh

RUN set -eux; \
    unzip -q /tmp/component.zip -d /opt/wso2-stage; \
    root_dir="$(find /opt/wso2-stage -mindepth 1 -maxdepth 1 -type d ! -name __MACOSX -print -quit)"; \
    test -n "${root_dir}"; \
    test -x "${root_dir}/bin/${START_SCRIPT}"; \
    mv "${root_dir}" "${WSO2_HOME}"; \
    cp /tmp/mysql-connector-j.jar "${WSO2_HOME}/repository/components/lib/mysql-connector-j.jar"; \
    chmod +x /usr/local/bin/start-component.sh; \
    chown -R wso2carbon:wso2carbon "${WSO2_HOME}"; \
    rm -rf /opt/wso2-stage /tmp/component.zip /tmp/mysql-connector-j.jar

USER wso2carbon
WORKDIR ${WSO2_HOME}
ENTRYPOINT ["/usr/local/bin/start-component.sh"]
