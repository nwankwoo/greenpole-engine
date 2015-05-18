/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

import java.util.ArrayList;
import java.util.List;
import org.greenpole.entity.security.Login;
import org.greenpole.hibernate.entity.Holder;
import org.greenpole.entity.model.holder.PowerOfAttorney;
import org.greenpole.hibernate.entity.AccountConsolidation;
import org.greenpole.hibernate.entity.Administrator;
import org.greenpole.hibernate.entity.AdministratorEmailAddress;
import org.greenpole.hibernate.entity.AdministratorPhoneNumber;
import org.greenpole.hibernate.entity.AdministratorResidentialAddress;
import org.greenpole.hibernate.entity.CompanyAccountConsolidation;
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
     public boolean checkHolderNubanNumber(String nubanAccount){
         boolean bool = false;
     return bool;
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
    @Override
    public void changeBondholderNubanAccount(HolderBondAccount bondholderAccount){
    
    }
    /**
     * @param holderId
     * @param clientCompanyId
     * @return 
    @Override
    public org.greenpole.hibernate.entity.HolderChanges getHolderEditedDetails(int holderId){
    return null;
    }
    @Override
    public org.greenpole.hibernate.entity.HolderChanges retrieveHolderChangesQueryOne(String changeType, String changeDate, int holderId){
    return null;
    }
    @Override
    public org.greenpole.hibernate.entity.HolderChanges retrieveHolderChangesQueryTwo(String changeType, String changeDate1, String changeDate2, int holderId){
    return null;
    }
*/
    @Override
    public HolderCompanyAccount retrieveHolderCompanyAccount(int holderId, int clientCompanyId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public HolderBondAccount retrieveHolderBondCompAccount(int holderId, int bondId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<CompanyAccountConsolidation> queryAccountConsolidation(String descriptor, CompanyAccountConsolidation compAccCon, String start_date, String end_date) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Login> getUserList(List<Login> login) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<AccountConsolidation> queryAccCon(int holderId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
/**
 * finds power of attorney with the specified holder ID and updates the isPrimaryPowerOfAttorney
 * to false 
 * @param holderId the holder Id whose power of attorney is to be updated.
 * @return boolean object
 */
    @Override
    public boolean updatePowerOfAttorneyStatus(int holderId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
/**
 * retrieves the power of attorney for the specified holder
 * @param holderId the Id of the holder's power of attorney to be retrieved
 * @return the hibernate entity object.
 */
    @Override
    public org.greenpole.hibernate.entity.PowerOfAttorney retrieveCurrentPowerOfAttorney(int holderId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

   

    
    
}
