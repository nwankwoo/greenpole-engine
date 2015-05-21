/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.service.implementation;

import javax.jws.WebService;
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
import org.greenpole.logic.HolderComponentLogic;
import org.greenpole.service.HolderComponentService;

/**
 *
 * @author Akin
 * Web service implementation for holder component.
 */
@WebService(serviceName = "holdercomponentservice", endpointInterface = "org.greenpole.service.HolderComponentService")
public class HolderComponentServiceImpl implements HolderComponentService {
    private final HolderComponentLogic request = new HolderComponentLogic();

    @Override
    public Response mergeHolderAccounts_Request(Login login, String authenticator, HolderMerger accountsToMerge) {
        return request.mergeHolderAccounts_Request(login, authenticator, accountsToMerge);
    }

    @Override
    public Response mergeHolderAccounts_Authorise(Login login, String notificationCode) {
        return request.mergeHolderAccounts_Authorise(login, notificationCode);
    }

    @Override
    public Response demergeHolderAccounts_Request(Login login, String authenticator, HolderMerger accountsToDemerge) {
        return request.demergeHolderAccounts_Request(login, authenticator, accountsToDemerge);
    }

    @Override
    public Response demergeHolderAccounts_Authorise(Login login, String notificationCode) {
        return request.demergeHolderAccounts_Authorise(login, notificationCode);
    }

    @Override
    public Response transferShareUnitManual_Request(Login login, String authenticator, UnitTransfer unitTransfer) {
        return request.transferShareUnitManual_Request(login, authenticator, unitTransfer);
    }

    @Override
    public Response transferShareUnitManual_Authorise(Login login, String notificationCode) {
        return request.transferShareUnitManual_Authorise(login, notificationCode);
    }

    @Override
    public Response transferBondUnitManual_Request(Login login, String authenticator, UnitTransfer unitTransfer) {
        return request.transferBondUnitManual_Request(login, authenticator, unitTransfer);
    }

    @Override
    public Response transferBondUnitManual_Authorise(Login login, String notificationCode) {
        return request.transferBondUnitManual_Authorise(login, notificationCode);
    }

    @Override
    public Response viewHolderChanges_Request(Login login, QueryHolderChanges queryParams) {
        return request.viewHolderChanges_Request(login, queryParams);
    }

    @Override
    public Response queryHolder_Request(Login login, QueryHolder queryParams) {
        return request.queryHolder_Request(login, queryParams);
    }

    @Override
    public Response createAdministrator_Request(Login login, String authenticator, Holder holder) {
        return request.createAdministrator_Request(login, authenticator, holder);
    }

    @Override
    public Response createAdministrator_Authorise(Login login, String notificationCode) {
        return request.createAdministrator_Authorise(login, notificationCode);
    }

    @Override
    public Response uploadPowerOfAttorney_Request(Login login, String authenticator, PowerOfAttorney poa) {
        return request.uploadPowerOfAttorney_Request(login, authenticator, poa);
    }

    @Override
    public Response uploadPowerOfAttorney_Authorise(Login login, String notificationCode) {
        return request.uploadHolderSignature_Authorise(login, notificationCode);
    }

    @Override
    public Response queryPowerOfAttorney_Request(Login login, PowerOfAttorney queryParams) {
        return request.queryPowerOfAttorney_Request(login, queryParams);
    }

    @Override
    public Response queryAllPowerOfAttorney_Request(Login login, PowerOfAttorney queryParams) {
        return request.queryAllPowerOfAttorney_Request(login, queryParams);
    }

    @Override
    public Response storeShareholderNubanAccountNumber_Request(Login login, String authenticator, HolderCompanyAccount compAcct) {
        return request.storeShareholderNubanAccountNumber_Request(login, authenticator, compAcct);
    }

    @Override
    public Response addShareholderNubanAccountNumber_Authorise(Login login, String notificationCode) {
        return request.addShareholderNubanAccountNumber_Authorise(login, notificationCode);
    }

    @Override
    public Response storeBondholderNubanAccountNumber_Request(Login login, String authenticator, HolderBondAccount bondAcct) {
        return request.storeBondholderNubanAccountNumber_Request(login, authenticator, bondAcct);
    }

    @Override
    public Response storeBondholderNubanAccountNumber_Authorise(Login login, String notificationCode) {
        return request.storeBondholderNubanAccountNumber_Authorise(login, notificationCode);
    }

    @Override
    public Response createShareHolder_Request(Login login, String authenticator, Holder holder) {
        return request.createShareHolder_Request(login, authenticator, holder);
    }

    @Override
    public Response createShareHolder_Authorise(Login login, String notificationCode) {
        return request.createShareHolder_Authorise(login, notificationCode);
    }

    @Override
    public Response createBondHolderAccount_Request(Login login, String authenticator, Holder holder) {
        return request.createBondHolderAccount_Request(login, authenticator, holder);
    }

    @Override
    public Response createBondHolderAccount_Authorise(Login login, String notificationCode) {
        return request.createBondHolderAccount_Authorise(login, notificationCode);
    }

    @Override
    public Response uploadHolderSignature_Request(Login login, String authenticator, HolderSignature holderSig) {
        return request.uploadHolderSignature_Request(login, authenticator, holderSig);
    }

    @Override
    public Response uploadHolderSignature_Authorise(Login login, String notificationCode) {
        return request.uploadHolderSignature_Authorise(login, notificationCode);
    }

    @Override
    public Response queryHolderSignature_Request(Login login, String authenticator, HolderSignature queryParams) {
        return request.queryHolderSignature_Request(login, authenticator, queryParams);
    }

    @Override
    public Response transposeHolderName_Request(Login login, String authenticator, Holder holder) {
        return request.transposeHolderName_Request(login, authenticator, holder);
    }

    @Override
    public Response transposeHolderName_Authorise(Login login, String notificationCode) {
        return request.transposeHolderName_Authorise(login, notificationCode);
    }

    @Override
    public Response editHolderDetails_Request(Login login, String authenticator, Holder holder) {
        return request.editHolderDetails_Request(login, authenticator, holder);
    }

    @Override
    public Response editHolderDetails_Authorise(Login login, String notificationCode) {
        return request.editHolderDetails_Authorise(login, notificationCode);
    }

    @Override
    public Response viewAccountConsolidation_request(Login login, QueryHolderConsolidation queryParams) {
        return request.viewAccountConsolidation_request(login, queryParams);
    }
    
}
