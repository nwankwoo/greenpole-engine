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
    private Double bondUnitPrice;
    @XmlElement
    private Date bondMaturity;
    @XmlElement
    private String bondType;
    @XmlElement
    private Double interestRate;
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
    public Bond(int clientCompanyId, String title, Double bondUnitPrice, Date bondMaturity, String bondType, Double interestRate, String paymentPlan) {
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

    public Double getBondUnitPrice() {
        return bondUnitPrice;
    }

    public void setBondUnitPrice(Double bondUnitPrice) {
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

    public Double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(Double interestRate) {
        this.interestRate = interestRate;
    }

    public String getPaymentPlan() {
        return paymentPlan;
    }

    public void setPaymentPlan(String paymentPlan) {
        this.paymentPlan = paymentPlan;
    }

}
