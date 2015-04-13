/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.logic;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.model.clientcompany.ClientCompany;
import org.greenpole.entity.model.clientcompany.ClientCompanyAddress;
import org.greenpole.entity.model.clientcompany.ClientCompanyEmailAddress;
import org.greenpole.entity.model.clientcompany.ClientCompanyPhoneNumber;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.security.Login;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.response.Response;
import org.greenpole.hibernate.entity.ClientCompanyAddressId;
import org.greenpole.hibernate.entity.ClientCompanyEmailAddressId;
import org.greenpole.hibernate.entity.ClientCompanyPhoneNumberId;
import org.greenpole.hibernate.entity.Depository;
import org.greenpole.hibernate.entity.NseSector;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.Notification;
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
            wrapper.setCode(Notification.createCode(login));
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
     * Processes request to create a client company that has already been saved
     * as a notification file, according to the specified notification code.
     * @param notificationCode the notification code
     * @return response to the client company creation request
     */
    public Response createClientCompany_Authorise(String notificationCode) {
        Response resp = new Response();
        
        logger.info("client company creation authorised - [{}]", notificationCode);
        try {
            ClientCompany ccModel = clientCompanyCreationMain(notificationCode);
            logger.info("client company created - [{}]", ccModel.getName());
            
            int cc_id = cq.getClientCompanyId(ccModel.getName());
            
            createClientCompanyAddress(ccModel, cc_id);
            logger.info("address added for company - ", ccModel.getName());
            
            createClientCompanyEmailAddress(ccModel, cc_id);
            logger.info("address added for company - ", ccModel.getName());
            
            createClientCompanyPhoneNumber(ccModel, cc_id);
            logger.info("address added for company - ", ccModel.getName());
            
            resp.setRetn(0);
            resp.setDesc("Successful");
            
            return resp;
        } catch (JAXBException ex) {
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);
            
            resp.setRetn(201);
            resp.setDesc("Unable to create client company from authorisation. Contact System Administrator");
            
            return resp;
        }
    }

    /**
     * Creates a new phone number for the client company.
     * @param ccModel the client company model (not to be confused with the client company hibernate entity)
     * @param cc_id the client company id
     */
    private void createClientCompanyPhoneNumber(ClientCompany ccModel, int cc_id) {
        org.greenpole.hibernate.entity.ClientCompanyPhoneNumber phone = new org.greenpole.hibernate.entity.ClientCompanyPhoneNumber();
        List<ClientCompanyPhoneNumber> phoneList = ccModel.getPhoneNumbers();
        
        for (ClientCompanyPhoneNumber ph : phoneList) {
            //set id
            ClientCompanyPhoneNumberId phoneId = new ClientCompanyPhoneNumberId(cc_id, ph.getPhoneNumber());
            //put id in phone
            phone.setId(phoneId);
            //set other phone variables
            phone.setIsPrimary(ph.isPrimaryPhoneNumber());
            
            //add phone number to the database
            cq.createPhoneNumber(phone);
        }
    }

    /**
     * Creates a new email address for the client company.
     * @param ccModel the client company model (not to be confused with the client company hibernate entity)
     * @param cc_id the client company id
     */
    private void createClientCompanyEmailAddress(ClientCompany ccModel, int cc_id) {
        org.greenpole.hibernate.entity.ClientCompanyEmailAddress email = new org.greenpole.hibernate.entity.ClientCompanyEmailAddress();
        List<ClientCompanyEmailAddress> emailList = ccModel.getEmailAddresses();
        
        for (ClientCompanyEmailAddress em : emailList) {
            //set id
            ClientCompanyEmailAddressId emailId = new ClientCompanyEmailAddressId(cc_id, em.getEmailAddress());
            //put id in email
            email.setId(emailId);
            //set other email variables
            email.setIsPrimary(em.isPrimaryEmail());
            
            //add email to the database
            cq.createEmailAddress(email);
        }
    }

    /**
     * Creates a new address for the client company.
     * @param ccModel the client company model (not to be confused with the client company hibernate entity)
     * @param cc_id the client company id
     */
    private void createClientCompanyAddress(ClientCompany ccModel, int cc_id) {
        org.greenpole.hibernate.entity.ClientCompanyAddress address = new org.greenpole.hibernate.entity.ClientCompanyAddress();
        List<ClientCompanyAddress> addressList = ccModel.getAddresses();
        
        for (ClientCompanyAddress addy : addressList) {
            //set id
            ClientCompanyAddressId addressId = new ClientCompanyAddressId(cc_id, addy.getAddressLine1(), addy.getState(), addy.getCountry());
            //put id in address
            address.setId(addressId);
            //set other address variables
            address.setIsPrimary(addy.isPrimaryAddress());
            address.setAddressLine2(addy.getAddressLine2());
            address.setAddressLine3(addy.getAddressLine3());
            address.setAddressLine4(addy.getAddressLine4());
            address.setCity(addy.getCity());
            address.setPostCode(addy.getPostCode());
            
            //add the address to the database
            cq.createAddress(address);
        }
    }

    /**
     * Creates a new client company according to the specified notification code.
     * @param notificationCode the notification code
     * @return the client company object
     * @throws JAXBException if error occurs while loading xml file for notification code
     */
    private ClientCompany clientCompanyCreationMain(String notificationCode) throws JAXBException {
        //get client company model from wrapper
        NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
        ClientCompany ccModel = (ClientCompany) wrapper.getModel();
        //instantiate required hibernate entities
        org.greenpole.hibernate.entity.ClientCompany cc_main = new org.greenpole.hibernate.entity.ClientCompany();
        Depository depository = new Depository();
        NseSector nseSector = new NseSector();
        //get values from client company model and insert into client company hibernate entity
        cc_main.setName(ccModel.getName());
        cc_main.setCeo(ccModel.getCeo());
        cc_main.setCode(ccModel.getCode());
        cc_main.setSecretary(ccModel.getSecretary());
        //set depository id in object before setting object in hibernate client company object
        depository.setId(ccModel.getDepositoryId());
        cc_main.setDepository(depository);
        //set nse sector id in object before setting object in hibernate client company object
        nseSector.setId(ccModel.getNseSectorId());
        cc_main.setNseSector(nseSector);
        //client company must be persisted first, and then its id retrieved before addresses, emails, and phone numbers
        //can be added
        cq.create(cc_main);
        return ccModel;
    }
}
