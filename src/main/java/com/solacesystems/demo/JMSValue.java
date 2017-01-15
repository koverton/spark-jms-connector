package com.solacesystems.demo;

import java.security.InvalidParameterException;

/**
 * Wraps the generic value in a pair with the JMSDestination it was consumed from.
 *
 * This should not be sub-classed.
 *
 * @param <V> Type of the internal value passed out from the JMSReceiver.
 */
public final class JMSValue<V> implements scala.Serializable {
    /**
     * Constructs a JMSValue from a destination and value for internal consumption.
     *
     * @param destination the JMS destination (either Queue or Topic) this message was consumed from
     * @param value the internal value after deserialization from the JMS Message payload for internal consumption
     */
    public JMSValue(JMSDestination destination, V value) {
        if (destination == null) throw new InvalidParameterException("JMSDestination cannot be null");
        if (value == null) throw new InvalidParameterException("Value cannot be null");
        _dest = destination;
        _val = value;
    }

    /**
     * The JMS Destination (either Queue or Topic) this message was consumed from
     *
     * @return the type and name of the JMS destination
     */
    public JMSDestination getDestination() {
        return _dest;
    }

    /**
     * The internal value after deserialization from the JMS Message payload
     *
     * @return the deserialized value
     */
    public V getValue() {
        return _val;
    }

    @Override
    public String toString() {
        return _val.toString();
    }

    private final JMSDestination _dest;
    private final V _val;
}
