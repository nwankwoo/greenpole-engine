/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph.bondmodule;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.hibernate.entity.ClientCompany;
import org.greenpole.entity.model.clientcompany.PrivatePlacement;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.logic.ClientCompanyComponentLogic;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.Notification;
import org.greenpole.util.properties.NotifierProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jephthah Sadare
 * @version 1.0 Business requirement to process private placement request
 */
public class PrivatePlacementComponent {

    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();
    private static final Logger logger = LoggerFactory.getLogger(ClientCompanyComponentLogic.class);

    /**
     * Processes request to create a Private Placement.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the
     * notification
     * @param privatePlacement the private placement details to be processed
     * @return response object back to sender indicating creation request status
     */
    public Response createPrivatePlacement_Request(Login login, String authenticator, PrivatePlacement privatePlacement) {
        logger.info("request to create private placement, invoked by [{}]", login.getUserId());

        Response resp = new Response();
        NotificationWrapper wrapper;
        QueueSender queue;
        NotifierProperties props;
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy");

        try {
            boolean exits = cq.checkClientCompany(privatePlacement.getClientCompanyId());
            if (exits) {
                ClientCompany cc = cq.getClientCompany(privatePlacement.getClientCompanyId());
                if (cc.isValid()) {
                    if (date.before(formatter.parse(privatePlacement.getClosingDate()))) {
                        if (cq.checkClientCompanyForShareholders(cc.getName())) {
                            if (cq.checkOpenPrivatePlacement(cc.getId())) {
                                wrapper = new NotificationWrapper();
                                props = new NotifierProperties(PrivatePlacementComponent.class);
                                queue = new QueueSender(props.getAuthoriserNotifierQueueFactory(), props.getAuthoriserNotifierQueueName());
                                List<PrivatePlacement> ppc = new ArrayList<>();
                                ppc.add(privatePlacement);
                                wrapper.setCode(Notification.createCode(login));
                                wrapper.setDescription("Create Private Placement for " + cc.getName());
                                wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
                                wrapper.setFrom(login.getUserId());
                                wrapper.setTo(authenticator);
                                wrapper.setModel(ppc);
                                resp = queue.sendAuthorisationRequest(wrapper);
                                logger.info("notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                                return resp;
                            } else {
                                resp.setRetn(205);
                                resp.setDesc("A private placement is currently opened");
                                logger.info("A private placement is currently opened [{}] - [{}]", resp.getRetn(), login.getUserId());
                                return resp;
                            }
                        } else {
                            resp.setRetn(204);
                            resp.setDesc("No shareholders in client company for private placement");
                            logger.info("No shareholders in client company for priate placement [{}] - [{}]", resp.getRetn(), login.getUserId());
                            return resp;
                        }
                    } else {
                        resp.setRetn(203);
                        resp.setDesc("Private placement cannot be closed before current date");
                        logger.info("Private placement cannot be closed before current date [{}] - [{}]", resp.getRetn(), login.getUserId());
                        return resp;
                    }
                } else {
                    resp.setRetn(202);
                    resp.setDesc("Client company for private placement is not valid");
                    logger.info("Client company for priate placement is not valid [{}] - [{}]", resp.getRetn(), login.getUserId());
                    return resp;
                }
            } else {
                resp.setRetn(201);
                resp.setDesc("Client company for private placement does not exist");
                logger.info("Client company for priate placement does not exist [{}] - [{}]", resp.getRetn(), login.getUserId());
                return resp;
            }
        } catch (Exception ex) {            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process private placement creation. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            logger.info("error processing private placement creation. See error log [{}] - [{}]", resp.getRetn(), login.getUserId());
            logger.error("error processing private placement creation - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to persist a privatePlacement object that has already
     * been saved as a notification file, according to the specified
     * notification code
     * @param notificationCode the notification code
     * @return response to the create private placement request
     */
    public Response setupPrivatePlacement_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        logger.info("Private Placement creation authorised - notification code: [{}], invoked by [{}]", notificationCode, login.getUserId());
        try { 
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<PrivatePlacement> pplist = (List<PrivatePlacement>) wrapper.getModel();
            PrivatePlacement ppModel = pplist.get(0);
            org.greenpole.hibernate.entity.PrivatePlacement ppEntity = new org.greenpole.hibernate.entity.PrivatePlacement();
            boolean clientCompExist = cq.checkClientCompany(ppModel.getClientCompanyId());
            if (clientCompExist) {
                org.greenpole.hibernate.entity.ClientCompany clComp = cq.getClientCompany(ppModel.getClientCompanyId());
                ppEntity.setClientCompany(clComp);
                ppEntity.setTotalSharesOnOffer(ppModel.getTotalSharesOnOffer());
                ppEntity.setMethodOnOffer(Integer.parseInt(ppModel.getMethodOfOffer()));
                ppEntity.setStartingMinSubscrptn(ppModel.getStartingMinimumSubscription());
                ppEntity.setContinuingMinSubscrptn(ppModel.getContinuingMinimumSubscription());
                ppEntity.setOfferPrice(ppModel.getOfferPrice());
                ppEntity.setOfferSize(ppModel.getOfferSize().doubleValue());
                ppEntity.setOpeningDate(formatter.parse(ppModel.getOpeningDate()));
                ppEntity.setClosingDate(formatter.parse(ppModel.getClosingDate()));
                ppEntity.setPlacementClosed(false);
                cq.createPrivatePlacement(ppEntity);
                resp.setRetn(0);
                resp.setDesc("Successful");
                logger.info("Private Placement create for Client Company ID: [{}] - [{}]", ppModel.getClientCompanyId(), login.getUserId());
            } else {
                resp.setRetn(202);
                resp.setDesc("Error: Client company for private placement does not exist.");
                logger.info("Error: Client company for private placement does not exist [{}] - [{}]", resp.getRetn(), login.getUserId());
            }
            return resp;
        } catch (JAXBException ex) {
            resp.setRetn(100);
            resp.setDesc("General Error: Unable to load notification xml file. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            logger.info("Error loading notification xml file. See error log [{}] - [{}]", resp.getRetn(), login.getUserId());
            logger.error("Error loading notification xml file to object - [" + login.getUserId() + "]", ex);
        } catch (ParseException ex) {
            resp.setRetn(100);
            resp.setDesc("General Error: Unable to convert from string to date type. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            logger.info("Error converting from string to date type [{}] - [{}]", resp.getRetn(), login.getUserId());
            logger.error("Error converting from string to date type - [" + login.getUserId() + "]", ex);
        } catch (Exception ex) {            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to create private placement. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            logger.info("error creating private placement. See error log [{}] - [{}]", resp.getRetn(), login.getUserId());
            logger.error("error creating private placement - [{}]", login.getUserId(), ex);
            return resp;
        }
        return resp;
    }
}
