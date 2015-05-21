/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.service.implementation;

import java.util.List;
import javax.jws.WebService;
import org.greenpole.entity.model.clientcompany.BondOffer;
import org.greenpole.entity.model.clientcompany.ClientCompany;
import org.greenpole.entity.model.clientcompany.InitialPublicOffer;
import org.greenpole.entity.model.clientcompany.PrivatePlacement;
import org.greenpole.entity.model.clientcompany.QueryClientCompany;
import org.greenpole.entity.model.clientcompany.ShareQuotation;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.logic.ClientCompanyComponentLogic;
import org.greenpole.service.ClientCompanyComponentService;

/**
 *
 * @author Akin
 */
@WebService(serviceName = "clientcompanycomponentservice", endpointInterface = "org.greenpole.service.ClientCompanyComponentService")
public class ClientCompanyComponentServiceImpl implements ClientCompanyComponentService {
    private final ClientCompanyComponentLogic request = new ClientCompanyComponentLogic();

    @Override
    public Response createClientCompany_Request(Login login, String authenticator, ClientCompany cc) {
        return request.createClientCompany_Request(login, authenticator, cc);
    }

    @Override
    public Response createClientCompany_Authorise(Login login, String notificationCode) {
        return request.createClientCompany_Authorise(login, notificationCode);
    }

    @Override
    public Response editClientCompany_Request(Login login, String authenticator, ClientCompany cc) {
        return request.editClientCompany_Request(login, authenticator, cc);
    }

    @Override
    public Response editClientCompany_Authorise(Login login, String notificationCode) {
        return request.editClientCompany_Authorise(login, notificationCode);
    }

    @Override
    public Response queryClientCompany_Request(Login login, QueryClientCompany queryParams) {
        return request.queryClientCompany_Request(login, queryParams);
    }

    @Override
    public Response uploadShareUnitQuotations_Request(Login login, String authenticator, List<ShareQuotation> shareQuotation) {
        return request.uploadShareUnitQuotations_Request(login, authenticator, shareQuotation);
    }

    @Override
    public Response uploadShareUnitQuotations_Authorise(Login login, String notificationCode) {
        return request.uploadShareUnitQuotations_Authorise(login, notificationCode);
    }

    @Override
    public Response getShareUnitQuotations_request(Login login) {
        return request.getShareUnitQuotations_request(login);
    }

    @Override
    public Response setupInitialPublicOffer_Request(Login login, InitialPublicOffer ipo, String authenticator) {
        return request.setupInitialPublicOffer_Request(login, ipo, authenticator);
    }

    @Override
    public Response setupInitialPublicOffer_Authorise(Login login, String notificationCode) {
        return request.setupInitialPublicOffer_Authorise(login, notificationCode);
    }

    @Override
    public Response createBondOffer_Request(Login login, String authenticator, BondOffer bond) {
        return request.createBondOffer_Request(login, authenticator, bond);
    }

    @Override
    public Response setupBondOffer_Authorise(Login login, String notificationCode) {
        return request.setupBondOffer_Authorise(login, notificationCode);
    }

    @Override
    public Response setupPrivatePlacement_Request(Login login, String authenticator, PrivatePlacement privatePlacement) {
        return request.setupPrivatePlacement_Request(login, authenticator, privatePlacement);
    }

    @Override
    public Response setupPrivatePlacement_Authorise(Login login, String notificationCode) {
        return request.setupPrivatePlacement_Authorise(login, notificationCode);
    }
    
}
