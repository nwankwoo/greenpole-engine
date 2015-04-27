/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.model.holder.HolderBondAccount;
import org.greenpole.entity.model.holder.HolderCompanyAccount;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
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
/** retrieves the list of NUBAN accounts for all share holders
 * 
 * @return listHolder_hib the list of hibernate entity object
 */
    public List<org.greenpole.hibernate.entity.HolderCompanyAccount> retrieveAllShareHolderCompanyAccounts() {
        List<org.greenpole.hibernate.entity.HolderCompanyAccount> listHolder_hib = new ArrayList();
        org.greenpole.hibernate.entity.HolderCompanyAccount holder_hib = new org.greenpole.hibernate.entity.HolderCompanyAccount();
        listHolder_hib = hd.getAllShareholderNubanAccounts();
        return listHolder_hib;
    }

    public Response retrieveAllShareHolderNubanAccounts() {
        Response resp = new Response();
        List<org.greenpole.hibernate.entity.HolderCompanyAccount> list = retrieveAllShareHolderCompanyAccounts();
        List<HolderCompanyAccount> holderList = new ArrayList();
        HolderCompanyAccount holderModel = new HolderCompanyAccount();
        try {
            for (org.greenpole.hibernate.entity.HolderCompanyAccount hc : list) {
                holderModel.setNubanAccount(hc.getNubanAccount());
                holderList.add(holderModel);
            }
            resp.setBody(holderList);
            resp.setRetn(0);
            resp.setDesc("Holder NUBAN accounts retrieved");
        } catch (Exception e) {
            resp.setRetn(200);
            resp.setDesc("Unable to retrieve NUBAN accounts");
        }
        return resp;
    }
    public List<org.greenpole.hibernate.entity.HolderBondAccount> retrieveAllBondHolderCompanyAccounts() {
        List<org.greenpole.hibernate.entity.HolderBondAccount> listHolder_hib = new ArrayList();
        org.greenpole.hibernate.entity.HolderBondAccount holder_hib = new org.greenpole.hibernate.entity.HolderBondAccount();
        listHolder_hib = hd.getAllBondholderNubanAccounts();
        return listHolder_hib;
    }

    public Response retrieveAllBondHolderNubanAccounts() {
        Response resp = new Response();
        List<org.greenpole.hibernate.entity.HolderBondAccount> list = retrieveAllBondHolderCompanyAccounts();
        List<HolderBondAccount> bondHolderList = new ArrayList();
        HolderBondAccount bondHolderModel = new HolderBondAccount();
        try {
            for (org.greenpole.hibernate.entity.HolderBondAccount hc : list) {
                bondHolderModel.setNubanAccount(hc.getNubanAccount());
                bondHolderList.add(bondHolderModel);
            }
            resp.setBody(bondHolderList);
            resp.setRetn(0);
            resp.setDesc("Bond Holder NUBAN accounts retrieved");
        } catch (Exception e) {
            resp.setRetn(200);
            resp.setDesc("Unable to retrieve bond holder NUBAN accounts");
        }
        return resp;
    }

    public Response addShareholderNubanAccountNumber_request(Login login, String authenticator, HolderCompanyAccount holderNubanAccount) {
        Response resp = new Response();
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;
        try {
            String nubanNumber = hd.checkHolderNubanNumber(holderNubanAccount.getNubanAccount());
            org.greenpole.hibernate.entity.Holder holder = hd.retrieveHolderObject(holderNubanAccount.getHolderId());
            logger.info("Create of NUBAN account number [{}] for holder [{}]", holderNubanAccount.getNubanAccount(), holder.getFirstName() + " " + holder.getLastName());
            if (nubanNumber.equalsIgnoreCase(" ") || nubanNumber.equalsIgnoreCase("null")) {
                wrapper = new NotificationWrapper();
                prop = new NotifierProperties(ShareholderAndBondholderNubanLogic.class);
                qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                        prop.getAuthoriserNotifierQueueName());
                List<HolderCompanyAccount> holderAccountList = new ArrayList();

                holderAccountList.add(holderNubanAccount);
                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription("Authenticate creation of NUBAN account number " + holderNubanAccount.getNubanAccount() + " for holder" + holder.getFirstName() + " " + holder.getLastName() + " by user " + login.getUserId());
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
            logger.info("Error in creating NUBAN number : "+e);
        }
        return resp;
    }
    public Response addShareholderNubanAccountNumber_authorise(Login login, String notificationCode){
        Response resp = new Response();
        logger.info("NUBAN account number creation authorised - [{}]", notificationCode);
        logger.info("NUBAN number persisted by user: "+login.getUserId());
        try{
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<HolderCompanyAccount> holderList = (List<HolderCompanyAccount>) wrapper.getModel();
            HolderCompanyAccount holderModel = holderList.get(0);
            org.greenpole.hibernate.entity.HolderCompanyAccount holder_hib = new org.greenpole.hibernate.entity.HolderCompanyAccount();
            holder_hib.setNubanAccount(holderModel.getNubanAccount());
            hd.createNubanAccount(holder_hib);
            resp.setRetn(0);
            resp.setDesc("NUBAN number successfully created");
        }catch(JAXBException ex){
        resp.setRetn(200);
        resp.setDesc("NUBAN number was not created due to error, please see error log");
        logger.error("Failed to create NUBAN number due to: "+ex);
        }
    return resp;
    }
   public Response addBondholderNubanAccountNumber_request(Login login, String authenticator, HolderBondAccount bondHolder) {
        Response resp = new Response();
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;
        try {
            String nubanNumber = hd.checkHolderNubanNumber(bondHolder.getNubanAccount());
            org.greenpole.hibernate.entity.Holder holder = hd.retrieveHolderObject(bondHolder.getHolderId());
            logger.info("Creation of NUBAN account number [{}] for bond holder [{}] by user ", bondHolder.getNubanAccount(), holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
            if (nubanNumber.equalsIgnoreCase(" ") || nubanNumber.equalsIgnoreCase("null")) {
                wrapper = new NotificationWrapper();
                prop = new NotifierProperties(ShareholderAndBondholderNubanLogic.class);
                qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                        prop.getAuthoriserNotifierQueueName());
                List<HolderBondAccount> bondHolderAccountList = new ArrayList();

                bondHolderAccountList.add(bondHolder);
                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription("Authenticate creation of NUBAN account number " + bondHolder.getNubanAccount() + " for holder" + holder.getFirstName() + " " + holder.getLastName() + " by user " + login.getUserId());
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
            logger.info("Error in creating NUBAN number : "+e);
        }
        return resp;
    }
    public Response addBondholderNubanAccountNumber_authorise(Login login, String notificationCode){
        Response resp = new Response();
        logger.info("NUBAN account number creation authorised - [{}]", notificationCode);
        logger.info("NUBAN number persisted by user: "+login.getUserId());
        try{
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<HolderBondAccount> bondHolderList = (List<HolderBondAccount>) wrapper.getModel();
            HolderBondAccount bondHolderModel = bondHolderList.get(0);
            org.greenpole.hibernate.entity.HolderBondAccount bondHolder_hib = new org.greenpole.hibernate.entity.HolderBondAccount();
            bondHolder_hib.setNubanAccount(bondHolderModel.getNubanAccount());
            hd.createBondNubanAccount(bondHolder_hib);
            resp.setRetn(0);
            resp.setDesc("NUBAN number successfully created");
        }catch(JAXBException ex){
        resp.setRetn(200);
        resp.setDesc("NUBAN number was not created due to error, please see error log");
        logger.error("Failed to create NUBAN number due to: "+ex);
        }
    return resp;
    }
  public Response changeShareholderNubanAccount_request(Login login, String authenticator, HolderCompanyAccount holderAccount){
  Response resp = new Response();
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;
         try {
            String nubanNumber = hd.checkHolderNubanNumber(holderAccount.getNubanAccount());
            org.greenpole.hibernate.entity.Holder holder = hd.retrieveHolderObject(holderAccount.getHolderId());
            logger.info("Update of NUBAN account number [{}] for holder [{}] by [{}]", holderAccount.getNubanAccount(), holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
            if (!nubanNumber.equals(" ") || !nubanNumber.equals("null")) {
                wrapper = new NotificationWrapper();
                prop = new NotifierProperties(ShareholderAndBondholderNubanLogic.class);
                qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                        prop.getAuthoriserNotifierQueueName());
                List<HolderCompanyAccount> holderAccountList = new ArrayList();

                holderAccountList.add(holderAccount);
                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription("Authenticate change of NUBAN account number " + holderAccount.getNubanAccount() + " for holder" + holder.getFirstName() + " " + holder.getLastName() + " by user " + login.getUserId());
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
            logger.info("Error in changing NUBAN number : "+e);
        }
        return resp;
  }
  public Response changeShareholderNubanAccount_authorise(Login login, String notificationCode){
        Response resp = new Response();
        logger.info("NUBAN account number update authorised - [{}]", notificationCode);
        logger.info("NUBAN number updated by user: "+login.getUserId());
        try{
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<HolderCompanyAccount> HolderList = (List<HolderCompanyAccount>) wrapper.getModel();
            HolderCompanyAccount HolderModel = HolderList.get(0);
            org.greenpole.hibernate.entity.HolderCompanyAccount Holder_hib = new org.greenpole.hibernate.entity.HolderCompanyAccount();
            Holder_hib.setNubanAccount(HolderModel.getNubanAccount());
            hd.changeShareholderNubanAccount(Holder_hib);
            resp.setRetn(0);
            resp.setDesc("NUBAN number successfully updated");
        }catch(JAXBException ex){
        resp.setRetn(200);
        resp.setDesc("NUBAN number was not updated due to error, please see error log");
        logger.error("Failed to update NUBAN number due to error: "+ex);
        }
    return resp;
    }
  public Response changeBondholderNubanAccount_request(Login login, String authenticator, HolderBondAccount bondHolder){
  Response resp = new Response();
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;
         try {
            String nubanNumber = hd.checkHolderNubanNumber(bondHolder.getNubanAccount());
            org.greenpole.hibernate.entity.Holder holder = hd.retrieveHolderObject(bondHolder.getHolderId());
            logger.info("Update of NUBAN account number [{}] for bond holder [{}] by [{}]", bondHolder.getNubanAccount(), holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
            if (!nubanNumber.equals(" ") || !nubanNumber.equals("null")) {
                wrapper = new NotificationWrapper();
                prop = new NotifierProperties(ShareholderAndBondholderNubanLogic.class);
                qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                        prop.getAuthoriserNotifierQueueName());
                List<HolderBondAccount> holderAccountList = new ArrayList();
                holderAccountList.add(bondHolder);
                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription("Authenticate change of NUBAN account number " + bondHolder.getNubanAccount() + " for bond holder" + holder.getFirstName() + " " + holder.getLastName() + " by user " + login.getUserId());
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
            logger.info("Error in changing NUBAN number : "+e);
        }
        return resp;
  }
  public Response changeBondholderNubanAccount_authorise(Login login, String notificationCode){
        Response resp = new Response();
        logger.info("NUBAN account number update authorised - [{}]", notificationCode);
        logger.info("NUBAN number updated by user: "+login.getUserId());
        try{
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<HolderBondAccount> bondHolderList = (List<HolderBondAccount>) wrapper.getModel();
            HolderBondAccount bondHolderModel = bondHolderList.get(0);
            org.greenpole.hibernate.entity.HolderBondAccount bondHolder_hib = new org.greenpole.hibernate.entity.HolderBondAccount();
            bondHolder_hib.setNubanAccount(bondHolderModel.getNubanAccount());
            hd.changeBondholderNubanAccount(bondHolder_hib);
            resp.setRetn(0);
            resp.setDesc("NUBAN number was successfully updated");
        }catch(JAXBException ex){
        resp.setRetn(200);
        resp.setDesc("NUBAN number was not updated due to error, please see error log");
        logger.error("Failed to update NUBAN number due to error: "+ex);
        }
    return resp;
    }
}
