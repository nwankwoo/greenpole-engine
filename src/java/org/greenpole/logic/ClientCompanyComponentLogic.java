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
import org.greenpole.entity.response.Response;
import org.greenpole.hibernate.entity.BondOfferPaymentPlan;
import org.greenpole.hibernate.entity.BondType;
import org.greenpole.hibernate.entity.ClientCompanyAddress;
import org.greenpole.hibernate.entity.ClientCompanyAddressId;
import org.greenpole.hibernate.entity.ClientCompanyEmailAddress;
import org.greenpole.hibernate.entity.ClientCompanyEmailAddressId;
import org.greenpole.hibernate.entity.ClientCompanyPhoneNumber;
import org.greenpole.hibernate.entity.ClientCompanyPhoneNumberId;
import org.greenpole.hibernate.entity.Depository;
import org.greenpole.hibernate.entity.NseSector;
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
    private final GreenpoleProperties greenProp = new GreenpoleProperties(ClientCompanyComponentLogic.class);
    private final NotificationProperties notificationProp = new NotificationProperties(ClientCompanyComponentLogic.class);
    private static final Logger logger = LoggerFactory.getLogger(ClientCompanyComponentLogic.class);
    
    /**
     * Processes request to create a new client company.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param cc the client company to be created
     * @return response to the client company creation request 
     */
    public Response createClientCompany_Request(Login login, String authenticator, ClientCompany cc) {
        logger.info("request to create the client company [{}], invoked by", cc.getName(), login.getUserId());
        Response resp = new Response();
        
        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;
            String desc = "";
            boolean flag = false;

            //check if client company exists
            if (!cq.checkClientCompany(cc.getName())) {
                if (cc.getName() == null || "".equals(cc.getName())) {
                    desc += "\nClient company name should not be empty";
                } else if (cc.getCode() == null || "".equals(cc.getCode())) {
                    desc += "\nClient company code should not be empty";
                } else {
                    flag = true;
                }
                
                if (flag) {
                    wrapper = new NotificationWrapper();
                    prop = new NotifierProperties(ClientCompanyComponentLogic.class);
                    qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                            prop.getAuthoriserNotifierQueueName());

                    logger.info("client company does not exist - [{}]: [{}]", login.getUserId(), cc.getName());
                    List<ClientCompany> cclist = new ArrayList<>();
                    cclist.add(cc);
                    //wrap client company object in notification object, along with other information
                    wrapper.setCode(Notification.createCode(login));
                    wrapper.setDescription("Authenticate creation of the client company, " + cc.getName());
                    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
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
        boolean freshCreation = true;
        logger.info("authorise client company creation, invoked by [{}] - [{}]", login.getUserId(), notificationCode);
        try {
            //get client company model from wrapper
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<ClientCompany> list = (List<ClientCompany>) wrapper.getModel();
            ClientCompany ccModel = list.get(0);
            
            String desc = "";
            boolean flag = false;

            //check if client company exists
            if (!cq.checkClientCompany(ccModel.getName())) {
                if (ccModel.getName() == null || "".equals(ccModel.getName())) {
                    desc += "\nClient company name should not be empty";
                } else if (ccModel.getCode() == null || "".equals(ccModel.getCode())) {
                    desc += "\nClient company code should not be empty";
                } else {
                    flag = true;
                }
                
                if (flag) {
                    boolean created = cq.createClientCompany(retrieveClientCompanyModel(ccModel, freshCreation), retrieveAddressModel(ccModel),
                            retrieveEmailAddressModel(ccModel), retrievePhoneNumberModel(ccModel));
                    
                    if (created) {
                        wrapper.setAttendedTo(true);
                        Notification.persistNotificationFile(notificationProp.getNotificationLocation(), notificationCode, wrapper);
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
       
        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;
            
            //client company must exist to be edited
            if (cq.checkClientCompany(cc.getName())) {
                logger.info("client company exists - [{}]: [{}]", login.getUserId(), cc.getName());
                //if client company exists, it must be a primary client company
                if (cq.getClientCompanyByName(cc.getName()).isClientCompanyPrimary()) {

                    String desc = "";
                    boolean flag = false;

                    if ("".equals(cc.getCode()) || cc.getCode() == null) {
                        desc += "\nCode should not be empty";
                    } else if (!cc.getAddresses().isEmpty()) {
                        for (Address addy : cc.getAddresses()) {
                            if ("".equals(addy.getAddressLine1()) || addy.getAddressLine1() == null) {
                                desc += "\nAddress line 1 should not be empty. Delete entire address if you must";
                            } else if ("".equals(addy.getState()) || addy.getState() == null) {
                                desc += "\nState should not be empty. Delete entire address if you must";
                            } else if ("".equals(addy.getCountry()) || addy.getCountry() == null) {
                                desc += "\nCountry should not be empty. Delete entire address if you must";
                            }
                        }
                    } else if (!cc.getEmailAddresses().isEmpty()) {
                        for (EmailAddress email : cc.getEmailAddresses()) {
                            if ("".equals(email.getEmailAddress()) || email.getEmailAddress() == null) {
                                desc += "\nEmail address should not be empty. Delete email entry if you must";
                            }
                        }
                    } else if (!cc.getPhoneNumbers().isEmpty()) {
                        for (PhoneNumber phone : cc.getPhoneNumbers()) {
                            if ("".equals(phone.getPhoneNumber()) || phone.getPhoneNumber() == null) {
                                desc += "\nPhone number should not be empty. Delete phone number entry if you must";
                            }
                        }
                    } else {
                        flag = true;
                    }

                    if (flag) {
                        wrapper = new NotificationWrapper();
                        prop = new NotifierProperties(ClientCompanyComponentLogic.class);
                        qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                                prop.getAuthoriserNotifierQueueName());

                        logger.info("client company is valid and primary - [{}]: [{}]", login.getUserId(), cc.getName());
                        List<ClientCompany> cclist = new ArrayList<>();
                        cclist.add(cc);
                        //wrap client company object in notification object, along with other information
                        wrapper.setCode(Notification.createCode(login));
                        wrapper.setDescription("Authenticate change to client company, " + cc.getName());
                        wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                        wrapper.setFrom(login.getUserId());
                        wrapper.setTo(authenticator);
                        wrapper.setModel(cclist);
                        
                        logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                        resp = qSender.sendAuthorisationRequest(wrapper);
                        return resp;
                    } else {
                        resp.setRetn(202);
                        resp.setDesc("Error: " + desc);
                        logger.info("Error filing client company details - [{}]", login.getUserId());
                    }
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
        boolean freshCreation = false;
        logger.info("authorise client company change, invoked by [{}] - notification code: [{}]", login.getUserId(), notificationCode);
        try {
            //get client company model from wrapper
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<ClientCompany> list = (List<ClientCompany>) wrapper.getModel();
            ClientCompany ccModel = list.get(0);
            
            boolean edited = cq.updateClientCompany(retrieveClientCompanyModel(ccModel, freshCreation), retrieveAddressModel(ccModel),
                    retrieveEmailAddressModel(ccModel), retrievePhoneNumberModel(ccModel), retrieveAddressModelForDeletion(ccModel),
                    retrieveEmailAddressModelForDeletion(ccModel), retrievePhoneNumberModelForDeletion(ccModel));
            
            if (edited) {
                wrapper.setAttendedTo(true);
                Notification.persistNotificationFile(notificationProp.getNotificationLocation(), notificationCode, wrapper);
                logger.info("client company edited - [{}]: [{}]", ccModel.getName(), login.getUserId());
                resp.setRetn(0);
                resp.setDesc("Successful");
                return resp;
            }
            
            resp.setRetn(203);
            resp.setDesc("Unable to change client company from authorisation. Contact System Administrator");
            logger.info("unable to change client company from authorisation - [{}]", login.getUserId());
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
        logger.info("request to query client company, invoked by [{}]", login.getUserId());
        
        try {
            Map<String, String> descriptors = Descriptor.decipherDescriptor(queryParams.getDescriptor());

            if (descriptors.size() == 4) {
                String descriptor = queryParams.getDescriptor();

                org.greenpole.hibernate.entity.ClientCompany cc_hib_search = new org.greenpole.hibernate.entity.ClientCompany();
                ClientCompanyAddress cc_hib_address_search = new ClientCompanyAddress();
                ClientCompanyAddressId cc_hib_address_id_search = new ClientCompanyAddressId();
                ClientCompanyEmailAddress cc_hib_email_search = new ClientCompanyEmailAddress();
                ClientCompanyEmailAddressId cc_hib_email_id_search = new ClientCompanyEmailAddressId();
                ClientCompanyPhoneNumber cc_hib_phone_search = new ClientCompanyPhoneNumber();
                ClientCompanyPhoneNumberId cc_hib_phone_id_search = new ClientCompanyPhoneNumberId();

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
                    cc_hib_search.setCode(cc_model_search.getCode());
                    cc_hib_search.setSecretary(cc_model_search.getSecretary());

                    cc_hib_search.setDepository(depository_hib_search);
                    cc_hib_search.setNseSector(nseSector_hib_search);

                }

                Address address_model_search;
                if (queryParams.getClientCompany().getAddresses() != null && !queryParams.getClientCompany().getAddresses().isEmpty()) {
                    address_model_search = queryParams.getClientCompany().getAddresses().get(0);

                    cc_hib_address_id_search.setAddressLine1(address_model_search.getAddressLine1());
                    cc_hib_address_id_search.setCountry(address_model_search.getCountry());
                    cc_hib_address_id_search.setState(address_model_search.getState());

                    cc_hib_address_search.setAddressLine2(address_model_search.getAddressLine2());
                    cc_hib_address_search.setAddressLine3(address_model_search.getAddressLine3());
                    cc_hib_address_search.setAddressLine4(address_model_search.getAddressLine4());
                    cc_hib_address_search.setCity(address_model_search.getCity());
                    cc_hib_address_search.setPostCode(address_model_search.getPostCode());

                    cc_hib_address_search.setId(cc_hib_address_id_search); //put address id in address

                    Set cc_hib_address_set = new HashSet();
                    cc_hib_address_set.add(cc_hib_address_search); //put address in set

                    cc_hib_search.setClientCompanyAddresses(cc_hib_address_set); //put address set in client company entity
                }

                EmailAddress emailAddress_model_search;
                if (queryParams.getClientCompany().getEmailAddresses() != null && queryParams.getClientCompany().getEmailAddresses().isEmpty()) {
                    emailAddress_model_search = queryParams.getClientCompany().getEmailAddresses().get(0);

                    cc_hib_email_id_search.setEmailAddress(emailAddress_model_search.getEmailAddress());

                    cc_hib_email_search.setId(cc_hib_email_id_search); //put email id in email

                    Set cc_hib_email_set = new HashSet();
                    cc_hib_email_set.add(cc_hib_email_search); //put email in set

                    cc_hib_search.setClientCompanyEmailAddresses(cc_hib_email_set); //put email set in client company entity
                }

                PhoneNumber phoneNumber_model_search;
                if (queryParams.getClientCompany().getPhoneNumbers() != null && !queryParams.getClientCompany().getPhoneNumbers().isEmpty()) {
                    phoneNumber_model_search = queryParams.getClientCompany().getPhoneNumbers().get(0);

                    cc_hib_phone_id_search.setPhoneNumber(phoneNumber_model_search.getPhoneNumber());

                    cc_hib_phone_search.setId(cc_hib_phone_id_search); //put phone id in phone

                    Set cc_hib_phone_set = new HashSet();
                    cc_hib_phone_set.add(cc_hib_phone_search); //put phone in set

                    cc_hib_search.setClientCompanyEmailAddresses(cc_hib_phone_set); //put phone set in client company entity
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

                List<Address> cc_model_addy_out = new ArrayList<>();
                List<PhoneNumber> cc_model_phone_out = new ArrayList<>();
                List<EmailAddress> cc_model_email_out = new ArrayList<>();

                for (org.greenpole.hibernate.entity.ClientCompany cc_hib_out : cc_search_result) {
                    ClientCompany c = new ClientCompany();

                    c.setDepositoryId(cc_hib_out.getDepository().getId());
                    c.setNseSectorId(cc_hib_out.getNseSector().getId());

                    c.setName(cc_hib_out.getName());
                    c.setCeo(cc_hib_out.getCeo());
                    c.setCode(cc_hib_out.getCode());
                    c.setSecretary(cc_hib_out.getSecretary());

                    //get all available addresses, email addresses and phone numbers
                    List<ClientCompanyAddress> addy_hib_list = cq.getClientCompanyAddress(cc_hib_out.getId());
                    for (ClientCompanyAddress addy_hib_out : addy_hib_list) {
                        ClientCompanyAddressId addy_id_hib_out = addy_hib_out.getId(); //get client company address id from hibernate entity
                        Address addy_model_out = new Address(); //prepare address model to set

                        addy_model_out.setAddressLine1(addy_id_hib_out.getAddressLine1());
                        addy_model_out.setState(addy_id_hib_out.getState());
                        addy_model_out.setCountry(addy_id_hib_out.getCountry());
                        addy_model_out.setAddressLine2(addy_hib_out.getAddressLine2());
                        addy_model_out.setAddressLine3(addy_hib_out.getAddressLine3());
                        addy_model_out.setAddressLine4(addy_hib_out.getAddressLine4());
                        addy_model_out.setCity(addy_hib_out.getCity());
                        addy_model_out.setPostCode(addy_hib_out.getPostCode());
                        addy_model_out.setPrimaryAddress(addy_hib_out.isIsPrimary());

                        cc_model_addy_out.add(addy_model_out); //set address in list of addresses
                    }
                    c.setAddresses(cc_model_addy_out); //set address list in client company

                    List<ClientCompanyEmailAddress> email_hib_list = cq.getClientCompanyEmailAddress(cc_hib_out.getId());
                    for (ClientCompanyEmailAddress email_hib_out : email_hib_list) {
                        ClientCompanyEmailAddressId email_id_hib_out = email_hib_out.getId();
                        EmailAddress email_model_out = new EmailAddress();

                        email_model_out.setEmailAddress(email_id_hib_out.getEmailAddress());
                        email_model_out.setPrimaryEmail(email_hib_out.isIsPrimary());

                        cc_model_email_out.add(email_model_out);
                    }
                    c.setEmailAddresses(cc_model_email_out);

                    List<ClientCompanyPhoneNumber> phone_hib_list = cq.getClientCompanyPhoneNumber(cc_hib_out.getId());
                    for (ClientCompanyPhoneNumber phone_hib_out : phone_hib_list) {
                        ClientCompanyPhoneNumberId phone_id_hib_out = phone_hib_out.getId();
                        PhoneNumber phone_model_out = new PhoneNumber();

                        phone_model_out.setPhoneNumber(phone_id_hib_out.getPhoneNumber());
                        phone_model_out.setPrimaryPhoneNumber(phone_hib_out.isIsPrimary());

                        cc_model_phone_out.add(phone_model_out);
                    }
                    c.setPhoneNumbers(cc_model_phone_out);

                    cc_model_out.add(c); //finally, add client company to list
                }

                logger.info("client company query successful - [{}]", login.getUserId());
                resp.setRetn(0);
                resp.setDesc("Successful");
                resp.setBody(cc_model_out);

                return resp;
            }

            logger.info("client company query unsuccessful - [{}]", login.getUserId());
            resp.setRetn(204);
            resp.setDesc("Unsuccessful client company query, due to incomplete descriptor. Contact system administrator");

            return resp;
        } catch (Exception ex) {
            logger.info("error querying client company. See error log - [{}]", login.getUserId());
            logger.error("error querying client company - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to querying client company. Contact system administrator."
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

        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;

            //ensure all companies in share quotation list are legit
            int pos;
            boolean exists = false;
            for (pos = 0; pos < shareQuotation.size(); pos++) {
                exists = cq.checkClientCompany(shareQuotation.get(pos).getClientCompany().getCode());
                if (!exists) {
                    break; //if any one company code doesn't exist, break out of loop
                }
            }

            //client company must exist before its share quotations can be uploaded
            if (exists) {
                wrapper = new NotificationWrapper();
                prop = new NotifierProperties(ClientCompanyComponentLogic.class);
                qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                        prop.getAuthoriserNotifierQueueName());

                logger.info("client company codes exist - [{}]", login.getUserId());
                //wrap client company object in notification object, along with other information
                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription("Upload of share-unit quotations");
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
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
        logger.info("authorisation for share unit quotation upload, invoked by [{}] - notification code: [{}]", login.getUserId(), notificationCode);
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<ShareQuotation> quotationList = (List<ShareQuotation>) wrapper.getModel();
            
            boolean uploaded = cq.uploadShareQuotation(retrieveShareQuotation(quotationList));
            
            if (uploaded) {
                wrapper.setAttendedTo(true);
                Notification.persistNotificationFile(notificationProp.getNotificationLocation(), notificationCode, wrapper);
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
            ShareQuotation shareQuotation_model = new ShareQuotation();
            
            for (org.greenpole.hibernate.entity.ShareQuotation share_q_hib : share_q_hib_list) {
                org.greenpole.hibernate.entity.ClientCompany cc_hib = cq.getClientCompany(share_q_hib.getClientCompany().getId());
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
     * @param ipo the initial public offer details for a client company
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the
     * notification
     * @return response to the initial public offer request
     */
    public Response setupInitialPoblicOffer_Request(InitialPublicOffer ipo, Login login, String authenticator) {
        logger.info("request to set up Initial Public Offer, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;

        try {
            if (cq.checkClientCompany(ipo.getClientCompanyId())) {
                org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(ipo.getClientCompanyId());
                logger.info("client company [{}] checks out - [{}]", cc.getName(), login.getUserId());

                if (!cq.checkClientCompanyForShareholders(cc.getName())) {
                    logger.info("client company [{}] checks out. No shareholders found - [{}]", cc.getName(), login.getUserId());
                    wrapper = new NotificationWrapper();
                    prop = new NotifierProperties(ClientCompanyComponentLogic.class);
                    qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                            prop.getAuthoriserNotifierQueueName());
                    List<InitialPublicOffer> ipoList = new ArrayList();
                    ipoList.add(ipo);
                    wrapper.setCode(Notification.createCode(login));
                    wrapper.setDescription("Authenticate set up of an Initial Public Offer under the client company " + cc.getName());
                    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
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
    public Response setUpInitialPublicOffer_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        logger.info("authorise Initial Public Offer setup, invoked by [{}]", login.getUserId());
        
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<InitialPublicOffer> ipoList = (List<InitialPublicOffer>) wrapper.getModel();
            InitialPublicOffer ipoModel = ipoList.get(0);
            
            if (cq.checkClientCompany(ipoModel.getClientCompanyId())) {
                org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(ipoModel.getClientCompanyId());
                logger.info("client company [{}] checks out - [{}]", cc.getName(), login.getUserId());
                
                if (!cq.checkClientCompanyForShareholders(cc.getName())) {
                    logger.info("client company [{}] checks out. No shareholders found - [{}]", cc.getName(), login.getUserId());
                    
                    org.greenpole.hibernate.entity.InitialPublicOffer ipo_hib = setUpInitialPublicOfferAfterAuthorisation(ipoModel, resp, login);
                    cq.createInitialPublicOffer(ipo_hib);

                    wrapper.setAttendedTo(true);
                    Notification.persistNotificationFile(notificationProp.getNotificationLocation(), notificationCode, wrapper);
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
    public Response createBondOffer_Request(Login login, String authenticator, BondOffer bond) {
        logger.info("request to create bond offer [{}] at [{}] unit price, invoked by - [{}]", 
                bond.getTitle(), bond.getUnitPrice(), login.getUserId());

        Response resp = new Response();
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
                prop = new NotifierProperties(ClientCompanyComponentLogic.class);
                queue = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), prop.getAuthoriserNotifierQueueName());
                List<BondOffer> bc = new ArrayList<>();
                bc.add(bond);
                
                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription("Authenticate Bond Setup - " + bond.getTitle());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(bc);
                
                logger.info("Notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                resp = queue.sendAuthorisationRequest(wrapper);
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
        logger.info("Bond setup creation authorised. [{}] - [{}]", notificationCode, login.getUserId());
        
        String desc = "";
        boolean flag = false;
        
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
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
                org.greenpole.hibernate.entity.BondOffer bondOffer = bondCreationMain(bondModel);
                cq.createBondOffer(bondOffer);
                
                wrapper.setAttendedTo(true);
                Notification.persistNotificationFile(notificationProp.getNotificationLocation(), notificationCode, wrapper);
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
    public Response createPrivatePlacement_Request(Login login, String authenticator, PrivatePlacement privatePlacement) {
        logger.info("request to create private placement, invoked by [{}]", login.getUserId());

        Response resp = new Response();
        NotificationWrapper wrapper;
        QueueSender queue;
        NotifierProperties props;
        
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());

        try {
            boolean exits = cq.checkClientCompany(privatePlacement.getClientCompanyId());
            if (exits) {
                org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(privatePlacement.getClientCompanyId());
                if (cc.isValid()) {
                    if (date.before(formatter.parse(privatePlacement.getClosingDate()))) {
                        if (cq.checkClientCompanyForShareholders(cc.getName())) {
                            if (cq.checkOpenPrivatePlacement(cc.getId())) {
                                wrapper = new NotificationWrapper();
                                props = new NotifierProperties(ClientCompanyComponentLogic.class);
                                queue = new QueueSender(props.getAuthoriserNotifierQueueFactory(), props.getAuthoriserNotifierQueueName());
                                List<PrivatePlacement> ppc = new ArrayList<>();
                                ppc.add(privatePlacement);
                                
                                wrapper.setCode(Notification.createCode(login));
                                wrapper.setDescription("Create Private Placement for " + cc.getName());
                                wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
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
        
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        Date date = new Date();
        
        try { 
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<PrivatePlacement> pplist = (List<PrivatePlacement>) wrapper.getModel();
            PrivatePlacement ppModel = pplist.get(0);
            org.greenpole.hibernate.entity.PrivatePlacement ppEntity = new org.greenpole.hibernate.entity.PrivatePlacement();
            
            if (cq.checkClientCompany(ppModel.getClientCompanyId())) {
                org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(ppModel.getClientCompanyId());
                if (cc.isValid()) {
                    if (date.before(formatter.parse(ppModel.getClosingDate()))) {
                        if (cq.checkClientCompanyForShareholders(cc.getName())) {
                            if (cq.checkOpenPrivatePlacement(cc.getId())) {
                                ppEntity.setClientCompany(cc);
                                ppEntity.setTotalSharesOnOffer(ppModel.getTotalSharesOnOffer());
                                ppEntity.setMethodOnOffer(Integer.parseInt(ppModel.getMethodOfOffer()));
                                ppEntity.setStartingMinSubscrptn(ppModel.getStartingMinimumSubscription());
                                ppEntity.setContinuingMinSubscrptn(ppModel.getContinuingMinimumSubscription());
                                ppEntity.setOfferPrice(ppModel.getOfferPrice());
                                ppEntity.setOfferSize(ppModel.getOfferSize().doubleValue());
                                ppEntity.setOpeningDate(formatter.parse(ppModel.getOpeningDate()));
                                ppEntity.setClosingDate(formatter.parse(ppModel.getClosingDate()));
                                ppEntity.setPlacementClosed(false);
                                
                                cq.createPrivatePlacement(ppEntity);
                                
                                wrapper.setAttendedTo(true);
                                Notification.persistNotificationFile(notificationProp.getNotificationLocation(), notificationCode, wrapper);
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
     * Unwraps bond model and inserts its details into the bond hibernate entity.
     * @param bondModel the bond model
     * @return BondOffer object to setupBondOfferAuthorise method
     */
    private org.greenpole.hibernate.entity.BondOffer bondCreationMain(BondOffer bondModel) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        
        // instantiate required hibernate entities
        org.greenpole.hibernate.entity.BondOffer bond_main = new org.greenpole.hibernate.entity.BondOffer();
        BondOfferPaymentPlan paymentPlan = new BondOfferPaymentPlan();
        BondType type = new BondType();
        
        org.greenpole.hibernate.entity.ClientCompany cc = new org.greenpole.hibernate.entity.ClientCompany();
        
        cc.setId(bondModel.getClientCompanyId());
        type.setId(bondModel.getBondTypeId());
        paymentPlan.setId(bondModel.getPaymentPlanId());
        
        bond_main.setTitle(bondModel.getTitle());
        bond_main.setBondMaturity(formatter.parse(bondModel.getBondMaturity()));
        bond_main.setBondUnitPrice(bondModel.getUnitPrice());
        bond_main.setBondType(type);
        bond_main.setInterestRate(bondModel.getInterestRate());
        bond_main.setPaymentPlan(paymentPlan);
        bond_main.setClientCompany(cc);

        return bond_main;
    }
    
    /**
     * creates the hibernate entity object using the InitialPublicOffer model.
     * @param ipoModel the InitialPublicOffer object
     * @return the hibernate entity object
     */
    private org.greenpole.hibernate.entity.InitialPublicOffer setUpInitialPublicOfferAfterAuthorisation(InitialPublicOffer ipoModel, Response resp, Login login) {
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        org.greenpole.hibernate.entity.InitialPublicOffer ipo_hib = new org.greenpole.hibernate.entity.InitialPublicOffer();
        org.greenpole.hibernate.entity.ClientCompany cc_hib = new org.greenpole.hibernate.entity.ClientCompany();
        
        cc_hib.setId(ipoModel.getClientCompanyId());
        
        ipo_hib.setClientCompany(cc_hib);
        ipo_hib.setTotalSharesOnOffer(ipoModel.getTotalSharesOnOffer());
        ipo_hib.setMethodOfOffer(ipoModel.getMethodOfOffer());
        ipo_hib.setStartingMinSub(ipoModel.getStartingMinimumSubscription());
        ipo_hib.setContMinSub(ipoModel.getContinuingMinimumSubscription());
        ipo_hib.setOfferPrice(ipoModel.getOfferPrice());
        ipo_hib.setOfferSize(ipoModel.getOfferPrice() * ipoModel.getTotalSharesOnOffer());
        try {
            ipo_hib.setOpeningDate(formatter.parse(ipoModel.getOpeningDate()));
        } catch (ParseException ex) {
            logger.info("an error was thrown while inputting the ipo model's opening date. See error log - [{}]", login.getUserId());
            resp.setRetn(208);
            resp.setDesc("Incorrect date format for opening date");
            logger.error("Incorrect date format for opening date [" + login.getUserId() + "]", ex);
        }
        try {
            ipo_hib.setClosingDate(formatter.parse(ipoModel.getClosingDate()));
        } catch (ParseException ex) {
            logger.info("an error was thrown while inputting the ipo model's closing date. See error log - [{}]", login.getUserId());
            resp.setRetn(208);
            resp.setDesc("Incorrect date format for closing date");
            logger.error("Incorrect date format for closing date [" + login.getUserId() + "]", ex);
        }
        return ipo_hib;
    }
    
    /**
     * Unwraps the list share quotation models to create the list hibernate share quotation entities.
     * @param quotationList the list of share quotation models
     * @return the list hibernate share quotation entities
     */
    private List<org.greenpole.hibernate.entity.ShareQuotation> retrieveShareQuotation(List<ShareQuotation> quotationList) {
        List<org.greenpole.hibernate.entity.ShareQuotation> shareQuotations_hib = new ArrayList<>();
        org.greenpole.hibernate.entity.ShareQuotation shareQuotation = new org.greenpole.hibernate.entity.ShareQuotation();
        if (quotationList != null) {//guard against null list, to avoid null pointer exception
            for (ShareQuotation sq : quotationList) {
                String code = sq.getClientCompany().getCode();
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
        org.greenpole.hibernate.entity.ClientCompanyPhoneNumber phone = new org.greenpole.hibernate.entity.ClientCompanyPhoneNumber();
        List<PhoneNumber> phoneList;
        if (ccModel.getPhoneNumbers() != null)
            phoneList = ccModel.getPhoneNumbers();
        else
            phoneList = new ArrayList<>();
        
        
        List<org.greenpole.hibernate.entity.ClientCompanyPhoneNumber> toSend = new ArrayList<>();
        
        for (PhoneNumber ph : phoneList) {
            ClientCompanyPhoneNumberId phoneId = new ClientCompanyPhoneNumberId();
            /*if (!freshCreation) {
                phoneId.setClientCompanyId(ph.getEntityId());
            }*/
            phoneId.setPhoneNumber(ph.getPhoneNumber());
            //put id in phone
            phone.setId(phoneId);
            //set other phone variables
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
        org.greenpole.hibernate.entity.ClientCompanyPhoneNumber phone = new org.greenpole.hibernate.entity.ClientCompanyPhoneNumber();
        List<PhoneNumber> phoneList;
        if (ccModel.getDeletedPhoneNumbers() != null)
            phoneList = ccModel.getDeletedPhoneNumbers();
        else
            phoneList = new ArrayList<>();
        
        
        List<org.greenpole.hibernate.entity.ClientCompanyPhoneNumber> toSend = new ArrayList<>();
        
        for (PhoneNumber ph : phoneList) {
            ClientCompanyPhoneNumberId phoneId = new ClientCompanyPhoneNumberId();
            /*if (!freshCreation) {
                phoneId.setClientCompanyId(ph.getEntityId());
            }*/
            phoneId.setPhoneNumber(ph.getPhoneNumber());
            //put id in phone
            phone.setId(phoneId);
            //set other phone variables
            phone.setIsPrimary(ph.isPrimaryPhoneNumber());
            
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
        org.greenpole.hibernate.entity.ClientCompanyEmailAddress email = new org.greenpole.hibernate.entity.ClientCompanyEmailAddress();
        List<EmailAddress> emailList;
        if (ccModel.getEmailAddresses() != null)
            emailList = ccModel.getEmailAddresses();
        else
            emailList = new ArrayList<>();
        
        List<org.greenpole.hibernate.entity.ClientCompanyEmailAddress> toSend = new ArrayList<>();
        
        for (EmailAddress em : emailList) {
            ClientCompanyEmailAddressId emailId = new ClientCompanyEmailAddressId();
            /*if (!freshCreation) {
                emailId.setClientCompanyId(em.getEntityId());
            }*/
            emailId.setEmailAddress(em.getEmailAddress());
            //put id in email
            email.setId(emailId);
            //set other email variables
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
        org.greenpole.hibernate.entity.ClientCompanyEmailAddress email = new org.greenpole.hibernate.entity.ClientCompanyEmailAddress();
        List<EmailAddress> emailList;
        if (ccModel.getDeletedEmailAddresses() != null)
            emailList = ccModel.getDeletedEmailAddresses();
        else
            emailList = new ArrayList<>();
        
        List<org.greenpole.hibernate.entity.ClientCompanyEmailAddress> toSend = new ArrayList<>();
        
        for (EmailAddress em : emailList) {
            ClientCompanyEmailAddressId emailId = new ClientCompanyEmailAddressId();
            /*if (!freshCreation) {
                emailId.setClientCompanyId(em.getEntityId());
            }*/
            emailId.setEmailAddress(em.getEmailAddress());
            //put id in email
            email.setId(emailId);
            //set other email variables
            email.setIsPrimary(em.isPrimaryEmail());
            
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
        org.greenpole.hibernate.entity.ClientCompanyAddress address = new org.greenpole.hibernate.entity.ClientCompanyAddress();
        List<Address> addressList;
        if (ccModel.getAddresses() != null)
            addressList = ccModel.getAddresses();
        else
            addressList = new ArrayList<>();
        
        List<org.greenpole.hibernate.entity.ClientCompanyAddress> toSend = new ArrayList<>();
        
        for (Address addy : addressList) {
            ClientCompanyAddressId addressId = new ClientCompanyAddressId();
            /*if (!freshCreation) {
                addressId.setClientCompanyId(addy.getEntityId());
            }*/
            addressId.setAddressLine1(addy.getAddressLine1());
            addressId.setState(addy.getState());
            addressId.setCountry(addy.getCountry());
            //put id in address
            address.setId(addressId);
            //set other address variables
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
    private List<org.greenpole.hibernate.entity.ClientCompanyAddress> retrieveAddressModelForDeletion(ClientCompany ccModel/*, boolean freshCreation*/) {
        org.greenpole.hibernate.entity.ClientCompanyAddress address = new org.greenpole.hibernate.entity.ClientCompanyAddress();
        List<Address> addressList;
        if (ccModel.getAddresses() != null) {
            addressList = ccModel.getDeletedAddresses();
        } else {
            addressList = new ArrayList<>();
        }
        
        List<org.greenpole.hibernate.entity.ClientCompanyAddress> toSend = new ArrayList<>();
        
        for (Address addy : addressList) {
            ClientCompanyAddressId addressId = new ClientCompanyAddressId();
            //not required. Hibernate method handles the retreval of the client company id
            //and setting it in the address id. Same goes for email and phone
            /*if (!freshCreation) {
                addressId.setClientCompanyId(addy.getEntityId());
            }*/
            addressId.setAddressLine1(addy.getAddressLine1());
            addressId.setState(addy.getState());
            addressId.setCountry(addy.getCountry());
            //put id in address
            address.setId(addressId);
            //set other address variables
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
     * Unwraps the client company model to create the hibernate client company entity.
     * @param ccModel the client company model (not to be confused with the client company hibernate entity)
     * @param freshCreation if the client company is being created for the first time or undergoing an edit
     * @return the hibernate client company entity object
     */
    private org.greenpole.hibernate.entity.ClientCompany retrieveClientCompanyModel(ClientCompany ccModel, boolean freshCreation) {
        //instantiate required hibernate entities
        org.greenpole.hibernate.entity.ClientCompany cc_main = new org.greenpole.hibernate.entity.ClientCompany();
        Depository depository = new Depository();
        NseSector nseSector = new NseSector();
        
        if (ccModel != null) {//guard against null pointer exception
            //get values from client company model and insert into client company hibernate entity
            if (!freshCreation) {
                cc_main = cq.getClientCompany(ccModel.getId());
            }
            cc_main.setName(ccModel.getName());
            cc_main.setCeo(ccModel.getCeo());
            cc_main.setCode(ccModel.getCode());
            cc_main.setSecretary(ccModel.getSecretary());
            cc_main.setValid(true);
            cc_main.setMerged(false);
            cc_main.setClientCompanyPrimary(true);
            //set depository id in object before setting object in hibernate client company object
            depository.setId(ccModel.getDepositoryId());
            cc_main.setDepository(depository);
            //set nse sector id in object before setting object in hibernate client company object
            nseSector.setId(ccModel.getNseSectorId());
            cc_main.setNseSector(nseSector);
        }
        return cc_main;
    }
}
