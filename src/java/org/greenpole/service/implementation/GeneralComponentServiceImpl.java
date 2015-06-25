/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.service.implementation;

import javax.jws.WebService;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.logic.GeneralComponentLogic;
import org.greenpole.security.SecurityCheck;
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

    @Override
    public Response rejectNotification(Login login, String notificationCode, String rejectionReason) {
        Response resp = new Response();
        return SecurityCheck.securityFailChecker(login, notificationCode, resp) ? resp : 
                request.rejectNotification(login, notificationCode, rejectionReason);
    }

    @Override
    public Response writeOffNotification(Login login, String notificationCode) {
        Response resp = new Response();
        return SecurityCheck.securityFailChecker(login, notificationCode, resp) ? resp : 
                request.writeOffNotification(login, notificationCode);
    }

    @Override
    public Response resendNotification(Login login, NotificationWrapper wrapper) {
        return request.resendNotification(login, wrapper);
    }
}
