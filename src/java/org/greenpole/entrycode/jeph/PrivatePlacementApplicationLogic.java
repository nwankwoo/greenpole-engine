/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph;

import org.greenpole.entirycode.jeph.model.ConfirmationDetails;
import org.greenpole.entirycode.jeph.model.PrivatePlacementApplication;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.model.Address;
import org.greenpole.entity.model.EmailAddress;
import org.greenpole.entity.model.PhoneNumber;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.entity.tags.AddressTag;
import org.greenpole.hibernate.entity.ClearingHouse;
import org.greenpole.hibernate.entity.Holder;
import org.greenpole.hibernate.entity.HolderCompanyAccount;
import org.greenpole.hibernate.entity.HolderCompanyAccountId;
import org.greenpole.hibernate.entity.HolderEmailAddress;
import org.greenpole.hibernate.entity.HolderPhoneNumber;
import org.greenpole.hibernate.entity.HolderPostalAddress;
import org.greenpole.hibernate.entity.HolderResidentialAddress;
import org.greenpole.hibernate.entity.HolderType;
import org.greenpole.hibernate.entity.PrivatePlacement;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.HolderComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.hibernate.query.impl.HolderComponentQueryImpl;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.Notification;
import org.greenpole.util.properties.GreenpoleProperties;
import org.greenpole.util.properties.NotificationProperties;
import org.greenpole.util.properties.NotifierProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jephthah Sadare
 */
public class PrivatePlacementApplicationLogic {

    private final HolderComponentQuery hcq = new HolderComponentQueryImpl();
    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();
    private final NotificationProperties notificationProp = NotificationProperties.getInstance();
    private final GreenpoleProperties greenProp = GreenpoleProperties.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(PrivatePlacementApplicationLogic.class);
    SimpleDateFormat formatter = new SimpleDateFormat();

    /**
     * Processes request to validate Private Placement application before
     * creation
     * @param login the user's login details
     * @param ppApply the Private Placement application object to be validated
     * @return response to the Private Placement validation request
     */
    public Response privatePlacementApplication_Request(Login login, PrivatePlacementApplication ppApply) {
        logger.info("Request to create Private Placement application of [{}] for [{}], invoked by [{}]", ppApply.getIssuer(), ppApply.getHolderId(), login.getUserId());
        Response resp = new Response();
        Date date = new Date();

        double amtPaid = 0;
        long continuingShares = 0;
        long remainingSharesNotBought = 0;
        long totalSharesBought = 0;
        long continuingSharesAvailable = 0;
        long continuingSharesSubscribed = 0;
        long subscribedShares = 0;
        double returnMoney = 0;
        double sharesSubscribedValue = 0;
        List<PrivatePlacementApplication> ppAppList = new ArrayList<>();

        try {
            if (hcq.checkHolderAccount(ppApply.getHolderId())) {// check if holder has company account with a particular client company                
                if (true) {// check if there is open private placement// check if holder has company account with a particular client company                    
                    // booleand openPP = cq.checkOpenPrivatePlacement(ppApply.getPrivatePlacementId());
                    if (true) {// checks if private placement is still opened 
                        // PrivatePlacement pp = hcq.getPrivatePlacement(ppApply.getPrivatePlacementId());
                        PrivatePlacement pp = new PrivatePlacement();
                        if (date.before(pp.getClosingDate())) {
                            if (ppApply.getSharesSubscribed() >= pp.getStartingMinSubscrptn()) {
                                continuingSharesSubscribed = ppApply.getSharesSubscribed() - pp.getStartingMinSubscrptn();
                                if (continuingSharesSubscribed % pp.getContinuingMinSubscrptn() == 0) {
                                    amtPaid = ppApply.getSharesSubscribed() * pp.getOfferPrice();
                                    if (ppApply.getAmountPaid() == amtPaid) {
                                        // List<org.greenpole.hibernate.entity.PrivatePlacementApplication> ppList = hcq.getPrivatePlacementApplication(ppApply.getPrivatePlacementId());
                                        List<org.greenpole.hibernate.entity.PrivatePlacementApplication> ppList = new ArrayList<>();
                                        for (org.greenpole.hibernate.entity.PrivatePlacementApplication ppApp : ppList) {
                                            totalSharesBought += ppApp.getSharesSubscribed();
                                        }
                                        remainingSharesNotBought = pp.getTotalSharesOnOffer() - totalSharesBought;
                                        if (ppApply.getSharesSubscribed() <= remainingSharesNotBought) {
                                            sharesSubscribedValue = ppApply.getSharesSubscribed() * pp.getOfferPrice();
                                            ppApply.setSharesSubscribedValue(sharesSubscribedValue);
                                            ppApply.setDateApplied(date.toString());
                                            ppAppList.add(ppApply);
                                            resp.setRetn(0);
                                            resp.setDesc("Private Placement Application Details for confirmation.");
                                            resp.setBody(ppAppList);
                                            logger.info("Private Placement Application Details for confirmation. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                            // send SMS and/or Email notification
                                            return resp;
                                        } else if ((ppApply.getSharesSubscribed() > remainingSharesNotBought) && (remainingSharesNotBought >= pp.getStartingMinSubscrptn())) {
                                            continuingSharesAvailable = remainingSharesNotBought - pp.getStartingMinSubscrptn();
                                            if (continuingSharesAvailable % pp.getContinuingMinSubscrptn() == 0) {
                                                continuingSharesSubscribed = (int) (Math.ceil(continuingSharesAvailable / pp.getContinuingMinSubscrptn()) * pp.getContinuingMinSubscrptn());
                                                subscribedShares = pp.getStartingMinSubscrptn() + continuingSharesSubscribed;
                                                sharesSubscribedValue = subscribedShares * pp.getOfferPrice();
                                                returnMoney = (ppApply.getSharesSubscribed() - subscribedShares) * pp.getOfferPrice();
                                                ppApply.setSharesSubscribedValue(sharesSubscribedValue);
                                                // ppApply.setReturnMoney(returnMoney);
                                                ppAppList.add(ppApply);
                                                String respDesc = "Private Placement Application Details for confirmation.";
                                                resp.setBody(ppAppList);
                                                // send SMS and/or Email notification
                                                resp.setRetn(200);
                                                resp.setDesc(respDesc + "\nInsufficient quantity of shares. Number of shares available is: " + subscribedShares);
                                                logger.info("Available shares is less than quantity to be bought. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                                return resp;
                                            }
                                            resp.setRetn(200);
                                            resp.setDesc("Insufficient quantity of shares. Number of shares available is: " + pp.getStartingMinSubscrptn());
                                            logger.info("Available shares is less than quantity to be bought. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                            return resp;
                                        }
                                        resp.setRetn(200);
                                        resp.setDesc("There is not enough shares to buy.");
                                        logger.info("There is not enough shares to buy. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                        return resp;
                                    }
                                    resp.setRetn(200);
                                    resp.setDesc("The amount paid for shares and the price of shares to be bought should be the same.");
                                    logger.info("The amount paid for shares and the price of shares to be bought should be the same. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                    return resp;
                                }
                                resp.setRetn(200);
                                resp.setDesc("Continuing shares should be in multiples of " + pp.getContinuingMinSubscrptn());
                                logger.info("Continuing shares is not in multiples of continuing minimum subscription - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                return resp;
                            }
                            resp.setRetn(200);
                            resp.setDesc("Shares subscribed is less than minimum subscription required and so holder cannot apply for Private Placement.");
                            logger.info("Shares subscribed is less than minimum subscription required and so holder cannot apply for Private Placement. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                            return resp;
                        }
                        resp.setRetn(200);
                        resp.setDesc("Application for Private placement is past closing date and so holder cannot apply");
                        logger.info("Application for Private placement is past closing date and so holder cannot apply [{}] - [{}]", login.getUserId(), resp.getRetn());
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Client company has not declared Private Placement and so holder cannot apply.");
                    logger.info("Client company does not declared Private Placement and so holder cannot apply. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Holder does not have and account with Client company and so holder cannot apply for Private Placement.");
                logger.info("Holder does not have and account with Client company and so holder cannot apply for Private Placement. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Holder does not exists and so cannot apply for Private Placement.");
            logger.info("Holder does not exists so cannot apply for Private Placement - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process Private Placement Application request. Contact system administrator.");
            logger.info("error processing Private Placement Application request. See error log [{}] - [{}]", login.getUserId(), resp.getRetn());
            logger.error("error processing Private Placement Application request. - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to create Private Placement on confirmation
     *
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the
     * notification
     * @param ppApply the Private Placement application object to be created
     * @return response to the Private Placement application creation
     * request
     */
    public Response privatePlacementApplicationConfirmation_Request(Login login, String authenticator, PrivatePlacementApplication ppApply) {
        logger.info("request to confirm private placement application [{}], invoked by", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        Date date = new Date();
        List<PrivatePlacementApplication> ppAppList = new ArrayList<>();

        double amtPaid = 0;
        long continuingShares = 0;
        long remainingSharesNotBought = 0;
        long totalSharesBought = 0;
        long continuingSharesAvailable = 0;
        long continuingSharesSubscribed = 0;
        long sharesSubscribed = 0;
        double returnMoney = 0;
        double sharesSubscribedValue = 0;

        try {
            NotificationWrapper wrapper;
            QueueSender queue;
            NotifierProperties prop;

            if (hcq.checkHolderAccount(ppApply.getHolderId())) {// check if holder has company account with a particular client company                
                if (true) {// check if there is open private placement// check if holder has company account with a particular client company                    
                    // booleand openPP = cq.checkOpenPrivatePlacement(ppApply.getPrivatePlacementId());
                    if (true) {// checks if private placement is still opened                        
                        // PrivatePlacement pp = hcq.getPrivatePlacement(ppApply.getPrivatePlacementId());
                        PrivatePlacement pp = new PrivatePlacement();
                        if (date.before(pp.getClosingDate())) {
                            if (ppApply.getSharesSubscribed() >= pp.getStartingMinSubscrptn()) {
                                continuingShares = ppApply.getSharesSubscribed() - pp.getStartingMinSubscrptn();
                                if (continuingShares % pp.getContinuingMinSubscrptn() == 0) {
                                    amtPaid = ppApply.getSharesSubscribed() * pp.getOfferPrice();
                                    if (ppApply.getAmountPaid() == amtPaid) {
                                        // List<org.greenpole.hibernate.entity.PrivatePlacementApplication> ppList = hcq.getPrivatePlacementApplication(ppApply.getPrivatePlacementId());
                                        List<org.greenpole.hibernate.entity.PrivatePlacementApplication> ppList = new ArrayList<>();
                                        for (org.greenpole.hibernate.entity.PrivatePlacementApplication ppApp : ppList) {
                                            totalSharesBought += ppApp.getSharesSubscribed();
                                        }
                                        remainingSharesNotBought = pp.getTotalSharesOnOffer() - totalSharesBought;
                                        if (ppApply.getSharesSubscribed() <= remainingSharesNotBought) {
                                            sharesSubscribedValue = ppApply.getSharesSubscribed() * pp.getOfferPrice();
                                            ppApply.setSharesSubscribedValue(sharesSubscribedValue);
                                            ppAppList.add(ppApply);

                                        } else if ((ppApply.getSharesSubscribed() > remainingSharesNotBought) && (remainingSharesNotBought >= pp.getStartingMinSubscrptn())) {
                                            continuingSharesAvailable = remainingSharesNotBought - pp.getStartingMinSubscrptn();
                                            if (continuingSharesAvailable % pp.getContinuingMinSubscrptn() == 0) {
                                                continuingSharesSubscribed = (int) (Math.ceil(continuingSharesAvailable / pp.getContinuingMinSubscrptn()) * pp.getContinuingMinSubscrptn());
                                                sharesSubscribed = pp.getStartingMinSubscrptn() + continuingSharesSubscribed;
                                                sharesSubscribedValue = sharesSubscribed * pp.getOfferPrice();
                                                returnMoney = (ppApply.getSharesSubscribed() - sharesSubscribed) * pp.getOfferPrice();
                                                ppApply.setSharesSubscribedValue(sharesSubscribedValue);
                                                // ppApply.setReturnMoney(returnMoney);
                                                ppAppList.add(ppApply);
                                            }
                                        }
                                        wrapper = new NotificationWrapper();
                                        prop = NotifierProperties.getInstance();
                                        queue = new QueueSender(prop.getNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());

                                        wrapper.setCode(notification.createCode(login));
                                        wrapper.setDescription("Authenticate creation of Private Placement Application for holder " + ppApply.getHolderId());
                                        wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
                                        wrapper.setFrom(login.getUserId());
                                        wrapper.setTo(authenticator);
                                        wrapper.setModel(ppAppList);
                                        resp = queue.sendAuthorisationRequest(wrapper);
                                        logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                                        // send SMS and/or Email confirmation
                                        return resp;
                                    }
                                    resp.setRetn(200);
                                    resp.setDesc("The amount paid for shares and the price of shares to be bought should be the same.");
                                    logger.info("The amount paid for shares and the price of shares to be bought should be the same. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                    return resp;
                                }
                                resp.setRetn(200);
                                resp.setDesc("Continuing shares should be in multiples of " + pp.getContinuingMinSubscrptn());
                                logger.info("Continuing shares is not in multiples of continuing minimum subscription - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                return resp;
                            }
                            resp.setRetn(200);
                            resp.setDesc("Shares subscribed is less than minimum subscription required and so holder cannot apply for Private Placement.");
                            logger.info("Shares subscribed is less than minimum subscription required and so holder cannot apply for Private Placement. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                            return resp;
                        }
                        resp.setRetn(200);
                        resp.setDesc("Private placement is past closing date and so holder cannot apply");
                        logger.info("Private placement is past closing date and so holder cannot apply [{}] - [{}]", login.getUserId(), resp.getRetn());
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Client company has not declared Private Placement and so holder cannot apply.");
                    logger.info("Client company does not declared Private Placement and so holder cannot apply. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Holder does not have and account with Client company and so holder cannot apply for Private Placement.");
                logger.info("Holder does not have and account with Client company and so holder cannot apply for Private Placement. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Holder does not exists and so cannot apply for Private Placement.");
            logger.info("Holder does not exists so cannot apply for Private Placement - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process Initial Public Offer Application request. Contact system administrator.");
            logger.info("error processing Initial Public Offer Application request. See error log [{}] - [{}]", login.getUserId(), resp.getRetn());
            logger.error("error processing Initial Public Offer Application request. - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to create Private Placement application that has been 
     * saved in a notification file, according to the specified notification code
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the Private Placement application creation request
     */
    public Response privatePlacementApplication_Authorise(Login login, String notificationCode) {
        logger.info("Authorisation request to private placement application, invoked by [{}]", login.getUserId());
        Notification notification = new Notification();
        Response resp = new Response();
        Date date = new Date();
        long continuingShares = 0;
        double amtPaid = 0;
        long totalSharesBought = 0;
        long availableShares = 0;
        long remainigSharesNotBought = 0;
        double returnMoney = 0;
        double sharesSubscribedValue = 0;

        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<PrivatePlacementApplication> ppApplicationList = (List<PrivatePlacementApplication>) wrapper.getModel();
            PrivatePlacementApplication ppApply = ppApplicationList.get(0);
            if (hcq.checkHolderAccount(ppApply.getHolderId())) {// check if holder has company account with a particular client company                
                if (true) {// check if there is open private placement// check if holder has company account with a particular client company                    
                    // booleand openPP = cq.checkOpenPrivatePlacement(ppApply.getPrivatePlacementId());
                    if (true) {// checks if private placement is still opened                        
                        // PrivatePlacement pp = hcq.getPrivatePlacement(ppApply.getPrivatePlacementId());
                        PrivatePlacement pp = new PrivatePlacement();
                        if (date.before(pp.getClosingDate())) {
                            if (ppApply.getSharesSubscribed() >= pp.getStartingMinSubscrptn()) {
                                continuingShares = ppApply.getSharesSubscribed() - pp.getStartingMinSubscrptn();
                                if (continuingShares % pp.getContinuingMinSubscrptn() == 0) {
                                    amtPaid = ppApply.getSharesSubscribed() * pp.getOfferPrice();
                                    if (ppApply.getAmountPaid() == amtPaid) {
                                        // List<org.greenpole.hibernate.entity.PrivatePlacementApplication> ppList = hcq.getPrivatePlacementApplication(ppApply.getPrivatePlacementId());
                                        List<org.greenpole.hibernate.entity.PrivatePlacementApplication> ppList = new ArrayList<>();
                                        for (org.greenpole.hibernate.entity.PrivatePlacementApplication ppApp : ppList) {
                                            totalSharesBought += ppApp.getSharesSubscribed();
                                        }
                                        remainigSharesNotBought = pp.getTotalSharesOnOffer() - totalSharesBought;
                                        if (ppApply.getSharesSubscribed() <= remainigSharesNotBought) {
                                            sharesSubscribedValue = ppApply.getSharesSubscribed() * pp.getOfferPrice();
                                            ppApply.setSharesSubscribedValue(sharesSubscribedValue);
                                            ppApply.setReturnMoney(returnMoney);
                                            ppApply.setCanceled(false);
                                        } else if ((ppApply.getSharesSubscribed() > remainigSharesNotBought) && (remainigSharesNotBought >= pp.getStartingMinSubscrptn())) {
                                            long continuingSharesAvailable = remainigSharesNotBought - pp.getStartingMinSubscrptn();
                                            if (continuingSharesAvailable % pp.getContinuingMinSubscrptn() == 0) {
                                                availableShares = (int) (pp.getStartingMinSubscrptn() + (Math.ceil(continuingSharesAvailable / pp.getContinuingMinSubscrptn()) * pp.getContinuingMinSubscrptn()));
                                                sharesSubscribedValue = availableShares * pp.getOfferPrice();
                                                returnMoney = (ppApply.getSharesSubscribed() - availableShares) * pp.getOfferPrice();
                                                ppApply.setSharesSubscribedValue(sharesSubscribedValue);
                                                ppApply.setReturnMoney(returnMoney);
                                                ppApply.setCanceled(false);
                                            }
                                        }
                                        org.greenpole.hibernate.entity.PrivatePlacementApplication ppApplicationEntity = new org.greenpole.hibernate.entity.PrivatePlacementApplication();

                                        ClearingHouse clHouse = new ClearingHouse();
                                        clHouse.setId(ppApply.getClearingHouseId());

                                        Holder holder = new Holder();
                                        holder.setId(ppApply.getHolderId());

                                        PrivatePlacement ppEntity = new PrivatePlacement();
                                        ppEntity.setId(ppApply.getPrivatePlacementId());

                                        ppApplicationEntity.setClearingHouse(clHouse);
                                        ppApplicationEntity.setHolder(holder);
                                        ppApplicationEntity.setPrivatePlacement(ppEntity);
                                        ppApplicationEntity.setIssuer(ppApply.getIssuer());
                                        ppApplicationEntity.setSharesSubscribed(ppApply.getSharesSubscribed());
                                        ppApplicationEntity.setAmountPaid(ppApply.getAmountPaid());
                                        ppApplicationEntity.setIssuingHouse(ppApply.getIssuingHouse());
                                        ppApplicationEntity.setSharesSubscribedValue(ppApply.getSharesSubscribedValue());
                                        ppApplicationEntity.setReturnMoney(ppApply.getReturnMoney());
                                        ppApplicationEntity.setProcessingPayment(ppApply.isProcessingPayment());
                                        ppApplicationEntity.setApproved(ppApply.isApproved());
                                        ppApplicationEntity.setCanceled(ppApply.isCanceled());
                                        ppApplicationEntity.setDateApplied(formatter.parse(ppApply.getDateApplied()));
                                        // hcq.createPrivatePlacementApplication(ppApplicationEntity);
                                        resp.setRetn(0);
                                        resp.setDesc("Application for Private Placement Successful ");
                                        logger.info("Application for Private Placement Successful. [{}] - [{}]", login.getUserId(), resp.getRetn());
                                        wrapper.setAttendedTo(true);
                                        notification.markAttended(notificationCode);
                                        // send SMS and/or Email confirmation
                                        return resp;
                                    }
                                    resp.setRetn(200);
                                    resp.setDesc("The amount paid for shares and the price of shares to be bought should be the same.");
                                    logger.info("The amount paid for shares and the price of shares to be bought should be the same. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                    return resp;
                                }
                                resp.setRetn(200);
                                resp.setDesc("Continuing shares subscribed should be in multiples of " + pp.getContinuingMinSubscrptn());
                                logger.info("Continuing shares subscribed is not in multiples of continuing minimum subscription - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                return resp;
                            }
                            resp.setRetn(200);
                            resp.setDesc("Shares subscribed is less than minimum subscription required and so holder cannot apply for Private Placement.");
                            logger.info("Shares subscribed is less than minimum subscription required and so holder cannot apply for Private Placement. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                            return resp;
                        }
                        resp.setRetn(200);
                        resp.setDesc("Application for Private placement is past closing date and so holder cannot apply");
                        logger.info("Application for Private placement is past closing date and so holder cannot apply [{}] - [{}]", login.getUserId(), resp.getRetn());
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Client company has not declared Private Placement and so holder cannot apply.");
                    logger.info("Client company does not declared Private Placement and so holder cannot apply. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Holder does not have and account with Client company and so holder cannot apply for Private Placement.");
                logger.info("Holder does not have and account with Client company and so holder cannot apply for Private Placement. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Holder does not exists and so cannot apply for Private Placement.");
            logger.info("Holder does not exists so cannot apply for Private Placement - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (JAXBException ex) {
            resp.setRetn(98);
            resp.setDesc("Unable to process Private Placement application. Contact System Administrator");
            logger.info("Error loading notification xml file. See error log [{}] - [{}]", login.getUserId(), resp.getRetn());
            logger.error("Error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process Private Placement Application request. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            logger.info("Error processing Private Placement Application request. See error log [{}] - [{}]", login.getUserId(), resp.getRetn());
            logger.error("Error processing Private Placement Application request. - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to cancel a shareholder's Private Placement application
     * @param login the user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param ppApply the Private Placement application object to be canceled
     * @return response to the Private Placement application cancel request
     */
    public Response cancelPrivatePlacementApplication_Request(Login login, String authenticator, PrivatePlacementApplication ppApply) {
        logger.info("Request to cancel Private Placement application [{}], invoked by", login.getUserId());
        Notification notification = new Notification();
        Response resp = new Response();

        try {
            NotificationWrapper wrapper;
            QueueSender queue;
            NotifierProperties prop;
            // org.greenpole.hibernate.entity.PrivatePlacementApplication ppApplicationEntity = hcq.getPrivatePlacementApplication(ipoApply.getId());
            org.greenpole.hibernate.entity.PrivatePlacementApplication ppApplicationEntity = new org.greenpole.hibernate.entity.PrivatePlacementApplication();
            if (ppApplicationEntity.getProcessingPayment() || ppApplicationEntity.getApproved()) {
                // if (true) {
                wrapper = new NotificationWrapper();
                prop = NotifierProperties.getInstance();
                queue = new QueueSender(prop.getNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
                List<PrivatePlacementApplication> ppAppList = new ArrayList<>();
                ppAppList.add(ppApply);
                wrapper.setCode(notification.createCode(login));
                wrapper.setDescription("Authenticate cancellation of Private Placement Application for " + ppApply.getHolderId());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(ppAppList);
                resp = queue.sendAuthorisationRequest(wrapper);
                logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                // send SMS and/or Email notification
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Payment for Private Placement Application is being processed or has been approved and so cannot be cancelled");
            logger.info("Payment for Private Placement Application is being processed or has been approved and so cannot be cancelled. - [{}]: [{}]", login.getUserId(), resp.getRetn());
            // send SMS and/or Email notification
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process cancellation of Private Placement Application request. Contact system administrator.");
            logger.info("Error processing cancellation of Private Placement Application request. See error log - [{}]", login.getUserId());
            logger.error("Error processing cancellaton of Private Placement Application request. - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to cancel a shareholder's Private Placement application that
     * has been saved as a notification file, according to the specified notification code
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return the response to the cancel Private Placement application request
     */
    public Response cancelPrivatePlacementApplication_Authorise(Login login, String notificationCode) {
        logger.info("Authorisation to cancel Private Placement Application, invoked by [{}]", login.getUserId());
        Notification notification = new Notification();
        Response resp = new Response();

        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<PrivatePlacementApplication> ppApplicationList = (List<PrivatePlacementApplication>) wrapper.getModel();
            PrivatePlacementApplication ppApply = ppApplicationList.get(0);
            // org.greenpole.hibernate.entity.PrivatePlacementApplication ppApplicationEntity = hcq.getPrivatePlacementApplication(ppApply.getId());
            org.greenpole.hibernate.entity.PrivatePlacementApplication ppApplicationEntity = new org.greenpole.hibernate.entity.PrivatePlacementApplication();
            if (ppApplicationEntity.getProcessingPayment() || ppApplicationEntity.getApproved()) {
                // if (true) {
                ppApply.setCanceled(true);
                ppApplicationEntity.setCanceled(ppApply.isCanceled());
                // hcq.updatePrivatePlacementApplication(ppApplicationEntity);
                resp.setRetn(0);
                resp.setDesc("Authorisation to cancel Private Placement Application successful");
                logger.info("Authorisation to cancel Private Placement Application successful - [{}]", login.getUserId());
                wrapper.setAttendedTo(true);
                notification.markAttended(notificationCode);
                // send SMS and/or Email notification
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Payment for Private Placement Application is being processed or has been approved and so cannot be cancelled");
            logger.info("Payment for Private Placement Application is being processed or has been approved and so cannot be cancelled. - [{}]: [{}]", login.getUserId(), resp.getRetn());
            // send SMS and/or Email notification
            return resp;
        } catch (JAXBException ex) {
            resp.setRetn(98);
            resp.setDesc("Unable to process cancellation of Private Placement application. Contact System Administrator");
            logger.info("Error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("Error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process cancellation of Private Placement application. Contact system administrator.");
            logger.info("Error processing cancellation of Private Placement application. See error log - [{}]", login.getUserId());
            logger.error("Error processing cancellation of Private Placement application  - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to add share units to a shareholder's company account
     * @param login the user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param ppApply Private Placement object
     * @return response to the Add Share Unit request
     */
    public Response addShareUnit_Request(Login login, String authenticator, PrivatePlacementApplication ppApply) {
        logger.info("Request to add share units [{}], invoked by", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        List<PrivatePlacementApplication> ppAppList = new ArrayList<>();

        try {
            NotificationWrapper wrapper;
            QueueSender queue;
            NotifierProperties prop;

            if ((ppApply.getHolderId() > 0) && (ppApply.getPrivatePlacementId() > 0)) {
                ppAppList.add(ppApply);
                wrapper = new NotificationWrapper();
                prop = NotifierProperties.getInstance();
                queue = new QueueSender(prop.getNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());

                wrapper.setCode(notification.createCode(login));
                wrapper.setDescription("Authenticate addition of share units for holder " + ppApply.getHolderId());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(ppAppList);
                resp = queue.sendAuthorisationRequest(wrapper);
                logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                // send SMS and/or Email confirmation
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Error: Values for Holder Id or Private Placement Id is missing or incorrect");
            logger.info("Values for Holder Id or Private Placement Application Id is missing or incorrect - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process add share unit request. Contact system administrator.");
            logger.info("Error processing add share unit request. See error log [{}] - [{}]", login.getUserId(), resp.getRetn());
            logger.error("Error processing add share unit request. - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to add share units to a shareholder's company account
     * that has been saved as a notification file, according to the specified
     * notification code
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the Add Share Unit request
     */
    public Response addShareUnit_Authorise(Login login, String notificationCode) {
        logger.info("Authorisation request to add share unit [{}], invoked by", login.getUserId());
        Notification notification = new Notification();
        Response resp = new Response();
        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<PrivatePlacementApplication> ppApplicationList = (List<PrivatePlacementApplication>) wrapper.getModel();
            PrivatePlacementApplication ppApply = ppApplicationList.get(0);

            if ((ppApply.getHolderId() > 0) && (ppApply.getPrivatePlacementId() > 0)) {
                org.greenpole.hibernate.entity.PrivatePlacementApplication ppa = new org.greenpole.hibernate.entity.PrivatePlacementApplication();
                // org.greenpole.hibernate.entity.PrivatePlacementApplication ppa = hcq.getPrivatePlacementApplication(ppApply.getId());
                PrivatePlacement privatePlacement = new PrivatePlacement();
                // PrivatePlacement privatePlacement = cq.getPrivatePlacement(ppApply.getPrivatePlacementId());

                HolderCompanyAccount holderCompAcct = hcq.getHolderCompanyAccount(ppApply.getHolderId(), privatePlacement.getClientCompany().getId());
                holderCompAcct.setShareUnits(holderCompAcct.getShareUnits() + ppa.getSharesSubscribed());

                hcq.createUpdateHolderCompanyAccount(holderCompAcct);

                resp.setRetn(0);
                resp.setDesc("Addition of share units successful");
                logger.info("Addition of share units successful - [{}]: [{}]", login.getUserId(), resp.getRetn());
                wrapper.setAttendedTo(true);
                notification.markAttended(notificationCode);
                // send SMS and/or Email confirmation
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Error: Values for Holder Id or Private Placement Id is missing or incorrect");
            logger.info("Values for Holder Id or Private Placement Application Id is missing or incorrect - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (JAXBException ex) {
            resp.setRetn(98);
            resp.setDesc("Unable to add shares unit. Contact System Administrator");
            logger.info("Error loading notification xml file. See error log [{}] - [{}]", login.getUserId(), resp.getRetn());
            logger.error("Error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to add shares unit. Contact system administrator.");
            logger.info("Error processing adding shares unit. See error log [{}] - [{}]", login.getUserId(), resp.getRetn());
            logger.error("Error processing adding shares unit. - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request validation to adjust share units to be added to shareholder's company account
     * @param login the user's login details
     * @param ppApply Private Placement Application object
     * @return response to the Adjust Share Unit request
     */
    public Response adjustShareUnit_Request(Login login, PrivatePlacementApplication ppApply) {
        logger.info("Request to adjust share units [{}], invoked by", login.getUserId());
        Response resp = new Response();
        Date date = new Date();
        Notification notification = new Notification();
        List<PrivatePlacementApplication> ppAppList = new ArrayList<>();
        double returnMoney = 0;

        try {
            NotificationWrapper wrapper;
            QueueSender queue;
            NotifierProperties prop;

            if ((ppApply.getHolderId() > 0) && (ppApply.getPrivatePlacementId() > 0)) {
                if (ppApply.getSharesSubscribed() > ppApply.getSharesAdjusted()) {
                    int returnShares = (ppApply.getSharesSubscribed() - ppApply.getSharesAdjusted());
                    long timeDiff = date.getTime() - formatter.parse(ppApply.getDateApplied()).getTime();
                    timeDiff = timeDiff / (24 * 60 * 60 * 1000);
                    // global value for rate and tax will be set
                    double rate = 0;
                    double tax = 0;
                    rate = rate / 100;
                    if (ppApply.getHolder().isTaxExempted()) {
                        returnMoney = (returnShares * timeDiff * rate);
                    } else {
                        returnMoney = (returnShares * timeDiff * rate) - tax;
                    }
                    ppApply.setReturnMoney(returnMoney);
                    ppAppList.add(ppApply);
                    resp.setRetn(0);
                    resp.setBody(ppAppList);
                    resp.setDesc("Adjust Shares Unit for Confirmation");
                    logger.info("Adjust Shares Unit for Confirmation");
                    // send SMS and/or Email confirmation
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Shares adjusted cannot be greater than shares subscribed");
                logger.info("Shares adjusted cannot be greater than shares subscribed - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Error: Values for Holder Id or Private Placement Id is missing or incorrect");
            logger.info("Values for Holder Id or Private Placement Application Id is missing or incorrect - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process adjust share unit request. Contact system administrator.");
            logger.info("Error processing adjust share unit request. See error log [{}] - [{}]", login.getUserId(), resp.getRetn());
            logger.error("Error processing adjust share unit request. - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request confirmation to adjust share units to be added to shareholder's company account
     * that has been saved as a notification file, according to the specified notification code
     * @param login the user's login details 
     * @param authenticator the authenticator meant to receive the notification 
     * @param ppApply Private Placement Application object
     * @return response to the Adjust Share Unit Confirmation
     */
    public Response adjustShareUnit_Confirmation(Login login, String authenticator, PrivatePlacementApplication ppApply) {
        logger.info("Request to adjust share units [{}], invoked by", login.getUserId());
        Response resp = new Response();
        Date date = new Date();
        Notification notification = new Notification();
        List<PrivatePlacementApplication> ppAppList = new ArrayList<>();
        double returnMoney = 0;

        try {
            NotificationWrapper wrapper;
            QueueSender queue;
            NotifierProperties prop;

            if ((ppApply.getHolderId() > 0) && (ppApply.getPrivatePlacementId() > 0)) {
                if (ppApply.getSharesSubscribed() > ppApply.getSharesAdjusted()) {
                    int returnShares = (ppApply.getSharesSubscribed() - ppApply.getSharesAdjusted());
                    long timeDiff = date.getTime() - formatter.parse(ppApply.getDateApplied()).getTime();
                    timeDiff = timeDiff / (24 * 60 * 60 * 1000);
                    // global value for rate and tax will be set
                    double rate = 0;
                    double tax = 0;
                    rate = rate / 100;
                    if (ppApply.getHolder().isTaxExempted()) {
                        returnMoney = (returnShares * timeDiff * rate);
                    } else {
                        returnMoney = (returnShares * timeDiff * rate) - tax;
                    }
                    ppApply.setReturnMoney(returnMoney);
                    ppAppList.add(ppApply);
                    wrapper = new NotificationWrapper();
                    prop = NotifierProperties.getInstance();
                    queue = new QueueSender(prop.getNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());

                    wrapper.setCode(notification.createCode(login));
                    wrapper.setDescription("Authenticate adjusting of share units for holder " + ppApply.getHolderId());
                    wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
                    wrapper.setFrom(login.getUserId());
                    wrapper.setTo(authenticator);
                    wrapper.setModel(ppAppList);
                    resp = queue.sendAuthorisationRequest(wrapper);
                    logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                    // send SMS and/or Email confirmation
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Shares adjusted cannot be greater than shares subscribed");
                logger.info("Shares adjusted cannot be greater than shares subscribed - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Error: Values for Holder Id or Private Placement Id is missing or incorrect");
            logger.info("Values for Holder Id or Private Placement Application Id is missing or incorrect - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process adjust share unit request. Contact system administrator.");
            logger.info("Error processing adjust share unit request. See error log [{}] - [{}]", login.getUserId(), resp.getRetn());
            logger.error("Error processing adjust share unit request. - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to adjust the share units to be added to a shareholder's company account
     * that has been saved as a notification file, according to the specified 
     * notification code
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the Adjust Share Unit authorisation request
     */
    public Response adjustShareUnit_Authorise(Login login, String notificationCode) {
        logger.info("Authorisation request to adjust share unit [{}], invoked by", login.getUserId());
        Notification notification = new Notification();
        Response resp = new Response();
        Date date = new Date();
        double returnMoney = 0;
        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<PrivatePlacementApplication> ppApplicationList = (List<PrivatePlacementApplication>) wrapper.getModel();
            PrivatePlacementApplication ppApply = ppApplicationList.get(0);

            if ((ppApply.getHolderId() > 0) && (ppApply.getPrivatePlacementId() > 0)) {
                if (ppApply.getSharesSubscribed() > ppApply.getSharesAdjusted()) {
                    int returnShares = (ppApply.getSharesSubscribed() - ppApply.getSharesAdjusted());
                    long timeDiff = date.getTime() - formatter.parse(ppApply.getDateApplied()).getTime();
                    timeDiff = timeDiff / (24 * 60 * 60 * 1000);
                    // global value for rate and tax will be set
                    double rate = 0;
                    double tax = 0;
                    rate = rate / 100;
                    if (ppApply.getHolder().isTaxExempted()) {
                        returnMoney = (returnShares * timeDiff * rate);
                    } else {
                        returnMoney = (returnShares * timeDiff * rate) - tax;
                    }
                    ppApply.setReturnMoney(returnMoney);
                    org.greenpole.hibernate.entity.PrivatePlacementApplication ppa = new org.greenpole.hibernate.entity.PrivatePlacementApplication();
                    // org.greenpole.hibernate.entity.PrivatePlacementApplication ppa = hcq.getPrivatePlacementApplication(ppApply.getId());
                    PrivatePlacement privatePlacement = new PrivatePlacement();
                    // PrivatePlacement privatePlacement = cq.getPrivatePlacement(ppApply.getPrivatePlacementId());
                    HolderCompanyAccount holderCompAcct = hcq.getHolderCompanyAccount(ppApply.getHolderId(), privatePlacement.getClientCompany().getId());
                    holderCompAcct.setShareUnits(ppApply.getSharesSubscribed());
                    // persist private placement application also
                    hcq.createUpdateHolderCompanyAccount(holderCompAcct);

                    resp.setRetn(0);
                    resp.setDesc("Adjust of share units successful");
                    logger.info("Adjust of share units successful - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    wrapper.setAttendedTo(true);
                    notification.markAttended(notificationCode);
                    // send SMS and/or Email confirmation
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Shares adjusted cannot be greater than shares subscribed");
                logger.info("Shares adjusted cannot be greater than shares subscribed - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Error: Values for Holder Id or Private Placement Id is missing or incorrect");
            logger.info("Values for Holder Id or Private Placement Application Id is missing or incorrect - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (JAXBException ex) {
            resp.setRetn(98);
            resp.setDesc("Unable to adjust shares unit. Contact System Administrator");
            logger.info("Error loading notification xml file. See error log [{}] - [{}]", login.getUserId(), resp.getRetn());
            logger.error("Error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to adjust shares unit. Contact system administrator.");
            logger.info("Error processing adjust shares unit. See error log [{}] - [{}]", login.getUserId(), resp.getRetn());
            logger.error("Error processing adjust shares unit. - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to upload shareholders Private Placement application en-mass
     * @param login the user's login details
     * @param authenticator the authenticator meant to receive the notification code
     * @param ppApplyList the list of Private Placement applications
     * @return response to the Private Placement Application list
     */
    public Response uploadPPAEnmass_Request(Login login, String authenticator,
            List<PrivatePlacementApplication> ppApplyList) {
        logger.info("Request to upload list of Private Placement application, invoked by [{}]", login.getUserId());
        List<Response> respList = new ArrayList<>();
        List<ConfirmationDetails> confirmationList = new ArrayList<>();
        ConfirmationDetails confirm = new ConfirmationDetails();
        Response resp = new Response();
        Notification notification = new Notification();

        try {
            NotificationWrapper wrapper;
            QueueSender queue;
            NotifierProperties prop;
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());

            wrapper = new NotificationWrapper();
            prop = NotifierProperties.getInstance();
            queue = new QueueSender(prop.getNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());

            logger.info("Preparing notification for a list of Private Placement applications, invoked by [{}]", login.getUserId());

            wrapper.setCode(notification.createCode(login));

            for (PrivatePlacementApplication ppApplicant : ppApplyList) {
                respList.add(validateHolderDetails(login, ppApplicant.getHolder()));
                wrapper.setDescription("Authenticate holder, " + ppApplicant.getHolder().getFirstName() + " " + ppApplicant.getHolder().getLastName()
                        + "'s application for Private Placement issued by - " + ppApplicant.getIssuer());
            }
            wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
            wrapper.setFrom(login.getUserId());
            wrapper.setTo(authenticator);
            wrapper.setModel(ppApplyList);
            resp = queue.sendAuthorisationRequest(wrapper);
            logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
            confirm.setDetails(respList);
            confirmationList.add(confirm);
            resp.setRetn(0);
            resp.setBody(confirmationList);
            // send SMS and/or Email notification
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process Private Placement Application request. Contact system administrator.");
            logger.info("error processing Private Placement Application request. See error log - [{}]", login.getUserId());
            logger.error("error processing Private Placement Application request. - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to upload shareholders Private Placement application en-mass
     * that has been saved as a notification file, according to the specified notification code
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the Upload Private Placement application en-mass
     */
    public Response uploadPPAEnmass_Authorise(Login login, String notificationCode) {
        logger.info("Authorise uploaded list of Private Placement application, invoked by [{}]", login.getUserId());
        Notification notification = new Notification();
        List<ConfirmationDetails> confirmationList = new ArrayList<>();
        ConfirmationDetails confirm = new ConfirmationDetails();

        Response resp = new Response();
        Response validPpApply;
        Response validHolder;
        List<Response> respList = new ArrayList<>();
        Date date = new Date();

        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            List<PrivatePlacementApplication> ppAppList = (List<PrivatePlacementApplication>) wrapper.getModel();
            List<PrivatePlacementApplication> ppApplications = (List<PrivatePlacementApplication>) ppAppList.get(0);

            List<org.greenpole.entity.model.holder.Holder> successfulHolders = new ArrayList<>();
            List<PrivatePlacementApplication> successfulPPAs = new ArrayList<>();
            List<PrivatePlacementApplication> unsuccessfulPPAs = new ArrayList<>();
            List<org.greenpole.entity.model.holder.Holder> unsuccessfulHolders = new ArrayList<>();

            for (PrivatePlacementApplication ppApplicant : ppApplications) {// check valid holders                
                validHolder = validateHolderDetails(login, ppApplicant.getHolder());// check response code
                validPpApply = validatePPA(login, ppApplicant);
                if (validHolder.getRetn() == 0) {// process ppApplication
                    successfulHolders.add(ppApplicant.getHolder());
                    respList.add(validHolder);
                    if (validPpApply.getRetn() == 0) {
                        successfulPPAs.add(ppApplicant);
                        respList.add(validPpApply);
                    }
                    unsuccessfulPPAs.add(ppApplicant);
                } else {// ignore and list as unsuccessful
                    unsuccessfulHolders.add(ppApplicant.getHolder());
                    unsuccessfulPPAs.add(ppApplicant);
                    respList.add(validHolder);
                    respList.add(validPpApply);
                }
            }
            List<org.greenpole.hibernate.entity.Holder> holdEntityChnList = new ArrayList<>();
            List<org.greenpole.hibernate.entity.Holder> holdEntityNoChnList = new ArrayList<>();
            List<org.greenpole.hibernate.entity.PrivatePlacementApplication> ppApplicationEntityList = new ArrayList<>();

            for (org.greenpole.entity.model.holder.Holder holder : successfulHolders) {
                org.greenpole.hibernate.entity.Holder holdEntityChn = new org.greenpole.hibernate.entity.Holder();
                org.greenpole.hibernate.entity.Holder holdEntityNoChn = new org.greenpole.hibernate.entity.Holder();
                HolderType typeEntity = new HolderType();

                if (holder.getChn() != null || !"".equals(holder.getChn())) {
                    holdEntityChn.setFirstName(holder.getFirstName());
                    holdEntityChn.setLastName(holder.getLastName());
                    holdEntityChn.setMiddleName(holder.getMiddleName());
                    holdEntityChn.setHolderType(typeEntity);
                    holdEntityChn.setGender(holder.getGender());
                    holdEntityChn.setDob(formatter.parse(holder.getDob()));
                    holdEntityChn.setPryHolder(true);
                    holdEntityChn.setMerged(false);
                    holdEntityChn.setChn(holder.getChn());
                    typeEntity.setId(holder.getTypeId());

                    holdEntityChnList.add(holdEntityChn);
                } else {
                    holdEntityNoChn.setFirstName(holder.getFirstName());
                    holdEntityNoChn.setLastName(holder.getLastName());
                    holdEntityNoChn.setMiddleName(holder.getMiddleName());
                    holdEntityNoChn.setHolderType(typeEntity);
                    holdEntityNoChn.setGender(holder.getGender());
                    holdEntityNoChn.setDob(formatter.parse(holder.getDob()));
                    holdEntityNoChn.setPryHolder(true);
                    holdEntityNoChn.setMerged(false);
                    typeEntity.setId(holder.getTypeId());

                    holdEntityNoChnList.add(holdEntityNoChn);
                }
            }

            for (PrivatePlacementApplication ppApply : successfulPPAs) {
                org.greenpole.hibernate.entity.PrivatePlacementApplication ppApplicationEntity = new org.greenpole.hibernate.entity.PrivatePlacementApplication();

                ClearingHouse clHouse = new ClearingHouse();
                clHouse.setId(ppApply.getClearingHouseId());

                Holder holder = new Holder();
                holder.setId(ppApply.getHolderId());

                PrivatePlacement ppEntity = new PrivatePlacement();
                ppEntity.setId(ppApply.getPrivatePlacementId());

                ppApplicationEntity.setClearingHouse(clHouse);
                ppApplicationEntity.setHolder(holder);
                ppApplicationEntity.setPrivatePlacement(ppEntity);
                ppApplicationEntity.setIssuer(ppApply.getIssuer());
                ppApplicationEntity.setSharesSubscribed(ppApply.getSharesSubscribed());
                ppApplicationEntity.setAmountPaid(ppApply.getAmountPaid());
                ppApplicationEntity.setIssuingHouse(ppApply.getIssuingHouse());
                ppApplicationEntity.setSharesSubscribedValue(ppApply.getSharesSubscribedValue());
                ppApplicationEntity.setReturnMoney(ppApply.getReturnMoney());
                ppApplicationEntity.setProcessingPayment(ppApply.isProcessingPayment());
                ppApplicationEntity.setApproved(ppApply.isApproved());
                ppApplicationEntity.setCanceled(ppApply.isCanceled());
                ppApplicationEntity.setDateApplied(formatter.parse(ppApply.getDateApplied()));

                ppApplicationEntityList.add(ppApplicationEntity);
            }

            boolean created = false;
            // TODO: persist list of holders and ipo applications
            // code here:
            // hcq.createUpdateHolderEnmass(holdEntityChnList, ppApplicationEntityList); 
            // hcq.createUpdateHolderEnmass(holdEntityNoChnList, ppApplicationEntityList); 
            if (created) {
                notification.markAttended(notificationCode);
                confirm.setDetails(respList);
                confirmationList.add(confirm);
                resp.setRetn(0);
                resp.setBody(confirmationList);
                resp.setDesc("Holder details saved: Successful");
                logger.info("Shareholder account creation successful - [{}]", login.getUserId());
                return resp;
            }
            confirm.setDetails(respList);
            confirmationList.add(confirm);
            resp.setRetn(320);
            resp.setBody(confirmationList);
            resp.setDesc("General Error. Unable to persist holder account. Contact system administrator.");
            logger.info("Error persist holder account - [{}]", login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            resp.setRetn(98);
            resp.setDesc("Unable to process Private Placement application. Contact System Administrator");
            logger.info("Error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("Error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process Private Placement application. Contact system administrator.");
            logger.info("Error processing Private Placement application. See error log - [{}]", login.getUserId());
            logger.error("Error processing Private Placement application - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }
    
    /**
     * Validates Holder details
     * @param login the user's login details
     * @param holder Holder object
     * @return response object to the calling method
     */
    private Response validateHolderDetails(Login login, org.greenpole.entity.model.holder.Holder holder) {
        Response resp = new Response();

        String desc = "";
        boolean flag = false;

        if (holder.getFirstName() == null || "".equals(holder.getFirstName())) {
            desc = "\nHolder first name should not be empty";
        } else if (holder.getLastName() == null || "".equals(holder.getLastName())) {
            desc += "\nHolder last name should not be empty";
        } else if (holder.getTypeId() <= 0) {
            desc += "\nHolder type should not be empty";
        } else if (holder.getPryAddress() == null || "".equals(holder.getPryAddress())) {
            desc += "\nPrimary Holder address is not specified";
        } else if (holder.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                || holder.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
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
            if (hcq.checkHolderAccount(holder.getChn())) {
                desc += "\nThe CHN already exists";
                flag = true;
            }
        }

        if (flag && holder.getTypeId() > 0) {
            boolean found = false;
            for (HolderType ht : hcq.getAllHolderTypes()) {
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

        if (flag && holder.getEmailAddresses() != null && !holder.getEmailAddresses().isEmpty()) {
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
        }

        if (flag) {
            resp.setRetn(0);
            resp.setDesc("Validation of holder details successful");
            logger.info("Validation of holder details successful - [{}] [{}]", login.getUserId(), resp.getRetn());
            return resp;
        }
        resp.setRetn(1);
        resp.setDesc("Error filing holder details: " + desc);
        logger.info("Error filing holder details: [{}] - [{}]", desc, login.getUserId());
        return resp;
    }

    /**
     * Validates Private Placement Application
     * @param login the user's login details
     * @param ppApply Private Placement Application object
     * @return response object to the caller method
     */
    private Response validatePPA(Login login, PrivatePlacementApplication ppApply) {
        logger.info("Request to create Private Placement application of [{}] for [{}], invoked by [{}]", ppApply.getIssuer(), ppApply.getHolderId(), login.getUserId());
        Response resp = new Response();
        Date date = new Date();

        double amtPaid = 0;
        long continuingShares = 0;
        long remainingSharesNotBought = 0;
        long totalSharesBought = 0;
        long continuingSharesAvailable = 0;
        long continuingSharesSubscribed = 0;
        long subscribedShares = 0;
        double returnMoney = 0;
        double sharesSubscribedValue = 0;
        List<PrivatePlacementApplication> ppAppList = new ArrayList<>();

        try {
            NotificationWrapper wrapper;
            QueueSender queue;
            NotifierProperties prop;

            if (hcq.checkHolderAccount(ppApply.getHolderId())) {
                // check if holder has company account with a particular client company
                if (true) {
                    // check if there is open private placement
                    // cq.checkOpenPrivatePlacement(ppApply.getPrivatePlacementId());
                    if (true) {
                        // checks if private placement is still opened
                        // PrivatePlacement pp = hcq.getPrivatePlacement(ppApply.getPrivatePlacementId());
                        PrivatePlacement pp = new PrivatePlacement();
                        if (date.before(pp.getClosingDate())) {
                            if (ppApply.getSharesSubscribed() >= pp.getStartingMinSubscrptn()) {
                                continuingSharesSubscribed = ppApply.getSharesSubscribed() - pp.getStartingMinSubscrptn();
                                if (continuingSharesSubscribed % pp.getContinuingMinSubscrptn() == 0) {
                                    amtPaid = ppApply.getSharesSubscribed() * pp.getOfferPrice();
                                    if (ppApply.getAmountPaid() == amtPaid) {
                                        // List<PrivatePlacementApplication> ppList = hcq.getPrivatePlacementApplication(ppApply.getPrivatePlacementId());
                                        List<PrivatePlacementApplication> ppList = new ArrayList<>();
                                        for (PrivatePlacementApplication ppApp : ppList) {
                                            totalSharesBought += ppApp.getSharesSubscribed();
                                        }
                                        remainingSharesNotBought = pp.getTotalSharesOnOffer() - totalSharesBought;
                                        if (ppApply.getSharesSubscribed() <= remainingSharesNotBought) {
                                            sharesSubscribedValue = ppApply.getSharesSubscribed() * pp.getOfferPrice();
                                            ppApply.setSharesSubscribedValue(sharesSubscribedValue);
                                            ppApply.setDateApplied(date.toString());
                                            ppAppList.add(ppApply);
                                            resp.setRetn(0);
                                            resp.setDesc("Private Placement Application Details for confirmation.");
                                            resp.setBody(ppAppList);
                                            logger.info("Private Placement Application Details for confirmation. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                            // send SMS and/or Email notification
                                            return resp;
                                        } else if ((ppApply.getSharesSubscribed() > remainingSharesNotBought) && (remainingSharesNotBought >= pp.getStartingMinSubscrptn())) {
                                            continuingSharesAvailable = remainingSharesNotBought - pp.getStartingMinSubscrptn();
                                            if (continuingSharesAvailable % pp.getContinuingMinSubscrptn() == 0) {
                                                continuingSharesSubscribed = (int) (Math.ceil(continuingSharesAvailable / pp.getContinuingMinSubscrptn()) * pp.getContinuingMinSubscrptn());
                                                subscribedShares = pp.getStartingMinSubscrptn() + continuingSharesSubscribed;
                                                sharesSubscribedValue = subscribedShares * pp.getOfferPrice();
                                                returnMoney = (ppApply.getSharesSubscribed() - subscribedShares) * pp.getOfferPrice();
                                                ppApply.setSharesSubscribedValue(sharesSubscribedValue);
                                                // ppApply.setReturnMoney(returnMoney);
                                                ppAppList.add(ppApply);
                                                String respDesc = "Private Placement Application Details for confirmation.";
                                                resp.setBody(ppAppList);
                                                // send SMS and/or Email notification
                                                resp.setRetn(200);
                                                resp.setDesc(respDesc + "\nInsufficient quantity of shares. Number of shares available is: " + subscribedShares);
                                                logger.info("Available shares is less than quantity to be bought. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                                return resp;
                                            }
                                            resp.setRetn(200);
                                            resp.setDesc("Insufficient quantity of shares. Number of shares available is: " + pp.getStartingMinSubscrptn());
                                            logger.info("Available shares is less than quantity to be bought. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                            return resp;
                                        }
                                        resp.setRetn(200);
                                        resp.setDesc("There is not enough shares to buy.");
                                        logger.info("There is not enough shares to buy. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                        return resp;
                                    }
                                    resp.setRetn(200);
                                    resp.setDesc("The amount paid for shares and the price of shares to be bought should be the same.");
                                    logger.info("The amount paid for shares and the price of shares to be bought should be the same. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                    return resp;
                                }
                                resp.setRetn(200);
                                resp.setDesc("Continuing shares should be in multiples of " + pp.getContinuingMinSubscrptn());
                                logger.info("Continuing shares is not in multiples of continuing minimum subscription - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                return resp;
                            }
                            resp.setRetn(200);
                            resp.setDesc("Shares subscribed is less than minimum subscription required and so holder cannot apply for Private Placement.");
                            logger.info("Shares subscribed is less than minimum subscription required and so holder cannot apply for Private Placement. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                            return resp;
                        }
                        resp.setRetn(200);
                        resp.setDesc("Application for Private placement is past closing date and so holder cannot apply");
                        logger.info("Application for Private placement is past closing date and so holder cannot apply [{}] - [{}]", login.getUserId(), resp.getRetn());
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Client company has not declared Private Placement and so holder cannot apply.");
                    logger.info("Client company does not declared Private Placement and so holder cannot apply. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Holder does not have and account with Client company and so holder cannot apply for Private Placement.");
                logger.info("Holder does not have and account with Client company and so holder cannot apply for Private Placement. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Holder does not exists and so cannot apply for Private Placement.");
            logger.info("Holder does not exists so cannot apply for Private Placement - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process Private Placement Application request. Contact system administrator.");
            logger.info("error processing Private Placement Application request. See error log [{}] - [{}]", login.getUserId(), resp.getRetn());
            logger.error("error processing Private Placement Application request. - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Unwraps holder company account details from the holder model.
     *
     * @param holdModel object of the holder model
     * @param newEntry boolean variable indicating whether or not entry is new
     * @return object of HolderCompanyAccount
     */
    private List<org.greenpole.hibernate.entity.HolderCompanyAccount> retrieveHolderCompanyAccount(List<org.greenpole.entity.model.holder.Holder> holdModelList) {
        List<org.greenpole.hibernate.entity.HolderCompanyAccount> companyAccountEntityList = new ArrayList<>();

        for (org.greenpole.entity.model.holder.Holder holdModel : holdModelList) {
            org.greenpole.hibernate.entity.HolderCompanyAccount companyAccountEntity = new org.greenpole.hibernate.entity.HolderCompanyAccount();
            org.greenpole.entity.model.holder.HolderCompanyAccount compAcct = holdModel.getCompanyAccounts().get(0);
            HolderCompanyAccountId compAcctId = new HolderCompanyAccountId();

            compAcctId.setClientCompanyId(compAcct.getClientCompanyId());

            companyAccountEntity.setId(compAcctId);
            companyAccountEntity.setEsop(compAcct.isEsop());
            companyAccountEntity.setHolderCompAccPrimary(true);
            companyAccountEntity.setMerged(false);

            companyAccountEntityList.add(companyAccountEntity);
        }
        return companyAccountEntityList;
    }

    /**
     * Unwraps Holder residential address from the holder model into
     * HolderResidentialAddress hibernate entity object.
     *
     * @param holdModel object of holder model
     * @param newEntry boolean variable indicating whether or not entry is new
     * @return List of HolderResidentialAddress hibernate entity objects
     */
    private List<HolderResidentialAddress> retrieveHolderResidentialAddress(List<org.greenpole.entity.model.holder.Holder> holdModelList) {
        List<HolderResidentialAddress> residentialAddressSend = new ArrayList<>();
        for (org.greenpole.entity.model.holder.Holder holdModel : holdModelList) {
            org.greenpole.hibernate.entity.HolderResidentialAddress residentialAddressEntity = new org.greenpole.hibernate.entity.HolderResidentialAddress();
            List<org.greenpole.entity.model.Address> residentialAddressList;
            if (holdModel.getResidentialAddresses() != null) {
                residentialAddressList = holdModel.getResidentialAddresses();
            } else {
                residentialAddressList = new ArrayList<>();
            }

            List<org.greenpole.hibernate.entity.HolderResidentialAddress> returnResidentialAddress = new ArrayList<>();

            for (org.greenpole.entity.model.Address rAddy : residentialAddressList) {
//                HolderResidentialAddressId rAddyId = new HolderResidentialAddressId();
//                rAddyId.setAddressLine1(rAddy.getAddressLine1());
//                rAddyId.setState(rAddy.getState());
//                rAddyId.setCountry(rAddy.getCountry());
//                residentialAddressEntity.setId(rAddyId);
//                HolderResidentialAddressId rAddyId = new HolderResidentialAddressId();
//                rAddyId.setAddressLine1(rAddy.getAddressLine1());
//                rAddyId.setState(rAddy.getState());
//                rAddyId.setCountry(rAddy.getCountry());
//                residentialAddressEntity.setId(rAddyId);
                residentialAddressEntity.setAddressLine1(rAddy.getAddressLine1());
                residentialAddressEntity.setAddressLine2(rAddy.getAddressLine2());
                residentialAddressEntity.setAddressLine3(rAddy.getAddressLine3());
                residentialAddressEntity.setAddressLine4(rAddy.getAddressLine4());
                residentialAddressEntity.setCity(rAddy.getCity());
                residentialAddressEntity.setPostCode(rAddy.getPostCode());

                returnResidentialAddress.add(residentialAddressEntity);
            }
            residentialAddressSend.addAll(returnResidentialAddress);
        }
        return residentialAddressSend;
    }

    /**
     * Unwraps Holder email address from the holder model into
     * HolderEmailAddress hibernate entity object
     *
     * @param holdModel object to holder model
     * @param newEntry boolean variable indicating whether or not entry is new
     * @return List of HolderEmailAddress hibernate entity objects
     */
    private List<HolderEmailAddress> retrieveHolderEmailAddress(List<org.greenpole.entity.model.holder.Holder> holdModelList) {
        List<org.greenpole.hibernate.entity.HolderEmailAddress> emailAddressSend = new ArrayList<>();
        for (org.greenpole.entity.model.holder.Holder holdModel : holdModelList) {
            org.greenpole.hibernate.entity.HolderEmailAddress emailAddressEntity = new org.greenpole.hibernate.entity.HolderEmailAddress();
            List<org.greenpole.entity.model.EmailAddress> emailAddressList;
            if (holdModel.getEmailAddresses() != null) {
                emailAddressList = holdModel.getEmailAddresses();
            } else {
                emailAddressList = new ArrayList<>();
            }
            List<org.greenpole.hibernate.entity.HolderEmailAddress> returnEmailAddress = new ArrayList<>();

            for (EmailAddress email : emailAddressList) {
//                HolderEmailAddressId emailId = new HolderEmailAddressId();
//                emailId.setEmailAddress(email.getEmailAddress());
                emailAddressEntity.setIsPrimary(email.isPrimaryEmail());
//                emailAddressEntity.setId(emailId);
            }
            emailAddressSend.addAll(returnEmailAddress);
        }
        return emailAddressSend;
    }

    /**
     * Unwraps holder phone number details from the holder model passed as
     * parameter into HolderPhoneNumber hibernate entity
     *
     * @param holdModel object of holder details
     * @param newEntry boolean variable indicating whether or not the entry is
     * new
     * @return List of HolderPhoneNumber objects retrieveHolderEmailAddress
     */
    private List<HolderPhoneNumber> retrieveHolderPhoneNumber(List<org.greenpole.entity.model.holder.Holder> holdModelList) {
        List<org.greenpole.hibernate.entity.HolderPhoneNumber> phoneNumberSend = new ArrayList<>();
        for (org.greenpole.entity.model.holder.Holder holdModel : holdModelList) {
            org.greenpole.hibernate.entity.HolderPhoneNumber phoneNumberEntity = new org.greenpole.hibernate.entity.HolderPhoneNumber();
            List<org.greenpole.entity.model.PhoneNumber> phoneNumberList;
            if (holdModel.getPhoneNumbers() != null) {
                phoneNumberList = holdModel.getPhoneNumbers();
            } else {
                phoneNumberList = new ArrayList<>();
            }

            List<org.greenpole.hibernate.entity.HolderPhoneNumber> returnPhoneNumber = new ArrayList<>();

            for (PhoneNumber pnList : phoneNumberList) {
//                HolderPhoneNumberId phoneNoId = new HolderPhoneNumberId();
//                phoneNoId.setPhoneNumber(pnList.getPhoneNumber());
                phoneNumberEntity.setIsPrimary(pnList.isPrimaryPhoneNumber());
//                phoneNumberEntity.setId(phoneNoId);
            }
            phoneNumberSend.addAll(returnPhoneNumber);
        }
        return phoneNumberSend;
    }

    /**
     * Unwraps the holder postal address details from the HolderModel into
     * HolderPostalAddress hibernate entity
     *
     * @param holdModel the holderModel of holder details
     * @param newEntry boolean value indicating new entry
     * @return List object of HolderPostalAddress hibernate entity
     */
    private List<HolderPostalAddress> retrieveHolderPostalAddress(List<org.greenpole.entity.model.holder.Holder> holdModelList) {
        List<org.greenpole.hibernate.entity.HolderPostalAddress> holderPostalAddressSend = new ArrayList<>();
        for (org.greenpole.entity.model.holder.Holder holdModel : holdModelList) {
            org.greenpole.hibernate.entity.HolderPostalAddress postalAddressEntity = new org.greenpole.hibernate.entity.HolderPostalAddress();
            List<org.greenpole.entity.model.Address> hpaddyList;
            if (holdModel.getPostalAddresses() != null) {
                hpaddyList = holdModel.getPostalAddresses();
            } else {
                hpaddyList = new ArrayList<>();
            }

            List<org.greenpole.hibernate.entity.HolderPostalAddress> returnHolderPostalAddress = new ArrayList<>();

            for (org.greenpole.entity.model.Address hpa : hpaddyList) {
//                HolderPostalAddressId postalAddyId = new HolderPostalAddressId();
//                postalAddyId.setAddressLine1(hpa.getAddressLine1());
//                postalAddyId.setState(hpa.getState());
//                postalAddyId.setCountry(hpa.getCountry());
//                postalAddressEntity.setId(postalAddyId);
                postalAddressEntity.setAddressLine1(hpa.getAddressLine1());
                postalAddressEntity.setAddressLine2(hpa.getAddressLine2());
                postalAddressEntity.setAddressLine3(hpa.getAddressLine3());
                postalAddressEntity.setCity(hpa.getCity());
                postalAddressEntity.setPostCode(hpa.getPostCode());
                postalAddressEntity.setIsPrimary(hpa.isPrimaryAddress());
                returnHolderPostalAddress.add(postalAddressEntity);
            }
            holderPostalAddressSend.addAll(returnHolderPostalAddress);
        }
        return holderPostalAddressSend;
    }

}
