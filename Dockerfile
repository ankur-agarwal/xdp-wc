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

RUN mkdir -p /opt/spark/conf \
    && chmod 777 /opt/spark/conf \
    && chown 1001:1001 /opt/spark/conf

# Set environment variables for configuration paths
ENV CONFIG_FILE=/opt/spark/conf/application.conf
ENV CORE_SITE_XML_PATH=/etc/hadoop/conf/core-site.xml
ENV HDFS_SITE_XML_PATH=/etc/hadoop/conf/hdfs-site.xml
ENV HIVE_SITE_XML_PATH=/etc/hadoop/conf/hive-site.xml
ENV KRB5_CONF_PATH=/etc/krb5.conf
ENV KERBEROS_KEYTAB_PATH=/etc/user.keytab

RUN chmod -R 777 /tmp \
    && chown -R 1001:1001 /tmp

RUN chmod -R 777 /opt/spark
RUN chown -R 1001:1001 /opt/spark

# Switch back to non-root user for security
USER 1001

# Example entrypoint (uncomment and adjust as needed):
# ENTRYPOINT ["/opt/bitnami/spark/bin/spark-submit", "--master", "local[*]", "--class", "io.acceldata.SparkApp", "/tmp/spark-hello-world-all.jar"]
