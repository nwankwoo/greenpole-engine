/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph.bondmodule;

import java.util.Date;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
import org.greenpole.hibernate.query.HolderComponentQuery;
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

        // check if necessary Holder actual paramenters are entered
        // TODO:    1. validate other parameters
        //          2. check if the user exist
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
            if ("".equals(holdModel.getHolderResidentialAddresses().getAddressLine1()) || "".equals(holdModel.getHolderResidentialAddresses().getAddressLine2())) {
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

    public Response uploadHolderSignature_Request(Login login, String authenticator, HolderSignature holderSign, byte[] holderSignImage) {
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
        // image size should not be larger than 2 megabytes = 2097152 bytes
        // 1,048,576 bytes or 1,024 kilobytes is 1 megabytes
        if (holderSignImage.length <= 2097152) {
            try {
                InputStream inputByteImage;
                inputByteImage = new ByteArrayInputStream(holderSignImage);
                BufferedImage byteToImage = ImageIO.read(inputByteImage);
                // implement random number + time stamp as image name            
                String signatureFileName = createSignatureFileName();
                String filePath = signProp.getSignaturePath() + signatureFileName + ".jpg";
                ImageIO.write(byteToImage, "jpg", new File(filePath));
                // file path not yet specified . . .
                holderSign.setSignaturePath(filePath);

                List<HolderSignature> holderListSignature = new ArrayList<>();
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
            org.greenpole.hibernate.entity.HolderSignature holderSignEntity = new org.greenpole.hibernate.entity.HolderSignature();

            holderSignEntity.setHolderSignaturePrimary(holderSignModel.isHolderSignaturePrimary());
            // HINT: 
            // holderSignEntity.setTitle() to be implemented in entity
            // holderSignEntity.setTitle(holderSignModel.getTitle());
            holderSignEntity.setSignaturePath(holderSignModel.getSignaturePath());
            // holderSignEntity.setHolder(holderSignModel);
            // TODO:
            // method createHolderSignature
            // ch.createHolderSingature(holderSignEntity);            
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

    public Response createBondHolderAccount_Request(Login login, String authenticator, Holder hold) {
        logger.info("request to create bond holder [{}] [{}] : Login - [{}]", hold.getFirstName(), hold.getLastName(), login.getUserId());

        Response res = new Response();
        NotificationWrapper wrapper;
        SignatureProperties signProp;
        QueueSender queue;
        NotifierProperties prop;

        // check if necessary Holder actual paramenters are entered
        // TODO:    1. validate other parameters
        //          2. check if the user exist
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
            // determine if residential address was set or postal address
            if ("".equals(holdModel.getHolderResidentialAddresses().getAddressLine1()) || "".equals(holdModel.getHolderResidentialAddresses().getAddressLine2())) {
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

    public Response editHolderTranspose_Request(Login login, String authenticator, Holder hold) {
        logger.info("request to transpose holder signature: Login - [{}]", login.getUserId());
        Response res = new Response();
        NotificationWrapper wrapper;
        SignatureProperties signProp;
        QueueSender queue;
        NotifierProperties prop;

        if (!hold.getFirstName().isEmpty() && !hold.getLastName().isEmpty()) {
            if (hold.getType().isEmpty()) {
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
                wrapper.setDescription("Authenticate holder transpose request, " + hold.getFirstName() + " " + hold.getLastName());
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

    public Response editHolderTranspose_Authorise(Login login, String notificationCode) {
        logger.info("request to edit transposed holder full name : Login - [{}]", login.getUserId());
        Response res = new Response();
        logger.info("Holder creation authorised - [{}]", notificationCode);

        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<Holder> holdList = (List<Holder>) wrapper.getModel();
            Holder holdModel = holdList.get(0);
            org.greenpole.hibernate.entity.Holder holdEntity = new org.greenpole.hibernate.entity.Holder();

            // TODO: get Holder entity based on HolderId as parameter
            // holdEntity = getHolder(holdModel.getHolderId());
            // Update FirstName and LastName of the Holder entity
            holdEntity.setFirstName(holdModel.getFirstName());
            holdEntity.setLastName(holdModel.getLastName());

            // TODO: update Holder entity
            // hcq.updateHolder(holdEntity);
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

    public Response shareUnitTransfer_Request(Login login, String authenticator, UnitTransfer transfer) {
        logger.info("request to transfer shares from [{}] to [{}]", transfer.getHolderIdFrom(), transfer.getHolderIdTo());
        Response res = new Response();
        NotificationWrapper wrapper;
        QueueSender queue;
        NotifierProperties prop;
        // NOTE: check if statement again!
        if (transfer.getClientCompanyIdFrom() == transfer.getClieintCompanyIdTo()) {
            wrapper = new NotificationWrapper();
            prop = new NotifierProperties(HolderComponent.class);
            queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());

            List<UnitTransfer> transferObj = new ArrayList<>();
            // List<org.greenpole.entity.model.holder.Holder> holderList = new ArrayList<>();            
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
            res.setRetn(202);
            res.setDesc("Source and destination company accounts types are not the same");
            logger.info("Source and destination company accounts types are not the same");
        }
        return res;
    }

    public Response shareUnitTransfer_Authorise(Login login, String notificationCode) {
        logger.info("share transfer authorisation reequest", login.getUserId());
        Response res = new Response();
        logger.info("Holder creation authorised - [{}]", notificationCode);

        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);

            List<UnitTransfer> transferList = (List<UnitTransfer>) wrapper.getModel();
            UnitTransfer unitTransferModel = transferList.get(0);
            // TODO: 
            // 1. check first if the holder exists THEN check if the holderAccount exist for both parties
            // 2. create method to retrieve HolderCompanyAccount ENTITY using holder company account id as parameter
            // 3. catch for empty list of result
            // SCENARIO:
            // 3. from and to have holder id AND company account id
            // 2. from and/or to have holder id BUT no company account for a PARTICULAR company e.g. UBA Capital
            //      thus create company account from existing CHN in holder details and transfer
            // 1. from and/or to have holder id BUT no CHN
            //      thus create certificate

            // check transfer type
            if ("shares".equals(unitTransferModel.getTransferType().toLowerCase())) {
                res.setRetn(202);
                res.setDesc("Transfer type should be shares");
                logger.info("Transfer type should be shares");
            } else {
                // check if the holder exist
                // boolean holderFromExist = hcq.checkHolderByHolderId(unitTransferModel.getHolderIdFrom());
                boolean holderFromExist = true;
                if (holderFromExist) {
                    // check if holder has (any) CHN
                    // boolean checkChn = hcq.checkHolderChnByHolderId(unitTransferModel.getHolderIdFrom());
                    boolean checkChnFrom = true;
                    if (checkChnFrom) {
                        // check if holder has clientCompanyAccount for the particular clientCompany else create clientCompanyAccount
                        // boolean checkCCAcct = hcq.checkClientCompanyByCompanyId(unitTransferModel.getClientCompanyIdFrom());
                        boolean checkCCAcct = true;
                        if (checkCCAcct) {
                            // begin to perform transfer
                            // combinatation fo HolderCompanyAccount and HolderCompanyAccountId to retrieve a particular HolderAccount Entity
                            // org.greenpole.hibernate.entity.HolderCompanyAccount compAcctFrom = hcq.getHolderAccountByHolderId(unitTransferModel.getHolderAcctIdFrom());
                            org.greenpole.hibernate.entity.HolderCompanyAccount compAcctFrom = new org.greenpole.hibernate.entity.HolderCompanyAccount();

                            boolean holderToExist = true;
                            if (holderToExist) {
                                // check if holder has (any) CHN
                                // boolean checkChn = hcq.checkHolderChnByHolderId(unitTransferModel.getHolderIdTo());
                                boolean checkChnTo = true;
                                if (checkChnTo) {
                                    // check if holder has clientCompanyAccount for the particular clientCompany else create clientCompanyAccount
                                    // boolean checkCCAcct = hcq.checkClientCompanyByCompanyId(unitTransferModel.getClientCompanyIdFrom());
                                    boolean checkCCAcctTo = true;
                                    if (checkCCAcctTo) {
                                        // begin to perform transfer
                                        // combinatation fo HolderCompanyAccount and HolderCompanyAccountId to retrieve a particular HolderAccount Entity
                                        // org.greenpole.hibernate.entity.HolderCompanyAccount compAcctFrom = hcq.getHolderAccountByHolderId(unitTransferModel.getHolderAcctIdFrom());
                                        org.greenpole.hibernate.entity.HolderCompanyAccount compAcctTo = new org.greenpole.hibernate.entity.HolderCompanyAccount();
                                        if (compAcctFrom.getShareUnits() < unitTransferModel.getUnits()) {
                                            logger.info("Insufficient unit of shares for transfer operation");
                                            res.setRetn(203);
                                            res.setDesc("Insufficient unit of shares for transfer operation");
                                        } else {
                                            compAcctFrom.setShareUnits((int) (compAcctFrom.getShareUnits() - unitTransferModel.getUnits()));
                                            compAcctTo.setShareUnits((int) (compAcctTo.getShareUnits() + unitTransferModel.getUnits()));

                                            hcq.createHolderCompanyAccount(compAcctFrom);
                                            hcq.createHolderCompanyAccount(compAcctTo);

                                            logger.info("share unit of [{}] transfered", unitTransferModel.getUnits());
                                            res.setRetn(0);
                                            res.setDesc("Successful Persistence");

                                        }
                                    } else {
                                        // response description: have shares with client company
                                        res.setRetn(205);
                                        res.setDesc("Holder to receive shares does not have shares with client company");
                                        logger.info("Holder to receive shares does not have shares with client company");
                                    }

                                } else {
                                    // create a certificate for the holder
                                    res.setRetn(204);
                                    res.setDesc("Create certificate for holder");
                                    logger.info("Create certificate for holder");
                                }
                            }
                        } else {
                            // response description: have shares with client company
                            res.setRetn(205);
                            res.setDesc("Holder does not have shares with client company");
                            logger.info("Holder does not have shares with client company");
                        }

                    } else {
                        // create a certificate for the holder
                        res.setRetn(204);
                        res.setDesc("Create certificate for holder");
                        logger.info("Create certificate for holder");
                    }
                } else {
                    res.setRetn(203);
                    res.setDesc("Holder to transfer shares does not exist in database");
                    logger.info("Holder to transfer shares does not exist in database");
                }
            }
        } catch (JAXBException ex) {
            // TODO: catch other types of exception
            // catch ArrayIndexOutOfBoundException for an empty list
            // 
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
        // NOTE: check if statement again!
        if (transfer.getClientCompanyIdFrom() == transfer.getClieintCompanyIdTo()) {

            wrapper = new NotificationWrapper();
            prop
                    = new NotifierProperties(HolderComponent.class
                    );
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

            logger.info(
                    "notification forwarded to queue - notification code: [{}]", wrapper.getCode());
        } else {
            res.setRetn(202);
            res.setDesc("Source and destination company accounts are not the same");
            logger.info("Source and destination company accounts are not the same");
        }
        return res;
    }

    public Response bondTransfer_Authorise(Login login, String notificationCode) {
        logger.info("share transfer authorisation reequest", login.getUserId());
        Response res = new Response();
        logger.info("Holder creation authorised - [{}]", notificationCode);

        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);

            List<UnitTransfer> bondTransferList = (List<UnitTransfer>) wrapper.getModel();
            UnitTransfer unitTransferModel = bondTransferList.get(0);
            // TODO: 
            // 1. check first if the holder exists THEN check if the holderAccount exist for both parties
            // 2. create method to retrieve HolderBondAccount ENTITY using holder company account id as parameter
            // 3. catch for empty list of result
            // SCENARIO:
            // 3. from and to have holder id AND company account id
            // 2. from and/or to have holder id BUT no bond account for a PARTICULAR bond offer
            //      thus create bond account from existing CHN in holder details and transfer
            // 1. from and/or to have holder id BUT no CHN
            //      thus create bond account

            // check transfer type
            if ("bonds".equals(unitTransferModel.getTransferType().toLowerCase())) {
                res.setRetn(202);
                res.setDesc("Transfer type should be bonds");
                logger.info("Transfer type should be bonds");
            } else {
                // check if the holder exist
                // boolean holderFromExist = hcq.checkHolderByHolderId(unitTransferModel.getHolderIdFrom());
                boolean holderFromExist = true;
                if (holderFromExist) {
                    // check if holder has (any) CHN
                    // boolean checkChnFrom = hcq.checkHolderChnByHolderId(unitTransferModel.getHolderIdFrom());
                    boolean checkChnFrom = true;
                    if (checkChnFrom) {
                        // check if holder has holderBondAccount for the particular bond offer else create holderBondAccount
                        // boolean checkHBAcctFrom = hcq.checkBondOfferBondOfferId(unitTransferModel.getBondOfferIdFrom());
                        boolean checkHBAcctFrom = true;
                        if (checkHBAcctFrom) {
                            // begin to perform transfer
                            // combinatation of HolderBondAccount and HolderBondAccountId to retrieve a particular HolderBondAccount Entity
                            // org.greenpole.hibernate.entity.HolderBondAccount compAcctFrom = hcq.getHolderAccountByBondOfferId(unitTransferModel.getBondOfferIdFrom());
                            org.greenpole.hibernate.entity.HolderBondAccount bondAcctFrom = new org.greenpole.hibernate.entity.HolderBondAccount();

                            boolean holderToExist = true;
                            if (holderToExist) {
                                // check if holder has (any) CHN
                                // boolean checkChn = hcq.checkHolderChnByHolderId(unitTransferModel.getHolderIdTo());
                                boolean checkChnTo = true;
                                if (checkChnTo) {
                                    // check if holder has clientCompanyAccount for the particular clientCompany else create clientCompanyAccount
                                    // boolean checkCCAcct = hcq.checkClientCompanyByCompanyId(unitTransferModel.getClientCompanyIdFrom());
                                    boolean checkCCAcctTo = true;
                                    if (checkCCAcctTo) {
                                        // begin to perform transfer
                                        // combinatation fo HolderCompanyAccount and HolderBondAccountId to retrieve a particular HolderAccount Entity
                                        // org.greenpole.hibernate.entity.HolderCompanyAccount bondAcctFrom = hcq.getHolderAccountByHolderId(unitTransferModel.getHolderAcctIdFrom());
                                        // org.greenpole.hibernate.entity.BondOffer bondOfferEntity = hcq.getBondOfferByHolderId(unitTransferModel.getHolderId());
                                        org.greenpole.hibernate.entity.BondOffer bondOfferEntity = new org.greenpole.hibernate.entity.BondOffer();
                                        org.greenpole.hibernate.entity.HolderBondAccount bondAcctTo = new org.greenpole.hibernate.entity.HolderBondAccount();
                                        if (bondAcctFrom.getBondUnits() < unitTransferModel.getUnits()) {
                                            logger.info("Insufficient unit of shares for transfer operation");
                                            res.setRetn(203);
                                            res.setDesc("Insufficient unit of shares for transfer operation");
                                        } else {
                                            double principalValue = bondOfferEntity.getBondUnitPrice() * bondAcctFrom.getBondUnits();
                                            // double principalValue = unitTransferModel.getUnitPrice() * compAcctFrom.getBondUnits();

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
                                        res.setDesc("Holder to receive bonds has no bond account for the bond offer - create bond account");
                                        logger.info("Holder to receive bonds has no bond account for the bond offer -  create bond account");
                                    }
                                } else {
                                    // response description: have shares with client company
                                    res.setRetn(205);
                                    res.setDesc("Holder to receive bonds has no CHN");
                                    logger.info("Holder to receive bonds has no CHN");
                                }

                            } else {
                                res.setRetn(205);
                                res.setDesc("Holder does not exist");
                                logger.info("Holder does not exist");
                            }
                        } else {
                            res.setRetn(205);
                            res.setDesc("Holder does not exist");
                            logger.info("Holder does not exist");
                        }
                    } else {
                        // response description: have shares with client company
                        res.setRetn(205);
                        res.setDesc("Holder does not have bonds with client company");
                        logger.info("Holder does not have bonds with client company");
                    }

                } else {
                    // create a certificate for the holder
                    res.setRetn(204);
                    res.setDesc("Holder does not have CHN");
                    logger.info("Holder does not have CHN");
                }
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
            bondAccountEntity.setStartingPrincipalValue(hBondAcct.getPrincipalValue());

            returnBondAccountList.add(bondAccountEntity);
        }
        return (HolderBondAccount) returnBondAccountList;
    }
}
