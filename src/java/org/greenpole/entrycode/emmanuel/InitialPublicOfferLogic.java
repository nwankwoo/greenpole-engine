/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.greenpole.entity.model.clientcompany.ClientCompany;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.hibernate.entity.ShareQuotation;
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
    /**
     * Processes request to setUpInitialPublicOffer_authorize an Initial public offer that has been saved to a file 
     * with the notificationCode
     * @param notificationCode the client company model (not to be confused with the client company hibernate entity)
     * @return resp object
     */
    public Response setUpInitialPublicOffer_authorize(String notificationCode){
     Response resp = new Response();
     logger.info("Initial Public Offer authorised - [{}]", notificationCode);
     try{
     org.greenpole.hibernate.entity.InitialPublicOffer ipo_hib = new org.greenpole.hibernate.entity.InitialPublicOffer();
     cq.createInitialPublicOffer(ipo_hib);
     resp.setRetn(0);
     resp.setDesc("Initial Public Offer was Successfully created");
     return resp;
     }catch(Exception ex){
     logger.info("error loading notification xml file. See error log");
     logger.error("error loading notification xml file to object - ", ex);
     resp.setRetn(200);
     resp.setDesc("Unable to perform initial public offer from authorisation. Contact System Administrator");
     }          
    return resp;
    }
    /**
     * creates the hibernate entity object
     * uses the InitialPublicOffer model
     * @param notificationCode the notificationCode that was used to save the data's to a file before authorisation 
     * @param ipo_hib the hibernate entity object
     * @param  ipoModel the InitialPublicOffer object
     * @return the hibernate entity object
     * @throws JAXBException 
     */
    private void setUpInitialPublicOfferAfterAuthorization(String notificationCode) throws JAXBException{
    NotificationWrapper wrapper = Notification.loadNotificationFile(notificationCode);
    org.greenpole.hibernate.entity.InitialPublicOffer ipo_hib = new org.greenpole.hibernate.entity.InitialPublicOffer();
    InitialPublicOffer ipoModel = (InitialPublicOffer) wrapper.getModel();
    ipo_hib.setClientCompany(ipoModel.getClientCompany());
    ipo_hib.setTotalSharesOnOffer(ipoModel.getTotalSharesOnOffer());
    ipo_hib.setMethodOnOffer(ipoModel.getMethodOnOffer());
    ipo_hib.setStartingMinSub(ipoModel.getStartingMinSub());
    ipo_hib.setContMinSub(ipoModel.getContMinSub());
    ipo_hib.setOfferPrice(ipoModel.getOfferPrice());
    ipo_hib.setOfferSize(ipoModel.getOfferPrice()*ipoModel.getTotalSharesOnOffer());
    ipo_hib.setOpeningDate(ipoModel.getOpeningDate());
    ipo_hib.setClosingDate(ipoModel.getClosingDate());
    }
    /**
     * views the share unit quotations of client companies
     * @return the list of share unit quotations
     */
    private List<org.greenpole.hibernate.entity.ShareQuotation> viewShareUnitQuotations(){
    List<org.greenpole.hibernate.entity.ShareQuotation> list = new ArrayList();
    list = cq.retrieveShareUnitQuatationList();
    return list;
    }
}
