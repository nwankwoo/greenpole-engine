/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.model.holder.HolderBondAccount;
import org.greenpole.entity.model.holder.HolderCompanyAccount;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.hibernate.entity.Bank;
import org.greenpole.hibernate.entity.Holder;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.HolderComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.Notification;
import org.greenpole.util.properties.NotifierProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author user
 */
public class ShareholderAndBondholderNubanLogic {

    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();//expecting IPO query and IPO query factory
    private final HolderComponentQuery hq = ComponentQueryFactory.getHolderComponentQuery();
    private static final Logger logger = LoggerFactory.getLogger(ShareholderAndBondholderNubanLogic.class);
    private final HibernatDummyQuerInterface hd = HibernateDummyQueryFactory.getHibernateDummyQuery();

    /**
     * Request to store a NUBAN account in a shareholder's company account.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param compAcct the shareholder's company account
     * @return response to the store NUBAN account request
     */
    /*public Response storeShareholderNubanAccountNumber_Request(Login login, String authenticator, HolderCompanyAccount compAcct) {
    Response resp = new Response();
    logger.info("Store NUBAN account number to holder company account, invoked by - [{}]", login.getUserId());
    
    NotificationWrapper wrapper;
    QueueSender qSender;
    NotifierProperties prop;
    
    try {
    if (hq.checkHolderAccount(compAcct.getHolderId())) {
    org.greenpole.hibernate.entity.Holder holder = hq.getHolder(compAcct.getHolderId());
    logger.info("Holder [{}] checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
    
    if (hq.checkHolderCompanyAccount(compAcct.getHolderId(), compAcct.getClientCompanyId())) {
    logger.info("[{}]'s company account checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
    
    if (compAcct.getNubanAccount() != null && !"".equals(compAcct.getNubanAccount())) {
    
    if (compAcct.getBank() != null && compAcct.getBank().getId() != 0) {
    
    if (hq.checkBank(compAcct.getBank().getId())) {
    wrapper = new NotificationWrapper();
    prop = new NotifierProperties(ShareholderAndBondholderNubanLogic.class);
    qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
    prop.getAuthoriserNotifierQueueName());
    
    List<HolderCompanyAccount> compAcctList = new ArrayList();
    compAcctList.add(compAcct);
    
    wrapper.setCode(Notification.createCode(login));
    wrapper.setDescription("Authenticate storage of NUBAN account number for holder - " + holder.getFirstName() + " " + holder.getLastName());
    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
    wrapper.setFrom(login.getUserId());
    wrapper.setTo(authenticator);
    wrapper.setModel(compAcctList);
    resp = qSender.sendAuthorisationRequest(wrapper);
    
    resp.setRetn(0);
    resp.setDesc("Successful");
    logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
    return resp;
    }
    resp.setRetn(300);//change
    resp.setDesc("Bank does not exist.");
    logger.info("Bank does not exist - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);//change
    resp.setDesc("Bank cannot be empty.");
    logger.info("Bank account cannot be empty - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);//change
    resp.setDesc("NUBAN account cannot be empty.");
    logger.info("NUBAN account cannot be empty - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);//change
    resp.setDesc("Holder's company account does not exist.");
    logger.info("Holder's company account does not exist - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);//change code
    resp.setDesc("The holder does not exist.");
    logger.info("The holder does not exist - [{}]", login.getUserId());
    return resp;
    } catch (Exception ex) {
    logger.info("error proccessing NUBAN account store. See error log - [{}]", login.getUserId());
    logger.error("error proccessing NUBAN account store - [" + login.getUserId() + "]", ex);
    
    resp.setRetn(99);
    resp.setDesc("General error. Unable to proccess NUBAN account store. Contact system administrator."
    + "\nMessage: " + ex.getMessage());
    return resp;
    }
    }*/

    /**
     * Processes the saved request to store a NUBAN account in a shareholder's company account.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the store NUBAN account request
     */
    /*public Response addShareholderNubanAccountNumber_authorise(Login login, String notificationCode) {
    Response resp = new Response();
    logger.info("authorise NUBAN account number addition to holder company account, invoked by - [{}]", login.getUserId());
    
    try {
    NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
    List<HolderCompanyAccount> compAcctList = (List<HolderCompanyAccount>) wrapper.getModel();
    HolderCompanyAccount compAcct = compAcctList.get(0);
    
    if (hq.checkHolderAccount(compAcct.getHolderId())) {
    org.greenpole.hibernate.entity.Holder holder = hq.getHolder(compAcct.getHolderId());
    logger.info("Holder [{}] checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
    
    if (hq.checkHolderCompanyAccount(compAcct.getHolderId(), compAcct.getClientCompanyId())) {
    org.greenpole.hibernate.entity.HolderCompanyAccount compAcct_hib = hq.getHolderCompanyAccount(compAcct.getHolderId(), compAcct.getClientCompanyId());
    logger.info("[{}]'s company account checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
    
    if (compAcct.getNubanAccount() != null && !"".equals(compAcct.getNubanAccount())) {
    
    if (compAcct.getBank() != null && compAcct.getBank().getId() != 0) {
    
    if (hq.checkBank(compAcct.getBank().getId())) {
    Bank bank = new Bank();
    bank.setId(compAcct.getBank().getId());
    
    compAcct_hib.setId(compAcct_hib.getId());
    compAcct_hib.setNubanAccount(compAcct.getNubanAccount());
    compAcct_hib.setBank(bank);
    
    hq.createUpdateHolderCompanyAccount(compAcct_hib);
    
    resp.setRetn(0);
    resp.setDesc("Successful");
    logger.info("NUBAN account stored - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);//change
    resp.setDesc("Bank does not exist.");
    logger.info("Bank does not exist - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);//change
    resp.setDesc("Bank cannot be empty.");
    logger.info("Bank account cannot be empty - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);//change
    resp.setDesc("NUBAN account cannot be empty.");
    logger.info("NUBAN account cannot be empty - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);//change
    resp.setDesc("Holder's company account does not exist.");
    logger.info("Holder's company account does not exist - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);//change code
    resp.setDesc("The holder does not exist.");
    logger.info("The holder does not exist - [{}]", login.getUserId());
    return resp;
    } catch (JAXBException ex) {
    logger.info("error loading notification xml file. See error log - [{}]", login.getUserId());
    logger.error("error loading notification xml file to object - [" + login.getUserId() + "]", ex);
    
    resp.setRetn(98);
    resp.setDesc("Unable to store NUBAN account. Contact System Administrator");
    
    return resp;
    } catch (Exception ex) {
    logger.info("error storing NUBAN account. See error log - [{}]", login.getUserId());
    logger.error("error storing NUBAN account - [" + login.getUserId() + "]", ex);
    
    resp.setRetn(99);
    resp.setDesc("General error. Unable to store NUBAN account. Contact system administrator."
    + "\nMessage: " + ex.getMessage());
    return resp;
    }
    }*/

    /*public Response storeBondholderNubanAccountNumber_Request(Login login, String authenticator, HolderBondAccount bondAcct) {
    Response resp = new Response();
    logger.info("Store NUBAN account number to holder bond account, invoked by - [{}]", login.getUserId());
    
    NotificationWrapper wrapper;
    QueueSender qSender;
    NotifierProperties prop;
    
    try {
    if (hq.checkHolderAccount(bondAcct.getHolderId())) {
    org.greenpole.hibernate.entity.Holder holder = hq.getHolder(bondAcct.getHolderId());
    logger.info("Holder [{}] checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
    
    if (hq.checkHolderBondAccount(bondAcct.getHolderId(), bondAcct.getBondOfferId())) {
    logger.info("[{}]'s bond account checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
    
    if (bondAcct.getNubanAccount() != null && !"".equals(bondAcct.getNubanAccount())) {
    
    if (bondAcct.getBank() != null && bondAcct.getBank().getId() != 0) {
    
    if (hq.checkBank(bondAcct.getBank().getId())) {
    wrapper = new NotificationWrapper();
    prop = new NotifierProperties(ShareholderAndBondholderNubanLogic.class);
    qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
    prop.getAuthoriserNotifierQueueName());
    
    List<HolderBondAccount> bondAcctList = new ArrayList();
    bondAcctList.add(bondAcct);
    
    wrapper.setCode(Notification.createCode(login));
    wrapper.setDescription("Authenticate storage of NUBAN account number for holder - " + holder.getFirstName() + " " + holder.getLastName());
    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
    wrapper.setFrom(login.getUserId());
    wrapper.setTo(authenticator);
    wrapper.setModel(bondAcctList);
    resp = qSender.sendAuthorisationRequest(wrapper);
    
    resp.setRetn(0);
    resp.setDesc("Successful");
    logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
    return resp;
    }
    resp.setRetn(300);//change
    resp.setDesc("Bank does not exist.");
    logger.info("Bank does not exist - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);//change
    resp.setDesc("Bank cannot be empty.");
    logger.info("Bank account cannot be empty - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);//change
    resp.setDesc("NUBAN account cannot be empty.");
    logger.info("NUBAN account cannot be empty - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);//change
    resp.setDesc("Holder's bond account does not exist.");
    logger.info("Holder's bond account does not exist - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);//change code
    resp.setDesc("The holder does not exist.");
    logger.info("The holder does not exist - [{}]", login.getUserId());
    return resp;
    } catch (Exception ex) {
    logger.info("error proccessing NUBAN account store. See error log - [{}]", login.getUserId());
    logger.error("error proccessing NUBAN account store - [" + login.getUserId() + "]", ex);
    
    resp.setRetn(99);
    resp.setDesc("General error. Unable to proccess NUBAN account store. Contact system administrator."
    + "\nMessage: " + ex.getMessage());
    return resp;
    }
    }
    
    public Response storeBondholderNubanAccountNumber_Authorise(Login login, String notificationCode) {
    Response resp = new Response();
    logger.info("authorise NUBAN account number addition to holder bond account, invoked by - [{}]", login.getUserId());
    
    try {
    NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
    List<HolderBondAccount> bondAcctList = (List<HolderBondAccount>) wrapper.getModel();
    HolderBondAccount bondAcct = bondAcctList.get(0);
    
    if (hq.checkHolderAccount(bondAcct.getHolderId())) {
    org.greenpole.hibernate.entity.Holder holder = hq.getHolder(bondAcct.getHolderId());
    logger.info("Holder [{}] checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
    
    if (hq.checkHolderBondAccount(bondAcct.getHolderId(), bondAcct.getBondOfferId())) {
    org.greenpole.hibernate.entity.HolderBondAccount bondAcct_hib = hq.getHolderBondAccount(bondAcct.getHolderId(), bondAcct.getBondOfferId());
    logger.info("[{}]'s bond account checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
    
    if (bondAcct.getNubanAccount() != null && !"".equals(bondAcct.getNubanAccount())) {
    
    if (bondAcct.getBank() != null && bondAcct.getBank().getId() != 0) {
    
    if (hq.checkBank(bondAcct.getBank().getId())) {
    Bank bank = new Bank();
    bank.setId(bondAcct.getBank().getId());
    
    bondAcct_hib.setId(bondAcct_hib.getId());
    bondAcct_hib.setNubanAccount(bondAcct.getNubanAccount());
    bondAcct_hib.setBank(bank);
    
    hq.createUpdateHolderBondAccount(bondAcct_hib);
    
    resp.setRetn(0);
    resp.setDesc("Successful");
    logger.info("NUBAN account stored - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);//change
    resp.setDesc("Bank does not exist.");
    logger.info("Bank does not exist - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);//change
    resp.setDesc("Bank cannot be empty.");
    logger.info("Bank account cannot be empty - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);//change
    resp.setDesc("NUBAN account cannot be empty.");
    logger.info("NUBAN account cannot be empty - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);//change
    resp.setDesc("Holder's bond account does not exist.");
    logger.info("Holder's bond account does not exist - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);//change code
    resp.setDesc("The holder does not exist.");
    logger.info("The holder does not exist - [{}]", login.getUserId());
    return resp;
    } catch (JAXBException ex) {
    logger.info("error loading notification xml file. See error log - [{}]", login.getUserId());
    logger.error("error loading notification xml file to object - [" + login.getUserId() + "]", ex);
    
    resp.setRetn(98);
    resp.setDesc("Unable to store NUBAN account. Contact System Administrator");
    
    return resp;
    } catch (Exception ex) {
    logger.info("error storing NUBAN account. See error log - [{}]", login.getUserId());
    logger.error("error storing NUBAN account - [" + login.getUserId() + "]", ex);
    
    resp.setRetn(99);
    resp.setDesc("General error. Unable to store NUBAN account. Contact system administrator."
    + "\nMessage: " + ex.getMessage());
    return resp;
    }
    }*/
}