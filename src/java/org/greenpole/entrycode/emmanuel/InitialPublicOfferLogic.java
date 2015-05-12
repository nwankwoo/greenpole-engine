/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.model.Address;
import org.greenpole.entity.model.AddressTag;
import org.greenpole.entity.model.EmailAddress;
import org.greenpole.entity.model.PhoneNumber;
import org.greenpole.entity.model.clientcompany.ClientCompany;
import org.greenpole.entity.model.clientcompany.ShareQuotation;
import org.greenpole.entity.model.holder.Administrator;
import org.greenpole.entity.model.holder.Holder;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.entrycode.emmanuel.model.InitialPublicOffer;
import org.greenpole.hibernate.entity.AdministratorEmailAddress;
import org.greenpole.hibernate.entity.AdministratorEmailAddressId;
import org.greenpole.hibernate.entity.AdministratorPhoneNumber;
import org.greenpole.hibernate.entity.AdministratorPhoneNumberId;
import org.greenpole.hibernate.entity.AdministratorResidentialAddress;
import org.greenpole.hibernate.entity.AdministratorResidentialAddressId;
//import org.greenpole.hibernate.entity.ShareQuotation;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.HolderComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.Notification;
import org.greenpole.util.properties.GreenpoleProperties;
import org.greenpole.util.properties.NotifierProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author user
 */
public class InitialPublicOfferLogic {
    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();//expecting IPO query and IPO query factory
    private final HolderComponentQuery hq = ComponentQueryFactory.getHolderComponentQuery();
    private static final Logger logger = LoggerFactory.getLogger(InitialPublicOffer.class);
    private final GreenpoleProperties greenProp = new GreenpoleProperties(null);//correct [not necessary to use here, but necessary in client company logic
    private final HibernatDummyQuerInterface hd = HibernateDummyQueryFactory.getHibernateDummyQuery();

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
                    prop = new NotifierProperties(InitialPublicOfferLogic.class);
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
                    resp = qSender.sendAuthorisationRequest(wrapper);

                    resp.setRetn(0);
                    resp.setDesc("Successful");
                    logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("The client company - " + cc.getName() + " - has shareholders. An IPO cannot be setup for it.");
                logger.info("The client company - [{}] - has shareholders. An IPO cannot be setup for it - [{}]",
                        cc.getName(), login.getUserId());
                return resp;
            }
            resp.setRetn(200);//change error code
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
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<InitialPublicOffer> ipoList = (List<InitialPublicOffer>) wrapper.getModel();
            InitialPublicOffer ipoModel = ipoList.get(0);
            
            if (cq.checkClientCompany(ipoModel.getClientCompanyId())) {
                org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(ipoModel.getClientCompanyId());
                logger.info("client company [{}] checks out - [{}]", cc.getName(), login.getUserId());
                
                if (!cq.checkClientCompanyForShareholders(cc.getName())) {
                    logger.info("client company [{}] checks out. No shareholders found - [{}]", cc.getName(), login.getUserId());
                    
                    org.greenpole.hibernate.entity.InitialPublicOffer ipo_hib = setUpInitialPublicOfferAfterAuthorization(ipoModel, resp, login);
                    cq.createInitialPublicOffer(ipo_hib);

                    resp.setRetn(0);
                    resp.setDesc("Success");
                    logger.info("Initial Public Offer was Successfully created - [{}]", login.getUserId());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("The client company - " + cc.getName() + " - has shareholders. An IPO cannot be setup for it.");
                logger.info("The client company - [{}] - has shareholders. An IPO cannot be setup for it - [{}]",
                        cc.getName(), login.getUserId());
                return resp;
            }
            resp.setRetn(200);//change error code
            resp.setDesc("The specified client company does not exist in the database.");
            logger.info("The specified client company does not exist in the database - [{}]", login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            logger.info("error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(98);
            resp.setDesc("Unable to transfer share units from authorisation. Contact System Administrator");
            
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
     * creates the hibernate entity object using the InitialPublicOffer model.
     * @param ipoModel the InitialPublicOffer object
     * @return the hibernate entity object
     */
    private org.greenpole.hibernate.entity.InitialPublicOffer setUpInitialPublicOfferAfterAuthorization(InitialPublicOffer ipoModel, Response resp, Login login) {
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        org.greenpole.hibernate.entity.InitialPublicOffer ipo_hib = new org.greenpole.hibernate.entity.InitialPublicOffer();
        org.greenpole.hibernate.entity.ClientCompany cc_hib = new org.greenpole.hibernate.entity.ClientCompany();
        
        cc_hib.setId(ipoModel.getClientCompanyId());
        
        ipo_hib.setClientCompany(cc_hib);
        ipo_hib.setTotalSharesOnOffer(ipoModel.getTotalSharesOnOffer());
        ipo_hib.setMethodOnOffer(ipoModel.getMethodOnOffer());
        ipo_hib.setStartingMinSub(ipoModel.getStartingMinSub());
        ipo_hib.setContMinSub(ipoModel.getContMinSub());
        ipo_hib.setOfferPrice(ipoModel.getOfferPrice());
        ipo_hib.setOfferSize(ipoModel.getOfferPrice() * ipoModel.getTotalSharesOnOffer());
        try {
            ipo_hib.setOpeningDate(formatter.parse(ipoModel.getOpeningDate()));
        } catch (ParseException ex) {
            logger.info("an error was thrown while inputting the ipo model's opening date. See error log - [{}]", login.getUserId());
            resp.setRetn(308);//change error code
            resp.setDesc("Incorrect date format for opening date");
            logger.error("Incorrect date format for opening date [" + login.getUserId() + "]", ex);
        }
        try {
            ipo_hib.setClosingDate(formatter.parse(ipoModel.getClosingDate()));
        } catch (ParseException ex) {
            logger.info("an error was thrown while inputting the ipo model's closing date. See error log - [{}]", login.getUserId());
            resp.setRetn(308);//change error code
            resp.setDesc("Incorrect date format for closing date");
            logger.error("Incorrect date format for closing date [" + login.getUserId() + "]", ex);
        }
        return ipo_hib;
    }

    /**
     * sends the retrieved share quotations units to the user
     *
     * @return the response object called resp
     */
    public Response getShareUnitQuotations_request() {
        Response resp = new Response();
        List<org.greenpole.hibernate.entity.ShareQuotation> list = getShareUnitQuotations();
        List<ShareQuotation> share = new ArrayList();
        ShareQuotation shareQuotation_model = new ShareQuotation();
        try {
            for (org.greenpole.hibernate.entity.ShareQuotation share_hib : list) {
                shareQuotation_model.setId(share_hib.getClientCompany().getId());
                shareQuotation_model.setUnitPrice(share_hib.getUnitPrice());
                share.add(shareQuotation_model);
            }
            resp.setBody(share);
            resp.setRetn(0);
            resp.setDesc("Share quotations details retrieved");
        } catch (Exception e) {
            resp.setRetn(200);
            resp.setDesc("Unable to retrieve share quotations records");
        }
        return resp;
    }

    /**
     * views the share unit quotations of client companies
     *
     * @return the list of share unit quotations
     */
    public List<org.greenpole.hibernate.entity.ShareQuotation> getShareUnitQuotations() {
        List<org.greenpole.hibernate.entity.ShareQuotation> hib_list = new ArrayList();
        hib_list = cq.retrieveShareUnitQuatationList();
        return hib_list;
    }

    public List<org.greenpole.hibernate.entity.Holder> retrieveAdministratorHolder(Administrator admin){
        org.greenpole.hibernate.entity.Holder holder_hib = new org.greenpole.hibernate.entity.Holder();
        List<org.greenpole.hibernate.entity.Holder> holderList = new ArrayList();
        List<Holder> holderModel = admin.getHolder();
        for(Holder holderObject: holderModel){
        holder_hib.setId(holderObject.getId());
        holderList.add(holder_hib);
        }
   return holderList;
    }
    
    /**
     * Request to create administrators for a holder.
     * @param login used to get the userId that is performing this transaction
     * @param authenticator the super user to accept the creation of this
     * request
     * @param holder the holder to create administrator(s) for
     * @return response to the create administrator request
     */
    public Response createAdministrator_Request(Login login, String authenticator, Holder holder) {
        logger.info("request to create administrator for holder [{}], invoked by [{}]", 
                holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
        Response resp = new Response();
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;
        
        boolean flag = false;
        String desc = "";
        
        try {
            if (hq.checkHolderAccount(holder.getHolderId())) {

                if (holder.getAdministrators() != null && !holder.getAdministrators().isEmpty()) {
                    
                    for (Administrator admin : holder.getAdministrators()) {
                        if (admin.getResidentialAddress() != null) {
                            Address res = admin.getResidentialAddress();
                            if (res.getAddressLine1() == null || "".equals(res.getAddressLine1())) {
                                desc += "\nResidential address line 1 should not be empty";
                            } else if (res.getState() == null || "".equals(res.getState())) {
                                desc += "\nResidential address state should not be empty";
                            } else if (res.getCountry() == null || "".equals(res.getCountry())) {
                                desc += "\nResidential address country should not be empty";
                            }
                        } else if (admin.getPostalAddress() != null) {
                            Address pos = admin.getPostalAddress();
                            if (pos.getAddressLine1() == null || "".equals(pos.getAddressLine1())) {
                                desc += "\nPostal address line 1 should not be empty";
                            } else if (pos.getState() == null || "".equals(pos.getState())) {
                                desc += "\nPostal address state should not be empty";
                            } else if (pos.getCountry() == null || "".equals(pos.getCountry())) {
                                desc += "\nPostal address line 1 should not be empty";
                            }
                        } else if (admin.getEmailAddress() != null && !admin.getEmailAddress().isEmpty()) {
                            for (EmailAddress email : admin.getEmailAddress()) {
                                if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
                                    desc += "\nEmail address should not be empty";
                                }
                            }
                        } else if (admin.getPhoneNumbers() != null && !admin.getPhoneNumbers().isEmpty()) {
                            for (PhoneNumber phone : admin.getPhoneNumbers()) {
                                if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
                                    desc += "\nPhone number should not be empty";
                                }
                            }
                        } else if (admin.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString()) && 
                                admin.getResidentialAddress() == null) {
                            desc += "\nResidential address cannot be empty, as it is the primary address";
                        } else if (admin.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString()) && 
                                admin.getPostalAddress() == null) {
                            desc += "\nPostal address cannot be empty, as it is the primary address";
                        } else {
                            flag = true;
                        }
                    }
                    
                    if (flag) {
                        wrapper = new NotificationWrapper();
                        prop = new NotifierProperties(InitialPublicOfferLogic.class);
                        qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                                prop.getAuthoriserNotifierQueueName());

                        List<Holder> holderList = new ArrayList();
                        holderList.add(holder);

                        wrapper.setCode(Notification.createCode(login));
                        wrapper.setDescription("Authenticate creation of administrator(s) for holder " + holder.getFirstName() + " " + holder.getLastName());
                        wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                        wrapper.setFrom(login.getUserId());
                        wrapper.setTo(authenticator);
                        wrapper.setModel(holderList);

                        resp = qSender.sendAuthorisationRequest(wrapper);
                        resp.setRetn(0);
                        resp.setDesc("Successful");
                        logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                        return resp;
                    }
                    resp.setRetn(300);
                    resp.setDesc("Error: " + desc);
                    logger.info("error detected in administrator creation process - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }
                resp.setRetn(300);//change code
                resp.setDesc("No administrator was sent to be added for the holder.");
                logger.info("No administrator was sent to be added for the holder - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(300);//change code
            resp.setDesc("The holder does not exist.");
            logger.info("The holder does not exist - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error proccessing holder administrator creation. See error log - [{}]", login.getUserId());
            logger.error("error proccessing holder administrator creation - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to proccess holder administrator creation. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     * Processes a saved request to create administrators for a holder.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the create administrator request
     */
    public Response createAdministrator_Authorise(Login login, String notificationCode) {
        logger.info("authorise bond unit transfer, invoked by [{}] - notification code: [{}]", login.getUserId(), notificationCode);
        Response resp = new Response();
        
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<Holder> holderList = (List<Holder>) wrapper.getModel();
            Holder holderModel = holderList.get(0);
            
            boolean flag = false;
            String desc = "";
            
            if (hq.checkHolderAccount(holderModel.getHolderId())) {

                if (holderModel.getAdministrators() != null && !holderModel.getAdministrators().isEmpty()) {
                    
                    for (Administrator admin : holderModel.getAdministrators()) {
                        if (admin.getResidentialAddress() != null) {
                            Address res = admin.getResidentialAddress();
                            if (res.getAddressLine1() == null || "".equals(res.getAddressLine1())) {
                                desc += "\nResidential address line 1 should not be empty";
                            } else if (res.getState() == null || "".equals(res.getState())) {
                                desc += "\nResidential address state should not be empty";
                            } else if (res.getCountry() == null || "".equals(res.getCountry())) {
                                desc += "\nResidential address country should not be empty";
                            }
                        } else if (admin.getPostalAddress() != null) {
                            Address pos = admin.getPostalAddress();
                            if (pos.getAddressLine1() == null || "".equals(pos.getAddressLine1())) {
                                desc += "\nPostal address line 1 should not be empty";
                            } else if (pos.getState() == null || "".equals(pos.getState())) {
                                desc += "\nPostal address state should not be empty";
                            } else if (pos.getCountry() == null || "".equals(pos.getCountry())) {
                                desc += "\nPostal address line 1 should not be empty";
                            }
                        } else if (admin.getEmailAddress() != null && !admin.getEmailAddress().isEmpty()) {
                            for (EmailAddress email : admin.getEmailAddress()) {
                                if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
                                    desc += "\nEmail address should not be empty";
                                }
                            }
                        } else if (admin.getPhoneNumbers() != null && !admin.getPhoneNumbers().isEmpty()) {
                            for (PhoneNumber phone : admin.getPhoneNumbers()) {
                                if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
                                    desc += "\nPhone number should not be empty";
                                }
                            }
                        } else if (admin.getPrimaryAddress().equalsIgnoreCase(AddressTag.residential.toString()) && 
                                admin.getResidentialAddress() == null) {
                            desc += "\nResidential address cannot be empty, as it is the primary address";
                        } else if (admin.getPrimaryAddress().equalsIgnoreCase(AddressTag.postal.toString()) && 
                                admin.getPostalAddress() == null) {
                            desc += "\nPostal address cannot be empty, as it is the primary address";
                        } else {
                            flag = true;
                        }
                    }
             
                    if (flag) {
                        hq.createAdministratorForHolder(createAdministrator(holderModel));

                        resp.setRetn(0);
                        resp.setDesc("Success");
                        logger.info("Administrators were created successfully - [{}]", login.getUserId());
                        return resp;
                    }
                    resp.setRetn(300);
                    resp.setDesc("Error: " + desc);
                    logger.info("error detected in administrator creation process - [{}]: [{}]", login.getUserId(), resp.getRetn());
                    return resp;
                }
                resp.setRetn(300);//change code
                resp.setDesc("No administrator was sent to be added for the holder.");
                logger.info("No administrator was sent to be added for the holder - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(300);//change code
            resp.setDesc("The holder does not exist.");
            logger.info("The holder does not exist - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error creating administrators. See error log - [{}]", login.getUserId());
            logger.error("error creating administrators - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to create administrators. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     * Unwraps the holder model to create the administrator hibernate entity model.
     * @param holder the holder which contains a list of its administrators to be created
     * @return a holder hibernate entity with the administrators to be created
     */
    private org.greenpole.hibernate.entity.Holder createAdministrator(Holder holder) {
        org.greenpole.hibernate.entity.Holder holder_hib;
        org.greenpole.hibernate.entity.Administrator admin_hib = new org.greenpole.hibernate.entity.Administrator();
        
        //get holder entity
        holder_hib = hq.getHolder(holder.getHolderId());
        String nameAddition = "Estate of " + holder_hib.getFirstName();
        holder_hib.setFirstName(nameAddition); //change holder name to begin with "estate of" because holder is now deceased
        
        //get all administrators
        Set admins_hib = new HashSet();
        for (Administrator admin_model : holder.getAdministrators()) {
            //add main administrator details to hibernate entity
            admin_hib.setFirstName(admin_model.getFirstName());
            admin_hib.setLastName(admin_model.getLastName());
            admin_hib.setMiddleName(admin_model.getMiddleName());
            admin_hib.setPryAddress(""); //should not be empty. Correct once corrected from Samsudeen's end.
                        
            //add residential addresses to hibernate entity
            retrieveAdministratorResidentialAddress(admin_model, admin_hib);
            //add postal addresses to hibernate entity
            retrieveAdministratorPostalAddress(admin_model, admin_hib); 
            //add email addresses to hibernate entity
            retrieveAdministratorEmailAddress(admin_model, admin_hib);
            //add phone number to hibernate entity
            retrieveAdministratorPhoneNumber(admin_model, admin_hib);
            //add hibernate administrator into set for administrators
            admins_hib.add(admin_hib);
        }
        //add administrator set into holder
        holder_hib.setAdministrators(admins_hib);
        
        return holder_hib;
    }

    private void retrieveAdministratorPhoneNumber(Administrator admin_model, org.greenpole.hibernate.entity.Administrator admin_hib) {
        //add phone number to hibernate entity
        Set phone_set = new HashSet();
        if (admin_model.getPhoneNumbers() != null && !admin_model.getPhoneNumbers().isEmpty()) {
            for (PhoneNumber admin_phone_model : admin_model.getPhoneNumbers()) {
                AdministratorPhoneNumber admin_phone_hib = new AdministratorPhoneNumber();
                AdministratorPhoneNumberId admin_phone_id_hib = new AdministratorPhoneNumberId();

                admin_phone_id_hib.setPhoneNumber(admin_phone_model.getPhoneNumber());

                admin_phone_hib.setIsPrimary(admin_phone_model.isPrimaryPhoneNumber());

                admin_phone_hib.setId(admin_phone_id_hib);

                phone_set.add(admin_phone_hib);
            }
            admin_hib.setAdministratorPhoneNumbers(phone_set);
        }
    }

    private void retrieveAdministratorEmailAddress(Administrator admin_model, org.greenpole.hibernate.entity.Administrator admin_hib) {
        //add email address to hibernate entity
        Set email_set = new HashSet();
        if (admin_model.getEmailAddress() != null && !admin_model.getEmailAddress().isEmpty()) {
            for (EmailAddress admin_email_model : admin_model.getEmailAddress()) {
                AdministratorEmailAddress admin_email_hib = new AdministratorEmailAddress();
                AdministratorEmailAddressId admin_email_id_hib = new AdministratorEmailAddressId();

                admin_email_id_hib.setEmailAddress(admin_email_model.getEmailAddress());

                admin_email_hib.setIsPrimary(admin_email_model.isPrimaryEmail());

                admin_email_hib.setId(admin_email_id_hib);

                email_set.add(admin_email_hib);
            }
            admin_hib.setAdministratorEmailAddresses(email_set);
        }
    }

    private void retrieveAdministratorPostalAddress(Administrator admin_model, org.greenpole.hibernate.entity.Administrator admin_hib) {
        //create set
        Set pos_set = new HashSet();
        if (admin_model.getPostalAddress() != null) {
            Address admin_pos_model = admin_model.getResidentialAddress();
            
            AdministratorResidentialAddress admin_pos_hib = new AdministratorResidentialAddress();
            AdministratorResidentialAddressId admin_pos_id_hib = new AdministratorResidentialAddressId();
            
            admin_pos_id_hib.setAddressLine1(admin_pos_model.getAddressLine1());
            admin_pos_id_hib.setState(admin_pos_model.getState());
            admin_pos_id_hib.setCountry(admin_pos_model.getCountry());
            
            admin_pos_hib.setAddressLine2(admin_pos_model.getAddressLine2());
            admin_pos_hib.setAddressLine3(admin_pos_model.getAddressLine3());
            admin_pos_hib.setAddressLine4(admin_pos_model.getAddressLine4());
            admin_pos_hib.setCity(admin_pos_model.getCity());
            admin_pos_hib.setPostCode(admin_pos_model.getPostCode());
            admin_pos_hib.setIsPrimary(admin_pos_model.isPrimaryAddress());
            
            admin_pos_hib.setId(admin_pos_id_hib);
            
            pos_set.add(admin_pos_hib); //add residential address to set
        }
        admin_hib.setAdministratorPostalAddresses(pos_set); //add set to administrator hibernate entity
    }

    private void retrieveAdministratorResidentialAddress(Administrator admin_model, org.greenpole.hibernate.entity.Administrator admin_hib) {
        //add residential addresses to hibernate entity
        Set res_set = new HashSet();
        if (admin_model.getResidentialAddress() != null) {
            Address admin_res_model = admin_model.getResidentialAddress();
            
            AdministratorResidentialAddress admin_res_hib = new AdministratorResidentialAddress();
            AdministratorResidentialAddressId admin_res_id_hib = new AdministratorResidentialAddressId();
            
            admin_res_id_hib.setAddressLine1(admin_res_model.getAddressLine1());
            admin_res_id_hib.setState(admin_res_model.getState());
            admin_res_id_hib.setCountry(admin_res_model.getCountry());
            
            admin_res_hib.setAddressLine2(admin_res_model.getAddressLine2());
            admin_res_hib.setAddressLine3(admin_res_model.getAddressLine3());
            admin_res_hib.setAddressLine4(admin_res_model.getAddressLine4());
            admin_res_hib.setCity(admin_res_model.getCity());
            admin_res_hib.setPostCode(admin_res_model.getPostCode());
            admin_res_hib.setIsPrimary(admin_res_model.isPrimaryAddress());
            
            admin_res_hib.setId(admin_res_id_hib);
            
            //create set
            res_set.add(admin_res_hib); //add residential address to set
        }
        admin_hib.setAdministratorResidentialAddresses(res_set); //add set to administrator hibernate entity
    }
    
    

    /**
     * query requires another query that returns the id of the holder updates
     * the holder account name by concatinating the firstName with Estate of
     *
     * @param holder object of the holder entity model
     * @return the holder entity model of the updated firstName
     */
    private void updateAdministratorHolderName(Administrator admin) {
        String replaceName = "Estate of";
        String name;
        String holdReplaceName;
        org.greenpole.hibernate.entity.Holder holderAccount = new org.greenpole.hibernate.entity.Holder();
        //require a method that will get the holder's id
        name = holderAccount.getFirstName();
        holdReplaceName = replaceName+" "+name;
        holderAccount.setFirstName(holdReplaceName);
        hd.updateAdministrationHolderCompanyAccount(holderAccount);
    }   
}
