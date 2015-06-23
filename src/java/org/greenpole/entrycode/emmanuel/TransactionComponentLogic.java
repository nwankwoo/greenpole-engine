/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

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
import org.greenpole.entity.model.clientcompany.UnitTransfer;
import org.greenpole.entity.model.holder.Holder;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.entity.tags.AddressTag;
import org.greenpole.entrycode.emmanuel.model.ProcessedTransaction;
import org.greenpole.entrycode.emmanuel.model.ProcessedTransactionHolder;
import org.greenpole.entrycode.emmanuel.model.QueryTransaction;
import org.greenpole.hibernate.entity.HolderCompanyAccountId;
import org.greenpole.hibernate.entity.HolderEmailAddress;
import org.greenpole.hibernate.entity.HolderPhoneNumber;
import org.greenpole.hibernate.entity.HolderPostalAddress;
import org.greenpole.hibernate.entity.HolderResidentialAddress;
import org.greenpole.hibernate.entity.HolderType;
import org.greenpole.hibernate.entity.ProcessedTransactionHolderId;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.GeneralComponentQuery;
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
 * @author user
 */
public class TransactionComponentLogic {

    private static final Logger logger = LoggerFactory.getLogger(HolderComponentLogic.class);
    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();
    private final GeneralComponentQuery gq = ComponentQueryFactory.getGeneralComponentQuery();
    private final GreenpoleProperties greenProp = GreenpoleProperties.getInstance();
    private final NotificationProperties notificationProp = NotificationProperties.getInstance();
    private final HibernatDummyQuerInterface hd = HibernateDummyQueryFactory.getHibernateDummyQuery();//not needed
    private final HolderComponentQuery hq = ComponentQueryFactory.getHolderComponentQuery();
/**
 * processes request to upload transfer of share units from transaction file
 * @param login the user details
 * @param authenticator the user meant to receive this notification
 * @param transferList list of transfer records from uploaded file
 * @param holder the details of the buyer and the seller
 * @return response to the upload transaction request
 */
    public Response uploadTransaction_Request(Login login, String authenticator, List<UnitTransfer> transferList, List<Holder> holder) {
        Response resp = new Response();
        logger.info("request to upload transaction files invoked by [{}]", login.getUserId());
        Notification notification = new Notification();
        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;
            int counter;
            boolean checkCSCSId = false;
            boolean sellerHolderExists = false;
            boolean buyerHolderExists = false;
            boolean checkSellerCompAcct = false;
            boolean checkBuyerCompAcct = false;
            org.greenpole.hibernate.entity.HolderCompanyAccount sellerCompAcct = new org.greenpole.hibernate.entity.HolderCompanyAccount();
            org.greenpole.hibernate.entity.HolderCompanyAccount buyerCompAcct = new org.greenpole.hibernate.entity.HolderCompanyAccount();
            for (counter = 0; counter < transferList.size(); counter++) {
                sellerHolderExists = hq.checkHolderAccount(transferList.get(counter).getChnFrom());
                buyerHolderExists = hq.checkHolderAccount(transferList.get(counter).getChnTo());
                checkSellerCompAcct = hq.checkHolderCompanyAccount(transferList.get(counter).getHolderIdFrom(), transferList.get(counter).getClientCompanyId());
                checkBuyerCompAcct = hq.checkHolderCompanyAccount(transferList.get(counter).getHolderIdTo(), transferList.get(counter).getClientCompanyId());
                checkCSCSId = hd.checkCSCSTransactionExistence(transferList.get(counter).getCscsTransactionId());//this query returns true if cscsId is not found
                if (checkCSCSId) {
                    logger.info("CSCS reference number does not exists - [{}]", login.getUserId());
                    if (sellerHolderExists) {//checks if seller exists
                        logger.info("seller holder exists - [{}]", login.getUserId());
                        org.greenpole.hibernate.entity.Holder sellerHolder = hq.getHolder(transferList.get(counter).getChnFrom());
                        String sellerName = sellerHolder.getFirstName() + " " + sellerHolder.getLastName();
                        if (buyerHolderExists) {//checks if buyer exists
                            org.greenpole.hibernate.entity.Holder buyerHolder = hq.getHolder(transferList.get(counter).getChnTo());
                            String buyerName = buyerHolder.getFirstName() + " " + buyerHolder.getLastName();
                            if (checkSellerCompAcct) {
                                org.greenpole.hibernate.entity.HolderCompanyAccount sellerHCA = hq.getHolderCompanyAccount(transferList.get(counter).getHolderIdFrom(), transferList.get(counter).getClientCompanyId());
                                if (checkBuyerCompAcct) {
                                    org.greenpole.hibernate.entity.HolderCompanyAccount buyerHCA = hq.getHolderCompanyAccount(transferList.get(counter).getHolderIdTo(), transferList.get(counter).getClientCompanyId());
                                    if (!transferList.get(counter).getChnFrom().equals(transferList.get(counter).getChnTo()) ) {//check that the seller is not the buyer
                                        if (sellerHCA.getShareUnits() >= transferList.get(counter).getUnits()) {
                                            if (!sellerHCA.getEsop()) {
                                                if (!buyerHCA.getEsop()) {
                                                    wrapper = new NotificationWrapper();
                                                    prop = NotifierProperties.getInstance();
                                                    qSender = new QueueSender(prop.getNotifierQueueFactory(),
                                                            prop.getAuthoriserNotifierQueueName());
                                                    wrapper.setCode(notification.createCode(login));
                                                    wrapper.setDescription("Upload of transaction files ");
                                                    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                                                    wrapper.setFrom(login.getUserId());
                                                    wrapper.setTo(authenticator);
                                                    List list = new ArrayList();
                                                    list.add(0, transferList);
                                                    list.add(1, holder);
                                                    wrapper.setModel(list);
                                                    logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                                                    resp = qSender.sendAuthorisationRequest(wrapper);
                                                    return resp;
                                                }
                                                resp.setRetn(200);
                                                resp.setDesc("Buyer Shareholder account has Esop");
                                                logger.info("Buyer Shareholder account has Esop ", login.getUserId());
                                                return resp;
                                            }
                                            resp.setRetn(200);
                                            resp.setDesc("Seller Shareholder account has Esop");
                                            logger.info("Seller Shareholder account has Esop ", login.getUserId());
                                            return resp;
                                        }
                                        resp.setRetn(200);
                                        resp.setDesc("Seller has less share units to sell");
                                        logger.info("Seller has less share units to sell ", login.getUserId());
                                        return resp;
                                    }
                                    resp.setRetn(200);
                                    resp.setDesc("Seller cannot be the buyer");
                                    logger.info("Seller cannot be the buyer ", login.getUserId());
                                    return resp;
                                }
                                resp.setRetn(200);//else is to create an account for the holder
                                resp.setDesc("Buyer has no account");
                                logger.info("Buyer has no account ", login.getUserId());
                                return resp;
                            }
                            resp.setRetn(200);
                            resp.setDesc("Seller has no company account");
                            logger.info("Seller has no company account ", login.getUserId());
                            return resp;

                        }
                        resp.setRetn(200);
                        resp.setDesc("Buyer does not exists");//else will be to create the buyer
                        logger.info("Buyer does not exists ", login.getUserId());
                        return resp;
                    }
                    resp.setRetn(200);//else is to create both holder acct and company accounts
                    resp.setDesc("Seller does not exists ");
                    logger.info("Seller does not exists ", login.getUserId());
                    return resp;
                }
                resp.setRetn(200);
                resp.setDesc("This transaction exists already");
                logger.info("This transaction exists already ", login.getUserId());
                return resp;
            }
        } catch (Exception ex) {
            logger.info("error processing upload transaction files request. See error log - [{}]", login.getUserId());
            logger.error("error processing upload transaction files request - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to process upload of transaction records request. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
        return resp;
    }

    /**
     * Processes saved request to upload transaction files.
     *
     * @param login the user details
     * @param notificationCode the notification code
     * @return response to the authorisation request
     */
    public Response uploadTransaction_Authorisation(Login login, String notificationCode) {
        Response resp = new Response();
        Notification notification = new Notification();
        logger.info("request to upload transaction files invoked by [{}]", login.getUserId());
        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<UnitTransfer> unitTransferList = (List<UnitTransfer>) wrapper.getModel().get(0);
            UnitTransfer unitTransfer_model = unitTransferList.get(0);
            List<Holder> holderList = (List<Holder>) wrapper.getModel().get(1);

            List<ProcessedTransactionHolder> ptholderList = new ArrayList<>();
            ProcessedTransactionHolder pth = new ProcessedTransactionHolder();
            List<ProcessedTransaction> ptList = new ArrayList();
            ProcessedTransaction pt = new ProcessedTransaction();
            org.greenpole.hibernate.entity.ProcessedTransaction pt_hib = new org.greenpole.hibernate.entity.ProcessedTransaction();
            org.greenpole.hibernate.entity.ProcessedTransactionHolder pth_hib = new org.greenpole.hibernate.entity.ProcessedTransactionHolder();
            org.greenpole.hibernate.entity.Holder holder_hib = new org.greenpole.hibernate.entity.Holder();
            org.greenpole.hibernate.entity.ClientCompany cc = new org.greenpole.hibernate.entity.ClientCompany();
            Set processedTransactionHolderSet = new HashSet();
            long millis = System.currentTimeMillis();
            Date current_date = new java.sql.Date(millis);
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            int counter;
            boolean checkCSCSId = false;
            boolean sellerHolderExists = false;
            boolean buyerHolderExists = false;
            boolean checkSellerCompAcct = false;
            boolean checkBuyerCompAcct = false;
            org.greenpole.hibernate.entity.HolderCompanyAccount sellerCompAcct = new org.greenpole.hibernate.entity.HolderCompanyAccount();
            org.greenpole.hibernate.entity.HolderCompanyAccount buyerCompAcct = new org.greenpole.hibernate.entity.HolderCompanyAccount();
            for (counter = 0; counter < unitTransferList.size(); counter++) {
                sellerHolderExists = hq.checkHolderAccount(unitTransferList.get(counter).getChnFrom());
                buyerHolderExists = hq.checkHolderAccount(unitTransferList.get(counter).getChnTo());
                checkCSCSId = hd.checkCSCSTransactionExistence(unitTransferList.get(counter).getCscsTransactionId());//this query returns true if cscsId is not found
                if (checkCSCSId) {
                    logger.info("CSCS reference number does not exists - [{}]", login.getUserId());
                    if (sellerHolderExists) {//checks if seller exists
                        logger.info("seller holder exists - [{}]", login.getUserId());
                        org.greenpole.hibernate.entity.Holder sellerHolder = hq.getHolder(unitTransferList.get(counter).getChnFrom());
                        String sellerName = sellerHolder.getFirstName() + " " + sellerHolder.getLastName();
                        if (buyerHolderExists) {//checks if buyer exists
                            org.greenpole.hibernate.entity.Holder buyerHolder = hq.getHolder(unitTransferList.get(counter).getChnTo());
                            String buyerName = buyerHolder.getFirstName() + " " + buyerHolder.getLastName();
                            checkSellerCompAcct = hq.checkHolderCompanyAccount(unitTransferList.get(counter).getHolderIdFrom(), unitTransferList.get(counter).getClientCompanyId());
                            if (checkSellerCompAcct) {
                                logger.info("seller holder has company account - [{}]", login.getUserId());
                                boolean buyerHolderChnExists = buyerHolder.getChn() != null && !"".equals(buyerHolder.getChn());
                                checkBuyerCompAcct = hq.checkHolderCompanyAccount(unitTransferList.get(counter).getHolderIdTo(), unitTransferList.get(counter).getClientCompanyId());
                                org.greenpole.hibernate.entity.HolderCompanyAccount sellerHCA = hq.getHolderCompanyAccount(unitTransferList.get(counter).getHolderIdFrom(), unitTransferList.get(counter).getClientCompanyId());
                                if (checkBuyerCompAcct) {
                                    logger.info("buyer holder has company account - [{}]", login.getUserId());
                                    org.greenpole.hibernate.entity.HolderCompanyAccount buyerHCA = hq.getHolderCompanyAccount(unitTransferList.get(counter).getHolderIdTo(), unitTransferList.get(counter).getClientCompanyId());
                                    if (!sellerHCA.getEsop() && !buyerHCA.getEsop()) {
                                        UnitTransfer unit = unitTransferList.get(counter);
                                        org.greenpole.hibernate.entity.TransactionType tt = hd.getTransactionType(unitTransferList.get(counter).getTransferTypeId());
                                        cc = cq.getClientCompany(unitTransferList.get(counter).getClientCompanyId());
                                        pt_hib.setClientCompany(cc);
                                        pt_hib.setCompanyName(unitTransferList.get(counter).getClientCompanyName());
                                        pt_hib.setCscsTransactionId(unitTransferList.get(counter).getCscsTransactionId());
                                        pt_hib.setTransactionType(tt);
                                        for (ProcessedTransactionHolder pth_holder : pt.getProcessedTransactionHolder()) {
                                            org.greenpole.hibernate.entity.Holder h_hib = hq.getHolder(pth_holder.getHolderChn());
                                            pth_hib.setHolder(h_hib);
                                            pth_hib.setHolderName(pth_holder.getHolderName());
                                            pth_hib.setHolderChn(pth_holder.getHolderChn());
                                            pth_hib.setFromTo(pth_holder.getFromTo());
                                            pth_hib.setUnitType(pth_holder.getUnitType());
                                            pth_hib.setUnits(pth_holder.getUnits());
                                            processedTransactionHolderSet.add(pth_hib);
                                        }
                                        pt_hib.setProcessedTransactionHolders(processedTransactionHolderSet);
                                        return invokeTransfer(sellerHCA, unit, buyerHCA, resp, sellerName, buyerName, login,
                                                notificationCode, notification);
                                    } else {
                                        org.greenpole.hibernate.entity.SuspendedTransaction suspendTrans = new org.greenpole.hibernate.entity.SuspendedTransaction();
                                        org.greenpole.hibernate.entity.SuspendedTransactionHolder suspendTranHolder = new org.greenpole.hibernate.entity.SuspendedTransactionHolder();
                                        org.greenpole.hibernate.entity.SuspendedTransactionHolderId suspendTranHolderId = new org.greenpole.hibernate.entity.SuspendedTransactionHolderId();
                                        if (sellerHCA.getEsop()) {
                                            suspendTrans.setClientCompany(cc);
                                            suspendTrans.setCompanyName(unitTransferList.get(counter).getClientCompanyName());
                                            suspendTrans.setCscsTransactionId(unitTransferList.get(counter).getCscsTransactionId());
                                            suspendTrans.setReconciled(false);
                                            suspendTrans.setSuspensionDate(current_date);
                                            suspendTrans.setSuspensionReason("Seller Holder account has Esop");
                                            suspendTrans.setTransactionDate(current_date);
                                            suspendTranHolderId.setHolderId(unitTransferList.get(counter).getHolderIdFrom());
                                                                    //suspendTranHolder.setFromTo(pth_holder.);
                                            //suspendTranHolder.setHolderChn((String)unitTransferList.get(counter).getChnFrom());
                                        }
                                        if (buyerHCA.getEsop()) {
                                            suspendTrans.setClientCompany(cc);
                                            suspendTrans.setCompanyName(unitTransferList.get(counter).getClientCompanyName());
                                            suspendTrans.setCscsTransactionId(unitTransferList.get(counter).getCscsTransactionId());
                                            suspendTrans.setReconciled(false);
                                            suspendTrans.setSuspensionDate(current_date);
                                            suspendTrans.setSuspensionReason("Buyer Holder account has Esop");
                                            suspendTrans.setTransactionDate(current_date);
                                            suspendTranHolderId.setHolderId(unitTransferList.get(counter).getHolderIdTo());
                                        }
                                    }
                                    // }
                                } else if (!checkBuyerCompAcct) {
                                    org.greenpole.hibernate.entity.HolderCompanyAccount newHolderCompAcct = new org.greenpole.hibernate.entity.HolderCompanyAccount();
                                    org.greenpole.hibernate.entity.HolderCompanyAccountId buyerCompAccId_hib = new org.greenpole.hibernate.entity.HolderCompanyAccountId(unitTransferList.get(counter).getHolderIdTo(), unitTransferList.get(counter).getClientCompanyId());
                                    //buyerCompAccId_hib = unitTransferList.get(counter).getChnTo();
                                    newHolderCompAcct.setId(buyerCompAccId_hib);
                                    newHolderCompAcct.setClientCompany(cc);
                                    newHolderCompAcct.setEsop(false);
                                    newHolderCompAcct.setHolder(holder_hib);
                                    newHolderCompAcct.setHolderCompAccPrimary(true);
                                    newHolderCompAcct.setMerged(false);
                                    newHolderCompAcct.setShareUnits(unitTransferList.get(counter).getUnits());
                                    hd.createNubanAccount(newHolderCompAcct);
                                    logger.info("buyer holder now has company account - [{}]", login.getUserId());
                                    UnitTransfer unit = unitTransferList.get(counter);
                                    return invokeTransfer(sellerHCA, unit, newHolderCompAcct, resp, sellerName, buyerName, login,
                                            notificationCode, notification);
                                }
                            } else {
                                org.greenpole.hibernate.entity.HolderCompanyAccount sellerHolderCompAcct = new org.greenpole.hibernate.entity.HolderCompanyAccount();
                                org.greenpole.hibernate.entity.HolderCompanyAccountId sellerCompAccId_hib = new org.greenpole.hibernate.entity.HolderCompanyAccountId(unitTransferList.get(counter).getHolderIdFrom(), unitTransferList.get(counter).getClientCompanyId());
                                sellerHolderCompAcct.setId(sellerCompAccId_hib);
                                sellerHolderCompAcct.setClientCompany(cc);
                                sellerHolderCompAcct.setEsop(false);
                                sellerHolderCompAcct.setHolder(holder_hib);
                                sellerHolderCompAcct.setHolderCompAccPrimary(true);
                                sellerHolderCompAcct.setMerged(false);
                                sellerHolderCompAcct.setNubanAccount(buyerName);//put the correct info
                                sellerHolderCompAcct.setShareUnits(0); //needs to know the actual share units left from the seller account
                                hd.createNubanAccount(sellerHolderCompAcct);
                            }
                        } else {//creating a new holder
                            String desc = "";
                            boolean flag = false;

                            for (Holder h : holderList) {
                                org.greenpole.hibernate.entity.Holder h_hib = new org.greenpole.hibernate.entity.Holder();
                                if (h.getFirstName() == null || "".equals(h.getFirstName())) {
                                    desc = "\nHolder first name should not be empty";
                                } else if (h.getLastName() == null || "".equals(h.getLastName())) {
                                    desc += "\nHolder last name should not be empty";
                                } else if (h.getTypeId() <= 0) {
                                    desc += "\nHolder type should not be empty";
                                } else if (h.getPryAddress() == null || "".equals(h.getPryAddress())) {
                                    desc += "\nPrimary Holder address is not specified";
                                } else if (!h.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                                        && !h.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
                                    desc += "\nPrimary address can only be residential or postal";
                                } else if (h.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                                        && (h.getResidentialAddresses() == null || h.getResidentialAddresses().isEmpty())) {
                                    desc += "\nResidential address cannot be empty, as it is the primary address";
                                } else if (h.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString())
                                        && (h.getPostalAddresses() == null || h.getPostalAddresses().isEmpty())) {
                                    desc += "\nPostal address cannot be empty, as it is the primary address";
                                } else {
                                    flag = true;
                                }
                                if (flag && h.getTypeId() > 0) {
                                    boolean found = false;
                                    for (HolderType ht : hq.getAllHolderTypes()) {
                                        if (h.getTypeId() == ht.getId()) {
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (!found) {
                                        desc += "\nHolder type is not valid";
                                        flag = false;
                                    }
                                }

                                if (flag && h.getResidentialAddresses() != null && !h.getResidentialAddresses().isEmpty()) {
                                    for (Address addr : h.getResidentialAddresses()) {
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

                                if (flag && h.getPostalAddresses() != null && !h.getPostalAddresses().isEmpty()) {
                                    for (Address addr : h.getPostalAddresses()) {
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

                                if (flag && h.getEmailAddresses() != null && !h.getEmailAddresses().isEmpty()) {
                                    for (EmailAddress email : h.getEmailAddresses()) {
                                        if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
                                            desc += "\nEmail address should not be empty. Delete email entry if you must";
                                            flag = false;
                                            break;
                                        }
                                    }
                                }

                                if (flag && h.getPhoneNumbers() != null && !h.getPhoneNumbers().isEmpty()) {
                                    for (PhoneNumber phone : h.getPhoneNumbers()) {
                                        if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
                                            desc += "\nPhone number should not be empty. Delete phone number entry if you must";
                                            flag = false;
                                            break;
                                        }
                                    }
                                }

                                if (flag) {
                                    org.greenpole.hibernate.entity.Holder holdEntity = new org.greenpole.hibernate.entity.Holder();
                                    HolderType typeEntity = hq.getHolderType(h.getTypeId());

                                    holdEntity.setFirstName(h.getFirstName());
                                    holdEntity.setLastName(h.getLastName());
                                    holdEntity.setMiddleName(h.getMiddleName());
                                    holdEntity.setHolderType(typeEntity);
                                    holdEntity.setGender(h.getGender());
                                    holdEntity.setDob(formatter.parse(h.getDob()));
                                    holdEntity.setPryHolder(true);
                                    holdEntity.setMerged(false);

                                    boolean created;

                                    if (h.getChn() == null || "".equals(h.getChn())) {
                                        //there are two versions of createHolderAccount, one for bond account and the other for company account
                                        //thus, null cannot be directly passed into the method, as java will not be able to distinguish between
                                        //both createHolderAccount methods.
                                        org.greenpole.hibernate.entity.HolderCompanyAccount emptyAcct = null;
                                        created = hq.createHolderAccount(holdEntity, emptyAcct,
                                                retrieveHolderResidentialAddress(h), retrieveHolderPostalAddress(h),
                                                retrieveHolderEmailAddress(h), retrieveHolderPhoneNumber(h));
                                    } else {
                                        holdEntity.setChn(h.getChn());//set chn
                                        created = hq.createHolderAccount(holdEntity, retrieveHolderCompanyAccount(h),
                                                retrieveHolderResidentialAddress(h), retrieveHolderPostalAddress(h),
                                                retrieveHolderEmailAddress(h), retrieveHolderPhoneNumber(h));
                                    }
                                }
                                resp.setRetn(200);
                                resp.setDesc("CHN of the new holder to be created cannot be empty");
                                logger.info("CHN of the new holder to be created cannot be empty ", login.getUserId());
                                return resp;

                            }
                        }

                    }
                    String desc = "";
                    boolean flag = false;

                    for (Holder h : holderList) {
                        org.greenpole.hibernate.entity.Holder h_hib = new org.greenpole.hibernate.entity.Holder();
                        if (h.getFirstName() == null || "".equals(h.getFirstName())) {
                            desc = "\nHolder first name should not be empty";
                        } else if (h.getLastName() == null || "".equals(h.getLastName())) {
                            desc += "\nHolder last name should not be empty";
                        } else if (h.getTypeId() <= 0) {
                            desc += "\nHolder type should not be empty";
                        } else if (h.getPryAddress() == null || "".equals(h.getPryAddress())) {
                            desc += "\nPrimary Holder address is not specified";
                        } else if (!h.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                                && !h.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString())) {
                            desc += "\nPrimary address can only be residential or postal";
                        } else if (h.getPryAddress().equalsIgnoreCase(AddressTag.residential.toString())
                                && (h.getResidentialAddresses() == null || h.getResidentialAddresses().isEmpty())) {
                            desc += "\nResidential address cannot be empty, as it is the primary address";
                        } else if (h.getPryAddress().equalsIgnoreCase(AddressTag.postal.toString())
                                && (h.getPostalAddresses() == null || h.getPostalAddresses().isEmpty())) {
                            desc += "\nPostal address cannot be empty, as it is the primary address";
                        } else {
                            flag = true;
                        }
                        if (flag && h.getTypeId() > 0) {
                            boolean found = false;
                            for (HolderType ht : hq.getAllHolderTypes()) {
                                if (h.getTypeId() == ht.getId()) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                desc += "\nHolder type is not valid";
                                flag = false;
                            }
                        }

                        if (flag && h.getResidentialAddresses() != null && !h.getResidentialAddresses().isEmpty()) {
                            for (Address addr : h.getResidentialAddresses()) {
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

                        if (flag && h.getPostalAddresses() != null && !h.getPostalAddresses().isEmpty()) {
                            for (Address addr : h.getPostalAddresses()) {
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

                        if (flag && h.getEmailAddresses() != null && !h.getEmailAddresses().isEmpty()) {
                            for (EmailAddress email : h.getEmailAddresses()) {
                                if (email.getEmailAddress() == null || "".equals(email.getEmailAddress())) {
                                    desc += "\nEmail address should not be empty. Delete email entry if you must";
                                    flag = false;
                                    break;
                                }
                            }
                        }

                        if (flag && h.getPhoneNumbers() != null && !h.getPhoneNumbers().isEmpty()) {
                            for (PhoneNumber phone : h.getPhoneNumbers()) {
                                if (phone.getPhoneNumber() == null || "".equals(phone.getPhoneNumber())) {
                                    desc += "\nPhone number should not be empty. Delete phone number entry if you must";
                                    flag = false;
                                    break;
                                }
                            }
                        }

                        if (flag) {
                            org.greenpole.hibernate.entity.Holder holdEntity = new org.greenpole.hibernate.entity.Holder();
                            HolderType typeEntity = hq.getHolderType(h.getTypeId());

                            holdEntity.setFirstName(h.getFirstName());
                            holdEntity.setLastName(h.getLastName());
                            holdEntity.setMiddleName(h.getMiddleName());
                            holdEntity.setHolderType(typeEntity);
                            holdEntity.setGender(h.getGender());
                            holdEntity.setDob(formatter.parse(h.getDob()));
                            holdEntity.setPryHolder(true);
                            holdEntity.setMerged(false);

                            boolean created;

                            if (h.getChn() == null || "".equals(h.getChn())) {
                                //there are two versions of createHolderAccount, one for bond account and the other for company account
                                //thus, null cannot be directly passed into the method, as java will not be able to distinguish between
                                //both createHolderAccount methods.
                                org.greenpole.hibernate.entity.HolderCompanyAccount emptyAcct = null;
                                created = hq.createHolderAccount(holdEntity, emptyAcct,
                                        retrieveHolderResidentialAddress(h), retrieveHolderPostalAddress(h),
                                        retrieveHolderEmailAddress(h), retrieveHolderPhoneNumber(h));
                            } else {
                                holdEntity.setChn(h.getChn());//set chn
                                created = hq.createHolderAccount(holdEntity, retrieveHolderCompanyAccount(h),
                                        retrieveHolderResidentialAddress(h), retrieveHolderPostalAddress(h),
                                        retrieveHolderEmailAddress(h), retrieveHolderPhoneNumber(h));
                            }
                        }
                        resp.setRetn(200);
                        resp.setDesc("CHN of the new holder to be created cannot be empty");
                        logger.info("CHN of the new holder to be created cannot be empty ", login.getUserId());
                        return resp;

                    }
                }
                resp.setRetn(200);
                resp.setDesc("This transaction exists already");
                logger.info("This transaction exists already ", login.getUserId());
                return resp;
            }
        } catch (Exception ex) {
            logger.info("error processing upload transaction files request. See error log - [{}]", login.getUserId());
            logger.error("error processing upload transaction files request - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to process upload of transaction records request. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
        return resp;
    }

    /**
     * Unwraps Holder residential address from the holder model into
     * HolderResidentialAddress hibernate entity object.
     *
     * @param holdModel object of holder model
     * @param newEntry boolean variable indicating whether or not entry is new
     * @return List of HolderResidentialAddress hibernate entity objects
     */
    private List<HolderResidentialAddress> retrieveHolderResidentialAddress(Holder holdModel/*, boolean newEntry*/) {
        List<org.greenpole.entity.model.Address> residentialAddressList;
        if (holdModel.getResidentialAddresses() != null) {
            residentialAddressList = holdModel.getResidentialAddresses();
        } else {
            residentialAddressList = new ArrayList<>();
        }

        List<org.greenpole.hibernate.entity.HolderResidentialAddress> returnResidentialAddress = new ArrayList();

        for (org.greenpole.entity.model.Address rAddy : residentialAddressList) {
            org.greenpole.hibernate.entity.HolderResidentialAddress residentialAddressEntity = new org.greenpole.hibernate.entity.HolderResidentialAddress();
            residentialAddressEntity.setAddressLine1(rAddy.getAddressLine1());
            residentialAddressEntity.setState(rAddy.getState());
            residentialAddressEntity.setCountry(rAddy.getCountry());
            residentialAddressEntity.setAddressLine2(rAddy.getAddressLine2());
            residentialAddressEntity.setAddressLine3(rAddy.getAddressLine3());
            residentialAddressEntity.setAddressLine4(rAddy.getAddressLine4());
            residentialAddressEntity.setCity(rAddy.getCity());
            residentialAddressEntity.setPostCode(rAddy.getPostCode());
            residentialAddressEntity.setIsPrimary(rAddy.isPrimaryAddress());

            returnResidentialAddress.add(residentialAddressEntity);
        }
        return returnResidentialAddress;
    }

    /**
     * Unwraps the holder postal address details from the HolderModel into
     * HolderPostalAddress hibernate entity
     *
     * @param holdModel the holderModel of holder details
     * @param newEntry boolean value indicating new entry
     * @return List object of HolderPostalAddress hibernate entity
     */
    private List<HolderPostalAddress> retrieveHolderPostalAddress(Holder holdModel/*, boolean newEntry*/) {
        List<org.greenpole.entity.model.Address> hpaddyList;
        if (holdModel.getPostalAddresses() != null) {
            hpaddyList = holdModel.getPostalAddresses();
        } else {
            hpaddyList = new ArrayList<>();
        }

        List<org.greenpole.hibernate.entity.HolderPostalAddress> returnHolderPostalAddress = new ArrayList<>();

        for (org.greenpole.entity.model.Address hpa : hpaddyList) {
            org.greenpole.hibernate.entity.HolderPostalAddress postalAddressEntity = new org.greenpole.hibernate.entity.HolderPostalAddress();
            postalAddressEntity.setAddressLine1(hpa.getAddressLine1());
            postalAddressEntity.setState(hpa.getState());
            postalAddressEntity.setCountry(hpa.getCountry());
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
     * Unwraps Holder email address from the holder model into
     * HolderEmailAddress hibernate entity object
     *
     * @param holdModel object to holder model
     * @param newEntry boolean variable indicating whether or not entry is new
     * @return List of HolderEmailAddress hibernate entity objects
     */
    private List<HolderEmailAddress> retrieveHolderEmailAddress(Holder holdModel/*, boolean newEntry*/) {
        List<org.greenpole.entity.model.EmailAddress> emailAddressList;
        if (holdModel.getEmailAddresses() != null) {
            emailAddressList = holdModel.getEmailAddresses();
        } else {
            emailAddressList = new ArrayList<>();
        }

        List<org.greenpole.hibernate.entity.HolderEmailAddress> returnEmailAddress = new ArrayList<>();

        for (EmailAddress email : emailAddressList) {
            org.greenpole.hibernate.entity.HolderEmailAddress emailAddressEntity = new org.greenpole.hibernate.entity.HolderEmailAddress();
            emailAddressEntity.setEmailAddress(email.getEmailAddress());
            emailAddressEntity.setIsPrimary(email.isPrimaryEmail());

            returnEmailAddress.add(emailAddressEntity);
        }
        return returnEmailAddress;
    }

    /**
     * Unwraps holder phone number details from the holder model passed as
     * parameter into HolderPhoneNumber hibernate entity
     *
     * @param holdModel object of holder details
     * @param newEntry boolean variable indicating whether or not the entry is
     * new
     * @return List of HolderPhoneNumber objects retrieveHolderEmailAddress
     */
    private List<HolderPhoneNumber> retrieveHolderPhoneNumber(Holder holdModel/*, boolean newEntry*/) {
        List<org.greenpole.entity.model.PhoneNumber> phoneNumberList;
        if (holdModel.getPhoneNumbers() != null) {
            phoneNumberList = holdModel.getPhoneNumbers();
        } else {
            phoneNumberList = new ArrayList<>();
        }

        List<org.greenpole.hibernate.entity.HolderPhoneNumber> returnPhoneNumber = new ArrayList<>();

        for (PhoneNumber pnList : phoneNumberList) {
            org.greenpole.hibernate.entity.HolderPhoneNumber phoneNumberEntity = new org.greenpole.hibernate.entity.HolderPhoneNumber();
            // phoneNoId.setHolderId(holdModel.getHolderId());
            phoneNumberEntity.setPhoneNumber(pnList.getPhoneNumber());
            phoneNumberEntity.setIsPrimary(pnList.isPrimaryPhoneNumber());
            phoneNumberEntity.setId(pnList.getEntityId());
            returnPhoneNumber.add(phoneNumberEntity);
        }
        return returnPhoneNumber;
    }

    /**
     * Unwraps holder company account details from the holder model.
     *
     * @param holdModel object of the holder model
     * @param newEntry boolean variable indicating whether or not entry is new
     * @return object of HolderCompanyAccount
     */
    private org.greenpole.hibernate.entity.HolderCompanyAccount retrieveHolderCompanyAccount(Holder holdModel) {
        org.greenpole.hibernate.entity.HolderCompanyAccount companyAccountEntity = new org.greenpole.hibernate.entity.HolderCompanyAccount();
        org.greenpole.entity.model.holder.HolderCompanyAccount compAcct = holdModel.getCompanyAccounts().get(0);
        HolderCompanyAccountId compAcctId = new HolderCompanyAccountId();

        compAcctId.setClientCompanyId(compAcct.getClientCompanyId());

        companyAccountEntity.setId(compAcctId);
        companyAccountEntity.setEsop(compAcct.isEsop());
        companyAccountEntity.setHolderCompAccPrimary(true);
        companyAccountEntity.setMerged(false);

        return companyAccountEntity;
    }

    public Response queryTransaction_Request(Login login, QueryTransaction queryParams) {
        Response resp = new Response();
        Descriptor descriptorUtil = new Descriptor();
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        logger.info("request to query transaction, invoked by [{}]", login.getUserId());
        try {
            if (queryParams.getDescriptor() == null || "".equals(queryParams.getDescriptor())) {
                logger.info("Uploade transaction query unsuccessful. Empty descriptor - [{}]", login.getUserId());
                resp.setRetn(300);
                resp.setDesc("Unsuccessful transaction query, due to empty descriptor. Contact system administrator");
                return resp;
            }

            Map<String, String> descriptors = descriptorUtil.decipherDescriptor(queryParams.getDescriptor());
            if (descriptors.size() == 6) {
                if (descriptors.get("date").equalsIgnoreCase("none")) {
                    try {
                        formatter.parse(queryParams.getStartDate());
                    } catch (ParseException ex) {
                        logger.info("an error was thrown while checking the start date. See error log invoked by [{}]", login.getUserId());
                        resp.setRetn(300);
                        resp.setDesc("Incorrect date format for start date");
                        logger.error("Incorrect date format for start date invoked by [{}]", login.getUserId(), ex);
                    }
                }
                //check end date is properly formatted
                if (descriptors.get("date").equalsIgnoreCase("between")) {
                    try {
                        formatter.parse(queryParams.getEndDate());
                    } catch (ParseException ex) {
                        logger.info("an error was thrown while checking the start date. See error log - [{}]", login.getUserId());
                        resp.setRetn(300);
                        resp.setDesc("Incorrect date format for end date");
                        logger.error("Incorrect date format for end date - [{}]", login.getUserId(), ex);

                        return resp;
                    }
                }
                String descriptor = queryParams.getDescriptor();
                org.greenpole.hibernate.entity.ProcessedTransaction ph_hib_search = new org.greenpole.hibernate.entity.ProcessedTransaction();
                org.greenpole.hibernate.entity.ProcessedTransactionHolderId pth_id_hib_search = new org.greenpole.hibernate.entity.ProcessedTransactionHolderId();
                org.greenpole.hibernate.entity.ProcessedTransaction pt_hib_search = new org.greenpole.hibernate.entity.ProcessedTransaction();
                ProcessedTransaction pt_model_search = new ProcessedTransaction();
                org.greenpole.hibernate.entity.ProcessedTransactionHolder pth_hib_search = new org.greenpole.hibernate.entity.ProcessedTransactionHolder();
                int holderIdFrom, holderIdTo;
                if (queryParams.getUnitTransfer().getClientCompanyName() != null) {
                    org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(queryParams.getUnitTransfer().getClientCompanyId());
                    ph_hib_search.setClientCompany(cc);
                    ph_hib_search.setCompanyName(cc.getCode());
                }
                ProcessedTransactionHolder proHolder_model_search;

                if (queryParams.getUnitTransfer().getHolderIdFrom() > 0) {
                    holderIdFrom = queryParams.getUnitTransfer().getHolderIdFrom();
                    pth_id_hib_search.setHolderId(holderIdFrom);
                }
                if (queryParams.getUnitTransfer().getHolderIdTo() > 0) {
                    holderIdTo = queryParams.getUnitTransfer().getHolderIdTo();
                    pth_id_hib_search.setHolderId(holderIdTo);
                }
                ProcessedTransactionHolder pth_model_search;
                Map<String, Integer> shareUnitSold_search;
                if (queryParams.getShareUnitSold() != null && !queryParams.getShareUnitSold().isEmpty()) {
                    shareUnitSold_search = queryParams.getShareUnitSold();
                } else {
                    shareUnitSold_search = new HashMap<>();
                }
                Map<String, Integer> shareUnitBought_search;
                if (queryParams.getShareUnitBought() != null && !queryParams.getShareUnitBought().isEmpty()) {
                    shareUnitBought_search = queryParams.getShareUnitBought();
                } else {
                    shareUnitBought_search = new HashMap<>();
                }
                Map<String, Integer> bondUnitSold_search;
                if (queryParams.getBondUnitSold() != null && !queryParams.getBondUnitSold().isEmpty()) {
                    bondUnitSold_search = queryParams.getBondUnitSold();
                } else {
                    bondUnitSold_search = new HashMap<>();
                }
                Map<String, Integer> bondUnitBought_search;
                if (queryParams.getBondUnitBought() != null && !queryParams.getBondUnitBought().isEmpty()) {
                    bondUnitBought_search = queryParams.getBondUnitBought();
                } else {
                    bondUnitBought_search = new HashMap<>();
                }
                List<org.greenpole.hibernate.entity.ProcessedTransaction> pt_hib_list = hd.queryTransaction(queryParams.getDescriptor(), ph_hib_search, queryParams.getStartDate(), queryParams.getEndDate(), shareUnitSold_search, shareUnitBought_search,
                        bondUnitSold_search, bondUnitBought_search);
                List<ProcessedTransactionHolder> pth_model_list_out = new ArrayList<>();
                List<ProcessedTransaction> pt_model_list_out = new ArrayList<>();
                for (org.greenpole.hibernate.entity.ProcessedTransaction pt : pt_hib_list) {
                    ProcessedTransaction ptModel = new ProcessedTransaction();
                    ptModel.setCompanyName(pt.getCompanyName());
                    ptModel.setCscsTransactionId(pt.getCscsTransactionId());
                    for (org.greenpole.hibernate.entity.ProcessedTransactionHolder pth : hd.getProcessedTransactionHolder(pt.getId())) {
                        ProcessedTransactionHolderId pth_id_model_out = pth.getId();
                        ProcessedTransactionHolder pthModel = new ProcessedTransactionHolder();
                        org.greenpole.hibernate.entity.Holder h = hd.getHolder(pth.getHolder().getId());
                        pth_id_model_out.setHolderId(pth.getHolder().getId());
                        pth_id_model_out.setTransactionId(pth.getProcessedTransaction().getId());
                        pthModel.setFromTo(pth.getFromTo());
                        pthModel.setHolderChn(pth.getHolderChn());
                        pthModel.setHolderId(pth.getHolder().getId());
                        pthModel.setProcessedTransactionId(pth_id_model_out.getTransactionId());
                        pthModel.setUnitType(pth.getUnitType());
                        pthModel.setUnits(pth.getUnits());
                        pth_model_list_out.add(pthModel);
                    }
                    ptModel.setProcessedTransactionHolder(pth_model_list_out);

                    pt_model_list_out.add(ptModel);
                }
                logger.info("Uploaded Transactions query successful - [{}]", login.getUserId());
                resp.setRetn(0);
                resp.setDesc("Successful");
                resp.setBody(pt_model_list_out);
            }
            logger.info("Upload transaction query unsuccessful - [{}]", login.getUserId());
            resp.setRetn(300);
            resp.setDesc("Unsuccessful upload transaction query, due to incomplete descriptor. Contact system administrator");

            return resp;
        } catch (Exception ex) {
            logger.info("error querying uploaded transaction. See error log - [{}]", login.getUserId());
            logger.error("error querying uploaded transaction - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to query uploaded transaction. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     * Begins transfer of share units.
     *
     * @param senderCompAcct the sender holder
     * @param unitTransfer the share units to be transferred
     * @param receiverCompAcct the receiver holder
     * @param resp the response object to store response from the transfer
     * @param senderName the sender's name
     * @param receiverName the receiver's name
     * @return the response from the transfer
     */
    private Response invokeTransfer(org.greenpole.hibernate.entity.HolderCompanyAccount sellerCompAcct, UnitTransfer unitTransfer, org.greenpole.hibernate.entity.HolderCompanyAccount buyerCompAcct, Response resp, String senderName, String receiverName, Login login,
            String notificationCode, Notification notification) throws JAXBException {
        if (unitTransfer.getHolderIdFrom() != unitTransfer.getHolderIdTo()) { //check if holder and sender are not the same
            if (sellerCompAcct.getShareUnits() >= unitTransfer.getUnits()) { //check if sender has sufficient units to transact
                boolean transfered = hq.transferShareUnits(sellerCompAcct, buyerCompAcct, unitTransfer.getUnits(), unitTransfer.getTransferTypeId());
                if (transfered) {
                    notification.markAttended(notificationCode);
                    resp.setRetn(0);
                    resp.setDesc("Transact");
                    logger.info("Transaction successful: [{}] units from [{}] to [{}] - [{}]",
                            unitTransfer.getUnits(), senderName, receiverName, login.getUserId());
                    return resp;
                }
                resp.setRetn(305);
                resp.setDesc("Transfer Unsuccesful. An error occured in the database engine. Contact System Administrator");
                logger.info("Transfer Unsuccesful. An error occured in the database engine - [{}]", login.getUserId()); //all methods in hibernate must throw hibernate exception
                return resp;
            }
            resp.setRetn(305);
            resp.setDesc("Transaction error. Insufficient balance in " + senderName + "'s company account.");
            logger.info("Transaction error. Insufficient balance in [{}]'s company account - [{}]", senderName, login.getUserId());
            return resp;
        }
        resp.setRetn(305);
        resp.setDesc("Transaction error. Sender and receiver are the same holder.");
        logger.info("Transaction error. Sender and receiver are the same holder - [{}]", login.getUserId());
        return resp;
    }
}
