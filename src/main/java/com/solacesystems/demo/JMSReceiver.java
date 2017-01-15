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
     * Constructor for the JMS receiver with all the info to connect to a JMS message-bus.
     *
     * @param brokerURL the URL of the JMS message-bus to connect to
     * @param username the username to authenticate to the JMS message-bus with
     * @param password the password for authentication to the JMS message-bus
     * @param destination the JMS destination to consume messages from, either a Topic or Queue
     * @param connectionFactory the JMS connection-factory defining how a JMS connection behaves
     * @param deserializer the deserializer applied to JMS messages as they arrive before storing them to Spark
     */
    public JMSReceiver(String brokerURL, String username, String password,
                       JMSDestination destination, String connectionFactory,
                       JMSDeserializer<V> deserializer) {
        super(StorageLevel.MEMORY_ONLY_SER_2());
        _url = brokerURL;
        _userName = username;
        _password = password;
        _destination = destination;
        _cfName = connectionFactory;
        _deserializer = deserializer;
    }

    @Override
    public void onStart() {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(InitialContext.INITIAL_CONTEXT_FACTORY, SOLJMS_INITIAL_CONTEXT_FACTORY);
        env.put(InitialContext.PROVIDER_URL, _url);
        env.put(Context.SECURITY_PRINCIPAL, _userName);
        env.put(Context.SECURITY_CREDENTIALS, _password);
        try {
            Context ctx = new InitialContext(env);
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

    private String _cfName;
    private String _url;
    private String _userName;
    private String _password;
    private JMSDestination _destination;
    private Connection _connection;
    private JMSDeserializer<V> _deserializer;
}
