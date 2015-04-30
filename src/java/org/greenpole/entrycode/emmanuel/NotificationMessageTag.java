/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

/**
 *
 * @author user
 * The acceptable list of notification message tags.
 */
public enum NotificationMessageTag {

    /**
     * The authorisation request tag for notifications involving authorisation requests.
     */
    Authorisation_request ("For notifications involving authorisation requests"),

    /**
     * The authorisation accept tag for notifications involving successful authorisations.
     */
    Authorisation_accept ("For notifications involving successful authorisations"),

    /**
     * The authorisation reject tag for notifications involving rejected authorisation requests.
     */
    Authorisation_reject ("For notifications involving rejected authorisation requests"),

    /**
     * The outbound text tag for notifications involving text messages meant for holders.
     */
    outbound_text ("For notifications involving text messages meant for holders"),

    /**
     * The outbound email for notifications involving emails meant for holders.
     */
    outbound_email ("For notifications involving emails meant for holders");

    private final String description;

    private NotificationMessageTag(String description) {
        this.description = description;
    }

    /**
     * Gets the description of the defined notification message tag.
     * @return the notification message tag
     */
    public String getDescription() {
        return description;
    }
}
