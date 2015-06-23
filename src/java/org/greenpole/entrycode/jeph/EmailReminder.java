/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.HolderComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.util.properties.GreenpoleProperties;
import org.greenpole.util.properties.NotificationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Jephthah Sadare
 */
public class EmailReminder implements Runnable {

    private final HolderComponentQuery hq = ComponentQueryFactory.getHolderComponentQuery();
    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();
    private final GreenpoleProperties greenProp = GreenpoleProperties.getInstance();
    private final NotificationProperties notificationProp = NotificationProperties.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(ClientCompanyLogic.class);
    SimpleDateFormat formatter = new SimpleDateFormat();

    @Override
    public void run() {// algorithm to be revised
        try {
            // List<org.greenpole.hibernate.entity.Dividend> dividendList = hq.getAllDividend();
            List<org.greenpole.hibernate.entity.Dividend> dividendList = new ArrayList<>();
            for (org.greenpole.hibernate.entity.Dividend d : dividendList) {
                if (!d.getPaid()) {
                    // send email
                } else if (d.getReIssued() && !d.getPaid()) {
                    // send email
                } else if (d.getCancelled()) {
                    // send email
                }
            }
        } catch (Exception ex) {
            logger.error("Error monitoring: [{}]", ex);
        }
    }

    public static void main(String[] args) {
        Thread t = new Thread(new EmailReminder());
        t.start();
    }
}
