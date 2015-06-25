/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.notifier.sender;

import java.io.IOException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.greenpole.entity.exception.ConfigNotFoundException;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.sms.TextSend;
import org.greenpole.util.properties.QueueConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Akinwale.Agbaje
 * Sends object messages to a stated queue for processing
 */
public class QueueSender {
    private static final QueueConfigProperties queueConfigProp = QueueConfigProperties.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(QueueSender.class);
    private Context context;
    private QueueConnectionFactory qconFactory;
    private QueueConnection qcon;
    private QueueSession qsession;
    private Queue queue;
    private MessageProducer producer;
    private MessageConsumer consumer;
    private final int TIME_OUT = 60000;
    
    /**
     * Initialises queue factory and prepares queue.
     * @param queueConnectionFactory the name of the queue factory
     * @param queueName the name of the queue to prepare
     */
    public QueueSender(String queueConnectionFactory, String queueName) {
        try {
            initialiseQueueFactory(queueConnectionFactory);
            prepareQueue(queueName);
        } catch (NamingException | ConfigNotFoundException | IOException | JMSException ex) {
            logger.info("Error thrown in QueueSender initialisation-preparation process. See error log");
            logger.error("An error(s) was thrown in the QueueSender", ex);
        }
    }
    
    /**
     * Loads queue configuration into initial context.
     * @return initial context
     * @throws NamingException property key-name, or queue factory name, or queue name is incorrect
     * @throws ConfigNotFoundException configuration file not found
     * @throws IOException error loading file into properties
     */
    private static Context getInitialContext() throws NamingException, ConfigNotFoundException, IOException {
        return new InitialContext(queueConfigProp);
    }
    
    /**
     * Initialises queue factory.
     * @param queueConnectionFactory the name of the queue factory to initialise
     * @throws NamingException property key-name is incorrect, or incorrect queue factory name
     * @throws ConfigNotFoundException configuration file not found
     * @throws IOException error loading file into properties
     */
    private void initialiseQueueFactory(String queueConnectionFactory) throws NamingException, ConfigNotFoundException, IOException {
        logger.info("initialising queue factory - [{}]", queueConnectionFactory);
        context = QueueSender.getInitialContext();
        qconFactory = (QueueConnectionFactory) context.lookup(queueConnectionFactory);
    }
    
    /**
     * Prepares the queue.
     * @param queueName the name of the queue to prepare
     * @throws JMSException error creating queue session
     * @throws NamingException incorrect queue name
     */
    private void prepareQueue(String queueName) throws JMSException, NamingException {
        logger.info("preparing queue - [{}]", queueName);
        qcon = qconFactory.createQueueConnection();
        qsession = qcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        queue = (Queue) context.lookup(queueName);
    }
    
    /**
     * Sends a rejection request to the rejecter notifier.
     * @param wrapper the notification rejection request
     * @return response from the rejecter notifier
     */
    public Response sendRejectionRequest(NotificationWrapper wrapper) {
        Response resp = new Response();
        try {
            ObjectMessage om = qsession.createObjectMessage(wrapper);
            return processRequest(om, resp, "rejection");
        } catch (JMSException ex) {
            logger.info("Error thrown in QueueSender initialisation-preparation process. See error log");
            logger.error("An error(s) was thrown in the QueueSender", ex);
            resp.setRetn(100);
            resp.setDesc("An error occurred while sending rejection request.\n"
                    + "Contact system administrator");
            return resp;
        }
    }
    
    /**
     * Sends an authorisation request to the authoriser notifier.
     * @param wrapper the notification request
     * @return response from the authoriser notifier
     */
    public Response sendAuthorisationRequest(NotificationWrapper wrapper) {
        Response resp = new Response();
        try {
            ObjectMessage om = qsession.createObjectMessage(wrapper);
            return processRequest(om, resp, "authorisation");
        } catch (JMSException ex) {
            logger.info("Error thrown in QueueSender initialisation-preparation process. See error log");
            logger.error("An error(s) was thrown in the QueueSender", ex);
            resp.setRetn(100);
            resp.setDesc("An error occurred while sending authorisation request.\n"
                    + "Contact system administrator");
            return resp;
        }
    }
    
    /**
     * Sends a text message request to the text notifier.
     * @param toSend the text request
     * @return response from the authoriser notifier
     */
    public Response sendTextMessageRequest(TextSend toSend){
        Response resp = new Response();
        try {
            ObjectMessage om = qsession.createObjectMessage(toSend);
            return processRequest(om, resp, "text message");
        } catch (JMSException ex) {
            logger.info("Error thrown in QueueSender initialisation-preparation process. See error log");
            logger.error("An error(s) was thrown in the QueueSender", ex);
            resp.setRetn(100);
            resp.setDesc("An error occurred while sending authorisation request.\n"
                    + "Contact system administrator");
            return resp;
        }
    }
    
    private Response processRequest(ObjectMessage om, Response resp, String whatFor) {
        try {
            producer = qsession.createProducer(queue);
            
            //create callback to ensure that reply from queue is captured
            TemporaryQueue tempqueue = qsession.createTemporaryQueue();
            om.setJMSReplyTo(tempqueue);
            
            qcon.start();
            
            logger.info("sending " + whatFor + " request to queue");
            producer.send(om);
            //producer.send(om, DeliveryMode.PERSISTENT, 9, 40000);
            consumer = qsession.createConsumer(tempqueue);
            Message callback = consumer.receive(TIME_OUT);
            
            if (callback != null) {
                logger.info("response received from queue - [{}]", queue.getQueueName());
                resp = (Response) ((ObjectMessage) callback).getObject();
            } else {
                resp.setRetn(100);
                resp.setDesc("Did not receive a response from the notification manager. Manager may be offline or exception was thrown.\n"
                        + "Contact system administrator.");
                logger.info("response not received from queue - [{}]", queue.getQueueName());
            }
            //close all connections
            closeConnections();
            return resp;
        } catch (JMSException ex) {
            logger.info("Error thrown in QueueSender initialisation-preparation process. See error log");
            logger.error("An error(s) was thrown in the QueueSender", ex);
            resp.setRetn(100);
            resp.setDesc("An error occurred while sending " + whatFor + " request.\n"
                    + "Contact system administrator");
            return resp;
        } catch (Exception ex) {
            logger.info("Error thrown in QueueSender. See error log");
            logger.error("An error(s) was thrown in the QueueSender", ex);
            resp.setRetn(100);
            resp.setDesc("An error occurred while sending " + whatFor + " request.\n"
                    + "Contact system administrator");
            return resp;
        }
    }
    
    /**
     * Closes all connections: consumer, producer, queue session, and queue connection.
     * @throws JMSException error closing any of the listed connections
     */
    private void closeConnections() throws JMSException {
        consumer.close();
        producer.close();
        qsession.close();
        qcon.close();
    }
}
