/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph.bondmodule;

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
     * Process request to create a Private Placement
     *
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the
     * notification
     * @param privatePlacement the private placement details to be processed
     * @return response object back to sender indicating creation request status
     */
    public Response createPrivatePlacement_Request(Login login, String authenticator, PrivatePlacement privatePlacement) {
        logger.info("user request to create private placement. Invoked by [{}]", login.getUserId());

        Response res = new Response();
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
                            if (true) {
                                wrapper = new NotificationWrapper();
                                props = new NotifierProperties(PrivatePlacementComponent.class);
                                queue = new QueueSender(props.getAuthoriserNotifierQueueFactory(), props.getAuthoriserNotifierQueueName());
                                List<PrivatePlacement> ppc = new ArrayList<>();
                                wrapper.setCode(Notification.createCode(login));
                                wrapper.setDescription("Create Private Placement for " + cc.getName());
                                wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
                                wrapper.setFrom(login.getUserId());
                                wrapper.setTo(authenticator);
                                wrapper.setModel(ppc);
                                res = queue.sendAuthorisationRequest(wrapper);
                                logger.info("notification forwarded to queue - notification code: [{}]", wrapper.getCode());
                                return res;
                            } else {
                                res.setRetn(205);
                                res.setDesc("A private placement is currently opened");
                                logger.info("A private placement is currently opened");
                                return res;
                            }
                        } else {
                            res.setRetn(204);
                            res.setDesc("No shareholders in client company for private placement");
                            logger.info("No shareholders in client company for priate placement");
                            return res;
                        }
                    } else {
                        res.setRetn(203);
                        res.setDesc("Private placement can not be closed before current date");
                        logger.info("Private placement can not be closed before current date");
                        return res;
                    }
                } else {
                    res.setRetn(202);
                    res.setDesc("No valid client company for private placement");
                    logger.info("No valid client company for priate placement");
                    return res;
                }
            } else {
                res.setRetn(201);
                res.setDesc("No client company for private placement does not exist");
                logger.info("No client company for priate placement does not exist");
                return res;
            }
        } catch (Exception ex) {
            res.setRetn(200);
            res.setDesc("Error in verifing Client Company for private placement ");
            logger.info("Error in verifing Client Company for private placement : [{}]", res.getRetn());
        }
        return res;
    }

    /**
     * Processes request to persist a privatePlacement object that has already
     * been saved as a notification file, according to the specified
     * notification code
     *
     * @param notificationCode the notification code
     * @return back to sender indicating authorization request status
     */
    public Response setupPrivatePlacement_Authorise(String notificationCode) {
        Response res = new Response();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        logger.info("Private Placement creation authorised - [{}]", notificationCode);
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
                cq.createPrivatePlacement(ppEntity);
                logger.info("Private Placement create for Client Company ID: [{}]", ppModel.getClientCompanyId());
                res.setRetn(0);
                res.setDesc("Successful Persistence");
            } else {
                logger.info("Error: Client company for private placement does not exist");
                res.setRetn(202);
                res.setDesc("Error: Client company for private placement does not exist.");
            }
            return res;
        } catch (JAXBException ex) {
            // TODO: catch other types of exception
            res.setRetn(100);
            res.setDesc("error loading notification xml file. See error log");
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);
        } catch (java.text.ParseException pe) {
            res.setRetn(100);
            res.setDesc("Error converting from string to date type");
            logger.info("Error converting from string to date type");
            logger.error("Error converting from string to date type - ", pe);
        }
        return res;
    }
}
