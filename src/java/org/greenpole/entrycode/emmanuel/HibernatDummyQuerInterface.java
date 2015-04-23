/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

import java.util.List;
import org.greenpole.hibernate.entity.Holder;
import org.greenpole.entrycode.emmanuel.model.PowerOfAttorney;
import org.greenpole.hibernate.entity.Administrator;
import org.greenpole.hibernate.entity.AdministratorEmailAddress;
import org.greenpole.hibernate.entity.AdministratorPhoneNumber;
import org.greenpole.hibernate.entity.AdministratorResidentialAddress;


/**
 *
 * @author user
 */
public interface HibernatDummyQuerInterface {

    /**
     *
     * @param admin
     * @param emailAddress
     * @param phoneNumber
     * @param residentialAddress 
     * @param holder 
     */
    public void createAdministratorForShareHolderAndBondHolder(Administrator admin, List<AdministratorEmailAddress> emailAddress, List<AdministratorPhoneNumber> phoneNumber, List<AdministratorResidentialAddress> residentialAddress, List<Holder> holder);

    public void createEmail(AdministratorEmailAddress emailAddress);

    public void createPhoneNumber(AdministratorPhoneNumber phoneNumber);

    public void createAdministratorResidentialAddress(AdministratorResidentialAddress adminResidentialAddress);
    
    //public boolean checkHolder(Holder holder);
    
    /**
     *
     * @param holder
     */
        
    public void updateAdministrationHolderCompanyAccount(org.greenpole.hibernate.entity.Holder holder);
    public Holder retrieveHolderObject(int holderId);

    /**
     *
     * @param power
     */
    public void uploadPowerOfAttorney(PowerOfAttorney power);
}
