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
import org.greenpole.entity.model.jeph.models.payment.Bond;
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
 * @version 1.0 Business requirement to process bond requests
 */
public class BondComponent {

    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();
    private static final Logger logger = LoggerFactory.getLogger(ClientCompanyComponentLogic.class);
    SimpleDateFormat formatter = new SimpleDateFormat();

    /**
     * processes request to create a bond offer
     *
     * @param login The user's login details
     * @param authenticator The authenticator user meant to receive the
     * notification
     * @param bond The bond details to be processed
     * @return Response object back to sender indicating creation request status
     */
    public Response createBondOffer_Request(Login login, String authenticator, Bond bond) {
        logger.info("User [{}] requests to create a bond offer [{}] at [{}] unit price", login.getUserId(), bond.getTitle(), bond.getBondUnitPrice());

        Response resp = new Response();
        NotificationWrapper wrapper;
        QueueSender queue;
        NotifierProperties prop;
        String resDesc = "";
        boolean flag = false;

        try {
            // Check if bond title has a value
            if ("".equals(bond.getTitle()) || bond.getTitle() == null) {
                resDesc += "\nBond title should not be empty";
            } else if ("".equals(bond.getBondType()) || bond.getBondType() == null) {
                resDesc += "\nBond type should not be empty";
            } else if (bond.getBondUnitPrice() <= 0) {
                resDesc += "\nBond unit price should be greater than zero";
            } else if (bond.getInterestRate() <= 0) {
                resDesc += "\nBond interest rate should be greater than zero";
            } else if (bond.getPaymentPlan() == null || "".equals(bond.getPaymentPlan())) {
                resDesc += "\nBond payment plan should be specified";
            } else {
                flag = true;
            }

            if (flag) {
                wrapper = new NotificationWrapper();
                prop = new NotifierProperties(BondComponent.class);
                queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
                List<Bond> bc = new ArrayList<>();
                bc.add(bond);
                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription("Authenticate Bond Setup - " + bond.getTitle());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(bc);
                resp = queue.sendAuthorisationRequest(wrapper);
                logger.info("Notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
            } else {
                resp.setRetn(200);
                resp.setDesc(resDesc);
                logger.info(resDesc + " [{}] - [{}]", resp.getRetn(), login.getUserId());
            }
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process bond setup request. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            logger.info("Error processing bond setup request. See error log. [{}] - [{}]", resp.getRetn(), login.getUserId());
            logger.error("Error processing bond setup request invoked by [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to persist a bond that had already been saved as a
     * notification file, according to the specified notification code.
     *
     * @param login The user's login details
     * @param notificationCode The notification code
     * @return Response object back to sender indicating authorization request
     * status
     * @throws java.text.ParseException
     */
    public Response setupBondOffer_Authorise(Login login, String notificationCode) throws ParseException {
        Response resp = new Response();
        logger.info("Bond setup creation authorised. [{}] - [{}]", notificationCode, login.getUserId());
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<Bond> list = (List<Bond>) wrapper.getModel();
            Bond bondModel = list.get(0);
            // get BondOffer entity initialised with Bond Model
            BondOffer bondOffer = bondCreationMain(bondModel);
            cq.createBondOffer(bondOffer);
            resp.setRetn(0);
            resp.setDesc("Bond offer created successfully");
            logger.info("Bond offer created successfully. [{}] - [{}]", resp.getRetn(), login.getUserId());
            return resp;
        } catch (ParseException pex) {
            resp.setRetn(100);
            resp.setDesc("Error converting string to date type. See error log");
            logger.info("Error converting string to date type. See error log. [{}] - [{}]", resp.getRetn(), login.getUserId());
            logger.error("Error converting string to date type, invoked by [" + login.getUserId() + "]", pex);
            return resp;
        } catch (JAXBException ex) {
            // TODO: catch other types of exception
            resp.setRetn(100);
            resp.setDesc("Error loading notification xml file. See error log");
            logger.info("Error loading notification xml file. See error log. [{}] - [{}]", resp.getRetn(), login.getUserId());
            logger.error("Error loading notification xml file to object, invoked by [" + login.getUserId() + "]", ex);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to create bond. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            logger.info("Error creating bond. See error log. [{}] - [{}]", resp.getRetn(), login.getUserId());
            logger.error("Error creating bond, invoked by [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Initializes org.greenpole.hibernate.entity.BondOffer entity attributes
     * with values of the org.greenpole.entrycode.jeph.models.Bond model
     * attributes
     *
     * @param bondModel representing a bond model object
     * @return BondOffer object to setupBondOfferAuthorise method
     */
    private BondOffer bondCreationMain(Bond bondModel) throws ParseException {
        // instantiate required hibernate entities
        org.greenpole.hibernate.entity.BondOffer bond_main = new org.greenpole.hibernate.entity.BondOffer();
        // TODO incorporate client company info into the database
        bond_main.setTitle(bondModel.getTitle());
        bond_main.setBondMaturity(formatter.parse(bondModel.getBondMaturity()));
        bond_main.setBondUnitPrice(bondModel.getBondUnitPrice());
        bond_main.setBondType(bondModel.getBondType());
        bond_main.setInterestRate(bondModel.getInterestRate());
        bond_main.setPaymentPlan(bondModel.getPaymentPlan());

        return bond_main;
    }
}
