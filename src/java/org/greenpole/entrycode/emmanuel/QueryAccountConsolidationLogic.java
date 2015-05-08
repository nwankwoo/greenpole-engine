/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.greenpole.entity.model.taguser.TagUser;
import org.greenpole.entity.security.Login;
import org.greenpole.entrycode.emmanuel.model.*;
import org.greenpole.hibernate.entity.CompanyAccountConsolidation;
import org.greenpole.hibernate.entity.AccountConsolidation;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.util.Descriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.greenpole.entity.response.Response;
import org.greenpole.hibernate.query.HolderComponentQuery;

/**
 *
 * @author user
 */
public class QueryAccountConsolidationLogic {

    private static final Logger logger = LoggerFactory.getLogger(MergerShareholdersAccountLogic.class);
    private final HolderComponentQuery hq = ComponentQueryFactory.getHolderComponentQuery();

    /**
     * Request to view report on consolidation of Shareholder Accounts
     * @param login the user Id of the user performing the view request
     * @param authenticator
     * @param queryParams the query parameters
     * @return
     */
    public Response viewAccountConsolidation_Request(Login login, String authenticator, QueryConsolidationOfShareholderAccount queryParams) {
        logger.info("request to query company account consolidation, invoked by [{}] ", login.getUserId());
        Response resp = new Response();

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

                    return resp;
                }
            }

            //check end date is properly formatted
            if (descriptors.get("date").equalsIgnoreCase("between")) {
                try {
                    formatter.parse(queryParams.getStartDate());
                } catch (ParseException ex) {
                    logger.info("an error was thrown while checking the start date. See error log - [{}]", login.getUserId());
                    resp.setRetn(300);
                    resp.setDesc("Incorrect date format for start date");
                    logger.error("Incorrect date format for start date - [{}]", login.getUserId(), ex);

                    return resp;
                }

                try {
                    formatter.parse(queryParams.getEndDate());
                } catch (ParseException ex) {
                    logger.info("an error was thrown while checking the end date. See error log - [{}]", login.getUserId());
                    resp.setRetn(300);
                    resp.setDesc("Incorrect date format for end date");
                    logger.error("Incorrect date format for end date - [{}]", login.getUserId(), ex);

                    return resp;
                }
            }

            List<AccountConsolidation> acctConsolList = hq.getAllHolderAccountConsolidation(queryParams.getDescriptor(), queryParams.getStartDate(), queryParams.getEndDate());
            List<org.greenpole.entity.model.holder.merge.AccountConsolidation> acctConsolModelList = new ArrayList<>();
            
            org.greenpole.entity.model.holder.merge.AccountConsolidation acctConsolModel = new org.greenpole.entity.model.holder.merge.AccountConsolidation();
            org.greenpole.entity.model.holder.merge.CompanyAccountConsolidation compAcctConsolModel = new org.greenpole.entity.model.holder.merge.CompanyAccountConsolidation();
            TagUser tag = new TagUser();
            
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
                
                List<CompanyAccountConsolidation> compAcctConsolList = hq.getCompAcctConsolidation(ac.getId());
                List<org.greenpole.entity.model.holder.merge.CompanyAccountConsolidation> cacList = new ArrayList<>();
                
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
                    compAcctConsolModel.setMergeDate(formatter.format(cac.getMergeDate()));
                    cacList.add(compAcctConsolModel);
                }
                acctConsolModel.setCompanyAccountConsolidation(cacList);
                acctConsolModelList.add(acctConsolModel);
            }
            
            List<TagUser> tagList = new ArrayList<>();
            tagList.add(tag);
            
            tag.setQueryParam(queryParams);
            tag.setResult(acctConsolModelList);
            
            resp.setBody(tagList);
            resp.setDesc("Query result with search parameter");
            resp.setRetn(0);            
            logger.info("Query successful - [{}]", login.getUserId());
        }
        return resp;
    }
}
