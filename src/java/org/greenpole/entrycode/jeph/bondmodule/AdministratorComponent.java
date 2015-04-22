/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph.bondmodule;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.entrycode.jeph.mocks.AdministratorComponentQuery;
import org.greenpole.entrycode.jeph.mocks.AdministratorComponentQueryImpl;
import org.greenpole.hibernate.entity.*;
import org.greenpole.hibernate.query.*;
import org.greenpole.hibernate.query.factory.*;
import org.greenpole.logic.ClientCompanyComponentLogic;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.Notification;
import org.greenpole.util.properties.NotifierProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jephthah Sadare
 */
public class AdministratorComponent {

    private final AdministratorComponentQuery ac = new AdministratorComponentQueryImpl();;
    private static final Logger logger = LoggerFactory.getLogger(Administrator.class);

    public AdministratorComponent() {
    }

    public Response createAdministrator_Request(Login login, String authenticator, Administrator admin) {
        logger.info("request to create an administrator [{}] [{}]", "first_name", "last_name");

        Response res = new Response();
        NotificationWrapper wrapper;
        QueueSender queue;
        NotifierProperties prop;

        // check if necessary Adminstrator actual paramenters are entered
        // TODO:    1. validate other parameters
        //          2. check if the user exist
        if (!admin.getFirstName().isEmpty() && !admin.getLastName().isEmpty()) {
            if (admin.getFirstName().isEmpty()) {
                res.setRetn(200);
                res.setDesc("administrator account first name is empty");
                logger.info("administrator account is empty");

            } else {
                wrapper = new NotificationWrapper();
                prop = new NotifierProperties(AdministratorComponent.class);
                queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());

                logger.info("administrator does not exits - [{}] [{}]", admin.getFirstName(), admin.getLastName());

                List<Administrator> adminList = new ArrayList<>();
                adminList.add(admin);

                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription("Authenticate creation of administrator account, " + admin.getFirstName() + " " + admin.getLastName());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(adminList);
                res = queue.sendAuthorisationRequest(wrapper);
                logger.info("notification forwarded to queue - notification code: [{}]", wrapper.getCode());
            }
            //return res;
        }
        return res;
    }

    public Response createAdministrator_Authorise(String notificationCode) {
        Response res = new Response();
        logger.info("Administrator creation authorised - [{}]", notificationCode);
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<Administrator> adminList = (List<Administrator>) wrapper.getModel();
            Administrator adminModel = adminList.get(0);
            org.greenpole.hibernate.entity.Holder adminEntity = new org.greenpole.hibernate.entity.Holder();
            
            adminEntity.setFirstName(adminModel.getFirstName());
            adminEntity.setLastName(adminModel.getLastName());
            adminEntity.setMiddleName(adminModel.getMiddleName());

            boolean created = true;
            // determine if residential address was set or postal address
            if (adminModel.getAdministratorResidentialAddresses().isEmpty()) {
                // created = ac.(adminEntity, retrieveAdministratorPostalAddress(adminModel), retrieveAdministratorPhoneNumber(adminModel));
                // created = ac.createAdministratorAccount(adminEntity, retrieveAdministratorPostalAddress(adminModel), retrieveAdministratorPhoneNumber(adminModel));
            } else {
                // created = ac.createAdministratorAccount(adminEntity, retrieveAdministratorResidentialAddress(adminModel), retrieveAdministratorPhoneNumber(adminModel));
            }

            if (created) {
                res.setRetn(0);
                res.setDesc("Successful Persistence");
                return res;
            } else {
                res.setRetn(200);
                res.setDesc("An error occured persisting the data residential and postal addresses are empty");
                return res;
            }

        } catch (JAXBException ex) {
            // TODO: catch other types of exception
            res.setRetn(100);
            res.setDesc("error loading notification xml file. See error log");
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);
        } catch (Exception ex) {
            res.setRetn(100);
            res.setDesc("error. See error log");
            logger.info("error. See error log");
            logger.error("error - ", ex);
        }
        return res;

    }
    
    private AdministratorPostalAddress retrieveAdministratorPostalAddress(Administrator adminModel) {
        
        return new AdministratorPostalAddress();
    }
    
    private AdministratorResidentialAddress retrieveAdministratorResidentialAddress(Administrator adminModel) {
        
        return new AdministratorResidentialAddress();
    }
    
    private AdministratorPhoneNumber retrieveAdministratorPhoneNumber(Administrator adminModel) {
        
        return new AdministratorPhoneNumber();
    }

}
