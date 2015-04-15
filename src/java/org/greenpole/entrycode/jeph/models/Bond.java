package org.greenpole.entrycode.jeph.models;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.*;

/**
 *
 * @author Jephthah Sadare
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

    public Bond(String title, Double bondUnitPrice, Date bondMaturity, String bondType, double taxRate, String paymentPlan) {
        setTitle(title);
        setBondUnitPrice(bondUnitPrice);
        setBondMaturity(bondMaturity);
        setBondType(bondType);
        setTaxRate(taxRate);
        setPaymentPlan(paymentPlan);
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
