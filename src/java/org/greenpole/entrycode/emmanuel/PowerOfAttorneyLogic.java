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
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.entrycode.emmanuel.model.Holder;
import org.greenpole.entrycode.emmanuel.model.PowerOfAttorney;
import org.greenpole.entrycode.jeph.mocks.SignatureProperties;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.Notification;
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
    private final HibernatDummyQuerInterface hd = HibernateDummyQueryFactory.getHibernateDummyQuery();

    public Response uploadPowerOfAttorney_request(Login login, String authenticator, PowerOfAttorney power, byte[] signatureOfAttorney) {
        logger.info("request to upload power of attorney for holder [{}] by user [{}]", power.getHolder().getFirstName() + " " + power.getHolder().getLastName(), login.getUserId());
        Response resp = new Response();
        NotificationWrapper wrapper;
        QueueSender qSender;
        NotifierProperties prop;
        long sizeOfSignature = 10485760;
        SignatureProperties signProp;
        signProp = new SignatureProperties();
        try {
            if (signatureOfAttorney.length <= sizeOfSignature) {
                InputStream inputStream = new ByteArrayInputStream(signatureOfAttorney);
                BufferedImage byteImageConverted = ImageIO.read(inputStream);
                String signatureFileName = createSignatureFileName();
                String filePath = signProp.getSignaturePath() + " " + signatureFileName + ".jpg ";
                ImageIO.write(byteImageConverted, "jpg", new File(filePath));
                power.setSignaturePath(filePath);
                wrapper = new NotificationWrapper();
                prop = new NotifierProperties(PowerOfAttorneyLogic.class);
                qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                        prop.getAuthoriserNotifierQueueName());
                List<PowerOfAttorney> powerList = new ArrayList();
                powerList.add(power);
                wrapper.setCode(Notification.createCode(login));
                wrapper.setDescription("Authenticate power of attorney for " + " " + power.getHolder().getFirstName() + " " + power.getHolder().getLastName() + " " + " by user" + login.getUserId());
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(powerList);
                resp = qSender.sendAuthorisationRequest(wrapper);
                resp.setRetn(0);
                resp.setDesc("Successful");
                return resp;
            } else {
                resp.setRetn(200);
                resp.setDesc("Unable to complete transaction because signaturesize is bigger than 10MB");
                logger.info("image cannot be saved because the size is bigger than 10MB");
            }

        } catch (Exception e) {
            resp.setRetn(200);
            resp.setDesc("Image size should not be bigger than 10MB. ");
            logger.info("see error log");
            logger.error("error: " + e);
        }
        return resp;
    }

    public Response uploadPowerOfAttorney_authorise(Login login, String notificationCode) {
        Response resp = new Response();
        logger.info("Upload of power of attorney authorised - [{}]", notificationCode);
        logger.info("Upload of power of attorney performed by: " + login.getUserId());
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        try {
            NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
            List<PowerOfAttorney> adminList = (List<PowerOfAttorney>) wrapper.getModel();
            PowerOfAttorney powerModel = adminList.get(0);
            Holder hold = powerModel.getHolder();
            org.greenpole.hibernate.entity.Holder holder = hd.retrieveHolderObject(hold.getId());
            org.greenpole.hibernate.entity.PowerOfAttorney power = new org.greenpole.hibernate.entity.PowerOfAttorney();
            power.setPeriodType(powerModel.getPeriodType());
            power.setPowerOfAttorneyPrimary(powerModel.isPrimaryPowerOfAttorney());
            power.setSignaturePath(powerModel.getSignaturePath());
            power.setTitle(powerModel.getTitle());
            power.setType(powerModel.getType());
            power.setStartDate(formatter.parse(powerModel.getStartDate()));
            power.setEndDate(formatter.parse(powerModel.getEndDate()));
            power.setHolder(holder);
            hd.uploadPowerOfAttorney(powerModel);
            resp.setRetn(0);
            resp.setDesc("Successful");
        } catch (Exception ex) {
            resp.setRetn(0);
            resp.setDesc("Unable to create administrator, please contact the system admin" + ex);
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
            byte[] convertedSignature = readFile(power_hib.getSignaturePath());
            signatureBody.add(convertedSignature);
            attorneyPower.setId(hold.getId());
            attorneyPower.setTitle(power_hib.getTitle());
            attorneyPower.setPeriodType(power_hib.getPeriodType());
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
