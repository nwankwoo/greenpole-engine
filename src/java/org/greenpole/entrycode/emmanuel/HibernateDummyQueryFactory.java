/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

/**
 *
 * @author user
 */
public class HibernateDummyQueryFactory {

    public static HibernatDummyQuerInterface getHibernateDummyQuery() {
        return new HibernateDummyQuery();
    }
}
