/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

import java.io.File;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.security.Login;
import org.greenpole.util.Date;

/**
 *
 * @author Akinwale.Agbaje
 * Responsible for common notification functions.
 */
public class Notification {

    /**
     * Creates a notification code from the user's login details.
     * @param login the user's login details
     * @return the notification code
     */
    public static String createCode(Login login) {
        Date date = new Date();
        Manipulator manipulate = new Manipulator();
        String[] names = manipulate.separateNameFromEmail(login.getUserId());
        return names[0] + "_" + names[1] + date.getDateTime();
    }
    
    /**
     * Loads the notification file according to the notification code.
     * This method will only load notification xml files that can be mapped 
     * to the {@link NotificationWrapper} class.
     * @param notificationCode the notification code
     * @return the notification wrapper containing contents of the xml file
     * @throws JAXBException if xml file cannot be found, or file does not map to
     * {@link NotificationWrapper}
     */
    public static NotificationWrapper loadNotificationFile(String notificationCode) throws JAXBException {
        File file = new File(notificationCode + ".xml");
        JAXBContext jaxbContext = JAXBContext.newInstance(NotificationWrapper.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        NotificationWrapper wrapper = (NotificationWrapper) jaxbUnmarshaller.unmarshal(file);
        return wrapper;
    }
}
