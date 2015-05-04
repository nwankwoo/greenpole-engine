/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.logic;

import com.sun.javafx.scene.control.skin.VirtualFlow;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.model.Address;
import org.greenpole.entity.model.EmailAddress;
import org.greenpole.entity.model.PhoneNumber;
import org.greenpole.entity.model.clientcompany.UnitTransfer;
import org.greenpole.entity.model.holder.Holder;
import org.greenpole.entity.model.holder.HolderBondAccount;
import org.greenpole.entity.model.holder.HolderChanges;
import org.greenpole.entity.model.holder.HolderCompanyAccount;
import org.greenpole.entity.model.holder.HolderMerger;
import org.greenpole.entity.model.holder.QueryHolder;
import org.greenpole.entity.model.holder.QueryHolderChanges;
import org.greenpole.entity.model.stockbroker.Stockbroker;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.hibernate.entity.Bank;
import org.greenpole.hibernate.entity.BondOffer;
import org.greenpole.hibernate.entity.ClientCompany;
import org.greenpole.hibernate.entity.HolderBondAccountId;
import org.greenpole.hibernate.entity.HolderChangeType;
import org.greenpole.hibernate.entity.HolderCompanyAccountId;
import org.greenpole.hibernate.entity.HolderEmailAddress;
import org.greenpole.hibernate.entity.HolderEmailAddressId;
import org.greenpole.hibernate.entity.HolderPhoneNumber;
import org.greenpole.hibernate.entity.HolderPhoneNumberId;
import org.greenpole.hibernate.entity.HolderPostalAddress;
import org.greenpole.hibernate.entity.HolderPostalAddressId;
import org.greenpole.hibernate.entity.HolderResidentialAddress;
import org.greenpole.hibernate.entity.HolderResidentialAddressId;
import org.greenpole.hibernate.query.HolderComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.Descriptor;
import org.greenpole.util.Manipulator;
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
public class HolderComponentLogic {
    private final HolderComponentQuery hq = ComponentQueryFactory.getHolderComponentQuery();
    private static final Logger logger = LoggerFactory.getLogger(HolderComponentLogic.class);
    
    /**
     * Request to merge a holder account / multiple holder accounts to a primary holder account.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param accountsToMerge the merge holder accounts object containing merge detailss
     * @return response to the merge holder accounts request
     */
    public Response mergeHolderAccounts_Request(Login login, String authenticator, HolderMerger accountsToMerge) {
        logger.info("request holder accounts merge, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        
        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;
            
            org.greenpole.hibernate.entity.Holder h_hib = new org.greenpole.hibernate.entity.Holder();
            List<org.greenpole.hibernate.entity.Holder> h_hib_list = new ArrayList<>();
            
            boolean pryCheckPassed = false;
            boolean secCheckPassed = false;
            
            //check primary account
            if (accountsToMerge.getPrimaryHolder().isPryHolder()) {
                pryCheckPassed = true;
                h_hib = hq.getHolder(accountsToMerge.getPrimaryHolder().getHolderId());
            }
            
            //check secondary accounts
            for (Holder h : accountsToMerge.getSecondaryHolders()) {
                if (!h.isPryHolder()) {
                    secCheckPassed = false;
                    break;
                } else {
                    org.greenpole.hibernate.entity.Holder h_entity = hq.getHolder(h.getHolderId());
                    h_hib_list.add(h_entity);
                    secCheckPassed = true;
                }
            }
            
            if (pryCheckPassed && secCheckPassed) {
                wrapper = new NotificationWrapper();
                
                prop = new NotifierProperties(HolderComponentLogic.class);
                qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                        prop.getAuthoriserNotifierQueueName());

                logger.info("all holder accounts qualify for merge");
                
                List<HolderMerger> holderMerger = new ArrayList<>();
                holderMerger.add(accountsToMerge);
                
                String pryName = h_hib.getFirstName() + " " + h_hib.getLastName();
                
                String descMsg = "Merge accounts for " + pryName + " and ";
                
                int pos = 0;
                for (org.greenpole.hibernate.entity.Holder h : h_hib_list) {
                    String secName = h.getFirstName() + " " + h.getLastName();
                    descMsg += secName;
                    pos++;
                    if (pos < h_hib_list.size())
                        descMsg += ", ";
                }
                descMsg = ", requested by " + login.getUserId();
                
                //wrap unit transfer object in notification object, along with other information
                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription(descMsg);
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(holderMerger);
                resp = qSender.sendAuthorisationRequest(wrapper);
                logger.info("notification fowarded to queue - notification code: [{}]", wrapper.getCode());
                return resp;
            } else {
                String errorMsg = "";
                if (!pryCheckPassed) {
                    errorMsg += "\nThe chosen primary holder account has already been merged into another account.";
                    logger.info("The chosen primary holder account has already been merged into another account");
                } else if (!secCheckPassed) {
                    errorMsg += "\nThe chosen secondary holder account (or one of them) has already been merged into another account.";
                    logger.info("The chosen secondary holder account (or one of them) has already been merged into another account");
                }
                resp.setRetn(306);
                resp.setDesc("Error: " + errorMsg);
                return resp;
            }
        } catch (Exception ex) {
            logger.info("error processing holder accounts merge. See error log");
            logger.error("error processing holder accounts merge - ", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process holder accounts merge. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    public Response mergeHolderAccounts_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        logger.info("authorise holder accounts merge, invoked by [{}] - notification code: [{}]", login.getUserId(), notificationCode);
        try {
            //get client company model from wrapper
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<HolderMerger> merger = (List<HolderMerger>) wrapper.getModel();
        } catch (JAXBException ex) {
        } catch (Exception ex) {
        }
        return null;
    }
    
    /**
     * Request to transfer share units between holder company accounts.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param unitTransfer the unit transfer object containing transfer details
     * @return response to the transfer share unit request
     */
    public Response transferShareUnitManual_Request(Login login, String authenticator, UnitTransfer unitTransfer) {
        logger.info("request share unit transfer, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        
        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;

            boolean senderHolderExists = hq.checkHolderAccount(unitTransfer.getHolderIdFrom());
            boolean receiverHolderExists = hq.checkHolderAccount(unitTransfer.getHolderIdTo());

            if (senderHolderExists) {//check if sender exists
                org.greenpole.hibernate.entity.Holder senderHolder = hq.getHolder(unitTransfer.getHolderIdFrom());
                String senderName = senderHolder.getFirstName() + " " + senderHolder.getLastName();

                if (receiverHolderExists) {//check if receiver exists
                    org.greenpole.hibernate.entity.Holder receiverHolder = hq.getHolder(unitTransfer.getHolderIdTo());
                    String receiverName = receiverHolder.getFirstName() + " " + receiverHolder.getLastName();
                    
                    if (senderHolder.isPryHolder()) {//check if sender is primary

                        if (receiverHolder.isPryHolder()) {//check if receiver is primary
                            boolean senderHolderCompAcctExists = hq.checkHolderCompanyAccount(unitTransfer.getHolderIdFrom(), unitTransfer.getClientCompanyId());

                            if (senderHolderCompAcctExists) {//check if sender has account
                                org.greenpole.hibernate.entity.HolderCompanyAccount senderCompAcct = hq.getHolderCompanyAccount(unitTransfer.getHolderIdFrom(), unitTransfer.getClientCompanyId());
                                boolean senderHolderCAChnExists = hq.checkHolderCompanyAccount(senderCompAcct.getChn());

                                if (senderHolderCAChnExists) {//check if sender has chn in company account

                                    if (senderCompAcct.getShareUnits() < unitTransfer.getUnits()) { //check if sender has sufficient units to transact
                                        boolean receiverHolderCompAcctExists = hq.checkHolderCompanyAccount(unitTransfer.getHolderIdTo(), unitTransfer.getClientCompanyId());
                                        boolean receiverHolderCAChnExists = false;
                                        if (receiverHolderCompAcctExists) {
                                            org.greenpole.hibernate.entity.HolderCompanyAccount receiverCompAcct = hq.getHolderCompanyAccount(unitTransfer.getHolderIdTo(), unitTransfer.getClientCompanyId());
                                            receiverHolderCAChnExists = hq.checkHolderCompanyAccount(receiverCompAcct.getChn());
                                        }

                                        wrapper = new NotificationWrapper();
                                        prop = new NotifierProperties(HolderComponentLogic.class);
                                        qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                                                prop.getAuthoriserNotifierQueueName());

                                        logger.info("holder accounts check out");
                                        logger.info("preparing notification for transaction between holders [{}] and [{}]", senderName, receiverName);

                                        List<UnitTransfer> transferList = new ArrayList<>();
                                        transferList.add(unitTransfer);

                                        //wrap unit transfer object in notification object, along with other information
                                        wrapper.setCode(Notification.createCode(login));
                                        wrapper.setDescription("Authenticate unit transfer between " + senderName + " and " + receiverName);
                                        wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                                        wrapper.setFrom(login.getUserId());
                                        wrapper.setTo(authenticator);
                                        wrapper.setModel(transferList);

                                        //check if receiver holder has company account and company account chn to send appropriate message
                                        if (!receiverHolderCompAcctExists) {//if receiver has no company account, inform user
                                            if (receiverHolder.getChn() != null && !receiverHolder.getChn().equals("")) {//...and has chn in main account
                                                resp = qSender.sendAuthorisationRequest(wrapper);
                                                logger.info("notification fowarded to queue - notification code: [{}]", wrapper.getCode());

                                                String originalMsg = resp.getDesc();
                                                resp.setDesc(originalMsg + "\nHolder - " + receiverName
                                                        + " - has no active account with the company. One will be created for them upon authorisation.");
                                                logger.info("Holder - [{}] - has no active account with the company. One will be created for them upon authorisation", receiverName);
                                                return resp;
                                            } else {//...and has no chn in main account
                                                resp = qSender.sendAuthorisationRequest(wrapper);
                                                logger.info("notification fowarded to queue - notification code: [{}]", wrapper.getCode());

                                                resp.setDesc("Holder - " + receiverName + " - has no active account with the company, with no underlying CHN."
                                                        + "\nA certificate will be created for them upon authorisation");
                                                logger.info("Holder - [{}] - has no active account with the company, with no underlying CHN. A certificate will be created for them upon authorisation", receiverName);
                                                return resp;
                                            }
                                        } else if (!receiverHolderCAChnExists) {//if receiver has company account, but no chn in it...
                                            if (receiverHolder.getChn() != null && !receiverHolder.getChn().equals("")) {//...and has chn in main account
                                                resp = qSender.sendAuthorisationRequest(wrapper);
                                                logger.info("notification fowarded to queue - notification code: [{}]", wrapper.getCode());

                                                String originalMsg = resp.getDesc();
                                                resp.setDesc(originalMsg + "\nHolder - " + receiverName + " - has no CHN in their account with the company."
                                                        + "\nOne will be added for them upon authorisation.");
                                                return resp;
                                            } else {//...and has no chn in main account
                                                resp.setRetn(302);
                                                resp.setDesc("Database mismatch. Holder - " + receiverName + " - has account with the company, with no underlying CHN."
                                                        + "\nA holder cannot have an account with a company without a CHN. Contact System Administrator.");
                                                return resp;
                                            }
                                        } else {//if receiver has company account and chn in it
                                            resp = qSender.sendAuthorisationRequest(wrapper);
                                            logger.info("notification fowarded to queue - notification code: [{}]", wrapper.getCode());
                                            return resp;
                                        }
                                    }
                                    resp.setRetn(302);
                                    resp.setDesc("The holder - " + senderName + " - does not have the sufficient share units to make this transaction.");
                                    logger.info("The holder - [{}] - does not have the sufficient share units to make this transaction", senderName);
                                    return resp;
                                }
                                resp.setRetn(302);
                                resp.setDesc("The holder - " + senderName + " - has no recorded CHN in his share company account.");
                                logger.info("The holder - [{}] - has no recorded CHN in his share company account", senderName);
                                return resp;
                            }
                            resp.setRetn(302);
                            resp.setDesc("The holder - " + senderName + " - has no share company account to send any units from.");
                            logger.info("The holder - [{}] - has no share company account to send any units from", senderName);
                            return resp;
                        }
                        resp.setRetn(302);
                        resp.setDesc("The holder - " + receiverName + " - has been merged into another account. It is not an active account.");
                        logger.info("The holder - [{}] - has been merged into another account. It is not an active account", receiverName);
                        return resp;
                    }
                    resp.setRetn(302);
                    resp.setDesc("The holder - " + senderName + " - has been merged into another account. It is not an active account.");
                    logger.info("The holder - [{}] - has been merged into another account. It is not an active account", senderName);
                    return resp;
                }
                resp.setRetn(302);
                resp.setDesc("The receiving holder does not exist.");
                logger.info("The receiving holder does not exist");
                return resp;
            }
            resp.setRetn(302);
            resp.setDesc("The sending holder does not exist.");
            logger.info("The sending holder does not exist");
            return resp;
        } catch (Exception ex) {
            logger.info("error processing share unit transfer. See error log");
            logger.error("error processing share unit transfer - ", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process share unit transfer. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Processes a saved request to transfer share units between holder company accounts.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the transfer share unit request
     */
    public Response transferShareUnitManual_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        logger.info("authorise share unit transfer, invoked by [{}] - notification code: [{}]", login.getUserId(), notificationCode);
        try {
            //get client company model from wrapper
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<UnitTransfer> transferList = (List<UnitTransfer>) wrapper.getModel();
            UnitTransfer unitTransfer = transferList.get(0);
            
            boolean senderHolderExists = hq.checkHolderAccount(unitTransfer.getHolderIdFrom());
            boolean receiverHolderExists = hq.checkHolderAccount(unitTransfer.getHolderIdTo());

            if (senderHolderExists) {//check if sender exists
                org.greenpole.hibernate.entity.Holder senderHolder = hq.getHolder(unitTransfer.getHolderIdFrom());
                String senderName = senderHolder.getFirstName() + " " + senderHolder.getLastName();
                
                if (receiverHolderExists) {//check if receiver exists
                    org.greenpole.hibernate.entity.Holder receiverHolder = hq.getHolder(unitTransfer.getHolderIdTo());
                    String receiverName = receiverHolder.getFirstName() + " " + receiverHolder.getLastName();

                    if (senderHolder.isPryHolder()) {//check if sender is a primary account

                        if (receiverHolder.isPryHolder()) {//check if receiver is a primary account
                            boolean senderHolderCompAcctExists = hq.checkHolderCompanyAccount(unitTransfer.getHolderIdFrom(), unitTransfer.getClientCompanyId());

                            if (senderHolderCompAcctExists) {//check if sender has account
                                org.greenpole.hibernate.entity.HolderCompanyAccount senderCompAcct = hq.getHolderCompanyAccount(unitTransfer.getHolderIdFrom(), unitTransfer.getClientCompanyId());
                                boolean senderHolderCAChnExists = hq.checkHolderCompanyAccount(senderCompAcct.getChn());

                                if (senderHolderCAChnExists) {//check if sender has chn in company account
                                    boolean receiverHolderCompAcctExists = hq.checkHolderCompanyAccount(unitTransfer.getHolderIdTo(), unitTransfer.getClientCompanyId());

                                    if (receiverHolderCompAcctExists) {//check if receiver has account
                                        org.greenpole.hibernate.entity.HolderCompanyAccount receiverCompAcct = hq.getHolderCompanyAccount(unitTransfer.getHolderIdTo(), unitTransfer.getClientCompanyId());
                                        boolean receiverHolderCAChnExists = hq.checkHolderCompanyAccount(receiverCompAcct.getChn());

                                        if (receiverHolderCAChnExists) {
                                            return invokeTransfer(senderCompAcct, unitTransfer, receiverCompAcct, resp, senderName, receiverName);
                                        }
                                        //no chn in receiver company account? attempt to put one for them
                                        if (receiverHolder.getChn() != null && !receiverHolder.getChn().equalsIgnoreCase("")) {
                                            receiverCompAcct.setChn(receiverHolder.getChn());
                                            //proceed with transfer
                                            return invokeTransfer(senderCompAcct, unitTransfer, receiverCompAcct, resp, senderName, receiverName);
                                        }
                                        //no chn in receiver main account as well? Inform user of database mismatch.
                                        //since company account exists, chn must be present.
                                        //system administrator should be informed to take appropriate action.
                                        resp.setRetn(302);
                                        resp.setDesc("Database mismatch. The holder - " + senderName + " - has a company account but no chn."
                                                + "\nContact Administrator. Holder cannot have company account without chn.");
                                        logger.info("Database mismatch. The holder - [{}] - has company account but no chn. Business rule violation", senderName);
                                        return resp;
                                    }
                                    //no company account? attempt to create one for them
                                    if (receiverHolder.getChn() != null && !receiverHolder.getChn().equalsIgnoreCase("")) {
                                        org.greenpole.hibernate.entity.HolderCompanyAccount receiverCompAcct = new org.greenpole.hibernate.entity.HolderCompanyAccount();
                                        receiverCompAcct.setChn(receiverHolder.getChn());
                                        receiverCompAcct.setTaxExempted(receiverHolder.isTaxExempted());
                                        hq.createUpdateHolderCompanyAccount(senderCompAcct);
                                        //proceed with transfer
                                        return invokeTransfer(senderCompAcct, unitTransfer, receiverCompAcct, resp, senderName, receiverName);
                                    }
                                    //no company account and no chn in main account? create certificate
                                    //METHOD TO CREATE CERTIFICATE
                                    boolean certCreated = true;
                                    if (certCreated) {
                                        //QUERY CERTIFICATE FOR NUMBER
                                        String certNumber = "temp";
                                        resp.setRetn(0);
                                        resp.setDesc("Transaction Successful. Certificate " + certNumber + " created for " + receiverName);
                                        logger.info("Transaction Successful. Certificate [{}] created for [{}]", certNumber, receiverName);
                                        return resp;
                                    } else {
                                        resp.setRetn(303);
                                        resp.setDesc("Transaction Unsuccessful. Certificate could not be created for " + receiverName + ". Contact System Administrator.");
                                        logger.info("Transaction Successful. Certificate could not be created for [{}]", receiverName);
                                        return resp;
                                    }
                                }
                                resp.setRetn(303);
                                resp.setDesc("The holder - " + senderName + " - has no recorded CHN in his share company account. Transaction cancelled.");
                                logger.info("The holder - [{}] - has no recorded CHN in his share company account. Transaction cancelled.", senderName);
                                return resp;
                            }
                            resp.setRetn(303);
                            resp.setDesc("The holder - " + senderName + " - has no share company account to send any units from. Transaction cancelled.");
                            logger.info("The holder - [{}] - has no share company account to send any units from. Transaction cancelled", senderName);
                            return resp;
                        }
                        resp.setRetn(303);
                        resp.setDesc("The holder - " + receiverName + " - has been merged into another account. It is not an active account.");
                        logger.info("The holder - [{}] - has been merged into another account. It is not an active account", receiverName);
                        return resp;
                    }
                    resp.setRetn(303);
                    resp.setDesc("The holder - " + senderName + " - has been merged into another account. It is not an active account.");
                    logger.info("The holder - [{}] - has been merged into another account. It is not an active account", senderName);
                    return resp;
                }
                resp.setRetn(303);
                resp.setDesc("The receiving holder does not exist. Transaction cancelled.");
                logger.info("The receiving holder does not exist. Transaction cancelled");
                return resp;
            }
            resp.setRetn(303);
            resp.setDesc("The sending holder does not exist. Transaction cancelled.");
            logger.info("The sending holder does not exist. Transaction cancelled");
            return resp;
        } catch (JAXBException ex) {
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);
            
            resp.setRetn(101);
            resp.setDesc("Unable to transfer share units from authorisation. Contact System Administrator");
            
            return resp;
        } catch (Exception ex) {
            logger.info("error transfering share units. See error log");
            logger.error("error transfering share units - ", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to transfer share units. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     * Request to transfer bond units between holder bond accounts.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param unitTransfer the unit transfer object containing transfer details
     * @return response to the transfer bond unit request
     */
    public Response transferBondUnitManual_Request(Login login, String authenticator, UnitTransfer unitTransfer) {
        logger.info("request bond unit transfer, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        
        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;

            boolean senderHolderExists = hq.checkHolderAccount(unitTransfer.getHolderIdFrom());
            boolean receiverHolderExists = hq.checkHolderAccount(unitTransfer.getHolderIdTo());

            if (senderHolderExists) {//check if sender exists
                org.greenpole.hibernate.entity.Holder senderHolder = hq.getHolder(unitTransfer.getHolderIdFrom());
                String senderName = senderHolder.getFirstName() + " " + senderHolder.getLastName();

                if (receiverHolderExists) {//check if receiver exists
                    org.greenpole.hibernate.entity.Holder receiverHolder = hq.getHolder(unitTransfer.getHolderIdTo());
                    String receiverName = receiverHolder.getFirstName() + " " + receiverHolder.getLastName();
                    
                    if (senderHolder.isPryHolder()) {
                        
                        if (receiverHolder.isPryHolder()) {
                            boolean senderHolderBondAcctExists = hq.checkHolderBondAccount(unitTransfer.getHolderIdFrom(), unitTransfer.getBondOfferId());

                            if (senderHolderBondAcctExists) {//check if sender has account
                                org.greenpole.hibernate.entity.HolderBondAccount senderBondAcct = hq.getHolderBondAccount(unitTransfer.getHolderIdFrom(), unitTransfer.getBondOfferId());
                                boolean senderHolderBAChnExists = hq.checkHolderBondAccount(senderBondAcct.getChn());

                                if (senderHolderBAChnExists) {//check if sender has chn in bond account

                                    if (senderBondAcct.getBondUnits() < unitTransfer.getUnits()) { //check if sender has sufficient units to transact
                                        boolean receiverHolderBondAcctExists = hq.checkHolderBondAccount(unitTransfer.getHolderIdTo(), unitTransfer.getBondOfferId());
                                        boolean receiverHolderBAChnExists = false;
                                        if (receiverHolderBondAcctExists) {
                                            org.greenpole.hibernate.entity.HolderBondAccount receiverBondAcct = hq.getHolderBondAccount(unitTransfer.getHolderIdTo(), unitTransfer.getBondOfferId());
                                            receiverHolderBAChnExists = hq.checkHolderCompanyAccount(receiverBondAcct.getChn());
                                        }

                                        wrapper = new NotificationWrapper();
                                        prop = new NotifierProperties(HolderComponentLogic.class);
                                        qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                                                prop.getAuthoriserNotifierQueueName());

                                        logger.info("holder accounts check out");
                                        logger.info("preparing notification for transaction between holders [{}] and [{}]", senderName, receiverName);

                                        List<UnitTransfer> transferList = new ArrayList<>();
                                        transferList.add(unitTransfer);

                                        //wrap unit transfer object in notification object, along with other information
                                        wrapper.setCode(Notification.createCode(login));
                                        wrapper.setDescription("Authenticate unit transfer between " + senderName + " and " + receiverName);
                                        wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                                        wrapper.setFrom(login.getUserId());
                                        wrapper.setTo(authenticator);
                                        wrapper.setModel(transferList);

                                        //check if receiver holder has bond account and bond acct chn to send appropriate message
                                        if (!receiverHolderBondAcctExists) {//if receiver has no company account, inform user
                                            if (receiverHolder.getChn() != null && !receiverHolder.getChn().equals("")) {//...and has chn in main account
                                                resp = qSender.sendAuthorisationRequest(wrapper);
                                                logger.info("notification fowarded to queue - notification code: [{}]", wrapper.getCode());

                                                String originalMsg = resp.getDesc();
                                                resp.setDesc(originalMsg + "\nHolder - " + receiverName
                                                        + " - has no active account with the company. One will be created for them upon authorisation.");
                                                logger.info("Holder - [{}] - has no active account with the company. One will be created for them upon authorisation", receiverName);
                                                return resp;
                                            } else {//...and has no chn in main account
                                                resp.setRetn(304);
                                                resp.setDesc("Holder - " + receiverName + " - has no active account with the company, with no underlying CHN."
                                                        + "\nBond transfer cancelled.");
                                                logger.info("Holder - [{}] - has no active account with the company, with no underlying CHN. Bond transfer cancelled", receiverName);
                                                return resp;
                                            }
                                        } else if (!receiverHolderBAChnExists) {//if receiver has company account, but no chn in it...
                                            if (receiverHolder.getChn() != null && !receiverHolder.getChn().equals("")) {//...and has chn in main account
                                                resp = qSender.sendAuthorisationRequest(wrapper);
                                                logger.info("notification fowarded to queue - notification code: [{}]", wrapper.getCode());

                                                String originalMsg = resp.getDesc();
                                                resp.setDesc(originalMsg + "\nHolder - " + receiverName
                                                        + " - has no CHN in their account with the company."
                                                        + "\nOne will be added for them upon authorisation.");
                                                logger.info("Holder - [{}] - has no CHN in their account with the company. One will be added for them upon authorisation", receiverName);
                                                return resp;
                                            } else {//...and has no chn in main account
                                                resp.setRetn(304);
                                                resp.setDesc("Holder - " + receiverName + " - has active account with the company, with no underlying CHN."
                                                        + "\nBond transfer cannot take place.");
                                                logger.info("The holder - [{}] - has no active account with the company, with no underlying CHN. Bond Transfer cannot take place", senderName);
                                                return resp;
                                            }
                                        } else {//if receiver has company account and chn in it
                                            resp = qSender.sendAuthorisationRequest(wrapper);
                                            logger.info("notification fowarded to queue - notification code: [{}]", wrapper.getCode());
                                            return resp;
                                        }
                                    }
                                    resp.setRetn(304);
                                    resp.setDesc("The holder - " + senderName + " - does not have the sufficient share units to make this transaction.");
                                    logger.info("The holder - [{}] - does not have the sufficient share units to make this transaction", senderName);
                                    return resp;
                                }
                                resp.setRetn(304);
                                resp.setDesc("The holder - " + senderName + " - has no recorded CHN in his share company account.");
                                logger.info("The holder - [{}] - has no recorded CHN in his share company account", senderName);
                                return resp;
                            }
                            resp.setRetn(304);
                            resp.setDesc("The holder - " + senderName + " - has no share company account to send any units from.");
                            logger.info("The holder - [{}] - has no share company account to send any units from", senderName);
                            return resp;
                        }
                        resp.setRetn(304);
                        resp.setDesc("The holder - " + receiverName + " - has been merged into another account. It is not an active account.");
                        logger.info("The holder - [{}] - has been merged into another account. It is not an active account", receiverName);
                        return resp;
                    }
                    resp.setRetn(304);
                    resp.setDesc("The holder - " + senderName + " - has been merged into another account. It is not an active account.");
                    logger.info("The holder - [{}] - has been merged into another account. It is not an active account", senderName);
                    return resp;
                }
                resp.setRetn(304);
                resp.setDesc("The receiving holder does not exist.");
                logger.info("The receiving holder does not exist");
                return resp;
            }
            resp.setRetn(304);
            resp.setDesc("The sending holder does not exist.");
            logger.info("The sending holder does not exist");
            return resp;
        } catch (Exception ex) {
            logger.info("error processing bond unit transfer. See error log");
            logger.error("error processing bond unit transfer - ", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process bond unit transfer. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Processes a saved request to transfer bond units between holder bond accounts.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the transfer bond unit request
     */
    public Response transferBondUnitManual_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        logger.info("authorise bond unit transfer, invoked by [{}] - notification code: [{}]", login.getUserId(), notificationCode);
        try {
            //get client company model from wrapper
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<UnitTransfer> transferList = (List<UnitTransfer>) wrapper.getModel();
            UnitTransfer unitTransfer = transferList.get(0);
            
            boolean senderHolderExists = hq.checkHolderAccount(unitTransfer.getHolderIdFrom());
            boolean receiverHolderExists = hq.checkHolderAccount(unitTransfer.getHolderIdTo());

            if (senderHolderExists) {//check if sender exists
                org.greenpole.hibernate.entity.Holder senderHolder = hq.getHolder(unitTransfer.getHolderIdFrom());
                String senderName = senderHolder.getFirstName() + " " + senderHolder.getLastName();
                
                if (receiverHolderExists) {//check if receiver exists
                    org.greenpole.hibernate.entity.Holder receiverHolder = hq.getHolder(unitTransfer.getHolderIdTo());
                    String receiverName = receiverHolder.getFirstName() + " " + receiverHolder.getLastName();
                    
                    if (senderHolder.isPryHolder()) {//check if sender is primary account
                        
                        if (receiverHolder.isPryHolder()) {//check if receiver is primary account

                            boolean senderHolderBondAcctExists = hq.checkHolderBondAccount(unitTransfer.getHolderIdFrom(), unitTransfer.getBondOfferId());

                            if (senderHolderBondAcctExists) {//check if sender has account
                                org.greenpole.hibernate.entity.HolderBondAccount senderBondAcct = hq.getHolderBondAccount(unitTransfer.getHolderIdFrom(), unitTransfer.getBondOfferId());
                                boolean senderHolderBAChnExists = hq.checkHolderBondAccount(senderBondAcct.getChn());

                                if (senderHolderBAChnExists) {//check if sender has chn in company account
                                    boolean receiverHolderBondAcctExists = hq.checkHolderBondAccount(unitTransfer.getHolderIdTo(), unitTransfer.getBondOfferId());

                                    if (receiverHolderBondAcctExists) {//check if receiver has account
                                        org.greenpole.hibernate.entity.HolderBondAccount receiverBondAcct = hq.getHolderBondAccount(unitTransfer.getHolderIdTo(), unitTransfer.getBondOfferId());
                                        boolean receiverHolderBAChnExists = hq.checkHolderBondAccount(receiverBondAcct.getChn());

                                        if (receiverHolderBAChnExists) {
                                            return invokeTransfer(senderBondAcct, unitTransfer, receiverBondAcct, resp, senderName, receiverName);
                                        }
                                        //no chn in receiver company account? attempt to put one for them
                                        if (receiverHolder.getChn() != null && !receiverHolder.getChn().equalsIgnoreCase("")) {
                                            receiverBondAcct.setChn(receiverHolder.getChn());
                                            //proceed with transfer
                                            return invokeTransfer(senderBondAcct, unitTransfer, receiverBondAcct, resp, senderName, receiverName);
                                        }
                                        //no chn in receiver main account as well? Inform user of database mismatch.
                                        //since company account exists, chn must be present.
                                        //system administrator should be informed to take appropriate action.
                                        resp.setRetn(305);
                                        resp.setDesc("Database mismatch. The holder - " + senderName + " - has a bond account but no chn."
                                                + "\nContact Administrator. Holder cannot have bond account without chn.");
                                        logger.info("Database mismatch. The holder - [{}] - has bond account but no chn. Business rule violation", senderName);
                                        return resp;
                                    }
                                    //no bond account? attempt to create one for them
                                    if (receiverHolder.getChn() != null && !receiverHolder.getChn().equalsIgnoreCase("")) {
                                        org.greenpole.hibernate.entity.HolderBondAccount receiverBondAcct = new org.greenpole.hibernate.entity.HolderBondAccount();
                                        receiverBondAcct.setChn(receiverHolder.getChn());
                                        hq.createUpdateHolderBondAccount(senderBondAcct);
                                        //proceed with transfer
                                        return invokeTransfer(senderBondAcct, unitTransfer, receiverBondAcct, resp, senderName, receiverName);
                                    }
                                    //no bond account and no chn in main account? cancel transaction
                                    resp.setRetn(305);
                                    resp.setDesc("Transaction Unsuccessful. Holder - " + receiverName + " - has no chn to create a bond account. Contact System Administrator.");
                                    logger.info("Transaction Successful. Certificate could not be created for [{}]", receiverName);
                                    return resp;
                                }
                                resp.setRetn(305);
                                resp.setDesc("The holder - " + senderName + " - has no recorded CHN in his bond account. Transaction cancelled.");
                                logger.info("The holder - [{}] - has no recorded CHN in his bond account. Transaction cancelled.", senderName);
                                return resp;
                            }
                            resp.setRetn(305);
                            resp.setDesc("The holder - " + senderName + " - has no bond account to send any units from. Transaction cancelled.");
                            logger.info("The holder - [{}] - has no bond account to send any units from. Transaction cancelled", senderName);
                            return resp;
                        }
                        resp.setRetn(304);
                        resp.setDesc("The holder - " + receiverName + " - has been merged into another account. It is not an active account.");
                        logger.info("The holder - [{}] - has been merged into another account. It is not an active account", receiverName);
                        return resp;
                    }
                    resp.setRetn(304);
                    resp.setDesc("The holder - " + senderName + " - has been merged into another account. It is not an active account.");
                    logger.info("The holder - [{}] - has been merged into another account. It is not an active account", senderName);
                    return resp;
                }
                resp.setRetn(305);
                resp.setDesc("The receiving holder does not exist. Transaction cancelled.");
                logger.info("The receiving holder does not exist. Transaction cancelled");
                return resp;
            }
            resp.setRetn(305);
            resp.setDesc("The sending holder does not exist. Transaction cancelled.");
            logger.info("The sending holder does not exist. Transaction cancelled");
            return resp;
        } catch (JAXBException ex) {
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);
            
            resp.setRetn(101);
            resp.setDesc("Unable to transfer share units from authorisation. Contact System Administrator");
            
            return resp;
        } catch (Exception ex) {
            logger.info("error transfering bond units. See error log");
            logger.error("error transfering bond units - ", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to transfer bond units. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Begins transfer of share units.
     * @param senderCompAcct the sender holder
     * @param unitTransfer the share units to be transferred
     * @param receiverCompAcct the receiver holder
     * @param resp the response object to store response from the transfer
     * @param senderName the sender's name
     * @param receiverName the receiver's name
     * @return the response from the transfer
     */
    private Response invokeTransfer(org.greenpole.hibernate.entity.HolderCompanyAccount senderCompAcct, UnitTransfer unitTransfer, org.greenpole.hibernate.entity.HolderCompanyAccount receiverCompAcct, Response resp, String senderName, String receiverName) {
        if (senderCompAcct.getShareUnits() < unitTransfer.getUnits()) { //check if sender has sufficient units to transact
            boolean transfered = hq.transferShareUnits(senderCompAcct, receiverCompAcct, unitTransfer.getUnits());
            if (transfered) {
                resp.setRetn(0);
                resp.setDesc("Successful Transfer");
                logger.info("Transaction successful: [{}] units from [{}] to [{}]", unitTransfer.getUnits(), senderName, receiverName);
                return resp;
            }
            resp.setRetn(303);
            resp.setDesc("Transfer Unsuccesful. An error occured in the database engine. Contact System Administrator");
            logger.info("Transfer Unsuccesful. An error occured in the database engine"); //all methods in hibernate must throw hibernate exception
            return resp;
        }
        resp.setRetn(303);
        resp.setDesc("Transaction error. Insufficient balance in " + senderName + "'s company account.");
        logger.info("Transaction error. Insufficient balance in [{}]'s company account", senderName);
        return resp;
    }
    
    /**
     * Begins transfer of bond units.
     * @param senderBondAcct the sender holder
     * @param unitTransfer the bond units to be transferred
     * @param receiverBondAcct the receiver holder
     * @param resp the response object to store response from the transfer
     * @param senderName the sender's name
     * @param receiverName the receiver's name
     * @return the response from the transfer
     */
    private Response invokeTransfer(org.greenpole.hibernate.entity.HolderBondAccount senderBondAcct, UnitTransfer unitTransfer, org.greenpole.hibernate.entity.HolderBondAccount receiverBondAcct, Response resp, String senderName, String receiverName) {
        if (senderBondAcct.getBondUnits() < unitTransfer.getUnits()) { //check if sender has sufficient units to transact
            boolean transfered = hq.transferBondUnits(senderBondAcct, receiverBondAcct, unitTransfer.getUnits(), unitTransfer.getUnitPrice());
            if (transfered) {
                resp.setRetn(0);
                resp.setDesc("Successful Transfer");
                logger.info("Transaction successful: [{}] units from [{}] to [{}]", unitTransfer.getUnits(), senderName, receiverName);
                return resp;
            }
            resp.setRetn(305);
            resp.setDesc("Transfer Unsuccesful. An error occured in the database engine. Contact System Administrator");
            logger.info("Transfer Unsuccesful. An error occured in the database engine"); //all methods in hibernate must throw hibernate exception
            return resp;
        }
        resp.setRetn(305);
        resp.setDesc("Transaction error. Insufficient balance in " + senderName + "'s company account.");
        logger.info("Transaction error. Insufficient balance in [{}]'s company account", senderName);
        return resp;
    }
    
    /**
     * Request to view changes to holder accounts.
     * @param login the user's login details
     * @param queryParams the query parameters
     * @return the response to the holder changes request
     */
    public Response viewHolderChanges_Request(Login login, QueryHolderChanges queryParams) {
        Response resp = new Response();
        logger.info("request to query holder changes, invoked by [{}]", login.getUserId());
        
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

            Map<String, String> descriptors = Descriptor.decipherDescriptor(queryParams.getDescriptor());
            org.greenpole.hibernate.entity.HolderChanges changes_hib = new org.greenpole.hibernate.entity.HolderChanges();
            HolderChangeType change_type_hib = new HolderChangeType();

            if (descriptors.size() == 1) {
                //check that start date is properly formatted
                if (!descriptors.get("date").equalsIgnoreCase("none")) {
                    try {
                        formatter.parse(queryParams.getStart_date());
                    } catch (ParseException ex) {
                        logger.info("an error was thrown while checking the start date. See error log");
                        resp.setRetn(300);
                        resp.setDesc("Incorrect date format for start date");
                        logger.error("Incorrect date format for start date", ex);

                        return resp;
                    }
                }

                //check that start and end dates are properly formatted
                if (descriptors.get("date").equalsIgnoreCase("between")) {
                    try {
                        formatter.parse(queryParams.getStart_date());
                    } catch (ParseException ex) {
                        logger.info("an error was thrown while checking the start date. See error log");
                        resp.setRetn(300);
                        resp.setDesc("Incorrect date format for start date");
                        logger.error("Incorrect date format for start date", ex);

                        return resp;
                    }
                    try {
                        formatter.parse(queryParams.getEnd_date());
                    } catch (ParseException ex) {
                        logger.info("an error was thrown while checking the end date. See error log");
                        resp.setRetn(300);
                        resp.setDesc("Incorrect date format for end date");
                        logger.error("Incorrect date format for end date", ex);

                        return resp;
                    }
                }

                change_type_hib.setId(queryParams.getHolderChanges().getChangeTypeId());
                changes_hib.setHolderChangeType(change_type_hib);

                List<org.greenpole.hibernate.entity.HolderChanges> changes_hib_result = hq.queryHolderChanges(queryParams.getDescriptor(), changes_hib, queryParams.getStart_date(), queryParams.getEnd_date());
                List<HolderChanges> return_list = new ArrayList<>();

                //unwrap returned result list
                for (org.greenpole.hibernate.entity.HolderChanges hc : changes_hib_result) {
                    org.greenpole.hibernate.entity.Holder holder_hib = hq.getHolder(hc.getHolder().getId()); //all variables of holder
                    HolderChanges hc_model = new HolderChanges();
                    Holder holder_model = new Holder();

                    holder_model.setHolderId(holder_hib.getId());
                    holder_model.setFirstName(holder_hib.getFirstName());
                    holder_model.setLastName(holder_hib.getLastName());

                    hc_model.setCurrentForm(hc.getCurrentForm());
                    hc_model.setInitialForm(hc.getInitialForm());
                    hc_model.setChangeTypeId(hc.getHolderChangeType().getId());
                    hc_model.setHolder(holder_model); //holder object

                    return_list.add(hc_model);
                }
                logger.info("holder changes query successful");
                resp.setRetn(0);
                resp.setDesc("Successful");
                resp.setBody(return_list);

                return resp;
            }

            logger.info("descriptor length does not match expected required length");
            resp.setRetn(300);
            resp.setDesc("descriptor length does not match expected required length");

            return resp;
        } catch (Exception ex) {
            logger.info("error querying holder changes. See error log");
            logger.error("error querying holder changes - ", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to query holder changes. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Searches for a list of holders according to query parameters.
     * @param login the user's login details
     * @param queryParams the query parameters
     * @return the response to the holder query request
     */
    public Response queryHolder_Request(Login login, QueryHolder queryParams) {
        Response resp = new Response();
        logger.info("request to query holder, invoked by [{}]", login.getUserId());
        
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

            Map<String, String> descriptors = Descriptor.decipherDescriptor(queryParams.getDescriptor());

            if (descriptors.size() == 3) {
                String descriptor = queryParams.getDescriptor();

                org.greenpole.hibernate.entity.Holder h_hib_search = new org.greenpole.hibernate.entity.Holder();

                HolderResidentialAddress h_res_hib_search = new HolderResidentialAddress();
                HolderResidentialAddressId h_res_id_hib_search = new HolderResidentialAddressId();

                HolderPostalAddress h_pos_hib_search = new HolderPostalAddress();
                HolderPostalAddressId h_pos_id_hib_search = new HolderPostalAddressId();

                HolderEmailAddress h_email_hib_search = new HolderEmailAddress();
                HolderEmailAddressId h_email_id_hib_search = new HolderEmailAddressId();

                HolderPhoneNumber h_phone_hib_search = new HolderPhoneNumber();
                HolderPhoneNumberId h_phone_id_hib_search = new HolderPhoneNumberId();

                org.greenpole.hibernate.entity.HolderCompanyAccount hca_hib_search = new org.greenpole.hibernate.entity.HolderCompanyAccount();
                HolderCompanyAccountId hca_id_hib_search = new HolderCompanyAccountId();

                org.greenpole.hibernate.entity.HolderBondAccount hba_hib_search = new org.greenpole.hibernate.entity.HolderBondAccount();
                HolderBondAccountId hba_id_hib_search = new HolderBondAccountId();

                org.greenpole.hibernate.entity.Stockbroker broker_hib_search = new org.greenpole.hibernate.entity.Stockbroker();

                Holder h_model_search;
                if (queryParams.getHolder() != null) {
                    h_model_search = queryParams.getHolder();

                    h_hib_search.setFirstName(h_model_search.getFirstName());
                    h_hib_search.setMiddleName(h_model_search.getMiddleName());
                    h_hib_search.setLastName(h_model_search.getLastName());
                    h_hib_search.setGender(h_model_search.getGender());
                    try {
                        h_hib_search.setDob(formatter.parse(h_model_search.getDob()));
                    } catch (ParseException ex) {
                        logger.info("an error was thrown while checking the holder's date of birth. See error log");
                        resp.setRetn(301);
                        resp.setDesc("Incorrect date format for date of birth");
                        logger.error("Incorrect date format for date of birth", ex);
                    }
                    h_hib_search.setChn(h_model_search.getChn());
                    h_hib_search.setHolderAcctNumber(h_model_search.getHolderAcctNumber());
                    h_hib_search.setTaxExempted(h_model_search.isTaxExempted());
                    h_hib_search.setPryAddress(h_model_search.getPryAddress());
                    h_hib_search.setType(h_model_search.getType());
                    h_hib_search.setPryHolder(true); //must be set
                }

                Stockbroker broker_search;
                if (queryParams.getHolder().getHolderStockbroker() != null) {
                    broker_search = queryParams.getHolder().getHolderStockbroker();

                    broker_hib_search.setName(broker_search.getName());

                    Set broker_set = new HashSet();
                    broker_set.add(broker_hib_search);

                    h_hib_search.setStockbrokers(broker_set);
                }

                Address h_res_model_search;
                if (!queryParams.getHolder().getHolderResidentialAddresses().isEmpty() && queryParams.getHolder().getHolderResidentialAddresses() != null) {
                    h_res_model_search = queryParams.getHolder().getHolderResidentialAddresses().get(0);

                    h_res_id_hib_search.setAddressLine1(h_res_model_search.getAddressLine1());
                    h_res_id_hib_search.setState(h_res_model_search.getState());
                    h_res_id_hib_search.setCountry(h_res_model_search.getCountry());

                    h_res_hib_search.setAddressLine2(h_res_model_search.getAddressLine2());
                    h_res_hib_search.setAddressLine3(h_res_model_search.getAddressLine3());
                    h_res_hib_search.setAddressLine4(h_res_model_search.getAddressLine4());
                    h_res_hib_search.setCity(h_res_model_search.getCity());
                    h_res_hib_search.setPostCode(h_res_model_search.getPostCode());
                    h_res_hib_search.setIsPrimary(h_res_model_search.isPrimaryAddress());

                    h_res_hib_search.setId(h_res_id_hib_search);

                    Set h_res_hib_set = new HashSet();
                    h_res_hib_set.add(h_res_hib_search);

                    h_hib_search.setHolderResidentialAddresses(h_res_hib_set);
                }

                Address h_pos_model_search;
                if (!queryParams.getHolder().getHolderPostalAddresses().isEmpty() && queryParams.getHolder().getHolderPostalAddresses() != null) {
                    h_pos_model_search = queryParams.getHolder().getHolderPostalAddresses().get(0);

                    h_pos_id_hib_search.setAddressLine1(h_pos_model_search.getAddressLine1());
                    h_pos_id_hib_search.setState(h_pos_model_search.getState());
                    h_pos_id_hib_search.setCountry(h_pos_model_search.getCountry());

                    h_pos_hib_search.setAddressLine2(h_pos_model_search.getAddressLine2());
                    h_pos_hib_search.setAddressLine3(h_pos_model_search.getAddressLine3());
                    h_pos_hib_search.setAddressLine4(h_pos_model_search.getAddressLine4());
                    h_pos_hib_search.setCity(h_pos_model_search.getCity());
                    h_pos_hib_search.setPostCode(h_pos_model_search.getPostCode());
                    h_pos_hib_search.setIsPrimary(h_pos_model_search.isPrimaryAddress());

                    h_pos_hib_search.setId(h_pos_id_hib_search);

                    Set h_pos_hib_set = new HashSet();
                    h_pos_hib_set.add(h_res_hib_search);

                    h_hib_search.setHolderPostalAddresses(h_pos_hib_set);
                }

                EmailAddress h_email_model_search;
                if (!queryParams.getHolder().getHolderEmailAddresses().isEmpty() && queryParams.getHolder().getHolderEmailAddresses() != null) {
                    h_email_model_search = queryParams.getHolder().getHolderEmailAddresses().get(0);

                    h_email_id_hib_search.setEmailAddress(h_email_model_search.getEmailAddress());

                    h_email_hib_search.setIsPrimary(h_email_model_search.isPrimaryEmail());

                    h_email_hib_search.setId(h_email_id_hib_search);

                    Set h_email_hib_set = new HashSet();
                    h_email_hib_set.add(h_email_hib_search);

                    h_hib_search.setHolderEmailAddresses(h_email_hib_set);
                }

                PhoneNumber h_phone_model_search;
                if (!queryParams.getHolder().getHolderPhoneNumbers().isEmpty() && queryParams.getHolder().getHolderPhoneNumbers() != null) {
                    h_phone_model_search = queryParams.getHolder().getHolderPhoneNumbers().get(0);

                    h_phone_id_hib_search.setPhoneNumber(h_phone_model_search.getPhoneNumber());

                    h_phone_hib_search.setIsPrimary(h_phone_model_search.isPrimaryPhoneNumber());

                    h_phone_hib_search.setId(h_phone_id_hib_search);

                    Set h_phone_hib_set = new HashSet();
                    h_phone_hib_set.add(h_email_hib_search);

                    h_hib_search.setHolderPhoneNumbers(h_phone_hib_set);
                }

                HolderCompanyAccount hca_model_search;
                if (!queryParams.getHolder().getHolderCompanyAccounts().isEmpty() && queryParams.getHolder().getHolderCompanyAccounts() != null) {
                    hca_model_search = queryParams.getHolder().getHolderCompanyAccounts().get(0);

                    hca_id_hib_search.setClientCompanyId(hca_model_search.getClientCompanyId());

                    hca_hib_search.setEsop(hca_model_search.isEsop());

                    hca_hib_search.setId(hca_id_hib_search);

                    Set hca_hib_set = new HashSet();
                    hca_hib_set.add(hca_hib_search);

                    h_hib_search.setHolderCompanyAccounts(hca_hib_set);
                }

                HolderBondAccount hba_model_search;
                if (!queryParams.getHolder().getHolderBondAccounts().isEmpty() && queryParams.getHolder().getHolderBondAccounts() != null) {
                    hba_model_search = queryParams.getHolder().getHolderBondAccounts().get(0);

                    hba_id_hib_search.setBondOfferId(hba_model_search.getBondOfferId());

                    hba_hib_search.setId(hba_id_hib_search);

                    Set hba_hib_set = new HashSet();
                    hba_hib_set.add(hba_hib_search);

                    h_hib_search.setHolderBondAccounts(hba_hib_set);
                }

                Map<String, Integer> shareUnits_search;
                if (!queryParams.getShareUnits().isEmpty() && queryParams.getShareUnits() != null) {
                    shareUnits_search = queryParams.getShareUnits();
                } else {
                    shareUnits_search = new HashMap<>();
                }

                Map<String, Integer> totalHoldings_search;
                if (!queryParams.getTotalHoldings().isEmpty() && queryParams.getTotalHoldings() != null) {
                    totalHoldings_search = queryParams.getTotalHoldings();
                } else {
                    totalHoldings_search = new HashMap<>();
                }

                List<org.greenpole.hibernate.entity.Holder> h_search_result = hq.queryHolderAccount(descriptor, h_hib_search, shareUnits_search, totalHoldings_search);

                //unwrap result and set in holder front-end model
                List<Holder> h_model_out = new ArrayList<>();

                List<Address> h_res_out = new ArrayList<>();
                List<Address> h_pos_out = new ArrayList<>();
                List<PhoneNumber> h_phone_out = new ArrayList<>();
                List<EmailAddress> h_email_out = new ArrayList<>();

                for (org.greenpole.hibernate.entity.Holder h_hib_out : h_search_result) {
                    Holder h = new Holder();

                    h.setFirstName(h_hib_out.getFirstName());
                    h.setMiddleName(h_hib_out.getMiddleName());
                    h.setLastName(h_hib_out.getLastName());
                    h.setGender(h_hib_out.getGender());
                    h.setDob(formatter.format(h_hib_out.getDob()));
                    h.setChn(h_hib_out.getChn());
                    h.setHolderAcctNumber(h_hib_out.getHolderAcctNumber());
                    h.setTaxExempted(h_hib_out.isTaxExempted());
                    h.setPryAddress(h_hib_out.getPryAddress());
                    h.setType(h_hib_out.getType());
                    h.setPryHolder(h_hib_out.isPryHolder());

                    //get all available addresses, email addresses and phone numbers
                    List<HolderResidentialAddress> res_hib_list = hq.getHolderResidentialAddress(h_hib_out.getId());
                    for (HolderResidentialAddress res_hib_out : res_hib_list) {
                        HolderResidentialAddressId res_id_hib_out = res_hib_out.getId();

                        Address addy_model = new Address();

                        addy_model.setAddressLine1(res_id_hib_out.getAddressLine1());
                        addy_model.setState(res_id_hib_out.getState());
                        addy_model.setCountry(res_id_hib_out.getCountry());
                        addy_model.setAddressLine2(res_hib_out.getAddressLine2());
                        addy_model.setAddressLine3(res_hib_out.getAddressLine3());
                        addy_model.setAddressLine4(res_hib_out.getAddressLine4());
                        addy_model.setPostCode(res_hib_out.getPostCode());
                        addy_model.setCity(res_hib_out.getCity());
                        addy_model.setPrimaryAddress(res_hib_out.isIsPrimary());

                        h_res_out.add(addy_model);
                    }
                    h.setHolderResidentialAddresses(h_res_out);

                    List<HolderPostalAddress> pos_hib_list = hq.getHolderPostalAddress(h_hib_out.getId());
                    for (HolderPostalAddress pos_hib_out : pos_hib_list) {
                        HolderPostalAddressId pos_id_hib_out = pos_hib_out.getId();

                        Address addy_model = new Address();

                        addy_model.setAddressLine1(pos_id_hib_out.getAddressLine1());
                        addy_model.setState(pos_id_hib_out.getState());
                        addy_model.setCountry(pos_id_hib_out.getCountry());
                        addy_model.setAddressLine2(pos_hib_out.getAddressLine2());
                        addy_model.setAddressLine3(pos_hib_out.getAddressLine3());
                        addy_model.setAddressLine4(pos_hib_out.getAddressLine4());
                        addy_model.setPostCode(pos_hib_out.getPostCode());
                        addy_model.setCity(pos_hib_out.getCity());
                        addy_model.setPrimaryAddress(pos_hib_out.isIsPrimary());

                        h_pos_out.add(addy_model);
                    }
                    h.setHolderPostalAddresses(h_pos_out);

                    List<HolderEmailAddress> email_hib_list = hq.getHolderEmailAddresses(h_hib_out.getId());
                    for (HolderEmailAddress email_hib_out : email_hib_list) {
                        HolderEmailAddressId email_id_hib_out = email_hib_out.getId();

                        EmailAddress email_model_out = new EmailAddress();

                        email_model_out.setEmailAddress(email_id_hib_out.getEmailAddress());
                        email_model_out.setPrimaryEmail(email_hib_out.isIsPrimary());

                        h_email_out.add(email_model_out);
                    }
                    h.setHolderEmailAddresses(h_email_out);

                    List<HolderPhoneNumber> phone_hib_list = hq.getHolderPhoneNumbers(h_hib_out.getId());
                    for (HolderPhoneNumber phone_hib_out : phone_hib_list) {
                        HolderPhoneNumberId phone_id_hib_out = phone_hib_out.getId();

                        PhoneNumber phone_model_out = new PhoneNumber();

                        phone_model_out.setPhoneNumber(phone_id_hib_out.getPhoneNumber());
                        phone_model_out.setPrimaryPhoneNumber(phone_hib_out.isIsPrimary());

                        h_phone_out.add(phone_model_out);
                    }
                    h.setHolderPhoneNumbers(h_phone_out);

                    h_model_out.add(h);
                }

                logger.info("holder query successful");
                resp.setRetn(0);
                resp.setDesc("Successful");
                resp.setBody(h_model_out);

                return resp;
            }

            logger.info("holder query unsuccessful");
            resp.setRetn(301);
            resp.setDesc("Unsuccessful holder query, due to incomplete descriptor. Contact system administrator");

            return resp;
        } catch (Exception ex) {
            logger.info("error querying holder. See error log");
            logger.error("error querying holder - ", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to query holder. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
}
