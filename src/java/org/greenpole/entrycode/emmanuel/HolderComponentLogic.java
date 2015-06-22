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
import org.greenpole.entity.model.holder.HolderCompanyAccount;
import org.greenpole.entity.model.taguser.TagUser;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.entrycode.emmanuel.model.Caution;
import org.greenpole.entrycode.emmanuel.model.QueryCautionedHolder;
import org.greenpole.entrycode.emmanuel.model.RightsIssueApplication;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.GeneralComponentQuery;
import org.greenpole.hibernate.query.HolderComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.Descriptor;
import org.greenpole.util.Notification;
import org.greenpole.util.properties.GreenpoleProperties;
import org.greenpole.util.properties.NotificationProperties;
import org.greenpole.util.properties.NotifierProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author user
 */
public class HolderComponentLogic {

    private static final Logger logger = LoggerFactory.getLogger(HolderComponentLogic.class);
    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();
    private final GeneralComponentQuery gq = ComponentQueryFactory.getGeneralComponentQuery();
    private final NotificationProperties notificationProp = new NotificationProperties(ClientCompanyLogic.class);
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
            org.greenpole.hibernate.entity.Holder holder = hq.getHolder(caution.getHolderId());
            if (hq.checkHolderAccount(holder.getChn())) {//checks if holder exist         
                logger.info("Holder [{}] checks out by - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
                org.greenpole.hibernate.entity.HolderType ht = hd.getHolderType(holder.getId());
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
                                    resp.setRetn(0);
                                    resp.setDesc("Successful");
                                    return resp;

                                case "special":
                                    boolean holderHasCompanyAcc = hq.hasCompanyAccount(holder.getId());
                                    boolean holderHasBondAcc = hq.hasBondAccount(holder.getId());
                                    if (holderHasCompanyAcc && !holderHasBondAcc) {
                                        caution_hib.setDescription(caution.getDescription());
                                        caution_hib.setTitle(caution.getTitle());
                                        caution_hib.setType(caution.getType());
                                        caution_hib.setHolder(holder);
                                        caution_hib.setId(caution.getId());
                                        caution_hib.setActive(true);
                                        caution_hib.setCautionDate(date_cautioned);
                                        hd.cautionShareHolderAndBondholder(caution_hib);
                                        resp.setRetn(0);
                                        resp.setDesc("Successful");
                                        return resp;
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
        boolean updated = false;
        try {
            org.greenpole.hibernate.entity.Caution caution_hib = new org.greenpole.hibernate.entity.Caution();
            if (hq.checkHolderAccount(caution.getHolderId())) {//checks if holder exist
                org.greenpole.hibernate.entity.Holder holder = hq.getHolder(caution.getHolderId());
                logger.info("Holder [{}] checks out by - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
                if (hd.checkCaution(holder.getId(), caution.getId())) {//checks if holder is cautioned
                    if (caution.getType() != null || !caution.getType().isEmpty()) {
                        caution_hib.setActive(false);
                        caution_hib.setUncautionDate(date_Uncautioned);
                        updated = hd.updateCaution(caution.getHolderId(), caution.getId());
                        if (updated) {
                            resp.setRetn(0);
                            resp.setDesc("Successful");
                            return resp;
                        }
                        if (!updated) {
                            logger.info("Unable to uncaution holder - [{}]", login.getUserId());
                            resp.setRetn(300);
                            resp.setDesc("Unable to uncaution holder");
                            return resp;
                        }
                    }
                    resp.setRetn(328);
                    resp.setDesc("Unable to uncaution holder " + holder.getFirstName() + " " + holder.getLastName() + " because caution type is not found");
                    logger.info("Caution type should not be empty. Check error logs - [{}]", login.getUserId());
                    return resp;
                }

                resp.setRetn(328);
                resp.setDesc("No caution record found for holder " + holder.getFirstName() + " " + holder.getLastName());
                logger.info("No caution record found for holder. Check error logs - [{}]", login.getUserId());
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
        Descriptor descriptorUtil = new Descriptor();
        logger.info("request to query cautioned holder account, invoked by [{}]", login.getUserId());
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());

            Map<String, String> descriptors = descriptorUtil.decipherDescriptor(queryParams.getDescriptor());
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
                org.greenpole.hibernate.entity.Caution caution_hib_search = new org.greenpole.hibernate.entity.Caution();
                Caution caution_model = new Caution();
                List<Caution> caution_list = new ArrayList<>();
                List<Holder> return_list = new ArrayList<>();
                TagUser tag = new TagUser();
                if (queryParams.getCaution() != null) {
                    caution_model = queryParams.getCaution();
                    try {
                        caution_hib_search.setCautionDate(formatter.parse(caution_model.getCautionDate()));
                    } catch (ParseException ex) {
                    }

                    // }
                    List<org.greenpole.hibernate.entity.Caution> caution_hib_result = hd.queryCautionedHolders(queryParams.getDescriptor(), caution_hib, queryParams.getStart_date(), queryParams.getEnd_date());
                    logger.info("retrieved cautioned holder accounts result from query. - [{}]", login.getUserId());
                    //unwrap returned result list
                    for (org.greenpole.hibernate.entity.Caution c : caution_hib_result) {
                        caution_model.setDescription(c.getDescription());
                        caution_model.setTitle(c.getTitle());
                        caution_model.setType(c.getType());
                        caution_model.setCautionDate(formatter.format(c.getCautionDate()));
                        caution_model.setActive(c.getActive());
                        caution_model.setHolderId(c.getHolder().getId());
                        caution_list.add(caution_model);
                    }
                    //}

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

    /**
     * processes a request to the user for the confirmation of a rights issue
     * application
     *
     * @param login the user login details
     * @param rightsIssueApp the details of the rights issue application
     * @return response to the confirmation request
     */
    public Response ConfirmApplicationForRightsIssue_Request(Login login, RightsIssueApplication rightsIssueApp) {
        logger.info("request to apply for rights issue by user " + login.getUserId());
        Response resp = new Response();
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        long millis = System.currentTimeMillis();
        Date current_date = new java.sql.Date(millis);
        try {
            boolean verify = false;
            int checkForQualifyShares = 0;
            //if (cq.checkClientCompany(rightsIssueApp.getClientCompanyId())) {
            org.greenpole.hibernate.entity.Holder holder = hd.getHolder(rightsIssueApp.getHolder().getHolderId());
            RightsIssueApplication confirmRight = new RightsIssueApplication();
            if (hd.checkRightsIssue(rightsIssueApp.getRightsIssueId(), rightsIssueApp.getClientCompanyId())) {
                org.greenpole.hibernate.entity.RightsIssue ri = hd.getRightsIssueById(rightsIssueApp.getRightsIssueId(), rightsIssueApp.getClientCompanyId());
                if (!ri.getRightsClosed() && ri.getClosingDate().equals(current_date) || ri.getClosingDate().before(current_date)) {
                    org.greenpole.hibernate.entity.RightsIssueApplication rightApp_hib = new org.greenpole.hibernate.entity.RightsIssueApplication();
                    org.greenpole.hibernate.entity.Holder h_hib = hd.getHolder(rightsIssueApp.getHolder().getHolderId());
                    List<org.greenpole.hibernate.entity.RightsIssueApplication> rightsApp_hib_list = hd.getAllRightsIssueApplications(rightsIssueApp.getClientCompanyId(), rightsIssueApp.getRightsIssueId());
                    List<RightsIssueApplication> rightList = new ArrayList();
                    org.greenpole.hibernate.entity.RightsIssueApplication oneShareholderApp = hd.getOneHolderRightApplication(rightsIssueApp.getHolder().getHolderId(), rightsIssueApp.getClientCompanyId(), rightsIssueApp.getRightsIssueId());
                    if (ri.getTotalSharesOnIssue() > rightsIssueApp.getSharesSubscribed()) {
                        if (h_hib.getChn() != null && !"".equals(h_hib.getChn())) {
                            verify = hd.checkHolderCompanyAccount(rightsIssueApp.getHolder().getHolderId(), rightsIssueApp.getClientCompanyId());
                            if (verify) {
                                if (rightsIssueApp.getSharesSubscribed() <= oneShareholderApp.getAllottedRights() || rightsIssueApp.getSharesSubscribed() > 0) {
                                    checkForQualifyShares = rightsIssueApp.getSharesSubscribed() % ri.getQualifyShareUnit();
                                    if (checkForQualifyShares == 0) {
                                        confirmRight.setSharesSubscribed(rightsIssueApp.getSharesSubscribed());
                                        confirmRight.setSharesSubscribedValue(rightsIssueApp.getSharesSubscribed() * ri.getIssuePrice());
                                        confirmRight.setAdditionalSharesSubscribed(0);
                                        confirmRight.setAmountPaid(rightsIssueApp.getAmountPaid());
                                        confirmRight.setIssuer(rightsIssueApp.getIssuer());
                                        confirmRight.setAdditionalSharesSubValue(0.0);
                                        confirmRight.setAdditionalSharesAwaitingSub(ri.getTotalSharesOnIssue() - rightsIssueApp.getSharesSubscribed());//with the assumption that additional shares is not given to shareholder at this point
                                        confirmRight.setTotalSharesSubscribed(rightsIssueApp.getSharesSubscribed() + rightsIssueApp.getAdditionalSharesSubscribed());
                                        confirmRight.setTotalValue((rightsIssueApp.getSharesSubscribed() + rightsIssueApp.getAdditionalSharesSubscribed()) * ri.getIssuePrice());
                                        rightList.add(rightsIssueApp);
                                        resp.setBody(rightList);
                                        return resp;
                                    } else {
                                        resp.setRetn(300);
                                        resp.setDesc("Unable to apply for rights because shareholder subscribing rights is less than alloted rights"
                                                + " but shares to subscribe does not add up after taking into account the qualify share unit "
                                                + "and the allowed share unit per qualify share unit.");
                                        return resp;
                                    }

                                } else if (rightsIssueApp.getSharesSubscribed() == oneShareholderApp.getAllottedRights() && rightsIssueApp.getAdditionalSharesSubscribed() > 0) {
                                    confirmRight.setSharesSubscribed(rightsIssueApp.getSharesSubscribed());
                                    confirmRight.setSharesSubscribedValue(rightsIssueApp.getSharesSubscribed() * ri.getIssuePrice());
                                    confirmRight.setAdditionalSharesSubscribed(rightsIssueApp.getAdditionalSharesSubscribed());
                                    confirmRight.setAmountPaid(rightsIssueApp.getAmountPaid());
                                    confirmRight.setIssuer(rightsIssueApp.getIssuer());
                                    confirmRight.setAdditionalSharesSubValue(rightsIssueApp.getAdditionalSharesSubscribed() * ri.getIssuePrice());
                                    confirmRight.setAdditionalSharesAwaitingSub(ri.getTotalSharesOnIssue() - rightsIssueApp.getSharesSubscribed());//with the assumption that additional shares is not given to shareholder at this point
                                    confirmRight.setTotalSharesSubscribed(rightsIssueApp.getSharesSubscribed() + rightsIssueApp.getAdditionalSharesSubscribed());
                                    confirmRight.setTotalValue((rightsIssueApp.getSharesSubscribed() + rightsIssueApp.getAdditionalSharesSubscribed()) * ri.getIssuePrice());
                                    rightList.add(rightsIssueApp);
                                    resp.setBody(rightList);
                                    return resp;
                                }
                            }
                            resp.setRetn(300);
                            resp.setDesc("Unable to apply for rights because shareholder does not have a company account ");
                            return resp;
                        }
                        resp.setRetn(300);
                        resp.setDesc("Unable to apply for rights because shareholder does not have a CHN ");
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Remaining shares is less than the subscribed shares");
                    logger.info("Remaining shares is less than the subscribed shares invoked by - " + login.getUserId());
                    return resp;
                    //}
                }
                resp.setRetn(200);
                resp.setDesc("Sorry, rights issue has been closed");
                logger.info("Sorry, rights issue has been closed invoked by " + login.getUserId());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Rights issue does not exist for this client company");
            logger.info("Rights issue does not exist for the client company invoked by " + login.getUserId());
            return resp;
            // }
        } catch (Exception ex) {
            logger.info("error applying for rights issue. See error log - [{}]", login.getUserId());
            logger.error("error applying for rights issue - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to apply for rights. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     * processes a request for rights issue application after confirmation has
     * been done
     *
     * @param login the user login details
     * @param authenticator the super
     * @param rightsIssueApp the details of the rights issue application
     * @return response to the application request
     */
    public Response ApplicationForRightsIssue_Request(Login login, String authenticator, RightsIssueApplication rightsIssueApp) {
        logger.info("request to apply for rights issue by user [{}] ", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        long millis = System.currentTimeMillis();
        Date current_date = new java.sql.Date(millis);
        boolean verify = false;
        int checkForQualifyShares = 0;
        try {
            org.greenpole.hibernate.entity.Holder holder = hd.getHolder(rightsIssueApp.getHolder().getHolderId());
            org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(rightsIssueApp.getClientCompanyId());
            org.greenpole.hibernate.entity.Holder h_hib = hd.getHolder(rightsIssueApp.getHolder().getHolderId());
            if (hd.checkRightsIssue(rightsIssueApp.getRightsIssueId(), rightsIssueApp.getClientCompanyId())) {
                org.greenpole.hibernate.entity.RightsIssue ri = hd.getRightsIssueById(rightsIssueApp.getRightsIssueId(), rightsIssueApp.getClientCompanyId());
                if (!ri.getRightsClosed() && ri.getClosingDate().equals(current_date) || ri.getClosingDate().before(current_date)) {
                    org.greenpole.hibernate.entity.RightsIssueApplication rightApp_hib = new org.greenpole.hibernate.entity.RightsIssueApplication();
                    org.greenpole.hibernate.entity.RightsIssueApplication oneShareholderApp = hd.getOneHolderRightApplication(rightsIssueApp.getHolder().getHolderId(), rightsIssueApp.getClientCompanyId(), rightsIssueApp.getRightsIssueId());
                    if (ri.getTotalSharesOnIssue() > rightsIssueApp.getSharesSubscribed()) {
                        if (h_hib.getChn() != null && !"".equals(h_hib.getChn())) {
                            verify = hd.checkHolderCompanyAccount(rightsIssueApp.getHolder().getHolderId(), rightsIssueApp.getClientCompanyId());
                            if (verify) {
                                if (rightsIssueApp.getSharesSubscribed() <= oneShareholderApp.getAllottedRights() || rightsIssueApp.getSharesSubscribed() > 0) {
                                    checkForQualifyShares = rightsIssueApp.getSharesSubscribed() % ri.getQualifyShareUnit();
                                    if (checkForQualifyShares == 0) {
                                        wrapper = new NotificationWrapper();
                                        prop = new NotifierProperties(HolderComponentLogic.class);
                                        qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                                                prop.getAuthoriserNotifierQueueName());
                                        List<RightsIssueApplication> riList = new ArrayList();
                                        riList.add(rightsIssueApp);
                                        wrapper.setCode(notification.createCode(login));
                                        wrapper.setDescription("Authenticate application of rights issue under the client company " + cc.getName() + " invoked by user " + login.getUserId());
                                        wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                                        wrapper.setFrom(login.getUserId());
                                        wrapper.setTo(authenticator);
                                        wrapper.setModel(riList);
                                        logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                                        resp = qSender.sendAuthorisationRequest(wrapper);
                                        return resp;
                                    }
                                    resp.setRetn(300);
                                    resp.setDesc("Unable to apply for rights because shareholder subscribing rights is less than alloted rights"
                                            + " but shares to subscribe does not add up after taking into account the qualify share unit "
                                            + "and the allowed share unit per qualify share unit.");
                                    return resp;
                                }
                                resp.setRetn(300);
                                resp.setDesc("Unable to apply for rights because shareholder subscribing units is empty");
                                return resp;
                            }
                            resp.setRetn(300);
                            resp.setDesc("Unable to apply for rights because shareholder does not have a company account ");
                            return resp;
                        }
                        resp.setRetn(300);
                        resp.setDesc("Unable to apply for rights because shareholder does not have a CHN ");
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Remaining shares is less than the subscribed shares");
                    logger.info("Remaining shares is less than the subscribed shares invoked by - " + login.getUserId());
                    return resp;
                    //}
                }
                resp.setRetn(200);
                resp.setDesc("Sorry, rights issue has been closed");
                logger.info("Sorry, rights issue has been closed invoked by " + login.getUserId());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Rights issue does not exist for this client company");
            logger.info("Rights issue does not exist for the client company invoked by " + login.getUserId());
            return resp;
            // }
        } catch (Exception ex) {
            logger.info("error applying for rights issue. See error log - [{}]", login.getUserId());
            logger.error("error applying for rights issue - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to apply for rights. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     *
     * @param login the user login details
     * @param notificationCode the notification code
     * @return response to the rights application
     */
    public Response ApplicationForRightsIssue_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        logger.info("authorise rights issue application, invoked by - [{}] " + login.getUserId());
        Notification notification = new Notification();
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        long millis = System.currentTimeMillis();
        Date current_date = new java.sql.Date(millis);
        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<RightsIssueApplication> riList = (List<RightsIssueApplication>) wrapper.getModel();
            RightsIssueApplication rightsIssueApp = riList.get(0);
            int checkForQualifyShares;
            long remClientCompanySharesAfterSub = 0;
            int newShares = 0;
            boolean verify = false;
            if (hd.checkRightsIssue(rightsIssueApp.getRightsIssueId(), rightsIssueApp.getClientCompanyId())) {
                org.greenpole.hibernate.entity.RightsIssue ri = hd.getRightsIssueById(rightsIssueApp.getRightsIssueId(), rightsIssueApp.getClientCompanyId());
                org.greenpole.hibernate.entity.HolderCompanyAccount hca_hib = new org.greenpole.hibernate.entity.HolderCompanyAccount();
                if (!ri.getRightsClosed() && ri.getClosingDate().before(current_date) || ri.getClosingDate().equals(current_date)) {
                    org.greenpole.hibernate.entity.RightsIssueApplication rightApp_hib = new org.greenpole.hibernate.entity.RightsIssueApplication();
                    org.greenpole.hibernate.entity.Holder h_hib = hd.getHolder(rightsIssueApp.getHolder().getHolderId());
                    org.greenpole.hibernate.entity.RightsIssueApplication oneHolderRightsApp = hd.getOneHolderRightApplication(rightsIssueApp.getHolder().getHolderId(), rightsIssueApp.getClientCompanyId(), rightsIssueApp.getRightsIssueId());
                    if (ri.getTotalSharesOnIssue() > rightsIssueApp.getSharesSubscribed()) {
                        if (h_hib.getChn() != null && !"".equals(h_hib.getChn())) {
                            verify = hd.checkHolderCompanyAccount(rightsIssueApp.getHolder().getHolderId(), rightsIssueApp.getClientCompanyId());
                            if (verify) {
                                if (oneHolderRightsApp.getAllottedRights() >= rightsIssueApp.getSharesSubscribed()) {
                                    checkForQualifyShares = rightsIssueApp.getSharesSubscribed() % ri.getQualifyShareUnit();
                                    if (checkForQualifyShares == 0) {
                                        if (rightsIssueApp.getAmountPaid() == (rightsIssueApp.getSharesSubscribed() * ri.getIssuePrice()) + (rightsIssueApp.getAdditionalSharesSubscribed() * ri.getIssuePrice())) {
                                            rightApp_hib.setSharesSubscribed(rightsIssueApp.getSharesSubscribed());
                                            rightApp_hib.setSharesSubscribedValue(rightsIssueApp.getSharesSubscribed() * ri.getIssuePrice());
                                            rightApp_hib.setAdditionalSharesSubscribed(0);
                                            rightApp_hib.setAdditionalSharesSubValue(0.0);
                                            rightApp_hib.setTotalSharesRenounced(oneHolderRightsApp.getAllottedRights() - rightsIssueApp.getSharesSubscribed());//renounced shares
                                            rightApp_hib.setReturnMoney(0.0);
                                            rightApp_hib.setProcessingPayment(true);
                                            hd.applyForRightIssue(rightApp_hib);//updates shareholder existing application
                                            hca_hib = hd.getOneHolderCompanyAccount(rightsIssueApp.getHolder().getHolderId(), rightsIssueApp.getClientCompanyId());
                                            newShares = hca_hib.getShareUnits() + rightsIssueApp.getSharesSubscribed();//adds shares subscribed to HCA
                                            hca_hib.setShareUnits(newShares);//sets holder new share units
                                            hd.updateHCA(hca_hib.getHolder().getId(), ri.getClientCompany().getId());//updates holder new share units value
                                            remClientCompanySharesAfterSub = ri.getTotalSharesOnIssue() - rightsIssueApp.getSharesSubscribed();
                                            ri.setTotalSharesOnIssue(remClientCompanySharesAfterSub);//change to value after subscription
                                            hd.updateRightIssueTotalShares(ri.getClientCompany().getId(), ri.getId());
                                            notification.markAttended(notificationCode);
                                            logger.info("Rights issue application authorised - [{}]", login.getUserId());
                                            resp.setRetn(0);
                                            resp.setDesc("Application was successful");
                                            return resp;
                                        }
                                        resp.setRetn(300);
                                        resp.setDesc("Unable to apply for rights because shareholder subscribing rights is less than alloted rights"
                                                + " but shares to subscribe does not add up after taking into account the qualify share unit "
                                                + "and the allowed share unit per qualify share unit.");
                                    } else if (oneHolderRightsApp.getAllottedRights() <= rightsIssueApp.getSharesSubscribed() && rightsIssueApp.getAdditionalSharesSubscribed() > 0) {
                                        rightApp_hib.setSharesSubscribed(rightsIssueApp.getSharesSubscribed());
                                        rightApp_hib.setAmountPaid(rightsIssueApp.getAmountPaid());
                                        rightApp_hib.setSharesSubscribedValue(rightsIssueApp.getSharesSubscribed() * ri.getIssuePrice());
                                        rightApp_hib.setAdditionalSharesSubscribed(rightsIssueApp.getAdditionalSharesSubscribed());//assuming that shares subscribe and additional shares are entered by the user
                                        rightApp_hib.setAdditionalSharesSubValue((rightsIssueApp.getAdditionalSharesSubscribed() * ri.getIssuePrice()));
                                        rightApp_hib.setTotalSharesRenounced(0);
                                        rightApp_hib.setReturnMoney(0.0);
                                        rightApp_hib.setProcessingPayment(true);
                                        rightApp_hib.setApproved(true);
                                        hd.applyForRightIssue(rightApp_hib);//updates shareholder existing application
                                        hca_hib = hd.getOneHolderCompanyAccount(rightsIssueApp.getHolder().getHolderId(), rightsIssueApp.getClientCompanyId());
                                        newShares = hca_hib.getShareUnits() + rightsIssueApp.getSharesSubscribed();//adds shares subscribed to HCA
                                        hca_hib.setShareUnits(newShares);//sets holder new share units
                                        hd.updateHCA(hca_hib.getHolder().getId(), ri.getClientCompany().getId());//updates holder new share units value
                                        remClientCompanySharesAfterSub = ri.getTotalSharesOnIssue() - rightsIssueApp.getSharesSubscribed();
                                        ri.setTotalSharesOnIssue(remClientCompanySharesAfterSub);//change to value after subscription
                                        hd.updateRightIssueTotalShares(ri.getClientCompany().getId(), ri.getId());
                                        notification.markAttended(notificationCode);
                                        logger.info("Rights issue application authorised - [{}]", login.getUserId());
                                        resp.setRetn(0);
                                        resp.setDesc("Application was successful");
                                        return resp;
                                    }

                                }
                                resp.setRetn(300);
                                resp.setDesc("Unable to apply for rights because shareholder does not have company account.");
                                logger.info("Unable to apply for rights because shareholder does not have company account ", login.getUserId());
                                return resp;
                            }
                            resp.setRetn(300);
                            resp.setDesc("Unable to apply for rights because the amount paid does not equal system calculation of the share value subscribing.");
                            logger.info("Unable to apply for rights because the amount paid does not equal system calculation of the share value subscribing ", login.getUserId());
                            return resp;
                        }
                        resp.setRetn(300);
                        resp.setDesc("Unable to apply for rights because the shareholder does not have a CHN.");
                        logger.info("Unable to apply for rights because the shareholder does not have a CHN ", login.getUserId());
                        return resp;
                    }
                    resp.setRetn(300);
                    resp.setDesc("Unable to apply for rights because available share units is less than subscribed share units.");
                    logger.info("Unable to apply for rights because available share units is less than subscribed share units ", login.getUserId());
                    return resp;
                }
                resp.setRetn(300);
                resp.setDesc("Unable to apply for rights because rights issue is closed.");
                logger.info("Unable to apply for rights because rights issue is closed ", login.getUserId());
                return resp;
            }
            resp.setRetn(300);
            resp.setDesc("Rights issue applying for does not exist.");
            logger.info("Rights issue applying for does not exist ", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error applying for rights issue. See error log - [{}]", login.getUserId());
            logger.error("error applying for rights issue - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to apply for rights. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     * processes request to cancel a holder rights issue application
     * @param login details of the user
     * @param authenticator the user meant to receive the authentication
     * @param holderRightsIssue the holder application to be cancelled
     * @return request to the cancellation of holder application
     */
    public Response cancelRightsIssue_Request(Login login, String authenticator, RightsIssueApplication holderRightsIssue) {
        Response resp = new Response();
        logger.info("authorise rights issue cancellation, invoked by - [{}] " + login.getUserId());
        Notification notification = new Notification();
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        try {
            org.greenpole.hibernate.entity.Holder holder = hd.getHolder(holderRightsIssue.getHolder().getHolderId());
            org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(holderRightsIssue.getClientCompanyId());
            org.greenpole.hibernate.entity.RightsIssueApplication riApp = hd.getOneHolderRightApplication(holderRightsIssue.getHolder().getHolderId(), holderRightsIssue.getClientCompanyId(), holderRightsIssue.getId());
            if (hq.checkHolderAccount(holderRightsIssue.getHolder().getHolderId())) {
                logger.info("Holder checks out ");
                //boolean verify = hd.checkHolderCompanyAccount(holderRightsIssue.getHolderId(), holderRightsIssue.getClientCompanyId());
                if (hd.checkHolderCompanyAccount(holderRightsIssue.getHolder().getHolderId(), holderRightsIssue.getClientCompanyId())) {
                    logger.info("Holder company account checks out " + login.getUserId());
                    if (!riApp.getApproved() || !riApp.getProcessingPayment()) {//checks that application is not approved and not processed for payment also
                        wrapper = new NotificationWrapper();
                        prop = new NotifierProperties(HolderComponentLogic.class);
                        qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                                prop.getAuthoriserNotifierQueueName());
                        List<RightsIssueApplication> riList = new ArrayList();
                        riList.add(holderRightsIssue);
                        wrapper.setCode(notification.createCode(login));
                        wrapper.setDescription("Authenticate cancellation of rights issue application for holder  " + holder.getFirstName() + holder.getLastName() + " invoked by user " + login.getUserId());
                        wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                        wrapper.setFrom(login.getUserId());
                        wrapper.setTo(authenticator);
                        wrapper.setModel(riList);
                        logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                        resp = qSender.sendAuthorisationRequest(wrapper);
                        //requires a method to send notification message to the holder
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Application has already been approved or is been processed for payment");
                    logger.info("Application has already been approved or is been processed for payment");
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Holder company account not found");
                logger.info("Holder company account not found");
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Holder does not exist");
            logger.info("Holder does not exist");
            return resp;
        } catch (Exception ex) {
            logger.info("error cancelling holder's rights issue application. See error log - [{}]", login.getUserId());
            logger.error("error cancelling holder's rights issue application - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to cancel holder's rights issue application. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     * processes request to cancel a holder rights issue application
     * @param login details of the user
     * @param notificationCode the user meant to receive the authentication
     * @return request to the cancellation of holder application
     */
    public Response cancelRightsIssue_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        logger.info("authorise rights issue cancellation, invoked by - [{}] " + login.getUserId());
        Notification notification = new Notification();
        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<RightsIssueApplication> riList = (List<RightsIssueApplication>) wrapper.getModel();
            RightsIssueApplication rightsIssueApp = riList.get(0);
            org.greenpole.hibernate.entity.Holder holder = hd.getHolder(rightsIssueApp.getHolder().getHolderId());
            org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(rightsIssueApp.getClientCompanyId());
            org.greenpole.hibernate.entity.RightsIssue rightIssue = hd.getRightsIssueById(rightsIssueApp.getRightsIssueId(), rightsIssueApp.getClientCompanyId());
            org.greenpole.hibernate.entity.RightsIssueApplication riApp = hd.getOneHolderRightApplication(rightsIssueApp.getHolder().getHolderId(), rightsIssueApp.getClientCompanyId(), rightsIssueApp.getId());
            if (hq.checkHolderAccount(rightsIssueApp.getHolder().getHolderId())) {
                logger.info("Holder checks out ");
                //boolean verify = hd.checkHolderCompanyAccount(holderRightsIssue.getHolderId(), holderRightsIssue.getClientCompanyId());
                if (hd.checkHolderCompanyAccount(rightsIssueApp.getHolder().getHolderId(), rightsIssueApp.getClientCompanyId())) {
                    logger.info("Holder company account checks out " + login.getUserId());
                    if (!riApp.getApproved() || !riApp.getProcessingPayment()) {//checks that application is not approved and not processed for payment also
                        long compAvailableRights = rightIssue.getTotalSharesOnIssue();//this increases every time a cancellation is done
                        org.greenpole.hibernate.entity.RightsIssue ri_hib = new org.greenpole.hibernate.entity.RightsIssue();
                        org.greenpole.hibernate.entity.RightsIssueApplication riApp_hib = new org.greenpole.hibernate.entity.RightsIssueApplication();
                        int holderAppliedRights = riApp.getSharesSubscribed();
                        long returnRights = holderAppliedRights + compAvailableRights;
                        ri_hib.setTotalSharesOnIssue(returnRights);
                        hd.updateRightIssueTotalShares(rightsIssueApp.getClientCompanyId(), rightsIssueApp.getRightsIssueId());
                        riApp_hib.setCanceled(true);
                        boolean status = hd.updateCancelleRightsApp(rightsIssueApp.getClientCompanyId(), rightsIssueApp.getHolder().getHolderId(), rightsIssueApp.getRightsIssueId());
                        notification.markAttended(notificationCode);
                        logger.info("Successfully cancelled rights application for holder " + holder.getFirstName() + " " + holder.getLastName() + " invoked by " + login.getUserId());
                        resp.setRetn(0);
                        resp.setDesc("Successfully cancelled rights application for holder " + holder.getFirstName() + " " + holder.getLastName());
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Application has already been approved or is been processed for payment");
                    logger.info("Application has already been approved or is been processed for payment");
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Holder company account not found");
                logger.info("Holder company account not found");
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Holder does not exist");
            logger.info("Holder does not exist");
            return resp;
        } catch (Exception ex) {
            logger.info("error cancelling holder's rights issue application. See error log - [{}]", login.getUserId());
            logger.error("error cancelling holder's rights issue application - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to cancel holder's rights issue application. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     * processes confirmation request to upload rights issue en - mass
     * @param login details of the user
     * @param authenticator the user meant to receive this notification
     * @param holdersRightApp the list of holders applying for rights issue
     * @return request to the confirmation holders application
     */
    public Response uploadShareholderRightsIssueEnmassConfirmation_Request(Login login, String authenticator, List<RightsIssueApplication> holdersRightApp) {
        logger.info("request to upload shareholders rights issue en-mass invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;
            int counter;
            double sumAmountPaid_hib = 0;
            double sumAmountPaid_model = 0;
            double valueOfSharesSub = 0.0;
            boolean checkHolders = false;
            boolean checkHCA = false;
            long availableRights = 0;
            long renouncedShares;
            int noOfHoldersShares_hib, allotedHolderRights;
            long sumOfSharesSubscribe = 0;
            int prevTotalSharesSub = 0;
            long additionalSharesAwaitingSub, remaingSharesAfterCurrentSub;
            org.greenpole.hibernate.entity.Holder holderObj = new org.greenpole.hibernate.entity.Holder();
            noOfHoldersShares_hib = 0;
            sumOfSharesSubscribe = 0;
            allotedHolderRights = 0;
            List<RightsIssueApplication> subscribingHolders_list = new ArrayList<>();
            RightsIssueApplication right_model = new RightsIssueApplication();
            for (counter = 0; counter < holdersRightApp.size(); counter++) {
                sumOfSharesSubscribe += holdersRightApp.get(counter).getSharesSubscribed();
                checkHolders = hq.checkHolderAccount(holdersRightApp.get(counter).getHolder().getHolderId());
                holderObj = hq.getHolder(holdersRightApp.get(counter).getHolder().getHolderId());
                checkHCA = hq.checkHolderCompanyAccount(holdersRightApp.get(counter).getHolder().getHolderId(), holdersRightApp.get(counter).getClientCompanyId());
                sumAmountPaid_model += holdersRightApp.get(counter).getAmountPaid();
                if (!checkHolders) {
                    logger.info("preparing to create non - existing holders " + login.getUserId());
                    Holder newHolder = new Holder();
                    // newHolder.setHolderId(holdersRightApp.get(counter)); ask if user will send holder object as well
                    newHolder.setFirstName(holdersRightApp.get(counter).getHolder().getFirstName());
                    newHolder.setMiddleName(holdersRightApp.get(counter).getHolder().getMiddleName());
                    newHolder.setLastName(holdersRightApp.get(counter).getHolder().getLastName());
                    newHolder.setChn(holdersRightApp.get(counter).getHolder().getChn());
                    newHolder.setHolderAcctNumber(holdersRightApp.get(counter).getHolderAPRAccountNo());
                    subscribingHolders_list.add(right_model);
                    //set in body of response object

                }
                if (!checkHCA) {
                    HolderCompanyAccount hca_model = new HolderCompanyAccount();
                    hca_model.setClientCompanyId(holdersRightApp.get(counter).getClearingHouseId());
                    hca_model.setHolderCompAccPrimary(true);
                    hca_model.setHolderId(counter);//note to verify source of holder's Id
                    hca_model.setShareUnits(holdersRightApp.get(counter).getSharesSubscribed());
                    hca_model.setMerged(false);
                    //set into body of response object and ask if to include list of HCA in rightsissueapplication model
                } //ends here
                else if (checkHolders && checkHCA) {
                    org.greenpole.hibernate.entity.RightsIssue right = hd.getRightsIssueById(holdersRightApp.get(counter).getRightsIssueId(), holdersRightApp.get(counter).getClearingHouseId());
                    if (hd.checkRightsIssue(right.getId(), right.getClientCompany().getId())) {
                        //if()
                        List<org.greenpole.hibernate.entity.RightsIssueApplication> rightsIssueApp_hib_list = hd.getAllRightsIssueApplications(right.getClientCompany().getId(), right.getId());
                        for (org.greenpole.hibernate.entity.RightsIssueApplication rp : rightsIssueApp_hib_list) {
                            sumAmountPaid_hib += rp.getAmountPaid();
                            noOfHoldersShares_hib += rp.getSharesSubscribed();
                            allotedHolderRights += rp.getAllottedRights();
                            availableRights = right.getTotalSharesOnIssue() - noOfHoldersShares_hib;//note: this reduction is based on what is left for right minus previous sub.
                            if (rp.getSharesSubscribed() == 0) {//set this field in setup rights issue to zero
                                valueOfSharesSub += sumOfSharesSubscribe * right.getIssuePrice();//to be use to check third condition of this requirement
                                additionalSharesAwaitingSub = right.getTotalSharesOnIssue() - sumOfSharesSubscribe;
                                if (sumAmountPaid_model == valueOfSharesSub) {//check if amount paid equals system calculation of share value
                                    RightsIssueApplication rightApp_model = new RightsIssueApplication();
                                    //rightApp_model.setApplicationType();
                                    rightApp_model.setIssuer(right.getClientCompany().getName());
                                    rightApp_model.setTotalRightsAvailable(availableRights);//assuming before subscription
                                    rightApp_model.setTotalSharesSubscribed((int)sumOfSharesSubscribe);//is total shares sub (to include this current one?)
                                    rightApp_model.setValueOfAdditionalShares(valueOfSharesSub);
                                    rightApp_model.setAdditionalSharesAwaitingSub(additionalSharesAwaitingSub);
                                    rightApp_model.setValueOfAdditionalShares(additionalSharesAwaitingSub * right.getIssuePrice());//value of additional shares awaiting subscription
                                    rightApp_model.setTotalValue(noOfHoldersShares_hib * right.getIssuePrice()
                                            + additionalSharesAwaitingSub * right.getIssuePrice() + sumOfSharesSubscribe * right.getIssuePrice());
                                    //rightApp_model.setTotalSharesRenounced(right.getTotalSharesOnIssue() - (int)sumOfSharesSubscribe);//asume shares remainging after this subscription to be looked into after this pull request
                                    renouncedShares = (right.getTotalSharesOnIssue() - sumOfSharesSubscribe);
                                    //rightApp_model.setIssuingHouse(right.geti);
                                    rightApp_model.setReturnMoney(valueOfSharesSub);
                                    subscribingHolders_list.add(rightApp_model);
                                    resp.setBody(subscribingHolders_list);
                                    return resp;
                                }
                                resp.setRetn(300);
                                resp.setDesc("Unable to perform transaction because total amount paid does not equal system's calculation of the value shares subscribing");
                                logger.info("Unable to perform transaction because total amount paid does not equal system's calculation of the value shares subscribing");
                                return resp;
                            }
                            resp.setRetn(300);
                            resp.setDesc("Unable to perform transaction because one or some of the subscribers has subscribed before");
                            logger.info("Unable to perform transaction because one or some of the subscribers has subscribed before");
                            return resp;

                        }//ends inner loop for rights application

                    }
                    resp.setRetn(300);
                    resp.setDesc("No rights issue found for this client company");
                    logger.info("No rights issue found for this client company");
                    return resp;
                }
                //resp.setRetn(300); commented out error since new account is to be created
                //resp.setDesc("Holder company account for one or some holders does not exist");
            }

        } catch (Exception ex) {
            logger.info("error processing upload rights issue en-mass. See error log - [{}]", login.getUserId());
            logger.error("error processing upload rights issue - [" + login.getUserId() + "]", ex);
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process upload rights issue request. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
        return resp;
    }
/**
 * Processes request to upload rights issue en - mass
 * @param login the user details
 * @param authenticator the user to receive the notification
 * @param holdersRightApp the holders details applying for the rights issue
 * @return response to the rights issue application
 */
    public Response uploadShareholderRightsIssueEnmass_Request(Login login, String authenticator, List<RightsIssueApplication> holdersRightApp) {
        logger.info("request to upload shareholders rights issue en-mass invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        NotifierProperties prop;
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        long millis = System.currentTimeMillis();
        Date current_date = new java.sql.Date(millis);
        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            int sumAppliedShares = 0, remShares = 0;
            int counter, sumOfSharesSubscribe = 0;
            boolean checkHolder = false;
            boolean checkHolderCompAcct = false;
            org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(holdersRightApp.get(0).getClientCompanyId());
            if (hd.checkRightsIssue(holdersRightApp.get(0).getRightsIssueId(), holdersRightApp.get(0).getClientCompanyId())) {
                org.greenpole.hibernate.entity.RightsIssue ri = hd.getRightsIssueById(holdersRightApp.get(0).getRightsIssueId(), holdersRightApp.get(0).getClientCompanyId());
                if (!ri.getRightsClosed() && ri.getClosingDate().before(current_date)) {
                    List<org.greenpole.hibernate.entity.RightsIssueApplication> rightsApp_hib_list = hd.getAllRightsIssueApplications(holdersRightApp.get(0).getClientCompanyId(), holdersRightApp.get(0).getRightsIssueId());
                    for (counter = 0; counter <= holdersRightApp.size(); counter++) {
                        sumOfSharesSubscribe += holdersRightApp.get(counter).getSharesSubscribed();
                        checkHolder = hq.checkHolderAccount(holdersRightApp.get(counter).getHolder().getHolderId());
                        checkHolderCompAcct = hq.checkHolderCompanyAccount(holdersRightApp.get(counter).getHolder().getHolderId(), holdersRightApp.get(counter).getClientCompanyId());
                        if (!checkHolder) {
                            break;
                        }
                        if (!checkHolderCompAcct) {
                            break;
                        }
                    }
                    if (checkHolder) {
                        if (checkHolderCompAcct) {
                            if (ri.getTotalSharesOnIssue() >= sumOfSharesSubscribe) {
                                wrapper = new NotificationWrapper();
                                prop = new NotifierProperties(HolderComponentLogic.class);
                                qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                                        prop.getAuthoriserNotifierQueueName());
                                wrapper.setCode(notification.createCode(login));
                                wrapper.setDescription("Authenticate application of rights issue en-mass under the client company " + cc.getName() + " invoked by user " + login.getUserId());
                                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                                wrapper.setFrom(login.getUserId());
                                wrapper.setTo(authenticator);
                                wrapper.setModel(holdersRightApp);
                                logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                                resp = qSender.sendAuthorisationRequest(wrapper);
                                return resp;
                            }
                            resp.setRetn(300);
                            resp.setDesc("Remaining shares is less than the subscribed shares");
                            logger.info("Remaining shares is less than the subscribed shares invoked by - " + login.getUserId());
                            return resp;
                        }
                        resp.setRetn(301);
                        resp.setDesc("No company account found for holder [" + holdersRightApp.get(counter).getHolder().getFirstName()
                                + " " + holdersRightApp.get(counter).getHolder().getLastName() + "].");
                        logger.info("No company account found for holder so cannot be apply for rights issue - [{}]: [{}]",
                                holdersRightApp.get(counter).getHolder().getFirstName()
                                + " " + holdersRightApp.get(counter).getHolder().getLastName(), login.getUserId());
                        return resp;
                    }
                    resp.setRetn(301);
                    resp.setDesc("Holder [" + holdersRightApp.get(counter).getHolder().getFirstName()
                            + " " + holdersRightApp.get(counter).getHolder().getLastName() + "] does not exist so cannot apply for rights issue.");
                    logger.info("Holder is does not exist so cannot apply for rights issue - [{}]: [{}]",
                            holdersRightApp.get(counter).getHolder().getFirstName()
                            + " " + holdersRightApp.get(counter).getHolder().getLastName(), login.getUserId());
                    return resp;
                }
                resp.setRetn(300);
                resp.setDesc("Sorry, rights issue has been closed");
                logger.info("Sorry, rights issue has been closed invoked by " + login.getUserId());
                return resp;
            }
            resp.setRetn(300);
            resp.setDesc("Rights issue does not exist for this client company");
            logger.info("Rights issue does not exist for the client company invoked by " + login.getUserId());
            return resp;
            // }
        } catch (Exception ex) {
            logger.info("error processing upload rights issue en-mass. See error log - [{}]", login.getUserId());
            logger.error("error processing upload rights issue - [" + login.getUserId() + "]", ex);
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process upload rights issue request. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
/**
 * Processes a request for the authorisation of rights issue application en - mass
 * @param login the user details
 * @param notificationCode the notification code
 * @return response to the authorisation request
 */
    public Response uploadShareholderRightsIssueEnmass_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        logger.info("authorise upload of rights application en-mass, invoked by [{}]", login.getUserId());
        Notification notification = new Notification();
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        long millis = System.currentTimeMillis();
        Date current_date = new java.sql.Date(millis);
        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<RightsIssueApplication> rightsIssueAppList = (List<RightsIssueApplication>) wrapper.getModel();
            RightsIssueApplication rightApp_model = rightsIssueAppList.get(0);
            boolean checkHolder = false;
            boolean checkHolderCompAcct = false;
            int counter;
            int sumAppliedShares_hib = 0;
            long remShares = 0;
            int sumOfSharesSubscribe_model = 0;
            org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(rightsIssueAppList.get(0).getClientCompanyId());
            if (hd.checkRightsIssue(rightsIssueAppList.get(0).getRightsIssueId(), rightsIssueAppList.get(0).getClientCompanyId())) {
                org.greenpole.hibernate.entity.RightsIssue ri = hd.getRightsIssueById(rightsIssueAppList.get(0).getRightsIssueId(), rightsIssueAppList.get(0).getClientCompanyId());
                if (!ri.getRightsClosed() && ri.getClosingDate().before(current_date)) {
                    List<org.greenpole.hibernate.entity.RightsIssueApplication> rightsApp_hib_list = hd.getAllRightsIssueApplications(rightsIssueAppList.get(0).getClientCompanyId(), rightsIssueAppList.get(0).getRightsIssueId());
                    for (counter = 0; counter <= rightsIssueAppList.size(); counter++) {
                        sumOfSharesSubscribe_model += rightsIssueAppList.get(counter).getSharesSubscribed();
                        checkHolder = hq.checkHolderAccount(rightsIssueAppList.get(counter).getHolder().getHolderId());
                        checkHolderCompAcct = hq.checkHolderCompanyAccount(rightsIssueAppList.get(counter).getHolder().getHolderId(), rightsIssueAppList.get(counter).getClientCompanyId());
                        if (!checkHolder) {
                            break;
                        }
                        if (!checkHolderCompAcct) {
                            break;
                        }
                    }
                    if (checkHolder) {
                        if (checkHolderCompAcct) {
                            if (ri.getTotalSharesOnIssue() >= sumOfSharesSubscribe_model) {
                                boolean updated = hd.uploadRightsApplicationEnmass(retrieveRightsApplication(rightsIssueAppList));//this is actually an update of the existing application
                                remShares = ri.getTotalSharesOnIssue() - sumOfSharesSubscribe_model;//get remaining shares after subscription
                                ri.setTotalSharesOnIssue(remShares);
                                hd.updateRightIssueSetup(ri.getId());
                                if (updated) {
                                    notification.markAttended(notificationCode);
                                    logger.info("Rights issue application upload authorised - [{}]", login.getUserId());
                                    resp.setRetn(0);
                                    resp.setDesc("Successful");
                                    return resp;
                                }
                                resp.setRetn(206);
                                resp.setDesc("Unable to upload rights issue application from authorisation. Contact System Administrator");
                                logger.info("Unable to upload rights issue application from authorisation - [{}]", login.getUserId());
                                return resp;
                            }
                            resp.setRetn(300);
                            resp.setDesc("Remaining shares is less than the subscribed shares");
                            logger.info("Remaining shares is less than the subscribed shares invoked by - " + login.getUserId());
                            return resp;
                        }
                        resp.setRetn(301);
                        resp.setDesc("No company account found for holder [" + rightsIssueAppList.get(counter).getHolder().getFirstName()
                                + " " + rightsIssueAppList.get(counter).getHolder().getLastName() + "].");
                        logger.info("No company account found for holder so cannot be apply for rights issue - [{}]: [{}]",
                                rightsIssueAppList.get(counter).getHolder().getFirstName()
                                + " " + rightsIssueAppList.get(counter).getHolder().getLastName(), login.getUserId());
                        return resp;
                    }
                    resp.setRetn(301);
                    resp.setDesc("Holder [" + rightsIssueAppList.get(counter).getHolder().getFirstName()
                            + " " + rightsIssueAppList.get(counter).getHolder().getLastName() + "] does not exist so cannot apply for rights issue.");
                    logger.info("Holder is does not exist so cannot apply for rights issue - [{}]: [{}]",
                            rightsIssueAppList.get(counter).getHolder().getFirstName()
                            + " " + rightsIssueAppList.get(counter).getHolder().getLastName(), login.getUserId());
                    return resp;
                }
                resp.setRetn(300);
                resp.setDesc("Sorry, rights issue has been closed");
                logger.info("Sorry, rights issue has been closed invoked by " + login.getUserId());
                return resp;
            }
            resp.setRetn(300);
            resp.setDesc("Rights issue does not exist for this client company");
            logger.info("Rights issue does not exist for the client company invoked by " + login.getUserId());
            return resp;
        } catch (Exception ex) {
        }
        return resp;
    }

    /**
     * Unwraps the list rights issue application models to create the list
     * hibernate rights issue application entities.
     *
     * @param rightsIssueAppList the list of share quotation models
     * @return the list hibernate rights issue application entities
     */
    private List<org.greenpole.hibernate.entity.RightsIssueApplication> retrieveRightsApplication(List<RightsIssueApplication> rightsIssueAppList) {
        List<org.greenpole.hibernate.entity.RightsIssueApplication> rightsList_hib = new ArrayList<>();
        org.greenpole.hibernate.entity.RightsIssueApplication rights_hib = new org.greenpole.hibernate.entity.RightsIssueApplication();
        int qualifySharesCheck;
        if (rightsIssueAppList != null && !rightsIssueAppList.isEmpty()) {//guard against null list, to avoid null pointer exception
            for (RightsIssueApplication ri : rightsIssueAppList) {
                org.greenpole.hibernate.entity.RightsIssue rights = hd.getRightsIssueById(ri.getRightsIssueId(), ri.getClientCompanyId());
                if (ri.getSharesSubscribed() > ri.getAllottedRights()) {
                    rights_hib.setSharesSubscribed(ri.getSharesSubscribed());
                    rights_hib.setAdditionalSharesSubscribed(ri.getAdditionalSharesSubscribed() - ri.getAllottedRights());
                    rights_hib.setAdditionalSharesSubValue((ri.getAdditionalSharesSubscribed() - ri.getAllottedRights()) * rights.getIssuePrice());
                    rights_hib.setProcessingPayment(true);
                    rightsList_hib.add(rights_hib);
                } else if (ri.getSharesSubscribed() <= ri.getAllottedRights()) {
                    qualifySharesCheck = ri.getSharesSubscribed() % rights.getQualifyShareUnit();
                    if (qualifySharesCheck == 0) {
                        rights_hib.setSharesSubscribed(ri.getSharesSubscribed());
                        rights_hib.setAmountPaid(ri.getAmountPaid());
                        rights_hib.setAdditionalSharesSubscribed(0);
                        rights_hib.setAdditionalSharesSubValue(0.0);
                        rights_hib.setProcessingPayment(true);
                        rightsList_hib.add(rights_hib);
                    }

                }

            }
        }
        return rightsList_hib;
    }
}
