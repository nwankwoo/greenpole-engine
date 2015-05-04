/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author user
 * Sends object messages to a stated queue for processing
 */
public class QueueSender {
    private static final Logger logger = LoggerFactory.getLogger(QueueSender.class);
    private Context context;
    private QueueConnectionFactory qconFactory;
    private QueueConnection qcon;
    private QueueSession qsession;
    private Queue queue;
    private MessageProducer producer;
    private MessageConsumer consumer;
    private final int TIME_OUT = 10000;
    
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
        String config_file = "queue_config.properties";
        Properties properties = new Properties();
        InputStream input = QueueSender.class.getClassLoader().getResourceAsStream(config_file);
        logger.info("Loading configuration file - {}", config_file);
        
        if (input == null) {
            logger.info("Failure to load configuration file - {}", config_file);
            throw new ConfigNotFoundException("queue_config.properties file missing from classpath");
        }
        
        properties.load(input);
        logger.info("Loaded configuration file - {}", config_file);
        
        return new InitialContext(properties);
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
     * Sends an authorisation request to the authoriser notifier.
     * @param wrapper the notification request
     * @return response from the authoriser notifier
     */
    public Response sendNotificationRequest(NotificationWrapper wrapper) {
        Response resp = new Response();
        try {
            ObjectMessage om = qsession.createObjectMessage(wrapper);
            producer = qsession.createProducer(queue);
            
            //create callback to ensure that reply from queue is captured
            TemporaryQueue tempqueue = qsession.createTemporaryQueue();
            om.setJMSReplyTo(tempqueue);
            
            qcon.start();
            
            producer.send(om);
            consumer = qsession.createConsumer(tempqueue);
            Message callback = consumer.receive(TIME_OUT);
            
            if (callback != null) {
                logger.info("response received from queue - [{}]", queue.getQueueName());
                resp = (Response) ((ObjectMessage) callback).getObject();
            } else {
                resp.setRetn(100);
                resp.setDesc("Did not receive a response from the notification manager.\n"
                        + "It is possible the manager is offline. Contact system administrator.");
                logger.info("response not received from queue - [{}]", queue.getQueueName());
            }
            //close all connections
            closeConnections();
            return resp;
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
