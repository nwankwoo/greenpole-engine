/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph.models;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author Jephthah Sadare
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"id", "holderId", "title", "signaturePath", "holderSignaturePrimary"})

public class HolderSignature {

    @XmlElement
    private int id;
    @XmlElement
    private int holderId;
    @XmlElement
    private String title;
    @XmlElement
    private String signaturePath;
    @XmlElement
    private boolean holderSignaturePrimary;

    public HolderSignature() {

    }

    /**
     *
     * @param id
     * @param holderId
     * @param title
     * @param signaturePath
     * @param holderSignaturePrimary
     */
    public HolderSignature(int id, int holderId, String title, String signaturePath, boolean holderSignaturePrimary) {
        this.id = id;
        this.holderId = holderId;
        this.title = title;
        this.signaturePath = signaturePath;
        this.holderSignaturePrimary = holderSignaturePrimary;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getHolderId() {
        return holderId;
    }

    public void setHolderId(int holderId) {
        this.holderId = holderId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSignaturePath() {
        return signaturePath;
    }

    public void setSignaturePath(String signaturePath) {
        this.signaturePath = signaturePath;
    }

    public boolean isHolderSignaturePrimary() {
        return holderSignaturePrimary;
    }

    public void setHolderSignaturePrimary(boolean holderSignaturePrimary) {
        this.holderSignaturePrimary = holderSignaturePrimary;
    }

}
