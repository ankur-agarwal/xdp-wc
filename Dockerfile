# Build stage
FROM gradle:8.5.0-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon

# Runtime stage
FROM bitnami/spark:3.5.0
USER root

# Create directory for the application
WORKDIR /opt/spark/apps

# Copy the built JAR from the build stage
COPY --from=build /app/build/libs/*.jar /tmp

# Copy Hadoop and Kerberos conf files
COPY src/main/resources/conf/core-site.xml /opt/spark/conf/core-site.xml
COPY src/main/resources/conf/hdfs-site.xml /opt/spark/conf/hdfs-site.xml
COPY src/main/resources/conf/hive-site.xml /opt/spark/conf/hive-site.xml
COPY src/main/resources/conf/krb5.conf /opt/spark/conf/krb5.conf
COPY src/main/resources/conf/hdfs.headless.keytab /opt/spark/conf/hdfs.headless.keytab

RUN chmod -R 777 /tmp \
    && chown -R 1001:1001 /tmp

RUN chmod -R 777 /opt/spark
RUN chown -R 1001:1001 /opt/spark
RUN chmod 600 /opt/spark/conf/hdfs.headless.keytab

# Set environment variable for Kerberos config
ENV JAVA_SECURITY_KRB5_CONF=/opt/spark/conf/krb5.conf

# Switch back to non-root user for security
USER 1001

# Example entrypoint (uncomment and adjust as needed):
# ENTRYPOINT ["/opt/bitnami/spark/bin/spark-submit", "--master", "local[*]", "--class", "io.acceldata.SparkApp", "/tmp/spark-hello-world-all.jar"]
