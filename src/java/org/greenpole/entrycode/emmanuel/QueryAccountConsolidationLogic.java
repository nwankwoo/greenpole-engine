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
import org.greenpole.entrycode.emmanuel.model.AccountConsolidation;
import org.greenpole.entrycode.emmanuel.model.CompanyAccountConsolidation;
import org.greenpole.entrycode.emmanuel.model.QueryAccountConsolidation;
import org.greenpole.entrycode.emmanuel.model.QueryConsolidationOfShareholderAccount;
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
    private final HibernatDummyQuerInterface hd = HibernateDummyQueryFactory.getHibernateDummyQuery();

    /**
     * Request to view report on consolidation of Shareholder Accounts
     *
     * @param login the user Id of the user performing the view request
     * @param queryParams the query parameters
     * @return
     */
    public Response viewAccountConsolidation_request(Login login, QueryConsolidationOfShareholderAccount queryParams) {
        logger.info("request to query company account consolidation by user [{}] ", login.getUserId());
        Response resp = new Response();
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        org.greenpole.hibernate.entity.CompanyAccountConsolidation compAccCon = new org.greenpole.hibernate.entity.CompanyAccountConsolidation();
        List<CompanyAccountConsolidation> compAccCon_list = new ArrayList();
        List<AccountConsolidation> AccCon_list = new ArrayList();
        AccountConsolidation AccCon = new AccountConsolidation();
        Map<String, String> descriptors = Descriptor.decipherDescriptor(queryParams.getDescriptor());
        QueryAccountConsolidation queryAccountConsolidation = new QueryAccountConsolidation();
        List<QueryAccountConsolidation> queryAccountConsolidation_list = new ArrayList();
        //org.greenpole.hibernate.entity.Holder holder = hd.retrieveHolderObject(queryParams.getAccountConsolidation().getHolder().getId());
        //Holder holder_model = new Holder();
        if (descriptors.size() == 1) {
            String descriptor = queryParams.getDescriptor();
            if (descriptors.get("date").equalsIgnoreCase("none")) {
                try {
                    formatter.parse(queryParams.getStart_date());
                } catch (ParseException ex) {
                    logger.info("An error occured while checking start date");
                    resp.setRetn(210);
                    resp.setDesc("Incorrect dat format for start date");
                    return resp;
                }
            }
            if (descriptors.get("date").equalsIgnoreCase("between")) {
                try {
                    formatter.parse(queryParams.getEnd_date());
                } catch (ParseException ex) {
                    logger.info("An error occured while checking end date");
                    resp.setRetn(210);
                    resp.setDesc("Incorrect date format for end date");
                    return resp;
                }
            }
            queryAccountConsolidation.setQueryConsolidationOfShareholderAccount(queryParams);
            List<org.greenpole.hibernate.entity.CompanyAccountConsolidation> compAccCon_result_list = hd.queryAccountConsolidation(descriptor, compAccCon, queryParams.getStart_date(), queryParams.getEnd_date());
            for (org.greenpole.hibernate.entity.CompanyAccountConsolidation can : compAccCon_result_list) {
                CompanyAccountConsolidation compAccCon_model = new CompanyAccountConsolidation();
                compAccCon_model.setId(can.getId());
                compAccCon_model.setInitialChn(can.getInitialChn());
                compAccCon_model.setCurrentChn(can.getCurrentChn());
                compAccCon_model.setMergeDate(formatter.format(can.getMergeDate()));
                compAccCon_model.setReceiverStartUnit(can.getReceiverStartUnit());
                compAccCon_model.setReceiverUnitState(can.getReceiverUnitState());
                compAccCon_model.setTiedToCurrentHolderId(can.getTiedToCurrentHolderId());
                compAccCon_model.setTiedToInitialHolderId(can.getTiedToInitialHolderId());
                compAccCon_model.setUnitAfterTransfer(can.getUnitAfterTransfer());
                compAccCon_model.setForCompanyId(can.getForCompanyId());
                compAccCon_list.add(compAccCon_model);
                List<org.greenpole.hibernate.entity.AccountConsolidation> accCon_list = hd.queryAccCon(queryParams.getAccountConsolidation().getHolder().getId());
                for (org.greenpole.hibernate.entity.AccountConsolidation ac : accCon_list) {

                    AccCon.setAdditionalChanges(ac.getAdditionalChanges());
                    AccCon.setDemerge(ac.isDemerge());
                    AccCon.setDemergeDate(formatter.format(ac.getDemergeDate()));
                    AccCon.setHolderName(ac.getHolderName());
                    AccCon.setId(ac.getId());
                    AccCon.setMergeDate(formatter.format(ac.getMergeDate()));
                    AccCon.setMergedToHolderId(ac.getMergedToHolderId());
                    AccCon.setMergedToHolderName(ac.getMergedToHolderName());
                    AccCon_list.add(AccCon);
                }
                //compAccCon_model.setAccountConsolidation(AccCon);
                queryAccountConsolidation.setCompanyAccountConsolidation(compAccCon_list);
                queryAccountConsolidation_list.add(queryAccountConsolidation);
            }
            resp.setRetn(0);
            resp.setDesc("Successful");
            resp.setBody(queryAccountConsolidation_list);
        }
        return resp;
    }
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
