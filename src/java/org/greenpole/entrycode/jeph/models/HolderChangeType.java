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
 */
public class HolderChangeType {
    
    @XmlElement
    private int id;
    @XmlElement
    private String changeType;
    @XmlElement
    private String description;
    @XmlElement
    private int holderChangesId;
    @XmlElementWrapper(name = "holderChanges")
    private List<HolderChanges> holderChanges;

    public HolderChangeType() {
    }

    public HolderChangeType(int id, String changeType, String description, int holderChangesId, List<HolderChanges> holderChanges) {
        this.id = id;
        this.changeType = changeType;
        this.description = description;
        this.holderChangesId = holderChangesId;
        this.holderChanges = holderChanges;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getHolderChangesId() {
        return holderChangesId;
    }

    public void setHolderChangesId(int holderChangesId) {
        this.holderChangesId = holderChangesId;
    }

    public List<HolderChanges> getHolderChanges() {
        return holderChanges;
    }

    public void setHolderChanges(List<HolderChanges> holderChanges) {
        this.holderChanges = holderChanges;
    }
    
    
}
