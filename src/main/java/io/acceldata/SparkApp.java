package io.acceldata;

import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparkApp {
    private static final Logger logger = LoggerFactory.getLogger(SparkApp.class);

    public static void main(String[] args) {
        // Create Spark session
        SparkSession spark = SparkSession.builder()
                .appName("SparkApp")
                .master("local[*]")
                .getOrCreate();

        try {
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
            
        } finally {
            // Stop Spark session
            spark.stop();
        }
    }
} 