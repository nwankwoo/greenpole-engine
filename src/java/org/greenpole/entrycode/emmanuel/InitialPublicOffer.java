/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.greenpole.entrycode.emmanuel;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import org.greenpole.hibernate.entity.ClientCompany;

/**
 *
 * @author Idoko Emmanuel 
 * The Initial Public Offer model, utilized by the front-end and middle-tier in
 * storing and retrieving Initial Public Offer information.
 */
@XmlRootElement
@XmlSeeAlso({InitialPublicOffer.class})
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"versionId","clientCompany","totalSharesOnOffer","methodOnOffer","startingMinSub","contMinSub","offerPrice","offerSize","openingDate","closingDate"," ipoApplications","certificates"})
public class InitialPublicOffer implements Serializable{
    @XmlElement
    private long versionId;
    @XmlElement
    private ClientCompany clientCompany;
    @XmlElement 
    private int totalSharesOnOffer;
    @XmlElement
    private String methodOnOffer;
    @XmlElement
    private int startingMinSub;
    @XmlElement
     private int contMinSub;
    @XmlElement
     private Double offerPrice;
    @XmlElement
     private int offerSize;
    @XmlElement
     private Date openingDate;
    @XmlElement
     private Date closingDate;
    @XmlElementWrapper(name = "ipoApplications")
    private List <?> ipoApplications;
    @XmlElementWrapper(name = "certificates")
     private  List <?> certificates;
    public InitialPublicOffer(){
    }

    @Override
    public String toString() {
        return "InitialPublicOffer{" + "versionId=" + getVersionId() + ", clientCompany=" + getClientCompany() + ", totalSharesOnOffer=" + getTotalSharesOnOffer() + ", methodOnOffer=" + getMethodOnOffer() + ", startingMinSub=" + getStartingMinSub() + ", contMinSub=" + getContMinSub() + ", offerPrice=" + getOfferPrice() + ", offerSize=" + getOfferSize() + ", openingDate=" + getOpeningDate() + ", closingDate=" + getClosingDate() + ", ipoApplications=" + getIpoApplications() + ", certificates=" + getCertificates() + '}';
    }

    public InitialPublicOffer(long versionId, ClientCompany clientCompany, int totalSharesOnOffer, String methodOnOffer, int startingMinSub, int contMinSub, Double offerPrice, int offerSize, Date openingDate, Date closingDate) {
        this.versionId = versionId;
        this.clientCompany = clientCompany;
        this.totalSharesOnOffer = totalSharesOnOffer;
        this.methodOnOffer = methodOnOffer;
        this.startingMinSub = startingMinSub;
        this.contMinSub = contMinSub;
        this.offerPrice = offerPrice;
        this.offerSize = offerSize;
        this.openingDate = openingDate;
        this.closingDate = closingDate;
    }

    /**
     * @return the versionId
     */
    public long getVersionId() {
        return versionId;
    }

    /**
     * @param versionId the versionId to set
     */
    public void setVersionId(long versionId) {
        this.versionId = versionId;
    }

    /**
     * @return the clientCompany
     */
    public ClientCompany getClientCompany() {
        return clientCompany;
    }

    /**
     * @param clientCompany the clientCompany to set
     */
    public void setClientCompany(ClientCompany clientCompany) {
        this.clientCompany = clientCompany;
    }

    /**
     * @return the totalSharesOnOffer
     */
    public int getTotalSharesOnOffer() {
        return totalSharesOnOffer;
    }

    /**
     * @param totalSharesOnOffer the totalSharesOnOffer to set
     */
    public void setTotalSharesOnOffer(int totalSharesOnOffer) {
        this.totalSharesOnOffer = totalSharesOnOffer;
    }

    /**
     * @return the methodOnOffer
     */
    public String getMethodOnOffer() {
        return methodOnOffer;
    }

    /**
     * @param methodOnOffer the methodOnOffer to set
     */
    public void setMethodOnOffer(String methodOnOffer) {
        this.methodOnOffer = methodOnOffer;
    }

    /**
     * @return the startingMinSub
     */
    public int getStartingMinSub() {
        return startingMinSub;
    }

    /**
     * @param startingMinSub the startingMinSub to set
     */
    public void setStartingMinSub(int startingMinSub) {
        this.startingMinSub = startingMinSub;
    }

    /**
     * @return the contMinSub
     */
    public int getContMinSub() {
        return contMinSub;
    }

    /**
     * @param contMinSub the contMinSub to set
     */
    public void setContMinSub(int contMinSub) {
        this.contMinSub = contMinSub;
    }

    /**
     * @return the offerPrice
     */
    public Double getOfferPrice() {
        return offerPrice;
    }

    /**
     * @param offerPrice the offerPrice to set
     */
    public void setOfferPrice(Double offerPrice) {
        this.offerPrice = offerPrice;
    }

    /**
     * @return the offerSize
     */
    public int getOfferSize() {
        return offerSize;
    }

    /**
     * @param offerSize the offerSize to set
     */
    public void setOfferSize(int offerSize) {
        this.offerSize = offerSize;
    }

    /**
     * @return the openingDate
     */
    public Date getOpeningDate() {
        return openingDate;
    }

    /**
     * @param openingDate the openingDate to set
     */
    public void setOpeningDate(Date openingDate) {
        this.openingDate = openingDate;
    }

    /**
     * @return the closingDate
     */
    public Date getClosingDate() {
        return closingDate;
    }

    /**
     * @param closingDate the closingDate to set
     */
    public void setClosingDate(Date closingDate) {
        this.closingDate = closingDate;
    }

    /**
     * @return the ipoApplications
     */
    public List<?> getIpoApplications() {
        return ipoApplications;
    }

    /**
     * @param ipoApplications the ipoApplications to set
     */
    public void setIpoApplications(List<?> ipoApplications) {
        this.ipoApplications = ipoApplications;
    }

    /**
     * @return the certificates
     */
    public List<?> getCertificates() {
        return certificates;
    }

    /**
     * @param certificates the certificates to set
     */
    public void setCertificates(List<?> certificates) {
        this.certificates = certificates;
    }
    
}
