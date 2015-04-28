/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.model.holder.HolderBondAccount;
import org.greenpole.entity.model.holder.HolderCompanyAccount;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.entrycode.emmanuel.model.HolderChanges;
import org.greenpole.entrycode.emmanuel.model.InitialPublicOffer;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.Notification;
import org.greenpole.util.properties.NotifierProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author user
 */
public class ShareholderAndBondholderNubanLogic {

    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();//expecting IPO query and IPO query factory
    private static final Logger logger = LoggerFactory.getLogger(InitialPublicOffer.class);
    private final HibernatDummyQuerInterface hd = HibernateDummyQueryFactory.getHibernateDummyQuery();

    public Response addShareholderNubanAccountNumber_request(Login login, String authenticator, HolderCompanyAccount holderCompAccount) {
        Response resp = new Response();
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;
        try {
            //boolean nubanNumber = hd.checkHolderNubanNumber(holderCompAccount.getNubanAccount());
            org.greenpole.hibernate.entity.Holder holder = hd.retrieveHolderObject(holderCompAccount.getHolderId());
            logger.info("Create of NUBAN account number [{}] for holder [{}]", holderCompAccount.getNubanAccount(), holder.getFirstName() + " " + holder.getLastName());
            if (holderCompAccount.getNubanAccount().isEmpty()) {
                wrapper = new NotificationWrapper();
                prop = new NotifierProperties(ShareholderAndBondholderNubanLogic.class);
                qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                        prop.getAuthoriserNotifierQueueName());
                List<HolderCompanyAccount> holderAccountList = new ArrayList();

                holderAccountList.add(holderCompAccount);
                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription("Authenticate creation of NUBAN account number " + holderCompAccount.getNubanAccount() + " for holder" + holder.getFirstName() + " " + holder.getLastName() + " by user " + login.getUserId());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(holderAccountList);
                resp = qSender.sendAuthorisationRequest(wrapper);
                resp.setRetn(0);
                resp.setDesc("Successful");
            } else {
                resp.setRetn(200);
                resp.setDesc("Unable to create new NUBAN account number for holder because NUBAN account number already exists");
            }
        } catch (Exception e) {
            resp.setRetn(2001);
            resp.setDesc("Unable to create NUBAN number for holder, please see error log for details");
            logger.info("Error in creating NUBAN number : " + e);
        }
        return resp;
    }

    public Response addShareholderNubanAccountNumber_authorise(Login login, String notificationCode) {
        Response resp = new Response();
        logger.info("NUBAN account number creation authorised - [{}]", notificationCode);
        logger.info("NUBAN number persisted by user: " + login.getUserId());
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<HolderCompanyAccount> holderCompAccList = (List<HolderCompanyAccount>) wrapper.getModel();
            HolderCompanyAccount holderModel = holderCompAccList.get(0);
            org.greenpole.hibernate.entity.HolderCompanyAccount holderCompanyAccount = new org.greenpole.hibernate.entity.HolderCompanyAccount();
            holderCompanyAccount.setNubanAccount(holderModel.getNubanAccount());
            hd.createNubanAccount(holderCompanyAccount);
            resp.setRetn(0);
            resp.setDesc("NUBAN number successfully created");
        } catch (JAXBException ex) {
            resp.setRetn(200);
            resp.setDesc("NUBAN number was not created due to error, please see error log");
            logger.error("Failed to create NUBAN number due to: " + ex);
        }
        return resp;
    }

    public Response addBondholderNubanAccountNumber_request(Login login, String authenticator, HolderBondAccount holderBondAcc) {
        Response resp = new Response();
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;
        try {
            //boolean nubanNumber = hd.checkHolderNubanNumber(holderBondAcc.getNubanAccount());
            org.greenpole.hibernate.entity.Holder holder = hd.retrieveHolderObject(holderBondAcc.getHolderId());
            logger.info("Creation of NUBAN account number [{}] for bond holder [{}] by user ", holderBondAcc.getNubanAccount(), holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
            if (holderBondAcc.getNubanAccount().isEmpty()) {
                wrapper = new NotificationWrapper();
                prop = new NotifierProperties(ShareholderAndBondholderNubanLogic.class);
                qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                        prop.getAuthoriserNotifierQueueName());
                List<HolderBondAccount> bondHolderAccountList = new ArrayList();

                bondHolderAccountList.add(holderBondAcc);
                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription("Authenticate creation of NUBAN account number " + holderBondAcc.getNubanAccount() + " for holder" + holder.getFirstName() + " " + holder.getLastName() + " by user " + login.getUserId());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(bondHolderAccountList);
                resp = qSender.sendAuthorisationRequest(wrapper);
                resp.setRetn(0);
                resp.setDesc("Successful");
            } else {
                resp.setRetn(200);
                resp.setDesc("Unable to create new NUBAN account number for bond holder because NUBAN account number already exists");
            }
        } catch (Exception e) {
            resp.setRetn(2001);
            resp.setDesc("Unable to create NUBAN number for bond holder, please see error log for details");
            logger.info("Error in creating NUBAN number : " + e);
        }
        return resp;
    }

    public Response addBondholderNubanAccountNumber_authorise(Login login, String notificationCode) {
        Response resp = new Response();
        logger.info("NUBAN account number creation authorised - [{}]", notificationCode);
        logger.info("NUBAN number persisted by user: " + login.getUserId());
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<HolderBondAccount> bondHolderList = (List<HolderBondAccount>) wrapper.getModel();
            HolderBondAccount bondHolderModel = bondHolderList.get(0);
            org.greenpole.hibernate.entity.HolderBondAccount bondHolder_hib = new org.greenpole.hibernate.entity.HolderBondAccount();
            bondHolder_hib.setNubanAccount(bondHolderModel.getNubanAccount());
            hd.createBondNubanAccount(bondHolder_hib);
            resp.setRetn(0);
            resp.setDesc("NUBAN number successfully created");
        } catch (JAXBException ex) {
            resp.setRetn(200);
            resp.setDesc("NUBAN number was not created due to error, please see error log");
            logger.error("Failed to create NUBAN number due to: " + ex);
        }
        return resp;
    }

    public Response changeShareholderNubanAccount_request(Login login, String authenticator, HolderCompanyAccount holderCompAccount) {
        Response resp = new Response();
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;
        try {
             //boolean nubanNumber = false;
            //nubanNumber = hd.checkHolderNubanNumber(holderAccount.getNubanAccount());
            org.greenpole.hibernate.entity.Holder holder = hd.retrieveHolderObject(holderCompAccount.getHolderId());
            logger.info("Update of NUBAN account number [{}] for holder [{}] by [{}]", holderCompAccount.getNubanAccount(), holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
            if (!holderCompAccount.getNubanAccount().isEmpty()) {
                wrapper = new NotificationWrapper();
                prop = new NotifierProperties(ShareholderAndBondholderNubanLogic.class);
                qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                        prop.getAuthoriserNotifierQueueName());
                List<HolderCompanyAccount> holderAccountList = new ArrayList();

                holderAccountList.add(holderCompAccount);
                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription("Authenticate change of NUBAN account number " + holderCompAccount.getNubanAccount() + " for holder" + holder.getFirstName() + " " + holder.getLastName() + " by user " + login.getUserId());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(holderAccountList);
                resp = qSender.sendAuthorisationRequest(wrapper);
                resp.setRetn(0);
                resp.setDesc("Successful");
            } else {
                resp.setRetn(200);
                resp.setDesc("Unable to change NUBAN account number for holder because holder hans no NUBAN account number");
            }
        } catch (Exception e) {
            resp.setRetn(2001);
            resp.setDesc("Unable to change NUBAN number for holder, please see error log for details");
            logger.info("Error in changing NUBAN number : " + e);
        }
        return resp;
    }

    /**
     * Persists the Holder NUBAN account after authorisation
     *
     * @param login the user Id that performed the transaction
     * @param notificationCode the notification code
     * @return the response object
     */
    public Response changeShareholderNubanAccount_authorise(Login login, String notificationCode) {
        Response resp = new Response();
        logger.info("NUBAN account number update authorised - [{}]", notificationCode);
        logger.info("NUBAN number updated by user: " + login.getUserId());
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<HolderCompanyAccount> HolderList = (List<HolderCompanyAccount>) wrapper.getModel();
            HolderCompanyAccount HolderModel = HolderList.get(0);
            org.greenpole.hibernate.entity.HolderCompanyAccount Holder_hib = new org.greenpole.hibernate.entity.HolderCompanyAccount();
            Holder_hib.setNubanAccount(HolderModel.getNubanAccount());
            hd.changeShareholderNubanAccount(Holder_hib);
            resp.setRetn(0);
            resp.setDesc("NUBAN number successfully updated");
        } catch (JAXBException ex) {
            resp.setRetn(200);
            resp.setDesc("NUBAN number was not updated due to error, please see error log");
            logger.error("Failed to update NUBAN number due to error: " + ex);
        }
        return resp;
    }

    /**
     * processes request for the creation of a bond holder NUBAN account
     *
     * @param login use to get the user Id of the user that persisted the NUBAN
     * account
     * @param authenticator the super user to authenticate the request
     * @param bondHolderAcc
     * @return the response object
     */
    public Response changeBondholderNubanAccount_request(Login login, String authenticator, HolderBondAccount bondHolderAcc) {
        Response resp = new Response();
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;
        try {
            //boolean nubanNumber = hd.checkHolderNubanNumber(bondHolderAcc.getNubanAccount());
            org.greenpole.hibernate.entity.Holder holder = hd.retrieveHolderObject(bondHolderAcc.getHolderId());
            logger.info("Update of NUBAN account number [{}] for bond holder [{}] by [{}]", bondHolderAcc.getNubanAccount(), holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
            if (!bondHolderAcc.getNubanAccount().isEmpty()) {
                wrapper = new NotificationWrapper();
                prop = new NotifierProperties(ShareholderAndBondholderNubanLogic.class);
                qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                        prop.getAuthoriserNotifierQueueName());
                List<HolderBondAccount> holderAccountList = new ArrayList();
                holderAccountList.add(bondHolderAcc);
                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription("Authenticate change of NUBAN account number " + bondHolderAcc.getNubanAccount() + " for bond holder" + holder.getFirstName() + " " + holder.getLastName() + " by user " + login.getUserId());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(holderAccountList);
                resp = qSender.sendAuthorisationRequest(wrapper);
                resp.setRetn(0);
                resp.setDesc("Successful");
            } else {
                resp.setRetn(200);
                resp.setDesc("Unable to change NUBAN account number for bond holder because holder hans no NUBAN account number");
            }
        } catch (Exception e) {
            resp.setRetn(2001);
            resp.setDesc("Unable to change NUBAN number for bond holder, please see error log for details");
            logger.info("Error in changing NUBAN number : " + e);
        }
        return resp;
    }

    /**
     *
     * @param login use to get the user Id that persisted the NUBAN account
     * @param notificationCode the notification code
     * @return the response object
     */
    public Response changeBondholderNubanAccount_authorise(Login login, String notificationCode) {
        Response resp = new Response();
        logger.info("NUBAN account number update authorised - [{}]", notificationCode);
        logger.info("NUBAN number updated by user: " + login.getUserId());
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<HolderBondAccount> bondHolderList = (List<HolderBondAccount>) wrapper.getModel();
            HolderBondAccount bondHolderModel = bondHolderList.get(0);
            org.greenpole.hibernate.entity.HolderBondAccount bondHolder_hib = new org.greenpole.hibernate.entity.HolderBondAccount();
            bondHolder_hib.setNubanAccount(bondHolderModel.getNubanAccount());
            hd.changeBondholderNubanAccount(bondHolder_hib);
            resp.setRetn(0);
            resp.setDesc("NUBAN number was successfully updated");
        } catch (JAXBException ex) {
            resp.setRetn(200);
            resp.setDesc("NUBAN number was not updated due to error, please see error log");
            logger.error("Failed to update NUBAN number due to error: " + ex);
        }
        return resp;
    }

    /**
     * Processes retrieving of holder edited records
     *
     * @param holderChanges
     * @return the response object
     */
    public Response viewReportOnHolderAccountEditing(HolderChanges holderChanges) {
        Response resp = new Response();
        org.greenpole.hibernate.entity.HolderChanges holderChanges_hib = getHolderDetails(holderChanges);
        List<org.greenpole.hibernate.entity.HolderChanges> holderChangeList = new ArrayList();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        
        try {
            Date convertedModelChangeDate = formatter.parse(holderChanges.getChangeDate());
            if (holderChanges.getChangeType().equalsIgnoreCase("Change of name") && holderChanges.getChangeDate().equalsIgnoreCase(formatter.format(holderChanges_hib.getChangeDate()))) {
            holderChanges_hib = hd.retrieveHolderChangesQueryOne(holderChanges.getChangeType(), holderChanges.getChangeDate(), holderChanges.getHolder().getId());
            holderChanges.setCurrentForm(holderChanges_hib.getCurrentForm());
            holderChanges.setInitialForm(holderChanges_hib.getInitialForm());
            holderChangeList.add(holderChanges_hib);
            resp.setBody(holderChangeList);
            }
            else if(holderChanges.getChangeType().equalsIgnoreCase("Change of name") && holderChanges.getFromDate().endsWith(formatter.format(holderChanges_hib.getChangeDate())) && holderChanges.getToDate().endsWith(formatter.format(holderChanges_hib.getChangeDate()))){
            holderChanges_hib = hd.retrieveHolderChangesQueryTwo(holderChanges.getChangeType(), holderChanges.getFromDate(), holderChanges.getToDate(), holderChanges.getHolder().getId());
            holderChanges.setCurrentForm(holderChanges_hib.getCurrentForm());
            holderChanges.setInitialForm(holderChanges_hib.getInitialForm());
            holderChangeList.add(holderChanges_hib);
            resp.setBody(holderChangeList);
            }
            else if(holderChanges.getChangeType().endsWith("Change of name") && convertedModelChangeDate.before(holderChanges_hib.getChangeDate())){
             holderChanges_hib = hd.retrieveHolderChangesQueryOne(holderChanges.getChangeType(), holderChanges.getChangeDate(), holderChanges.getHolder().getId());
            holderChanges.setCurrentForm(holderChanges_hib.getCurrentForm());
            holderChanges.setInitialForm(holderChanges_hib.getInitialForm());
            holderChangeList.add(holderChanges_hib);
            resp.setBody(holderChangeList);
            }
            else if(holderChanges.getChangeType().endsWith("Change of name") && convertedModelChangeDate.after(holderChanges_hib.getChangeDate())){
             holderChanges_hib = hd.retrieveHolderChangesQueryOne(holderChanges.getChangeType(), holderChanges.getChangeDate(), holderChanges.getHolder().getId());
            holderChanges.setCurrentForm(holderChanges_hib.getCurrentForm());
            holderChanges.setInitialForm(holderChanges_hib.getInitialForm());
            holderChangeList.add(holderChanges_hib);
            resp.setBody(holderChangeList);
            }
            else if(holderChanges.getChangeType().equalsIgnoreCase("Correction of name") && holderChanges.getChangeDate().equalsIgnoreCase(formatter.format(holderChanges_hib.getChangeDate()))){
            holderChanges_hib = hd.retrieveHolderChangesQueryOne(holderChanges.getChangeType(), holderChanges.getChangeDate(), holderChanges.getHolder().getId());
            holderChanges.setCurrentForm(holderChanges_hib.getCurrentForm());
            holderChanges.setInitialForm(holderChanges_hib.getInitialForm());
            holderChangeList.add(holderChanges_hib);
            resp.setBody(holderChangeList);
            }
            else if(holderChanges.getChangeType().equalsIgnoreCase("Correction of name") && holderChanges.getFromDate().endsWith(formatter.format(holderChanges_hib.getChangeDate())) && holderChanges.getToDate().endsWith(formatter.format(holderChanges_hib.getChangeDate()))){
            holderChanges_hib = hd.retrieveHolderChangesQueryTwo(holderChanges.getChangeType(), holderChanges.getFromDate(), holderChanges.getToDate(), holderChanges.getHolder().getId());
            holderChanges.setCurrentForm(holderChanges_hib.getCurrentForm());
            holderChanges.setInitialForm(holderChanges_hib.getInitialForm());
            holderChangeList.add(holderChanges_hib);
            resp.setBody(holderChangeList);
            }
            else if(holderChanges.getChangeType().equalsIgnoreCase("Change of address") && holderChanges.getChangeDate().equalsIgnoreCase(formatter.format(holderChanges_hib.getChangeDate()))) {
            holderChanges_hib = hd.retrieveHolderChangesQueryOne(holderChanges.getChangeType(), holderChanges.getChangeDate(), holderChanges.getHolder().getId());
            holderChanges.setCurrentForm(holderChanges_hib.getCurrentForm());
            holderChanges.setInitialForm(holderChanges_hib.getInitialForm());
            holderChangeList.add(holderChanges_hib);
            resp.setBody(holderChangeList);
            }
        } catch (Exception ex) {
            resp.setRetn(200);
            resp.setDesc("Edited Records not found");
        }
        return resp;
    }

    /**
     * retrieves the edited records of a particular holder using the search
     * criteria's specified
     *
     * @param holderChanges
     * @return holderChange_hib the hibernate entity object of the edited record
     */
    
    public org.greenpole.hibernate.entity.HolderChanges getHolderDetails(HolderChanges holderChanges) {
        org.greenpole.hibernate.entity.HolderChanges holderChange_hib = hd.getHolderEditedDetails(holderChanges.getHolder().getId());
        return holderChange_hib;
    }
}
