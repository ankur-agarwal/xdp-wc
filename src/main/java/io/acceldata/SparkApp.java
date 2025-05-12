package io.acceldata;

import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;
import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;

public class SparkApp {
    private static final Logger logger = LoggerFactory.getLogger(SparkApp.class);
    
    public static void main(String[] args) {
        try {
            // Set path to Kerberos configuration
            System.setProperty("java.security.krb5.conf", "/etc/krb5.conf");
            System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");
            
            // Create Hadoop configuration first
            Configuration hadoopConf = new Configuration();
            
            // Add specific configuration files
            hadoopConf.addResource(new Path("/etc/hadoop/conf/core-site.xml"));
            hadoopConf.addResource(new Path("/etc/hadoop/conf/hdfs-site.xml"));
            hadoopConf.addResource(new Path("/etc/hadoop/conf/hive-site.xml"));
            
            // Set critical Kerberos configurations explicitly
            hadoopConf.set("hadoop.security.authentication", "kerberos");
            hadoopConf.set("hadoop.security.authorization", "true");
            hadoopConf.set("fs.defaultFS", "hdfs://qenamenode1:8020");
            
            // Initialize Kerberos security
            UserGroupInformation.setConfiguration(hadoopConf);
            
            // Login using the keytab - this is critical!
            String principal = "hdfs-adocqecluster@ADSRE.COM";
            String keytabPath = "/etc/user.keytab";
            
            logger.info("Attempting Kerberos login with principal: {} and keytab: {}", principal, keytabPath);
            UserGroupInformation.loginUserFromKeytab(principal, keytabPath);
            logger.info("Successfully authenticated with Kerberos");
            
            // Get filesystem with the authenticated configuration
            FileSystem fileSystem = FileSystem.get(hadoopConf);
            logger.info("Successfully connected to HDFS filesystem: {}", fileSystem.getUri());
            
            // Path to write the file
            Path filePath = new Path("/user/hdfs/hello-world.txt");
            
            // Using Hadoop FileSystem API for direct write
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fileSystem.create(filePath)))) {
                writer.write("Hello World");
                logger.info("File successfully written using Hadoop API at: {}", filePath);
            }
            
            // Close resource
            fileSystem.close();
            
        } catch (IOException e) {
            logger.error("Error writing to HDFS: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("General error: {}", e.getMessage(), e);
        }
    }
}