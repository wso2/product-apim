FROM openjdk:8u111-jdk
COPY /maven/wso2am.zip .
RUN apt-get update && apt-get install unzip
RUN unzip wso2am.zip -d wso2am
EXPOSE 9090
ENTRYPOINT ["wso2am/WSO2APIManager-1.0.0-SNAPSHOT/bin/carbon.sh"]