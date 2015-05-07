/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph.models;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.*;

/**
 * @author Jephthah Sadare
 * @version 1.0 Used by the middle-tier to capture private placement details and
 * also to pass bond model values to
 * org.greenpole.hibernate.entity.PrivatePlacement entity
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"clientCompanyId", "totalSharesOnOffer", "methodOnOffer", "startingMinSubscrptn", "continuingMinSubscrptn", "offerPrice", "offerSize", "openingDate", "closingDate"})

public class PrivatePlacement implements Serializable {

    @XmlElement
    private int clientCompanyId;
    @XmlElement
    private int totalSharesOnOffer;
    @XmlElement
    private int methodOnOffer;
    @XmlElement
    private int startingMinSubscrptn;
    @XmlElement
    private int continuingMinSubscrptn;
    @XmlElement
    private double offerPrice;
    @XmlElement
    private int offerSize;
    @XmlElement
    private String openingDate;
    @XmlElement
    private String closingDate;

    /**
     * Initializes PrivatePlacement member variables
     *
     * @param clientCompanyId
     * @param totalSharesOnOffer
     * @param methodOnOffer
     * @param startingMinSubscrptn
     * @param continuingMinSubscrptn
     * @param offerPrice
     * @param offerSize
     * @param openingDate
     * @param closingDate
     */
    public PrivatePlacement(int clientCompanyId, int totalSharesOnOffer, int methodOnOffer,
            int startingMinSubscrptn, int continuingMinSubscrptn, double offerPrice, int offerSize, String openingDate, String closingDate) {
        this.clientCompanyId = clientCompanyId;
        this.totalSharesOnOffer = totalSharesOnOffer;
        this.methodOnOffer = methodOnOffer;
        this.startingMinSubscrptn = startingMinSubscrptn;
        this.continuingMinSubscrptn = continuingMinSubscrptn;
        this.offerPrice = offerPrice;
        this.offerSize = offerSize;
        this.openingDate = openingDate;
        this.closingDate = closingDate;

    }

    public int getClientCompanyId() {
        return clientCompanyId;
    }

    public int getTotalSharesOnOffer() {
        return totalSharesOnOffer;
    }

    public int getMethodOnOffer() {
        return methodOnOffer;
    }

    public int getStartingMinSubscrptn() {
        return startingMinSubscrptn;
    }

    public int getContinuingMinSubscrptn() {
        return continuingMinSubscrptn;
    }

    public double getOfferPrice() {
        return offerPrice;
    }

    public int getOfferSize() {
        return offerSize;
    }

    public String getOpeningDate() {
        return openingDate;
    }

    public String getClosingDate() {
        return closingDate;
    }

    public void setClientCompanyId(int clientCompanyId) {
        this.clientCompanyId = clientCompanyId;
    }

    public void setTotalSharesOnOffer(int totalSharesOnOffer) {
        this.totalSharesOnOffer = totalSharesOnOffer;
    }

    public void setMethodOnOffer(int methodOnOffer) {
        this.methodOnOffer = methodOnOffer;
    }

    public void setStartingMinSubscrptn(int startingMinSubscrptn) {
        this.startingMinSubscrptn = startingMinSubscrptn;
    }

    public void setContinuingMinSubscrptn(int continuingMinSubscrptn) {
        this.continuingMinSubscrptn = continuingMinSubscrptn;
    }

    public void setOfferPrice(double offerPrice) {
        this.offerPrice = offerPrice;
    }

    public void setOfferSize(int offerSize) {
        this.offerSize = offerSize;
    }

    public void setOpeningDate(String openingDate) {
        this.openingDate = openingDate;
    }

    public void setClosingDate(String closingDate) {
        this.closingDate = closingDate;
    }
}
