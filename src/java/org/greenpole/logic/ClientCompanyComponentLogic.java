/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.logic;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.model.Address;
import org.greenpole.entity.model.EmailAddress;
import org.greenpole.entity.model.PhoneNumber;
import org.greenpole.entity.model.clientcompany.BondOffer;
import org.greenpole.entity.model.clientcompany.ClientCompany;
import org.greenpole.entity.model.clientcompany.InitialPublicOffer;
import org.greenpole.entity.model.clientcompany.PrivatePlacement;
import org.greenpole.entity.model.clientcompany.QueryClientCompany;
import org.greenpole.entity.model.clientcompany.ShareQuotation;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.security.Login;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationType;
import org.greenpole.entity.response.Response;
import org.greenpole.hibernate.entity.BondOfferPaymentPlan;
import org.greenpole.hibernate.entity.BondType;
import org.greenpole.hibernate.entity.ClientCompanyAddress;
import org.greenpole.hibernate.entity.ClientCompanyEmailAddress;
import org.greenpole.hibernate.entity.ClientCompanyPhoneNumber;
import org.greenpole.hibernate.entity.Depository;
import org.greenpole.hibernate.entity.NseSector;
import org.greenpole.hibernate.exception.GreenpoleQueryException;
import org.greenpole.hibernate.query.GeneralComponentQuery;
import org.greenpole.hibernate.util.HibernateUtil;
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
 * @author Akinwale Agbaje
 * @version 1.0
 * Business requirement implementations to do with client companies
 */
public class ClientCompanyComponentLogic {
    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();
    private final GeneralComponentQuery gq = ComponentQueryFactory.getGeneralComponentQuery();
    private final GreenpoleProperties greenProp = GreenpoleProperties.getInstance();
    private final NotificationProperties notificationProp = NotificationProperties.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(ClientCompanyComponentLogic.class);
    
    /**
     * Processes request to create a new client company.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param cc the client company to be created
     * @return response to the client company creation request 
     */
    public Response createClientCompany_Request(Login login, String authenticator, ClientCompany cc) {
        logger.info("request to create the client company [{}], invoked by [{}]", cc.getName(), login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        
        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;
            String desc = "";
            boolean flag = false;

            //check if client company exists
            if (!cq.checkClientCompany(cc.getName())) {
                if (cc.getCode() == null || "".equals(cc.getCode())) {
                    desc += "\nClient company code should not be empty";
                } else if (cq.checkClientCompanyByCode(cc.getCode())) {
                    desc += "\nClient company code is already being used by another company";
                } else {
                    flag = true;
                }
                
                if (flag && cc.getDepositoryId() > 0) {
                    boolean found = false;
                    for (Depository d : cq.getAllDepositories()) {
                        if (cc.getDepositoryId() == d.getId()) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        desc += "\nDepository is not valid";
                        flag = false;
                    }
                } else if (cc.getDepositoryId() <= 0) {
                    desc += "\nDepository must be entered";
                    flag = false;
                }
                
                if (flag && cc.getNseSectorId() > 0) {
                    boolean found = false;
                    for (NseSector sec : cq.getAllNseSectors()) {
                        if (cc.getNseSectorId() == sec.getId()) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        desc += "\nNSE Sector is not valid";
                        flag = false;
                    }
                } else if (cc.getNseSectorId() <= 0) {
                    desc += "\nNSE Sector must be entered";
                    flag = false;
                }
                
                if (flag && cc.getAddresses() != null && !cc.getAddresses().isEmpty()) {
                    for (Address addr : cc.getAddresses()) {
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

                /*if (flag && cc.getEmailAddresses() != null && !cc.getEmailAddresses().isEmpty()) {
                for (EmailAddress email : cc.getEmailAddresses()) {
                if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
                desc += "\nEmail address should not be empty. Delete email entry if you must";
                flag = false;
                break;
                }
                }
                }
                
                if (flag && cc.getPhoneNumbers() != null && !cc.getPhoneNumbers().isEmpty()) {
                for (PhoneNumber phone : cc.getPhoneNumbers()) {
                if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
                desc += "\nPhone number should not be empty. Delete phone number entry if you must";
                flag = false;
                break;
                }
                }
                }*/
                
                if (flag) {
                    wrapper = new NotificationWrapper();
                    prop = NotifierProperties.getInstance();
                    qSender = new QueueSender(prop.getNotifierQueueFactory(),
                            prop.getAuthoriserNotifierQueueName());

                    logger.info("client company does not exist - [{}]: [{}]", login.getUserId(), cc.getName());
                    List<ClientCompany> cclist = new ArrayList<>();
                    cclist.add(cc);
                    //wrap client company object in notification object, along with other information
                    wrapper.setCode(notification.createCode(login));
                    wrapper.setDescription("Authenticate creation of the client company, " + cc.getName());
                    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                    wrapper.setNotificationType(NotificationType.create_client_company.toString());
                    wrapper.setFrom(login.getUserId());
                    wrapper.setTo(authenticator);
                    wrapper.setModel(cclist);
                    
                    logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                    resp = qSender.sendAuthorisationRequest(wrapper);
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("Error: " + desc);
                logger.info("error detected in client company creation process - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Client company already exists and so cannot be created.");
            logger.info("client company exists so cannot be created - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (Exception ex) {
            logger.info("error processing client company creation. See error log - [{}]", login.getUserId());
            logger.error("error processing client company creation - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process client company creation. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Processes request to create a client company that has already been saved 
     * as a notification file, according to the specified notification code.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the client company creation request
     */
    public Response createClientCompany_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        Notification notification = new Notification();
        boolean freshCreation = true;
        logger.info("authorise client company creation, invoked by [{}] - [{}]", login.getUserId(), notificationCode);
        try {
            //get client company model from wrapper
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<ClientCompany> list = (List<ClientCompany>) wrapper.getModel();
            ClientCompany ccModel = list.get(0);
            
            String desc = "";
            boolean flag = false;

            //check if client company exists
            if (!cq.checkClientCompany(ccModel.getName())) {
                if (ccModel.getCode() == null || "".equals(ccModel.getCode())) {
                    desc += "\nClient company code should not be empty";
                } else if (cq.checkClientCompanyByCode(ccModel.getCode())) {
                    desc += "\nClient company code is already being used by another company";
                } else {
                    flag = true;
                }
                
                if (flag && ccModel.getDepositoryId() > 0) {
                    boolean found = false;
                    for (Depository d : cq.getAllDepositories()) {
                        if (ccModel.getDepositoryId() == d.getId()) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        desc += "\nDepository is not valid";
                        flag = false;
                    }
                } else if (ccModel.getDepositoryId() <= 0) {
                    desc += "\nDepository must be entered";
                    flag = false;
                }
                
                if (flag && ccModel.getNseSectorId() > 0) {
                    boolean found = false;
                    for (NseSector sec : cq.getAllNseSectors()) {
                        if (ccModel.getNseSectorId() == sec.getId()) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        desc += "\nNse Sector is not valid";
                        flag = false;
                    }
                } else if (ccModel.getNseSectorId() <= 0) {
                    desc += "\nNSE Sector must be entered";
                    flag = false;
                }
                
                if (flag && ccModel.getAddresses() != null && !ccModel.getAddresses().isEmpty()) {
                    for (Address addr : ccModel.getAddresses()) {
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

                /*if (flag && ccModel.getEmailAddresses() != null && !ccModel.getEmailAddresses().isEmpty()) {
                for (EmailAddress email : ccModel.getEmailAddresses()) {
                if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
                desc += "\nEmail address should not be empty. Delete email entry if you must";
                flag = false;
                break;
                }
                }
                }
                
                if (flag && ccModel.getPhoneNumbers() != null && !ccModel.getPhoneNumbers().isEmpty()) {
                for (PhoneNumber phone : ccModel.getPhoneNumbers()) {
                if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
                desc += "\nPhone number should not be empty. Delete phone number entry if you must";
                flag = false;
                break;
                }
                }
                }*/
                
                if (flag) {
                    boolean created = cq.createClientCompany(retrieveClientCompanyModel(ccModel, freshCreation), retrieveAddressModel(ccModel),
                            retrieveEmailAddressModel(ccModel), retrievePhoneNumberModel(ccModel));
                    
                    if (created) {
                        notification.markAttended(notificationCode);
                        logger.info("client company created - [{}]: [{}]", login.getUserId(), ccModel.getName());
                        resp.setRetn(0);
                        resp.setDesc("Successful");
                        return resp;
                    }
                    resp.setRetn(201);
                    resp.setDesc("Unable to create client company from authorisation. Contact System Administrator");
                    logger.info("unable to create client company from authorisation - [{}]", login.getUserId());
                    return resp;
                }
                resp.setRetn(201);
                resp.setDesc("Error: " + desc);
                logger.info("error detected in client company creation process - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(201);
            resp.setDesc("Client company already exists and so cannot be created.");
            logger.info("client company exists so cannot be created - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (JAXBException ex) {
            logger.info("error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(98);
            resp.setDesc("Unable to create client company from authorisation. Contact System Administrator");
            
            return resp;
        } catch (Exception ex) {
            logger.info("error creating client company. See error log - [{}]", login.getUserId());
            logger.error("error creating client company - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to create client company. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Processes request to edit an existing client company.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param cc the client company to be created
     * @return response to the client company creation request
     */
    public Response editClientCompany_Request(Login login, String authenticator, ClientCompany cc) {
        logger.info("request to edit the client company [{}], invoked by [{}]", cc.getName(), login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
       
        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;
            
            //client company must exist to be edited
            if (cq.checkClientCompany(cc.getName())) {
                logger.info("client company exists - [{}]: [{}]", login.getUserId(), cc.getName());
                //if client company exists, it must be a primary client company
                if (cq.getClientCompanyByName(cc.getName()).getClientCompanyPrimary()) {

                    String desc = "";
                    boolean flag = false;

                    if (cc.getCode() == null || "".equals(cc.getCode())) {
                        desc += "\nCode should not be empty";
                    } else {
                        flag = true;
                    }
                    
                    if (flag && cc.getDepositoryId() > 0) {
                        boolean found = false;
                        for (Depository d : cq.getAllDepositories()) {
                            if (cc.getDepositoryId() == d.getId()) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            desc += "\nDepository is not valid";
                            flag = false;
                        }
                    } else if (cc.getDepositoryId() <= 0) {
                        desc += "\nDepository must be entered";
                        flag = false;
                    }

                    if (flag && cc.getNseSectorId() > 0) {
                        boolean found = false;
                        for (NseSector sec : cq.getAllNseSectors()) {
                            if (cc.getNseSectorId() == sec.getId()) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            desc += "\nNse Sector is not valid";
                            flag = false;
                        }
                    } else if (cc.getNseSectorId() <= 0) {
                        desc += "\nNSE Sector must be entered";
                        flag = false;
                    }
                    
                    if (flag && cc.getAddresses() != null && !cc.getAddresses().isEmpty()) {
                        for (Address addy : cc.getAddresses()) {
                            if ("".equals(addy.getAddressLine1()) || addy.getAddressLine1() == null) {
                                desc += "\nAddress line 1 should not be empty. Delete entire address if you must";
                                flag = false;
                                break;
                            } else if ("".equals(addy.getState()) || addy.getState() == null) {
                                desc += "\nState should not be empty. Delete entire address if you must";
                                flag = false;
                                break;
                            } else if ("".equals(addy.getCountry()) || addy.getCountry() == null) {
                                desc += "\nCountry should not be empty. Delete entire address if you must";
                                flag = false;
                                break;
                            }
                        }
                    }
                    
                    /*if (flag && cc.getEmailAddresses() != null && !cc.getEmailAddresses().isEmpty()) {
                    for (EmailAddress email : cc.getEmailAddresses()) {
                    if ("".equals(email.getEmailAddress()) || email.getEmailAddress() == null) {
                    desc += "\nEmail address should not be empty. Delete email entry if you must";
                    flag = false;
                    break;
                    }
                    }
                    }
                    
                    if (flag && cc.getPhoneNumbers() != null && !cc.getPhoneNumbers().isEmpty()) {
                    for (PhoneNumber phone : cc.getPhoneNumbers()) {
                    if ("".equals(phone.getPhoneNumber()) || phone.getPhoneNumber() == null) {
                    desc += "\nPhone number should not be empty. Delete phone number entry if you must";
                    flag = false;
                    break;
                    }
                    }
                    }*/

                    if (flag) {
                        wrapper = new NotificationWrapper();
                        prop = NotifierProperties.getInstance();
                        qSender = new QueueSender(prop.getNotifierQueueFactory(),
                                prop.getAuthoriserNotifierQueueName());

                        logger.info("client company is valid and primary - [{}]: [{}]", login.getUserId(), cc.getName());
                        List<ClientCompany> cclist = new ArrayList<>();
                        cclist.add(cc);
                        //wrap client company object in notification object, along with other information
                        wrapper.setCode(notification.createCode(login));
                        wrapper.setDescription("Authenticate change to client company, " + cc.getName());
                        wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                        wrapper.setNotificationType(NotificationType.edit_client_company.toString());
                        wrapper.setFrom(login.getUserId());
                        wrapper.setTo(authenticator);
                        wrapper.setModel(cclist);
                        
                        logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                        resp = qSender.sendAuthorisationRequest(wrapper);
                        return resp;
                    }
                    resp.setRetn(202);
                    resp.setDesc("Error: " + desc);
                    logger.info("Error filing client company details - [{}]", login.getUserId());
                    return resp;
                }
                resp.setRetn(202);
                resp.setDesc("Client company is not valid, so cannot be edited.");
                logger.info("client company is not valid so cannot be edited - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(202);
            resp.setDesc("Client company does not exist, so cannot be edited.");
            logger.info("client company does not exist so cannot be edited - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (Exception ex) {
            logger.info("error processing client company edit. See error log - [{}]", login.getUserId());
            logger.error("error processing client company edit - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process client company edit. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Processes request to change a client company that has already been saved 
     * as a notification file, according to the specified notification code.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the client company creation request
     */
    public Response editClientCompany_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        Notification notification = new Notification();
        boolean freshCreation = false;
        logger.info("authorise client company change, invoked by [{}] - notification code: [{}]", login.getUserId(), notificationCode);
        try {
            //get client company model from wrapper
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<ClientCompany> list = (List<ClientCompany>) wrapper.getModel();
            ClientCompany ccModel = list.get(0);
            
            //client company must exist to be edited
            if (cq.checkClientCompany(ccModel.getName()) && cq.checkClientCompany(ccModel.getId())) {
                logger.info("client company exists - [{}]: [{}]", login.getUserId(), ccModel.getName());
                //if client company exists, it must be a primary client company
                if (cq.getClientCompanyByName(ccModel.getName()).getClientCompanyPrimary()) {

                    String desc = "";
                    boolean flag = false;

                    if (ccModel.getCode() == null || "".equals(ccModel.getCode())) {
                        desc += "\nCode should not be empty";
                    } else {
                        flag = true;
                    }
                    
                    if (flag && ccModel.getDepositoryId() > 0) {
                        boolean found = false;
                        for (Depository d : cq.getAllDepositories()) {
                            if (ccModel.getDepositoryId() == d.getId()) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            desc += "\nDepository is not valid";
                            flag = false;
                        }
                    } else if (ccModel.getDepositoryId() <= 0) {
                        desc += "\nDepository must be entered";
                        flag = false;
                    }

                    if (flag && ccModel.getNseSectorId() > 0) {
                        boolean found = false;
                        for (NseSector sec : cq.getAllNseSectors()) {
                            if (ccModel.getNseSectorId() == sec.getId()) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            desc += "\nNse Sector is not valid";
                            flag = false;
                        }
                    } else if (ccModel.getNseSectorId() <= 0) {
                        desc += "\nNSE Sector must be entered";
                        flag = false;
                    }
                    
                    if (flag && ccModel.getAddresses() != null && !ccModel.getAddresses().isEmpty()) {
                        for (Address addy : ccModel.getAddresses()) {
                            if ("".equals(addy.getAddressLine1()) || addy.getAddressLine1() == null) {
                                desc += "\nAddress line 1 should not be empty. Delete entire address if you must";
                                flag = false;
                                break;
                            } else if ("".equals(addy.getState()) || addy.getState() == null) {
                                desc += "\nState should not be empty. Delete entire address if you must";
                                flag = false;
                                break;
                            } else if ("".equals(addy.getCountry()) || addy.getCountry() == null) {
                                desc += "\nCountry should not be empty. Delete entire address if you must";
                                flag = false;
                                break;
                            }
                        }
                    }
                    
                    /*if (flag && ccModel.getEmailAddresses() != null && !ccModel.getEmailAddresses().isEmpty()) {
                    for (EmailAddress email : ccModel.getEmailAddresses()) {
                    if ("".equals(email.getEmailAddress()) || email.getEmailAddress() == null) {
                    desc += "\nEmail address should not be empty. Delete email entry if you must";
                    flag = false;
                    break;
                    }
                    }
                    }
                    
                    if (flag && ccModel.getPhoneNumbers() != null && !ccModel.getPhoneNumbers().isEmpty()) {
                    for (PhoneNumber phone : ccModel.getPhoneNumbers()) {
                    if ("".equals(phone.getPhoneNumber()) || phone.getPhoneNumber() == null) {
                    desc += "\nPhone number should not be empty. Delete phone number entry if you must";
                    flag = false;
                    break;
                    }
                    }
                    }*/
                    
                    if (flag) {
                        boolean edited = cq.updateClientCompany(retrieveClientCompanyModel(ccModel, freshCreation), retrieveAddressModel(ccModel),
                            retrieveEmailAddressModel(ccModel), retrievePhoneNumberModel(ccModel), retrieveAddressModelForDeletion(ccModel),
                            retrieveEmailAddressModelForDeletion(ccModel), retrievePhoneNumberModelForDeletion(ccModel));

                        if (edited) {
                            notification.markAttended(notificationCode);
                            logger.info("client company edited - [{}]: [{}]", ccModel.getName(), login.getUserId());
                            resp.setRetn(0);
                            resp.setDesc("Successful");
                            return resp;
                        }

                        resp.setRetn(203);
                        resp.setDesc("Unable to change client company from authorisation. Contact System Administrator");
                        logger.info("unable to change client company from authorisation - [{}]", login.getUserId());
                        return resp;
                    }
                    resp.setRetn(203);
                    resp.setDesc("Error: " + desc);
                    logger.info("Error filing client company details - [{}]", login.getUserId());
                    return resp;
                }
                resp.setRetn(203);
                resp.setDesc("Client company is not valid, so cannot be edited.");
                logger.info("client company is not valid so cannot be edited - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(203);
            resp.setDesc("Client company does not exist, so cannot be edited.");
            logger.info("client company does not exist so cannot be edited - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (JAXBException ex) {
            logger.info("error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(203);
            resp.setDesc("Unable to change client company from authorisation. Contact System Administrator");
            
            return resp;
        } catch (Exception ex) {
            logger.info("error editing client company. See error log - [{}]", login.getUserId());
            logger.error("error editing client company - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to edit client company. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     * Searches for a list of client companies according to query parameters.
     * @param login the user's login details
     * @param queryParams the query parameters
     * @return response to the client company query request
     */
    public Response queryClientCompany_Request(Login login, QueryClientCompany queryParams) {
        Response resp = new Response();
        Descriptor descriptorUtil = new Descriptor();
        logger.info("request to query client company, invoked by [{}]", login.getUserId());
        
        try {
            if (queryParams.getDescriptor() == null || "".equals(queryParams.getDescriptor())) {
                logger.info("client company query unsuccessful. Empty descriptor - [{}]", login.getUserId());
                resp.setRetn(204);
                resp.setDesc("Unsuccessful client company query, due to empty descriptor. Contact system administrator");
                return resp;
            }
            
            Map<String, String> descriptors = descriptorUtil.decipherDescriptor(queryParams.getDescriptor());

            if (descriptors.size() == 4) {
                String descriptor = queryParams.getDescriptor();

                org.greenpole.hibernate.entity.ClientCompany cc_hib_search = new org.greenpole.hibernate.entity.ClientCompany();
                ClientCompanyAddress cc_hib_address_search = new ClientCompanyAddress();
                ClientCompanyEmailAddress cc_hib_email_search = new ClientCompanyEmailAddress();
                ClientCompanyPhoneNumber cc_hib_phone_search = new ClientCompanyPhoneNumber();

                Depository depository_hib_search = new Depository();
                NseSector nseSector_hib_search = new NseSector();

                ClientCompany cc_model_search;
                if (queryParams.getClientCompany() != null) {
                    cc_model_search = queryParams.getClientCompany();

                    depository_hib_search.setName(cc_model_search.getDepositoryName());
                    nseSector_hib_search.setName(cc_model_search.getNseSectorName());

                    cc_hib_search.setName(cc_model_search.getName());
                    cc_hib_search.setCeo(cc_model_search.getCeo());
                    cc_hib_search.setClientCompanyPrimary(true); //primary must be set
                    cc_hib_search.setMerged(false); //merged must be set
                    cc_hib_search.setValid(true); //valid must be set
                    cc_hib_search.setCode(cc_model_search.getCode());
                    cc_hib_search.setSecretary(cc_model_search.getSecretary());

                    cc_hib_search.setDepository(depository_hib_search);
                    cc_hib_search.setNseSector(nseSector_hib_search);

                }
                
                if (queryParams.getClientCompany() != null) {
                    Address address_model_search;
                    Set cc_hib_address_set = new HashSet();
                    if (queryParams.getClientCompany().getAddresses() != null && !queryParams.getClientCompany().getAddresses().isEmpty()) {
                        address_model_search = queryParams.getClientCompany().getAddresses().get(0);

                        cc_hib_address_search.setAddressLine1(address_model_search.getAddressLine1());
                        cc_hib_address_search.setCountry(address_model_search.getCountry());
                        cc_hib_address_search.setState(address_model_search.getState());
                        cc_hib_address_search.setAddressLine2(address_model_search.getAddressLine2());
                        cc_hib_address_search.setAddressLine3(address_model_search.getAddressLine3());
                        cc_hib_address_search.setAddressLine4(address_model_search.getAddressLine4());
                        cc_hib_address_search.setCity(address_model_search.getCity());
                        cc_hib_address_search.setPostCode(address_model_search.getPostCode());
                        
                        cc_hib_address_set.add(cc_hib_address_search); //put address in set

                        cc_hib_search.setClientCompanyAddresses(cc_hib_address_set); //put address set in client company entity
                    }

                    EmailAddress emailAddress_model_search;
                    Set cc_hib_email_set = new HashSet();
                    if (queryParams.getClientCompany().getEmailAddresses() != null && !queryParams.getClientCompany().getEmailAddresses().isEmpty()) {
                        emailAddress_model_search = queryParams.getClientCompany().getEmailAddresses().get(0);

                        cc_hib_email_search.setEmailAddress(emailAddress_model_search.getEmailAddress());
                        
                        cc_hib_email_set.add(cc_hib_email_search); //put email in set

                        cc_hib_search.setClientCompanyEmailAddresses(cc_hib_email_set); //put email set in client company entity
                    }

                    PhoneNumber phoneNumber_model_search;
                    Set cc_hib_phone_set = new HashSet();
                    if (queryParams.getClientCompany().getPhoneNumbers() != null && !queryParams.getClientCompany().getPhoneNumbers().isEmpty()) {
                        phoneNumber_model_search = queryParams.getClientCompany().getPhoneNumbers().get(0);

                        cc_hib_phone_search.setPhoneNumber(phoneNumber_model_search.getPhoneNumber());
                        
                        cc_hib_phone_set.add(cc_hib_phone_search); //put phone in set

                        cc_hib_search.setClientCompanyPhoneNumbers(cc_hib_phone_set); //put phone set in client company entity
                    }
                }

                Map<String, Double> shareUnit_search;
                if (queryParams.getShareUnit() != null && !queryParams.getShareUnit().isEmpty()) {
                    shareUnit_search = queryParams.getShareUnit();
                } else {
                    shareUnit_search = new HashMap<>();
                }

                Map<String, Integer> numberOfShareholders_search;
                if (queryParams.getNumberOfShareholders() != null && !queryParams.getNumberOfShareholders().isEmpty()) {
                    numberOfShareholders_search = queryParams.getNumberOfShareholders();
                } else {
                    numberOfShareholders_search = new HashMap<>();
                }

                Map<String, Integer> numberOfBondholders_search;
                if (queryParams.getNumberOfBondholders() != null && !queryParams.getNumberOfBondholders().isEmpty()) {
                    numberOfBondholders_search = queryParams.getNumberOfBondholders();
                } else {
                    numberOfBondholders_search = new HashMap<>();
                }

                List<org.greenpole.hibernate.entity.ClientCompany> cc_search_result = cq.queryClientCompany(descriptor, cc_hib_search, shareUnit_search, numberOfShareholders_search, numberOfBondholders_search);
                logger.info("retrieved client company result from query. Preparing local model - [{}]", login.getUserId());
                
                //unwrap result and set in client company front-end model
                List<ClientCompany> cc_model_out = new ArrayList<>();

                

                for (org.greenpole.hibernate.entity.ClientCompany cc_hib_out : cc_search_result) {
                    List<Address> cc_model_addy_out = new ArrayList<>();
                    List<PhoneNumber> cc_model_phone_out = new ArrayList<>();
                    List<EmailAddress> cc_model_email_out = new ArrayList<>();
                    
                    ClientCompany c = new ClientCompany();

                    if (cc_hib_out.getDepository() != null) {
                        Depository d = cq.getDepository(cc_hib_out.getDepository().getId());
                        c.setDepositoryId(d.getId());
                        c.setDepositoryName(d.getName());
                    }
                    
                    if (cc_hib_out.getNseSector() != null) {
                        NseSector n = cq.getNseSector(cc_hib_out.getNseSector().getId());
                        c.setNseSectorId(n.getId());
                        c.setNseSectorName(n.getName());
                    }
                    
                    c.setId(cc_hib_out.getId());
                    c.setName(cc_hib_out.getName());
                    c.setCeo(cc_hib_out.getCeo());
                    c.setCode(cc_hib_out.getCode());
                    c.setSecretary(cc_hib_out.getSecretary());

                    //get all available addresses, email addresses and phone numbers
                    List<ClientCompanyAddress> addy_hib_list = cq.getClientCompanyAddresses(cc_hib_out.getId());
                    for (ClientCompanyAddress addy_hib_out : addy_hib_list) {
                        Address addy_model_out = new Address(); //prepare address model to set
                        
                        addy_model_out.setId(addy_hib_out.getId());
                        addy_model_out.setAddressLine1(addy_hib_out.getAddressLine1());
                        addy_model_out.setState(addy_hib_out.getState());
                        addy_model_out.setCountry(addy_hib_out.getCountry());
                        addy_model_out.setAddressLine2(addy_hib_out.getAddressLine2());
                        addy_model_out.setAddressLine3(addy_hib_out.getAddressLine3());
                        addy_model_out.setAddressLine4(addy_hib_out.getAddressLine4());
                        addy_model_out.setCity(addy_hib_out.getCity());
                        addy_model_out.setPostCode(addy_hib_out.getPostCode());
                        addy_model_out.setPrimaryAddress(addy_hib_out.getIsPrimary());
                        addy_model_out.setEntityId(addy_hib_out.getClientCompany().getId());
                        
                        cc_model_addy_out.add(addy_model_out); //set address in list of addresses
                    }
                    c.setAddresses(cc_model_addy_out); //set address list in client company

                    List<ClientCompanyEmailAddress> email_hib_list = cq.getClientCompanyEmailAddresses(cc_hib_out.getId());
                    for (ClientCompanyEmailAddress email_hib_out : email_hib_list) {
                        EmailAddress email_model_out = new EmailAddress();
                        
                        email_model_out.setId(email_hib_out.getId());
                        email_model_out.setEmailAddress(email_hib_out.getEmailAddress());
                        email_model_out.setPrimaryEmail(email_hib_out.getIsPrimary());
                        email_model_out.setEntityId(email_hib_out.getClientCompany().getId());
                        
                        cc_model_email_out.add(email_model_out);
                    }
                    c.setEmailAddresses(cc_model_email_out);

                    List<ClientCompanyPhoneNumber> phone_hib_list = cq.getClientCompanyPhoneNumbers(cc_hib_out.getId());
                    for (ClientCompanyPhoneNumber phone_hib_out : phone_hib_list) {
                        PhoneNumber phone_model_out = new PhoneNumber();
                        
                        phone_model_out.setId(phone_hib_out.getId());
                        phone_model_out.setPhoneNumber(phone_hib_out.getPhoneNumber());
                        phone_model_out.setPrimaryPhoneNumber(phone_hib_out.getIsPrimary());
                        phone_model_out.setEntityId(phone_hib_out.getClientCompany().getId());
                        
                        cc_model_phone_out.add(phone_model_out);
                    }
                    c.setPhoneNumbers(cc_model_phone_out);
                    
                    c.setNoShareholders(cq.getNumberOfShareholders(cc_hib_out.getId()));
                    c.setNoBondholders(cq.getNumberOfBondholders(cc_hib_out.getId()));
                    c.setShareUnitPrice(cq.getUnitPrice(cc_hib_out.getId()));

                    cc_model_out.add(c); //finally, add client company to list
                }

                logger.info("client company query successful - [{}]", login.getUserId());
                resp.setRetn(0);
                resp.setDesc("Successful");
                resp.setBody(cc_model_out);

                return resp;
            }

            logger.info("Unsuccessful client company query, due to incomplete descriptor. Contact system administrator - [{}]", login.getUserId());
            resp.setRetn(204);
            resp.setDesc("Unsuccessful client company query, due to incomplete descriptor. Contact system administrator");

            return resp;
        } catch (Exception ex) {
            logger.info("error querying client company. See error log - [{}]", login.getUserId());
            logger.error("error querying client company - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to query client company. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Processes request to upload a list of share quotations.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param shareQuotation the list of share quotations to be uploaded
     * @return response to the share quotation upload request
     */
    public Response uploadShareUnitQuotations_Request(Login login, String authenticator, List<ShareQuotation> shareQuotation) {
        logger.info("request to upload share unit quotations, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();

        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;

            //ensure all companies in share quotation list are legit
            int pos;
            boolean exists = false;
            for (pos = 0; pos < shareQuotation.size(); pos++) {
                exists = cq.checkClientCompanyByCode(shareQuotation.get(pos).getClientCompany().getCode());
                if (!exists) {
                    break; //if any one company code doesn't exist, break out of loop
                }
            }

            //client company must exist before its share quotations can be uploaded
            if (exists) {
                wrapper = new NotificationWrapper();
                prop = NotifierProperties.getInstance();
                qSender = new QueueSender(prop.getNotifierQueueFactory(),
                        prop.getAuthoriserNotifierQueueName());

                logger.info("client company codes exist - [{}]", login.getUserId());
                //wrap client company object in notification object, along with other information
                wrapper.setCode(notification.createCode(login));
                wrapper.setDescription("Upload of share-unit quotations");
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setNotificationType(NotificationType.upload_unit_quotations.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(shareQuotation);
                
                logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                resp = qSender.sendAuthorisationRequest(wrapper);
                return resp;
            }
            resp.setRetn(205);
            resp.setDesc("The client company code [" + shareQuotation.get(pos).getClientCompany().getCode() + "] does not exist.");
            logger.info("The client company code does not exist so cannot be edited - [{}]: [{}]",
                    shareQuotation.get(pos).getClientCompany().getCode(), login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error processing upload share unit quotations request. See error log - [{}]", login.getUserId());
            logger.error("error processing upload share unit quotations request - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process upload share quotation records request. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Processes request to upload a list of share quotations that has already been saved.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the share quotation upload request
     */
    public Response uploadShareUnitQuotations_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        Notification notification = new Notification();
        logger.info("authorisation for share unit quotation upload, invoked by [{}] - notification code: [{}]", login.getUserId(), notificationCode);
        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<ShareQuotation> quotationList = (List<ShareQuotation>) wrapper.getModel();
            
            boolean uploaded = cq.uploadShareQuotation(retrieveShareQuotation(quotationList));
            
            if (uploaded) {
                notification.markAttended(notificationCode);
                logger.info("share unit quotation upload authorised - [{}]", login.getUserId());
                resp.setRetn(0);
                resp.setDesc("Successful");
                return resp;
            }
            
            resp.setRetn(206);
            resp.setDesc("Unable to load share unit quotation from authorisation. Contact System Administrator");
            logger.info("unable to load share unit quotation from authorisation - [{}]", login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            logger.info("error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(206);
            resp.setDesc("Unable to load share unit quotation from authorisation. Contact System Administrator");
            
            return resp;
        } catch (Exception ex) {
            logger.info("error uploading share unit quotations. See error log - [{}]", login.getUserId());
            logger.error("error uploading share unit quotations - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to upload share quotation records. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     * Retrieves the share unit quotation.
     * @param login the user's login details
     * @return the response to the share unit quotation view request
     */
    public Response getShareUnitQuotations_request(Login login) {
        Response resp = new Response();
        logger.info("view share unit quotations, invoked by [{}]", login.getUserId());
        
        try {
            List<org.greenpole.hibernate.entity.ShareQuotation> share_q_hib_list = cq.retrieveShareUnitQuatationList();
            List<ShareQuotation> shareQuotations = new ArrayList();
            
            for (org.greenpole.hibernate.entity.ShareQuotation share_q_hib : share_q_hib_list) {
                org.greenpole.hibernate.entity.ClientCompany cc_hib = cq.getClientCompany(share_q_hib.getClientCompany().getId());
                ShareQuotation shareQuotation_model = new ShareQuotation();
                ClientCompany cc_model = new ClientCompany();
                cc_model.setName(cc_hib.getName());
                cc_model.setCode(cc_hib.getCode());
                shareQuotation_model.setClientCompany(cc_model);
                shareQuotation_model.setUnitPrice(share_q_hib.getUnitPrice());
                shareQuotations.add(shareQuotation_model);
            }
            resp.setBody(shareQuotations);
            resp.setRetn(0);
            resp.setDesc("Successful");
            logger.info("share unit quotations retreival successful - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error retrieving share unit quotations. See error log - [{}]", login.getUserId());
            logger.error("error retrieving share unit quotations - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to retrieve share quotation records. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Processes request to set up an Initial Public Offer.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the
     * notification
     * @param ipo the initial public offer details for a client company
     * @return response to the initial public offer request
     */
    public Response setupInitialPublicOffer_Request(Login login, String authenticator, InitialPublicOffer ipo) {
        logger.info("request to set up Initial Public Offer, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;

        try {
            if (cq.checkClientCompany(ipo.getClientCompanyId())) {
                org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(ipo.getClientCompanyId());
                logger.info("client company [{}] checks out - [{}]", cc.getName(), login.getUserId());
                
                if (cq.clientCompanyHasIpo(ipo.getClientCompanyId())) {
                    resp.setRetn(207);
                    resp.setDesc("The client company - " + cc.getName() + " - already has an IPO.");
                    logger.info("The client company - [{}] - already has an IPO - [{}]",
                            cc.getName(), login.getUserId());
                    return resp;
                }

                if (!cq.checkClientCompanyForShareholders(cc.getName())) {
                    logger.info("client company [{}] checks out. No shareholders found - [{}]", cc.getName(), login.getUserId());
                    wrapper = new NotificationWrapper();
                    prop = NotifierProperties.getInstance();
                    qSender = new QueueSender(prop.getNotifierQueueFactory(),
                            prop.getAuthoriserNotifierQueueName());
                    List<InitialPublicOffer> ipoList = new ArrayList();
                    ipoList.add(ipo);
                    wrapper.setCode(notification.createCode(login));
                    wrapper.setDescription("Authenticate Initial Public Offer setup under the client company " + cc.getName());
                    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                    wrapper.setNotificationType(NotificationType.setup_ipo.toString());
                    wrapper.setFrom(login.getUserId());
                    wrapper.setTo(authenticator);
                    wrapper.setModel(ipoList);
                    
                    logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                    resp = qSender.sendAuthorisationRequest(wrapper);
                    return resp;
                }
                resp.setRetn(207);
                resp.setDesc("The client company - " + cc.getName() + " - has shareholders. An IPO cannot be setup for it.");
                logger.info("The client company - [{}] - has shareholders. An IPO cannot be setup for it - [{}]",
                        cc.getName(), login.getUserId());
                return resp;
            }
            resp.setRetn(207);//change error code
            resp.setDesc("The specified client company does not exist in the database.");
            logger.info("The specified client company does not exist in the database - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error processing IPO setup. See error log - [{}]", login.getUserId());
            logger.error("error processing IPO setup - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process IPO setup. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Processes request to setup Initial public offer that has been saved to file with the notificationCode.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the setup initial public offer request
     */
    public Response setupInitialPublicOffer_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        Notification notification = new Notification();
        logger.info("authorise Initial Public Offer setup, invoked by [{}]", login.getUserId());
        
        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<InitialPublicOffer> ipoList = (List<InitialPublicOffer>) wrapper.getModel();
            InitialPublicOffer ipoModel = ipoList.get(0);
            
            if (cq.checkClientCompany(ipoModel.getClientCompanyId())) {
                org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(ipoModel.getClientCompanyId());
                logger.info("client company [{}] checks out - [{}]", cc.getName(), login.getUserId());
                
                if (cq.clientCompanyHasIpo(ipoModel.getClientCompanyId())) {
                    resp.setRetn(207);
                    resp.setDesc("The client company - " + cc.getName() + " - already has an IPO.");
                    logger.info("The client company - [{}] - already has an IPO - [{}]",
                            cc.getName(), login.getUserId());
                    return resp;
                }
                
                if (!cq.checkClientCompanyForShareholders(cc.getName())) {
                    logger.info("client company [{}] checks out. No shareholders found - [{}]", cc.getName(), login.getUserId());
                    
                    org.greenpole.hibernate.entity.InitialPublicOffer ipo_hib = unwrapInitialPublicOfferModel(ipoModel, resp, login);
                    cq.createInitialPublicOffer(ipo_hib);

                    notification.markAttended(notificationCode);
                    resp.setRetn(0);
                    resp.setDesc("Success");
                    logger.info("Initial Public Offer was Successfully created - [{}]", login.getUserId());
                    return resp;
                }
                resp.setRetn(208);
                resp.setDesc("The client company - " + cc.getName() + " - has shareholders. An IPO cannot be setup for it.");
                logger.info("The client company - [{}] - has shareholders. An IPO cannot be setup for it - [{}]",
                        cc.getName(), login.getUserId());
                return resp;
            }
            resp.setRetn(208);//change error code
            resp.setDesc("The specified client company does not exist in the database.");
            logger.info("The specified client company does not exist in the database - [{}]", login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            logger.info("error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(98);
            resp.setDesc("Unable to setup IPO. Contact System Administrator");
            
            return resp;
        } catch (Exception ex) {
            logger.info("error setting up IPO. See error log - [{}]", login.getUserId());
            logger.error("error setting up IPO - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to setup IPO. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }
    
    /**
     * Request to create a bond offer.
     * @param login The user's login details
     * @param authenticator The authenticator user meant to receive the
     * notification
     * @param bond The bond details to be created
     * @return response to the create bond offer request
     */
    public Response setupBondOffer_Request(Login login, String authenticator, BondOffer bond) {
        logger.info("request to create bond offer [{}] at [{}] unit price, invoked by - [{}]", 
                bond.getTitle(), bond.getUnitPrice(), login.getUserId());

        Response resp = new Response();
        Notification notification = new Notification();
        
        NotificationWrapper wrapper;
        QueueSender queue;
        NotifierProperties prop;
        
        String desc = "";
        boolean flag = false;

        try {
            // Check if bond title has a value
            if (bond.getTitle() == null || "".equals(bond.getTitle()) ) {
                desc += "\nBond title should not be empty";
            } else if (bond.getBondTypeId() <= 0) {
                desc += "\nBond type should not be empty";
            } else if (bond.getUnitPrice() <= 0) {
                desc += "\nBond unit price should be greater than zero";
            } else if (bond.getInterestRate() <= 0) {
                desc += "\nBond interest rate should be greater than zero";
            } else if (bond.getPaymentPlanId() <= 0) {
                desc += "\nBond payment plan should be specified";
            } else {
                flag = true;
            }
            
            if (flag && bond.getBondTypeId() > 0) {
                boolean found = false;
                for (org.greenpole.hibernate.entity.BondType bt : cq.getAllBondTypes()) {
                    if (bond.getBondTypeId() == bt.getId()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    desc += "\nBond type not valid";
                    flag = false;
                }
            }
            
            if (flag && bond.getPaymentPlanId() > 0) {
                boolean found = false;
                for (org.greenpole.hibernate.entity.BondOfferPaymentPlan bp : cq.getAllBondOfferPaymentPlans()) {
                    if (bond.getPaymentPlanId() == bp.getId()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    desc += "\nBond offer payment plan not valid";
                    flag = false;
                }
            }

            if (flag) {
                wrapper = new NotificationWrapper();
                prop = NotifierProperties.getInstance();
                queue = new QueueSender(prop.getNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
                List<BondOffer> bc = new ArrayList<>();
                bc.add(bond);
                
                wrapper.setCode(notification.createCode(login));
                wrapper.setDescription("Authenticate Bond Setup - " + bond.getTitle());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setNotificationType(NotificationType.setup_bond_offer.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(bc);
                
                logger.info("Notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                resp = queue.sendAuthorisationRequest(wrapper);
                return resp;
            }
            resp.setRetn(209);
            resp.setDesc("Error filing bond offer details: " + desc);
            logger.info("Error filing bond offer details: [{}] - [{}]", desc, login.getUserId());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process bond setup request. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            logger.info("Error processing bond setup request. See error log. [{}] - [{}]", resp.getRetn(), login.getUserId());
            logger.error("Error processing bond setup request - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes saved request to create bond offer.
     * @param login The user's login details
     * @param notificationCode The notification code
     * @return response to request to create bond offer
     */
    public Response setupBondOffer_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        Notification notification = new Notification();
        logger.info("Bond setup creation authorised. [{}] - [{}]", notificationCode, login.getUserId());
        
        String desc = "";
        boolean flag = false;
        
        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<BondOffer> list = (List<BondOffer>) wrapper.getModel();
            BondOffer bondModel = list.get(0);
            
            // Check if bond title has a value
            if (bondModel.getTitle() == null || "".equals(bondModel.getTitle()) ) {
                desc += "\nBond title should not be empty";
            } else if (bondModel.getBondTypeId() <= 0) {
                desc += "\nBond type should not be empty";
            } else if (bondModel.getUnitPrice() <= 0) {
                desc += "\nBond unit price should be greater than zero";
            } else if (bondModel.getInterestRate() <= 0) {
                desc += "\nBond interest rate should be greater than zero";
            } else if (bondModel.getPaymentPlanId() <= 0) {
                desc += "\nBond payment plan should be specified";
            } else {
                flag = true;
            }
            
            if (flag && bondModel.getBondTypeId() > 0) {
                boolean found = false;
                for (org.greenpole.hibernate.entity.BondType bt : cq.getAllBondTypes()) {
                    if (bondModel.getBondTypeId() == bt.getId()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    desc += "\nBond type not valid";
                    flag = false;
                }
            }
            
            if (flag && bondModel.getPaymentPlanId() > 0) {
                boolean found = false;
                for (org.greenpole.hibernate.entity.BondOfferPaymentPlan bp : cq.getAllBondOfferPaymentPlans()) {
                    if (bondModel.getPaymentPlanId() == bp.getId()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    desc += "\nBond offer payment plan not valid";
                    flag = false;
                }
            }
            
            if (flag) {
                // get BondOffer entity initialised with Bond Model
                org.greenpole.hibernate.entity.BondOffer bondOffer = unwrapBondOfferModel(bondModel);
                cq.createBondOffer(bondOffer);
                
                notification.markAttended(notificationCode);
                resp.setRetn(0);
                resp.setDesc("Successful");
                logger.info("Bond offer created successfully - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(210);
            resp.setDesc("Error filing bond offer details: " + desc);
            logger.info("Error filing bond offer details: [{}] - [{}]", desc, login.getUserId());
            return resp;
        } catch (ParseException ex) {
            resp.setRetn(210);
            resp.setDesc("Error converting string to date type. See error log");
            logger.info("Error converting string to date type. See error log. [{}] - [{}]", resp.getRetn(), login.getUserId());
            logger.error("Error converting string to date type - [" + login.getUserId() + "]", ex);
            return resp;
        } catch (JAXBException ex) {
            resp.setRetn(98);
            resp.setDesc("Error loading notification xml file. See error log");
            logger.info("Error loading notification xml file. See error log. [{}] - [{}]", resp.getRetn(), login.getUserId());
            logger.error("Error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to create bond. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            logger.info("Error creating bond. See error log. [{}] - [{}]", resp.getRetn(), login.getUserId());
            logger.error("Error creating bond - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }
    
    /**
     * Request to create a Private Placement.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the
     * notification
     * @param privatePlacement the private placement details to be processed
     * @return response object back to sender indicating creation request status
     */
    public Response setupPrivatePlacement_Request(Login login, String authenticator, PrivatePlacement privatePlacement) {
        logger.info("request to create private placement, invoked by [{}]", login.getUserId());

        Response resp = new Response();
        Notification notification = new Notification();
        
        NotificationWrapper wrapper;
        QueueSender queue;
        NotifierProperties props;

        try {
            Date date = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            
            boolean exits = cq.checkClientCompany(privatePlacement.getClientCompanyId());
            if (exits) {
                org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(privatePlacement.getClientCompanyId());
                if (cc.getValid()) {
                    if (date.before(formatter.parse(privatePlacement.getClosingDate()))) {
                        if (cq.checkClientCompanyForShareholders(cc.getName())) {
                            if (cq.checkOpenPrivatePlacement(cc.getId())) {
                                wrapper = new NotificationWrapper();
                                props = NotifierProperties.getInstance();
                                queue = new QueueSender(props.getNotifierQueueFactory(), props.getAuthoriserNotifierQueueName());
                                List<PrivatePlacement> ppc = new ArrayList<>();
                                ppc.add(privatePlacement);
                                
                                wrapper.setCode(notification.createCode(login));
                                wrapper.setDescription("Create Private Placement for " + cc.getName());
                                wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
                                wrapper.setNotificationType(NotificationType.setup_private_placement.toString());
                                wrapper.setFrom(login.getUserId());
                                wrapper.setTo(authenticator);
                                wrapper.setModel(ppc);
                                
                                logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                                resp = queue.sendAuthorisationRequest(wrapper);
                                return resp;
                            }
                            resp.setRetn(211);
                            resp.setDesc("A private placement is currently open");
                            logger.info("A private placement is currently open - [{}]", login.getUserId());
                            return resp;
                        }
                        resp.setRetn(211);
                        resp.setDesc("No shareholders in client company for private placement");
                        logger.info("No shareholders in client company for priate placement - [{}]", login.getUserId());
                        return resp;
                    }
                    resp.setRetn(211);
                    resp.setDesc("Private placement cannot be closed before current date");
                    logger.info("Private placement cannot be closed before current date - [{}]", login.getUserId());
                    return resp;
                }
                resp.setRetn(211);
                resp.setDesc("Client company for private placement is not valid");
                logger.info("Client company for priate placement is not valid - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(211);
            resp.setDesc("Client company for private placement does not exist");
            logger.info("Client company for priate placement does not exist - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process private placement creation. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            logger.info("Error processing private placement creation. See error log - [{}]", login.getUserId());
            logger.error("Error processing private placement creation - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes saved request to create private placement.
     * @param login The user's login details
     * @param notificationCode The notification code
     * @return Response to the create private placement request
     */
    public Response setupPrivatePlacement_Authorise(Login login, String notificationCode) {
        logger.info("authorise private placement creation, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            Date date = new Date();
            
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<PrivatePlacement> pplist = (List<PrivatePlacement>) wrapper.getModel();
            PrivatePlacement ppModel = pplist.get(0);
            org.greenpole.hibernate.entity.PrivatePlacement ppEntity = new org.greenpole.hibernate.entity.PrivatePlacement();
            
            if (cq.checkClientCompany(ppModel.getClientCompanyId())) {
                org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(ppModel.getClientCompanyId());
                if (cc.getValid()) {
                    if (date.before(formatter.parse(ppModel.getClosingDate()))) {
                        if (cq.checkClientCompanyForShareholders(cc.getName())) {
                            if (cq.checkOpenPrivatePlacement(cc.getId())) {
                                ppEntity.setClientCompany(cc);
                                ppEntity.setTotalSharesOnOffer(ppModel.getTotalSharesOnOffer().longValue());
                                ppEntity.setMethodOnOffer(Integer.parseInt(ppModel.getMethodOfOffer()));
                                ppEntity.setStartingMinSubscrptn(ppModel.getStartingMinimumSubscription());
                                ppEntity.setContinuingMinSubscrptn(ppModel.getContinuingMinimumSubscription());
                                ppEntity.setOfferPrice(ppModel.getOfferPrice());
                                ppEntity.setOfferSize(ppModel.getOfferSize().doubleValue());
                                ppEntity.setOpeningDate(formatter.parse(ppModel.getOpeningDate()));
                                ppEntity.setClosingDate(formatter.parse(ppModel.getClosingDate()));
                                ppEntity.setPlacementClosed(false);
                                
                                cq.createPrivatePlacement(ppEntity);
                                
                                notification.markAttended(notificationCode);
                                resp.setRetn(0);
                                resp.setDesc("Successful");
                                logger.info("Private Placement created for Client Company: [{}] - [{}]", cc.getName(), login.getUserId());
                            }
                            resp.setRetn(212);
                            resp.setDesc("A private placement is currently open");
                            logger.info("A private placement is currently open - [{}]", login.getUserId());
                            return resp;
                        }
                        resp.setRetn(212);
                        resp.setDesc("No shareholders in client company for private placement");
                        logger.info("No shareholders in client company for priate placement - [{}]", login.getUserId());
                        return resp;
                    }
                    resp.setRetn(212);
                    resp.setDesc("Private placement cannot be closed before current date");
                    logger.info("Private placement cannot be closed before current date - [{}]", login.getUserId());
                    return resp;
                }
                resp.setRetn(212);
                resp.setDesc("Client company for private placement is not valid");
                logger.info("Client company for priate placement is not valid - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(212);
            resp.setDesc("Client company for private placement does not exist");
            logger.info("Client company for priate placement does not exist - [{}]", login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            resp.setRetn(98);
            resp.setDesc("General Error: Unable to load notification xml file. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            logger.info("Error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("Error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            return resp;
        } catch (ParseException ex) {
            resp.setRetn(212);
            resp.setDesc("General Error: Unable to convert from string to date type. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            logger.info("Error converting from string to date type - [{}]", login.getUserId());
            logger.error("Error converting from string to date type - [" + login.getUserId() + "]", ex);
            return resp;
        } catch (Exception ex) {            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to create private placement. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            logger.info("Error creating private placement. See error log - [{}]", login.getUserId());
            logger.error("Error creating private placement - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }
    
    /**
     * Request to query all available client companies in the system.
     * @param login The user's login details
     * @return Response to the create private placement request
     */
    public Response queryAllClientCompanies_Request(Login login) {
        logger.info("request to query all client companies, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        
        try {
            
            List<ClientCompany> cclist = new ArrayList<>();
            List<org.greenpole.hibernate.entity.ClientCompany> cc_hiblist = cq.getAllClientCompanies();
            
            for (org.greenpole.hibernate.entity.ClientCompany cc_hib : cc_hiblist) {
                ClientCompany cc = new ClientCompany();
                cc.setId(cc_hib.getId());
                cc.setCode(cc_hib.getCode());
                cc.setName(cc_hib.getName());
                
                cclist.add(cc);
            }
            resp.setRetn(0);
            resp.setDesc("Successful");
            resp.setBody(cclist);
            logger.info("All client companies queried successfully - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to create private placement. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            logger.info("Error creating private placement. See error log - [{}]", login.getUserId());
            logger.error("Error creating private placement - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }
    
    /**
     * Request to query specific client company.
     * @param login The user's login details
     * @param clientCompanyId the client company id
     * @return Response to the create private placement request
     */
    public Response queryClientCompany_Request(Login login, int clientCompanyId) {
        logger.info("request to query specific client company [{}], invoked by [{}]", clientCompanyId, login.getUserId());
        Response resp = new Response();
        
        try {
            if (!cq.checkClientCompany(clientCompanyId)) {
                resp.setRetn(213);
                resp.setDesc("Client company is not valid or does not exist");
                logger.info("Client company is not valid or does not exist - [{}]", login.getUserId());
                return resp;
            }
            
            List<ClientCompany> cc_list = new ArrayList<>();
            
            org.greenpole.hibernate.entity.ClientCompany cc_hib = cq.getClientCompany(clientCompanyId);
            
            List<Address> cc_addy_list = new ArrayList<>();
            List<PhoneNumber> cc_phone_list = new ArrayList<>();
            List<EmailAddress> cc_email_list = new ArrayList<>();
            
            ClientCompany cc = new ClientCompany();

            cc.setDepositoryId(cc_hib.getDepository().getId());
            cc.setNseSectorId(cc_hib.getNseSector().getId());

            cc.setId(cc_hib.getId());
            cc.setName(cc_hib.getName());
            cc.setCeo(cc_hib.getCeo());
            cc.setCode(cc_hib.getCode());
            cc.setSecretary(cc_hib.getSecretary());

            //get all available addresses, email addresses and phone numbers
            List<ClientCompanyAddress> addy_hib_list = cq.getClientCompanyAddresses(cc_hib.getId());
            for (ClientCompanyAddress addy_hib_out : addy_hib_list) {
                Address addy = new Address(); //prepare address model to set

                addy.setId(addy_hib_out.getId());
                addy.setAddressLine1(addy_hib_out.getAddressLine1());
                addy.setState(addy_hib_out.getState());
                addy.setCountry(addy_hib_out.getCountry());
                addy.setAddressLine2(addy_hib_out.getAddressLine2());
                addy.setAddressLine3(addy_hib_out.getAddressLine3());
                addy.setAddressLine4(addy_hib_out.getAddressLine4());
                addy.setCity(addy_hib_out.getCity());
                addy.setPostCode(addy_hib_out.getPostCode());
                addy.setPrimaryAddress(addy_hib_out.getIsPrimary());
                addy.setEntityId(addy_hib_out.getClientCompany().getId());

                cc_addy_list.add(addy); //set address in list of addresses
            }
            cc.setAddresses(cc_addy_list); //set address list in client company

            List<ClientCompanyEmailAddress> email_hib_list = cq.getClientCompanyEmailAddresses(cc_hib.getId());
            for (ClientCompanyEmailAddress email_hib_out : email_hib_list) {
                EmailAddress email = new EmailAddress();

                email.setId(email_hib_out.getId());
                email.setEmailAddress(email_hib_out.getEmailAddress());
                email.setPrimaryEmail(email_hib_out.getIsPrimary());
                email.setEntityId(email_hib_out.getClientCompany().getId());

                cc_email_list.add(email);
            }
            cc.setEmailAddresses(cc_email_list);

            List<ClientCompanyPhoneNumber> phone_hib_list = cq.getClientCompanyPhoneNumbers(cc_hib.getId());
            for (ClientCompanyPhoneNumber phone_hib_out : phone_hib_list) {
                PhoneNumber phone = new PhoneNumber();

                phone.setId(phone_hib_out.getId());
                phone.setPhoneNumber(phone_hib_out.getPhoneNumber());
                phone.setPrimaryPhoneNumber(phone_hib_out.getIsPrimary());
                phone.setEntityId(phone_hib_out.getClientCompany().getId());

                cc_phone_list.add(phone);
            }
            cc.setPhoneNumbers(cc_phone_list);

            cc.setNoShareholders(cq.getNumberOfShareholders(cc_hib.getId()));
            cc.setNoBondholders(cq.getNumberOfBondholders(cc_hib.getId()));
            cc.setShareUnitPrice(cq.getUnitPrice(cc_hib.getId()));

            cc_list.add(cc); //finally, add client company to list
            
            resp.setRetn(0);
            resp.setDesc("Successful");
            resp.setBody(cc_list);
            logger.info("All client companies queried successfully - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to query client company. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            logger.info("Error querying client company. See error log - [{}]", login.getUserId());
            logger.error("Error querying client company - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }
    
    /**
     * Searches for a specified bond offer.
     * @param login the user's login details
     * @param bondOfferId bond offer id
     * @return the response to the query bond offer request
     */
    public Response queryBondOffer_Request(Login login, int bondOfferId) {
        logger.info("request to query specific bond offer, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            
            if (!cq.checkBondOffer(bondOfferId)) {
                resp.setRetn(214);
                resp.setDesc("Bond offer does not exist");
                logger.info("Bond offer does not exist - [{}]", login.getUserId());
                return resp;
            }
            
            List<BondOffer> bo_list = new ArrayList<>();
            org.greenpole.hibernate.entity.BondOffer bo_hib = cq.getBondOffer(bondOfferId);
            
            BondOffer bo = new BondOffer();
            
            bo.setId(bo_hib.getId());
            bo.setTitle(bo_hib.getTitle());
            
            if (bo_hib.getBondUnitPrice() != null)
                bo.setUnitPrice(bo_hib.getBondUnitPrice());
            
            if (bo_hib.getBondMaturity() != null)
                bo.setBondMaturity(formatter.format(bo_hib.getBondMaturity()));
            
            if (bo_hib.getBondType() != null) {
                BondType type = cq.getBondType(bo_hib.getBondType().getId());
                bo.setBondType(type.getType());
                bo.setBondTypeId(type.getId());
            }
            
            if (bo_hib.getClientCompany() != null) {
                org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(bo_hib.getClientCompany().getId());
                bo.setClientCompanyId(cc.getId());
                bo.setClientCompanyName(cc.getName());
            }
            
            if (bo_hib.getInterestRate() != null)
                bo.setInterestRate(bo_hib.getInterestRate());
            
            if (bo_hib.getBondOfferPaymentPlan() != null) {
                BondOfferPaymentPlan plan = cq.getBondOfferPaymentPlan(bo_hib.getBondOfferPaymentPlan().getId());
                bo.setPaymentPlan(plan.getPaymentPlan());
                bo.setPaymentPlanId(plan.getId());
            }
            
            bo_list.add(bo);
            
            resp.setRetn(0);
            resp.setDesc("Successful");
            resp.setBody(bo_list);
            logger.info("Bond offer queried successfully - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to query bond offer. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            logger.info("Error querying bond offer. See error log - [{}]", login.getUserId());
            logger.error("Error querying bond offer - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Unwraps bond model and inserts its details into the bond hibernate entity.
     * @param bondModel the bond model
     * @return BondOffer object to setupBondOfferAuthorise method
     */
    private org.greenpole.hibernate.entity.BondOffer unwrapBondOfferModel(BondOffer bondModel) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        
        // instantiate required hibernate entities
        org.greenpole.hibernate.entity.BondOffer bond_main = new org.greenpole.hibernate.entity.BondOffer();
        
        org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(bondModel.getClientCompanyId());
        BondType type = cq.getBondType(bondModel.getBondTypeId());
        BondOfferPaymentPlan paymentPlan = cq.getBondOfferPaymentPlan(bondModel.getPaymentPlanId());
        
        bond_main.setTitle(bondModel.getTitle());
        bond_main.setBondMaturity(formatter.parse(bondModel.getBondMaturity()));
        bond_main.setBondUnitPrice(bondModel.getUnitPrice());
        bond_main.setBondType(type);
        bond_main.setInterestRate(bondModel.getInterestRate());
        bond_main.setBondOfferPaymentPlan(paymentPlan);
        bond_main.setClientCompany(cc);
        bond_main.setValid(true);

        return bond_main;
    }
    
    /**
     * creates the hibernate entity object using the InitialPublicOffer model.
     * @param ipoModel the InitialPublicOffer object
     * @return the hibernate entity object
     */
    private org.greenpole.hibernate.entity.InitialPublicOffer unwrapInitialPublicOfferModel(InitialPublicOffer ipoModel, Response resp, Login login) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        org.greenpole.hibernate.entity.InitialPublicOffer ipo_hib = new org.greenpole.hibernate.entity.InitialPublicOffer();
        
        org.greenpole.hibernate.entity.ClientCompany cc_hib = cq.getClientCompany(ipoModel.getClientCompanyId());
        ipo_hib.setClientCompany(cc_hib);
        ipo_hib.setTotalSharesOnOffer(ipoModel.getTotalSharesOnOffer().longValue());
        ipo_hib.setMethodOfOffer(ipoModel.getMethodOfOffer());
        ipo_hib.setStartingMinSub(ipoModel.getStartingMinimumSubscription());
        ipo_hib.setContMinSub(ipoModel.getContinuingMinimumSubscription());
        ipo_hib.setOfferPrice(ipoModel.getOfferPrice());
        ipo_hib.setOfferSize(ipoModel.getOfferPrice() * ipoModel.getTotalSharesOnOffer().longValue());
        ipo_hib.setIpoClosed(false);
        ipo_hib.setOpeningDate(formatter.parse(ipoModel.getOpeningDate()));
        ipo_hib.setClosingDate(formatter.parse(ipoModel.getClosingDate()));
        ipo_hib.setTax(ipoModel.getTax());
        ipo_hib.setInterestRate(ipoModel.getInterestRate());
        return ipo_hib;
    }
    
    /**
     * Unwraps the list share quotation models to create the list hibernate share quotation entities.
     * @param quotationList the list of share quotation models
     * @return the list hibernate share quotation entities
     */
    private List<org.greenpole.hibernate.entity.ShareQuotation> retrieveShareQuotation(List<ShareQuotation> quotationList) {
        List<org.greenpole.hibernate.entity.ShareQuotation> shareQuotations_hib = new ArrayList<>();
        if (quotationList != null) {//guard against null list, to avoid null pointer exception
            for (ShareQuotation sq : quotationList) {
                org.greenpole.hibernate.entity.ShareQuotation shareQuotation = new org.greenpole.hibernate.entity.ShareQuotation();
                String code = sq.getClientCompany().getCode();
                if (cq.companyHasShareQuotation(code)) {
                    shareQuotation = cq.getShareQuotation(code);
                }
                org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(code);
                shareQuotation.setClientCompany(cc);
                shareQuotation.setUnitPrice(sq.getUnitPrice());
                shareQuotations_hib.add(shareQuotation);
            }
        }
        return shareQuotations_hib;
    }
    
    /**
     * Unwraps the client company model to create the hibernate client company phone number entity.
     * @param ccModel the client company model (not to be confused with the client company hibernate entity)
     * @param freshCreation if the client company phone number is being created for the first time or undergoing an edit
     * @return a list of hibernate client company phone number entity object(s)
     */
    private List<org.greenpole.hibernate.entity.ClientCompanyPhoneNumber> retrievePhoneNumberModel(ClientCompany ccModel/*, boolean freshCreation*/) {
        List<PhoneNumber> phoneList;
        if (ccModel.getPhoneNumbers() != null)
            phoneList = ccModel.getPhoneNumbers();
        else
            phoneList = new ArrayList<>();
        
        
        List<org.greenpole.hibernate.entity.ClientCompanyPhoneNumber> toSend = new ArrayList<>();
        
        for (PhoneNumber ph : phoneList) {
            org.greenpole.hibernate.entity.ClientCompanyPhoneNumber phone = new org.greenpole.hibernate.entity.ClientCompanyPhoneNumber();
            
            if (ph.getId() > 0) {
                //phone.setId(ph.getId());
                phone = cq.getClientCompanyPhoneNumber(ph.getId());
            }
            
            phone.setPhoneNumber(ph.getPhoneNumber());
            phone.setIsPrimary(ph.isPrimaryPhoneNumber());
            
            //put phone entity in list
            toSend.add(phone);
        }
        return toSend;
    }

    /**
     * Unwraps the client company model to delete the hibernate client company phone number entity.
     * @param ccModel the client company model (not to be confused with the client company hibernate entity)
     * @param freshCreation if the client company phone number is being created for the first time or undergoing an edit
     * @return a list of hibernate client company phone number entity object(s)
     */
    private List<org.greenpole.hibernate.entity.ClientCompanyPhoneNumber> retrievePhoneNumberModelForDeletion(ClientCompany ccModel/*, boolean freshCreation*/) {
        List<PhoneNumber> phoneList;
        if (ccModel.getDeletedPhoneNumbers() != null)
            phoneList = ccModel.getDeletedPhoneNumbers();
        else
            phoneList = new ArrayList<>();
        
        
        List<org.greenpole.hibernate.entity.ClientCompanyPhoneNumber> toSend = new ArrayList<>();
        
        for (PhoneNumber ph : phoneList) {
            org.greenpole.hibernate.entity.ClientCompanyPhoneNumber phone;// = new org.greenpole.hibernate.entity.ClientCompanyPhoneNumber();
            
            phone = cq.getClientCompanyPhoneNumber(ph.getId());
            /*phone.setId(ph.getId());
            phone.setPhoneNumber(ph.getPhoneNumber());
            phone.setIsPrimary(ph.isPrimaryPhoneNumber());*/
            
            //put phone entity in list
            toSend.add(phone);
        }
        return toSend;
    }
    
    /**
     * Unwraps the client company model to create the hibernate client company email address entity.
     * @param ccModel the client company model (not to be confused with the client company hibernate entity)
     * @param freshCreation if the client company email address is being created for the first time or undergoing an edit
     * @return a list of hibernate client company email address entity object(s)
     */
    private List<org.greenpole.hibernate.entity.ClientCompanyEmailAddress> retrieveEmailAddressModel(ClientCompany ccModel/*, boolean freshCreation*/) {
        List<EmailAddress> emailList;
        if (ccModel.getEmailAddresses() != null)
            emailList = ccModel.getEmailAddresses();
        else
            emailList = new ArrayList<>();
        
        List<org.greenpole.hibernate.entity.ClientCompanyEmailAddress> toSend = new ArrayList<>();
        
        for (EmailAddress em : emailList) {
            org.greenpole.hibernate.entity.ClientCompanyEmailAddress email = new org.greenpole.hibernate.entity.ClientCompanyEmailAddress();
            
            if (em.getId() > 0) {
                //email.setId(em.getId());
                email = cq.getClientCompanyEmailAddress(em.getId());
            }
            email.setEmailAddress(em.getEmailAddress());
            email.setIsPrimary(em.isPrimaryEmail());
            
            //add email to list
            toSend.add(email);
        }
        return toSend;
    }
    
    /**
     * Unwraps the client company model to delete the hibernate client company email address entity.
     * @param ccModel the client company model (not to be confused with the client company hibernate entity)
     * @param freshCreation if the client company email address is being created for the first time or undergoing an edit
     * @return a list of hibernate client company email address entity object(s)
     */
    private List<org.greenpole.hibernate.entity.ClientCompanyEmailAddress> retrieveEmailAddressModelForDeletion(ClientCompany ccModel/*, boolean freshCreation*/) {
        List<EmailAddress> emailList;
        if (ccModel.getDeletedEmailAddresses() != null)
            emailList = ccModel.getDeletedEmailAddresses();
        else
            emailList = new ArrayList<>();
        
        List<org.greenpole.hibernate.entity.ClientCompanyEmailAddress> toSend = new ArrayList<>();
        
        for (EmailAddress em : emailList) {
            org.greenpole.hibernate.entity.ClientCompanyEmailAddress email;// = new org.greenpole.hibernate.entity.ClientCompanyEmailAddress();
            
            email = cq.getClientCompanyEmailAddress(em.getId());
            /*email.setId(em.getId());
            email.setEmailAddress(em.getEmailAddress());
            email.setIsPrimary(em.isPrimaryEmail());*/
            
            //add email to list
            toSend.add(email);
        }
        return toSend;
    }

    /**
     * Unwraps the client company model to create the hibernate client company address entity.
     * @param ccModel the client company model (not to be confused with the client company hibernate entity)
     * @param freshCreation if the client company address is being created for the first time or undergoing an edit
     * @return a list of hibernate client company address entity object(s)
     */
    private List<org.greenpole.hibernate.entity.ClientCompanyAddress> retrieveAddressModel(ClientCompany ccModel/*, boolean freshCreation*/) {
        List<Address> addressList;
        if (ccModel.getAddresses() != null)
            addressList = ccModel.getAddresses();
        else
            addressList = new ArrayList<>();
        
        List<org.greenpole.hibernate.entity.ClientCompanyAddress> toSend = new ArrayList<>();
        
        for (Address addy : addressList) {
            org.greenpole.hibernate.entity.ClientCompanyAddress address = new org.greenpole.hibernate.entity.ClientCompanyAddress();
            
            if (addy.getId() > 0) {
                //address.setId(addy.getId());
                address = cq.getClientCompanyAddress(addy.getId());
            }
            address.setAddressLine1(addy.getAddressLine1());
            address.setState(addy.getState());
            address.setCountry(addy.getCountry());
            address.setIsPrimary(addy.isPrimaryAddress());
            address.setAddressLine2(addy.getAddressLine2());
            address.setAddressLine3(addy.getAddressLine3());
            address.setAddressLine4(addy.getAddressLine4());
            address.setCity(addy.getCity());
            address.setPostCode(addy.getPostCode());
            
            //add the address to list
            toSend.add(address);
        }
        
        return toSend;
    }
    
    /**
     * Unwraps the client company model to delete the hibernate client company address entity.
     * @param ccModel the client company model (not to be confused with the client company hibernate entity)
     * @param freshCreation if the client company address is being created for the first time or undergoing an edit
     * @return a list of hibernate client company address entity object(s)
     */
    private List<org.greenpole.hibernate.entity.ClientCompanyAddress> retrieveAddressModelForDeletion(ClientCompany ccModel) {
        List<Address> addressList;
        if (ccModel.getDeletedAddresses() != null) {
            addressList = ccModel.getDeletedAddresses();
        } else {
            addressList = new ArrayList<>();
        }
        
        List<org.greenpole.hibernate.entity.ClientCompanyAddress> toSend = new ArrayList<>();
        
        for (Address addy : addressList) {
            org.greenpole.hibernate.entity.ClientCompanyAddress address;// = new org.greenpole.hibernate.entity.ClientCompanyAddress();
            
            //address.setId(ccModel.getId());
            
            address = cq.getClientCompanyAddress(addy.getId());
            
            /*address.setAddressLine1(addy.getAddressLine1());
            address.setState(addy.getState());
            address.setCountry(addy.getCountry());
            address.setIsPrimary(addy.isPrimaryAddress());
            address.setAddressLine2(addy.getAddressLine2());
            address.setAddressLine3(addy.getAddressLine3());
            address.setAddressLine4(addy.getAddressLine4());
            address.setCity(addy.getCity());
            address.setPostCode(addy.getPostCode());*/
            
            //add the address to list
            toSend.add(address);
        }
        return toSend;
    }

    /**
     * Unwraps the client company model to create the hibernate client company entity.
     * @param ccModel the client company model (not to be confused with the client company hibernate entity)
     * @param freshCreation if the client company is being created for the first time or undergoing an edit
     * @return the hibernate client company entity object
     */
    private org.greenpole.hibernate.entity.ClientCompany retrieveClientCompanyModel(ClientCompany ccModel, boolean freshCreation) throws ParseException {
        //instantiate required hibernate entities
        org.greenpole.hibernate.entity.ClientCompany cc_main = new org.greenpole.hibernate.entity.ClientCompany();
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        
        if (ccModel != null) {//guard against null pointer exception
            //get values from client company model and insert into client company hibernate entity
            if (!freshCreation) {
                cc_main = cq.getClientCompany(ccModel.getId());
            } else {
                cc_main.setName(ccModel.getName());
                cc_main.setCode(ccModel.getCode());
            }
            cc_main.setCeo(ccModel.getCeo());
            cc_main.setSecretary(ccModel.getSecretary());
            if (ccModel.getDateIncorp() != null && !"".equals(ccModel.getDateIncorp()))
                cc_main.setDateIncorp(formatter.parse(ccModel.getDateIncorp()));
            cc_main.setValid(true);
            cc_main.setMerged(false);
            cc_main.setClientCompanyPrimary(true);
            
            //set depository id in object before setting object in hibernate client company object
            Depository depository = cq.getDepository(ccModel.getDepositoryId());
            cc_main.setDepository(depository);
            
            //set nse sector id in object before setting object in hibernate client company object
            NseSector nseSector = cq.getNseSector(ccModel.getNseSectorId());
            cc_main.setNseSector(nseSector);
            
        }
        return cc_main;
    }
}
