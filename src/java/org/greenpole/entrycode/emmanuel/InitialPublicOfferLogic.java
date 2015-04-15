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
       if(cq.checkClientCompany(cc.getName())){//requires a method to check that client company has shareholders
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
        resp.setDesc("Client company has no shareholders so initial public offer cannot be created.");
        return resp;
    }
    /**
     * Process request that has been saved as notification file using the
     * notificationCode provided
     * @param notificationCode the notificationCode that was used to save the file 
     * @return response to the initial public offer set up request
     */
    public Response setUpInitialPublicOffer_authorize(String notificationCode){
        Response resp = new Response();
              logger.info("Initial Public Offer authorised - [{}]", notificationCode);
              try{
               InitialPublicOffer ipoModel =  setUpInitialPublicOfferAfterAuthorization(notificationCode); 
               logger.info("initial public offer created - [{}]", ipoModel.getClientCompany()+"/"+ipoModel.getVersionId());
               ipoModel.getVersionId();
               logger.info("versionID - ", ipoModel.getVersionId());
               ipoModel.getClientCompany();
               logger.info("clientCompany - ", ipoModel.getClientCompany());
               ipoModel.getTotalSharesOnOffer();
               logger.info("total shares on offer - ", ipoModel.getTotalSharesOnOffer());
               ipoModel.getMethodOnOffer();
               logger.info("method on offer - ", ipoModel.getMethodOnOffer());
               ipoModel.getStartingMinSub();
               logger.info("starting minimum subscription - ", ipoModel.getStartingMinSub());
               ipoModel.getContMinSub();
               logger.info("cont minimum subscription - ", ipoModel.getContMinSub());
               ipoModel.getOfferSize();
               logger.info("size of offer - ", ipoModel.getOfferSize());
               ipoModel.getOfferPrice();
               logger.info("offer price - ", ipoModel.getOfferPrice());
               ipoModel.getOpeningDate();
               logger.info("opening date - ", ipoModel.getOpeningDate());
               ipoModel.getClosingDate();
               logger.info("closing date - ", ipoModel.getClosingDate());
               resp.setRetn(0);
               resp.setDesc("Successful");
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
     * This method persist the data's into the database after the authorisation must have been accepted
     * @param notificationCode the notificationCode that was used to save the data's to a file before authorisation 
     * @return the hibernate Initial Public Offer entity
     * @throws JAXBException 
     */
    private InitialPublicOffer setUpInitialPublicOfferAfterAuthorization(String notificationCode) throws JAXBException{
    NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
        InitialPublicOffer ipoModel = (InitialPublicOffer) wrapper.getModel();
        org.greenpole.hibernate.entity.InitialPublicOffer ipo_hib = new org.greenpole.hibernate.entity.InitialPublicOffer();
        try{
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
        cq.createInitialPublicOffer(ipo_hib);
        }catch(Exception e){
        }
        
        return ipo_hib;
    }
}
