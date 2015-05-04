/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph.models;

import javax.xml.bind.annotation.XmlElement;

/**
 *
 * @author Jephthah Sadare
 */
public class HolderChanges {
    
    @XmlElement
    private int id;
    @XmlElement
    private String initialForm;
    @XmlElement
    private String currentForm;
    @XmlElement
    private int holderChangeTypeId;
    @XmlElement
    private String changeDate;
    @XmlElement
    private int holderId;

    public HolderChanges() {
    }

    public HolderChanges(int id, String initialForm, String currentForm, int holderChangeTypeId, String changeDate, int holderId) {
        this.id = id;
        this.initialForm = initialForm;
        this.currentForm = currentForm;
        this.holderChangeTypeId = holderChangeTypeId;
        this.changeDate = changeDate;
        this.holderId = holderId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getInitialForm() {
        return initialForm;
    }

    public void setInitialForm(String initialForm) {
        this.initialForm = initialForm;
    }

    public String getCurrentForm() {
        return currentForm;
    }

    public void setCurrentForm(String currentForm) {
        this.currentForm = currentForm;
    }

    public int getHolderChangeTypeId() {
        return holderChangeTypeId;
    }

    public void setHolderChangeTypeId(int holderChangeTypeId) {
        this.holderChangeTypeId = holderChangeTypeId;
    }

    public String getChangeDate() {
        return changeDate;
    }

    public void setChangeDate(String changeDate) {
        this.changeDate = changeDate;
    }

    public int getHolderId() {
        return holderId;
    }

    public void setHolderId(int holderId) {
        this.holderId = holderId;
    }
    
    

}
