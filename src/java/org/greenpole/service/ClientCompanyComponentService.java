/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.service;

import java.util.List;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import org.greenpole.entity.model.clientcompany.BondOffer;
import org.greenpole.entity.model.clientcompany.ClientCompany;
import org.greenpole.entity.model.clientcompany.InitialPublicOffer;
import org.greenpole.entity.model.clientcompany.PrivatePlacement;
import org.greenpole.entity.model.clientcompany.QueryClientCompany;
import org.greenpole.entity.model.clientcompany.ShareQuotation;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;

/**
 *
 * @author Akin
 * Web service for client company component.
 */
@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface ClientCompanyComponentService {
    
    /**
     * Processes request to create a new client company.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param cc the client company to be created
     * @return response to the client company creation request 
     */
    @WebMethod
    @WebResult(name = "response")
    public Response createClientCompany_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "authenticator") String authenticator,
            @WebParam(name = "clientcompany") ClientCompany cc);
    
    /**
     * Processes request to create a client company that has already been saved 
     * as a notification file, according to the specified notification code.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the client company creation request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response createClientCompany_Authorise(@WebParam(name = "login", header = true) Login login, @WebParam(name = "notificationCode") String notificationCode);
    
    /**
     * Processes request to edit an existing client company.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param cc the client company to be created
     * @return response to the client company creation request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response editClientCompany_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "authenticator") String authenticator,
            @WebParam(name = "clientcompany") ClientCompany cc);
    
    /**
     * Processes request to change a client company that has already been saved 
     * as a notification file, according to the specified notification code.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the client company creation request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response editClientCompany_Authorise(@WebParam(name = "login", header = true) Login login, @WebParam(name = "notificationCode") String notificationCode);
    
    /**
     * Searches for a list of client companies according to query parameters.
     * @param login the user's login details
     * @param queryParams the query parameters
     * @return response to the client company query request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response queryClientCompany_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "queryParams") QueryClientCompany queryParams);
    
    /**
     * Processes request to upload a list of share quotations.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param shareQuotation the list of share quotations to be uploaded
     * @return response to the share quotation upload request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response uploadShareUnitQuotations_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "authenticator") String authenticator,
            @WebParam(name = "shareQuotation") List<ShareQuotation> shareQuotation);
    
    /**
     * Processes request to upload a list of share quotations that has already been saved.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the share quotation upload request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response uploadShareUnitQuotations_Authorise(@WebParam(name = "login", header = true) Login login, @WebParam(name = "notificationCode") String notificationCode);
    
    /**
     * Retrieves the share unit quotation.
     * @param login the user's login details
     * @return the response to the share unit quotation view request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response getShareUnitQuotations_request(@WebParam(name = "login", header = true) Login login);
    
    /**
     * Processes request to set up an Initial Public Offer.
     * @param ipo the initial public offer details for a client company
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the
     * notification
     * @return response to the initial public offer request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response setupInitialPublicOffer_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "initialpublicoffer") InitialPublicOffer ipo,
            @WebParam(name = "authenticator") String authenticator);
    
    /**
     * Processes request to setup Initial public offer that has been saved to file with the notificationCode.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the setup initial public offer request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response setupInitialPublicOffer_Authorise(@WebParam(name = "login", header = true) Login login, @WebParam(name = "notificationCode") String notificationCode);
    
    /**
     * Request to create a bond offer.
     * @param login The user's login details
     * @param authenticator The authenticator user meant to receive the
     * notification
     * @param bond The bond details to be created
     * @return response to the create bond offer request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response setupBondOffer_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "authenticator") String authenticator,
            @WebParam(name = "bond") BondOffer bond);
    
    /**
     * Processes saved request to create bond offer.
     * @param login The user's login details
     * @param notificationCode The notification code
     * @return response to request to create bond offer
     */
    @WebMethod
    @WebResult(name = "response")
    public Response setupBondOffer_Authorise(@WebParam(name = "login", header = true) Login login, @WebParam(name = "notificationCode") String notificationCode);
    
    /**
     * Request to create a Private Placement.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the
     * notification
     * @param privatePlacement the private placement details to be processed
     * @return response object back to sender indicating creation request status
     */
    @WebMethod
    @WebResult(name = "response")
    public Response setupPrivatePlacement_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "authenticator") String authenticator,
            @WebParam(name = "privatePlacement") PrivatePlacement privatePlacement);
    
    /**
     * Processes saved request to create private placement.
     * @param login The user's login details
     * @param notificationCode The notification code
     * @return Response to the create private placement request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response setupPrivatePlacement_Authorise(@WebParam(name = "login", header = true) Login login, @WebParam(name = "notificationCode") String notificationCode);
    
}
