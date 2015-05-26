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
import org.greenpole.entity.model.holder.Holder;
import org.greenpole.entity.model.taguser.TagUser;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.entrycode.emmanuel.model.Caution;
import org.greenpole.entrycode.emmanuel.model.QueryCautionedHolder;
import org.greenpole.entrycode.emmanuel.model.RightsIssueApplication;
import org.greenpole.hibernate.entity.HolderType;
import org.greenpole.hibernate.query.HolderComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.util.Descriptor;
import org.greenpole.util.properties.GreenpoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author user
 */
public class HolderComponentLogic {

    private static final Logger logger = LoggerFactory.getLogger(HolderComponentLogic.class);
    private final HolderComponentQuery hq = ComponentQueryFactory.getHolderComponentQuery();
    private final GreenpoleProperties greenProp = new GreenpoleProperties(org.greenpole.logic.HolderComponentLogic.class);
    private final HibernatDummyQuerInterface hd = HibernateDummyQueryFactory.getHibernateDummyQuery();//not needed

    /**
     * Request to caution a shareholder / bondholder account
     *
     * @param login the user's login details
     * @param caution details of the cautioned holder
     * @return response to the cautioning of a shareholder / bondholder account
     */
    public Response cautionHolder_Request(Login login, Caution caution) {
        Response resp = new Response();
        //SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        long millis = System.currentTimeMillis();
        Date date_cautioned = new java.sql.Date(millis);
        logger.info("cautioning of holder's account, invoked by [{}]", login.getUserId());
        try {
            org.greenpole.hibernate.entity.Caution caution_hib = new org.greenpole.hibernate.entity.Caution();
            if (hq.checkHolderAccount(caution.getHolder().getHolderId())) {//checks if holder exist
                org.greenpole.hibernate.entity.Holder holder = hq.getHolder(caution.getHolder().getHolderId());
                logger.info("Holder [{}] checks out by - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
                //org.greenpole.hibernate.entity.HolderType ht = hd.getHolderType(caution.getHolderTypeId());
                if (caution.getType() != null || !caution.getType().isEmpty()) {
                    if (caution.getTitle() != null || !caution.getTitle().isEmpty()) {
                        if (caution.getDescription() != null || !caution.getDescription().isEmpty()) {
                            switch (caution.getType()) {
                                case "normal":
                                    caution_hib.setDescription(caution.getDescription());
                                    caution_hib.setTitle(caution.getTitle());
                                    caution_hib.setType(caution.getType());
                                    caution_hib.setHolder(holder);
                                    caution_hib.setId(caution.getId());
                                    caution_hib.setActive(true);
                                    caution_hib.setCautionDate(date_cautioned);
                                    hd.cautionShareHolderAndBondholder(caution_hib);
                                    break;
                                case "special":
                                    boolean holderHasCompanyAcc = hq.hasCompanyAccount(caution.getHolder().getHolderId());
                                    boolean holderHasBondAcc = hq.hasBondAccount(caution.getHolder().getHolderId());
                                    if (holderHasCompanyAcc && !holderHasBondAcc) {
                                        caution_hib.setDescription(caution.getDescription());
                                        caution_hib.setTitle(caution.getTitle());
                                        caution_hib.setType(caution.getType());
                                        caution_hib.setHolder(holder);
                                        caution_hib.setId(caution.getId());
                                        caution_hib.setActive(true);
                                        caution_hib.setCautionDate(date_cautioned);
                                        hd.cautionShareHolderAndBondholder(caution_hib);
                                    } else if (holderHasBondAcc && !holderHasCompanyAcc) {
                                        logger.info("Caution rejected because holder account is bondholder account - [{}]", login.getUserId());
                                        resp.setRetn(328);
                                        resp.setDesc("Caution rejected because holder account is bondholder account");
                                        return resp;
                                    }
                                    break;

                                default:
                                    resp.setRetn(99);
                                    resp.setDesc("Caution type does not exist");
                                    logger.info("Caution type does not exist - [{}]", login.getUserId());
                            }
                        }
                        resp.setRetn(328);
                        resp.setDesc("Reason for caution cannot be empty");
                        logger.info("Reason for caution cannot be empty - [{}]", login.getUserId());
                        return resp;
                    }
                    resp.setRetn(328);
                    resp.setDesc("Caution title cannot be empty");
                    logger.info("Caution title cannot be empty. - [{}]", login.getUserId());
                    return resp;
                }
                resp.setRetn(328);
                resp.setDesc("Caution type not specified");
                logger.info("Cautioning of holder failed because caution type is empty. Check error logs - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(328);
            resp.setDesc("The holder does not exist.");
            logger.info("The holder does not exist - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("Error cautioning holder. See error log - [{}]", login.getUserId());
            logger.error("Error cautioning holder - [" + login.getUserId() + "] - ", ex);
            resp.setRetn(99);
            resp.setDesc("General error: Unable to caution holder, please contact the system administrator." + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    public Response unCautionHolderAccount(Login login, Caution caution) {
        Response resp = new Response();
        //SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        long millis = System.currentTimeMillis();
        Date date_Uncautioned = new java.sql.Date(millis);
        logger.info("uncautioning of holder's account, invoked by [{}]", login.getUserId());
        try {
            org.greenpole.hibernate.entity.Caution caution_hib = new org.greenpole.hibernate.entity.Caution();
            if (hq.checkHolderAccount(caution.getHolder().getHolderId())) {//checks if holder exist
                org.greenpole.hibernate.entity.Holder holder = hq.getHolder(caution.getHolder().getHolderId());
                logger.info("Holder [{}] checks out by - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
                //org.greenpole.hibernate.entity.HolderType ht = hd.getHolderType(caution.getHolderTypeId());
                if (caution.getType() != null || !caution.getType().isEmpty()) {
                    caution_hib.setActive(false);
                    caution_hib.setUncautionDate(date_Uncautioned);
                }
                resp.setRetn(328);
                resp.setDesc("Caution type not specified");
                logger.info("Cautioning of holder failed because caution type is empty. Check error logs - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(328);
            resp.setDesc("The holder does not exist.");
            logger.info("The holder does not exist - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("Error uncautioning holder. See error log - [{}]", login.getUserId());
            logger.error("Error uncautioning holder - [" + login.getUserId() + "] - ", ex);
            resp.setRetn(99);
            resp.setDesc("General error: Unable to uncaution holder, please contact the system administrator." + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     * Request to query cautioned holder accounts.
     *
     * @param login the user's login details
     * @param queryParams the query parameters
     * @return the response to the cautioned holder account request
     */
    public Response queryCautionedHolderAccount_Request(Login login, QueryCautionedHolder queryParams) {
        Response resp = new Response();
        logger.info("request to query cautioned holder account, invoked by [{}]", login.getUserId());
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());

            Map<String, String> descriptors = Descriptor.decipherDescriptor(queryParams.getDescriptor());
            org.greenpole.hibernate.entity.Caution caution_hib = new org.greenpole.hibernate.entity.Caution();
            //HolderType holder_type_hib = hd.getHolderType(queryParams.getCaution().);
            if (descriptors.size() == 1) {
                //check that start date is properly formatted
                if (descriptors.get("date").equalsIgnoreCase("none")) {
                    try {
                        formatter.parse(queryParams.getStart_date());
                    } catch (ParseException ex) {
                        logger.info("an error occured while checking the start date. See error log - [{}]", login.getUserId());
                        resp.setRetn(308);
                        resp.setDesc("Incorrect date format for start date");
                        logger.error("Incorrect date format for start date - [" + login.getUserId() + "]", ex);

                        return resp;
                    }
                }

                //check that start and end dates are properly formatted
                if (descriptors.get("date").equalsIgnoreCase("between")) {
                    try {
                        formatter.parse(queryParams.getStart_date());
                    } catch (ParseException ex) {
                        logger.info("An error occured while checking the start date. See error log - [{}]", login.getUserId());
                        resp.setRetn(308);
                        resp.setDesc("Incorrect date format for start date");
                        logger.error("Incorrect date format for start date - [" + login.getUserId() + "]", ex);

                        return resp;
                    }
                    try {
                        formatter.parse(queryParams.getEnd_date());
                    } catch (ParseException ex) {
                        logger.info("An error occured while checking the end date. See error log - [{}]", login.getUserId());
                        resp.setRetn(308);
                        resp.setDesc("Incorrect date format for end date");
                        logger.error("Incorrect date format for end date - [" + login.getUserId() + "]", ex);

                        return resp;
                    }
                }
                List<org.greenpole.hibernate.entity.Caution> caution_hib_result = hd.queryCautionedHolders(queryParams.getDescriptor(), caution_hib, queryParams.getStart_date(), queryParams.getEnd_date());
                logger.info("retrieved cautioned holder accounts result from query. - [{}]", login.getUserId());
                List<Holder> return_list = new ArrayList<>();
                TagUser tag = new TagUser();
                List<Caution> caution_list = new ArrayList<>();
                //unwrap returned result list
                for (org.greenpole.hibernate.entity.Caution c : caution_hib_result) {
                    org.greenpole.hibernate.entity.Holder h = hd.getHolder(c.getHolder().getId());
                    Holder h_model = new Holder();
                    h_model.setHolderId(h.getId());
                    h_model.setFirstName(h.getFirstName());
                    h_model.setMiddleName(h.getMiddleName());
                    h_model.setLastName(h.getLastName());
                    Caution caution_model = new Caution();
                    caution_model.setDescription(c.getDescription());
                    caution_model.setTitle(c.getTitle());
                    caution_model.setType(c.getType());
                    caution_model.setCautionDate(formatter.format(c.getCautionDate()));
                    caution_model.setActive(c.isActive());
                    caution_model.setHolder(h_model);
                    caution_list.add(caution_model);
                }

                List<TagUser> tagList = new ArrayList<>();

                tag.setQueryParam(queryParams);
                tag.setResult(caution_list);
                tagList.add(tag);

                resp.setBody(tagList);
                resp.setDesc("Query result with search parameter");
                resp.setRetn(0);
                logger.info("Query successful - [{}]", login.getUserId());
                return resp;
            }
        } catch (Exception ex) {
            logger.info("error querying cautioned holder account. See error log - [{}]", login.getUserId());
            logger.error("error querying cautioned holder account - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to query cautioned holder account. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
        return resp;
    }

    public Response applyForRightsIssue_Request(Login login, String authenticator, RightsIssueApplication rightsIssueApp) {
        logger.info("request to apply for rights issue by user " + login.getUserId());
        Response resp = new Response();
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        long millis = System.currentTimeMillis();
        Date date_cautioned = new java.sql.Date(millis);
        try {

        } catch (Exception ex) {
        }
        return resp;
    }
}
