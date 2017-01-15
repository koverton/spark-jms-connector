package com.solacesystems.demo;

import javax.jms.*;

class JMSMessageUtility {

    static JMSDestination toDest(Destination jmsdest) throws JMSException {
        if (jmsdest instanceof Queue) {
            Queue queue = (Queue) jmsdest;
            return new JMSDestination(JMSDestination.DestinationType.QUEUE, queue.getQueueName());
        }
        Topic topic = (Topic) jmsdest;
        return new JMSDestination(JMSDestination.DestinationType.TOPIC, topic.getTopicName());
    }

    static Destination fromDest(Session session, JMSDestination dest) throws JMSException {
        if (dest.getType() == JMSDestination.DestinationType.QUEUE) {
            return session.createQueue(dest.getName());
        }
        return session.createTopic(dest.getName());
    }
}
