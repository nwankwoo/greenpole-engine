/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph.mocks;

import org.greenpole.hibernate.entity.Administrator;
import org.greenpole.hibernate.entity.AdministratorPhoneNumber;
import org.greenpole.hibernate.entity.AdministratorPostalAddress;
import org.greenpole.hibernate.entity.AdministratorResidentialAddress;

/**
 *
 * @author Jephthah Sadare
 */
public interface AdministratorComponentQuery {

    public boolean createAdministratorAccount(Administrator admin, AdministratorPostalAddress postalAddress, AdministratorPhoneNumber phoneNumber);

    public boolean createAdministratorAccount(Administrator admin, AdministratorResidentialAddress residentialAddress, AdministratorPhoneNumber phoneNumber);
    
    
}
