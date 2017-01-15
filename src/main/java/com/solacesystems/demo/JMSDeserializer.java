package com.solacesystems.demo;

import scala.Serializable;
import scala.runtime.AbstractFunction1;

import javax.jms.Message;

/**
 * The JMSDeserializer is an instance of a Scala AbstractFunction1 which
 * accepts a javax.jms.Message input and returns an instance of the abstract
 * Output type.
 *
 * @param <Output> the type of the deserialized payload from the input javax.jms.Message instance.
 */
public abstract class JMSDeserializer<Output>
        extends AbstractFunction1<Message,JMSValue<Output>>
        implements Serializable {
}
