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
import org.greenpole.entity.model.clientcompany.BondOffer;
import org.greenpole.entity.model.jeph.models.payment.Bond;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.hibernate.entity.BondOfferPaymentPlan;
import org.greenpole.hibernate.entity.BondType;
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
     * Request to create a bond offer.
     * @param login The user's login details
     * @param authenticator The authenticator user meant to receive the
     * notification
     * @param bond The bond details to be created
     * @return response to the create bond offer request
     */
    /*public Response createBondOffer_Request(Login login, String authenticator, BondOffer bond) {
    logger.info("request to create bond offer [{}] at [{}] unit price, invoked by - [{}]",
    bond.getTitle(), bond.getUnitPrice(), login.getUserId());
    
    Response resp = new Response();
    NotificationWrapper wrapper;
    QueueSender queue;
    NotifierProperties prop;
    
    String desc = "";
    boolean flag = false;
    
    try {
    // Check if bond title has a value
    if (bond.getTitle() == null || "".equals(bond.getTitle()) ) {
    desc += "\nBond title should not be empty";
    } else if (bond.getBondTypeId() <= 0) {
    desc += "\nBond type should not be empty";
    } else if (bond.getUnitPrice() <= 0) {
    desc += "\nBond unit price should be greater than zero";
    } else if (bond.getInterestRate() <= 0) {
    desc += "\nBond interest rate should be greater than zero";
    } else if (bond.getPaymentPlanId() <= 0) {
    desc += "\nBond payment plan should be specified";
    } else {
    flag = true;
    }
    
    if (flag && bond.getBondTypeId() > 0) {
    boolean found = false;
    for (org.greenpole.hibernate.entity.BondType bt : cq.getAllBondTypes()) {
    if (bond.getBondTypeId() == bt.getId()) {
    found = true;
    break;
    }
    }
    if (!found) {
    desc += "\nBond type not valid";
    flag = false;
    }
    }
    
    if (flag && bond.getPaymentPlanId() > 0) {
    boolean found = false;
    for (org.greenpole.hibernate.entity.BondOfferPaymentPlan bp : cq.getAllBondOfferPaymentPlans()) {
    if (bond.getPaymentPlanId() == bp.getId()) {
    found = true;
    break;
    }
    }
    if (!found) {
    desc += "\nBond offer payment plan not valid";
    flag = false;
    }
    }
    
    if (flag) {
    wrapper = new NotificationWrapper();
    prop = new NotifierProperties(BondComponent.class);
    queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
    List<BondOffer> bc = new ArrayList<>();
    bc.add(bond);
    
    wrapper.setCode(Notification.createCode(login));
    wrapper.setDescription("Authenticate Bond Setup - " + bond.getTitle());
    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
    wrapper.setFrom(login.getUserId());
    wrapper.setTo(authenticator);
    wrapper.setModel(bc);
    
    logger.info("Notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
    resp = queue.sendAuthorisationRequest(wrapper);
    }
    resp.setRetn(200);
    resp.setDesc("Error filing bond offer details: " + desc);
    logger.info("Error filing bond offer details: [{}] - [{}]", desc, login.getUserId());
    return resp;
    } catch (Exception ex) {
    resp.setRetn(99);
    resp.setDesc("General error. Unable to process bond setup request. Contact system administrator."
    + "\nMessage: " + ex.getMessage());
    logger.info("Error processing bond setup request. See error log. [{}] - [{}]", resp.getRetn(), login.getUserId());
    logger.error("Error processing bond setup request - [" + login.getUserId() + "]", ex);
    return resp;
    }
    }*/

    /**
     * Processes saved request to create bond offer.
     * @param login The user's login details
     * @param notificationCode The notification code
     * @return response to request to create bond offer
     */
    /*public Response setupBondOffer_Authorise(Login login, String notificationCode) {
    Response resp = new Response();
    logger.info("Bond setup creation authorised. [{}] - [{}]", notificationCode, login.getUserId());
    
    String desc = "";
    boolean flag = false;
    
    try {
    NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
    List<BondOffer> list = (List<BondOffer>) wrapper.getModel();
    BondOffer bondModel = list.get(0);
    
    // Check if bond title has a value
    if (bondModel.getTitle() == null || "".equals(bondModel.getTitle()) ) {
    desc += "\nBond title should not be empty";
    } else if (bondModel.getBondTypeId() <= 0) {
    desc += "\nBond type should not be empty";
    } else if (bondModel.getUnitPrice() <= 0) {
    desc += "\nBond unit price should be greater than zero";
    } else if (bondModel.getInterestRate() <= 0) {
    desc += "\nBond interest rate should be greater than zero";
    } else if (bondModel.getPaymentPlanId() <= 0) {
    desc += "\nBond payment plan should be specified";
    } else {
    flag = true;
    }
    
    if (flag && bondModel.getBondTypeId() > 0) {
    boolean found = false;
    for (org.greenpole.hibernate.entity.BondType bt : cq.getAllBondTypes()) {
    if (bondModel.getBondTypeId() == bt.getId()) {
    found = true;
    break;
    }
    }
    if (!found) {
    desc += "\nBond type not valid";
    flag = false;
    }
    }
    
    if (flag && bondModel.getPaymentPlanId() > 0) {
    boolean found = false;
    for (org.greenpole.hibernate.entity.BondOfferPaymentPlan bp : cq.getAllBondOfferPaymentPlans()) {
    if (bondModel.getPaymentPlanId() == bp.getId()) {
    found = true;
    break;
    }
    }
    if (!found) {
    desc += "\nBond offer payment plan not valid";
    flag = false;
    }
    }
    
    if (flag) {
    // get BondOffer entity initialised with Bond Model
    org.greenpole.hibernate.entity.BondOffer bondOffer = bondCreationMain(bondModel);
    cq.createBondOffer(bondOffer);
    resp.setRetn(0);
    resp.setDesc("Successful");
    logger.info("Bond offer created successfully - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(200);
    resp.setDesc("Error filing bond offer details: " + desc);
    logger.info("Error filing bond offer details: [{}] - [{}]", desc, login.getUserId());
    return resp;
    } catch (ParseException ex) {
    resp.setRetn(100);
    resp.setDesc("Error converting string to date type. See error log");
    logger.info("Error converting string to date type. See error log. [{}] - [{}]", resp.getRetn(), login.getUserId());
    logger.error("Error converting string to date type - [" + login.getUserId() + "]", ex);
    return resp;
    } catch (JAXBException ex) {
    resp.setRetn(98);
    resp.setDesc("Error loading notification xml file. See error log");
    logger.info("Error loading notification xml file. See error log. [{}] - [{}]", resp.getRetn(), login.getUserId());
    logger.error("Error loading notification xml file to object - [" + login.getUserId() + "]", ex);
    return resp;
    } catch (Exception ex) {
    resp.setRetn(99);
    resp.setDesc("General error. Unable to create bond. Contact system administrator."
    + "\nMessage: " + ex.getMessage());
    logger.info("Error creating bond. See error log. [{}] - [{}]", resp.getRetn(), login.getUserId());
    logger.error("Error creating bond - [" + login.getUserId() + "]", ex);
    return resp;
    }
    }*/

    /**
     * Unwraps bond model and inserts its details into the bond hibernate entity.
     * @param bondModel the bond model
     * @return BondOffer object to setupBondOfferAuthorise method
     */
    private org.greenpole.hibernate.entity.BondOffer bondCreationMain(BondOffer bondModel) throws ParseException {
        // instantiate required hibernate entities
        org.greenpole.hibernate.entity.BondOffer bond_main = new org.greenpole.hibernate.entity.BondOffer();
        BondOfferPaymentPlan paymentPlan = new BondOfferPaymentPlan();
        BondType type = new BondType();
        
        org.greenpole.hibernate.entity.ClientCompany cc = new org.greenpole.hibernate.entity.ClientCompany();
        
        cc.setId(bondModel.getClientCompanyId());
        type.setId(bondModel.getBondTypeId());
        paymentPlan.setId(bondModel.getPaymentPlanId());
        
        bond_main.setTitle(bondModel.getTitle());
        bond_main.setBondMaturity(formatter.parse(bondModel.getBondMaturity()));
        bond_main.setBondUnitPrice(bondModel.getUnitPrice());
        bond_main.setBondType(type);
        bond_main.setInterestRate(bondModel.getInterestRate());
        bond_main.setPaymentPlan(paymentPlan);
        bond_main.setClientCompany(cc);

        return bond_main;
    }
}
