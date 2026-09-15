FROM openjdk:11-jdk
LABEL maintainer="research-maint@itcast.cn"
RUN echo "Asia/Shanghai" > /etc/timezone
ARG PACKAGE_PATH=./target/jzo2o-aigc.jar

ADD ${PACKAGE_PATH:-./} app.jar

EXPOSE 11511
ENTRYPOINT ["sh","-c","java -jar $JAVA_OPTS app.jar"]
