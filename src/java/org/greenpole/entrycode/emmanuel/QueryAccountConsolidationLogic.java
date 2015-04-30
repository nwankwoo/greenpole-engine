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
import org.greenpole.entrycode.emmanuel.model.QueryConsolidationOfShareholderAccount;
import org.greenpole.hibernate.entity.AccountConsolidation;
import org.greenpole.hibernate.entity.CompanyAccountConsolidation;
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
        CompanyAccountConsolidation compAccCon = new CompanyAccountConsolidation();
        List<CompanyAccountConsolidation> compAccCon_list = new ArrayList();
        List<AccountConsolidation> AccCon_list = new ArrayList();
        AccountConsolidation AccCon = new AccountConsolidation();
        Map<String, String> descriptors = Descriptor.decipherDescriptor(queryParams.getDescriptor());
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
            List<org.greenpole.hibernate.entity.CompanyAccountConsolidation> compAccCon_result_list = hd.queryAccountConsolidation(descriptor, compAccCon, queryParams.getStart_date(), queryParams.getEnd_date());
            /**
             * if(!compAccCon_result_list.isEmpty()){ /** Iterator iterator =
             * compAccCon_result_list.iterator(); while(iterator.hasNext()){
             * wrapper = new NotificationWrapper(); prop = new
             * NotifierProperties(QueryAccountConsolidationLogic.class); qSender
             * = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
             * prop.getAuthoriserNotifierQueueName());
             * wrapper.setCode(Notification.createCode(login));
             * wrapper.setDescription("You have been tagged to view report on
             * consolidation of share holder account " + " by user" +
             * login.getUserId());
             * wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
             * wrapper.setFrom(login.getUserId()); wrapper.setTo(tagUsers);
             * wrapper.setModel(tagUsers); resp =
             * qSender.sendNotificationRequest(wrapper); }
             *
             * }
             * else{ resp.setRetn(210); resp.setDesc("Account consolidation list
             * is empty"); return resp; }
             */

            for (CompanyAccountConsolidation can : compAccCon_result_list) {
                compAccCon.setId(can.getId());
                compAccCon.setInitialChn(can.getInitialChn());
                compAccCon.setCurrentChn(can.getCurrentChn());
                compAccCon.setMergeDate(can.getMergeDate());
                compAccCon.setReceiverStartUnit(can.getReceiverStartUnit());
                compAccCon.setReceiverUnitState(can.getReceiverUnitState());
                compAccCon.setTiedToCurrentHolderId(can.getTiedToCurrentHolderId());
                compAccCon.setTiedToInitialHolderId(can.getTiedToInitialHolderId());
                compAccCon.setUnitAfterTransfer(can.getUnitAfterTransfer());
                compAccCon.setForCompanyId(can.getForCompanyId());
                compAccCon_list.add(compAccCon);

                List<org.greenpole.hibernate.entity.AccountConsolidation> accCon_list = hd.queryAccCon(queryParams.getAccountConsolidation().getHolder().getId());
                for (AccountConsolidation ac : accCon_list) {
                    AccCon.setAdditionalChanges(ac.getAdditionalChanges());
                    AccCon.setDemerge(ac.isDemerge());
                    AccCon.setDemergeDate(ac.getDemergeDate());
                    AccCon.setHolder(ac.getHolder());
                    AccCon.setHolderName(ac.getHolderName());
                    AccCon.setId(ac.getId());
                    AccCon.setMergeDate(ac.getMergeDate());
                    AccCon.setMergedToHolderId(ac.getMergedToHolderId());
                    AccCon.setMergedToHolderName(ac.getMergedToHolderName());
                }

            }
            resp.setRetn(0);
            resp.setDesc("Successful");
            resp.setBody(compAccCon_result_list);
        }
        return resp;
    }
}
