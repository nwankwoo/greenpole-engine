/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph.bondmodule;

import java.util.*;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.entrycode.jeph.models.PrivatePlacement;
import org.greenpole.hibernate.entity.ClientCompany;
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
        logger.info("user request to create private placement");

        Response res = new Response();
        NotificationWrapper wrapper;
        QueueSender queue;
        NotifierProperties props;
        Date date = new Date();

        ClientCompany clientCompany = cq.getClientCompany(privatePlacement.getClientCompanyId());

        try {
            // checks if the client company exits and the current system date is earlier than closing date
            // TODO: more validateion - if company has a number of shareholders 
            // as specified in specification document.
            if (clientCompany.getId() != 0 && date.before(privatePlacement.getClosingDate())) {
                wrapper = new NotificationWrapper();
                props = new NotifierProperties(PrivatePlacement.class);
                queue = new QueueSender(props.getAuthoriserNotifierQueueFactory(), props.getAuthoriserNotifierQueueName());

                List<PrivatePlacementComponent> ppc = new ArrayList<>();
                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription("Create Private Placement for " + clientCompany.getName());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(ppc);

                res = queue.sendAuthorisationRequest(wrapper);
                logger.info("notification forwarded to queue - notification code: [{}]", wrapper.getCode());

                return res;
            }

            logger.info("notification forwarded to queue - notitication code: [{}]");
            return res;
        } catch (Exception ex) {
            // TODO: change from Exception class to specific user-defined exceptions later
            res.setRetn(200);
            res.setDesc("Private placement already exists or has empty parameters and so cannot be created.");
            logger.info("Private Placement exists or has empty parameters and so cannot be created - [{}]: [{}]", clientCompany.getName(), res.getRetn());
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
        logger.info("Private Placement creation authorised - [{}]", notificationCode);
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<PrivatePlacement> pplist = (List<PrivatePlacement>) wrapper.getModel();
            PrivatePlacement ppModel = pplist.get(0);
            org.greenpole.hibernate.entity.PrivatePlacement ppEntity = new org.greenpole.hibernate.entity.PrivatePlacement();
            ppEntity.setId(ppModel.getClientCompanyId());
            ppEntity.setTotalSharesOnOffer(ppModel.getTotalSharesOnOffer());
            ppEntity.setMethodOnOffer(ppModel.getMethodOnOffer());
            ppEntity.setStartingMinSubscrptn(ppModel.getStartingMinSubscrptn());
            ppEntity.setContinuingMinSubscrptn(ppModel.getContinuingMinSubscrptn());
            ppEntity.setOfferPrice(ppModel.getOfferPrice());
            ppEntity.setOfferSize(ppModel.getOfferSize());
            ppEntity.setOpeningDate(ppModel.getOpeningDate());
            ppEntity.setClosingDate(ppModel.getClosingDate());
            cq.createPrivatePlacement(ppEntity);

            logger.info("Private Placement create for Client Company ID: [{}]", ppModel.getClientCompanyId());
            res.setRetn(0);
            res.setDesc("Successful Persistence");

            return res;
        } catch (JAXBException ex) {
            // TODO: change to more appropriate exception
            res.setRetn(100);
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);
        }
        return res;
    }

}
