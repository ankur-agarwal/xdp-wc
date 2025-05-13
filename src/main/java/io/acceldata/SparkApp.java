package io.acceldata;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class SparkApp {
    private static final Logger logger = LoggerFactory.getLogger(SparkApp.class);
    
    // Inner Tweet class to store tweet data
    static class Tweet {
        private String id;
        private String text;
        private String author;
        private String timestamp;
        
        public Tweet(String id, String text, String author, String timestamp) {
            this.id = id;
            this.text = text;
            this.author = author;
            this.timestamp = timestamp;
        }
        
        @Override
        public String toString() {
            return "Tweet ID: " + id + "\n" +
                   "Author: " + author + "\n" +
                   "Time: " + timestamp + "\n" +
                   "Tweet: " + text + "\n" +
                   "----------------------------------------\n";
        }
    }
    
    public static void main(String[] args) {
        // Default values
        String topic = "artificial intelligence";
        int tweetCount = 20;
        
        // Parse command line arguments
        if (args.length > 0) {
            // Check if the last argument is a number (tweet count)
            if (args.length > 1 && args[args.length - 1].matches("\\d+")) {
                tweetCount = Integer.parseInt(args[args.length - 1]);
                // Combine all arguments except the last one as the topic
                topic = String.join(" ", java.util.Arrays.copyOfRange(args, 0, args.length - 1));
            } else {
                // All arguments form the topic
                topic = String.join(" ", args);
            }
        }
        
        String slugTopic = topic.toLowerCase().replaceAll("[^a-z0-9]", "-");
        
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
            
            // Generate simulated tweets with random sentiment distribution
            logger.info("Generating {} simulated tweets about: \"{}\"", tweetCount, topic);
            List<Tweet> tweets = generateTweetsWithSentiment(topic, tweetCount);
            
            logger.info("Successfully generated {} tweets", tweets.size());
            
            // Get filesystem with the authenticated configuration
            FileSystem fileSystem = FileSystem.get(hadoopConf);
            logger.info("Successfully connected to HDFS filesystem: {}", fileSystem.getUri());
            
            // Path to write the file - use the topic slug in the filename
            Path filePath = new Path("/user/hdfs/tweets-" + slugTopic + ".txt");
            
            // Using Hadoop FileSystem API for direct write
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fileSystem.create(filePath)))) {
                writer.write("SIMULATED TWEETS ABOUT: " + topic.toUpperCase() + "\n");
                writer.write("Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "\n");
                writer.write("=================================================\n\n");
                
                for (Tweet tweet : tweets) {
                    writer.write(tweet.toString());
                }
                
                logger.info("Successfully wrote {} tweets to file: {}", tweets.size(), filePath);
            }
            
            // Close resource
            fileSystem.close();
            
        } catch (IOException e) {
            logger.error("Error writing to HDFS: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("General error: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Generate simulated tweets with mixed sentiments about a topic
     */
    private static List<Tweet> generateTweetsWithSentiment(String topic, int count) {
        List<Tweet> tweets = new ArrayList<>();
        Random random = new Random();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // Determine the sentiment distribution for this run (random each time)
        // Random between 0-60% positive, 0-60% negative, with the remainder neutral
        int positivePercentage = random.nextInt(61);  // 0-60%
        int negativePercentage = random.nextInt(61 - positivePercentage); // Ensure doesn't exceed 100% with positive
        int neutralPercentage = 100 - positivePercentage - negativePercentage;
        
        logger.info("Generated sentiment distribution - Positive: {}%, Negative: {}%, Neutral: {}%", 
                     positivePercentage, negativePercentage, neutralPercentage);
        
        // Calculate the count of each type based on percentages
        int positiveCount = Math.round(count * positivePercentage / 100f);
        int negativeCount = Math.round(count * negativePercentage / 100f);
        int neutralCount = count - positiveCount - negativeCount;
        
        // Templates for generating positive tweets
        String[] positiveTemplates = {
            "Just read an amazing article about #TOPIC#. The future is bright! 😍",
            "#TOPIC# is revolutionizing everything in the best way possible! So excited!",
            "Incredible advances in #TOPIC# are opening wonderful new possibilities. Love it!",
            "My latest project using #TOPIC# has exceeded all expectations! Thrilled with the results!",
            "Attended a fantastic workshop on #TOPIC# yesterday. Blown away by the possibilities! 🚀",
            "The latest research on #TOPIC# is absolutely groundbreaking. Game changer for good!",
            "So impressed with how #TOPIC# is improving lives. This is why I love technology!",
            "#TOPIC# success stories are inspiring - proud to be part of this community!",
            "Just published my positive experience with #TOPIC# - it's been transformative!",
            "Been studying #TOPIC# for months now. It's even better than I thought! 👏",
            "Prediction: #TOPIC# will create incredible opportunities in the next decade.",
            "#TOPIC# is living up to the hype! Exceeded my expectations in every way.",
            "The benefits of #TOPIC# for society are truly remarkable and exciting!",
            "Loving the latest #TOPIC# developments! Let's celebrate these achievements!",
            "Working with #TOPIC# brings joy to my professional life every single day."
        };
        
        // Templates for generating negative tweets
        String[] negativeTemplates = {
            "Frustrated with the overhyped promises of #TOPIC#. Not living up to expectations at all. 😡",
            "#TOPIC# is causing more problems than it's solving. We need to rethink this approach.",
            "Concerning trends in #TOPIC# are raising serious ethical questions we're ignoring.",
            "My experience with #TOPIC# has been disappointing. Waste of resources and time.",
            "That conference on #TOPIC# was a letdown. Nothing but empty promises. 👎",
            "The dark side of #TOPIC# isn't getting enough attention. Serious downsides.",
            "Getting tired of the exaggerated claims about #TOPIC#. Show me real results!",
            "#TOPIC# failures are piling up - why isn't anyone talking about this?",
            "Published my critique of #TOPIC# - someone needs to address these problems.",
            "After months studying #TOPIC#, I'm alarmed by the potential negative impacts.",
            "Warning: #TOPIC# might create more inequality and problems than solutions.",
            "Unpopular opinion: #TOPIC# is mostly hype with little substance behind it.",
            "The risks of #TOPIC# far outweigh the benefits, based on current evidence.",
            "Worried about the direction #TOPIC# development is heading. We need safeguards!",
            "Implementation of #TOPIC# has been a nightmare. Avoid the mistakes we made."
        };
        
        // Templates for generating neutral tweets
        String[] neutralTemplates = {
            "Considering different perspectives on #TOPIC#. Mixed results so far.",
            "#TOPIC# has both advantages and limitations worth discussing.",
            "An objective analysis of #TOPIC# reveals a complex picture with tradeoffs.",
            "Currently researching #TOPIC# applications. Results vary by context.",
            "Attended a balanced panel on #TOPIC# yesterday. Interesting points on all sides.",
            "Current research on #TOPIC# shows a mix of promising and concerning outcomes.",
            "Question for experts: what metrics should we use to evaluate #TOPIC#?",
            "Let's discuss #TOPIC# objectively - what are your experiences?",
            "Collecting diverse viewpoints on #TOPIC# for a comprehensive analysis.",
            "Studying the various applications of #TOPIC# across different sectors.",
            "#TOPIC# implementation requires careful consideration of multiple factors.",
            "The conversation around #TOPIC# needs more nuance and less polarization.",
            "Interesting to compare #TOPIC# approaches across different industries.",
            "Following developments in #TOPIC# with an analytical perspective.",
            "The #TOPIC# landscape is evolving - still too early to make definitive judgments."
        };
        
        // Fictional user handles and names
        String[] userHandles = {
            "@techvisionary", "@futuristThinker", "@innovationHub", "@digitalNomad",
            "@researchGuru", "@dataScienceWiz", "@techTrendsetter", "@aiEnthusiast",
            "@codeCrafter", "@futureForward", "@technologyPro", "@digitalPioneer",
            "@techExplorer", "@codeArtisan", "@dataPhilosopher", "@futureNavigator",
            "@analyticsMind", "@techEthicist", "@digitalStrategist", "@techCritic"
        };
        
        String[] userNames = {
            "Tech Insights", "Future Tech Today", "Innovation Station", "Digital Frontiers",
            "Research Realm", "Data Science Daily", "Tech Trends", "AI Adventures",
            "Code Creators", "Future Forward", "Technology Pulse", "Digital Pioneers",
            "Tech Explorers", "Code Artisans", "Data Philosophers", "Future Navigators",
            "Analytics Mind", "Tech Ethics", "Digital Strategy", "Critical Tech Review"
        };
        
        // Generate positive tweets
        generateTweetsOfType(tweets, positiveTemplates, userHandles, userNames, topic, positiveCount, random, formatter);
        
        // Generate negative tweets
        generateTweetsOfType(tweets, negativeTemplates, userHandles, userNames, topic, negativeCount, random, formatter);
        
        // Generate neutral tweets
        generateTweetsOfType(tweets, neutralTemplates, userHandles, userNames, topic, neutralCount, random, formatter);
        
        // Shuffle the tweets to mix the sentiments
        java.util.Collections.shuffle(tweets, random);
        
        return tweets;
    }
    
    /**
     * Helper method to generate tweets of a specific sentiment type
     */
    private static void generateTweetsOfType(List<Tweet> tweets, String[] templates, String[] userHandles, 
                                             String[] userNames, String topic, int count, 
                                             Random random, DateTimeFormatter formatter) {
        for (int i = 0; i < count; i++) {
            // Generate a UUID-based ID
            String id = UUID.randomUUID().toString().replace("-", "").substring(0, 19);
            
            // Select a random template and replace #TOPIC# with the actual topic
            String template = templates[random.nextInt(templates.length)];
            String text = template.replace("#TOPIC#", topic);
            
            // Create hashtags from topic words
            String[] topicWords = topic.split("\\s+");
            for (String word : topicWords) {
                if (word.length() > 3 && !text.toLowerCase().contains("#" + word.toLowerCase())) {
                    text += " #" + word.toLowerCase();
                }
            }
            
            // Select a random user
            int userIndex = random.nextInt(userHandles.length);
            String userHandle = userHandles[userIndex];
            String userName = userNames[userIndex];
            String author = userHandle + " (" + userName + ")";
            
            // Generate a timestamp within the last 24 hours
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime tweetTime = now.minusMinutes(random.nextInt(1440));
            String timestamp = tweetTime.format(formatter);
            
            // Create and add the tweet
            tweets.add(new Tweet(id, text, author, timestamp));
        }
    }
}