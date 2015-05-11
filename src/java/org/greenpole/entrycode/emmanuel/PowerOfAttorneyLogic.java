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
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
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
     * @param authenticator the super user to accept the creation of this
     * request
     * @param poa the power of attorney to be uploaded
     * @return response to the upload power of attorney request
     */
    public Response uploadPowerOfAttorney_Request(Login login, String authenticator, PowerOfAttorney poa) {
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
            logger.info("error proccessing holder administrator creation. See error log - [{}]", login.getUserId());
            logger.error("error proccessing holder administrator creation - [" + login.getUserId() + "]", ex);
            
            resp.setRetn(99);
            resp.setDesc("General error. Unable to proccess holder administrator creation. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    public Response uploadPowerOfAttorney_authorise(Login login, String notificationCode) {
        Response resp = new Response();
        logger.info("Upload of power of attorney authorised - [{}]", notificationCode);
        logger.info("Upload of power of attorney performed by: " + login.getUserId());
        Date current_date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<PowerOfAttorney> adminList = (List<PowerOfAttorney>) wrapper.getModel();
            PowerOfAttorney powerModel = adminList.get(0);
            Holder hold = powerModel.getHolder();
            org.greenpole.hibernate.entity.Holder holder = hd.retrieveHolderObject(hold.getId());
            org.greenpole.hibernate.entity.PowerOfAttorney power = new org.greenpole.hibernate.entity.PowerOfAttorney();
            //power.setPeriodType(powerModel.getPeriodType());
            boolean isPrimaryPowerOfAttorney_status = hd.updatePowerOfAttorneyStatus(powerModel.getHolder().getId());
            org.greenpole.hibernate.entity.PowerOfAttorney currentPowerofAttorney = hd.retrieveCurrentPowerOfAttorney(powerModel.getHolder().getId());
            //if (isPrimaryPowerOfAttorney_status == true) {
            if (current_date.after(currentPowerofAttorney.getEndDate()) || powerModel.getTitle().isEmpty()) {
                power.setPowerOfAttorneyPrimary(powerModel.isPrimaryPowerOfAttorney());
                power.setFilePath(powerModel.getFilePath());
                power.setTitle(powerModel.getTitle());
                power.setType(powerModel.getType());
                power.setStartDate(formatter.parse(powerModel.getStartDate()));
                power.setEndDate(formatter.parse(powerModel.getEndDate()));
                power.setHolder(holder);
                power.setId(powerModel.getId());
                hd.uploadPowerOfAttorney(powerModel);
                logger.info("Power of attorney uploaded with the title [{}] " + powerModel.getTitle());
                resp.setRetn(0);
                resp.setDesc("Successful");
                return resp;
            } else {
                resp.setRetn(210);
                resp.setDesc("Failed to upload power of attorney because the expiration period of the previous one has not ended");
                logger.info("Power of attorney not uploaded, see error logg for details ");
                return resp;
            }

        } catch (Exception ex) {
            resp.setRetn(0);
            resp.setDesc("Unable to upload power of attorney, please contact the system admin" + ex);
            logger.info("see error log for details");
            logger.error("error: " + ex);
        }
        return resp;
    }

    private String createSignatureFileName() {
        Date date = new Date();
        Random rand = new Random();
        int randomNumber = rand.nextInt(9999999);
        String fileName = randomNumber + "" + date.getTime();
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

    public Response getPowerOfAttorney_request(PowerOfAttorney attorneyPower) {
        Response resp = new Response();
        //byte[] image = null;
        //List<BufferedImage> imageList = new ArrayList();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy");
        List<byte[]> signatureBody = new ArrayList<>();
        org.greenpole.hibernate.entity.PowerOfAttorney power_hib = new org.greenpole.hibernate.entity.PowerOfAttorney();
        try {
            org.greenpole.hibernate.entity.Holder hold = getPowerOfAttorney(attorneyPower.getHolder());
            byte[] convertedSignature = readFile(power_hib.getFilePath());
            signatureBody.add(convertedSignature);
            attorneyPower.setId(hold.getId());
            attorneyPower.setTitle(power_hib.getTitle());
            //attorneyPower.setPeriodType(power_hib.getPeriodType());
            attorneyPower.setType(power_hib.getType());
            attorneyPower.setStartDate(formatter.format(power_hib.getStartDate()));
            attorneyPower.setEndDate(formatter.format(power_hib.getEndDate()));
            attorneyPower.setId(power_hib.getId());
            attorneyPower.setPrimaryPowerOfAttorney(power_hib.isPowerOfAttorneyPrimary());
            attorneyPower.setSignatureFile(convertedSignature);
            resp.setBody(signatureBody);
            resp.setRetn(0);
            resp.setDesc("Power of Attorney records found");
            return resp;
        } catch (Exception ex) {
            resp.setRetn(220);
            resp.setDesc("No records found for power of attorney");
        }
        return resp;
    }

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
