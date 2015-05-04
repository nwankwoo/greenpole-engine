/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

/**
 *
 * @author user
 * Manipulates the string in many different ways.
 */
public class Manipulator {
    /**
     * Separates a user's first and last name from their email id.
     * @param username the user's username - email id
     * @return the first and last name in a string array
     */
    public String[] separateNameFromEmail(String username) {
        int i = username.indexOf("@");
        String name = username.substring(0, i);
        return name.split("\\.");
    }
}
