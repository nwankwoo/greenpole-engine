/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph.bondmodule;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.model.jeph.models.holder.Holder;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.hibernate.entity.*;
import org.greenpole.entity.model.jeph.models.holder.*;
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

    private final HolderComponentQuery ch;
    private static final Logger logger = LoggerFactory.getLogger(HolderComponent.class);

    public HolderComponent() {
        this.ch = new HolderComponentQueryImpl();
    }

    public Response createHolder_Request(Login login, String authenticator, Holder hold) {
        logger.info("request to create holder [{}] [{}]", "first_name", "last_name");

        Response res = new Response();
        NotificationWrapper wrapper;
        QueueSender queue;
        NotifierProperties prop;

        // check if necessary Holder actual paramenters are entered
        // TODO:    1. validate other parameters
        //          2. check if the user exist
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
                wrapper.setDescription("Authenticate creation of holder account, " + hold.getFirstName() + " " + hold.getLastName());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(holdList);
                res = queue.sendAuthorisationRequest(wrapper);
                logger.info("notification forwarded to queue - notification code: [{}]", wrapper.getCode());
            }
            return res;
        }

        // res.setRetn(200);
        // res.setDesc("holder account already exists and so cannot be created");
        // logger.info("holder account already exists and so cannot be created - [{}] [{}]", hold.getFirstName(), hold.getLastName());
        return res;
    }

    public Response createHolder_Authorise(String notificationCode) {
        Response res = new Response();
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
            holdEntity.setDob(holdModel.getDob());
            holdEntity.setChn(holdModel.getChn());
            
            boolean created;
            // determine if residential address was set or postal address
            if (holdModel.getHolderResidentialAddresses().isEmpty()) {
                created = ch.createHolderAccount(holdEntity, retrieveHolderCompanyAccount(holdModel), retrieveHolderPostalAddress(holdModel), retrieveHolderPhoneNumber(holdModel));
            } else {
                // boolean created = ch.createHolderAccount(holdEntity, retrieveHolderCompanyAccount(holdModel), retrieveHolderPostalAddress(holdModel), retrieveHolderPhoneNumber(holdModel));
                created = ch.createHolderAccount(holdEntity, retrieveHolderCompanyAccount(holdModel), retrieveHolderResidentialAddress(holdModel), retrieveHolderPhoneNumber(holdModel));
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

    private HolderPostalAddress retrieveHolderPostalAddress(Holder holdModel) {
        org.greenpole.hibernate.entity.HolderPostalAddress postalAddressEntity = new org.greenpole.hibernate.entity.HolderPostalAddress();
        List<org.greenpole.entity.model.jeph.models.holder.HolderPostalAddress> hpaddyList = holdModel.getHolderPostalAddresses();
        List<org.greenpole.hibernate.entity.HolderPostalAddress> returnHolderPostalAddress = new ArrayList<>();
        
        for (org.greenpole.entity.model.jeph.models.holder.HolderPostalAddress hpa : hpaddyList) {
            HolderPostalAddressId postalAddyId = new HolderPostalAddressId();
            postalAddyId.setHolderId(hpa.getHolderId());
            postalAddressEntity.setId(postalAddyId);
            postalAddressEntity.setIsPrimary(hpa.isIsPrimary());
            returnHolderPostalAddress.add(postalAddressEntity);
        }
        return (HolderPostalAddress) returnHolderPostalAddress;
    }

    private HolderPhoneNumber retrieveHolderPhoneNumber(Holder holdModel) {
        
        org.greenpole.hibernate.entity.HolderPhoneNumber phoneNumberEntity = new org.greenpole.hibernate.entity.HolderPhoneNumber();
        List<org.greenpole.entity.model.jeph.models.holder.HolderPhoneNumber> phoneNumberList = holdModel.getHolderPhoneNumbers();
        List<org.greenpole.hibernate.entity.HolderPhoneNumber> returnPhoneNumber = new ArrayList<>();
        
        for (org.greenpole.entity.model.jeph.models.holder.HolderPhoneNumber pnList : phoneNumberList) {
            HolderPhoneNumberId phoneNoId = new HolderPhoneNumberId();
            phoneNoId.setHolderId(pnList.getHolderId());
            
            phoneNumberEntity.setId(phoneNoId);
            // phoneNumberEntity.setIsPrimary(phoneNoId);
        }
        return (HolderPhoneNumber) returnPhoneNumber;
    }

    private HolderResidentialAddress retrieveHolderResidentialAddress(Holder holdModel) {
        org.greenpole.hibernate.entity.HolderResidentialAddress residentialAddressEntity = new org.greenpole.hibernate.entity.HolderResidentialAddress();
        List<org.greenpole.entity.model.jeph.models.holder.HolderResidentialAddress> residentialAddressList = holdModel.getHolderResidentialAddresses();
        List<org.greenpole.hibernate.entity.HolderResidentialAddress> returnResidentialAddress = new ArrayList();
        
        for (org.greenpole.entity.model.jeph.models.holder.HolderResidentialAddress rAddy : residentialAddressList) {
            HolderResidentialAddressId rAddyId = new HolderResidentialAddressId();
            rAddyId.setHolderId(rAddy.getHolderId());
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
        
        return (HolderResidentialAddress)returnResidentialAddress;
        
    }

    private HolderCompanyAccount retrieveHolderCompanyAccount(Holder holdModel) {
        org.greenpole.hibernate.entity.HolderCompanyAccount companyAccountEntity = new org.greenpole.hibernate.entity.HolderCompanyAccount();
        List<org.greenpole.entity.model.jeph.models.holder.HolderCompanyAccount> companyAccountList = holdModel.getHolderCompanyAccounts();
        List<org.greenpole.hibernate.entity.HolderCompanyAccount> returnCompanyAccountList = new ArrayList();
        
        for (org.greenpole.entity.model.jeph.models.holder.HolderCompanyAccount compAcct : companyAccountList) {
            HolderCompanyAccountId hCompAcctId = new HolderCompanyAccountId();
            hCompAcctId.setHolderId(compAcct.getHolderId());
            hCompAcctId.setClientCompanyId(compAcct.getClientCompanyId());
            
            // companyAccountEntity.setBank(compAcct.getBankId());
            companyAccountEntity.setChn(compAcct.getChn());
            companyAccountEntity.setId(hCompAcctId);
            returnCompanyAccountList.add(companyAccountEntity);
        }
        
        return (HolderCompanyAccount) returnCompanyAccountList;
    }
}
