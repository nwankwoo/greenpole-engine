/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.logic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.model.Address;
import org.greenpole.entity.tags.AddressTag;
import org.greenpole.entity.model.EmailAddress;
import org.greenpole.entity.model.PhoneNumber;
import org.greenpole.entity.model.clientcompany.UnitTransfer;
import org.greenpole.entity.model.holder.Administrator;
import org.greenpole.entity.model.holder.Holder;
import org.greenpole.entity.model.holder.HolderBondAccount;
import org.greenpole.entity.model.holder.HolderChanges;
import org.greenpole.entity.model.holder.HolderCompanyAccount;
import org.greenpole.entity.model.holder.HolderMerger;
import org.greenpole.entity.model.holder.HolderSignature;
import org.greenpole.entity.model.holder.PowerOfAttorney;
import org.greenpole.entity.model.holder.QueryHolder;
import org.greenpole.entity.model.holder.QueryHolderChanges;
import org.greenpole.entity.model.stockbroker.Stockbroker;
import org.greenpole.entity.model.taguser.TagUser;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.entity.model.holder.QueryHolderConsolidation;
import org.greenpole.entity.notification.NotificationType;
import org.greenpole.hibernate.entity.AdministratorEmailAddress;
import org.greenpole.hibernate.entity.AdministratorPhoneNumber;
import org.greenpole.hibernate.entity.AdministratorPostalAddress;
import org.greenpole.hibernate.entity.AdministratorResidentialAddress;
import org.greenpole.hibernate.entity.Bank;
import org.greenpole.hibernate.entity.BondOffer;
import org.greenpole.hibernate.entity.ClientCompany;
import org.greenpole.hibernate.entity.CompanyAccountConsolidation;
import org.greenpole.hibernate.entity.HolderBondAccountId;
import org.greenpole.hibernate.entity.HolderChangeType;
import org.greenpole.hibernate.entity.HolderCompanyAccountId;
import org.greenpole.hibernate.entity.HolderEmailAddress;
import org.greenpole.hibernate.entity.HolderPhoneNumber;
import org.greenpole.hibernate.entity.HolderPostalAddress;
import org.greenpole.hibernate.entity.HolderResidentialAddress;
import org.greenpole.hibernate.entity.HolderType;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.GeneralComponentQuery;
import org.greenpole.hibernate.query.HolderComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.BytesConverter;
import org.greenpole.util.Descriptor;
import org.greenpole.util.GreenpoleFile;
import org.greenpole.util.Notification;
import org.greenpole.util.properties.GreenpoleProperties;
import org.greenpole.util.properties.NotificationProperties;
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
    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();
    private final GreenpoleProperties greenProp = new GreenpoleProperties(HolderComponentLogic.class);
    private final NotificationProperties notificationProp = new NotificationProperties(HolderComponentLogic.class);
    private static final Logger logger = LoggerFactory.getLogger(HolderComponentLogic.class);
    
    /**
     * Request to merge a holder account / multiple holder accounts to a primary holder account.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param accountsToMerge the merge holder accounts object containing merge detail
     * @return response to the merge holder accounts request
     */
    public Response mergeHolderAccounts_Request(Login login, String authenticator, HolderMerger accountsToMerge) {
        logger.info("request holder accounts merge, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        
        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;
            
            org.greenpole.hibernate.entity.Holder h_hib;
            List<org.greenpole.hibernate.entity.Holder> h_hib_list = new ArrayList<>();
            
            if (accountsToMerge.getPrimaryHolder() == null) {
                resp.setRetn(300);
                resp.setDesc("Primary holder not set.");
                logger.info("Primary holder not set - [{}]", login.getUserId());
                return resp;
            }
            
            if (!hq.checkHolderAccount(accountsToMerge.getPrimaryHolder().getHolderId())) {
                resp.setRetn(300);
                resp.setDesc("The chosen primary holder account does not exist - ID " + accountsToMerge.getPrimaryHolder().getHolderId());
                logger.info("The chosen primary holder account does not exist - [{}]", login.getUserId());
                return resp;
            }
            
            if (!hq.hasCompanyAccount(accountsToMerge.getPrimaryHolder().getHolderId())) {
                resp.setRetn(300);
                resp.setDesc("The chosen primary holder account has no company account.");
                logger.info("The chosen primary holder account has no company account - [{}]", login.getUserId());
                return resp;
            }
            
            if (hq.holderHasEsopAccount(accountsToMerge.getPrimaryHolder().getHolderId())) {
                resp.setRetn(300);
                resp.setDesc("The chosen primary holder account has an ESOP account. Merge not allowed.");
                logger.info("The chosen primary holder account has an ESOP account. Merge not allowed - [{}]", login.getUserId());
                return resp;
            }
            
            if (accountsToMerge.getSecondaryHolders() == null || accountsToMerge.getSecondaryHolders().isEmpty()) {
                resp.setRetn(300);
                resp.setDesc("There are no secondary holder accounts to merge.");
                logger.info("There are no secondary holder accounts to merge - [{}]", login.getUserId());
                return resp;
            }
            
            h_hib = hq.getHolder(accountsToMerge.getPrimaryHolder().getHolderId());
            
            boolean pryCheckPassed = false;
            boolean secCheckPassed = false;
            boolean discovered = false;
            
            Set bonds = new HashSet();
            int bondCount = 0;
            
            //check primary account
            if (h_hib.getPryHolder() && h_hib.getChn() != null && !"".equals(h_hib.getChn()) && !h_hib.getMerged()) {
                pryCheckPassed = true;
            }
            
            //check secondary accounts
            for (Holder h : accountsToMerge.getSecondaryHolders()) {
                if (!hq.checkHolderAccount(h.getHolderId())) {
                    resp.setRetn(300);
                    resp.setDesc("The chosen secondary holder account does not exist. Holder ID - " + h.getHolderId());
                    logger.info("The chosen secondary holder account does not exist. Holder ID {} - [{}]", h.getHolderId(), login.getUserId());
                    return resp;
                }
                
                if (!hq.hasCompanyAccount(h.getHolderId())) {
                    resp.setRetn(300);
                    resp.setDesc("The chosen secondary holder account has no company account.");
                    logger.info("The chosen secondary holder account has no company account - [{}]", login.getUserId());
                    return resp;
                }
                
                if (hq.holderHasEsopAccount(h.getHolderId())) {
                    resp.setRetn(300);
                    resp.setDesc("The chosen secondary holder account has an ESOP account. Merge not allowed.");
                    logger.info("The chosen secondary holder account has an ESOP account. Merge not allowed - [{}]", login.getUserId());
                    return resp;
                }
                
                org.greenpole.hibernate.entity.Holder sec_h = hq.getHolder(h.getHolderId());
                if (sec_h.getPryHolder() && sec_h.getChn() != null && !"".equals(sec_h.getChn()) && !sec_h.getMerged()) {
                    h_hib_list.add(sec_h);
                    secCheckPassed = true;
                } else {
                    secCheckPassed = false;
                    break;
                }
                
                List<org.greenpole.hibernate.entity.HolderBondAccount> pry_bonds = hq.getAllHolderBondAccounts(h_hib.getId());
                List<org.greenpole.hibernate.entity.HolderBondAccount> sec_bonds = hq.getAllHolderBondAccounts(h.getHolderId());
                for (org.greenpole.hibernate.entity.HolderBondAccount sec_bond : sec_bonds) {
                    discovered = false;
                    for (org.greenpole.hibernate.entity.HolderBondAccount pry_bond : pry_bonds) {
                        if (sec_bond.getId().getBondOfferId() == pry_bond.getId().getBondOfferId()) {
                            discovered = true;
                            break;
                        }
                    }
                    if (!discovered) {
                        bonds.add(sec_bond.getId().getBondOfferId());
                        ++bondCount;
                        if (bondCount != bonds.size()) {
                            secCheckPassed = false;
                            break;
                        }
                    } else {
                        secCheckPassed = false;
                        break;
                    }
                }
            }
            
            if (pryCheckPassed && secCheckPassed) {
                wrapper = new NotificationWrapper();
                
                prop = new NotifierProperties(HolderComponentLogic.class);
                qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                        prop.getAuthoriserNotifierQueueName());

                logger.info("all holder accounts qualify for merge - [{}]", login.getUserId());
                
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
                descMsg += ", requested by " + login.getUserId();
                
                //wrap unit transfer object in notification object, along with other information
                wrapper.setCode(notification.createCode(login));
                wrapper.setDescription(descMsg);
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setNotificationType(NotificationType.merge_accounts.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(holderMerger);
                
                logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                resp = qSender.sendAuthorisationRequest(wrapper);
                return resp;
            } else {
                String errorMsg = "";
                if (!pryCheckPassed) {
                    errorMsg += "\nThe chosen primary holder account has already been merged into another account.";
                    logger.info("The chosen primary holder account has already been merged into another account - [{}]", login.getUserId());
                } else if (!secCheckPassed) {
                    if (bondCount != bonds.size()) {
                        errorMsg += "\nSeveral choosen secondary holder accoutns have bonds of the same bond offer. Cannot merge.";
                        logger.info("Several choosen secondary holder accoutns have bonds of the same bond offer. Cannot merge - [{}]", login.getUserId());
                    } else if (discovered) {
                        errorMsg += "\nThe chosen secondary holder accounts (or one of them) have similar bond offers to the primary account. Cannot merge.";
                        logger.info("The chosen secondary holder accounts (or one of them) have similar bond offers to the primary account. Cannot merge - [{}]", login.getUserId());
                    } else {
                        errorMsg += "\nThe chosen secondary holder accounts (or one of them) has already been merged into another account or has no CHN.";
                        logger.info("The chosen secondary holder accounts (or one of them) has already been merged into another account or has no CHN - [{}]", login.getUserId());
                    }
                }
                resp.setRetn(300);
                resp.setDesc("Error: " + errorMsg);
                return resp;
            }
        } catch (Exception ex) {
            logger.info("error processing holder accounts merge. See error log - [{}]", login.getUserId());
            logger.error("error processing holder accounts merge - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process holder accounts merge. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Processes a saved request to merge holder accounts, according to a specified notification code.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the merge holder accounts request
     */
    public Response mergeHolderAccounts_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        Notification notification = new Notification();
        logger.info("authorise holder accounts merge, invoked by [{}] - notification code: [{}]", login.getUserId(), notificationCode);
        
        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<HolderMerger> merger_list = (List<HolderMerger>) wrapper.getModel();
            HolderMerger merger = merger_list.get(0);
            
            boolean pryHolderExists = hq.checkHolderAccount(merger.getPrimaryHolder().getHolderId());
            
            if (pryHolderExists) {
                //get the primary holder first, and company / bond accounts
                org.greenpole.hibernate.entity.Holder pryHolder = hq.getHolder(merger.getPrimaryHolder().getHolderId());
                logger.info("retrieved primary holder information from hibernate - [{}]", login.getUserId());

                if (pryHolder.getPryHolder() && !"".equals(pryHolder.getChn()) && pryHolder.getChn() != null && !pryHolder.getMerged()) {
                    logger.info("primary holder passes set rules - [{}]", login.getUserId());
                    List<org.greenpole.hibernate.entity.Holder> secHolders = new ArrayList<>();
                    boolean similarToActive = false;

                    List<org.greenpole.hibernate.entity.HolderBondAccount> pryBondAccts = new ArrayList<>();//all pry bond accts to be checked
                    boolean pryHasBondAccts = hq.hasBondAccount(pryHolder.getId());

                    if (pryHasBondAccts) {
                        pryBondAccts = hq.getAllHolderBondAccounts(pryHolder.getId());
                        logger.info("primary holder has bond accounts - [{}]", login.getUserId());
                    }

                    List<org.greenpole.hibernate.entity.HolderCompanyAccount> secHolderCompAccts = new ArrayList<>();//all sec comp accts to be moved
                    List<org.greenpole.hibernate.entity.HolderBondAccount> secHolderBondAccts = new ArrayList<>();//all sec bond accts to be checked

                    //attend to each secondary holder account
                    mainloop:
                    for (Holder sec_h_model : merger.getSecondaryHolders()) {
                        boolean secHolderExists = hq.checkHolderAccount(sec_h_model.getHolderId());
                        if (secHolderExists) {
                            logger.info("selected secondary holder exists - [{}]", login.getUserId());
                            org.greenpole.hibernate.entity.Holder secHolder = hq.getHolder(sec_h_model.getHolderId());//get the secondary holder
                            
                            if (secHolder.getPryHolder() && !"".equals(secHolder.getChn()) && secHolder.getChn() != null && !secHolder.getMerged()) {
                                logger.info("selected secondary passes set rules. Adding to special secondary holders list - [{}]", login.getUserId());
                                secHolders.add(secHolder);//add secondary holder to list

                                List<org.greenpole.hibernate.entity.HolderCompanyAccount> secCompAccts = new ArrayList<>();//the secondary holder's company accounts
                                List<org.greenpole.hibernate.entity.HolderBondAccount> secBondAccts = new ArrayList<>();//the secondary holder's bond accounts

                                boolean secHasCompAccts = hq.hasCompanyAccount(sec_h_model.getHolderId());//if secondary holder has company accounts
                                boolean secHasBondAccts = hq.hasBondAccount(sec_h_model.getHolderId());//if secondary holder has bond accounts

                                if (secHasCompAccts) {
                                    secCompAccts = hq.getAllHolderCompanyAccounts(sec_h_model.getHolderId());
                                    logger.info("selected secondary holder has company accounts - [{}]", login.getUserId());
                                } else {
                                    resp.setRetn(301);
                                    resp.setDesc("Merge unsuccessful. The secondary Holder - " + secHolder.getFirstName() + " " + secHolder.getLastName()
                                            + " - account has no company accounts."
                                            + "\nContact Administrator.");
                                    logger.info("Merge unsuccessful. The secondary Holder - [{}] - account has no company accounts - [{}]",
                                            secHolder.getFirstName() + " " + secHolder.getLastName(), login.getUserId());
                                    return resp;
                                }

                                if (secHasBondAccts) {
                                    secBondAccts = hq.getAllHolderBondAccounts(sec_h_model.getHolderId());
                                    logger.info("selected secondary holder has bond accounts - [{}]", login.getUserId());
                                }

                                //search through company and bond accounts 
                                for (org.greenpole.hibernate.entity.HolderCompanyAccount sec_hca : secCompAccts) {
                                    secHolderCompAccts.add(sec_hca);//add secondary company accounts to list
                                }

                                for (org.greenpole.hibernate.entity.HolderBondAccount sec_hba : secBondAccts) {
                                    if ((sec_hba.getRemainingPrincipalValue() != null && sec_hba.getRemainingPrincipalValue() > 0) ||
                                            (sec_hba.getBondUnits() != null && sec_hba.getBondUnits() > 0)) {
                                        for (org.greenpole.hibernate.entity.HolderBondAccount pry_hba : pryBondAccts) {
                                            if (sec_hba.getBondOffer().getId() == pry_hba.getBondOffer().getId()) {
                                                similarToActive = true;
                                            }
                                        }
                                        if (!similarToActive) {
                                            secHolderBondAccts.add(sec_hba);//only add active bonds that have no similar bond offers with any bond acct in the pry holder
                                        } else {
                                            break mainloop;//more readable, otherwise use return statement
                                        }
                                    }
                                }
                            } else {
                                resp.setRetn(301);
                                resp.setDesc("Merge unsuccessful. The secondary Holder - " + secHolder.getFirstName() + " " + secHolder.getLastName()
                                        + " - account has already been merged, or is missing its CHN."
                                        + "\nContact Administrator.");
                                logger.info("Merge unsuccessful. The secondary Holder - [{}] - account has already been merged, or is missing its CHN - [{}]",
                                        secHolder.getFirstName() + " " + secHolder.getLastName(), login.getUserId());
                                return resp;
                            }
                        } else {
                            resp.setRetn(301);
                            resp.setDesc("Merge unsuccessful. The secondary Holder, id - " + sec_h_model.getHolderId()
                                    + " - has already been merged, or is missing its CHN."
                                    + "\nContact Administrator.");
                            logger.info("Merge unsuccessful. The secondary Holder, id - [{}]  - has already been merged, or is missing its CHN  - [{}]",
                                    sec_h_model.getHolderId(), login.getUserId());
                            return resp;
                        }
                    }
                    boolean merged;
                    if (!similarToActive) {
                        merged = hq.mergeHolderAccounts(pryHolder, secHolders, secHolderCompAccts, secHolderBondAccts, merger.getPryHolderOriginalValues());
                    } else {
                        resp.setRetn(301);
                        resp.setDesc("Merge unsuccessful, because one of the secondary holders has an active bond account "
                                + "similar to a bond account in the primary holder");
                        logger.info("Merge unsuccessful, because one of the secondary holders has an active bond account "
                                + "similar to a bond account in the primary holder - [{}]", login.getUserId());
                        return resp;
                    }

                    if (merged) {
                        notification.markAttended(notificationCode);
                        resp.setRetn(0);
                        resp.setDesc("Successful merge");
                        logger.info("Successful merge - [{}]", login.getUserId());
                        return resp;
                    } else {
                        resp.setRetn(301);
                        resp.setDesc("Merge unsuccessful due to database error. Contact Administrator.");
                        logger.info("Merge unsuccessful due to database error - [{}]", login.getUserId());
                        return resp;
                    }
                }
                resp.setRetn(301);
                resp.setDesc("Merge unsuccessful. Primary Holder account has already been merged, or is missing its CHN."
                        + "\nContact Administrator.");
                logger.info("Merge unsuccessful. Primary Holder account has already been merged, or is missing its CHN - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(301);
            resp.setDesc("Merge unsuccessful. Primary Holder account does not exist. Contact Administrator.");
            logger.info("Merge unsuccessful. Primary Holder account does not exist - [{}]", login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);
            
            resp.setRetn(98);
            resp.setDesc("Unable to merge holder accounts. Contact System Administrator");
            
            return resp;
        } catch (Exception ex) {
            logger.info("error merging holder accounts. See error log");
            logger.error("error merging holder accounts - ", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to merge. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Request to demerge multiple holder accounts from a primary holder account.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param accountsToDemerge the merge holder accounts object containing demerge details
     * @return response to the demerge holder accounts request
     */
    public Response demergeHolderAccounts_Request(Login login, String authenticator, HolderMerger accountsToDemerge) {
        logger.info("request holder accounts demerge, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        
        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;
            
            org.greenpole.hibernate.entity.Holder h_hib;
            
            if (accountsToDemerge.getPrimaryHolder() == null) {
                resp.setRetn(302);
                resp.setDesc("Primary holder not set.");
                logger.info("Primary holder not set - [{}]", login.getUserId());
                return resp;
            }
            
            if (!hq.checkHolderAccount(accountsToDemerge.getPrimaryHolder().getHolderId())) {
                resp.setRetn(302);
                resp.setDesc("The chosen primary holder account has does not exist.");
                logger.info("The chosen primary holder account has does not exist - [{}]", login.getUserId());
                return resp;
            }
            
            if (!hq.hasCompanyAccount(accountsToDemerge.getPrimaryHolder().getHolderId())) {
                resp.setRetn(302);
                resp.setDesc("The chosen primary holder account has no company account.");
                logger.info("The chosen primary holder account has no company account - [{}]", login.getUserId());
                return resp;
            }
            
            if (!hq.checkSecondaryHolders(accountsToDemerge.getPrimaryHolder().getHolderId())) {
                resp.setRetn(302);
                resp.setDesc("The chosen primary holder account has no secondary holder accounts merged to it.");
                logger.info("The chosen primary holder account has no secondary holder accounts merged to it - [{}]", login.getUserId());
                return resp;
            }
            
            if (hq.holderHasEsopAccount(accountsToDemerge.getPrimaryHolder().getHolderId())) {
                resp.setRetn(302);
                resp.setDesc("The chosen primary holder account has an ESOP account. Merge not allowed.");
                logger.info("The chosen primary holder account has an ESOP account. Merge not allowed - [{}]", login.getUserId());
                return resp;
            }
            
            h_hib = hq.getHolder(accountsToDemerge.getPrimaryHolder().getHolderId());
            
            boolean pryCheckPassed = false;
            
            //check primary account
            if (h_hib.getPryHolder() && h_hib.getMerged() && h_hib.getChn() != null && !"".equals(h_hib.getChn())) {
                pryCheckPassed = true;
            }
            
            if (pryCheckPassed) {
                wrapper = new NotificationWrapper();
                
                prop = new NotifierProperties(HolderComponentLogic.class);
                qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                        prop.getAuthoriserNotifierQueueName());

                logger.info("holder account qualifies for demerge operation - [{}]", login.getUserId());
                
                List<HolderMerger> holderMerger = new ArrayList<>();
                holderMerger.add(accountsToDemerge);
                
                String pryName = h_hib.getFirstName() + " " + h_hib.getLastName();
                
                //wrap unit transfer object in notification object, along with other information
                wrapper.setCode(notification.createCode(login));
                wrapper.setDescription("Demerge accounts from " + pryName);
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setNotificationType(NotificationType.demerge_accounts.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(holderMerger);
                
                logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                resp = qSender.sendAuthorisationRequest(wrapper);
                return resp;
            } else {
                logger.info("The chosen primary holder account has not been merged, or has but isn't a primary account - [{}]", login.getUserId());
                resp.setRetn(302);
                resp.setDesc("Error: \nThe chosen primary holder account has not been merged, or has but isn't a primary account.");
                return resp;
            }
        } catch (Exception ex) {
            logger.info("error processing holder accounts demerge. See error log - [{}]", login.getUserId());
            logger.error("error processing holder accounts demerge - ", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process holder accounts demerge. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Processes a saved request to demerge holder accounts, according to a specified notification code.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the demerge holder accounts request
     */
    public Response demergeHolderAccounts_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        Notification notification = new Notification();
        logger.info("authorise holder accounts merge, invoked by [{}] - notification code: [{}]", login.getUserId(), notificationCode);

        try {
            //get Holder Merger model from wrapper
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<HolderMerger> merger_list = (List<HolderMerger>) wrapper.getModel();
            HolderMerger demerger = merger_list.get(0);
            
            Map<org.greenpole.hibernate.entity.Holder, List<CompanyAccountConsolidation>> secondaryMergeInfo = new HashMap<>();

            boolean pryExists = hq.checkHolderAccount(demerger.getPrimaryHolder().getHolderId());
            if (pryExists) {
                logger.info("primary holder exists - [{}]", login.getUserId());
                org.greenpole.hibernate.entity.Holder pryHolder = hq.getHolder(demerger.getPrimaryHolder().getHolderId());
                
                if (pryHolder.getPryHolder() && pryHolder.getMerged() && pryHolder.getChn() != null && !"".equals(pryHolder.getChn())) {
                    logger.info("primary holder passes set rules - [{}]", login.getUserId());
                    boolean hasSecHolders = hq.checkSecondaryHolders(demerger.getPrimaryHolder().getHolderId());

                    if (hasSecHolders) {
                        logger.info("primary holder has secondary holders - [{}]", login.getUserId());
                        List<org.greenpole.hibernate.entity.Holder> secHolders = hq.getSecondaryHolderAccounts(demerger.getPrimaryHolder().getHolderId());
                        
                        logger.info("check secondary holders for necessary consolidation records - [{}]", login.getUserId());
                        for (org.greenpole.hibernate.entity.Holder secHolder : secHolders) {
                            boolean hasRecords = hq.checkInConsolidation(pryHolder.getId(), secHolder.getId());
                            boolean hasCompRecords = hq.checkInCompAcctConsolidation(pryHolder.getId(), secHolder.getId());//search to inclulde bond accounts that are still active
                            if (!hasRecords) {
                                resp.setRetn(303);
                                resp.setDesc("Demerge unsuccessful. The secondary holder - " + secHolder.getFirstName() + " " + secHolder.getLastName()
                                        + " - has no record to suggest it was involved in a merge."
                                        + "\nContact Administrator.");
                                logger.info("Demerge unsuccessful. The secondary holder - [{}] - has no record to suggest it was involved in a merge"
                                        + " - [{}]", secHolder.getFirstName() + " " + secHolder.getLastName(), login.getUserId());
                                return resp;
                            }
                            if (!hasCompRecords) {
                                resp.setRetn(303);
                                resp.setDesc("Demerge unsuccessful. The secondary holder - " + secHolder.getFirstName() + " " + secHolder.getLastName()
                                        + " - has no record to suggest its company accounts were involved in the merge."
                                        + "\nContact Administrator.");
                                logger.info("Demerge unsuccessful. The secondary holder - [{}] - has no record to suggest its company accounts were"
                                        + " involved in a merge - [{}]", secHolder.getFirstName() + " " + secHolder.getLastName(), login.getUserId());
                                return resp;
                            }
                        }
                        logger.info("check secondary holders for merged company accounts."
                                + "Ensure primary company account still has all merged share units - [{}]", login.getUserId());
                        List<CompanyAccountConsolidation> compAcctRecords;
                        for (org.greenpole.hibernate.entity.Holder secHolder : secHolders) {
                            compAcctRecords = hq.getCompanyAccountMerges(secHolder.getId());

                            for (CompanyAccountConsolidation cac : compAcctRecords) {
                                if (cac.getTransfer() != null && cac.getTransfer()) {//company account merge is set to transfer as a result of similar company acct in pry & sec holder
                                    if (cac.getForCompanyId() != null && cac.getForCompanyId() > 0) {//determine if transfer is for comp acct
                                        int finalUnit = hq.getFinalUnitAfterTransfer(cac.getTiedToCurrentHolderId(), cac.getForCompanyId(), true);
                                        org.greenpole.hibernate.entity.HolderCompanyAccount pry_hca = hq.getHolderCompanyAccount(cac.getTiedToCurrentHolderId(), cac.getForCompanyId());
                                        ClientCompany cc_info = cq.getClientCompany(cac.getForCompanyId());//for client company information
                                        if (finalUnit != pry_hca.getShareUnits()) {
                                            resp.setRetn(303);
                                            resp.setDesc("The account for the company - " + cc_info.getName() + " - has already been "
                                                    + "involved in a number of transactions, and so demerge cannot occur."
                                                    + "\nContact Administrator.");
                                            logger.info("The account for the company - [{}] - has already been "
                                                    + "involved in a number of transactions, and so demerge cannot occur - [{}]", cc_info.getName(), login.getUserId());
                                            return resp;
                                        }
                                    }
                                    
                                    if (cac.getForBondOfferId() != null && cac.getForBondOfferId() > 0) {//determine if transfer is for bond acct
                                        int finalUnit = hq.getFinalUnitAfterTransfer(cac.getTiedToCurrentHolderId(), cac.getForBondOfferId(), false);
                                        org.greenpole.hibernate.entity.HolderBondAccount pry_hba = hq.getHolderBondAccount(cac.getTiedToCurrentHolderId(), cac.getForBondOfferId());
                                        BondOffer bo_info = cq.getBondOffer(cac.getForBondOfferId());//for bond offer information
                                        if (finalUnit != pry_hba.getBondUnits()) {
                                            resp.setRetn(303);
                                            resp.setDesc("The account for the bond offer - " + bo_info.getTitle() + " - has already been "
                                                    + "involved in a number of transactions, and so demerge cannot occur."
                                                    + "\nContact Administrator.");
                                            logger.info("The account for the bond offer - [{}] - has already been "
                                                    + "involved in a number of transactions, and so demerge cannot occur - [{}]", bo_info.getTitle(), login.getUserId());
                                            return resp;
                                        }
                                    }
                                }
                            }
                            secondaryMergeInfo.put(secHolder, compAcctRecords);//if everything checks out, add secondary holder and corresponding list into map
                        }
                        
                        boolean demerge = hq.demergeHolderAccounts(pryHolder, secondaryMergeInfo);
                        if (demerge) {
                            notification.markAttended(notificationCode);
                            resp.setRetn(0);
                            resp.setDesc("Demerge Successful.");
                            logger.info("Demerge Successful - [{}]", login.getUserId());
                            return resp;
                        } else {
                            resp.setRetn(303);
                            resp.setDesc("Demerge unsuccessful. The database rejected the demerge operation. Contact Administrator.");
                            logger.info("Demerge unsuccessful. The database rejected the demerge operation - [{}]", login.getUserId());
                            return resp;
                        }
                    }
                    resp.setRetn(303);
                    resp.setDesc("Demerge unsuccessful. Primary Holder account has no secondary holder accounts."
                            + "\nContact Administrator.");
                    logger.info("Demerge unsuccessful. Primary Holder account has no secondary holder accounts - [{}]", login.getUserId());
                    return resp;
                }
                resp.setRetn(303);
                resp.setDesc("Demerge unsuccessful. Primary Holder account has not been merged, or is missing its CHN."
                        + "\nContact Administrator.");
                logger.info("Demerge unsuccessful. Primary Holder account has not been merged, or is missing its CHN - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(303);
            resp.setDesc("Demerge unsuccessful. Primary Holder account does not exist. Contact Administrator.");
            logger.info("Demerge unsuccessful. Primary Holder account does not exist - [{}]", login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            logger.info("error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(98);
            resp.setDesc("Unable to demerge holder accounts. Contact System Administrator");
            
            return resp;
        } catch (Exception ex) {
            logger.info("error demerging holder accounts. See error log - [{}]", login.getUserId());
            logger.error("error demerging holder accounts - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to demerge. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
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
        Notification notification = new Notification();
        
        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;

            boolean senderHolderExists = hq.checkHolderAccount(unitTransfer.getHolderIdFrom());
            boolean receiverHolderExists = hq.checkHolderAccount(unitTransfer.getHolderIdTo());

            if (senderHolderExists) {//check if sender exists
                logger.info("sender holder exists - [{}]", login.getUserId());
                org.greenpole.hibernate.entity.Holder senderHolder = hq.getHolder(unitTransfer.getHolderIdFrom());
                String senderName = senderHolder.getFirstName() + " " + senderHolder.getLastName();

                if (receiverHolderExists) {//check if receiver exists
                    logger.info("receiver holder exists - [{}]", login.getUserId());
                    org.greenpole.hibernate.entity.Holder receiverHolder = hq.getHolder(unitTransfer.getHolderIdTo());
                    String receiverName = receiverHolder.getFirstName() + " " + receiverHolder.getLastName();
                    
                    if (senderHolder.getPryHolder()) {//check if sender is primary
                        logger.info("sender holder is a primary account - [{}]", login.getUserId());

                        if (receiverHolder.getPryHolder()) {//check if receiver is primary
                            logger.info("receiver holder is a primary account - [{}]", login.getUserId());
                            boolean senderHolderChnExists = !"".equals(senderHolder.getChn()) && senderHolder.getChn() != null;
                            
                            if (senderHolderChnExists) {//check if sender has chn in their accounts
                                logger.info("sender holder has CHN - [{}]", login.getUserId());
                                boolean senderHolderCompAcctExists = hq.checkHolderCompanyAccount(unitTransfer.getHolderIdFrom(), unitTransfer.getClientCompanyId());

                                if (senderHolderCompAcctExists) {//check if sender account has chn
                                    logger.info("sender holder has company account - [{}]", login.getUserId());
                                    org.greenpole.hibernate.entity.HolderCompanyAccount senderCompAcct = hq.getHolderCompanyAccount(unitTransfer.getHolderIdFrom(), unitTransfer.getClientCompanyId());
                                    
                                    if (unitTransfer.getHolderIdFrom() != unitTransfer.getHolderIdTo()) {//cannot transfer between same accounts
                                        
                                        if (cq.clientCompanyIsValid(unitTransfer.getClientCompanyId())) {//transaction can only happen with valid client company
                                            
                                            if (senderCompAcct.getShareUnits() >= unitTransfer.getUnits()) {//check if sender has sufficient units to transact
                                                logger.info("sender holder has appropriate units to send - [{}]", login.getUserId());
                                                boolean receiverHolderChnExists = !"".equals(receiverHolder.getChn()) && receiverHolder.getChn() != null;
                                                boolean receiverHolderCompAcctExists = hq.checkHolderCompanyAccount(unitTransfer.getHolderIdTo(), unitTransfer.getClientCompanyId());

                                                wrapper = new NotificationWrapper();
                                                prop = new NotifierProperties(HolderComponentLogic.class);
                                                qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                                                        prop.getAuthoriserNotifierQueueName());

                                                logger.info("preparing notification for transaction between holders [{}] and [{}] - [{}]",
                                                        senderName, receiverName, login.getUserId());

                                                List<UnitTransfer> transferList = new ArrayList<>();
                                                transferList.add(unitTransfer);

                                                //wrap unit transfer object in notification object, along with other information
                                                wrapper.setCode(notification.createCode(login));
                                                wrapper.setDescription("Authenticate unit transfer between " + senderName + " and " + receiverName);
                                                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                                                wrapper.setNotificationType(NotificationType.transfer_shares.toString());
                                                wrapper.setFrom(login.getUserId());
                                                wrapper.setTo(authenticator);
                                                wrapper.setModel(transferList);

                                                if (!receiverHolderCompAcctExists) {
                                                    if (receiverHolderChnExists) {
                                                        logger.info("notification fowarded to queue - notification code: [{}] - [{}]",
                                                                wrapper.getCode(), login.getUserId());
                                                        resp = qSender.sendAuthorisationRequest(wrapper);

                                                        String originalMsg = resp.getDesc();
                                                        resp.setDesc(originalMsg + "\nHolder - " + receiverName
                                                                + " - has no active account with the company. One will be created for them upon authorisation.");
                                                        logger.info("Holder - [{}] - has no active account with the company. "
                                                                + "One will be created for them upon authorisation - [{}]", receiverName, login.getUserId());
                                                        return resp;
                                                    }
                                                    logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                                                    resp = qSender.sendAuthorisationRequest(wrapper);

                                                    String originalMsg = resp.getDesc();
                                                    resp.setDesc(originalMsg + "\nHolder - " + receiverName
                                                            + " - has no active account with the company and no CHN. A certificate will be created for them upon authorisation.");
                                                    logger.info("Holder - [{}] - has no active account with the company. A certificate will be created for them upon authorisation - [{}]",
                                                            receiverName, login.getUserId());
                                                    return resp;
                                                }
                                                logger.info("notification fowarded to queue - notification code: [{}] - [{}]",
                                                        wrapper.getCode(), login.getUserId());
                                                resp = qSender.sendAuthorisationRequest(wrapper);
                                                return resp;
                                            }
                                            resp.setRetn(304);
                                            resp.setDesc("The holder - " + senderName + " - does not have the sufficient share units to make this transaction.");
                                            logger.info("The holder - [{}] - does not have the sufficient share units to make this transaction - [{}]",
                                                    senderName, login.getUserId());
                                            return resp;
                                        }
                                        resp.setRetn(304);
                                        resp.setDesc("The client company is not valid. You cannot transact on invalid client company units.");
                                        logger.info("The client company is not valid. You cannot transact on invalid client company units - [{}]",
                                                login.getUserId());
                                        return resp;
                                    }
                                    resp.setRetn(304);
                                    resp.setDesc("Both holders are the same. Cannot transfer accounts between the same holders.");
                                    logger.info("Both holders are the same. Cannot transfer accounts between the same holders - [{}]",
                                            login.getUserId());
                                    return resp;
                                }
                                resp.setRetn(304);
                                resp.setDesc("The holder - " + senderName + " - has no share company account to send any units from.");
                                logger.info("The holder - [{}] - has no share company account to send any units from - [{}]",
                                        senderName, login.getUserId());
                                return resp;
                            }
                            resp.setRetn(304);
                            resp.setDesc("The holder - " + senderName + " - has no recorded CHN in their account."
                                    + "\nCannot transfer from an account without a CHN.");
                            logger.info("The holder - [{}] - has no recorded CHN in his account. "
                                    + "Cannot transfer from an account without a CHN - [{}]", senderName, login.getUserId());
                            return resp;
                        }
                        resp.setRetn(304);
                        resp.setDesc("The holder - " + receiverName + " - has been merged into another account. It is not an active account.");
                        logger.info("The holder - [{}] - has been merged into another account. It is not an active account - [{}]",
                                receiverName, login.getUserId());
                        return resp;
                    }
                    resp.setRetn(304);
                    resp.setDesc("The holder - " + senderName + " - has been merged into another account. It is not an active account.");
                    logger.info("The holder - [{}] - has been merged into another account. It is not an active account - [{}]",
                            senderName);
                    return resp;
                }
                resp.setRetn(304);
                resp.setDesc("The receiving holder does not exist.");
                logger.info("The receiving holder does not exist - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(304);
            resp.setDesc("The sending holder does not exist.");
            logger.info("The sending holder does not exist - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error processing share unit transfer. See error log - [{}]", login.getUserId());
            logger.error("error processing share unit transfer - [" + login.getUserId() + "]", ex);
            
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
        Notification notification = new Notification();
        logger.info("authorise share unit transfer, invoked by [{}] - notification code: [{}]", login.getUserId(), notificationCode);
        try {
            //get client company model from wrapper
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<UnitTransfer> transferList = (List<UnitTransfer>) wrapper.getModel();
            UnitTransfer unitTransfer = transferList.get(0);
            
            boolean senderHolderExists = hq.checkHolderAccount(unitTransfer.getHolderIdFrom());
            boolean receiverHolderExists = hq.checkHolderAccount(unitTransfer.getHolderIdTo());

            if (senderHolderExists) {//check if sender exists
                logger.info("sender holder exists - [{}]", login.getUserId());
                org.greenpole.hibernate.entity.Holder senderHolder = hq.getHolder(unitTransfer.getHolderIdFrom());
                String senderName = senderHolder.getFirstName() + " " + senderHolder.getLastName();
                
                if (receiverHolderExists) {//check if receiver exists
                    logger.info("receiver holder exists - [{}]", login.getUserId());
                    org.greenpole.hibernate.entity.Holder receiverHolder = hq.getHolder(unitTransfer.getHolderIdTo());
                    String receiverName = receiverHolder.getFirstName() + " " + receiverHolder.getLastName();

                    if (senderHolder.getPryHolder()) {//check if sender is a primary account
                        logger.info("sender holder is a primary account - [{}]", login.getUserId());

                        if (receiverHolder.getPryHolder()) {//check if receiver is a primary account
                            logger.info("receiver holder is a primary account - [{}]", login.getUserId());
                            boolean senderHolderChnExists = !"".equals(senderHolder.getChn()) && senderHolder.getChn() != null;
                            
                            if (senderHolderChnExists) {//check if sender receiver has chn in account
                                logger.info("sender holder is a primary account - [{}]", login.getUserId());
                                boolean senderHolderCompAcctExists = hq.checkHolderCompanyAccount(unitTransfer.getHolderIdFrom(), unitTransfer.getClientCompanyId());

                                if (senderHolderCompAcctExists) {//check if sender has company account
                                    logger.info("sender holder has company account - [{}]", login.getUserId());
                                    org.greenpole.hibernate.entity.HolderCompanyAccount senderCompAcct = hq.getHolderCompanyAccount(unitTransfer.getHolderIdFrom(), unitTransfer.getClientCompanyId());
                                    boolean receiverHolderChnExists = !"".equals(receiverHolder.getChn()) && receiverHolder.getChn() != null;
                                    boolean receiverHolderCompAcctExists = hq.checkHolderCompanyAccount(unitTransfer.getHolderIdTo(), unitTransfer.getClientCompanyId());
                                    
                                    if (receiverHolderCompAcctExists) {//check if receiver has company account and chn
                                        logger.info("receiver holder has company account - [{}]", login.getUserId());
                                        org.greenpole.hibernate.entity.HolderCompanyAccount receiverCompAcct = hq.getHolderCompanyAccount(unitTransfer.getHolderIdTo(), unitTransfer.getClientCompanyId());
                                        //begin transfer
                                        return invokeTransfer(senderCompAcct, unitTransfer, receiverCompAcct, resp, senderName, receiverName, login,
                                                notificationCode, notification);
                                    }
                                    //no company account? attempt to create one for them if they have a chn
                                    if (receiverHolderChnExists) {
                                        logger.info("receiver holder has no company account, but has CHN. "
                                                + "Company account will be created for receiver holder - [{}]", login.getUserId());
                                        org.greenpole.hibernate.entity.HolderCompanyAccount receiverCompAcct = new org.greenpole.hibernate.entity.HolderCompanyAccount();
                                        HolderCompanyAccountId receiverCompAcctId = new HolderCompanyAccountId(unitTransfer.getHolderIdTo(), unitTransfer.getClientCompanyId());
                                        receiverCompAcct.setId(receiverCompAcctId);
                                        receiverCompAcct.setEsop(false);
                                        receiverCompAcct.setShareUnits(0);
                                        receiverCompAcct.setHolderCompAccPrimary(true);
                                        receiverCompAcct.setMerged(false);
                                        hq.createUpdateHolderCompanyAccount(receiverCompAcct);
                                        logger.info("receiver holder now has company account - [{}]", login.getUserId());
                                        //proceed with transfer
                                        return invokeTransfer(senderCompAcct, unitTransfer, receiverCompAcct, resp, senderName, receiverName, login,
                                                notificationCode, notification);
                                    }
                                    //no chn in main account? create certificate
                                    //METHOD TO CREATE CERTIFICATE HERE!!!
                                    boolean certCreated = true;
                                    if (certCreated) {
                                        //QUERY CERTIFICATE FOR NUMBER
                                        String certNumber = "temp";
                                        notification.markAttended(notificationCode);
                                        resp.setRetn(0);
                                        resp.setDesc("Transaction Successful. Certificate " + certNumber + " created for " + receiverName);
                                        logger.info("Transaction Successful. Certificate [{}] created for [{}] - [{}]",
                                                certNumber, receiverName, login.getUserId());
                                        return resp;
                                    } else {
                                        resp.setRetn(305);
                                        resp.setDesc("Transaction Unsuccessful. Certificate could not be created for " + receiverName + ". Contact System Administrator.");
                                        logger.info("Transaction Successful. Certificate could not be created for [{}] - [{}]",
                                                receiverName, login.getUserId());
                                        return resp;
                                    }
                                }
                                resp.setRetn(305);
                                resp.setDesc("The holder - " + senderName + " - has no share company account to send any units from. Transaction cancelled.");
                                logger.info("The holder - [{}] - has no share company account to send any units from. Transaction cancelled - [{}]",
                                        senderName, login.getUserId());
                                return resp;
                            }
                            resp.setRetn(305);
                            resp.setDesc("The holder - " + senderName + " - has no recorded CHN in their account. Transaction cancelled.");
                            logger.info("The holder - [{}] - has no recorded CHN in their account. Transaction cancelled - [{}]",
                                    senderName, login.getUserId());
                            return resp;
                        }
                        resp.setRetn(305);
                        resp.setDesc("The holder - " + receiverName + " - has been merged into another account. It is not an active account.");
                        logger.info("The holder - [{}] - has been merged into another account. It is not an active account - [{}]",
                                receiverName, login.getUserId());
                        return resp;
                    }
                    resp.setRetn(305);
                    resp.setDesc("The holder - " + senderName + " - has been merged into another account. It is not an active account.");
                    logger.info("The holder - [{}] - has been merged into another account. It is not an active account - [{}]",
                            senderName, login.getUserId());
                    return resp;
                }
                resp.setRetn(305);
                resp.setDesc("The receiving holder does not exist. Transaction cancelled.");
                logger.info("The receiving holder does not exist. Transaction cancelled - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(305);
            resp.setDesc("The sending holder does not exist. Transaction cancelled.");
            logger.info("The sending holder does not exist. Transaction cancelled - [{}]",
                    login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            logger.info("error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(98);
            resp.setDesc("Unable to transfer share units from authorisation. Contact System Administrator");
            
            return resp;
        } catch (Exception ex) {
            logger.info("error transfering share units. See error log - [{}]", login.getUserId());
            logger.error("error transfering share units - [" + login.getUserId() + "]", ex);
            
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
        Notification notification = new Notification();
        
        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;

            boolean senderHolderExists = hq.checkHolderAccount(unitTransfer.getHolderIdFrom());
            boolean receiverHolderExists = hq.checkHolderAccount(unitTransfer.getHolderIdTo());

            if (senderHolderExists) {//check if sender exists
                logger.info("sender holder exists - [{}]", login.getUserId());
                org.greenpole.hibernate.entity.Holder senderHolder = hq.getHolder(unitTransfer.getHolderIdFrom());
                String senderName = senderHolder.getFirstName() + " " + senderHolder.getLastName();

                if (receiverHolderExists) {//check if receiver exists
                    logger.info("receiver holder exists - [{}]", login.getUserId());
                    org.greenpole.hibernate.entity.Holder receiverHolder = hq.getHolder(unitTransfer.getHolderIdTo());
                    String receiverName = receiverHolder.getFirstName() + " " + receiverHolder.getLastName();
                    
                    if (senderHolder.getPryHolder()) {
                        logger.info("sender holder is a primary account - [{}]", login.getUserId());
                        
                        if (receiverHolder.getPryHolder()) {
                            logger.info("receiver holder is a primary account - [{}]", login.getUserId());
                            boolean senderHolderChnExists = !"".equals(senderHolder.getChn()) && senderHolder.getChn() != null;
                            boolean receiverHolderChnExists = !"".equals(receiverHolder.getChn()) && receiverHolder.getChn() != null;
                            
                            if (senderHolderChnExists && receiverHolderChnExists) {//check if sender and receiver have chn in accounts
                                boolean senderHolderBondAcctExists = hq.checkHolderBondAccount(unitTransfer.getHolderIdFrom(), unitTransfer.getBondOfferId());
                                
                                if (senderHolderBondAcctExists) {//check if sender has bond account
                                    logger.info("sender holder has bond account - [{}]", login.getUserId());
                                    org.greenpole.hibernate.entity.HolderBondAccount senderBondAcct = hq.getHolderBondAccount(unitTransfer.getHolderIdFrom(), unitTransfer.getBondOfferId());
                                    boolean receiverHolderBondAcctExists = hq.checkHolderBondAccount(unitTransfer.getHolderIdTo(), unitTransfer.getBondOfferId());
                                    
                                    if (unitTransfer.getHolderIdFrom() != unitTransfer.getHolderIdTo()) {
                                        
                                        if (cq.bondOfferIsValid(unitTransfer.getBondOfferId())) {//check that bond offer is still valid
                                            
                                            if (senderBondAcct.getBondUnits() >= unitTransfer.getUnits()) { //check if sender has sufficient units to transact
                                                logger.info("sender holder has necessary units for transfer - [{}]", login.getUserId());

                                                wrapper = new NotificationWrapper();
                                                prop = new NotifierProperties(HolderComponentLogic.class);
                                                qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                                                        prop.getAuthoriserNotifierQueueName());

                                                logger.info("preparing notification for transaction between holders [{}] and [{}] - [{}]",
                                                        senderName, receiverName, login.getUserId());

                                                List<UnitTransfer> transferList = new ArrayList<>();
                                                transferList.add(unitTransfer);

                                                //wrap unit transfer object in notification object, along with other information
                                                wrapper.setCode(notification.createCode(login));
                                                wrapper.setDescription("Authenticate unit transfer between " + senderName + " and " + receiverName);
                                                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                                                wrapper.setNotificationType(NotificationType.transfer_bonds.toString());
                                                wrapper.setFrom(login.getUserId());
                                                wrapper.setTo(authenticator);
                                                wrapper.setModel(transferList);

                                                logger.info("notification fowarded to queue - notification code: [{}] - [{}]",
                                                        wrapper.getCode(), login.getUserId());
                                                resp = qSender.sendAuthorisationRequest(wrapper);

                                                //check if receiver holder has bond account, adjust message accordingly
                                                if (!receiverHolderBondAcctExists) {//if receiver has no bond account, inform user
                                                    String originalMsg = resp.getDesc();
                                                    resp.setDesc(originalMsg + "\nHolder - " + receiverName
                                                            + " - has no active bond account for the offer. One will be created for them upon authorisation.");
                                                    logger.info("Holder - [{}] - has no active bond account for the offer. "
                                                            + "One will be created for them upon authorisation - [{}]", receiverName, login.getUserId());
                                                }

                                                return resp;
                                            }
                                            resp.setRetn(306);
                                            resp.setDesc("The holder - " + senderName + " - does not have the sufficient share units to make this transaction.");
                                            logger.info("The holder - [{}] - does not have the sufficient share units to make this transaction - [{}]",
                                                    senderName, login.getUserId());
                                            return resp;
                                        }
                                        resp.setRetn(306);
                                        resp.setDesc("The bond offer is not currently valid. You cannot transact on a bond offer that has ended.");
                                        logger.info("The bond offer is not currently valid. You cannot transact on a bond offer that has ended - [{}]",
                                                login.getUserId());
                                        return resp;
                                    }
                                    resp.setRetn(306);
                                    resp.setDesc("Both holders are the same. Cannnot transfer units between the same holder.");
                                    logger.info("Both holders are the same. Cannnot transfer units between the same holder - [{}]",
                                            login.getUserId());
                                    return resp;
                                }
                                resp.setRetn(306);
                                resp.setDesc("The holder - " + senderName + " - has no bond account to send any units from.");
                                logger.info("The holder - [{}] - has no bond account to send any units from - [{}]",
                                        senderName, login.getUserId());
                                return resp;
                            }
                            resp.setRetn(306);
                            resp.setDesc("Both holders must each have a CHN in their account.");
                            logger.info("Both holders must each have a CHN in their account - [{}]", login.getUserId());
                            return resp;
                        }
                        resp.setRetn(306);
                        resp.setDesc("The holder - " + receiverName + " - has been merged into another account. It is not an active account.");
                        logger.info("The holder - [{}] - has been merged into another account. It is not an active account - [{}]",
                                receiverName, login.getUserId());
                        return resp;
                    }
                    resp.setRetn(306);
                    resp.setDesc("The holder - " + senderName + " - has been merged into another account. It is not an active account.");
                    logger.info("The holder - [{}] - has been merged into another account. It is not an active account - [{}]",
                            senderName, login.getUserId());
                    return resp;
                }
                resp.setRetn(306);
                resp.setDesc("The receiving holder does not exist.");
                logger.info("The receiving holder does not exist - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(306);
            resp.setDesc("The sending holder does not exist.");
            logger.info("The sending holder does not exist - [{}]",  login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error processing bond unit transfer. See error log - [{}]", login.getUserId());
            logger.error("error processing bond unit transfer - [" + login.getUserId() + "]", ex);
            
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
        Notification notification = new Notification();
        logger.info("authorise bond unit transfer, invoked by [{}] - notification code: [{}]", login.getUserId(), notificationCode);
        try {
            //get client company model from wrapper
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<UnitTransfer> transferList = (List<UnitTransfer>) wrapper.getModel();
            UnitTransfer unitTransfer = transferList.get(0);
            
            boolean senderHolderExists = hq.checkHolderAccount(unitTransfer.getHolderIdFrom());
            boolean receiverHolderExists = hq.checkHolderAccount(unitTransfer.getHolderIdTo());

            if (senderHolderExists) {//check if sender exists
                logger.info("sender holder exists - [{}]", login.getUserId());
                org.greenpole.hibernate.entity.Holder senderHolder = hq.getHolder(unitTransfer.getHolderIdFrom());
                String senderName = senderHolder.getFirstName() + " " + senderHolder.getLastName();
                
                if (receiverHolderExists) {//check if receiver exists
                    logger.info("receiver holder exists - [{}]", login.getUserId());
                    org.greenpole.hibernate.entity.Holder receiverHolder = hq.getHolder(unitTransfer.getHolderIdTo());
                    String receiverName = receiverHolder.getFirstName() + " " + receiverHolder.getLastName();
                    
                    if (senderHolder.getPryHolder()) {//check if sender is primary account
                        logger.info("sender holder is a primary account - [{}]", login.getUserId());
                        
                        if (receiverHolder.getPryHolder()) {//check if receiver is primary account
                            logger.info("receiver holder is a primary account - [{}]", login.getUserId());
                            boolean senderHolderChnExists = !"".equals(senderHolder.getChn()) && senderHolder.getChn() != null;
                            boolean receiverHolderChnExists = !"".equals(receiverHolder.getChn()) && receiverHolder.getChn() != null;
                            
                            if (senderHolderChnExists && receiverHolderChnExists) {//check if sender and receiver have CHN in their accounts
                                boolean senderHolderBondAcctExists = hq.checkHolderBondAccount(unitTransfer.getHolderIdFrom(), unitTransfer.getBondOfferId());
                                
                                if (senderHolderBondAcctExists) {//check if sender has bond account
                                    logger.info("sender holder has bond account - [{}]", login.getUserId());
                                    org.greenpole.hibernate.entity.HolderBondAccount senderBondAcct = hq.getHolderBondAccount(unitTransfer.getHolderIdFrom(), unitTransfer.getBondOfferId());
                                    boolean receiverHolderBondAcctExists = hq.checkHolderBondAccount(unitTransfer.getHolderIdTo(), unitTransfer.getBondOfferId());
                                    
                                    if (receiverHolderBondAcctExists) {//check if receiver has bond account
                                        logger.info("receiver holder has bond account - [{}]", login.getUserId());
                                        org.greenpole.hibernate.entity.HolderBondAccount receiverBondAcct = hq.getHolderBondAccount(unitTransfer.getHolderIdTo(), unitTransfer.getBondOfferId());
                                        //proceed with transaction
                                        return invokeTransfer(senderBondAcct, unitTransfer, receiverBondAcct, resp, senderName, receiverName, login,
                                                notificationCode, notification);
                                    }
                                    //no bond account? attempt to create one for them
                                    logger.info("receiver holder has no bond account, but has CHN. "
                                                + "Bond account will be created for receiver holder - [{}]", login.getUserId());
                                    org.greenpole.hibernate.entity.HolderBondAccount receiverBondAcct = new org.greenpole.hibernate.entity.HolderBondAccount();
                                    HolderBondAccountId receiverBondAcctId = new HolderBondAccountId(unitTransfer.getHolderIdTo(), unitTransfer.getBondOfferId());
                                    receiverBondAcct.setId(receiverBondAcctId);
                                    receiverBondAcct.setStartingPrincipalValue(0.00);
                                    receiverBondAcct.setBondUnits(0);
                                    receiverBondAcct.setDateApplied(new Date());
                                    receiverBondAcct.setMerged(false);
                                    receiverBondAcct.setHolderBondAcctPrimary(true);
                                    hq.createUpdateHolderBondAccount(receiverBondAcct);
                                    //proceed with transfer
                                    return invokeTransfer(senderBondAcct, unitTransfer, receiverBondAcct, resp, senderName, receiverName, login,
                                            notificationCode, notification);
                                }
                                resp.setRetn(307);
                                resp.setDesc("The holder - " + senderName + " - has no recorded CHN in his bond account. Transaction cancelled.");
                                logger.info("The holder - [{}] - has no recorded CHN in his bond account. Transaction cancelled - [{}]",
                                        senderName, login.getUserId());
                                return resp;
                            }
                            resp.setRetn(307);
                            resp.setDesc("The holder - " + senderName + " - has no bond account to send any units from. Transaction cancelled.");
                            logger.info("The holder - [{}] - has no bond account to send any units from. Transaction cancelled - [{}]",
                                    senderName, login.getUserId());
                            return resp;
                        }
                        resp.setRetn(307);
                        resp.setDesc("The holder - " + receiverName + " - has been merged into another account. It is not an active account.");
                        logger.info("The holder - [{}] - has been merged into another account. It is not an active account - [{}]",
                                receiverName, login.getUserId());
                        return resp;
                    }
                    resp.setRetn(307);
                    resp.setDesc("The holder - " + senderName + " - has been merged into another account. It is not an active account.");
                    logger.info("The holder - [{}] - has been merged into another account. It is not an active account - [{}]",
                            senderName, login.getUserId());
                    return resp;
                }
                resp.setRetn(307);
                resp.setDesc("The receiving holder does not exist. Transaction cancelled.");
                logger.info("The receiving holder does not exist. Transaction cancelled - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(307);
            resp.setDesc("The sending holder does not exist. Transaction cancelled.");
            logger.info("The sending holder does not exist. Transaction cancelled - [{}]", login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            logger.info("error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(98);
            resp.setDesc("Unable to transfer share units from authorisation. Contact System Administrator");
            
            return resp;
        } catch (Exception ex) {
            logger.info("error transfering bond units. See error log - [{}]", login.getUserId());
            logger.error("error transfering bond units - [" + login.getUserId() + "]", ex);
            
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
    private Response invokeTransfer(org.greenpole.hibernate.entity.HolderCompanyAccount senderCompAcct, UnitTransfer unitTransfer, org.greenpole.hibernate.entity.HolderCompanyAccount receiverCompAcct, Response resp, String senderName, String receiverName, Login login,
            String notificationCode, Notification notification)  throws JAXBException {
        if (unitTransfer.getHolderIdFrom() != unitTransfer.getHolderIdTo()) { //check if holder and sender are not the same
            if (senderCompAcct.getShareUnits() >= unitTransfer.getUnits()) { //check if sender has sufficient units to transact
                boolean transfered = hq.transferShareUnits(senderCompAcct, receiverCompAcct, unitTransfer.getUnits(), unitTransfer.getTransferTypeId());
                if (transfered) {
                    notification.markAttended(notificationCode);
                    resp.setRetn(0);
                    resp.setDesc("Successful Transfer");
                    logger.info("Transaction successful: [{}] units from [{}] to [{}] - [{}]",
                            unitTransfer.getUnits(), senderName, receiverName, login.getUserId());
                    return resp;
                }
                resp.setRetn(305);
                resp.setDesc("Transfer Unsuccesful. An error occured in the database engine. Contact System Administrator");
                logger.info("Transfer Unsuccesful. An error occured in the database engine - [{}]", login.getUserId()); //all methods in hibernate must throw hibernate exception
                return resp;
            }
            resp.setRetn(305);
            resp.setDesc("Transaction error. Insufficient balance in " + senderName + "'s company account.");
            logger.info("Transaction error. Insufficient balance in [{}]'s company account - [{}]", senderName, login.getUserId());
            return resp;
        }
        resp.setRetn(305);
        resp.setDesc("Transaction error. Sender and receiver are the same holder.");
        logger.info("Transaction error. Sender and receiver are the same holder - [{}]", login.getUserId());
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
    private Response invokeTransfer(org.greenpole.hibernate.entity.HolderBondAccount senderBondAcct, UnitTransfer unitTransfer, org.greenpole.hibernate.entity.HolderBondAccount receiverBondAcct, Response resp, String senderName, String receiverName, Login login,
            String notificationCode, Notification notification) throws JAXBException {
        if (unitTransfer.getHolderIdFrom() != unitTransfer.getHolderIdTo()) { //check if holder and sender are not the same
            if (senderBondAcct.getBondUnits() >= unitTransfer.getUnits()) { //check if sender has sufficient units to transact
                double transferValue = unitTransfer.getUnits() * unitTransfer.getUnitPrice();
                if (senderBondAcct.getRemainingPrincipalValue() >= transferValue) {
                    boolean transfered = hq.transferBondUnits(senderBondAcct, receiverBondAcct, unitTransfer.getUnits(), unitTransfer.getUnitPrice(), unitTransfer.getTransferTypeId());
                    if (transfered) {
                        notification.markAttended(notificationCode);
                        resp.setRetn(0);
                        resp.setDesc("Successful Transfer");
                        logger.info("Transaction successful: [{}] units from [{}] to [{}] - [{}]", unitTransfer.getUnits(), senderName, receiverName, login.getUserId());
                        return resp;
                    }
                    resp.setRetn(307);
                    resp.setDesc("Transfer Unsuccesful. An error occured in the database engine. Contact System Administrator");
                    logger.info("Transfer Unsuccesful. An error occured in the database engine - [{}]", login.getUserId()); //all methods in hibernate must throw hibernate exception
                    return resp;
                }
                resp.setRetn(307);
                resp.setDesc("Transfer Unsuccesful. Value of transfer is greater than value left to be redeemed.");
                logger.info("Transfer Unsuccesful. Value of transfer is greater than value left to be redeemed - [{}]", login.getUserId()); //all methods in hibernate must throw hibernate exception
                return resp;
            }
            resp.setRetn(307);
            resp.setDesc("Transaction error. Insufficient balance in " + senderName + "'s company account.");
            logger.info("Transaction error. Insufficient balance in [{}]'s company account - [{}]", senderName, login.getUserId());
            return resp;
        }
        resp.setRetn(307);
        resp.setDesc("Transaction error. Sender and receiver are the same holder.");
        logger.info("Transaction error. Sender and receiver are the same holder - [{}]", login.getUserId());
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
        Descriptor descriptorUtil = new Descriptor();
        logger.info("request to query holder changes, invoked by [{}]", login.getUserId());
        
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());

            Map<String, String> descriptors = descriptorUtil.decipherDescriptor(queryParams.getDescriptor());
            org.greenpole.hibernate.entity.HolderChanges changes_hib = new org.greenpole.hibernate.entity.HolderChanges();
            HolderChangeType change_type_hib;

            if (descriptors.size() == 1) {
                //check that start date is properly formatted
                if (!descriptors.get("date").equalsIgnoreCase("none")) {
                    try {
                        formatter.parse(queryParams.getStartDate());
                    } catch (ParseException ex) {
                        logger.info("an error was thrown while checking the start date. See error log - [{}]", login.getUserId());
                        resp.setRetn(308);
                        resp.setDesc("Incorrect date format for start date");
                        logger.error("Incorrect date format for start date - [" + login.getUserId() + "]", ex);

                        return resp;
                    }
                }

                //check that start and end dates are properly formatted
                if (descriptors.get("date").equalsIgnoreCase("between")) {
                    try {
                        formatter.parse(queryParams.getStartDate());
                    } catch (ParseException ex) {
                        logger.info("an error was thrown while checking the start date. See error log - [{}]", login.getUserId());
                        resp.setRetn(308);
                        resp.setDesc("Incorrect date format for start date");
                        logger.error("Incorrect date format for start date - [" + login.getUserId() + "]", ex);

                        return resp;
                    }
                    try {
                        formatter.parse(queryParams.getEndDate());
                    } catch (ParseException ex) {
                        logger.info("an error was thrown while checking the end date. See error log - [{}]", login.getUserId());
                        resp.setRetn(308);
                        resp.setDesc("Incorrect date format for end date");
                        logger.error("Incorrect date format for end date - [" + login.getUserId() + "]", ex);

                        return resp;
                    }
                }
                
                if (queryParams.getHolderChanges() == null || queryParams.getHolderChanges().getChangeTypeId() <= 0) {
                    logger.info("Holder change type not properly set - [{}]", login.getUserId());
                    resp.setRetn(308);
                    resp.setDesc("Holder change type not properly set.");
                    return resp;
                }
                
                if (!hq.checkChangeType(queryParams.getHolderChanges().getChangeTypeId())) {
                    logger.info("Invalid change type - [{}]", login.getUserId());
                    resp.setRetn(308);
                    resp.setDesc("Invalid change type.");
                    return resp;
                }

                change_type_hib = hq.getChangeType(queryParams.getHolderChanges().getChangeTypeId());
                changes_hib.setHolderChangeType(change_type_hib);

                List<org.greenpole.hibernate.entity.HolderChanges> changes_hib_result = hq.queryHolderChanges(queryParams.getDescriptor(), changes_hib,
                        queryParams.getStartDate(), queryParams.getEndDate(), greenProp.getDateFormat());
                logger.info("retrieved holder changes result from query. Preparing local model - [{}]", login.getUserId());
                List<Holder> return_list = new ArrayList<>();

                //unwrap returned result list
                List<HolderChanges> change_list = new ArrayList<>();
                for (org.greenpole.hibernate.entity.HolderChanges hc : changes_hib_result) {
                    org.greenpole.hibernate.entity.Holder holder_hib = hq.getHolder(hc.getHolder().getId()); //all variables of holder
                    HolderChanges hc_model = new HolderChanges();
                    Holder holder_model = new Holder();

                    holder_model.setHolderId(holder_hib.getId());
                    holder_model.setFirstName(holder_hib.getFirstName());
                    holder_model.setLastName(holder_hib.getLastName());

                    hc_model.setCurrentForm(hc.getCurrentForm());
                    hc_model.setInitialForm(hc.getInitialForm());
                    hc_model.setChangeDate(formatter.format(hc.getChangeDate()));
                    hc_model.setChangeTypeId(hc.getHolderChangeType().getId());
                    
                    change_list.add(hc_model);
                    
                    holder_model.setChanges(change_list);
                    
                    return_list.add(holder_model);
                }
                logger.info("holder changes query successful - [{}]", login.getUserId());
                resp.setRetn(0);
                resp.setDesc("Successful");
                resp.setBody(return_list);
                return resp;
            }
            logger.info("descriptor length does not match expected required length - [{}]", login.getUserId());
            resp.setRetn(308);
            resp.setDesc("descriptor length does not match expected required length");
            return resp;
        } catch (Exception ex) {
            logger.info("error querying holder changes. See error log - [{}]", login.getUserId());
            logger.error("error querying holder changes - [" + login.getUserId() + "]", ex);
            
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
        Descriptor descriptorUtil =  new Descriptor();
        logger.info("request to query holder, invoked by [{}]", login.getUserId());
        
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());

            Map<String, String> descriptors = descriptorUtil.decipherDescriptor(queryParams.getDescriptor());

            if (descriptors.size() == 3) {
                String descriptor = queryParams.getDescriptor();

                org.greenpole.hibernate.entity.Holder h_hib_search = new org.greenpole.hibernate.entity.Holder();
                org.greenpole.hibernate.entity.HolderType h_type_hib_search = new org.greenpole.hibernate.entity.HolderType();

                HolderResidentialAddress h_res_hib_search = new HolderResidentialAddress();
                HolderPostalAddress h_pos_hib_search = new HolderPostalAddress();
                HolderEmailAddress h_email_hib_search = new HolderEmailAddress();
                HolderPhoneNumber h_phone_hib_search = new HolderPhoneNumber();

                org.greenpole.hibernate.entity.HolderCompanyAccount hca_hib_search = new org.greenpole.hibernate.entity.HolderCompanyAccount();
                HolderCompanyAccountId hca_id_hib_search = new HolderCompanyAccountId();

                org.greenpole.hibernate.entity.HolderBondAccount hba_hib_search = new org.greenpole.hibernate.entity.HolderBondAccount();
                HolderBondAccountId hba_id_hib_search = new HolderBondAccountId();

                org.greenpole.hibernate.entity.Stockbroker broker_hib_search = new org.greenpole.hibernate.entity.Stockbroker();

                Holder h_model_search;
                if (queryParams.getHolder() != null) {
                    h_model_search = queryParams.getHolder();
                    
                    h_type_hib_search.setId(h_model_search.getTypeId());

                    h_hib_search.setFirstName(h_model_search.getFirstName());
                    h_hib_search.setMiddleName(h_model_search.getMiddleName());
                    h_hib_search.setLastName(h_model_search.getLastName());
                    h_hib_search.setGender(h_model_search.getGender());
                    if (h_model_search.getDob() != null && !"".equals(h_model_search.getDob()))
                        h_hib_search.setDob(formatter.parse(h_model_search.getDob()));
                    h_hib_search.setChn(h_model_search.getChn());
                    if (h_model_search.getHolderAcctNumber() > 0)
                        h_hib_search.setHolderAcctNumber(h_model_search.getHolderAcctNumber());
                    if (h_model_search.isTaxExempted())
                        h_hib_search.setTaxExempted(h_model_search.isTaxExempted());
                    h_hib_search.setPryAddress(h_model_search.getPryAddress());
                    h_hib_search.setHolderType(h_type_hib_search);
                    h_hib_search.setPryHolder(true); //must be set
                    
                    Stockbroker broker_search;
                    Set broker_set = new HashSet();
                    if (queryParams.getHolder().getHolderStockbroker() != null) {
                        broker_search = queryParams.getHolder().getHolderStockbroker();

                        broker_hib_search.setName(broker_search.getName());

                        broker_set.add(broker_hib_search);

                        h_hib_search.setStockbrokers(broker_set);
                    }

                    Address h_res_model_search;
                    Set h_res_hib_set = new HashSet();
                    if (queryParams.getHolder().getResidentialAddresses() != null && !queryParams.getHolder().getResidentialAddresses().isEmpty()) {
                        h_res_model_search = queryParams.getHolder().getResidentialAddresses().get(0);

                        h_res_hib_search.setAddressLine1(h_res_model_search.getAddressLine1());
                        h_res_hib_search.setState(h_res_model_search.getState());
                        h_res_hib_search.setCountry(h_res_model_search.getCountry());
                        h_res_hib_search.setAddressLine2(h_res_model_search.getAddressLine2());
                        h_res_hib_search.setAddressLine3(h_res_model_search.getAddressLine3());
                        h_res_hib_search.setAddressLine4(h_res_model_search.getAddressLine4());
                        h_res_hib_search.setCity(h_res_model_search.getCity());
                        h_res_hib_search.setPostCode(h_res_model_search.getPostCode());
                        h_res_hib_search.setIsPrimary(h_res_model_search.isPrimaryAddress());

                        h_res_hib_set.add(h_res_hib_search);

                        h_hib_search.setHolderResidentialAddresses(h_res_hib_set);
                    }

                    Address h_pos_model_search;
                    Set h_pos_hib_set = new HashSet();
                    if (queryParams.getHolder().getPostalAddresses() != null && !queryParams.getHolder().getPostalAddresses().isEmpty()) {
                        h_pos_model_search = queryParams.getHolder().getPostalAddresses().get(0);

                        h_pos_hib_search.setAddressLine1(h_pos_model_search.getAddressLine1());
                        h_pos_hib_search.setState(h_pos_model_search.getState());
                        h_pos_hib_search.setCountry(h_pos_model_search.getCountry());
                        h_pos_hib_search.setAddressLine2(h_pos_model_search.getAddressLine2());
                        h_pos_hib_search.setAddressLine3(h_pos_model_search.getAddressLine3());
                        h_pos_hib_search.setAddressLine4(h_pos_model_search.getAddressLine4());
                        h_pos_hib_search.setCity(h_pos_model_search.getCity());
                        h_pos_hib_search.setPostCode(h_pos_model_search.getPostCode());
                        h_pos_hib_search.setIsPrimary(h_pos_model_search.isPrimaryAddress());

                        h_pos_hib_set.add(h_pos_hib_search);

                        h_hib_search.setHolderPostalAddresses(h_pos_hib_set);
                    }

                    EmailAddress h_email_model_search;
                    Set h_email_hib_set = new HashSet();
                    if (queryParams.getHolder().getEmailAddresses() != null && !queryParams.getHolder().getEmailAddresses().isEmpty()) {
                        h_email_model_search = queryParams.getHolder().getEmailAddresses().get(0);

                        h_email_hib_search.setEmailAddress(h_email_model_search.getEmailAddress());

                        h_email_hib_set.add(h_email_hib_search);

                        h_hib_search.setHolderEmailAddresses(h_email_hib_set);
                    }

                    PhoneNumber h_phone_model_search;
                    Set h_phone_hib_set = new HashSet();
                    if (queryParams.getHolder().getPhoneNumbers() != null && !queryParams.getHolder().getPhoneNumbers().isEmpty()) {
                        h_phone_model_search = queryParams.getHolder().getPhoneNumbers().get(0);

                        h_phone_hib_search.setPhoneNumber(h_phone_model_search.getPhoneNumber());

                        h_phone_hib_set.add(h_phone_hib_search);

                        h_hib_search.setHolderPhoneNumbers(h_phone_hib_set);
                    }

                    HolderCompanyAccount hca_model_search;
                    Set hca_hib_set = new HashSet();
                    if (queryParams.getHolder().getCompanyAccounts() != null && !queryParams.getHolder().getCompanyAccounts().isEmpty()) {
                        hca_model_search = queryParams.getHolder().getCompanyAccounts().get(0);

                        hca_id_hib_search.setClientCompanyId(hca_model_search.getClientCompanyId());

                        hca_hib_search.setEsop(hca_model_search.isEsop());
                        hca_hib_search.setHolderCompAccPrimary(true);//always set this
                        hca_hib_search.setMerged(false);//always set this

                        hca_hib_search.setId(hca_id_hib_search);

                        hca_hib_set.add(hca_hib_search);

                        h_hib_search.setHolderCompanyAccounts(hca_hib_set);
                    }

                    HolderBondAccount hba_model_search;
                    Set hba_hib_set = new HashSet();
                    if (queryParams.getHolder().getBondAccounts() != null && !queryParams.getHolder().getBondAccounts().isEmpty()) {
                        hba_model_search = queryParams.getHolder().getBondAccounts().get(0);

                        hba_id_hib_search.setBondOfferId(hba_model_search.getBondOfferId());

                        hba_hib_search.setStartingPrincipalValue(hba_model_search.getStartingPrincipalValue());
                        hba_hib_search.setRemainingPrincipalValue(hba_model_search.getRemainingPrincipalValue());
                        hba_hib_search.setHolderBondAcctPrimary(true);//always set this
                        hba_hib_search.setMerged(false);//always set this

                        hba_hib_search.setId(hba_id_hib_search);

                        hba_hib_set.add(hba_hib_search);

                        h_hib_search.setHolderBondAccounts(hba_hib_set);
                    }
                }
                
                
                Map<String, Integer> shareUnits_search;
                if (queryParams.getUnits() != null && !queryParams.getUnits().isEmpty()) {
                    shareUnits_search = queryParams.getUnits();
                } else {
                    shareUnits_search = new HashMap<>();
                }

                Map<String, Integer> totalHoldings_search;
                if (queryParams.getTotalHoldings() != null && !queryParams.getTotalHoldings().isEmpty()) {
                    totalHoldings_search = queryParams.getTotalHoldings();
                } else {
                    totalHoldings_search = new HashMap<>();
                }

                List<org.greenpole.hibernate.entity.Holder> h_search_result;
                if (queryParams.isIsShareHolder())
                    h_search_result = hq.queryShareholderAccount(descriptor, h_hib_search, shareUnits_search, totalHoldings_search);
                else
                    h_search_result = hq.queryBondholderAccount(descriptor, h_hib_search, shareUnits_search, totalHoldings_search);
                
                
                logger.info("retrieved holder result from query. Preparing local model - [{}]", login.getUserId());
                
                //unwrap result and set in holder front-end model
                List<Holder> h_model_out = new ArrayList<>();
                
                for (org.greenpole.hibernate.entity.Holder h_hib_out : h_search_result) {
                    List<Address> h_res_out = new ArrayList<>();
                    List<Address> h_pos_out = new ArrayList<>();
                    List<PhoneNumber> h_phone_out = new ArrayList<>();
                    List<EmailAddress> h_email_out = new ArrayList<>();
                    List<HolderCompanyAccount> hca_out = new ArrayList<>();
                    List<HolderBondAccount> hba_out = new ArrayList<>();
                    
                    Holder h = new Holder();

                    h.setHolderId(h_hib_out.getId());
                    if (h_hib_out.getHolderAcctNumber() != null)
                        h.setHolderAcctNumber(h_hib_out.getHolderAcctNumber());
                    h.setFirstName(h_hib_out.getFirstName());
                    h.setMiddleName(h_hib_out.getMiddleName());
                    h.setLastName(h_hib_out.getLastName());
                    h.setGender(h_hib_out.getGender());
                    h.setDob(formatter.format(h_hib_out.getDob()));
                    h.setChn(h_hib_out.getChn());
                    if (h_hib_out.getHolderAcctNumber() != null)
                        h.setHolderAcctNumber(h_hib_out.getHolderAcctNumber());
                    h.setTaxExempted(h_hib_out.getTaxExempted());
                    h.setPryAddress(h_hib_out.getPryAddress());
                    h.setTypeId(h_hib_out.getHolderType().getId());
                    h.setPryHolder(h_hib_out.getPryHolder());
                    
                    //set prymary address separately
                    if ("residential".equalsIgnoreCase(h_hib_out.getPryAddress())) {
                        List<HolderResidentialAddress> res_hib_list = hq.getHolderResidentialAddresses(h_hib_out.getId());
                        for (HolderResidentialAddress res_hib_out : res_hib_list) {
                            if (res_hib_out.getIsPrimary() != null && res_hib_out.getIsPrimary()) {
                                Address addy_model = new Address();
                                
                                addy_model.setId(res_hib_out.getId());
                                addy_model.setAddressLine1(res_hib_out.getAddressLine1());
                                addy_model.setState(res_hib_out.getState());
                                addy_model.setCountry(res_hib_out.getCountry());
                                addy_model.setAddressLine2(res_hib_out.getAddressLine2());
                                addy_model.setAddressLine3(res_hib_out.getAddressLine3());
                                addy_model.setAddressLine4(res_hib_out.getAddressLine4());
                                addy_model.setPostCode(res_hib_out.getPostCode());
                                addy_model.setCity(res_hib_out.getCity());
                                addy_model.setPrimaryAddress(res_hib_out.getIsPrimary());
                                addy_model.setEntityId(res_hib_out.getHolder().getId());
                                
                                h.setAddressPrimary(addy_model);
                                
                                break;
                            }
                        }
                    } else if ("postal".equalsIgnoreCase(h_hib_out.getPryAddress())) {
                        List<HolderPostalAddress> pos_hib_list = hq.getHolderPostalAddresses(h_hib_out.getId());
                        for (HolderPostalAddress pos_hib_out : pos_hib_list) {
                            if (pos_hib_out.getIsPrimary() != null && pos_hib_out.getIsPrimary()) {
                                Address addy_model = new Address();
                                
                                addy_model.setId(pos_hib_out.getId());
                                addy_model.setAddressLine1(pos_hib_out.getAddressLine1());
                                addy_model.setState(pos_hib_out.getState());
                                addy_model.setCountry(pos_hib_out.getCountry());
                                addy_model.setAddressLine2(pos_hib_out.getAddressLine2());
                                addy_model.setAddressLine3(pos_hib_out.getAddressLine3());
                                addy_model.setAddressLine4(pos_hib_out.getAddressLine4());
                                addy_model.setPostCode(pos_hib_out.getPostCode());
                                addy_model.setCity(pos_hib_out.getCity());
                                addy_model.setPrimaryAddress(pos_hib_out.getIsPrimary());
                                addy_model.setEntityId(pos_hib_out.getHolder().getId());
                                
                                h.setAddressPrimary(addy_model);
                                
                                break;
                            }
                        }
                    }

                    //get all available addresses, email addresses and phone numbers
                    List<HolderResidentialAddress> res_hib_list = hq.getHolderResidentialAddresses(h_hib_out.getId());
                    for (HolderResidentialAddress res_hib_out : res_hib_list) {
                        Address addy_model = new Address();

                        addy_model.setId(res_hib_out.getId());
                        addy_model.setAddressLine1(res_hib_out.getAddressLine1());
                        addy_model.setState(res_hib_out.getState());
                        addy_model.setCountry(res_hib_out.getCountry());
                        addy_model.setAddressLine2(res_hib_out.getAddressLine2());
                        addy_model.setAddressLine3(res_hib_out.getAddressLine3());
                        addy_model.setAddressLine4(res_hib_out.getAddressLine4());
                        addy_model.setPostCode(res_hib_out.getPostCode());
                        addy_model.setCity(res_hib_out.getCity());
                        addy_model.setPrimaryAddress(res_hib_out.getIsPrimary());
                        addy_model.setEntityId(res_hib_out.getHolder().getId());

                        h_res_out.add(addy_model);
                    }
                    h.setResidentialAddresses(h_res_out);

                    List<HolderPostalAddress> pos_hib_list = hq.getHolderPostalAddresses(h_hib_out.getId());
                    for (HolderPostalAddress pos_hib_out : pos_hib_list) {
                        Address addy_model = new Address();
                        
                        addy_model.setId(pos_hib_out.getId());
                        addy_model.setAddressLine1(pos_hib_out.getAddressLine1());
                        addy_model.setState(pos_hib_out.getState());
                        addy_model.setCountry(pos_hib_out.getCountry());
                        addy_model.setAddressLine2(pos_hib_out.getAddressLine2());
                        addy_model.setAddressLine3(pos_hib_out.getAddressLine3());
                        addy_model.setAddressLine4(pos_hib_out.getAddressLine4());
                        addy_model.setPostCode(pos_hib_out.getPostCode());
                        addy_model.setCity(pos_hib_out.getCity());
                        addy_model.setPrimaryAddress(pos_hib_out.getIsPrimary());
                        addy_model.setEntityId(pos_hib_out.getHolder().getId());
                        
                        h_pos_out.add(addy_model);
                    }
                    h.setPostalAddresses(h_pos_out);

                    List<HolderEmailAddress> email_hib_list = hq.getHolderEmailAddresses(h_hib_out.getId());
                    for (HolderEmailAddress email_hib_out : email_hib_list) {
                        EmailAddress email_model_out = new EmailAddress();
                        
                        email_model_out.setEmailAddress(email_hib_out.getEmailAddress());
                        email_model_out.setPrimaryEmail(email_hib_out.getIsPrimary());

                        h_email_out.add(email_model_out);
                    }
                    h.setEmailAddresses(h_email_out);

                    List<HolderPhoneNumber> phone_hib_list = hq.getHolderPhoneNumbers(h_hib_out.getId());
                    for (HolderPhoneNumber phone_hib_out : phone_hib_list) {
                        PhoneNumber phone_model_out = new PhoneNumber();
                        
                        phone_model_out.setId(phone_hib_out.getId());
                        phone_model_out.setPhoneNumber(phone_hib_out.getPhoneNumber());
                        phone_model_out.setPrimaryPhoneNumber(phone_hib_out.getIsPrimary());
                        phone_model_out.setEntityId(phone_hib_out.getHolder().getId());
                        
                        h_phone_out.add(phone_model_out);
                    }
                    h.setPhoneNumbers(h_phone_out);
                    
                    List<org.greenpole.hibernate.entity.HolderCompanyAccount> hca_hib_list = hq.getAllHolderCompanyAccounts(h_hib_out.getId());
                    for (org.greenpole.hibernate.entity.HolderCompanyAccount hca_hib_out : hca_hib_list) {
                        HolderCompanyAccount hca_model_out = new HolderCompanyAccount();
                        ClientCompany cc = cq.getClientCompany(hca_hib_out.getId().getClientCompanyId());
                        
                        hca_model_out.setHolderId(hca_hib_out.getId().getHolderId());
                        hca_model_out.setClientCompanyId(hca_hib_out.getId().getClientCompanyId());
                        hca_model_out.setClientCompanyName(cc.getName());
                        hca_model_out.setShareUnits(hca_hib_out.getShareUnits());
                        hca_model_out.setEsop(hca_hib_out.getEsop());
                        hca_model_out.setHolderCompAccPrimary(hca_hib_out.getHolderCompAccPrimary());
                        hca_model_out.setMerged(hca_hib_out.getMerged());
                        hca_model_out.setNubanAccount(hca_hib_out.getNubanAccount());
                        
                        if (hca_hib_out.getBank() != null){
                            Bank bank_hib = hq.getBankDetails(hca_hib_out.getBank().getId());
                            org.greenpole.entity.model.clientcompany.Bank bank_model_out = new org.greenpole.entity.model.clientcompany.Bank();
                            bank_model_out.setId(bank_hib.getId());
                            bank_model_out.setBankName(bank_hib.getBankName());
                            bank_model_out.setBankCode(bank_hib.getBankCode());
                            
                            hca_model_out.setBank(bank_model_out);
                        }
                        
                        hca_out.add(hca_model_out);
                    }
                    h.setCompanyAccounts(hca_out);
                    
                    List<org.greenpole.hibernate.entity.HolderBondAccount> hba_hib_list = hq.getAllHolderBondAccounts(h_hib_out.getId());
                    for (org.greenpole.hibernate.entity.HolderBondAccount hba_hib_out : hba_hib_list) {
                        HolderBondAccount hba_model_out = new HolderBondAccount();
                        BondOffer bo = cq.getBondOffer(hba_hib_out.getId().getBondOfferId());
                        
                        hba_model_out.setHolderId(hba_hib_out.getId().getHolderId());
                        hba_model_out.setBondOfferId(hba_hib_out.getId().getBondOfferId());
                        hba_model_out.setBondOfferTitle(bo.getTitle());
                        hba_model_out.setBondUnits(hba_hib_out.getBondUnits());
                        hba_model_out.setStartingPrincipalValue(hba_hib_out.getStartingPrincipalValue());
                        hba_model_out.setRemainingPrincipalValue(hba_hib_out.getRemainingPrincipalValue());
                        hba_model_out.setMerged(hba_hib_out.getMerged());
                        hba_model_out.setHolderBondAccPrimary(hba_hib_out.getHolderBondAcctPrimary());
                        hba_model_out.setNubanAccount(hba_hib_out.getNubanAccount());
                        
                        if (hba_hib_out.getBank() != null){
                            Bank bank_hib = hq.getBankDetails(hba_hib_out.getBank().getId());
                            org.greenpole.entity.model.clientcompany.Bank bank_model_out = new org.greenpole.entity.model.clientcompany.Bank();
                            bank_model_out.setId(bank_hib.getId());
                            bank_model_out.setBankName(bank_hib.getBankName());
                            bank_model_out.setBankCode(bank_hib.getBankCode());
                            
                            hba_model_out.setBank(bank_model_out);
                        }
                        
                        hba_out.add(hba_model_out);
                    }
                    h.setBondAccounts(hba_out);
                    
                    h_model_out.add(h);
                }

                logger.info("holder query successful - [{}]", login.getUserId());
                resp.setRetn(0);
                resp.setDesc("Successful");
                resp.setBody(h_model_out);

                return resp;
            }

            logger.info("holder query unsuccessful - [{}]", login.getUserId());
            resp.setRetn(308);
            resp.setDesc("Unsuccessful holder query, due to incomplete descriptor. Contact system administrator");

            return resp;
        } catch (Exception ex) {
            logger.info("error querying holder. See error log - [{}]", login.getUserId());
            logger.error("error querying holder - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to query holder. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Request to create administrators for a holder.
     * @param login used to get the userId that is performing this transaction
     * @param authenticator the super user to accept the creation of this
     * request
     * @param holder the holder to create administrator(s) for
     * @return response to the create administrator request
     */
    public Response createAdministrator_Request(Login login, String authenticator, Holder holder) {
        logger.info("request to create administrator for holder [{}], invoked by [{}]", 
                holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;
        
        boolean flag = false;
        String desc = "";
        
        try {
            if (hq.checkHolderAccount(holder.getHolderId())) {

                if (holder.getAdministrators() != null && !holder.getAdministrators().isEmpty()) {
                    
                    for (Administrator admin : holder.getAdministrators()) {
                        if (admin.getPryAddress() == null || "".equals(admin.getPryAddress())) {
                            desc += "\nPrimary address must be set";
                        } else if (!admin.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString()) && 
                                !admin.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
                            desc += "\nPrimary address can only be residential or postal";
                        } else if (admin.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString()) && 
                                admin.getResidentialAddress() == null) {
                            desc += "\nResidential address cannot be empty, as it is the primary address";
                        } else if (admin.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString()) && 
                                admin.getPostalAddress() == null) {
                            desc += "\nPostal address cannot be empty, as it is the primary address";
                        } else {
                            flag = true;
                        }
                        
                        if (flag && admin.getResidentialAddress() != null) {
                            Address res = admin.getResidentialAddress();
                            if (res.getAddressLine1() == null || "".equals(res.getAddressLine1())) {
                                desc += "\nResidential address line 1 should not be empty";
                                flag = false;
                                break;
                            } else if (res.getState() == null || "".equals(res.getState())) {
                                desc += "\nResidential state should not be empty";
                                flag = false;
                                break;
                            } else if (res.getCountry() == null || "".equals(res.getCountry())) {
                                desc += "\nResidential country should not be empty";
                                flag = false;
                                break;
                            }
                        }
                        
                        if (flag && admin.getPostalAddress() != null) {
                            Address pos = admin.getPostalAddress();
                            if (pos.getAddressLine1() == null || "".equals(pos.getAddressLine1())) {
                                desc += "\nPostal address line 1 should not be empty";
                                flag = false;
                                break;
                            } else if (pos.getState() == null || "".equals(pos.getState())) {
                                desc += "\nPostal state should not be empty";
                                flag = false;
                                break;
                            } else if (pos.getCountry() == null || "".equals(pos.getCountry())) {
                                desc += "\nPostal country should not be empty";
                                flag = false;
                                break;
                            }
                        }
                        
                        /*if (flag && admin.getEmailAddresses() != null && !admin.getEmailAddresses().isEmpty()) {
                        for (EmailAddress email : admin.getEmailAddresses()) {
                        if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
                        desc += "\nEmail address should not be empty";
                        flag = false;
                        break;
                        }
                        }
                        }*/
                        
                        /*if (flag && admin.getPhoneNumbers() != null && !admin.getPhoneNumbers().isEmpty()) {
                        for (PhoneNumber phone : admin.getPhoneNumbers()) {
                        if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
                        desc += "\nPhone number should not be empty";
                        flag = false;
                        break;
                        }
                        }
                        }*/
                    }
                    
                    if (flag) {
                        wrapper = new NotificationWrapper();
                        prop = new NotifierProperties(HolderComponentLogic.class);
                        qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                                prop.getAuthoriserNotifierQueueName());

                        List<Holder> holderList = new ArrayList();
                        holderList.add(holder);

                        wrapper.setCode(notification.createCode(login));
                        wrapper.setDescription("Authenticate creation of administrator(s) for holder " + holder.getFirstName() + " " + holder.getLastName());
                        wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                        wrapper.setNotificationType(NotificationType.create_administrator.toString());
                        wrapper.setFrom(login.getUserId());
                        wrapper.setTo(authenticator);
                        wrapper.setModel(holderList);
                        
                        logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                        resp = qSender.sendAuthorisationRequest(wrapper);
                        return resp;
                    }
                    resp.setRetn(309);
                    resp.setDesc("Error: " + desc);
                    logger.info("error detected in administrator creation process - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }
                resp.setRetn(309);
                resp.setDesc("No administrator was sent to be added for the holder.");
                logger.info("No administrator was sent to be added for the holder - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(309);
            resp.setDesc("The holder does not exist.");
            logger.info("The holder does not exist - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error proccessing holder administrator creation. See error log - [{}]", login.getUserId());
            logger.error("error proccessing holder administrator creation - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to proccess holder administrator creation. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Processes a saved request to create administrators for a holder.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the create administrator request
     */
    public Response createAdministrator_Authorise(Login login, String notificationCode) {
        logger.info("authorise bond unit transfer, invoked by [{}] - notification code: [{}]", login.getUserId(), notificationCode);
        Response resp = new Response();
        Notification notification = new Notification();
        
        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<Holder> holderList = (List<Holder>) wrapper.getModel();
            Holder holderModel = holderList.get(0);
            
            boolean flag = false;
            String desc = "";
            
            if (hq.checkHolderAccount(holderModel.getHolderId())) {

                if (holderModel.getAdministrators() != null && !holderModel.getAdministrators().isEmpty()) {
                    
                    for (Administrator admin : holderModel.getAdministrators()) {
                        if (admin.getPryAddress() == null || "".equals(admin.getPryAddress())) {
                            desc += "\nPrimary address must be set";
                        } else if (!admin.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString()) && 
                                !admin.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
                            desc += "\nPrimary address can only be residential or postal";
                        } else if (admin.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString()) && 
                                admin.getResidentialAddress() == null) {
                            desc += "\nResidential address cannot be empty, as it is the primary address";
                        } else if (admin.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString()) && 
                                admin.getPostalAddress() == null) {
                            desc += "\nPostal address cannot be empty, as it is the primary address";
                        } else {
                            flag = true;
                        }
                        
                        if (flag && admin.getResidentialAddress() != null) {
                            Address res = admin.getResidentialAddress();
                            if (res.getAddressLine1() == null || "".equals(res.getAddressLine1())) {
                                desc += "\nResidential address line 1 should not be empty";
                                flag = false;
                                break;
                            } else if (res.getState() == null || "".equals(res.getState())) {
                                desc += "\nResidential state should not be empty";
                                flag = false;
                                break;
                            } else if (res.getCountry() == null || "".equals(res.getCountry())) {
                                desc += "\nResidential country should not be empty";
                                flag = false;
                                break;
                            }
                        }
                        
                        if (flag && admin.getPostalAddress() != null) {
                            Address pos = admin.getPostalAddress();
                            if (pos.getAddressLine1() == null || "".equals(pos.getAddressLine1())) {
                                desc += "\nPostal address line 1 should not be empty";
                                flag = false;
                                break;
                            } else if (pos.getState() == null || "".equals(pos.getState())) {
                                desc += "\nPostal state should not be empty";
                                flag = false;
                                break;
                            } else if (pos.getCountry() == null || "".equals(pos.getCountry())) {
                                desc += "\nPostal country should not be empty";
                                flag = false;
                                break;
                            }
                        }
                        
                        /*if (flag && admin.getEmailAddresses() != null && !admin.getEmailAddresses().isEmpty()) {
                        for (EmailAddress email : admin.getEmailAddresses()) {
                        if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
                        desc += "\nEmail address should not be empty";
                        flag = false;
                        break;
                        }
                        }
                        }*/
                        
                        /*if (flag && admin.getPhoneNumbers() != null && !admin.getPhoneNumbers().isEmpty()) {
                        for (PhoneNumber phone : admin.getPhoneNumbers()) {
                        if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
                        desc += "\nPhone number should not be empty";
                        flag = false;
                        break;
                        }
                        }
                        }*/
                    }
             
                    if (flag) {
                        org.greenpole.hibernate.entity.Holder holderBefore = hq.getHolder(holderModel.getHolderId());
                        org.greenpole.hibernate.entity.Holder holderEntity = upwrapAdministrator(holderModel);
                        
                        org.greenpole.hibernate.entity.HolderChanges change = new org.greenpole.hibernate.entity.HolderChanges();
                        HolderChangeType changeType = hq.getChangeType(1);
                        
                        change.setHolder(holderEntity);
                        change.setInitialForm(holderBefore.getFirstName() + " " + holderBefore.getLastName());
                        change.setCurrentForm(holderEntity.getFirstName() + " " + holderEntity.getLastName());
                        change.setChangeDate(new Date());
                        change.setHolderChangeType(changeType);
                        
                        boolean created = hq.createAdministratorForHolder(holderEntity, change);

                        if (created) {
                            notification.markAttended(notificationCode);
                            resp.setRetn(0);
                            resp.setDesc("Successful");
                            logger.info("Administrators were created successfully - [{}]", login.getUserId());
                            return resp;
                        } else {
                            resp.setRetn(310);
                            resp.setDesc("General Error. Unable to persist administrator. Contact system administrator.");
                            logger.info("Error persist holder account - [{}]", login.getUserId());
                            return resp;
                        }
                    }
                    resp.setRetn(310);
                    resp.setDesc("Error: " + desc);
                    logger.info("error detected in administrator creation process - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }
                resp.setRetn(310);
                resp.setDesc("No administrator was sent to be added for the holder.");
                logger.info("No administrator was sent to be added for the holder - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(310);
            resp.setDesc("The holder does not exist.");
            logger.info("The holder does not exist - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error creating administrators. See error log - [{}]", login.getUserId());
            logger.error("error creating administrators - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to create administrators. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Request to upload power of attorney for a holder.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param poa the power of attorney to be uploaded
     * @return response to the upload power of attorney request
     */
    public Response uploadPowerOfAttorney_Request(Login login, String authenticator, PowerOfAttorney poa) {
        logger.info("request to upload power of attorney, invoked by [{}]", login.getUserId());
        
        Response resp = new Response();
        Notification notification = new Notification();
        BytesConverter bytesConverter = new BytesConverter();
        
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;
        
        boolean flag = false;
        String desc = "";
        
        try {
            long defaultSize = Long.valueOf(greenProp.getPowerOfAttorneySize());
            Date current_date = new Date();
            
            if (hq.checkHolderAccount(poa.getHolderId())) {
                org.greenpole.hibernate.entity.Holder holder = hq.getHolder(poa.getHolderId());
                logger.info("Holder [{}] checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
                
                if (hq.checkCurrentPowerOfAttorney(poa.getHolderId())) {
                    org.greenpole.hibernate.entity.PowerOfAttorney currentPoa = hq.getCurrentPowerOfAttorney(poa.getHolderId());
                    logger.info("Holder has current power of attorney - [{}]", login.getUserId());
                    
                    if (current_date.before(currentPoa.getEndDate())) {
                        desc += "\nThe current power of attorney is yet to expire";
                    } else {
                        flag = true;
                    }
                } else {
                    flag = true;
                }

                if (flag) {
                    long fileSize = bytesConverter.decodeToBytes(poa.getFileContents()).length;

                    if (fileSize <= defaultSize) {
                        wrapper = new NotificationWrapper();
                        prop = new NotifierProperties(HolderComponentLogic.class);
                        qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                                prop.getAuthoriserNotifierQueueName());
                        
                        List<PowerOfAttorney> powerList = new ArrayList();
                        powerList.add(poa);
                        
                        wrapper.setCode(notification.createCode(login));
                        wrapper.setDescription("Authenticate power of attorney for " + holder.getFirstName() + " " + holder.getLastName());
                        wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                        wrapper.setNotificationType(NotificationType.upload_power_of_attorney.toString());
                        wrapper.setFrom(login.getUserId());
                        wrapper.setTo(authenticator);
                        wrapper.setModel(powerList);
                        
                        logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                        resp = qSender.sendAuthorisationRequest(wrapper);
                        return resp;
                    }
                    resp.setRetn(311);
                    resp.setDesc("The size of the power of attorney cannot exceed 10MB.");
                    logger.info("The size of the power of attorney cannot exceed 10MB - [{}]", login.getUserId());
                    return resp;
                }
                resp.setRetn(311);
                resp.setDesc("Error: " + desc);
                logger.info("error detected in upload power of attorney process - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(311);
            resp.setDesc("The holder does not exist.");
            logger.info("The holder does not exist - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error proccessing power of attorney upload. See error log - [{}]", login.getUserId());
            logger.error("error proccessing power of attorney upload - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to proccess power of attorney upload. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Processes a saved request to upload power of attorney
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the upload power of attorney request
     */
    public Response uploadPowerOfAttorney_Authorise(Login login, String notificationCode) {
        logger.info("authorise upload power of attorney, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        BytesConverter bytesConverter = new BytesConverter();
        
        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<PowerOfAttorney> poaList = (List<PowerOfAttorney>) wrapper.getModel();
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            
            boolean flag = false;
            String desc = "";
            
            PowerOfAttorney poaModel = poaList.get(0);
            org.greenpole.hibernate.entity.PowerOfAttorney poa_hib = new org.greenpole.hibernate.entity.PowerOfAttorney();
            org.greenpole.hibernate.entity.PowerOfAttorney currentPoa = new org.greenpole.hibernate.entity.PowerOfAttorney();
            
            long defaultSize = Long.valueOf(greenProp.getPowerOfAttorneySize());
            Date current_date = new Date();
            
            if (hq.checkHolderAccount(poaModel.getHolderId())) {
                org.greenpole.hibernate.entity.Holder holder = hq.getHolder(poaModel.getHolderId());
                logger.info("Holder [{}] checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
                
                boolean currentPoaExists = hq.checkCurrentPowerOfAttorney(poaModel.getHolderId());
                if (currentPoaExists) {
                    currentPoa = hq.getCurrentPowerOfAttorney(poaModel.getHolderId());
                    logger.info("Holder has current power of attorney - [{}]", login.getUserId());
                    
                    if (current_date.before(currentPoa.getEndDate())) {
                        desc += "\nThe current power of attorney is yet to expire";
                    } else {
                        flag = true;
                    }
                } else {
                    flag = true;
                }
                
                if (flag) {
                    GreenpoleFile file = new GreenpoleFile(greenProp.getPowerOfAttorneyPath());
                    long fileSize = bytesConverter.decodeToBytes(poaModel.getFileContents()).length;
                    
                    if (fileSize <= defaultSize) {
                        logger.info("Power of attorney met file size requirement - [{}]", login.getUserId());
                        
                        if (file.createFile(bytesConverter.decodeToBytes(poaModel.getFileContents()))) {
                            logger.info("Power of attorney file created and saved - [{}]", login.getUserId());
                            
                            String filepath = file.getFolderPath() + file.getFileName();
                            poa_hib.setTitle(poaModel.getTitle());
                            poa_hib.setType(poaModel.getType());
                            poa_hib.setStartDate(formatter.parse(poaModel.getStartDate()));
                            if (poaModel.getEndDate() != null && !"".equals(poaModel.getEndDate()))
                                poa_hib.setEndDate(formatter.parse(poaModel.getEndDate()));
                            poa_hib.setFilePath(filepath);
                            
                            poa_hib.setPowerOfAttorneyPrimary(true);
                            poa_hib.setHolder(holder);
                            
                            currentPoa.setPowerOfAttorneyPrimary(false);
                            
                            boolean uploaded;
                            if (currentPoaExists) {
                                uploaded = hq.uploadPowerOfAttorney(poa_hib, currentPoa);
                            } else {
                                uploaded = hq.uploadPowerOfAttorney(poa_hib, null);
                            }
                            
                            if (uploaded) {
                                notification.markAttended(notificationCode);
                                resp.setRetn(0);
                                resp.setDesc("Successful");
                                logger.info("Power of attorney successfully uploaded - [{}]", login.getUserId());
                                return resp;
                            }
                            resp.setRetn(312);
                            resp.setDesc("Power of attorney upload failed due to database error. Contact Administrator.");
                            logger.info("Power of attorney upload failed due to database error. Check error logs - [{}]", login.getUserId());
                            return resp;
                        }
                        resp.setRetn(312);
                        resp.setDesc("The Power of Attorney file could not be uploaded onto the server. Contact Administrator");
                        logger.info("The Power of Attorney file could not be uploaded onto the server. See error logs - [{}]", login.getUserId());
                        return resp;
                    }
                    resp.setRetn(312);
                    resp.setDesc("The size of the power of attorney cannot exceed 10MB.");
                    logger.info("The size of the power of attorney cannot exceed 10MB - [{}]", login.getUserId());
                    return resp;
                }
                resp.setRetn(312);
                resp.setDesc("Error: " + desc);
                logger.info("error detected in upload power of attorney process - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(312);
            resp.setDesc("The holder does not exist.");
            logger.info("The holder does not exist - [{}]", login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            logger.info("error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(98);
            resp.setDesc("Unable to upload power of attorney. Contact System Administrator");
            
            return resp;
        } catch (IOException ex) {
            logger.info("Power of attorney file upload failed with an I/O error. See error log - [{}]", login.getUserId());
            logger.error("Power of attorney file upload failed with an I/O error - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(312);
            resp.setDesc("Power of attorney file upload failed with an I/O error. Contact Administrator");
            
            return resp;
        } catch (ParseException ex) {
            logger.info("Error occured while parsing start / end date into power of attorney. See error log - [{}]", login.getUserId());
            logger.error("Error occured while parsing start / end date into power of attorney - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(312);
            resp.setDesc("Error occured while parsing start / end date into power of attorney. Contact Administrator");
            
            return resp;
        } catch (Exception ex) {
            logger.info("error uploading power of attorney. See error log - [{}]", login.getUserId());
            logger.error("error uploading power of attorney - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to upload power of attorney. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Searches for a specific power of attorney for a holder.
     * @param login the user's login details
     * @param queryParams the query parameters
     * @return response to the query power of attorney
     */
    public Response queryPowerOfAttorney_Request(Login login, PowerOfAttorney queryParams) {
        logger.info("request to query power of attorney, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        BytesConverter bytesConverter = new BytesConverter();
        
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            PowerOfAttorney poa_model = new PowerOfAttorney();
            org.greenpole.hibernate.entity.PowerOfAttorney poa_hib;
            
            if (hq.checkHolderAccount(queryParams.getHolderId())) {
                org.greenpole.hibernate.entity.Holder holder = hq.getHolder(queryParams.getHolderId());
                logger.info("Holder [{}] checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
                
                poa_hib = hq.getHolderPowerOfAttorney(queryParams.getId());
                
                File file = new File(poa_hib.getFilePath());
                byte[] read = Files.readAllBytes(file.toPath());
                String encodedContents = bytesConverter.encodeToString(read);
                logger.info("Power of attorney file successfully read - [{}]", login.getUserId());
                
                poa_model.setId(poa_hib.getId());
                poa_model.setHolderId(poa_hib.getHolder().getId());
                poa_model.setTitle(poa_hib.getTitle());
                poa_model.setType(poa_hib.getType());
                poa_model.setStartDate(formatter.format(poa_hib.getStartDate()));
                poa_model.setEndDate(formatter.format(poa_hib.getEndDate()));
                poa_model.setPrimaryPowerOfAttorney(poa_hib.getPowerOfAttorneyPrimary());
                poa_model.setFilePath(poa_hib.getFilePath());
                poa_model.setFileContents(encodedContents);
                
                List<PowerOfAttorney> poa_result = new ArrayList<>();
                poa_result.add(poa_model);
                
                resp.setRetn(0);
                resp.setDesc("Successful");
                logger.info("Power of attorney successfully queried - [{}]", login.getUserId());
                resp.setBody(poa_result);
                return resp;
            }
            resp.setRetn(313);
            resp.setDesc("The holder does not exist.");
            logger.info("The holder does not exist - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error querying power of attorney. See error log - [{}]", login.getUserId());
            logger.error("error querying power of attorney - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to query power of attorney. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Searches for all powers of attorney for a specific holder
     * @param login the user's login details
     * @param queryParams the query parameters
     * @return response to the query all power of attorney request
     */
    public Response queryAllPowerOfAttorney_Request(Login login, PowerOfAttorney queryParams) {
        logger.info("request to query power of attorney, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        BytesConverter bytesConverter = new BytesConverter();
        
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            List<org.greenpole.hibernate.entity.PowerOfAttorney> poa_hib_list;
            
            if (hq.checkHolderAccount(queryParams.getHolderId())) {
                org.greenpole.hibernate.entity.Holder holder = hq.getHolder(queryParams.getHolderId());
                logger.info("Holder [{}] checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
                
                poa_hib_list = hq.getAllHolderPowerOfAttorney(queryParams.getHolderId());
                List<PowerOfAttorney> poa_result = new ArrayList<>();
                
                for (org.greenpole.hibernate.entity.PowerOfAttorney poa_hib : poa_hib_list) {
                    PowerOfAttorney poa_model = new PowerOfAttorney();
                    
                    File file = new File(poa_hib.getFilePath());
                    byte[] read = Files.readAllBytes(file.toPath());
                    String encodedContents = bytesConverter.encodeToString(read);
                    logger.info("Power of attorney file successfully read - [{}]", login.getUserId());

                    poa_model.setId(poa_hib.getId());
                    poa_model.setHolderId(poa_hib.getHolder().getId());
                    poa_model.setTitle(poa_hib.getTitle());
                    poa_model.setType(poa_hib.getType());
                    poa_model.setStartDate(formatter.format(poa_hib.getStartDate()));
                    poa_model.setEndDate(formatter.format(poa_hib.getEndDate()));
                    poa_model.setPrimaryPowerOfAttorney(poa_hib.getPowerOfAttorneyPrimary());
                    poa_model.setFilePath(poa_hib.getFilePath());
                    poa_model.setFileContents(encodedContents);
                    
                    poa_result.add(poa_model);
                }
                
                resp.setRetn(0);
                resp.setDesc("Successful");
                logger.info("Power of attorney successfully queried - [{}]", login.getUserId());
                resp.setBody(poa_result);
                return resp;
            }
            resp.setRetn(314);
            resp.setDesc("The holder does not exist.");
            logger.info("The holder does not exist - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error querying power of attorney. See error log - [{}]", login.getUserId());
            logger.error("error querying power of attorney - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to query power of attorney. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Request to store a NUBAN account in a shareholder's company account.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param compAcct the shareholder's company account
     * @return response to the store NUBAN account request
     */
    public Response storeShareholderNubanAccountNumber_Request(Login login, String authenticator, HolderCompanyAccount compAcct) {
        logger.info("Store NUBAN account number to holder company account, invoked by - [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;

        try {
            if (hq.checkHolderAccount(compAcct.getHolderId())) {
                org.greenpole.hibernate.entity.Holder holder = hq.getHolder(compAcct.getHolderId());
                logger.info("Holder [{}] checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
                
                if (hq.checkHolderCompanyAccount(compAcct.getHolderId(), compAcct.getClientCompanyId())) {
                    logger.info("[{}]'s company account checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
                    
                    if (compAcct.getNubanAccount() != null && !"".equals(compAcct.getNubanAccount())) {
                        
                        if (compAcct.getBank() != null && compAcct.getBank().getId() != 0) {
                            
                            if (hq.checkBank(compAcct.getBank().getId())) {
                                wrapper = new NotificationWrapper();
                                prop = new NotifierProperties(HolderComponentLogic.class);
                                qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                                        prop.getAuthoriserNotifierQueueName());
                                
                                List<HolderCompanyAccount> compAcctList = new ArrayList();
                                compAcctList.add(compAcct);
                                
                                wrapper.setCode(notification.createCode(login));
                                wrapper.setDescription("Authenticate storage of NUBAN account number for holder - " + holder.getFirstName() + " " + holder.getLastName());
                                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                                wrapper.setNotificationType(NotificationType.store_nuban.toString());
                                wrapper.setFrom(login.getUserId());
                                wrapper.setTo(authenticator);
                                wrapper.setModel(compAcctList);
                                
                                logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                                resp = qSender.sendAuthorisationRequest(wrapper);
                                return resp;
                            }
                            resp.setRetn(315);
                            resp.setDesc("Bank does not exist.");
                            logger.info("Bank does not exist - [{}]", login.getUserId());
                            return resp;
                        }
                        resp.setRetn(315);
                        resp.setDesc("Bank cannot be empty.");
                        logger.info("Bank account cannot be empty - [{}]", login.getUserId());
                        return resp;
                    }
                    resp.setRetn(315);
                    resp.setDesc("NUBAN account cannot be empty.");
                    logger.info("NUBAN account cannot be empty - [{}]", login.getUserId());
                    return resp;
                }
                resp.setRetn(315);
                resp.setDesc("Holder's company account does not exist.");
                logger.info("Holder's company account does not exist - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(315);
            resp.setDesc("The holder does not exist.");
            logger.info("The holder does not exist - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error proccessing NUBAN account store. See error log - [{}]", login.getUserId());
            logger.error("error proccessing NUBAN account store - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to proccess NUBAN account store. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     * Processes the saved request to store a NUBAN account in a shareholder's company account.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the store NUBAN account request
     */
    public Response storeShareholderNubanAccountNumber_Authorise(Login login, String notificationCode) {
        logger.info("authorise NUBAN account number addition to holder company account, invoked by - [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        
        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<HolderCompanyAccount> compAcctList = (List<HolderCompanyAccount>) wrapper.getModel();
            HolderCompanyAccount compAcct = compAcctList.get(0);
            
            if (hq.checkHolderAccount(compAcct.getHolderId())) {
                org.greenpole.hibernate.entity.Holder holder = hq.getHolder(compAcct.getHolderId());
                logger.info("Holder [{}] checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
                
                if (hq.checkHolderCompanyAccount(compAcct.getHolderId(), compAcct.getClientCompanyId())) {
                    org.greenpole.hibernate.entity.HolderCompanyAccount compAcct_hib = hq.getHolderCompanyAccount(compAcct.getHolderId(), compAcct.getClientCompanyId());
                    logger.info("[{}]'s company account checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
                    
                    if (compAcct.getNubanAccount() != null && !"".equals(compAcct.getNubanAccount())) {
                        
                        if (compAcct.getBank() != null && compAcct.getBank().getId() != 0) {
                            
                            if (hq.checkBank(compAcct.getBank().getId())) {
                                Bank bank = hq.getBankDetails(compAcct.getBank().getId());
                                
                                compAcct_hib.setId(compAcct_hib.getId());
                                compAcct_hib.setNubanAccount(compAcct.getNubanAccount());
                                compAcct_hib.setBank(bank);
                                
                                hq.createUpdateHolderCompanyAccount(compAcct_hib);
                                
                                notification.markAttended(notificationCode);
                                resp.setRetn(0);
                                resp.setDesc("Successful");
                                logger.info("NUBAN account stored - [{}]", login.getUserId());
                                return resp;
                            }
                            resp.setRetn(316);
                            resp.setDesc("Bank does not exist.");
                            logger.info("Bank does not exist - [{}]", login.getUserId());
                            return resp;
                        }
                        resp.setRetn(316);
                        resp.setDesc("Bank cannot be empty.");
                        logger.info("Bank account cannot be empty - [{}]", login.getUserId());
                        return resp;
                    }
                    resp.setRetn(316);
                    resp.setDesc("NUBAN account cannot be empty.");
                    logger.info("NUBAN account cannot be empty - [{}]", login.getUserId());
                    return resp;
                }
                resp.setRetn(316);
                resp.setDesc("Holder's company account does not exist.");
                logger.info("Holder's company account does not exist - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(316);
            resp.setDesc("The holder does not exist.");
            logger.info("The holder does not exist - [{}]", login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            logger.info("error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(98);
            resp.setDesc("Unable to store NUBAN account. Contact System Administrator");
            
            return resp;
        } catch (Exception ex) {
            logger.info("error storing NUBAN account. See error log - [{}]", login.getUserId());
            logger.error("error storing NUBAN account - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to store NUBAN account. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     * Request to store a NUBAN account in a bond holder's company account.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param bondAcct the bond holder's company account
     * @return response to the store NUBAN account request
     */
    public Response storeBondholderNubanAccountNumber_Request(Login login, String authenticator, HolderBondAccount bondAcct) {
        Response resp = new Response();
        Notification notification = new Notification();
        logger.info("Store NUBAN account number to holder bond account, invoked by - [{}]", login.getUserId());
        
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;

        try {
            if (hq.checkHolderAccount(bondAcct.getHolderId())) {
                org.greenpole.hibernate.entity.Holder holder = hq.getHolder(bondAcct.getHolderId());
                logger.info("Holder [{}] checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
                
                if (hq.checkHolderBondAccount(bondAcct.getHolderId(), bondAcct.getBondOfferId())) {
                    logger.info("[{}]'s bond account checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
                    
                    if (bondAcct.getNubanAccount() != null && !"".equals(bondAcct.getNubanAccount())) {
                        
                        if (bondAcct.getBank() != null && bondAcct.getBank().getId() != 0) {
                            
                            if (hq.checkBank(bondAcct.getBank().getId())) {
                                wrapper = new NotificationWrapper();
                                prop = new NotifierProperties(HolderComponentLogic.class);
                                qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                                        prop.getAuthoriserNotifierQueueName());
                                
                                List<HolderBondAccount> bondAcctList = new ArrayList();
                                bondAcctList.add(bondAcct);
                                
                                wrapper.setCode(notification.createCode(login));
                                wrapper.setDescription("Authenticate storage of NUBAN account number for holder - " + holder.getFirstName() + " " + holder.getLastName());
                                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                                wrapper.setNotificationType(NotificationType.store_nuban.toString());
                                wrapper.setFrom(login.getUserId());
                                wrapper.setTo(authenticator);
                                wrapper.setModel(bondAcctList);
                                
                                logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                                resp = qSender.sendAuthorisationRequest(wrapper);
                                return resp;
                            }
                            resp.setRetn(317);
                            resp.setDesc("Bank does not exist.");
                            logger.info("Bank does not exist - [{}]", login.getUserId());
                            return resp;
                        }
                        resp.setRetn(317);
                        resp.setDesc("Bank cannot be empty.");
                        logger.info("Bank account cannot be empty - [{}]", login.getUserId());
                        return resp;
                    }
                    resp.setRetn(317);
                    resp.setDesc("NUBAN account cannot be empty.");
                    logger.info("NUBAN account cannot be empty - [{}]", login.getUserId());
                    return resp;
                }
                resp.setRetn(317);
                resp.setDesc("Holder's bond account does not exist.");
                logger.info("Holder's bond account does not exist - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(317);
            resp.setDesc("The holder does not exist.");
            logger.info("The holder does not exist - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error proccessing NUBAN account store. See error log - [{}]", login.getUserId());
            logger.error("error proccessing NUBAN account store - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to proccess NUBAN account store. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     * Processes the saved request to store a NUBAN account in a bond holder's company account.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the store NUBAN account request
     */
    public Response storeBondholderNubanAccountNumber_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        Notification notification = new Notification();
        logger.info("authorise NUBAN account number addition to holder bond account, invoked by - [{}]", login.getUserId());
        
        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<HolderBondAccount> bondAcctList = (List<HolderBondAccount>) wrapper.getModel();
            HolderBondAccount bondAcct = bondAcctList.get(0);
            
            if (hq.checkHolderAccount(bondAcct.getHolderId())) {
                org.greenpole.hibernate.entity.Holder holder = hq.getHolder(bondAcct.getHolderId());
                logger.info("Holder [{}] checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
                
                if (hq.checkHolderBondAccount(bondAcct.getHolderId(), bondAcct.getBondOfferId())) {
                    org.greenpole.hibernate.entity.HolderBondAccount bondAcct_hib = hq.getHolderBondAccount(bondAcct.getHolderId(), bondAcct.getBondOfferId());
                    logger.info("[{}]'s bond account checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
                    
                    if (bondAcct.getNubanAccount() != null && !"".equals(bondAcct.getNubanAccount())) {
                        
                        if (bondAcct.getBank() != null && bondAcct.getBank().getId() != 0) {
                            
                            if (hq.checkBank(bondAcct.getBank().getId())) {
                                Bank bank = hq.getBankDetails(bondAcct.getBank().getId());
                                
                                bondAcct_hib.setId(bondAcct_hib.getId());
                                bondAcct_hib.setNubanAccount(bondAcct.getNubanAccount());
                                bondAcct_hib.setBank(bank);
                                
                                hq.createUpdateHolderBondAccount(bondAcct_hib);
                                
                                notification.markAttended(notificationCode);
                                resp.setRetn(0);
                                resp.setDesc("Successful");
                                logger.info("NUBAN account stored - [{}]", login.getUserId());
                                return resp;
                            }
                            resp.setRetn(318);
                            resp.setDesc("Bank does not exist.");
                            logger.info("Bank does not exist - [{}]", login.getUserId());
                            return resp;
                        }
                        resp.setRetn(318);
                        resp.setDesc("Bank cannot be empty.");
                        logger.info("Bank account cannot be empty - [{}]", login.getUserId());
                        return resp;
                    }
                    resp.setRetn(318);
                    resp.setDesc("NUBAN account cannot be empty.");
                    logger.info("NUBAN account cannot be empty - [{}]", login.getUserId());
                    return resp;
                }
                resp.setRetn(318);
                resp.setDesc("Holder's bond account does not exist.");
                logger.info("Holder's bond account does not exist - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(318);
            resp.setDesc("The holder does not exist.");
            logger.info("The holder does not exist - [{}]", login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            logger.info("error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(98);
            resp.setDesc("Unable to store NUBAN account. Contact System Administrator");
            
            return resp;
        } catch (Exception ex) {
            logger.info("error storing NUBAN account. See error log - [{}]", login.getUserId());
            logger.error("error storing NUBAN account - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to store NUBAN account. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Request to create a holder account.
     * @param login The user's login details
     * @param authenticator The authenticator user meant to receive the
     * notification
     * @param holder Object representing holder details
     * @return Response to create holder account request
     */
    public Response createShareHolder_Request(Login login, String authenticator, Holder holder) {
        Response resp = new Response();
        Notification notification = new Notification();
        logger.info("Request to create holder account, invoked by [{}]", login.getUserId());
        
        NotificationWrapper wrapper;
        QueueSender queue;
        NotifierProperties prop;
        
        try {
            String desc = "";
            boolean flag = false;
            
            if (holder.getFirstName() == null || "".equals(holder.getFirstName())) {
                desc = "\nHolder first name should not be empty";
            } else if (holder.getLastName() == null || "".equals(holder.getLastName())) {
                desc += "\nHolder last name should not be empty";
            } else if (holder.getTypeId() <= 0) {
                desc += "\nHolder type should not be empty";
            }else if (holder.getPryAddress() == null || "".equals(holder.getPryAddress())) {
                desc += "\nPrimary Holder address is not specified";
            } else if (!holder.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                    && !holder.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
                desc += "\nPrimary address can only be residential or postal";
            } else if (holder.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                    && (holder.getResidentialAddresses() == null || holder.getResidentialAddresses().isEmpty())) {
                desc += "\nResidential address cannot be empty, as it is the primary address";
            } else if (holder.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString())
                    && (holder.getPostalAddresses() == null || holder.getPostalAddresses().isEmpty())) {
                desc += "\nPostal address cannot be empty, as it is the primary address";
            } else {
                flag = true;
            } 
            
            if (flag && (!"".equals(holder.getChn()) || holder.getChn() != null)) {
                if (hq.checkHolderAccount(holder.getChn())) {
                    desc += "\nThe CHN already exists";
                    flag = true;
                }
            }
            
            if (flag && holder.getTypeId() > 0) {
                boolean found = false;
                for (HolderType ht : hq.getAllHolderTypes()) {
                    if (holder.getTypeId() == ht.getId()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    desc += "\nHolder type is not valid";
                    flag = false;
                }
            } else if (holder.getTypeId() <= 0) {
                desc += "\nHolder type must be entered";
                flag = false;
            }
            
            if (flag && holder.getResidentialAddresses() != null && !holder.getResidentialAddresses().isEmpty()) {
                for (Address addr : holder.getResidentialAddresses()) {
                    if (addr.getAddressLine1() == null || "".equals(addr.getAddressLine1())) {
                        desc += "\nAddress line 1 should not be empty. Delete entire address if you must";
                        flag = false;
                        break;
                    } else if (addr.getState() == null || "".equals(addr.getState())) {
                        desc += "\nState should not be empty. Delete entire address if you must";
                        flag = false;
                        break;
                    } else if (addr.getCountry() == null || "".equals(addr.getCountry())) {
                        desc += "\nCountry should not be empty. Delete entire address if you must";
                        flag = false;
                        break;
                    }
                }
            }
            
            if (flag && holder.getPostalAddresses() != null && !holder.getPostalAddresses().isEmpty()) {
                for (Address addr : holder.getPostalAddresses()) {
                    if (addr.getAddressLine1() == null || "".equals(addr.getAddressLine1())) {
                        desc += "\nAddress line 1 should not be empty. Delete entire address if you must";
                        flag = false;
                        break;
                    } else if (addr.getState() == null || "".equals(addr.getState())) {
                        desc += "\nState should not be empty. Delete entire address if you must";
                        flag = false;
                        break;
                    } else if (addr.getCountry() == null || "".equals(addr.getCountry())) {
                        desc += "\nCountry should not be empty. Delete entire address if you must";
                        flag = false;
                        break;
                    }
                }
            }
            
            /*if (flag && holder.getEmailAddresses() != null && !holder.getEmailAddresses().isEmpty()) {
            for (EmailAddress email : holder.getEmailAddresses()) {
            if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
            desc += "\nEmail address should not be empty. Delete email entry if you must";
            flag = false;
            break;
            }
            }
            }
            
            if (flag && holder.getPhoneNumbers() != null && !holder.getPhoneNumbers().isEmpty()) {
            for (PhoneNumber phone : holder.getPhoneNumbers()) {
            if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
            desc += "\nPhone number should not be empty. Delete phone number entry if you must";
            flag = false;
            break;
            }
            }
            }*/
            
            if (flag) {
                
                wrapper = new NotificationWrapper();
                prop = new NotifierProperties(HolderComponentLogic.class);
                queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
                
                List<Holder> holdList = new ArrayList<>();
                holdList.add(holder);
                
                wrapper.setCode(notification.createCode(login));
                wrapper.setDescription("Authenticate holder account creation for " + holder.getFirstName() + " " + holder.getLastName());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setNotificationType(NotificationType.create_shareholder.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(holdList);
                logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                resp = queue.sendAuthorisationRequest(wrapper);
                return resp;
            }
            resp.setRetn(319);
            resp.setDesc("Error filing holder details: " + desc);
            logger.info("Error filing holder details - [{}] : ", login.getUserId(), desc);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General Error: Unable to create holder account. Contact system administrator." + "\nMessage: " + ex.getMessage());
            logger.info("Error creating holder account. See error log. [{}] - [{}]", resp.getRetn(), login.getUserId());
            logger.error("Error creating holder account - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }
    
    /**
     * Processes saved request to create holder account.
     * @param login The user's login details
     * @param notificationCode The notification code
     * @return response to holder account creation request
     */
    public Response createShareHolder_Authorise(Login login, String notificationCode) {
        logger.info("authorise shareholder account creation, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<Holder> holdList = (List<Holder>) wrapper.getModel();
            Holder holder = holdList.get(0);
            
            String desc = "";
            boolean flag = false;
            
            if (holder.getFirstName() == null || "".equals(holder.getFirstName())) {
                desc = "\nHolder first name should not be empty";
            } else if (holder.getLastName() == null || "".equals(holder.getLastName())) {
                desc += "\nHolder last name should not be empty";
            } else if (holder.getTypeId() <= 0) {
                desc += "\nHolder type should not be empty";
            }else if (holder.getPryAddress() == null || "".equals(holder.getPryAddress())) {
                desc += "\nPrimary Holder address is not specified";
            } else if (!holder.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                    && !holder.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
                desc += "\nPrimary address can only be residential or postal";
            } else if (holder.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                    && (holder.getResidentialAddresses() == null || holder.getResidentialAddresses().isEmpty())) {
                desc += "\nResidential address cannot be empty, as it is the primary address";
            } else if (holder.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString())
                    && (holder.getPostalAddresses() == null || holder.getPostalAddresses().isEmpty())) {
                desc += "\nPostal address cannot be empty, as it is the primary address";
            } else {
                flag = true;
            } 
            
            if (flag && (!"".equals(holder.getChn()) || holder.getChn() != null)) {
                if (hq.checkHolderAccount(holder.getChn())) {
                    desc += "\nThe CHN already exists";
                    flag = true;
                }
            }
            
            if (flag && holder.getTypeId() > 0) {
                boolean found = false;
                for (HolderType ht : hq.getAllHolderTypes()) {
                    if (holder.getTypeId() == ht.getId()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    desc += "\nHolder type is not valid";
                    flag = false;
                }
            } else if (holder.getTypeId() <= 0) {
                desc += "\nHolder type must be entered";
                flag = false;
            }
            
            if (flag && holder.getResidentialAddresses() != null && !holder.getResidentialAddresses().isEmpty()) {
                for (Address addr : holder.getResidentialAddresses()) {
                    if (addr.getAddressLine1() == null || "".equals(addr.getAddressLine1())) {
                        desc += "\nAddress line 1 should not be empty. Delete entire address if you must";
                        flag = false;
                        break;
                    } else if (addr.getState() == null || "".equals(addr.getState())) {
                        desc += "\nState should not be empty. Delete entire address if you must";
                        flag = false;
                        break;
                    } else if (addr.getCountry() == null || "".equals(addr.getCountry())) {
                        desc += "\nCountry should not be empty. Delete entire address if you must";
                        flag = false;
                        break;
                    }
                }
            }
            
            if (flag && holder.getPostalAddresses() != null && !holder.getPostalAddresses().isEmpty()) {
                for (Address addr : holder.getPostalAddresses()) {
                    if (addr.getAddressLine1() == null || "".equals(addr.getAddressLine1())) {
                        desc += "\nAddress line 1 should not be empty. Delete entire address if you must";
                        flag = false;
                        break;
                    } else if (addr.getState() == null || "".equals(addr.getState())) {
                        desc += "\nState should not be empty. Delete entire address if you must";
                        flag = false;
                        break;
                    } else if (addr.getCountry() == null || "".equals(addr.getCountry())) {
                        desc += "\nCountry should not be empty. Delete entire address if you must";
                        flag = false;
                        break;
                    }
                }
            }
            
            /*if (flag && holder.getEmailAddresses() != null && !holder.getEmailAddresses().isEmpty()) {
            for (EmailAddress email : holder.getEmailAddresses()) {
            if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
            desc += "\nEmail address should not be empty. Delete email entry if you must";
            flag = false;
            break;
            }
            }
            }
            
            if (flag && holder.getPhoneNumbers() != null && !holder.getPhoneNumbers().isEmpty()) {
            for (PhoneNumber phone : holder.getPhoneNumbers()) {
            if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
            desc += "\nPhone number should not be empty. Delete phone number entry if you must";
            flag = false;
            break;
            }
            }
            }*/
            
            if (flag) {
                org.greenpole.hibernate.entity.Holder holdEntity = new org.greenpole.hibernate.entity.Holder();
                HolderType typeEntity = hq.getHolderType(holder.getTypeId());
                
                holdEntity.setFirstName(holder.getFirstName());
                holdEntity.setLastName(holder.getLastName());
                holdEntity.setMiddleName(holder.getMiddleName());
                holdEntity.setHolderType(typeEntity);
                holdEntity.setGender(holder.getGender());
                holdEntity.setDob(formatter.parse(holder.getDob()));
                holdEntity.setPryAddress(holder.getPryAddress());
                holdEntity.setTaxExempted(holder.isTaxExempted());
                holdEntity.setPryHolder(true);
                holdEntity.setMerged(false);
                
                boolean created;
                
                if (holder.getChn() == null || "".equals(holder.getChn())) {
                    //there are two versions of createHolderAccount, one for bond account and the other for company account
                    //thus, null cannot be directly passed into the method, as java will not be able to distinguish between
                    //both createHolderAccount methods.
                    org.greenpole.hibernate.entity.HolderCompanyAccount emptyAcct = null;
                    created = hq.createHolderAccount(holdEntity, emptyAcct,
                            retrieveHolderResidentialAddress(holder), retrieveHolderPostalAddress(holder),
                            retrieveHolderEmailAddress(holder), retrieveHolderPhoneNumber(holder));
                } else {
                    holdEntity.setChn(holder.getChn());//set chn
                    created = hq.createHolderAccount(holdEntity, retrieveHolderCompanyAccount(holder),
                            retrieveHolderResidentialAddress(holder), retrieveHolderPostalAddress(holder),
                            retrieveHolderEmailAddress(holder), retrieveHolderPhoneNumber(holder));
                }

                if (created) {
                    notification.markAttended(notificationCode);
                    resp.setRetn(0);
                    resp.setDesc("Successful");
                    logger.info("Shareholder account creation successful - [{}]", login.getUserId());
                    return resp;
                } else {
                    resp.setRetn(320);
                    resp.setDesc("General Error. Unable to persist holder account. Contact system administrator.");
                    logger.info("Error persist holder account - [{}]", login.getUserId());
                    return resp;
                }
            }
            resp.setRetn(320);
            resp.setDesc("Error filing holder details: " + desc);
            logger.info("Error filing holder details: [{}] - [{}]", desc, login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            resp.setRetn(98);
            resp.setDesc("Error loading notification xml file. See error log");
            logger.info("Error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("Error loading notification xml file to object - [" + login.getUserId() + "] - ", ex);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General Error. Unable to persist holder account. Contact system administrator." + "\nMessage: " + ex.getMessage());
            logger.info("Error persist holder account. See error log - [{}]", login.getUserId());
            logger.error("Error persist holder account - [" + login.getUserId() + "] - ", ex);
            return resp;
        }
    }
    
    /**
     * Request to create bond holder account.
     * @param login user's login details
     * @param authenticator the authenticator user meant to receive the
     * notification
     * @param holder the holder details object
     * @return response to the bondholder account creation request
     */
    public Response createBondHolderAccount_Request(Login login, String authenticator, Holder holder) {
        logger.info("Request to create bondholder account, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        
        try {
            NotificationWrapper wrapper;
            QueueSender queue;
            NotifierProperties prop;
            
            String desc = "";
            boolean flag = false;

            if (holder.getFirstName() == null || "".equals(holder.getFirstName())) {
                desc = "\nHolder first name should not be empty";
            } else if (holder.getLastName() == null || "".equals(holder.getLastName())) {
                desc += "\nHolder last name should not be empty";
            } else if (holder.getChn() == null || "".equals(holder.getChn())) {
                desc += "\nCHN cannot be empty";
            } else if (holder.getTypeId() <= 0) {
                desc += "\nHolder type should not be empty";
            }else if (holder.getPryAddress() == null || "".equals(holder.getPryAddress())) {
                desc += "\nPrimary Holder address is not specified";
            } else if (!holder.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                    && !holder.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
                desc += "\nPrimary address can only be residential or postal";
            } else if (holder.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                    && (holder.getResidentialAddresses() == null || holder.getResidentialAddresses().isEmpty())) {
                desc += "\nResidential address cannot be empty, as it is the primary address";
            } else if (holder.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString())
                    && (holder.getPostalAddresses() == null || holder.getPostalAddresses().isEmpty())) {
                desc += "\nPostal address cannot be empty, as it is the primary address";
            } else {
                flag = true;
            } 
            
            if (flag && holder.getTypeId() > 0) {
                boolean found = false;
                for (HolderType ht : hq.getAllHolderTypes()) {
                    if (holder.getTypeId() == ht.getId()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    desc += "\nHolder type is not valid";
                    flag = false;
                }
            } else if (holder.getTypeId() <= 0) {
                desc += "\nHolder type must be entered";
                flag = false;
            }
            
            if (flag && holder.getResidentialAddresses() != null && !holder.getResidentialAddresses().isEmpty()) {
                for (Address addr : holder.getResidentialAddresses()) {
                    if (addr.getAddressLine1() == null || "".equals(addr.getAddressLine1())) {
                        desc += "\nAddress line 1 should not be empty. Delete entire address if you must";
                        flag = false;
                        break;
                    } else if (addr.getState() == null || "".equals(addr.getState())) {
                        desc += "\nState should not be empty. Delete entire address if you must";
                        flag = false;
                        break;
                    } else if (addr.getCountry() == null || "".equals(addr.getCountry())) {
                        desc += "\nCountry should not be empty. Delete entire address if you must";
                        flag = false;
                        break;
                    }
                }
            }
            
            if (flag && holder.getPostalAddresses() != null && !holder.getPostalAddresses().isEmpty()) {
                for (Address addr : holder.getPostalAddresses()) {
                    if (addr.getAddressLine1() == null || "".equals(addr.getAddressLine1())) {
                        desc += "\nAddress line 1 should not be empty. Delete entire address if you must";
                        flag = false;
                        break;
                    } else if (addr.getState() == null || "".equals(addr.getState())) {
                        desc += "\nState should not be empty. Delete entire address if you must";
                        flag = false;
                        break;
                    } else if (addr.getCountry() == null || "".equals(addr.getCountry())) {
                        desc += "\nCountry should not be empty. Delete entire address if you must";
                        flag = false;
                        break;
                    }
                }
            }
            
            /*if (flag && holder.getEmailAddresses() != null && !holder.getEmailAddresses().isEmpty()) {
            for (EmailAddress email : holder.getEmailAddresses()) {
            if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
            desc += "\nEmail address should not be empty. Delete email entry if you must";
            flag = false;
            break;
            }
            }
            }
            
            if (flag && holder.getPhoneNumbers() != null && !holder.getPhoneNumbers().isEmpty()) {
            for (PhoneNumber phone : holder.getPhoneNumbers()) {
            if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
            desc += "\nPhone number should not be empty. Delete phone number entry if you must";
            flag = false;
            break;
            }
            }
            }*/

            if (flag) {
                wrapper = new NotificationWrapper();
                prop = new NotifierProperties(HolderComponentLogic.class);
                queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());

                List<Holder> holdList = new ArrayList<>();
                holdList.add(holder);

                wrapper.setCode(notification.createCode(login));
                wrapper.setDescription("Authenticate bond holder account, " + holder.getFirstName() + " " + holder.getLastName());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setNotificationType(NotificationType.create_bondholder.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(holdList);
                logger.info("notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                resp = queue.sendAuthorisationRequest(wrapper);
                return resp;
            }
            resp.setRetn(321);
            resp.setDesc("Error filing bond holder details: " + desc);
            logger.info("Error filing bond holder details: [{}] - [{}]", desc, login.getUserId());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General Error: Unable to create holder account. Contact system administrator." + "\nMessage: " + ex.getMessage());
            logger.info("Error creating holder account. See error log - [{}]", login.getUserId());
            logger.error("Error creating holder account - [" + login.getUserId() + "] - ", ex);
            return resp;
        }
    }
    
    /**
     * Processes saved request to create bondholder account.
     * @param login user's login details
     * @param notificationCode the notification code
     * @return response to the bondholder account creation request
     */
    public Response createBondHolderAccount_Authorise(Login login, String notificationCode) {
        logger.info("authorise bondholder creation, invoked by - [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<Holder> holdList = (List<Holder>) wrapper.getModel();
            Holder holder = holdList.get(0);
            
            String desc = "";
            boolean flag = false;
            
            if (holder.getFirstName() == null || "".equals(holder.getFirstName())) {
                desc = "\nHolder first name should not be empty";
            } else if (holder.getLastName() == null || "".equals(holder.getLastName())) {
                desc += "\nHolder last name should not be empty";
            } else if (holder.getChn() == null || "".equals(holder.getChn())) {
                desc += "\nCHN cannot be empty";
            } else if (holder.getTypeId() <= 0) {
                desc += "\nHolder type should not be empty";
            }else if (holder.getPryAddress() == null || "".equals(holder.getPryAddress())) {
                desc += "\nPrimary Holder address is not specified";
            } else if (!holder.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                    && !holder.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
                desc += "\nPrimary address can only be residential or postal";
            } else if (holder.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                    && (holder.getResidentialAddresses() == null || holder.getResidentialAddresses().isEmpty())) {
                desc += "\nResidential address cannot be empty, as it is the primary address";
            } else if (holder.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString())
                    && (holder.getPostalAddresses() == null || holder.getPostalAddresses().isEmpty())) {
                desc += "\nPostal address cannot be empty, as it is the primary address";
            } else {
                flag = true;
            } 
            
            if (flag && holder.getTypeId() > 0) {
                boolean found = false;
                for (HolderType ht : hq.getAllHolderTypes()) {
                    if (holder.getTypeId() == ht.getId()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    desc += "\nHolder type is not valid";
                    flag = false;
                }
            } else if (holder.getTypeId() <= 0) {
                desc += "\nHolder type must be entered";
                flag = false;
            }
            
            if (flag && holder.getResidentialAddresses() != null && !holder.getResidentialAddresses().isEmpty()) {
                for (Address addr : holder.getResidentialAddresses()) {
                    if (addr.getAddressLine1() == null || "".equals(addr.getAddressLine1())) {
                        desc += "\nAddress line 1 should not be empty. Delete entire address if you must";
                        flag = false;
                        break;
                    } else if (addr.getState() == null || "".equals(addr.getState())) {
                        desc += "\nState should not be empty. Delete entire address if you must";
                        flag = false;
                        break;
                    } else if (addr.getCountry() == null || "".equals(addr.getCountry())) {
                        desc += "\nCountry should not be empty. Delete entire address if you must";
                        flag = false;
                        break;
                    }
                }
            }
            
            if (flag && holder.getPostalAddresses() != null && !holder.getPostalAddresses().isEmpty()) {
                for (Address addr : holder.getPostalAddresses()) {
                    if (addr.getAddressLine1() == null || "".equals(addr.getAddressLine1())) {
                        desc += "\nAddress line 1 should not be empty. Delete entire address if you must";
                        flag = false;
                        break;
                    } else if (addr.getState() == null || "".equals(addr.getState())) {
                        desc += "\nState should not be empty. Delete entire address if you must";
                        flag = false;
                        break;
                    } else if (addr.getCountry() == null || "".equals(addr.getCountry())) {
                        desc += "\nCountry should not be empty. Delete entire address if you must";
                        flag = false;
                        break;
                    }
                }
            }
            
            /*if (flag && holder.getEmailAddresses() != null && !holder.getEmailAddresses().isEmpty()) {
            for (EmailAddress email : holder.getEmailAddresses()) {
            if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
            desc += "\nEmail address should not be empty. Delete email entry if you must";
            flag = false;
            break;
            }
            }
            }
            
            if (flag && holder.getPhoneNumbers() != null && !holder.getPhoneNumbers().isEmpty()) {
            for (PhoneNumber phone : holder.getPhoneNumbers()) {
            if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
            desc += "\nPhone number should not be empty. Delete phone number entry if you must";
            flag = false;
            break;
            }
            }
            }*/
            
            if (flag) {
                org.greenpole.hibernate.entity.Holder holdEntity = new org.greenpole.hibernate.entity.Holder();
                HolderType typeEntity = hq.getHolderType(holder.getTypeId());
                
                holdEntity.setFirstName(holder.getFirstName());
                holdEntity.setLastName(holder.getLastName());
                holdEntity.setMiddleName(holder.getMiddleName());
                holdEntity.setHolderType(typeEntity);
                holdEntity.setGender(holder.getGender());
                holdEntity.setDob(formatter.parse(holder.getDob()));
                holdEntity.setChn(holder.getChn());
                holdEntity.setPryAddress(holder.getPryAddress());
                holdEntity.setTaxExempted(holder.isTaxExempted());
                holdEntity.setPryHolder(true);
                holdEntity.setMerged(false);

                boolean created = hq.createHolderAccount(holdEntity, retrieveHolderBondAccount(holder),
                        retrieveHolderResidentialAddress(holder), retrieveHolderPostalAddress(holder),
                        retrieveHolderEmailAddress(holder), retrieveHolderPhoneNumber(holder));

                if (created) {
                    notification.markAttended(notificationCode);
                    resp.setRetn(0);
                    resp.setDesc("Successful");
                    logger.info("Bond holder account creation successful - [{}]", login.getUserId());
                    return resp;
                }
                resp.setRetn(322);
                resp.setDesc("Unable to create bond holder from authorisation. Contact System Administrator");
                logger.info("Unable to create bond holder from authorisation - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(322);
            resp.setDesc("Error filing bond holder details: " + desc);
            logger.info("Error filing bond holder details: [{}] - [{}]", desc, login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            resp.setRetn(98);
            resp.setDesc("Error loading notification xml file. Contact administrator");
            logger.info("Error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("Error loading notification xml file to object, invoked by [" + login.getUserId() + "] - ", ex);
            return resp;
        } catch (ParseException ex) {
            resp.setRetn(322);
            resp.setDesc("Error creating bondholder account, due to formatting with date of birth");
            logger.info("Error creating bondholder account, due to formatting with date of birth - [{}]", login.getUserId());
            logger.error("Error creating bondholder account - [" + login.getUserId() + "] - ", ex);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("Error creating bondholder account. Contact administrator");
            logger.info("Error creating bondholder account - See error log - [{}]", login.getUserId());
            logger.error("Error creating bondholder account - [" + login.getUserId() + "] - ", ex);
            return resp;
        }
    }
    
    /**
     * Request to upload a holder signature.
     * @param login The user's login details
     * @param authenticator The authenticator meant to receive the notification
     * @param holderSig Holder signature details
     * @return response to the upload holder signature request
     */
    public Response uploadHolderSignature_Request(Login login, String authenticator, HolderSignature holderSig) {
        logger.info("Request to upload holder signature, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        BytesConverter bytesConverter = new BytesConverter();
        
        NotificationWrapper wrapper;
        QueueSender queue;
        NotifierProperties prop;
        
        boolean flag = false;

        try {
            org.greenpole.hibernate.entity.Holder holder = new org.greenpole.hibernate.entity.Holder();
            long defaultSize = Long.valueOf(greenProp.getSignatureSize());
            
            if (hq.checkHolderAccount(holderSig.getHolderId())) {
                holder = hq.getHolder(holderSig.getHolderId());
                logger.info("Holder [{}] checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
                
                if (hq.checkCurrentSignature(holderSig.getHolderId())) {
                    logger.info("Holder has current signature - [{}]", login.getUserId());
                    flag = true;
                } else {
                    flag = true;
                }
            }
            
            if (flag) {
                long fileSize = bytesConverter.decodeToBytes(holderSig.getSignatureContent()).length;
                
                if (fileSize <= defaultSize) {
                    wrapper = new NotificationWrapper();
                    prop = new NotifierProperties(HolderComponentLogic.class);
                    queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
                    
                    List<HolderSignature> holderListSignature = new ArrayList<>();
                    holderListSignature.add(holderSig);
                    
                    wrapper.setCode(notification.createCode(login));
                    wrapper.setDescription("Authenticate creation of holder signature for holder " + holder.getFirstName() + " " + holder.getLastName());
                    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                    wrapper.setNotificationType(NotificationType.upload_holder_signature.toString());
                    wrapper.setFrom(login.getUserId());
                    wrapper.setTo(authenticator);
                    wrapper.setModel(holderListSignature);
                    
                    logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                    resp = queue.sendAuthorisationRequest(wrapper);
                    return resp;
                }
                resp.setRetn(323);
                resp.setDesc("Error uploading signature. Signature can be no bigger than 2MB");
                logger.info("Error uploading signature. Signature can be no bigger than 2MB: [{}] - [{}]", resp.getRetn(), login.getUserId());
                return resp;
            }
            resp.setRetn(323);
            resp.setDesc("The holder does not exist.");
            logger.info("The holder does not exist - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error proccessing holder signature upload. See error log - [{}]", login.getUserId());
            logger.error("error proccessing holder signature upload - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to proccess holder signature upload. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Processes the saved request to upload holder signature.
     * @param login user's login details
     * @param notificationCode the notification code
     * @return response to the upload holder signature request
     */
    public Response uploadHolderSignature_Authorise(Login login, String notificationCode) {
        logger.info("authorise holder signature upload, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        BytesConverter bytesConverter = new BytesConverter();

        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<HolderSignature> holdSigntureList = (List<HolderSignature>) wrapper.getModel();
            HolderSignature sigModel = holdSigntureList.get(0);
            
            org.greenpole.hibernate.entity.HolderSignature sigEntity = new org.greenpole.hibernate.entity.HolderSignature();
            org.greenpole.hibernate.entity.HolderSignature currentSig = new org.greenpole.hibernate.entity.HolderSignature();
            
            long defaultSize = Long.valueOf(greenProp.getSignatureSize());
            
            if (hq.checkHolderAccount(sigModel.getHolderId())) {
                org.greenpole.hibernate.entity.Holder holder = hq.getHolder(sigModel.getHolderId());
                logger.info("Holder [{}] checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
                
                boolean currentSigExists = hq.checkCurrentSignature(sigModel.getHolderId());
                if (currentSigExists) {
                    currentSig = hq.getCurrentSignature(sigModel.getHolderId());
                    logger.info("Holder has current signature - [{}]", login.getUserId());
                }
                
                GreenpoleFile file = new GreenpoleFile(greenProp.getSignaturePath());
                long fileSize = bytesConverter.decodeToBytes(sigModel.getSignatureContent()).length;

                if (fileSize <= defaultSize) {
                    logger.info("Holder signature met file size requirement - [{}]", login.getUserId());

                    if (file.createFile(bytesConverter.decodeToBytes(sigModel.getSignatureContent()))) {
                        logger.info("Holder signature file created and saved - [{}]", login.getUserId());

                        String filepath = file.getFolderPath() + file.getFileName();

                        sigEntity.setTitle(sigModel.getTitle());
                        sigEntity.setSignaturePath(filepath);

                        sigEntity.setHolderSignaturePrimary(true);
                        sigEntity.setHolder(holder);

                        boolean uploaded;
                        if (currentSigExists) {
                            uploaded = hq.uploadHolderSignature(sigEntity, currentSig);
                        } else {
                            uploaded = hq.uploadHolderSignature(sigEntity, null);
                        }

                        if (uploaded) {
                            notification.markAttended(notificationCode);
                            resp.setRetn(0);
                            resp.setDesc("Successful");
                            logger.info("Holder signature successfully uploaded - [{}]", login.getUserId());
                            return resp;
                        }
                        resp.setRetn(324);
                        resp.setDesc("Holder signature upload failed due to database error. Contact Administrator.");
                        logger.info("Holder signature upload failed due to database error. Check error logs - [{}]", login.getUserId());
                        return resp;
                    }
                    resp.setRetn(324);
                    resp.setDesc("The Holder signature file could not be uploaded onto the server. Contact Administrator");
                    logger.info("The Holder signature file could not be uploaded onto the server. See error logs - [{}]", login.getUserId());
                    return resp;
                }
                resp.setRetn(324);
                resp.setDesc("The size of the holder signature cannot exceed 2MB.");
                logger.info("The size of the holder signature cannot exceed 2MB - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(324);
            resp.setDesc("The holder does not exist.");
            logger.info("The holder does not exist - [{}]", login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            logger.info("error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(98);
            resp.setDesc("Unable to upload holder signature. Contact System Administrator");
            
            return resp;
        } catch (Exception ex) {
            logger.info("error uploading holder signature. See error log - [{}]", login.getUserId());
            logger.error("error uploading holder signature - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to upload holder signature. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Request to query holder signature.
     * @param login user's login details
     * @param authenticator the authenticator user meant to receive the
     * notification details
     * @param queryParams the query parameters
     * @return response to query holder signature request
     */
    public Response queryHolderSignature_Request(Login login, String authenticator, HolderSignature queryParams) {
        logger.info("request to query holder signature, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        BytesConverter bytesConverter = new BytesConverter();
        
        try {
            HolderSignature sigModel = new HolderSignature();
            
            if (hq.checkHolderAccount(queryParams.getHolderId())) {
                org.greenpole.hibernate.entity.Holder holder = hq.getHolder(queryParams.getHolderId());
                logger.info("Holder [{}] checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());

                org.greenpole.hibernate.entity.HolderSignature sig_hib = hq.getCurrentSignature(queryParams.getHolderId());

                File file = new File(sig_hib.getSignaturePath());
                byte[] read = Files.readAllBytes(file.toPath());
                String encodedContents = bytesConverter.encodeToString(read);
                logger.info("Holder signature file successfully read - [{}]", login.getUserId());

                sigModel.setId(sig_hib.getId());
                sigModel.setHolderId(sig_hib.getHolder().getId());
                sigModel.setTitle(sig_hib.getTitle());
                sigModel.setSignaturePath(sig_hib.getSignaturePath());
                sigModel.setSignatureContent(encodedContents);
                sigModel.setPrimarySignature(sig_hib.getHolderSignaturePrimary());

                List<HolderSignature> sig_result = new ArrayList<>();
                sig_result.add(sigModel);

                resp.setRetn(0);
                resp.setDesc("Successful");
                resp.setBody(sig_result);
                logger.info("Holder signature query successful - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(325);
            resp.setDesc("The holder does not exist.");
            logger.info("The holder does not exist - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error querying holder signature. See error log - [{}]", login.getUserId());
            logger.error("error querying holder signature - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to query holder signature. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Request to transpose holder name.
     * @param login user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param holder the holder detail
     * @return response to the transpose name request
     */
    public Response transposeHolderName_Request(Login login, String authenticator, Holder holder) {
        logger.info("request to transpose holder name, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        
        NotificationWrapper wrapper;
        QueueSender queue;
        NotifierProperties prop;
        
        String desc = "";
        boolean flag = false;
        
        try {
            
            if (hq.checkHolderAccount(holder.getHolderId())) {
                org.greenpole.hibernate.entity.Holder holder_hib = hq.getHolder(holder.getHolderId());
                logger.info("holder exits - [{}] [{}]", holder.getFirstName(), holder.getLastName());
                
                if (holder.getFirstName() == null || "".equals(holder.getFirstName())) {
                    desc = "\nThe holder's first name cannot be empty";
                } else if (holder.getLastName() == null || "".equals(holder.getLastName())) {
                    desc = "\nThe holder's last name cannot be empty";
                } else {
                    flag = true;
                }

                if (flag) {
                    wrapper = new NotificationWrapper();
                    prop = new NotifierProperties(HolderComponentLogic.class);
                    queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());

                    List<Holder> holdList = new ArrayList<>();
                    holdList.add(holder);

                    wrapper.setCode(notification.createCode(login));
                    wrapper.setDescription("Authenticate transpose request for holder, " + holder_hib.getFirstName() + " " + holder_hib.getLastName());
                    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                    wrapper.setNotificationType(NotificationType.transpose.toString());
                    wrapper.setFrom(login.getUserId());
                    wrapper.setTo(authenticator);
                    wrapper.setModel(holdList);
                    
                    logger.info("notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                    resp = queue.sendAuthorisationRequest(wrapper);
                    return resp;
                }
                resp.setRetn(326);
                resp.setDesc("Error: " + desc);
                logger.info("error detected in holder name tranpose process - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(326);
            resp.setDesc("The holder does not exist.");
            logger.info("The holder does not exist - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error proccessing holder name transpose. See error log - [{}]", login.getUserId());
            logger.error("error proccessing holder name transpose - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to proccess holder name transpose. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Processes request to transpose holder names.
     * @param login user's login details
     * @param notificationCode the notification code
     * @return response object for the authorised request
     */
    public Response transposeHolderName_Authorise(Login login, String notificationCode) {
        logger.info("authorise holder name transpose, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        
        String desc = "";
        boolean flag = false;

        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<Holder> holdList = (List<Holder>) wrapper.getModel();
            Holder holder = holdList.get(0);
            
            if (hq.checkHolderAccount(holder.getHolderId())) {
                org.greenpole.hibernate.entity.Holder holder_hib = hq.getHolder(holder.getHolderId());
                logger.info("holder exits - [{}] [{}]", holder.getFirstName(), holder.getLastName());
                
                if (holder.getFirstName() == null || "".equals(holder.getFirstName())) {
                    desc = "\nThe holder's first name cannot be empty";
                } else if (holder.getLastName() == null || "".equals(holder.getLastName())) {
                    desc = "\nThe holder's last name cannot be empty";
                } else {
                    flag = true;
                }
                
                if (flag) {
                    holder_hib.setFirstName(holder.getFirstName());
                    holder_hib.setLastName(holder.getLastName());
                    
                    hq.updateHolderAccountForTranspose(holder_hib);
                    
                    notification.markAttended(notificationCode);
                    resp.setRetn(0);
                    resp.setDesc("Successful");
                    logger.info("Holder name transpose sucessful - [{}]", login.getUserId());
                    return resp;
                }
                resp.setRetn(327);
                resp.setDesc("Error: " + desc);
                logger.info("error detected in holder name tranpose process - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(327);
            resp.setDesc("The holder does not exist.");
            logger.info("The holder does not exist - [{}]", login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            logger.info("error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(98);
            resp.setDesc("Unable to proccess holder name transpose. Contact System Administrator");
            
            return resp;
        } catch (Exception ex) {
            logger.info("error proccessing holder name transpose. See error log - [{}]", login.getUserId());
            logger.error("error proccessing holder name transpose - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to proccess holder name transpose. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Request to edit holder details.
     * @param login user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param holder the edited holder details
     * @return response to the edit holder details request
     */
    public Response editHolderDetails_Request(Login login, String authenticator, Holder holder) {
        logger.info("request to edit holder details, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        
        NotificationWrapper wrapper;
        QueueSender queue;
        NotifierProperties prop;
        
        String desc = "";
        boolean flag = false;

        try {
            wrapper = new NotificationWrapper();
            prop = new NotifierProperties(HolderComponentLogic.class);
            queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
            
            //holder must exist
            if (hq.checkHolderAccount(holder.getHolderId())) {
                org.greenpole.hibernate.entity.Holder holderEntity = hq.getHolder(holder.getHolderId());
                logger.info("holder exists - [{}]: [{}]", login.getUserId(), holderEntity.getFirstName() + " " + holderEntity.getLastName());

                //holder must be primary to be edited
                if (holderEntity.getPryHolder()) {

                    if (holder.getFirstName() == null || "".equals(holder.getFirstName())) {
                        desc = "\nHolder first name should not be empty";
                    } else if (holder.getLastName() == null || "".equals(holder.getLastName())) {
                        desc += "\nHolder last name should not be empty";
                    } else if (holder.getTypeId() <= 0) {
                        desc += "\nHolder type should not be empty";
                    } else if (holder.getPryAddress() == null || "".equals(holder.getPryAddress())) {
                        desc += "\nPrimary Holder address is not specified";
                    } else if (!holder.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                            && !holder.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
                        desc += "\nPrimary address can only be residential or postal";
                    } else if (holder.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                            && (holder.getResidentialAddresses() == null || holder.getResidentialAddresses().isEmpty())) {
                        desc += "\nResidential address cannot be empty, as it is the primary address";
                    } else if (holder.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString())
                            && (holder.getPostalAddresses() == null || holder.getPostalAddresses().isEmpty())) {
                        desc += "\nPostal address cannot be empty, as it is the primary address";
                    } else {
                        flag = true;
                    }

                    if (flag && holder.getTypeId() > 0) {
                        boolean found = false;
                        for (HolderType ht : hq.getAllHolderTypes()) {
                            if (holder.getTypeId() == ht.getId()) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            desc += "\nHolder type is not valid";
                            flag = false;
                        }
                    } else if (holder.getTypeId() <= 0) {
                        desc += "\nHolder type must be entered";
                        flag = false;
                    }
                    
                    if (flag && holder.getChanges() != null && !holder.getChanges().isEmpty()) {
                        for (HolderChanges hc : holder.getChanges()) {
                            boolean found = false;
                            for (HolderChangeType hct : hq.getAllChangeTypes()) {
                                if (hc.getChangeTypeId() == hct.getId()) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                desc += "\nHolder change type is not valid";
                                flag = false;
                                break;
                            }
                        }
                    }

                    if (flag && holder.getResidentialAddresses() != null && !holder.getResidentialAddresses().isEmpty()) {
                        for (Address addr : holder.getResidentialAddresses()) {
                            if (addr.getAddressLine1() == null || "".equals(addr.getAddressLine1())) {
                                desc += "\nAddress line 1 should not be empty. Delete entire address if you must";
                                flag = false;
                                break;
                            } else if (addr.getState() == null || "".equals(addr.getState())) {
                                desc += "\nState should not be empty. Delete entire address if you must";
                                flag = false;
                                break;
                            } else if (addr.getCountry() == null || "".equals(addr.getCountry())) {
                                desc += "\nCountry should not be empty. Delete entire address if you must";
                                flag = false;
                                break;
                            }
                        }
                    }

                    if (flag && holder.getPostalAddresses() != null && !holder.getPostalAddresses().isEmpty()) {
                        for (Address addr : holder.getPostalAddresses()) {
                            if (addr.getAddressLine1() == null || "".equals(addr.getAddressLine1())) {
                                desc += "\nAddress line 1 should not be empty. Delete entire address if you must";
                                flag = false;
                                break;
                            } else if (addr.getState() == null || "".equals(addr.getState())) {
                                desc += "\nState should not be empty. Delete entire address if you must";
                                flag = false;
                                break;
                            } else if (addr.getCountry() == null || "".equals(addr.getCountry())) {
                                desc += "\nCountry should not be empty. Delete entire address if you must";
                                flag = false;
                                break;
                            }
                        }
                    }

                    /*if (flag && holder.getEmailAddresses() != null && !holder.getEmailAddresses().isEmpty()) {
                    for (EmailAddress email : holder.getEmailAddresses()) {
                    if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
                    desc += "\nEmail address should not be empty. Delete email entry if you must";
                    flag = false;
                    break;
                    }
                    }
                    }
                    
                    if (flag && holder.getPhoneNumbers() != null && !holder.getPhoneNumbers().isEmpty()) {
                    for (PhoneNumber phone : holder.getPhoneNumbers()) {
                    if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
                    desc += "\nPhone number should not be empty. Delete phone number entry if you must";
                    flag = false;
                    break;
                    }
                    }
                    }*/
   
                    if (flag && (holderEntity.getChn() != null || "".equals(holderEntity.getChn()))
                            && (holder.getChn() == null || "".equals(holder.getChn()))) {
                        desc += "\nCHN cannot be erased";
                        flag = false;
                    }
                    
                    if (flag) {
                        List<Holder> holdList = new ArrayList<>();
                        holdList.add(holder);
                        wrapper.setCode(notification.createCode(login));
                        wrapper.setDescription("Authenticate edit of holder account, " + holder.getFirstName() + " " + holder.getLastName());
                        wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                        wrapper.setNotificationType(NotificationType.edit_holder.toString());
                        wrapper.setFrom(login.getUserId());
                        wrapper.setTo(authenticator);
                        wrapper.setModel(holdList);
                        
                        logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                        resp = queue.sendAuthorisationRequest(wrapper);
                        return resp;
                    }
                    resp.setRetn(328);
                    resp.setDesc("Error filing holder details: " + desc);
                    logger.info("Error filing holder details: [{}] - [{}]", desc, login.getUserId());
                    return resp;
                }
                resp.setRetn(328);
                resp.setDesc("Holder account is not a primary account. Non-primary accounts cannot be edited");
                logger.info("Holder account is not a primary account. Non-primary accounts cannot be edited - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(328);
            resp.setDesc("The holder does not exist.");
            logger.info("The holder does not exist - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("Error editing holder account. See error log - [{}]", login.getUserId());
            logger.error("Error editing holder account - [" + login.getUserId() + "] - ", ex);
            resp.setRetn(99);
            resp.setDesc("General Error. Unable to editing holder account. Contact system administrator." + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Processes saved request to edit holder details.
     * @param login user's login details
     * @param notificationCode the notification code
     * @return response to the edit holder request
     */
    public Response editHolderDetails_Authorise(Login login, String notificationCode) {
        logger.info("authorise holder details edit, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        
        String desc = "";
        boolean flag = false;

        try {
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<Holder> holderEditList = (List<Holder>) wrapper.getModel();
            Holder holder = holderEditList.get(0);

            //holder must exist
            if (hq.checkHolderAccount(holder.getHolderId())) {
                org.greenpole.hibernate.entity.Holder holderEntity = hq.getHolder(holder.getHolderId());
                logger.info("holder exists - [{}]: [{}]", login.getUserId(), holderEntity.getFirstName() + " " + holderEntity.getLastName());

                //holder must be primary to be edited
                if (holderEntity.getPryHolder()) {

                    if (holder.getFirstName() == null || "".equals(holder.getFirstName())) {
                        desc = "\nHolder first name should not be empty";
                    } else if (holder.getLastName() == null || "".equals(holder.getLastName())) {
                        desc += "\nHolder last name should not be empty";
                    } else if (holder.getTypeId() <= 0) {
                        desc += "\nHolder type should not be empty";
                    } else if (holder.getPryAddress() == null || "".equals(holder.getPryAddress())) {
                        desc += "\nPrimary Holder address is not specified";
                    } else if (!holder.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                            && !holder.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
                        desc += "\nPrimary address can only be residential or postal";
                    } else if (holder.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                            && (holder.getResidentialAddresses() == null || holder.getResidentialAddresses().isEmpty())) {
                        desc += "\nResidential address cannot be empty, as it is the primary address";
                    } else if (holder.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString())
                            && (holder.getPostalAddresses() == null || holder.getPostalAddresses().isEmpty())) {
                        desc += "\nPostal address cannot be empty, as it is the primary address";
                    } else {
                        flag = true;
                    }

                    if (flag && holder.getTypeId() > 0) {
                        boolean found = false;
                        for (HolderType ht : hq.getAllHolderTypes()) {
                            if (holder.getTypeId() == ht.getId()) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            desc += "\nHolder type is not valid";
                            flag = false;
                        }
                    } else if (holder.getTypeId() <= 0) {
                        desc += "\nHolder type must be entered";
                        flag = false;
                    }
                    
                    if (flag && holder.getChanges() != null && !holder.getChanges().isEmpty()) {
                        for (HolderChanges hc : holder.getChanges()) {
                            boolean found = false;
                            for (HolderChangeType hct : hq.getAllChangeTypes()) {
                                if (hc.getChangeTypeId() == hct.getId()) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                desc += "\nHolder change type is not valid";
                                flag = false;
                                break;
                            }
                        }
                    }

                    if (flag && holder.getResidentialAddresses() != null && !holder.getResidentialAddresses().isEmpty()) {
                        for (Address addr : holder.getResidentialAddresses()) {
                            if (addr.getAddressLine1() == null || "".equals(addr.getAddressLine1())) {
                                desc += "\nAddress line 1 should not be empty. Delete entire address if you must";
                                flag = false;
                                break;
                            } else if (addr.getState() == null || "".equals(addr.getState())) {
                                desc += "\nState should not be empty. Delete entire address if you must";
                                flag = false;
                                break;
                            } else if (addr.getCountry() == null || "".equals(addr.getCountry())) {
                                desc += "\nCountry should not be empty. Delete entire address if you must";
                                flag = false;
                                break;
                            }
                        }
                    }

                    if (flag && holder.getPostalAddresses() != null && !holder.getPostalAddresses().isEmpty()) {
                        for (Address addr : holder.getPostalAddresses()) {
                            if (addr.getAddressLine1() == null || "".equals(addr.getAddressLine1())) {
                                desc += "\nAddress line 1 should not be empty. Delete entire address if you must";
                                flag = false;
                                break;
                            } else if (addr.getState() == null || "".equals(addr.getState())) {
                                desc += "\nState should not be empty. Delete entire address if you must";
                                flag = false;
                                break;
                            } else if (addr.getCountry() == null || "".equals(addr.getCountry())) {
                                desc += "\nCountry should not be empty. Delete entire address if you must";
                                flag = false;
                                break;
                            }
                        }
                    }

                    /*if (flag && holder.getEmailAddresses() != null && !holder.getEmailAddresses().isEmpty()) {
                    for (EmailAddress email : holder.getEmailAddresses()) {
                    if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
                    desc += "\nEmail address should not be empty. Delete email entry if you must";
                    flag = false;
                    break;
                    }
                    }
                    }
                    
                    if (flag && holder.getPhoneNumbers() != null && !holder.getPhoneNumbers().isEmpty()) {
                    for (PhoneNumber phone : holder.getPhoneNumbers()) {
                    if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
                    desc += "\nPhone number should not be empty. Delete phone number entry if you must";
                    flag = false;
                    break;
                    }
                    }
                    }*/
   
                    if (flag && (holderEntity.getChn() != null || "".equals(holderEntity.getChn()))
                            && (holder.getChn() == null || "".equals(holder.getChn()))) {
                        desc += "\nCHN cannot be erased";
                        flag = false;
                    }

                    if (flag) {
                        HolderType typeEntity = hq.getHolderType(holder.getTypeId());
                        
                        holderEntity.setFirstName(holder.getFirstName());
                        holderEntity.setMiddleName(holder.getMiddleName());
                        holderEntity.setLastName(holder.getLastName());
                        holderEntity.setHolderType(typeEntity);
                        holderEntity.setGender(holder.getGender());
                        holderEntity.setDob(formatter.parse(holder.getDob()));
                        holderEntity.setChn(holder.getChn());
                        holderEntity.setPryAddress(holder.getPryAddress());
                        List<org.greenpole.hibernate.entity.HolderChanges> holderChangesList = new ArrayList<>();
                        org.greenpole.hibernate.entity.HolderChanges changes = new org.greenpole.hibernate.entity.HolderChanges();
                        
                        if (holder.getChanges() != null && !holder.getChanges().isEmpty()) {
                            for (org.greenpole.entity.model.holder.HolderChanges hc : holder.getChanges()) {
                                HolderChangeType changeType = hq.getChangeType(hc.getChangeTypeId());
                                
                                changes.setHolder(holderEntity);
                                changes.setInitialForm(hc.getInitialForm());
                                changes.setCurrentForm(hc.getCurrentForm());
                                changes.setChangeDate(formatter.parse(hc.getChangeDate()));
                                changes.setHolderChangeType(changeType);
                                
                                holderChangesList.add(changes);
                            }
                        }
                        
                        boolean updated = hq.updateHolderAccount(holderEntity, retrieveHolderResidentialAddress(holder),
                                retrieveHolderPostalAddress(holder), retrieveHolderPhoneNumber(holder),
                                retrieveHolderEmailAddress(holder), holderChangesList);
                        
                        if (updated) {
                            notification.markAttended(notificationCode);
                            resp.setRetn(0);
                            resp.setDesc("Holder details saved");
                            logger.info("Holder account update successful - [{}]", login.getUserId());
                            // Send SMS/Email notification to shareholder
                            return resp;
                        } else {
                            resp.setRetn(329);
                            resp.setDesc("An error occurred while updating the holder's details. Contact Administrator.");
                            logger.info("An error occurred while updating the holder's details - [{}]", login.getUserId());
                            // Send SMS/Email notification to shareholder IF USER PERMITS
                            return resp;
                        }
                    }
                    resp.setRetn(329);
                    resp.setDesc("Error filing holder details: " + desc);
                    logger.info("Error filing holder details: [{}] - [{}]", desc, login.getUserId());
                    return resp;
                }
                resp.setRetn(329);
                resp.setDesc("Holder account is not a primary account. Non-primary accounts cannot be edited");
                logger.info("Holder account is not a primary account. Non-primary accounts cannot be edited - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(329);
            resp.setDesc("The holder does not exist.");
            logger.info("The holder does not exist - [{}]", login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            logger.info("error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(98);
            resp.setDesc("Unable to proccess holder details edit. Contact System Administrator");
            
            return resp;
        } catch (Exception ex) {
            logger.info("error proccessing holder details edit. See error log - [{}]", login.getUserId());
            logger.error("error proccessing holder details edit - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to proccess holder details edit. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Request to view report on consolidation of Shareholder Accounts
     * @param login the user Id of the user performing the view request
     * @param queryParams the query parameters
     * @return response to the view report on shareholder accounts consolidation request
     */
    public Response viewAccountConsolidation_request(Login login, QueryHolderConsolidation queryParams) {
        logger.info("request to query company account consolidation, invoked by [{}] ", login.getUserId());
        Response resp = new Response();
        Descriptor descriptorUtil = new Descriptor();
        
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            
            Map<String, String> descriptors = descriptorUtil.decipherDescriptor(queryParams.getDescriptor());
            if (descriptors.size() == 1) {
                //check start date is properly formatted
                if (descriptors.get("date").equalsIgnoreCase("none")) {
                    try {
                        formatter.parse(queryParams.getStartDate());
                    } catch (ParseException ex) {
                        logger.info("an error was thrown while checking the start date. See error log invoked by [{}]", login.getUserId());
                        resp.setRetn(300);
                        resp.setDesc("Incorrect date format for start date");
                        logger.error("Incorrect date format for start date invoked by [{}]", login.getUserId(), ex);
                        
                        return resp;
                    }
                }
                //check end date is properly formatted
                if (descriptors.get("date").equalsIgnoreCase("between")) {
                    try {
                        formatter.parse(queryParams.getEndDate());
                    } catch (ParseException ex) {
                        logger.info("an error was thrown while checking the start date. See error log - [{}]", login.getUserId());
                        resp.setRetn(300);
                        resp.setDesc("Incorrect date format for end date");
                        logger.error("Incorrect date format for end date - [{}]", login.getUserId(), ex);

                        return resp;
                    }
                }
                List<org.greenpole.hibernate.entity.AccountConsolidation> acctConsolList = hq.getAllHolderAccountConsolidation(queryParams.getDescriptor(), queryParams.getStartDate(),
                        queryParams.getEndDate(), greenProp.getDateFormat());
                List<org.greenpole.entity.model.holder.merge.AccountConsolidation> acctConsolModelList = new ArrayList<>();
                TagUser tag = new TagUser();
                for (org.greenpole.hibernate.entity.AccountConsolidation ac : acctConsolList) {
                    org.greenpole.entity.model.holder.merge.AccountConsolidation acctConsolModel = new org.greenpole.entity.model.holder.merge.AccountConsolidation();
                    acctConsolModel.setId(ac.getId());
                    acctConsolModel.setHolderId(ac.getHolder().getId());
                    acctConsolModel.setHolderName(ac.getHolderName());
                    acctConsolModel.setMergedToHolderId(ac.getMergedToHolderId());
                    acctConsolModel.setMergedToHolderName(ac.getMergedToHolderName());
                    if (ac.getMergeDate() != null)
                        acctConsolModel.setMergeDate(formatter.format(ac.getMergeDate()));
                    acctConsolModel.setAdditionalChanges(ac.getAdditionalChanges());
                    if (ac.getDemerge() != null)
                        acctConsolModel.setDemerge(ac.getDemerge());
                    if (ac.getDemergeDate() != null)
                        acctConsolModel.setDemergeDate(formatter.format(ac.getDemergeDate()));

                    List<org.greenpole.entity.model.holder.merge.CompanyAccountConsolidation> cacList = new ArrayList<>();
                    for (org.greenpole.hibernate.entity.CompanyAccountConsolidation cac : hq.getCompAcctConsolidationIgnoreDemerge(ac.getId())) {
                        org.greenpole.entity.model.holder.merge.CompanyAccountConsolidation compAcctConsolModel = new org.greenpole.entity.model.holder.merge.CompanyAccountConsolidation();
                        if (cac.getForCompanyId() != null)
                            compAcctConsolModel.setForCompanyId(cac.getForCompanyId());
                        if (cac.getForBondOfferId() != null)
                            compAcctConsolModel.setForBondOfferId(cac.getForBondOfferId());
                        System.out.println(":::company consolidation reached!!::");
                        compAcctConsolModel.setTiedToInitialHolderId(cac.getTiedToCurrentHolderId());
                        compAcctConsolModel.setTiedToCurrentHolderId(cac.getTiedToCurrentHolderId());
                        compAcctConsolModel.setInitialChn(cac.getInitialChn());
                        compAcctConsolModel.setCurrentChn(cac.getCurrentChn());
                        compAcctConsolModel.setBondShareUnit(cac.getBondShareUnit());
                        compAcctConsolModel.setTransfer(cac.getTransfer());
                        if (cac.getReceiverUnitState() != null)
                            compAcctConsolModel.setReceiverUnitState(cac.getReceiverUnitState());
                        if (cac.getReceiverStartUnit() != null)
                            compAcctConsolModel.setReceiverStartUnit(cac.getReceiverStartUnit());
                        if (cac.getUnitAfterTransfer() != null)
                            compAcctConsolModel.setUnitAfterTransfer(cac.getUnitAfterTransfer());
                        if (cac.getMergeDate() != null)
                            compAcctConsolModel.setMergeDate(formatter.format(cac.getMergeDate()));
                        
                        cacList.add(compAcctConsolModel);
                    }
                    acctConsolModel.setCompanyAccountConsolidation(cacList);
                    acctConsolModelList.add(acctConsolModel);
                }
                List<TagUser> tagList = new ArrayList<>();

                tag.setQueryParam(queryParams);
                tag.setResult(acctConsolModelList);
                tagList.add(tag);

                resp.setBody(tagList);
                resp.setDesc("Query Successful");
                resp.setRetn(0);
                logger.info("Query successful - [{}]", login.getUserId());
                return resp;
            }
            logger.info("descriptor length does not match expected required length - [{}]", login.getUserId());
            resp.setRetn(330);
            resp.setDesc("descriptor length does not match expected required length");
            return resp;
        } catch (Exception ex) {
            logger.info("error querying holder changes. See error log - [{}]", login.getUserId());
            logger.error("error querying holder changes - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to query holder changes. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Request to apply for a bond offer.
     * @param login the user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param bondAccount the bond account containing the bond application
     * @return response to the bond offer application request
     */
    public Response applyForBondOffer_Request(Login login, String authenticator, HolderBondAccount bondAccount) {
        logger.info("request to apply for bond offer, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        
        NotificationWrapper wrapper;
        QueueSender queue;
        NotifierProperties prop;
        
        try {
            
            if (hq.checkHolderAccount(bondAccount.getHolderId())) {
                org.greenpole.hibernate.entity.Holder holder_hib = hq.getHolder(bondAccount.getHolderId());

                if (cq.bondOfferIsValid(bondAccount.getBondOfferId())) {
                    BondOffer bond = cq.getBondOffer(bondAccount.getBondOfferId());
                    
                    wrapper = new NotificationWrapper();
                    prop = new NotifierProperties(HolderComponentLogic.class);
                    queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());

                    List<HolderBondAccount> acctList = new ArrayList<>();
                    acctList.add(bondAccount);

                    wrapper.setCode(notification.createCode(login));
                    wrapper.setDescription("Authenticate holder, " + holder_hib.getFirstName() + " " + holder_hib.getLastName() + 
                            "'s application for the bond offer - " + bond.getTitle());
                    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                    wrapper.setNotificationType(NotificationType.apply_for_bond_offer.toString());
                    wrapper.setFrom(login.getUserId());
                    wrapper.setTo(authenticator);
                    wrapper.setModel(acctList);

                    logger.info("notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                    resp = queue.sendAuthorisationRequest(wrapper);
                    return resp;
                }
                resp.setRetn(331);
                resp.setDesc("The bond offer is no longer valid.");
                logger.info("The bond offer is no longer valid - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(331);
            resp.setDesc("The holder does not exist.");
            logger.info("The holder does not exist - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error proccessing holder bond application. See error log - [{}]", login.getUserId());
            logger.error("error proccessing holder bond application - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to proccess holder bond application. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Processes a saved request to apply for a bond offer.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the bond offer application request
     */
    public Response applyForBondOffer_Authorise(Login login, String notificationCode) {
        logger.info("request authorisation to persist holder details. Invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();

        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<HolderBondAccount> acctList = (List<HolderBondAccount>) wrapper.getModel();
            HolderBondAccount bondAccount = acctList.get(0);
            
            if (hq.checkHolderAccount(bondAccount.getHolderId())) {

                if (cq.bondOfferIsValid(bondAccount.getBondOfferId())) {
                    
                    org.greenpole.hibernate.entity.HolderBondAccount bondAcct_hib = new org.greenpole.hibernate.entity.HolderBondAccount();
                    HolderBondAccountId id = new HolderBondAccountId();
                    
                    id.setBondOfferId(bondAccount.getBondOfferId());
                    id.setHolderId(bondAccount.getHolderId());
                    bondAcct_hib.setId(id);
                    bondAcct_hib.setBondUnits(bondAccount.getBondUnits());
                    bondAcct_hib.setStartingPrincipalValue(bondAccount.getStartingPrincipalValue());
                    bondAcct_hib.setRemainingPrincipalValue(0.00);
                    bondAcct_hib.setDateApplied(new Date());
                    bondAcct_hib.setHolderBondAcctPrimary(true);
                    bondAcct_hib.setMerged(false);
                    
                    hq.createUpdateHolderBondAccount(bondAcct_hib);
                    
                    notification.markAttended(notificationCode);
                    resp.setRetn(0);
                    resp.setDesc("Bond Offer application successful");
                    logger.info("Bond Offer application successful - [{}]", login.getUserId());
                    return resp;
                }
                resp.setRetn(332);
                resp.setDesc("The bond offer is no longer valid.");
                logger.info("The bond offer is no longer valid - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(332);
            resp.setDesc("The holder does not exist.");
            logger.info("The holder does not exist - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error proccessing holder bond application. See error log - [{}]", login.getUserId());
            logger.error("error proccessing holder bond application - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to proccess holder bond application. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     * Unwraps the holder model to create the administrator hibernate entity model.
     * @param holder the holder which contains a list of its administrators to be created
     * @return a holder hibernate entity with the administrators to be created
     */
    private org.greenpole.hibernate.entity.Holder upwrapAdministrator(Holder holder) {
        org.greenpole.hibernate.entity.Holder holder_hib;
        
        //get holder entity
        holder_hib = hq.getHolder(holder.getHolderId());
        String nameAddition = "Estate of " + holder_hib.getFirstName();
        holder_hib.setFirstName(nameAddition); //change holder name to begin with "estate of" because holder is now deceased
        
        //get all administrators
        Set admins_hib = new HashSet();
        for (Administrator admin_model : holder.getAdministrators()) {
            org.greenpole.hibernate.entity.Administrator admin_hib = new org.greenpole.hibernate.entity.Administrator();
            //add main administrator details to hibernate entity
            admin_hib.setFirstName(admin_model.getFirstName());
            admin_hib.setLastName(admin_model.getLastName());
            admin_hib.setMiddleName(admin_model.getMiddleName());
            admin_hib.setPryAddress(admin_model.getPryAddress());
            
            //admin id is only set during edit
            if (admin_model.getId() > 0) {
                admin_hib = hq.getAdministrator(admin_model.getId());
            }
                        
            //add residential addresses to hibernate entity
            retrieveAdministratorResidentialAddress(admin_model, admin_hib);
            //add postal addresses to hibernate entity
            retrieveAdministratorPostalAddress(admin_model, admin_hib); 
            //add email addresses to hibernate entity
            retrieveAdministratorEmailAddress(admin_model, admin_hib);
            //add phone number to hibernate entity
            retrieveAdministratorPhoneNumber(admin_model, admin_hib);
            
            //add hibernate administrator into set for administrators
            admins_hib.add(admin_hib);
        }
        //add administrator set into holder
        holder_hib.setAdministrators(admins_hib);
        
        return holder_hib;
    }

    private void retrieveAdministratorPhoneNumber(Administrator admin_model, org.greenpole.hibernate.entity.Administrator admin_hib) {
        //add phone number to hibernate entity
        if (admin_model.getPhoneNumbers() != null && !admin_model.getPhoneNumbers().isEmpty()) {
            Set phone_set = new HashSet();
            for (PhoneNumber admin_phone_model : admin_model.getPhoneNumbers()) {
                AdministratorPhoneNumber admin_phone_hib = new AdministratorPhoneNumber();
                
                admin_phone_hib.setPhoneNumber(admin_phone_model.getPhoneNumber());
                admin_phone_hib.setIsPrimary(admin_phone_model.isPrimaryPhoneNumber());
                
                phone_set.add(admin_phone_hib);
            }
            admin_hib.setAdministratorPhoneNumbers(phone_set);
        }
    }

    private void retrieveAdministratorEmailAddress(Administrator admin_model, org.greenpole.hibernate.entity.Administrator admin_hib) {
        //add email address to hibernate entity
        if (admin_model.getEmailAddresses() != null && !admin_model.getEmailAddresses().isEmpty()) {
            Set email_set = new HashSet();
            for (EmailAddress admin_email_model : admin_model.getEmailAddresses()) {
                AdministratorEmailAddress admin_email_hib = new AdministratorEmailAddress();
                
                admin_email_hib.setEmailAddress(admin_email_model.getEmailAddress());
                admin_email_hib.setIsPrimary(admin_email_model.isPrimaryEmail());
                
                email_set.add(admin_email_hib);
            }
            admin_hib.setAdministratorEmailAddresses(email_set);
        }
    }

    private void retrieveAdministratorPostalAddress(Administrator admin_model, org.greenpole.hibernate.entity.Administrator admin_hib) {
        //create set
        Set pos_set = new HashSet();
        if (admin_model.getPostalAddress() != null) {
            Address admin_pos_model = admin_model.getResidentialAddress();
            
            AdministratorPostalAddress admin_pos_hib = new AdministratorPostalAddress();
            
            admin_pos_hib.setAddressLine1(admin_pos_model.getAddressLine1());
            admin_pos_hib.setState(admin_pos_model.getState());
            admin_pos_hib.setCountry(admin_pos_model.getCountry());
            
            admin_pos_hib.setAddressLine2(admin_pos_model.getAddressLine2());
            admin_pos_hib.setAddressLine3(admin_pos_model.getAddressLine3());
            admin_pos_hib.setAddressLine4(admin_pos_model.getAddressLine4());
            admin_pos_hib.setCity(admin_pos_model.getCity());
            admin_pos_hib.setPostCode(admin_pos_model.getPostCode());
            admin_pos_hib.setIsPrimary(admin_pos_model.isPrimaryAddress());
            
            pos_set.add(admin_pos_hib); //add residential address to set
        }
        admin_hib.setAdministratorPostalAddresses(pos_set); //add set to administrator hibernate entity
    }

    private void retrieveAdministratorResidentialAddress(Administrator admin_model, org.greenpole.hibernate.entity.Administrator admin_hib) {
        //add residential addresses to hibernate entity
        Set res_set = new HashSet();
        if (admin_model.getResidentialAddress() != null) {
            Address admin_res_model = admin_model.getResidentialAddress();
            
            AdministratorResidentialAddress admin_res_hib = new AdministratorResidentialAddress();
            
            admin_res_hib.setAddressLine1(admin_res_model.getAddressLine1());
            admin_res_hib.setState(admin_res_model.getState());
            admin_res_hib.setCountry(admin_res_model.getCountry());
            admin_res_hib.setAddressLine2(admin_res_model.getAddressLine2());
            admin_res_hib.setAddressLine3(admin_res_model.getAddressLine3());
            admin_res_hib.setAddressLine4(admin_res_model.getAddressLine4());
            admin_res_hib.setCity(admin_res_model.getCity());
            admin_res_hib.setPostCode(admin_res_model.getPostCode());
            admin_res_hib.setIsPrimary(admin_res_model.isPrimaryAddress());
            
            //create set
            res_set.add(admin_res_hib); //add residential address to set
        }
        admin_hib.setAdministratorResidentialAddresses(res_set); //add set to administrator hibernate entity
    }
    
    
    /**
     * Unwraps the holder postal address details from the HolderModel into
     * HolderPostalAddress hibernate entity
     *
     * @param holdModel the holderModel of holder details
     * @param newEntry boolean value indicating new entry
     * @return List object of HolderPostalAddress hibernate entity
     */
    private List<HolderPostalAddress> retrieveHolderPostalAddress(Holder holdModel/*, boolean newEntry*/) {
        List<org.greenpole.entity.model.Address> pAddyList;
        if (holdModel.getPostalAddresses() != null)
            pAddyList = holdModel.getPostalAddresses();
        else
            pAddyList = new ArrayList<>();
        
        
        List<org.greenpole.hibernate.entity.HolderPostalAddress> returnHolderPostalAddress = new ArrayList<>();

        for (org.greenpole.entity.model.Address pAddy : pAddyList) {
            org.greenpole.hibernate.entity.HolderPostalAddress postalAddressEntity = new org.greenpole.hibernate.entity.HolderPostalAddress();
            
            if (pAddy.getId() > 0) {
                postalAddressEntity.setId(pAddy.getId());
                postalAddressEntity = hq.getHolderPostalAddress(pAddy.getId());
            }
            postalAddressEntity.setAddressLine1(pAddy.getAddressLine1());
            postalAddressEntity.setState(pAddy.getState());
            postalAddressEntity.setCountry(pAddy.getCountry());
            postalAddressEntity.setAddressLine2(pAddy.getAddressLine2());
            postalAddressEntity.setAddressLine3(pAddy.getAddressLine3());
            postalAddressEntity.setCity(pAddy.getCity());
            postalAddressEntity.setPostCode(pAddy.getPostCode());
            postalAddressEntity.setIsPrimary(pAddy.isPrimaryAddress());
            
            returnHolderPostalAddress.add(postalAddressEntity);
        }
        return returnHolderPostalAddress;
    }
    
    /**
     * Unwraps the holder postal address details from the HolderModel into
     * HolderPostalAddress hibernate entity
     *
     * @param holdModel the holderModel of holder details
     * @param newEntry boolean value indicating new entry
     * @return List object of HolderPostalAddress hibernate entity
     */
    private List<HolderPostalAddress> retrieveHolderPostalAddressForDeletion(Holder holdModel/*, boolean newEntry*/) {
        List<org.greenpole.entity.model.Address> pAddyList;
        if (holdModel.getDeletedPostalAddresses() != null)
            pAddyList = holdModel.getDeletedPostalAddresses();
        else
            pAddyList = new ArrayList<>();
        
        
        List<org.greenpole.hibernate.entity.HolderPostalAddress> returnHolderPostalAddress = new ArrayList<>();

        for (org.greenpole.entity.model.Address pAddy : pAddyList) {
            org.greenpole.hibernate.entity.HolderPostalAddress postalAddressEntity;// = new org.greenpole.hibernate.entity.HolderPostalAddress();
            
            postalAddressEntity = hq.getHolderPostalAddress(pAddy.getId());
            /*postalAddressEntity.setId(pAddy.getId());
            postalAddressEntity.setAddressLine1(pAddy.getAddressLine1());
            postalAddressEntity.setState(pAddy.getState());
            postalAddressEntity.setCountry(pAddy.getCountry());
            postalAddressEntity.setAddressLine2(pAddy.getAddressLine2());
            postalAddressEntity.setAddressLine3(pAddy.getAddressLine3());
            postalAddressEntity.setCity(pAddy.getCity());
            postalAddressEntity.setPostCode(pAddy.getPostCode());
            postalAddressEntity.setIsPrimary(pAddy.isPrimaryAddress());*/
            
            returnHolderPostalAddress.add(postalAddressEntity);
        }
        return returnHolderPostalAddress;
    }

    /**
     * Unwraps holder phone number details from the holder model passed as
     * parameter into HolderPhoneNumber hibernate entity
     * @param holdModel object of holder details
     * @param newEntry boolean variable indicating whether or not the entry is
     * new
     * @return List of HolderPhoneNumber objects retrieveHolderEmailAddress
     */
    private List<HolderPhoneNumber> retrieveHolderPhoneNumber(Holder holdModel/*, boolean newEntry*/) {
        List<org.greenpole.entity.model.PhoneNumber> phoneNumberList;
        if (holdModel.getPhoneNumbers() != null)
            phoneNumberList = holdModel.getPhoneNumbers();
        else
            phoneNumberList = new ArrayList<>();
        
        List<org.greenpole.hibernate.entity.HolderPhoneNumber> returnPhoneNumber = new ArrayList<>();

        for (PhoneNumber phone : phoneNumberList) {
            org.greenpole.hibernate.entity.HolderPhoneNumber phoneNumberEntity = new org.greenpole.hibernate.entity.HolderPhoneNumber();
            
            if (phone.getId() > 0) {
                //phoneNumberEntity.setId(phone.getId());
                phoneNumberEntity = hq.getHolderPhoneNumber(phone.getId());
            }
            phoneNumberEntity.setPhoneNumber(phone.getPhoneNumber());
            phoneNumberEntity.setIsPrimary(phone.isPrimaryPhoneNumber());
            
            returnPhoneNumber.add(phoneNumberEntity);
        }
        return returnPhoneNumber;
    }
    
    /**
     * Unwraps holder phone number details from the holder model passed as
     * parameter into HolderPhoneNumber hibernate entity
     * @param holdModel object of holder details
     * @param newEntry boolean variable indicating whether or not the entry is
     * new
     * @return List of HolderPhoneNumber objects retrieveHolderEmailAddress
     */
    private List<HolderPhoneNumber> retrieveHolderPhoneNumberForDeletion(Holder holdModel/*, boolean newEntry*/) {
        List<org.greenpole.entity.model.PhoneNumber> phoneNumberList;
        if (holdModel.getDeletedPhoneNumbers() != null)
            phoneNumberList = holdModel.getDeletedPhoneNumbers();
        else
            phoneNumberList = new ArrayList<>();
        
        List<org.greenpole.hibernate.entity.HolderPhoneNumber> returnPhoneNumber = new ArrayList<>();

        for (PhoneNumber phone : phoneNumberList) {
            org.greenpole.hibernate.entity.HolderPhoneNumber phoneNumberEntity;// = new org.greenpole.hibernate.entity.HolderPhoneNumber();
            
            phoneNumberEntity = hq.getHolderPhoneNumber(phone.getId());
            /*phoneNumberEntity.setId(phone.getId());
            phoneNumberEntity.setPhoneNumber(phone.getPhoneNumber());
            phoneNumberEntity.setIsPrimary(phone.isPrimaryPhoneNumber());*/
            
            returnPhoneNumber.add(phoneNumberEntity);
        }
        return returnPhoneNumber;
    }

    /**
     * Unwraps Holder email address from the holder model into
     * HolderEmailAddress hibernate entity object
     * @param holdModel object to holder model
     * @param newEntry boolean variable indicating whether or not entry is new
     * @return List of HolderEmailAddress hibernate entity objects
     */
    private List<HolderEmailAddress> retrieveHolderEmailAddress(Holder holdModel/*, boolean newEntry*/) {
        List<org.greenpole.entity.model.EmailAddress> emailAddressList;
        if (holdModel.getEmailAddresses() != null)
            emailAddressList = holdModel.getEmailAddresses();
        else
            emailAddressList = new ArrayList<>();
        
        List<org.greenpole.hibernate.entity.HolderEmailAddress> returnEmailAddress = new ArrayList<>();

        for (EmailAddress email : emailAddressList) {
            org.greenpole.hibernate.entity.HolderEmailAddress emailAddressEntity = new org.greenpole.hibernate.entity.HolderEmailAddress();
            
            if (email.getId() > 0) {
                //emailAddressEntity.setId(email.getId());
                emailAddressEntity = hq.getHolderEmailAddress(email.getId());
            }
            emailAddressEntity.setEmailAddress(email.getEmailAddress());
            emailAddressEntity.setIsPrimary(email.isPrimaryEmail());
            
            returnEmailAddress.add(emailAddressEntity);
        }
        return returnEmailAddress;
    }
    
    /**
     * Unwraps Holder email address from the holder model into
     * HolderEmailAddress hibernate entity object
     * @param holdModel object to holder model
     * @param newEntry boolean variable indicating whether or not entry is new
     * @return List of HolderEmailAddress hibernate entity objects
     */
    private List<HolderEmailAddress> retrieveHolderEmailAddressForDeletion(Holder holdModel/*, boolean newEntry*/) {
        List<org.greenpole.entity.model.EmailAddress> emailAddressList;
        if (holdModel.getDeletedEmailAddresses() != null)
            emailAddressList = holdModel.getDeletedEmailAddresses();
        else
            emailAddressList = new ArrayList<>();
        
        List<org.greenpole.hibernate.entity.HolderEmailAddress> returnEmailAddress = new ArrayList<>();

        for (EmailAddress email : emailAddressList) {
            org.greenpole.hibernate.entity.HolderEmailAddress emailAddressEntity;// = new org.greenpole.hibernate.entity.HolderEmailAddress();
            
            emailAddressEntity = hq.getHolderEmailAddress(email.getId());
            /*emailAddressEntity.setId(email.getId());
            emailAddressEntity.setEmailAddress(email.getEmailAddress());
            emailAddressEntity.setIsPrimary(email.isPrimaryEmail());*/
            
            returnEmailAddress.add(emailAddressEntity);
        }
        return returnEmailAddress;
    }

    /**
     * Unwraps Holder residential address from the holder model into
     * HolderResidentialAddress hibernate entity object.
     * @param holdModel object of holder model
     * @param newEntry boolean variable indicating whether or not entry is new
     * @return List of HolderResidentialAddress hibernate entity objects
     */
    private List<HolderResidentialAddress> retrieveHolderResidentialAddress(Holder holdModel/*, boolean newEntry*/) {
        List<org.greenpole.entity.model.Address> residentialAddressList;
        if (holdModel.getResidentialAddresses() != null)
            residentialAddressList = holdModel.getResidentialAddresses();
        else
            residentialAddressList = new ArrayList<>();
        
        List<org.greenpole.hibernate.entity.HolderResidentialAddress> returnResidentialAddress = new ArrayList();

        for (org.greenpole.entity.model.Address rAddy : residentialAddressList) {
            org.greenpole.hibernate.entity.HolderResidentialAddress residentialAddressEntity = new org.greenpole.hibernate.entity.HolderResidentialAddress();
            
            if (rAddy.getId() > 0) {
                //residentialAddressEntity.setId(rAddy.getId());
                residentialAddressEntity = hq.getHolderResidentialAddress(rAddy.getId());
            }
            residentialAddressEntity.setAddressLine1(rAddy.getAddressLine1());
            residentialAddressEntity.setState(rAddy.getState());
            residentialAddressEntity.setCountry(rAddy.getCountry());
            residentialAddressEntity.setAddressLine2(rAddy.getAddressLine2());
            residentialAddressEntity.setAddressLine3(rAddy.getAddressLine3());
            residentialAddressEntity.setAddressLine4(rAddy.getAddressLine4());
            residentialAddressEntity.setCity(rAddy.getCity());
            residentialAddressEntity.setPostCode(rAddy.getPostCode());
            residentialAddressEntity.setIsPrimary(rAddy.isPrimaryAddress());

            returnResidentialAddress.add(residentialAddressEntity);
        }
        return returnResidentialAddress;
    }
    
    /**
     * Unwraps Holder residential address from the holder model into
     * HolderResidentialAddress hibernate entity object.
     * @param holdModel object of holder model
     * @param newEntry boolean variable indicating whether or not entry is new
     * @return List of HolderResidentialAddress hibernate entity objects
     */
    private List<HolderResidentialAddress> retrieveHolderResidentialAddressForDeletion(Holder holdModel/*, boolean newEntry*/) {
        List<org.greenpole.entity.model.Address> residentialAddressList;
        if (holdModel.getDeletedResidentialAddresses() != null)
            residentialAddressList = holdModel.getDeletedResidentialAddresses();
        else
            residentialAddressList = new ArrayList<>();
        
        List<org.greenpole.hibernate.entity.HolderResidentialAddress> returnResidentialAddress = new ArrayList();
        
        for (org.greenpole.entity.model.Address rAddy : residentialAddressList) {
            org.greenpole.hibernate.entity.HolderResidentialAddress residentialAddressEntity;// = new org.greenpole.hibernate.entity.HolderResidentialAddress();
            
            residentialAddressEntity = hq.getHolderResidentialAddress(rAddy.getId());
            /*residentialAddressEntity.setId(rAddy.getId());
            residentialAddressEntity.setAddressLine1(rAddy.getAddressLine1());
            residentialAddressEntity.setState(rAddy.getState());
            residentialAddressEntity.setCountry(rAddy.getCountry());
            residentialAddressEntity.setAddressLine2(rAddy.getAddressLine2());
            residentialAddressEntity.setAddressLine3(rAddy.getAddressLine3());
            residentialAddressEntity.setAddressLine4(rAddy.getAddressLine4());
            residentialAddressEntity.setCity(rAddy.getCity());
            residentialAddressEntity.setPostCode(rAddy.getPostCode());
            residentialAddressEntity.setIsPrimary(rAddy.isPrimaryAddress());*/
            
            returnResidentialAddress.add(residentialAddressEntity);
        }
        return returnResidentialAddress;
    }

    /**
     * Unwraps holder company account details from the holder model.
     * @param holdModel object of the holder model
     * @param newEntry boolean variable indicating whether or not entry is new
     * @return object of HolderCompanyAccount
     */
    private org.greenpole.hibernate.entity.HolderCompanyAccount retrieveHolderCompanyAccount(Holder holdModel/*, boolean newEntry*/) {
        org.greenpole.hibernate.entity.HolderCompanyAccount companyAccountEntity = new org.greenpole.hibernate.entity.HolderCompanyAccount();
        org.greenpole.entity.model.holder.HolderCompanyAccount compAcct = holdModel.getCompanyAccounts().get(0);
        HolderCompanyAccountId compAcctId = new HolderCompanyAccountId();
        
        compAcctId.setClientCompanyId(compAcct.getClientCompanyId());
        
        companyAccountEntity.setId(compAcctId);
        companyAccountEntity.setEsop(compAcct.isEsop());
        companyAccountEntity.setHolderCompAccPrimary(true);
        companyAccountEntity.setMerged(false);
        
        return companyAccountEntity;
    }
    
    /**
     * Unwraps Holder bond account details from holder model.
     * @param holdModel object of holder model
     * @param newEntry boolean variable indicating whether or not entry is new
     * @return HolderBondAccount object
     */
    private org.greenpole.hibernate.entity.HolderBondAccount retrieveHolderBondAccount(Holder holdModel/*, boolean newEntry*/) {
        org.greenpole.hibernate.entity.HolderBondAccount bondAccountEntity = new org.greenpole.hibernate.entity.HolderBondAccount();
        org.greenpole.entity.model.holder.HolderBondAccount bondAcct = holdModel.getBondAccounts().get(0);
        HolderBondAccountId bondAcctId = new HolderBondAccountId();
        
        bondAcctId.setBondOfferId(bondAcct.getBondOfferId());
        
        bondAccountEntity.setId(bondAcctId);
        bondAccountEntity.setStartingPrincipalValue(bondAcct.getStartingPrincipalValue());
        bondAccountEntity.setRemainingPrincipalValue(0.00);
        bondAccountEntity.setHolderBondAcctPrimary(true);
        bondAccountEntity.setMerged(false);
        
        return bondAccountEntity;
    }
}
