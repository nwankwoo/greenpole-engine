/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph.bondmodule;

import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;

/**
 *
 * @author Jephthah Sadare
 */
public interface BondComponent {
    public Response createBondOffer(Login login, String authenticator, Bond bond);
    public Response setupBondOfferAuthorise(String notificationCode);
}
