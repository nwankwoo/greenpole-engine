/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph.bondmodule;

import org.greenpole.entrycode.jeph.models.Bond;
import java.util.*;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.hibernate.entity.BondOffer;
import org.greenpole.hibernate.query.*;
import org.greenpole.hibernate.query.factory.*;
import org.greenpole.logic.ClientCompanyComponentLogic;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.Notification;
import org.greenpole.util.properties.NotifierProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jephthah Sadare
 */
public class BondCompomentImpl implements BondComponent {

    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();
    private static final Logger logger = LoggerFactory.getLogger(ClientCompanyComponentLogic.class);

    /**
     * creates a bond offer
     * @param login
     * @param authenticator
     * @param bond
     * @return response object
     */
    @Override
    public Response createBondOffer(Login login, String authenticator, Bond bond) {
        logger.info("user [{}] requests to create a bond offer [{}] at [{}] unit price", login.getUserId(), bond.getTitle(), bond.getBondUnitPrice());

        Response res = new Response();
        NotificationWrapper wrapper;
        QueueSender queue;
        NotifierProperties prop;

        // checks if the bond was entered correctly
        try {
            if (bond.getTitle() != null && !(bond.getTitle().equals(""))) {
                System.out.println(bond.getTitle());
                wrapper = new NotificationWrapper();
                prop = new NotifierProperties(BondComponent.class);
                queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());

                logger.info("bond [{}]  does not exist", bond.getTitle());
                List<BondComponent> bc = new ArrayList<>();

                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription("Creates Bond " + bond.getTitle());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(bc);

                res = queue.sendAuthorisationRequest(wrapper);
                logger.info("notification fowarded to queue - notification code: [{}]", wrapper.getCode());
                return res;
            }
        } catch (NullPointerException npx) {
            res.setRetn(200);
            res.setDesc("Bond offer already exists and so cannot be created.");
            logger.info("Bond offer exists so cannot be created - [{}]: [{}]", bond.getTitle(), res.getRetn());            
        }
        return res;
    }

    /**
     * Processes request to createBondOffer to persist a bond that already been
     * saved as a notification file, according to the specified notification code.
     * @param notificationCode
     * @return response object
     */
    @Override
    public Response setupBondOfferAuthorise(String notificationCode) {
        Response res = new Response();
        logger.info("bond setup creation authorised - [{}]", notificationCode);
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<Bond> list = (List<Bond>) wrapper.getModel();
            Bond bondModel = list.get(0);
            BondOffer bondOffer = bondCreationMain(bondModel);
            cq.createBondOffer(bondOffer);
            logger.info("bond offer created - [{}]", bondModel.getTitle());
            res.setRetn(0);
            res.setDesc("Successful persistence");
            
            return res;
        } catch (JAXBException ex) {
            res.setRetn(100);
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);

            return res;
        }
    }

    /**
     * initializes BondOffer entity with Bond model attributes
     * @param bondModel
     * @return BondOffer object
     */
    private BondOffer bondCreationMain(Bond bondModel) {
        // instantiate required hibernate entities
        org.greenpole.hibernate.entity.BondOffer bond_main = new org.greenpole.hibernate.entity.BondOffer();
        // TODO incorporate client company infor into the database
        // ClientCompany clientCompany = new ClientCompany();
        bond_main.setTitle(bondModel.getTitle());
        bond_main.setBondMaturity((Date) bondModel.getBondMaturity());
        bond_main.setBondUnitPrice(bondModel.getBondUnitPrice());
        bond_main.setBondType(bondModel.getBondType());
        bond_main.setTaxRate(bondModel.getTaxRate());
        bond_main.setPaymentPlan(bondModel.getPaymentPlan());

        return bond_main;
    }
}
