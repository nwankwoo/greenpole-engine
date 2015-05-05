/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.jeph.models;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import org.greenpole.entity.model.Address;
import org.greenpole.entity.model.EmailAddress;
import org.greenpole.entity.model.PhoneNumber;
import org.greenpole.entity.model.holder.HolderBondAccount;
import org.greenpole.entity.model.holder.HolderCompanyAccount;
import org.greenpole.entity.model.stockbroker.Stockbroker;

/**
 *
 * @author Jephthah Sadare
 * @version 1.0 Used by the middle-tier to capture details holder and holder changes
 */
public class HolderEdit {
    @XmlElement
    private int holderId;
    @XmlElement
    private int holderAcctNumber;
    @XmlElement
    private String chn;
    @XmlElement
    private String firstName;
    @XmlElement
    private String middleName;
    @XmlElement
    private String lastName;
    @XmlElement
    private String type;
    @XmlElement
    private String gender;
    @XmlElement
    private String dob;
    @XmlElement
    private boolean taxExempted;
    @XmlElement
    private boolean merged;
    @XmlElement
    private boolean pryHolder;
    @XmlElement
    private String pryAddress;
    
    @XmlElementWrapper(name = "addresses")
    private List <Address> addresses;

    @XmlElementWrapper(name = "holderPhoneNumbers")
    private List<PhoneNumber> holderPhoneNumbers;
    @XmlElementWrapper(name = "holderEmailAddresses")
    private List<EmailAddress> holderEmailAddresses;
    @XmlElementWrapper(name = "holderResidentialAddresses")
    private List<Address> holderResidentialAddresses;
    @XmlElementWrapper(name = "holderPostalAddresses")
    private List<Address> holderPostalAddresses;
    @XmlTransient
    private Stockbroker holderStockbroker;
    @XmlElementWrapper(name = "holderCompanyAccounts")
    private List<HolderCompanyAccount> holderCompanyAccounts;
    @XmlElementWrapper(name = "holderBondAccounts")
    private List<HolderBondAccount> holderBondAccounts;    
    @XmlTransient
    private List<Address> deletedAddresses;
    @XmlTransient
    private List<EmailAddress> deletedEmailAddresses;
    @XmlTransient
    private List<PhoneNumber> deletedPhoneNumbers;

    @XmlElement
    private String changeType;
    @XmlElement
    private String description;
    @XmlElement
    private int holderChangesId;
    @XmlElementWrapper(name = "holderChanges")
    private List<HolderChanges> holderChanges;

    /**
     * 
     * @param holderId holder id
     * @param holderAcctNumber holder account number
     * @param chn holder chn
     * @param firstName holder first name
     * @param middleName holder middle name
     * @param lastName holder last name
     * @param type type of account
     * @param gender gender of holder
     * @param dob date of birth
     * @param taxExempted is holder exempted from tax
     * @param merged is it a merged account
     * @param pryHolder is ti a primary holder
     * @param pryAddress primary address
     * @param addresses list of addresses
     * @param holderPhoneNumbers list of holder phone numbers
     * @param holderEmailAddresses list of holder email addresses
     * @param holderResidentialAddresses list of holder residential addresses
     * @param holderPostalAddresses list of holder postal addresses
     * @param holderStockbroker holder stock broker
     * @param holderCompanyAccounts list of holder other company accounts
     * @param holderBondAccounts list of holder other bond accounts
     * @param deletedAddresses
     * @param deletedEmailAddresses
     * @param deletedPhoneNumbers
     * @param changeType type of change to holder details
     * @param description description of change to holder changes
     * @param holderChangesId holder change id
     * @param holderChanges list of holder changes
     */
    public HolderEdit(int holderId, int holderAcctNumber, String chn, String firstName, String middleName, String lastName, String type, String gender, String dob, boolean taxExempted, boolean merged, boolean pryHolder, String pryAddress, List<Address> addresses, List<PhoneNumber> holderPhoneNumbers, List<EmailAddress> holderEmailAddresses, List<Address> holderResidentialAddresses, List<Address> holderPostalAddresses, Stockbroker holderStockbroker, List<HolderCompanyAccount> holderCompanyAccounts, List<HolderBondAccount> holderBondAccounts, List<Address> deletedAddresses, List<EmailAddress> deletedEmailAddresses, List<PhoneNumber> deletedPhoneNumbers, String changeType, String description, int holderChangesId, List<HolderChanges> holderChanges) {
        this.holderId = holderId;
        this.holderAcctNumber = holderAcctNumber;
        this.chn = chn;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.type = type;
        this.gender = gender;
        this.dob = dob;
        this.taxExempted = taxExempted;
        this.merged = merged;
        this.pryHolder = pryHolder;
        this.pryAddress = pryAddress;
        this.addresses = addresses;
        this.holderPhoneNumbers = holderPhoneNumbers;
        this.holderEmailAddresses = holderEmailAddresses;
        this.holderResidentialAddresses = holderResidentialAddresses;
        this.holderPostalAddresses = holderPostalAddresses;
        this.holderStockbroker = holderStockbroker;
        this.holderCompanyAccounts = holderCompanyAccounts;
        this.holderBondAccounts = holderBondAccounts;
        this.deletedAddresses = deletedAddresses;
        this.deletedEmailAddresses = deletedEmailAddresses;
        this.deletedPhoneNumbers = deletedPhoneNumbers;
        this.changeType = changeType;
        this.description = description;
        this.holderChangesId = holderChangesId;
        this.holderChanges = holderChanges;
    }

    public int getHolderId() {
        return holderId;
    }

    public void setHolderId(int holderId) {
        this.holderId = holderId;
    }

    public int getHolderAcctNumber() {
        return holderAcctNumber;
    }

    public void setHolderAcctNumber(int holderAcctNumber) {
        this.holderAcctNumber = holderAcctNumber;
    }

    public String getChn() {
        return chn;
    }

    public void setChn(String chn) {
        this.chn = chn;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public boolean isTaxExempted() {
        return taxExempted;
    }

    public void setTaxExempted(boolean taxExempted) {
        this.taxExempted = taxExempted;
    }

    public boolean isMerged() {
        return merged;
    }

    public void setMerged(boolean merged) {
        this.merged = merged;
    }

    public boolean isPryHolder() {
        return pryHolder;
    }

    public void setPryHolder(boolean pryHolder) {
        this.pryHolder = pryHolder;
    }

    public String getPryAddress() {
        return pryAddress;
    }

    public void setPryAddress(String pryAddress) {
        this.pryAddress = pryAddress;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    public List<PhoneNumber> getHolderPhoneNumbers() {
        return holderPhoneNumbers;
    }

    public void setHolderPhoneNumbers(List<PhoneNumber> holderPhoneNumbers) {
        this.holderPhoneNumbers = holderPhoneNumbers;
    }

    public List<EmailAddress> getHolderEmailAddresses() {
        return holderEmailAddresses;
    }

    public void setHolderEmailAddresses(List<EmailAddress> holderEmailAddresses) {
        this.holderEmailAddresses = holderEmailAddresses;
    }

    public List<Address> getHolderResidentialAddresses() {
        return holderResidentialAddresses;
    }

    public void setHolderResidentialAddresses(List<Address> holderResidentialAddresses) {
        this.holderResidentialAddresses = holderResidentialAddresses;
    }

    public List<Address> getHolderPostalAddresses() {
        return holderPostalAddresses;
    }

    public void setHolderPostalAddresses(List<Address> holderPostalAddresses) {
        this.holderPostalAddresses = holderPostalAddresses;
    }

    public Stockbroker getHolderStockbroker() {
        return holderStockbroker;
    }

    public void setHolderStockbroker(Stockbroker holderStockbroker) {
        this.holderStockbroker = holderStockbroker;
    }

    public List<HolderCompanyAccount> getHolderCompanyAccounts() {
        return holderCompanyAccounts;
    }

    public void setHolderCompanyAccounts(List<HolderCompanyAccount> holderCompanyAccounts) {
        this.holderCompanyAccounts = holderCompanyAccounts;
    }

    public List<HolderBondAccount> getHolderBondAccounts() {
        return holderBondAccounts;
    }

    public void setHolderBondAccounts(List<HolderBondAccount> holderBondAccounts) {
        this.holderBondAccounts = holderBondAccounts;
    }

    public List<Address> getDeletedAddresses() {
        return deletedAddresses;
    }

    public void setDeletedAddresses(List<Address> deletedAddresses) {
        this.deletedAddresses = deletedAddresses;
    }

    public List<EmailAddress> getDeletedEmailAddresses() {
        return deletedEmailAddresses;
    }

    public void setDeletedEmailAddresses(List<EmailAddress> deletedEmailAddresses) {
        this.deletedEmailAddresses = deletedEmailAddresses;
    }

    public List<PhoneNumber> getDeletedPhoneNumbers() {
        return deletedPhoneNumbers;
    }

    public void setDeletedPhoneNumbers(List<PhoneNumber> deletedPhoneNumbers) {
        this.deletedPhoneNumbers = deletedPhoneNumbers;
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
