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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.greenpole.entity.model.holder.HolderCompanyAccount;
import org.greenpole.entity.model.taguser.TagUser;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.entrycode.emmanuel.model.Certificate;
import org.greenpole.entrycode.emmanuel.model.CertificateLodgement;
import org.greenpole.entrycode.emmanuel.model.CertificateSplitConfirm;
import org.greenpole.entrycode.emmanuel.model.CertificateVerification;
import org.greenpole.entrycode.emmanuel.model.ProcessCertificateSplit;
import org.greenpole.entrycode.emmanuel.model.QueryCertificate;
import org.greenpole.entrycode.emmanuel.model.ViewCertificateLodgements;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.GeneralComponentQuery;
import org.greenpole.hibernate.query.HolderComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.logic.ClientCompanyComponentLogic;
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
public class CertificateComponentLogic {

    private static final Logger logger = LoggerFactory.getLogger(HolderComponentLogic.class);
    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();
    private final GeneralComponentQuery gq = ComponentQueryFactory.getGeneralComponentQuery();
    private final GreenpoleProperties greenProp = GreenpoleProperties.getInstance();
    private final NotificationProperties notificationProp = NotificationProperties.getInstance();
    private final HolderComponentQuery hq = ComponentQueryFactory.getHolderComponentQuery();
    private final HibernatDummyQuerInterface hd = HibernateDummyQueryFactory.getHibernateDummyQuery();

    /**
     * processes certificate for a holder
     *
     * @param login the user details
     * @param cert the certificate been processed
     * @return response to the certificate creation request
     */
    public Response createCertificate(Login login, Certificate cert) {
        Response resp = new Response();
        logger.info("request to create certificate invoked by [{}] ", login.getUserId());
        try {
            long millis = System.currentTimeMillis();
            Date current_date = new java.sql.Date(millis);
            org.greenpole.hibernate.entity.Certificate cert_hib = new org.greenpole.hibernate.entity.Certificate();
            org.greenpole.hibernate.entity.Holder holder = hq.getHolder(cert.getHolderId());
            org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(cert.getClientCompanyId());
            boolean created = false;
            if (cq.checkClientCompany(cert.getClientCompanyId())) {
                logger.info("client company checks out for certificate creation by ", login.getUserId());
                if (hq.checkHolderAccount(cert.getHolderId())) {

                    if (cert.getCertificateNumber() > 0) {
                        if (cert.getShareVolume() > 0 || cert.getBondHolding() > 0) {
                            if (cert.getHolderName() != null && !cert.getHolderName().isEmpty()) {
                                if (cert.getHolderAddress() != null && !cert.getHolderAddress().isEmpty()) {
                                    if (cert.getCertNarration() != null && !cert.getCertNarration().isEmpty()) {
                                        if (cert.getIssueDate() != null && !cert.getIssueDate().isEmpty()) {
                                            if (cert.isIsShareholder()) {//holder is a shareholder
                                                cert_hib.setShareVolume(cert.getShareVolume());
                                                cert_hib.setHolder(holder);
                                                cert_hib.setHolderName(cert.getHolderName());
                                                cert_hib.setHolderAddress(cert.getHolderAddress());
                                                cert_hib.setClientCompany(cc);
                                                cert_hib.setIssueDate(current_date);
                                                cert_hib.setCertNarration(cert.getCertNarration());
                                                cert_hib.setCancelled(false);
                                                cert_hib.setClaimed(false);
                                                created = hd.createCertificate(cert_hib);
                                            } else if (!cert.isIsShareholder()) {
                                                //cert_hib.setCertificateNumber(cert.getCertificateNumber());
                                                cert_hib.setBondHolding(cert.getBondHolding());
                                                cert_hib.setHolder(holder);
                                                cert_hib.setHolderName(cert.getHolderName());
                                                cert_hib.setHolderAddress(cert.getHolderAddress());
                                                cert_hib.setClientCompany(cc);
                                                cert_hib.setIssueDate(current_date);
                                                cert_hib.setCertNarration(cert.getCertNarration());
                                                cert_hib.setCancelled(false);
                                                cert_hib.setClaimed(false);
                                                created = hd.createCertificate(cert_hib);
                                            }
                                            if (created) {
                                                //sms/email notification to holder
                                                resp.setRetn(0);
                                                resp.setDesc("Successful");
                                                logger.info("Certificate successfully created for holder [{}] by [{}] ", cert.getHolderName(), login.getUserId());
                                                return resp;
                                            }
                                            if (!created) {
                                                resp.setRetn(300);
                                                resp.setDesc("Failed to create certificate ");
                                                logger.info("Failed to create certificate for holder [{}] by [{}] ", cert.getHolderName(), login.getUserId());
                                                return resp;
                                            }
                                        }
                                        resp.setRetn(300);
                                        resp.setDesc("Certificate issue date must be specified ");
                                        logger.info("Failed because certificate issue date is not specified ", login.getUserId());
                                        return resp;
                                    }
                                    resp.setRetn(300);
                                    resp.setDesc("Certificate narration must be specified ");
                                    logger.info("Failed because certificate narration is not specified ", login.getUserId());
                                    return resp;
                                }
                                resp.setRetn(300);
                                resp.setDesc("Holder of certificate address must be specified ");
                                logger.info("Failed because certificate holder address is not specified ", login.getUserId());
                                return resp;
                            }
                            resp.setRetn(300);
                            resp.setDesc("Certificate holder name must be specified ");
                            logger.info("Failed because certificate holder name is not specified ", login.getUserId());
                            return resp;
                        }
                        resp.setRetn(300);
                        resp.setDesc("Certificate share volume / bond units must be specified ");
                        logger.info("Failed because certificate share volume / bond unit is not specified ", login.getUserId());
                        return resp;
                    }
                    resp.setRetn(300);
                    resp.setDesc("Certificate number must be specified ");
                    logger.info("Failed because certificate number is not specified ", login.getUserId());
                    return resp;
                }
                resp.setRetn(300);
                resp.setDesc("Certificate holder does not exists ");
                logger.info("Failed because certificate holder does not exists ", login.getUserId());
                return resp;
            }
            resp.setRetn(300);
            resp.setDesc("Client company for holder certificate does not exists ");
            logger.info("Failed because client company for holder certificate does not exists ", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error creating certificate for holder [{}] . See error log - [{}]", cert.getHolderName(), login.getUserId());
            logger.error("error revalidating dividend record - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to revalidate dividend record . Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    public Response queryCertificate(Login login, QueryCertificate queryParams) {
        Response resp = new Response();
        logger.info("request to query certificate details invoked by [{}] ", login.getUserId());
        Descriptor descriptorUtil = new Descriptor();
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        org.greenpole.hibernate.entity.Certificate cert_hib_search = new org.greenpole.hibernate.entity.Certificate();
        try {
            if (queryParams.getDescriptor() == null || "".equals(queryParams.getDescriptor())) {
                logger.info("certificate query unsuccessful. Empty descriptor - [{}]", login.getUserId());
                resp.setRetn(300);
                resp.setDesc("Unsuccessful transaction query, due to empty descriptor. Contact system administrator");
                return resp;
            }

            Map<String, String> descriptors = descriptorUtil.decipherDescriptor(queryParams.getDescriptor());
            if (descriptors.size() == 3) {
                Certificate cert_model_serach = new Certificate();
                if (queryParams.getCertificate() != null) {
                    cert_model_serach = queryParams.getCertificate();
                    org.greenpole.hibernate.entity.Holder holder = hq.getHolder(queryParams.getCertificate().getHolderId());
                    //cert_hib_search.setCertificateNumber(cert_model_serach.getCertificateNumber());
                    cert_hib_search.setHolder(holder);//search by holder APR account number
                    cert_hib_search.setHolderAddress(cert_model_serach.getHolderAddress());
                    cert_hib_search.setIssuingCompName(cert_model_serach.getIssuingCompName());
                    if (cert_model_serach.getIssueDate() != null && !"".equals(cert_model_serach.getIssueDate())) {
                        try {
                            cert_hib_search.setIssueDate(formatter.parse(cert_model_serach.getIssueDate()));
                        } catch (Exception ex) {
                            logger.info("an error was thrown while checking the certificate issue date. See error log - [{}]", login.getUserId());
                            resp.setRetn(308);
                            resp.setDesc("Incorrect date format for issue date");
                            logger.error("Incorrect date format for issue date [" + login.getUserId() + "]", ex);
                        }
                    }
                    Map<String, Integer> shareVolume_search;
                    if (queryParams.getShareVolume() != null && !queryParams.getShareVolume().isEmpty()) {
                        shareVolume_search = queryParams.getShareVolume();
                    } else {
                        shareVolume_search = new HashMap<>();
                    }
                    List<Certificate> cert_model_list_out = new ArrayList<>();
                    List<org.greenpole.hibernate.entity.Certificate> cert_hib_list = hd.queryCertificates(queryParams.getDescriptor(), cert_hib_search, shareVolume_search, queryParams.getStartDate(), queryParams.getEndDate());
                    for (org.greenpole.hibernate.entity.Certificate cert : cert_hib_list) {
                        Certificate cert_model = new Certificate();
                        cert_model.setHolderId(cert.getHolder().getId());
                        cert_model.setHolderAddress(cert.getHolderAddress());
                        cert_model.setIssuingCompName(cert.getIssuingCompName());
                        cert_model.setIssueDate(formatter.format(cert.getIssueDate()));
                        cert_model.setCertNarration(cert.getCertNarration());
                        cert_model.setClaimed(cert.getClaimed());
                        cert_model.setClientCompanyId(cert.getClientCompany().getId());
                        cert_model.setHolderName(cert.getHolderName());
                        if (queryParams.getCertificate().isIsShareholder()) {
                            cert_model.setShareVolume(cert.getShareVolume());
                        } else if (!queryParams.getCertificate().isIsShareholder()) {
                            cert_model.setBondHolding(cert.getBondHolding());
                        }
                        cert_model_list_out.add(cert_model);
                    }
                    resp.setRetn(0);
                    resp.setDesc("Certificates successfully queried ");
                    resp.setBody(cert_hib_list);
                    return resp;
                }

            }
        } catch (Exception ex) {
            logger.info("error querying certificates. See error log - [{}]", login.getUserId());
            logger.error("error querying certificates - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to query certificates. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
        return resp;
    }

    /**
     * processes request to transfer certificate ownership
     *
     * @param login the user details
     * @param authenticator the user meant to receive this notification
     * @param cert the details of the certificate to be transfered
     * @return response to the transfer of certificate ownership
     */
    public Response transferCertOwnership_Request(Login login, String authenticator, Certificate cert) {
        Response resp = new Response();
        logger.info("request to transfer certificate ownership from invoked by [{}] ", login.getUserId());
        Notification notification = new Notification();
        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;
            org.greenpole.hibernate.entity.Holder h = hd.getHolder(cert.getNewHolderId());
            org.greenpole.hibernate.entity.HolderCompanyAccount newhca = hd.getHolderCompanyAccount(cert.getNewHolderId());
            boolean certExists = hd.checkCertificate(cert.getCertificateNumber(), cert.getHolderId());
            if (certExists) {//checks if certificate exists
                if (h.getChn() != null && !"".equals(h.getChn())) {
                    if (cert.getClientCompanyId() == newhca.getClientCompany().getId()) {
                        if (cert.getCertificateNumber() > 0) {
                            wrapper = new NotificationWrapper();
                            prop = NotifierProperties.getInstance();
                            qSender = new QueueSender(prop.getNotifierQueueFactory(),
                                    prop.getAuthoriserNotifierQueueName());
                            List<Certificate> certlist = new ArrayList<>();
                            certlist.add(cert);
                            wrapper.setCode(notification.createCode(login));
                            wrapper.setDescription("Authenticate transfer of certificate ownership from " + cert.getHolderName() + " to " + h.getFirstName()
                                    + " " + h.getLastName() + " invoked by " + login.getUserId());
                            wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                            wrapper.setFrom(login.getUserId());
                            wrapper.setTo(authenticator);
                            wrapper.setModel(certlist);
                            logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                            resp = qSender.sendAuthorisationRequest(wrapper);
                            return resp;
                        }
                        resp.setRetn(300);
                        resp.setDesc("Certificate number must not be empty");
                        logger.info("Certificate ownership transfer cannot be completed because certificate number is empty ", login.getUserId());
                        return resp;
                    }
                    resp.setRetn(300);
                    resp.setDesc("Transfer of certificate ownership can only take place between holders of the same company");
                    logger.info("Transfer of certificate ownership can only take place between holders of the same company ", login.getUserId());
                    return resp;
                }
                resp.setRetn(300);
                resp.setDesc("Transfer of certificate ownership can not be completed because owner of certificate has no chn ");
                logger.info("Transfer of certificate ownership can not be completed because owner of certificate has no chn ", login.getUserId());
                return resp;
            }
            resp.setRetn(300);
            resp.setDesc("The certificate does not exists ");
            logger.info("The certificate does not exists ", login.getUserId());
            return resp;

        } catch (Exception ex) {
            logger.info("error transfering certificate ownership. See error log - [{}]", login.getUserId());
            logger.error("error transfering certificate ownership - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to transfer certificate ownership. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     * Processes request to transfer certificate ownership that has already been
     * saved
     *
     * @param login the user details
     * @param notificationCode the notification code
     * @return response to the transfer of certificate ownership request
     */
    public Response transferCertOwnership_Authorisation(Login login, String notificationCode) {
        Response resp = new Response();
        logger.info("authorise transfer of certificate ownership, invoked by [{}] - notification code: [{}]", login.getUserId(), notificationCode);
        Notification notification = new Notification();
        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<Certificate> certlist = (List<Certificate>) wrapper.getModel();
            Certificate certModel = certlist.get(0);
            org.greenpole.hibernate.entity.Holder h = hd.getHolder(certModel.getNewHolderId());
            org.greenpole.hibernate.entity.HolderCompanyAccount newhca = hd.getHolderCompanyAccount(certModel.getNewHolderId());
            boolean certExists = hd.checkCertificate(certModel.getCertificateNumber(), certModel.getHolderId());
            if (certExists) {
                if (h.getChn() != null && !"".equals(h.getChn())) {
                    if (certModel.getClientCompanyId() == newhca.getClientCompany().getId()) {
                        if (certModel.getCertificateNumber() > 0) {
                            org.greenpole.hibernate.entity.Certificate transferCert = hd.getCertificate(certModel.getCertificateNumber(), certModel.getHolderId());
                            transferCert.setHolder(h);
                            boolean updated = hd.updateCertOwnership(transferCert);
                            if (updated) {
                                notification.markAttended(notificationCode);
                                resp.setRetn(0);
                                resp.setDesc("Successful");
                                logger.info("Successful");
                                return resp;
                            }
                            if (!updated) {
                                resp.setRetn(300);
                                resp.setDesc("Failed to transfer certificate ownership due to error");
                                logger.info("Failed to transfer certificate ownership due to error invokde by ", login.getUserId());
                                return resp;
                            }
                        }
                        resp.setRetn(300);
                        resp.setDesc("Certificate number must not be empty");
                        logger.info("Certificate ownership transfer cannot be completed because certificate number is empty ", login.getUserId());
                        return resp;
                    }
                    resp.setRetn(300);
                    resp.setDesc("Transfer of certificate ownership can only take place between holders of the same company");
                    logger.info("Transfer of certificate ownership can only take place between holders of the same company ", login.getUserId());
                    return resp;
                }
                resp.setRetn(300);
                resp.setDesc("Transfer of certificate ownership can not be completed because owner of certificate has no chn ");
                logger.info("Transfer of certificate ownership can not be completed because owner of certificate has no chn ", login.getUserId());
                return resp;
            }
            resp.setRetn(300);
            resp.setDesc("The certificate does not exists ");
            logger.info("The certificate does not exists ", login.getUserId());
            return resp;

        } catch (Exception ex) {
            logger.info("error transfering certificate ownership. See error log - [{}]", login.getUserId());
            logger.error("error transfering certificate ownership - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to transfer certificate ownership. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     * processes confirmation request to split a certificate ownership
     *
     * @param login the user details
     * @param splitCert details of the certificate to be splitted
     * @return response to the splitting of the certificate confirmation
     */
    public Response splitCertificateConfirm_Request(Login login, ProcessCertificateSplit splitCert) {
        Response resp = new Response();
        logger.info("request certificate split confirmation invoked by ", login.getUserId());
        try {
            int counter;
            boolean holderCompAccExists = false;
            boolean chnExists = false;
            CertificateSplitConfirm csc = new CertificateSplitConfirm();
            HolderCompanyAccount hca = new HolderCompanyAccount();
            List<HolderCompanyAccount> hca_model_list_out = new ArrayList<>();
            boolean certExists = hd.checkCertificate(splitCert.getOriginalCertificate().getCertificateNumber(), splitCert.getOriginalCertificate().getHolderId());
            if (certExists) {
                if (splitCert.getCreatedCetificates().size() <= 4) {
                    for (counter = 0; counter < splitCert.getCreatedCetificates().size(); counter++) {
                        org.greenpole.hibernate.entity.Holder h = hd.getHolder(splitCert.getCreatedCetificates().get(counter).getHolderId());
                        chnExists = hq.checkHolderAccount(h.getChn());
                        holderCompAccExists = hq.checkHolderCompanyAccount(splitCert.getCreatedCetificates().get(counter).getHolderId(), splitCert.getOriginalCertificate().getClientCompanyId());
                        if (!holderCompAccExists) {
                            break;
                        }
                        if (!chnExists) {
                            break;
                        }
                        if (holderCompAccExists && chnExists) {
                            if (splitCert.getOriginalCertificate().getClientCompanyId() == splitCert.getCreatedCetificates().get(counter).getClientCompanyId()) {
                                org.greenpole.hibernate.entity.HolderCompanyAccount hca_hib = hq.getHolderCompanyAccount(splitCert.getCreatedCetificates().get(counter).getHolderId(), splitCert.getOriginalCertificate().getClientCompanyId());
                                hca.setClientCompanyId(hca_hib.getClientCompany().getId());
                                hca.setHolderId(hca_hib.getHolder().getId());
                                hca.setClientCompanyName(hca_hib.getClientCompany().getName());
                                hca.setHolderCompAccPrimary(hca_hib.getHolderCompAccPrimary());
                                hca.setShareUnits(hca_hib.getShareUnits());
                                hca_model_list_out.add(hca);
                                csc.setCertificateShareVolume(splitCert.getOriginalCertificate().getShareVolume());
                                csc.setHolderCompanyAccount(hca_model_list_out);
                            }
                            resp.setRetn(300);
                            resp.setDesc("One of the shareholders does not have the same company account");
                            logger.info("One of the shareholders does not have the same company account ", login.getUserId());
                            return resp;
                        }
                        resp.setRetn(300);
                        resp.setDesc("One of the shareholders does not have a company account or has no chn ");
                        logger.info("One of the shareholders does not have a company account or has no chn ", login.getUserId());
                        return resp;

                    }
                    resp.setRetn(0);
                    resp.setBody(hca_model_list_out);
                    return resp;
                }
                resp.setRetn(300);
                resp.setDesc("The maximum number of split per certificate cannot exceed four ");
                logger.info("The maximum number of split per certificate cannot exceed four ", login.getUserId());
                return resp;
            }
            resp.setRetn(300);
            resp.setDesc("The certificate to be splitted does not exists ");
            logger.info("The certificate to be splitted does not exists ", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error splitting certificate ownership. See error log - [{}]", login.getUserId());
            logger.error("error splitting certificate ownership - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to split certificate ownership. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     * processes request to split a certificate ownership
     *
     * @param login the user details
     * @param authenticator the user meant to receive this notification
     * @param splitCert details of the certificate to be splitted
     * @return response to the splitting of the certificate
     */
    public Response splitCertificate_Request(Login login, String authenticator, ProcessCertificateSplit splitCert) {
        Response resp = new Response();
        logger.info("request certificate split invoked by ", login.getUserId());
        Notification notification = new Notification();
        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;
            int counter;
            boolean holderCompAccExists = false;
            boolean chnExists = false;
            boolean certExists = hd.checkCertificate(splitCert.getOriginalCertificate().getCertificateNumber(), splitCert.getOriginalCertificate().getHolderId());
            if (certExists) {
                if (splitCert.getCreatedCetificates().size() <= 4) {
                    for (counter = 0; counter < splitCert.getCreatedCetificates().size(); counter++) {
                        org.greenpole.hibernate.entity.Holder h = hd.getHolder(splitCert.getCreatedCetificates().get(counter).getHolderId());
                        chnExists = hq.checkHolderAccount(h.getChn());
                        holderCompAccExists = hq.checkHolderCompanyAccount(splitCert.getCreatedCetificates().get(counter).getHolderId(), splitCert.getOriginalCertificate().getClientCompanyId());
                        if (!chnExists) {
                            break;
                        }
                        if (!holderCompAccExists) {
                            break;
                        }
                        if (holderCompAccExists && chnExists) {

                            if (splitCert.getOriginalCertificate().getClientCompanyId() == splitCert.getCreatedCetificates().get(counter).getClientCompanyId()) {//check if shareholders are of the same account type
                                wrapper = new NotificationWrapper();
                                prop = NotifierProperties.getInstance();
                                qSender = new QueueSender(prop.getNotifierQueueFactory(),
                                        prop.getAuthoriserNotifierQueueName());
                                List<ProcessCertificateSplit> certlist = new ArrayList<>();
                                certlist.add(splitCert);
                                wrapper.setCode(notification.createCode(login));
                                wrapper.setDescription("Authenticate certificate split invoked by " + login.getUserId());
                                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                                wrapper.setFrom(login.getUserId());
                                wrapper.setTo(authenticator);
                                wrapper.setModel(certlist);
                                logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                                resp = qSender.sendAuthorisationRequest(wrapper);
                                return resp;
                            }
                            resp.setRetn(300);
                            resp.setDesc("One of the shareholder has a different company account ");
                            logger.info("One of the shareholder has a different company account ", login.getUserId());
                            return resp;
                        }
                        resp.setRetn(300);
                        resp.setDesc("One of the shareholders does not have a company account or has no chn ");
                        logger.info("One of the shareholders does not have a company account or has no chn ", login.getUserId());
                        return resp;
                    }
                }
                resp.setRetn(300);
                resp.setDesc("Maximum split cannot exceed four ");
                logger.info("Maximum split cannot exceed four ", login.getUserId());
                return resp;
            }
            resp.setRetn(300);
            resp.setDesc("The certificate to be splitted does not exists ");
            logger.info("The certificate to be splitted does not exists ", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error splitting certificate ownership. See error log - [{}]", login.getUserId());
            logger.error("error splitting certificate ownership - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to split certificate ownership. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     * Processes saved request to splitting of certificate
     *
     * @param login the user details
     * @param notificationCode the notification code
     * @return response to the splitting of certificate request
     */
    public Response splitCertificate_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        logger.info("authorise splitting of certificate ", login.getUserId());
        Notification notification = new Notification();
        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<ProcessCertificateSplit> certlist = (List<ProcessCertificateSplit>) wrapper.getModel();
            ProcessCertificateSplit certModel = certlist.get(0);
            long millis = System.currentTimeMillis();
            Date current_date = new java.sql.Date(millis);
            int counter;
            boolean holderCompAccExists = false;
            boolean created = false;
            boolean chnExists = false;
            boolean update = false;
            Random ran = new Random();
            int ac = 0;
            org.greenpole.hibernate.entity.Certificate cert_hib = new org.greenpole.hibernate.entity.Certificate();
            boolean certExists = hd.checkCertificate(certModel.getOriginalCertificate().getCertificateNumber(), certModel.getOriginalCertificate().getHolderId());
            org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(certModel.getOriginalCertificate().getClientCompanyId());
            org.greenpole.hibernate.entity.Certificate originalCert = hd.getCertificate(certModel.getOriginalCertificate().getCertificateNumber(), certModel.getOriginalCertificate().getHolderId());
            org.greenpole.hibernate.entity.CertificateEvent certEvent = new org.greenpole.hibernate.entity.CertificateEvent();
            if (certExists) {
                if (certModel.getCreatedCetificates().size() <= 4) {
                    for (counter = 0; counter < certModel.getCreatedCetificates().size(); counter++) {
                        org.greenpole.hibernate.entity.Holder h = hd.getHolder(certModel.getCreatedCetificates().get(counter).getHolderId());
                        chnExists = hq.checkHolderAccount(h.getChn());
                        holderCompAccExists = hq.checkHolderCompanyAccount(certModel.getCreatedCetificates().get(counter).getHolderId(), certModel.getOriginalCertificate().getClientCompanyId());
                        if (!chnExists) {
                            break;
                        }
                        if (!holderCompAccExists) {
                            break;
                        }
                        int ser = ran.nextInt() % 1000000000;
                        int ser2 = ran.nextInt() % 1000000000;

                        if (ser < 0) {
                            ser = ser / (-1);
                        }
                        if (ser2 < 0) {
                            ser2 = ser2 / (-1);
                        }
                        String sr = Integer.toString(ser).substring(0, 5);
                        String ssl = Integer.toString(ser2).substring(0, 5);
                        String certificateNo;
                        certificateNo = sr + ssl;
                        ac = Integer.parseInt(certificateNo);
                        if (holderCompAccExists && chnExists) {
                            if (certModel.getOriginalCertificate().getClientCompanyId() == certModel.getCreatedCetificates().get(counter).getClientCompanyId()) {
                                cert_hib.setShareVolume(certModel.getCreatedCetificates().get(counter).getShareVolume());
                                cert_hib.setHolder(h);
                                cert_hib.setHolderName(certModel.getCreatedCetificates().get(counter).getHolderName());
                                cert_hib.setHolderAddress(certModel.getCreatedCetificates().get(counter).getHolderAddress());
                                cert_hib.setClientCompany(cc);
                                cert_hib.setIssueDate(current_date);
                                cert_hib.setCertNarration(certModel.getOriginalCertificate().getCertNarration());
                                cert_hib.setCancelled(false);
                                cert_hib.setClaimed(false);
                                created = hd.createCertificate(cert_hib);
                                originalCert.setCancelled(true);
                                update = hd.updateCertOwnership(originalCert);
                            }
                            resp.setRetn(300);
                            resp.setDesc("One of the shareholder has a different company account ");
                            logger.info("One of the shareholder has a different company account ", login.getUserId());
                            return resp;
                        }
                        resp.setRetn(300);
                        resp.setDesc("One of the shareholders does not have a company account or has no chn ");
                        logger.info("One of the shareholders does not have a company account or has no chn ", login.getUserId());
                        return resp;
                    }
                    if (created && update) {
                        certEvent.setCertificate(cert_hib);
                        certEvent.setEventDate(current_date);
                        certEvent.setSplitStatus(true);
                        hd.createCertEvent(certEvent);
                        resp.setRetn(0);
                        resp.setDesc("Successfully splitted certificate ownership ");
                        logger.info("Successful splitted certificate ownership ", login.getUserId());
                        return resp;
                    }
                    if (!created && !update) {
                        resp.setRetn(0);
                        resp.setDesc("Failed to split certificate ownership ");
                        logger.info("Failed to split certificate ownership ", login.getUserId());
                        return resp;
                    }
                }
                resp.setRetn(300);
                resp.setDesc("Maximum split cannot exceed four ");
                logger.info("Maximum split cannot exceed four ", login.getUserId());
                return resp;
            }
            resp.setRetn(300);
            resp.setDesc("The certificate to be splitted does not exists ");
            logger.info("The certificate to be splitted does not exists ", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error splitting certificate ownership from authorise. See error log - [{}]", login.getUserId());
            logger.error("error splitting certificate ownership from authorise- [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to split certificate ownership from authorise. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     * processes confirmation request to lodge or dematerialize a certificate
     *
     * @param login the user details
     * @param authenticator the user meant to receive this notification
     * @param certList list of certificates for lodgement
     * @return response to the certificate lodgement request
     */
    public Response lodgeCertificateConfirm_Request(Login login, String authenticator, List<Certificate> certList) {
        Response resp = new Response();
        logger.info(" request to lodge / dematerialise certificate ", login.getUserId());
        Notification notification = new Notification();

        NotificationWrapper wrapper;
        QueueSender queue;
        NotifierProperties prop;
        List<CertificateLodgement> certLodgeList = new ArrayList<>();
        try {
            if (certList != null && !certList.isEmpty()) {
                for (Certificate cert : certList) {
                    org.greenpole.hibernate.entity.Holder holder = hd.getHolder(cert.getHolderId());
                    if (holder.getChn() != null && !"".equals(holder.getChn())) {
                        CertificateLodgement cl = new CertificateLodgement();
                        cl.setTitle(cert.getCertificateLodgement().getTitle());
                        cl.setChn(holder.getChn());
                        certLodgeList.add(cl);
                    }
                }
                resp.setRetn(0);
                resp.setBody(certList);
                return resp;
            }
            resp.setRetn(300);
            resp.setDesc("No certificate found for lodgement operation");
            logger.info("No certificate found for lodgement operation ", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error lodging certificate. See error log - [{}]", login.getUserId());
            logger.error("error lodging certificate - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to lodge certificate. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }

    }

    /**
     * processes saved request to lodge or dematerialize a certificate
     *
     * @param login the user details
     * @param notificationCode the notification code
     * @return response to the certificate lodgement request
     */
    public Response lodgeCertificate_Authorisation(Login login, String notificationCode) {
        Response resp = new Response();
        logger.info(" request to lodge / dematerialise certificate ", login.getUserId());
        try {

        } catch (Exception ex) {
        }
        return resp;
    }

    /**
     * Request to view report on certificate lodgement
     *
     * @param login the user Id of the user performing the view request
     * @param queryParams the query parameters
     * @return response to the view report on shareholder accounts consolidation
     * request
     */
    public Response viewCertificateLodgementReport(Login login, ViewCertificateLodgements queryParams) {
        Response resp = new Response();
        logger.info(" request to view certificate lodgement report ", login.getUserId());
        Descriptor descriptorUtil = new Descriptor();
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());

            Map<String, String> descriptors = descriptorUtil.decipherDescriptor(queryParams.getDescriptor());
            if (descriptors.size() == 1) {
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
                List<org.greenpole.hibernate.entity.CertificateLodgement> certLodge_hib_list = hd.viewCertLodgementReport(queryParams.getDescriptor(), queryParams.getStartDate(), queryParams.getEndDate(),
                        greenProp.getDateFormat());
                List<CertificateLodgement> certLodge_model_list_out = new ArrayList<>();
                TagUser tag = new TagUser();
                for (org.greenpole.hibernate.entity.CertificateLodgement cl : certLodge_hib_list) {
                    CertificateLodgement cert_model = new CertificateLodgement();
                    //cert_model.setCertificateNumber(cl.getCertificateNumber());
                    //cert_model.setChn(cl.getChn());
                    //cert_model.setControlNumber(cl.getControlNumber());
                    cert_model.setDate(formatter.format(cl.getDate()));
                    //cert_model.setHoldings(cl.getHoldings());
                    cert_model.setProcessed(cl.getProcessed());
                    //cert_model.setShareholderName(cl.getShareholderName());
                    cert_model.setTitle(cl.getStatus());
                    certLodge_model_list_out.add(cert_model);
                }
                List<TagUser> tagList = new ArrayList<>();

                tag.setQueryParam(queryParams);
                tag.setResult(certLodge_model_list_out);
                tagList.add(tag);

                resp.setBody(tagList);
                resp.setDesc("Query Successful");
                resp.setRetn(0);
                logger.info("Query successful - [{}]", login.getUserId());
                return resp;
            }
            logger.info("descriptor length does not match expected required length - [{}]", login.getUserId());
            resp.setRetn(330);
            resp.setDesc("descriptor length does not match expected required length");
            return resp;
        } catch (Exception ex) {
            logger.info("error querying certificate lodgement details. See error log - [{}]", login.getUserId());
            logger.error("error querying certificate lodgement details - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to query certificate lodgement details. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     * processes request to confirm verification of certificate details
     *
     * @param login the user details
     * @param certVerification the certificate to be verified
     * @return response to the confirmation of certificate verification
     */
    public Response verifyCertificateConfirmation_Request(Login login, CertificateVerification certVerification) {
        Response resp = new Response();
        logger.info("request to verify certificate invoked by ", login.getUserId());
        CertificateVerification cv_model = new CertificateVerification();
        List<CertificateVerification> cv_model_list_out = new ArrayList<>();
        boolean exists = false;
        try {
            exists = hd.checkCertByCertNo(certVerification.getCertificateId());
            if (exists) {
                if (certVerification.getCertificateId() > 0) {
                    if (certVerification.getStatus() != null && !"".equals(certVerification.getStatus()) && certVerification.getStatus().equals("irregular")) {
                        cv_model.setCertificateId(certVerification.getCertificateId());
                        cv_model.setNote(certVerification.getNote());
                        cv_model.setStatus(certVerification.getStatus());
                        cv_model_list_out.add(cv_model);
                    } else if (certVerification.getStatus() != null && !"".equals(certVerification.getStatus()) && !certVerification.getStatus().equals("irregular")) {
                        cv_model.setCertificateId(certVerification.getCertificateId());
                        cv_model.setStatus(certVerification.getStatus());
                        cv_model_list_out.add(cv_model);
                    }
                    resp.setRetn(0);
                    resp.setBody(cv_model_list_out);
                    return resp;
                }
                resp.setRetn(300);
                resp.setDesc("Certificate number not specified");
                logger.info("Certificate number not specified ", login.getUserId());
                return resp;
            }
            resp.setRetn(300);
            resp.setDesc("Certificate does not exists ");
            logger.info("Certificate does not exists ", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error verifying certificate. See error log - [{}]", login.getUserId());
            logger.error("error verifying certificate - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to certificate. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     * processes request for verification of certificate details
     *
     * @param login the user details
     * @param certVerification the certificate to be verified
     * @return response to the confirmation of certificate verification
     */
    public Response verifyCertificate_Request(Login login, CertificateVerification certVerification) {
        Response resp = new Response();
        logger.info("request to verify certificate invoked by ", login.getUserId());
        org.greenpole.hibernate.entity.CertificateVerification cv_hib = new org.greenpole.hibernate.entity.CertificateVerification();
        CertificateVerification cv_model = new CertificateVerification();
        boolean exists = false;
        boolean saved = false;
        try {
            exists = hd.checkCertByCertNo(certVerification.getCertificateId());
            if (exists) {//checks for certificate existence
                logger.info("certificate checks out by [{}] ", login.getUserId());
                org.greenpole.hibernate.entity.Certificate cert = hd.getCertByCertNumber(certVerification.getCertificateId());
                /*if (cert.getCertificateNumber() > 0) {
                if (certVerification.getStatus() != null && !"".equals(certVerification.getStatus()) && certVerification.getStatus().equals("irregular")) {
                cv_hib.setCertificate(cert);
                cv_hib.setNote(certVerification.getNote());
                cv_hib.setStatus(certVerification.getStatus());
                saved = hd.saveCertificateVerification(cv_hib);
                } else if (certVerification.getStatus() != null && !"".equals(certVerification.getStatus()) && !certVerification.getStatus().equals("irregular")) {
                cv_hib.setCertificate(cert);
                cv_hib.setStatus(certVerification.getStatus());
                saved = hd.saveCertificateVerification(cv_hib);
                }
                if (saved) {
                resp.setRetn(0);
                resp.setDesc("Successful");
                logger.info("Successful ", login.getUserId());
                return resp;
                }
                if (!saved) {
                resp.setRetn(300);
                resp.setDesc("Certification verification failed");
                logger.info("Certification verification failed ", login.getUserId());
                return resp;
                }
                }*/
                resp.setRetn(300);
                resp.setDesc("Certificate number not specified");
                logger.info("Certificate number not specified ", login.getUserId());
                return resp;
            }
            resp.setRetn(300);
            resp.setDesc("Certificate does not exists ");
            logger.info("Certificate does not exists ", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error verifying certificate. See error log - [{}]", login.getUserId());
            logger.error("error verifying certificate - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to certificate. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

}
