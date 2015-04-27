/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

import java.util.ArrayList;
import java.util.List;
import org.greenpole.hibernate.entity.Holder;
import org.greenpole.entrycode.emmanuel.model.PowerOfAttorney;
import org.greenpole.hibernate.entity.Administrator;
import org.greenpole.hibernate.entity.AdministratorEmailAddress;
import org.greenpole.hibernate.entity.AdministratorPhoneNumber;
import org.greenpole.hibernate.entity.AdministratorResidentialAddress;
import org.greenpole.hibernate.entity.HolderBondAccount;
import org.greenpole.hibernate.entity.HolderCompanyAccount;
import org.greenpole.hibernate.query.GeneralisedAbstractDao;

/**
 *
 * @author user
 */
public class HibernateDummyQuery extends GeneralisedAbstractDao implements HibernatDummyQuerInterface {
    @Override
    public void createAdministratorForShareHolderAndBondHolder(Administrator admin, List<AdministratorEmailAddress> emailAddress, List<AdministratorPhoneNumber> phoneNumber, List<AdministratorResidentialAddress> residentialAddress, List<Holder> holder){
    }
    @Override
    public void createEmail(AdministratorEmailAddress emailAddress){
    }

    /**
     *
     * @param phoneNumber
     */
    @Override
    public void createPhoneNumber(AdministratorPhoneNumber phoneNumber){  
    }
    @Override
    public void createAdministratorResidentialAddress(AdministratorResidentialAddress adminResidentialAddress){
    }
    @Override
     public void updateAdministrationHolderCompanyAccount(org.greenpole.hibernate.entity.Holder holder){
     }
    @Override
     public Holder retrieveHolderObject(int holderId){
     Holder holder = new Holder();
     return holder;
     }
    @Override
     public void uploadPowerOfAttorney(PowerOfAttorney power){
     }
    @Override
     public String checkHolderNubanNumber(String nubanAccount){
     return " ";
     }
    @Override
    public List<HolderCompanyAccount> getAllShareholderNubanAccounts(){
        List<org.greenpole.hibernate.entity.HolderCompanyAccount> holderAccount = new ArrayList();
        return holderAccount;
    }
    @Override
    public List<HolderBondAccount> getAllBondholderNubanAccounts(){
    List<HolderBondAccount> bondHolder = new ArrayList();
    return bondHolder;
    }
    @Override
    public void addShareholderNubanAccount(){
    }
    @Override
    public void createNubanAccount(HolderCompanyAccount holderAccount){
    }
    @Override
    public void createBondNubanAccount(HolderBondAccount holderAccount){
    }
    @Override
    public void changeShareholderNubanAccount(HolderCompanyAccount holderAccount){
    
    }
    public void changeBondholderNubanAccount(HolderBondAccount bondholderAccount){
    
    }
}
