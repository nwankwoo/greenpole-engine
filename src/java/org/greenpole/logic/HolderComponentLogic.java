/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.logic;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.greenpole.entity.model.Address;
import org.greenpole.entity.model.EmailAddress;
import org.greenpole.entity.model.PhoneNumber;
import org.greenpole.entity.model.holder.Holder;
import org.greenpole.entity.model.holder.HolderBondAccount;
import org.greenpole.entity.model.holder.HolderChanges;
import org.greenpole.entity.model.holder.HolderCompanyAccount;
import org.greenpole.entity.model.holder.QueryHolder;
import org.greenpole.entity.model.holder.QueryHolderChanges;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.hibernate.entity.Bank;
import org.greenpole.hibernate.entity.BondOffer;
import org.greenpole.hibernate.entity.ClientCompany;
import org.greenpole.hibernate.entity.HolderBondAccountId;
import org.greenpole.hibernate.entity.HolderChangeType;
import org.greenpole.hibernate.entity.HolderCompanyAccountId;
import org.greenpole.hibernate.entity.HolderEmailAddress;
import org.greenpole.hibernate.entity.HolderEmailAddressId;
import org.greenpole.hibernate.entity.HolderPhoneNumber;
import org.greenpole.hibernate.entity.HolderPhoneNumberId;
import org.greenpole.hibernate.entity.HolderPostalAddress;
import org.greenpole.hibernate.entity.HolderPostalAddressId;
import org.greenpole.hibernate.entity.HolderResidentialAddress;
import org.greenpole.hibernate.entity.HolderResidentialAddressId;
import org.greenpole.hibernate.query.HolderComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.util.Descriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Akinwale Agbaje
 * @version 1.0
 * Business requirement implementations to do with client companies
 */
public class HolderComponentLogic {
    private final HolderComponentQuery hq = ComponentQueryFactory.getHolderComponentQuery();
    private static final Logger logger = LoggerFactory.getLogger(HolderComponentLogic.class);
    
    /**
     * Request to view changes to holder accounts.
     * @param login the user's login details
     * @param queryParams the query parameters
     * @return the response to the holder changes request
     */
    public Response viewHolderChanges_Request(Login login, QueryHolderChanges queryParams) {
        Response resp = new Response();
        logger.info("request to query holder changes, invoked by [{}]", login.getUserId());
        
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        
        Map<String, String> descriptors = Descriptor.decipherDescriptor(queryParams.getDescriptor());
        org.greenpole.hibernate.entity.HolderChanges changes_hib = new org.greenpole.hibernate.entity.HolderChanges();
        HolderChangeType change_type_hib = new HolderChangeType();
        
        if (descriptors.size() == 1) {
            //check that start date is properly formatted
            if (!descriptors.get("date").equalsIgnoreCase("none")) {
                try {
                    formatter.parse(queryParams.getStart_date());
                } catch (ParseException ex) {
                    logger.info("an error was thrown while checking the start date. See error log");
                    resp.setRetn(300);
                    resp.setDesc("Incorrect date format for start date");
                    logger.error("Incorrect date format for start date", ex);
                    
                    return resp;
                }
            }
            
            //check that start and end dates are properly formatted
            if (descriptors.get("date").equalsIgnoreCase("between")) {
                try {
                    formatter.parse(queryParams.getStart_date());
                } catch (ParseException ex) {
                    logger.info("an error was thrown while checking the start date. See error log");
                    resp.setRetn(300);
                    resp.setDesc("Incorrect date format for start date");
                    logger.error("Incorrect date format for start date", ex);
                    
                    return resp;
                }
                try {
                    formatter.parse(queryParams.getEnd_date());
                } catch (ParseException ex) {
                    logger.info("an error was thrown while checking the end date. See error log");
                    resp.setRetn(300);
                    resp.setDesc("Incorrect date format for end date");
                    logger.error("Incorrect date format for end date", ex);
                    
                    return resp;
                }
            }

            change_type_hib.setId(queryParams.getHolderChanges().getChangeTypeId());
            changes_hib.setHolderChangeType(change_type_hib);

            List<org.greenpole.hibernate.entity.HolderChanges> changes_hib_result = hq.queryHolderChanges(queryParams.getDescriptor(), changes_hib, queryParams.getStart_date(), queryParams.getEnd_date());
            List<HolderChanges> return_list = new ArrayList<>();
            
            //unwrap returned result list
            for (org.greenpole.hibernate.entity.HolderChanges hc : changes_hib_result) {
                org.greenpole.hibernate.entity.Holder holder_hib = hq.getHolder(hc.getHolder().getId()); //all variables of holder
                HolderChanges hc_model = new HolderChanges();
                Holder holder_model = new Holder();
                
                holder_model.setHolderId(holder_hib.getId());
                holder_model.setFirstName(holder_hib.getFirstName());
                holder_model.setLastName(holder_hib.getLastName());
                
                hc_model.setCurrentForm(hc.getCurrentForm());
                hc_model.setInitialForm(hc.getInitialForm());
                hc_model.setChangeTypeId(hc.getHolderChangeType().getId());
                hc_model.setHolder(holder_model); //holder object
                
                return_list.add(hc_model);
            }
            logger.info("holder changes query successful");
            resp.setRetn(0);
            resp.setDesc("Successful");
            resp.setBody(return_list);
            
            return resp;
        }
        
        logger.info("descriptor length does not match expected required length");
        resp.setRetn(300);
        resp.setDesc("descriptor length does not match expected required length");

        return resp;
    }
    
    /**
     * Searches for a list of holders according to query parameters.
     * @param login the user's login details
     * @param queryParams the query parameters
     * @return the response to the holder query request
     */
    public Response queryHolder_Request(Login login, QueryHolder queryParams) {
        Response resp = new Response();
        logger.info("request to query holder, invoked by [{}]", login.getUserId());
        
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        
        Map<String, String> descriptors = Descriptor.decipherDescriptor(queryParams.getDescriptor());
        
        if (descriptors.size() == 3) {
            String descriptor = queryParams.getDescriptor();
            
            org.greenpole.hibernate.entity.Holder h_hib_search = new org.greenpole.hibernate.entity.Holder();
            
            HolderResidentialAddress h_res_hib_search = new HolderResidentialAddress();
            HolderResidentialAddressId h_res_id_hib_search = new HolderResidentialAddressId();
            
            HolderPostalAddress h_pos_hib_search = new HolderPostalAddress();
            HolderPostalAddressId h_pos_id_hib_search = new HolderPostalAddressId();
            
            HolderEmailAddress h_email_hib_search = new HolderEmailAddress();
            HolderEmailAddressId h_email_id_hib_search = new HolderEmailAddressId();
            
            HolderPhoneNumber h_phone_hib_search = new HolderPhoneNumber();
            HolderPhoneNumberId h_phone_id_hib_search = new HolderPhoneNumberId();
            
            org.greenpole.hibernate.entity.HolderCompanyAccount hca_hib_search = new org.greenpole.hibernate.entity.HolderCompanyAccount();
            HolderCompanyAccountId hca_id_hib_search = new HolderCompanyAccountId();
            ClientCompany cc_hib_search = new ClientCompany();
            
            org.greenpole.hibernate.entity.HolderBondAccount hba_hib_search = new org.greenpole.hibernate.entity.HolderBondAccount();
            HolderBondAccountId hba_id_hib_search = new HolderBondAccountId();
            
            org.greenpole.hibernate.entity.Stockbroker broker_hib_search = new org.greenpole.hibernate.entity.Stockbroker();
            
            Holder h_model_search;
            if (queryParams.getHolder() != null) {
                h_model_search = queryParams.getHolder();
                
                h_hib_search.setFirstName(h_model_search.getFirstName());
                h_hib_search.setMiddleName(h_model_search.getMiddleName());
                h_hib_search.setLastName(h_model_search.getLastName());
                h_hib_search.setGender(h_model_search.getGender());
                try {
                    h_hib_search.setDob(formatter.parse(h_model_search.getDob()));
                } catch (ParseException ex) {
                    logger.info("an error was thrown while checking the holder's date of birth. See error log");
                    resp.setRetn(301);
                    resp.setDesc("Incorrect date format for date of birth");
                    logger.error("Incorrect date format for date of birth", ex);
                }
                h_hib_search.setChn(h_model_search.getChn());
                h_hib_search.setHolderAcctNumber(h_model_search.getHolderAcctNumber());
                h_hib_search.setTaxExempted(h_model_search.isTaxExempted());
                h_hib_search.setPryAddress(h_model_search.getPryAddress());
                h_hib_search.setType(h_model_search.getType());
                h_hib_search.setPryHolder(true); //must be set
            }

            Address h_res_model_search;
            if (!queryParams.getHolder().getHolderResidentialAddresses().isEmpty()) {
                h_res_model_search = queryParams.getHolder().getHolderResidentialAddresses().get(0);
                
                h_res_id_hib_search.setAddressLine1(h_res_model_search.getAddressLine1());
                h_res_id_hib_search.setState(h_res_model_search.getState());
                h_res_id_hib_search.setCountry(h_res_model_search.getCountry());
                
                h_res_hib_search.setAddressLine2(h_res_model_search.getAddressLine2());
                h_res_hib_search.setAddressLine3(h_res_model_search.getAddressLine3());
                h_res_hib_search.setAddressLine4(h_res_model_search.getAddressLine4());
                h_res_hib_search.setCity(h_res_model_search.getCity());
                h_res_hib_search.setPostCode(h_res_model_search.getPostCode());
                h_res_hib_search.setIsPrimary(h_res_model_search.isPrimaryAddress());
                
                h_res_hib_search.setId(h_res_id_hib_search);
                
                Set h_res_hib_set = new HashSet();
                h_res_hib_set.add(h_res_hib_search);
                
                h_hib_search.setHolderResidentialAddresses(h_res_hib_set);
            }

            Address h_pos_model_search;
            if (!queryParams.getHolder().getHolderPostalAddresses().isEmpty()) {
                h_pos_model_search = queryParams.getHolder().getHolderPostalAddresses().get(0);
                
                h_pos_id_hib_search.setAddressLine1(h_pos_model_search.getAddressLine1());
                h_pos_id_hib_search.setState(h_pos_model_search.getState());
                h_pos_id_hib_search.setCountry(h_pos_model_search.getCountry());
                
                h_pos_hib_search.setAddressLine2(h_pos_model_search.getAddressLine2());
                h_pos_hib_search.setAddressLine3(h_pos_model_search.getAddressLine3());
                h_pos_hib_search.setAddressLine4(h_pos_model_search.getAddressLine4());
                h_pos_hib_search.setCity(h_pos_model_search.getCity());
                h_pos_hib_search.setPostCode(h_pos_model_search.getPostCode());
                h_pos_hib_search.setIsPrimary(h_pos_model_search.isPrimaryAddress());
                
                h_pos_hib_search.setId(h_pos_id_hib_search);
                
                Set h_pos_hib_set = new HashSet();
                h_pos_hib_set.add(h_res_hib_search);
                
                h_hib_search.setHolderPostalAddresses(h_pos_hib_set);
            }

            EmailAddress h_email_model_search;
            if (!queryParams.getHolder().getHolderEmailAddresses().isEmpty()) {
                h_email_model_search = queryParams.getHolder().getHolderEmailAddresses().get(0);
                
                h_email_id_hib_search.setEmailAddress(h_email_model_search.getEmailAddress());
                
                h_email_hib_search.setIsPrimary(h_email_model_search.isPrimaryEmail());
                
                h_email_hib_search.setId(h_email_id_hib_search);
                
                Set h_email_hib_set = new HashSet();
                h_email_hib_set.add(h_email_hib_search);
                
                h_hib_search.setHolderEmailAddresses(h_email_hib_set);
            }

            PhoneNumber h_phone_model_search;
            if (!queryParams.getHolder().getHolderPhoneNumbers().isEmpty()) {
                h_phone_model_search = queryParams.getHolder().getHolderPhoneNumbers().get(0);
                
                h_phone_id_hib_search.setPhoneNumber(h_phone_model_search.getPhoneNumber());
                
                h_phone_hib_search.setIsPrimary(h_phone_model_search.isPrimaryPhoneNumber());
                
                h_phone_hib_search.setId(h_phone_id_hib_search);
                
                Set h_phone_hib_set = new HashSet();
                h_phone_hib_set.add(h_email_hib_search);
                
                h_hib_search.setHolderPhoneNumbers(h_phone_hib_set);
            }
            
            HolderCompanyAccount hca_model_search;
            if (!queryParams.getHolder().getHolderCompanyAccounts().isEmpty()) {
                hca_model_search = queryParams.getHolder().getHolderCompanyAccounts().get(0);
                
                hca_id_hib_search.setClientCompanyId(hca_model_search.getClientCompanyId());
                
                hca_hib_search.setEsop(hca_model_search.isEsop());
                
                hca_hib_search.setId(hca_id_hib_search);
                
                Set hca_hib_set = new HashSet();
                hca_hib_set.add(hca_hib_search);
                
                h_hib_search.setHolderCompanyAccounts(hca_hib_set);
            }
            
            HolderBondAccount hba_model_search;
            if (!queryParams.getHolder().getHolderBondAccounts().isEmpty()) {
                hba_model_search = queryParams.getHolder().getHolderBondAccounts().get(0);
                
                hba_id_hib_search.setBondOfferId(hba_model_search.getBondOfferId());
                
                hba_hib_search.setId(hba_id_hib_search);
                
                Set hba_hib_set = new HashSet();
                hba_hib_set.add(hba_hib_search);
                
                h_hib_search.setHolderBondAccounts(hba_hib_set);
            }

            Map<String, Integer> shareUnits_search;
            if (!queryParams.getShareUnits().isEmpty()) {
                shareUnits_search = queryParams.getShareUnits();
            } else {
                shareUnits_search = new HashMap<>();
            }

            Map<String, Integer> totalHoldings_search;
            if (!queryParams.getTotalHoldings().isEmpty()) {
                totalHoldings_search = queryParams.getTotalHoldings();
            } else {
                totalHoldings_search = new HashMap<>();
            }

            List<org.greenpole.hibernate.entity.Holder> h_search_result = hq.queryHolderAccount(descriptor, h_hib_search, shareUnits_search, totalHoldings_search);
            
            //unwrap result and set in holder front-end model
            List<Holder> h_model_out = new ArrayList<>();
            
            List<Address> h_res_out = new ArrayList<>();
            List<Address> h_pos_out = new ArrayList<>();
            List<PhoneNumber> h_phone_out = new ArrayList<>();
            List<EmailAddress> h_email_out = new ArrayList<>();
            
            for (org.greenpole.hibernate.entity.Holder h_hib_out : h_search_result) {
                Holder h = new Holder();
                
                h.setFirstName(h_hib_out.getFirstName());
                h.setMiddleName(h_hib_out.getMiddleName());
                h.setLastName(h_hib_out.getLastName());
                h.setGender(h_hib_out.getGender());
                h.setDob(formatter.format(h_hib_out.getDob()));
                h.setChn(h_hib_out.getChn());
                h.setHolderAcctNumber(h_hib_out.getHolderAcctNumber());
                h.setTaxExempted(h_hib_out.isTaxExempted());
                h.setPryAddress(h_hib_out.getPryAddress());
                h.setType(h_hib_out.getType());
                h.setPryHolder(h_hib_out.isPryHolder());
                
                //get all available addresses, email addresses and phone numbers
                List<HolderResidentialAddress> res_hib_list = hq.getHolderResidentialAddress(h_hib_out.getId());
                for (HolderResidentialAddress res_hib_out : res_hib_list) {
                    HolderResidentialAddressId res_id_hib_out = res_hib_out.getId();
                    
                    Address addy_model = new Address();
                    
                    addy_model.setAddressLine1(res_id_hib_out.getAddressLine1());
                    addy_model.setState(res_id_hib_out.getState());
                    addy_model.setCountry(res_id_hib_out.getCountry());
                    addy_model.setAddressLine2(res_hib_out.getAddressLine2());
                    addy_model.setAddressLine3(res_hib_out.getAddressLine3());
                    addy_model.setAddressLine4(res_hib_out.getAddressLine4());
                    addy_model.setPostCode(res_hib_out.getPostCode());
                    addy_model.setCity(res_hib_out.getCity());
                    addy_model.setPrimaryAddress(res_hib_out.isIsPrimary());
                }
            }
        }
        //failed response here
        
        return null;
    }
}
