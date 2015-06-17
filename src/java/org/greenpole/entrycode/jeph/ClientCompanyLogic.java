/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph;

import org.greenpole.entirycode.jeph.model.Dividend;
import org.greenpole.entirycode.jeph.model.Reconstruction;
import org.greenpole.entity.model.holder.HolderBondAccount;
import org.greenpole.entirycode.jeph.model.ConfirmationDetails;
import org.greenpole.entirycode.jeph.model.ShareBonus;
import org.greenpole.entirycode.jeph.model.DividendDeclared;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBException;
import org.greenpole.entirycode.jeph.model.BondOfferReport;
import org.greenpole.entirycode.jeph.model.QueryCorporateAction;
import org.greenpole.entity.model.Address;
import org.greenpole.entity.model.EmailAddress;
import org.greenpole.entity.model.PhoneNumber;
import org.greenpole.entity.model.taguser.TagUser;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.entity.tags.AddressTag;
import org.greenpole.entrycode.emmanuel.model.QueryDividend;
import org.greenpole.hibernate.entity.BondOffer;
import org.greenpole.hibernate.entity.ClientCompany;
import org.greenpole.hibernate.entity.HolderCompanyAccount;
import org.greenpole.hibernate.entity.HolderCompanyAccountId;
import org.greenpole.hibernate.entity.HolderEmailAddress;
import org.greenpole.hibernate.entity.HolderPhoneNumber;
import org.greenpole.hibernate.entity.HolderPostalAddress;
import org.greenpole.hibernate.entity.HolderResidentialAddress;
import org.greenpole.hibernate.entity.HolderType;
import org.greenpole.hibernate.entity.ShareQuotation;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.HolderComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.util.Descriptor;
import org.greenpole.util.Notification;
import org.greenpole.util.properties.GreenpoleProperties;
import org.greenpole.util.properties.NotificationProperties;
import org.greenpole.util.properties.NotifierProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jephthah Sadare
 */
public class ClientCompanyLogic {

    private final HolderComponentQuery hq = ComponentQueryFactory.getHolderComponentQuery();
    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();
    private final GreenpoleProperties greenProp = new GreenpoleProperties(ClientCompanyLogic.class);
    NotificationProperties noteProp = new NotificationProperties(ClientCompanyLogic.class);
    private static final Logger logger = LoggerFactory.getLogger(ClientCompanyLogic.class);
    SimpleDateFormat formatter = new SimpleDateFormat();

    /**
     * Processes request to view generated report on client company's bond offer
     *
     * @param login the user's login details
     * @param bondOfferReport Bond offer Report object
     * @return response to the View Generated Report request
     */
    public Response viewBondOfferReport_Request(Login login, BondOfferReport bondOfferReport) {
        Response resp = new Response();
        logger.info("Request to view Bond Offer report, invoked by [{}]", login.getUserId());
        try {
            if (bondOfferReport.getClientCompanyId() > 0 && bondOfferReport.getClientCompanyId() != 0) {
                if (bondOfferReport.getTitle() != null && !"".equals(bondOfferReport.getTitle())) {
                    // BondOffer chkBondOffer = cq.checkBondOffer(bondOfferReport.getClientCompanyId(), bondOfferReport.getId());
                    if (true) {
                        // List<BondOffer> bo = cq.getBondOffer(bondOfferReport.getClientCompanyId(), bondOfferReport.getId());
                        List<BondOffer> bondOfferList = new ArrayList<>();
                        List<BondOfferReport> bondOfferReportList = new ArrayList<>();
                        for (BondOffer bo : bondOfferList) {
                            bondOfferReport.setBondUnitPrice(bo.getBondUnitPrice());
                            bondOfferReport.setBondMaturity(bo.getBondMaturity().toString());
                            // bondOfferReport.setBondType(bo.getBondType());
                            // List<Holder> holder = cq.getAllBondHolders(bondOfferReport.getId());
                            List<org.greenpole.entity.model.holder.Holder> holder = new ArrayList<>();
                            bondOfferReport.setHolder(holder);
                            bondOfferReportList.add(bondOfferReport);
                        }
                        resp.setBody(bondOfferReportList);
                        resp.setRetn(0);
                        resp.setDesc("Generated Bond Offer Report");
                        logger.info("Generated Bond Offer Report. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Bond Offers do not exist for client company");
                    logger.info("Bond Offers do not exist for client company. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Bond title used to search for bonds is empty");
                logger.info("Bond title used to search for bonds is empty. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Client company id is not specified.");
            logger.info("Client company id is not specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (NullPointerException ex) {
            resp.setRetn(99);
            resp.setDesc("General Error. Unable to generate bond offer report. Contact system administrator." + "\nMessage: " + ex.getMessage());
            logger.info("Error generating bond offer report. See error log [{}] - [{}]", resp.getRetn(), login.getUserId());
            logger.error("Error generating bond offer report, invoked by [" + login.getUserId() + "] - ", ex);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General Error. Unable to generate bond offer report. Contact system administrator." + "\nMessage: " + ex.getMessage());
            logger.info("Error generating bond offer report. See error log [{}] - [{}]", resp.getRetn(), login.getUserId());
            logger.error("Error generating bond offer report, invoked by [" + login.getUserId() + "] - ", ex);
            return resp;
        }
    }

    /**
     * Processes request to declare share bonus for client company
     *
     * @param login the user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param shareBonus Share Bonus object
     * @return response to the Declare Share Bonus Request
     */
    public Response declareShareBonus_Request(Login login, String authenticator, ShareBonus shareBonus) {
        Response resp = new Response();
        Notification notification = new Notification();
        Date date = new Date();
        logger.info("Request to declare share bonus, invoked by [{}]", login.getUserId());

        try {
            NotificationWrapper wrapper;
            org.greenpole.notifier.sender.QueueSender queue;
            NotifierProperties props;
            List<ShareBonus> shareBonusList = new ArrayList();
            if (shareBonus.getTitle() != null && !"".equals(shareBonus.getTitle())) {
                if (shareBonus.getQualifyShareUnit() > 0) {
                    if (shareBonus.getBonusUnitPerQualifyUnit() > 0) {
                        if (shareBonus.getQualifyDate() != null && date.before(formatter.parse(shareBonus.getQualifyDate()))) {
                            wrapper = new NotificationWrapper();
                            props = new NotifierProperties(ClientCompanyLogic.class);
                            queue = new org.greenpole.notifier.sender.QueueSender(props.getAuthoriserNotifierQueueFactory(), props.getAuthoriserNotifierQueueName());

                            shareBonusList.add(shareBonus);

                            wrapper.setCode(notification.createCode(login));
                            wrapper.setDescription("Authenticate Share Bonus Declaration process for shareholders of client company " + shareBonus.getClientCompanyId());
                            wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
                            wrapper.setFrom(login.getUserId());
                            wrapper.setTo(authenticator);
                            wrapper.setModel(shareBonusList);
                            resp = queue.sendAuthorisationRequest(wrapper);
                            logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                            resp.setRetn(0);
                            // send SMS and/or Email notification
                            return resp;
                        } else if (shareBonus.getQualifyDate() != null) {
                            wrapper = new NotificationWrapper();
                            props = new NotifierProperties(ClientCompanyLogic.class);
                            queue = new org.greenpole.notifier.sender.QueueSender(props.getAuthoriserNotifierQueueFactory(), props.getAuthoriserNotifierQueueName());

                            shareBonusList.add(shareBonus);

                            wrapper.setCode(notification.createCode(login));
                            wrapper.setDescription("Authenticate Share Bonus Declaration process for shareholders of client company " + shareBonus.getClientCompanyId());
                            wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
                            wrapper.setFrom(login.getUserId());
                            wrapper.setTo(authenticator);
                            wrapper.setModel(shareBonusList);
                            resp = queue.sendAuthorisationRequest(wrapper);
                            logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                            resp.setRetn(0);
                            // send SMS and/or Email notification
                            return resp;
                        } // date not specified
                        resp.setRetn(200);
                        resp.setDesc("Qualify date is not specified.");
                        logger.info("Qualify date is not specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                        return resp;
                    } // bonus unit per qualify unit is not specified
                    resp.setRetn(200);
                    resp.setDesc("Bonus unit per qualify unit is not specified.");
                    logger.info("Bonus unit per qualify unit is not specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                } // resp qualify share unit cannot be less than zero
                resp.setRetn(200);
                resp.setDesc("Qualify share unit cannot be less than zero.");
                logger.info("Qualify share unit cannot be less than zero. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            } // resp share bonus cannot be empty
            resp.setRetn(200);
            resp.setDesc("Share bonus from client company cannot to empty.");
            logger.info("Share bonus from client company cannot to empty. - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process 'declare share bonus' request. Contact system administrator.");
            logger.info("Error processing 'declare share bonus' request. See error log - [{}]", login.getUserId());
            logger.error("Error processing 'declare share bonus' request - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to declare share bonus for client company that has been
     * saved as a notification file, according to the specified notification
     * code
     *
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the declare Share Bonus request
     */
    public Response declareShareBonus_Authorise(Login login, String notificationCode) {
        logger.info("Authorise reverse stock split, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        SimpleDateFormat formatter = new SimpleDateFormat();
        Date date = new Date();

        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(noteProp.getNotificationLocation(), notificationCode);
            List<ShareBonus> shareBonusList = (List<ShareBonus>) wrapper.getModel();
            ShareBonus shareBonus = shareBonusList.get(0);
            boolean checkShareHolders = cq.checkClientCompanyForShareholders(Integer.toString(shareBonus.getClientCompanyId()));
            if (checkShareHolders) {
                if (shareBonus.getTitle() != null && !"".equals(shareBonus.getTitle())) {
                    if (shareBonus.getQualifyShareUnit() > 0) {
                        if (shareBonus.getBonusUnitPerQualifyUnit() > 0) {
                            if (shareBonus.getQualifyDate() != null && date.after(formatter.parse(shareBonus.getQualifyDate()))) {
                                // List<HolderCompanyAccount> holderCompAcctList = cq.getAllClientCompanyAccount(shareBonus.getClientCompanyId());
                                List<HolderCompanyAccount> holderCompAcctList = new ArrayList();
                                List<HolderCompanyAccount> holderCompAcctListSend = new ArrayList();
                                for (HolderCompanyAccount hca : holderCompAcctList) {
                                    hca.setShareUnits((int) Math.floor(hca.getShareUnits() / shareBonus.getQualifyShareUnit()) * shareBonus.getBonusUnitPerQualifyUnit());
                                    holderCompAcctListSend.add(hca);
                                }
                                // hq.createUpdateHolderCompanyAccount(holderCompAcctListSend);
                                resp.setRetn(0);
                                resp.setDesc("Declare Share Bonus process successful.");
                                logger.info("Declare Share Bonus process successful. [{}] - [{}]", login.getUserId(), resp.getRetn());
                                wrapper.setAttendedTo(true);
                                notification.markAttended(notificationCode);
                                // send SMS and/or Email notification
                                return resp;
                            }
                            resp.setRetn(200);
                            resp.setDesc("Qualify date is not specified.");
                            logger.info("Qualify date is not specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                            return resp;
                        } // bonus unit per qualify unit is not specified
                        resp.setRetn(200);
                        resp.setDesc("Bonus unit per qualify unit is not specified.");
                        logger.info("Bonus unit per qualify unit is not specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                        return resp;
                    } // resp qualify share unit cannot be less than zero
                    resp.setRetn(200);
                    resp.setDesc("Qualify share unit cannot be less than zero.");
                    logger.info("Qualify share unit cannot be less than zero. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                } // resp share bonus cannot be empty
                resp.setRetn(200);
                resp.setDesc("Share bonus from client company cannot to empty.");
                logger.info("Share bonus from client company cannot to empty. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            } // check for shareholders
            resp.setRetn(200);
            resp.setDesc("There are no shareholders for the client company.");
            logger.info("There are no shareholders for the client company. - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process 'declare share bonus' request. Contact system administrator.");
            logger.info("Error processing 'declare share bonus' request. See error log - [{}]", login.getUserId());
            logger.error("Error processing 'declare share bonus' request - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request validation to apply stock split (share reconstruction)
     *
     * @param login the user's login details
     * @param reconstructor Reconstruction object
     * @return response to the Apply Stock Split request
     */
    public Response applyStockSplit_Request(Login login, Reconstruction reconstructor) {
        Response resp = new Response();
        Notification notification = new Notification();
        logger.info("Request to apply stock split, invoked by [{}]", login.getUserId());

        try { // code needs to be modified to consider fractional shares account (APR)
            if (cq.checkClientCompany(reconstructor.getClientCompanyId())) {
                if (cq.checkClientCompanyForShareholders(cq.getClientCompany(reconstructor.getClientCompanyId()).getName())) {
                    // ShareQuotation shareQuoteEntity = cq.getClientCompanyShareQuotation(reconstructor.getClientCompanyId());
                    ShareQuotation shareQuoteEntity = new ShareQuotation();
                    double shareUnitPriceBefore = 0;
                    double shareUnitPriceAfter = 0;
                    double actionShareUnit = 0;
                    double unqualifiedShares = 0;
                    BigDecimal totalShareVolumeBefore = BigDecimal.valueOf(0);
                    BigDecimal totalShareVolumeAfter = BigDecimal.valueOf(0);
                    if (reconstructor.getActionShareUnit() > 0 && reconstructor.getQualifyShareUnit() > 0) {
                        // List<HolderCompanyAccount> holderCompAcctList = cq.getAllClientCompanyAccount(reconstructor.getClientCompanyId());
                        HolderCompanyAccount holderCompAcct = new HolderCompanyAccount();
                        List<HolderCompanyAccount> holderCompAcctList = new ArrayList<>();
                        List<HolderCompanyAccount> holderCompAcctListSend = new ArrayList<>();
                        // List<Holder> holderList = cq.getAllHolders();
                        for (HolderCompanyAccount hca : holderCompAcctList) {
                            actionShareUnit += Math.floor(hca.getShareUnits() / reconstructor.getActionShareUnit());
                            totalShareVolumeBefore.add(BigDecimal.valueOf(hca.getShareUnits()));
                            unqualifiedShares += (hca.getShareUnits() % reconstructor.getActionShareUnit());
                            totalShareVolumeAfter.add(BigDecimal.valueOf(actionShareUnit));
                        }
                        shareUnitPriceBefore = shareQuoteEntity.getUnitPrice();
                        shareUnitPriceAfter = (shareQuoteEntity.getUnitPrice() / reconstructor.getQualifyShareUnit()) * reconstructor.getActionShareUnit();

                        reconstructor.setShareVolumeBefore(totalShareVolumeBefore.toString());
                        reconstructor.setUnitPriceBefore(Double.toString(shareUnitPriceBefore));
                        reconstructor.setShareVolumeAfter(totalShareVolumeAfter.toString());
                        reconstructor.setUnitPriceAfter(Double.toString(shareUnitPriceAfter));
                        // reconstructor.setHolders(holderCompAcctListSend);
                        List<Reconstruction> reconstructionList = new ArrayList<>();
                        reconstructionList.add(reconstructor);

                        resp.setRetn(0);
                        resp.setDesc("Confirmation for Apply Stock Split process.");
                        resp.setBody(reconstructionList);
                        logger.info("Confirmation for Apply Stock Split process. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                        // send SMS and/or Email notification
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Share ratio is not speccified");
                    logger.info("Share ratio is not speccified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Client company does not have shareholders and so corporate action can not be taken.");
                logger.info("Client company does not have shareholders and so corporate action can not be taken. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Client company does not exist and so corporate action can not be taken.");
            logger.info("Client company does not exist and so corporate action can not be taken. - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process apply stock split request. Contact system administrator.");
            logger.info("Error processing apply stock split request. See error log - [{}]", login.getUserId());
            logger.error("Error processing apply stock split request - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request confirmation to Apply Stock Split
     *
     * @param login the user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param reconstructor Reconstruction object
     * @return response to the Apply Stock Split request
     */
    public Response applyStockSplitConfirmation_Request(Login login, String authenticator, Reconstruction reconstructor) {
        Response resp = new Response();
        Notification notification = new Notification();
        logger.info("Request confirmation to apply stock split, invoked by [{}]", login.getUserId());

        try { // code needs to be modified to consider fractional shares account (APR)
            NotificationWrapper wrapper;
            org.greenpole.notifier.sender.QueueSender queue;
            NotifierProperties props;

            if (cq.checkClientCompany(reconstructor.getClientCompanyId())) {
                if (cq.checkClientCompanyForShareholders(cq.getClientCompany(reconstructor.getClientCompanyId()).getName())) {
                    // ShareQuotation shareQuoteEntity shareUnitPriceBefore = cq.getClientCompanyShareQuotation(reconstructor.getClientCompanyId());
                    ShareQuotation shareQuoteEntity = new ShareQuotation();
                    double shareUnitPriceBefore = 0;
                    double shareUnitPriceAfter = 0;
                    double actionShareUnit = 0;
                    double qualifySharesUnit = 0;
                    double unqualifiedShares = 0;
                    BigDecimal totalShareVolumeBefore = BigDecimal.valueOf(0);
                    BigDecimal totalShareVolumeAfter = BigDecimal.valueOf(0);
                    if (reconstructor.getActionShareUnit() > 0 && reconstructor.getQualifyShareUnit() > 0) {
                        // List<HolderCompanyAccount> holderCompAcctList = cq.getAllClientCompanyAccount(reconstructor.getClientCompanyId());
                        HolderCompanyAccount holderCompAcct = new HolderCompanyAccount();
                        List<HolderCompanyAccount> holderCompAcctList = new ArrayList();
                        List<HolderCompanyAccount> holderCompAcctListSend = new ArrayList();
                        // List<Holder> holderList = cq.getAllHolders();
                        for (HolderCompanyAccount hca : holderCompAcctList) {
                            actionShareUnit += Math.floor(hca.getShareUnits() / reconstructor.getActionShareUnit());
                            totalShareVolumeBefore.add(BigDecimal.valueOf(hca.getShareUnits()));
                            unqualifiedShares += (hca.getShareUnits() % reconstructor.getActionShareUnit());
                            totalShareVolumeAfter.add(BigDecimal.valueOf(actionShareUnit));
                        }
                        shareUnitPriceBefore = shareQuoteEntity.getUnitPrice();
                        shareUnitPriceAfter = (shareQuoteEntity.getUnitPrice() / reconstructor.getQualifyShareUnit()) * reconstructor.getActionShareUnit();

                        reconstructor.setShareVolumeBefore(totalShareVolumeBefore.toString());
                        reconstructor.setUnitPriceBefore(Double.toString(shareUnitPriceBefore));
                        reconstructor.setShareVolumeAfter(totalShareVolumeAfter.toString());
                        reconstructor.setUnitPriceAfter(Double.toString(shareUnitPriceAfter));
                        // reconstructor.setHolders(holderCompAcctListSend);
                        List<Reconstruction> reconstructionList = new ArrayList<>();
                        reconstructionList.add(reconstructor);

                        wrapper = new NotificationWrapper();
                        props = new NotifierProperties(ClientCompanyLogic.class);
                        queue = new org.greenpole.notifier.sender.QueueSender(props.getAuthoriserNotifierQueueFactory(), props.getAuthoriserNotifierQueueName());

                        wrapper.setCode(notification.createCode(login));
                        wrapper.setDescription("Authenticate Apply Stock Split confirmation process for " + reconstructor.getClientCompanyId());
                        wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
                        wrapper.setFrom(login.getUserId());
                        wrapper.setTo(authenticator);
                        wrapper.setModel(reconstructionList);
                        resp = queue.sendAuthorisationRequest(wrapper);
                        logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                        resp.setRetn(0);
                        // send SMS and/or Email notification
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Share ratio is not speccified");
                    logger.info("Share ratio is not speccified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Client company does not have shareholders and so corporate action can not be taken.");
                logger.info("Client company does not have shareholders and so corporate action can not be taken. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Client company does not exist and so corporate action can not be taken.");
            logger.info("Client company does not exist and so corporate action can not be taken. - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process apply stock split confirmation request. Contact system administrator.");
            logger.info("Error processing apply stock split confirmation request. See error log - [{}]", login.getUserId());
            logger.error("Error processing apply stock split confirmation request - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to Apply Stock Split that has been saved as a
     * notification file, according to the specified notification code;
     *
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the Apply Stock Spit request
     */
    public Response applyStockSplit_Authorise(Login login, String notificationCode) {
        logger.info("Authorise apply stock split request, invoked by [{}]", login.getUserId());
        Notification notification = new Notification();
        Response resp = new Response();

        try { // code needs to be modified to consider fractional shares account (APR)
            NotificationWrapper wrapper = notification.loadNotificationFile(noteProp.getNotificationLocation(), notificationCode);
            List<Reconstruction> reconstructorList = (List<Reconstruction>) wrapper.getModel();
            Reconstruction reconstructor = reconstructorList.get(0);

            if (cq.checkClientCompany(reconstructor.getClientCompanyId())) {
                if (cq.checkClientCompanyForShareholders(cq.getClientCompany(reconstructor.getClientCompanyId()).getName())) {
                    // ShareQuotation shareQuoteEntity = cq.getClientCompanyShareQuotation(reconstructor.getClientCompanyId());
                    ShareQuotation shareQuoteEntity = new ShareQuotation();
                    // double shareUnitPriceBefore = 0;
                    double shareUnitPriceAfter = 0;
                    int unqualifiedShares = 0;
                    BigDecimal totalShareVolumeBefore = BigDecimal.valueOf(0);
                    BigDecimal totalShareVolumeAfter = BigDecimal.valueOf(0);
                    if (reconstructor.getActionShareUnit() > 0 && reconstructor.getQualifyShareUnit() > 0) {
                        // List<HolderCompanyAccount> holderCompAcctList = cq.getAllClientCompanyAccount(reconstructor.getClientCompanyId());
                        List<HolderCompanyAccount> holderCompAcctList = new ArrayList();
                        List<HolderCompanyAccount> holderCompAcctListSend = new ArrayList();
                        for (HolderCompanyAccount hca : holderCompAcctList) {
                            hca.setShareUnits(hca.getShareUnits() + (int) (Math.floor(hca.getShareUnits() / reconstructor.getActionShareUnit()) * reconstructor.getActionShareUnit()));
                            unqualifiedShares += (hca.getShareUnits() % reconstructor.getQualifyShareUnit());
                            holderCompAcctListSend.add(hca);
                        }
                        /**
                         * apply stock split of unqualifiedShares to the
                         * fractional shares account yet to be identified.
                         */
                        // shareUnitPriceBefore = shareQuoteEntity.getUnitPrice();
                        shareUnitPriceAfter = (shareQuoteEntity.getUnitPrice() / reconstructor.getQualifyShareUnit()) * reconstructor.getQualifyShareUnit();
                        ClientCompany cc = new ClientCompany();
                        cc.setId(reconstructor.getClientCompanyId());

                        shareQuoteEntity.setUnitPrice(shareUnitPriceAfter);
                        // org.greenpole.hibernate.entity.ReconstructionType reconTypeEntity = cq.getReconstructionType(reconstructor.getReconstructionTypeId());
                        org.greenpole.hibernate.entity.ReconstructionType reconTypeEntity = new org.greenpole.hibernate.entity.ReconstructionType();
                        org.greenpole.hibernate.entity.Reconstruction reconstructionEntity = new org.greenpole.hibernate.entity.Reconstruction();
                        reconstructionEntity.setClientCompany(cc);
                        reconstructionEntity.setReconstructionType(reconTypeEntity);
                        // cq.updateClientHolderShareQuotation(holderCompAcctListSend, shareQuoteEntitySend, reconstructionEntity);
                        resp.setRetn(0);
                        resp.setDesc("Apply Stock Split authorisation process successful");
                        logger.info("Apply Stock Split authorisaton process successful. [{}] - [{}]", login.getUserId(), resp.getRetn());
                        wrapper.setAttendedTo(true);
                        notification.markAttended(notificationCode);
                        // send SMS and/or Email notification
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Share ratio is not speccified");
                    logger.info("Share ratio is not speccified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Client company does not have shareholders and so corporate action can not be taken.");
                logger.info("Client company does not have shareholders and so corporate action can not be taken. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Client company does not exist and so corporate action can not be taken.");
            logger.info("Client company does not exist and so corporate action can not be taken. - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (JAXBException ex) {
            resp.setRetn(98);
            resp.setDesc("Unable to process apply stock split authorisation request. Contact System Administrator");
            logger.info("Error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("Error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process apply stock split authorisaton request. Contact system administrator.");
            logger.info("Error processing apply stock split authorisaton request. See error log - [{}]", login.getUserId());
            logger.error("Error processing apply stock split authorisation request - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    // re-implement
    /**
     * Processes request validation for Apply Reverse Stock Split (Share
     * reconstruction)
     *
     * @param login the user's login details
     * @param reconstructor Reconstruction object
     * @return response to the Apply Reverse Stock Split request validation
     */
    public Response reverseStockSplit_Request(Login login, Reconstruction reconstructor) {
        Response resp = new Response();
        logger.info("Request to reverse stock split, invoked by [{}]", login.getUserId());

        try {
            if (cq.checkClientCompany(reconstructor.getClientCompanyId())) {
                if (cq.checkClientCompanyForShareholders(cq.getClientCompany(reconstructor.getClientCompanyId()).getName())) {
                    // ShareQuotation shareQuoteEntity = cq.getClientCompanyShareQuotation(reconstructor.getClientCompanyId());
                    ShareQuotation shareQuoteEntity = new ShareQuotation();
                    double shareUnitPriceBefore = 0;
                    double shareUnitPriceAfter = 0;
                    double actionShareUnit = 0;
                    double unqualifiedShares = 0;
                    BigDecimal totalShareVolumeBefore = BigDecimal.valueOf(0);
                    BigDecimal totalShareVolumeAfter = BigDecimal.valueOf(0);
                    if (reconstructor.getActionShareUnit() > 0 && reconstructor.getQualifyShareUnit() > 0) {
                        // List<HolderCompanyAccount> holderCompAcctList = cq.getAllClientCompanyAccount(reconstructor.getClientCompanyId());
                        HolderCompanyAccount holderCompAcct = new HolderCompanyAccount();
                        List<HolderCompanyAccount> holderCompAcctList = new ArrayList();
                        List<HolderCompanyAccount> holderCompAcctListSend = new ArrayList();
                        // List<Holder> holderList = cq.getAllHolders();
                        for (HolderCompanyAccount hca : holderCompAcctList) {
                            actionShareUnit += Math.floor(hca.getShareUnits() / reconstructor.getActionShareUnit());
                            totalShareVolumeBefore.add(BigDecimal.valueOf(hca.getShareUnits()));
                            unqualifiedShares += (hca.getShareUnits() % reconstructor.getActionShareUnit());
                            totalShareVolumeAfter.add(BigDecimal.valueOf(actionShareUnit));
                        }
                        shareUnitPriceBefore = shareQuoteEntity.getUnitPrice();
                        shareUnitPriceAfter = (shareQuoteEntity.getUnitPrice() / reconstructor.getQualifyShareUnit()) * reconstructor.getActionShareUnit();

                        reconstructor.setShareVolumeBefore(totalShareVolumeBefore.toString());
                        reconstructor.setUnitPriceBefore(Double.toString(shareUnitPriceBefore));
                        reconstructor.setShareVolumeAfter(totalShareVolumeAfter.toString());
                        reconstructor.setUnitPriceAfter(Double.toString(shareUnitPriceAfter));
                        // reconstructor.setHolders(holderCompAcctListSend);
                        List<Reconstruction> reconstructionList = new ArrayList<>();
                        reconstructionList.add(reconstructor);

                        resp.setRetn(0);
                        resp.setDesc("Confirmation for Reverse Stock Split process.");
                        resp.setBody(reconstructionList);
                        logger.info("Confirmation for Reverse Stock Split process. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                        // send SMS and/or Email notification
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Share ratio is not speccified");
                    logger.info("Share ratio is not speccified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Client company does not have shareholders and so corporate action can not be taken.");
                logger.info("Client company does not have shareholders and so corporate action can not be taken. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Client company does not exist and so corporate action can not be taken.");
            logger.info("Client company does not exist and so corporate action can not be taken. - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process apply stock split request. Contact system administrator.");
            logger.info("Error processing apply stock split request. See error log - [{}]", login.getUserId());
            logger.error("Error processing apply stock split request - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    // re-implement
    /**
     * Processes request confirmation for Apply Reverse Stock Split (Share
     * reconstruction)
     *
     * @param login the user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param reconstructor Reconstruction object
     * @return response to the Apply Reverse Stock Split request confirmation
     */
    public Response reverseStockSplitConfirmation_Request(Login login, String authenticator, Reconstruction reconstructor) {
        Response resp = new Response();
        Notification notification = new Notification();
        logger.info("Request confirmation for reverse stock split, invoked by [{}]", login.getUserId());

        try {
            NotificationWrapper wrapper;
            org.greenpole.notifier.sender.QueueSender queue;
            NotifierProperties props;

            if (cq.checkClientCompany(reconstructor.getClientCompanyId())) {
                if (cq.checkClientCompanyForShareholders(cq.getClientCompany(reconstructor.getClientCompanyId()).getName())) {
                    // ShareQuotation shareQuoteEntity shareUnitPriceBefore = cq.getClientCompanyShareQuotation(reconstructor.getClientCompanyId());
                    ShareQuotation shareQuoteEntity = new ShareQuotation();
                    double shareUnitPriceBefore = 0;
                    double shareUnitPriceAfter = 0;
                    double actionShareUnit = 0;
                    double qualifySharesUnit = 0;
                    double unqualifiedShares = 0;
                    BigDecimal totalShareVolumeBefore = BigDecimal.valueOf(0);
                    BigDecimal totalShareVolumeAfter = BigDecimal.valueOf(0);
                    if (reconstructor.getActionShareUnit() > 0 && reconstructor.getQualifyShareUnit() > 0) {
                        // List<HolderCompanyAccount> holderCompAcctList = cq.getAllClientCompanyAccount(reconstructor.getClientCompanyId());
                        HolderCompanyAccount holderCompAcct = new HolderCompanyAccount();
                        List<HolderCompanyAccount> holderCompAcctList = new ArrayList();
                        List<HolderCompanyAccount> holderCompAcctListSend = new ArrayList();
                        // List<Holder> holderList = cq.getAllHolders();
                        for (HolderCompanyAccount hca : holderCompAcctList) {
                            actionShareUnit += Math.floor(hca.getShareUnits() / reconstructor.getActionShareUnit());
                            totalShareVolumeBefore.add(BigDecimal.valueOf(hca.getShareUnits()));
                            unqualifiedShares += (hca.getShareUnits() % reconstructor.getActionShareUnit());
                            totalShareVolumeAfter.add(BigDecimal.valueOf(actionShareUnit));
                        }
                        shareUnitPriceBefore = shareQuoteEntity.getUnitPrice();
                        shareUnitPriceAfter = (shareQuoteEntity.getUnitPrice() / reconstructor.getQualifyShareUnit()) * reconstructor.getActionShareUnit();

                        reconstructor.setShareVolumeBefore(totalShareVolumeBefore.toString());
                        reconstructor.setUnitPriceBefore(Double.toString(shareUnitPriceBefore));
                        reconstructor.setShareVolumeAfter(totalShareVolumeAfter.toString());
                        reconstructor.setUnitPriceAfter(Double.toString(shareUnitPriceAfter));
                        // reconstructor.setHolders(holderCompAcctListSend);
                        List<Reconstruction> reconstructionList = new ArrayList<>();
                        reconstructionList.add(reconstructor);

                        wrapper = new NotificationWrapper();
                        props = new NotifierProperties(ClientCompanyLogic.class);
                        queue = new org.greenpole.notifier.sender.QueueSender(props.getAuthoriserNotifierQueueFactory(), props.getAuthoriserNotifierQueueName());

                        wrapper.setCode(notification.createCode(login));
                        wrapper.setDescription("Authenticate Reverse Stock Split process for " + reconstructor.getClientCompanyId());
                        wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
                        wrapper.setFrom(login.getUserId());
                        wrapper.setTo(authenticator);
                        wrapper.setModel(reconstructionList);
                        resp = queue.sendAuthorisationRequest(wrapper);
                        logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                        resp.setRetn(0);
                        // send SMS and/or Email notification
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Share ratio is not speccified");
                    logger.info("Share ratio is not speccified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Client company does not have shareholders and so corporate action can not be taken.");
                logger.info("Client company does not have shareholders and so corporate action can not be taken. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Client company does not exist and so corporate action can not be taken.");
            logger.info("Client company does not exist and so corporate action can not be taken. - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process apply stock split request. Contact system administrator.");
            logger.info("Error processing apply stock split request. See error log - [{}]", login.getUserId());
            logger.error("Error processing apply stock split request - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    // re-implement
    /**
     * Processes request for Apply Reverse Stock Split that has been saved as a
     * notification file, according to the specified notification code
     *
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response object to the Apply Reverse Stock Split
     */
    public Response reverseStockSplit_Authorise(Login login, String notificationCode) {
        logger.info("Authorise reverse stock split, invoked by [{}]", login.getUserId());
        Notification notification = new Notification();
        Response resp = new Response();

        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(noteProp.getNotificationLocation(), notificationCode);
            List<Reconstruction> reconstructorList = (List<Reconstruction>) wrapper.getModel();
            Reconstruction reconstructor = reconstructorList.get(0);

            if (cq.checkClientCompany(reconstructor.getClientCompanyId())) {
                if (cq.checkClientCompanyForShareholders(cq.getClientCompany(reconstructor.getClientCompanyId()).getName())) {
                    // ShareQuotation shareQuoteEntity = cq.getClientCompanyShareQuotation(reconstructor.getClientCompanyId());
                    ShareQuotation shareQuoteEntity = new ShareQuotation();
                    // double shareUnitPriceBefore = 0;
                    double shareUnitPriceAfter = 0;
                    int unqualifiedShares = 0;
                    BigDecimal totalShareVolumeBefore = BigDecimal.valueOf(0);
                    BigDecimal totalShareVolumeAfter = BigDecimal.valueOf(0);
                    if (reconstructor.getActionShareUnit() > 0 && reconstructor.getQualifyShareUnit() > 0) {
                        // List<HolderCompanyAccount> holderCompAcctList = cq.getAllClientCompanyAccount(reconstructor.getClientCompanyId());
                        List<HolderCompanyAccount> holderCompAcctList = new ArrayList();
                        List<HolderCompanyAccount> holderCompAcctListSend = new ArrayList();
                        for (HolderCompanyAccount hca : holderCompAcctList) {
                            hca.setShareUnits(hca.getShareUnits() + (int) (Math.floor(hca.getShareUnits() / reconstructor.getActionShareUnit()) * reconstructor.getActionShareUnit()));
                            unqualifiedShares += (hca.getShareUnits() % reconstructor.getQualifyShareUnit());
                            holderCompAcctListSend.add(hca);
                        }
                        /**
                         * reverse stock split of unqualifiedShares to the
                         * fractional shares account yet to be identified.
                         */
                        // shareUnitPriceBefore = shareQuoteEntity.getUnitPrice();
                        shareUnitPriceAfter = (shareQuoteEntity.getUnitPrice() / reconstructor.getQualifyShareUnit()) * reconstructor.getQualifyShareUnit();

                        ClientCompany cc = new ClientCompany();
                        cc.setId(reconstructor.getClientCompanyId());

                        shareQuoteEntity.setUnitPrice(shareUnitPriceAfter);
                        // org.greenpole.hibernate.entity.ReconstructionType reconTypeEntity = cq.getReconstructionType(reconstructor.getReconstructionTypeId());
                        org.greenpole.hibernate.entity.ReconstructionType reconTypeEntity = new org.greenpole.hibernate.entity.ReconstructionType();
                        org.greenpole.hibernate.entity.Reconstruction reconstructionEntity = new org.greenpole.hibernate.entity.Reconstruction();
                        reconstructionEntity.setClientCompany(cc);
                        reconstructionEntity.setReconstructionType(reconTypeEntity);
                        // cq.updateClientHolderShareQuotation(holderCompAcctListSend, shareQuoteEntitySend, reconstructionEntity);
                        resp.setRetn(0);
                        resp.setDesc("Reverse Stock Split authorisation process successful");
                        logger.info("Reverse Stock Split authorisaton process successful. [{}] - [{}]", login.getUserId(), resp.getRetn());
                        wrapper.setAttendedTo(true);
                        notification.markAttended(notificationCode);
                        // send SMS and/or Email notification
                        return resp;
                    }
                    resp.setRetn(200);
                    resp.setDesc("Client company does not have shareholders and so corporate action can not be taken.");
                    logger.info("Client company does not have shareholders and so corporate action can not be taken. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Client company does not have shareholders and so corporate action can not be taken.");
                logger.info("Client company does not have shareholders and so corporate action can not be taken. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Client company does not exist and so corporate action can not be taken.");
            logger.info("Client company does not exist and so corporate action can not be taken. - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (JAXBException ex) {
            resp.setRetn(98);
            resp.setDesc("Unable to process 'reverse stock split' authorisation request. Contact System Administrator");
            logger.info("Error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("Error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process 'reverse stock split' authorisation request. Contact system administrator.");
            logger.info("Error processing 'reverse stock split' authorisation request. See error log - [{}]", login.getUserId());
            logger.error("Error processing 'reverse stock split' authorisation request - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to Upload Bond Offer Application en-mass
     *
     * @param login the user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param bondAccountList
     * @return response to the bond offer application request
     */
    public Response uploadBondOfferEnmass_Request(Login login, String authenticator, List<HolderBondAccount> bondAccountList) {
        logger.info("request to upload list of bond offer, invoked by [{}]", login.getUserId());
        List<Response> respList = new ArrayList<>();
        List<ConfirmationDetails> confirmationList = new ArrayList<>();
        ConfirmationDetails confirm = new ConfirmationDetails();
        Response resp = new Response();
        Notification notification = new Notification();

        try {
            NotificationWrapper wrapper;
            org.greenpole.notifier.sender.QueueSender queue;
            NotifierProperties prop;

            wrapper = new NotificationWrapper();
            prop = new NotifierProperties(ClientCompanyLogic.class);
            queue = new org.greenpole.notifier.sender.QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());

            wrapper.setCode(notification.createCode(login));

            for (HolderBondAccount bondAccount : bondAccountList) {
                Response response = new Response();
                response = validateHolderDetails(login, bondAccount.getHolder());
                if (hq.checkHolderAccount(bondAccount.getHolderId())) {// checking holder account with CHN is more appropriate
                    if (cq.bondOfferIsValid(bondAccount.getBondOfferId())) {
                        BondOffer bond = cq.getBondOffer(bondAccount.getBondOfferId());
                        respList.add(response);
                        wrapper.setDescription("Authenticate holder, " + bondAccount.getHolder().getFirstName() + " " + bondAccount.getHolder().getLastName()
                                + "'s application for the bond offer - " + bond.getTitle());
                    }
                    response.setDesc(response.getDesc() + "\nThe bond offer is no longer valid.");
                    logger.info("The bond offer is no longer valid - [{}]: [{}]", login.getUserId(), response.getRetn());
                    respList.add(response);
                    confirm.setDetails(respList);
                    confirmationList.add(confirm);
                }
                response.setDesc(response.getDesc() + "\nThe holder does not exist.");
                logger.info("The holder does not exist - [{}]", login.getUserId());
                respList.add(response);
                confirm.setDetails(respList);
                confirmationList.add(confirm);
            }
            wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
            wrapper.setFrom(login.getUserId());
            wrapper.setTo(authenticator);
            wrapper.setModel(bondAccountList);

            logger.info("notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
            resp = queue.sendAuthorisationRequest(wrapper);
            confirm.setDetails(respList);
            confirmationList.add(confirm);
            resp.setBody(confirmationList);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to proccess holder bond application. Contact system administrator.");
            logger.info("error proccessing holder bond applications. See error log - [{}]", login.getUserId());
            logger.error("error proccessing holder bond applications - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to Upload Bond Offer Application that has been saved in
     * a notification file, according to the specified notification code
     *
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the bond offer application request
     */
    public Response uploadBondOfferEnmass_Authorise(Login login, String notificationCode) {
        logger.info("request authorisation to persist holder details. Invoked by [{}]", login.getUserId());
        Response resp = new Response();
        List<ConfirmationDetails> confirmationList = new ArrayList<>();
        ConfirmationDetails confirm = new ConfirmationDetails();
        Response validBondApply;
        Response validHolder;
        List<Response> respList = new ArrayList<>();
        Notification notification = new Notification();

        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(noteProp.getNotificationLocation(), notificationCode);
            List<HolderBondAccount> acctList = (List<HolderBondAccount>) wrapper.getModel();
            List<HolderBondAccount> bondAccountList = (List<HolderBondAccount>) acctList.get(0);
            List<org.greenpole.hibernate.entity.HolderBondAccount> bondAccountListSend = new ArrayList<>();
            // TODO: more validation to be performed
            for (HolderBondAccount bondAccount : bondAccountList) {
                Response response = new Response();
                validBondApply = validateHolderDetails(login, bondAccount.getHolder());
                if (validBondApply.getRetn() == 0) {
                    if (hq.checkHolderAccount(bondAccount.getHolderId())) {// checking holder account with CHN is more appropriate
                        if (cq.bondOfferIsValid(bondAccount.getBondOfferId())) {
                            BondOffer bond = cq.getBondOffer(bondAccount.getBondOfferId());
                            org.greenpole.hibernate.entity.Holder holderEntity = hq.getHolder(bondAccount.getHolderId());
                            org.greenpole.hibernate.entity.HolderBondAccount bondAcct_hib = new org.greenpole.hibernate.entity.HolderBondAccount();

                            bondAcct_hib.setBondOffer(bond);
                            bondAcct_hib.setHolder(holderEntity);
                            bondAcct_hib.setBondUnits(bondAccount.getBondUnits());
                            bondAcct_hib.setStartingPrincipalValue(bondAccount.getStartingPrincipalValue());
                            bondAcct_hib.setRemainingPrincipalValue(0.00);
                            bondAcct_hib.setDateApplied(new Date());
                            bondAcct_hib.setHolderBondAcctPrimary(true);
                            bondAcct_hib.setMerged(false);

                            bondAccountListSend.add(bondAcct_hib);
                        }
                        response.setDesc(response.getDesc() + "\nThe bond offer is no longer valid.");
                        logger.info("The bond offer is no longer valid - [{}]: [{}]", login.getUserId(), response.getRetn());
                        respList.add(response);
                        confirm.setDetails(respList);
                        confirmationList.add(confirm);
                    }
                    response.setDesc(response.getDesc() + "\nThe holder does not exist.");
                    logger.info("The holder does not exist - [{}]", login.getUserId());
                    respList.add(response);
                    confirm.setDetails(respList);
                    confirmationList.add(confirm);
                }
            }
            // hq.createUpdateHolderBondAccount(bondAccountListSend);
            notification.markAttended(notificationCode);
            resp.setRetn(0);
            confirm.setDetails(respList);
            confirmationList.add(confirm);
            resp.setBody(confirmationList);
            resp.setDesc("Bond Offer application successful");
            logger.info("Bond Offer application successful - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error proccessing holder bond application. See error log - [{}]", login.getUserId());
            logger.error("error proccessing holder bond application - [" + login.getUserId() + "]", ex);
            resp.setRetn(99);
            resp.setDesc("General error. Unable to proccess holder bond application. Contact system administrator.");
            return resp;
        }
    }

    /**
     * Processes request to declare Dividend for Client company
     *
     * @param login the user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param dividendDeclared Dividend Declared object
     * @return
     */
    public Response declaredDividend_Request(Login login, String authenticator, DividendDeclared dividendDeclared) {
        logger.info("request to create declare dividend, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();

        try {
            NotificationWrapper wrapper;
            org.greenpole.notifier.sender.QueueSender queue;
            NotifierProperties props;

            Response res = validateDividendDeclaration(login, dividendDeclared);
            if (res.getRetn() != 0) {
                // send SMS and/or Email notification
                return res;
            }
            List<DividendDeclared> dividendDeclaredList = new ArrayList<>();
            dividendDeclaredList.add(dividendDeclared);

            wrapper = new NotificationWrapper();
            props = new NotifierProperties(ClientCompanyLogic.class);
            queue = new org.greenpole.notifier.sender.QueueSender(props.getAuthoriserNotifierQueueFactory(), props.getAuthoriserNotifierQueueName());

            wrapper.setCode(notification.createCode(login));
            wrapper.setDescription("Authenticate declare dividend process for " + dividendDeclared.getClientCompanyId());
            wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
            wrapper.setFrom(login.getUserId());
            wrapper.setTo(authenticator);
            wrapper.setModel(dividendDeclaredList);
            resp = queue.sendAuthorisationRequest(wrapper);
            logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
            resp.setRetn(0);
            // send SMS and/or Email notification
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process client company dividend declaration. Contact system administrator.");
            logger.info("Error processing client company creation. See error log - [{}]", login.getUserId());
            logger.error("Error processing client company creation - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to declare Dividend for Client company that has been
     * saved in a notification file, according to the specified notification
     * code
     *
     * @param login
     * @param notificationCode
     * @return
     */
    public Response declaredDividend_Authorise(Login login, String notificationCode) {
        logger.info("Authorise declare dividend process, invoked by [{}]", login.getUserId());
        Notification notification = new Notification();
        Response resp = new Response();

        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(noteProp.getNotificationLocation(), notificationCode);
            List<DividendDeclared> dividendDeclaredList = (List<DividendDeclared>) wrapper.getModel();
            DividendDeclared dividendDeclared = dividendDeclaredList.get(0);

            Response res = validateDividendDeclaration(login, dividendDeclared);
            if (res.getRetn() != 0) {
                // send SMS and/or Email notification
                return res;
            }
            boolean status = false;
            int returnStatus = 0;
            // status = cq.createClientCompanyDividend(dividendDeclared);
            if (status) {// create dividend
                returnStatus = createDividend(login, dividendDeclared).getRetn();
                if (returnStatus == 0) {
                    resp.setRetn(0);
                    resp.setDesc("Declare dividend authorisation process successful");
                    logger.info("Declare dividend authorisaton process successful. [{}] - [{}]", login.getUserId(), resp.getRetn());
                    wrapper.setAttendedTo(true);
                    notification.markAttended(notificationCode);
                    // send SMS and/or Email notification
                    return resp;
                }
            }
            resp.setRetn(200);
            resp.setDesc("Unable to process dividend creation request.");
            logger.info("Unable to process dividend creation request - [{}]", login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            resp.setRetn(98);
            resp.setDesc("Unable to process 'declared dividend' authorisation request. Contact System Administrator");
            logger.info("Error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("Error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process 'declared dividend' authorisation request. Contact system administrator.");
            logger.info("Error processing 'declared dividend' authorisation request. See error log - [{}]", login.getUserId());
            logger.error("Error processing 'declared dividend' authorisation request - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Generates a report to view corporate action on declared dividend
     *
     * @param login the user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param viewDividendDeclared QueryCorporateAction object
     * @return response to the view corporate action dividend report process
     */
    public Response viewCorporateActionDividendReport_Request(Login login, String authenticator, QueryCorporateAction viewDividendDeclared) {
        logger.info("Request to view generated report on declared dividend [{}], invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Descriptor descriptorUtil = new Descriptor();

        try {
            if (viewDividendDeclared.getDescriptor() == null || "".equals(viewDividendDeclared.getDescriptor())) {
                resp.setRetn(300);
                resp.setDesc("View corporate action report on declared dividend unsuccessful, empty descriptor found.");
                logger.info("View corporate action report on declared dividend unsuccessful. Empty descriptor - [{}]", login.getUserId());
                return resp;
            }

            Map<String, String> descriptors = descriptorUtil.decipherDescriptor(viewDividendDeclared.getDescriptor());
            if (descriptors.size() == 1) {
                //check start date is properly formatted
                if (descriptors.get("date").equalsIgnoreCase("none")) {
                    try {
                        formatter.parse(viewDividendDeclared.getStartDate());
                    } catch (ParseException ex) {
                        logger.info("an error was thrown while checking the start date. See error log invoked by [{}]", login.getUserId());
                        resp.setRetn(300);
                        resp.setDesc("Incorrect date format for start date");
                        logger.error("Incorrect date format for start date invoked by [{}]", login.getUserId(), ex);

                        return resp;
                    }
                }
                // List<org.greenpole.hibernate.entity.DividendDeclared> viewDividendList = cq.getAllDeclaredDividend(viewDividend.getDescriptor(), 
                //           viewDividend.getStartDate(), greenProp.getDateFormat());

                List<org.greenpole.hibernate.entity.DividendDeclared> viewDividendDeclaredEntityList = new ArrayList<>();
                List<DividendDeclared> declaredDividendModelList = new ArrayList<>();
                TagUser tag = new TagUser();

                for (org.greenpole.hibernate.entity.DividendDeclared divDeclrd : viewDividendDeclaredEntityList) {
                    DividendDeclared veiwDividendDeclared = new DividendDeclared();

                    veiwDividendDeclared.setClientCompanyId(divDeclrd.getClientCompany().getId());
                    veiwDividendDeclared.setYearType(divDeclrd.getYearType());
                    veiwDividendDeclared.setIssueType(divDeclrd.getIssueType());
                    veiwDividendDeclared.setQualifyDate(divDeclrd.getQualifyDate().toString());
                    veiwDividendDeclared.setWithholdingTaxRateInd(divDeclrd.getWithholdingTaxRateInd());
                    veiwDividendDeclared.setWithholdingTaxRateCorp(divDeclrd.getWithholdingTaxRateCorp());
                    veiwDividendDeclared.setYearEnding(divDeclrd.getYearEnding());
                    veiwDividendDeclared.setDatePayable(divDeclrd.getDatePayable().toString());
                    veiwDividendDeclared.setRate(divDeclrd.getRate());

                    List<org.greenpole.entity.model.holder.Holder> holderListSend = new ArrayList<>();
                    List<org.greenpole.hibernate.entity.Holder> holderList = new ArrayList<>();
                    // List<org.greenpole.hibernate.entity.Holder> holderList = cq.getAllHolders(dividendDeclared.getClientCompanyId());
                    for (org.greenpole.hibernate.entity.Holder h : holderList) {
                        org.greenpole.entity.model.holder.Holder hold = new org.greenpole.entity.model.holder.Holder();
                        hold.setHolderAcctNumber(h.getHolderAcctNumber());
                        hold.setChn(h.getChn());
                        hold.setFirstName(h.getFirstName());
                        hold.setMiddleName(h.getMiddleName());
                        hold.setLastName(h.getLastName());
                        hold.setGender(h.getGender());
                        hold.setDob(h.getDob().toString());
                        hold.setTaxExempted(h.getTaxExempted());
                        hold.setPryAddress(h.getPryAddress());

                        holderListSend.add(hold);
                    }
                    veiwDividendDeclared.setHolders(holderListSend);
                    declaredDividendModelList.add(veiwDividendDeclared);
                }
                List<TagUser> tagList = new ArrayList<>();

                tag.setQueryParam(viewDividendDeclared);
                tag.setResult(declaredDividendModelList);
                tagList.add(tag);

                resp.setBody(tagList);
                resp.setDesc("Generation of Report on Corporate Action: Declared Dividend Successful");
                resp.setRetn(0);
                logger.info("Generation of Report on Corporate Action: Declared Dividend Successful - [{}]", login.getUserId());
                return resp;
            }
            logger.info("Descriptor length does not match expected required length - [{}]", login.getUserId());
            resp.setRetn(330);
            resp.setDesc("Descriptor length does not match expected required length");
            return resp;

        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to view generated report on declared dividends. Contact system administrator.");
            logger.info("Error generating report on declared dividends. See error log - [{}]", login.getUserId());
            logger.error("Error generating report on declared dividends - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Generates a report to view corporate action on share bonus
     *
     * @param login the user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param viewShareBonus QueryCorporateAction object
     * @return response to the view corporate action bonus report process
     */
    public Response viewCorporateActionBonusReport_Request(Login login, String authenticator, QueryCorporateAction viewShareBonus) {
        logger.info("Request to view generated report on declared share bonus [{}], invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Descriptor descriptorUtil = new Descriptor();

        try {
            if (viewShareBonus.getDescriptor() == null || "".equals(viewShareBonus.getDescriptor())) {
                resp.setRetn(300);
                resp.setDesc("View corporate action report on declared bonus unsuccessful, empty descriptor found.");
                logger.info("View corporate action report on declared bonus unsuccessful. Empty descriptor - [{}]", login.getUserId());
                return resp;
            }

            Map<String, String> descriptors = descriptorUtil.decipherDescriptor(viewShareBonus.getDescriptor());
            if (descriptors.size() == 1) {
                //check start date is properly formatted
                if (descriptors.get("date").equalsIgnoreCase("none")) {
                    try {
                        formatter.parse(viewShareBonus.getStartDate());
                    } catch (ParseException ex) {
                        logger.info("an error was thrown while checking the start date. See error log invoked by [{}]", login.getUserId());
                        resp.setRetn(300);
                        resp.setDesc("Incorrect date format for start date");
                        logger.error("Incorrect date format for start date invoked by [{}]", login.getUserId(), ex);

                        return resp;
                    }
                }
                // List<org.greenpole.hibernate.entity.ShareBonus> viewShareBonusEntityList = cq.getAllShareBonus(viewDividend.getDescriptor(), 
                //           viewDividend.getStartDate(), greenProp.getDateFormat());

                List<org.greenpole.hibernate.entity.ShareBonus> viewShareBonusEntityList = new ArrayList<>();
                List<ShareBonus> shareBonusModelList = new ArrayList<>();
                TagUser tag = new TagUser();

                for (org.greenpole.hibernate.entity.ShareBonus bonus : viewShareBonusEntityList) {
                    ShareBonus viewBonus = new ShareBonus();

                    viewBonus.setClientCompanyId(bonus.getClientCompany().getId());
                    viewBonus.setTitle(bonus.getTitle());
                    viewBonus.setQualifyDate(bonus.getQualifyDate().toString());
                    viewBonus.setQualifyShareUnit(bonus.getQualifyShareUnit());
                    viewBonus.setBonusUnitPerQualifyUnit(bonus.getBonusUnitPerQualifyUnit());

                    List<org.greenpole.entity.model.holder.Holder> holderListSend = new ArrayList<>();
                    List<org.greenpole.hibernate.entity.Holder> holderList = new ArrayList<>();
                    // List<org.greenpole.hibernate.entity.Holder> holderList = cq.getAllHolders(viewShareBonus.getClientCompanyId());
                    for (org.greenpole.hibernate.entity.Holder h : holderList) {
                        org.greenpole.entity.model.holder.Holder hold = new org.greenpole.entity.model.holder.Holder();
                        hold.setHolderAcctNumber(h.getHolderAcctNumber());
                        hold.setChn(h.getChn());
                        hold.setFirstName(h.getFirstName());
                        hold.setMiddleName(h.getMiddleName());
                        hold.setLastName(h.getLastName());
                        hold.setGender(h.getGender());
                        hold.setDob(h.getDob().toString());
                        hold.setTaxExempted(h.getTaxExempted());
                        hold.setPryAddress(h.getPryAddress());

                        holderListSend.add(hold);
                    }
                    viewBonus.setHolders(holderListSend);
                    shareBonusModelList.add(viewBonus);
                }
                List<TagUser> tagList = new ArrayList<>();

                tag.setQueryParam(viewShareBonus);
                tag.setResult(shareBonusModelList);
                tagList.add(tag);

                resp.setBody(tagList);
                resp.setDesc("Generation of Report on Corporate Action: Declared Share Bonus Successful");
                resp.setRetn(0);
                logger.info("Generation of Report on Corporate Action: Declared Share Bonus Successful - [{}]", login.getUserId());
                return resp;
            }
            logger.info("Descriptor length does not match expected required length - [{}]", login.getUserId());
            resp.setRetn(330);
            resp.setDesc("Descriptor length does not match expected required length");
            return resp;

        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to view generated report on Share Bonus dividends. Contact system administrator.");
            logger.info("Error generating report on declared Share Bonus. See error log - [{}]", login.getUserId());
            logger.error("Error generating report on declared Share Bonus - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to cancel shareholder dividend
     *
     * @param login the user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param dividend Dividend object
     * @return response to the Cancel Dividend request
     */
    public Response cancelDividend_Request(Login login, String authenticator, Dividend dividend) {
        logger.info("request to create declare dividend [{}], invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();

        try {
            NotificationWrapper wrapper;
            org.greenpole.notifier.sender.QueueSender queue;
            NotifierProperties props;

            Response res = checkDividendStatus(login, dividend);
            if (res.getRetn() != 0) {
                // send SMS and/or Email notification
                return res;
            }
            // set cancelled to true - authorise
            // set cancelledDate to present date - authorise
            List<Dividend> dividendList = new ArrayList<>();
            dividendList.add(dividend);

            wrapper = new NotificationWrapper();
            props = new NotifierProperties(ClientCompanyLogic.class);
            queue = new org.greenpole.notifier.sender.QueueSender(props.getAuthoriserNotifierQueueFactory(), props.getAuthoriserNotifierQueueName());

            wrapper.setCode(notification.createCode(login));
            wrapper.setDescription("Authenticate cancel dividend process for " + dividend.getClientCompanyId());
            wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
            wrapper.setFrom(login.getUserId());
            wrapper.setTo(authenticator);
            wrapper.setModel(dividendList);
            resp = queue.sendAuthorisationRequest(wrapper);
            logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
            resp.setRetn(0);
            // send SMS and/or Email notification
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process client company dividend declaration. Contact system administrator.");
            logger.info("Error processing client company creation. See error log - [{}]", login.getUserId());
            logger.error("Error processing client company creation - [" + login.getUserId() + "]", ex);
            return resp;
        }

    }

    /**
     * Processes request to cancel shareholder dividend that has been saved in a
     * notification file, according to the specified notification code
     *
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the Cancel Dividend request
     */
    public Response cancelDividend_Authorise(Login login, String notificationCode) {
        logger.info("Authorise cancel dividend process, invoked by [{}]", login.getUserId());
        Notification notification = new Notification();
        Response resp = new Response();
        Date date = new Date();

        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(noteProp.getNotificationLocation(), notificationCode);
            List<Dividend> dividendList = (List<Dividend>) wrapper.getModel();
            Dividend dividend = dividendList.get(0);

            Response res = checkDividendStatus(login, dividend);
            if (res.getRetn() != 0) {
                // send SMS and/or Email notification
                return res;
            }
            dividend.setCancelled(true);
            dividend.setCanelledDate(date.toString());
            // org.greenpole.hibernate.entity.Dividend dividendEntity = hq.getShareHolderDividend(dividend.getHolderCompanyAccountId());
            org.greenpole.hibernate.entity.Dividend dividendEntity = new org.greenpole.hibernate.entity.Dividend();
            dividendEntity.setCancelled(dividend.isCancelled());
            dividendEntity.setCanelledDate(formatter.parse(dividend.getCanelledDate()));

            boolean status = false;
            // status = hq.updateShareholderDividend(dividendEntity);
            if (status) {
                resp.setRetn(0);
                resp.setDesc("Cancel dividend authorisation process successful");
                logger.info("Cancel dividend authorisaton process successful. [{}] - [{}]", login.getUserId(), resp.getRetn());
                wrapper.setAttendedTo(true);
                notification.markAttended(notificationCode);
                // send SMS and/or Email notification
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Unable to process dividend cancellation authorisation.");
            logger.info("Unable to process dividend cancellation authorisation - [{}]", login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            resp.setRetn(98);
            resp.setDesc("Unable to process 'cancel dividend' authorisation request. Contact System Administrator");
            logger.info("Error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("Error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process 'cancel dividend' authorisation request. Contact system administrator.");
            logger.info("Error processing 'cancel dividend' authorisation request. See error log - [{}]", login.getUserId());
            logger.error("Error processing 'cancel dividend' authorisation request - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Generates a report of canceled dividend
     *
     * @param login the user's login details
     * @param queryCanceledDividend QueryCorporateAction object
     * @return response to the View Canceled Dividend Report query
     */
    public Response viewCanceledDividendReport_Request(Login login, QueryCorporateAction queryCanceledDividend) {
        logger.info("Request to view generated report on cancelled dividends [{}], invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Descriptor descriptorUtil = new Descriptor();

        try {

            Map<String, String> descriptors = descriptorUtil.decipherDescriptor(queryCanceledDividend.getDescriptor());
            if (descriptors.size() == 1) {
                //check start date is properly formatted
                if (descriptors.get("date").equalsIgnoreCase("none")) {
                    try {
                        formatter.parse(queryCanceledDividend.getStartDate());
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
                        formatter.parse(queryCanceledDividend.getEndDate());
                    } catch (ParseException ex) {
                        logger.info("an error was thrown while checking the start date. See error log - [{}]", login.getUserId());
                        resp.setRetn(300);
                        resp.setDesc("Incorrect date format for end date");
                        logger.error("Incorrect date format for end date - [{}]", login.getUserId(), ex);

                        return resp;
                    }
                }
                // List<org.greenpole.hibernate.entity.Dividend> canceledDividendList = hq.getAllCanceledDividend(queryCanceledDividend.getDescriptor(), 
                //           queryCanceledDividend.getStartDate(), queryCanceledDividend.getEndDate(), greenProp.getDateFormat());

                List<org.greenpole.hibernate.entity.Dividend> canceledDividendEntityList = new ArrayList<>();
                List<Dividend> canceledDividendLModelList = new ArrayList<>();
                TagUser tag = new TagUser();

                for (org.greenpole.hibernate.entity.Dividend div : canceledDividendEntityList) {
                    Dividend canceledDividend = new Dividend();
                    canceledDividend.setClientCompName(div.getClientCompName());
                    canceledDividend.setDividendDeclaredId(div.getDividendDeclared().getId());
                    // canceledDividend.setHolderCompanyAccountId();
                    canceledDividend.setWarrantNumber(div.getWarrantNumber().intValue());
                    canceledDividend.setIssueType(div.getIssueType());
                    canceledDividend.setIssueDate(div.getIssueDate().toString());
                    canceledDividend.setDivNumber(div.getDivNumber());
                    canceledDividend.setYearType(div.getYearType());
                    canceledDividend.setYearEnding(div.getYearEnding().toString());
                    canceledDividend.setSHolderMailingAddr(div.getSHolderMailingAddr());
                    canceledDividend.setRate(div.getRate());
                    canceledDividend.setCompAccHoldings(div.getCompAccHoldings());
                    canceledDividend.setWithldingTaxRate(div.getWithldingTaxRate());
                    canceledDividend.setGrossAmount(div.getGrossAmount());
                    canceledDividend.setTax(div.getTax());
                    canceledDividend.setPayableAmount(div.getPayableAmount());
                    canceledDividend.setPayableDate(div.getPayableDate().toString());
                    canceledDividend.setIssued(div.getIssued());
                    canceledDividend.setIssueDate(div.getIssueDate().toString());
                    // canceledDividend.setReIssued(div.getReIssued());
                    // canceledDividend.setReIssuedDate(div.getReIssuedDate().toString());
                    // canceledDividend.setPaid(div.getPaid());
                    // canceledDividend.setPaidDate(div.getPaidDate().toString());
                    // canceledDividend.setPaymentMethod(div.getPaymentMethod());
                    // canceledDividend.setUnclaimed(div.getUnclaimed());
                    // canceledDividend.setUnclaimedDate(div.getUnclaimedDate().toString());
                    // canceledDividend.setCancelled(div.getCancelled());
                    canceledDividend.setCanelledDate(div.getCanelledDate().toString());

                    canceledDividendLModelList.add(canceledDividend);
                }
                List<TagUser> tagList = new ArrayList<>();

                tag.setQueryParam(queryCanceledDividend);
                tag.setResult(canceledDividendLModelList);
                tagList.add(tag);

                resp.setBody(tagList);
                resp.setDesc("Canceled Dividend Query Report Generation Successful");
                resp.setRetn(0);
                logger.info("Canceled Dividend Query Report Generation - [{}]", login.getUserId());
                return resp;
            }
            logger.info("Descriptor length does not match expected required length - [{}]", login.getUserId());
            resp.setRetn(330);
            resp.setDesc("Descriptor length does not match expected required length");
            return resp;

        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to view generated report on cancelled dividends. Contact system administrator.");
            logger.info("Error generating report on cancelled dividends. See error log - [{}]", login.getUserId());
            logger.error("Error generating report on cancelled dividends - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to replace dividend warrant
     *
     * @param login the user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param dividend Dividend object
     * @return response to the replace dividend warrant request
     */
    public Response replaceDividendWarrant_Request(Login login, String authenticator, Dividend dividend) {
        logger.info("request to create declare dividend [{}], invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();

        try {
            NotificationWrapper wrapper;
            org.greenpole.notifier.sender.QueueSender queue;
            NotifierProperties props;

            Response res = checkDividendReplaced(login, dividend);
            if (res.getRetn() != 0) {
                // send SMS and/or Email notification
                return res;
            }
            List<Dividend> dividendList = new ArrayList<>();
            dividendList.add(dividend);

            wrapper = new NotificationWrapper();
            props = new NotifierProperties(ClientCompanyLogic.class);
            queue = new org.greenpole.notifier.sender.QueueSender(props.getAuthoriserNotifierQueueFactory(), props.getAuthoriserNotifierQueueName());

            wrapper.setCode(notification.createCode(login));
            wrapper.setDescription("Authenticate dividend warrant replacement process for " + dividend.getClientCompanyId());
            wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
            wrapper.setFrom(login.getUserId());
            wrapper.setTo(authenticator);
            wrapper.setModel(dividendList);
            resp = queue.sendAuthorisationRequest(wrapper);
            logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
            resp.setRetn(0);
            // send SMS and/or Email notification
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process dividend warrant replacement. Contact system administrator.");
            logger.info("Error processing dividend warrant replacement. See error log - [{}]", login.getUserId());
            logger.error("Error processing dividend warrant replacement - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to replace dividend warrant that has been saved in a
     * notification file, according to the specified notification code
     *
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the replace dividend warrant request
     */
    public Response replaceDividendWarrant_Authorise(Login login, String notificationCode) {
        logger.info("Authorise replace dividend warrant process, invoked by [{}]", login.getUserId());
        Notification notification = new Notification();
        Response resp = new Response();
        Date date = new Date();

        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(noteProp.getNotificationLocation(), notificationCode);
            List<Dividend> dividendList = (List<Dividend>) wrapper.getModel();
            Dividend dividend = dividendList.get(0);

            Response res = checkDividendStatus(login, dividend);
            if (res.getRetn() != 0) {
                // send SMS and/or Email notification
                return res;
            }
            dividend.setIssued(false);
            dividend.setReIssued(false);

            // org.greenpole.hibernate.entity.Dividend dividendEntity = hq.getShareHolderDividend(dividend.getHolderCompanyAccountId());
            org.greenpole.hibernate.entity.Dividend dividendEntity = new org.greenpole.hibernate.entity.Dividend();
            org.greenpole.hibernate.entity.DividenAnnotation dividendAnnotEntity = new org.greenpole.hibernate.entity.DividenAnnotation();

            Set dividendAnnot = new HashSet();

            dividendAnnotEntity.setAnnotation(dividend.getAnnotation());
            dividendEntity.setIssued(dividend.isIssued());
            dividendEntity.setReIssued(dividend.isReIssued());
            dividendAnnot.add(dividend.getAnnotation());
            dividendEntity.setDividenAnnotations(dividendAnnot);

            boolean status = false;
            // status = hq.updateShareholderDividend(dividendEntity);
            if (status) {
                resp.setRetn(0);
                resp.setDesc("Replace dividend warrant authorisation process successful");
                logger.info("Replace dividend warrant authorisaton process successful. [{}] - [{}]", login.getUserId(), resp.getRetn());
                wrapper.setAttendedTo(true);
                notification.markAttended(notificationCode);
                // send SMS and/or Email notification
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Unable to process replace dividend warrant authorisation.");
            logger.info("Unable to process replace dividend warrant authorisation - [{}]", login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            resp.setRetn(98);
            resp.setDesc("Unable to process 'replace dividend warrant' authorisation request. Contact System Administrator");
            logger.info("Error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("Error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process 'replace dividend warrant' authorisation request. Contact system administrator.");
            logger.info("Error processing 'replace dividend warrant' authorisation request. See error log - [{}]", login.getUserId());
            logger.error("Error processing 'replace dividend warrant' authorisation request - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes recreation of a new dividend which will retain all previous
     * information on a canceled dividend except with a new warrant number
     *
     * @param login the user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param dividend Dividend object
     * @return response to the recreate dividend request
     */
    public Response recreateDividend_Request(Login login, String authenticator, Dividend dividend) {
        logger.info("request to recreate dividend, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();

        try {
            NotificationWrapper wrapper;
            org.greenpole.notifier.sender.QueueSender queue;
            NotifierProperties props;

            Response res = verifyDividendDetails(login, dividend);
            if (res.getRetn() != 0) {
                // send SMS and/or Email notification
                return res;
            }
            List<Dividend> dividendList = new ArrayList<>();
            dividendList.add(dividend);

            wrapper = new NotificationWrapper();
            props = new NotifierProperties(ClientCompanyLogic.class);
            queue = new org.greenpole.notifier.sender.QueueSender(props.getAuthoriserNotifierQueueFactory(), props.getAuthoriserNotifierQueueName());

            wrapper.setCode(notification.createCode(login));
            wrapper.setDescription("Authenticate recreate dividend process for " + dividend.getClientCompanyId());
            wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
            wrapper.setFrom(login.getUserId());
            wrapper.setTo(authenticator);
            wrapper.setModel(dividendList);
            resp = queue.sendAuthorisationRequest(wrapper);
            logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
            resp.setRetn(0);
            // send SMS and/or Email notification
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process recreate dividend process. Contact system administrator.");
            logger.info("Error processing recreate dividend process. See error log - [{}]", login.getUserId());
            logger.error("Error processing recreate dividend process - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to recreate dividend that has been saved in a
     * notification file, according to the specified notification code
     *
     * @param login
     * @param notificationCode
     * @return
     */
    public Response recreateDividend_Authorise(Login login, String notificationCode) {
        logger.info("Authorise replace dividend warrant process, invoked by [{}]", login.getUserId());
        Notification notification = new Notification();
        Response resp = new Response();
        Date date = new Date();

        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(noteProp.getNotificationLocation(), notificationCode);
            List<Dividend> dividendList = (List<Dividend>) wrapper.getModel();
            Dividend dividend = dividendList.get(0);

            Response res = verifyDividendDetails(login, dividend);
            if (res.getRetn() != 0) {
                // send SMS and/or Email notification
                return res;
            }

            List<org.greenpole.hibernate.entity.Dividend> dividendEntityList = new ArrayList<>();
            org.greenpole.hibernate.entity.Dividend dividendEntity = new org.greenpole.hibernate.entity.Dividend();

            dividendEntity.setClientCompName(dividend.getClientCompName());
            dividendEntity.setIssueType(dividend.getIssueType());
            dividendEntity.setIssueDate(formatter.parse(dividend.getIssueDate()));
            dividendEntity.setDivNumber(dividend.getDivNumber());
            dividendEntity.setYearType(dividend.getYearType());
            // TODO: warranty number to be auto generated #important
            // dividendEntity.setWarrantNumber(warrantNumber);

            // dividendEntity.setHolderCompanyAccount(hq.getHolderCompanyAccount(dividend.getHolderCompanyAccountId()));
            dividendEntity.setSHolderMailingAddr(dividend.getSHolderMailingAddr());
            dividendEntity.setRate(dividend.getRate());
            dividend.setCompAccHoldings(dividend.getCompAccHoldings());
            dividendEntity.setWithldingTaxRate(dividend.getWithldingTaxRate());
            dividendEntity.setGrossAmount(dividend.getGrossAmount());
            dividendEntity.setTax(dividendEntity.getTax());
            dividendEntity.setPayableAmount(dividendEntity.getPayableAmount());
            dividendEntity.setIssued(dividend.isIssued());
            dividendEntity.setIssueDate(formatter.parse(dividend.getIssueDate()));
            dividendEntity.setPayableDate(formatter.parse(dividend.getPayableDate()));
            dividendEntityList.add(dividendEntity);

            boolean status = false;
            // status hq.createUpdateShareholderDividend(dividendEntityList);
            if (!status) {
                resp.setRetn(200);
                resp.setDesc("Dividends for shareholders NOT successful");
                logger.info("Dividends for shareholders NOT successful. [{}] - [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(0);
            resp.setDesc("Dividends for shareholders successful");
            logger.info("Dividends for shareholders successful. [{}] - [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process dividend creation for shareholders. Contact system administrator.");
            logger.info("Error processing dividend creation for shareholders. See error log - [{}]", login.getUserId());
            logger.error("Error processing dividend creation for shareholders - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Validates holder details
     *
     * @param login the user's login details
     * @param holder Holder object
     * @return response to the calling method
     */
    private Response validateHolderDetails(Login login, org.greenpole.entity.model.holder.Holder holder) {
        Response resp = new Response();

        String desc = "";
        boolean flag = false;
        if (hq.checkHolderAccount(holder.getHolderId())) {
            desc = "\nThe holder does not exist.";
        } else if (holder.getFirstName() == null || "".equals(holder.getFirstName())) {
            desc = "\nHolder first name should not be empty";
        } else if (holder.getLastName() == null || "".equals(holder.getLastName())) {
            desc += "\nHolder last name should not be empty";
        } else if (holder.getTypeId() <= 0) {
            desc += "\nHolder type should not be empty";
        } else if (holder.getPryAddress() == null || "".equals(holder.getPryAddress())) {
            desc += "\nPrimary Holder address is not specified";
        } else if (holder.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                || holder.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
            desc += "\nPrimary address can only be residential or postal";
        } else if (holder.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                && (holder.getResidentialAddresses() == null || holder.getResidentialAddresses().isEmpty())) {
            desc += "\nResidential address cannot be empty, as it is the primary address";
        } else if (holder.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString())
                && (holder.getPostalAddresses() == null || holder.getPostalAddresses().isEmpty())) {
            desc += "\nPostal address cannot be empty, as it is the primary address";
        } else {
            flag = true;
        }

        if (flag && (!"".equals(holder.getChn()) || holder.getChn() != null)) {
            if (hq.checkHolderAccount(holder.getChn())) {
                desc += "\nThe CHN already exists";
                flag = true;
            }
        }

        if (flag && holder.getTypeId() > 0) {
            boolean found = false;
            for (HolderType ht : hq.getAllHolderTypes()) {
                if (holder.getTypeId() == ht.getId()) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                desc += "\nHolder type is not valid";
                flag = false;
            }
        }

        if (flag && holder.getResidentialAddresses() != null && !holder.getResidentialAddresses().isEmpty()) {
            for (Address addr : holder.getResidentialAddresses()) {
                if (addr.getAddressLine1() == null || "".equals(addr.getAddressLine1())) {
                    desc += "\nAddress line 1 should not be empty. Delete entire address if you must";
                    flag = false;
                    break;
                } else if (addr.getState() == null || "".equals(addr.getState())) {
                    desc += "\nState should not be empty. Delete entire address if you must";
                    flag = false;
                    break;
                } else if (addr.getCountry() == null || "".equals(addr.getCountry())) {
                    desc += "\nCountry should not be empty. Delete entire address if you must";
                    flag = false;
                    break;
                }
            }
        }

        if (flag && holder.getPostalAddresses() != null && !holder.getPostalAddresses().isEmpty()) {
            for (Address addr : holder.getPostalAddresses()) {
                if (addr.getAddressLine1() == null || "".equals(addr.getAddressLine1())) {
                    desc += "\nAddress line 1 should not be empty. Delete entire address if you must";
                    flag = false;
                    break;
                } else if (addr.getState() == null || "".equals(addr.getState())) {
                    desc += "\nState should not be empty. Delete entire address if you must";
                    flag = false;
                    break;
                } else if (addr.getCountry() == null || "".equals(addr.getCountry())) {
                    desc += "\nCountry should not be empty. Delete entire address if you must";
                    flag = false;
                    break;
                }
            }
        }

        if (flag && holder.getEmailAddresses() != null && !holder.getEmailAddresses().isEmpty()) {
            for (EmailAddress email : holder.getEmailAddresses()) {
                if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
                    desc += "\nEmail address should not be empty. Delete email entry if you must";
                    flag = false;
                    break;
                }
            }
        }

        if (flag && holder.getPhoneNumbers() != null && !holder.getPhoneNumbers().isEmpty()) {
            for (PhoneNumber phone : holder.getPhoneNumbers()) {
                if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
                    desc += "\nPhone number should not be empty. Delete phone number entry if you must";
                    flag = false;
                    break;
                }
            }
        }

        if (flag) {
            resp.setRetn(0);
            resp.setDesc("Validation of holder details successful");
            logger.info("Validation of holder details successful - [{}] [{}]", login.getUserId(), resp.getRetn());
            return resp;
        }
        resp.setRetn(1);
        resp.setDesc("Error filing holder details: " + desc);
        logger.info("Error filing holder details: [{}] - [{}]", desc, login.getUserId());
        return resp;
    }

    /**
     * Creates Dividend for shareholders of a client company
     *
     * @param login the user's login details
     * @param dividendDeclared Dividend Declared object
     * @return response object to the calling method
     */
    private Response createDividend(Login login, DividendDeclared dividendDeclared) {
        Response resp = new Response();
        Date date = new Date();
        List<org.greenpole.hibernate.entity.Holder> holderList = new ArrayList<>();
        // List<org.greenpole.hibernate.entity.Holder> holderList = cq.getAllHolders(dividendDeclared.getClientCompanyId());
        List<org.greenpole.hibernate.entity.Dividend> dividendList = new ArrayList<>();
        try {
            for (org.greenpole.hibernate.entity.Holder holder : holderList) {
                org.greenpole.hibernate.entity.Dividend dividend = new org.greenpole.hibernate.entity.Dividend();

                dividend.setClientCompName(cq.getClientCompany(dividendDeclared.getClientCompanyId()).getName());
                dividend.setIssueType(dividendDeclared.getIssueType());
                dividend.setIssueDate(date);
                // dividend.setDivNumber(Integer.MIN_VALUE);
                dividend.setYearType(dividendDeclared.getYearType());
                // dividend.setWarrantNumber(warrantNumber);
                dividend.setHolderCompanyAccount(hq.getHolderCompanyAccount(holder.getId(), dividendDeclared.getClientCompanyId()));
                dividend.setSHolderMailingAddr(holder.getPryAddress());
                dividend.setRate(dividendDeclared.getRate());
                // dividend.setCompAccHoldings(Integer.MAX_VALUE);
                if (!holder.getTaxExempted()) {
                    if (holder.getHolderType().getId() == 1) {
                        dividend.setWithldingTaxRate(dividendDeclared.getWithholdingTaxRateInd());
                        dividend.setGrossAmount(dividendDeclared.getRate() * dividend.getCompAccHoldings());
                        dividend.setTax(dividend.getGrossAmount() * dividend.getWithldingTaxRate());
                        dividend.setPayableAmount(dividend.getGrossAmount() - dividend.getTax());
                    } else if (holder.getHolderType().getId() == 2) {
                        dividend.setWithldingTaxRate(dividendDeclared.getWithholdingTaxRateCorp());
                        dividend.setGrossAmount(dividendDeclared.getRate() * dividend.getCompAccHoldings());
                        dividend.setTax(dividend.getGrossAmount() * dividend.getWithldingTaxRate());
                        dividend.setPayableAmount(dividend.getGrossAmount() - dividend.getTax());
                    }
                } else {
                    if (holder.getHolderType().getId() == 1) {
                        dividend.setTax(0.0);
                        dividend.setPayableAmount(dividend.getGrossAmount());
                    } else if (holder.getHolderType().getId() == 2) {
                        dividend.setTax(0.0);
                        dividend.setPayableAmount(dividend.getGrossAmount());
                    }
                }
                dividend.setIssued(Boolean.TRUE);
                dividend.setIssueDate(date);
                dividend.setPayableDate(formatter.parse(dividendDeclared.getDatePayable()));
                dividendList.add(dividend);
            }
            boolean status = false;
            // status hq.createShareholderDividend(dividendList);
            if (!status) {
                resp.setRetn(200);
                resp.setDesc("Dividends for shareholders NOT successful");
                logger.info("Dividends for shareholders NOT successful. [{}] - [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(0);
            resp.setDesc("Dividends for shareholders successful");
            logger.info("Dividends for shareholders successful. [{}] - [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process dividend creation for shareholders. Contact system administrator.");
            logger.info("Error processing dividend creation for shareholders. See error log - [{}]", login.getUserId());
            logger.error("Error processing dividend creation for shareholders - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Verifies dividend details for processing Dividend recreation
     *
     * @param login the user's login details
     * @param dividend Dividend object
     * @return response to the dividend verification request
     */
    private Response verifyDividendDetails(Login login, Dividend dividend) {
        logger.info("process to verify dividend details, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        try {
            if (dividend.getClientCompanyId() > 0) {// check clientcompanyid
                if (dividend.getDividendDeclaredId() > 0) {// check dividend declared id
                    if (true) {// check dividend issue type id
                        if (dividend.getHolderCompanyAccountId() > 0) {// check holder company account id
                            if (dividend.getClientCompName() != null || !"".equals(dividend.getClientCompName())) {//check client company name
                                if (dividend.getIssueType() != null || !"".equals(dividend.getIssueType())) {// check issue type
                                    if (dividend.getIssueDate() != null || !"".equals(dividend.getIssueDate())) {// check issue date
                                        try {// check issue date format
                                            formatter.parse(dividend.getIssueDate());
                                        } catch (ParseException ex) {// issue date format is wrong
                                            resp.setRetn(200);
                                            resp.setDesc("Incorrect date format for issue date");
                                            logger.info("An error was thrown while checking the issue date format. See error log invoked by [{}] - [{}]", login.getUserId(), resp.getRetn());
                                            logger.error("Incorrect date format for issue date invoked by [{}]", login.getUserId(), ex);
                                            return resp;
                                        }
                                        if (dividend.getDivNumber() > 0) {// check dividend number
                                            if (dividend.getYearType() != null || !"".equals(dividend.getYearType())) {// check year type
                                                if (dividend.getYearEnding() != null || !"".equals(dividend.getYearEnding())) {// check year ending
                                                    if (dividend.getSHolderMailingAddr() != null || !"".equals(dividend.getSHolderMailingAddr())) {// check shareholder mailing address
                                                        if (dividend.getRate() > 0.0) {// check rate
                                                            if (dividend.getCompAccHoldings() > 0.0) {// check company account holdings
                                                                if (dividend.getWithldingTaxRate() > 0) {// check withholding tax rate
                                                                    if (dividend.getGrossAmount() > 0) {// gross amount
                                                                        if (true) {// tax
                                                                            if (dividend.getPayableAmount() > 0) {// check payable amount
                                                                                if (dividend.getPayableDate() != null || !"".equals(dividend.getPayableDate())) {// check payable date
                                                                                    try {
                                                                                        formatter.parse(dividend.getPayableDate());
                                                                                    } catch (ParseException ex) {
                                                                                        resp.setRetn(200);
                                                                                        resp.setDesc("Incorrect date format for payable date");
                                                                                        logger.info("An error was thrown while checking the payable date format. See error log invoked by [{}] - [{}]", login.getUserId(), resp.getRetn());
                                                                                        logger.error("Incorrect date format for payable date invoked by [{}]", login.getUserId(), ex);
                                                                                        return resp;
                                                                                    }
                                                                                    if (dividend.isCancelled()) {// checks if it is canceled
                                                                                        resp.setRetn(0);
                                                                                        return resp;
                                                                                    }// dividend is not canceled
                                                                                    resp.setRetn(200);
                                                                                    resp.setDesc("Dividend is not canceled");
                                                                                    logger.info("Dividend is not canceled. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                                                                    return resp;
                                                                                }// payable date is not specified
                                                                                resp.setRetn(200);
                                                                                resp.setDesc("Payable date is not specified");
                                                                                logger.info("Payable date is not specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                                                                return resp;
                                                                            }// payable amount is not specified
                                                                            resp.setRetn(200);
                                                                            resp.setDesc("Payable amount is not specified");
                                                                            logger.info("Payable amount is not specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                                                            return resp;
                                                                        }// tax amount is missing
                                                                        resp.setRetn(200);
                                                                        resp.setDesc("Tax amount is not specified");
                                                                        logger.info("Tax amount is not specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                                                        return resp;
                                                                    }// gross amount is not specified
                                                                    resp.setRetn(200);
                                                                    resp.setDesc("Gross amount is not specified");
                                                                    logger.info("Gross amount is not specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                                                    return resp;
                                                                }// withholding tax rate is not specified
                                                                resp.setRetn(200);
                                                                resp.setDesc("Withholding tax rate is not specified");
                                                                logger.info("Withholding tax rate is not specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                                                return resp;
                                                            }// company account holdings is not specified
                                                            resp.setRetn(200);
                                                            resp.setDesc("Company account holdings is not specified");
                                                            logger.info("Company account holdings is not specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                                            return resp;
                                                        }// rate is not specified
                                                        resp.setRetn(200);
                                                        resp.setDesc("Rate is not specified");
                                                        logger.info("Rate is not specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                                        return resp;
                                                    }// shareholder mailing address is not specified
                                                    resp.setRetn(200);
                                                    resp.setDesc("Shareholder mailing address is not specified");
                                                    logger.info("Shareholder mailing address is not specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                                    return resp;
                                                }// year ending is not specified
                                                resp.setRetn(200);
                                                resp.setDesc("Year ending is not specified");
                                                logger.info("Year ending is not specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                                return resp;
                                            }// year type is not specified
                                            resp.setRetn(200);
                                            resp.setDesc("Year type is not specified");
                                            logger.info("Year type is not specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                            return resp;
                                        }// dividend number is not specified
                                        resp.setRetn(200);
                                        resp.setDesc("Dividend number is not specified");
                                        logger.info("Dividend number is not specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                        return resp;
                                    }// issue date is not specified
                                    resp.setRetn(200);
                                    resp.setDesc("Issue date is not specified");
                                    logger.info("Issue date is not specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                    return resp;
                                }// issue type is not specified
                                resp.setRetn(200);
                                resp.setDesc("Issue type is not specified");
                                logger.info("Issue type is not specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                return resp;
                            }// client company name is not specified
                            resp.setRetn(200);
                            resp.setDesc("Client company name is not specified");
                            logger.info("Client company name is not specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                            return resp;
                        }// holder company account id is not specified
                        resp.setRetn(200);
                        resp.setDesc("Holder company account ID is not specified");
                        logger.info("Holder company account ID is not specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                        return resp;
                    }// dividend issue type id is not specified
                    resp.setRetn(200);
                    resp.setDesc("Dividend issue type ID is not specified");
                    logger.info("Dividend issue type ID is not specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }// dividend declared id is not specified
                resp.setRetn(200);
                resp.setDesc("Dividend declared ID is not specified");
                logger.info("Dividend declared ID is not specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }// clientcompanyid is not specified
            resp.setRetn(200);
            resp.setDesc("Client company ID is not specified");
            logger.info("Client company ID is not specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process dividend warrant verification. Contact system administrator.");
            logger.info("Error processing dividend warrant verification. See error log - [{}]", login.getUserId());
            logger.error("Error processing dividend warrant verification - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Unwraps holder company account details from the holder model.
     *
     * @param holdModel object of the holder model
     * @param newEntry boolean variable indicating whether or not entry is new
     * @return object of HolderCompanyAccount
     */
    private List<org.greenpole.hibernate.entity.HolderCompanyAccount> retrieveHolderCompanyAccount(List<org.greenpole.entity.model.holder.Holder> holdModelList) {
        List<org.greenpole.hibernate.entity.HolderCompanyAccount> companyAccountEntityList = new ArrayList<>();

        for (org.greenpole.entity.model.holder.Holder holdModel : holdModelList) {
            org.greenpole.hibernate.entity.HolderCompanyAccount companyAccountEntity = new org.greenpole.hibernate.entity.HolderCompanyAccount();
            org.greenpole.entity.model.holder.HolderCompanyAccount compAcct = holdModel.getCompanyAccounts().get(0);
            HolderCompanyAccountId compAcctId = new HolderCompanyAccountId();

            compAcctId.setClientCompanyId(compAcct.getClientCompanyId());

            companyAccountEntity.setId(compAcctId);
            companyAccountEntity.setEsop(compAcct.isEsop());
            companyAccountEntity.setHolderCompAccPrimary(true);
            companyAccountEntity.setMerged(false);

            companyAccountEntityList.add(companyAccountEntity);
        }
        return companyAccountEntityList;
    }

    /**
     * Unwraps Holder residential address from the holder model into
     * HolderResidentialAddress hibernate entity object.
     *
     * @param holdModel object of holder model
     * @param newEntry boolean variable indicating whether or not entry is new
     * @return List of HolderResidentialAddress hibernate entity objects
     */
    private List<HolderResidentialAddress> retrieveHolderResidentialAddress(List<org.greenpole.entity.model.holder.Holder> holdModelList) {
        List<HolderResidentialAddress> residentialAddressSend = new ArrayList<>();
        for (org.greenpole.entity.model.holder.Holder holdModel : holdModelList) {
            org.greenpole.hibernate.entity.HolderResidentialAddress residentialAddressEntity = new org.greenpole.hibernate.entity.HolderResidentialAddress();
            List<org.greenpole.entity.model.Address> residentialAddressList;
            if (holdModel.getResidentialAddresses() != null) {
                residentialAddressList = holdModel.getResidentialAddresses();
            } else {
                residentialAddressList = new ArrayList<>();
            }

            List<org.greenpole.hibernate.entity.HolderResidentialAddress> returnResidentialAddress = new ArrayList<>();

            for (org.greenpole.entity.model.Address rAddy : residentialAddressList) {
                // HolderResidentialAddressId rAddyId = new HolderResidentialAddressId();
                // rAddyId.setAddressLine1(rAddy.getAddressLine1());
                // rAddyId.setState(rAddy.getState());
                // rAddyId.setCountry(rAddy.getCountry());
                // residentialAddressEntity.setId(rAddyId);
                residentialAddressEntity.setAddressLine1(rAddy.getAddressLine1());
                residentialAddressEntity.setAddressLine2(rAddy.getAddressLine2());
                residentialAddressEntity.setAddressLine3(rAddy.getAddressLine3());
                residentialAddressEntity.setAddressLine4(rAddy.getAddressLine4());
                residentialAddressEntity.setCity(rAddy.getCity());
                residentialAddressEntity.setPostCode(rAddy.getPostCode());

                returnResidentialAddress.add(residentialAddressEntity);
            }
            residentialAddressSend.addAll(returnResidentialAddress);
        }
        return residentialAddressSend;
    }

    /**
     * Unwraps Holder email address from the holder model into
     * HolderEmailAddress hibernate entity object
     *
     * @param holdModel object to holder model
     * @param newEntry boolean variable indicating whether or not entry is new
     * @return List of HolderEmailAddress hibernate entity objects
     */
    private List<HolderEmailAddress> retrieveHolderEmailAddress(List<org.greenpole.entity.model.holder.Holder> holdModelList) {
        List<org.greenpole.hibernate.entity.HolderEmailAddress> emailAddressSend = new ArrayList<>();
        for (org.greenpole.entity.model.holder.Holder holdModel : holdModelList) {
            org.greenpole.hibernate.entity.HolderEmailAddress emailAddressEntity = new org.greenpole.hibernate.entity.HolderEmailAddress();
            List<org.greenpole.entity.model.EmailAddress> emailAddressList;
            if (holdModel.getEmailAddresses() != null) {
                emailAddressList = holdModel.getEmailAddresses();
            } else {
                emailAddressList = new ArrayList<>();
            }
            List<org.greenpole.hibernate.entity.HolderEmailAddress> returnEmailAddress = new ArrayList<>();

            for (EmailAddress email : emailAddressList) {
                // HolderEmailAddressId emailId = new HolderEmailAddressId();
                // emailId.setEmailAddress(email.getEmailAddress());
                emailAddressEntity.setIsPrimary(email.isPrimaryEmail());
                // emailAddressEntity.setId(emailId);
            }
            emailAddressSend.addAll(returnEmailAddress);
        }
        return emailAddressSend;
    }

    /**
     * Unwraps holder phone number details from the holder model passed as
     * parameter into HolderPhoneNumber hibernate entity
     *
     * @param holdModel object of holder details
     * @param newEntry boolean variable indicating whether or not the entry is
     * new
     * @return List of HolderPhoneNumber objects retrieveHolderEmailAddress
     */
    private List<HolderPhoneNumber> retrieveHolderPhoneNumber(List<org.greenpole.entity.model.holder.Holder> holdModelList) {
        List<org.greenpole.hibernate.entity.HolderPhoneNumber> phoneNumberSend = new ArrayList<>();
        for (org.greenpole.entity.model.holder.Holder holdModel : holdModelList) {
            org.greenpole.hibernate.entity.HolderPhoneNumber phoneNumberEntity = new org.greenpole.hibernate.entity.HolderPhoneNumber();
            List<org.greenpole.entity.model.PhoneNumber> phoneNumberList;
            if (holdModel.getPhoneNumbers() != null) {
                phoneNumberList = holdModel.getPhoneNumbers();
            } else {
                phoneNumberList = new ArrayList<>();
            }

            List<org.greenpole.hibernate.entity.HolderPhoneNumber> returnPhoneNumber = new ArrayList<>();

            for (PhoneNumber pnList : phoneNumberList) {
                // HolderPhoneNumberId phoneNoId = new HolderPhoneNumberId();
                // phoneNoId.setPhoneNumber(pnList.getPhoneNumber());
                phoneNumberEntity.setIsPrimary(pnList.isPrimaryPhoneNumber());
                // phoneNumberEntity.setId(phoneNoId);
            }
            phoneNumberSend.addAll(returnPhoneNumber);
        }
        return phoneNumberSend;
    }

    /**
     * Unwraps the holder postal address details from the HolderModel into
     * HolderPostalAddress hibernate entity
     *
     * @param holdModel the holderModel of holder details
     * @param newEntry boolean value indicating new entry
     * @return List object of HolderPostalAddress hibernate entity
     */
    private List<HolderPostalAddress> retrieveHolderPostalAddress(List<org.greenpole.entity.model.holder.Holder> holdModelList) {
        List<org.greenpole.hibernate.entity.HolderPostalAddress> holderPostalAddressSend = new ArrayList<>();
        for (org.greenpole.entity.model.holder.Holder holdModel : holdModelList) {
            org.greenpole.hibernate.entity.HolderPostalAddress postalAddressEntity = new org.greenpole.hibernate.entity.HolderPostalAddress();
            List<org.greenpole.entity.model.Address> hpaddyList;
            if (holdModel.getPostalAddresses() != null) {
                hpaddyList = holdModel.getPostalAddresses();
            } else {
                hpaddyList = new ArrayList<>();
            }

            List<org.greenpole.hibernate.entity.HolderPostalAddress> returnHolderPostalAddress = new ArrayList<>();

            for (org.greenpole.entity.model.Address hpa : hpaddyList) {
                // HolderPostalAddressId postalAddyId = new HolderPostalAddressId();
                // postalAddyId.setAddressLine1(hpa.getAddressLine1());
                // postalAddyId.setState(hpa.getState());
                // postalAddyId.setCountry(hpa.getCountry());
                // postalAddressEntity.setId(postalAddyId);
                postalAddressEntity.setAddressLine2(hpa.getAddressLine2());
                postalAddressEntity.setAddressLine3(hpa.getAddressLine3());
                postalAddressEntity.setCity(hpa.getCity());
                postalAddressEntity.setPostCode(hpa.getPostCode());
                postalAddressEntity.setIsPrimary(hpa.isPrimaryAddress());
                returnHolderPostalAddress.add(postalAddressEntity);
            }
            holderPostalAddressSend.addAll(returnHolderPostalAddress);
        }
        return holderPostalAddressSend;
    }

    /**
     * Validates declared dividend
     *
     * @param login the user's login details
     * @param dividendDeclared Dividend Declared object
     * @return response object to the calling method
     */
    private Response validateDividendDeclaration(Login login, DividendDeclared dividendDeclared) {
        Response resp = new Response();
        Date date = new Date();
        try {
            boolean clientCompanyExist = cq.checkClientCompany(dividendDeclared.getClientCompanyId());
            boolean shareHoldersExist = cq.checkClientCompanyForShareholders(cq.getClientCompany(dividendDeclared.getClientCompanyId()).getName());
            if (clientCompanyExist) {
                if (shareHoldersExist) {
                    if (dividendDeclared.getQualifyDate() != null && !"".equals(dividendDeclared.getQualifyDate())) {// check if qualify date is set
                        try {// check for date format
                            formatter.parse(dividendDeclared.getQualifyDate());
                            if (formatter.parse(dividendDeclared.getQualifyDate()).before(date)) {// date validity
                                resp.setRetn(200);
                                resp.setDesc("Qualify date cannot be before current date");
                                logger.info("Qualify date cannot be before current date - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                return resp;
                            }
                        } catch (ParseException ex) {
                            resp.setRetn(200);
                            resp.setDesc("Incorrect date format for qualify date");
                            logger.info("an error was thrown while checking the qualify date format. See error log invoked by [{}] - [{}]", login.getUserId(), resp.getRetn());
                            logger.error("Incorrect date format for qualify date invoked by [{}]", login.getUserId(), ex);
                            return resp;
                        }
                        if (dividendDeclared.getRate() > 0) {// check if rate is set
                            if (dividendDeclared.getDatePayable() != null && !"".equals(dividendDeclared.getDatePayable())) {//check if date payable is set and is on/after qualify date
                                try {// check for date format
                                    formatter.parse(dividendDeclared.getDatePayable());
                                    if (formatter.parse(dividendDeclared.getDatePayable()).before(date)) {// date validity
                                        resp.setRetn(200);
                                        resp.setDesc("Date payable cannot be before current date");
                                        logger.info("Date payable cannot be before current date - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                        return resp;
                                    }
                                } catch (ParseException ex) {
                                    logger.info("an error was thrown while checking the date payable format. See error log invoked by [{}] - [{}]", login.getUserId(), resp.getRetn());
                                    resp.setRetn(200);
                                    resp.setDesc("Incorrect date format for date payable");
                                    logger.error("Incorrect date format for date payable invoked by [{}]", login.getUserId(), ex);
                                    return resp;
                                }
                                if (formatter.parse(dividendDeclared.getDatePayable()).after(formatter.parse(dividendDeclared.getQualifyDate()))) {// checks if qualify date is less earlier than date payable
                                    if (dividendDeclared.getWithholdingTaxRateInd() > 0 || dividendDeclared.getWithholdingTaxRateCorp() > 0) {// check if withholding tax is set for individuals and corporate
                                        if (dividendDeclared.getIssueType() != null && !"".equals(dividendDeclared.getIssueType())) {// check if issue type is set
                                            if (dividendDeclared.getYearType() != null && !"".equals(dividendDeclared.getYearType())) {// check if year type (interim/final) is set
                                                resp.setRetn(0);
                                                return resp;
                                            }// year type (interim/final) is not specified
                                            resp.setRetn(200);
                                            resp.setDesc("Year type (interim/final) is not specified");
                                            logger.info("Year type (interim/final) is not specified - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                            return resp;
                                        }// issue type is not set
                                        resp.setRetn(200);
                                        resp.setDesc("Issue type is not set");
                                        logger.info("Issue type is not set - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                        return resp;
                                    }// withholding tax is not set either for individuals or corporate
                                    resp.setRetn(200);
                                    resp.setDesc("Withholding tax is not set either for individuals or corporate");
                                    logger.info("Withholding tax is not set either for individuals or corporate - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                    return resp;
                                }// qualify date should be earlier than date payable
                                resp.setRetn(200);
                                resp.setDesc("Qualify date should be ealier than Date payable");
                                logger.info("Qualify date should be ealier than Date payable - [{}]: [{}]", login.getUserId(), resp.getRetn());
                                return resp;
                            }// date payable is not set or it is earlier than qualify date
                            resp.setRetn(200);
                            resp.setDesc("Date payable is not set or it is earlier than qualify date");
                            logger.info("Date payable is not set or it is earlier than qualify date - [{}]: [{}]", login.getUserId(), resp.getRetn());
                            return resp;
                        }// rate is not set
                        resp.setRetn(200);
                        resp.setDesc("Rate (price per share-unit) is not set");
                        logger.info("Rate (price per share-unit) is not set - [{}]: [{}]", login.getUserId(), resp.getRetn());
                        return resp;
                    }// qualify date not set
                    resp.setRetn(200);
                    resp.setDesc("Qualify date is not set");
                    logger.info("Qualify date is not set - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }// no shareholder
                resp.setRetn(200);
                resp.setDesc("Client company does not have shareholders");
                logger.info("Client company does not have shareholders. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }// client company does not exist
            resp.setRetn(200);
            resp.setDesc("Client company does not exist");
            logger.info("Client company does not exist. - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process client company dividend declaration. Contact system administrator.");
            logger.info("Error processing client company creation. See error log - [{}]", login.getUserId());
            logger.error("Error processing client company creation - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * checks the status of a Dividend
     *
     * @param login the user's login details
     * @param dividend Dividend object
     * @return response to the check dividend status method
     */
    private Response checkDividendStatus(Login login, Dividend dividend) {
        Response resp = new Response();
        if (dividend.getPaid()) {// check if dividend has been NOT paid
            if (dividend.isCancelled()) {// check if it has not been cancelled already
                if (dividend.getClientCompanyId() <= 0) {// check if client company id is present
                    resp.setRetn(0);
                    return resp;
                }// client company id needed for search should be specified
                resp.setRetn(200);
                resp.setDesc("Client company id needed for search should be specified");
                logger.info("Client company id needed for search should be specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }// dividend has being previously cancelled
            resp.setRetn(200);
            resp.setDesc("Dividend has previously being cancelled");
            logger.info("Dividend has previously being cancelled. - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        }// dividend has been paid for and so cannot be cancelled
        resp.setRetn(200);
        resp.setDesc("Dividend has been paid for and so cannot be cancelled");
        logger.info("Dividend has been paid for and so cannot be cancelled. - [{}]: [{}]", login.getUserId(), resp.getRetn());
        return resp;
    }

    /**
     * Processes a dividend for replacement
     *
     * @param login the user's login details
     * @param dividend Dividend object
     * @return response to the check dividend replaced
     */
    private Response checkDividendReplaced(Login login, Dividend dividend) {
        Response resp = new Response();
        try {
            if (!dividend.isReIssued() || !dividend.isIssued()) {// checks if dividend is issued or re-issued
                if ((dividend.getPaymentMethod() == null || "".equals(dividend.getPaymentMethod())) || !dividend.getPaid()) {//checks if dividend has NOT been paid for or does not belong to shareholder with a mandated e-payment
                    if (dividend.getAnnotation() != null && !"".equals(dividend.getAnnotation())) {// check for dividend annotation
                        resp.setRetn(0);
                        return resp;
                    }// dividend annotation (reason for replacing dividend warrant) is not specified
                    resp.setRetn(200);
                    resp.setDesc("Dividend annotation (reason for wanting to replace dividend warrant) is not specified");
                    logger.info("Dividend annotation (reason for wanting to replace dividend warrant) is not specified. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }// dividend has been paid for or shareholder belongs with a mandated e-payment
                resp.setRetn(200);
                resp.setDesc("Dividend has been paid for or shareholder belongs with a mandated e-payment and so cannot replace dividend");
                logger.info("Dividend has been paid for or shareholder belongs with a mandated e-payment and so cannot replace dividend. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }// dividend has not been issued or re-issued
            resp.setRetn(200);
            resp.setDesc("Dividend has not been issued or re-issued and so cannot replace dividend");
            logger.info("Dividend has not been issued or re-issued and so cannot replace dividend. - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (NullPointerException ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to validate 'replace dividend warrant' authorisation request. Contact system administrator.");
            logger.info("Error validating 'replace dividend warrant' authorisation request. See error log - [{}]", login.getUserId());
            logger.error("Error validating 'replace dividend warrant' authorisation request - [" + login.getUserId() + "]", ex);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to validate 'replace dividend warrant' authorisation request. Contact system administrator.");
            logger.info("Error validating 'replace dividend warrant' authorisation request. See error log - [{}]", login.getUserId());
            logger.error("Error validating 'replace dividend warrant' authorisation request - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

}
