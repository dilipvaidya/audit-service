
# Stage 1: Build the WAR
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml first and resolve dependencies (cache-friendly)
COPY pom.xml .
COPY minio-bucket-init.sh .
COPY wait-for-it.sh .
RUN mvn dependency:go-offline

# Copy all source files
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Base CentOS + Tomcat Runtime
FROM centos:7

# Install Java & other utils
RUN sed -i 's|mirrorlist=|#mirrorlist=|g' /etc/yum.repos.d/CentOS-Base.repo && \
    sed -i 's|#baseurl=http://mirror.centos.org|baseurl=http://vault.centos.org|g' /etc/yum.repos.d/CentOS-Base.repo && \
    yum install -y wget unzip net-tools nmap-ncat && \
    yum clean all

# Install JDK 17 on CentOS 7 (via Adoptium)
RUN curl -L -o openjdk17.tar.gz https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.10+7/OpenJDK17U-jdk_x64_linux_hotspot_17.0.10_7.tar.gz && \
    mkdir -p /opt/java-17-openjdk && \
    tar -xzf openjdk17.tar.gz -C /opt/java-17-openjdk --strip-components=1 && \
    rm openjdk17.tar.gz

# Install mc and dependencies
RUN wget https://dl.min.io/client/mc/release/linux-amd64/mc -O /usr/local/bin/mc && \
    chmod +x /usr/local/bin/mc

# Set environment
ENV JAVA_HOME=/opt/java-17-openjdk
ENV CATALINA_HOME=/opt/tomcat
ENV PATH=$CATALINA_HOME/bin:$PATH

# Install Tomcat
RUN wget https://archive.apache.org/dist/tomcat/tomcat-9/v9.0.85/bin/apache-tomcat-9.0.85.tar.gz -O /tmp/tomcat.tar.gz && \
    mkdir -p $CATALINA_HOME && \
    tar -xzf /tmp/tomcat.tar.gz -C $CATALINA_HOME --strip-components=1 && \
    rm /tmp/tomcat.tar.gz

# Optionally remove the default ROOT,docs & examples.
RUN rm -rf $CATALINA_HOME/webapps/*

# Copy WAR
COPY --from=build /app/target/*.war $CATALINA_HOME/webapps/audit-service.war

# Copy scripts (for runtime use)
COPY wait-for-it.sh /usr/local/bin/wait-for-it.sh
COPY minio-bucket-init.sh /usr/local/bin/minio-bucket-init.sh
RUN chmod +x /usr/local/bin/wait-for-it.sh /usr/local/bin/minio-bucket-init.sh

# Expose port
EXPOSE 8080

# Start sequence: wait for dependencies, initialize bucket, then start Tomcat
CMD /usr/local/bin/wait-for-it.sh minio:9000 && \
    /usr/local/bin/wait-for-it.sh elasticsearch:9200 && \
    /usr/local/bin/wait-for-it.sh kafka:9092 && \
    /usr/local/bin/minio-bucket-init.sh && \
    $CATALINA_HOME/bin/catalina.sh run
