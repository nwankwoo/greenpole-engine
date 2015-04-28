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
import org.greenpole.entrycode.emmanuel.model.InitialPublicOffer;
import org.greenpole.hibernate.entity.Holder;
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
            HolderCompanyAccount holderCompAcc = holderCompAccList.get(0);
            org.greenpole.hibernate.entity.HolderCompanyAccount holderCompanyAccount = new org.greenpole.hibernate.entity.HolderCompanyAccount();
            holderCompanyAccount.setNubanAccount(holderCompAcc.getNubanAccount());
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
            HolderCompanyAccount holderCompAcct = HolderList.get(0);
            if(!holderCompAcct.getNubanAccount().isEmpty()){
            Holder holder = hd.retrieveHolderObject(holderCompAcct.getHolderId());
            org.greenpole.hibernate.entity.HolderCompanyAccount hca = hd.retrieveHolderCompanyAccount(holderCompAcct.getHolderId());
            org.greenpole.hibernate.entity.HolderCompanyAccount holderCompAcct_hib = new org.greenpole.hibernate.entity.HolderCompanyAccount();
            holderCompAcct_hib.setId(hca.getId());
            holderCompAcct_hib.setNubanAccount(holderCompAcct.getNubanAccount());
            hd.changeShareholderNubanAccount(holderCompAcct_hib);
            resp.setRetn(0);
            resp.setDesc("NUBAN number successfully updated");
            return resp;
            }
        } catch (JAXBException ex) {
            resp.setRetn(300);
            resp.setDesc("NUBAN number was not updated due to error, please see error log");
            logger.error("Failed to update NUBAN number due to error: " + ex);
            return resp;
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
                resp.setRetn(300);
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
            HolderBondAccount bondHolderAcct = bondHolderList.get(0);
            if(!bondHolderAcct.getNubanAccount().isEmpty()){
            org.greenpole.hibernate.entity.HolderBondAccount bondHolderAcct_hib = hd.retrieveHolderBondCompAccount(bondHolderAcct.getHolderId());
            org.greenpole.hibernate.entity.HolderBondAccount holderBondAcc = new org.greenpole.hibernate.entity.HolderBondAccount();
            holderBondAcc.setNubanAccount(bondHolderAcct.getNubanAccount());
            holderBondAcc.setId(bondHolderAcct_hib.getId());
            hd.changeBondholderNubanAccount(holderBondAcc);
            logger.info("NUBAN number successfully changed");
            resp.setRetn(0);
            resp.setDesc("NUBAN number was successfully updated");
            return resp;
            }
            else{
             logger.info("NUBAN number is empty");
            resp.setRetn(300);
            resp.setDesc("NUBAN number to update must not be empty");
            return resp;
            }
        } catch (JAXBException ex) {
            resp.setRetn(200);
            resp.setDesc("NUBAN number was not updated due to error, please see error log");
            logger.error("Failed to update NUBAN number due to error: " + ex);
        }
        return resp;
    }
}