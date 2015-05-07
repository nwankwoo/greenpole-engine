package org.greenpole.entrycode.jeph.models;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.*;

/**
 *
 * @author Jephthah Sadare
 * @version 1.0 Used by the middle-tier to capture bond details and also to pass
 * bond model values to org.greenpole.hibernate.entity.BondOffer entity
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"clientCompanyId", "title", "bondUnitPrice", "bondMaturity", "bondType", "taxRate", "paymentPlan"})
public class Bond implements Serializable {

    @XmlElement
    private int clientCompanyId;
    @XmlElement
    private String title;
    @XmlElement
    private double bondUnitPrice;
    @XmlElement
    private String bondMaturity;
    @XmlElement
    private String bondType;
    @XmlElement
    private double interestRate;
    @XmlElement
    private String paymentPlan;

    /**
     * Collects all the values for a type of Bond
     *
     * @param clientCompanyId
     * @param title bond title
     * @param bondUnitPrice price per bond
     * @param bondMaturity final date for the transaction to end
     * @param bondType whether fixed or redeemable
     * @param interestRate interest rate on bonds
     * @param paymentPlan period for which each coupon is received which can
     * annually, bi-annually, quarterly e.t.c
     */
    public Bond(int clientCompanyId, String title, double bondUnitPrice, String bondMaturity, String bondType, double interestRate, String paymentPlan) {
        this.clientCompanyId = clientCompanyId;
        this.title = title;
        this.bondUnitPrice = bondUnitPrice;
        this.bondMaturity = bondMaturity;
        this.bondType = bondType;
        this.interestRate = interestRate;
        this.paymentPlan = paymentPlan;
    }

    public int getClientCompanyId() {
        return clientCompanyId;
    }

    public void setClientCompanyId(int clientCompanyId) {
        this.clientCompanyId = clientCompanyId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getBondUnitPrice() {
        return bondUnitPrice;
    }

    public void setBondUnitPrice(double bondUnitPrice) {
        this.bondUnitPrice = bondUnitPrice;
    }

    public String getBondMaturity() {
        return bondMaturity;
    }

    public void setBondMaturity(String bondMaturity) {
        this.bondMaturity = bondMaturity;
    }

    public String getBondType() {
        return bondType;
    }

    public void setBondType(String bondType) {
        this.bondType = bondType;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(double interestRate) {
        this.interestRate = interestRate;
    }

    public String getPaymentPlan() {
        return paymentPlan;
    }

    public void setPaymentPlan(String paymentPlan) {
        this.paymentPlan = paymentPlan;
    }

}
