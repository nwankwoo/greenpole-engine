/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.greenpole.entity.model.holder.Holder;
//import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.entity.model.taguser.TagUser;
//import org.greenpole.entrycode.emmanuel.model.AccountConsolidation;
//import org.greenpole.entrycode.emmanuel.model.CompanyAccountConsolidation;
import org.greenpole.entrycode.emmanuel.model.QueryAccountConsolidation;
import org.greenpole.entity.model.holder.QueryHolderConsolidation;
import org.greenpole.hibernate.query.HolderComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
//import org.greenpole.hibernate.entity.AccountConsolidation;
//import org.greenpole.hibernate.entity.CompanyAccountConsolidation;
import org.greenpole.util.Descriptor;
import org.greenpole.util.properties.NotifierProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author user
 */
public class QueryAccountConsolidationLogic {

    private static final Logger logger = LoggerFactory.getLogger(MergerShareholdersAccountLogic.class);
    private final HolderComponentQuery hq = ComponentQueryFactory.getHolderComponentQuery();
    private final HibernatDummyQuerInterface hd = HibernateDummyQueryFactory.getHibernateDummyQuery();

    /**
     * Request to view report on consolidation of Shareholder Accounts
     * @param login the user Id of the user performing the view request
     * @param queryParams the query parameters
     * @return response to the view report on shareholder accounts consolidation request
     */
    /*public Response viewAccountConsolidation_request(Login login, QueryHolderConsolidation queryParams) {
    logger.info("request to query company account consolidation, invoked by [{}] ", login.getUserId());
    Response resp = new Response();
    
    try {
    SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
    
    Map<String, String> descriptors = Descriptor.decipherDescriptor(queryParams.getDescriptor());
    if (descriptors.size() == 1) {
    //check start date is properly formatted
    if (descriptors.get("date").equalsIgnoreCase("none")) {
    try {
    formatter.parse(queryParams.getStartDate());
    } catch (ParseException ex) {
    logger.info("an error was thrown while checking the start date. See error log invoked by [{}]", login.getUserId());
    resp.setRetn(300);
    resp.setDesc("Incorrect date format for start date");
    logger.error("Incorrect date format for start date invoked by [{}]", login.getUserId(), ex);
    }
    }
    //check end date is properly formatted
    if (descriptors.get("date").equalsIgnoreCase("between")) {
    try {
    formatter.parse(queryParams.getEndDate());
    } catch (ParseException ex) {
    logger.info("an error was thrown while checking the start date. See error log - [{}]", login.getUserId());
    resp.setRetn(300);
    resp.setDesc("Incorrect date format for end date");
    logger.error("Incorrect date format for end date - [{}]", login.getUserId(), ex);
    
    return resp;
    }
    }
    List<org.greenpole.hibernate.entity.AccountConsolidation> acctConsolList = hq.getAllHolderAccountConsolidation(queryParams.getDescriptor(), queryParams.getStartDate(), queryParams.getEndDate());
    List<org.greenpole.entity.model.holder.merge.AccountConsolidation> acctConsolModelList = new ArrayList<>();
    TagUser tag = new TagUser();
    for (org.greenpole.hibernate.entity.AccountConsolidation ac : acctConsolList) {
    org.greenpole.entity.model.holder.merge.AccountConsolidation acctConsolModel = new org.greenpole.entity.model.holder.merge.AccountConsolidation();
    acctConsolModel.setId(ac.getId());
    acctConsolModel.setHolderId(ac.getHolder().getId());
    acctConsolModel.setHolderName(ac.getHolderName());
    acctConsolModel.setMergedToHolderId(ac.getMergedToHolderId());
    acctConsolModel.setMergedToHolderName(ac.getMergedToHolderName());
    acctConsolModel.setMergeDate(ac.getMergeDate().toString());
    acctConsolModel.setDemerge(ac.isDemerge());
    acctConsolModel.setAdditionalChanges(ac.getAdditionalChanges());
    acctConsolModel.setDemergeDate(ac.getDemergeDate().toString());
    
    List<org.greenpole.entity.model.holder.merge.CompanyAccountConsolidation> cacList = new ArrayList<>();
    for (org.greenpole.hibernate.entity.CompanyAccountConsolidation cac : hq.getCompAcctConsolidation(ac.getId())) {
    org.greenpole.entity.model.holder.merge.CompanyAccountConsolidation compAcctConsolModel = new org.greenpole.entity.model.holder.merge.CompanyAccountConsolidation();
    compAcctConsolModel.setForCompanyId(cac.getForCompanyId());
    compAcctConsolModel.setTiedToInitialHolderId(cac.getTiedToCurrentHolderId());
    compAcctConsolModel.setTiedToCurrentHolderId(cac.getTiedToCurrentHolderId());
    compAcctConsolModel.setInitialChn(cac.getInitialChn());
    compAcctConsolModel.setCurrentChn(cac.getCurrentChn());
    compAcctConsolModel.setBondShareUnit(cac.getBondShareUnit());
    compAcctConsolModel.setTransfer(cac.isTransfer());
    compAcctConsolModel.setReceiverUnitState(cac.getReceiverUnitState());
    compAcctConsolModel.setReceiverStartUnit(cac.getReceiverStartUnit());
    compAcctConsolModel.setUnitAfterTransfer(cac.getUnitAfterTransfer());
    compAcctConsolModel.setMergeDate(formatter.format(cac.getMergeDate()));
    cacList.add(compAcctConsolModel);
    }
    acctConsolModel.setCompanyAccountConsolidation(cacList);
    acctConsolModelList.add(acctConsolModel);
    }
    List<TagUser> tagList = new ArrayList<>();
    
    tag.setQueryParam(queryParams);
    tag.setResult(acctConsolModelList);
    tagList.add(tag);
    
    resp.setBody(tagList);
    resp.setDesc("Query result with search parameter");
    resp.setRetn(0);
    logger.info("Query successful - [{}]", login.getUserId());
    return resp;
    }
    logger.info("descriptor length does not match expected required length - [{}]", login.getUserId());
    resp.setRetn(308);
    resp.setDesc("descriptor length does not match expected required length");
    return resp;
    } catch (Exception ex) {
    logger.info("error querying holder changes. See error log - [{}]", login.getUserId());
    logger.error("error querying holder changes - [" + login.getUserId() + "]", ex);
    
    resp.setRetn(99);
    resp.setDesc("General error. Unable to query holder changes. Contact system administrator."
    + "\nMessage: " + ex.getMessage());
    return resp;
    }
    }*/

    /**
     * public Response tagUsers_Response(Login login_user, List<Login> tagUsers)
     * { Response resp = new Response(); NotificationWrapper wrapper;
     * QueueSender qSender; NotifierProperties prop; if (!tagUsers.isEmpty()) {
     * Iterator iterator = tagUsers.iterator(); while (iterator.hasNext()) {
     * wrapper = new NotificationWrapper(); prop = new
     * NotifierProperties(QueryAccountConsolidationLogic.class); qSender = new
     * QueueSender(prop.getAuthoriserNotifierQueueFactory(),
     * prop.getAuthoriserNotifierQueueName());
     * wrapper.setCode(Notification.createCode(login_user));
     * wrapper.setDescription("You have been tagged to view report on
     * consolidation of share holder account by " + login_user);
     * wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
     * wrapper.setFrom(login_user.getUserId()); wrapper.setTo(tagUsers);
     * //wrapper.setModel(tagUsers); resp =
     * qSender.sendNotificationRequest(wrapper); //resp =
     * viewAccountConsolidation_request(login_user, queryParams); return resp; }
     * } else{ resp.setRetn(210); resp.setDesc("Tagged users list is empty");
     * return resp; } return resp; }
     *
     */
}
