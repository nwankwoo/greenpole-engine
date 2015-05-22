/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.service;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import org.greenpole.entity.model.clientcompany.UnitTransfer;
import org.greenpole.entity.model.holder.Holder;
import org.greenpole.entity.model.holder.HolderBondAccount;
import org.greenpole.entity.model.holder.HolderCompanyAccount;
import org.greenpole.entity.model.holder.HolderMerger;
import org.greenpole.entity.model.holder.HolderSignature;
import org.greenpole.entity.model.holder.PowerOfAttorney;
import org.greenpole.entity.model.holder.QueryHolder;
import org.greenpole.entity.model.holder.QueryHolderChanges;
import org.greenpole.entity.model.holder.QueryHolderConsolidation;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;

/**
 *
 * @author Akin
 * Web service for holder component.
 */
@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface HolderComponentService {
    /**
     * Request to merge a holder account / multiple holder accounts to a primary holder account.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param accountsToMerge the merge holder accounts object containing merge detail
     * @return response to the merge holder accounts request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response mergeHolderAccounts_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "authenticator") String authenticator,
            @WebParam(name = "accountsToMerge") HolderMerger accountsToMerge);
    
    /**
     * Processes a saved request to merge holder accounts, according to a specified notification code.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the merge holder accounts request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response mergeHolderAccounts_Authorise(@WebParam(name = "login", header = true) Login login, @WebParam(name = "notificationCode") String notificationCode);
    
    /**
     * Request to demerge multiple holder accounts from a primary holder account.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param accountsToDemerge the merge holder accounts object containing demerge details
     * @return response to the demerge holder accounts request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response demergeHolderAccounts_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "authenticator") String authenticator,
            @WebParam(name = "accountsToDemerge") HolderMerger accountsToDemerge);
    
    /**
     * Processes a saved request to demerge holder accounts, according to a specified notification code.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the demerge holder accounts request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response demergeHolderAccounts_Authorise(@WebParam(name = "login", header = true) Login login, @WebParam(name = "notificationCode") String notificationCode);
    
    /**
     * Request to transfer share units between holder company accounts.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param unitTransfer the unit transfer object containing transfer details
     * @return response to the transfer share unit request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response transferShareUnitManual_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "authenticator") String authenticator,
            @WebParam(name = "unitTransfer") UnitTransfer unitTransfer);
    
    /**
     * Processes a saved request to transfer share units between holder company accounts.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the transfer share unit request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response transferShareUnitManual_Authorise(@WebParam(name = "login", header = true) Login login, @WebParam(name = "notificationCode") String notificationCode);
    
    /**
     * Request to transfer bond units between holder bond accounts.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param unitTransfer the unit transfer object containing transfer details
     * @return response to the transfer bond unit request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response transferBondUnitManual_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "authenticator") String authenticator,
            @WebParam(name = "unitTransfer") UnitTransfer unitTransfer);
    
    /**
     * Processes a saved request to transfer bond units between holder bond accounts.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the transfer bond unit request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response transferBondUnitManual_Authorise(@WebParam(name = "login", header = true) Login login, @WebParam(name = "notificationCode") String notificationCode);
    
    /**
     * Request to view changes to holder accounts.
     * @param login the user's login details
     * @param queryParams the query parameters
     * @return the response to the holder changes request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response viewHolderChanges_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "queryParams") QueryHolderChanges queryParams);
    
    /**
     * Searches for a list of holders according to query parameters.
     * @param login the user's login details
     * @param queryParams the query parameters
     * @return the response to the holder query request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response queryHolder_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "queryParams") QueryHolder queryParams);
    
    /**
     * Request to create administrators for a holder.
     * @param login used to get the userId that is performing this transaction
     * @param authenticator the super user to accept the creation of this
     * request
     * @param holder the holder to create administrator(s) for
     * @return response to the create administrator request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response createAdministrator_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "authenticator") String authenticator,
            @WebParam(name = "holder") Holder holder);
    
    /**
     * Processes a saved request to create administrators for a holder.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the create administrator request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response createAdministrator_Authorise(@WebParam(name = "login", header = true) Login login, @WebParam(name = "notificationCode") String notificationCode);
    
    /**
     * Request to upload power of attorney for a holder.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param poa the power of attorney to be uploaded
     * @return response to the upload power of attorney request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response uploadPowerOfAttorney_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "authenticator") String authenticator,
            @WebParam(name = "powerofattorney") PowerOfAttorney poa);
    
    /**
     * Processes a saved request to upload power of attorney
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the upload power of attorney request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response uploadPowerOfAttorney_Authorise(@WebParam(name = "login", header = true) Login login, @WebParam(name = "notificationCode") String notificationCode);
    
    /**
     * Searches for a specific power of attorney for a holder.
     * @param login the user's login details
     * @param queryParams the query parameters
     * @return response to the query power of attorney
     */
    @WebMethod
    @WebResult(name = "response")
    public Response queryPowerOfAttorney_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "queryParams") PowerOfAttorney queryParams);
    
    /**
     * Searches for all powers of attorney for a specific holder
     * @param login the user's login details
     * @param queryParams the query parameters
     * @return response to the query all power of attorney request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response queryAllPowerOfAttorney_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "queryParams") PowerOfAttorney queryParams);
    
    /**
     * Request to store a NUBAN account in a shareholder's company account.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param compAcct the shareholder's company account
     * @return response to the store NUBAN account request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response storeShareholderNubanAccountNumber_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "authenticator") String authenticator,
            @WebParam(name = "compAcct") HolderCompanyAccount compAcct);
    
    /**
     * Processes the saved request to store a NUBAN account in a shareholder's company account.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the store NUBAN account request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response storeShareholderNubanAccountNumber_Authorise(@WebParam(name = "login", header = true) Login login, @WebParam(name = "notificationCode") String notificationCode);
    
    /**
     * Request to store a NUBAN account in a bond holder's company account.
     * @param login the user's login details
     * @param authenticator the authenticator user meant to receive the notification
     * @param bondAcct the bond holder's company account
     * @return response to the store NUBAN account request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response storeBondholderNubanAccountNumber_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "authenticator") String authenticator,
            @WebParam(name = "bondAcct") HolderBondAccount bondAcct);
    
    /**
     * Processes the saved request to store a NUBAN account in a bond holder's company account.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the store NUBAN account request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response storeBondholderNubanAccountNumber_Authorise(@WebParam(name = "login", header = true) Login login, @WebParam(name = "notificationCode") String notificationCode);
    
    /**
     * Request to create a holder account.
     * @param login The user's login details
     * @param authenticator The authenticator user meant to receive the
     * notification
     * @param holder Object representing holder details
     * @return Response to create holder account request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response createShareHolder_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "authenticator") String authenticator,
            @WebParam(name = "holder") Holder holder);
    
    /**
     * Processes saved request to create holder account.
     * @param login The user's login details
     * @param notificationCode The notification code
     * @return response to holder account creation request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response createShareHolder_Authorise(@WebParam(name = "login", header = true) Login login, @WebParam(name = "notificationCode") String notificationCode);
    
    /**
     * Request to create bond holder account.
     * @param login user's login details
     * @param authenticator the authenticator user meant to receive the
     * notification
     * @param holder the holder details object
     * @return response to the bondholder account creation request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response createBondHolderAccount_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "authenticator") String authenticator,
            @WebParam(name = "holder") Holder holder);
    
    /**
     * Processes saved request to create bondholder account.
     * @param login user's login details
     * @param notificationCode the notification code
     * @return response to the bondholder account creation request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response createBondHolderAccount_Authorise(@WebParam(name = "login", header = true) Login login, @WebParam(name = "notificationCode") String notificationCode);
    
    /**
     * Request to upload a holder signature.
     * @param login The user's login details
     * @param authenticator The authenticator meant to receive the notification
     * @param holderSig Holder signature details
     * @return response to the upload holder signature request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response uploadHolderSignature_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "authenticator") String authenticator,
            @WebParam(name = "holderSig") HolderSignature holderSig);
    
    /**
     * Processes the saved request to upload holder signature.
     * @param login user's login details
     * @param notificationCode the notification code
     * @return response to the upload holder signature request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response uploadHolderSignature_Authorise(@WebParam(name = "login", header = true) Login login, @WebParam(name = "notificationCode") String notificationCode);
    
    /**
     * Request to query holder signature.
     * @param login user's login details
     * @param authenticator the authenticator user meant to receive the
     * notification details
     * @param queryParams the query parameters
     * @return response to query holder signature request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response queryHolderSignature_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "authenticator") String authenticator,
            @WebParam(name = "queryParams") HolderSignature queryParams);
    
    /**
     * Request to transpose holder name.
     * @param login user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param holder the holder detail
     * @return response to the transpose name request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response transposeHolderName_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "authenticator") String authenticator,
            @WebParam(name = "holder") Holder holder);
    
    /**
     * Processes request to transpose holder names.
     * @param login user's login details
     * @param notificationCode the notification code
     * @return response object for the authorised request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response transposeHolderName_Authorise(@WebParam(name = "login", header = true) Login login, @WebParam(name = "notificationCode") String notificationCode);
    
    /**
     * Request to edit holder details.
     * @param login user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param holder the edited holder details
     * @return response to the edit holder details request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response editHolderDetails_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "authenticator") String authenticator,
            @WebParam(name = "holder") Holder holder);
    
    /**
     * Processes saved request to edit holder details.
     * @param login user's login details
     * @param notificationCode the notification code
     * @return response to the edit holder request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response editHolderDetails_Authorise(@WebParam(name = "login", header = true) Login login, @WebParam(name = "notificationCode") String notificationCode);
    
    /**
     * Request to view report on consolidation of Shareholder Accounts
     * @param login the user Id of the user performing the view request
     * @param queryParams the query parameters
     * @return response to the view report on shareholder accounts consolidation request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response viewAccountConsolidation_request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "queryParams") QueryHolderConsolidation queryParams);
    
    /**
     * Request to apply for a bond offer.
     * @param login the user's login details
     * @param authenticator the authenticator meant to receive the notification
     * @param bondAccount the bond account containing the bond application
     * @return response to the bond offer application request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response applyForBondOffer_Request(@WebParam(name = "login", header = true) Login login, @WebParam(name = "authenticator") String authenticator,
            @WebParam(name = "bondAccount") HolderBondAccount bondAccount);
    
    /**
     * Processes a saved request to apply for a bond offer.
     * @param login the user's login details
     * @param notificationCode the notification code
     * @return response to the bond offer application request
     */
    @WebMethod
    @WebResult(name = "response")
    public Response applyForBondOffer_Authorise(@WebParam(name = "login", header = true) Login login, @WebParam(name = "notificationCode") String notificationCode);
}
