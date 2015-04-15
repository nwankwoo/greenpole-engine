package org.greenpole.entrycode.jeph.models;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.*;

/**
 *
 * @author Jephthah Sadare
 * @version 1.0
 * Used by the middle-tier to capture bond details and also to 
 * pass bond model values to org.greenpole.hibernate.entity.BondOffer entity
 * 
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"title", "code", "bondUnitPrice", "bondMaturity", "bondType", "taxRate", "paymentPlan"})
public class Bond implements Serializable {

    @XmlElement
    private String title;
    @XmlElement
    private Double bondUnitPrice;
    @XmlElement
    private Date bondMaturity;
    @XmlElement
    private String bondType;
    @XmlElement
    private Double taxRate;
    @XmlElement
    private String paymentPlan;
    
    /**
     * Collects all the values for a type of Bond
     * @param title bond title
     * @param bondUnitPrice price per bond
     * @param bondMaturity final date for the transaction to end
     * @param bondType whether fixed or redeemable
     * @param taxRate interest rate on bonds
     * @param paymentPlan period for which each coupon is received which can annually, bi-annually, quarterly e.t.c
     */
    public Bond(String title, Double bondUnitPrice, Date bondMaturity, String bondType, double taxRate, String paymentPlan) {
        this.title = title;
        this.bondUnitPrice = bondUnitPrice;
        this.bondMaturity = bondMaturity;
        this.bondType = bondType;
        this.taxRate = taxRate;
        this.paymentPlan = paymentPlan;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Double getBondUnitPrice() {
        return bondUnitPrice;
    }

    public void setBondUnitPrice(double bondUnitPrice) {
        this.bondUnitPrice = bondUnitPrice;
    }

    public Date getBondMaturity() {
        return bondMaturity;
    }

    public void setBondMaturity(Date bondMaturity) {
        this.bondMaturity = bondMaturity;
    }

    public String getBondType() {
        return bondType;
    }

    public void setBondType(String bondType) {
        this.bondType = bondType;
    }

    public String getPaymentPlan() {
        return paymentPlan;
    }

    public void setPaymentPlan(String paymentPlan) {
        this.paymentPlan = paymentPlan;
    }

    public double getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(double taxRate) {
        this.taxRate = taxRate;
    }

    @Override
    public String toString() {
        return "Bond { " + "title = " + title + ", bondUnitPrice = " + bondUnitPrice + ", bondMaturity = " + bondMaturity + ", bondType = " + bondType + ", taxRate = " + taxRate + ", paymentPlan = " + paymentPlan + " } ";
    }

}
