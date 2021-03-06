/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.security;

import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.hibernate.query.GeneralComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.util.Notification;
import org.greenpole.util.properties.NotificationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Akinwale Agbaje
 * Checks the notification and logged in user against certain security rules.
 * Should be used in web service layer, under authorisation
 */
public class SecurityCheck {

    /**
     * Checks the notification and logged in user against certain security rules.
     * Rule 1: notification code must have a record in the database and an xml file
     * Rule 2: notification code can only be authorised by the user it was sent to
     * @param login the logged in user (typically the authoriser)
     * @param notificationCode the notification code
     * @param resp response to the check
     * @return true, if notification fails. Otherwise, false
     */
    public static boolean securityFailChecker(Login login, String notificationCode, Response resp) {
        Logger logger = LoggerFactory.getLogger(SecurityCheck.class);
        GeneralComponentQuery gq = ComponentQueryFactory.getGeneralComponentQuery();
        NotificationProperties notificationProp = NotificationProperties.getInstance();
        Notification notification = new Notification();
        //notification code must exist
        if (!gq.checkNotification(notificationCode)) {
            resp.setRetn(900);
            resp.setDesc("Illegal request. This notification code does not exist.");
            logger.info("Illegal request. This notification code does not exist - [{}]", login.getUserId());
            return true;
        }
        
        //notification code must have xml file and db record
        if (!notification.checkFile(notificationProp.getNotificationLocation(), notificationCode)) {
            if (gq.checkNotification(notificationCode)) {
                notification.writeOffNotification(notificationCode);
                resp.setRetn(900);
                resp.setDesc("The notification file has been tampered with. System will write off notification. Send a new request.");
                logger.info("The notification file has been tampered with. System will write off notification. Send a new request - [{}]", login.getUserId());
                return true;
            }
            resp.setRetn(900);
            resp.setDesc("Illegal notification code sent.");
            logger.info("Illegal notification code sent - [{}]", login.getUserId());
            return true;
        }
        
        //notification code must be tied to logged in user
        if (!gq.checkNotificationAgainstUser(login.getUserId(), notificationCode)) {
            resp.setRetn(900);
            resp.setDesc("Notification code does not belong to logged in user.");
            logger.info("Notification code does not belong to logged in user - [{}]", login.getUserId());
            return true;
        }
        
        //notification code must not be tied to both the sender and receiver
        if (gq.checkFromToSame(login.getUserId(), notificationCode)) {
            resp.setRetn(900);
            resp.setDesc("Illegal entry. Notification code cannot have its sender and receiver as the same user.");
            logger.info("Illegal entry. Notification code cannot have its sender and receiver as the same user - [{}]", login.getUserId());
            return true;
        }
        return false;
    }
    
}
