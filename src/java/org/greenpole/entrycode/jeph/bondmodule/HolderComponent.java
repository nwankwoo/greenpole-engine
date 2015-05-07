/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph.bondmodule;

import java.util.Date;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.model.Address;
import org.greenpole.entity.model.EmailAddress;
import org.greenpole.entity.model.PhoneNumber;
import org.greenpole.entity.model.holder.Holder;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.entrycode.jeph.mocks.SignatureProperties;
import org.greenpole.entrycode.jeph.models.HolderEdit;
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
    private final SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");

    public HolderComponent() {
        this.hcq = new HolderComponentQueryImpl();
    }

    /**
     * Processes request to create a holder account
     *
     * @param login user's login details
     * @param authenticator the authenticator user meant to receive the
     * notification
     * @param holder object representing holder details
     * @return response object for the request
     */
    public Response createHolder_Request(Login login, String authenticator, Holder holder) {
        Response res = new Response();
        NotificationWrapper wrapper;
        QueueSender queue;
        NotifierProperties prop;
        String resDes = null;
        boolean flag = false;

        if ("".equals(holder.getFirstName()) || holder.getFirstName() == null) {
            resDes = "\nError: Holder first name should not be empty";
        } else {
            flag = true;
        }
        if ("".equals(holder.getMiddleName()) || holder.getMiddleName() == null) {
            resDes += "\nError: Holder middle name should not be empty";
        } else {
            flag = true;
        }
        if ("".equals(holder.getLastName()) || holder.getLastName() == null) {
            resDes += "\nError: Holder last name should not be empty";
        } else {
            flag = true;
        }
        if ("".equals(holder.getGender()) || holder.getGender() == null) {
            resDes += "\nError: Holder gender should not be empty";
        } else {
            flag = true;
        }
        if ("".equals(holder.getDob()) || holder.getDob() == null) {
            resDes += "\nError: Holder date of birth should not be empty";
        } else {
            flag = true;
        }
        if (!holder.getAddresses().isEmpty()) {
            for (Address addr : holder.getAddresses()) {
                if ("".equals(addr.getAddressLine1()) || addr.getAddressLine1() == null) {
                    resDes += "\nAddress line 1 should not be empty. Delete entire address if you must";
                } else if ("".equals(addr.getState()) || addr.getState() == null) {
                    resDes += "\nState should not be empty. Delete entire address if you must";
                } else if ("".equals(addr.getCountry()) || addr.getCountry() == null) {
                    resDes += "\nCountry should not be empty. Delete entire address if you must";
                }
            }
        } else if (!holder.getHolderEmailAddresses().isEmpty()) {
            for (EmailAddress email : holder.getHolderEmailAddresses()) {
                if ("".equals(email.getEmailAddress()) || email.getEmailAddress() == null) {
                    resDes += "\nEmail address should not be empty. Delete email entry if you must";
                }
            }
        } else if (!holder.getHolderPhoneNumbers().isEmpty()) {
            for (PhoneNumber phone : holder.getHolderPhoneNumbers()) {
                if ("".equals(phone.getPhoneNumber()) || phone.getPhoneNumber() == null) {
                    resDes += "\nPhone number should not be empty. Delete phone number entry if you must";
                }
            }
        } else {
            flag = true;
        }
        if (flag) {
            try {
                wrapper = new NotificationWrapper();
                prop = new NotifierProperties(HolderComponent.class);
                queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
                List<Holder> holdList = new ArrayList<>();
                holdList.add(holder);
                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription("Authenticate holder account, " + holder.getFirstName() + " " + holder.getLastName());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(holdList);
                res = queue.sendAuthorisationRequest(wrapper);
                logger.info("notification forwarded to queue - notification code: [{}]", wrapper.getCode());
            } catch (Exception ex) {
                logger.info("Error creating holder account. See error log");
                logger.error("Error creating holder account - ", ex);
                res.setRetn(99);
                res.setDesc("General Error: Unable to create holder account. Contact system administrator." + "\nMessage: " + ex.getMessage());
            }
        } else {
            res.setRetn(300);
            res.setDesc("Error: " + resDes);
            logger.info("Error filing holder details: ", resDes);
        }
        return res;
    }

    /**
     * Processes request authorisation to create holder details
     *
     * @param login user's login details
     * @param notificationCode the notification code
     * @return response object for the authorization request
     */
    public Response createHolder_Authorise(Login login, String notificationCode) {
        logger.info("request for authorisation to persist holder details. Invoked by Login [{}]", login.getUserId());
        Response res = new Response();
        
        try {
            logger.info("Holder creation authorised - [{}]", notificationCode);
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<Holder> holdList = (List<Holder>) wrapper.getModel();
            Holder holdModel = holdList.get(0);
            org.greenpole.hibernate.entity.Holder holdEntity = new org.greenpole.hibernate.entity.Holder();

            holdEntity.setFirstName(holdModel.getFirstName());
            holdEntity.setLastName(holdModel.getLastName());
            holdEntity.setMiddleName(holdModel.getMiddleName());
            holdEntity.setType(holdModel.getType());
            holdEntity.setGender(holdModel.getGender());
            holdEntity.setDob(formatter.parse(holdModel.getDob()));
            holdEntity.setChn(holdModel.getChn());

            boolean created = hcq.createHolderAccount(holdEntity, retrieveHolderCompanyAccount(holdModel, false), retrieveHolderResidentialAddress(holdModel, false), retrieveHolderPostalAddress(holdModel, false), retrieveHolderPhoneNumber(holdModel, false));

            if (created) {
                res.setRetn(0);
                res.setDesc("Holder details saved: Successful");
                return res;
            } else {
                logger.info("error persist holder account.");
                logger.error("error persist holder account.");
                res.setRetn(99);
                res.setDesc("General error. Unable to persist holder account. Contact system administrator.");
                return res;
            }
        } catch (JAXBException ex) {
            res.setRetn(98);
            res.setDesc("error loading notification xml file. See error log");
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);
        } catch (Exception ex) {
            logger.info("error persist holder account. See error log");
            logger.error("error persist holder account - ", ex);
            res.setRetn(99);
            res.setDesc("General error. Unable to persist holder account. Contact system administrator." + "\nMessage: " + ex.getMessage());
        }
        return res;
    }

    /**
     * Processes the request to upload a holder signature
     *
     * @param login user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param holderSign holder signature details
     * @param holderSignImage holder signature image
     * @return response object for the request
     */
    public Response uploadHolderSignature_Request(Login login, String authenticator,
            org.greenpole.entrycode.jeph.models.HolderSignature holderSign, byte[] holderSignImage) {
        logger.info("request to upload holder signature. invoked by [{}]", login.getUserId());

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
                logger.info("Error in saving image. See error log");
                logger.error("Error in saving image - ", ioex);
                res.setRetn(99);
                res.setDesc("General error. Unable to upload holder signature. Contact system administrator." + "\nMessage: " + ioex.getMessage());
            }
        } else {
            res.setRetn(99);
            res.setDesc("Error in uploading image - image size should be less than 2 megabytes");
            logger.info("Error in uploading image -  See error log");
            logger.error("Error in uploading image -image size should be less than 2 megabytes");
        }
        return res;
    }

    /**
     * Processes the request authorisation to upload a holder signature
     *
     * @param login user's login details
     * @param notificationCode the notification code
     * @return response object for the authorization request
     */
    public Response uploadHolderSignature_Authorise(Login login, String notificationCode) {
        logger.info("Authorisation request to persist holder signature. Invoked by [{}]", login.getUserId());
        Response res = new Response();

        try {
            logger.info("Holder signature upload authorised - [{}]", notificationCode);
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<org.greenpole.entrycode.jeph.models.HolderSignature> holdSigntureList = (List<org.greenpole.entrycode.jeph.models.HolderSignature>) wrapper.getModel();
            org.greenpole.entrycode.jeph.models.HolderSignature holderSignModel = holdSigntureList.get(0);
            // org.greenpole.hibernate.entity.HolderSignature holderSignEntity = hcq.getHolderSignature(holderSignModel.getHolderId());
            org.greenpole.hibernate.entity.HolderSignature holderSignEntity = new org.greenpole.hibernate.entity.HolderSignature();
            holderSignEntity.setHolderSignaturePrimary(false);
            // hcq.createOrUpdateHolderSingature(holderSignEntity);
            // org.greenpole.hibernate.entity.HolderSignature holderSignEntityUpdate = hcq.getHolderSignature(holderSignModel.getHolderId());
            org.greenpole.hibernate.entity.HolderSignature holderSignEntityUpdate = new org.greenpole.hibernate.entity.HolderSignature();
            holderSignEntityUpdate.setHolderSignaturePrimary(true);
            holderSignEntityUpdate.setTitle(holderSignModel.getTitle());
            holderSignEntityUpdate.setSignaturePath(holderSignModel.getSignaturePath());
            // hcq.createOrUpdateHolderSingature(holderSignEntityUpdate);            
            res.setRetn(0);
            res.setDesc("Successful Persistence - Holder Signature");
            return res;

        } catch (JAXBException ex) {
            res.setRetn(98);
            res.setDesc("error loading notification xml file. See error log");
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);
        } catch (Exception ex) {
            logger.info("Error in saving uploaded image. See error log");
            logger.error("Error in saving uploaded image - ", ex);
            res.setRetn(99);
            res.setDesc("General error. Unable to save uploaded holder signature. Contact system administrator." + "\nMessage: " + ex.getMessage());
        }
        return res;
    }

    /**
     * Processes the request to query holder signature
     *
     * @param login user's login details
     * @param authenticator the authenticator user meant to receive the
     * notification details
     * @param holderSign holder signature object
     * @return response object for the request
     */
    public Response queryHolderSignature_Request(Login login, String authenticator,
            org.greenpole.entrycode.jeph.models.HolderSignature holderSign) {
        logger.info("request to query holder signature for [{}]. Invoked by [{}]", holderSign.getHolderId(), login.getUserId());

        Response res = new Response();
        NotificationWrapper wrapper;
        QueueSender queue;
        NotifierProperties prop;
        boolean holderIdExist;

        if (holderSign.getHolderId() <= 0) {
            // holderIdExist = hcq.chkHolderSignature(holderSign.getHolderId());
            holderIdExist = true;
            if (holderIdExist) {
                try {
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
                } catch (Exception ex) {
                    logger.info("error querying holder signature. See error log");
                    logger.error("error querying holder signature - ", ex);
                    res.setRetn(99);
                    res.setDesc("General error. Unable to query holder signature. Contact system administrator." + "\nMessage: " + ex.getMessage());
                }
            } else {
                res.setRetn(301);
                res.setDesc("Error: No signature exists for holder");
                logger.info("Error: No signature exists for holder");
            }
        } else {
            logger.info("Error: Holder Id is either invalid or empty");
            logger.error("Error: Holder Id is either invalid or empty");
            res.setRetn(99);
            res.setDesc("General Error: Holder Id is either invalid or empty. Contact system administrator.");
        }
        return res;
    }

    /**
     * Processes the request authorisation to query holder signature taking,
     *
     * @param login user's login details,
     * @param notificationCode the notification code,
     * @return response object for the authorized request
     */
    public Response queryHolderSignature_Authorise(Login login, String notificationCode) {
        logger.info("request authorisation to query holder signature. Invoked by [{}]", login.getUserId());
        Response res = new Response();

        try {
            logger.info("Holder signature query authorised - [{}]", notificationCode);
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<org.greenpole.entrycode.jeph.models.HolderSignature> holdSignList = (List<org.greenpole.entrycode.jeph.models.HolderSignature>) wrapper.getModel();
            // org.greenpole.hibernate.entity.HolderSignature holdSignEntity = hcq.getHolderSignature(holdSignList.get(0).getHolderId());
            org.greenpole.hibernate.entity.HolderSignature holdSignEntity = new org.greenpole.hibernate.entity.HolderSignature();
            org.greenpole.entrycode.jeph.models.HolderSignature holderSignSend = new org.greenpole.entrycode.jeph.models.HolderSignature();
            holderSignSend.setTitle(holdSignEntity.getTitle());
            byte[] signatureImage = readSignatureFile(holdSignEntity.getSignaturePath());
            holderSignSend.setSignImg(signatureImage);
            // holdSignList.clear();
            holdSignList.add(holderSignSend);
            res.setRetn(0);
            res.setDesc("Holder signature details");
            res.setBody(holdSignList);
        } catch (JAXBException jbex) {
            res.setRetn(98);
            res.setDesc("Error retrieving holder signature details");
            logger.info("Error retrieving holder signature details");
            logger.error("Error retrieving holder signature details");
        } catch (Exception ex) {
            logger.info("Error retrieving holder signature details");
            logger.error("Error retrieving holder signature details");
            res.setRetn(99);
            res.setDesc("General Error: retrieving holder signature details. Contact system administrator.");
        }
        return res;
    }

    /**
     * Retrieves an image in byte array
     *
     * @param signaturePath the full path of the image on disk
     * @return byte array object of the image
     */
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

    /**
     * Processes request to create bond holder account
     *
     * @param login user's login details
     * @param authenticator the authenticator user meant to receive the
     * notification
     * @param holder the holder details object
     * @return response object for the request
     */
    public Response createBondHolderAccount_Request(Login login, String authenticator, Holder holder) {
        logger.info("request to create bond holder account. Invoked by [{}]", login.getUserId());
        Response res = new Response();
        NotificationWrapper wrapper;
        QueueSender queue;
        NotifierProperties prop;
        String resDes = null;
        boolean flag = false;

        if ("".equals(holder.getFirstName()) || holder.getFirstName() == null) {
            resDes = "\nError: Holder first name should not be empty";
        } else {
            flag = true;
        }
        if ("".equals(holder.getMiddleName()) || holder.getMiddleName() == null) {
            resDes += "\nError: Holder middle name should not be empty";
        } else {
            flag = true;
        }
        if ("".equals(holder.getLastName()) || holder.getLastName() == null) {
            resDes += "\nError: Holder last name should not be empty";
        } else {
            flag = true;
        }
        if ("".equals(holder.getGender()) || holder.getGender() == null) {
            resDes += "\nError: Holder gender should not be empty";
        } else {
            flag = true;
        }
        if ("".equals(holder.getDob()) || holder.getDob() == null) {
            resDes += "\nError: Holder date of birth should not be empty";
        } else {
            flag = true;
        }
        if (!holder.getAddresses().isEmpty()) {
            for (Address addr : holder.getAddresses()) {
                if ("".equals(addr.getAddressLine1()) || addr.getAddressLine1() == null) {
                    resDes += "\nAddress line 1 should not be empty. Delete entire address if you must";
                } else if ("".equals(addr.getState()) || addr.getState() == null) {
                    resDes += "\nState should not be empty. Delete entire address if you must";
                } else if ("".equals(addr.getCountry()) || addr.getCountry() == null) {
                    resDes += "\nCountry should not be empty. Delete entire address if you must";
                }
            }
        } else if (!holder.getHolderEmailAddresses().isEmpty()) {
            for (EmailAddress email : holder.getHolderEmailAddresses()) {
                if ("".equals(email.getEmailAddress()) || email.getEmailAddress() == null) {
                    resDes += "\nEmail address should not be empty. Delete email entry if you must";
                }
            }
        } else if (!holder.getHolderPhoneNumbers().isEmpty()) {
            for (PhoneNumber phone : holder.getHolderPhoneNumbers()) {
                if ("".equals(phone.getPhoneNumber()) || phone.getPhoneNumber() == null) {
                    resDes += "\nPhone number should not be empty. Delete phone number entry if you must";
                }
            }
        } else {
            flag = true;
        }
        if (flag) {
            try {
                wrapper = new NotificationWrapper();
                prop = new NotifierProperties(HolderComponent.class);
                queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
                List<Holder> holdList = new ArrayList<>();
                holdList.add(holder);
                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription("Authenticate holder account, " + holder.getFirstName() + " " + holder.getLastName());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(holdList);
                res = queue.sendAuthorisationRequest(wrapper);
                logger.info("notification forwarded to queue - notification code: [{}]", wrapper.getCode());
            } catch (Exception ex) {
                logger.info("Error creating bond holder account. See error log");
                logger.error("Error creating bond holder account - ", ex);
                res.setRetn(99);
                res.setDesc("General Error: Unable to create bond holder account. Contact system administrator." + "\nMessage: " + ex.getMessage());
            }
        } else {
            res.setRetn(300);
            res.setDesc("Error: " + resDes);
            logger.info("Error filing bond holder details: ", resDes);
        }
        return res;
    }

    /**
     * Processes request authorisation to create bondholder account
     *
     * @param login user's login details
     * @param notificationCode the notification code
     * @return response object for the request
     */
    public Response createBondHolderAccount_Authorise(Login login, String notificationCode) {
        logger.info("Authorization request to persist bond holder details: Invoked by - [{}]", login.getUserId());
        Response res = new Response();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");

        try {
            logger.info("Holder creation authorised - [{}]", notificationCode);
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<Holder> holdList = (List<Holder>) wrapper.getModel();
            Holder holdModel = holdList.get(0);
            org.greenpole.hibernate.entity.Holder holdEntity = new org.greenpole.hibernate.entity.Holder();

            holdEntity.setFirstName(holdModel.getFirstName());
            holdEntity.setLastName(holdModel.getLastName());
            holdEntity.setMiddleName(holdModel.getMiddleName());
            holdEntity.setType(holdModel.getType());
            holdEntity.setGender(holdModel.getGender());
            holdEntity.setDob(formatter.parse(holdModel.getDob()));
            holdEntity.setChn(holdModel.getChn());

            boolean created = hcq.createHolderAccount(holdEntity, retrieveHolderBondAccount(holdModel, false), retrieveHolderResidentialAddress(holdModel, false), retrieveHolderPostalAddress(holdModel, false), retrieveHolderPhoneNumber(holdModel, false));

            if (created) {
                res.setRetn(0);
                res.setDesc("Successful Persistence");
                return res;
            } else {
                res.setRetn(300);
                res.setDesc("An error occured persisting the data residential and postal addresses are empty");
                return res;
            }

        } catch (JAXBException ex) {
            res.setRetn(98);
            res.setDesc("error loading notification xml file. See error log");
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);
        } catch (Exception ex) {
            res.setRetn(99);
            res.setDesc("error creating bondholder account - See error log");
            logger.info("error creating bondholder account - See error log");
            logger.error("error creating bondholder account - ", ex);
        }
        return res;
    }

    /**
     * Processes request to transpose holder first name and last name
     *
     * @param login user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param hold the holder details object
     * @return response object for the request
     */
    public Response transposeHolderName_Request(Login login, String authenticator, Holder hold) {
        logger.info("request to transpose holder signature: Invoked by [{}]", login.getUserId());
        Response res = new Response();
        NotificationWrapper wrapper;
        QueueSender queue;
        NotifierProperties prop;

        if (hold.getFirstName() != null || !"".equals(hold.getFirstName())) {
            if (hold.getLastName() != null || !"".equals(hold.getLastName())) {
                if (hold.getType() == null || "".equals(hold.getType())) {
                    res.setRetn(300);
                    res.setDesc("Error: holder account type should not be empty");
                    logger.info("Error: holder account type should not be empty");
                } else {
                    try {
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
                    } catch (Exception ex) {
                        logger.info("error transposing holder names. See error log");
                        logger.error("error transposing holder names - ", ex);
                        res.setRetn(99);
                        res.setDesc("General error. Unable to transposing holder names. Contact system administrator." + "\nMessage: " + ex.getMessage());
                    }
                }
            } else {
                res.setRetn(300);
                res.setDesc("Error: holder last name should not be empty");
                logger.info("Error: holder last name should not be empty");
            }
        } else {
            res.setRetn(300);
            res.setDesc("Error: holder first name should not be empty");
            logger.info("Error: holder first name should not be empty");
        }
        return res;
    }

    /**
     * Processes request authorisation to transpose holder names
     *
     * @param login user's login details
     * @param notificationCode the notification code
     * @return response object for the authorized request
     */
    public Response transposeHolderName_Authorise(Login login, String notificationCode) {
        logger.info("Authorizatin request to save transposed holder name. Invoked by [{}]", login.getUserId());
        Response res = new Response();
        org.greenpole.hibernate.entity.Holder holdEntity;

        try {
            logger.info("Transpose holder name authorised - [{}]", notificationCode);
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<Holder> holdList = (List<Holder>) wrapper.getModel();
            Holder holdModel = holdList.get(0);
            // holdEntity = getHolder(holdModel.getHolderId());
            holdEntity = new org.greenpole.hibernate.entity.Holder();
            holdEntity.setFirstName(holdModel.getFirstName());
            holdEntity.setLastName(holdModel.getLastName());
            // hcq.updateHolder(holdEntity);
        } catch (JAXBException ex) {
            res.setRetn(98);
            res.setDesc("error loading notification xml file. See error log");
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);
        } catch (Exception ex) {
            logger.info("error saving transposed holder name. See error log");
            logger.error("error saving transposed holder name - ", ex);
            res.setRetn(99);
            res.setDesc("General error. Unable to save transposed holder name. Contact system administrator." + "\nMessage: " + ex.getMessage());
        }
        return res;
    }

    /**
     * Processes request to edit holder details
     *
     * @param login user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param holder the edited holder details
     * @return response object for the edit holder details request
     */
    public Response editHolderDetails_Request(Login login, String authenticator, HolderEdit holder) {
        if (!"".equals(holder.getChangeType()) || holder.getChangeType() == null) {
            logger.info("request [{}] on holder details: Login - [{}]", holder.getChangeType(), login.getUserId());
        } else {
            logger.info("Change type for holder details was not specified");
        }
        Response res = new Response();
        NotificationWrapper wrapper;
        QueueSender queue;
        NotifierProperties prop;
        String resDes = null;
        boolean flag = false;

        if (!holder.getHolderChanges().isEmpty()) {
            if ("".equals(holder.getFirstName()) || holder.getFirstName() == null) {
                resDes = "\nError: Holder first name should not be empty";
            } else {
                flag = true;
            }
            if ("".equals(holder.getMiddleName()) || holder.getMiddleName() == null) {
                resDes += "\nError: Holder middle name should not be empty";
            } else {
                flag = true;
            }
            if ("".equals(holder.getLastName()) || holder.getLastName() == null) {
                resDes += "\nError: Holder last name should not be empty";
            } else {
                flag = true;
            }
            if ("".equals(holder.getChn()) || holder.getChn() == null) {
                resDes += "\nError: Holder CHN should not be empty";
            } else {
                flag = true;
            }
            if ("".equals(holder.getGender()) || holder.getGender() == null) {
                resDes += "\nError: Holder gender should not be empty";
            } else {
                flag = true;
            }
            if ("".equals(holder.getDob()) || holder.getDob() == null) {
                resDes += "\nError: Holder date of birth should not be empty";
            } else {
                flag = true;
            }
            if (!holder.getAddresses().isEmpty()) {
                for (Address addr : holder.getAddresses()) {
                    if ("".equals(addr.getAddressLine1()) || addr.getAddressLine1() == null) {
                        resDes += "\nAddress line 1 should not be empty. Delete entire address if you must";
                    } else if ("".equals(addr.getState()) || addr.getState() == null) {
                        resDes += "\nState should not be empty. Delete entire address if you must";
                    } else if ("".equals(addr.getCountry()) || addr.getCountry() == null) {
                        resDes += "\nCountry should not be empty. Delete entire address if you must";
                    }
                }
            } else if (!holder.getHolderEmailAddresses().isEmpty()) {
                for (EmailAddress email : holder.getHolderEmailAddresses()) {
                    if ("".equals(email.getEmailAddress()) || email.getEmailAddress() == null) {
                        resDes += "\nEmail address should not be empty. Delete email entry if you must";
                    }
                }
            } else if (!holder.getHolderPhoneNumbers().isEmpty()) {
                for (PhoneNumber phone : holder.getHolderPhoneNumbers()) {
                    if ("".equals(phone.getPhoneNumber()) || phone.getPhoneNumber() == null) {
                        resDes += "\nPhone number should not be empty. Delete phone number entry if you must";
                    }
                }
            } else {
                flag = true;
            }
            if (flag) {
                try {
                    wrapper = new NotificationWrapper();
                    prop = new NotifierProperties(HolderComponent.class);
                    queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
                    List<HolderEdit> holdList = new ArrayList<>();
                    holdList.add(holder);
                    wrapper.setCode(Notification.createCode(login));
                    wrapper.setDescription("Authenticate " + holder.getChangeType() + " of holder account, " + holder.getFirstName() + " " + holder.getLastName());
                    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                    wrapper.setFrom(login.getUserId());
                    wrapper.setTo(authenticator);
                    wrapper.setModel(holdList);
                    res = queue.sendAuthorisationRequest(wrapper);
                    logger.info("notification forwarded to queue - notification code: [{}]", wrapper.getCode());
                } catch (Exception ex) {
                    logger.info("error editing holder account. See error log");
                    logger.error("error editing holder account - ", ex);
                    res.setRetn(99);
                    res.setDesc("General error. Unable to editing holder account. Contact system administrator." + "\nMessage: " + ex.getMessage());
                }
            } else {
                res.setRetn(300);
                res.setDesc("Error: " + resDes);
                logger.info("Error filing holder details: ", resDes);
            }
        } else {
            res.setRetn(300);
            res.setDesc("Error: Changes to holder details were not captured");
            logger.info("Error: Changes to holder details were not captured");
        }
        return res;
    }

    /**
     * Processes request authorisation to edit holder details
     *
     * @param login user's login details
     * @param notificationCode the notification code
     * @return response object for the authorized edit holder details request
     */
    public Response editHolderDetails_Authorise(Login login, String notificationCode) {
        logger.info("request authorisation to persist holder details. Invoked by [{}]", login.getUserId());
        Response res = new Response();
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<HolderEdit> holderEditList = (List<HolderEdit>) wrapper.getModel();
            HolderEdit holderEdit = holderEditList.get(0);

            org.greenpole.hibernate.entity.Holder holdEntity = hcq.getHolder(holderEdit.getHolderId());
            holdEntity.setFirstName(holderEdit.getFirstName());
            holdEntity.setLastName(holderEdit.getLastName());
            holdEntity.setMiddleName(holderEdit.getMiddleName());
            holdEntity.setType(holderEdit.getType());
            holdEntity.setGender(holderEdit.getGender());
            holdEntity.setDob(formatter.parse(holderEdit.getDob()));
            holdEntity.setChn(holderEdit.getChn());

            boolean created = hcq.updateHolderAccount(holdEntity, retrieveHolderResidentialAddress(holderEdit, false), retrieveHolderPostalAddress(holderEdit, false), retrieveHolderPhoneNumber(holderEdit, false));
            // boolean updated = hcq.updateHolderChanges(holder, holderEdit)
            boolean updated = true;
            if (created && updated) {
                res.setRetn(0);
                res.setDesc("Holder details saved");
                return res;
            } else {
                res.setRetn(300);
                res.setDesc("An error occured saving hodler details");
                return res;
            }
        } catch (JAXBException ex) {
            res.setRetn(98);
            res.setDesc("error loading notification xml file. See error log");
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);
        } catch (Exception ex) {
            logger.info("error persist edited holder details. See error log");
            logger.error("error persist edited holder details - ", ex);
            res.setRetn(99);
            res.setDesc("General error. Unable to persist edited holder details. Contact system administrator." + "\nMessage: " + ex.getMessage());
        }
        return res;
    }

    /**
     * Unwraps the changes made to details of a holder into the holder changes
     * entity
     *
     * @param holderModel object of the holder
     * @param hd object of holder details edited
     * @return boolean value indicating status
     * @throws ParseException for parsing String to Date type
     */
    private boolean updateHolderChanges(Holder holderModel, org.greenpole.entrycode.jeph.models.HolderEdit hd) throws ParseException {
        HolderChanges holderChg = new HolderChanges();

        HolderChangeType holderChgType = new HolderChangeType();
        holderChgType.setChangeType(hd.getChangeType());
        holderChgType.setDescription(hd.getDescription());

        org.greenpole.hibernate.entity.Holder holder = new org.greenpole.hibernate.entity.Holder();
        holder.setId(hd.getHolderId());

        if (!hd.getHolderChanges().isEmpty() || hd.getHolderChanges() != null) {
            for (org.greenpole.entrycode.jeph.models.HolderChanges hChg : hd.getHolderChanges()) {
                holderChg.setInitialForm(hChg.getInitialForm());
                holderChg.setCurrentForm(hChg.getCurrentForm());
                holderChg.setHolderChangeType(holderChgType);
                holderChg.setChangeDate(formatter.parse(hChg.getChangeDate()));
                holderChg.setHolder(holder);
            }
        }
        return true;
    }

    /**
     * An adapter of retrieveHolderPostalAddress that unwraps the edited holder
     * details from the holderEdit model into a holder model
     *
     * @param holderEdit the edited holder details
     * @param newEntry boolean value to indicate new data entry
     * @return List of HolderPostalAddress entity object
     */
    private List<HolderPostalAddress> retrieveHolderPostalAddress(HolderEdit holderEdit, boolean newEntry) {
        org.greenpole.entity.model.holder.Holder holderModel = new org.greenpole.entity.model.holder.Holder();
        holderModel.setHolderId(holderEdit.getHolderId());
        holderModel.setFirstName(holderEdit.getFirstName());
        holderModel.setLastName(holderEdit.getLastName());
        holderModel.setMiddleName(holderEdit.getMiddleName());
        holderModel.setAddresses(holderEdit.getAddresses());
        holderModel.setChn(holderEdit.getChn());
        holderModel.setDob(holderEdit.getDob());
        holderModel.setGender(holderEdit.getGender());
        holderModel.setHolderAcctNumber(holderEdit.getHolderAcctNumber());
        holderModel.setHolderPhoneNumbers(holderEdit.getHolderPhoneNumbers());
        holderModel.setHolderPostalAddresses(holderEdit.getHolderPostalAddresses());
        holderModel.setHolderResidentialAddresses(holderEdit.getHolderResidentialAddresses());
        holderModel.setPryAddress(holderEdit.getPryAddress());
        holderModel.setPryHolder(holderEdit.isPryHolder());
        holderModel.setTaxExempted(holderEdit.isTaxExempted());

        return retrieveHolderPostalAddress(holderModel, newEntry);
    }

    /**
     * Unwraps the holder postal address details from the HolderModel into
     * HolderPostalAddress hibernate entity
     *
     * @param holdModel the holderModel of holder details
     * @param newEntry boolean value indicating new entry
     * @return List object of HolderPostalAddress hibernate entity
     */
    private List<HolderPostalAddress> retrieveHolderPostalAddress(Holder holdModel, boolean newEntry) {
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
            postalAddressEntity.setAddressLine2(hpa.getAddressLine2());
            postalAddressEntity.setAddressLine3(hpa.getAddressLine3());
            postalAddressEntity.setCity(hpa.getCity());
            postalAddressEntity.setPostCode(hpa.getPostCode());
            postalAddressEntity.setIsPrimary(hpa.isPrimaryAddress());
            returnHolderPostalAddress.add(postalAddressEntity);
        }
        return returnHolderPostalAddress;
    }

    /**
     * An adapter of the retieveHolderPhoneNumber method that unwraps holder
     * edited holder details from HolderEdit object in holder model
     *
     * @param holderEdit object holding edited holder details
     * @param newEntry boolean value indicating new entry
     * @return List of HolderPhoneNumber objects
     */
    private List<HolderPhoneNumber> retrieveHolderPhoneNumber(HolderEdit holderEdit, boolean newEntry) {
        org.greenpole.entity.model.holder.Holder holderModel = new org.greenpole.entity.model.holder.Holder();
        holderModel.setHolderId(holderEdit.getHolderId());
        holderModel.setFirstName(holderEdit.getFirstName());
        holderModel.setLastName(holderEdit.getLastName());
        holderModel.setMiddleName(holderEdit.getMiddleName());
        holderModel.setAddresses(holderEdit.getAddresses());
        holderModel.setChn(holderEdit.getChn());
        holderModel.setDob(holderEdit.getDob());
        holderModel.setGender(holderEdit.getGender());
        holderModel.setHolderAcctNumber(holderEdit.getHolderAcctNumber());
        holderModel.setHolderPhoneNumbers(holderEdit.getHolderPhoneNumbers());
        holderModel.setHolderPostalAddresses(holderEdit.getHolderPostalAddresses());
        holderModel.setHolderResidentialAddresses(holderEdit.getHolderResidentialAddresses());
        holderModel.setPryAddress(holderEdit.getPryAddress());
        holderModel.setPryHolder(holderEdit.isPryHolder());
        holderModel.setTaxExempted(holderEdit.isTaxExempted());

        return retrieveHolderPhoneNumber(holderModel, newEntry);
    }

    /**
     * Unwraps holder phone number details from the holder model passed as
     * parameter into HolderPhoneNumber hibernate entity
     *
     * @param holdModel object of holder details
     * @param newEntry boolean variable indicating whether or not the entry is
     * new
     * @return List of HolderPhoneNumber objects
     */
    private List<HolderPhoneNumber> retrieveHolderPhoneNumber(Holder holdModel, boolean newEntry) {

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
        return returnPhoneNumber;
    }

    /**
     * An adapter method of the retrieveHolderResidentialAddress that unwraps
     * Holder residential address from the holderEdit object
     *
     * @param holderEdit object of edited holder details
     * @param newEntry boolean variable indicating whether or not the entry is
     * new
     * @return List of HolderResidentialAddress objects
     */
    private List<HolderResidentialAddress> retrieveHolderResidentialAddress(HolderEdit holderEdit, boolean newEntry) {
        org.greenpole.entity.model.holder.Holder holderModel = new org.greenpole.entity.model.holder.Holder();
        holderModel.setHolderId(holderEdit.getHolderId());
        holderModel.setFirstName(holderEdit.getFirstName());
        holderModel.setLastName(holderEdit.getLastName());
        holderModel.setMiddleName(holderEdit.getMiddleName());
        holderModel.setAddresses(holderEdit.getAddresses());
        holderModel.setChn(holderEdit.getChn());
        holderModel.setDob(holderEdit.getDob());
        holderModel.setGender(holderEdit.getGender());
        holderModel.setHolderAcctNumber(holderEdit.getHolderAcctNumber());
        holderModel.setHolderPhoneNumbers(holderEdit.getHolderPhoneNumbers());
        holderModel.setHolderPostalAddresses(holderEdit.getHolderPostalAddresses());
        holderModel.setHolderResidentialAddresses(holderEdit.getHolderResidentialAddresses());
        holderModel.setPryAddress(holderEdit.getPryAddress());
        holderModel.setPryHolder(holderEdit.isPryHolder());
        holderModel.setTaxExempted(holderEdit.isTaxExempted());

        return retrieveHolderResidentialAddress(holderModel, newEntry);
    }

    /**
     * Unwraps Holder residential address from the holder model into
     * HolderResidentialAddress hibernate entity object
     *
     * @param holdModel object of holder model
     * @param newEntry boolean variable indicating whether or not entry is new
     * @return List of HolderResidentialAddress hibernate entity objects
     */
    private List<HolderResidentialAddress> retrieveHolderResidentialAddress(Holder holdModel, boolean newEntry) {
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
            residentialAddressEntity.setAddressLine2(rAddy.getAddressLine2());
            residentialAddressEntity.setAddressLine3(rAddy.getAddressLine3());
            residentialAddressEntity.setAddressLine4(rAddy.getAddressLine4());
            residentialAddressEntity.setCity(rAddy.getCity());
            residentialAddressEntity.setPostCode(rAddy.getPostCode());

            returnResidentialAddress.add(residentialAddressEntity);
        }
        return returnResidentialAddress;
    }

    /**
     * Unwraps holder company account details from the holder model
     *
     * @param holdModel object of the holder model
     * @param newEntry boolean variable indicating whether or not entry is new
     * @return object of HolderCompanyAccount
     */
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

    /**
     * Generates unique file name from a combination of date and a random number
     *
     * @return
     */
    private String createSignatureFileName() {
        Date date = new Date();
        Random rand = new Random();
        int randomNumber = rand.nextInt(9999999);
        String fileName = randomNumber + "" + date.getTime();
        return fileName;
    }

    /**
     * Unwraps Holder bond account details from holder model
     *
     * @param holdModel object of holder model
     * @param newEntry boolean variable indicating whether or not entry is new
     * @return HolderBondAccount object
     */
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
