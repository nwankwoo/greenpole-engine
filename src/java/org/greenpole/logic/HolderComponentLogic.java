/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.logic;

import com.sun.javafx.scene.control.skin.VirtualFlow;
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
import java.util.Set;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.model.Address;
import org.greenpole.entity.model.AddressTag;
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
import org.greenpole.hibernate.entity.AccountConsolidation;
import org.greenpole.hibernate.entity.AdministratorEmailAddress;
import org.greenpole.hibernate.entity.AdministratorEmailAddressId;
import org.greenpole.hibernate.entity.AdministratorPhoneNumber;
import org.greenpole.hibernate.entity.AdministratorPhoneNumberId;
import org.greenpole.hibernate.entity.AdministratorResidentialAddress;
import org.greenpole.hibernate.entity.AdministratorResidentialAddressId;
import org.greenpole.hibernate.entity.Bank;
import org.greenpole.hibernate.entity.BondOffer;
import org.greenpole.hibernate.entity.ClientCompany;
import org.greenpole.hibernate.entity.CompanyAccountConsolidation;
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
import org.greenpole.hibernate.entity.HolderType;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.HolderComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.BytesConverter;
import org.greenpole.util.Descriptor;
import org.greenpole.util.GreenpoleFile;
import org.greenpole.util.Manipulator;
import org.greenpole.util.Notification;
import org.greenpole.util.properties.GreenpoleProperties;
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
        
        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;
            
            org.greenpole.hibernate.entity.Holder h_hib = new org.greenpole.hibernate.entity.Holder();
            List<org.greenpole.hibernate.entity.Holder> h_hib_list = new ArrayList<>();
            
            boolean pryCheckPassed = false;
            boolean secCheckPassed = false;
            
            //check primary account
            if (accountsToMerge.getPrimaryHolder().isPryHolder() && accountsToMerge.getPrimaryHolder().getChn() != null 
                    && !"".equals(accountsToMerge.getPrimaryHolder().getChn())) {
                h_hib = hq.getHolder(accountsToMerge.getPrimaryHolder().getHolderId());
                if (!h_hib.isMerged())
                    pryCheckPassed = true;
            }
            
            //check secondary accounts
            for (Holder h : accountsToMerge.getSecondaryHolders()) {
                if (h.isPryHolder() && h.getChn() != null && !"".equals(h.getChn())) {
                    org.greenpole.hibernate.entity.Holder h_entity = hq.getHolder(h.getHolderId());
                    if (!h_entity.isMerged()) {
                        h_hib_list.add(h_entity);
                        secCheckPassed = true;
                    }
                } else {
                    secCheckPassed = false;
                    break;
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
                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription(descMsg);
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
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
                    errorMsg += "\nThe chosen secondary holder account (or one of them) has already been merged into another account.";
                    logger.info("The chosen secondary holder account (or one of them) has already been merged into another account - [{}]", login.getUserId());
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
        logger.info("authorise holder accounts merge, invoked by [{}] - notification code: [{}]", login.getUserId(), notificationCode);
        
        try {
            //get Holder Merger model from wrapper
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<HolderMerger> merger_list = (List<HolderMerger>) wrapper.getModel();
            HolderMerger merger = merger_list.get(0);
            
            boolean pryHolderExists = hq.checkHolderAccount(merger.getPrimaryHolder().getHolderId());
            
            if (pryHolderExists) {
                //get the primary holder first, and company / bond accounts
                org.greenpole.hibernate.entity.Holder pryHolder = hq.getHolder(merger.getPrimaryHolder().getHolderId());
                logger.info("retrieved primary holder information from hibernate - [{}]", login.getUserId());

                if (pryHolder.isPryHolder() && !"".equals(pryHolder.getChn()) && pryHolder.getChn() != null) {
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
                            
                            if (secHolder.isPryHolder() && !"".equals(secHolder.getChn()) && secHolder.getChn() != null) {
                                logger.info("selected secondary passes set rules. Adding to special secondary holders list - [{}]", login.getUserId());
                                secHolders.add(secHolder);//add secondary holder to list

                                List<org.greenpole.hibernate.entity.HolderCompanyAccount> secCompAccts = new ArrayList<>();//the secondary holder's company accounts
                                List<org.greenpole.hibernate.entity.HolderBondAccount> secBondAccts = new ArrayList<>();//the secondary holder's bond accounts

                                boolean secHasCompAccts = hq.hasCompanyAccount(sec_h_model.getHolderId());//if secondary holder has company accounts
                                boolean secHasBondAccts = hq.hasBondAccount(sec_h_model.getHolderId());//if secondary holder has bond accounts

                                if (secHasCompAccts) {
                                    secCompAccts = hq.getAllHolderCompanyAccounts(sec_h_model.getHolderId());
                                    logger.info("selected secondary holder has company accounts - [{}]", login.getUserId());
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
                                    if (sec_hba.getRemainingPrincipalValue() > 0) {
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
                            }
                            resp.setRetn(301);
                            resp.setDesc("Merge unsuccessful. The secondary Holder - " + secHolder.getFirstName() + " " + secHolder.getLastName()
                                    + " - account has already been merged, or is missing its CHN."
                                    + "\nContact Administrator.");
                            logger.info("Merge unsuccessful. The secondary Holder - [{}]  - account has already been merged, or is missing its CHN  - [{}]",
                                    secHolder.getFirstName() + " " + secHolder.getLastName(), login.getUserId());
                            return resp;
                        }
                        resp.setRetn(301);
                        resp.setDesc("Merge unsuccessful. The secondary Holder, id - " + sec_h_model.getHolderId()
                                + " - has already been merged, or is missing its CHN."
                                + "\nContact Administrator.");
                        logger.info("Merge unsuccessful. The secondary Holder, id - [{}]  - has already been merged, or is missing its CHN  - [{}]",
                                    sec_h_model.getHolderId(), login.getUserId());
                        return resp;
                    }
                    boolean merged;
                    if (!similarToActive) {
                        merged = hq.mergeHolderAccounts(pryHolder, secHolders, secHolderCompAccts, secHolderBondAccts);
                    } else {
                        resp.setRetn(301);
                        resp.setDesc("Merge unsuccessful, because one of the secondary holders has an active bond account "
                                + "similar to a bond account in the primary holder");
                        logger.info("Merge unsuccessful, because one of the secondary holders has an active bond account "
                                + "similar to a bond account in the primary holder - [{}]", login.getUserId());
                        return resp;
                    }

                    if (merged) {
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
        
        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;
            
            org.greenpole.hibernate.entity.Holder h_hib = new org.greenpole.hibernate.entity.Holder();
            
            boolean pryCheckPassed = false;
            
            //check primary account
            if (accountsToDemerge.getPrimaryHolder().isPryHolder() && accountsToDemerge.getPrimaryHolder().isMerged()
                    && accountsToDemerge.getPrimaryHolder().getChn() != null && "".equals(accountsToDemerge.getPrimaryHolder().getChn())) {
                pryCheckPassed = true;
                h_hib = hq.getHolder(accountsToDemerge.getPrimaryHolder().getHolderId());
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
                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription("Demerge accounts from " + pryName);
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
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
        logger.info("authorise holder accounts merge, invoked by [{}] - notification code: [{}]", login.getUserId(), notificationCode);

        try {
            //get Holder Merger model from wrapper
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<HolderMerger> merger_list = (List<HolderMerger>) wrapper.getModel();
            HolderMerger demerger = merger_list.get(0);
            
            Map<org.greenpole.hibernate.entity.Holder, List<CompanyAccountConsolidation>> secondaryMergeInfo = new HashMap<>();

            boolean pryExists = hq.checkHolderAccount(demerger.getPrimaryHolder().getHolderId());
            if (pryExists) {
                logger.info("primary holder exists - [{}]", login.getUserId());
                org.greenpole.hibernate.entity.Holder pryHolder = hq.getHolder(demerger.getPrimaryHolder().getHolderId());
                
                if (pryHolder.isPryHolder() && pryHolder.isMerged() && !"".equals(pryHolder.getChn()) && pryHolder.getChn() != null) {
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
                                if (cac.isTransfer()) {//company account merge was transfer as a result of similar company acct in pry & sec holder
                                    int finalUnit = hq.getFinalUnitAfterTransfer(cac.getTiedToCurrentHolderId(), cac.getForCompanyId());
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
                            }
                            secondaryMergeInfo.put(secHolder, compAcctRecords);//if everything checks out, add secondary holder and corresponding list into map
                        }
                        
                        boolean demerge = hq.demergeHolderAccounts(pryHolder, secondaryMergeInfo);
                        if (demerge) {
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
                    
                    if (senderHolder.isPryHolder()) {//check if sender is primary
                        logger.info("sender holder is a primary account - [{}]", login.getUserId());

                        if (receiverHolder.isPryHolder()) {//check if receiver is primary
                            logger.info("receiver holder is a primary account - [{}]", login.getUserId());
                            boolean senderHolderChnExists = !"".equals(senderHolder.getChn()) && senderHolder.getChn() != null;
                            
                            if (senderHolderChnExists) {//check if sender has chn in their accounts
                                logger.info("sender holder has CHN - [{}]", login.getUserId());
                                boolean senderHolderCompAcctExists = hq.checkHolderCompanyAccount(unitTransfer.getHolderIdFrom(), unitTransfer.getClientCompanyId());
                                org.greenpole.hibernate.entity.HolderCompanyAccount senderCompAcct = hq.getHolderCompanyAccount(unitTransfer.getHolderIdFrom(), unitTransfer.getClientCompanyId());

                                if (senderHolderCompAcctExists) {//check if sender account has chn
                                    logger.info("sender holder has company account - [{}]", login.getUserId());
                                    
                                    if (unitTransfer.getHolderIdFrom() != unitTransfer.getHolderIdTo()) {//cannot transfer between same accounts
                                        
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
                                            wrapper.setCode(Notification.createCode(login));
                                            wrapper.setDescription("Authenticate unit transfer between " + senderName + " and " + receiverName);
                                            wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
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
        logger.info("authorise share unit transfer, invoked by [{}] - notification code: [{}]", login.getUserId(), notificationCode);
        try {
            //get client company model from wrapper
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
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

                    if (senderHolder.isPryHolder()) {//check if sender is a primary account
                        logger.info("sender holder is a primary account - [{}]", login.getUserId());

                        if (receiverHolder.isPryHolder()) {//check if receiver is a primary account
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
                                        return invokeTransfer(senderCompAcct, unitTransfer, receiverCompAcct, resp, senderName, receiverName, login);
                                    }
                                    //no company account? attempt to create one for them if they have a chn
                                    if (receiverHolderChnExists) {
                                        logger.info("receiver holder has no company account, but has CHN. "
                                                + "Company account will be created for receiver holder - [{}]", login.getUserId());
                                        org.greenpole.hibernate.entity.HolderCompanyAccount receiverCompAcct = new org.greenpole.hibernate.entity.HolderCompanyAccount();
                                        HolderCompanyAccountId receiverCompAcctId = new HolderCompanyAccountId(unitTransfer.getHolderIdTo(), unitTransfer.getClientCompanyId());
                                        receiverCompAcct.setId(receiverCompAcctId);
                                        receiverCompAcct.setEsop(false);
                                        receiverCompAcct.setHolderCompAccPrimary(true);
                                        receiverCompAcct.setMerged(false);
                                        hq.createUpdateHolderCompanyAccount(receiverCompAcct);
                                        logger.info("receiver holder now has company account - [{}]", login.getUserId());
                                        //proceed with transfer
                                        return invokeTransfer(senderCompAcct, unitTransfer, receiverCompAcct, resp, senderName, receiverName, login);
                                    }
                                    //no chn in main account? create certificate
                                    //METHOD TO CREATE CERTIFICATE HERE!!!
                                    boolean certCreated = true;
                                    if (certCreated) {
                                        //QUERY CERTIFICATE FOR NUMBER
                                        String certNumber = "temp";
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
                    
                    if (senderHolder.isPryHolder()) {
                        logger.info("sender holder is a primary account - [{}]", login.getUserId());
                        
                        if (receiverHolder.isPryHolder()) {
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
                                            wrapper.setCode(Notification.createCode(login));
                                            wrapper.setDescription("Authenticate unit transfer between " + senderName + " and " + receiverName);
                                            wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
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
                                                        + " - has no active account with the company. One will be created for them upon authorisation.");
                                                logger.info("Holder - [{}] - has no active account with the company. "
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
                                    resp.setDesc("Both holders are the same. Cannnot transfer units between the same holder.");
                                    logger.info("Both holders are the same. Cannnot transfer units between the same holder - [{}]",
                                            login.getUserId());
                                    return resp;
                                }
                                resp.setRetn(306);
                                resp.setDesc("The holder - " + senderName + " - has no share company account to send any units from.");
                                logger.info("The holder - [{}] - has no share company account to send any units from - [{}]",
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
        logger.info("authorise bond unit transfer, invoked by [{}] - notification code: [{}]", login.getUserId(), notificationCode);
        try {
            //get client company model from wrapper
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
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
                    
                    if (senderHolder.isPryHolder()) {//check if sender is primary account
                        logger.info("sender holder is a primary account - [{}]", login.getUserId());
                        
                        if (receiverHolder.isPryHolder()) {//check if receiver is primary account
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
                                        return invokeTransfer(senderBondAcct, unitTransfer, receiverBondAcct, resp, senderName, receiverName, login);
                                    }
                                    //no bond account? attempt to create one for them
                                    logger.info("receiver holder has no bond account, but has CHN. "
                                                + "Bond account will be created for receiver holder - [{}]", login.getUserId());
                                    org.greenpole.hibernate.entity.HolderBondAccount receiverBondAcct = new org.greenpole.hibernate.entity.HolderBondAccount();
                                    HolderBondAccountId receiverBondAcctId = new HolderBondAccountId(unitTransfer.getHolderIdTo(), unitTransfer.getBondOfferId());
                                    receiverBondAcct.setId(receiverBondAcctId);
                                    receiverBondAcct.setMerged(false);
                                    receiverBondAcct.setHolderBondAccPrimary(true);
                                    hq.createUpdateHolderBondAccount(receiverBondAcct);
                                    //proceed with transfer
                                    return invokeTransfer(senderBondAcct, unitTransfer, receiverBondAcct, resp, senderName, receiverName, login);
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
    private Response invokeTransfer(org.greenpole.hibernate.entity.HolderCompanyAccount senderCompAcct, UnitTransfer unitTransfer, org.greenpole.hibernate.entity.HolderCompanyAccount receiverCompAcct, Response resp, String senderName, String receiverName, Login login) {
        if (unitTransfer.getHolderIdFrom() != unitTransfer.getHolderIdTo()) { //check if holder and sender are not the same
            if (senderCompAcct.getShareUnits() >= unitTransfer.getUnits()) { //check if sender has sufficient units to transact
                boolean transfered = hq.transferShareUnits(senderCompAcct, receiverCompAcct, unitTransfer.getUnits());
                if (transfered) {
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
    private Response invokeTransfer(org.greenpole.hibernate.entity.HolderBondAccount senderBondAcct, UnitTransfer unitTransfer, org.greenpole.hibernate.entity.HolderBondAccount receiverBondAcct, Response resp, String senderName, String receiverName, Login login) {
        if (unitTransfer.getHolderIdFrom() != unitTransfer.getHolderIdTo()) { //check if holder and sender are not the same
            if (senderBondAcct.getBondUnits() >= unitTransfer.getUnits()) { //check if sender has sufficient units to transact
                boolean transfered = hq.transferBondUnits(senderBondAcct, receiverBondAcct, unitTransfer.getUnits(), unitTransfer.getUnitPrice());
                if (transfered) {
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
        logger.info("request to query holder changes, invoked by [{}]", login.getUserId());
        
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());

            Map<String, String> descriptors = Descriptor.decipherDescriptor(queryParams.getDescriptor());
            org.greenpole.hibernate.entity.HolderChanges changes_hib = new org.greenpole.hibernate.entity.HolderChanges();
            HolderChangeType change_type_hib = new HolderChangeType();

            if (descriptors.size() == 1) {
                //check that start date is properly formatted
                if (!descriptors.get("date").equalsIgnoreCase("none")) {
                    try {
                        formatter.parse(queryParams.getStart_date());
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
                        formatter.parse(queryParams.getStart_date());
                    } catch (ParseException ex) {
                        logger.info("an error was thrown while checking the start date. See error log - [{}]", login.getUserId());
                        resp.setRetn(308);
                        resp.setDesc("Incorrect date format for start date");
                        logger.error("Incorrect date format for start date - [" + login.getUserId() + "]", ex);

                        return resp;
                    }
                    try {
                        formatter.parse(queryParams.getEnd_date());
                    } catch (ParseException ex) {
                        logger.info("an error was thrown while checking the end date. See error log - [{}]", login.getUserId());
                        resp.setRetn(308);
                        resp.setDesc("Incorrect date format for end date");
                        logger.error("Incorrect date format for end date - [" + login.getUserId() + "]", ex);

                        return resp;
                    }
                }

                change_type_hib.setId(queryParams.getHolderChanges().getChangeTypeId());
                changes_hib.setHolderChangeType(change_type_hib);

                List<org.greenpole.hibernate.entity.HolderChanges> changes_hib_result = hq.queryHolderChanges(queryParams.getDescriptor(), changes_hib, queryParams.getStart_date(), queryParams.getEnd_date());
                logger.info("retrieved holder changes result from query. Preparing local model - [{}]", login.getUserId());
                List<Holder> return_list = new ArrayList<>();

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
                    List<HolderChanges> change_list = new ArrayList<>();
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
        logger.info("request to query holder, invoked by [{}]", login.getUserId());
        
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());

            Map<String, String> descriptors = Descriptor.decipherDescriptor(queryParams.getDescriptor());

            if (descriptors.size() == 3) {
                String descriptor = queryParams.getDescriptor();

                org.greenpole.hibernate.entity.Holder h_hib_search = new org.greenpole.hibernate.entity.Holder();
                org.greenpole.hibernate.entity.HolderType h_type_hib_search = new org.greenpole.hibernate.entity.HolderType();

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
                    
                    h_type_hib_search.setId(h_model_search.getTypeId());

                    h_hib_search.setFirstName(h_model_search.getFirstName());
                    h_hib_search.setMiddleName(h_model_search.getMiddleName());
                    h_hib_search.setLastName(h_model_search.getLastName());
                    h_hib_search.setGender(h_model_search.getGender());
                    try {
                        h_hib_search.setDob(formatter.parse(h_model_search.getDob()));
                    } catch (ParseException ex) {
                        logger.info("an error was thrown while checking the holder's date of birth. See error log - [{}]", login.getUserId());
                        resp.setRetn(308);
                        resp.setDesc("Incorrect date format for date of birth");
                        logger.error("Incorrect date format for date of birth [" + login.getUserId() + "]", ex);
                    }
                    h_hib_search.setChn(h_model_search.getChn());
                    h_hib_search.setHolderAcctNumber(h_model_search.getHolderAcctNumber());
                    h_hib_search.setTaxExempted(h_model_search.isTaxExempted());
                    h_hib_search.setPryAddress(h_model_search.getPrimaryAddress());
                    h_hib_search.setType(h_type_hib_search);
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
                    hca_hib_search.setHolderCompAccPrimary(true);//always set this
                    hca_hib_search.setMerged(false);//always set this

                    hca_hib_search.setId(hca_id_hib_search);

                    Set hca_hib_set = new HashSet();
                    hca_hib_set.add(hca_hib_search);

                    h_hib_search.setHolderCompanyAccounts(hca_hib_set);
                }

                HolderBondAccount hba_model_search;
                if (!queryParams.getHolder().getHolderBondAccounts().isEmpty() && queryParams.getHolder().getHolderBondAccounts() != null) {
                    hba_model_search = queryParams.getHolder().getHolderBondAccounts().get(0);

                    hba_id_hib_search.setBondOfferId(hba_model_search.getBondOfferId());
                    
                    hba_hib_search.setStartingPrincipalValue(hba_model_search.getStartingPrincipalValue());
                    hba_hib_search.setRemainingPrincipalValue(hba_model_search.getRemainingPrincipalValue());
                    hba_hib_search.setHolderBondAccPrimary(true);//always set this
                    hba_hib_search.setMerged(false);//always set this
                    
                    hba_hib_search.setId(hba_id_hib_search);

                    Set hba_hib_set = new HashSet();
                    hba_hib_set.add(hba_hib_search);

                    h_hib_search.setHolderBondAccounts(hba_hib_set);
                }

                Map<String, Integer> shareUnits_search;
                if (!queryParams.getUnits().isEmpty() && queryParams.getUnits() != null) {
                    shareUnits_search = queryParams.getUnits();
                } else {
                    shareUnits_search = new HashMap<>();
                }

                Map<String, Integer> totalHoldings_search;
                if (!queryParams.getTotalHoldings().isEmpty() && queryParams.getTotalHoldings() != null) {
                    totalHoldings_search = queryParams.getTotalHoldings();
                } else {
                    totalHoldings_search = new HashMap<>();
                }

                List<org.greenpole.hibernate.entity.Holder> h_search_result = hq.queryShareholderAccount(descriptor, h_hib_search, shareUnits_search, totalHoldings_search);
                logger.info("retrieved holder result from query. Preparing local model - [{}]", login.getUserId());
                
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
                    h.setPrimaryAddress(h_hib_out.getPryAddress());
                    h.setTypeId(h_hib_out.getType().getId());
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
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;
        
        boolean flag = false;
        String desc = "";
        
        try {
            if (hq.checkHolderAccount(holder.getHolderId())) {

                if (holder.getAdministrators() != null && !holder.getAdministrators().isEmpty()) {
                    
                    for (Administrator admin : holder.getAdministrators()) {
                        if (admin.getResidentialAddress() != null) {
                            Address res = admin.getResidentialAddress();
                            if (res.getAddressLine1() == null || "".equals(res.getAddressLine1())) {
                                desc += "\nResidential address line 1 should not be empty";
                            } else if (res.getState() == null || "".equals(res.getState())) {
                                desc += "\nResidential address state should not be empty";
                            } else if (res.getCountry() == null || "".equals(res.getCountry())) {
                                desc += "\nResidential address country should not be empty";
                            }
                        } else if (admin.getPostalAddress() != null) {
                            Address pos = admin.getPostalAddress();
                            if (pos.getAddressLine1() == null || "".equals(pos.getAddressLine1())) {
                                desc += "\nPostal address line 1 should not be empty";
                            } else if (pos.getState() == null || "".equals(pos.getState())) {
                                desc += "\nPostal address state should not be empty";
                            } else if (pos.getCountry() == null || "".equals(pos.getCountry())) {
                                desc += "\nPostal address line 1 should not be empty";
                            }
                        } else if (admin.getEmailAddress() != null && !admin.getEmailAddress().isEmpty()) {
                            for (EmailAddress email : admin.getEmailAddress()) {
                                if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
                                    desc += "\nEmail address should not be empty";
                                }
                            }
                        } else if (admin.getPhoneNumbers() != null && !admin.getPhoneNumbers().isEmpty()) {
                            for (PhoneNumber phone : admin.getPhoneNumbers()) {
                                if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
                                    desc += "\nPhone number should not be empty";
                                }
                            }
                        } else if (admin.getPrimaryAddress() == null || "".equals(admin.getPrimaryAddress())) {
                            desc += "\nPrimary address must be set";
                        } else if (admin.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString()) || 
                                admin.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
                            desc += "\nPrimary address can only be residential or postal";
                        } else if (admin.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString()) && 
                                admin.getResidentialAddress() == null) {
                            desc += "\nResidential address cannot be empty, as it is the primary address";
                        } else if (admin.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString()) && 
                                admin.getPostalAddress() == null) {
                            desc += "\nPostal address cannot be empty, as it is the primary address";
                        } else {
                            flag = true;
                        }
                    }
                    
                    if (flag) {
                        wrapper = new NotificationWrapper();
                        prop = new NotifierProperties(HolderComponentLogic.class);
                        qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                                prop.getAuthoriserNotifierQueueName());

                        List<Holder> holderList = new ArrayList();
                        holderList.add(holder);

                        wrapper.setCode(Notification.createCode(login));
                        wrapper.setDescription("Authenticate creation of administrator(s) for holder " + holder.getFirstName() + " " + holder.getLastName());
                        wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
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
        
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<Holder> holderList = (List<Holder>) wrapper.getModel();
            Holder holderModel = holderList.get(0);
            
            boolean flag = false;
            String desc = "";
            
            if (hq.checkHolderAccount(holderModel.getHolderId())) {

                if (holderModel.getAdministrators() != null && !holderModel.getAdministrators().isEmpty()) {
                    
                    for (Administrator admin : holderModel.getAdministrators()) {
                        if (admin.getResidentialAddress() != null) {
                            Address res = admin.getResidentialAddress();
                            if (res.getAddressLine1() == null || "".equals(res.getAddressLine1())) {
                                desc += "\nResidential address line 1 should not be empty";
                            } else if (res.getState() == null || "".equals(res.getState())) {
                                desc += "\nResidential address state should not be empty";
                            } else if (res.getCountry() == null || "".equals(res.getCountry())) {
                                desc += "\nResidential address country should not be empty";
                            }
                        } else if (admin.getPostalAddress() != null) {
                            Address pos = admin.getPostalAddress();
                            if (pos.getAddressLine1() == null || "".equals(pos.getAddressLine1())) {
                                desc += "\nPostal address line 1 should not be empty";
                            } else if (pos.getState() == null || "".equals(pos.getState())) {
                                desc += "\nPostal address state should not be empty";
                            } else if (pos.getCountry() == null || "".equals(pos.getCountry())) {
                                desc += "\nPostal address line 1 should not be empty";
                            }
                        } else if (admin.getEmailAddress() != null && !admin.getEmailAddress().isEmpty()) {
                            for (EmailAddress email : admin.getEmailAddress()) {
                                if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
                                    desc += "\nEmail address should not be empty";
                                }
                            }
                        } else if (admin.getPhoneNumbers() != null && !admin.getPhoneNumbers().isEmpty()) {
                            for (PhoneNumber phone : admin.getPhoneNumbers()) {
                                if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
                                    desc += "\nPhone number should not be empty";
                                }
                            }
                        } else if (admin.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString()) && 
                                admin.getResidentialAddress() == null) {
                            desc += "\nResidential address cannot be empty, as it is the primary address";
                        } else if (admin.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString()) && 
                                admin.getPostalAddress() == null) {
                            desc += "\nPostal address cannot be empty, as it is the primary address";
                        } else {
                            flag = true;
                        }
                    }
             
                    if (flag) {
                        hq.createAdministratorForHolder(createAdministrator(holderModel));

                        resp.setRetn(0);
                        resp.setDesc("Success");
                        logger.info("Administrators were created successfully - [{}]", login.getUserId());
                        return resp;
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
                    long fileSize = BytesConverter.decodeToBytes(poa.getFileContents()).length;

                    if (fileSize <= defaultSize) {
                        wrapper = new NotificationWrapper();
                        prop = new NotifierProperties(HolderComponentLogic.class);
                        qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                                prop.getAuthoriserNotifierQueueName());
                        
                        List<PowerOfAttorney> powerList = new ArrayList();
                        powerList.add(poa);
                        
                        wrapper.setCode(Notification.createCode(login));
                        wrapper.setDescription("Authenticate power of attorney for " + holder.getFirstName() + " " + holder.getLastName());
                        wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
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
        
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
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
                    long fileSize = BytesConverter.decodeToBytes(poaModel.getFileContents()).length;
                    
                    if (fileSize <= defaultSize) {
                        logger.info("Power of attorney met file size requirement - [{}]", login.getUserId());
                        
                        if (file.createFile(BytesConverter.decodeToBytes(poaModel.getFileContents()))) {
                            logger.info("Power of attorney file created and saved - [{}]", login.getUserId());
                            
                            String filepath = file.getFolderPath() + file.getFileName();
                            poa_hib.setTitle(poaModel.getTitle());
                            poa_hib.setType(poaModel.getType());
                            poa_hib.setStartDate(formatter.parse(poaModel.getStartDate()));
                            poa_hib.setEndDate(formatter.parse(poaModel.getEndDate()));
                            poa_hib.setFilePath(filepath);
                            
                            poa_hib.setPowerOfAttorneyPrimary(true);
                            poa_hib.setHolder(holder);
                            
                            boolean uploaded;
                            if (currentPoaExists) {
                                uploaded = hq.uploadPowerOfAttorney(poa_hib, currentPoa);
                            } else {
                                uploaded = hq.uploadPowerOfAttorney(poa_hib, null);
                            }
                            
                            if (uploaded) {
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
                String encodedContents = BytesConverter.encodeToString(read);
                logger.info("Power of attorney file successfully read - [{}]", login.getUserId());
                
                poa_model.setId(poa_hib.getId());
                poa_model.setHolderId(poa_hib.getHolder().getId());
                poa_model.setTitle(poa_hib.getTitle());
                poa_model.setType(poa_hib.getType());
                poa_model.setStartDate(formatter.format(poa_hib.getStartDate()));
                poa_model.setEndDate(formatter.format(poa_hib.getEndDate()));
                poa_model.setPrimaryPowerOfAttorney(poa_hib.isPowerOfAttorneyPrimary());
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
                    String encodedContents = BytesConverter.encodeToString(read);
                    logger.info("Power of attorney file successfully read - [{}]", login.getUserId());

                    poa_model.setId(poa_hib.getId());
                    poa_model.setHolderId(poa_hib.getHolder().getId());
                    poa_model.setTitle(poa_hib.getTitle());
                    poa_model.setType(poa_hib.getType());
                    poa_model.setStartDate(formatter.format(poa_hib.getStartDate()));
                    poa_model.setEndDate(formatter.format(poa_hib.getEndDate()));
                    poa_model.setPrimaryPowerOfAttorney(poa_hib.isPowerOfAttorneyPrimary());
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
        Response resp = new Response();
        logger.info("Store NUBAN account number to holder company account, invoked by - [{}]", login.getUserId());
        
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
                                
                                wrapper.setCode(Notification.createCode(login));
                                wrapper.setDescription("Authenticate storage of NUBAN account number for holder - " + holder.getFirstName() + " " + holder.getLastName());
                                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
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
    public Response addShareholderNubanAccountNumber_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        logger.info("authorise NUBAN account number addition to holder company account, invoked by - [{}]", login.getUserId());
        
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
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
                                Bank bank = new Bank();
                                bank.setId(compAcct.getBank().getId());
                                
                                compAcct_hib.setId(compAcct_hib.getId());
                                compAcct_hib.setNubanAccount(compAcct.getNubanAccount());
                                compAcct_hib.setBank(bank);
                                
                                hq.createUpdateHolderCompanyAccount(compAcct_hib);
                                
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
                                
                                wrapper.setCode(Notification.createCode(login));
                                wrapper.setDescription("Authenticate storage of NUBAN account number for holder - " + holder.getFirstName() + " " + holder.getLastName());
                                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
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
        logger.info("authorise NUBAN account number addition to holder bond account, invoked by - [{}]", login.getUserId());
        
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
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
                                Bank bank = new Bank();
                                bank.setId(bondAcct.getBank().getId());
                                
                                bondAcct_hib.setId(bondAcct_hib.getId());
                                bondAcct_hib.setNubanAccount(bondAcct.getNubanAccount());
                                bondAcct_hib.setBank(bank);
                                
                                hq.createUpdateHolderBondAccount(bondAcct_hib);
                                
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
            }else if (holder.getPrimaryAddress() == null || "".equals(holder.getPrimaryAddress())) {
                desc += "\nPrimary Holder address is not specified";
            } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                    || holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
                desc += "\nPrimary address can only be residential or postal";
            } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                    && (holder.getHolderResidentialAddresses() == null || holder.getHolderResidentialAddresses().isEmpty())) {
                desc += "\nResidential address cannot be empty, as it is the primary address";
            } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())
                    && (holder.getHolderPostalAddresses() == null || holder.getHolderPostalAddresses().isEmpty())) {
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
            }
            
            if (flag && holder.getHolderResidentialAddresses() != null && !holder.getHolderResidentialAddresses().isEmpty()) {
                for (Address addr : holder.getHolderResidentialAddresses()) {
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
            
            if (flag && holder.getHolderPostalAddresses() != null && !holder.getHolderPostalAddresses().isEmpty()) {
                for (Address addr : holder.getHolderPostalAddresses()) {
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
            
            if (flag && holder.getHolderEmailAddresses() != null && !holder.getHolderEmailAddresses().isEmpty()) {
                for (EmailAddress email : holder.getHolderEmailAddresses()) {
                    if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
                        desc += "\nEmail address should not be empty. Delete email entry if you must";
                        flag = false;
                        break;
                    }
                }
            }
            
            if (flag && holder.getHolderPhoneNumbers() != null && !holder.getHolderPhoneNumbers().isEmpty()) {
                for (PhoneNumber phone : holder.getHolderPhoneNumbers()) {
                    if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
                        desc += "\nPhone number should not be empty. Delete phone number entry if you must";
                        flag = false;
                        break;
                    }
                }
            }
            
            if (flag) {
                
                wrapper = new NotificationWrapper();
                prop = new NotifierProperties(HolderComponentLogic.class);
                queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
                
                List<Holder> holdList = new ArrayList<>();
                holdList.add(holder);
                
                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription("Authenticate holder account creation for " + holder.getFirstName() + " " + holder.getLastName());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(holdList);
                logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                resp = queue.sendAuthorisationRequest(wrapper);
                return resp;
            }
            resp.setRetn(319);
            resp.setDesc("Error filing holder details: " + desc);
            logger.info("Error filing holder details: [{}] - [{}]", desc, login.getUserId());
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
        
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
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
            }else if (holder.getPrimaryAddress() == null || "".equals(holder.getPrimaryAddress())) {
                desc += "\nPrimary Holder address is not specified";
            } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                    || holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
                desc += "\nPrimary address can only be residential or postal";
            } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                    && (holder.getHolderResidentialAddresses() == null || holder.getHolderResidentialAddresses().isEmpty())) {
                desc += "\nResidential address cannot be empty, as it is the primary address";
            } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())
                    && (holder.getHolderPostalAddresses() == null || holder.getHolderPostalAddresses().isEmpty())) {
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
            }
            
            if (flag && holder.getHolderResidentialAddresses() != null && !holder.getHolderResidentialAddresses().isEmpty()) {
                for (Address addr : holder.getHolderResidentialAddresses()) {
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
            
            if (flag && holder.getHolderPostalAddresses() != null && !holder.getHolderPostalAddresses().isEmpty()) {
                for (Address addr : holder.getHolderPostalAddresses()) {
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
            
            if (flag && holder.getHolderEmailAddresses() != null && !holder.getHolderEmailAddresses().isEmpty()) {
                for (EmailAddress email : holder.getHolderEmailAddresses()) {
                    if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
                        desc += "\nEmail address should not be empty. Delete email entry if you must";
                        flag = false;
                        break;
                    }
                }
            }
            
            if (flag && holder.getHolderPhoneNumbers() != null && !holder.getHolderPhoneNumbers().isEmpty()) {
                for (PhoneNumber phone : holder.getHolderPhoneNumbers()) {
                    if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
                        desc += "\nPhone number should not be empty. Delete phone number entry if you must";
                        flag = false;
                        break;
                    }
                }
            }
            
            if (flag) {
                org.greenpole.hibernate.entity.Holder holdEntity = new org.greenpole.hibernate.entity.Holder();
                HolderType typeEntity = new HolderType();
                
                typeEntity.setId(holder.getTypeId());
                
                holdEntity.setFirstName(holder.getFirstName());
                holdEntity.setLastName(holder.getLastName());
                holdEntity.setMiddleName(holder.getMiddleName());
                holdEntity.setType(typeEntity);
                holdEntity.setGender(holder.getGender());
                holdEntity.setDob(formatter.parse(holder.getDob()));
                
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
                    resp.setRetn(0);
                    resp.setDesc("Holder details saved: Successful");
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
            }else if (holder.getPrimaryAddress() == null || "".equals(holder.getPrimaryAddress())) {
                desc += "\nPrimary Holder address is not specified";
            } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                    || holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
                desc += "\nPrimary address can only be residential or postal";
            } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                    && (holder.getHolderResidentialAddresses() == null || holder.getHolderResidentialAddresses().isEmpty())) {
                desc += "\nResidential address cannot be empty, as it is the primary address";
            } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())
                    && (holder.getHolderPostalAddresses() == null || holder.getHolderPostalAddresses().isEmpty())) {
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
            }
            
            if (flag && holder.getHolderResidentialAddresses() != null && !holder.getHolderResidentialAddresses().isEmpty()) {
                for (Address addr : holder.getHolderResidentialAddresses()) {
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
            
            if (flag && holder.getHolderPostalAddresses() != null && !holder.getHolderPostalAddresses().isEmpty()) {
                for (Address addr : holder.getHolderPostalAddresses()) {
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
            
            if (flag && holder.getHolderEmailAddresses() != null && !holder.getHolderEmailAddresses().isEmpty()) {
                for (EmailAddress email : holder.getHolderEmailAddresses()) {
                    if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
                        desc += "\nEmail address should not be empty. Delete email entry if you must";
                        flag = false;
                        break;
                    }
                }
            }
            
            if (flag && holder.getHolderPhoneNumbers() != null && !holder.getHolderPhoneNumbers().isEmpty()) {
                for (PhoneNumber phone : holder.getHolderPhoneNumbers()) {
                    if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
                        desc += "\nPhone number should not be empty. Delete phone number entry if you must";
                        flag = false;
                        break;
                    }
                }
            }

            if (flag) {
                wrapper = new NotificationWrapper();
                prop = new NotifierProperties(HolderComponentLogic.class);
                queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());

                List<Holder> holdList = new ArrayList<>();
                holdList.add(holder);

                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription("Authenticate bond holder account, " + holder.getFirstName() + " " + holder.getLastName());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
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
        
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
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
            }else if (holder.getPrimaryAddress() == null || "".equals(holder.getPrimaryAddress())) {
                desc += "\nPrimary Holder address is not specified";
            } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                    || holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
                desc += "\nPrimary address can only be residential or postal";
            } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                    && (holder.getHolderResidentialAddresses() == null || holder.getHolderResidentialAddresses().isEmpty())) {
                desc += "\nResidential address cannot be empty, as it is the primary address";
            } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())
                    && (holder.getHolderPostalAddresses() == null || holder.getHolderPostalAddresses().isEmpty())) {
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
            }
            
            if (flag && holder.getHolderResidentialAddresses() != null && !holder.getHolderResidentialAddresses().isEmpty()) {
                for (Address addr : holder.getHolderResidentialAddresses()) {
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
            
            if (flag && holder.getHolderPostalAddresses() != null && !holder.getHolderPostalAddresses().isEmpty()) {
                for (Address addr : holder.getHolderPostalAddresses()) {
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
            
            if (flag && holder.getHolderEmailAddresses() != null && !holder.getHolderEmailAddresses().isEmpty()) {
                for (EmailAddress email : holder.getHolderEmailAddresses()) {
                    if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
                        desc += "\nEmail address should not be empty. Delete email entry if you must";
                        flag = false;
                        break;
                    }
                }
            }
            
            if (flag && holder.getHolderPhoneNumbers() != null && !holder.getHolderPhoneNumbers().isEmpty()) {
                for (PhoneNumber phone : holder.getHolderPhoneNumbers()) {
                    if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
                        desc += "\nPhone number should not be empty. Delete phone number entry if you must";
                        flag = false;
                        break;
                    }
                }
            }
            
            if (flag) {
                org.greenpole.hibernate.entity.Holder holdEntity = new org.greenpole.hibernate.entity.Holder();
                HolderType typeEntity = new HolderType();
                
                typeEntity.setId(holder.getTypeId());
                
                holdEntity.setFirstName(holder.getFirstName());
                holdEntity.setLastName(holder.getLastName());
                holdEntity.setMiddleName(holder.getMiddleName());
                holdEntity.setType(typeEntity);
                holdEntity.setGender(holder.getGender());
                holdEntity.setDob(formatter.parse(holder.getDob()));
                holdEntity.setChn(holder.getChn());

                boolean created = hq.createHolderAccount(holdEntity, retrieveHolderBondAccount(holder),
                        retrieveHolderResidentialAddress(holder), retrieveHolderPostalAddress(holder),
                        retrieveHolderEmailAddress(holder), retrieveHolderPhoneNumber(holder));

                if (created) {
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
                long fileSize = BytesConverter.decodeToBytes(holderSig.getSignatureContent()).length;
                
                if (fileSize <= defaultSize) {
                    wrapper = new NotificationWrapper();
                    prop = new NotifierProperties(HolderComponentLogic.class);
                    queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
                    
                    List<HolderSignature> holderListSignature = new ArrayList<>();
                    holderListSignature.add(holderSig);
                    
                    wrapper.setCode(Notification.createCode(login));
                    wrapper.setDescription("Authenticate creation of holder signature for holder " + holder.getFirstName() + " " + holder.getLastName());
                    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
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

        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
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
                long fileSize = BytesConverter.decodeToBytes(sigModel.getSignatureContent()).length;

                if (fileSize <= defaultSize) {
                    logger.info("Holder signature met file size requirement - [{}]", login.getUserId());

                    if (file.createFile(BytesConverter.decodeToBytes(sigModel.getSignatureContent()))) {
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
        
        try {
            HolderSignature sigModel = new HolderSignature();
            
            if (hq.checkHolderAccount(queryParams.getHolderId())) {
                org.greenpole.hibernate.entity.Holder holder = hq.getHolder(queryParams.getHolderId());
                logger.info("Holder [{}] checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());

                org.greenpole.hibernate.entity.HolderSignature sig_hib = hq.getCurrentSignature(queryParams.getHolderId());

                File file = new File(sig_hib.getSignaturePath());
                byte[] read = Files.readAllBytes(file.toPath());
                String encodedContents = BytesConverter.encodeToString(read);
                logger.info("Holder signature file successfully read - [{}]", login.getUserId());

                sigModel.setId(sig_hib.getId());
                sigModel.setHolderId(sig_hib.getHolder().getId());
                sigModel.setTitle(sig_hib.getTitle());
                sigModel.setSignaturePath(sig_hib.getSignaturePath());
                sigModel.setSignatureContent(encodedContents);
                sigModel.setPrimarySignature(sig_hib.isHolderSignaturePrimary());

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
        
        NotificationWrapper wrapper;
        QueueSender queue;
        NotifierProperties prop;
        
        String desc = "";
        boolean flag = false;
        
        try {
            
            if (hq.checkHolderAccount(holder.getHolderId())) {
                org.greenpole.hibernate.entity.Holder holder_hib = hq.getHolder(holder.getHolderId());
                logger.info("holder does not exits - [{}] [{}]", holder.getFirstName(), holder.getLastName());
                
                if (!"".equals(holder.getFirstName()) || holder.getFirstName() != null) {
                    desc = "\nThe holder's first name cannot be empty";
                } else if (!"".equals(holder.getLastName()) || holder.getLastName() != null) {
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

                    wrapper.setCode(Notification.createCode(login));
                    wrapper.setDescription("Authenticate transpose request for holder, " + holder_hib.getFirstName() + " " + holder_hib.getLastName());
                    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
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
        
        String desc = "";
        boolean flag = false;

        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<Holder> holdList = (List<Holder>) wrapper.getModel();
            Holder holder = holdList.get(0);
            
            if (hq.checkHolderAccount(holder.getHolderId())) {
                org.greenpole.hibernate.entity.Holder holder_hib = hq.getHolder(holder.getHolderId());
                logger.info("holder does not exits - [{}] [{}]", holder.getFirstName(), holder.getLastName());
                
                if (!"".equals(holder.getFirstName()) || holder.getFirstName() != null) {
                    desc = "\nThe holder's first name cannot be empty";
                } else if (!"".equals(holder.getLastName()) || holder.getLastName() != null) {
                    desc = "\nThe holder's last name cannot be empty";
                } else {
                    flag = true;
                }
                
                if (flag) {
                    holder_hib.setFirstName(holder.getFirstName());
                    holder_hib.setLastName(holder.getLastName());
                    
                    hq.updateHolderAccountForTranspose(holder_hib);
                    
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

        Response resp = new Response();
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
                if (holderEntity.isPryHolder()) {

                    if (holder.getFirstName() == null || "".equals(holder.getFirstName())) {
                        desc = "\nHolder first name should not be empty";
                    } else if (holder.getLastName() == null || "".equals(holder.getLastName())) {
                        desc += "\nHolder last name should not be empty";
                    } else if (holder.getTypeId() <= 0) {
                        desc += "\nHolder type should not be empty";
                    } else if (holder.getPrimaryAddress() == null || "".equals(holder.getPrimaryAddress())) {
                        desc += "\nPrimary Holder address is not specified";
                    } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                            || holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
                        desc += "\nPrimary address can only be residential or postal";
                    } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                            && (holder.getHolderResidentialAddresses() == null || holder.getHolderResidentialAddresses().isEmpty())) {
                        desc += "\nResidential address cannot be empty, as it is the primary address";
                    } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())
                            && (holder.getHolderPostalAddresses() == null || holder.getHolderPostalAddresses().isEmpty())) {
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
                    }

                    if (flag && holder.getHolderResidentialAddresses() != null && !holder.getHolderResidentialAddresses().isEmpty()) {
                        for (Address addr : holder.getHolderResidentialAddresses()) {
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

                    if (flag && holder.getHolderPostalAddresses() != null && !holder.getHolderPostalAddresses().isEmpty()) {
                        for (Address addr : holder.getHolderPostalAddresses()) {
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

                    if (flag && holder.getHolderEmailAddresses() != null && !holder.getHolderEmailAddresses().isEmpty()) {
                        for (EmailAddress email : holder.getHolderEmailAddresses()) {
                            if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
                                desc += "\nEmail address should not be empty. Delete email entry if you must";
                                flag = false;
                                break;
                            }
                        }
                    }

                    if (flag && holder.getHolderPhoneNumbers() != null && !holder.getHolderPhoneNumbers().isEmpty()) {
                        for (PhoneNumber phone : holder.getHolderPhoneNumbers()) {
                            if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
                                desc += "\nPhone number should not be empty. Delete phone number entry if you must";
                                flag = false;
                                break;
                            }
                        }
                    }
   
                    if (flag && (holderEntity.getChn() != null || "".equals(holderEntity.getChn()))
                            && (holder.getChn() == null || "".equals(holder.getChn()))) {
                        desc += "\nCHN cannot be erased";
                        flag = false;
                    }
                    
                    if (flag) {
                        List<Holder> holdList = new ArrayList<>();
                        holdList.add(holder);
                        wrapper.setCode(Notification.createCode(login));
                        wrapper.setDescription("Authenticate edit of holder account, " + holder.getFirstName() + " " + holder.getLastName());
                        wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
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
     * Processes request authorisation to edit holder details
     *
     * @param login user's login details
     * @param notificationCode the notification code
     * @return response object for the authorized edit holder details request
     */
    public Response editHolderDetails_Authorise(Login login, String notificationCode) {
        logger.info("request authorisation to persist holder details. Invoked by [{}]", login.getUserId());
        Response resp = new Response();
        
        String desc = "";
        boolean flag = false;

        try {
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<Holder> holderEditList = (List<Holder>) wrapper.getModel();
            Holder holder = holderEditList.get(0);

            //holder must exist
            if (hq.checkHolderAccount(holder.getHolderId())) {
                org.greenpole.hibernate.entity.Holder holderEntity = hq.getHolder(holder.getHolderId());
                logger.info("holder exists - [{}]: [{}]", login.getUserId(), holderEntity.getFirstName() + " " + holderEntity.getLastName());

                //holder must be primary to be edited
                if (holderEntity.isPryHolder()) {

                    if (holder.getFirstName() == null || "".equals(holder.getFirstName())) {
                        desc = "\nHolder first name should not be empty";
                    } else if (holder.getLastName() == null || "".equals(holder.getLastName())) {
                        desc += "\nHolder last name should not be empty";
                    } else if (holder.getTypeId() <= 0) {
                        desc += "\nHolder type should not be empty";
                    } else if (holder.getPrimaryAddress() == null || "".equals(holder.getPrimaryAddress())) {
                        desc += "\nPrimary Holder address is not specified";
                    } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                            || holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
                        desc += "\nPrimary address can only be residential or postal";
                    } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                            && (holder.getHolderResidentialAddresses() == null || holder.getHolderResidentialAddresses().isEmpty())) {
                        desc += "\nResidential address cannot be empty, as it is the primary address";
                    } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())
                            && (holder.getHolderPostalAddresses() == null || holder.getHolderPostalAddresses().isEmpty())) {
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
                    }

                    if (flag && holder.getHolderResidentialAddresses() != null && !holder.getHolderResidentialAddresses().isEmpty()) {
                        for (Address addr : holder.getHolderResidentialAddresses()) {
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

                    if (flag && holder.getHolderPostalAddresses() != null && !holder.getHolderPostalAddresses().isEmpty()) {
                        for (Address addr : holder.getHolderPostalAddresses()) {
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

                    if (flag && holder.getHolderEmailAddresses() != null && !holder.getHolderEmailAddresses().isEmpty()) {
                        for (EmailAddress email : holder.getHolderEmailAddresses()) {
                            if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
                                desc += "\nEmail address should not be empty. Delete email entry if you must";
                                flag = false;
                                break;
                            }
                        }
                    }

                    if (flag && holder.getHolderPhoneNumbers() != null && !holder.getHolderPhoneNumbers().isEmpty()) {
                        for (PhoneNumber phone : holder.getHolderPhoneNumbers()) {
                            if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
                                desc += "\nPhone number should not be empty. Delete phone number entry if you must";
                                flag = false;
                                break;
                            }
                        }
                    }
   
                    if (flag && (holderEntity.getChn() != null || "".equals(holderEntity.getChn()))
                            && (holder.getChn() == null || "".equals(holder.getChn()))) {
                        desc += "\nCHN cannot be erased";
                        flag = false;
                    }

                    if (flag) {
                        HolderType typeEntity = new HolderType();
                        
                        typeEntity.setId(holder.getTypeId());
                        
                        holderEntity.setFirstName(holder.getFirstName());
                        holderEntity.setMiddleName(holder.getMiddleName());
                        holderEntity.setLastName(holder.getLastName());
                        holderEntity.setType(typeEntity);
                        holderEntity.setGender(holder.getGender());
                        holderEntity.setDob(formatter.parse(holder.getDob()));
                        holderEntity.setChn(holder.getChn());
                        List<org.greenpole.hibernate.entity.HolderChanges> holderChangesList = new ArrayList<>();
                        org.greenpole.hibernate.entity.HolderChanges changes = new org.greenpole.hibernate.entity.HolderChanges();

                        for (org.greenpole.entity.model.holder.HolderChanges hc : holder.getChanges()) {
                            HolderChangeType changeType = new HolderChangeType();
                            changeType.setId(hc.getChangeTypeId());
                            
                            changes.setHolder(holderEntity);
                            changes.setInitialForm(hc.getInitialForm());
                            changes.setCurrentForm(hc.getCurrentForm());
                            changes.setChangeDate(formatter.parse(hc.getChangeDate()));
                            changes.setHolderChangeType(changeType);
                            
                            holderChangesList.add(changes);
                        }
                        
                        boolean updated = hq.updateHolderAccount(holderEntity, retrieveHolderResidentialAddress(holder),
                                retrieveHolderPostalAddress(holder), retrieveHolderPhoneNumber(holder),
                                retrieveHolderEmailAddress(holder), holderChangesList);
                        
                        if (updated) {
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
            logger.info("error proccessing holder name transpose. See error log - [{}]", login.getUserId());
            logger.error("error proccessing holder name transpose - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to proccess holder name transpose. Contact system administrator."
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
        
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
            
            Map<String, String> descriptors = Descriptor.decipherDescriptor(queryParams.getDescriptor());
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
                List<org.greenpole.hibernate.entity.AccountConsolidation> acctConsolList = hq.getAllHolderAccountConsolidation(queryParams.getDescriptor(), queryParams.getStartDate(), queryParams.getEndDate());
                List<org.greenpole.entity.model.holder.merge.AccountConsolidation> acctConsolModelList = new ArrayList<>();
                TagUser tag = new TagUser();
                for (org.greenpole.hibernate.entity.AccountConsolidation ac : acctConsolList) {
                    org.greenpole.entity.model.holder.merge.AccountConsolidation acctConsolModel = new org.greenpole.entity.model.holder.merge.AccountConsolidation();
                    acctConsolModel.setId(ac.getId());
                    acctConsolModel.setHolderId(ac.getHolder().getId());
                    acctConsolModel.setHolderName(ac.getHolderName());
                    acctConsolModel.setMergedToHolderId(ac.getMergedToHolderId());
                    acctConsolModel.setMergedToHolderName(ac.getMergedToHolderName());
                    acctConsolModel.setMergeDate(ac.getMergeDate().toString());
                    acctConsolModel.setDemerge(ac.isDemerge());
                    acctConsolModel.setAdditionalChanges(ac.getAdditionalChanges());
                    acctConsolModel.setDemergeDate(ac.getDemergeDate().toString());

                    List<org.greenpole.entity.model.holder.merge.CompanyAccountConsolidation> cacList = new ArrayList<>();
                    for (org.greenpole.hibernate.entity.CompanyAccountConsolidation cac : hq.getCompAcctConsolidation(ac.getId())) {
                        org.greenpole.entity.model.holder.merge.CompanyAccountConsolidation compAcctConsolModel = new org.greenpole.entity.model.holder.merge.CompanyAccountConsolidation();
                        compAcctConsolModel.setForCompanyId(cac.getForCompanyId());
                        compAcctConsolModel.setTiedToInitialHolderId(cac.getTiedToCurrentHolderId());
                        compAcctConsolModel.setTiedToCurrentHolderId(cac.getTiedToCurrentHolderId());
                        compAcctConsolModel.setInitialChn(cac.getInitialChn());
                        compAcctConsolModel.setCurrentChn(cac.getCurrentChn());
                        compAcctConsolModel.setBondShareUnit(cac.getBondShareUnit());
                        compAcctConsolModel.setTransfer(cac.isTransfer());
                        compAcctConsolModel.setReceiverUnitState(cac.getReceiverUnitState());
                        compAcctConsolModel.setReceiverStartUnit(cac.getReceiverStartUnit());
                        compAcctConsolModel.setUnitAfterTransfer(cac.getUnitAfterTransfer());
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
                resp.setDesc("Query result with search parameter");
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
     * Unwraps the holder model to create the administrator hibernate entity model.
     * @param holder the holder which contains a list of its administrators to be created
     * @return a holder hibernate entity with the administrators to be created
     */
    private org.greenpole.hibernate.entity.Holder createAdministrator(Holder holder) {
        org.greenpole.hibernate.entity.Holder holder_hib;
        org.greenpole.hibernate.entity.Administrator admin_hib = new org.greenpole.hibernate.entity.Administrator();
        
        //get holder entity
        holder_hib = hq.getHolder(holder.getHolderId());
        String nameAddition = "Estate of " + holder_hib.getFirstName();
        holder_hib.setFirstName(nameAddition); //change holder name to begin with "estate of" because holder is now deceased
        
        //get all administrators
        Set admins_hib = new HashSet();
        for (Administrator admin_model : holder.getAdministrators()) {
            //add main administrator details to hibernate entity
            admin_hib.setFirstName(admin_model.getFirstName());
            admin_hib.setLastName(admin_model.getLastName());
            admin_hib.setMiddleName(admin_model.getMiddleName());
            admin_hib.setPryAddress(""); //should not be empty. Correct once corrected from Samsudeen's end.
                        
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
        Set phone_set = new HashSet();
        if (admin_model.getPhoneNumbers() != null && !admin_model.getPhoneNumbers().isEmpty()) {
            for (PhoneNumber admin_phone_model : admin_model.getPhoneNumbers()) {
                AdministratorPhoneNumber admin_phone_hib = new AdministratorPhoneNumber();
                AdministratorPhoneNumberId admin_phone_id_hib = new AdministratorPhoneNumberId();

                admin_phone_id_hib.setPhoneNumber(admin_phone_model.getPhoneNumber());

                admin_phone_hib.setIsPrimary(admin_phone_model.isPrimaryPhoneNumber());

                admin_phone_hib.setId(admin_phone_id_hib);

                phone_set.add(admin_phone_hib);
            }
            admin_hib.setAdministratorPhoneNumbers(phone_set);
        }
    }

    private void retrieveAdministratorEmailAddress(Administrator admin_model, org.greenpole.hibernate.entity.Administrator admin_hib) {
        //add email address to hibernate entity
        Set email_set = new HashSet();
        if (admin_model.getEmailAddress() != null && !admin_model.getEmailAddress().isEmpty()) {
            for (EmailAddress admin_email_model : admin_model.getEmailAddress()) {
                AdministratorEmailAddress admin_email_hib = new AdministratorEmailAddress();
                AdministratorEmailAddressId admin_email_id_hib = new AdministratorEmailAddressId();

                admin_email_id_hib.setEmailAddress(admin_email_model.getEmailAddress());

                admin_email_hib.setIsPrimary(admin_email_model.isPrimaryEmail());

                admin_email_hib.setId(admin_email_id_hib);

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
            
            AdministratorResidentialAddress admin_pos_hib = new AdministratorResidentialAddress();
            AdministratorResidentialAddressId admin_pos_id_hib = new AdministratorResidentialAddressId();
            
            admin_pos_id_hib.setAddressLine1(admin_pos_model.getAddressLine1());
            admin_pos_id_hib.setState(admin_pos_model.getState());
            admin_pos_id_hib.setCountry(admin_pos_model.getCountry());
            
            admin_pos_hib.setAddressLine2(admin_pos_model.getAddressLine2());
            admin_pos_hib.setAddressLine3(admin_pos_model.getAddressLine3());
            admin_pos_hib.setAddressLine4(admin_pos_model.getAddressLine4());
            admin_pos_hib.setCity(admin_pos_model.getCity());
            admin_pos_hib.setPostCode(admin_pos_model.getPostCode());
            admin_pos_hib.setIsPrimary(admin_pos_model.isPrimaryAddress());
            
            admin_pos_hib.setId(admin_pos_id_hib);
            
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
            AdministratorResidentialAddressId admin_res_id_hib = new AdministratorResidentialAddressId();
            
            admin_res_id_hib.setAddressLine1(admin_res_model.getAddressLine1());
            admin_res_id_hib.setState(admin_res_model.getState());
            admin_res_id_hib.setCountry(admin_res_model.getCountry());
            
            admin_res_hib.setAddressLine2(admin_res_model.getAddressLine2());
            admin_res_hib.setAddressLine3(admin_res_model.getAddressLine3());
            admin_res_hib.setAddressLine4(admin_res_model.getAddressLine4());
            admin_res_hib.setCity(admin_res_model.getCity());
            admin_res_hib.setPostCode(admin_res_model.getPostCode());
            admin_res_hib.setIsPrimary(admin_res_model.isPrimaryAddress());
            
            admin_res_hib.setId(admin_res_id_hib);
            
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
        org.greenpole.hibernate.entity.HolderPostalAddress postalAddressEntity = new org.greenpole.hibernate.entity.HolderPostalAddress();
        List<org.greenpole.entity.model.Address> hpaddyList = (List<org.greenpole.entity.model.Address>) holdModel.getHolderPostalAddresses();
        List<org.greenpole.hibernate.entity.HolderPostalAddress> returnHolderPostalAddress = new ArrayList<>();

        for (org.greenpole.entity.model.Address hpa : hpaddyList) {
            HolderPostalAddressId postalAddyId = new HolderPostalAddressId();
            /*if (newEntry) {
                postalAddyId.setHolderId(holdModel.getHolderId());
            }*/
            postalAddyId.setAddressLine1(hpa.getAddressLine1());
            postalAddyId.setState(hpa.getState());
            postalAddyId.setCountry(hpa.getCountry());
            postalAddressEntity.setId(postalAddyId);
            postalAddressEntity.setAddressLine2(hpa.getAddressLine2());
            postalAddressEntity.setAddressLine3(hpa.getAddressLine3());
            postalAddressEntity.setCity(hpa.getCity());
            postalAddressEntity.setPostCode(hpa.getPostCode());
            postalAddressEntity.setIsPrimary(hpa.isPrimaryAddress());
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

        org.greenpole.hibernate.entity.HolderPhoneNumber phoneNumberEntity = new org.greenpole.hibernate.entity.HolderPhoneNumber();
        List<org.greenpole.entity.model.PhoneNumber> phoneNumberList = holdModel.getHolderPhoneNumbers();
        List<org.greenpole.hibernate.entity.HolderPhoneNumber> returnPhoneNumber = new ArrayList<>();

        for (PhoneNumber pnList : phoneNumberList) {
            HolderPhoneNumberId phoneNoId = new HolderPhoneNumberId();
            /*if (newEntry) {
                phoneNoId.setHolderId(holdModel.getHolderId());
            }*/
            phoneNoId.setPhoneNumber(pnList.getPhoneNumber());
            phoneNumberEntity.setIsPrimary(pnList.isPrimaryPhoneNumber());
            phoneNumberEntity.setId(phoneNoId);
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

        org.greenpole.hibernate.entity.HolderEmailAddress emailAddressEntity = new org.greenpole.hibernate.entity.HolderEmailAddress();
        List<org.greenpole.entity.model.EmailAddress> emailAddressList = holdModel.getHolderEmailAddresses();
        List<org.greenpole.hibernate.entity.HolderEmailAddress> returnEmailAddress = new ArrayList<>();

        for (EmailAddress email : emailAddressList) {
            HolderEmailAddressId emailId = new HolderEmailAddressId();
            /*if (newEntry) {
                emailId.setHolderId(holdModel.getHolderId());
            }*/
            emailId.setEmailAddress(email.getEmailAddress());
            emailAddressEntity.setIsPrimary(email.isPrimaryEmail());
            emailAddressEntity.setId(emailId);
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
        org.greenpole.hibernate.entity.HolderResidentialAddress residentialAddressEntity = new org.greenpole.hibernate.entity.HolderResidentialAddress();
        List<org.greenpole.entity.model.Address> residentialAddressList = (List<org.greenpole.entity.model.Address>) holdModel.getHolderResidentialAddresses();
        List<org.greenpole.hibernate.entity.HolderResidentialAddress> returnResidentialAddress = new ArrayList();

        for (org.greenpole.entity.model.Address rAddy : residentialAddressList) {
            HolderResidentialAddressId rAddyId = new HolderResidentialAddressId();
            /*if (newEntry) {
                rAddyId.setHolderId(holdModel.getHolderId());
            }*/
            rAddyId.setAddressLine1(rAddy.getAddressLine1());
            rAddyId.setState(rAddy.getState());
            rAddyId.setCountry(rAddy.getCountry());

            residentialAddressEntity.setId(rAddyId);
            residentialAddressEntity.setAddressLine2(rAddy.getAddressLine2());
            residentialAddressEntity.setAddressLine3(rAddy.getAddressLine3());
            residentialAddressEntity.setAddressLine4(rAddy.getAddressLine4());
            residentialAddressEntity.setCity(rAddy.getCity());
            residentialAddressEntity.setPostCode(rAddy.getPostCode());

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
        org.greenpole.entity.model.holder.HolderCompanyAccount compAcct = holdModel.getHolderCompanyAccounts().get(0);
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
        org.greenpole.entity.model.holder.HolderBondAccount bondAcct = holdModel.getHolderBondAccounts().get(0);
        HolderBondAccountId bondAcctId = new HolderBondAccountId();
        
        bondAcctId.setBondOfferId(bondAcct.getBondOfferId());
        
        bondAccountEntity.setId(bondAcctId);
        bondAccountEntity.setStartingPrincipalValue(bondAcct.getStartingPrincipalValue());
        bondAccountEntity.setHolderBondAccPrimary(true);
        bondAccountEntity.setMerged(false);
        
        return bondAccountEntity;
    }
}
