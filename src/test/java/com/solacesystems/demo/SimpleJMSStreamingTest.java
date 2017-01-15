package com.solacesystems.demo;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

public class SimpleJMSStreamingTest
{
    static final String brokerURL = "smf://192.168.56.101";
    static final String username = "test@poc_vpn";
    static final String password = "whatever";
    static final String connectionFactoryName = "spark_cf";
    // static final JMSDestination queue = JMSDestination.newQueue("spark_queue");
    static final JMSDestination topic = JMSDestination.newTopic("spark/topic");

    public static void main( String[] args ) throws InterruptedException
    {
        final SparkConf conf = new SparkConf()
                .setMaster("local")
                .setAppName("Spark Streaming App");
        final JavaStreamingContext sc = new JavaStreamingContext(conf, Durations.seconds(1));

        final JavaReceiverInputDStream<JMSValue<String>> msgstream =
                sc.receiverStream(
                        new JMSReceiver(
                                brokerURL,
                                username,
                                password,
                                topic,
                                connectionFactoryName,
                                JMSDeserializerFactory.createStringDeserializer()
                        )
                );

        msgstream.foreachRDD(new VoidFunction<JavaRDD<JMSValue<String>>>() {
            @Override
            public void call(JavaRDD<JMSValue<String>> rdd) throws Exception {
                System.out.println("GOT RDD WITH " + rdd.count() + " itms");
                for(final JMSValue<String> val : rdd.collect())
                    System.out.println("MSG: " + val.getValue());
            }
        });

        sc.start();
        sc.awaitTermination();
    }
}
