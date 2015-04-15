package org.greenpole.entrycode.jeph.mocks;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


// import java.util.*;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;

/**
 *
 * @author Jephthah Sadare
 */
public class QueueSender {

    public QueueSender(String queueConnectionFactory, String queueName) {

    }

    public Response sendAuthorisationRequest(NotificationWrapper wrapper) {
        Response resp = new Response();
        resp.setRetn(0);
        resp.setDesc("Successfull");
        return resp;
    }
}
