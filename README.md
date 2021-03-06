# spark-jms-connector
## Overview

This library provides a minimal Spark-style wrapping around a JMS Receiver so that JMS messages can be easily consumed from Spark Streaming processes and stored to the Spark cluster with the desired storage level.  Messages are acknowledged to the broker upon succesful storage, but not if any processing exception prevents storage. This allows the JMS message broker to redeliver the message.

For example:
```java
        final JMSReceiver<String> receiver = new JMSReceiver<>(
                StorageLevel.MEMORY_ONLY_SER_2(), JMSDeserializerFactory.createStringDeserializer()
	);

        final Hashtable<String, String> env = new Hashtable<>();
        env.put(InitialContext.INITIAL_CONTEXT_FACTORY,
                "com.solacesystems.jndi.SolJNDIInitialContextFactory");
        env.put(InitialContext.PROVIDER_URL, brokerURL);
        env.put(Context.SECURITY_PRINCIPAL, username);
        env.put(Context.SECURITY_CREDENTIALS, password);

        receiver.configure(env, connectionFactoryName, topic);
        final JavaReceiverInputDStream<JMSValue<String>> msgstream = sc.receiverStream(receiver);
```

## `JMSReceiver<ValueType>` Produces an Input Data Stream of `JMSValue<ValueType>`
The `JMSReceiver<ValueType>` connects to a JMS bus, subscribes to the desired message destination and marshals incoming messages to the datatype your Spark Streaming application expects to use. Once a message has been successfully converted into the desired datatype, it is stored to the Spark cluster and the message is acknowledged to the JMS message bus.

### Constructor
The `JMSReceiver<ValueType>` constructor accepts the following parameters:
| Parameter | Description |
| --- | --- |
| storageLevel | The Spark storage-level for this particular instance
| deserializer | an instance of abstract class `JMSDeserializer<ValueType>` that is used to marshal the message payloads into the desired internal type `ValueType`; more on that below |

This constructs the object in the Spark runtime.

### Configure
The `configure` function is where you configure the particular JMS bus you want to connect to. In particular, a Hashtable of properties for the preferred platform `javax.naming.InitialContext` construction must be populated and passed in as the first parameter, along with the JNDI name of a JMS connection-factory to look up for the connection, and a JMSDestination the `JMSReciever` will consume messages from. 

| Parameter | Description |
| --- | --- |
| jmsEnvironment | Configures the details around connecting to the JMS Message bus. |
| connectionFactory | Name of the JMS connection-factory defining how a JMS connection behaves; this is instantiated by the `InitialContext` and used to create connections. |
| destination | The destination to consume messages from; can be a Topic (non-persistent) or a Queue (persistent). |

The most significant configuration work is to populate the property-map for the JNDI InitialContext. That is what is used to connect to a JNDI store, look up your configured Connection-Factory, and use that to create a new JMS `Connection` to the message bus. The necessary details to connect to a standard JMS message bus can vary; some common details are as follows:

| Key | Value |
| --- | --- |
| InitialContext.PROVIDER_URL | Connection-string for the JMS broker; typicall a protocol and IP-address+port like 'http://localhost:8080' or 'smf://192.168.56.101' |
| Context.SECURITY_PRINCIPAL | The username to authenticate to the JMS bus as |
| Context.SECURITY_CREDENTIALS | The password for the above username |
| Other Params | JMS buses may vary, so it is possible that other `InitialContext` parameters are required; consult your JMS bus documentation. |

The `JMSReceiver<ValueType>` is a subclass of `org.apache.spark.streaming.receiver.Receiver`. It is designed to be passed into the `JavaStreamingContext.receiverStream( )` method to construct a `JavaReceiverInputDStream` for stream handling. The `JMSReceiver<ValueType>` is passed a generic type parameter `ValueType` representing the payload data type your streaming program deals with. Note however, that the resulting `JavaReceiverInputDStream` has a slightly different type parameter, `JMSValue<ValueType>` wrapping your type parameter.

## `JMSValue<ValueType>`
JMS Messages are marshalled into instances of your application's internal data type `ValueType`, then wrapped in instances of `JMSValue<ValueType>`. The `JMSValue` wrapper associates other useful JMS data with the payload, like the destination topic or queue the message was consumed from. But before the `JMSReceiver<ValueType>` can contruct the `JMSValue<ValueType>` instance, it needs to marshal the native JMS message payload into an instance of the desired value type `ValueType`.

## `JMSDeserializer<ValueType>`: `Message` -> `JMSValue<ValueType>`
To marshal message payloads into the target internal data type, the `JMSReceiver<ValueType>` applies a `JMSDeserializer<ValueType>` function to the message upon arrival. This `JMSDeserializer<ValueType>` is passed into the `JMSReceiver<ValueType>`'s constructor. Some samples of common deserialization routines are available in the `JMSDeserializerFactory` but writing deserialization logic will likely be the most significant code you write in order to leverage this connector library.

Deserializer functions are instances of Scala `Function1<T,R>` functors where the input type is always `javax.jms.Message` and the return type is a `JMSValue<ValueType>` wrapping `ValueType`, the datatype produced by your deserialization logic for your Spark streaming application. The bulk of the logic is expected to be around converting the payload content from the inbound JMS Message into the desired `ValueType` instance, and wrapping it into a `JMSValue<ValueType>`.  A helper abstract class `JMSDeserializer<Output>` hides the `JMSValue` wrapper and the input type parameter because it is only ever called within the `JMSReceiver<ValueType>` instance.

Here is an example String deserializer function:

```java
       return new JMSDeserializer<String>() {

            public JMSValue<String> apply(Message msg) {
                try {
                    if (msg instanceof TextMessage) {
                        TextMessage txtmsg = (TextMessage) msg;
                        return new JMSValue<>(msg.getJMSDestination(), txtmsg.getText());
                    }
                    return new JMSValue<>(msg.getJMSDestination(), msg.toString());
                }
                catch(JMSException jmse) {
                    // ... up to you ...
                }
                return null;
            }
        };
```

## A Caution About `Serializable`
Spark may move objects around the cluster, and in order to do that it may need to Serialize/Deserialize those objects. So the above classes implement Serializable where necessary. Beware that if you change internals of this library, things may compile just fine but will throw exceptions if Spark requires classes to be Serializable and you haven't supported that.

Instances from this library that are Serializable:
- `JMSDeserializer<Output>`
- `JMSValue<ValueType>`
- `JMSDestination`
- `JMSReceiver<ValueType>` via inherited `Receiver<ValueType>` class

## TODO List
The `JMSReceiver<ValueType>` internally sets the Spark StorageLevel to `MEMORY_ONLY_SER_2`. This should be parameterized to allow the user to choose.

The `JMSValue` wrapper class only provides the JMS destination details; other info from the JMS Message could be made available in this wrapper too, like important header fields, timestamps, etc.
