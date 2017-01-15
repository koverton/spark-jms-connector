# spark-jms-connector
A sample JMS connector for Spark streaming apps

This library provides a minimal Spark-style wrapping around a JMS Receiver so that JMS messages can be easily consumed from Spark Streaming processes and stored to the Spark cluster with the desired storage level.  Messages are acknowledged to the broker upon succesful storage, but not if any processing exception prevents storage. This allows the JMS message broker to redeliver the message.

The `JMSReceiver` is a subclass of `org.apache.spark.streaming.receiver.Receiver`. It is designed to be passed into the `JavaStreamingContext.receiverStream( )` method to construct a `JavaReceiverInputDStream` for stream handling. The `JMSReceiver<V>` is passed a generic type parameter `V` representing the payload data type your streaming program deals with. Note however, that the resulting `JavaReceiverInputDStream` has a slightly different type parameter, `JMSValue<V>` wrapping your type parameter.

JMS Messages are marshalled into instances of your application's internal data type `V`, then wrapped in instances of `JMSValue<V>`. The `JMSValue` wrapper associates other useful JMS data with the payload, like the destination topic or queue the message was consumed from. But before the `JMSReceiver<V>` can contruct the `JMSValue<V>` instance, it needs to marshal the native JMS message payload into an instance of the desired value type `V`.

To marshal message payloads into the target internal data type, the `JMSReceiver<V>` applies a `JMSDeserializer<V>` function to the message upon arrival. This `JMSDeserializer<V>` is passed into the `JMSReceiver<V>`'s constructor. Some samples of common deserialization routines are available in the `JMSDeserializerFactory`. Deserializer functions are instances of Scala `Function1<T,R>` functors where the input type is always `javax.jms.Message` and the return type is the desired type for your Spark streaming application. A helper abstract class `JMSDeserializer<Output>` hides the input type parameter because it is only ever called within the `JMSReceiver<V>` instance.

Here is an example String deserializer function:

```java
       return new JMSDeserializer<String>() {

            public JMSValue<String> apply(Message msg) {
                try {
                    if (msg instanceof TextMessage) {
                        TextMessage txtmsg = (TextMessage) msg;
                        return new JMSValue<>(toDest(msg.getJMSDestination()), txtmsg.getText());
                    }
                    return new JMSValue<>(toDest(msg.getJMSDestination()), msg.toString());
                }
                catch(JMSException jmse) {
                    // ... up to you ...
                }
                return null;
            }
        };
```
