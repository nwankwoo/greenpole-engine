/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.model.Address;
import org.greenpole.entity.model.EmailAddress;
import org.greenpole.entity.model.PhoneNumber;
import org.greenpole.entity.model.clientcompany.InitialPublicOffer;
import org.greenpole.entity.model.clientcompany.PrivatePlacement;
import org.greenpole.entity.model.holder.Administrator;
import org.greenpole.entity.model.holder.Holder;
import org.greenpole.entity.model.holder.HolderBondAccount;
import org.greenpole.entity.model.holder.HolderCompanyAccount;
import org.greenpole.entity.model.stockbroker.Stockbroker;
import org.greenpole.entity.model.taguser.TagUser;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.entity.model.clientcompany.ClientCompany;
import org.greenpole.entrycode.emmanuel.model.ClientCompanyMerger;
import org.greenpole.entrycode.emmanuel.model.IpoApplication;
import org.greenpole.entrycode.emmanuel.model.PrivatePlacementApplication;
import org.greenpole.entrycode.emmanuel.model.QueryIPORightsIssuePrivatePlacement;
import org.greenpole.entrycode.emmanuel.model.QueryShareholders;
import org.greenpole.entrycode.emmanuel.model.RightsIssue;
import org.greenpole.entrycode.emmanuel.model.RightsIssueApplication;
import org.greenpole.hibernate.entity.AdministratorPostalAddress;
import org.greenpole.hibernate.entity.HolderCompanyAccountId;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.GeneralComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.Descriptor;
import org.greenpole.util.Notification;
import org.greenpole.util.properties.GreenpoleProperties;
import org.greenpole.util.properties.NotificationProperties;
import org.greenpole.util.properties.NotifierProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author user
 */
public class ClientCompanyLogic {

    private static final Logger logger = LoggerFactory.getLogger(HolderComponentLogic.class);
    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();
    private final GeneralComponentQuery gq = ComponentQueryFactory.getGeneralComponentQuery();
    private final GreenpoleProperties greenProp = new GreenpoleProperties(ClientCompanyLogic.class);
    private final NotificationProperties notificationProp = new NotificationProperties(ClientCompanyLogic.class);
    private final HibernatDummyQuerInterface hd = HibernateDummyQueryFactory.getHibernateDummyQuery();//not needed

    /**
     * Request to setup rights issue
     *
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the
     * notification
     * @param rightIssue the rights issue to be set up
     * @return response to the set up of right issue request
     */
    public Response setupRightsIssue_Request(Login login, String authenticator, RightsIssue rightIssue) {
        logger.info("request to set up rights issue, invoked by", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        QueueSender qSender;
        NotificationWrapper wrapper;
        NotifierProperties prop;
        try {
            org.greenpole.hibernate.entity.ClientCompany clientCompany = cq.getClientCompany(rightIssue.getClientCompanyId());
            if (cq.checkClientCompany(clientCompany.getName())) {
                logger.info("client company [{}] checks out - [{}]", clientCompany.getName(), login.getUserId());
                if (cq.checkClientCompanyForShareholders(clientCompany.getName())) {
                    logger.info("client company [{}] checks out. No shareholders found - [{}]", clientCompany.getName(), login.getUserId());
                    if (rightIssue.getTotalSharesOnIssue() > 0) {
                        if (rightIssue.getQualifyDate() != null || !"".equals(rightIssue.getQualifyDate())) {
                            if (rightIssue.getOpeningDate() != null || !"".equals(rightIssue.getOpeningDate())) {
                                if (rightIssue.getClosingDate() != null || !"".equals(rightIssue.getClosingDate())) {
                                    wrapper = new NotificationWrapper();
                                    prop = new NotifierProperties(ClientCompanyLogic.class);
                                    qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                                            prop.getAuthoriserNotifierQueueName());
                                    List<RightsIssue> rights_list = new ArrayList();
                                    rights_list.add(rightIssue);
                                    wrapper.setCode(notification.createCode(login));
                                    wrapper.setDescription("Authenticate set up of right issue under the client company " + clientCompany.getName());
                                    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                                    wrapper.setFrom(login.getUserId());
                                    wrapper.setTo(authenticator);
                                    wrapper.setModel(rights_list);
                                    logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                                    resp = qSender.sendAuthorisationRequest(wrapper);
                                    return resp;
                                }
                                resp.setRetn(200);
                                resp.setDesc("Right issue closing date is empty");
                                logger.info("Right issue closing date is empty ", login.getUserId());
                                return resp;
                            }
                            resp.setRetn(200);
                            resp.setDesc("Right issue opening date cannot be empty");
                            logger.info("Right issue opening date is empty ", login.getUserId());
                            return resp;
                        }
                        resp.setRetn(200);
                        resp.setDesc("Right issue qualify date cannot be empty");
                        logger.info("Right issue qualify date cannot be empty ", login.getUserId());
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Total shares on issue cannot be less than one");
                    logger.info("Total shares on issue is less than ", login.getUserId());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Client company has no share holders so rights issue cannot be set");
                logger.info("Client company has no share holders so rights issue cannot be set ", login.getUserId());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Client company does not exist");
            logger.info("Client company does not exist ", login.getUserId());
        } catch (Exception ex) {
            logger.info("error processing right issue setup. See error log - [{}]", login.getUserId());
            logger.error("error processing right issue setup - [" + login.getUserId() + "]", ex);
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process right issue setup. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
        return resp;
    }

    /**
     * Processes saved request to set up right issue.
     *
     * @param login The user's login details
     * @param notificationCode The notification code
     * @return Response to the set up of rights issue request
     */
    public Response setupRightsIssue_Authorise(Login login, String notificationCode) {
        logger.info("authorise set up of rights issue, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        NotificationProperties noteProps = new NotificationProperties(ClientCompanyLogic.class);
        try {

            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<RightsIssue> rights_List = (List<RightsIssue>) wrapper.getModel();
            RightsIssue right_model = rights_List.get(0);
            double converted_issueSize;
            boolean created;
            long millis = System.currentTimeMillis();
            Date current_date = new java.sql.Date(millis);
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            boolean done = false;
            org.greenpole.hibernate.entity.RightsIssue right_hib = new org.greenpole.hibernate.entity.RightsIssue();
            org.greenpole.hibernate.entity.ClientCompany clientCompany = cq.getClientCompany(right_model.getClientCompanyId());
            List<org.greenpole.hibernate.entity.HolderCompanyAccount> hca_list = hd.getAllHolderCompanyAccountsByClientCompanyId(right_model.getClientCompanyId());
            org.greenpole.hibernate.entity.ClearingHouse chn = new org.greenpole.hibernate.entity.ClearingHouse();
            if (cq.checkClientCompany(clientCompany.getName())) {
                //if(!hca_list.isEmpty())
                logger.info("client company [{}] checks out - [{}]", clientCompany.getName(), login.getUserId());
                if (cq.checkClientCompanyForShareholders(clientCompany.getName())) {
                    logger.info("client company [{}] is checked for shareholders - [{}]", clientCompany.getName(), login.getUserId());
                    if (right_model.getTotalSharesOnIssue() > 0) {
                        if (right_model.getQualifyDate() != null || !"".equals(right_model.getQualifyDate())) {
                            if (right_model.getOpeningDate() != null || !"".equals(right_model.getOpeningDate())) {
                                if (right_model.getClosingDate() != null || !"".equals(right_model.getClosingDate())) {
                                    right_hib.setTotalSharesOnIssue(right_model.getTotalSharesOnIssue());
                                    right_hib.setMethodOnOffer(right_model.getMethodOnOffer());
                                    right_hib.setIssuePrice(right_model.getIssuePrice());
                                    right_hib.setIssueSize(right_model.getTotalSharesOnIssue() * right_model.getIssuePrice());
                                    right_hib.setQualifyShareUnit(right_model.getQualifyShareUnit());
                                    right_hib.setAlottedUnitPerQualifyUnit(right_model.getAlottedUnitPerQualifyUnit());
                                    right_hib.setQualifyDate(formatter.parse(right_model.getQualifyDate()));
                                    right_hib.setOpeningDate(formatter.parse(right_model.getOpeningDate()));
                                    right_hib.setClosingDate(formatter.parse(right_model.getClosingDate()));
                                    right_hib.setClientCompany(clientCompany);
                                    created = hd.setUp_RightIssue(right_hib);
                                    if (created) {
                                        List<org.greenpole.hibernate.entity.Holder> holder_list = hd.getHoldersByClientCompanyId(clientCompany.getId());
                                        for (org.greenpole.hibernate.entity.Holder holder : holder_list) {//loop through holders list
                                            org.greenpole.hibernate.entity.Holder holder_hib = hd.getHolder(holder.getId());
                                            List<org.greenpole.hibernate.entity.HolderCompanyAccount> holderComp_list = hd.getHoldersShareUnitsByClientCompanyId(clientCompany.getId());
                                            for (org.greenpole.hibernate.entity.HolderCompanyAccount hca : holderComp_list) {//loop through HolderCompanyAccount                                                
                                                org.greenpole.hibernate.entity.RightsIssueApplication right_app_hib = new org.greenpole.hibernate.entity.RightsIssueApplication();
                                                RightsIssueApplication right_app_model = new RightsIssueApplication();
                                                double qualifyHolderShares, expectedPayableAmount;
                                                int holderDueShares, unqualifyShares, sum_unqualifyShares = 0, specialRights;
                                                List<org.greenpole.hibernate.entity.RightsIssueApplication> right_app_list = new ArrayList<>();

                                                if (hca.getShareUnits() % right_model.getQualifyShareUnit() != 0) {
                                                    qualifyHolderShares = Math.floor((hca.getShareUnits() / right_model.getQualifyShareUnit()) * right_model.getQualifyShareUnit());
                                                    holderDueShares = ((int) qualifyHolderShares / right_model.getQualifyShareUnit() * right_model.getAlottedUnitPerQualifyUnit());
                                                    unqualifyShares = hca.getShareUnits() % right_model.getQualifyShareUnit();
                                                    sum_unqualifyShares += unqualifyShares;
                                                    specialRights = sum_unqualifyShares / right_model.getQualifyShareUnit();//the rights allocated to the unqualify shares
                                                    //expectedPayableAmount = holderDueShares * right_model.getIssuePrice();                                                  
                                                    if (formatter.parse(right_model.getQualifyDate()).before(current_date) || formatter.parse(right_model.getQualifyDate()).equals(current_date)) {
                                                        right_app_hib.setHolder(holder_hib);
                                                        right_app_hib.setTotalHoldings(hca.getShareUnits());//total share units of shareholder before application
                                                        right_app_hib.setAllottedRights(holderDueShares);
                                                        //right_app_hib.set(expectedPayableAmount);//entity for expected payable amount is not present
                                                        right_app_list.add(right_app_hib);
                                                        done = hd.rightsIssueAppFromSetup(right_app_hib);
                                                    }
                                                } else if (hca.getShareUnits() % right_model.getQualifyShareUnit() == 0) {
                                                    holderDueShares = hca.getShareUnits() / right_model.getQualifyShareUnit() * right_model.getAlottedUnitPerQualifyUnit();//get shares due to holder using set rule
                                                    expectedPayableAmount = holderDueShares * right_model.getIssuePrice();
                                                    if (formatter.parse(right_model.getQualifyDate()).before(current_date) || formatter.parse(right_model.getQualifyDate()).equals(current_date)) {
                                                        right_app_hib.setHolder(holder_hib);
                                                        right_app_hib.setTotalHoldings(hca.getShareUnits());
                                                        right_app_hib.setAllottedRights(holderDueShares);
                                                        right_app_hib.setSharesSubscribedValue(expectedPayableAmount);
                                                        done = hd.rightsIssueAppFromSetup(right_app_hib);
                                                    }
                                                }

                                                if (formatter.parse(right_model.getOpeningDate()).after(current_date) || formatter.parse(right_model.getOpeningDate()).equals(current_date)) {
                                                    right_hib.setRightsClosed(false);
                                                    hd.updateRightIssueSetup(right_model.getId());
                                                }
                                                //the part of the closing date is not taken into consideration for now
                                            }

                                        }
                                        if (done) {
                                            notification.markAttended(notificationCode);
                                            logger.info("Rights issue setup was successful - [{}]", login.getUserId());
                                            resp.setRetn(0);
                                            resp.setDesc("Successful");
                                            return resp;
                                        }
                                        if (!done) {
                                            logger.info("Rights issue setup was unsuccessful - [{}]", login.getUserId());
                                            resp.setRetn(98);
                                            resp.setDesc("Rights issue setup was unsuccessful");
                                            return resp;
                                        }

                                    }
                                    resp.setRetn(200);
                                    resp.setDesc("Unable to perform rights issue setup due to error");
                                    logger.info("Unable to perform rights issue setup due to error ", login.getUserId());
                                    return resp;
                                }
                                resp.setRetn(200);
                                resp.setDesc("Right issue closing date MUST be specified");
                                logger.info("Right issue closing date MUST be specified ", login.getUserId());
                                return resp;
                            }
                            resp.setRetn(200);
                            resp.setDesc("Right issue opening date MUST be specified");
                            logger.info("Right issue opening date MUST be specified ", login.getUserId());
                            return resp;
                        }
                        resp.setRetn(200);
                        resp.setDesc("Right issue qualify date cannot be empty");
                        logger.info("Right issue qualify date cannot be empty ", login.getUserId());
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Total shares on issue cannot be less than one");
                    logger.info("Total shares on issue is less than ", login.getUserId());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Client company has no share holders so rights issue cannot be set");
                logger.info("Client company has no share holders so rights issue cannot be set ", login.getUserId());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Client company does not exist");
            logger.info("Client company does not exist ", login.getUserId());
        } catch (JAXBException | ParseException ex) {
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);

            resp.setRetn(98);
            resp.setDesc("Unable to set up rights issue. Contact System Administrator");

            return resp;
        } catch (Exception ex) {
            logger.info("error in processing rights issue set up. See error log");
            logger.error("error in processing rights issue set up - ", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to process rights issue set up. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
        return resp;
    }

    /**
     * Request to merge a client company account / multiple client company
     * accounts to a primary client company account
     *
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the
     * notification
     * @param companiesToMerge the merge client company accounts object
     * containing the merge details
     * @return response to the merge client company accounts request
     */
    public Response mergeClientCompanies_Request(Login login, String authenticator, ClientCompanyMerger companiesToMerge) {
        logger.info("request client companies accounts merge, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;
            org.greenpole.hibernate.entity.ClientCompany cc_hib = new org.greenpole.hibernate.entity.ClientCompany();
            List<org.greenpole.hibernate.entity.ClientCompany> cc_hib_list = new ArrayList<>();

            boolean primaryCheck = false;
            boolean secondaryCheck = false;
            //check that the primary client company exist
            if (cq.checkClientCompany(companiesToMerge.getPrimaryClientCompany().getId())) {//check that client company exist
                if (companiesToMerge.getPrimaryClientCompany().isClientCompanyPrimary()) {//check for primary client company
                    cc_hib = cq.getClientCompany(companiesToMerge.getPrimaryClientCompany().getId());
                    if (!cc_hib.getMerged()) {
                        primaryCheck = true;
                    }
                }
            }
            //check for secondary client companies
            for (ClientCompany cc : companiesToMerge.getSecondaryClientCompany()) {
                if (!cc.isClientCompanyPrimary() && cc.getName() != null && !"".equals(cc.getName())) {
                    org.greenpole.hibernate.entity.ClientCompany cc_hib_entity = cq.getClientCompany(cc.getId());
                    if (!cc.isMerged()) {
                        cc_hib_list.add(cc_hib_entity);
                        secondaryCheck = true;
                    }
                } else {
                    secondaryCheck = false;
                    break;
                }
            }
            if (primaryCheck && secondaryCheck) {
                wrapper = new NotificationWrapper();
                prop = new NotifierProperties(ClientCompanyLogic.class);
                qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                        prop.getAuthoriserNotifierQueueName());
                logger.info("clients companies qualify for merge - [{}]", login.getUserId());
                List<ClientCompanyMerger> ccMerger_list = new ArrayList<>();
                ccMerger_list.add(companiesToMerge);
                String pryClientCompanyName = cc_hib.getName();
                String msgDesc = "Merge client company accounts for " + pryClientCompanyName + " as primary and ";
                int counter = 0;
                for (org.greenpole.hibernate.entity.ClientCompany cc_h : cc_hib_list) {
                    String secName = cc_h.getName();
                    msgDesc += secName;
                    counter++;
                    if (counter < cc_hib_list.size()) {
                        msgDesc += ", as secondary accounts";
                    }
                }
                msgDesc += ", requested by " + login.getUserId();
                wrapper.setCode(notification.createCode(login));
                wrapper.setDescription(msgDesc);
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(ccMerger_list);

                logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                resp = qSender.sendAuthorisationRequest(wrapper);
                return resp;
            } else {
                String errorMsg = "";
                if (!primaryCheck) {
                    errorMsg += "\nThe chosen primary client company account has already been merged into another account.";
                    logger.info("The chosen primary client company account has already been merged into another account - [{}]", login.getUserId());
                } else if (!secondaryCheck) {
                    errorMsg += "\nThe chosen secondary client company account (or one of them) has already been merged into another account.";
                    logger.info("The chosen secondary client company account (or one of them) has already been merged into another account - [{}]", login.getUserId());
                }
                resp.setRetn(200);
                resp.setDesc("Error: " + errorMsg);
                return resp;
            }

        } catch (Exception ex) {
            logger.info("error processing client company accounts merge. See error log - [{}]", login.getUserId());
            logger.error("error processing client company accounts merge - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to process client company accounts merge. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     * Processes a saved request to merge client company accounts, according to
     * a specified notification code.
     *
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the merge client company accounts request
     */
    public Response mergeClientCompanies_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        Notification notification = new Notification();
        logger.info("authorise client companies accounts merge, invoked by [{}] - notification code: [{}]", login.getUserId(), notificationCode);
        NotificationProperties noteProps = new NotificationProperties(ClientCompanyLogic.class);
        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<ClientCompanyMerger> merger_list = (List<ClientCompanyMerger>) wrapper.getModel();
            ClientCompanyMerger cc_merger = merger_list.get(0);

            boolean pryClientExists = cq.checkClientCompany(cc_merger.getPrimaryClientCompany().getId());
            if (pryClientExists) {
                //get the primary client company first, holders , and company / bond accounts
                org.greenpole.hibernate.entity.ClientCompany pryClient = cq.getClientCompany(cc_merger.getPrimaryClientCompany().getId());
                if (pryClient.getClientCompanyPrimary()) {
                    logger.info("primary client passes set rules - [{}]", login.getUserId());
                    List<org.greenpole.hibernate.entity.ClientCompany> secClients = new ArrayList<>();
                    boolean similarToActive = false;

                    org.greenpole.hibernate.entity.BondOffer pryBondOffer = hd.getBondOfferId(pryClient.getId());//used to check for holder bond account
                    boolean pryHasBondAccts = hd.hasBondAccounts(pryBondOffer.getId());
                    org.greenpole.hibernate.entity.Holder prholder = hd.getHolderIdFromHolderBondAccount(pryBondOffer.getId());
                    boolean pryHasCompAccts = hd.hasCompanyAccounts(prholder.getId());
                    List<org.greenpole.hibernate.entity.HolderBondAccount> pryBondAccts = new ArrayList<>();//all pry bond accts to be checked
                    List<org.greenpole.hibernate.entity.HolderCompanyAccount> pryCompAccts = new ArrayList<>();//all pry bond accts to be checked
                    //boolean pryHasBondAccts = hq.hasBondAccount(pryHolder.getId());
                    if (pryHasBondAccts) {
                        pryBondAccts = hd.getAllBondAccounts(prholder.getId());
                        logger.info("primary holder has bond accounts - [{}]", login.getUserId());
                    }
                    if (pryHasCompAccts) {
                        pryCompAccts = hd.getAllCompAccounts(prholder.getId());
                        logger.info("primary holder has company accounts - [{}]", login.getUserId());
                    }
                    List<org.greenpole.hibernate.entity.HolderCompanyAccount> secHolderCompAccts = new ArrayList<>();//all sec comp accts to be moved to the primary client company account
                    List<org.greenpole.hibernate.entity.HolderBondAccount> secHolderBondAccts = new ArrayList<>();//all sec bond accts to be to the primary client company account
                    mainloop:
                    for (ClientCompany cc : cc_merger.getSecondaryClientCompany()) {
                        boolean secClientsExist = cq.checkClientCompany(cc.getId());
                        if (secClientsExist) {
                            logger.info("selected secondary client company exists - [{}]", login.getUserId());
                            org.greenpole.hibernate.entity.ClientCompany secClients_obj = cq.getClientCompany(cc.getId());
                            if (!secClients_obj.getClientCompanyPrimary()) {
                                secClients.add(secClients_obj);
                                org.greenpole.hibernate.entity.BondOffer secBondOffer = hd.getBondOfferId(cc.getId());//used to get the secondary client company
                                org.greenpole.hibernate.entity.Holder secHolder = hd.getHolderIdFromHolderBondAccount(secBondOffer.getId());
                                boolean secHasCompAccts = hd.hasCompanyAccounts(secHolder.getId());
                                boolean secHasBondAccts = hd.hasBondAccounts(secHolder.getId());
                                if (secHasCompAccts) {
                                    secHolderCompAccts = hd.getAllCompAccounts(secHolder.getId());
                                    logger.info("selected secondary client company has holder company accounts - [{}]", login.getUserId());
                                }

                                if (secHasBondAccts) {
                                    secHolderBondAccts = hd.getAllBondAccounts(secHolder.getId());
                                    logger.info("selected secondary client company has bond accounts - [{}]", login.getUserId());
                                }
                                //search through company and bond accounts
                                for (org.greenpole.hibernate.entity.HolderCompanyAccount sec_hca : secHolderCompAccts) {
                                    secHolderCompAccts.add(sec_hca);//add secondary company accounts to list
                                }
                                for (org.greenpole.hibernate.entity.HolderBondAccount sec_hba : secHolderBondAccts) {
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
                            resp.setRetn(200);
                            resp.setDesc("Merge unsuccessful. The secondary client company - " + secClients_obj.getName()
                                    + " - account has already been merged, \nContact Administrator.");
                            logger.info("Merge unsuccessful. The secondary client company - [{}]  - account has already been merged  - [{}]",
                                    secClients_obj.getName(), login.getUserId());
                            return resp;
                        }
                        resp.setRetn(200);
                        resp.setDesc("Merge unsuccessful. The secondary client company - " + cc.getName() + " does not exist "
                                + "\nContact Administrator ");
                        logger.info("Merge unsuccessful. The secondary client company - [{}]  - account does not exist  - [{}]",
                                cc.getName(), login.getUserId());
                        return resp;
                    }
                    boolean merged;
                    if (!similarToActive) {
                        merged = hd.mergeClientCompanyAccounts(pryClient, secClients, secHolderCompAccts, secHolderBondAccts);
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
                        resp.setDesc("Successfully merged");
                        logger.info("Successfully merged - [{}]", login.getUserId());
                        return resp;
                    } else {
                        resp.setRetn(301);
                        resp.setDesc("Merge unsuccessful due to error. Contact Administrator.");
                        logger.info("Merge unsuccessful due to  error - [{}]", login.getUserId());
                        return resp;
                    }

                }
                resp.setRetn(301);
                resp.setDesc("Merge unsuccessful. Primary client company account has already been merged."
                        + "\nContact Administrator.");
                logger.info("Merge unsuccessful. Primary client company account has already been merged - [{}]", login.getUserId());
                return resp;

            }
            resp.setRetn(200);
            resp.setDesc("Merge unsuccessful. Primary client company account does not exist. Contact Administrator.");
            logger.info("Merge unsuccessful. Primary client company account does not exist - [{}]", login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);

            resp.setRetn(98);
            resp.setDesc("Unable to merge client companies accounts. Contact System Administrator");

            return resp;
        } catch (Exception ex) {
        }
        return resp;
    }

    /**
     *
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the
     * notification
     * @param clientCompany the client company to be invalidated
     * @return response to invalidate client company accounts request
     */
    public Response invalidateClientCompany_Request(Login login, String authenticator, ClientCompany clientCompany) {
        logger.info("request to invalidate client company account, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;
            if (cq.checkClientCompany(clientCompany.getName())) {
                logger.info("client company [{}] checks out - [{}]", clientCompany.getName(), login.getUserId());
                if (!cq.checkClientCompanyForShareholders(clientCompany.getName())) {
                    logger.info("client company [{}] is checked for shareholders - [{}]", clientCompany.getName(), login.getUserId());
                    wrapper = new NotificationWrapper();
                    prop = new NotifierProperties(ClientCompanyLogic.class);
                    qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                            prop.getAuthoriserNotifierQueueName());
                    List<ClientCompany> cc_list = new ArrayList();
                    cc_list.add(clientCompany);
                    wrapper.setCode(notification.createCode(login));
                    wrapper.setDescription("Authenticate invalidation of client company " + clientCompany.getName());
                    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                    wrapper.setFrom(login.getUserId());
                    wrapper.setTo(authenticator);
                    wrapper.setModel(cc_list);
                    logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                    resp = qSender.sendAuthorisationRequest(wrapper);
                    return resp;
                } else {
                    //org.greenpole.hibernate.entity.ClientCompany cc_hib = cq.getClientCompany(clientCompany.getId());
                    if (clientCompany.isMerged()) {
                        //cc_hib.setValid(false);
                        resp.setRetn(200);
                        resp.setDesc("Client company invalidation is came from merge");
                        logger.info("client company [{}] invalidation came from merge - [{}]", clientCompany.getName(), login.getUserId());
                        return resp;
                    } else if (!clientCompany.isMerged()) {
                        wrapper = new NotificationWrapper();
                        prop = new NotifierProperties(ClientCompanyLogic.class);
                        qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                                prop.getAuthoriserNotifierQueueName());
                        List<ClientCompany> cc_list = new ArrayList();
                        cc_list.add(clientCompany);
                        wrapper.setCode(notification.createCode(login));
                        wrapper.setDescription("Authenticate invalidation of client company " + clientCompany.getName());
                        wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                        wrapper.setFrom(login.getUserId());
                        wrapper.setTo(authenticator);
                        wrapper.setModel(cc_list);
                        logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                        resp = qSender.sendAuthorisationRequest(wrapper);
                        return resp;
                    }
                }
            }

            resp.setRetn(200);
            resp.setDesc("Client company does not exist");
            logger.info("Client company does not exist ", login.getUserId());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(200);
            resp.setDesc("Client company does not exist");
            logger.info("Client company does not exist ", login.getUserId());
        }
        return resp;
    }

    /**
     * Processes a saved request to invalidate client company
     *
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to invalidate client company account request
     */
    public Response invalidateClientCompany_Authorise(Login login, String notificationCode) {
        logger.info("request to invalidate client company, invoked by: [{}] ", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        NotifierProperties prop;
        NotificationProperties noteProps = new NotificationProperties(ClientCompanyLogic.class);
        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(noteProps.getNotificationLocation(), notificationCode);
            List<ClientCompany> cc_List = (List<ClientCompany>) wrapper.getModel();
            ClientCompany cc_model = cc_List.get(0);
            if (cq.checkClientCompany(cc_model.getName())) {
                logger.info("client company [{}] checks out - [{}]", cc_model.getName(), login.getUserId());
                if (!cq.checkClientCompanyForShareholders(cc_model.getName())) {
                    logger.info("client company [{}] is checked for shareholders - [{}]", cc_model.getName(), login.getUserId());
                    boolean deleted = hd.removeClientCompany(cc_model.getId());
                    if (deleted) {
                        resp.setRetn(0);
                        resp.setDesc("Client company " + cc_model.getName() + " was successfully purged from the system. Invoked by: " + login.getUserId());
                        logger.info("Client company " + cc_model.getName() + " was successfully purged from the system. Invoked by: " + login.getUserId());
                    } else {
                        resp.setRetn(200);
                        resp.setDesc("Client company " + cc_model.getName() + " was not purged from the system. Invoked by: " + login.getUserId());
                        logger.info("Client company " + cc_model.getName() + " was not purged from the system. Invoked by: " + login.getUserId());
                    }

                } else {
                    org.greenpole.hibernate.entity.ClientCompany cc_hib = cq.getClientCompany(cc_model.getId());
                    if (cc_model.isMerged()) {
                        cc_model.setValid(false);
                        boolean isValid = hd.updateClientCompanyValidStatus(cc_hib.getId());//sets valid status to false 
                    } else {
                        cc_model.setValid(false);
                        boolean isValid = hd.updateClientCompanyValidStatus(cc_hib.getId());//sets valid status to false
                        List<org.greenpole.hibernate.entity.HolderCompanyAccount> holderComp_list = hd.getHoldersShareUnitsByClientCompanyId(cc_hib.getId());

                        //yet to implement the part to invalidate share holders company accounts tied to this client company
                        for (org.greenpole.hibernate.entity.HolderCompanyAccount hca : holderComp_list) {
                            org.greenpole.hibernate.entity.HolderCompanyAccount h_hib = hd.getHolderCompanyAccount(hca.getHolder().getId());
                        }
                        notification.markAttended(notificationCode);
                        resp.setRetn(0);
                        resp.setDesc("Successful");
                        logger.info("Client company invalidated successfully, invoked by ", login.getUserId());
                        return resp;
                    }
                }
            }
            resp.setRetn(200);
            resp.setDesc("Client company does not exist");
            logger.info("Client company does not exist ", login.getUserId());
            return resp;
        } catch (Exception ex) {
        }
        return resp;
    }

    /**
     * Processes a query to get list of IPO of a client company
     *
     * @param login the user's login details
     * @param queryParams the query parameters to search by
     * @return response to the query list
     */
    public Response queryIPO_Request(Login login, QueryIPORightsIssuePrivatePlacement queryParams) {
        Response resp = new Response();
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            Descriptor descriptorUtil = new Descriptor();
            Map<String, String> descriptors = descriptorUtil.decipherDescriptor(queryParams.getDescriptor());
            //check that specified date is properly formatted
            if (descriptors.size() == 1) {
                if (descriptors.get("date").equalsIgnoreCase("none")) {
                    try {
                        formatter.parse(queryParams.getStart_date());
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
                        formatter.parse(queryParams.getEnd_date());
                    } catch (ParseException ex) {
                        logger.info("an error was thrown while checking the start date. See error log - [{}]", login.getUserId());
                        resp.setRetn(300);
                        resp.setDesc("Incorrect date format for end date");
                        logger.error("Incorrect date format for end date - [{}]", login.getUserId(), ex);

                        return resp;
                    }
                }
                org.greenpole.hibernate.entity.IpoApplication ipoApp_hib = new org.greenpole.hibernate.entity.IpoApplication();
                List<org.greenpole.hibernate.entity.InitialPublicOffer> ipo_hib_list = hd.getInitialPublicOfferByClientCompanyId(queryParams.getInitialPublicOffer().getClientCompanyId(), queryParams.getDescriptor(), queryParams.getStart_date(), queryParams.getEnd_date(), queryParams.getDate_format());
                //List<org.greenpole.hibernate.entity.IpoApplication> ipo_list = hd.getAllIpoApplication(ipo_hib.getId(), queryParams.getInitialPublicOffer().getClientCompanyId());
                int totalSharesSubscribed = 0;
                int remaingShares = 0;
                List<IpoApplication> ipoApp_list_out = new ArrayList<>();
                List<InitialPublicOffer> ipo_list_out = new ArrayList<>();
                TagUser tag = new TagUser();
                for (org.greenpole.hibernate.entity.InitialPublicOffer ipo : ipo_hib_list) {
                    InitialPublicOffer ipoModel = new InitialPublicOffer();
                    IpoApplication ipoApp_model = new IpoApplication();
                    ipoModel.setTotalSharesOnOffer(ipo.getTotalSharesOnOffer());
                    for (org.greenpole.hibernate.entity.IpoApplication ipoApp : hd.getAllIpoApplication(ipo.getId(), queryParams.getInitialPublicOffer().getClientCompanyId())) {
                        org.greenpole.hibernate.entity.ClearingHouse ch_hib = hd.getClearingHouse(ipoApp.getClearingHouse().getId());
                        totalSharesSubscribed += ipoApp.getSharesSubscribed();
                        remaingShares = ipo.getTotalSharesOnOffer() - totalSharesSubscribed;
                        ipoApp_model.setInitialPublicOffer(ipoModel);
                        ipoApp_model.setClearingHouseName(ch_hib.getName());
                        ipoApp_model.setClearingHouseBrokerage(ipo.getTotalSharesOnOffer() * ipo.getOfferPrice() * 0.75 / 100);//total values of shares submitted to clearing house
                        ipoApp_list_out.add(ipoApp_model);
                    }
                }
                List<TagUser> tagList = new ArrayList<>();

                tag.setQueryParam(queryParams);
                tag.setResult(ipoApp_list_out);
                tagList.add(tag);

                resp.setBody(tagList);
                resp.setDesc("Query result with search parameter");
                resp.setRetn(0);
                logger.info("Query successful - [{}]", login.getUserId());
                return resp;
            }
        } catch (Exception ex) {
        }
        return resp;
    }

    /**
     *
     * @param login
     * @param queryParams
     * @return
     */
    public Response viewRightsIssue_Request(Login login, QueryIPORightsIssuePrivatePlacement queryParams) {
        Response resp = new Response();
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            Descriptor descriptorUtil = new Descriptor();
            Map<String, String> descriptors = descriptorUtil.decipherDescriptor(queryParams.getDescriptor());
            //check that specified date is properly formatted
            if (descriptors.size() == 1) {
                if (descriptors.get("date").equalsIgnoreCase("none")) {
                    try {
                        formatter.parse(queryParams.getStart_date());
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
                        formatter.parse(queryParams.getEnd_date());
                    } catch (ParseException ex) {
                        logger.info("an error was thrown while checking the start date. See error log - [{}]", login.getUserId());
                        resp.setRetn(300);
                        resp.setDesc("Incorrect date format for end date");
                        logger.error("Incorrect date format for end date - [{}]", login.getUserId(), ex);

                        return resp;
                    }
                }
                List<org.greenpole.hibernate.entity.RightsIssue> ri_hib_list = hd.getRightsIssue(queryParams.getRightsIssue().getClientCompanyId(), queryParams.getDescriptor(), queryParams.getStart_date(), queryParams.getEnd_date(), queryParams.getDate_format());
                //List<org.greenpole.hibernate.entity.IpoApplication> ipo_list = hd.getAllIpoApplication(ipo_hib.getId(), queryParams.getInitialPublicOffer().getClientCompanyId());
                int totalSharesSubscribed = 0, sharesDistributed = 0, totalSharesDistributed = 0;
                int remaingShares = 0, amountOfTotalSharesPaid = 0, totalSharesOnOffer = 0;
                List<RightsIssueApplication> riApp_list_out = new ArrayList<>();
                List<RightsIssue> ri_list_out = new ArrayList<>();
                TagUser tag = new TagUser();
                for (org.greenpole.hibernate.entity.RightsIssue ri : ri_hib_list) {
                    RightsIssue riModel = new RightsIssue();
                    RightsIssueApplication riApp_model = new RightsIssueApplication();
                    riModel.setTotalSharesOnIssue(ri.getTotalSharesOnIssue());
                    totalSharesOnOffer = ri.getTotalSharesOnIssue();
                    List<org.greenpole.hibernate.entity.RightsIssueApplication> rightsIssueApp_hib_list = hd.getAllRightsIssueApplications(ri.getId(), queryParams.getRightsIssue().getClientCompanyId());
                    for (org.greenpole.hibernate.entity.RightsIssueApplication riApp : hd.getAllRightsIssueApplications(ri.getId(), queryParams.getRightsIssue().getClientCompanyId())) {
                        org.greenpole.hibernate.entity.ClearingHouse ch_hib = hd.getClearingHouse(riApp.getClearingHouse().getId());
                        totalSharesSubscribed += riApp.getSharesSubscribed();
                        totalSharesDistributed += riApp.getAllottedRights() + riApp.getAdditionalSharesGiven();//total shares given out to holders
                        remaingShares = totalSharesOnOffer - totalSharesDistributed;
                        amountOfTotalSharesPaid += riApp.getAmountPaid();
                        //sharesOverSub = ri.getTotalSharesOnIssue() - totalSharesSubscribed;
                        riModel.setTotalSharesOnIssue(ri.getTotalSharesOnIssue());
                        riModel.setTotalSharesRem(remaingShares);
                        if (totalSharesSubscribed > ri.getTotalSharesOnIssue()) {
                            int overSubShares = totalSharesSubscribed - ri.getTotalSharesOnIssue();
                            riModel.setTotalSharesOnIssue(ri.getTotalSharesOnIssue());
                            riModel.setTotalSharesDistributed(totalSharesDistributed);
                            riModel.setTotalSharesPaidForAfterClose(amountOfTotalSharesPaid);
                            riModel.setTotalSharesRem(0);
                            riModel.setTotalSharesOverSub(overSubShares);
                            if (riApp.getAdditionalSharesSubscribed() == 0) {
                                riModel.setTotalSharesHolderOverSub(0);
                            } else if (riApp.getAdditionalSharesSubscribed() != 0) {
                                riModel.setTotalSharesHolderOverSub(rightsIssueApp_hib_list.size());
                            }
                            riApp_model.setClearingHouseName(ch_hib.getName());
                            riApp_model.setClearingHouseBrokerage(ri.getTotalSharesOnIssue() * ri.getIssuePrice() * 0.75 / 100);
                            riApp_list_out.add(riApp_model);
                            riModel.setRightsIssueApplication(riApp_list_out);
                        } else {
                            riModel.setTotalSharesOverSub(0);
                        }
                        riApp_model.setRightsIssueId(riModel.getId());
                    }
                }
                List<TagUser> tagList = new ArrayList<>();

                tag.setQueryParam(queryParams);
                tag.setResult(riApp_list_out);
                tagList.add(tag);

                resp.setBody(tagList);
                resp.setDesc("Query result with search parameter");
                resp.setRetn(0);
                logger.info("Query successful - [{}]", login.getUserId());
                return resp;
            }
        } catch (Exception ex) {
        }
        return resp;
    }

    /**
     *
     * @param login
     * @param queryParams
     * @return
     */
    public Response viewPrivatePlacement_Request(Login login, QueryIPORightsIssuePrivatePlacement queryParams) {
        Response resp = new Response();
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            Descriptor descriptorUtil = new Descriptor();
            Map<String, String> descriptors = descriptorUtil.decipherDescriptor(queryParams.getDescriptor());
            //check that specified date is properly formatted
            if (descriptors.size() == 1) {
                if (descriptors.get("date").equalsIgnoreCase("none")) {
                    try {
                        formatter.parse(queryParams.getStart_date());
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
                        formatter.parse(queryParams.getEnd_date());
                    } catch (ParseException ex) {
                        logger.info("an error was thrown while checking the start date. See error log - [{}]", login.getUserId());
                        resp.setRetn(300);
                        resp.setDesc("Incorrect date format for end date");
                        logger.error("Incorrect date format for end date - [{}]", login.getUserId(), ex);

                        return resp;
                    }
                }
                List<org.greenpole.hibernate.entity.PrivatePlacement> pp_hib_list = hd.getPrivatePlacement(queryParams.getPrivatePlacement().getClientCompanyId(), queryParams.getDescriptor(), queryParams.getStart_date(), queryParams.getEnd_date(), queryParams.getDate_format());
                //List<org.greenpole.hibernate.entity.IpoApplication> ipo_list = hd.getAllIpoApplication(ipo_hib.getId(), queryParams.getInitialPublicOffer().getClientCompanyId());
                int totalSharesSubscribed = 0;
                int remaingShares = 0;
                int totalSharesOnOffer = 0;
                List<PrivatePlacementApplication> ppApp_list_out = new ArrayList<>();
                TagUser tag = new TagUser();
                for (org.greenpole.hibernate.entity.PrivatePlacement pp : pp_hib_list) {
                    PrivatePlacement ppModel = new PrivatePlacement();
                    PrivatePlacementApplication ppApp_model = new PrivatePlacementApplication();
                    totalSharesOnOffer = pp.getTotalSharesOnOffer();
                    ppModel.setTotalSharesOnOffer(pp.getTotalSharesOnOffer());
                    for (org.greenpole.hibernate.entity.PrivatePlacementApplication ppApp : hd.getPrivatePlacementApplication(pp.getId(), queryParams.getPrivatePlacement().getClientCompanyId())) {
                        org.greenpole.hibernate.entity.ClearingHouse ch_hib = hd.getClearingHouse(ppApp.getClearingHouse().getId());
                        totalSharesSubscribed += ppApp.getSharesSubscribed();
                        remaingShares = totalSharesOnOffer - totalSharesSubscribed;
                        ppModel.setClientCompanyId(pp.getClientCompany().getId());
                        ppApp_model.setPrivatePlacement(ppModel);
                        ppApp_model.setClearingHouseName(ch_hib.getName());
                        ppApp_model.setClearingHouseBrokerage(pp.getTotalSharesOnOffer() * pp.getOfferPrice() * 0.75 / 100);//total values of shares submitted to clearing house
                        ppApp_list_out.add(ppApp_model);
                    }

                    // ppModel.setPrivatePlacementApplication(ppApp_list_out);
                }
                List<TagUser> tagList = new ArrayList<>();

                tag.setQueryParam(queryParams);
                tag.setResult(ppApp_list_out);
                tagList.add(tag);

                resp.setBody(tagList);
                resp.setDesc("Query result with search parameter");
                resp.setRetn(0);
                logger.info("Query successful - [{}]", login.getUserId());
                return resp;
            }
        } catch (Exception ex) {
        }
        return resp;
    }

    /**
     *
     * @param login
     * @param queryParams
     * @return
     */
    public Response queryShareholdersList_Request(Login login, QueryShareholders queryParams) {
        Response resp = new Response();
        Descriptor descriptorUtil = new Descriptor();
        try {
            List<org.greenpole.entity.model.holder.Holder> holder_model_list = new ArrayList();
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            Map<String, String> descriptors = descriptorUtil.decipherDescriptor(queryParams.getDescriptor());
            int currentYear = Calendar.getInstance().get(Calendar.YEAR); //get the current year in integer format
            if (descriptors.size() == 1) {
                org.greenpole.hibernate.entity.Holder holder_hib_search = new org.greenpole.hibernate.entity.Holder();
                org.greenpole.hibernate.entity.Administrator holderAdmin_serach_hib = new org.greenpole.hibernate.entity.Administrator();
                org.greenpole.hibernate.entity.HolderCompanyAccount hca_search_hib = new org.greenpole.hibernate.entity.HolderCompanyAccount();
                org.greenpole.hibernate.entity.HolderCompanyAccountId hca_Id_search_hib = new org.greenpole.hibernate.entity.HolderCompanyAccountId();
                org.greenpole.hibernate.entity.HolderEmailAddress holder_email_search_hib = new org.greenpole.hibernate.entity.HolderEmailAddress();
                org.greenpole.hibernate.entity.HolderPhoneNumber holder_phone_hib = new org.greenpole.hibernate.entity.HolderPhoneNumber();
                org.greenpole.hibernate.entity.HolderPostalAddress holder_postal_hib = new org.greenpole.hibernate.entity.HolderPostalAddress();
                org.greenpole.hibernate.entity.HolderResidentialAddress holder_residential_hib = new org.greenpole.hibernate.entity.HolderResidentialAddress();
                org.greenpole.hibernate.entity.Stockbroker broker_hib_search = new org.greenpole.hibernate.entity.Stockbroker();
                Holder h_model_search = new Holder();
                List<org.greenpole.hibernate.entity.Holder> holder_hib_list = hd.getHoldersByClientCompanyId(queryParams.getClientCompanyId());
                int shareHolderAge;
                if (queryParams.getHolder() != null) {
                    h_model_search = queryParams.getHolder();
                    holder_hib_search.setGender(h_model_search.getGender());
                    if (queryParams.getStartAge() > 0 && queryParams.getEndAge() > 0) {
                        for (org.greenpole.hibernate.entity.Holder h : holder_hib_list) {
                            Date dob = h.getDob();
                            shareHolderAge = currentYear - this.getBirthYear(dob);
                            if (shareHolderAge >= queryParams.getStartAge() && shareHolderAge <= queryParams.getEndAge()) {
                                try {
                                    holder_hib_search.setDob(dob);
                                } catch (Exception ex) {
                                    logger.info("an error was thrown while checking the start date. See error log - [{}]", login.getUserId());
                                    resp.setRetn(300);
                                    resp.setDesc("Incorrect date format for date of birth");
                                    logger.error("Incorrect date format for date of birth - [{}]", login.getUserId(), ex);

                                    return resp;
                                }
                            }
                        }
                    }
                }
                Address h_resAddress_model_search = new Address();
                if (queryParams.getHolder().getResidentialAddresses() != null && queryParams.getHolder().getResidentialAddresses().isEmpty()) {
                    h_resAddress_model_search = queryParams.getHolder().getResidentialAddresses().get(0);
                    holder_residential_hib.setAddressLine1(h_resAddress_model_search.getAddressLine1());
                    holder_residential_hib.setCountry(h_resAddress_model_search.getCountry());
                    holder_residential_hib.setState(h_resAddress_model_search.getState());
                    holder_residential_hib.setAddressLine2(h_resAddress_model_search.getAddressLine2());
                    holder_residential_hib.setAddressLine3(h_resAddress_model_search.getAddressLine3());
                    holder_residential_hib.setAddressLine4(h_resAddress_model_search.getAddressLine4());
                    holder_residential_hib.setCity(h_resAddress_model_search.getCity());
                    holder_residential_hib.setPostCode(h_resAddress_model_search.getPostCode());
                    Set h_resAddy_set = new HashSet();
                    h_resAddy_set.add(holder_residential_hib);
                    holder_hib_search.setHolderResidentialAddresses(h_resAddy_set);//put holder residential address set in holder entity
                }
                Stockbroker broker_search;
                if (queryParams.getHolder().getHolderStockbroker() != null) {
                    broker_search = queryParams.getHolder().getHolderStockbroker();

                    broker_hib_search.setName(broker_search.getName());

                    Set broker_set = new HashSet();
                    broker_set.add(broker_hib_search);

                    holder_hib_search.setStockbrokers(broker_set);
                }
                List<org.greenpole.hibernate.entity.Holder> holderhib_list = hd.queryShareholders(queryParams.getDescriptor(), queryParams.getClientCompanyId(), queryParams.getStartAge(), queryParams.getEndAge());
                logger.info("retrieved shareholders result from query. - [{}]", login.getUserId());
                List<Address> residential_addy_list_out = new ArrayList<>();//hold list of residential address
                List<PhoneNumber> h_model_phone_list_out = new ArrayList<>();
                for (org.greenpole.hibernate.entity.Holder holder : holder_hib_list) {
                    org.greenpole.entity.model.holder.Holder holder_model = new org.greenpole.entity.model.holder.Holder();
                    holder_model.setHolderId(holder.getId());
                    holder_model.setGender(holder.getGender());
                    holder_model.setDob(formatter.format(holder.getDob()));
                    List<org.greenpole.hibernate.entity.HolderResidentialAddress> hra_out_list = hd.getHolderResidentialAddresses(holder.getId());
                    for (org.greenpole.hibernate.entity.HolderResidentialAddress hra : hra_out_list) {
                        Address residential_addy_out = new Address();
                        residential_addy_out.setAddressLine1(hra.getAddressLine1());
                        residential_addy_out.setState(hra.getState());
                        residential_addy_out.setCountry(hra.getCountry());
                        residential_addy_out.setAddressLine2(hra.getAddressLine2());
                        residential_addy_out.setAddressLine3(hra.getAddressLine3());
                        residential_addy_out.setAddressLine4(hra.getAddressLine4());
                        residential_addy_out.setCity(hra.getCity());
                        residential_addy_out.setPostCode(hra.getPostCode());
                        residential_addy_out.setPrimaryAddress(hra.getIsPrimary());
                        residential_addy_list_out.add(residential_addy_out);
                    }
                    holder_model.setResidentialAddresses(residential_addy_list_out);
                    Stockbroker broke = new Stockbroker();
                    List<Stockbroker> broke_model_list_out = new ArrayList<>();
                    List<org.greenpole.hibernate.entity.Stockbroker> broker_hib_list = hd.getStockBrokers(holder.getId());
                    for (org.greenpole.hibernate.entity.Stockbroker st : broker_hib_list) {
                        broke.setName(st.getName());
                        //broke_model_list_out.add(broke); 
                    }
                    holder_model.setHolderStockbroker(broke);
                    List<HolderCompanyAccount> hca_model_list_out = new ArrayList<>();
                    List<org.greenpole.hibernate.entity.HolderCompanyAccount> hca_hib_list = hd.getAllHolderCompanyAccountsByClientCompanyId(queryParams.getClientCompanyId());
                    for (org.greenpole.hibernate.entity.HolderCompanyAccount hca : hca_hib_list) {
                        HolderCompanyAccount hca_model = new HolderCompanyAccount();
                        hca_model.setClientCompanyId(hca.getClientCompany().getId());
                        hca_model.setClientCompanyName(hca.getClientCompany().getName());
                        hca_model.setEsop(hca.getEsop());
                        hca_model.setHolderCompAccPrimary(hca.getEsop());
                        hca_model.setHolderId(hca.getHolder().getId());
                        hca_model.setMerged(hca.getMerged());
                        hca_model.setNubanAccount(hca.getNubanAccount());
                        hca_model.setShareUnits(hca.getShareUnits());
                        hca_model_list_out.add(hca_model);
                    }
                    holder_model.setCompanyAccounts(hca_model_list_out);
                    holder_model_list.add(holder_model);
                }
                resp.setBody(holder_model_list);
                resp.setDesc("Query result with search parameter");
                resp.setRetn(0);
                logger.info("Query successful - [{}]", login.getUserId());
                return resp;
            }
            logger.info("descriptor length does not match expected required length - [{}]", login.getUserId());
            resp.setRetn(200);
            resp.setDesc("descriptor length does not match expected required length");
            return resp;
        } catch (Exception ex) {
            logger.info("error querying shareholders list. See error log - [{}]", login.getUserId());
            logger.error("error querying shareholders - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to querying shareholders. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     *
     * @param login
     * @param queryParams
     * @return
     */
    public Response queryBondholdersList_Request(Login login, QueryShareholders queryParams) {
        Response resp = new Response();
        Descriptor descriptorUtil = new Descriptor();
        try {
            List<org.greenpole.entity.model.holder.Holder> holder_model_list = new ArrayList();
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            Map<String, String> descriptors = descriptorUtil.decipherDescriptor(queryParams.getDescriptor());
            int currentYear = Calendar.getInstance().get(Calendar.YEAR); //get the current year in integer format
            if (descriptors.size() == 1) {
                org.greenpole.hibernate.entity.Holder holder_hib_search = new org.greenpole.hibernate.entity.Holder();
                org.greenpole.hibernate.entity.HolderResidentialAddress holder_residential_hib = new org.greenpole.hibernate.entity.HolderResidentialAddress();
                org.greenpole.hibernate.entity.Stockbroker broker_hib_search = new org.greenpole.hibernate.entity.Stockbroker();
                Holder h_model_search = new Holder();
                List<org.greenpole.hibernate.entity.Holder> holder_hib_list = hd.getHoldersByClientCompanyId(queryParams.getClientCompanyId());
                int shareHolderAge;
                if (queryParams.getHolder() != null) {
                    h_model_search = queryParams.getHolder();
                    holder_hib_search.setGender(h_model_search.getGender());
                    if (queryParams.getStartAge() > 0 && queryParams.getEndAge() > 0) {
                        for (org.greenpole.hibernate.entity.Holder h : holder_hib_list) {
                            Date dob = h.getDob();
                            shareHolderAge = currentYear - this.getBirthYear(dob);
                            if (shareHolderAge >= queryParams.getStartAge() && shareHolderAge <= queryParams.getEndAge()) {
                                try {
                                    holder_hib_search.setDob(dob);
                                } catch (Exception ex) {
                                    logger.info("an error was thrown while checking the start date. See error log - [{}]", login.getUserId());
                                    resp.setRetn(300);
                                    resp.setDesc("Incorrect date format for date of birth");
                                    logger.error("Incorrect date format for date of birth - [{}]", login.getUserId(), ex);

                                    return resp;
                                }
                            }
                        }
                    }
                }
                Address h_resAddress_model_search = new Address();
                if (queryParams.getHolder().getResidentialAddresses() != null && queryParams.getHolder().getResidentialAddresses().isEmpty()) {
                    h_resAddress_model_search = queryParams.getHolder().getResidentialAddresses().get(0);
                    holder_residential_hib.setAddressLine1(h_resAddress_model_search.getAddressLine1());
                    holder_residential_hib.setCountry(h_resAddress_model_search.getCountry());
                    holder_residential_hib.setState(h_resAddress_model_search.getState());
                    holder_residential_hib.setAddressLine2(h_resAddress_model_search.getAddressLine2());
                    holder_residential_hib.setAddressLine3(h_resAddress_model_search.getAddressLine3());
                    holder_residential_hib.setAddressLine4(h_resAddress_model_search.getAddressLine4());
                    holder_residential_hib.setCity(h_resAddress_model_search.getCity());
                    holder_residential_hib.setPostCode(h_resAddress_model_search.getPostCode());
                    Set h_resAddy_set = new HashSet();
                    h_resAddy_set.add(holder_residential_hib);
                    holder_hib_search.setHolderResidentialAddresses(h_resAddy_set);//put holder residential address set in holder entity
                }
                Stockbroker broker_search;
                if (queryParams.getHolder().getHolderStockbroker() != null) {
                    broker_search = queryParams.getHolder().getHolderStockbroker();

                    broker_hib_search.setName(broker_search.getName());

                    Set broker_set = new HashSet();
                    broker_set.add(broker_hib_search);

                    holder_hib_search.setStockbrokers(broker_set);
                }
                List<org.greenpole.hibernate.entity.Holder> holderhib_list = hd.queryShareholders(queryParams.getDescriptor(), queryParams.getClientCompanyId(), queryParams.getStartAge(), queryParams.getEndAge());
                logger.info("retrieved shareholders result from query. - [{}]", login.getUserId());
                List<Address> residential_addy_list_out = new ArrayList<>();//hold list of residential address
                List<PhoneNumber> h_model_phone_list_out = new ArrayList<>();
                for (org.greenpole.hibernate.entity.Holder holder : holder_hib_list) {
                    org.greenpole.entity.model.holder.Holder holder_model = new org.greenpole.entity.model.holder.Holder();
                    holder_model.setHolderId(holder.getId());
                    holder_model.setGender(holder.getGender());
                    holder_model.setDob(formatter.format(holder.getDob()));
                    List<org.greenpole.hibernate.entity.HolderResidentialAddress> hra_out_list = hd.getHolderResidentialAddresses(holder.getId());
                    for (org.greenpole.hibernate.entity.HolderResidentialAddress hra : hra_out_list) {
                        Address residential_addy_out = new Address();
                        residential_addy_out.setAddressLine1(hra.getAddressLine1());
                        residential_addy_out.setState(hra.getState());
                        residential_addy_out.setCountry(hra.getCountry());
                        residential_addy_out.setAddressLine2(hra.getAddressLine2());
                        residential_addy_out.setAddressLine3(hra.getAddressLine3());
                        residential_addy_out.setAddressLine4(hra.getAddressLine4());
                        residential_addy_out.setCity(hra.getCity());
                        residential_addy_out.setPostCode(hra.getPostCode());
                        residential_addy_out.setPrimaryAddress(hra.getIsPrimary());
                        residential_addy_list_out.add(residential_addy_out);
                    }
                    holder_model.setResidentialAddresses(residential_addy_list_out);
                    Stockbroker broke = new Stockbroker();
                    List<Stockbroker> broke_model_list_out = new ArrayList<>();
                    List<org.greenpole.hibernate.entity.Stockbroker> broker_hib_list = hd.getStockBrokers(holder.getId());
                    for (org.greenpole.hibernate.entity.Stockbroker st : broker_hib_list) {
                        broke.setName(st.getName());
                        //broke_model_list_out.add(broke); 
                    }
                    holder_model.setHolderStockbroker(broke);
                    List<HolderBondAccount> hba_model_list_out = new ArrayList<>();
                    List<org.greenpole.hibernate.entity.HolderBondAccount> bondAcc_hib_list = hd.getAllBondsAccountByClientCompanyId(queryParams.getClientCompanyId());
                    for (org.greenpole.hibernate.entity.HolderBondAccount hba : bondAcc_hib_list) {
                        HolderBondAccount hba_model = new HolderBondAccount();
                        hba_model.setBondOfferId(hba.getBondOffer().getId());
                        hba_model.setBondOfferTitle(hba.getBondOffer().getTitle());
                        hba_model.setBondUnits(hba.getBondUnits());
                        hba_model.setHolderBondAccPrimary(hba.getHolderBondAcctPrimary());
                        hba_model.setHolderId(hba.getHolder().getId());
                        hba_model.setMerged(hba.getMerged());
                        hba_model.setNubanAccount(hba.getNubanAccount());
                        hba_model.setRemainingPrincipalValue(hba.getRemainingPrincipalValue());
                        hba_model.setStartingPrincipalValue(hba.getStartingPrincipalValue());
                        hba_model_list_out.add(hba_model);
                    }
                    holder_model.setBondAccounts(hba_model_list_out);
                    holder_model_list.add(holder_model);
                }
                resp.setBody(holder_model_list);
                resp.setDesc("Query result with search parameter");
                resp.setRetn(0);
                logger.info("Query successful - [{}]", login.getUserId());
                return resp;
            }
            logger.info("descriptor length does not match expected required length - [{}]", login.getUserId());
            resp.setRetn(200);
            resp.setDesc("descriptor length does not match expected required length");
            return resp;
        } catch (Exception ex) {
            logger.info("error querying shareholders list. See error log - [{}]", login.getUserId());
            logger.error("error querying shareholders - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to querying shareholders. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     *
     * @param login the user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param rightApp
     * @return
     */
    public Response ApplyAdditionalSharesToShareholders_Request(Login login, String authenticator, RightsIssueApplication rightApp) {
        logger.info("request to apply additional shares to shareholders, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        try {
            long millis = System.currentTimeMillis();
            Date current_date = new java.sql.Date(millis);
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());

            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;

            boolean rightsIssueExist = hd.checkRightsIssue(rightApp.getRightsIssueId(), rightApp.getClientCompanyId());
            org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(rightApp.getClientCompanyId());
            if (rightsIssueExist) {
                logger.info("rights issue exist for client company [{}], invoked by [{}]", cc.getName(), login.getUserId());
                org.greenpole.hibernate.entity.RightsIssue rightIssue = hd.getRightsIssueById(rightApp.getRightsIssueId(), rightApp.getClientCompanyId());
                if (rightIssue.getRightsClosed() && rightIssue.getClosingDate().before(current_date)) {
                    wrapper = new NotificationWrapper();
                    prop = new NotifierProperties(ClientCompanyLogic.class);
                    qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                            prop.getAuthoriserNotifierQueueName());
                    List<RightsIssueApplication> rightApp_list = new ArrayList<>();
                    rightApp_list.add(rightApp);
                    wrapper.setCode(notification.createCode(login));
                    wrapper.setDescription("Authenticate adding of additional shares to shareholder by" + login.getUserId());
                    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                    wrapper.setFrom(login.getUserId());
                    wrapper.setTo(authenticator);
                    wrapper.setModel(rightApp_list);
                    logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                    resp = qSender.sendAuthorisationRequest(wrapper);
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Sorry, transaction suspended because rights issue is still on going");
                logger.info("Transaction suspended because rights issue is still on going - [{}] ", login.getUserId());
                return resp;

            }
            resp.setRetn(200);
            resp.setDesc("Rights issue not found for client company " + cc.getName());
            logger.info("Rights issue not found for client company - [{}] invoked by [{}] ", cc.getName(), login.getUserId());
            return resp;

        } catch (Exception ex) {
            logger.info("error proccessing addition of shares to shareholders. See error log - [{}]", login.getUserId());
            logger.error("error proccessing addition of shares to shareholders - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to addition of shares to shareholders. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    public Response ApplyAdditionalSharesToShareholders_Authorise(Login login, String notificationCode) {
        logger.info("request authorisation to store holder additional shares. Invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        try {
            long millis = System.currentTimeMillis();
            Date current_date = new java.sql.Date(millis);
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<RightsIssueApplication> rightsList = (List<RightsIssueApplication>) wrapper.getModel();
            RightsIssueApplication rightApp_model = rightsList.get(0);
            List<org.greenpole.hibernate.entity.RightsIssueApplication> rightApp_list_hib = hd.getHoldersRightsIssueApplicationByClientCompany(rightApp_model.getClientCompanyId());
            boolean rightsIssueExist = hd.checkRightsIssue(rightApp_model.getRightsIssueId(), rightApp_model.getClientCompanyId());
            org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(rightApp_model.getClientCompanyId());
            org.greenpole.hibernate.entity.RightsIssue rightsIssueSetup = hd.getRightsIssueById(rightApp_model.getRightsIssueId(), rightApp_model.getClientCompanyId());
            if (rightsIssueExist) {
                logger.info("rights issue exist for client company [{}], invoked by [{}]", cc.getName(), login.getUserId());
                org.greenpole.hibernate.entity.RightsIssue rightIssue = hd.getRightsIssueById(rightApp_model.getRightsIssueId(), rightApp_model.getClientCompanyId());
                if (rightIssue.getClosingDate().equals(current_date) || rightIssue.getClosingDate().after(current_date)) {//remove from here
                    if (!rightApp_list_hib.isEmpty()) {
                        int totalAdditionaSharesRequested = 0, sumOfSharesGivenToHolder;
                        int holderUnallottedRights;
                        double valueOfHolderUnallotedRights, interest, taxRate, returnMoney;
                        int sunHoldersAllottedRights = 0;
                        int totalSharesSub = 0;
                        int sumHolderAdditionalShares = 0;
                        int compRemainingRights = 0;
                        int lowestAdditionalShares;
                        int sumTotalAdditionalShares = 0;
                        int eachHolderShares, noOfHolders = 0;
                        int holderId;
                        int holderSharesSub = 0, holderSharesSubCompare = 0;
                        int avg1 = 0, avg2 = 0, excess = 0;
                        int noOfShareHoldersLessThanAvg = 0, newShareholderList = 0, extraExcess = 0;

                       
                        double holderShares;//holds shares given to shareholders after distribution
                        long noOfDays;
                        boolean sharesAdded = false;
                        int totalSharesOnOIssue;
                        int remainingSharesAfterSub;
                        org.greenpole.hibernate.entity.HolderCompanyAccount hca_hib = new org.greenpole.hibernate.entity.HolderCompanyAccount();
                        noOfHolders = rightApp_list_hib.size();
                        for (org.greenpole.hibernate.entity.RightsIssueApplication ri : rightApp_list_hib) {
                            totalSharesOnOIssue = rightsIssueSetup.getTotalSharesOnIssue();
                            totalAdditionaSharesRequested += ri.getAdditionalSharesSubscribed();
                            totalSharesSub += ri.getSharesSubscribed();
                            compRemainingRights = totalSharesOnOIssue - totalSharesSub;//get company remaing shares
                            avg1 = (avg1 + compRemainingRights) / noOfHolders; //divides company remaining shares by no of shareholders
                            excess = compRemainingRights - (avg1 * noOfHolders);//get remaining shares after distribution
                            org.greenpole.hibernate.entity.RightsIssueApplication oneHolderRightsApp_hib = new org.greenpole.hibernate.entity.RightsIssueApplication();
                            List<org.greenpole.hibernate.entity.HolderCompanyAccount> hca_list_hib = hd.getAllHolderCompanyAccountsByClientCompanyId(rightApp_model.getClientCompanyId());
                            if (compRemainingRights > totalAdditionaSharesRequested) {//company has enough shares to give shareholders
                                if (!hca_list_hib.isEmpty() && ri.getAdditionalSharesSubscribed() > 0) {
                                    for (org.greenpole.hibernate.entity.HolderCompanyAccount hca : hca_list_hib) {
                                        hd.updateHCA(hca.getHolder().getId(), rightApp_model.getAdditionalSharesSubscribed());//adds shares given to holder existing shares
                                        org.greenpole.hibernate.entity.RightsIssueApplication oneHolderRightApplication = hd.getOneHolderRightApplication(hca.getHolder().getId(), hca.getClientCompany().getId(), ri.getRightsIssue().getId());
                                        oneHolderRightApplication.setAdditionalSharesGiven(rightApp_model.getAdditionalSharesSubscribed());
                                        oneHolderRightApplication.setAdditionalSharesGivenValue(rightApp_model.getAdditionalSharesSubscribed() * rightIssue.getIssuePrice());
                                        oneHolderRightApplication.setReturnMoney(0.0);
                                        hd.updateShareholderRightsIssueApplication(ri.getHolder().getId(), rightIssue.getClientCompany().getId(), rightIssue.getId());
                                        remainingSharesAfterSub = rightIssue.getTotalSharesOnIssue() - ri.getAdditionalSharesSubscribed();
                                        rightIssue.setTotalSharesOnIssue(remainingSharesAfterSub);
                                        hd.updateRightIssueTotalShares(ri.getHolder().getId(), ri.getRightsIssue().getClientCompany().getId());
                                    }
                                    notification.markAttended(notificationCode);
                                    resp.setRetn(0);
                                    resp.setDesc("Successfully added additional shares to shareholders account");
                                    logger.info("Successfully added additional shares to shareholders account ", login.getUserId());
                                    return resp;
                                }
                                resp.setRetn(200);
                                resp.setDesc("No holder company account found under the client company");
                                logger.info("No holder company account found under the client company");
                                return resp;
                            } else if (compRemainingRights < totalAdditionaSharesRequested) {
                                if (!hca_list_hib.isEmpty() && ri.getAdditionalSharesSubscribed() != 0) {
                                    for (org.greenpole.hibernate.entity.HolderCompanyAccount hca : hca_list_hib) {
                                        hca_hib = hd.getOneHolderCompanyAccount(hca.getHolder().getId(), hca.getClientCompany().getId());
                                        //}
                                        String distributionType = "";
                                        if (distributionType.equals("even")) {
                                            holderSharesSub = ri.getAdditionalSharesSubscribed();
                                            holderSharesSubCompare = ri.getAdditionalSharesSubscribed();
                                            oneHolderRightsApp_hib = hd.getOneHolderRightApplication(ri.getHolder().getId(), rightIssue.getClientCompany().getId(), rightIssue.getId());
                                            if (avg1 >= oneHolderRightsApp_hib.getAdditionalSharesSubscribed()) {//compare additional shares requested with average
                                                holderSharesSub = holderSharesSubCompare;
                                                excess = excess + (avg1 - holderSharesSubCompare);
                                                holderSharesSubCompare = 0;
                                                noOfShareHoldersLessThanAvg++;//number of holders that get this average
                                                newShareholderList = noOfHolders - noOfShareHoldersLessThanAvg;
                                                oneHolderRightsApp_hib.setAdditionalSharesGiven(holderSharesSub);
                                                oneHolderRightsApp_hib.setAdditionalSharesGivenValue(holderSharesSub * rightsIssueSetup.getIssuePrice());
                                                oneHolderRightsApp_hib.setTotalValue((holderSharesSub * rightsIssueSetup.getIssuePrice()) + (oneHolderRightsApp_hib.getSharesSubscribed() * rightsIssueSetup.getIssuePrice()));
                                            } else if (avg1 <= oneHolderRightsApp_hib.getAdditionalSharesSubscribed()) {//if additional shares requested is the same as shares given
                                                avg2 = excess / newShareholderList;//get average from the excess given to holders who got more than requested shares
                                                holderSharesSub = avg1 + avg2;
                                                if (holderSharesSub > holderSharesSubCompare) {
                                                    extraExcess = holderSharesSub - holderSharesSubCompare;
                                                    holderSharesSub = holderSharesSubCompare;
                                                }
                                                holderSharesSubCompare = holderSharesSubCompare - holderSharesSub;
                                                holderUnallottedRights = (oneHolderRightsApp_hib.getAdditionalSharesSubscribed() - holderSharesSub);
                                                valueOfHolderUnallotedRights = holderUnallottedRights * rightsIssueSetup.getIssuePrice();
                                                noOfDays = getDateDiff(current_date, oneHolderRightsApp_hib.getDateApplied(), TimeUnit.DAYS);
                                                interest = (valueOfHolderUnallotedRights * rightApp_model.getRate() * noOfDays) / 100;
                                                taxRate = rightApp_model.getTaxRate() * interest;
                                                returnMoney = valueOfHolderUnallotedRights + (interest - taxRate);
                                                oneHolderRightsApp_hib.setAdditionalSharesGiven(holderSharesSub);
                                                oneHolderRightsApp_hib.setAdditionalSharesGivenValue(holderSharesSub * rightsIssueSetup.getIssuePrice());
                                                oneHolderRightsApp_hib.setTotalValue((holderSharesSub * rightsIssueSetup.getIssuePrice()) + (oneHolderRightsApp_hib.getAllottedRights() * rightsIssueSetup.getIssuePrice()));
                                                oneHolderRightsApp_hib.setReturnMoney(returnMoney);
                                                hd.updateShareholderRightsIssueApplication(ri.getHolder().getId(), ri.getRightsIssue().getClientCompany().getId(), ri.getRightsIssue().getId());
                                                remainingSharesAfterSub = rightIssue.getTotalSharesOnIssue() - holderSharesSub;
                                                rightIssue.setTotalSharesOnIssue(remainingSharesAfterSub);
                                                hd.updateRightIssueTotalShares(ri.getHolder().getId(), ri.getRightsIssue().getClientCompany().getId());
                                                hd.updateHCA(hca.getHolder().getId(), holderSharesSub);//adds shares given to holder existing shares
                                                resp.setRetn(0);
                                                resp.setDesc("Successfully added additional shares to shareholders account");
                                                logger.info("Successfully added additional shares to shareholders account ", login.getUserId());
                                                return resp;
                                            }

                                        } else if (distributionType.equals("percentage")) {
                                            double sharesNotGiven, sharesNotGivenValue;
                                            for (org.greenpole.hibernate.entity.HolderCompanyAccount hc : hca_list_hib) {
                                                org.greenpole.hibernate.entity.RightsIssueApplication ria = new org.greenpole.hibernate.entity.RightsIssueApplication();
                                                org.greenpole.hibernate.entity.HolderCompanyAccount h = hd.getHolderCompanyAccount(hc.getHolder().getId());
                                                oneHolderRightsApp_hib = hd.getOneHolderRightApplication(ri.getHolder().getId(), rightIssue.getClientCompany().getId(), rightIssue.getId());
                                                holderShares = Math.floor((double) ((ri.getAdditionalSharesSubscribed() * 100) / totalAdditionaSharesRequested) * (compRemainingRights / 100));
                                                sharesNotGiven = ri.getAdditionalSharesSubscribed() - holderShares;
                                                sharesNotGivenValue = sharesNotGiven * rightsIssueSetup.getIssuePrice();
                                                oneHolderRightsApp_hib.setAdditionalSharesGiven((int) holderShares);
                                                oneHolderRightsApp_hib.setAdditionalSharesGivenValue(holderShares * rightsIssueSetup.getIssuePrice());
                                                //oneHolderRightsApp_hib.setTotalHoldings(noOfHolders);
                                                oneHolderRightsApp_hib.setReturnMoney(sharesNotGivenValue);
                                                hd.updateShareholderRightsIssueApplication(ri.getHolder().getId(), rightIssue.getClientCompany().getId(), rightIssue.getId());
                                                hd.updateRightIssueTotalShares(ri.getHolder().getId(), ri.getRightsIssue().getClientCompany().getId());
                                                hd.updateHCA(hca.getHolder().getId(), holderSharesSub);//adds shares given to holder existing shares
                                            }
                                            resp.setRetn(0);
                                            resp.setDesc("Successful");
                                            return resp;
                                        }
                                    }//remove from here2
                                    //end here now
                                }
                                resp.setRetn(200);
                                resp.setDesc("No holder company account found for this client company or no additional shares requested");
                                logger.info("No holder company account found for this client company or no additional shares requested");
                                return resp;
                            }

                        }//end of first for loop //remove from here
                    }
                    resp.setRetn(200);
                    resp.setDesc("No subscribers found for this rights issue");
                    logger.info("No subscribers found for this rights issue " + login.getUserId());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Rights issue is still on going");
                logger.info("Rights issue is still on going " + login.getUserId());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Rights issue does not exists for the client company");
            logger.info("Rights issue does not exists for the client company " + login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error adding additional share units to shareholders. See error log - [{}]", login.getUserId());
            logger.error("error adding additional share units to shareholders - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable add additional share units to shareholders accounts. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    private static long getDateDiff(Date currentDate, Date dateApplied, TimeUnit timeUnit) {
        long diffInDays = currentDate.getTime() - dateApplied.getTime();
        return timeUnit.convert(diffInDays, TimeUnit.MILLISECONDS);
    }

    public int getBirthYear(Date holderDob) {
        String[] dates = holderDob.toString().split("-"); //split the date string around matches of the given regular "-"
        int year = 0;
        for (String dateToken : dates) {
            if (dateToken.length() == 4) {      //check where the length of the figure is 4
                year = Integer.parseInt(dateToken); //convert the string to int
            }
        }
        return year;
    }
}
