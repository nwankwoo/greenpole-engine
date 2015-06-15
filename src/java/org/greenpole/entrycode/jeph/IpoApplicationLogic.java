/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph;

import org.greenpole.entirycode.jeph.model.ConfirmationDetails;
import org.greenpole.entirycode.jeph.model.IpoApplication;
import org.greenpole.entirycode.jeph.model.PrivatePlacementApplication;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.model.Address;
import org.greenpole.entity.model.EmailAddress;
import org.greenpole.entity.model.PhoneNumber;
import org.greenpole.entity.model.clientcompany.InitialPublicOffer;
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
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.GeneralComponentQuery;
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
public class IpoApplicationLogic {

    private final HolderComponentQuery hq = new HolderComponentQueryImpl();
    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();
    private final GeneralComponentQuery gq = ComponentQueryFactory.getGeneralComponentQuery();
    private final GreenpoleProperties greenProp = new GreenpoleProperties(IpoApplicationLogic.class);
    NotificationProperties noteProp = new NotificationProperties(IpoApplicationLogic.class);
    private static final Logger logger = LoggerFactory.getLogger(IpoApplicationLogic.class);
    // SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy");

    /**
     * Processes request to validate IPO application before creation
     * @param login the user's login details
     * @param ipoApply the IPO application object to be validated
     * @return response object to IPO application validation request
     */
    public Response ipoApplication_Request(Login login, IpoApplication ipoApply) {
        logger.info("Request to create Initial Public Offer application of [{}] for [{}], invoked by [{}]", ipoApply.getIssuer(), ipoApply.getHolderId(), login.getUserId());
        Response resp = new Response();
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        Date date = new Date();

        try {
            List<IpoApplication> iAppList = new ArrayList<>();

            int remainingSharesNotBought = 0;
            int totalSharesBought = 0;
            int continuingSharesSubscribed = 0;
            int continuingShares = 0;
            double amtPaid = 0;
            double returnMoney = 0;
            int continuingSharesAvailable = 0;
            int sharesBought = 0;
            double sharesSubscribedValue = 0;
            if (hq.checkHolderAccount(ipoApply.getHolderId())) {
                // if (hcq.checkIpo(applyIpo.getInitialPublicOfferId())) {
                if (true) {
                    // InitialPublicOffer ipo = hcq.getIpo(applyIpo.getInitialPublicOfferId());
                    InitialPublicOffer ipo = new InitialPublicOffer();
                    if (date.before(formatter.parse(ipo.getClosingDate()))) {
                        if (ipoApply.getSharesSubscribed() >= ipo.getStartingMinimumSubscription()) {
                            continuingShares = ipoApply.getSharesSubscribed() - ipo.getStartingMinimumSubscription();
                            if (continuingShares % ipo.getContinuingMinimumSubscription() == 0) {
                                amtPaid = ipoApply.getSharesSubscribed() * ipo.getOfferPrice();
                                if (ipoApply.getAmountPaid() == amtPaid) {
                                    // List<org.greenpole.hibernate.entity.IpoApplication> ipoApplyList = hcq.getIpoApplication(ipoApply.getInitialPublicOfferId(), false);
                                    List<org.greenpole.hibernate.entity.IpoApplication> ipoApplyList = new ArrayList<>();
                                    for (org.greenpole.hibernate.entity.IpoApplication ipoapp : ipoApplyList) {
                                        totalSharesBought += ipoapp.getSharesSubscribed();
                                    }
                                    remainingSharesNotBought = ipo.getTotalSharesOnOffer() - totalSharesBought;
                                    if (ipoApply.getSharesSubscribed() <= remainingSharesNotBought) {
                                        sharesSubscribedValue = ipoApply.getSharesSubscribed() * ipo.getOfferPrice();
                                        ipoApply.setSharesSubscribedValue(sharesSubscribedValue);
                                        ipoApply.setDateApplied(date.toString());
                                        iAppList.add(ipoApply);
                                        resp.setRetn(0);
                                        resp.setDesc("IPO Application Details for confirmation.");
                                        resp.setBody(iAppList);
                                        logger.info("IPO Application Details for confirmation. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                        // send SMS and/or Email notification
                                        return resp;
                                    } else if ((ipoApply.getSharesSubscribed() > remainingSharesNotBought) && (remainingSharesNotBought >= ipo.getStartingMinimumSubscription())) {
                                        continuingSharesAvailable = remainingSharesNotBought - ipo.getStartingMinimumSubscription();
                                        if (continuingSharesAvailable % ipo.getContinuingMinimumSubscription() == 0) {
                                            continuingSharesSubscribed = (int) (Math.floor(continuingSharesAvailable / ipo.getContinuingMinimumSubscription()) * ipo.getContinuingMinimumSubscription());
                                            sharesSubscribedValue = (ipo.getStartingMinimumSubscription() + continuingSharesSubscribed) * ipo.getOfferPrice();
                                            sharesBought = ipo.getStartingMinimumSubscription() + continuingSharesSubscribed;
                                            returnMoney = (ipoApply.getSharesSubscribed() - sharesBought) * ipo.getOfferPrice();
                                            ipoApply.setSharesSubscribedValue(sharesSubscribedValue);
                                            ipoApply.setDateApplied(date.toString());
                                            // ipoApply.setReturnMoney(returnMoney);
                                            iAppList.add(ipoApply);
                                            resp.setRetn(200);
                                            resp.setDesc("Insufficient quantity of shares. Number of shares available is: " + sharesBought);
                                            resp.setBody(iAppList);
                                            logger.info("Available shares is less than quantity to be bought. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                            return resp;
                                        }
                                        iAppList.add(ipoApply);
                                        resp.setBody(iAppList);
                                        resp.setRetn(200);
                                        resp.setDesc("Insufficient quantity of shares. Number of shares available is: " + remainingSharesNotBought);
                                        logger.info("Available shares is less than quantity to be bought. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                        return resp;
                                    }
                                    resp.setRetn(200);
                                    resp.setDesc("There is not enough shares to buy. Number of available shares is: " + remainingSharesNotBought);
                                    logger.info("There is not enough shares to buy. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                    return resp;
                                }
                                resp.setRetn(200);
                                resp.setDesc("The amount paid for shares and the price of shares to be bought should be the same.");
                                logger.info("The amount paid for shares and the price of shares to be bought should be the same. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                return resp;
                            }
                            resp.setRetn(200);
                            resp.setDesc("Continuing shares subscription should be in multiples of " + ipo.getContinuingMinimumSubscription());
                            logger.info("Continuing shares subscription is not in multiples of continuing minimum subscription - [{}]: [{}]", login.getUserId(), resp.getRetn());
                            return resp;
                        }
                        resp.setRetn(200);
                        resp.setDesc("Shares subscribed is less than minimum subscription required and so holder cannot apply for IPO.");
                        logger.info("Shares subscribed is less than minimum subscription required and so holder cannot apply for IPO. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Application for Initial Public Offer is past closing date and so holder cannot apply");
                    logger.info("Application for Initial Public Offer is past closing date and so holder cannot apply. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Client company has not declared Initial Public Offer and so holder cannot apply.");
                logger.info("Client company does not declared Initial Public Offer and so holder cannot apply. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Holder does not exists and so cannot apply for Initial Public Offer.");
            logger.info("Holder does not exists so cannot apply for Initial Public Offer - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process Initial Public Offer Application request. Contact system administrator.");
            logger.info("error processing Initial Public Offer Application request. See error log - [{}]", login.getUserId());
            logger.error("error processing Initial Public Offer Application request. - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to create IPO application on confirmation
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param ipoApply the IPO application object to be created
     * @return response object to for creation of IPO application
     */
    public Response ipoApplicationConfirmation_Request(Login login, String authenticator, IpoApplication ipoApply) {
        logger.info("Request to confirm Initial Public Offer application of [{}] for [{}], invoked by [{}]", ipoApply.getIssuer(), ipoApply.getHolderId(), login.getUserId());
        Response resp = new Response();
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        Notification notification = new Notification();
        Date date = new Date();

        List<IpoApplication> iAppList = new ArrayList<>();

        int remainingSharesNotBought = 0;
        int totalSharesBought = 0;
        int continuingShares = 0;
        double amtPaid = 0;
        double returnMoney = 0;
        int continuingSharesAvailable = 0;
        int sharesSubscribed = 0;
        double sharesSubscribedValue = 0;

        try {
            NotificationWrapper wrapper;
            QueueSender queue;
            NotifierProperties props;

            if (hq.checkHolderAccount(ipoApply.getHolderId())) {
                // if (hcq.checkIpo(applyIpo.getInitialPublicOfferId())) {
                if (true) {
                    // InitialPublicOffer ipo = hcq.getIpo(applyIpo.getInitialPublicOfferId());
                    InitialPublicOffer ipo = new InitialPublicOffer();
                    if (date.before(formatter.parse(ipo.getClosingDate()))) {
                        if (ipoApply.getSharesSubscribed() >= ipo.getStartingMinimumSubscription()) {
                            continuingSharesAvailable = ipoApply.getSharesSubscribed() - ipo.getStartingMinimumSubscription();
                            if (continuingSharesAvailable % ipo.getContinuingMinimumSubscription() == 0) {
                                amtPaid = ipoApply.getSharesSubscribed() * ipo.getOfferPrice();
                                if (ipoApply.getAmountPaid() == amtPaid) {
                                    // List<org.greenpole.hibernate.entity.IpoApplication> ipoApplyList = hcq.getIpoApplication(ipoApply.getInitialPublicOfferId(), false);
                                    List<org.greenpole.hibernate.entity.IpoApplication> ipoApplyList = new ArrayList<>();
                                    for (org.greenpole.hibernate.entity.IpoApplication ipoapp : ipoApplyList) {
                                        totalSharesBought += ipoapp.getSharesSubscribed();
                                    }
                                    remainingSharesNotBought = ipo.getTotalSharesOnOffer() - totalSharesBought;
                                    if (ipoApply.getSharesSubscribed() <= remainingSharesNotBought) {
                                        sharesSubscribedValue = ipoApply.getSharesSubscribed() * ipo.getOfferPrice();
                                        ipoApply.setSharesSubscribedValue(sharesSubscribedValue);
                                        ipoApply.setDateApplied(date.toString());
                                        iAppList.add(ipoApply);
                                    } else if ((ipoApply.getSharesSubscribed() > remainingSharesNotBought) && (remainingSharesNotBought >= ipo.getStartingMinimumSubscription())) {
                                        continuingSharesAvailable = remainingSharesNotBought - ipo.getStartingMinimumSubscription();
                                        if (continuingSharesAvailable % ipo.getContinuingMinimumSubscription() == 0) {
                                            continuingShares = (int) (Math.floor(continuingSharesAvailable / ipo.getContinuingMinimumSubscription()) * ipo.getContinuingMinimumSubscription());
                                            sharesSubscribed = ipo.getStartingMinimumSubscription() + continuingShares;
                                            sharesSubscribedValue = sharesSubscribed * ipo.getOfferPrice();
                                            returnMoney = (ipoApply.getSharesSubscribed() - sharesSubscribed) * ipo.getOfferPrice();
                                            ipoApply.setSharesSubscribedValue(sharesSubscribedValue);
                                            // ipoApply.setReturnMoney(returnMoney);
                                            ipoApply.setDateApplied(date.toString());
                                            iAppList.add(ipoApply);
                                        }
                                    }
                                    wrapper = new NotificationWrapper();
                                    props = new NotifierProperties(IpoApplicationLogic.class);
                                    queue = new QueueSender(props.getAuthoriserNotifierQueueFactory(), props.getAuthoriserNotifierQueueName());

                                    logger.info("Preparing notification for confirm on Initial Public Offer application of [{}] for [{}], invoked by [{}]", ipoApply.getIssuer(), ipoApply.getHolderId(), login.getUserId());

                                    wrapper.setCode(notification.createCode(login));
                                    wrapper.setDescription("Authenticate confirmation of Initial Public Offer Application for " + ipoApply.getHolderId());
                                    wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
                                    wrapper.setFrom(login.getUserId());
                                    wrapper.setTo(authenticator);
                                    wrapper.setModel(iAppList);
                                    resp = queue.sendAuthorisationRequest(wrapper);
                                    logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                                    resp.setRetn(0);
                                    // send SMS and/or Email notification
                                    return resp;
                                }
                                resp.setRetn(200);
                                resp.setDesc("The amount paid for shares and the price of shares to be bought should be the same.");
                                logger.info("The amount paid for shares and the price of shares to be bought should be the same. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                return resp;
                            }
                            resp.setRetn(200);
                            resp.setDesc("Continuing shares subscription should be in multiples of " + ipo.getContinuingMinimumSubscription());
                            logger.info("Continuing shares subscription is not in multiples of continuing minimum subscription - [{}]: [{}]", login.getUserId(), resp.getRetn());
                            return resp;
                        }

                    }
                    resp.setRetn(200);
                    resp.setDesc("Application for Initial Public Offer is past closing date and so holder cannot apply");
                    logger.info("Application for Initial Public Offer is past closing date and so holder cannot apply. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Client company has not declared Initial Public Offer and so holder cannot apply.");
                logger.info("Client company does not declared Initial Public Offer and so holder cannot apply. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Holder does not exists and so cannot apply for Initial Public Offer.");
            logger.info("Holder does not exists so cannot apply for Initial Public Offer - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process Initial Public Offer Application request. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            logger.info("error processing Initial Public Offer Application request. See error log - [{}]", login.getUserId());
            logger.error("error processing Initial Public Offer Application request. - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to create IPO application that has already been saved
     * as a notification file, according to the specified notification code
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response object to the IPO application creation request
     */
    public Response ipoApplication_Authorise(Login login, String notificationCode) {
        logger.info("Authorise Initial Public Offer application, invoked by [{}]", login.getUserId());
        Notification notification = new Notification();
        Response resp = new Response();
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        Date date = new Date();
        List<IpoApplication> iAppList = new ArrayList<>();

        int remainingSharesNotBought = 0;
        int totalSharesBought = 0;
        int returnShares = 0;
        int continuingShares = 0;
        double amtPaid = 0;
        double returnMoney = 0;
        int continuingSharesAvailable = 0;
        int sharesSubscribed = 0;
        double sharesSubscribedValue = 0;

        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(noteProp.getNotificationLocation(), notificationCode);
            List<IpoApplication> ipoApplicationList = (List<IpoApplication>) wrapper.getModel();
            IpoApplication ipoApply = ipoApplicationList.get(0);
            logger.info("Authorise Initial Public Offer application of [{}] for [{}], invoked by [{}]", ipoApply.getIssuer(), ipoApply.getHolderId(), login.getUserId());

            if (hq.checkHolderAccount(ipoApply.getHolderId())) {
                // if (hcq.checkIpo(applyIpo.getInitialPublicOfferId())) {
                if (true) {
                    // InitialPublicOffer ipo = hcq.getIpo(applyIpo.getInitialPublicOfferId());
                    InitialPublicOffer ipo = new InitialPublicOffer();
                    if (date.before(formatter.parse(ipo.getClosingDate()))) {
                        if (ipoApply.getSharesSubscribed() > ipo.getStartingMinimumSubscription()) {
                            continuingSharesAvailable = ipoApply.getSharesSubscribed() - ipo.getStartingMinimumSubscription();
                            if (continuingSharesAvailable % ipo.getContinuingMinimumSubscription() == 0) {
                                amtPaid = ipoApply.getSharesSubscribed() * ipo.getOfferPrice();
                                if (ipoApply.getAmountPaid() == amtPaid) {
                                    // List<org.greenpole.hibernate.entity.IpoApplication> ipoApplyList = hcq.getIpoApplication(ipoApply.getInitialPublicOfferId(), false);
                                    List<org.greenpole.hibernate.entity.IpoApplication> ipoApplyList = new ArrayList();
                                    for (org.greenpole.hibernate.entity.IpoApplication ipoapp : ipoApplyList) {
                                        totalSharesBought += ipoapp.getSharesSubscribed();
                                    }
                                    remainingSharesNotBought = ipo.getTotalSharesOnOffer() - totalSharesBought;
                                    if (ipoApply.getSharesSubscribed() <= remainingSharesNotBought) {
                                        sharesSubscribedValue = ipoApply.getSharesSubscribed() * ipo.getOfferPrice();
                                        ipoApply.setSharesSubscribedValue(sharesSubscribedValue);
                                        ipoApply.setReturnMoney(returnMoney);
                                        ipoApply.setCancelled(false);
                                    } else if ((ipoApply.getSharesSubscribed() > remainingSharesNotBought) && (remainingSharesNotBought >= ipo.getStartingMinimumSubscription())) {
                                        continuingSharesAvailable = remainingSharesNotBought - ipo.getStartingMinimumSubscription();
                                        if (continuingSharesAvailable % ipo.getContinuingMinimumSubscription() == 0) {
                                            sharesSubscribed = (int) (ipo.getStartingMinimumSubscription() + (Math.floor(continuingSharesAvailable / ipo.getContinuingMinimumSubscription()) * ipo.getContinuingMinimumSubscription()));
                                            returnMoney = (ipoApply.getSharesSubscribed() - sharesSubscribed) * ipo.getOfferPrice();
                                            sharesSubscribedValue = sharesSubscribed * ipo.getOfferPrice();
                                            ipoApply.setSharesSubscribedValue(sharesSubscribedValue);
                                            ipoApply.setReturnMoney(returnMoney);
                                            ipoApply.setCancelled(false);
                                        }
                                    }
                                    org.greenpole.hibernate.entity.IpoApplication ipoApplicationEntity = new org.greenpole.hibernate.entity.IpoApplication();

                                    ClearingHouse clHouseEntity = new ClearingHouse();
                                    clHouseEntity.setId(ipoApply.getClearingHouseId());

                                    Holder holderEntity = new Holder();
                                    holderEntity.setId(ipoApply.getHolderId());

                                    org.greenpole.hibernate.entity.InitialPublicOffer ipoEntity = new org.greenpole.hibernate.entity.InitialPublicOffer();
                                    ipoEntity.setId(ipoApply.getInitialPublicOfferId());

                                    ipoApplicationEntity.setClearingHouse(clHouseEntity);
                                    ipoApplicationEntity.setHolder(holderEntity);
                                    ipoApplicationEntity.setInitialPublicOffer(ipoEntity);
                                    ipoApplicationEntity.setIssuer(ipoApply.getIssuer());
                                    ipoApplicationEntity.setSharesSubscribed(ipoApply.getSharesSubscribed());
                                    ipoApplicationEntity.setAmountPaid(ipoApply.getAmountPaid());
                                    ipoApplicationEntity.setIssuingHouse(ipoApply.getIssuingHouse());
                                    ipoApplicationEntity.setSharesSubscribedValue(ipoApply.getSharesSubscribedValue());
                                    // ipoApplicationEntity.setReturnMoney(ipoApply.getReturnMoney());
                                    ipoApplicationEntity.setCanceled(ipoApply.isCancelled());
                                    ipoApplicationEntity.setProcessingPayment(ipoApply.isProcessingPayment());
                                    ipoApplicationEntity.setApproved(ipoApply.isApproved());

                                    HolderCompanyAccount holderCompAcct = new HolderCompanyAccount();
                                    Holder holder = new Holder();
                                    holder.setId(ipoApply.getHolderId());
                                    holderCompAcct.setHolder(holder);
                                    // create holder company account and ipo application . . .
                                    resp.setRetn(0);
                                    // hcq.createUpdateIpoApplication(ipoApply);
                                    hq.createUpdateHolderCompanyAccount(holderCompAcct);
                                    logger.info("Holder Company Account created for holder. [{}] - [{}]", login.getUserId(), resp.getRetn());
                                    resp.setDesc("Application for Initial Public Offer Successful");
                                    logger.info("Application for Initial Public Offer Successful. [{}] - [{}]", login.getUserId(), resp.getRetn());
                                    wrapper.setAttendedTo(true);
                                    notification.markAttended(notificationCode);
                                    // send SMS and/or Email notification
                                    return resp;
                                }
                                resp.setRetn(200);
                                resp.setDesc("The amount paid for shares and the price of shares to be bought should be the same.");
                                logger.info("The amount paid for shares and the price of shares to be bought should be the same. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                return resp;
                            }
                            resp.setRetn(200);
                            resp.setDesc("Continuing shares subscription should be in multiples of " + ipo.getContinuingMinimumSubscription());
                            logger.info("Continuing shares subscription is not in multiples of continuing minimum subscription - [{}]: [{}]", login.getUserId(), resp.getRetn());
                            return resp;
                        }
                        resp.setRetn(200);
                        resp.setDesc("Shares subscribed is less than minimum subscription required and so holder cannot apply for IPO.");
                        logger.info("Shares subscribed is less than minimum subscription required and so holder cannot apply for IPO. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Application for Initial Public Offer is past closing date and so holder cannot apply");
                    logger.info("Application for Initial Public Offer is past closing date and so holder cannot apply. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Client company has not declared Initial Public Offer and so holder cannot apply.");
                logger.info("Client company does not declared Initial Public Offer and so holder cannot apply. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Holder does not exists and so cannot apply for Initial Public Offer.");
            logger.info("Holder does not exists so cannot apply for Initial Public Offer - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (JAXBException ex) {
            resp.setRetn(98);
            resp.setDesc("Unable to process IPO application. Contact System Administrator");
            logger.info("Error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("Error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process IPO application. Contact system administrator.");
            logger.info("Error processing IPO application. See error log - [{}]", login.getUserId());
            logger.error("Error processing IPO application - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to cancel a shareholder's IPO application 
     * @param login the user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param ipoApply the object of IPO application to be canceled
     * @return response object to the cancel IPO application request
     */
    public Response cancelIpoApplication_Request(Login login, String authenticator, IpoApplication ipoApply) {
        logger.info("Request to cancel Initial Public Offer application of [{}] for [{}], invoked by [{}]", ipoApply.getIssuer(), ipoApply.getHolderId(), login.getUserId());
        Response resp = new Response();
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        Notification notification = new Notification();

        try {
            NotificationWrapper wrapper;
            QueueSender queue;
            NotifierProperties props;
            // org.greenpole.hibernate.entity.IpoApplication ipoApplicationEntity = hcq.getIpoApplication(ipoApply.getId());
            org.greenpole.hibernate.entity.IpoApplication ipoApplicationEntity = new org.greenpole.hibernate.entity.IpoApplication();
            if (ipoApplicationEntity.getProcessingPayment() || ipoApplicationEntity.getApproved()) {
                // if (true) {
                wrapper = new NotificationWrapper();
                props = new NotifierProperties(IpoApplicationLogic.class);
                queue = new QueueSender(props.getAuthoriserNotifierQueueFactory(), props.getAuthoriserNotifierQueueName());
                List<IpoApplication> iAppList = new ArrayList<>();
                iAppList.add(ipoApply);
                wrapper.setCode(notification.createCode(login));
                wrapper.setDescription("Authenticate cancellation of Initial Public Offer Application for " + ipoApply.getHolderId());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(iAppList);
                resp = queue.sendAuthorisationRequest(wrapper);
                logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                // send SMS and/or Email notification
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Initial Public Offer Application payment is being processed or has been approved and so cannot be cancelled");
            logger.info("Initial Public Offer Application payment is being processed or has been approved and so cannot be cancelled. - [{}]: [{}]", login.getUserId(), resp.getRetn());
            // send SMS and/or Email notification
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process Initial Public Offer Application request. Contact system administrator.");
            logger.info("error processing Initial Public Offer Application request. See error log - [{}]", login.getUserId());
            logger.error("error processing Initial Public Offer Application request. - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to cancel a shareholder's IPO request that has been saved
     * as a notification file, according to the specified notification code
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response object to the cancel IPO application request
     */
    public Response cancelIpoApplication_Authorise(Login login, String notificationCode) {
        logger.info("Authorise Initial Public Offer Application cancellation, invoked by [{}]", login.getUserId());
        Notification notification = new Notification();
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        Response resp = new Response();

        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(noteProp.getNotificationLocation(), notificationCode);
            List<IpoApplication> ipoApplicationList = (List<IpoApplication>) wrapper.getModel();
            IpoApplication ipoApply = ipoApplicationList.get(0);
            // org.greenpole.hibernate.entity.IpoApplication ipoApplicationEntity = hcq.getIpoApplication(ipoApply.getId());
            org.greenpole.hibernate.entity.IpoApplication ipoApplicationEntity = new org.greenpole.hibernate.entity.IpoApplication();
            if (ipoApplicationEntity.getProcessingPayment() || ipoApplicationEntity.getApproved()) {
                // if (true) {
                ipoApply.setCancelled(true);
                ipoApplicationEntity.setCanceled(ipoApply.isCancelled());
                // hcq.createUpdateIpoApplication(ipoApplicationEntity);
                resp.setRetn(0);
                resp.setDesc("Authorise Initial Public Offer Application cancellation successful");
                logger.info("Authorise Initial Public Offer Application cancellation successful - [{}]", login.getUserId());
                wrapper.setAttendedTo(true);
                notification.markAttended(notificationCode);
                // send SMS and/or Email notification
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Initial Public Offer Application payment is being processed or has been approved and so cannot be cancelled");
            logger.info("Initial Public Offer Application payment is being processed or has been approved and so cannot be cancelled. - [{}]: [{}]", login.getUserId(), resp.getRetn());
            // send SMS and/or Email notification
            return resp;
        } catch (JAXBException ex) {
            resp.setRetn(98);
            resp.setDesc("Unable to process IPO application. Contact System Administrator");
            logger.info("Error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("Error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process IPO application. Contact system administrator.");
            logger.info("Error processing IPO application. See error log - [{}]", login.getUserId());
            logger.error("Error processing IPO application - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to add share units to a shareholder's company account
     * @param login the user's login details
     * @param authenticator the authenticator meant to receive the notification code
     * @param ipoApply IPO application object
     * @return response object to the Add share unit request
     */
    public Response addShareUnit_Request(Login login, String authenticator, IpoApplication ipoApply) {
        logger.info("Request to add share units [{}], invoked by", login.getUserId());
        Response resp = new Response();
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        Notification notification = new Notification();
        List<IpoApplication> ipoAppList = new ArrayList<>();

        try {
            NotificationWrapper wrapper;
            QueueSender queue;
            NotifierProperties props;

            if ((ipoApply.getHolderId() > 0) && (ipoApply.getInitialPublicOfferId() > 0)) {
                ipoAppList.add(ipoApply);
                wrapper = new NotificationWrapper();
                props = new NotifierProperties(IpoApplicationLogic.class);
                queue = new QueueSender(props.getAuthoriserNotifierQueueFactory(), props.getAuthoriserNotifierQueueName());

                wrapper.setCode(notification.createCode(login));
                wrapper.setDescription("Authenticate addition of share units for holder " + ipoApply.getHolderId());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(ipoAppList);
                resp = queue.sendAuthorisationRequest(wrapper);
                logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                // send SMS and/or Email confirmation
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Error: Values for Holder Id or Initial Public Offer Id is missing or incorrect");
            logger.info("Values for Holder Id or Initial Public Offer Id is missing or incorrect - [{}]: [{}]", login.getUserId(), resp.getRetn());
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
     * @return response object to the Add Share Unit request
     */
    public Response addShareUnit_Authorise(Login login, String notificationCode) {
        logger.info("Authorisation request to add share unit [{}], invoked by", login.getUserId());
        Notification notification = new Notification();
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        Response resp = new Response();

        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(noteProp.getNotificationLocation(), notificationCode);
            List<PrivatePlacementApplication> ppApplicationList = (List<PrivatePlacementApplication>) wrapper.getModel();
            PrivatePlacementApplication ppApply = ppApplicationList.get(0);

            if ((ppApply.getHolderId() > 0) && (ppApply.getPrivatePlacementId() > 0)) {
                org.greenpole.hibernate.entity.IpoApplication ipoApp = new org.greenpole.hibernate.entity.IpoApplication();
                // org.greenpole.hibernate.entity.InitialPublicOffer ipo = hcq.getInitialPublicOffer(ppApply.getId());
                org.greenpole.hibernate.entity.InitialPublicOffer ipo = new org.greenpole.hibernate.entity.InitialPublicOffer();
                // InitialPublicOffer ipo = cq.getInitialPublicOffer(ipoApply.getIpoApplicationId());
                HolderCompanyAccount holderCompAcct = hq.getHolderCompanyAccount(ppApply.getHolderId(), ipo.getClientCompany().getId());
                holderCompAcct.setShareUnits(holderCompAcct.getShareUnits() + ipoApp.getSharesSubscribed());

                hq.createUpdateHolderCompanyAccount(holderCompAcct);

                resp.setRetn(0);
                resp.setDesc("Addition of share units successful");
                logger.info("Addition of share units successful - [{}]: [{}]", login.getUserId(), resp.getRetn());
                wrapper.setAttendedTo(true);
                notification.markAttended(notificationCode);
                // send SMS and/or Email confirmation
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Error: Values for Holder Id or Initial Public Offer Application Id is missing or incorrect");
            logger.info("Values for Holder Id or Initial Public Offer Application Id is missing or incorrect - [{}]: [{}]", login.getUserId(), resp.getRetn());
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
     *
     * @param login
     * @param authenticator
     * @param ipoApply
     * @return
     */
    public Response adjustShareUnit_Request(Login login, String authenticator, IpoApplication ipoApply) {
        logger.info("Request to adjust share units [{}], invoked by", login.getUserId());
        Response resp = new Response();
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        Notification notification = new Notification();
        List<IpoApplication> ipoAppList = new ArrayList<>();
        double returnMoney = 0;

        try {
            NotificationWrapper wrapper;
            QueueSender queue;
            NotifierProperties props;

            if ((ipoApply.getHolderId() > 0) && (ipoApply.getInitialPublicOfferId() > 0)) {
                if (ipoApply.getSharesSubscribed() > ipoApply.getSharesAdjusted()) {
                    int returnShares = (ipoApply.getSharesSubscribed() - ipoApply.getSharesAdjusted());
                    long timeDiff = date.getTime() - formatter.parse(ipoApply.getDateApplied()).getTime();
                    timeDiff = timeDiff / (24 * 60 * 60 * 1000);
                    // global value for rate and tax will be inserted
                    double rate = 0;
                    double tax = 0;
                    rate = rate / 100;
                    if (ipoApply.getHolder().isTaxExempted()) {
                        returnMoney = (returnShares * timeDiff * rate);
                    } else {
                        returnMoney = (returnShares * timeDiff * rate) - tax;
                    }
                    ipoApply.setReturnMoney(returnMoney);
                    ipoAppList.add(ipoApply);
                    resp.setRetn(0);
                    resp.setBody(ipoAppList);
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
            resp.setDesc("Error: Values for Holder Id or Initial Public Offer Application Id is missing or incorrect");
            logger.info("Values for Holder Id or Initial Public Offer Application Id is missing or incorrect - [{}]: [{}]", login.getUserId(), resp.getRetn());
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
     *
     * @param login
     * @param authenticator
     * @param ipoApply
     * @return
     */
    public Response adjustShareUnit_Confirmation(Login login, String authenticator, IpoApplication ipoApply) {
        logger.info("Request to adjust share units [{}], invoked by", login.getUserId());
        Response resp = new Response();
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        Notification notification = new Notification();
        List<IpoApplication> ipoAppList = new ArrayList<>();
        double returnMoney = 0;

        try {
            NotificationWrapper wrapper;
            QueueSender queue;
            NotifierProperties props;

            if ((ipoApply.getHolderId() > 0) && (ipoApply.getInitialPublicOfferId() > 0)) {
                if (ipoApply.getSharesSubscribed() > ipoApply.getSharesAdjusted()) {
                    int returnShares = (ipoApply.getSharesSubscribed() - ipoApply.getSharesAdjusted());
                    long timeDiff = date.getTime() - formatter.parse(ipoApply.getDateApplied()).getTime();
                    timeDiff = timeDiff / (24 * 60 * 60 * 1000);
                    // global value for rate and tax will be inserted
                    double rate = 0;
                    double tax = 0;
                    rate = rate / 100;
                    if (ipoApply.getHolder().isTaxExempted()) {
                        returnMoney = (returnShares * timeDiff * rate);
                    } else {
                        returnMoney = (returnShares * timeDiff * rate) - tax;
                    }
                    ipoApply.setReturnMoney(returnMoney);
                    ipoAppList.add(ipoApply);
                    wrapper = new NotificationWrapper();
                    props = new NotifierProperties(IpoApplicationLogic.class);
                    queue = new QueueSender(props.getAuthoriserNotifierQueueFactory(), props.getAuthoriserNotifierQueueName());

                    wrapper.setCode(notification.createCode(login));
                    wrapper.setDescription("Authenticate adjusting of share units for holder " + ipoApply.getHolderId());
                    wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
                    wrapper.setFrom(login.getUserId());
                    wrapper.setTo(authenticator);
                    wrapper.setModel(ipoAppList);
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
            resp.setDesc("Error: Values for Holder Id or Initial Public Offer Application Id is missing or incorrect");
            logger.info("Values for Holder Id or Initial Public Offer Application Id is missing or incorrect - [{}]: [{}]", login.getUserId(), resp.getRetn());
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
     *
     * @param login
     * @param notificationCode
     * @return
     */
    public Response adjustShareUnit_Authorise(Login login, String notificationCode) {
        logger.info("Authorisation request to adjust share unit [{}], invoked by", login.getUserId());
        Notification notification = new Notification();
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        Response resp = new Response();
        Date date = new Date();
        double returnMoney = 0;
        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(noteProp.getNotificationLocation(), notificationCode);
            List<IpoApplication> ipoApplicationList = (List<IpoApplication>) wrapper.getModel();
            IpoApplication ipoApply = ipoApplicationList.get(0);

            if ((ipoApply.getHolderId() > 0) && (ipoApply.getInitialPublicOfferId() > 0)) {
                if (ipoApply.getSharesSubscribed() > ipoApply.getSharesAdjusted()) {
                    int returnShares = (ipoApply.getSharesSubscribed() - ipoApply.getSharesAdjusted());
                    long timeDiff = date.getTime() - formatter.parse(ipoApply.getDateApplied()).getTime();
                    timeDiff = timeDiff / (24 * 60 * 60 * 1000);
                    // global value for rate and tax will be inserted
                    double rate = 0;
                    double tax = 0;
                    rate = rate / 100;
                    if (ipoApply.getHolder().isTaxExempted()) {
                        returnMoney = (returnShares * timeDiff * rate);
                    } else {
                        returnMoney = (returnShares * timeDiff * rate) - tax;
                    }
                    org.greenpole.hibernate.entity.IpoApplication ipoApp = new org.greenpole.hibernate.entity.IpoApplication();
                    // org.greenpole.hibernate.entity.PrivatePlacementApplication ppa = hcq.getPrivatePlacementApplication(ppApply.getId());
                    org.greenpole.hibernate.entity.InitialPublicOffer ipo = new org.greenpole.hibernate.entity.InitialPublicOffer();
                    // org.greenpole.hibernate.entity.InitialPublicOffer ipo = cq.getInitialPublicOffer(ipoApply.getInitialPublicOfferId());
                    HolderCompanyAccount holderCompAcct = hq.getHolderCompanyAccount(ipoApply.getHolderId(), ipo.getClientCompany().getId());
                    holderCompAcct.setShareUnits(ipoApply.getSharesSubscribed());
                    // persist update IPO application also
                    hq.createUpdateHolderCompanyAccount(holderCompAcct);

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
     * Processes request to upload IPO application en-mass
     * @param login the user's login details
     * @param authenticator the authenticator meant to receive the notification code
     * @param ipoApplyList a list of IPO applications
     * @return response object to the upload IPO application
     */
    public Response uploadIpoApplicationEnmass_Request(Login login, String authenticator, List<IpoApplication> ipoApplyList) {
        logger.info("Request to upload list of Initial Public Offer application, invoked by [{}]", login.getUserId());
        List<Response> respList = new ArrayList<>();
        List<ConfirmationDetails> confirmationList = new ArrayList<>();
        ConfirmationDetails confirm = new ConfirmationDetails();
        Response resp = new Response();
        Notification notification = new Notification();

        try {
            NotificationWrapper wrapper;
            QueueSender queue;
            NotifierProperties props;
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());

            wrapper = new NotificationWrapper();
            props = new NotifierProperties(IpoApplicationLogic.class);
            queue = new QueueSender(props.getAuthoriserNotifierQueueFactory(), props.getAuthoriserNotifierQueueName());

            logger.info("Preparing notification for a list of Initial Public Offer applications, invoked by [{}]", login.getUserId());
            wrapper.setCode(notification.createCode(login));

            for (IpoApplication ipoApplicant : ipoApplyList) {
                respList.add(validateHolderDetails(login, ipoApplicant.getHolder()));
                wrapper.setDescription("Authenticate holder, " + ipoApplicant.getHolder().getFirstName() + " " + ipoApplicant.getHolder().getLastName()
                        + "'s application for the IPO issued by - " + ipoApplicant.getIssuer());
            }
            wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
            wrapper.setFrom(login.getUserId());
            wrapper.setTo(authenticator);
            wrapper.setModel(ipoApplyList);
            resp = queue.sendAuthorisationRequest(wrapper);
            logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
            confirm.setTitle("List of Initial Public Offer Applications Status");
            confirm.setDetails(respList);
            confirmationList.add(confirm);
            resp.setRetn(0);
            resp.setBody(confirmationList);
            // send SMS and/or Email notification
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setBody(respList);
            resp.setDesc("General error. Unable to process Initial Public Offer Application request. Contact system administrator.");
            logger.info("error processing Initial Public Offer Application request. See error log - [{}]", login.getUserId());
            logger.error("error processing Initial Public Offer Application request. - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to upload shareholders IPO application en-mass that has been
     * saved as a notification file
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response object to the Upload IPO application en-mass
     */
    public Response uploadIpoApplicationEnmass_Authorise(Login login, String notificationCode) {
        logger.info("Authorise uploaded list of Initial Public Offer application, invoked by [{}]", login.getUserId());
        Notification notification = new Notification();
        List<ConfirmationDetails> confirmationList = new ArrayList<>();
        ConfirmationDetails confirm = new ConfirmationDetails();

        Response resp = new Response();
        Response validIpoApply;
        Response validHolder;
        List<Response> respList = new ArrayList<>();
        Date date = new Date();

        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(noteProp.getNotificationLocation(), notificationCode);
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            List<IpoApplication> ipoAppList = (List<IpoApplication>) wrapper.getModel();
            List<IpoApplication> ipoApplications = (List<IpoApplication>) ipoAppList.get(0);

            List<org.greenpole.entity.model.holder.Holder> successfulHolders = new ArrayList<>();
            List<IpoApplication> successfulIpoApplications = new ArrayList<>();
            List<IpoApplication> unsuccessfulIpoApplications = new ArrayList<>();
            List<org.greenpole.entity.model.holder.Holder> unsuccessfulHolders = new ArrayList<>();

            for (IpoApplication ipoApplicant : ipoApplications) {// check valid holders                
                validHolder = validateHolderDetails(login, ipoApplicant.getHolder());// check response code                
                if (validHolder.getRetn() == 0) {// process ipoApplication
                    validIpoApply = validateHolderIpoApplication(login, ipoApplicant);
                    if (validIpoApply.getRetn() == 0) {
                        successfulHolders.add(ipoApplicant.getHolder());
                        successfulIpoApplications.add(ipoApplicant);
                    }
                    unsuccessfulIpoApplications.add(ipoApplicant);
                } else {// ignore and list as unsuccessful
                    unsuccessfulHolders.add(ipoApplicant.getHolder());
                    unsuccessfulIpoApplications.add(ipoApplicant);
                }
            }
            // respList.add(validIpoApply);
            List<org.greenpole.hibernate.entity.Holder> holdEntityChnList = new ArrayList<>();
            List<org.greenpole.hibernate.entity.Holder> holdEntityNoChnList = new ArrayList<>();
            List<org.greenpole.hibernate.entity.IpoApplication> ipoApplicationEntityList = new ArrayList<>();

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

            for (IpoApplication ipoApp : successfulIpoApplications) {
                org.greenpole.hibernate.entity.IpoApplication ipoApplicationEntity = new org.greenpole.hibernate.entity.IpoApplication();

                ClearingHouse clHouse = new ClearingHouse();
                clHouse.setId(ipoApp.getClearingHouseId());
                ipoApplicationEntity.setClearingHouse(clHouse);

                Holder holder = new Holder();
                holder.setId(ipoApp.getHolderId());
                ipoApplicationEntity.setHolder(holder);

                org.greenpole.hibernate.entity.InitialPublicOffer ipo = new org.greenpole.hibernate.entity.InitialPublicOffer();
                ipo.setId(ipoApp.getInitialPublicOfferId());
                ipoApplicationEntity.setInitialPublicOffer(ipo);

                ipoApplicationEntity.setIssuer(ipoApp.getIssuer());
                ipoApplicationEntity.setSharesSubscribed(ipoApp.getSharesSubscribed());
                ipoApplicationEntity.setAmountPaid(ipoApp.getAmountPaid());
                ipoApplicationEntity.setIssuingHouse(ipoApp.getIssuingHouse());
                ipoApplicationEntity.setSharesSubscribedValue(ipoApp.getSharesSubscribedValue());
                ipoApplicationEntity.setSharesAdjusted(ipoApp.getSharesAdjusted());
                ipoApplicationEntity.setReturnMoney(ipoApp.getReturnMoney());
                ipoApplicationEntity.setProcessingPayment(ipoApp.isProcessingPayment());
                ipoApplicationEntity.setApproved(ipoApp.isApproved());
                ipoApplicationEntity.setCanceled(ipoApp.isCancelled());
                ipoApplicationEntity.setDateApplied(formatter.parse(ipoApp.getDateApplied()));

                ipoApplicationEntityList.add(ipoApplicationEntity);
            }
            boolean created = false;
            // TODO: persist list of holders and ipo applications together
            // code here:

            // created = hcq.createHolderAccount(holdEntity, emptyAcct,
            //         retrieveHolderResidentialAddress(holder), retrieveHolderPostalAddress(successfulHolders),
            //         retrieveHolderEmailAddress(successfulHolders), retrieveHolderPhoneNumber(successfulHolders), ipoApplicationEntityList);
            //set chn
            // created = hq.createHolderAccount(holdEntityChnList, retrieveHolderCompanyAccount(successfulHolders),
            //         retrieveHolderResidentialAddress(successfulHolders), retrieveHolderPostalAddress(successfulHolders),
            //         retrieveHolderEmailAddress(successfulHolders), retrieveHolderPhoneNumber(successfulHolders), ipoApplicationEntityList);
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
            resp.setRetn(320);
            resp.setBody(respList);
            resp.setDesc("General Error. Unable to persist holder account. Contact system administrator.");
            logger.info("Error persist holder account - [{}]", login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            resp.setRetn(98);
            resp.setDesc("Unable to process IPO application. Contact System Administrator");
            logger.info("Error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("Error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process IPO application. Contact system administrator.");
            logger.info("Error processing IPO application. See error log - [{}]", login.getUserId());
            logger.error("Error processing IPO application - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     *
     * @param login
     * @param holder
     * @return
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
     *
     * @param login
     * @param ipoApply
     * @return
     */
    private Response validateHolderIpoApplication(Login login, IpoApplication ipoApply) {
        logger.info("Validate Initial Public Offer application details of [{}] for [{}], invoked by [{}]", ipoApply.getIssuer(), ipoApply.getHolderId(), login.getUserId());
        Response resp = new Response();
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        Date date = new Date();

        try {
            NotificationWrapper wrapper;
            QueueSender queue;
            NotifierProperties props;

            List<IpoApplication> iAppList = new ArrayList<>();

            int remainingSharesNotBought = 0;
            int totalSharesBought = 0;
            int continuingSharesSubscribed = 0;
            int continuingShares = 0;
            double amtPaid = 0;
            double returnMoney = 0;
            int continuingSharesAvailable = 0;
            int sharesBought = 0;
            double sharesSubscribedValue = 0;
            if (hq.checkHolderAccount(ipoApply.getHolderId())) {
                // if (hcq.checkIpo(applyIpo.getInitialPublicOfferId())) {
                if (true) {
                    // InitialPublicOffer ipo = hcq.getIpo(applyIpo.getInitialPublicOfferId());
                    InitialPublicOffer ipo = new InitialPublicOffer();
                    if (date.before(formatter.parse(ipo.getClosingDate()))) {
                        if (ipoApply.getSharesSubscribed() >= ipo.getStartingMinimumSubscription()) {
                            continuingShares = ipoApply.getSharesSubscribed() - ipo.getStartingMinimumSubscription();
                            if (continuingShares % ipo.getContinuingMinimumSubscription() == 0) {
                                amtPaid = ipoApply.getSharesSubscribed() * ipo.getOfferPrice();
                                if (ipoApply.getAmountPaid() == amtPaid) {
                                    // List<IpoApplication> ipoApplyList = hcq.getIpoApplication(ipoApply.getInitialPublicOfferId(), false);
                                    List<IpoApplication> ipoApplyList = new ArrayList<>();
                                    for (IpoApplication ipoapp : ipoApplyList) {
                                        totalSharesBought += ipoapp.getSharesSubscribed();
                                    }
                                    remainingSharesNotBought = ipo.getTotalSharesOnOffer() - totalSharesBought;
                                    if (ipoApply.getSharesSubscribed() <= remainingSharesNotBought) {
                                        sharesSubscribedValue = ipoApply.getSharesSubscribed() * ipo.getOfferPrice();
                                        ipoApply.setSharesSubscribedValue(sharesSubscribedValue);
                                        ipoApply.setDateApplied(date.toString());
                                        iAppList.add(ipoApply);
                                        resp.setRetn(0);
                                        resp.setDesc("IPO Application Details for confirmation.");
                                        resp.setBody(iAppList);
                                        logger.info("IPO Application Details for confirmation. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                        // send SMS and/or Email notification
                                        return resp;
                                    } else if ((ipoApply.getSharesSubscribed() > remainingSharesNotBought) && (remainingSharesNotBought >= ipo.getStartingMinimumSubscription())) {
                                        continuingSharesAvailable = remainingSharesNotBought - ipo.getStartingMinimumSubscription();
                                        if (continuingSharesAvailable % ipo.getContinuingMinimumSubscription() == 0) {
                                            continuingSharesSubscribed = (int) (Math.floor(continuingSharesAvailable / ipo.getContinuingMinimumSubscription()) * ipo.getContinuingMinimumSubscription());
                                            sharesSubscribedValue = (ipo.getStartingMinimumSubscription() + continuingSharesSubscribed) * ipo.getOfferPrice();
                                            sharesBought = ipo.getStartingMinimumSubscription() + continuingSharesSubscribed;
                                            returnMoney = (ipoApply.getSharesSubscribed() - sharesBought) * ipo.getOfferPrice();
                                            ipoApply.setSharesSubscribedValue(sharesSubscribedValue);
                                            ipoApply.setDateApplied(date.toString());
                                            // ipoApply.setReturnMoney(returnMoney);
                                            iAppList.add(ipoApply);
                                            resp.setRetn(0);
                                            resp.setDesc("Insufficient quantity of shares. Number of shares available is: " + sharesBought);
                                            resp.setBody(iAppList);
                                            logger.info("Available shares is less than quantity to be bought. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                            return resp;
                                        }
                                        iAppList.add(ipoApply);
                                        resp.setBody(iAppList);
                                        resp.setRetn(200);
                                        resp.setDesc("Insufficient quantity of shares. Number of shares available is: " + remainingSharesNotBought);
                                        logger.info("Available shares is less than quantity to be bought. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                        return resp;
                                    }
                                    resp.setRetn(200);
                                    resp.setDesc("There is not enough shares to buy. Number of available shares is: " + remainingSharesNotBought);
                                    logger.info("There is not enough shares to buy. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                    return resp;
                                }
                                resp.setRetn(200);
                                resp.setDesc("The amount paid for shares and the price of shares to be bought should be the same.");
                                logger.info("The amount paid for shares and the price of shares to be bought should be the same. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                return resp;
                            }
                            resp.setRetn(200);
                            resp.setDesc("Continuing shares subscription should be in multiples of " + ipo.getContinuingMinimumSubscription());
                            logger.info("Continuing shares subscription is not in multiples of continuing minimum subscription - [{}]: [{}]", login.getUserId(), resp.getRetn());
                            return resp;
                        }
                        resp.setRetn(200);
                        resp.setDesc("Shares subscribed is less than minimum subscription required and so holder cannot apply for IPO.");
                        logger.info("Shares subscribed is less than minimum subscription required and so holder cannot apply for IPO. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Application for Initial Public Offer is past closing date and so holder cannot apply");
                    logger.info("Application for Initial Public Offer is past closing date and so holder cannot apply. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Client company has not declared Initial Public Offer and so holder cannot apply.");
                logger.info("Client company does not declared Initial Public Offer and so holder cannot apply. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Holder does not exists and so cannot apply for Initial Public Offer.");
            logger.info("Holder does not exists so cannot apply for Initial Public Offer - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process Initial Public Offer Application request. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            logger.info("error processing Initial Public Offer Application request. See error log - [{}]", login.getUserId());
            logger.error("error processing Initial Public Offer Application request. - [" + login.getUserId() + "]", ex);
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
                // HolderResidentialAddressId rAddyId = new HolderResidentialAddressId();
//                rAddyId.setAddressLine1(rAddy.getAddressLine1());
//                rAddyId.setState(rAddy.getState());
//                rAddyId.setCountry(rAddy.getCountry());
//
//                residentialAddressEntity.setId(rAddyId);
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
