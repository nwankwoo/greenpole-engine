/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph;

import org.greenpole.entirycode.jeph.model.ProcessedTransaction;
import org.greenpole.entirycode.jeph.model.SuspendedTransaction;
import org.greenpole.entirycode.jeph.model.ProcessedTransactionHolder;
import org.greenpole.entirycode.jeph.model.SuspendedTransactionHolder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.model.Address;
import org.greenpole.entity.model.EmailAddress;
import org.greenpole.entity.model.PhoneNumber;
import org.greenpole.entity.model.taguser.TagUser;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.entity.tags.AddressTag;
import org.greenpole.hibernate.entity.HolderType;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.GeneralComponentQuery;
import org.greenpole.hibernate.query.HolderComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.hibernate.query.impl.HolderComponentQueryImpl;
import org.greenpole.util.properties.GreenpoleProperties;
import org.greenpole.util.properties.NotificationProperties;
import org.greenpole.util.properties.NotifierProperties;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jephthah Sadare
 */
public class TransactionComponentLogic {

    private final HolderComponentQuery hq = new HolderComponentQueryImpl();
    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();
    private final GeneralComponentQuery gq = ComponentQueryFactory.getGeneralComponentQuery();
    private final GreenpoleProperties greenProp = new GreenpoleProperties(TransactionComponentLogic.class);
    NotificationProperties noteProp = new NotificationProperties(TransactionComponentLogic.class);
    private static final Logger logger = LoggerFactory.getLogger(TransactionComponentLogic.class);

    /**
     * Process request to Reconcile Transaction
     * @param login the user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param suspendedTransaction Suspended Transaction object
     * @return response to the Reconcile Transaction request
     */
    public Response reconcileTransaction_Request(Login login, String authenticator, SuspendedTransaction suspendedTransaction) {
        logger.info("Request transaction reconciliation of [{}] share units, invoked by [{}]", suspendedTransaction.getCompanyName(), login.getUserId());
        Response resp = new Response();
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        Notification notification = new Notification();
        Date date = new Date();

        try {
            NotificationWrapper wrapper;
            QueueSender queue;
            NotifierProperties props;

            if (!suspendedTransaction.isReconciled()) {// check if reconciled is false
                if (suspendedTransaction.getCscsTransactionId() > 0) {// check for CSCS transaction id
                    for (SuspendedTransactionHolder suspend : suspendedTransaction.getSuspendedTransactionHolders()) {// loop through holders (buyer and seller)
                        if (!hq.checkHolderAccount(suspend.getHolderChn())) {// if chn is absent the check if holder details are entered
                            Response holderResp = this.validateHolderDetails(login, suspend.getHolder());// validate holder details if holder account does not exist
                            if (holderResp.getRetn() != 0) {
                                resp = holderResp;
                                return resp;
                            }// else create TEMPORARY shareholder account
                            resp.setRetn(200);
                            resp.setDesc("Suspended transaction is not selected for reconciliation");
                            logger.info("Suspended transaction is not selected for reconciliation - [{}]: [{}]", login.getUserId(), resp.getRetn());
                            return resp;
                        }
                        resp.setRetn(200);
                        resp.setDesc("Suspended transaction does not have CHN");
                        logger.info("Suspended transaction does not have CHN - [{}]: [{}]", login.getUserId(), resp.getRetn());
                        return resp;
                    }
                    wrapper = new NotificationWrapper();
                    props = new NotifierProperties(TransactionComponentLogic.class);
                    queue = new QueueSender(props.getAuthoriserNotifierQueueFactory(), props.getAuthoriserNotifierQueueName());

                    List<SuspendedTransaction> suspendedList = new ArrayList<>();
                    suspendedList.add(suspendedTransaction);

                    logger.info("Preparing notification for reconciliation of suspended transaction, invoked by [{}]", login.getUserId());

                    wrapper.setCode(notification.createCode(login));
                    wrapper.setDescription("Authenticate transaction reconciliation request ");
                    wrapper.setMessageTag(NotificationMessageTag.Authorisation_accept.toString());
                    wrapper.setFrom(login.getUserId());
                    wrapper.setTo(authenticator);
                    wrapper.setModel(suspendedList);
                    resp = queue.sendAuthorisationRequest(wrapper);
                    logger.info("Notification forwarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                    resp.setRetn(0);
                    // send SMS and/or Email notification
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("CSCS Transaction Id is not specified");
                logger.info("CSCS Transaction Id is not specified - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Suspended transaction is not selected for reconciliation");
            logger.info("Suspended transaction is not selected for reconciliation - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process transaction reconciliation request. Contact system administrator.");
            logger.info("error processing transaction reconciliation request. See error log - [{}]", login.getUserId());
            logger.error("error processing transaction reconciliation request. - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to Reconcile Transaction that have been saves as a notification
     * file, according to the specified notification code
     * @param login user's login details
     * @param notificationCode the notification code
     * @return return response object to the Reconcile request
     */
    public Response reconcileTransaction_Authorise(Login login, String notificationCode) {
        logger.info("Authorise Reconciliation of suspended transaction, invoked by [{}]", login.getUserId());
        Notification notification = new Notification();
        // SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        Response resp = new Response();

        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(noteProp.getNotificationLocation(), notificationCode);
            List<SuspendedTransaction> suspendedTransactionList = (List<SuspendedTransaction>) wrapper.getModel();
            SuspendedTransaction suspendedTransaction = suspendedTransactionList.get(0);

            if (!suspendedTransaction.isReconciled()) {
                if (suspendedTransaction.getCscsTransactionId() > 0) {
                    for (SuspendedTransactionHolder suspend : suspendedTransaction.getSuspendedTransactionHolders()) {// loop through holders (buyer and seller)
                        if (!hq.checkHolderAccount(suspend.getHolderChn())) {// if chn is absent the check if holder details are entered
                            Response holderResp = this.validateHolderDetails(login, suspend.getHolder());// validate holder details if holder account does not exist
                            if (holderResp.getRetn() != 0) {
                                resp = holderResp;
                                return resp;
                            }
                        }
                    }
                    org.greenpole.hibernate.entity.ProcessedTransaction processedTransEntity = new org.greenpole.hibernate.entity.ProcessedTransaction();
                    org.greenpole.hibernate.entity.ProcessedTransactionHolder processedTransHolderEntity = new org.greenpole.hibernate.entity.ProcessedTransactionHolder();
                    Set processedTransHoldersEntityList = new HashSet();

                    org.greenpole.hibernate.entity.ClientCompany clientComp = new org.greenpole.hibernate.entity.ClientCompany();
                    org.greenpole.hibernate.entity.SuspendedTransaction suspendedTransEntity = new org.greenpole.hibernate.entity.SuspendedTransaction();
                    // org.greenpole.hibernate.entity.SuspendedTransaction suspendedTransEntity = hq.getSuspendedTransaction(suspendedTransaction.getId());
                    // org.greenpole.hibernate.entity.TransactionType transType = new org.greenpole.hibernate.entity.TransactionType();
                    // transType.setId();
                    processedTransEntity.setClientCompany(clientComp);
                    processedTransEntity.setCscsTransactionId(suspendedTransaction.getCscsTransactionId());
                    processedTransEntity.setCompanyName(suspendedTransaction.getCompanyName());

                    for (SuspendedTransactionHolder suspend : suspendedTransaction.getSuspendedTransactionHolders()) {
                        org.greenpole.hibernate.entity.Holder holder = hq.getHolder(suspend.getHolderChn());
                        processedTransHolderEntity.setHolder(holder);
                        processedTransHolderEntity.setHolderName(suspend.getHolderName());
                        processedTransHolderEntity.setHolderChn(suspend.getHolderChn());
                        processedTransHolderEntity.setUnits(suspend.getUnits());
                        processedTransHolderEntity.setUnitType(suspend.getUnitType());
                        processedTransHolderEntity.setFromTo(suspend.getFromTo());

                        processedTransHoldersEntityList.add(processedTransHolderEntity);
                    }
                    processedTransEntity.setProcessedTransactionHolders(processedTransHoldersEntityList);

                    suspendedTransaction.setReconciled(true);
                    suspendedTransEntity.setReconciled(suspendedTransaction.isReconciled());

                    boolean status = false;
                    // status = hq.createUpdateTransactions(processTransEntity, suspendedTransEntity);
                    if (!status) {
                        resp.setRetn(0);
                        resp.setDesc("Authorise reconciliation of suspended transaction failed");
                        logger.info("Authorise reconciliation of suspended transaction failed - [{}]", login.getUserId());
                        // send SMS and/or Email notification
                        return resp;
                    }
                    resp.setRetn(0);
                    resp.setDesc("Authorise reconciliation of suspended transaction successful");
                    logger.info("Authorise reconciliation of suspended transaction successful - [{}]", login.getUserId());
                    wrapper.setAttendedTo(true);
                    notification.markAttended(notificationCode);
                    // send SMS and/or Email notification
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("CSCS Transaction Id is not specified");
                logger.info("CSCS Transaction Id is not specified - [{}]: [{}]", login.getUserId(), resp.getRetn());
                return resp;
            }
            resp.setRetn(200);
            resp.setDesc("Suspended transaction is not selected for reconciliation");
            logger.info("Suspended transaction is not selected for reconciliation - [{}]: [{}]", login.getUserId(), resp.getRetn());
            return resp;
        } catch (JAXBException ex) {
            resp.setRetn(98);
            resp.setDesc("Unable to process reconciliation of suspended transaction. Contact System Administrator");
            logger.info("Error loading notification xml file. See error log - [{}]", login.getUserId());
            logger.error("Error loading notification xml file to object - [" + login.getUserId() + "]", ex);
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to process reconciliation of suspended transaction. Contact system administrator.");
            logger.info("Error processing reconciliation of suspended transaction. See error log - [{}]", login.getUserId());
            logger.error("Error processing reconciliation of suspended transaction - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     * Processes request to view Transaction Report generated on queried transaction
     * @param login the user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param processedTrans Processed Transaction object
     * @return response to the View Transaction Report request
     */
    public Response viewTransactionReport_Request(Login login, String authenticator, ProcessedTransaction processedTrans) {
        logger.info("Request to view transaction report, invoked by [{}]", login.getUserId());
        Response resp = new Response();

        try {
            if (processedTrans.getCscsTransactionId() > 0 || processedTrans.getTransactionTypeId() > 0) {                
                // List<org.greenpole.hibernate.entity.ProcessedTransaction> processTransList = hq.getProcessedTranactions(processedTrans.getCscsTransactionId()); // to be replaced by appropriate query
                List<org.greenpole.hibernate.entity.ProcessedTransaction> processTransList = new ArrayList<>(); // to be replaced by appropriate query
                List<ProcessedTransaction> processedTransModelList = new ArrayList<>();
                TagUser tag = new TagUser();
                for (org.greenpole.hibernate.entity.ProcessedTransaction pt : processTransList) {
                    ProcessedTransaction processedTransModel = new ProcessedTransaction();
                    processedTransModel.setCscsTransactionId(pt.getCscsTransactionId());
                    processedTransModel.setCompanyName(pt.getCompanyName());
                    processedTransModel.setTransactionTypeId(pt.getTransactionType().getId());
                    // processedTransModel.setClientCompany(pt.getClientCompany());

                    List<ProcessedTransactionHolder> processedTransHolderList = new ArrayList<>();
                    List<org.greenpole.hibernate.entity.ProcessedTransactionHolder> processTH = new ArrayList<>(pt.getProcessedTransactionHolders());
                    for (org.greenpole.hibernate.entity.ProcessedTransactionHolder pth : processTH) {
                        ProcessedTransactionHolder processedTransHolderModel = new ProcessedTransactionHolder();
                        processedTransHolderModel.setHolderName(pth.getHolderName());
                        processedTransHolderModel.setHolderChn(pth.getHolderChn());
                        processedTransHolderModel.setUnits(pth.getUnits());
                        processedTransHolderModel.setUnitType(pth.getUnitType());
                        processedTransHolderModel.setFromTo(pth.getFromTo());
                        // processedTransHolderModel.setHolder(pth.getHolder());

                        processedTransHolderList.add(processedTransHolderModel);
                    }
                    processedTransModel.setProcessedTransactionHolders(processedTransHolderList);
                    processedTransModelList.add(processedTransModel);
                }
                List<TagUser> tagList = new ArrayList<>();

                tag.setQueryParam(processedTrans);
                tag.setResult(processedTransModelList);
                tagList.add(tag);

                resp.setBody(tagList);
                resp.setDesc("Transaction Report");
                resp.setRetn(0);
                logger.info("Query successful - [{}]", login.getUserId());
                return resp;
            }
            resp.setRetn(330);
            resp.setDesc("Error: Transaction type ID or CSCS transaction ID not specified");
            logger.info("Error: Transaction type ID or CSCS transaction ID not specified - [{}]", login.getUserId());
            return resp;
        } catch (Exception ex) {
            resp.setRetn(99);
            resp.setDesc("General error. Unable to generate transaction report. Contact system administrator.");
            logger.info("Error generating transaction report. See error log - [{}]", login.getUserId());
            logger.error("Error generating transaction report - [" + login.getUserId() + "]", ex);
            return resp;
        }
    }

    /**
     *
     * @param login
     * @param holder
     * @return
     */
    private Response validateHolderDetails(Login login, org.greenpole.entity.model.holder.Holder holder) {
        Response resp = new Response();

        String desc = "";
        boolean flag = false;

        if (holder.getFirstName() == null || "".equals(holder.getFirstName())) {
            desc = "\nHolder first name should not be empty";
        } else if (holder.getLastName() == null || "".equals(holder.getLastName())) {
            desc += "\nHolder last name should not be empty";
        } else if (holder.getTypeId() <= 0) {
            desc += "\nHolder type should not be empty";
        } else if (holder.getPryAddress() == null || "".equals(holder.getPryAddress())) {
            desc += "\nPrimary Holder address is not specified";
        } else if (holder.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                || holder.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
            desc += "\nPrimary address can only be residential or postal";
        } else if (holder.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                && (holder.getResidentialAddresses() == null || holder.getResidentialAddresses().isEmpty())) {
            desc += "\nResidential address cannot be empty, as it is the primary address";
        } else if (holder.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString())
                && (holder.getPostalAddresses() == null || holder.getPostalAddresses().isEmpty())) {
            desc += "\nPostal address cannot be empty, as it is the primary address";
        } else {
            flag = true;
        }

        if (flag && (!"".equals(holder.getChn()) || holder.getChn() != null)) {
            if (hq.checkHolderAccount(holder.getChn())) {
                desc += "\nThe CHN already exists";
                flag = true;
            }
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

        if (flag && holder.getResidentialAddresses() != null && !holder.getResidentialAddresses().isEmpty()) {
            for (Address addr : holder.getResidentialAddresses()) {
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

        if (flag && holder.getPostalAddresses() != null && !holder.getPostalAddresses().isEmpty()) {
            for (Address addr : holder.getPostalAddresses()) {
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

        if (flag && holder.getEmailAddresses() != null && !holder.getEmailAddresses().isEmpty()) {
            for (EmailAddress email : holder.getEmailAddresses()) {
                if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
                    desc += "\nEmail address should not be empty. Delete email entry if you must";
                    flag = false;
                    break;
                }
            }
        }

        if (flag && holder.getPhoneNumbers() != null && !holder.getPhoneNumbers().isEmpty()) {
            for (PhoneNumber phone : holder.getPhoneNumbers()) {
                if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
                    desc += "\nPhone number should not be empty. Delete phone number entry if you must";
                    flag = false;
                    break;
                }
            }
        }

        if (flag) {
            resp.setRetn(0);
            resp.setDesc("Validation of holder details successful");
            logger.info("Validation of holder details successful - [{}] [{}]", login.getUserId(), resp.getRetn());
            return resp;
        }
        resp.setRetn(1);
        resp.setDesc("Error filing holder details: " + desc);
        logger.info("Error filing holder details: [{}] - [{}]", desc, login.getUserId());
        return resp;
    }

}
