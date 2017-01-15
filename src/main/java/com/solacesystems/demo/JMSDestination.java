package com.solacesystems.demo;

import java.security.InvalidParameterException;

/**
 * JMSDestination being consumed by a JMSReceiver. JMS Destinations are either
 * Queues or Topics, no further distinction of temporary-queues or durable
 * topic subscribers is made.
 *
 * Includes a public internal ENUM for DestinationType. This type was created
 * because the JMS JMSDestination type offers no way to distinguish queues from
 * topics without attempting a down-cast.
 */
public final class JMSDestination implements scala.Serializable {
    public enum DestinationType {
        QUEUE,
        TOPIC
    }

    public JMSDestination(DestinationType type, String name) {
        if (type == null) throw new InvalidParameterException("Dest-type cannot be null");
        if (name == null) throw new InvalidParameterException("Dest-name cannot be null");
        _type = type;
        _name = name;
    }

    public static JMSDestination newQueue(String queueName) {
        return new JMSDestination(DestinationType.QUEUE, queueName);
    }

    public static JMSDestination newTopic(String topicString) {
        return new JMSDestination(DestinationType.TOPIC, topicString);
    }

    /**
     * The destination type (queue or topic) of this destination
     * @return enum value QUEUE or TOPIC
     */
    public DestinationType getType() {
        return _type;
    }

    /**
     * The name of this destination as a string; assumes topics and queues
     * can have the same name.
     * @return name of this destination as a String
     */
    public String getName() {
        return _name;
    }

    /**
     * Equals compares two destinations and distinguishes a queue and
     * topic with the same name from each other.
     * @param otherDest the destination being compared to this one
     * @return True if this dest has the same type and name as otherDest
     */
    @Override
    public boolean equals(Object otherDest) {
        if (this == otherDest) return true;
        if (otherDest == null || getClass() != otherDest.getClass()) return false;

        JMSDestination that = (JMSDestination) otherDest;

        if (_type != that._type) return false;
        return _name.equals(that._name);

    }

    @Override
    public int hashCode() {
        int result = _type.hashCode();
        result = 31 * result + _name.hashCode();
        return result;
    }

    private final DestinationType _type;
    private final String _name;
}
