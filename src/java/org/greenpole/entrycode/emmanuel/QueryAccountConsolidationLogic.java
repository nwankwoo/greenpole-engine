/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.greenpole.entity.security.Login;
import org.greenpole.entrycode.emmanuel.model.*;
import org.greenpole.hibernate.entity.CompanyAccountConsolidation;
import org.greenpole.hibernate.entity.AccountConsolidation;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.Notification;
import org.greenpole.util.Descriptor;
import org.greenpole.util.properties.NotifierProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;

/**
 *
 * @author user
 */
public class QueryAccountConsolidationLogic {

    private static final Logger logger = LoggerFactory.getLogger(MergerShareholdersAccountLogic.class);
    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();

    /**
     * Request to view report on consolidation of Shareholder Accounts
     *
     * @param login the user Id of the user performing the view request
     * @param authenticator
     * @param queryParams the query parameters
     * @return
     */
    public Response viewAccountConsolidation_Request(Login login, String authenticator, QueryConsolidationOfShareholderAccount queryParams) {
        logger.info("request to query company account consolidation by user [{}] ", login.getUserId());

        Response resp = new Response();
        // NotificationWrapper wrapper;
        // QueueSender queue;
        // NotifierProperties prop;
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date startDate = null;
        Date endDate = null;

        Map<String, String> descriptors = Descriptor.decipherDescriptor(queryParams.getDescriptor());

        if (descriptors.size() == 1) {
            String descriptor = queryParams.getDescriptor();
            if (descriptors.get("date").equalsIgnoreCase("none")) {
                try {
                    startDate = formatter.parse(queryParams.getStartDate());
                } catch (ParseException ex) {
                    logger.info("An error occured while checking start date");
                    resp.setRetn(210);
                    resp.setDesc("Incorrect dat format for start date");
                    return resp;
                }
            }
            if (descriptors.get("date").equalsIgnoreCase("between")) {
                try {
                    startDate = formatter.parse(queryParams.getStartDate());
                    endDate = formatter.parse(queryParams.getEndDate());
                } catch (ParseException ex) {
                    logger.info("An error occured while checking end date");
                    resp.setRetn(210);
                    resp.setDesc("Incorrect date format for end date");
                    return resp;
                }
            }
            if (descriptors.get("date").equalsIgnoreCase("on")) {
                try {
                    startDate = formatter.parse(queryParams.getStartDate());
                } catch (ParseException ex) {
                    logger.info("An error occured while checking start date");
                    resp.setRetn(210);
                    resp.setDesc("Incorrect dat format for start date");
                    return resp;
                }
            }
            if (descriptors.get("date").equalsIgnoreCase("before")) {
                try {
                    startDate = formatter.parse(queryParams.getStartDate());
                } catch (ParseException ex) {
                    logger.info("An error occured while checking start date");
                    resp.setRetn(210);
                    resp.setDesc("Incorrect dat format for start date");
                    return resp;
                }
            }
            // List<CompanyAccountConsolidation> acctConsolList = hd.queryCompanyAccountConsolidation(descriptor, queryCompAcctConsolModel.getStart_date(), queryCompAcctConsolModel.getEnd_date());
            List<CompanyAccountConsolidation> compAcctConsolList = new ArrayList<>();
            // List<AccountConsolidation> acctConsolList;
            List<AccountConsolidation> acctConsolList;

            org.greenpole.entrycode.emmanuel.model.AccountConsolidation acctConsolModel = new org.greenpole.entrycode.emmanuel.model.AccountConsolidation();
            org.greenpole.entrycode.emmanuel.model.CompanyAccountConsolidation compAcctConsolModel = new org.greenpole.entrycode.emmanuel.model.CompanyAccountConsolidation();
            List<QueryConsolidationOfShareholderAccount> qcsaList = new ArrayList<>();

            for (CompanyAccountConsolidation cac : compAcctConsolList) {
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
                compAcctConsolModel.setMergeDate(cac.getMergeDate().toString());
                compAcctConsolModel.setAccountConsolidationId(cac.getAccountConsolidation().getId());
                // acctConsolList = hd.queryAccountConsolidation(descriptor, cac.getAccountConsolidation().getId(), queryCompAcctConsolModel.getStart_date(), queryCompAcctConsolModel.getEnd_date());
                acctConsolList = new ArrayList<>();

                for (AccountConsolidation ac : acctConsolList) {
                    acctConsolModel.setId(ac.getId());
                    acctConsolModel.setHolderId(ac.getHolder().getId());
                    acctConsolModel.setHolderName(ac.getHolderName());
                    acctConsolModel.setMergedToHolderId(ac.getMergedToHolderId());
                    acctConsolModel.setMergedToHolderName(ac.getMergedToHolderName());
                    acctConsolModel.setMergeDate(ac.getMergeDate().toString());
                    acctConsolModel.setDemerge(ac.isDemerge());
                    acctConsolModel.setAdditionalChanges(ac.getAdditionalChanges());
                    acctConsolModel.setDemergeDate(ac.getDemergeDate().toString());
                }
                compAcctConsolModel.setAccountConsolidation(acctConsolModel);
                queryParams.getCompanyAccountConsolidation().add(compAcctConsolModel);
                queryParams.getAccountConsolidation().add(acctConsolModel);
            }
            qcsaList.add(queryParams);
            resp.setBody(qcsaList);
            resp.setDesc("Query result with search parameter");
            resp.setRetn(0);
        }
        return resp;
    }
}
