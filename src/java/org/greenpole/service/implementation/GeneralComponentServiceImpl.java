/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.service.implementation;

import javax.jws.WebService;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.logic.GeneralComponentLogic;
import org.greenpole.service.GeneralComponentService;

/**
 *
 * @author Akinwale Agbaje
 */
@WebService(serviceName = "generalservice", endpointInterface = "org.greenpole.service.GeneralComponentService")
public class GeneralComponentServiceImpl implements GeneralComponentService {
    GeneralComponentLogic request = new GeneralComponentLogic();

    @Override
    public Response getReceiverNotifications_Request(Login login) {
        return request.getReceiverNotifications_Request(login);
    }
    
}