package com.example.wordcount;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class WordCount {
    private static final Logger logger = LoggerFactory.getLogger(WordCount.class);

    public static void main(String[] args) {
        logger.info("Starting WordCount application");
        
        // Create Spark session
        SparkSession spark = SparkSession.builder()
                .appName("WordCount")
                .master("local[*]")
                .getOrCreate();

        logger.info("Spark session created successfully");

        // Create JavaSparkContext
        JavaSparkContext sc = new JavaSparkContext(spark.sparkContext());

        try {
            // Read the input file
            String inputFile = "src/main/resources/lorem.txt";
            logger.info("Reading input file: {}", inputFile);
            JavaRDD<String> lines = sc.textFile(inputFile);

            // Perform word count
            logger.info("Starting word count processing");
            JavaRDD<String> words = lines.flatMap(line -> Arrays.asList(line.split("\\s+")).iterator());
            JavaPairRDD<String, Integer> wordCounts = words.mapToPair(word -> new Tuple2<>(word.toLowerCase(), 1))
                    .reduceByKey((a, b) -> a + b);

            // Print the results
            logger.info("Word Count Results:");
            wordCounts.collect().forEach(tuple -> 
                logger.info("{}: {}", tuple._1(), tuple._2()));

        } catch (Exception e) {
            logger.error("An error occurred during processing", e);
            throw e;
        } finally {
            // Stop Spark context
            logger.info("Stopping Spark context");
            sc.stop();
            spark.stop();
            logger.info("Application completed");
        }
    }
} 