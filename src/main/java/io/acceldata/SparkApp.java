package io.acceldata;

import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.conf.Configuration;
import java.nio.file.Paths;
import org.apache.hadoop.security.UserGroupInformation;
import java.io.IOException;

public class SparkApp {
    private static final Logger logger = LoggerFactory.getLogger(SparkApp.class);

    public static void main(String[] args) {
        // Get paths from environment variables
        String coreSitePath = System.getenv().getOrDefault("CORE_SITE_XML_PATH", "/etc/hadoop/conf/core-site.xml");
        String hdfsSitePath = System.getenv().getOrDefault("HDFS_SITE_XML_PATH", "/etc/hadoop/conf/hdfs-site.xml");
        String hiveSitePath = System.getenv().getOrDefault("HIVE_SITE_XML_PATH", "/etc/hadoop/conf/hive-site.xml");
        String krb5ConfPath = System.getenv().getOrDefault("KRB5_CONF_PATH", "/etc/krb5.conf");
        String keytabPath = System.getenv().getOrDefault("KERBEROS_KEYTAB_PATH", "/etc/user.keytab");

        // Set Kerberos configuration
        System.setProperty("java.security.krb5.conf", krb5ConfPath);
        logger.info("Set java.security.krb5.conf to {}", krb5ConfPath);

        // Load Hadoop configuration
        Configuration hadoopConf = new Configuration();
        hadoopConf.addResource(coreSitePath);
        hadoopConf.addResource(hdfsSitePath);
        hadoopConf.addResource(hiveSitePath);
        logger.info("Loaded Hadoop configuration files");

        // Set Hadoop security authentication to Kerberos
        hadoopConf.set("hadoop.security.authentication", "kerberos");
        UserGroupInformation.setConfiguration(hadoopConf);
        String principal = "hdfs-adocqecluster@ADSRE.COM";
        try {
            UserGroupInformation.loginUserFromKeytab(principal, keytabPath);
            logger.info("Kerberos authentication successful for principal {} using keytab {}", principal, keytabPath);
        } catch (IOException e) {
            logger.error("Kerberos authentication failed", e);
            System.exit(1);
        }

        // Create Spark session with Hadoop conf directory
        SparkSession spark = SparkSession.builder()
                .appName("SparkApp")
                .master("local[*]")
                .config("spark.hadoop.hadoop.security.authentication", "kerberos")
                .config("spark.hadoop.fs.defaultFS", hadoopConf.get("fs.defaultFS"))
                .config("spark.hadoop.hadoop.security.authorization", "true")
                .config("spark.hadoop.java.security.krb5.conf", krb5ConfPath)
                .getOrCreate();

        try {
            // Validate Spark can access HDFS as authenticated user
            try {
                org.apache.hadoop.fs.FileSystem fs = org.apache.hadoop.fs.FileSystem.get(hadoopConf);
                org.apache.hadoop.fs.Path rootPath = new org.apache.hadoop.fs.Path("/");
                boolean exists = fs.exists(rootPath);
                logger.info("HDFS root directory exists: {}", exists);
            } catch (IOException e) {
                logger.error("Error accessing HDFS root directory", e);
            }
            
            // Log the number of arguments received
            logger.debug("Number of arguments received: {}", args.length);
            
            // Default message
            String message = "Hello World";
            
            // If arguments are provided, use the first argument as the message
            if (args.length > 0) {
                message = args[0];
                logger.debug("Using custom message: {}", message);
            } else {
                logger.debug("No arguments provided, using default message");
            }
            
            // Log the message
            logger.info("Final message: {}", message);
            
            // Write the message to HDFS using Spark
            try {
                String outputPath = "hdfs://qenamenode1:8020/user/hdfs/ankur-spark-message.txt";
                spark.createDataset(java.util.Collections.singletonList(message), org.apache.spark.sql.Encoders.STRING())
                        .write()
                        .mode("overwrite")
                        .text(outputPath);
                logger.info("Successfully wrote message to HDFS at {}", outputPath);
            } catch (Exception e) {
                logger.error("Failed to write message to HDFS via Spark", e);
            }
            
        } finally {
            // Stop Spark session
            spark.stop();
        }
    }
} 