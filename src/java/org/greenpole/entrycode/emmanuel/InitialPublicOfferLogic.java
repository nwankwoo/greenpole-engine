/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.model.clientcompany.ClientCompany;
import org.greenpole.entity.model.clientcompany.ShareQuotation;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.entrycode.emmanuel.model.Administrator;
import org.greenpole.entrycode.emmanuel.model.AdministratorEmailAddress;
import org.greenpole.entrycode.emmanuel.model.AdministratorPhoneNumber;
import org.greenpole.entrycode.emmanuel.model.AdministratorResidentialAddress;
import org.greenpole.entrycode.emmanuel.model.Holder;
import org.greenpole.entrycode.emmanuel.model.InitialPublicOffer;
import org.greenpole.hibernate.entity.AdministratorEmailAddressId;
import org.greenpole.hibernate.entity.AdministratorPhoneNumberId;
import org.greenpole.hibernate.entity.AdministratorResidentialAddressId;
//import org.greenpole.hibernate.entity.ShareQuotation;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.Notification;
import org.greenpole.util.properties.NotifierProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author user
 */
public class InitialPublicOfferLogic {

    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();//expecting IPO query and IPO query factory
    private static final Logger logger = LoggerFactory.getLogger(InitialPublicOffer.class);
    private final HibernatDummyQuerInterface hd = HibernateDummyQueryFactory.getHibernateDummyQuery();

    /**
     * Processes request to set up an Initial Public Offer.
     *
     * @param ipo the ClientCompany initial public offer details
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the
     * notification
     * @param cc the client company under which the IPO is to be set
     * @return response to the initial public offer request
     */
    public Response setUpInitialPoblicOffer_request(InitialPublicOffer ipo, Login login, String authenticator, ClientCompany cc) {
        logger.info("request to set up an Initial Public Offer [{}] from [{}]", cc.getName(), login.getUserId());
        Response resp = new Response();
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;
        if (cq.checkClientCompanyForShareholders(cc.getName()) == true) {
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
            return resp;

        }

        resp.setRetn(200);
        resp.setDesc("Client company has no shareholders accounts or certificates so initial public offer cannot be created.");
        return resp;
    }

    /**
     * Processes request to setUpInitialPublicOffer_authorize an Initial public
     * offer that has been saved to a file with the notificationCode
     *
     * @param notificationCode the client company model (not to be confused with
     * the client company hibernate entity)
     * @return resp object
     */
    public Response setUpInitialPublicOffer_authorize(String notificationCode) {
        Response resp = new Response();
        logger.info("Initial Public Offer authorised - [{}]", notificationCode);
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<InitialPublicOffer> ipoList = (List<InitialPublicOffer>) wrapper.getModel();
            InitialPublicOffer ipoModel = ipoList.get(0);
            org.greenpole.hibernate.entity.InitialPublicOffer ipo_hib = setUpInitialPublicOfferAfterAuthorization(ipoModel);
            cq.createInitialPublicOffer(ipo_hib);
            resp.setRetn(0);
            resp.setDesc("Initial Public Offer was Successfully created");
            return resp;
        } catch (Exception ex) {
            logger.info("error loading notification xml file. See error log");
            logger.error("error loading notification xml file to object - ", ex);
            resp.setRetn(200);
            resp.setDesc("Unable to perform initial public offer from authorisation. Contact System Administrator");
        }
        return resp;
    }

    /**
     * creates the hibernate entity object uses the InitialPublicOffer model
     *
     * @param notificationCode the notificationCode that was used to save the
     * data's to a file before authorisation
     * @param ipo_hib the hibernate entity object
     * @param ipoModel the InitialPublicOffer object
     * @return the hibernate entity object
     * @throws JAXBException
     */
    private org.greenpole.hibernate.entity.InitialPublicOffer setUpInitialPublicOfferAfterAuthorization(InitialPublicOffer ipoModel) throws JAXBException {
        org.greenpole.hibernate.entity.InitialPublicOffer ipo_hib = new org.greenpole.hibernate.entity.InitialPublicOffer();
        ipo_hib.setId(ipoModel.getClientCompany().getId());
        ipo_hib.setTotalSharesOnOffer(ipoModel.getTotalSharesOnOffer());
        ipo_hib.setMethodOnOffer(ipoModel.getMethodOnOffer());
        ipo_hib.setStartingMinSub(ipoModel.getStartingMinSub());
        ipo_hib.setContMinSub(ipoModel.getContMinSub());
        ipo_hib.setOfferPrice(ipoModel.getOfferPrice());
        ipo_hib.setOfferSize(ipoModel.getOfferPrice() * ipoModel.getTotalSharesOnOffer());
        ipo_hib.setOpeningDate(ipoModel.getOpeningDate());
        ipo_hib.setClosingDate(ipoModel.getClosingDate());
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

    /**
     * creates the AdministratorEmailAddresses of a particular holder
     *
     * @param adminEmail administrator entity object
     * @return a list of hibernate Administrator's email address entity object
     */
    public List<org.greenpole.hibernate.entity.AdministratorEmailAddress> retrieveEmailAddress(Administrator adminEmail) {
        org.greenpole.hibernate.entity.AdministratorEmailAddress adminOject = new org.greenpole.hibernate.entity.AdministratorEmailAddress();
        List<AdministratorEmailAddress> adminEmail_list = adminEmail.getEmailAddress();
        List<org.greenpole.hibernate.entity.AdministratorEmailAddress> adminEmailHib = new ArrayList();
        for (AdministratorEmailAddress eA : adminEmail_list) {
            AdministratorEmailAddressId emailId = new AdministratorEmailAddressId();
            emailId.setAdministratorId(eA.getAdministratorId());
            emailId.setEmailAddress(eA.getEmailAddress());
            adminOject.setId(emailId);
            adminOject.setIsPrimary(eA.isPrimaryEmail());
            adminEmailHib.add(adminOject);
        }
        return adminEmailHib;
    }

    /**
     * creates the AdministratorPhoneNumber of a particular holder
     *
     * @param adminPhone object of the Administrator entity model
     * @return a list of hibernate Administrator's phone number entity object
     */
    public List<org.greenpole.hibernate.entity.AdministratorPhoneNumber> retrieveAdminPhoneNumber(Administrator adminPhone) {
        org.greenpole.hibernate.entity.AdministratorPhoneNumber adminPhoneObject = new org.greenpole.hibernate.entity.AdministratorPhoneNumber();
        List<AdministratorPhoneNumber> adminPhoneNumberList_model = adminPhone.getPhoneNumber();
        List<org.greenpole.hibernate.entity.AdministratorPhoneNumber> adminPhoneNumberList_hib = new ArrayList();
        for (AdministratorPhoneNumber pn : adminPhoneNumberList_model) {
            AdministratorPhoneNumberId phoneId = new AdministratorPhoneNumberId();
            phoneId.setAdministratorId(pn.getAdministratorId());
            phoneId.setPhoneNumber(pn.getPhoneNumber());
            adminPhoneObject.setId(phoneId);
            adminPhoneObject.setIsPrimary(pn.isPrimaryPhoneNumber());
            adminPhoneNumberList_hib.add(adminPhoneObject);
        }
        return adminPhoneNumberList_hib;
    }

    /**
     * creates the AdministratorResidentialAddress of a particular holder
     *
     * @param admin object of the administrator entity model
     * @return a list of hibernate Administrator's residential address entity
     * object
     */
    public List<org.greenpole.hibernate.entity.AdministratorResidentialAddress> retrieveResidentialAddress(Administrator admin) {
        org.greenpole.hibernate.entity.AdministratorResidentialAddress adminResidentialAddressObject_hib = new org.greenpole.hibernate.entity.AdministratorResidentialAddress();
        List<AdministratorResidentialAddress> adminResidentialAddressList = admin.getResidentialAddress();
        List<org.greenpole.hibernate.entity.AdministratorResidentialAddress> adminResidentialAddsessList_hib = new ArrayList();
        for (AdministratorResidentialAddress ad : adminResidentialAddressList) {
            AdministratorResidentialAddressId adminId = new AdministratorResidentialAddressId();
            adminId.setAdministratorId(ad.getAdministratorId());
            adminId.setAddressLine1(ad.getAddressLine1());
            adminId.setState(ad.getState());
            adminId.setCountry(ad.getCountry());
            adminResidentialAddressObject_hib.setId(adminId);
            adminResidentialAddressObject_hib.setIsPrimary(ad.isPrimaryAddress());
            adminResidentialAddressObject_hib.setAddressLine2(ad.getAddressLine2());
            adminResidentialAddressObject_hib.setAddressLine3(ad.getAddressLine3());
            adminResidentialAddressObject_hib.setAddressLine4(ad.getAddressLine4());
            adminResidentialAddressObject_hib.setCity(ad.getCity());
            adminResidentialAddressObject_hib.setPostCode(ad.getPostCode());
            adminResidentialAddsessList_hib.add(adminResidentialAddressObject_hib);
        }
        return adminResidentialAddsessList_hib;
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
     * sends an authorisation request to a super user for the creation of an
     * administrator
     *
     * @param login used to get the userId that is performing this transaction
     * @param authenticator the super user to accept the creation of this
     * request 
     * @param admin object the administrator entity model
     * @return a list of hibernate Administrator's email address entity object
     */
    public Response createAdministrator_request(Login login, String authenticator, Administrator admin) {
        logger.info("request to create an administrator [{}] for holder [{}]", admin.getFirstName() + " " + admin.getLastName(), login.getUserId());
        Response resp = new Response();
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;
        try {
            wrapper = new NotificationWrapper();
            prop = new NotifierProperties(InitialPublicOfferLogic.class);
            qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                    prop.getAuthoriserNotifierQueueName());
            List<Administrator> adminList = new ArrayList();
            adminList.add(admin);
            wrapper.setCode(Notification.createCode(login));
            wrapper.setDescription("Authenticate creation of administration for" + " " + admin.getFirstName() + " " + admin.getLastName());
            wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
            wrapper.setFrom(login.getUserId());
            wrapper.setTo(authenticator);
            wrapper.setModel(adminList);
            resp = qSender.sendAuthorisationRequest(wrapper);
            resp.setRetn(0);
            resp.setDesc("Successful");
            return resp;
        } catch (Exception e) {
            resp.setRetn(200);
            resp.setDesc("unable to create administrator, please contact the system administrator");
            logger.error("administrator creation failed");
        }
        return resp;
    }
    /**
     * creates administrator for a deceased holder 
     * @param notificationCode
     * @return response object
     */

    /**
     * creates administrator for a deceased holder
     * @param login is used to obtain the login details of the user who performed the transaction
     * @param notificationCode
     * @return response object
     */
    public Response createAdministrator_authorise(Login login, String notificationCode) {
        Response resp = new Response();
        boolean done = false;
        logger.info("Administrator creation authorised - [{}]", notificationCode);
        logger.info("details persisted by user: "+login.getUserId());
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<Administrator> adminList = (List<Administrator>) wrapper.getModel();
            Administrator adminModel = adminList.get(0);
            hd.createAdministratorForShareHolderAndBondHolder(createAdministrator(adminModel), retrieveEmailAddress(adminModel), retrieveAdminPhoneNumber(adminModel), retrieveResidentialAddress(adminModel),retrieveAdministratorHolder(adminModel));
            this.updateAdministratorHolderName(adminModel);
            resp.setRetn(0);
            resp.setDesc("Estate of"+ " "+adminModel.getFirstName());
        } catch (Exception ex) {
            resp.setRetn(0);
            resp.setDesc("Unable to create administrator, please contact the system admin");
        }
        return resp;
    }

    /**
     * Unwraps the administrator entity model to create the administrator
     * hibernate entity model
     *
     * @param admin administrator entity object
     * @return a list of hibernate Administrator's email address entity object
     */
    private org.greenpole.hibernate.entity.Administrator createAdministrator(Administrator admin) {
        org.greenpole.hibernate.entity.Administrator admin_hib = new org.greenpole.hibernate.entity.Administrator();
        admin_hib.setFirstName(admin.getFirstName());
        admin_hib.setMiddleName(admin.getMiddleName());
        admin_hib.setLastName(admin.getLastName());
        return admin_hib;
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
