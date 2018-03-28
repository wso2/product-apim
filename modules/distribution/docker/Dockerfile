FROM openjdk:jre-alpine
ARG version=3.0.0-SNAPSHOT
ENV dist_version=3.0.0-SNAPSHOT
ADD wso2apim-$version.zip /mnt
WORKDIR /mnt
RUN unzip /mnt/wso2apim-$version.zip
EXPOSE 9443
ENTRYPOINT exec "/mnt/wso2apim-${dist_version}/wso2/default/bin/carbon.sh"