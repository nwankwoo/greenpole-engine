/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.logic;

import java.util.ArrayList;
import java.util.List;
import org.greenpole.entity.model.clientcompany.ClientCompany;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.security.Login;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.util.Date;
import org.greenpole.util.Manipulator;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.response.Response;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.properties.NotifierProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Akinwale Agbaje
 * @version 1.0
 * Business requirement implementations to do with client companies
 */
public class ClientCompanyComponentLogic {
    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();
    private static final Logger logger = LoggerFactory.getLogger(ClientCompanyComponentLogic.class);
    
    /**
     * Processes request to create a new client company.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param cc the client company to be created
     * @return response to the client company creation request 
     */
    public Response createClientCompany_Request(Login login, String authenticator, ClientCompany cc) {
        logger.info("request to create the client company [{}] from [{}]", cc.getName(), login.getUserId());
        
        Response resp = new Response();
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;
        
        //check if client company exists
        if (!cq.checkClientCompany(cc.getName())) {
            wrapper = new NotificationWrapper();
            prop = new NotifierProperties(ClientCompanyComponentLogic.class);
            qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), 
                    prop.getAuthoriserNotifierQueueName());
            
            logger.info("client company does not exist - [{}]", cc.getName());
            List<ClientCompany> cclist = new ArrayList<>();
            cclist.add(cc);
            //wrap client company object in notification object, along with other information
            wrapper.setCode(createNotificationCode(login));
            wrapper.setDescription("Authenticate creation of the client company, " + cc.getName());
            wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
            wrapper.setFrom(login.getUserId());
            wrapper.setTo(authenticator);
            wrapper.setModel(cclist);
            
            resp = qSender.sendAuthorisationRequest(wrapper);
            logger.info("notification fowarded to queue - notification code: [{}]", wrapper.getCode());
            return resp;
        }
        resp.setRetn(200);
        resp.setDesc("Client company already exists and so cannot be created.");
        logger.info("client company exists so cannot be created - [{}]: [{}]", cc.getName(), resp.getRetn());
        return resp;
    }

    /**
     * Creates a notification code from the user's login details.
     * @param login the user's login details
     * @return the notification code
     */
    private String createNotificationCode(Login login) {
        Date date = new Date();
        Manipulator manipulate = new Manipulator();
        String[] names = manipulate.separateNameFromEmail(login.getUserId());
        return names[0] + "_" + names[1] + date.getDateTime();
    }
}
