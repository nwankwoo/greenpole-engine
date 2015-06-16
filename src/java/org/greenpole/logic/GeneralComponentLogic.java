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
import org.greenpole.util.properties.NotificationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Akin
 * Business Requirements implementation that affects all components.
 */
public class GeneralComponentLogic {
    private final GeneralComponentQuery gq = ComponentQueryFactory.getGeneralComponentQuery();
    private final NotificationProperties notificationProp = new NotificationProperties(GeneralComponentLogic.class);
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
     * @return response to the reject notification request
     */
    public Response rejectNotification(Login login, String notificationCode) {
        Response resp = new Response();
        org.greenpole.util.Notification notification = new org.greenpole.util.Notification();
        logger.info("request to reject notification, invoked by [{}]", login.getUserId());
        
        try {
            if (gq.checkValidUser(login.getUserId())) {
                notification.markRejected(notificationCode);
                resp.setRetn(0);
                resp.setDesc("successful");
                logger.info("Notification rejected - [{}]", login.getUserId());
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
