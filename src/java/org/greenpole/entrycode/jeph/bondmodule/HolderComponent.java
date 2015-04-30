/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph.bondmodule;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import java.util.Date;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.model.PhoneNumber;
import org.greenpole.entity.model.holder.Holder;
import org.greenpole.entity.model.jeph.models.payment.UnitTransfer;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.entrycode.jeph.mocks.SignatureProperties;
import org.greenpole.hibernate.entity.*;
import org.greenpole.hibernate.entity.HolderCompanyAccount;
import org.greenpole.hibernate.entity.HolderPhoneNumber;
import org.greenpole.hibernate.entity.HolderPostalAddress;
import org.greenpole.hibernate.entity.HolderResidentialAddress;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.HolderComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.hibernate.query.impl.HolderComponentQueryImpl;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.Notification;
import org.greenpole.util.properties.NotifierProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jephthah Sadare
 * @version 1.0 Business requirement implementation to do with Holders
 */
public class HolderComponent {

    private final HolderComponentQuery hcq;
    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();
    private static final Logger logger = LoggerFactory.getLogger(HolderComponent.class);

    public HolderComponent() {
        this.hcq = new HolderComponentQueryImpl();
    }

    public Response createHolder_Request(Login login, String authenticator, Holder hold) {
        logger.info("request to create holder [{}] [{}] : Login - [{}]", hold.getFirstName(), hold.getLastName(), login.getUserId());

        Response res = new Response();
        NotificationWrapper wrapper;
        SignatureProperties signProp;
        QueueSender queue;
        NotifierProperties prop;

        if ((!hold.getFirstName().isEmpty() || !"".equals(hold.getFirstName())) && (!hold.getLastName().isEmpty() || !"".equals(hold.getLastName()))) {
            if (hold.getType().isEmpty() || "".equals(hold.getType())) {
                res.setRetn(200);
                res.setDesc("holder account first name is empty");
                logger.info("holder account is empty");

            } else {
                wrapper = new NotificationWrapper();
                prop = new NotifierProperties(HolderComponent.class);
                queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());

                logger.info("holder does not exits - [{}] [{}]", hold.getFirstName(), hold.getLastName());

                List<Holder> holdList = new ArrayList<>();
                holdList.add(hold);

                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription("Authenticate creation of holder account, " + hold.getFirstName() + " " + hold.getLastName());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(holdList);
                res = queue.sendAuthorisationRequest(wrapper);
                logger.info("notification forwarded to queue - notification code: [{}]", wrapper.getCode());
            }
        }
        return res;
    }

    public Response createHolder_Authorise(Login login, String notificationCode) {
        logger.info("request persist holder details : Login - [{}]", login.getUserId());
        Response res = new Response();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        logger.info("Holder creation authorised - [{}]", notificationCode);

        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<Holder> holdList = (List<Holder>) wrapper.getModel();
            Holder holdModel = holdList.get(0);
            org.greenpole.hibernate.entity.Holder holdEntity = new org.greenpole.hibernate.entity.Holder();
            // holdEntity.setId(holdModel.getId());
            // holdEntity.setHolder(holdModel.getHolder());
            holdEntity.setFirstName(holdModel.getFirstName());
            holdEntity.setLastName(holdModel.getLastName());
            holdEntity.setMiddleName(holdModel.getMiddleName());
            holdEntity.setType(holdModel.getType());
            holdEntity.setGender(holdModel.getGender());
            holdEntity.setDob(formatter.parse(holdModel.getDob()));
            holdEntity.setChn(holdModel.getChn());

            boolean created;
            // determine if residential address was set or postal address
            // if ("".equals(holdModel.getHolderResidentialAddresses().getAddressLine1()) || "".equals(holdModel.getHolderResidentialAddresses().getAddressLine2())) {
            if (holdModel.getHolderResidentialAddresses().isEmpty()) {
                created = hcq.createHolderAccount(holdEntity, retrieveHolderCompanyAccount(holdModel, false), retrieveHolderPostalAddress(holdModel, false), retrieveHolderPhoneNumber(holdModel, false));
            } else {
                created = hcq.createHolderAccount(holdEntity, retrieveHolderCompanyAccount(holdModel, false), retrieveHolderResidentialAddress(holdModel, false), retrieveHolderPhoneNumber(holdModel, false));
            }
            if (created) {
                res.setRetn(0);
                res.setDesc("Successful Persistence");
                return res;
            } else {
                res.setRetn(200);
                res.setDesc("An error occured persisting the data residential and postal addresses are empty");
                return res;
            }

        } catch (JAXBException ex) {
            // TODO: catch other types of exception
            res.setRetn(100);
            res.setDesc("error loading notification xml file. See error log");
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);
        } catch (Exception ex) {
            res.setRetn(100);
            res.setDesc("error. See error log");
            logger.info("error. See error log");
            logger.error("error - ", ex);
        }
        return res;
    }

    public Response uploadHolderSignature_Request(Login login, String authenticator,
            org.greenpole.entrycode.jeph.models.HolderSignature holderSign, byte[] holderSignImage) {
        logger.info("request to upload holder signature: Login - [{}]", login.getUserId());

        Response res = new Response();
        NotificationWrapper wrapper;
        SignatureProperties signProp;
        QueueSender queue;
        NotifierProperties prop;

        wrapper = new NotificationWrapper();
        signProp = new SignatureProperties();

        prop = new NotifierProperties(HolderComponent.class);
        queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
        if (holderSignImage.length <= 2097152) {
            try {
                InputStream inputByteImage;
                inputByteImage = new ByteArrayInputStream(holderSignImage);
                BufferedImage byteToImage = ImageIO.read(inputByteImage);
                String signatureFileName = createSignatureFileName();
                String filePath = signProp.getSignaturePath() + signatureFileName + ".jpg";
                ImageIO.write(byteToImage, "jpg", new File(filePath));
                holderSign.setSignaturePath(filePath);
                List<org.greenpole.entrycode.jeph.models.HolderSignature> holderListSignature = new ArrayList<>();
                holderListSignature.add(holderSign);
                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription("Authenticate creation of holder singature");
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(holderListSignature);
                res = queue.sendAuthorisationRequest(wrapper);
                logger.info("notification forwarded to queue - notification code: [{}]", wrapper.getCode());

            } catch (IOException ioex) {
                res.setRetn(201);
                res.setDesc("Error in saving image - " + ioex);
                logger.info("Error in saving image - ", ioex);
            }
        } else {
            res.setRetn(201);
            res.setDesc("Error in saving image - size larger than 2 megabytes");
            logger.info("Error in saving image - size larger than 2 megabytes");
        }
        return res;
    }

    public Response uploadHolderSignature_Authorise(Login login, String notificationCode) {
        logger.info("Authorisation to persist holder signature: Login - [{}]", login.getUserId());
        Response res = new Response();
        logger.info("Holder creation authorised - [{}]", notificationCode);
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<org.greenpole.entrycode.jeph.models.HolderSignature> holdSigntureList = (List<org.greenpole.entrycode.jeph.models.HolderSignature>) wrapper.getModel();
            org.greenpole.entrycode.jeph.models.HolderSignature holderSignModel = holdSigntureList.get(0);
            // org.greenpole.hibernate.entity.HolderSignature holderSignEntity = hcq.getHolderSignature(holderSignModel.getHolderId());
            org.greenpole.hibernate.entity.HolderSignature holderSignEntity = new org.greenpole.hibernate.entity.HolderSignature();
            holderSignEntity.setHolderSignaturePrimary(false);
            // hcq.createOrUpdateHolderSingature(holderSignEntity);
            // org.greenpole.hibernate.entity.HolderSignature holderSignEntity = hcq.getHolderSignature(holderSignModel.getHolderId());
            org.greenpole.hibernate.entity.HolderSignature holderSignEntityUpdate = new org.greenpole.hibernate.entity.HolderSignature();
            holderSignEntityUpdate.setHolderSignaturePrimary(true);
            holderSignEntityUpdate.setTitle(holderSignModel.getTitle());
            holderSignEntityUpdate.setSignaturePath(holderSignModel.getSignaturePath());
            // hcq.createOrUpdateHolderSingature(holderSignEntityUpdate);            
            res.setRetn(0);
            res.setDesc("Successful Persistence - Holder Signature");
            return res;

        } catch (JAXBException ex) {
            // TODO: catch other types of exception
            res.setRetn(100);
            res.setDesc("error loading notification xml file. See error log");
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);
        } catch (Exception ex) {
            res.setRetn(100);
            res.setDesc("error. See error log");
            logger.info("error. See error log");
            logger.error("error - ", ex);
        }
        return res;
    }

    public Response queryHolderSignature_Request(Login login, String authenticator,
            org.greenpole.entrycode.jeph.models.HolderSignature holderSign) {
        logger.info("request to query holder signature for [{}] : Login - [{}]", holderSign.getHolderId(), login.getUserId());

        Response res = new Response();
        NotificationWrapper wrapper;
        SignatureProperties signProp;
        QueueSender queue;
        NotifierProperties prop;
        boolean holderIdExist;

        if (holderSign.getHolderId() <= 0) {
            // holderIdExist = hcq.chkHolder(holderSign.getHolderId());
            holderIdExist = true;
            if (holderIdExist) {
                wrapper = new NotificationWrapper();
                prop = new NotifierProperties(HolderComponent.class);
                queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
                List<org.greenpole.entrycode.jeph.models.HolderSignature> holderSignList = new ArrayList<>();
                holderSignList.add(holderSign);
                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription("Authenticate query for signature - Holder id: " + holderSign.getHolderId());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(holderSignList);
                res = queue.sendAuthorisationRequest(wrapper);
                logger.info("notification forwarded to queue - notification code: [{}]", wrapper.getCode());
            } else {
                res.setRetn(201);
                res.setDesc("Error: No signature exists for holder");
                logger.info("Error: No signature exists for holder");
            }

        } else {
            res.setRetn(200);
            res.setDesc("Error: Holder Id should not be empty");
        }

        return res;
    }

    public Response queryHolderSignature_Authorise(Login login, String notificationCode) {
        logger.info("request authorisation to query holder signature: Login - [{}]", login.getUserId());
        Response res = new Response();
        logger.info("Holder creation authorised - [{}]", notificationCode);
        try {
            logger.info("Holder creation authorised - [{}]", notificationCode);
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<org.greenpole.entrycode.jeph.models.HolderSignature> holdSignList = (List<org.greenpole.entrycode.jeph.models.HolderSignature>) wrapper.getModel();
            // org.greenpole.hibernate.entity.HolderSignature holdSignEntity = hcq.getHolderSignature(holdSignList.get(0).getHolderId());
            org.greenpole.hibernate.entity.HolderSignature holdSignEntity = new org.greenpole.hibernate.entity.HolderSignature();
            org.greenpole.entrycode.jeph.models.HolderSignature holderSignSend = new org.greenpole.entrycode.jeph.models.HolderSignature();
            holderSignSend.setTitle(holdSignEntity.getTitle());
            byte[] signatureImage = readSignatureFile(holdSignEntity.getSignaturePath());
            holderSignSend.setSignImg(signatureImage);
            holdSignList.clear();
            holdSignList.add(holderSignSend);
            res.setRetn(0);
            res.setDesc("Holder signature details");
            res.setBody(holdSignList);
        } catch (JAXBException jbex) {
        }
        return res;
    }

    public byte[] readSignatureFile(String signaturePath) {
        ByteArrayOutputStream signByteOut = null;
        byte[] signInByte = null;
        try {
            File signFile = new File(signaturePath);
            BufferedImage signImg = ImageIO.read(signFile);
            signByteOut = new ByteArrayOutputStream();
            ImageIO.write(signImg, "jpg", signByteOut);
            signByteOut.flush();
            signInByte = signByteOut.toByteArray();
        } catch (IOException ioe) {
        } finally {
            try {
                signByteOut.close();
            } catch (Exception e) {
            }
        }
        return signInByte;
    }

    public Response createBondHolderAccount_Request(Login login, String authenticator, Holder hold) {
        logger.info("request to create bond holder [{}] [{}] : Login - [{}]", hold.getFirstName(), hold.getLastName(), login.getUserId());

        Response res = new Response();
        NotificationWrapper wrapper;
        SignatureProperties signProp;
        QueueSender queue;
        NotifierProperties prop;

        if (!hold.getFirstName().isEmpty() || hold.getFirstName() != null) {
            if (hold.getType().isEmpty() || hold.getType() == null) {
                res.setRetn(200);
                res.setDesc("holder account first name is empty");
                logger.info("holder account is empty");
            } else {
                wrapper = new NotificationWrapper();
                prop = new NotifierProperties(HolderComponent.class);
                queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());

                logger.info("holder does not exits - [{}] [{}]", hold.getFirstName(), hold.getLastName());

                List<Holder> holdList = new ArrayList<>();
                holdList.add(hold);

                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription("Authenticate creation of holder account, " + hold.getFirstName() + " " + hold.getLastName());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(holdList);
                res = queue.sendAuthorisationRequest(wrapper);
                logger.info("notification forwarded to queue - notification code: [{}]", wrapper.getCode());
            }
        }
        return res;
    }

    public Response createBondHolderAccount_Authorise(Login login, String notificationCode) {
        logger.info("request persist bond holder details : Login - [{}]", login.getUserId());
        Response res = new Response();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        logger.info("Holder creation authorised - [{}]", notificationCode);

        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<Holder> holdList = (List<Holder>) wrapper.getModel();
            Holder holdModel = holdList.get(0);
            org.greenpole.hibernate.entity.Holder holdEntity = new org.greenpole.hibernate.entity.Holder();
            // holdEntity.setId(holdModel.getId());
            // holdEntity.setHolder(holdModel.getHolder());
            holdEntity.setFirstName(holdModel.getFirstName());
            holdEntity.setLastName(holdModel.getLastName());
            holdEntity.setMiddleName(holdModel.getMiddleName());
            holdEntity.setType(holdModel.getType());
            holdEntity.setGender(holdModel.getGender());
            holdEntity.setDob(formatter.parse(holdModel.getDob()));
            holdEntity.setChn(holdModel.getChn());

            boolean created;
            // if ("".equals(holdModel.getHolderResidentialAddresses().getAddressLine1()) || "".equals(holdModel.getHolderResidentialAddresses().getAddressLine2())) {
            if (holdModel.getHolderResidentialAddresses().isEmpty()) {
                created = hcq.createHolderAccount(holdEntity, retrieveHolderBondAccount(holdModel, false), retrieveHolderPostalAddress(holdModel, false), retrieveHolderPhoneNumber(holdModel, false));
            } else {
                created = hcq.createHolderAccount(holdEntity, retrieveHolderBondAccount(holdModel, false), retrieveHolderResidentialAddress(holdModel, false), retrieveHolderPhoneNumber(holdModel, false));
            }
            if (created) {
                res.setRetn(0);
                res.setDesc("Successful Persistence");
                return res;
            } else {
                res.setRetn(200);
                res.setDesc("An error occured persisting the data residential and postal addresses are empty");
                return res;
            }

        } catch (JAXBException ex) {
            // TODO: catch other types of exception
            res.setRetn(100);
            res.setDesc("error loading notification xml file. See error log");
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);
        } catch (Exception ex) {
            res.setRetn(100);
            res.setDesc("error. See error log");
            logger.info("error. See error log");
            logger.error("error - ", ex);
        }
        return res;
    }

    public Response transposeHolderName_Request(Login login, String authenticator, Holder hold) {
        logger.info("request to transpose holder signature: Login - [{}]", login.getUserId());
        Response res = new Response();
        NotificationWrapper wrapper;
        SignatureProperties signProp;
        QueueSender queue;
        NotifierProperties prop;

        if (hold.getFirstName() != null || !"".equals(hold.getFirstName())) {
            if (hold.getLastName() != null || !"".equals(hold.getLastName())) {
                if (hold.getType() == null || "".equals(hold.getType())) {
                    res.setRetn(200);
                    res.setDesc("Error: holder account type should not be empty");
                    logger.info("Error: holder account type should not be empty");
                } else {
                    wrapper = new NotificationWrapper();
                    prop = new NotifierProperties(HolderComponent.class);
                    queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
                    logger.info("holder does not exits - [{}] [{}]", hold.getFirstName(), hold.getLastName());
                    List<Holder> holdList = new ArrayList<>();
                    holdList.add(hold);
                    wrapper.setCode(Notification.createCode(login));
                    wrapper.setDescription("Authenticate holder transpose request, " + hold.getFirstName() + " " + hold.getLastName());
                    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                    wrapper.setFrom(login.getUserId());
                    wrapper.setTo(authenticator);
                    wrapper.setModel(holdList);
                    res = queue.sendAuthorisationRequest(wrapper);
                    logger.info("notification forwarded to queue - notification code: [{}]", wrapper.getCode());
                }
            } else {
                res.setRetn(200);
                res.setDesc("Error: holder last name should not be empty");
                logger.info("Error: holder last name should not be empty");
            }
        } else {
            res.setRetn(200);
            res.setDesc("Error: holder first name should not be empty");
            logger.info("Error: holder first name should not be empty");
        }
        return res;
    }

    public Response transposeHolderName_Authorise(Login login, String notificationCode) {
        logger.info("request to edit transposed holder full name : Login - [{}]", login.getUserId());
        Response res = new Response();
        org.greenpole.hibernate.entity.Holder holdEntity;
        logger.info("Holder creation authorised - [{}]", notificationCode);
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<Holder> holdList = (List<Holder>) wrapper.getModel();
            Holder holdModel = holdList.get(0);
            // holdEntity = getHolder(holdModel.getHolderId());
            holdEntity = new org.greenpole.hibernate.entity.Holder();
            holdEntity.setFirstName(holdModel.getFirstName());
            holdEntity.setLastName(holdModel.getLastName());
            // hcq.updateHolder(holdEntity);
        } catch (JAXBException ex) {
            res.setRetn(100);
            res.setDesc("error loading notification xml file. See error log");
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);
        } catch (Exception ex) {
            res.setRetn(100);
            res.setDesc("error. See error log");
            logger.info("error. See error log");
            logger.error("error - ", ex);
        }
        return res;
    }

    public Response shareUnitTransfer_Request(Login login, String authenticator, UnitTransfer transfer) {
        logger.info("request to transfer shares from [{}] to [{}]", transfer.getHolderIdFrom(), transfer.getHolderIdTo());
        Response res = new Response();
        NotificationWrapper wrapper;
        QueueSender queue;
        NotifierProperties prop;

        boolean holderFromExist;
        boolean holderToExist;
        boolean chkHCAcctFrom;
        org.greenpole.hibernate.entity.Holder toHolder;
        org.greenpole.hibernate.entity.Holder fromHolder;
        org.greenpole.hibernate.entity.HolderCompanyAccount toHCAcct;
        org.greenpole.hibernate.entity.HolderCompanyAccount fromHCAcct;
        // boolean holderFromExist = hcq.checkHolderByHolderId(transfer.getHolderIdFrom());        
        holderFromExist = true;
        if (holderFromExist) {
            // fromHolder = hcq.getHolder(transfer.getHolderIdFrom());
            fromHolder = new org.greenpole.hibernate.entity.Holder();
            // chkHCAcctFrom = hcq.checkHolderCompanyAccount(transfer.getClientCompanyIdFrom(), transfer.getHolderIdFrom());
            chkHCAcctFrom = true;
            if (chkHCAcctFrom) {
                // hCompAcctFrom = getHolderCompanyAccount(transfer.getClientCompanyIdFrom(), transfer.getHolderIdFrom());
                fromHCAcct = new org.greenpole.hibernate.entity.HolderCompanyAccount();
                if (!"".equals(fromHolder.getChn()) || fromHolder.getChn() != null) {
                    if ("".equals(fromHCAcct.getChn()) || fromHCAcct.getChn() == null) {
                        fromHCAcct.setChn(fromHolder.getChn());
                    }
                    if (fromHCAcct.getShareUnits() < transfer.getUnits()) {
                        // boolean holderToExist = hcq.checkHolderByHolderId(transfer.getHolderIdTo());
                        holderToExist = true;
                        if (holderToExist) {
                            // toHolder = hcq.getHolderByHolderId(transfer.getHolderIdFrom());
                            toHolder = new org.greenpole.hibernate.entity.Holder();
                            if (!"".equals(toHolder.getChn()) || toHolder.getChn() != null) {
                                wrapper = new NotificationWrapper();
                                prop = new NotifierProperties(HolderComponent.class);
                                queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
                                List<UnitTransfer> transferObj = new ArrayList<>();
                                transferObj.add(transfer);
                                wrapper.setCode(Notification.createCode(login));
                                wrapper.setDescription("Authenticate " + transfer.getUnits() + " share transfer request for, " + transfer.getHolderIdFrom() + " " + transfer.getHolderIdTo());
                                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                                wrapper.setFrom(login.getUserId());
                                wrapper.setTo(authenticator);
                                wrapper.setModel(transferObj);
                                res = queue.sendAuthorisationRequest(wrapper);
                                logger.info("notification forwarded to queue - notification code: [{}]", wrapper.getCode());
                            } else {
                                wrapper = new NotificationWrapper();
                                prop = new NotifierProperties(HolderComponent.class);
                                queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
                                wrapper.setCode(Notification.createCode(login));
                                wrapper.setDescription("Holder does not have CHN");
                                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                                wrapper.setFrom(login.getUserId());
                                wrapper.setTo(authenticator);
                                res = queue.sendAuthorisationRequest(wrapper);
                                logger.info("notification forwarded to queue - notification code: [{}]", wrapper.getCode());
                            }
                        } else {
                            res.setRetn(202);
                            res.setDesc("Create holder account for share unit transfer recipient");
                            logger.info("Create holder account for share unit transfer recipient");
                        }
                    } else {
                        res.setRetn(202);
                        res.setDesc("Insufficient share unit for transfer - transaction rejected");
                        logger.info("Insufficient share unit for transfer - transaction rejected");
                    }
                } else {
                    res.setRetn(202);
                    res.setDesc("Holder does not have CHN - transaction terminated");
                    logger.info("Holder does not have CHN - transaction terminated");
                }
            } else {
                res.setRetn(202);
                res.setDesc("Error: holder company account does not exist - transaction terminated");
                logger.info("Error: holder company account does not exist - transaction terminated");
            }
        } else {
            res.setRetn(202);
            res.setDesc("Error: Holder transfering shares does not exist - transaction terminated");
            logger.info("Error: Holder transfering shares does not exist - transaction terminated");
        }
        return res;
    }

    public Response shareUnitTransfer_Authorise(Login login, String notificationCode) {
        logger.info("share transfer authorisation request", login.getUserId());
        Response res = new Response();
        logger.info("Holder creation authorised - [{}]", notificationCode);
        boolean chkHCAcctToExist;
        boolean chkHCAcctFromExist;
        org.greenpole.hibernate.entity.HolderCompanyAccount compAcctFrom;
        org.greenpole.hibernate.entity.HolderCompanyAccount compAcctTo;
        org.greenpole.hibernate.entity.Holder fromHolder;
        org.greenpole.hibernate.entity.Holder toHolder;
        org.greenpole.entity.model.holder.Holder toHolderModel;
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<UnitTransfer> transferList = (List<UnitTransfer>) wrapper.getModel();
            UnitTransfer unitTransferModel = transferList.get(0);
            // boolean holderFromExist = hcq.checkHolderByHolderId(unitTransferModel.getHolderIdFrom());
            boolean holderFromExist = true;
            if (holderFromExist) {
                // fromHolder = hcq.getHolder(unitTransferModel.getHolderIdFrom());
                fromHolder = new org.greenpole.hibernate.entity.Holder();
                // chkHCAcctFromExist = hcq.checkHolderCompanyAccount(unitTransferModel.getHolderIdFrom(), unitTransferModel.getClientCompanyIdFrom());
                chkHCAcctFromExist = true;
                if (chkHCAcctFromExist) {
                    // compAcctFrom = hcq.getHolderCompanyAccount(unitTransferModel.getHolderIdFrom(), unitTransferModel.getClientCompanyIdFrom());
                    compAcctFrom = new org.greenpole.hibernate.entity.HolderCompanyAccount();
                    if (!"".equals(compAcctFrom.getChn()) || compAcctFrom.getChn() != null) { // W A T C H
                        // boolean holderToExist = hcq.checkHolderByHolderId(unitTransferModel.getHolderIdFrom());
                        boolean holderToExist = true;
                        if (holderToExist) {
                            // toHolder = hcq.getHolder(unitTransferModel.getHolderIdTo());
                            toHolder = new org.greenpole.hibernate.entity.Holder();
                            // chkHCAcctToExist = hcq.checkHolderCompAccount(unitTransferModel.getHolderIdTo(), unitTransferModel.getClientCompanyIdTo());
                            chkHCAcctToExist = true;
                            if (chkHCAcctToExist) {
                                // compAcctTo = hcq.getHolderCompanyAccount(unitTransferModel.getHolderIdTo(), unitTransferModel.getClientCompanyIdTo());
                                compAcctTo = new org.greenpole.hibernate.entity.HolderCompanyAccount();
                                if (!"".equals(toHolder.getChn()) || toHolder.getChn() != null) {
                                    if ("".equals(compAcctTo.getChn()) || compAcctTo.getChn() == null) {
                                        compAcctTo.setChn(toHolder.getChn());
                                    }
                                    if (compAcctFrom.getShareUnits() < unitTransferModel.getUnits()) {
                                        logger.info("Insufficient unit of shares for transfer operation");
                                        res.setRetn(202);
                                        res.setDesc("Insufficient unit of shares for transfer operation");
                                    } else {
                                        compAcctFrom.setShareUnits((int) (compAcctFrom.getShareUnits() - unitTransferModel.getUnits()));
                                        compAcctTo.setShareUnits((int) (compAcctTo.getShareUnits() + unitTransferModel.getUnits()));
                                        hcq.createHolderCompanyAccount(compAcctFrom);
                                        hcq.createHolderCompanyAccount(compAcctTo);
                                        logger.info("share unit of [{}] transfered from holder id [{}] to holder id [{}]",
                                                unitTransferModel.getUnits(), unitTransferModel.getHolderIdFrom(), unitTransferModel.getHolderIdTo());
                                        res.setRetn(0);
                                        res.setDesc("Share units transfer operation successful");
                                    }
                                } else {
                                    res.setRetn(204);
                                    res.setDesc("CHN not available - Certificate for holder created");
                                    logger.info("CHN not available - Certificate for holder created");
                                }
                            } else {
                                String resDesc = "";
                                res.setDesc("Creating holder company account for holder " + unitTransferModel.getHolderIdTo());
                                logger.info("Creating holder company account for holder [{}]", unitTransferModel.getHolderIdTo());
                                toHolderModel = new org.greenpole.entity.model.holder.Holder(toHolder.getId(),
                                        toHolder.getHolderAcctNumber(), toHolder.getChn(), toHolder.getFirstName(),
                                        toHolder.getMiddleName(), toHolder.getLastName(), toHolder.getType(),
                                        toHolder.getGender(), toHolder.getDob().toString(), false, false, true,
                                        toHolder.getPryAddress(), (List) toHolder.getHolderPhoneNumbers(),
                                        (List) toHolder.getHolderResidentialAddresses(),
                                        (List) toHolder.getHolderCompanyAccounts(),
                                        (List) toHolder.getHolderBondAccounts(),
                                        (List) toHolder.getHolderPostalAddresses(),
                                        (List) toHolder.getHolderEmailAddresses(),
                                        (org.greenpole.entity.model.stockbroker.Stockbroker) toHolder.getStockbrokers());
                                hcq.createHolderCompanyAccount(retrieveHolderCompanyAccount(toHolderModel, true));
                                resDesc = res.getDesc();
                                res.setDesc(resDesc + "\nHolder company account for holder " + unitTransferModel.getHolderIdTo() + " created");
                                // compAcctTo = hcq.getHolderCompanyAccount(unitTransferModel.getHolderIdTo(), unitTransferModel.getClientCompanyIdTo());
                                compAcctTo = new org.greenpole.hibernate.entity.HolderCompanyAccount();
                                if (compAcctFrom.getShareUnits() < unitTransferModel.getUnits()) {
                                    logger.info("Insufficient unit of shares for transfer operation");
                                    resDesc = res.getDesc();
                                    res.setRetn(202);
                                    res.setDesc(resDesc + "\nInsufficient unit of shares for transfer operation");
                                } else {
                                    compAcctFrom.setShareUnits((int) (compAcctFrom.getShareUnits() - unitTransferModel.getUnits()));
                                    compAcctTo.setShareUnits((int) (compAcctTo.getShareUnits() + unitTransferModel.getUnits()));
                                    hcq.createHolderCompanyAccount(compAcctFrom);
                                    hcq.createHolderCompanyAccount(compAcctTo);
                                    logger.info("share unit of [{}] transfered from holder id [{}] to holder id [{}]",
                                            unitTransferModel.getUnits(), unitTransferModel.getHolderIdFrom(), unitTransferModel.getHolderIdTo());
                                    res.setRetn(0);
                                    res.setDesc(resDesc + "\nShare units transfer operation successful");
                                }
                                res.setRetn(203);
                                res.setDesc("Error: Holder does not have a holder company account - create company account for holder");
                                logger.info("Error: Holder does not have a holder company account - create company account for holder");
                            }
                        } else {
                            res.setRetn(203);
                            res.setDesc("Error: Holder does not exist - create holder account");
                            logger.info("Error: Holder does not exist - create holder account");
                        }
                    } else {
//                        if (!"".equals(fromHolder.getChn()) || fromHolder.getChn() != null) {
//                            compAcctFrom.setChn(fromHolder.getChn());
//                            // boolean holderToExist = hcq.checkHolderByHolderId(unitTransferModel.getHolderIdFrom());
//                            boolean holderToExist = true;
//                            if (holderToExist) {
//                                // toHolder = hcq.getHolder(unitTransferModel.getHolderIdTo());
//                                toHolder = new org.greenpole.hibernate.entity.Holder();
//                                // chkHCAcctToExist = hcq.checkHolderCompAccount(unitTransferModel.getHolderIdTo(), unitTransferModel.getClientCompanyIdTo());
//                                chkHCAcctToExist = true;
//                                if (chkHCAcctToExist) {
//                                    // compAcctTo = hcq.getHolderCompanyAccount(unitTransferModel.getHolderIdTo(), unitTransferModel.getClientCompanyIdTo());
//                                    compAcctTo = new org.greenpole.hibernate.entity.HolderCompanyAccount();
//                                    if (!"".equals(toHolder.getChn()) || toHolder.getChn() != null) {
//                                        if ("".equals(compAcctTo.getChn()) || compAcctTo.getChn() == null) {
//                                            compAcctTo.setChn(toHolder.getChn());
//                                        }
//                                        if (compAcctFrom.getShareUnits() < unitTransferModel.getUnits()) {
//                                            logger.info("Insufficient unit of shares for transfer operation");
//                                            res.setRetn(202);
//                                            res.setDesc("Insufficient unit of shares for transfer operation");
//                                        } else {
//                                            compAcctFrom.setShareUnits((int) (compAcctFrom.getShareUnits() - unitTransferModel.getUnits()));
//                                            compAcctTo.setShareUnits((int) (compAcctTo.getShareUnits() + unitTransferModel.getUnits()));
//                                            hcq.createHolderCompanyAccount(compAcctFrom);
//                                            hcq.createHolderCompanyAccount(compAcctTo);
//                                            logger.info("share unit of [{}] transfered from holder id [{}] to holder id [{}]",
//                                                    unitTransferModel.getUnits(), unitTransferModel.getHolderIdFrom(), unitTransferModel.getHolderIdTo());
//                                            res.setRetn(0);
//                                            res.setDesc("Share units transfer operation successful");
//                                        }
//                                    } else {
//                                        res.setRetn(204);
//                                        res.setDesc("CHN not available - Certificate for holder created");
//                                        logger.info("CHN not available - Certificate for holder created");
//                                    }
//                                } else {
//                                    String resDesc = "";
//                                    res.setDesc("Creating holder company account for holder " + unitTransferModel.getHolderIdTo());
//                                    logger.info("Creating holder company account for holder [{}]", unitTransferModel.getHolderIdTo());
//                                    toHolderModel = new org.greenpole.entity.model.holder.Holder(toHolder.getId(),
//                                            toHolder.getHolderAcctNumber(), toHolder.getChn(), toHolder.getFirstName(),
//                                            toHolder.getMiddleName(), toHolder.getLastName(), toHolder.getType(),
//                                            toHolder.getGender(), toHolder.getDob().toString(), false, false, true,
//                                            toHolder.getPryAddress(), (List) toHolder.getHolderPhoneNumbers(),
//                                            (List) toHolder.getHolderResidentialAddresses(),
//                                            (List) toHolder.getHolderCompanyAccounts(),
//                                            (List) toHolder.getHolderBondAccounts(),
//                                            (List) toHolder.getHolderPostalAddresses(),
//                                            (List) toHolder.getHolderEmailAddresses(),
//                                            (org.greenpole.entity.model.stockbroker.Stockbroker) toHolder.getStockbrokers());
//                                    hcq.createHolderCompanyAccount(retrieveHolderCompanyAccount(toHolderModel, true));
//                                    // compAcctTo = hcq.getHolderCompanyAccount(unitTransferModel.getHolderIdTo(), unitTransferModel.getClientCompanyIdTo());
//                                    compAcctTo = new org.greenpole.hibernate.entity.HolderCompanyAccount();
//                                    if (compAcctFrom.getShareUnits() < unitTransferModel.getUnits()) {
//                                        logger.info("Insufficient unit of shares for transfer operation");
//                                        resDesc = res.getDesc();
//                                        res.setRetn(202);
//                                        res.setDesc(resDesc + "\nInsufficient unit of shares for transfer operation");
//                                    } else {
//                                        compAcctFrom.setShareUnits((int) (compAcctFrom.getShareUnits() - unitTransferModel.getUnits()));
//                                        compAcctTo.setShareUnits((int) (compAcctTo.getShareUnits() + unitTransferModel.getUnits()));
//                                        hcq.createHolderCompanyAccount(compAcctFrom);
//                                        hcq.createHolderCompanyAccount(compAcctTo);
//                                        logger.info("share unit of [{}] transfered from holder id [{}] to holder id [{}]",
//                                                unitTransferModel.getUnits(), unitTransferModel.getHolderIdFrom(), unitTransferModel.getHolderIdTo());
//                                        res.setRetn(0);
//                                        res.setDesc(resDesc + "\nShare units transfer operation successful");
//                                    }
//                                    res.setRetn(203);
//                                    res.setDesc("Error: Holder does not have a holder company account - create company account for holder");
//                                    logger.info("Error: Holder does not have a holder company account - create company account for holder");
//                                }
//                            } else {
//                                res.setRetn(203);
//                                res.setDesc("Error: Holder does not exist - create holder account");
//                                logger.info("Error: Holder does not exist - create holder account");
//                            }
//                        } else {
                        res.setRetn(204);
                        res.setDesc("Holder transfering shares unit does not have CHN - transaction terminated");
                        logger.info("Holder transfering shares unit does not have CHN - transaction terminated");
                        //}
                    }
                } else {
                    res.setRetn(204);
                    res.setDesc("Holder transfering shares unit does not have a company account - transaction terminated");
                    logger.info("Holder transfering shares unit does not have a company account - transaction terminated");
                }
            } else {
                res.setRetn(203);
                res.setDesc("Error: Holder transfering shares unit does not exist - transaction terminated");
                logger.info("Error: Holder transfering shares unit does not exist - transaction terminated");
            }
        } catch (JAXBException ex) {
            res.setRetn(100);
            res.setDesc("error loading notification xml file. See error log");
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);
        }
        return res;
    }

    public Response bondTransfer_Request(Login login, String authenticator, UnitTransfer transfer) {
        logger.info("request to transfer bond units from holder id [{}] to holder id [{}]", transfer.getHolderIdFrom(), transfer.getHolderIdTo());
        Response res = new Response();
        NotificationWrapper wrapper;
        QueueSender queue;
        NotifierProperties prop;

        boolean holderToExist;
        boolean holderFromExist;
        boolean chkHBAcctFrom;
        org.greenpole.hibernate.entity.Holder toHolder;
        org.greenpole.hibernate.entity.Holder fromHolder;
        org.greenpole.hibernate.entity.HolderBondAccount toHBAEntity;
        org.greenpole.hibernate.entity.HolderBondAccount fromHBAEntity;
        // boolean holderFromExist = hcq.checkHolderByHolderId(transfer.getHolderIdFrom());        
        holderFromExist = true;
        if (holderFromExist) {
            // fromHolder = hcq.getHolderEntity(transfer.getHolderIdFrom());
            fromHolder = new org.greenpole.hibernate.entity.Holder();
            // chkHBAcctFrom = hcq.checkHolderBondAccount(transfer.getHolderIdFrom(), transfer.getBondOfferId());
            chkHBAcctFrom = true;
            // fromHBAEntity = hcq.getHolderBondAccount(transfer.getHolderIdFrom(), transfer.getBondOfferId());
            fromHBAEntity = new org.greenpole.hibernate.entity.HolderBondAccount();
            if (chkHBAcctFrom) {
                if (!"".equals(fromHolder.getChn()) || fromHolder.getChn() != null) {
                    if ("".equals(fromHBAEntity.getChn()) || fromHBAEntity.getChn() == null) {
                        fromHBAEntity.setChn(fromHolder.getChn());
                    }
                    // boolean holderToExist = hcq.checkHolderByHolderId(transfer.getHolderIdFrom());        
                    holderToExist = true;
                    if (holderToExist) {
                        // toHBAEntity = hcq.getHolderBondAccount(transfer.getHolderIdFrom(), transfer.getBondOfferId());
                        toHBAEntity = new org.greenpole.hibernate.entity.HolderBondAccount();
                        // toHolder = hcq.getHolderEntity(transfer.getHolderIdTo());
                        toHolder = new org.greenpole.hibernate.entity.Holder();
                        if (!"".equals(toHolder.getChn()) || toHolder.getChn() != null) {
                            if ("".equals(toHBAEntity.getChn()) || toHBAEntity.getChn() == null) {
                                toHBAEntity.setChn(toHolder.getChn());
                            }
                            wrapper = new NotificationWrapper();
                            prop = new NotifierProperties(HolderComponent.class);
                            queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
                            List<UnitTransfer> transferObj = new ArrayList<>();
                            transferObj.add(transfer);
                            wrapper.setCode(Notification.createCode(login));
                            wrapper.setDescription("Authenticate " + transfer.getUnits() + " bond unit transfer request for, " + transfer.getHolderIdFrom() + " " + transfer.getHolderIdTo());
                            wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                            wrapper.setFrom(login.getUserId());
                            wrapper.setTo(authenticator);
                            wrapper.setModel(transferObj);
                            res = queue.sendAuthorisationRequest(wrapper);
                            logger.info("notification forwarded to queue - notification code: [{}]", wrapper.getCode());
                        } else {
                            res.setRetn(204);
                            res.setDesc("Holder does not have CHN - transaction terminated");
                            logger.info("Holder does not have CHN - transaction terminated");
                        }
                    } else {
                        res.setRetn(203);
                        res.setDesc("Holder does not exist - Action: create holder");
                        logger.info("Holder does not exist - Action: create holder");
                    }
                } else {
                    res.setRetn(204);
                    res.setDesc("Holder does not have CHN - transaction terminated");
                    logger.info("Holder does not have CHN - transaction terminated");
                }
            } else {
                res.setRetn(205);
                res.setDesc("Bond account not available - transaction terminated");
                logger.info("Bond account not available - transaction terminated");
            }
        } else {
            res.setRetn(203);
            res.setDesc("Holder does not exist - transaction terminated");
            logger.info("Holder does not exist - transaction terminated");
        }
        return res;
    }

    public Response bondTransfer_Authorise(Login login, String notificationCode) {
        logger.info("share transfer authorisation reequest", login.getUserId());
        Response res = new Response();
        logger.info("Holder creation authorised - [{}]", notificationCode);
        boolean holderToExist;
        boolean holderFromExist;
        boolean bondAcctToExist;
        boolean bondAcctFromExist;
        org.greenpole.hibernate.entity.Holder toHolder;
        org.greenpole.hibernate.entity.Holder fromHolder;
        org.greenpole.hibernate.entity.HolderBondAccount bondAcctFrom;
        org.greenpole.hibernate.entity.HolderBondAccount bondAcctTo;
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<UnitTransfer> bondTransferList = (List<UnitTransfer>) wrapper.getModel();
            UnitTransfer unitTransferModel = bondTransferList.get(0);
            // boolean holderFromExist = hcq.checkHolderBondAccount(unitTransferModel.getHolderIdFrom());
            holderFromExist = true;
            if (holderFromExist) {
                // fromHolder = hcq.getHolderBondAccount(unitTransferModel.getHolderIdFrom());
                fromHolder = new org.greenpole.hibernate.entity.Holder();
                // bondAcctFromExist = hcq.chkHolderBondAccount(unitTransferModel.getHolderIdFrom(), unitTransferModel.getBondOfferId());
                bondAcctFromExist = true;
                if (bondAcctFromExist) {
                    // bondAcctFrom = hcq.getHolderBondAccount(unitTransferModel.getHolderIdFrom(), unitTransferModel.getBondOfferId());
                    bondAcctFrom = new org.greenpole.hibernate.entity.HolderBondAccount();
                    if (!"".equals(fromHolder.getChn()) || fromHolder.getChn() != null) {
                        if ("".equals(bondAcctFrom.getChn()) || bondAcctFrom.getChn() == null) {
                            bondAcctFrom.setChn(fromHolder.getChn());
                        }
                        // holderToExist = hcq.checkHolderBondAccount(unitTransferModel.getHolderIdFrom());
                        holderToExist = true;
                        if (holderToExist) {
                            // toHolder = hcq.getHolderBondAccount(unitTransferModel.getHolderIdFrom());
                            toHolder = new org.greenpole.hibernate.entity.Holder();
                            // bondAcctToExist = hcq.chkHolderBondAccount(unitTransferModel.getHolderIdFrom(), unitTransferModel.getBondOfferId());
                            bondAcctToExist = true;
                            if (bondAcctToExist) {
                                // bondAcctTo = hcq.getHolderBondAccount(unitTransferModel.getHolderIdTo(), unitTransferModel.getBondOfferId());
                                bondAcctTo = new org.greenpole.hibernate.entity.HolderBondAccount();
                                if (bondAcctFrom.getBondUnits() < unitTransferModel.getUnits()) {
                                    logger.info("Insufficient unit of shares for transfer operation");
                                    res.setRetn(203);
                                    res.setDesc("Insufficient unit of shares for transfer operation");
                                } else {
                                    double principalValue = unitTransferModel.getUnitPrice() * bondAcctFrom.getBondUnits();
                                    bondAcctFrom.setStartingPrincipalValue(bondAcctFrom.getStartingPrincipalValue() - principalValue);
                                    bondAcctFrom.setRemainingPrincipalValue(bondAcctFrom.getRemainingPrincipalValue() - principalValue);
                                    bondAcctFrom.setBondUnits(bondAcctFrom.getBondUnits() - unitTransferModel.getUnits());
                                    bondAcctTo.setStartingPrincipalValue(bondAcctTo.getStartingPrincipalValue() + principalValue);
                                    bondAcctTo.setRemainingPrincipalValue(bondAcctTo.getRemainingPrincipalValue() + principalValue);
                                    bondAcctTo.setBondUnits(bondAcctTo.getBondUnits() + unitTransferModel.getUnits());
                                    hcq.createHolderCompanyAccount(bondAcctFrom);
                                    hcq.createHolderCompanyAccount(bondAcctTo);
                                    logger.info("bonds unit of [{}] transfered", unitTransferModel.getUnits());
                                    res.setRetn(0);
                                    res.setDesc("Successful Persistence");
                                }
                            } else {
                                res.setRetn(205);
                                res.setDesc("Error: Holder bond account does not exist - transaction terminated");
                                logger.info("Error: Holder bond account does not exist - transaction terminated");
                            }
                        } else {
                            res.setRetn(205);
                            res.setDesc("Error: Holder does not exist - transaction terminated");
                            logger.info("Error: Holder does not exist - transaction terminated");
                        }
                    } else {
                        res.setRetn(205);
                        res.setDesc("Holder has no CHN - transaction terminated");
                        logger.info("Holder has no CHN - transaction terminated");
                    }
                } else {
                    res.setRetn(205);
                    res.setDesc("Error: holder bond account does not exits");
                    logger.info("Error: holder bond account does not exits");
                }
            } else {
                res.setRetn(205);
                res.setDesc("Holder does not exist - transaction terminated");
                logger.info("Holder does not exist - transaction terminated");
            }
        } catch (JAXBException ex) {
            // TODO: catch other types of exception
            res.setRetn(100);
            res.setDesc("error loading notification xml file. See error log");
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);
        } catch (Exception ex) {
            res.setRetn(100);
            res.setDesc("error. See error log");
            logger.info("error. See error log");
            logger.error("error - ", ex);
        }
        return res;
    }

    public Response editShareHolderDetails_Request(Login login, String authenticator, Holder holder) {
        logger.info("request to transpose holder signature: Login - [{}]", login.getUserId());
        Response res = new Response();
        NotificationWrapper wrapper;
        SignatureProperties signProp;
        QueueSender queue;
        NotifierProperties prop;

        return res;
    }

    public Response editShareHolderDetails_Authorise(Login login, String notificationCode) {
        Response res = new Response();

        return res;
    }

    public Response editBondHolderDetails_Request(Login login, String authenticator, Holder holder) {
        logger.info("request to transpose holder signature: Login - [{}]", login.getUserId());
        Response res = new Response();
        NotificationWrapper wrapper;
        SignatureProperties signProp;
        QueueSender queue;
        NotifierProperties prop;

        return res;
    }

    public Response editBondHolderDetails_Authorise(Login login, String notificationCode) {
        Response res = new Response();

        return res;
    }

    private HolderPostalAddress retrieveHolderPostalAddress(Holder holdModel, boolean newEntry) {
        org.greenpole.hibernate.entity.HolderPostalAddress postalAddressEntity = new org.greenpole.hibernate.entity.HolderPostalAddress();
        List<org.greenpole.entity.model.Address> hpaddyList = (List<org.greenpole.entity.model.Address>) holdModel.getHolderPostalAddresses();
        List<org.greenpole.hibernate.entity.HolderPostalAddress> returnHolderPostalAddress = new ArrayList<>();

        for (org.greenpole.entity.model.Address hpa : hpaddyList) {
            HolderPostalAddressId postalAddyId = new HolderPostalAddressId();
            if (newEntry) {
                postalAddyId.setHolderId(holdModel.getHolderId());
            }
            postalAddyId.setAddressLine1(hpa.getAddressLine1());
            postalAddyId.setState(hpa.getState());
            postalAddyId.setCountry(hpa.getCountry());
            postalAddressEntity.setId(postalAddyId);
            postalAddressEntity.setAddressLine2(Integer.parseInt(hpa.getAddressLine2()));
            postalAddressEntity.setAddressLine3(hpa.getAddressLine3());
            postalAddressEntity.setCity(hpa.getCity());
            postalAddressEntity.setPostCode(hpa.getPostCode());
            postalAddressEntity.setIsPrimary(hpa.isPrimaryAddress());
            returnHolderPostalAddress.add(postalAddressEntity);
        }
        return (HolderPostalAddress) returnHolderPostalAddress;
    }

    private HolderPhoneNumber retrieveHolderPhoneNumber(Holder holdModel, boolean newEntry) {

        org.greenpole.hibernate.entity.HolderPhoneNumber phoneNumberEntity = new org.greenpole.hibernate.entity.HolderPhoneNumber();
        List<org.greenpole.entity.model.PhoneNumber> phoneNumberList = holdModel.getHolderPhoneNumbers();
        List<org.greenpole.hibernate.entity.HolderPhoneNumber> returnPhoneNumber = new ArrayList<>();

        for (PhoneNumber pnList : phoneNumberList) {
            HolderPhoneNumberId phoneNoId = new HolderPhoneNumberId();
            if (newEntry) {
                phoneNoId.setHolderId(holdModel.getHolderId());
            }
            phoneNoId.setPhoneNumber(pnList.getPhoneNumber());
            phoneNumberEntity.setIsPrimary(pnList.isPrimaryPhoneNumber());
            phoneNumberEntity.setId(phoneNoId);
        }
        return (HolderPhoneNumber) returnPhoneNumber;
    }

    private HolderResidentialAddress retrieveHolderResidentialAddress(Holder holdModel, boolean newEntry) {
        org.greenpole.hibernate.entity.HolderResidentialAddress residentialAddressEntity = new org.greenpole.hibernate.entity.HolderResidentialAddress();
        List<org.greenpole.entity.model.Address> residentialAddressList = (List<org.greenpole.entity.model.Address>) holdModel.getHolderResidentialAddresses();
        List<org.greenpole.hibernate.entity.HolderResidentialAddress> returnResidentialAddress = new ArrayList();

        for (org.greenpole.entity.model.Address rAddy : residentialAddressList) {
            HolderResidentialAddressId rAddyId = new HolderResidentialAddressId();
            if (newEntry) {
                rAddyId.setHolderId(holdModel.getHolderId());
            }
            rAddyId.setAddressLine1(rAddy.getAddressLine1());
            rAddyId.setState(rAddy.getState());
            rAddyId.setCountry(rAddy.getCountry());

            residentialAddressEntity.setId(rAddyId);
            residentialAddressEntity.setAddressLine2(Integer.parseInt(rAddy.getAddressLine2()));
            residentialAddressEntity.setAddressLine3(rAddy.getAddressLine3());
            residentialAddressEntity.setAddressLine4(rAddy.getAddressLine4());
            residentialAddressEntity.setCity(rAddy.getCity());
            residentialAddressEntity.setPostCode(rAddy.getPostCode());

            returnResidentialAddress.add(residentialAddressEntity);
        }

        return (HolderResidentialAddress) returnResidentialAddress;

    }

    private HolderCompanyAccount retrieveHolderCompanyAccount(Holder holdModel, boolean newEntry) {
        org.greenpole.hibernate.entity.HolderCompanyAccount companyAccountEntity = new org.greenpole.hibernate.entity.HolderCompanyAccount();
        List<org.greenpole.entity.model.holder.HolderCompanyAccount> companyAccountList = holdModel.getHolderCompanyAccounts();
        List<org.greenpole.hibernate.entity.HolderCompanyAccount> returnCompanyAccountList = new ArrayList();

        for (org.greenpole.entity.model.holder.HolderCompanyAccount compAcct : companyAccountList) {
            HolderCompanyAccountId hCompAcctId = new HolderCompanyAccountId();
            if (newEntry) {
                hCompAcctId.setHolderId(holdModel.getHolderId());
            }
            // hCompAcctId.setHolderId(compAcct.getHolderId());
            hCompAcctId.setClientCompanyId(compAcct.getClientCompanyId());
            // companyAccountEntity.setBank(compAcct.getBankId());
            companyAccountEntity.setChn(compAcct.getChn());
            companyAccountEntity.setId(hCompAcctId);
            companyAccountEntity.setHolderCompAccPrimary(compAcct.isHolderCompAccPrimary());
            returnCompanyAccountList.add(companyAccountEntity);
        }
        return (HolderCompanyAccount) returnCompanyAccountList;
    }

    private String createSignatureFileName() {
        Date date = new Date();
        Random rand = new Random();
        int randomNumber = rand.nextInt(9999999);
        String fileName = randomNumber + "" + date.getTime();
        return fileName;
    }

    private HolderBondAccount retrieveHolderBondAccount(Holder holdModel, boolean newEntry) {
        org.greenpole.hibernate.entity.HolderBondAccount bondAccountEntity = new org.greenpole.hibernate.entity.HolderBondAccount();
        List<org.greenpole.entity.model.holder.HolderBondAccount> holderBondAcctList = holdModel.getHolderBondAccounts();
        List<org.greenpole.hibernate.entity.HolderBondAccount> returnBondAccountList = new ArrayList();

        for (org.greenpole.entity.model.holder.HolderBondAccount hBondAcct : holderBondAcctList) {
            HolderBondAccountId holdBondAcctId = new HolderBondAccountId();
            if (newEntry) {
                holdBondAcctId.setHolderId(hBondAcct.getHolderId());
            }
            bondAccountEntity.setId(holdBondAcctId);
            bondAccountEntity.setChn(hBondAcct.getChn());
            bondAccountEntity.setHolderBondAccPrimary(hBondAcct.isHolderBondAccPrimary());
            // NOTE: Bond Units is reperesented as interger in the entity and database
            // but represented as double from the model
            bondAccountEntity.setBondUnits((int) Math.round(hBondAcct.getBondUnits()));
            bondAccountEntity.setStartingPrincipalValue(hBondAcct.getStartingPrincipalValue());

            returnBondAccountList.add(bondAccountEntity);
        }
        return (HolderBondAccount) returnBondAccountList;
    }

}
