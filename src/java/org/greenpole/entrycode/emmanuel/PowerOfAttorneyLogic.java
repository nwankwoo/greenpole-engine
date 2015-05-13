/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.model.holder.PowerOfAttorney;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.entrycode.emmanuel.model.Holder;
import org.greenpole.entrycode.jeph.mocks.SignatureProperties;
import org.greenpole.hibernate.query.HolderComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.BytesConverter;
import org.greenpole.util.GreenpoleFile;
import org.greenpole.util.Notification;
import org.greenpole.util.properties.GreenpoleProperties;
import org.greenpole.util.properties.NotifierProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * sends an authorisation requesst to a super user for the upload of power of
 * attorney
 *
 * @author user
 */
public class PowerOfAttorneyLogic {

    private static final Logger logger = LoggerFactory.getLogger(PowerOfAttorneyLogic.class);
    private final HolderComponentQuery hq = ComponentQueryFactory.getHolderComponentQuery();
    private final HibernatDummyQuerInterface hd = HibernateDummyQueryFactory.getHibernateDummyQuery();//not needed
    private final GreenpoleProperties greenProp = new GreenpoleProperties(null);//no need to set this here

    /**
     * Request to upload power of attorney for a holder.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param poa the power of attorney to be uploaded
     * @return response to the upload power of attorney request
     */
    /*public Response uploadPowerOfAttorney_Request(Login login, String authenticator, PowerOfAttorney poa) {
    logger.info("request to upload power of attorney, invoked by [{}]", login.getUserId());
    
    Response resp = new Response();
    NotificationWrapper wrapper;
    QueueSender qSender;
    NotifierProperties prop;
    
    boolean flag = false;
    String desc = "";
    
    try {
    long defaultSize = Long.valueOf(greenProp.getPowerOfAttorneySize());
    Date current_date = new Date();
    
    if (hq.checkHolderAccount(poa.getHolderId())) {
    org.greenpole.hibernate.entity.Holder holder = hq.getHolder(poa.getHolderId());
    logger.info("Holder [{}] checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
    
    if (hq.checkCurrentPowerOfAttorney(poa.getHolderId())) {
    org.greenpole.hibernate.entity.PowerOfAttorney currentPoa = hq.getCurrentPowerOfAttorney(poa.getHolderId());
    logger.info("Holder has current power of attorney - [{}]", login.getUserId());
    
    if (current_date.before(currentPoa.getEndDate())) {
    desc += "\nThe current power of attorney is yet to expire";
    } else {
    flag = true;
    }
    } else {
    flag = true;
    }
    
    if (flag) {
    long fileSize = BytesConverter.decodeToBytes(poa.getFileContents()).length;
    
    if (fileSize <= defaultSize) {
    wrapper = new NotificationWrapper();
    prop = new NotifierProperties(PowerOfAttorneyLogic.class);
    qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
    prop.getAuthoriserNotifierQueueName());
    
    List<PowerOfAttorney> powerList = new ArrayList();
    powerList.add(poa);
    
    wrapper.setCode(Notification.createCode(login));
    wrapper.setDescription("Authenticate power of attorney for " + holder.getFirstName() + " " + holder.getLastName());
    wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
    wrapper.setFrom(login.getUserId());
    wrapper.setTo(authenticator);
    wrapper.setModel(powerList);
    resp = qSender.sendAuthorisationRequest(wrapper);
    
    resp.setRetn(0);
    resp.setDesc("Successful");
    logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
    return resp;
    }
    resp.setRetn(300);
    resp.setDesc("The size of the power of attorney cannot exceed 10MB.");
    logger.info("The size of the power of attorney cannot exceed 10MB - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);
    resp.setDesc("Error: " + desc);
    logger.info("error detected in upload power of attorney process - [{}]: [{}]", login.getUserId(), resp.getRetn());
    return resp;
    }
    resp.setRetn(300);//change code
    resp.setDesc("The holder does not exist.");
    logger.info("The holder does not exist - [{}]", login.getUserId());
    return resp;
    } catch (Exception ex) {
    logger.info("error proccessing power of attorney upload. See error log - [{}]", login.getUserId());
    logger.error("error proccessing power of attorney upload - [" + login.getUserId() + "]", ex);
    
    resp.setRetn(99);
    resp.setDesc("General error. Unable to proccess power of attorney upload. Contact system administrator."
    + "\nMessage: " + ex.getMessage());
    return resp;
    }
    }*/

    /**
     * Processes a saved request to upload power of attorney
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the upload power of attorney request
     */
    /*public Response uploadPowerOfAttorney_Authorise(Login login, String notificationCode) {
    logger.info("authorise upload power of attorney, invoked by [{}]", login.getUserId());
    Response resp = new Response();
    
    try {
    NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
    List<PowerOfAttorney> poaList = (List<PowerOfAttorney>) wrapper.getModel();
    SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
    
    boolean flag = false;
    String desc = "";
    
    PowerOfAttorney poaModel = poaList.get(0);
    org.greenpole.hibernate.entity.PowerOfAttorney poa_hib = new org.greenpole.hibernate.entity.PowerOfAttorney();
    org.greenpole.hibernate.entity.PowerOfAttorney currentPoa = new org.greenpole.hibernate.entity.PowerOfAttorney();
    
    long defaultSize = Long.valueOf(greenProp.getPowerOfAttorneySize());
    Date current_date = new Date();
    
    if (hq.checkHolderAccount(poaModel.getHolderId())) {
    org.greenpole.hibernate.entity.Holder holder = hq.getHolder(poaModel.getHolderId());
    logger.info("Holder [{}] checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
    
    boolean currentPoaExists = hq.checkCurrentPowerOfAttorney(poaModel.getHolderId());
    if (currentPoaExists) {
    currentPoa = hq.getCurrentPowerOfAttorney(poaModel.getHolderId());
    logger.info("Holder has current power of attorney - [{}]", login.getUserId());
    
    if (current_date.before(currentPoa.getEndDate())) {
    desc += "\nThe current power of attorney is yet to expire";
    } else {
    flag = true;
    }
    } else {
    flag = true;
    }
    
    if (flag) {
    GreenpoleFile file = new GreenpoleFile(greenProp.getPowerOfAttorneyPath());
    long fileSize = BytesConverter.decodeToBytes(poaModel.getFileContents()).length;
    
    if (fileSize <= defaultSize) {
    logger.info("Power of attorney met file size requirement - [{}]", login.getUserId());
    
    if (file.createFile(BytesConverter.decodeToBytes(poaModel.getFileContents()))) {
    logger.info("Power of attorney file created and saved - [{}]", login.getUserId());
    
    String filepath = file.getFolderPath() + file.getFileName();
    poa_hib.setTitle(poaModel.getTitle());
    poa_hib.setType(poaModel.getType());
    poa_hib.setStartDate(formatter.parse(poaModel.getStartDate()));
    poa_hib.setEndDate(formatter.parse(poaModel.getEndDate()));
    poa_hib.setFilePath(filepath);
    
    boolean uploaded;
    if (currentPoaExists) {
    uploaded = hq.uploadPowerOfAttorney(poa_hib, currentPoa);
    } else {
    uploaded = hq.uploadPowerOfAttorney(poa_hib, null);
    }
    
    if (uploaded) {
    resp.setRetn(0);
    resp.setDesc("Successful");
    logger.info("Power of attorney successfully uploaded - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300); //change
    resp.setDesc("Power of attorney upload failed due to database error. Contact Administrator.");
    logger.info("Power of attorney upload failed due to database error. Check error logs - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);
    resp.setDesc("The Power of Attorney file could not be uploaded onto the server. Contact Administrator");
    logger.info("The Power of Attorney file could not be uploaded onto the server. See error logs - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);
    resp.setDesc("The size of the power of attorney cannot exceed 10MB.");
    logger.info("The size of the power of attorney cannot exceed 10MB - [{}]", login.getUserId());
    return resp;
    }
    resp.setRetn(300);
    resp.setDesc("Error: " + desc);
    logger.info("error detected in upload power of attorney process - [{}]: [{}]", login.getUserId(), resp.getRetn());
    return resp;
    }
    resp.setRetn(300);//change code
    resp.setDesc("The holder does not exist.");
    logger.info("The holder does not exist - [{}]", login.getUserId());
    return resp;
    } catch (JAXBException ex) {
    logger.info("error loading notification xml file. See error log - [{}]", login.getUserId());
    logger.error("error loading notification xml file to object - [" + login.getUserId() + "]", ex);
    
    resp.setRetn(98);
    resp.setDesc("Unable to upload power of attorney. Contact System Administrator");
    
    return resp;
    } catch (IOException ex) {
    logger.info("Power of attorney file upload failed with an I/O error. See error log - [{}]", login.getUserId());
    logger.error("Power of attorney file upload failed with an I/O error - [" + login.getUserId() + "]", ex);
    
    resp.setRetn(300);//change code
    resp.setDesc("Power of attorney file upload failed with an I/O error. Contact Administrator");
    
    return resp;
    } catch (ParseException ex) {
    logger.info("Error occured while parsing start / end date into power of attorney. See error log - [{}]", login.getUserId());
    logger.error("Error occured while parsing start / end date into power of attorney - [" + login.getUserId() + "]", ex);
    
    resp.setRetn(300);//change code
    resp.setDesc("Error occured while parsing start / end date into power of attorney. Contact Administrator");
    
    return resp;
    } catch (Exception ex) {
    logger.info("error uploading power of attorney. See error log - [{}]", login.getUserId());
    logger.error("error uploading power of attorney - [" + login.getUserId() + "]", ex);
    
    resp.setRetn(99);
    resp.setDesc("General error. Unable to upload power of attorney. Contact system administrator."
    + "\nMessage: " + ex.getMessage());
    return resp;
    }
    }*/

    private String createSignatureFileName() {
        Date date = new Date();
        Random rand = new Random();
        int randomNumber = rand.nextInt(99999999);
        String fileName = randomNumber + "_" + date.getTime();
        return fileName;
    }

    /**
     *
     * @param holder
     * @return list of hibernate entity object
     */
    public org.greenpole.hibernate.entity.Holder getPowerOfAttorney(Holder holder) {
        org.greenpole.hibernate.entity.Holder holderObject = hd.retrieveHolderObject(holder.getId());
        return holderObject;
    }

    /**
     * Searches for a specific power of attorney for a holder.
     * @param login the user's login details
     * @param queryParams the query parameters
     * @return response to the query power of attorney
     */
    /*public Response queryPowerOfAttorney_Request(Login login, PowerOfAttorney queryParams) {
    logger.info("request to query power of attorney, invoked by [{}]", login.getUserId());
    Response resp = new Response();
    
    try {
    SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
    PowerOfAttorney poa_model = new PowerOfAttorney();
    org.greenpole.hibernate.entity.PowerOfAttorney poa_hib;
    
    if (hq.checkHolderAccount(queryParams.getHolderId())) {
    org.greenpole.hibernate.entity.Holder holder = hq.getHolder(queryParams.getHolderId());
    logger.info("Holder [{}] checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
    
    poa_hib = hq.getHolderPowerOfAttorney(queryParams.getId());
    
    File file = new File(poa_hib.getFilePath());
    byte[] read = Files.readAllBytes(file.toPath());
    String encodedContents = BytesConverter.encodeToString(read);
    logger.info("Power of attorney file successfully read - [{}]", login.getUserId());
    
    poa_model.setId(poa_hib.getId());
    poa_model.setHolderId(poa_hib.getHolder().getId());
    poa_model.setTitle(poa_hib.getTitle());
    poa_model.setType(poa_hib.getType());
    poa_model.setStartDate(formatter.format(poa_hib.getStartDate()));
    poa_model.setEndDate(formatter.format(poa_hib.getEndDate()));
    poa_model.setPrimaryPowerOfAttorney(poa_hib.isPowerOfAttorneyPrimary());
    poa_model.setFilePath(poa_hib.getFilePath());
    poa_model.setFileContents(encodedContents);
    
    List<PowerOfAttorney> poa_result = new ArrayList<>();
    poa_result.add(poa_model);
    
    resp.setRetn(0);
    resp.setDesc("Successful");
    logger.info("Power of attorney successfully queried - [{}]", login.getUserId());
    resp.setBody(poa_result);
    return resp;
    }
    resp.setRetn(300);//change code
    resp.setDesc("The holder does not exist.");
    logger.info("The holder does not exist - [{}]", login.getUserId());
    return resp;
    } catch (Exception ex) {
    logger.info("error querying power of attorney. See error log - [{}]", login.getUserId());
    logger.error("error querying power of attorney - [" + login.getUserId() + "]", ex);
    
    resp.setRetn(99);
    resp.setDesc("General error. Unable to query power of attorney. Contact system administrator."
    + "\nMessage: " + ex.getMessage());
    return resp;
    }
    }*/
    
    /**
     * Searches for all powers of attorney for a specific holder
     * @param login the user's login details
     * @param queryParams the query parameters
     * @return response to the query all power of attorney request
     */
    /*public Response queryAllPowerOfAttorney_Request(Login login, PowerOfAttorney queryParams) {
    logger.info("request to query power of attorney, invoked by [{}]", login.getUserId());
    Response resp = new Response();
    
    try {
    SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
    List<org.greenpole.hibernate.entity.PowerOfAttorney> poa_hib_list;
    
    if (hq.checkHolderAccount(queryParams.getHolderId())) {
    org.greenpole.hibernate.entity.Holder holder = hq.getHolder(queryParams.getHolderId());
    logger.info("Holder [{}] checks out - [{}]", holder.getFirstName() + " " + holder.getLastName(), login.getUserId());
    
    poa_hib_list = hq.getAllHolderPowerOfAttorney(queryParams.getHolderId());
    List<PowerOfAttorney> poa_result = new ArrayList<>();
    
    for (org.greenpole.hibernate.entity.PowerOfAttorney poa_hib : poa_hib_list) {
    PowerOfAttorney poa_model = new PowerOfAttorney();
    
    File file = new File(poa_hib.getFilePath());
    byte[] read = Files.readAllBytes(file.toPath());
    String encodedContents = BytesConverter.encodeToString(read);
    logger.info("Power of attorney file successfully read - [{}]", login.getUserId());
    
    poa_model.setId(poa_hib.getId());
    poa_model.setHolderId(poa_hib.getHolder().getId());
    poa_model.setTitle(poa_hib.getTitle());
    poa_model.setType(poa_hib.getType());
    poa_model.setStartDate(formatter.format(poa_hib.getStartDate()));
    poa_model.setEndDate(formatter.format(poa_hib.getEndDate()));
    poa_model.setPrimaryPowerOfAttorney(poa_hib.isPowerOfAttorneyPrimary());
    poa_model.setFilePath(poa_hib.getFilePath());
    poa_model.setFileContents(encodedContents);
    
    poa_result.add(poa_model);
    }
    
    resp.setRetn(0);
    resp.setDesc("Successful");
    logger.info("Power of attorney successfully queried - [{}]", login.getUserId());
    resp.setBody(poa_result);
    return resp;
    }
    resp.setRetn(300);//change code
    resp.setDesc("The holder does not exist.");
    logger.info("The holder does not exist - [{}]", login.getUserId());
    return resp;
    } catch (Exception ex) {
    logger.info("error querying power of attorney. See error log - [{}]", login.getUserId());
    logger.error("error querying power of attorney - [" + login.getUserId() + "]", ex);
    
    resp.setRetn(99);
    resp.setDesc("General error. Unable to query power of attorney. Contact system administrator."
    + "\nMessage: " + ex.getMessage());
    return resp;
    }
    }*/

    public byte[] readFile(String path) {
        FileInputStream fileInputStream = null;
        File file = new File(path);
        byte[] signatureFile = new byte[(int) file.length()];
        try {
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(signatureFile);
            fileInputStream.close();
        } catch (Exception ex) {
        }
        return signatureFile;
    }
}
