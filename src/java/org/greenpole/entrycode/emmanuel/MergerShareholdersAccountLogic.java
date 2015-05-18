/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.greenpole.entity.model.holder.Holder;
import org.greenpole.entity.model.holder.HolderCompanyAccount;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
//import org.greenpole.entrycode.emmanuel.model.PowerOfAttorney;
import org.greenpole.hibernate.entity.ClientCompany;
import org.greenpole.hibernate.entity.HolderCompanyAccountId;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.Notification;
import org.greenpole.util.properties.NotifierProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author user
 */
public class MergerShareholdersAccountLogic {

    /*private static final Logger logger = LoggerFactory.getLogger(MergerShareholdersAccountLogic.class);
    private final HibernatDummyQuerInterface hd = HibernateDummyQueryFactory.getHibernateDummyQuery();
    
    public Response mergeShareholderAccounts_request(Login login, String authenticator, List<Holder> holder) {
    Response resp = new Response();
    org.greenpole.hibernate.entity.Holder hold = new org.greenpole.hibernate.entity.Holder();
    Holder holderModel = new Holder();
    String fullName = "";
    NotificationWrapper wrapper;
    QueueSender qSender;
    NotifierProperties prop;
    try {
    wrapper = new NotificationWrapper();
    prop = new NotifierProperties(MergerShareholdersAccountLogic.class);
    qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
    prop.getAuthoriserNotifierQueueName());
    List<Holder> listHolder = new ArrayList();
    for (Holder holderObject : holder) {
    holderModel.setFirstName(holderObject.getFirstName());
    holderModel.setLastName(holderObject.getLastName());
    if(holderObject.isPryHolder()==true)
    fullName = holderObject.getFirstName() + " " + holderObject.getLastName();
    //if()
    logger.info("request merge of accounts for holder [{}] by user [{}]" + fullName + " and " + holderObject.isPryHolder() + " as the primary holder account requested by user " + login.getUserId());
    listHolder.add(holderObject);
    wrapper.setCode(Notification.createCode(login));
    wrapper.setDescription("Authorise merge of accounts for holder [{and}] by user [{}]" + fullName + " and " + holderObject.isPryHolder() + " as the primary holder account requested by user " + login.getUserId());
    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
    wrapper.setFrom(login.getUserId());
    wrapper.setTo(authenticator);
    wrapper.setModel(listHolder);
    resp = qSender.sendAuthorisationRequest(wrapper);
    resp.setRetn(0);
    resp.setDesc("Successful");
    //}
    }
    } catch (Exception ex) {
    resp.setRetn(0);
    resp.setDesc("Unable to perform transaction, please contact the administrator");
    logger.error("error: " + ex);
    }
    
    return resp;
    }
    public Response mergeShareholderAccounts_authorise(Login login, String notificationCode){
    Response resp = new Response();
    boolean done = false;
    logger.info("merge of shareholder accounts   authorised - [{}]", notificationCode);
    logger.info("transaction performed by user: "+login.getUserId());
    try{
    NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
    List<Holder> holderList = (List<Holder>)wrapper.getModel();
    
    }catch(Exception ex){}
    
    return resp;
    }
    public List<org.greenpole.hibernate.entity.HolderCompanyAccount> retrieveSecondaryHolderAccounts(Holder hold){
    org.greenpole.hibernate.entity.HolderCompanyAccount holder_hib = new org.greenpole.hibernate.entity.HolderCompanyAccount();
    List<HolderCompanyAccount> holderAccountList = hold.getHolderCompanyAccounts();
    List<org.greenpole.hibernate.entity.HolderCompanyAccount> holderList_hib = new ArrayList();
    for(HolderCompanyAccount accountModel: holderAccountList){
    HolderCompanyAccountId accountId = new HolderCompanyAccountId();
    accountId.setHolderId(accountModel.getHolderId());
    accountId.setClientCompanyId(accountModel.getClientCompanyId());
    holder_hib.setId(accountId);
    holderList_hib.add(holder_hib);
    }
    return holderList_hib;
    }*/
}
