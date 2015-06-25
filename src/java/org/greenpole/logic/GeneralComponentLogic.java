/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.hibernate.entity.Notification;
import org.greenpole.hibernate.query.GeneralComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.properties.NotificationProperties;
import org.greenpole.util.properties.NotifierProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Akin
 * Business Requirements implementation that affects all components.
 */
public class GeneralComponentLogic {
    private final GeneralComponentQuery gq = ComponentQueryFactory.getGeneralComponentQuery();
    private final NotificationProperties notificationProp = NotificationProperties.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(GeneralComponentLogic.class);
    
    /**
     * Request to get all notifications for logged in user
     * @param login the user's login details
     * @return response to the get user notifications request
     */
    public Response getReceiverNotifications_Request(Login login) {
        Response resp = new Response();
        org.greenpole.util.Notification notification = new org.greenpole.util.Notification();
        logger.info("request to get all receiver's notification, invoked by [{}]", login.getUserId());
        
        try {
            if (gq.checkValidUser(login.getUserId())) {
                List<Notification> notifi_hib_list = gq.getNotificationsForReceiver(login.getUserId());
                List<NotificationWrapper> wrappers = new ArrayList<>();
                
                for (Notification notif : notifi_hib_list) {
                    NotificationWrapper wrapper;
                    boolean fileExists = notification.checkFile(notificationProp.getNotificationLocation(), notif.getFileName());
                    if (fileExists) {
                        wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notif.getFileName());
                        wrappers.add(wrapper);
                    } else {
                        notification.writeOffNotification(notif.getFileName());
                        resp.setRetn(400);
                        resp.setDesc("The notification file has been tampered with. System will write off notification. Send a new request.");
                        logger.info("The notification file has been tampered with. System will write off notification. Send a new request - [{}]",
                                login.getUserId());
                        return resp;
                        
                    }
                }
                
                if (wrappers.size() > 0) {
                    resp.setRetn(0);
                    resp.setDesc("successful");
                    resp.setBody(wrappers);
                    logger.info("All notifications successfully retrieved - [{}]", login.getUserId());
                    return resp;
                } else {
                    resp.setRetn(400);
                    resp.setDesc("User has no notifications");
                    resp.setBody(wrappers);
                    logger.info("User has no notifications - [{}]", login.getUserId());
                    return resp;
                }
            }
            resp.setRetn(400);
            resp.setDesc("Illegal user.");
            logger.info("Illegal user - [{}]", login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            logger.info("error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(98);
            resp.setDesc("Unable to get receiver notifications. Contact System Administrator");
            
            return resp;
        } catch (Exception ex) {
            logger.info("error getting receiver notification. See error log - [{}]", login.getUserId());
            logger.error("error getting receiver notification - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to get receiver notification. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Request to reject notification.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @param rejectionReason authoriser's reason for rejection
     * @return response to the reject notification request
     */
    public Response rejectNotification(Login login, String notificationCode, String rejectionReason) {
        Response resp = new Response();
        org.greenpole.util.Notification notification = new org.greenpole.util.Notification();
        logger.info("request to reject notification, invoked by [{}]", login.getUserId());
        
        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;
            
            if (gq.checkValidUser(login.getUserId())) {
                wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
                prop = NotifierProperties.getInstance();
                qSender = new QueueSender(prop.getNotifierQueueFactory(), prop.getRejectNotifierQueueName());
                
                wrapper.setRejectionReason(rejectionReason);
                logger.info("notification rejection fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                resp = qSender.sendRejectionRequest(wrapper);
                return resp;
            }
            resp.setRetn(400);
            resp.setDesc("Illegal user.");
            logger.info("Illegal user - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error marking notification as rejected. See error log - [{}]", login.getUserId());
            logger.error("error marking notification as rejected - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to mark notification as rejected. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Request to invalidate a notification according to the specified notification code.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the invalidate notification request
     */
    public Response writeOffNotification(Login login, String notificationCode) {
        Response resp = new Response();
        org.greenpole.util.Notification notification = new org.greenpole.util.Notification();
        logger.info("request to write-off notification, invoked by [{}]", login.getUserId());
        try {
            if (gq.checkValidUser(login.getUserId())) {
                notification.writeOffNotification(notificationCode);
                resp.setRetn(0);
                resp.setDesc("Successful");
                return resp;
            }
            resp.setRetn(400);
            resp.setDesc("Illegal user.");
            logger.info("Illegal user - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error marking notification as rejected. See error log - [{}]", login.getUserId());
            logger.error("error marking notification as rejected - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to mark notification as rejected. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Request to resend a notification that was rejected.
     * @param login the user's login details
     * @param wrapper the notification wrapper to resend
     * @return response to the resend notification request
     */
    public Response resendNotification(Login login, NotificationWrapper wrapper) {
        Response resp = new Response();
        logger.info("request to resend notification, invoked by [{}]", login.getUserId());
        org.greenpole.util.Notification notification = new org.greenpole.util.Notification();
        try {
            QueueSender qSender;
            NotifierProperties prop;
            
            if (gq.checkValidUser(login.getUserId())) {
                prop = NotifierProperties.getInstance();
                qSender = new QueueSender(prop.getNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
                
                notification.writeOffNotification(wrapper.getCode());//get rid of old notification code
                wrapper.setCode(notification.createCode(login));
                
                logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                resp = qSender.sendAuthorisationRequest(wrapper);
                return resp;
            }
            resp.setRetn(400);
            resp.setDesc("Illegal user.");
            logger.info("Illegal user - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error marking notification as rejected. See error log - [{}]", login.getUserId());
            logger.error("error marking notification as rejected - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to mark notification as rejected. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
}
