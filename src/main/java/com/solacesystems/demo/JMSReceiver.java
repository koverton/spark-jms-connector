package com.solacesystems.demo;

import org.apache.spark.storage.StorageLevel;
import org.apache.spark.streaming.receiver.Receiver;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;

import static com.solacesystems.demo.JMSMessageUtility.fromDest;

/**
 * A generic JMS MessageConsumer that binds to a JMS message-bus, consumes messages,
 * then stores the deserialized messages to the Spark subsystem.
 *
 * @param <V> Internal type that message payloads must conform to for consumption by Spark apps.
 */
public class JMSReceiver<V> extends Receiver<JMSValue<V>> implements MessageListener {
    private static final long serialVersionUID = 1L;
    private static final String SOLJMS_INITIAL_CONTEXT_FACTORY =
            "com.solacesystems.jndi.SolJNDIInitialContextFactory";

    /**
     * Constructor for the JMS receiver with minimal spark storage info and type-directed serialization function.
     *
     * @param deserializer the deserializer applied to JMS messages as they arrive before storing them to Spark
     */
    public JMSReceiver(StorageLevel storageLevel,
                       JMSDeserializer<V> deserializer) {
        super(storageLevel);
        _deserializer = deserializer;
    }

    /**
     * Configures the details around connecting to the JMS Message bus. the expected parameters to the JMS
     * environment are variable according to the JMS platform being used. Some typical parameters can include
     * <li> the URL of the JMS message-bus to connect to</li>
     * <li> the username to authenticate to the JMS message-bus with</li>
     * <li> the password for authentication to the JMS message-bus</li>
     * @param jmsEnvironment a hashtable containing all the properties for initialization of the JMS platform;
     * @param connectionFactory the JMS connection-factory defining how a JMS connection behaves
     * @param destination the topic or queue to consume messages from
     */
    public void configure(Hashtable<String, String> jmsEnvironment, String connectionFactory, JMSDestination destination) {
        _jmsEnv = jmsEnvironment;
        _cfName = connectionFactory;
        _destination = destination;
    }

    @Override
    public void onStart() {
        try {
            Context ctx = new InitialContext(_jmsEnv);
            ConnectionFactory factory = (ConnectionFactory) ctx.lookup(_cfName);
            _connection = factory.createConnection();
            Session session = _connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            MessageConsumer consumer = session.createConsumer(fromDest(session, _destination));
            consumer.setMessageListener(this);
            _connection.start();
        }
        catch(NamingException ne) {
            ne.printStackTrace();
        }
        catch(JMSException je) {
            je.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        try {
            _connection.close();
        }
        catch(JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(Message msg) {
        try {
            System.out.println("GOT A MESSAGE!");
            store(_deserializer.apply(msg));
            msg.acknowledge();
        }
        catch(JMSException e) {
            e.printStackTrace();
        }
    }

    private Hashtable<String, String> _jmsEnv;
    private String _cfName;
    private JMSDestination _destination;
    private Connection _connection;
    private JMSDeserializer<V> _deserializer;
}
