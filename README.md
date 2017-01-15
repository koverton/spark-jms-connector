# spark-jms-connector
## Overview

This library provides a minimal Spark-style wrapping around a JMS Receiver so that JMS messages can be easily consumed from Spark Streaming processes and stored to the Spark cluster with the desired storage level.  Messages are acknowledged to the broker upon succesful storage, but not if any processing exception prevents storage. This allows the JMS message broker to redeliver the message.

For example:
```java
       JMSDeserializer<String> deserializer = JMSDeserializerFactory.createStringDeserializer();

       Receiver<JMSValue<String>> receiver = new JMSReceiver(url, user, pass, topic, cfname, deserializer);

       JavaReceiverInputDStream<JMSValue<String>> msgstream = sc.receiverStream( receiver );
```

## `JMSReceiver<Value>`
The `JMSReceiver<V>` connects to a JMS bus, subscribes to the desired message destination and marshals incoming messages to the datatype your Spark Streaming application expects to use. Once a message has been successfully converted into the desired datatype, it is stored to the Spark cluster and the message is acknowledged to the JMS message bus.

The `JMSReceiver<V>` constructor accepts all the necessary details to connect to a standard JMS message bus; it expects to use a JNDI `InitialContext` based upon these details to lookup a JMS `ConnectionFactory` which is used to create a JMS `Connection`. These details are as follows:

| Parameter | Description |
| --- | --- |
| brokerUrl | Connection-string for the JMS broker; typicall a protocol and IP-address+port like 'http://localhost:8080' or 'smf://192.168.56.101' |
| username | the username to authenticate to the JMS bus as |
| password | the credentials for the above username |
| destination | the JMS topic or queue the `JMSReceiver` will consume messages from |
| connection-factory | the name of the JMS connection-factory that will be used to create all JMS connections |
| deserializer | an instance of abstract class `JMSDeserializer<V>` that is used to marshal the message payloads into the desired internal type `V`; more on that below |

The `JMSReceiver<V>` is a subclass of `org.apache.spark.streaming.receiver.Receiver`. It is designed to be passed into the `JavaStreamingContext.receiverStream( )` method to construct a `JavaReceiverInputDStream` for stream handling. The `JMSReceiver<V>` is passed a generic type parameter `V` representing the payload data type your streaming program deals with. Note however, that the resulting `JavaReceiverInputDStream` has a slightly different type parameter, `JMSValue<V>` wrapping your type parameter.

## `JMSValue<V>`
JMS Messages are marshalled into instances of your application's internal data type `V`, then wrapped in instances of `JMSValue<V>`. The `JMSValue` wrapper associates other useful JMS data with the payload, like the destination topic or queue the message was consumed from. But before the `JMSReceiver<V>` can contruct the `JMSValue<V>` instance, it needs to marshal the native JMS message payload into an instance of the desired value type `V`.

## `JMSDeserializer<V>`
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
