/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph;

import java.text.MessageFormat;
import org.greenpole.entirycode.jeph.model.ConfirmationDetails;
import org.greenpole.entirycode.jeph.model.IpoApplication;
import org.greenpole.entirycode.jeph.model.PrivatePlacementApplication;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.model.Address;
import org.greenpole.entity.model.EmailAddress;
import org.greenpole.entity.model.PhoneNumber;
import org.greenpole.entity.model.clientcompany.InitialPublicOffer;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationType;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.entity.sms.TextSend;
import org.greenpole.entity.tags.AddressTag;
import org.greenpole.hibernate.entity.ClearingHouse;
import org.greenpole.hibernate.entity.ClientCompany;
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
import org.greenpole.util.Manipulator;
import org.greenpole.util.Notification;
import org.greenpole.util.properties.GreenpoleProperties;
import org.greenpole.util.properties.NotificationProperties;
import org.greenpole.util.properties.NotifierProperties;
import org.greenpole.util.properties.SMSProperties;
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
    private final GreenpoleProperties greenProp = GreenpoleProperties.getInstance();
    private final NotificationProperties notificationProp = NotificationProperties.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(IpoApplicationLogic.class);
    // SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy");

    /**
     * Processes request to validate IPO application before creation
     * @param login the user's login details
     * @param ipoApp the IPO application object to be validated
     * @return response object to IPO application validation request
     */
    public Response applyForIpo_Confirmation_Request(Login login, IpoApplication ipoApp) {
        logger.info("Request to create Initial Public Offer application, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            Date date = new Date();
            List<IpoApplication> appList = new ArrayList<>();

            long remainingSharesNotBought;
            long totalSharesBought = 0;
            long continuingShares;
            double amtToBePaid;
            double sharesSubscribedValue;
            if (hq.checkHolderAccount(ipoApp.getHolderId())) {
                if (cq.checkActiveInitialPublicOffer(ipoApp.getInitialPublicOfferId())) {//ipo must be active
                    org.greenpole.hibernate.entity.InitialPublicOffer ipo = cq.getActiveClientCompanyIpo(ipoApp.getInitialPublicOfferId());
                    if (ipoApp.getSharesSubscribed() >= ipo.getStartingMinSub()) {//application must have subscribed shares greater than min starting subscription
                        continuingShares = ipoApp.getSharesSubscribed() - ipo.getStartingMinSub();
                        if (continuingShares % ipo.getContMinSub() == 0) {//application must have continuing shares in multiples of continuing subscription
                            amtToBePaid = ipoApp.getSharesSubscribed() * ipo.getOfferPrice();
                            if (ipoApp.getAmountPaid() == amtToBePaid) {//application must have amount paid calculated to the amount to be paid
                                List<org.greenpole.hibernate.entity.IpoApplication> ipoList = cq.getActiveIpoApplications(ipoApp.getInitialPublicOfferId());
                                for (org.greenpole.hibernate.entity.IpoApplication ipoapp : ipoList) {
                                    totalSharesBought += ipoapp.getSharesSubscribed();//get all shares subscribed from both approved and processing applications
                                }
                                remainingSharesNotBought = ipo.getTotalSharesOnOffer() - totalSharesBought;
                                if (ipoApp.getSharesSubscribed() <= remainingSharesNotBought) {//just a check for information purpose
                                    resp.setRetn(0);
                                    resp.setDesc("IPO application checks out.");
                                    logger.info("IPO application checks out - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                } else {
                                    resp.setRetn(0);
                                    resp.setDesc("IPO application's subscription is greater than available shares.");
                                    logger.info("IPO application's subscription is greater than available shares - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                }
                                sharesSubscribedValue = ipoApp.getSharesSubscribed() * ipo.getOfferPrice();
                                ipoApp.setSharesSubscribedValue(sharesSubscribedValue);
                                ipoApp.setDateApplied(formatter.format(date));
                                appList.add(ipoApp);
                                resp.setBody(appList);
                                return resp;
                            }
                            resp.setRetn(200);
                            resp.setDesc("The amount paid for shares and the price of shares to be bought should be the same.");
                            logger.info("The amount paid for shares and the price of shares to be bought should be the same. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                            return resp;
                        }
                        resp.setRetn(200);
                        resp.setDesc("Continuing shares subscription should be in multiples of " + ipo.getContMinSub());
                        logger.info("Continuing shares subscription is not in multiples of continuing minimum subscription - [{}]: [{}]", login.getUserId(), resp.getRetn());
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Shares subscribed is less than minimum subscription required and so holder cannot apply for IPO.");
                    logger.info("Shares subscribed is less than minimum subscription required and so holder cannot apply for IPO. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Initial Public Offer is past closing date and so holder cannot apply");
                logger.info("Initial Public Offer is past closing date and so holder cannot apply. - [{}]: [{}]", login.getUserId(), resp.getRetn());
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
     * @param ipoApp the IPO application object to be created
     * @return response object to for creation of IPO application
     */
    public Response applyForIpo_Request(Login login, String authenticator, IpoApplication ipoApp) {
        logger.info("Request to confirm Initial Public Offer application, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        
        try {
            Notification notification = new Notification();
            List<IpoApplication> iAppList = new ArrayList<>();
            
            NotificationWrapper wrapper;
            QueueSender queue;
            NotifierProperties prop;
            
            if (hq.checkHolderAccount(ipoApp.getHolderId())) {
                wrapper = new NotificationWrapper();
                prop = NotifierProperties.getInstance();
                queue = new QueueSender(prop.getNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
                
                logger.info("Preparing notification for confirmation of Initial Public Offer application");
                
                //set necessary variables
                ipoApp.setCancelled(false);
                ipoApp.setApproved(false);
                ipoApp.setProcessingPayment(true);
                
                Holder h = hq.getHolder(ipoApp.getHolderId());
                iAppList.add(ipoApp);
                
                wrapper.setCode(notification.createCode(login));
                wrapper.setDescription("Authorise confirmation of Initial Public Offer Application for " + 
                        h.getFirstName() + " " + h.getLastName());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setNotificationType(NotificationType.apply_for_ipo.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(iAppList);
                
                logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                resp = queue.sendAuthorisationRequest(wrapper);
                
                if (resp.getRetn() == 0) {
                    SMSProperties smsProp = SMSProperties.getInstance();
                    int holderId = ipoApp.getHolderId();
                    String holderPhone = hq.getHolderPryPhoneNumber(holderId);
                    ClientCompany cc = cq.getIpoClientCompany(ipoApp.getInitialPublicOfferId());

                    String text = MessageFormat.format(smsProp.getTextIpoProcessing(), cc.getName());
                    String notificationType = NotificationType.apply_for_ipo.toString();
                    boolean function_status = Boolean.valueOf(smsProp.getTextIpoSend());

                    sendTextMessage(holderPhone, wrapper.getCode(), text, notificationType, holderId, function_status, true, login);
                }
                
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
    public Response applyForIpo_Authorise(Login login, String notificationCode) {
        logger.info("Authorise Initial Public Offer application, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        
        long continuingShares = 0;
        double amtToBePaid = 0;

        try {
            Notification notification = new Notification();
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<IpoApplication> ipoApplicationList = (List<IpoApplication>) wrapper.getModel();
            IpoApplication ipoApp = ipoApplicationList.get(0);
            logger.info("Authorise Initial Public Offer application of [{}] for [{}], invoked by [{}]", ipoApp.getIssuer(), ipoApp.getHolderId(), login.getUserId());
            
            if (hq.checkHolderAccount(ipoApp.getHolderId())) {
                if (cq.checkActiveInitialPublicOffer(ipoApp.getInitialPublicOfferId())) {//ipo must be active
                    org.greenpole.hibernate.entity.InitialPublicOffer ipo = cq.getActiveClientCompanyIpo(ipoApp.getInitialPublicOfferId());
                    if (ipoApp.getSharesSubscribed() >= ipo.getStartingMinSub()) {//application must have subscribed shares greater than min starting subscription
                        continuingShares = ipoApp.getSharesSubscribed() - ipo.getStartingMinSub();
                        if (continuingShares % ipo.getContMinSub() == 0) {//application must have continuing shares in multiples of continuing subscription
                            amtToBePaid = ipoApp.getSharesSubscribed() * ipo.getOfferPrice();
                            if (ipoApp.getAmountPaid() == amtToBePaid) {//application must have amount paid calculated to the amount to be paid
                                org.greenpole.hibernate.entity.IpoApplication ipoApplicationEntity = new org.greenpole.hibernate.entity.IpoApplication();
                                
                                ClearingHouse clHouseEntity = cq.getClearingHouse(ipoApp.getClearingHouseId());
                                Holder holderEntity = hq.getHolder(ipoApp.getHolderId());
                                org.greenpole.hibernate.entity.InitialPublicOffer ipoEntity = cq.getIpo(ipoApp.getInitialPublicOfferId());
                                
                                ipoApplicationEntity.setClearingHouse(clHouseEntity);
                                ipoApplicationEntity.setHolder(holderEntity);
                                ipoApplicationEntity.setInitialPublicOffer(ipoEntity);
                                ipoApplicationEntity.setIssuer(ipoApp.getIssuer());
                                ipoApplicationEntity.setSharesSubscribed(ipoApp.getSharesSubscribed());
                                ipoApplicationEntity.setAmountPaid(ipoApp.getAmountPaid());
                                ipoApplicationEntity.setIssuingHouse(ipoApp.getIssuingHouse());
                                ipoApplicationEntity.setSharesSubscribedValue(ipoApp.getSharesSubscribedValue());
                                ipoApplicationEntity.setCanceled(false);
                                ipoApplicationEntity.setDateApplied(formatter.parse(ipoApp.getDateApplied()));
                                ipoApplicationEntity.setProcessingPayment(true);
                                ipoApplicationEntity.setApproved(true);
                                
                                boolean applied = hq.applyForIpo(ipoApplicationEntity);
                                
                                if (applied) {
                                    resp.setRetn(0);
                                    resp.setDesc("Application for Initial Public Offer Successful");
                                    logger.info("Application for Initial Public Offer Successful - [{}]", login.getUserId());
                                    
                                    notification.markAttended(notificationCode);
                                    SMSProperties smsProp = SMSProperties.getInstance();
                                    int holderId = ipoApp.getHolderId();
                                    String holderPhone = hq.getHolderPryPhoneNumber(holderId);
                                    ClientCompany cc = cq.getIpoClientCompany(ipoApp.getInitialPublicOfferId());
                                    
                                    String text = MessageFormat.format(smsProp.getTextIpoPaymentSuccess(), cc.getName());
                                    String notificationType = NotificationType.apply_for_ipo.toString();
                                    boolean function_status = Boolean.valueOf(smsProp.getTextIpoSend());
                                    
                                    sendTextMessage(holderPhone, wrapper.getCode(), text, notificationType, holderId, function_status, true, login);
                                    return resp;
                                }
                                resp.setRetn(200);
                                resp.setDesc("Application for Initial Public Offer failed. Please contact Administrator");
                                logger.info("Application for Initial Public Offer failed. Please contact Administrator - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                return resp;
                            }
                            resp.setRetn(200);
                            resp.setDesc("The amount paid for shares and the price of shares to be bought should be the same.");
                            logger.info("The amount paid for shares and the price of shares to be bought should be the same. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                            return resp;
                        }
                        resp.setRetn(200);
                        resp.setDesc("Continuing shares subscription should be in multiples of " + ipo.getContMinSub());
                        logger.info("Continuing shares subscription is not in multiples of continuing minimum subscription - [{}]: [{}]", login.getUserId(), resp.getRetn());
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Shares subscribed is less than minimum subscription required and so holder cannot apply for IPO.");
                    logger.info("Shares subscribed is less than minimum subscription required and so holder cannot apply for IPO. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Initial Public Offer is past closing date and so holder cannot apply");
                logger.info("Initial Public Offer is past closing date and so holder cannot apply. - [{}]: [{}]", login.getUserId(), resp.getRetn());
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
     * @param ipoApp the object of IPO application to be cancelled
     * @return response object to the cancel IPO application request
     */
    public Response cancelIpoApplication_Request(Login login, String authenticator, IpoApplication ipoApp) {
        logger.info("Request to cancel Initial Public Offer application, invoked by [{}]", login.getUserId());
        Response resp = new Response();

        try {
            Notification notification = new Notification();
            
            NotificationWrapper wrapper;
            QueueSender queue;
            NotifierProperties prop;
            
            org.greenpole.hibernate.entity.IpoApplication ipoApplicationEntity = hq.getIpoApplication(ipoApp.getInitialPublicOfferId());
            if (!ipoApplicationEntity.getApproved()) {
                wrapper = new NotificationWrapper();
                prop = NotifierProperties.getInstance();
                queue = new QueueSender(prop.getNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
                
                List<IpoApplication> iAppList = new ArrayList<>();
                iAppList.add(ipoApp);
                
                Holder h = hq.getHolder(ipoApp.getHolderId());
                
                wrapper.setCode(notification.createCode(login));
                wrapper.setDescription("Authorise cancellation of Initial Public Offer Application for " + 
                        h.getFirstName() + " " + h.getLastName());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setNotificationType(NotificationType.cancel_ipo.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(iAppList);
                
                logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                resp = queue.sendAuthorisationRequest(wrapper);
                
                if (resp.getRetn() == 0) {
                    SMSProperties smsProp = SMSProperties.getInstance();
                    int holderId = ipoApp.getHolderId();
                    String holderPhone = hq.getHolderPryPhoneNumber(holderId);
                    ClientCompany cc = cq.getIpoClientCompany(ipoApp.getInitialPublicOfferId());
                    
                    String text = MessageFormat.format(smsProp.getTextIpoCancelProcessing(), cc.getName());
                    String notificationType = NotificationType.cancel_ipo.toString();
                    boolean function_status = Boolean.valueOf(smsProp.getTextIpoSend());

                    sendTextMessage(holderPhone, wrapper.getCode(), text, notificationType, holderId, function_status, true, login);
                }
                
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Initial Public Offer Application payment is being processed or has been approved and so cannot be cancelled");
            logger.info("Initial Public Offer Application payment is being processed or has been approved and so cannot be cancelled. - [{}]: [{}]", login.getUserId(), resp.getRetn());
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
        Response resp = new Response();

        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<IpoApplication> ipoAppList = (List<IpoApplication>) wrapper.getModel();
            IpoApplication ipoApp = ipoAppList.get(0);
            
            org.greenpole.hibernate.entity.IpoApplication ipoApplicationEntity = hq.getIpoApplication(ipoApp.getInitialPublicOfferId());
            if (!ipoApplicationEntity.getApproved()) {
                ipoApplicationEntity.setCanceled(true);
                ipoApplicationEntity.setProcessingPayment(false);
                
                boolean updated = hq.applyForIpo(ipoApplicationEntity);
                
                if (updated) {
                    resp.setRetn(0);
                    resp.setDesc("Successful");
                    logger.info("Authorise Initial Public Offer Application cancellation successful - [{}]", login.getUserId());
                    
                    notification.markAttended(notificationCode);
                    SMSProperties smsProp = SMSProperties.getInstance();
                    int holderId = ipoApp.getHolderId();
                    String holderPhone = hq.getHolderPryPhoneNumber(holderId);
                    ClientCompany cc = cq.getIpoClientCompany(ipoApp.getInitialPublicOfferId());
                    
                    String text = MessageFormat.format(smsProp.getTextIpoCancelConfirm(), cc.getName());
                    String notificationType = NotificationType.cancel_ipo.toString();
                    boolean function_status = Boolean.valueOf(smsProp.getTextIpoSend());

                    sendTextMessage(holderPhone, wrapper.getCode(), text, notificationType, holderId, function_status, true, login);
                    
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Initial Public Offer Application cancellation unsuccessful. Please contract System Administrator.");
                logger.info("Initial Public Offer Application cancellation unsuccessful. Please contract System Administrator - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Initial Public Offer Application payment is being processed or has been approved and so cannot be cancelled");
            logger.info("Initial Public Offer Application payment is being processed or has been approved and so cannot be cancelled. - [{}]: [{}]", login.getUserId(), resp.getRetn());
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
     * @param authenticator the authenticator meant to receive the notification
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
            NotifierProperties prop;

            if ((ipoApply.getHolderId() > 0) && (ipoApply.getInitialPublicOfferId() > 0)) {
                ipoAppList.add(ipoApply);
                wrapper = new NotificationWrapper();
                prop = NotifierProperties.getInstance();
                queue = new QueueSender(prop.getNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());

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
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
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
            NotifierProperties prop;

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
            NotifierProperties prop;

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
                    prop = NotifierProperties.getInstance();
                    queue = new QueueSender(prop.getNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());

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
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
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
     * @param ipoAppList a list of IPO applications
     * @return response object to the upload IPO application
     */
    public Response uploadIpoApplicationEnmass_Request(Login login, String authenticator, List<IpoApplication> ipoAppList) {
        logger.info("Request to upload list of Initial Public Offer application, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        
        List<Response> positiveResponses = new ArrayList<>();
        List<ConfirmationDetails> confirmationList = new ArrayList<>();
        ConfirmationDetails confirm = new ConfirmationDetails();

        try {
            NotificationWrapper wrapper;
            QueueSender queue;
            NotifierProperties prop;
            
            boolean flag = false;
            String fail_desc = "";
            
            wrapper = new NotificationWrapper();
            prop = NotifierProperties.getInstance();
            queue = new QueueSender(prop.getNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
            
            for (IpoApplication ipoApplicant : ipoAppList) {
                Response rulesCheck = validateIpoApplication(login, ipoApplicant);
                if (rulesCheck.getRetn() != 0) {
                    return rulesCheck;//return and exit
                }
            }

            for (IpoApplication ipoApplicant : ipoAppList) {
                Response detailsCheck = validateHolderDetails(login, ipoApplicant.getHolder());
                Response recordCheck = validateHolderRecord(login, ipoApplicant.getHolder());
                
                if (detailsCheck.getRetn() == 0 && recordCheck.getRetn() == 0) {
                    positiveResponses.add(recordCheck);
                }
                
                if (detailsCheck.getRetn() != 0) {
                    flag = true;
                    fail_desc += "\n" + detailsCheck.getDesc();
                } else if (recordCheck.getRetn() != 0) {
                    flag = true;
                    fail_desc += "\n" + recordCheck.getDesc();
                }
            }
            
            if (flag) {
                resp.setRetn(200);
                resp.setDesc(fail_desc);
                return resp;
            }
            
            ClientCompany cc_info = cq.getIpoClientCompany(ipoAppList.get(0).getInitialPublicOfferId());
            
            wrapper.setCode(notification.createCode(login));
            wrapper.setDescription("Authorise multiple application for the IPO issued by - " + cc_info.getName());
            wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
            wrapper.setMessageTag(NotificationType.apply_for_ipo_enmass.toString());
            wrapper.setFrom(login.getUserId());
            wrapper.setTo(authenticator);
            wrapper.setModel(ipoAppList);
            
            logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
            resp = queue.sendAuthorisationRequest(wrapper);
            
            confirm.setTitle("List of Initial Public Offer Applications Status");
            confirm.setDetails(positiveResponses);
            confirmationList.add(confirm);
            
            resp.setBody(confirmationList);
            // send SMS and/or Email notification
            if (resp.getRetn() == 0) {
                SMSProperties smsProp = SMSProperties.getInstance();
                org.greenpole.util.Date txtdate = new org.greenpole.util.Date();
                Map<String, String> phoneMessageIds = new HashMap<>();
                for (IpoApplication ipoApp : ipoAppList) {
                    String holderPhone = "";
                    boolean foundNumber = false;
                    if (ipoApp.getHolder().getPhoneNumbers() != null && !ipoApp.getHolder().getPhoneNumbers().isEmpty()) {
                        for (PhoneNumber phone : ipoApp.getHolder().getPhoneNumbers()) {
                            if (phone.isPrimaryPhoneNumber()) {
                                if (phone.getPhoneNumber() != null && !"".equals(phone.getPhoneNumber())) {
                                    holderPhone = phone.getPhoneNumber();
                                    foundNumber = true;
                                    break;
                                }
                            }
                        }
                    }
                    
                    if (foundNumber) {
                        String msgId = ipoApp.getHolder().getFirstName().substring(0, 3) + "_" +
                                ipoApp.getHolder().getLastName().substring(0, 3) + "_" + txtdate.getDateTime();
                        phoneMessageIds.put(holderPhone, msgId);
                    }
                }
                
                if (!phoneMessageIds.isEmpty()) {
                    ClientCompany cc = cq.getIpoClientCompany(ipoAppList.get(0).getInitialPublicOfferId());
                    
                    String text = MessageFormat.format(smsProp.getTextIpoProcessing(), cc.getName());
                    String notificationType = NotificationType.apply_for_ipo_enmass.toString();
                    boolean function_status = Boolean.valueOf(smsProp.getTextIpoSend());
                    
                    sendBulkTextMessage(text, phoneMessageIds, notificationType, function_status, false, login);
                }
            }
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setBody(positiveResponses);
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
        List<Response> positiveResponses = new ArrayList<>();

        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            List<IpoApplication> ipoAppList = (List<IpoApplication>) wrapper.getModel();
            
            List<IpoApplication> approvedIpoApplications = new ArrayList<>();
            
            boolean flag = false;
            String fail_desc = "";
            
            for (IpoApplication ipoApplicant : ipoAppList) {
                Response rulesCheck = validateIpoApplication(login, ipoApplicant);
                if (rulesCheck.getRetn() != 0) {
                    return rulesCheck;//return and exit
                }
            }

            for (IpoApplication ipoApplicant : ipoAppList) {
                Response detailsCheck = validateHolderDetails(login, ipoApplicant.getHolder());
                Response recordCheck = validateHolderRecord(login, ipoApplicant.getHolder());
                
                if (detailsCheck.getRetn() == 0 && recordCheck.getRetn() == 0) {
                    approvedIpoApplications.add(ipoApplicant);
                    positiveResponses.add(recordCheck);
                }
                
                if (detailsCheck.getRetn() != 0) {
                    flag = true;
                    fail_desc += "\n" + detailsCheck.getDesc();
                } else if (recordCheck.getRetn() != 0) {
                    flag = true;
                    fail_desc += "\n" + recordCheck.getDesc();
                }
            }
            
            if (flag) {
                resp.setRetn(200);
                resp.setDesc(fail_desc);
                return resp;
            }
            
            List<org.greenpole.hibernate.entity.IpoApplication> ipoApplicationEntityList = new ArrayList<>();
            for (IpoApplication ipoApp : approvedIpoApplications) {
                org.greenpole.hibernate.entity.IpoApplication ipoApplicationEntity = new org.greenpole.hibernate.entity.IpoApplication();

                ClearingHouse clHouse = cq.getClearingHouse(ipoApp.getClearingHouseId());
                Holder holder = hq.getHolder(ipoApp.getHolderId());
                org.greenpole.hibernate.entity.InitialPublicOffer ipo = cq.getIpo(ipoApp.getInitialPublicOfferId());
                
                ipoApplicationEntity.setClearingHouse(clHouse);
                ipoApplicationEntity.setHolder(holder);
                ipoApplicationEntity.setInitialPublicOffer(ipo);
                
                ipoApplicationEntity.setIssuer(ipoApp.getIssuer());
                ipoApplicationEntity.setSharesSubscribed(ipoApp.getSharesSubscribed());
                ipoApplicationEntity.setAmountPaid(ipoApp.getAmountPaid());
                ipoApplicationEntity.setIssuingHouse(ipoApp.getIssuingHouse());
                ipoApplicationEntity.setSharesSubscribedValue(ipoApp.getSharesSubscribedValue());
                ipoApplicationEntity.setCanceled(false);
                ipoApplicationEntity.setDateApplied(formatter.parse(ipoApp.getDateApplied()));
                ipoApplicationEntity.setProcessingPayment(true);
                ipoApplicationEntity.setApproved(true);
                
                ipoApplicationEntityList.add(ipoApplicationEntity);
            }
            
            boolean created = hq.applyForIpoMultiple(ipoApplicationEntityList);
            
            if (created) {
                notification.markAttended(notificationCode);
                confirm.setDetails(positiveResponses);
                confirmationList.add(confirm);
                resp.setRetn(0);
                resp.setBody(confirmationList);
                resp.setDesc("Successful");
                logger.info("Shareholder account creation successful - [{}]", login.getUserId());
                
                
                SMSProperties smsProp = SMSProperties.getInstance();
                org.greenpole.util.Date txtdate = new org.greenpole.util.Date();
                Map<String, String> phoneMessageIds = new HashMap<>();
                for (IpoApplication ipoApp : ipoAppList) {
                    String holderPhone = "";
                    boolean foundNumber = false;
                    if (ipoApp.getHolder().getPhoneNumbers() != null && !ipoApp.getHolder().getPhoneNumbers().isEmpty()) {
                        for (PhoneNumber phone : ipoApp.getHolder().getPhoneNumbers()) {
                            if (phone.isPrimaryPhoneNumber()) {
                                if (phone.getPhoneNumber() != null && !"".equals(phone.getPhoneNumber())) {
                                    holderPhone = phone.getPhoneNumber();
                                    foundNumber = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (foundNumber) {
                        String msgId = ipoApp.getHolder().getFirstName().substring(0, 3) + "_"
                                + ipoApp.getHolder().getLastName().substring(0, 3) + "_" + txtdate.getDateTime();
                        phoneMessageIds.put(holderPhone, msgId);
                    }
                }

                if (!phoneMessageIds.isEmpty()) {
                    ClientCompany cc = cq.getIpoClientCompany(ipoAppList.get(0).getInitialPublicOfferId());

                    String text = MessageFormat.format(smsProp.getTextIpoPaymentSuccess(), cc.getName());
                    String notificationType = NotificationType.apply_for_ipo_enmass.toString();
                    boolean function_status = Boolean.valueOf(smsProp.getTextIpoSend());

                    sendBulkTextMessage(text, phoneMessageIds, notificationType, function_status, false, login);
                }
                
                return resp;
            }
            resp.setRetn(320);
            resp.setBody(positiveResponses);
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
    
    private Response validateHolderRecord(Login login, org.greenpole.entity.model.holder.Holder holder) {
        Response resp = new Response();
        String desc = "";
        String addendum = "";
        boolean flag = false;
        
        try {
            flag =  true;
            
            if (!"".equals(holder.getChn()) && holder.getChn() != null) {
                if (hq.checkHolderAccount(holder.getChn())) {
                    Holder h = hq.getHolder(holder.getChn());
                    if (h.getFirstName().equalsIgnoreCase(holder.getFirstName())
                            && h.getLastName().equalsIgnoreCase(holder.getLastName())) {
                        flag = true;
                    } else {
                        desc += "\nThe provided CHN belongs to a holder with a different first and last name combination";
                        flag = false;
                    }
                } else {
                    desc += "\nThe provided CHN does not exist.";
                    flag = false;
                }
            } else if (hq.checkHolderAccount(holder.getFirstName(), holder.getLastName())) {
                addendum += "The applicant - " + holder.getFirstName() + " " + holder.getLastName() + 
                        " - already has an account in the system, though it is without a CHN";
            }
            
            if (flag) {
                resp.setRetn(0);
                resp.setDesc("Successful validation\n"+
                        addendum);
                logger.info("Validation of holder database record successful - [{}] [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(1);
            resp.setDesc("Error validating holder database record: " + desc);
            logger.info("Error validating holder database record: [{}] - [{}]", desc, login.getUserId());
            return resp;
            
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable check holder's details in database. Contact system administrator.");
            logger.info("Error checking holder's details in database. See error log - [{}]", login.getUserId());
            logger.error("Error checking holder's details in database - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

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

        if (flag && (!"".equals(holder.getChn()) && holder.getChn() != null)) {
            if (hq.checkHolderAccount(holder.getChn())) {
                flag = true;
            } else {
                desc += "\nThe CHN for " + holder.getFirstName() + " " + holder.getLastName() + "does not exists";
                flag = false;
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

        if (flag && holder.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                && holder.getResidentialAddresses() != null && !holder.getResidentialAddresses().isEmpty()) {
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

        if (flag && holder.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString())
                && holder.getPostalAddresses() != null && !holder.getPostalAddresses().isEmpty()) {
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
                if (email.isPrimaryEmail() && (email.getEmailAddress() == null || "".equals(email.getEmailAddress()))) {
                    desc += "\nEmail address should not be empty. Delete email entry if you must";
                    flag = false;
                    break;
                }
            }
        }

        if (flag && holder.getPhoneNumbers() != null && !holder.getPhoneNumbers().isEmpty()) {
            for (PhoneNumber phone : holder.getPhoneNumbers()) {
                if (phone.isPrimaryPhoneNumber() && (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber()))) {
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
        resp.setDesc("Error validating holder details: " + desc);
        logger.info("Error validating holder details: [{}] - [{}]", desc, login.getUserId());
        return resp;
    }
    
    private Response validateIpoApplication(Login login, IpoApplication ipoApp) {
        logger.info("Validate ipo application for en-mass upload, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            Date date = new Date();
            List<IpoApplication> appList = new ArrayList<>();

            long remainingSharesNotBought;
            long totalSharesBought = 0;
            long continuingShares;
            double amtToBePaid;
            double sharesSubscribedValue;
            if (cq.checkActiveInitialPublicOffer(ipoApp.getInitialPublicOfferId())) {//ipo must be active
                org.greenpole.hibernate.entity.InitialPublicOffer ipo = cq.getActiveClientCompanyIpo(ipoApp.getInitialPublicOfferId());
                if (ipoApp.getSharesSubscribed() >= ipo.getStartingMinSub()) {//application must have subscribed shares greater than min starting subscription
                    continuingShares = ipoApp.getSharesSubscribed() - ipo.getStartingMinSub();
                    if (continuingShares % ipo.getContMinSub() == 0) {//application must have continuing shares in multiples of continuing subscription
                        amtToBePaid = ipoApp.getSharesSubscribed() * ipo.getOfferPrice();
                        if (ipoApp.getAmountPaid() == amtToBePaid) {//application must have amount paid calculated to the amount to be paid
                            List<org.greenpole.hibernate.entity.IpoApplication> ipoList = cq.getActiveIpoApplications(ipoApp.getInitialPublicOfferId());
                            for (org.greenpole.hibernate.entity.IpoApplication ipoapp : ipoList) {
                                totalSharesBought += ipoapp.getSharesSubscribed();//get all shares subscribed from both approved and processing applications
                            }
                            remainingSharesNotBought = ipo.getTotalSharesOnOffer() - totalSharesBought;
                            if (ipoApp.getSharesSubscribed() <= remainingSharesNotBought) {//just a check for information purpose
                                resp.setRetn(0);
                                resp.setDesc("IPO application for " + ipoApp.getHolder().getFirstName() + ipoApp.getHolder().getLastName() + 
                                        " checks out.");
                                logger.info("IPO application for " + ipoApp.getHolder().getFirstName() + ipoApp.getHolder().getLastName() + 
                                        " checks out - [{}]: [{}]", login.getUserId(), resp.getRetn());
                            } else {
                                resp.setRetn(0);
                                resp.setDesc("IPO application's subscription for " + ipoApp.getHolder().getFirstName() + " " + ipoApp.getHolder().getLastName() + 
                                        " is greater than available shares.");
                                logger.info("IPO application's subscription for " + ipoApp.getHolder().getFirstName() + " " + ipoApp.getHolder().getLastName() + 
                                        " is greater than available shares - [{}]: [{}]", login.getUserId(), resp.getRetn());
                            }
                            sharesSubscribedValue = ipoApp.getSharesSubscribed() * ipo.getOfferPrice();
                            ipoApp.setSharesSubscribedValue(sharesSubscribedValue);
                            ipoApp.setDateApplied(formatter.format(date));
                            appList.add(ipoApp);
                            resp.setBody(appList);
                            return resp;
                        }
                        resp.setRetn(200);
                        resp.setDesc("The amount paid for shares and the price of shares to be bought should be the same - for " + ipoApp.getHolder().getFirstName() + " " + ipoApp.getHolder().getLastName());
                        logger.info("The amount paid for shares and the price of shares to be bought should be the same - for [{}] - [{}]: [{}]", ipoApp.getHolder().getFirstName() + " " + ipoApp.getHolder().getLastName(),
                                login.getUserId(), resp.getRetn());
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Continuing shares subscription should be in multiples of "
                            + ipo.getContMinSub() + " - for " + ipoApp.getHolder().getFirstName() + " " + ipoApp.getHolder().getLastName());
                    logger.info("Continuing shares subscription is not in multiples of continuing minimum subscription  - for [{}] - [{}]: [{}]", ipoApp.getHolder().getFirstName() + " " + ipoApp.getHolder().getLastName(),
                            login.getUserId(), resp.getRetn());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Shares subscribed is less than minimum subscription required and so holder cannot apply for IPO - for " + ipoApp.getHolder().getFirstName() + " " + ipoApp.getHolder().getLastName());
                logger.info("Shares subscribed is less than minimum subscription required and so holder cannot apply for IPO - for [{}] - [{}]: [{}]", ipoApp.getHolder().getFirstName() + " " + ipoApp.getHolder().getLastName(),
                        login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Initial Public Offer is past closing date and so holder cannot apply - for " + ipoApp.getHolder().getFirstName() + " " + ipoApp.getHolder().getLastName());
            logger.info("Initial Public Offer is past closing date and so holder cannot apply - for [{}] - [{}]: [{}]", ipoApp.getHolder().getFirstName() + " " + ipoApp.getHolder().getLastName(),
                    login.getUserId(), resp.getRetn());
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
     * Sends a text message object to the text notifier.
     * @param holderPhone the holder's phone number
     * @param notificationCode the notification code
     * @param text the text message
     * @param notificationType the notification type
     * @param holderId the holder id
     * @param function_status the status of the text message function (whether it should be sent or not)
     * @param hasDbInfo holder has info in database or not
     * @param login the user's login details
     */
    private void sendTextMessage(String holderPhone, String notificationCode, String text, String notificationType, int holderId, boolean function_status, boolean hasDbInfo, Login login) {
        if (!"".equals(holderPhone)) {
            NotifierProperties prop = NotifierProperties.getInstance();
            QueueSender qSender = new QueueSender(prop.getNotifierQueueFactory(), prop.getTextNotifierQueueName());
            
            TextSend textSend = new TextSend();
            textSend.setMessage_id(notificationCode);
            textSend.setIsFlash(false);
            textSend.setPhoneNumber(holderPhone);
            textSend.setText(text);
            textSend.setIsBulk(false);
            textSend.setPurpose(notificationType);
            textSend.setHolderId(holderId);
            textSend.setAllowText(function_status);
            textSend.setWithDbInfo(hasDbInfo);
            
            Response textResp = qSender.sendTextMessageRequest(textSend);
            logger.info("response from text notifier - [{}] : [{}]",
                    textResp.getRetn() + " - " + textResp.getDesc(), login.getUserId());
        }
    }
    
    /**
     * Sends a bulk message object to the text notifier.
     * @param text the text message to be sent
     * @param bulk_text the text message and ids
     * @param notificationType the notification type
     * @param function_status the status of the text message function (whether it should be sent or not)
     * @param hasDbInfo holder has info in database or not
     * @param login the user's login details
     */
    private void sendBulkTextMessage(String text, Map<String, String> bulk_text, String notificationType, boolean function_status, boolean hasDbInfo, Login login) {
        if (!bulk_text.isEmpty()) {
            NotifierProperties prop = NotifierProperties.getInstance();
            QueueSender qSender = new QueueSender(prop.getNotifierQueueFactory(), prop.getTextNotifierQueueName());
            
            TextSend textSend = new TextSend();
            textSend.setIsFlash(false);
            textSend.setText(text);
            textSend.setNumbersAndIds(bulk_text);
            textSend.setIsBulk(true);
            textSend.setPurpose(notificationType);
            textSend.setAllowText(function_status);
            textSend.setWithDbInfo(hasDbInfo);
            
            Response textResp = qSender.sendTextMessageRequest(textSend);
            logger.info("response from text notifier - [{}] : [{}]",
                    textResp.getRetn() + " - " + textResp.getDesc(), login.getUserId());
        }
    }
}
