/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.bind.JAXBException;
import org.greenpole.entirycode.jeph.model.QueryStockbroker;
import org.greenpole.entirycode.jeph.model.Stockbroker;
import org.greenpole.entirycode.jeph.model.StockbrokerAddress;
import org.greenpole.entirycode.jeph.model.StockbrokerEmailAddress;
import org.greenpole.entirycode.jeph.model.StockbrokerPhoneNumber;
import org.greenpole.entity.model.Address;
import org.greenpole.entity.model.EmailAddress;
import org.greenpole.entity.model.PhoneNumber;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.hibernate.query.HolderComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
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
 * @author Jephthah Sadare
 */
public class StockbrokerComponentLogic {

    private final HolderComponentQuery hq = ComponentQueryFactory.getHolderComponentQuery();
//    private final StockbrokerComponentQuery cq = ComponentQueryFactory.getStockbrokerQuery();
    private final GreenpoleProperties greenProp = GreenpoleProperties.getInstance();
    private final NotificationProperties notificationProp = NotificationProperties.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(StockbrokerComponentLogic.class);
    SimpleDateFormat formatter = new SimpleDateFormat();

    /**
     * Process request to create Stockbroker
     *
     * @param login the user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param stockerbroker Stockbroker object
     * @return response to the edit stockbroker process
     */
    public Response createStockbroker_Request(Login login, String authenticator, Stockbroker stockerbroker) {
        logger.info("request to create stockbroker company, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();

        try {
            NotificationWrapper wrapper;
            QueueSender queue;
            NotifierProperties prop;

            Response res = validateStockbroker(login, stockerbroker, false);
            if (res.getRetn() != 0) {
                // send SMS and/or Email notification
                logger.info("Stockbroker validation failed " + res.getDesc());
                return res;
            }

            wrapper = new NotificationWrapper();
            prop = NotifierProperties.getInstance();
            queue = new QueueSender(prop.getNotifierQueueFactory(),                                prop.getAuthoriserNotifierQueueName());

            logger.info("client company does not exist - [{}]: [{}]", login.getUserId(), stockerbroker.getName());
            List<Stockbroker> stockbrokerlist = new ArrayList<>();
            stockbrokerlist.add(stockerbroker);
            wrapper.setCode(notification.createCode(login));
            wrapper.setDescription("Authenticate creation of stockbroker, " + stockerbroker.getName());
            wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
            wrapper.setFrom(login.getUserId());
            wrapper.setTo(authenticator);
            wrapper.setModel(stockbrokerlist);
            logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
            resp.setRetn(0);
            // send SMS and/or Email notification
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process client company creation. Contact system administrator.");
            logger.info("error processing client company creation. See error log - [{}]", login.getUserId());
            logger.error("error processing client company creation - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to create stockbroker that has been saved as a
     * notification file, according to the specified notification code
     *
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the create stockbroker process
     */
    public Response createStockbroker_Authorise(Login login, String notificationCode) {
        logger.info("Authorise request to create stockbroker company, invoked by [{}]", login.getUserId());
        Notification notification = new Notification();
        Response resp = new Response();

        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<Stockbroker> stockbrokerList = (List<Stockbroker>) wrapper.getModel();
            Stockbroker stockbroker = stockbrokerList.get(0);

            Response res = validateStockbroker(login, stockbroker, false);
            if (res.getRetn() != 0) {
                // send SMS and/or Email notification
                logger.info("Stockbroker validation failed " + res.getDesc());
                return res;
            }
            org.greenpole.hibernate.entity.Stockbroker stockbrokerEntity = new org.greenpole.hibernate.entity.Stockbroker();
            stockbrokerEntity.setName(stockbroker.getName());
            stockbrokerEntity.setCscsAccNo(stockbroker.getCscsAccNo());
            stockbrokerEntity.setActive(Boolean.TRUE);
            stockbrokerEntity.setValid(Boolean.TRUE);

            boolean status = false;
//            boolean status = cq.createUpdateStockbroker(stockbrokerEntity, retrieveAddressModel(stockbroker);,
//                            retrieveEmailAddressModel(stockbroker), retrievePhoneNumberModel(stockbroker));
            if (status) {
                resp.setRetn(0);
                resp.setDesc("Create stockbroker authorisation process successful");
                logger.info("Create stockbroker process successful. [{}] - [{}]", login.getUserId(), resp.getRetn());
                wrapper.setAttendedTo(true);
                notification.markAttended(notificationCode);
                // send SMS and/or Email notification
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Unable to process create stockbroker authorisation.");
            logger.info("Unable to process create stockbroker authorisation - [{}]", login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            resp.setRetn(98);
            resp.setDesc("Unable to process 'create stockbroker' authorisation request. Contact System Administrator");
            logger.info("Error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("Error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process create stockbroker process. Contact system administrator.");
            logger.info("Error processing creating stockbroker. See error log - [{}]", login.getUserId());
            logger.error("Error processing creating stockbroker - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Process request to edit Stockbroker
     *
     * @param login the user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param stockerbroker Stockbroker object
     * @return response to the edit stockbroker process
     */
    public Response editStockbroker_Request(Login login, String authenticator, Stockbroker stockerbroker) {
        logger.info("Request to edit stockbroker details, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();

        try {
            NotificationWrapper wrapper;
            QueueSender queue;
            NotifierProperties prop;

            Response res = validateStockbroker(login, stockerbroker, true);
            if (res.getRetn() != 0) {
                // send SMS and/or Email notification
                logger.info("Stockbroker validation failed " + res.getDesc());
                return res;
            }

            wrapper = new NotificationWrapper();
            prop = NotifierProperties.getInstance();
            queue = new QueueSender(prop.getNotifierQueueFactory(),                                prop.getAuthoriserNotifierQueueName());

            List<Stockbroker> stockbrokerlist = new ArrayList<>();
            stockbrokerlist.add(stockerbroker);
            wrapper.setCode(notification.createCode(login));
            wrapper.setDescription("Authenticate creation of stockbroker, " + stockerbroker.getName());
            wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
            wrapper.setFrom(login.getUserId());
            wrapper.setTo(authenticator);
            wrapper.setModel(stockbrokerlist);
            logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
            resp.setRetn(0);
            // send SMS and/or Email notification
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process client company creation. Contact system administrator.");
            logger.info("error processing client company creation. See error log - [{}]", login.getUserId());
            logger.error("error processing client company creation - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to edit stockbroker edit that has been saved as a
     * notification file, according to the specified notification code
     *
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the edit stockbroker process
     */
    public Response editStockbroker_Authorise(Login login, String notificationCode) {
        logger.info("Authorise request to edit stockbroker company, invoked by [{}]", login.getUserId());
        Notification notification = new Notification();
        Response resp = new Response();

        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<Stockbroker> stockbrokerList = (List<Stockbroker>) wrapper.getModel();
            Stockbroker stockbroker = stockbrokerList.get(0);

            Response res = validateStockbroker(login, stockbroker, false);
            if (res.getRetn() != 0) {
                // send SMS and/or Email notification
                logger.info("Stockbroker validation failed " + res.getDesc());
                return res;
            }

            org.greenpole.hibernate.entity.Stockbroker stockbrokerEntity = new org.greenpole.hibernate.entity.Stockbroker();
            stockbrokerEntity.setName(stockbroker.getName());
            stockbrokerEntity.setCscsAccNo(stockbroker.getCscsAccNo());
            stockbrokerEntity.setActive(Boolean.TRUE);
            stockbrokerEntity.setValid(Boolean.TRUE);

            boolean status = false;
//            boolean status = cq.createUpdateStockbroker(stockbrokerEntity, retrieveAddressModel(stockbroker);,
//                            retrieveEmailAddressModel(stockbroker), retrievePhoneNumberModel(stockbroker));
            if (status) {
                resp.setRetn(0);
                resp.setDesc("Edit stockbroker authorisation process successful");
                logger.info("Edit stockbroker process successful. [{}] - [{}]", login.getUserId(), resp.getRetn());
                wrapper.setAttendedTo(true);
                notification.markAttended(notificationCode);
                // send SMS and/or Email notification
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Unable to process edit stockbroker authorisation.");
            logger.info("Unable to process edit stockbroker authorisation - [{}]", login.getUserId());
            return resp;
        } catch (JAXBException ex) {
            resp.setRetn(98);
            resp.setDesc("Unable to process 'edit stockbroker details' authorisation request. Contact System Administrator");
            logger.info("error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process edit stockbroker process. Contact system administrator.");
            logger.info("Error processing editing stockbroker details. See error log - [{}]", login.getUserId());
            logger.error("Error processing editing stockbroker details - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Searches for a list of stockbroker according to query parameters.
     *
     * @param login the user's login details
     * @param queryParams the query parameters
     * @return response to the stockbroker query request
     */
    public Response queryStockbroker_Request(Login login, QueryStockbroker queryParams) {
        Response resp = new Response();
        Descriptor descriptorUtil = new Descriptor();
        logger.info("Request to query stockbroker, invoked by [{}]", login.getUserId());

        try {
            if (queryParams.getDescriptor() == null || "".equals(queryParams.getDescriptor())) {
                logger.info("Stockbroker query unsuccessful. Empty descriptor - [{}]", login.getUserId());
                resp.setRetn(200);
                resp.setDesc("Unsuccessful stockbroker query, due to empty descriptor. Contact system administrator");
                return resp;
            }

            Map<String, String> descriptors = descriptorUtil.decipherDescriptor(queryParams.getDescriptor());

            if (descriptors.size() == 4) {
                String descriptor = queryParams.getDescriptor();

                org.greenpole.hibernate.entity.Stockbroker stockbrokerEntity = new org.greenpole.hibernate.entity.Stockbroker();
                StockbrokerAddress stockbrokerAddressSearch = new StockbrokerAddress();
                StockbrokerEmailAddress stockbrokerEmailSearch = new StockbrokerEmailAddress();
                StockbrokerPhoneNumber stockbrokerPhoneNumberSearch = new StockbrokerPhoneNumber();

                Stockbroker stockbrokerModelSearch;
                if (queryParams.getStockbroker() != null) {
                    stockbrokerModelSearch = queryParams.getStockbroker();
                    // use model to set entity
                    stockbrokerEntity.setName(stockbrokerModelSearch.getName());
                }

                if (queryParams.getStockbroker() != null) {
                    Address addressModelSearch;
                    Set addressEntitySet = new HashSet();
                    if (queryParams.getStockbroker().getStockbrokerAddresses() != null && !queryParams.getStockbroker().getStockbrokerAddresses().isEmpty()) {
                        addressModelSearch = queryParams.getStockbroker().getStockbrokerAddresses().get(0);

                        stockbrokerAddressSearch.setAddressLine1(addressModelSearch.getAddressLine1());
                        stockbrokerAddressSearch.setCountry(addressModelSearch.getCountry());
                        stockbrokerAddressSearch.setState(addressModelSearch.getState());
                        stockbrokerAddressSearch.setAddressLine2(addressModelSearch.getAddressLine2());
                        stockbrokerAddressSearch.setAddressLine3(addressModelSearch.getAddressLine3());
                        stockbrokerAddressSearch.setAddressLine4(addressModelSearch.getAddressLine4());
                        stockbrokerAddressSearch.setCity(addressModelSearch.getCity());
                        stockbrokerAddressSearch.setPostCode(addressModelSearch.getPostCode());

                        addressEntitySet.add(stockbrokerAddressSearch); //put address in set
                        stockbrokerEntity.setStockbrokerAddresses(addressEntitySet); //put address set in stockbroker entity
                    }

                    EmailAddress emailModelSearch;
                    Set stockEnityEmailSet = new HashSet();
                    if (queryParams.getStockbroker().getStockbrokerEmailAddresses() != null && !queryParams.getStockbroker().getStockbrokerEmailAddresses().isEmpty()) {
                        emailModelSearch = queryParams.getStockbroker().getStockbrokerEmailAddresses().get(0);
                        stockbrokerEmailSearch.setEmailAddress(emailModelSearch.getEmailAddress());
                        stockEnityEmailSet.add(stockbrokerEmailSearch); //put email in set
                        stockbrokerEntity.setStockbrokerEmailAddresses(stockEnityEmailSet); //put email set in stockbroker entity
                    }

                    PhoneNumber phoneModelSearch;
                    Set stockEntitySet = new HashSet();
                    if (queryParams.getStockbroker().getStockbrokerPhoneNumbers() != null && !queryParams.getStockbroker().getStockbrokerPhoneNumbers().isEmpty()) {
                        phoneModelSearch = queryParams.getStockbroker().getStockbrokerPhoneNumbers().get(0);
                        stockbrokerPhoneNumberSearch.setPhoneNumber(phoneModelSearch.getPhoneNumber());
                        stockEntitySet.add(stockbrokerPhoneNumberSearch); //put phone in set
                        stockbrokerEntity.setStockbrokerPhoneNumbers(stockEntitySet); //put phone set in stockbroker entity
                    }
                }

                Map<String, Double> noOfLodgedCertificatesSearch;
                if (queryParams.getNumberOfLodgedCertificates() != null && !queryParams.getNumberOfLodgedCertificates().isEmpty()) {
                    noOfLodgedCertificatesSearch = queryParams.getNumberOfLodgedCertificates();
                } else {
                    noOfLodgedCertificatesSearch = new HashMap<>();
                }

                Map<String, Integer> noOfShareholders_search;
                if (queryParams.getNumberOfShareholders() != null && !queryParams.getNumberOfShareholders().isEmpty()) {
                    noOfShareholders_search = queryParams.getNumberOfShareholders();
                } else {
                    noOfShareholders_search = new HashMap<>();
                }

                Map<String, Integer> noOfBondholdersSearch;
                if (queryParams.getNumberOfBondholders() != null && !queryParams.getNumberOfBondholders().isEmpty()) {
                    noOfBondholdersSearch = queryParams.getNumberOfBondholders();
                } else {
                    noOfBondholdersSearch = new HashMap<>();
                }

//                List<org.greenpole.hibernate.entity.Stockbroker> stockbrokerSearchResult = cq.queryStockbroker(descriptor, stockbrokerEntity, noOfLodgedCertificatesSearch, noOfShareholders_search, noOfBondholdersSearch);
                List<org.greenpole.hibernate.entity.Stockbroker> stockbrokerSearchResult = new ArrayList<>();
                logger.info("Retrieved stockbroker result from query. Preparing local model - [{}]", login.getUserId());

                //unwrap result and set in client company front-end model
                List<Stockbroker> stockModelOut = new ArrayList<>();

                for (org.greenpole.hibernate.entity.Stockbroker stockEntityOut : stockbrokerSearchResult) {
                    List<Address> stockModelAddress = new ArrayList<>();
                    List<PhoneNumber> stockModelPhone = new ArrayList<>();
                    List<EmailAddress> stockModelEmail = new ArrayList<>();

                    Stockbroker stockbroker = new Stockbroker();

                    stockbroker.setId(stockEntityOut.getId());
                    stockbroker.setName(stockEntityOut.getName());
                    stockbroker.setCscsAccNo(stockEntityOut.getCscsAccNo());
                    stockbroker.setActive(stockEntityOut.getActive());
                    stockbroker.setValid(stockEntityOut.getValid());

                    //get all available addresses, email addresses and phone numbers
//                    List<StockbrokerAddress> adderessEntityList = cq.getStockbrokerAddresses(stockEntityOut.getId());
                    List<StockbrokerAddress> adderessEntityList = new ArrayList<>();
                    for (StockbrokerAddress addressEntityOut : adderessEntityList) {
                        Address addressModelOut = new Address(); //prepare address model to set

                        addressModelOut.setId(addressEntityOut.getId());
                        addressModelOut.setAddressLine1(addressEntityOut.getAddressLine1());
                        addressModelOut.setState(addressEntityOut.getState());
                        addressModelOut.setCountry(addressEntityOut.getCountry());
                        addressModelOut.setAddressLine2(addressEntityOut.getAddressLine2());
                        addressModelOut.setAddressLine3(addressEntityOut.getAddressLine3());
                        addressModelOut.setAddressLine4(addressEntityOut.getAddressLine4());
                        addressModelOut.setCity(addressEntityOut.getCity());
                        addressModelOut.setPostCode(addressEntityOut.getPostCode());
                        addressModelOut.setPrimaryAddress(addressEntityOut.isIsPrimary());
                        addressModelOut.setEntityId(addressEntityOut.getStockbrokerId());

                        stockModelAddress.add(addressModelOut); //set address in list of addresses
                    }
                    stockbroker.setStockbrokerAddresses(stockModelAddress); //set address list in client company

//                    List<StockbrokerEmailAddress> emailEntityList = cq.getStockbrokerEmailAddresses(stockEntityOut.getId());
                    List<StockbrokerEmailAddress> emailEntityList = new ArrayList<>();
                    for (StockbrokerEmailAddress emailEntityOut : emailEntityList) {
                        EmailAddress emailModelOut = new EmailAddress();

                        emailModelOut.setId(emailEntityOut.getId());
                        emailModelOut.setEmailAddress(emailEntityOut.getEmailAddress());
                        emailModelOut.setPrimaryEmail(emailEntityOut.isIsPrimary());
                        emailModelOut.setEntityId(emailEntityOut.getStockbrokerId());

                        stockModelEmail.add(emailModelOut);
                    }
                    stockbroker.setStockbrokerEmailAddresses(stockModelEmail);

//                    List<StockbrokerPhoneNumber> phoneEntityList = cq.getStockbrokerPhoneNumbers(stockEntityOut.getId());
                    List<StockbrokerPhoneNumber> phoneEntityList = new ArrayList<>();
                    for (StockbrokerPhoneNumber phoneEntityOut : phoneEntityList) {
                        PhoneNumber phoneModelOut = new PhoneNumber();

                        phoneModelOut.setId(phoneEntityOut.getId());
                        phoneModelOut.setPhoneNumber(phoneEntityOut.getPhoneNumber());
                        phoneModelOut.setPrimaryPhoneNumber(phoneEntityOut.isIsPrimary());
                        phoneModelOut.setEntityId(phoneEntityOut.getStockbrokerId());

                        stockModelPhone.add(phoneModelOut);
                    }
                    stockbroker.setStockbrokerPhoneNumbers(stockModelPhone);

//                    s.setNoShareholders(cq.getNumberOfShareholders(stockEntityOut.getId()));
//                    s.setNoBondholders(cq.getNumberOfBondholders(stockEntityOut.getId()));
//                    s.setShareUnitPrice(cq.getNumberOfLodgedCertificates(stockEntityOut.getId()));
                    stockModelOut.add(stockbroker); //finally, add client company to list
                }
                logger.info("Stockbroker query successful - [{}]", login.getUserId());
                resp.setRetn(0);
                resp.setDesc("Successful");
                resp.setBody(stockModelOut);
                return resp;
            }
            resp.setRetn(204);
            resp.setDesc("Unsuccessful stockbroker query, due to incomplete descriptor. Contact system administrator");
            logger.info("Unsuccessful stockbroker query, due to incomplete descriptor. Contact system administrator - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to query stockbroker. Contact system administrator.");
            logger.info("Error querying stockbroker. See error log - [{}]", login.getUserId());
            logger.error("Error querying stockbroker - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Validate Stockbroker details
     *
     * @param login the user's login details
     * @param stockbroker Stockbroker object
     * @return response to the validate stockbroker details process
     */
    private Response validateStockbroker(Login login, Stockbroker stockbroker, boolean newEntry) {
        logger.info("validating the stockbroker details, invoked by [{}]", login.getUserId());
        Response resp = new Response();
        String desc = "";
        boolean flag = false;
        boolean status = false;
        // boolean status = cq.checkStockbroker(stockbroker.getName());
        
        // if stockbroker is NEW then it should NOT EXIST in database OR
        // if stockbroker is NOT NEW then it should EXIST in database
        // else error .   .  . . ...
        if ((newEntry && !status) || (!newEntry && status)) {//check if stockbroker does not exists
            if (stockbroker.getName() == null || "".equals(stockbroker.getName())) {
                desc += "\nStockbroker company code should not be empty";
            } else if (stockbroker.getCscsAccNo() == null || "".equals(stockbroker.getCscsAccNo())) {
                desc += "\nStockbroker CSCS account number is already being used by another stockbroker";
            } else {
                flag = true;
            }

            if (flag && stockbroker.getStockbrokerAddresses() != null && !stockbroker.getStockbrokerAddresses().isEmpty()) {
                for (Address addr : stockbroker.getStockbrokerAddresses()) {
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

            if (flag && stockbroker.getStockbrokerEmailAddresses() != null && !stockbroker.getStockbrokerEmailAddresses().isEmpty()) {
                for (EmailAddress email : stockbroker.getStockbrokerEmailAddresses()) {
                    if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
                        desc += "\nEmail address should not be empty. Delete email entry if you must";
                        flag = false;
                        break;
                    }
                }
            }

            if (flag && stockbroker.getStockbrokerPhoneNumbers() != null && !stockbroker.getStockbrokerPhoneNumbers().isEmpty()) {
                for (PhoneNumber phone : stockbroker.getStockbrokerPhoneNumbers()) {
                    if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
                        desc += "\nPhone number should not be empty. Delete phone number entry if you must";
                        flag = false;
                        break;
                    }
                }
            }

            if (flag) {// if all entries are successfully
                resp.setRetn(0);
                resp.setDesc("Stockbroker details saved sucessfully: ");
                logger.info("Stockbroker details saved successfully. - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }// else return desc
            resp.setRetn(200);
            resp.setDesc("Stockbroker details error: " + desc);
            logger.info("Stockbroker details are missing. - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        }
        resp.setRetn(200);
        resp.setDesc("Stockbroker does not exist");
        logger.info("Stockbroker does not exist. - [{}]: [{}]", login.getUserId(), resp.getRetn());
        return resp;
    }

    /**
     * Unwraps the stockbroker model to create the hibernate stockbroker address
     * entity.
     *
     * @param stockbroker stockbroker company model (not to be confused with the
     * stockbroker address hibernate entity)
     * @return a list of hibernate stockbroker address entity object(s)
     */
    private List<org.greenpole.hibernate.entity.StockbrokerAddress> retrieveAddressModel(Stockbroker stockbroker) {
        List<Address> addressList;
        if (stockbroker.getStockbrokerAddresses() != null) {
            addressList = stockbroker.getStockbrokerAddresses();
        } else {
            addressList = new ArrayList<>();
        }
        List<org.greenpole.hibernate.entity.StockbrokerAddress> toSend = new ArrayList<>();
        for (Address addy : addressList) {
            org.greenpole.hibernate.entity.StockbrokerAddress address = new org.greenpole.hibernate.entity.StockbrokerAddress();
            // review section
            if (addy.getId() > 0) {
                // address.setId(addy.getId());
                // address = cq.getStockbroker(addy.getId());
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
     * Unwraps the stockbroker model to create the hibernate stockbroker email
     * address entity.
     *
     * @param stockbroker stockbroker company model (not to be confused with the
     * stockbroker email address hibernate entity)
     * @return a list of hibernate stockbroker email address entity object(s)
     */
    private List<org.greenpole.hibernate.entity.StockbrokerEmailAddress> retrieveEmailAddressModel(Stockbroker stockbroker) {
        List<EmailAddress> emailList;
        if (stockbroker.getStockbrokerEmailAddresses() != null) {
            emailList = stockbroker.getStockbrokerEmailAddresses();
        } else {
            emailList = new ArrayList<>();
        }
        List<org.greenpole.hibernate.entity.StockbrokerEmailAddress> toSend = new ArrayList<>();
        for (EmailAddress email : emailList) {
            org.greenpole.hibernate.entity.StockbrokerEmailAddress emailAddress = new org.greenpole.hibernate.entity.StockbrokerEmailAddress();
            // review section
            if (email.getId() > 0) {
                // emailAddress.setId(email.getId());
                // emailAddress = cq.getStockbrokerEmailAddress(email.getId());
            }
            emailAddress.setEmailAddress(email.getEmailAddress());
            emailAddress.setIsPrimary(email.isPrimaryEmail());
            //add the address to list
            toSend.add(emailAddress);
        }
        return toSend;
    }

    /**
     * Unwraps the stockbroker model to create the hibernate stockbroker phone
     * number entity.
     *
     * @param stockbroker stockbroker company model (not to be confused with the
     * stockbroker phone number hibernate entity)
     * @return a list of hibernate stockbroker phone number entity object(s)
     */
    private List<org.greenpole.hibernate.entity.StockbrokerPhoneNumber> retrievePhoneNumberModel(Stockbroker stockbroker) {
        List<PhoneNumber> phoneList;
        if (stockbroker.getStockbrokerPhoneNumbers() != null) {
            phoneList = stockbroker.getStockbrokerPhoneNumbers();
        } else {
            phoneList = new ArrayList<>();
        }
        List<org.greenpole.hibernate.entity.StockbrokerPhoneNumber> toSend = new ArrayList<>();
        for (PhoneNumber ph : phoneList) {
            org.greenpole.hibernate.entity.StockbrokerPhoneNumber phone = new org.greenpole.hibernate.entity.StockbrokerPhoneNumber();
            // review section
            if (ph.getId() > 0) {
                //phone.setId(ph.getId());
                // phone = cq.getStockbrokerPhoneNumber(ph.getId());
            }
            phone.setPhoneNumber(ph.getPhoneNumber());
            phone.setIsPrimary(ph.isPrimaryPhoneNumber());
            //add the address to list
            toSend.add(phone);
        }
        return toSend;
    }
}
