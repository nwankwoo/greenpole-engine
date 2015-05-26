/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.listener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import org.greenpole.hibernate.query.GeneralComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;

/**
 * Web application lifecycle listener.
 *
 * @author Akinwale Agbaje
 */
@WebListener()
public class ServletListener implements ServletContextListener {
    private final GeneralComponentQuery gq = ComponentQueryFactory.getGeneralComponentQuery();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        gq.testConnection();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {}
}
