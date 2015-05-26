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
        NotificationProperties notificationProp = new NotificationProperties(SecurityCheck.class);
        Notification notification = new Notification();
        if (!notification.checkFile(notificationProp.getNotificationLocation(), notificationCode)) {
            if (gq.checkNotification(notificationCode)) {
                notification.writeOffNotification(notificationCode);
                resp.setRetn(301);
                resp.setDesc("The notification file has been tampered with. System will write off notification. Send a new request.");
                logger.info("The notification file has been tampered with. System will write off notification. Send a new request - [{}]", login.getUserId());
                return true;
            }
            resp.setRetn(301);
            resp.setDesc("Illegal notification code sent.");
            logger.info("Illegal notification code sent - [{}]", login.getUserId());
            return true;
        }
        
        if (!gq.checkNotificationAgainstUser(login.getUserId(), notificationCode)) {
            resp.setRetn(301);
            resp.setDesc("Notification code does not belong to logged in user.");
            logger.info("Notification code does not belong to logged in user - [{}]", login.getUserId());
            return true;
        }
        return false;
    }
    
}
