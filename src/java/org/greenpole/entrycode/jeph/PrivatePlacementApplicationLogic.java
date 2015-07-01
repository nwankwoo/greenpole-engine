/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph;

import java.text.MessageFormat;
import org.greenpole.entirycode.jeph.model.ConfirmationDetails;
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
import org.greenpole.util.properties.SMSProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jephthah Sadare
 */
public class PrivatePlacementApplicationLogic {

    private final HolderComponentQuery hcq = new HolderComponentQueryImpl();
    private final HolderComponentQuery hq = ComponentQueryFactory.getHolderComponentQuery();
    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();
    private final NotificationProperties notificationProp = NotificationProperties.getInstance();
    private final GreenpoleProperties greenProp = GreenpoleProperties.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(PrivatePlacementApplicationLogic.class);
    SimpleDateFormat formatter = new SimpleDateFormat();

    /**
     * Processes request to validate Private Placement application before
     * creation
     * @param login the user's login details
     * @param ppApp the Private Placement application object to be validated
     * @return response to the Private Placement validation request
     */
    public Response applyForPrivatePlacement_Confirmation_Request(Login login, PrivatePlacementApplication ppApp) {
        logger.info("Request to create Private Placement application, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Date date = new Date();

        double amtToBePaid = 0;
        long remainingSharesNotBought;
        long totalSharesBought = 0;
        long continuingSharesSubscribed;
        double sharesSubscribedValue = 0;
        List<PrivatePlacementApplication> ppAppList = new ArrayList<>();

        try {
            if (hq.checkHolderAccount(ppApp.getHolderId())) {
                if (cq.checkActivePrivatePlacement(ppApp.getPrivatePlacementId())) {//check if private placement exists
                    PrivatePlacement pp = cq.getPrivatePlacement(ppApp.getPrivatePlacementId());
                    if (hq.checkHolderCompanyAccount(ppApp.getHolderId(), pp.getClientCompany().getId())) {//check if holder has account with company
                        if (!pp.getPlacementClosed()) {//checks if private placement is still opened
                            if (ppApp.getSharesSubscribed() >= pp.getStartingMinSubscrptn()) {
                                continuingSharesSubscribed = ppApp.getSharesSubscribed() - pp.getStartingMinSubscrptn();
                                if (continuingSharesSubscribed % pp.getContinuingMinSubscrptn() == 0) {
                                    amtToBePaid = ppApp.getSharesSubscribed() * pp.getOfferPrice();
                                    if (ppApp.getAmountPaid() == amtToBePaid) {
                                        List<org.greenpole.hibernate.entity.PrivatePlacementApplication> appList = cq.getAllActivePrivatePlacementApplications(ppApp.getPrivatePlacementId());
                                        for (org.greenpole.hibernate.entity.PrivatePlacementApplication app : appList) {
                                            totalSharesBought += app.getSharesSubscribed();
                                        }
                                        remainingSharesNotBought = pp.getTotalSharesOnOffer() - totalSharesBought;
                                        if (ppApp.getSharesSubscribed() <= remainingSharesNotBought) {//just a check for information
                                            resp.setRetn(0);
                                            resp.setDesc("Private Placement Application checks out.");
                                            logger.info("Private Placement Application checks out - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                        } else {
                                            resp.setRetn(0);
                                            resp.setDesc("Private Placement Application's subscription is greater than available shares.");
                                            logger.info("Private Placement Application's subscription is greater than available shares - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                        }
                                        sharesSubscribedValue = ppApp.getSharesSubscribed() * pp.getOfferPrice();
                                        ppApp.setSharesSubscribedValue(sharesSubscribedValue);
                                        ppApp.setDateApplied(formatter.format(date));
                                        ppAppList.add(ppApp);
                                        resp.setBody(ppAppList);
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
                        logger.info("Application for Private placement is past closing date and so holder cannot apply - [{}]: [{}]", login.getUserId(), resp.getRetn());
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Holder has no account with Client company and so cannot apply for Private Placement.");
                    logger.info("Holder has no account with Client company and so cannot apply for Private Placement - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Private placement does not exist.");
                logger.info("Private placement does not exist - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Holder does not exists and so cannot apply for Private Placement.");
            logger.info("Holder does not exists so cannot apply for Private Placement - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process Private Placement Application request. Contact system administrator.");
            logger.info("error processing Private Placement Application request. See error log - [{}]: [{}]", login.getUserId(), resp.getRetn());
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
     * @param ppApp the Private Placement application object to be created
     * @return response to the Private Placement application creation
     * request
     */
    public Response applyForPrivatePlacement_Request(Login login, String authenticator, PrivatePlacementApplication ppApp) {
        logger.info("request to confirm private placement application [{}], invoked by", login.getUserId());
        Response resp = new Response();
        
        try {
            Notification notification = new Notification();
            List<PrivatePlacementApplication> ppAppList = new ArrayList<>();
            
            NotificationWrapper wrapper;
            QueueSender queue;
            NotifierProperties prop;

            if (hq.checkHolderAccount(ppApp.getHolderId())) {// check if holder has company account with a particular client company
                wrapper = new NotificationWrapper();
                prop = NotifierProperties.getInstance();
                queue = new QueueSender(prop.getNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
                
                //set necessary variables
                ppApp.setCanceled(false);
                ppApp.setApproved(false);
                ppApp.setProcessingPayment(true);
                
                Holder h = hq.getHolder(ppApp.getHolderId());
                ppAppList.add(ppApp);

                wrapper.setCode(notification.createCode(login));
                wrapper.setDescription("Authorise confirmation of Private Placement Application for " + 
                        h.getFirstName() + " " + h.getLastName());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setNotificationType(NotificationType.apply_for_private_placement.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(ppAppList);
                
                logger.info("Notification forwarded to queue - notification code: [{}]  [{}]", wrapper.getCode(), login.getUserId());
                resp = queue.sendAuthorisationRequest(wrapper);
                
                if (resp.getRetn() == 0) {
                    SMSProperties smsProp = SMSProperties.getInstance();
                    int holderId = ppApp.getHolderId();
                    String holderPhone = hq.getHolderPryPhoneNumber(holderId);
                    ClientCompany cc = cq.getPrivatePlacementClientCompany(ppApp.getPrivatePlacementId());
                    
                    String text = MessageFormat.format(smsProp.getTextPlacementProcessing(), cc.getName());
                    String notificationType = NotificationType.apply_for_private_placement.toString();
                    boolean function_status = Boolean.valueOf(smsProp.getTextPlacementSend());
                    
                    sendTextMessage(holderPhone, wrapper.getCode(), text, notificationType, holderId, function_status, true, login);
                }
                
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Holder does not exists and so cannot apply for Private Placement.");
            logger.info("Holder does not exists so cannot apply for Private Placement - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process Initial Public Offer Application request. Contact system administrator.");
            logger.info("error processing Initial Public Offer Application request. See error log - [{}]: [{}]", login.getUserId(), resp.getRetn());
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
    public Response applyForPrivatePlacement_Authorise(Login login, String notificationCode) {
        logger.info("Authorisation request to private placement application, invoked by [{}]", login.getUserId());
        Notification notification = new Notification();
        Response resp = new Response();
        long continuingSharesSubscribed = 0;
        double amtToBePaid = 0;
        
        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<PrivatePlacementApplication> ppApplicationList = (List<PrivatePlacementApplication>) wrapper.getModel();
            PrivatePlacementApplication ppApp = ppApplicationList.get(0);
            
            if (hq.checkHolderAccount(ppApp.getHolderId())) {
                if (cq.checkPrivatePlacement(ppApp.getPrivatePlacementId())) {//check if private placement exists
                    PrivatePlacement pp = cq.getPrivatePlacement(ppApp.getPrivatePlacementId());
                    if (hq.checkHolderCompanyAccount(ppApp.getHolderId(), pp.getClientCompany().getId())) {//check if holder has account with company
                        if (!pp.getPlacementClosed()) {//checks if private placement is still opened
                            if (ppApp.getSharesSubscribed() >= pp.getStartingMinSubscrptn()) {
                                continuingSharesSubscribed = ppApp.getSharesSubscribed() - pp.getStartingMinSubscrptn();
                                if (continuingSharesSubscribed % pp.getContinuingMinSubscrptn() == 0) {
                                    amtToBePaid = ppApp.getSharesSubscribed() * pp.getOfferPrice();
                                    if (ppApp.getAmountPaid() == amtToBePaid) {
                                        org.greenpole.hibernate.entity.PrivatePlacementApplication ppApplicationEntity = new org.greenpole.hibernate.entity.PrivatePlacementApplication();
                                        
                                        ClearingHouse clHouse = cq.getClearingHouse(ppApp.getClearingHouseId());
                                        Holder holder = hq.getHolder(ppApp.getHolderId());
                                        PrivatePlacement ppEntity = cq.getPrivatePlacement(ppApp.getPrivatePlacementId());

                                        ppApplicationEntity.setClearingHouse(clHouse);
                                        ppApplicationEntity.setHolder(holder);
                                        ppApplicationEntity.setPrivatePlacement(ppEntity);
                                        ppApplicationEntity.setIssuer(ppApp.getIssuer());
                                        ppApplicationEntity.setSharesSubscribed(ppApp.getSharesSubscribed());
                                        ppApplicationEntity.setAmountPaid(ppApp.getAmountPaid());
                                        ppApplicationEntity.setIssuingHouse(ppApp.getIssuingHouse());
                                        ppApplicationEntity.setSharesSubscribedValue(ppApp.getSharesSubscribedValue());
                                        ppApplicationEntity.setReturnMoney(ppApp.getReturnMoney());
                                        ppApplicationEntity.setProcessingPayment(true);
                                        ppApplicationEntity.setApproved(true);
                                        ppApplicationEntity.setCanceled(false);
                                        ppApplicationEntity.setDateApplied(formatter.parse(ppApp.getDateApplied()));
                                        
                                        boolean applied = hq.applyForPrivatePlacement(ppApplicationEntity);
                                        
                                        if (applied) {
                                            resp.setRetn(0);
                                            resp.setDesc("Application for Private Placement Successful.");
                                            logger.info("Application for Private Placement Successful - [{}]", login.getUserId());
                                            
                                            notification.markAttended(notificationCode);
                                            SMSProperties smsProp = SMSProperties.getInstance();
                                            int holderId = ppApp.getHolderId();
                                            String holderPhone = hq.getHolderPryPhoneNumber(holderId);
                                            ClientCompany cc = cq.getPrivatePlacementClientCompany(ppApp.getPrivatePlacementId());

                                            String text = MessageFormat.format(smsProp.getTextPlacementPaymentSuccess(), cc.getName());
                                            String notificationType = NotificationType.apply_for_private_placement.toString();
                                            boolean function_status = Boolean.valueOf(smsProp.getTextPlacementSend());
                                            
                                            sendTextMessage(holderPhone, wrapper.getCode(), text, notificationType, holderId, function_status, true, login);
                                            
                                            return resp;
                                        }
                                        resp.setRetn(200);
                                        resp.setDesc("Application for Private Placement failed. Please contact Administrator.");
                                        logger.info("Application for Private Placement failed. Please contact Administrator - [{}]: [{}]", resp.getRetn(), login.getUserId());
                                        return resp;
                                    }
                                    resp.setRetn(200);
                                    resp.setDesc("The amount paid for shares and the price of shares to be bought should be the same.");
                                    logger.info("The amount paid for shares and the price of shares to be bought should be the same. - [{}]: [{}]", resp.getRetn(), login.getUserId());
                                    return resp;
                                }
                                resp.setRetn(200);
                                resp.setDesc("Continuing shares should be in multiples of " + pp.getContinuingMinSubscrptn());
                                logger.info("Continuing shares is not in multiples of continuing minimum subscription - [{}]: [{}]", resp.getRetn(), login.getUserId());
                                return resp;
                            }
                            resp.setRetn(200);
                            resp.setDesc("Shares subscribed is less than minimum subscription required and so holder cannot apply for Private Placement.");
                            logger.info("Shares subscribed is less than minimum subscription required and so holder cannot apply for Private Placement. - [{}]: [{}]", resp.getRetn(), login.getUserId());
                            return resp;
                        }
                        resp.setRetn(200);
                        resp.setDesc("Application for Private placement is past closing date and so holder cannot apply");
                        logger.info("Application for Private placement is past closing date and so holder cannot apply [{}] - [{}]", resp.getRetn(), login.getUserId());
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Holder has no account with Client company and so cannot apply for Private Placement.");
                    logger.info("Holder has no account with Client company and so cannot apply for Private Placement - [{}]: [{}]", resp.getRetn(), login.getUserId());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Private placement does not exist.");
                logger.info("Private placement does not exist - [{}]: [{}]", resp.getRetn(), login.getUserId());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Holder does not exists and so cannot apply for Private Placement.");
            logger.info("Holder does not exists so cannot apply for Private Placement - [{}]: [{}]", resp.getRetn(), login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            resp.setRetn(98);
            resp.setDesc("Unable to process Private Placement application. Contact System Administrator");
            logger.info("Error loading notification xml file. See error log [{}] - [{}]", resp.getRetn(), login.getUserId());
            logger.error("Error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process Private Placement Application request. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            logger.info("Error processing Private Placement Application request. See error log [{}] - [{}]", resp.getRetn(), login.getUserId());
            logger.error("Error processing Private Placement Application request. - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to cancel a shareholder's Private Placement application
     * @param login the user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param ppApp the Private Placement application object to be cancelled
     * @return response to the Private Placement application cancel request
     */
    public Response cancelPrivatePlacementApplication_Request(Login login, String authenticator, PrivatePlacementApplication ppApp) {
        logger.info("Request to cancel Private Placement application, invoked by [{}]", login.getUserId());
        Notification notification = new Notification();
        Response resp = new Response();

        try {
            NotificationWrapper wrapper;
            QueueSender queue;
            NotifierProperties prop;
            org.greenpole.hibernate.entity.PrivatePlacementApplication ppApplicationEntity = hq.getPrivatePlacementApplication(ppApp.getId());
            if (!ppApplicationEntity.getApproved()) {
                wrapper = new NotificationWrapper();
                prop = NotifierProperties.getInstance();
                queue = new QueueSender(prop.getNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
                
                List<PrivatePlacementApplication> ppAppList = new ArrayList<>();
                ppAppList.add(ppApp);
                
                Holder h = hq.getHolder(ppApp.getHolderId());
                
                wrapper.setCode(notification.createCode(login));
                wrapper.setDescription("Authorise cancellation of Private Placement Application for " + 
                        h.getFirstName() + " " + h.getLastName());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
                wrapper.setNotificationType(NotificationType.cancel_private_placement.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(ppAppList);
                logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                resp = queue.sendAuthorisationRequest(wrapper);
                if (resp.getRetn() == 0) {
                    SMSProperties smsProp = SMSProperties.getInstance();
                    int holderId = ppApp.getHolderId();
                    String holderPhone = hq.getHolderPryPhoneNumber(holderId);
                    ClientCompany cc = cq.getPrivatePlacementClientCompany(ppApp.getPrivatePlacementId());
                    
                    String text = MessageFormat.format(smsProp.getTextPlacementCancelProcessing(), cc.getName());
                    String notificationType = NotificationType.cancel_private_placement.toString();
                    boolean function_status = Boolean.valueOf(smsProp.getTextPlacementSend());

                    sendTextMessage(holderPhone, wrapper.getCode(), text, notificationType, holderId, function_status, true, login);
                }
                
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Payment for Private Placement Application is being processed or has been approved and so cannot be cancelled");
            logger.info("Payment for Private Placement Application is being processed or has been approved and so cannot be cancelled. - [{}]: [{}]", login.getUserId(), resp.getRetn());
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
            PrivatePlacementApplication ppApp = ppApplicationList.get(0);
            
            org.greenpole.hibernate.entity.PrivatePlacementApplication ppApplicationEntity = hq.getPrivatePlacementApplication(ppApp.getId());
            if (!ppApplicationEntity.getApproved()) {
                ppApplicationEntity.setCanceled(true);
                ppApplicationEntity.setProcessingPayment(false);
                
                boolean updated = hq.applyForPrivatePlacement(ppApplicationEntity);
                
                if (updated) {
                    resp.setRetn(0);
                    resp.setDesc("Successful");
                    logger.info("Authorisation of Private Placement Application cancellation successful - [{}]", login.getUserId());
                    
                    notification.markAttended(notificationCode);
                    SMSProperties smsProp = SMSProperties.getInstance();
                    int holderId = ppApp.getHolderId();
                    String holderPhone = hq.getHolderPryPhoneNumber(holderId);
                    ClientCompany cc = cq.getPrivatePlacementClientCompany(ppApp.getPrivatePlacementId());
                    
                    String text = MessageFormat.format(smsProp.getTextPlacementCancelConfirm(), cc.getName());
                    String notificationType = NotificationType.cancel_private_placement.toString();
                    boolean function_status = Boolean.valueOf(smsProp.getTextPlacementSend());
                    
                    sendTextMessage(holderPhone, wrapper.getCode(), text, notificationType, holderId, function_status, true, login);
                    
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Private Placement Application cancellation unsuccessful. Please contract System Administrator.");
                logger.info("Private Placement Application cancellation unsuccessful. Please contract System Administrator - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Payment for Private Placement Application is being processed or has been approved and so cannot be cancelled");
            logger.info("Payment for Private Placement Application is being processed or has been approved and so cannot be cancelled. - [{}]: [{}]", login.getUserId(), resp.getRetn());
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
     * @param ppAppList the list of Private Placement applications
     * @return response to the Private Placement Application list
     */
    public Response uploadPrivatePlacementApplicationEnmass_Request(Login login, String authenticator,
            List<PrivatePlacementApplication> ppAppList) {
        logger.info("Request to upload list of Private Placement application, invoked by [{}]", login.getUserId());
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
            
            for (PrivatePlacementApplication ppApplicant : ppAppList) {
                Response rulesCheck = validatePrivatePlacementApplication(login, ppApplicant);
                if (rulesCheck.getRetn() != 0) {
                    return rulesCheck;//return and exit
                }
            }

            for (PrivatePlacementApplication ppApplicant : ppAppList) {
                Response detailsCheck = validateHolderDetails(login, ppApplicant.getHolder());
                Response recordCheck = validateHolderRecord(login, ppApplicant.getHolder());
                
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
            
            ClientCompany cc_info = cq.getIpoClientCompany(ppAppList.get(0).getPrivatePlacementId());
            
            wrapper.setCode(notification.createCode(login));
            wrapper.setDescription("Authorise multiple application for the private placement issued by - " + cc_info.getName());
            wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
            wrapper.setFrom(login.getUserId());
            wrapper.setTo(authenticator);
            wrapper.setModel(ppAppList);
            
            logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
            resp = queue.sendAuthorisationRequest(wrapper);
            
            confirm.setTitle("List of Private Placement Applications Status");
            confirm.setDetails(positiveResponses);
            confirmationList.add(confirm);
            
            resp.setBody(confirmationList);
            resp.setRetn(0);
            // send SMS and/or Email notification
            if (resp.getRetn() == 0) {
                SMSProperties smsProp = SMSProperties.getInstance();
                org.greenpole.util.Date txtdate = new org.greenpole.util.Date();
                Map<String, String> phoneMessageIds = new HashMap<>();
                for (PrivatePlacementApplication ppApp : ppAppList) {
                    String holderPhone = "";
                    boolean foundNumber = false;
                    if (ppApp.getHolder().getPhoneNumbers() != null && !ppApp.getHolder().getPhoneNumbers().isEmpty()) {
                        for (PhoneNumber phone : ppApp.getHolder().getPhoneNumbers()) {
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
                        String msgId = ppApp.getHolder().getFirstName().substring(0, 3) + "_" +
                                ppApp.getHolder().getLastName().substring(0, 3) + "_" + txtdate.getDateTime();
                        phoneMessageIds.put(holderPhone, msgId);
                    }
                }
                
                if (!phoneMessageIds.isEmpty()) {
                    ClientCompany cc = cq.getIpoClientCompany(ppAppList.get(0).getPrivatePlacementId());
                    
                    String text = MessageFormat.format(smsProp.getTextPlacementProcessing(), cc.getName());
                    String notificationType = NotificationType.apply_for_ipo_enmass.toString();
                    boolean function_status = Boolean.valueOf(smsProp.getTextPlacementSend());
                    
                    sendBulkTextMessage(text, phoneMessageIds, notificationType, function_status, false, login);
                }
            }
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
    public Response uploadPrivatePlacementApplicationEnmass_Authorise(Login login, String notificationCode) {
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
                validPpApply = validatePrivatePlacementApplication(login, ppApplicant);
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
    

    /**
     * Validates Private Placement Application
     * @param login the user's login details
     * @param ppApp Private Placement Application object
     * @return response object to the caller method
     */
    private Response validatePrivatePlacementApplication(Login login, PrivatePlacementApplication ppApp) {
        logger.info("Validate private placement application for en-mass upload, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Date date = new Date();

        double amtToBePaid = 0;
        long remainingSharesNotBought = 0;
        long totalSharesBought = 0;
        long continuingSharesSubscribed = 0;
        double sharesSubscribedValue = 0;
        List<PrivatePlacementApplication> ppAppList = new ArrayList<>();

        try {
            String holdername = ppApp.getHolder().getFirstName() + " " + ppApp.getHolder().getLastName();
            if (hq.checkHolderAccount(ppApp.getHolderId())) {
                if (cq.checkActivePrivatePlacement(ppApp.getPrivatePlacementId())) {//check if private placement exists
                    PrivatePlacement pp = cq.getPrivatePlacement(ppApp.getPrivatePlacementId());
                    if (hq.checkHolderCompanyAccount(ppApp.getHolderId(), pp.getClientCompany().getId())) {//check if holder has account with company
                        if (!pp.getPlacementClosed()) {//checks if private placement is still opened
                            if (ppApp.getSharesSubscribed() >= pp.getStartingMinSubscrptn()) {
                                continuingSharesSubscribed = ppApp.getSharesSubscribed() - pp.getStartingMinSubscrptn();
                                if (continuingSharesSubscribed % pp.getContinuingMinSubscrptn() == 0) {
                                    amtToBePaid = ppApp.getSharesSubscribed() * pp.getOfferPrice();
                                    if (ppApp.getAmountPaid() == amtToBePaid) {
                                        List<org.greenpole.hibernate.entity.PrivatePlacementApplication> appList = cq.getAllActivePrivatePlacementApplications(ppApp.getPrivatePlacementId());
                                        for (org.greenpole.hibernate.entity.PrivatePlacementApplication app : appList) {
                                            totalSharesBought += app.getSharesSubscribed();
                                        }
                                        remainingSharesNotBought = pp.getTotalSharesOnOffer() - totalSharesBought;
                                        if (ppApp.getSharesSubscribed() <= remainingSharesNotBought) {//just a check for information
                                            resp.setRetn(0);
                                            resp.setDesc("Private Placement Application for " + holdername + " checks out.");
                                            logger.info("Private Placement Application for [{}] checks out - [{}]: [{}]", holdername, login.getUserId(), resp.getRetn());
                                        } else {
                                            resp.setRetn(0);
                                            resp.setDesc("Private Placement Application's subscription for " + holdername + " is greater than available shares.");
                                            logger.info("Private Placement Application's subscription for [{}] is greater than available shares - [{}]: [{}]", holdername, login.getUserId(), resp.getRetn());
                                        }
                                        sharesSubscribedValue = ppApp.getSharesSubscribed() * pp.getOfferPrice();
                                        ppApp.setSharesSubscribedValue(sharesSubscribedValue);
                                        ppApp.setDateApplied(formatter.format(date));
                                        ppAppList.add(ppApp);
                                        resp.setBody(ppAppList);
                                        return resp;
                                    }
                                    resp.setRetn(200);
                                    resp.setDesc("The amount paid for shares and the price of shares to be bought should be the same - for " + holdername);
                                    logger.info("The amount paid for shares and the price of shares to be bought should be the same - for [{}] - [{}]: [{}]", holdername, login.getUserId(), resp.getRetn());
                                    return resp;
                                }
                                resp.setRetn(200);
                                resp.setDesc("Continuing shares should be in multiples of " + pp.getContinuingMinSubscrptn() + " - for " + holdername);
                                logger.info("Continuing shares is not in multiples of continuing minimum subscription - for [{}] - [{}]: [{}]", holdername, login.getUserId(), resp.getRetn());
                                return resp;
                            }
                            resp.setRetn(200);
                            resp.setDesc("Shares subscribed is less than minimum subscription required and so holder cannot apply for Private Placement - for " + holdername);
                            logger.info("Shares subscribed is less than minimum subscription required and so holder cannot apply for Private Placement - for [{}] - [{}]: [{}]", holdername, login.getUserId(), resp.getRetn());
                            return resp;
                        }
                        resp.setRetn(200);
                        resp.setDesc("Application for Private placement is past closing date and so holder cannot apply - for " + holdername);
                        logger.info("Application for Private placement is past closing date and so holder cannot apply - for [{}] - [{}]: [{}]", holdername, login.getUserId(), resp.getRetn());
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Holder has no account with Client company and so cannot apply for Private Placement - for " + holdername);
                    logger.info("Holder has no account with Client company and so cannot apply for Private Placement - for [{}] - [{}]: [{}]", holdername, login.getUserId(), resp.getRetn());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Private placement does not exist or is past its closing date - for " + holdername);
                logger.info("Private placement does not exist or is past its closing date - for[{}] - [{}]: [{}]", holdername, login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Holder does not exists and so cannot apply for Private Placement - for " + holdername);
            logger.info("Holder does not exists so cannot apply for Private Placement - for[{}] - [{}]: [{}]", holdername, login.getUserId(), resp.getRetn());
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
     * Sends a text message object to the text notifier.
     * @param holderPhone the holder's phone number
     * @param notificationCode the notification code
     * @param text the text message
     * @param notificationType the notification type
     * @param holderId the holder id
     * @param function_status the status of the text message function (whether it should be sent or not)
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
