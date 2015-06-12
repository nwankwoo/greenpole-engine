/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.service.implementation;

import java.util.List;
import javax.jws.WebService;
import org.greenpole.entity.model.Carrier;
import org.greenpole.entity.model.clientcompany.BondOffer;
import org.greenpole.entity.model.clientcompany.ClientCompany;
import org.greenpole.entity.model.clientcompany.InitialPublicOffer;
import org.greenpole.entity.model.clientcompany.PrivatePlacement;
import org.greenpole.entity.model.clientcompany.QueryClientCompany;
import org.greenpole.entity.model.clientcompany.ShareQuotation;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.logic.ClientCompanyComponentLogic;
import org.greenpole.security.SecurityCheck;
import org.greenpole.service.ClientCompanyComponentService;

/**
 *
 * @author Akin
 */
@WebService(serviceName = "clientcompanyservice", endpointInterface = "org.greenpole.service.ClientCompanyComponentService")
public class ClientCompanyComponentServiceImpl implements ClientCompanyComponentService {
    private final ClientCompanyComponentLogic request = new ClientCompanyComponentLogic();

    @Override
    public Response createClientCompany_Request(Login login, String authenticator, ClientCompany cc) {
        return request.createClientCompany_Request(login, authenticator, cc);
    }

    @Override
    public Response createClientCompany_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        return SecurityCheck.securityFailChecker(login, notificationCode, resp) ? resp : 
                request.createClientCompany_Authorise(login, notificationCode);
    }

    @Override
    public Response editClientCompany_Request(Login login, String authenticator, ClientCompany cc) {
        return request.editClientCompany_Request(login, authenticator, cc);
    }

    @Override
    public Response editClientCompany_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        return SecurityCheck.securityFailChecker(login, notificationCode, resp) ? resp : 
                request.editClientCompany_Authorise(login, notificationCode);
    }

    @Override
    public Response queryClientCompany_Request(Login login, QueryClientCompany queryParams) {
        return request.queryClientCompany_Request(login, queryParams);
    }

    @Override
    public Response uploadShareUnitQuotations_Request(Login login, String authenticator, Carrier shareQuotation) {
        List<ShareQuotation> quotations = (List<ShareQuotation>) shareQuotation.getCarriedList();
        return request.uploadShareUnitQuotations_Request(login, authenticator, quotations);
    }

    @Override
    public Response uploadShareUnitQuotations_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        return SecurityCheck.securityFailChecker(login, notificationCode, resp) ? resp : 
                request.uploadShareUnitQuotations_Authorise(login, notificationCode);
    }

    @Override
    public Response getShareUnitQuotations_request(Login login) {
        return request.getShareUnitQuotations_request(login);
    }

    @Override
    public Response setupInitialPublicOffer_Request(Login login, String authenticator, InitialPublicOffer ipo) {
        return request.setupInitialPublicOffer_Request(login, authenticator, ipo);
    }

    @Override
    public Response setupInitialPublicOffer_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        return SecurityCheck.securityFailChecker(login, notificationCode, resp) ? resp : 
                request.setupInitialPublicOffer_Authorise(login, notificationCode);
    }

    @Override
    public Response setupBondOffer_Request(Login login, String authenticator, BondOffer bond) {
        return request.setupBondOffer_Request(login, authenticator, bond);
    }

    @Override
    public Response setupBondOffer_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        return SecurityCheck.securityFailChecker(login, notificationCode, resp) ? resp : 
                request.setupBondOffer_Authorise(login, notificationCode);
    }

    @Override
    public Response setupPrivatePlacement_Request(Login login, String authenticator, PrivatePlacement privatePlacement) {
        return request.setupPrivatePlacement_Request(login, authenticator, privatePlacement);
    }

    @Override
    public Response setupPrivatePlacement_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        return SecurityCheck.securityFailChecker(login, notificationCode, resp) ? resp : 
                request.setupPrivatePlacement_Authorise(login, notificationCode);
    }

    @Override
    public Response queryAllClientCompanies_Request(Login login) {
        return request.queryAllClientCompanies_Request(login);
    }

    @Override
    public Response queryClientCompany_Single_Request(Login login, int clientCompanyId) {
        return request.queryClientCompany_Request(login, clientCompanyId);
    }

    @Override
    public Response queryBondOffer_Request(Login login, int bondOfferId) {
        return request.queryBondOffer_Request(login, bondOfferId);
    }
    
}
