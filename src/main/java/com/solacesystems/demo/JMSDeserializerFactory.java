package com.solacesystems.demo;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import static com.solacesystems.demo.JMSMessageUtility.toDest;

/**
 * A sample JMS Deserializer implementation. JMS deserializers
 */
public class JMSDeserializerFactory {

    public static final JMSDeserializer<String>
    createStringDeserializer() {
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
                    jmse.printStackTrace();
                }
                return null;
            }
        };
    }

    public static final JMSDeserializer<Message>
    createPassthroughDeserializer() {
        return new JMSDeserializer<Message>() {

            public JMSValue<Message> apply(Message msg) {
                try {
                    return new JMSValue<>(msg.getJMSDestination(), msg);
                }
                catch(JMSException jmse) {
                    jmse.printStackTrace();
                }
                return null;
            }
        };
    }
}
