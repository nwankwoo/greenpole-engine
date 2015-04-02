/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.logic;

import org.greenpole.entity.model.ClientCompany;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.hibernate.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author Akinwale Agbaje
 * @version 1.0
 * Business requirement implementations to do with client companies
 */
public class ClientCompanyComponentLogic {
    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();
    
    /**
     * Processes request to create a new client company.
     * @param cc the client company to be created
     */
    public void createClientCompany_Request(ClientCompany cc) {
        //check if client company exists
        if (!cq.checkClientCompany(cc.getName())) {
            //notifier code enters here
        }
    }
}
