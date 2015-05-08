/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph.models;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

/**
 *
 * @author Jephthah Sadare
 * @version 1.0 Used by the middle-tier to capture details holder and holder
 * changes
 */
public class HolderEdit {

    @XmlElement
    private int holderId;
    @XmlElement
    private int holderChangeTypeId;
    @XmlElement
    private String initialForm;
    @XmlElement
    private String currentForm;
    @XmlElement
    private String changeDate;

    public HolderEdit(int holderId, int holderChangeTypeId, String initialForm, String currentForm, String changeDate) {
        this.holderId = holderId;
        this.holderChangeTypeId = holderChangeTypeId;
        this.initialForm = initialForm;
        this.currentForm = currentForm;
        this.changeDate = changeDate;
    }

    public int getHolderId() {
        return holderId;
    }

    public void setHolderId(int holderId) {
        this.holderId = holderId;
    }

    public int getHolderChangeTypeId() {
        return holderChangeTypeId;
    }

    public void setHolderChangeTypeId(int holderChangeTypeId) {
        this.holderChangeTypeId = holderChangeTypeId;
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

    public String getChangeDate() {
        return changeDate;
    }

    public void setChangeDate(String changeDate) {
        this.changeDate = changeDate;
    }
    
    
}
