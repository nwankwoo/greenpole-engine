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
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.model.Address;
import org.greenpole.entity.model.AddressTag;
import org.greenpole.entity.model.EmailAddress;
import org.greenpole.entity.model.PhoneNumber;
import org.greenpole.entity.model.holder.Holder;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.entrycode.jeph.mocks.SignatureProperties;
import org.greenpole.entity.model.holder.HolderSignature;
import org.greenpole.hibernate.entity.HolderBondAccount;
import org.greenpole.hibernate.entity.HolderBondAccountId;
import org.greenpole.hibernate.entity.HolderChangeType;
import org.greenpole.hibernate.entity.HolderChanges;
import org.greenpole.hibernate.entity.HolderCompanyAccount;
import org.greenpole.hibernate.entity.HolderCompanyAccountId;
import org.greenpole.hibernate.entity.HolderEmailAddress;
import org.greenpole.hibernate.entity.HolderEmailAddressId;
import org.greenpole.hibernate.entity.HolderPhoneNumber;
import org.greenpole.hibernate.entity.HolderPhoneNumberId;
import org.greenpole.hibernate.entity.HolderPostalAddress;
import org.greenpole.hibernate.entity.HolderPostalAddressId;
import org.greenpole.hibernate.entity.HolderResidentialAddress;
import org.greenpole.hibernate.entity.HolderResidentialAddressId;
import org.greenpole.hibernate.entity.HolderType;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.HolderComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.hibernate.query.impl.HolderComponentQueryImpl;
import org.greenpole.logic.HolderComponentLogic;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.BytesConverter;
import org.greenpole.util.GreenpoleFile;
import org.greenpole.util.Notification;
import org.greenpole.util.properties.GreenpoleProperties;
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
    private final HolderComponentQuery hq = ComponentQueryFactory.getHolderComponentQuery();
    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();
    private static final Logger logger = LoggerFactory.getLogger(HolderComponent.class);
    private final SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
    private final GreenpoleProperties greenProp = new GreenpoleProperties(HolderComponent.class);

    public HolderComponent() {
        this.hcq = new HolderComponentQueryImpl();
    }

    /**
     * Request to create a holder account.
     * @param login The user's login details
     * @param authenticator The authenticator user meant to receive the
     * notification
     * @param holder Object representing holder details
     * @return Response to create holder account request
     */
    /*public Response createShareHolder_Request(Login login, String authenticator, Holder holder) {
    Response resp = new Response();
    logger.info("Request to create holder account, invoked by [{}]", login.getUserId());
    
    NotificationWrapper wrapper;
    QueueSender queue;
    NotifierProperties prop;
    
    try {
    String desc = "";
    boolean flag = false;
    
    if (holder.getFirstName() == null || "".equals(holder.getFirstName())) {
    desc = "\nHolder first name should not be empty";
    } else if (holder.getLastName() == null || "".equals(holder.getLastName())) {
    desc += "\nHolder last name should not be empty";
    } else if (holder.getTypeId() <= 0) {
    desc += "\nHolder type should not be empty";
    }else if (holder.getPrimaryAddress() == null || "".equals(holder.getPrimaryAddress())) {
    desc += "\nPrimary Holder address is not specified";
    } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString())
    || holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
    desc += "\nPrimary address can only be residential or postal";
    } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString())
    && (holder.getHolderResidentialAddresses() == null || holder.getHolderResidentialAddresses().isEmpty())) {
    desc += "\nResidential address cannot be empty, as it is the primary address";
    } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())
    && (holder.getHolderPostalAddresses() == null || holder.getHolderPostalAddresses().isEmpty())) {
    desc += "\nPostal address cannot be empty, as it is the primary address";
    } else {
    flag = true;
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
    
    if (flag && holder.getHolderResidentialAddresses() != null && !holder.getHolderResidentialAddresses().isEmpty()) {
    for (Address addr : holder.getHolderResidentialAddresses()) {
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
    
    if (flag && holder.getHolderPostalAddresses() != null && !holder.getHolderPostalAddresses().isEmpty()) {
    for (Address addr : holder.getHolderPostalAddresses()) {
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
    
    if (flag && holder.getHolderEmailAddresses() != null && !holder.getHolderEmailAddresses().isEmpty()) {
    for (EmailAddress email : holder.getHolderEmailAddresses()) {
    if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
    desc += "\nEmail address should not be empty. Delete email entry if you must";
    flag = false;
    break;
    }
    }
    }
    
    if (flag && holder.getHolderPhoneNumbers() != null && !holder.getHolderPhoneNumbers().isEmpty()) {
    for (PhoneNumber phone : holder.getHolderPhoneNumbers()) {
    if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
    desc += "\nPhone number should not be empty. Delete phone number entry if you must";
    flag = false;
    break;
    }
    }
    }
    
    if (flag) {
    
    wrapper = new NotificationWrapper();
    prop = new NotifierProperties(HolderComponent.class);
    queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
    
    List<Holder> holdList = new ArrayList<>();
    holdList.add(holder);
    
    wrapper.setCode(Notification.createCode(login));
    wrapper.setDescription("Authenticate holder account creation for " + holder.getFirstName() + " " + holder.getLastName());
    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
    wrapper.setFrom(login.getUserId());
    wrapper.setTo(authenticator);
    wrapper.setModel(holdList);
    resp = queue.sendAuthorisationRequest(wrapper);
    logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
    return resp;
    }
    resp.setRetn(300);
    resp.setDesc("Error filing holder details: " + desc);
    logger.info("Error filing holder details: [{}] - [{}]", desc, login.getUserId());
    return resp;
    } catch (Exception ex) {
    resp.setRetn(99);
    resp.setDesc("General Error: Unable to create holder account. Contact system administrator." + "\nMessage: " + ex.getMessage());
    logger.info("Error creating holder account. See error log. [{}] - [{}]", resp.getRetn(), login.getUserId());
    logger.error("Error creating holder account - [" + login.getUserId() + "]", ex);
    return resp;
    }
    }*/

    /**
     * Processes saved request to create holder account.
     * @param login The user's login details
     * @param notificationCode The notification code
     * @return response to holder account creation request
     */
    /*public Response createShareHolder_Authorise(Login login, String notificationCode) {
    logger.info("authorise shareholder account creation, invoked by [{}]", login.getUserId());
    Response resp = new Response();
    
    try {
    NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
    List<Holder> holdList = (List<Holder>) wrapper.getModel();
    Holder holder = holdList.get(0);
    
    String desc = "";
    boolean flag = false;
    
    if (holder.getFirstName() == null || "".equals(holder.getFirstName())) {
    desc = "\nHolder first name should not be empty";
    } else if (holder.getLastName() == null || "".equals(holder.getLastName())) {
    desc += "\nHolder last name should not be empty";
    } else if (holder.getTypeId() <= 0) {
    desc += "\nHolder type should not be empty";
    }else if (holder.getPrimaryAddress() == null || "".equals(holder.getPrimaryAddress())) {
    desc += "\nPrimary Holder address is not specified";
    } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString())
    || holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
    desc += "\nPrimary address can only be residential or postal";
    } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString())
    && (holder.getHolderResidentialAddresses() == null || holder.getHolderResidentialAddresses().isEmpty())) {
    desc += "\nResidential address cannot be empty, as it is the primary address";
    } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())
    && (holder.getHolderPostalAddresses() == null || holder.getHolderPostalAddresses().isEmpty())) {
    desc += "\nPostal address cannot be empty, as it is the primary address";
    } else {
    flag = true;
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
    
    if (flag && holder.getHolderResidentialAddresses() != null && !holder.getHolderResidentialAddresses().isEmpty()) {
    for (Address addr : holder.getHolderResidentialAddresses()) {
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
    
    if (flag && holder.getHolderPostalAddresses() != null && !holder.getHolderPostalAddresses().isEmpty()) {
    for (Address addr : holder.getHolderPostalAddresses()) {
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
    
    if (flag && holder.getHolderEmailAddresses() != null && !holder.getHolderEmailAddresses().isEmpty()) {
    for (EmailAddress email : holder.getHolderEmailAddresses()) {
    if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
    desc += "\nEmail address should not be empty. Delete email entry if you must";
    flag = false;
    break;
    }
    }
    }
    
    if (flag && holder.getHolderPhoneNumbers() != null && !holder.getHolderPhoneNumbers().isEmpty()) {
    for (PhoneNumber phone : holder.getHolderPhoneNumbers()) {
    if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
    desc += "\nPhone number should not be empty. Delete phone number entry if you must";
    flag = false;
    break;
    }
    }
    }
    
    if (flag) {
    org.greenpole.hibernate.entity.Holder holdEntity = new org.greenpole.hibernate.entity.Holder();
    HolderType typeEntity = new HolderType();
    
    typeEntity.setId(holder.getTypeId());
    
    holdEntity.setFirstName(holder.getFirstName());
    holdEntity.setLastName(holder.getLastName());
    holdEntity.setMiddleName(holder.getMiddleName());
    holdEntity.setType(typeEntity);
    holdEntity.setGender(holder.getGender());
    holdEntity.setDob(formatter.parse(holder.getDob()));
    
    boolean created;
    
    if (holder.getChn() == null || "".equals(holder.getChn())) {
    //there are two versions of createHolderAccount, one for bond account and the other for company account
    //thus, null cannot be directly passed into the method, as java will not be able to distinguish between
    //both createHolderAccount methods.
    HolderCompanyAccount emptyAcct = null;
    created = hq.createHolderAccount(holdEntity, emptyAcct,
    retrieveHolderResidentialAddress(holder), retrieveHolderPostalAddress(holder),
    retrieveHolderEmailAddress(holder), retrieveHolderPhoneNumber(holder));
    } else {
    holdEntity.setChn(holder.getChn());//set chn
    created = hq.createHolderAccount(holdEntity, retrieveHolderCompanyAccount(holder),
    retrieveHolderResidentialAddress(holder), retrieveHolderPostalAddress(holder),
    retrieveHolderEmailAddress(holder), retrieveHolderPhoneNumber(holder));
    }
    
    if (created) {
    resp.setRetn(0);
    resp.setDesc("Holder details saved: Successful");
    logger.info("Shareholder account creation successful - [{}]", login.getUserId());
    return resp;
    } else {
    resp.setRetn(99);
    resp.setDesc("General Error. Unable to persist holder account. Contact system administrator.");
    logger.info("Error persist holder account - [{}]", login.getUserId());
    return resp;
    }
    }
    resp.setRetn(300);
    resp.setDesc("Error filing holder details: " + desc);
    logger.info("Error filing holder details: [{}] - [{}]", desc, login.getUserId());
    return resp;
    } catch (JAXBException ex) {
    resp.setRetn(98);
    resp.setDesc("Error loading notification xml file. See error log");
    logger.info("Error loading notification xml file. See error log - [{}]", login.getUserId());
    logger.error("Error loading notification xml file to object - [" + login.getUserId() + "] - ", ex);
    return resp;
    } catch (Exception ex) {
    resp.setRetn(99);
    resp.setDesc("General Error. Unable to persist holder account. Contact system administrator." + "\nMessage: " + ex.getMessage());
    logger.info("Error persist holder account. See error log - [{}]", login.getUserId());
    logger.error("Error persist holder account - [" + login.getUserId() + "] - ", ex);
    return resp;
    }
    }*/

    /**
     * Request to create bond holder account.
     * @param login user's login details
     * @param authenticator the authenticator user meant to receive the
     * notification
     * @param holder the holder details object
     * @return response to the bondholder account creation request
     */
    /*public Response createBondHolderAccount_Request(Login login, String authenticator, Holder holder) {
    logger.info("Request to create bondholder account, invoked by [{}]", login.getUserId());
    Response resp = new Response();
    
    try {
    NotificationWrapper wrapper;
    QueueSender queue;
    NotifierProperties prop;
    
    String desc = "";
    boolean flag = false;
    
    if (holder.getFirstName() == null || "".equals(holder.getFirstName())) {
    desc = "\nHolder first name should not be empty";
    } else if (holder.getLastName() == null || "".equals(holder.getLastName())) {
    desc += "\nHolder last name should not be empty";
    } else if (holder.getChn() == null || "".equals(holder.getChn())) {
    desc += "\nCHN cannot be empty";
    } else if (holder.getTypeId() <= 0) {
    desc += "\nHolder type should not be empty";
    }else if (holder.getPrimaryAddress() == null || "".equals(holder.getPrimaryAddress())) {
    desc += "\nPrimary Holder address is not specified";
    } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString())
    || holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
    desc += "\nPrimary address can only be residential or postal";
    } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString())
    && (holder.getHolderResidentialAddresses() == null || holder.getHolderResidentialAddresses().isEmpty())) {
    desc += "\nResidential address cannot be empty, as it is the primary address";
    } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())
    && (holder.getHolderPostalAddresses() == null || holder.getHolderPostalAddresses().isEmpty())) {
    desc += "\nPostal address cannot be empty, as it is the primary address";
    } else {
    flag = true;
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
    
    if (flag && holder.getHolderResidentialAddresses() != null && !holder.getHolderResidentialAddresses().isEmpty()) {
    for (Address addr : holder.getHolderResidentialAddresses()) {
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
    
    if (flag && holder.getHolderPostalAddresses() != null && !holder.getHolderPostalAddresses().isEmpty()) {
    for (Address addr : holder.getHolderPostalAddresses()) {
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
    
    if (flag && holder.getHolderEmailAddresses() != null && !holder.getHolderEmailAddresses().isEmpty()) {
    for (EmailAddress email : holder.getHolderEmailAddresses()) {
    if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
    desc += "\nEmail address should not be empty. Delete email entry if you must";
    flag = false;
    break;
    }
    }
    }
    
    if (flag && holder.getHolderPhoneNumbers() != null && !holder.getHolderPhoneNumbers().isEmpty()) {
    for (PhoneNumber phone : holder.getHolderPhoneNumbers()) {
    if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
    desc += "\nPhone number should not be empty. Delete phone number entry if you must";
    flag = false;
    break;
    }
    }
    }
    
    if (flag) {
    wrapper = new NotificationWrapper();
    prop = new NotifierProperties(HolderComponent.class);
    queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
    
    List<Holder> holdList = new ArrayList<>();
    holdList.add(holder);
    
    wrapper.setCode(Notification.createCode(login));
    wrapper.setDescription("Authenticate bond holder account, " + holder.getFirstName() + " " + holder.getLastName());
    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
    wrapper.setFrom(login.getUserId());
    wrapper.setTo(authenticator);
    wrapper.setModel(holdList);
    resp = queue.sendAuthorisationRequest(wrapper);
    logger.info("notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
    return resp;
    }
    resp.setRetn(300);
    resp.setDesc("Error filing bond holder details: " + desc);
    logger.info("Error filing bond holder details: [{}] - [{}]", desc, login.getUserId());
    return resp;
    } catch (Exception ex) {
    resp.setRetn(99);
    resp.setDesc("General Error: Unable to create holder account. Contact system administrator." + "\nMessage: " + ex.getMessage());
    logger.info("Error creating holder account. See error log - [{}]", login.getUserId());
    logger.error("Error creating holder account - [" + login.getUserId() + "] - ", ex);
    return resp;
    }
    }*/

    /**
     * Processes saved request to create bondholder account.
     * @param login user's login details
     * @param notificationCode the notification code
     * @return response to the bondholder account creation request
     */
    /*public Response createBondHolderAccount_Authorise(Login login, String notificationCode) {
    logger.info("authorise bondholder creation, invoked by - [{}]", login.getUserId());
    Response resp = new Response();
    
    try {
    logger.info("Holder creation authorised. [{}] - [{}]", notificationCode, login.getUserId());
    NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
    List<Holder> holdList = (List<Holder>) wrapper.getModel();
    Holder holder = holdList.get(0);
    
    String desc = "";
    boolean flag = false;
    
    if (holder.getFirstName() == null || "".equals(holder.getFirstName())) {
    desc = "\nHolder first name should not be empty";
    } else if (holder.getLastName() == null || "".equals(holder.getLastName())) {
    desc += "\nHolder last name should not be empty";
    } else if (holder.getChn() == null || "".equals(holder.getChn())) {
    desc += "\nCHN cannot be empty";
    } else if (holder.getTypeId() <= 0) {
    desc += "\nHolder type should not be empty";
    }else if (holder.getPrimaryAddress() == null || "".equals(holder.getPrimaryAddress())) {
    desc += "\nPrimary Holder address is not specified";
    } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString())
    || holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
    desc += "\nPrimary address can only be residential or postal";
    } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString())
    && (holder.getHolderResidentialAddresses() == null || holder.getHolderResidentialAddresses().isEmpty())) {
    desc += "\nResidential address cannot be empty, as it is the primary address";
    } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())
    && (holder.getHolderPostalAddresses() == null || holder.getHolderPostalAddresses().isEmpty())) {
    desc += "\nPostal address cannot be empty, as it is the primary address";
    } else {
    flag = true;
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
    
    if (flag && holder.getHolderResidentialAddresses() != null && !holder.getHolderResidentialAddresses().isEmpty()) {
    for (Address addr : holder.getHolderResidentialAddresses()) {
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
    
    if (flag && holder.getHolderPostalAddresses() != null && !holder.getHolderPostalAddresses().isEmpty()) {
    for (Address addr : holder.getHolderPostalAddresses()) {
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
    
    if (flag && holder.getHolderEmailAddresses() != null && !holder.getHolderEmailAddresses().isEmpty()) {
    for (EmailAddress email : holder.getHolderEmailAddresses()) {
    if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
    desc += "\nEmail address should not be empty. Delete email entry if you must";
    flag = false;
    break;
    }
    }
    }
    
    if (flag && holder.getHolderPhoneNumbers() != null && !holder.getHolderPhoneNumbers().isEmpty()) {
    for (PhoneNumber phone : holder.getHolderPhoneNumbers()) {
    if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
    desc += "\nPhone number should not be empty. Delete phone number entry if you must";
    flag = false;
    break;
    }
    }
    }
    
    if (flag) {
    org.greenpole.hibernate.entity.Holder holdEntity = new org.greenpole.hibernate.entity.Holder();
    HolderType typeEntity = new HolderType();
    
    typeEntity.setId(holder.getTypeId());
    
    holdEntity.setFirstName(holder.getFirstName());
    holdEntity.setLastName(holder.getLastName());
    holdEntity.setMiddleName(holder.getMiddleName());
    holdEntity.setType(typeEntity);
    holdEntity.setGender(holder.getGender());
    holdEntity.setDob(formatter.parse(holder.getDob()));
    holdEntity.setChn(holder.getChn());
    
    boolean created = hq.createHolderAccount(holdEntity, retrieveHolderBondAccount(holder),
    retrieveHolderResidentialAddress(holder), retrieveHolderPostalAddress(holder),
    retrieveHolderEmailAddress(holder), retrieveHolderPhoneNumber(holder));
    
    if (created) {
    resp.setRetn(0);
    resp.setDesc("Successful");
    logger.info("Bond holder account creation successful - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);
    resp.setDesc("Unable to create bond holder from authorisation. Contact System Administrator");
    logger.info("Unable to create bond holder from authorisation - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);
    resp.setDesc("Error filing bond holder details: " + desc);
    logger.info("Error filing bond holder details: [{}] - [{}]", desc, login.getUserId());
    return resp;
    } catch (JAXBException ex) {
    resp.setRetn(98);
    resp.setDesc("Error loading notification xml file. Contact administrator");
    logger.info("Error loading notification xml file. See error log - [{}]", login.getUserId());
    logger.error("Error loading notification xml file to object, invoked by [" + login.getUserId() + "] - ", ex);
    return resp;
    } catch (ParseException ex) {
    resp.setRetn(300);
    resp.setDesc("Error creating bondholder account, due to formatting with date of birth");
    logger.info("Error creating bondholder account, due to formatting with date of birth - [{}]", login.getUserId());
    logger.error("Error creating bondholder account - [" + login.getUserId() + "] - ", ex);
    return resp;
    } catch (Exception ex) {
    resp.setRetn(99);
    resp.setDesc("Error creating bondholder account. Contact administrator");
    logger.info("Error creating bondholder account - See error log - [{}]", login.getUserId());
    logger.error("Error creating bondholder account - [" + login.getUserId() + "] - ", ex);
    return resp;
    }
    }*/
    
    /**
     * Request to upload a holder signature.
     * @param login The user's login details
     * @param authenticator The authenticator meant to receive the notification
     * @param holderSig Holder signature details
     * @return response to the upload holder signature request
     */
    /*public Response uploadHolderSignature_Request(Login login, String authenticator, HolderSignature holderSig) {
    logger.info("Request to upload holder signature, invoked by [{}]", login.getUserId());
    Response resp = new Response();
    
    NotificationWrapper wrapper;
    QueueSender queue;
    NotifierProperties prop;
    
    boolean flag = false;
    
    try {
    org.greenpole.hibernate.entity.Holder holder = new org.greenpole.hibernate.entity.Holder();
    long defaultSize = Long.valueOf(greenProp.getSignatureSize());
    
    if (hq.checkHolderAccount(holderSig.getHolderId())) {
    holder = hq.getHolder(holderSig.getHolderId());
    logger.info("Holder [{}] checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
    
    if (hq.checkCurrentSignature(holderSig.getHolderId())) {
    logger.info("Holder has current signature - [{}]", login.getUserId());
    flag = true;
    } else {
    flag = true;
    }
    }
    
    if (flag) {
    long fileSize = BytesConverter.decodeToBytes(holderSig.getSignatureContent()).length;
    
    if (fileSize <= defaultSize) {
    wrapper = new NotificationWrapper();
    prop = new NotifierProperties(HolderComponent.class);
    queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
    
    List<HolderSignature> holderListSignature = new ArrayList<>();
    holderListSignature.add(holderSig);
    
    wrapper.setCode(Notification.createCode(login));
    wrapper.setDescription("Authenticate creation of holder signature for holder " + holder.getFirstName() + " " + holder.getLastName());
    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
    wrapper.setFrom(login.getUserId());
    wrapper.setTo(authenticator);
    wrapper.setModel(holderListSignature);
    
    logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
    resp = queue.sendAuthorisationRequest(wrapper);
    return resp;
    }
    resp.setRetn(300);
    resp.setDesc("Error uploading signature. Signature can be no bigger than 2MB");
    logger.info("Error uploading signature. Signature can be no bigger than 2MB: [{}] - [{}]", resp.getRetn(), login.getUserId());
    return resp;
    }
    resp.setRetn(300);
    resp.setDesc("The holder does not exist.");
    logger.info("The holder does not exist - [{}]", login.getUserId());
    return resp;
    } catch (Exception ex) {
    logger.info("error proccessing holder signature upload. See error log - [{}]", login.getUserId());
    logger.error("error proccessing holder signature upload - [" + login.getUserId() + "]", ex);
    
    resp.setRetn(99);
    resp.setDesc("General error. Unable to proccess holder signature upload. Contact system administrator."
    + "\nMessage: " + ex.getMessage());
    return resp;
    }
    }*/

    /**
     * Processes the saved request to upload holder signature.
     * @param login user's login details
     * @param notificationCode the notification code
     * @return response to the upload holder signature request
     */
    /*public Response uploadHolderSignature_Authorise(Login login, String notificationCode) {
    logger.info("authorise holder signature upload, invoked by [{}]", login.getUserId());
    Response resp = new Response();
    
    try {
    NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
    List<HolderSignature> holdSigntureList = (List<HolderSignature>) wrapper.getModel();
    HolderSignature sigModel = holdSigntureList.get(0);
    
    org.greenpole.hibernate.entity.HolderSignature sigEntity = new org.greenpole.hibernate.entity.HolderSignature();
    org.greenpole.hibernate.entity.HolderSignature currentSig = new org.greenpole.hibernate.entity.HolderSignature();
    
    long defaultSize = Long.valueOf(greenProp.getSignatureSize());
    
    if (hq.checkHolderAccount(sigModel.getHolderId())) {
    org.greenpole.hibernate.entity.Holder holder = hq.getHolder(sigModel.getHolderId());
    logger.info("Holder [{}] checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
    
    boolean currentSigExists = hq.checkCurrentSignature(sigModel.getHolderId());
    if (currentSigExists) {
    currentSig = hq.getCurrentSignature(sigModel.getHolderId());
    logger.info("Holder has current signature - [{}]", login.getUserId());
    }
    
    GreenpoleFile file = new GreenpoleFile(greenProp.getSignaturePath());
    long fileSize = BytesConverter.decodeToBytes(sigModel.getSignatureContent()).length;
    
    if (fileSize <= defaultSize) {
    logger.info("Holder signature met file size requirement - [{}]", login.getUserId());
    
    if (file.createFile(BytesConverter.decodeToBytes(sigModel.getSignatureContent()))) {
    logger.info("Holder signature file created and saved - [{}]", login.getUserId());
    
    String filepath = file.getFolderPath() + file.getFileName();
    
    sigEntity.setTitle(sigModel.getTitle());
    sigEntity.setSignaturePath(filepath);
    
    sigEntity.setHolderSignaturePrimary(true);
    sigEntity.setHolder(holder);
    
    boolean uploaded;
    if (currentSigExists) {
    uploaded = hq.uploadHolderSignature(sigEntity, currentSig);
    } else {
    uploaded = hq.uploadHolderSignature(sigEntity, null);
    }
    
    if (uploaded) {
    resp.setRetn(0);
    resp.setDesc("Successful");
    logger.info("Holder signature successfully uploaded - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);
    resp.setDesc("Holder signature upload failed due to database error. Contact Administrator.");
    logger.info("Holder signature upload failed due to database error. Check error logs - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);
    resp.setDesc("The Holder signature file could not be uploaded onto the server. Contact Administrator");
    logger.info("The Holder signature file could not be uploaded onto the server. See error logs - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);
    resp.setDesc("The size of the holder signature cannot exceed 2MB.");
    logger.info("The size of the holder signature cannot exceed 2MB - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);
    resp.setDesc("The holder does not exist.");
    logger.info("The holder does not exist - [{}]", login.getUserId());
    return resp;
    } catch (JAXBException ex) {
    logger.info("error loading notification xml file. See error log - [{}]", login.getUserId());
    logger.error("error loading notification xml file to object - [" + login.getUserId() + "]", ex);
    
    resp.setRetn(98);
    resp.setDesc("Unable to upload holder signature. Contact System Administrator");
    
    return resp;
    } catch (Exception ex) {
    logger.info("error uploading holder signature. See error log - [{}]", login.getUserId());
    logger.error("error uploading holder signature - [" + login.getUserId() + "]", ex);
    
    resp.setRetn(99);
    resp.setDesc("General error. Unable to upload holder signature. Contact system administrator."
    + "\nMessage: " + ex.getMessage());
    return resp;
    }
    }*/

    /**
     * Request to query holder signature.
     * @param login user's login details
     * @param authenticator the authenticator user meant to receive the
     * notification details
     * @param queryParams the query parameters
     * @return response to query holder signature request
     */
    /*public Response queryHolderSignature_Request(Login login, String authenticator, HolderSignature queryParams) {
    logger.info("request to query holder signature, invoked by [{}]", login.getUserId());
    Response resp = new Response();
    
    try {
    HolderSignature sigModel = new HolderSignature();
    
    if (hq.checkHolderAccount(queryParams.getHolderId())) {
    org.greenpole.hibernate.entity.Holder holder = hq.getHolder(queryParams.getHolderId());
    logger.info("Holder [{}] checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
    
    org.greenpole.hibernate.entity.HolderSignature sig_hib = hq.getCurrentSignature(queryParams.getHolderId());
    
    File file = new File(sig_hib.getSignaturePath());
    byte[] read = Files.readAllBytes(file.toPath());
    String encodedContents = BytesConverter.encodeToString(read);
    logger.info("Holder signature file successfully read - [{}]", login.getUserId());
    
    sigModel.setId(sig_hib.getId());
    sigModel.setHolderId(sig_hib.getHolder().getId());
    sigModel.setTitle(sig_hib.getTitle());
    sigModel.setSignaturePath(sig_hib.getSignaturePath());
    sigModel.setSignatureContent(encodedContents);
    sigModel.setPrimarySignature(sig_hib.isHolderSignaturePrimary());
    
    List<HolderSignature> sig_result = new ArrayList<>();
    sig_result.add(sigModel);
    
    resp.setRetn(0);
    resp.setDesc("Successful");
    resp.setBody(sig_result);
    logger.info("Holder signature query successful - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);
    resp.setDesc("The holder does not exist.");
    logger.info("The holder does not exist - [{}]", login.getUserId());
    return resp;
    } catch (Exception ex) {
    logger.info("error querying holder signature. See error log - [{}]", login.getUserId());
    logger.error("error querying holder signature - [" + login.getUserId() + "]", ex);
    
    resp.setRetn(99);
    resp.setDesc("General error. Unable to query holder signature. Contact system administrator."
    + "\nMessage: " + ex.getMessage());
    return resp;
    }
    }*/

    /**
     * Retrieves an image in byte array
     *
     * @param signaturePath the full path of the image on disk
     * @return byte array object of the image
     */
    private byte[] readSignatureFile(String signaturePath) {
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
     * Request to transpose holder name.
     * @param login user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param holder the holder detail
     * @return response to the transpose name request
     */
    /*public Response transposeHolderName_Request(Login login, String authenticator, Holder holder) {
    logger.info("request to transpose holder name, invoked by [{}]", login.getUserId());
    Response resp = new Response();
    
    NotificationWrapper wrapper;
    QueueSender queue;
    NotifierProperties prop;
    
    String desc = "";
    boolean flag = false;
    
    try {
    
    if (hq.checkHolderAccount(holder.getHolderId())) {
    org.greenpole.hibernate.entity.Holder holder_hib = hq.getHolder(holder.getHolderId());
    logger.info("holder does not exits - [{}] [{}]", holder.getFirstName(), holder.getLastName());
    
    if (!"".equals(holder.getFirstName()) || holder.getFirstName() != null) {
    desc = "\nThe holder's first name cannot be empty";
    } else if (!"".equals(holder.getLastName()) || holder.getLastName() != null) {
    desc = "\nThe holder's last name cannot be empty";
    } else {
    flag = true;
    }
    
    if (flag) {
    wrapper = new NotificationWrapper();
    prop = new NotifierProperties(HolderComponent.class);
    queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
    
    List<Holder> holdList = new ArrayList<>();
    holdList.add(holder);
    
    wrapper.setCode(Notification.createCode(login));
    wrapper.setDescription("Authenticate transpose request for holder, " + holder_hib.getFirstName() + " " + holder_hib.getLastName());
    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
    wrapper.setFrom(login.getUserId());
    wrapper.setTo(authenticator);
    wrapper.setModel(holdList);
    
    logger.info("notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
    resp = queue.sendAuthorisationRequest(wrapper);
    return resp;
    }
    resp.setRetn(310);
    resp.setDesc("Error: " + desc);
    logger.info("error detected in holder name tranpose process - [{}]: [{}]", login.getUserId(), resp.getRetn());
    return resp;
    }
    resp.setRetn(300);
    resp.setDesc("The holder does not exist.");
    logger.info("The holder does not exist - [{}]", login.getUserId());
    return resp;
    } catch (Exception ex) {
    logger.info("error proccessing holder name transpose. See error log - [{}]", login.getUserId());
    logger.error("error proccessing holder name transpose - [" + login.getUserId() + "]", ex);
    
    resp.setRetn(99);
    resp.setDesc("General error. Unable to proccess holder name transpose. Contact system administrator."
    + "\nMessage: " + ex.getMessage());
    return resp;
    }
    }*/

    /**
     * Processes request to transpose holder names.
     * @param login user's login details
     * @param notificationCode the notification code
     * @return response object for the authorised request
     */
    /*public Response transposeHolderName_Authorise(Login login, String notificationCode) {
    logger.info("authorise holder name transpose, invoked by [{}]", login.getUserId());
    Response resp = new Response();
    
    String desc = "";
    boolean flag = false;
    
    try {
    NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
    List<Holder> holdList = (List<Holder>) wrapper.getModel();
    Holder holder = holdList.get(0);
    
    if (hq.checkHolderAccount(holder.getHolderId())) {
    org.greenpole.hibernate.entity.Holder holder_hib = hq.getHolder(holder.getHolderId());
    logger.info("holder does not exits - [{}] [{}]", holder.getFirstName(), holder.getLastName());
    
    if (!"".equals(holder.getFirstName()) || holder.getFirstName() != null) {
    desc = "\nThe holder's first name cannot be empty";
    } else if (!"".equals(holder.getLastName()) || holder.getLastName() != null) {
    desc = "\nThe holder's last name cannot be empty";
    } else {
    flag = true;
    }
    
    if (flag) {
    holder_hib.setFirstName(holder.getFirstName());
    holder_hib.setLastName(holder.getLastName());
    
    hq.updateHolderAccountForTranspose(holder_hib);
    
    resp.setRetn(0);
    resp.setDesc("Successful");
    logger.info("Holder name transpose sucessful - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);
    resp.setDesc("Error: " + desc);
    logger.info("error detected in holder name tranpose process - [{}]: [{}]", login.getUserId(), resp.getRetn());
    return resp;
    }
    resp.setRetn(300);
    resp.setDesc("The holder does not exist.");
    logger.info("The holder does not exist - [{}]", login.getUserId());
    return resp;
    } catch (JAXBException ex) {
    logger.info("error loading notification xml file. See error log - [{}]", login.getUserId());
    logger.error("error loading notification xml file to object - [" + login.getUserId() + "]", ex);
    
    resp.setRetn(98);
    resp.setDesc("Unable to proccess holder name transpose. Contact System Administrator");
    
    return resp;
    } catch (Exception ex) {
    logger.info("error proccessing holder name transpose. See error log - [{}]", login.getUserId());
    logger.error("error proccessing holder name transpose - [" + login.getUserId() + "]", ex);
    
    resp.setRetn(99);
    resp.setDesc("General error. Unable to proccess holder name transpose. Contact system administrator."
    + "\nMessage: " + ex.getMessage());
    return resp;
    }
    }*/

    /**
     * Request to edit holder details.
     * @param login user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param holder the edited holder details
     * @return response to the edit holder details request
     */
    /* public Response editHolderDetails_Request(Login login, String authenticator, Holder holder) {
    
    Response resp = new Response();
    NotificationWrapper wrapper;
    QueueSender queue;
    NotifierProperties prop;
    
    String desc = "";
    boolean flag = false;
    
    try {
    wrapper = new NotificationWrapper();
    prop = new NotifierProperties(HolderComponent.class);
    queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
    
    //holder must exist
    if (hq.checkHolderAccount(holder.getHolderId())) {
    org.greenpole.hibernate.entity.Holder holderEntity = hq.getHolder(holder.getHolderId());
    logger.info("holder exists - [{}]: [{}]", login.getUserId(), holderEntity.getFirstName() + " " + holderEntity.getLastName());
    
    //holder must be primary to be edited
    if (holderEntity.isPryHolder()) {
    
    if (holder.getFirstName() == null || "".equals(holder.getFirstName())) {
    desc = "\nHolder first name should not be empty";
    } else if (holder.getLastName() == null || "".equals(holder.getLastName())) {
    desc += "\nHolder last name should not be empty";
    } else if (holder.getTypeId() <= 0) {
    desc += "\nHolder type should not be empty";
    } else if (holder.getPrimaryAddress() == null || "".equals(holder.getPrimaryAddress())) {
    desc += "\nPrimary Holder address is not specified";
    } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString())
    || holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
    desc += "\nPrimary address can only be residential or postal";
    } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString())
    && (holder.getHolderResidentialAddresses() == null || holder.getHolderResidentialAddresses().isEmpty())) {
    desc += "\nResidential address cannot be empty, as it is the primary address";
    } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())
    && (holder.getHolderPostalAddresses() == null || holder.getHolderPostalAddresses().isEmpty())) {
    desc += "\nPostal address cannot be empty, as it is the primary address";
    } else {
    flag = true;
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
    
    if (flag && holder.getHolderResidentialAddresses() != null && !holder.getHolderResidentialAddresses().isEmpty()) {
    for (Address addr : holder.getHolderResidentialAddresses()) {
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
    
    if (flag && holder.getHolderPostalAddresses() != null && !holder.getHolderPostalAddresses().isEmpty()) {
    for (Address addr : holder.getHolderPostalAddresses()) {
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
    
    if (flag && holder.getHolderEmailAddresses() != null && !holder.getHolderEmailAddresses().isEmpty()) {
    for (EmailAddress email : holder.getHolderEmailAddresses()) {
    if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
    desc += "\nEmail address should not be empty. Delete email entry if you must";
    flag = false;
    break;
    }
    }
    }
    
    if (flag && holder.getHolderPhoneNumbers() != null && !holder.getHolderPhoneNumbers().isEmpty()) {
    for (PhoneNumber phone : holder.getHolderPhoneNumbers()) {
    if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
    desc += "\nPhone number should not be empty. Delete phone number entry if you must";
    flag = false;
    break;
    }
    }
    }
    
    if (flag && (holderEntity.getChn() != null || "".equals(holderEntity.getChn()))
    && (holder.getChn() == null || "".equals(holder.getChn()))) {
    desc += "\nCHN cannot be erased";
    flag = false;
    }
    
    if (flag) {
    List<Holder> holdList = new ArrayList<>();
    holdList.add(holder);
    wrapper.setCode(Notification.createCode(login));
    wrapper.setDescription("Authenticate edit of holder account, " + holder.getFirstName() + " " + holder.getLastName());
    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
    wrapper.setFrom(login.getUserId());
    wrapper.setTo(authenticator);
    wrapper.setModel(holdList);
    
    logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
    resp = queue.sendAuthorisationRequest(wrapper);
    return resp;
    }
    resp.setRetn(300);
    resp.setDesc("Error filing holder details: " + desc);
    logger.info("Error filing holder details: [{}] - [{}]", desc, login.getUserId());
    return resp;
    }
    resp.setRetn(300);
    resp.setDesc("Holder account is not a primary account. Non-primary accounts cannot be edited");
    logger.info("Holder account is not a primary account. Non-primary accounts cannot be edited - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);
    resp.setDesc("The holder does not exist.");
    logger.info("The holder does not exist - [{}]", login.getUserId());
    return resp;
    } catch (Exception ex) {
    logger.info("Error editing holder account. See error log - [{}]", login.getUserId());
    logger.error("Error editing holder account - [" + login.getUserId() + "] - ", ex);
    resp.setRetn(99);
    resp.setDesc("General Error. Unable to editing holder account. Contact system administrator." + "\nMessage: " + ex.getMessage());
    return resp;
    }
    }*/

    /**
     * Processes request authorisation to edit holder details
     *
     * @param login user's login details
     * @param notificationCode the notification code
     * @return response object for the authorized edit holder details request
     */
    /*public Response editHolderDetails_Authorise(Login login, String notificationCode) {
    logger.info("request authorisation to persist holder details. Invoked by [{}]", login.getUserId());
    Response resp = new Response();
    
    String desc = "";
    boolean flag = false;
    
    try {
    NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
    List<Holder> holderEditList = (List<Holder>) wrapper.getModel();
    Holder holder = holderEditList.get(0);
    
    //holder must exist
    if (hq.checkHolderAccount(holder.getHolderId())) {
    org.greenpole.hibernate.entity.Holder holderEntity = hq.getHolder(holder.getHolderId());
    logger.info("holder exists - [{}]: [{}]", login.getUserId(), holderEntity.getFirstName() + " " + holderEntity.getLastName());
    
    //holder must be primary to be edited
    if (holderEntity.isPryHolder()) {
    
    if (holder.getFirstName() == null || "".equals(holder.getFirstName())) {
    desc = "\nHolder first name should not be empty";
    } else if (holder.getLastName() == null || "".equals(holder.getLastName())) {
    desc += "\nHolder last name should not be empty";
    } else if (holder.getTypeId() <= 0) {
    desc += "\nHolder type should not be empty";
    } else if (holder.getPrimaryAddress() == null || "".equals(holder.getPrimaryAddress())) {
    desc += "\nPrimary Holder address is not specified";
    } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString())
    || holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
    desc += "\nPrimary address can only be residential or postal";
    } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString())
    && (holder.getHolderResidentialAddresses() == null || holder.getHolderResidentialAddresses().isEmpty())) {
    desc += "\nResidential address cannot be empty, as it is the primary address";
    } else if (holder.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString())
    && (holder.getHolderPostalAddresses() == null || holder.getHolderPostalAddresses().isEmpty())) {
    desc += "\nPostal address cannot be empty, as it is the primary address";
    } else {
    flag = true;
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
    
    if (flag && holder.getHolderResidentialAddresses() != null && !holder.getHolderResidentialAddresses().isEmpty()) {
    for (Address addr : holder.getHolderResidentialAddresses()) {
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
    
    if (flag && holder.getHolderPostalAddresses() != null && !holder.getHolderPostalAddresses().isEmpty()) {
    for (Address addr : holder.getHolderPostalAddresses()) {
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
    
    if (flag && holder.getHolderEmailAddresses() != null && !holder.getHolderEmailAddresses().isEmpty()) {
    for (EmailAddress email : holder.getHolderEmailAddresses()) {
    if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
    desc += "\nEmail address should not be empty. Delete email entry if you must";
    flag = false;
    break;
    }
    }
    }
    
    if (flag && holder.getHolderPhoneNumbers() != null && !holder.getHolderPhoneNumbers().isEmpty()) {
    for (PhoneNumber phone : holder.getHolderPhoneNumbers()) {
    if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
    desc += "\nPhone number should not be empty. Delete phone number entry if you must";
    flag = false;
    break;
    }
    }
    }
    
    if (flag && (holderEntity.getChn() != null || "".equals(holderEntity.getChn()))
    && (holder.getChn() == null || "".equals(holder.getChn()))) {
    desc += "\nCHN cannot be erased";
    flag = false;
    }
    
    if (flag) {
    HolderType typeEntity = new HolderType();
    
    typeEntity.setId(holder.getTypeId());
    
    holderEntity.setFirstName(holder.getFirstName());
    holderEntity.setMiddleName(holder.getMiddleName());
    holderEntity.setLastName(holder.getLastName());
    holderEntity.setType(typeEntity);
    holderEntity.setGender(holder.getGender());
    holderEntity.setDob(formatter.parse(holder.getDob()));
    holderEntity.setChn(holder.getChn());
    List<HolderChanges> holderChangesList = new ArrayList<>();
    HolderChanges changes = new HolderChanges();
    
    for (org.greenpole.entity.model.holder.HolderChanges hc : holder.getChanges()) {
    HolderChangeType changeType = new HolderChangeType();
    changeType.setId(hc.getChangeTypeId());
    
    changes.setHolder(holderEntity);
    changes.setInitialForm(hc.getInitialForm());
    changes.setCurrentForm(hc.getCurrentForm());
    changes.setChangeDate(formatter.parse(hc.getChangeDate()));
    changes.setHolderChangeType(changeType);
    
    holderChangesList.add(changes);
    }
    
    boolean updated = hq.updateHolderAccount(holderEntity, retrieveHolderResidentialAddress(holder),
    retrieveHolderPostalAddress(holder), retrieveHolderPhoneNumber(holder),
    retrieveHolderEmailAddress(holder), holderChangesList);
    
    if (updated) {
    resp.setRetn(0);
    resp.setDesc("Holder details saved");
    logger.info("Holder account update successful - [{}]", login.getUserId());
    // Send SMS/Email notification to shareholder
    return resp;
    } else {
    resp.setRetn(300);
    resp.setDesc("An error occurred while updating the holder's details. Contact Administrator.");
    logger.info("An error occurred while updating the holder's details - [{}]", login.getUserId());
    // Send SMS/Email notification to shareholder IF USER PERMITS
    return resp;
    }
    }
    resp.setRetn(300);
    resp.setDesc("Error filing holder details: " + desc);
    logger.info("Error filing holder details: [{}] - [{}]", desc, login.getUserId());
    return resp;
    }
    resp.setRetn(300);
    resp.setDesc("Holder account is not a primary account. Non-primary accounts cannot be edited");
    logger.info("Holder account is not a primary account. Non-primary accounts cannot be edited - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);
    resp.setDesc("The holder does not exist.");
    logger.info("The holder does not exist - [{}]", login.getUserId());
    return resp;
    } catch (JAXBException ex) {
    logger.info("error loading notification xml file. See error log - [{}]", login.getUserId());
    logger.error("error loading notification xml file to object - [" + login.getUserId() + "]", ex);
    
    resp.setRetn(98);
    resp.setDesc("Unable to proccess holder details edit. Contact System Administrator");
    
    return resp;
    } catch (Exception ex) {
    logger.info("error proccessing holder name transpose. See error log - [{}]", login.getUserId());
    logger.error("error proccessing holder name transpose - [" + login.getUserId() + "]", ex);
    
    resp.setRetn(99);
    resp.setDesc("General error. Unable to proccess holder name transpose. Contact system administrator."
    + "\nMessage: " + ex.getMessage());
    return resp;
    }
    }*/

    /**
     * Unwraps the holder postal address details from the HolderModel into
     * HolderPostalAddress hibernate entity
     *
     * @param holdModel the holderModel of holder details
     * @param newEntry boolean value indicating new entry
     * @return List object of HolderPostalAddress hibernate entity
     */
    private List<HolderPostalAddress> retrieveHolderPostalAddress(Holder holdModel/*, boolean newEntry*/) {
        org.greenpole.hibernate.entity.HolderPostalAddress postalAddressEntity = new org.greenpole.hibernate.entity.HolderPostalAddress();
        List<org.greenpole.entity.model.Address> hpaddyList = (List<org.greenpole.entity.model.Address>) holdModel.getHolderPostalAddresses();
        List<org.greenpole.hibernate.entity.HolderPostalAddress> returnHolderPostalAddress = new ArrayList<>();

        for (org.greenpole.entity.model.Address hpa : hpaddyList) {
            HolderPostalAddressId postalAddyId = new HolderPostalAddressId();
            /*if (newEntry) {
                postalAddyId.setHolderId(holdModel.getHolderId());
            }*/
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
     * Unwraps holder phone number details from the holder model passed as
     * parameter into HolderPhoneNumber hibernate entity
     * @param holdModel object of holder details
     * @param newEntry boolean variable indicating whether or not the entry is
     * new
     * @return List of HolderPhoneNumber objects retrieveHolderEmailAddress
     */
    private List<HolderPhoneNumber> retrieveHolderPhoneNumber(Holder holdModel/*, boolean newEntry*/) {

        org.greenpole.hibernate.entity.HolderPhoneNumber phoneNumberEntity = new org.greenpole.hibernate.entity.HolderPhoneNumber();
        List<org.greenpole.entity.model.PhoneNumber> phoneNumberList = holdModel.getHolderPhoneNumbers();
        List<org.greenpole.hibernate.entity.HolderPhoneNumber> returnPhoneNumber = new ArrayList<>();

        for (PhoneNumber pnList : phoneNumberList) {
            HolderPhoneNumberId phoneNoId = new HolderPhoneNumberId();
            /*if (newEntry) {
                phoneNoId.setHolderId(holdModel.getHolderId());
            }*/
            phoneNoId.setPhoneNumber(pnList.getPhoneNumber());
            phoneNumberEntity.setIsPrimary(pnList.isPrimaryPhoneNumber());
            phoneNumberEntity.setId(phoneNoId);
        }
        return returnPhoneNumber;
    }

    /**
     * Unwraps Holder email address from the holder model into
     * HolderEmailAddress hibernate entity object
     * @param holdModel object to holder model
     * @param newEntry boolean variable indicating whether or not entry is new
     * @return List of HolderEmailAddress hibernate entity objects
     */
    private List<HolderEmailAddress> retrieveHolderEmailAddress(Holder holdModel/*, boolean newEntry*/) {

        org.greenpole.hibernate.entity.HolderEmailAddress emailAddressEntity = new org.greenpole.hibernate.entity.HolderEmailAddress();
        List<org.greenpole.entity.model.EmailAddress> emailAddressList = holdModel.getHolderEmailAddresses();
        List<org.greenpole.hibernate.entity.HolderEmailAddress> returnEmailAddress = new ArrayList<>();

        for (EmailAddress email : emailAddressList) {
            HolderEmailAddressId emailId = new HolderEmailAddressId();
            /*if (newEntry) {
                emailId.setHolderId(holdModel.getHolderId());
            }*/
            emailId.setEmailAddress(email.getEmailAddress());
            emailAddressEntity.setIsPrimary(email.isPrimaryEmail());
            emailAddressEntity.setId(emailId);
        }
        return returnEmailAddress;
    }

    /**
     * Unwraps Holder residential address from the holder model into
     * HolderResidentialAddress hibernate entity object.
     * @param holdModel object of holder model
     * @param newEntry boolean variable indicating whether or not entry is new
     * @return List of HolderResidentialAddress hibernate entity objects
     */
    private List<HolderResidentialAddress> retrieveHolderResidentialAddress(Holder holdModel/*, boolean newEntry*/) {
        org.greenpole.hibernate.entity.HolderResidentialAddress residentialAddressEntity = new org.greenpole.hibernate.entity.HolderResidentialAddress();
        List<org.greenpole.entity.model.Address> residentialAddressList = (List<org.greenpole.entity.model.Address>) holdModel.getHolderResidentialAddresses();
        List<org.greenpole.hibernate.entity.HolderResidentialAddress> returnResidentialAddress = new ArrayList();

        for (org.greenpole.entity.model.Address rAddy : residentialAddressList) {
            HolderResidentialAddressId rAddyId = new HolderResidentialAddressId();
            /*if (newEntry) {
                rAddyId.setHolderId(holdModel.getHolderId());
            }*/
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
     * Unwraps holder company account details from the holder model.
     * @param holdModel object of the holder model
     * @param newEntry boolean variable indicating whether or not entry is new
     * @return object of HolderCompanyAccount
     */
    private HolderCompanyAccount retrieveHolderCompanyAccount(Holder holdModel/*, boolean newEntry*/) {
        HolderCompanyAccount companyAccountEntity = new org.greenpole.hibernate.entity.HolderCompanyAccount();
        org.greenpole.entity.model.holder.HolderCompanyAccount compAcct = holdModel.getHolderCompanyAccounts().get(0);
        HolderCompanyAccountId compAcctId = new HolderCompanyAccountId();
        
        compAcctId.setClientCompanyId(compAcct.getClientCompanyId());
        
        companyAccountEntity.setId(compAcctId);
        companyAccountEntity.setEsop(compAcct.isEsop());
        companyAccountEntity.setHolderCompAccPrimary(true);
        companyAccountEntity.setMerged(false);
        
        return companyAccountEntity;
    }

    /**
     * Generates unique file name from a combination of date and a random number.
     * @return String object containing generated file name
     */
    private String createSignatureFileName() {
        Date date = new Date();
        Random rand = new Random();
        int randomNumber = rand.nextInt(9999999);
        String fileName = randomNumber + "" + date.getTime();
        return fileName;
    }

    /**
     * Unwraps Holder bond account details from holder model.
     * @param holdModel object of holder model
     * @param newEntry boolean variable indicating whether or not entry is new
     * @return HolderBondAccount object
     */
    private HolderBondAccount retrieveHolderBondAccount(Holder holdModel/*, boolean newEntry*/) {
        org.greenpole.hibernate.entity.HolderBondAccount bondAccountEntity = new org.greenpole.hibernate.entity.HolderBondAccount();
        org.greenpole.entity.model.holder.HolderBondAccount bondAcct = holdModel.getHolderBondAccounts().get(0);
        HolderBondAccountId bondAcctId = new HolderBondAccountId();
        
        bondAcctId.setBondOfferId(bondAcct.getBondOfferId());
        
        bondAccountEntity.setId(bondAcctId);
        bondAccountEntity.setStartingPrincipalValue(bondAcct.getStartingPrincipalValue());
        bondAccountEntity.setHolderBondAccPrimary(true);
        bondAccountEntity.setMerged(false);
        
        return bondAccountEntity;
    }

}
