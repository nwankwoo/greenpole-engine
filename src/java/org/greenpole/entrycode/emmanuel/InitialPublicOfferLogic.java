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
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.hibernate.entity.InitialPublicOffer;
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
    
    /**
     * Processes request to set up an Initial Public Offer.
     * @param ipo the ClientCompany initial public offer details
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param cc the client company under which the IPO is to be set
     * @return response to the initial public offer request 
     */
    public Response setUpInitialPoblicOffer(InitialPublicOffer ipo,Login login,String authenticator,ClientCompany cc){
       logger.info("request to set up an Initial Public Offer [{}] from [{}]", cc.getName(), login.getUserId());
       Response resp = new Response();
       NotificationWrapper wrapper;
       QueueSender qSender;
       NotifierProperties prop;
       if(cq.checkClientCompanyForShareholders(cc.getName())==true){
       wrapper = new NotificationWrapper();
       prop = new NotifierProperties(InitialPublicOfferLogic.class);
       qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(), 
       prop.getAuthoriserNotifierQueueName()); 
       List<InitialPublicOffer> ipoList = new ArrayList();
       ipoList.add(ipo);
       wrapper.setCode(Notification.createCode(login));
       wrapper.setDescription("Authenticate set up of an Initial Public Offer under the client company" +" "+cc.getName());
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
    public Response setUpInitialPublicOffer_authorize(String notificationCode){
     Response resp = new Response();
     logger.info("Initial Public Offer authorised - [{}]", notificationCode);
     try{
     InitialPublicOffer ipoModel =  setUpInitialPublicOfferAfterAuthorization(notificationCode);
     org.greenpole.hibernate.entity.InitialPublicOffer ipo_hib = new org.greenpole.hibernate.entity.InitialPublicOffer();
     cq.createInitialPublicOffer(ipo_hib);
     resp.setRetn(0);
     resp.setDesc("Initial Public Offer was Successfully created");
     return resp;
     }catch(JAXBException ex){
     logger.info("error loading notification xml file. See error log");
     logger.error("error loading notification xml file to object - ", ex);
     resp.setRetn(200);
     resp.setDesc("Unable to perform initial public offer from authorisation. Contact System Administrator");
     }          
    return resp;
    }
    /**
     * persist the data's into the database after the authorisation must have been accepted
     * uses the InitialPublicOffer model
     * @param notificationCode the notificationCode that was used to save the data's to a file before authorisation 
     * @return the hibernate Initial Public Offer entity
     * @throws JAXBException 
     */
    private InitialPublicOffer setUpInitialPublicOfferAfterAuthorization(String notificationCode) throws JAXBException{
    NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
    InitialPublicOffer ipoModel = (InitialPublicOffer) wrapper.getModel();
    org.greenpole.hibernate.entity.InitialPublicOffer ipo_hib = new org.greenpole.hibernate.entity.InitialPublicOffer();
    double mult;
    ipo_hib.setClientCompany(ipoModel.getClientCompany());
    ipo_hib.setTotalSharesOnOffer(ipoModel.getTotalSharesOnOffer());
    ipo_hib.setMethodOnOffer(ipoModel.getMethodOnOffer());
    ipo_hib.setStartingMinSub(ipoModel.getStartingMinSub());
    ipo_hib.setContMinSub(ipoModel.getContMinSub());
    ipo_hib.setOfferPrice(ipoModel.getOfferPrice());
    mult = ipoModel.getOfferPrice()*ipoModel.getTotalSharesOnOffer();
    ipo_hib.setOfferSize((int) mult);
    ipo_hib.setOpeningDate(ipoModel.getOpeningDate());
    ipo_hib.setClosingDate(ipoModel.getClosingDate());
    return ipo_hib;
    }
}
