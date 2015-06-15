/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

import java.util.List;
import java.util.Map;
import org.greenpole.entity.security.Login;
import org.greenpole.hibernate.entity.Holder;
import org.greenpole.entity.model.holder.PowerOfAttorney;
import org.greenpole.hibernate.entity.AccountConsolidation;
import org.greenpole.hibernate.entity.Administrator;
import org.greenpole.hibernate.entity.AdministratorEmailAddress;
import org.greenpole.hibernate.entity.AdministratorPhoneNumber;
import org.greenpole.hibernate.entity.AdministratorResidentialAddress;
import org.greenpole.hibernate.entity.Caution;
import org.greenpole.hibernate.entity.ClientCompany;
import org.greenpole.hibernate.entity.CompanyAccountConsolidation;
import org.greenpole.hibernate.entity.HolderBondAccount;
import org.greenpole.hibernate.entity.HolderCompanyAccount;
import org.greenpole.hibernate.entity.InitialPublicOffer;
import org.greenpole.hibernate.entity.IpoApplication;
import org.greenpole.hibernate.entity.PrivatePlacement;
import org.greenpole.hibernate.entity.PrivatePlacementApplication;
import org.greenpole.hibernate.entity.ProcessedTransaction;
import org.greenpole.hibernate.entity.ProcessedTransactionHolder;
import org.greenpole.hibernate.entity.RightsIssue;
import org.greenpole.hibernate.entity.RightsIssueApplication;

/**
 *
 * @author user
 */
public interface HibernatDummyQuerInterface {

    /**
     *
     * @param admin
     * @param emailAddress
     * @param phoneNumber
     * @param residentialAddress
     * @param holder
     */
    public void createAdministratorForShareHolderAndBondHolder(Administrator admin, List<AdministratorEmailAddress> emailAddress, List<AdministratorPhoneNumber> phoneNumber, List<AdministratorResidentialAddress> residentialAddress, List<Holder> holder);

    public void createEmail(AdministratorEmailAddress emailAddress);

    public void createPhoneNumber(AdministratorPhoneNumber phoneNumber);

    public void createAdministratorResidentialAddress(AdministratorResidentialAddress adminResidentialAddress);

    //public boolean checkHolder(Holder holder);
    /**
     *
     * @param holder
     */
    public void updateAdministrationHolderCompanyAccount(org.greenpole.hibernate.entity.Holder holder);

    public Holder retrieveHolderObject(int holderId);

    /**
     *
     * @param power
     */
    public void uploadPowerOfAttorney(PowerOfAttorney power);

    public boolean checkHolderNubanNumber(String nubanAccount);

    public List getAllShareholderNubanAccounts();

    public List getAllBondholderNubanAccounts();

    public void addShareholderNubanAccount();

    public void createNubanAccount(HolderCompanyAccount holderAccount);

    public void createBondNubanAccount(HolderBondAccount holderAccount);

    public void changeShareholderNubanAccount(HolderCompanyAccount holderAccount);

    public void changeBondholderNubanAccount(HolderBondAccount bondholderAccount);

    //public org.greenpole.hibernate.entity.HolderChanges getHolderEditedDetails(int holderId);
    //public org.greenpole.hibernate.entity.HolderChanges retrieveHolderChangesQueryOne(String changeType, String changeDate, int holderId);
    // public org.greenpole.hibernate.entity.HolderChanges retrieveHolderChangesQueryTwo(String changeType, String changeDate1, String changeDate2, int holderId);
    public org.greenpole.hibernate.entity.HolderCompanyAccount retrieveHolderCompanyAccount(int holderId, int clientCompanyId);

    public org.greenpole.hibernate.entity.HolderBondAccount retrieveHolderBondCompAccount(int holderId, int bondId);

    public List<CompanyAccountConsolidation> queryAccountConsolidation(String descriptor, CompanyAccountConsolidation compAccCon, String start_date, String end_date);

    public List<Login> getUserList(List<Login> login);

    public List<AccountConsolidation> queryAccCon(int holderId);

    public boolean updatePowerOfAttorneyStatus(int holderId);

    public org.greenpole.hibernate.entity.PowerOfAttorney retrieveCurrentPowerOfAttorney(int holderId);

    public org.greenpole.hibernate.entity.HolderType getHolderType(int holderTypeId);

    public void cautionShareHolderAndBondholder(Caution caution);

    /**
     * Queries the cautioned holder account according to the search parameters.
     *
     * @param descriptor the description of the type of search to carry out
     * @param searchParams the cautioned holder search parameter
     * @param startDate the start date of the search
     * @param endDate the end date of the search
     * @return the list of cautioned holder account from the search
     */
    public List<Caution> queryCautionedHolders(String descriptor, Caution searchParams, String startDate, String endDate);

    /**
     * Sets up of rights issue
     *
     * @param rightsIssue
     * @return
     */
    public boolean setUp_RightIssue(RightsIssue rightsIssue);

    public List<Holder> getHoldersByClientCompanyId(int clientCompanyId);

    public List<HolderCompanyAccount> getHoldersShareUnitsByClientCompanyId(int clientCompanyId);

    public List<org.greenpole.hibernate.entity.RightsIssueApplication> getHoldersRightsIssueApplicationByClientCompany(int clientCompanyId);

    public boolean removeClientCompany(int clientCompanyId);

    public boolean updateClientCompanyValidStatus(int clientCompanyId);

    public org.greenpole.hibernate.entity.HolderCompanyAccount getHolderCompanyAccount(int holderId);

    public org.greenpole.hibernate.entity.BondOffer getBondOfferId(int clientCompanyId);

    public org.greenpole.hibernate.entity.Holder getHolderIdFromHolderBondAccount(int bondOfferId);

    boolean hasBondAccounts(int holderId);

    boolean hasCompanyAccounts(int holderId);

    public List<org.greenpole.hibernate.entity.HolderBondAccount> getAllBondAccounts(int holderId);

    public List<org.greenpole.hibernate.entity.HolderCompanyAccount> getAllCompAccounts(int holderId);

    public org.greenpole.hibernate.entity.Holder getHolder(int holderId);

    public boolean mergeClientCompanyAccounts(ClientCompany pryClientComp, List<ClientCompany> secClientComps, List<HolderCompanyAccount> secHolderCompAccts,
            List<HolderBondAccount> secHolderBondAccts);

    public List<org.greenpole.hibernate.entity.HolderCompanyAccount> getAllHolderCompanyAccountsByClientCompanyId(int clientCompanyId);

    /**
     * Gets all account consolidation according to specified dates.
     *
     * @param clientCompanyId the client company whose IPO is to be retrieved
     * @param descriptor
     * @param startDate
     * @param endDate
     * @param dateFormat
     * @return the initial public offer record from hibernate
     */
    public List<InitialPublicOffer> getInitialPublicOfferByClientCompanyId(int clientCompanyId, String descriptor, String startDate, String endDate, String dateFormat);

    public List<IpoApplication> getAllIpoApplication(int ipoId, int clientCompanyId);

    public org.greenpole.hibernate.entity.ClearingHouse getClearingHouse(int clearingHouseId);

    public List<org.greenpole.hibernate.entity.Holder> queryShareholders(String descriptor, int clientCompanyId, int startAge, int endAge);

    public List<org.greenpole.hibernate.entity.HolderCompanyAccount> getHolderCompanyAccounts(int holderId);

    public List<org.greenpole.hibernate.entity.HolderEmailAddress> getHolderEmailAddresses(int holderId);

    public List<org.greenpole.hibernate.entity.HolderPhoneNumber> getHolderPhoneNumbers(int holderId);

    public List<org.greenpole.hibernate.entity.HolderResidentialAddress> getHolderResidentialAddresses(int holderId);

    public List<org.greenpole.hibernate.entity.HolderPostalAddress> getHolderPostalAddresses(int holderId);

    public List<org.greenpole.hibernate.entity.Administrator> getHolderAdministrators(int holderId);

    public List<org.greenpole.hibernate.entity.HolderBondAccount> getHolderBondAccounts(int holderId);

    public List<org.greenpole.hibernate.entity.AdministratorEmailAddress> getAdministratorEmail(int administratorId);

    public List<org.greenpole.hibernate.entity.AdministratorPhoneNumber> getAdministratorPhone(int administratorId);

    public List<org.greenpole.hibernate.entity.AdministratorPostalAddress> getAdministratorPostalAddress(int administratorId);

    public List<org.greenpole.hibernate.entity.AdministratorResidentialAddress> getAdministratorResidentialAddress(int administratorId);

    public boolean RightIssueApplication(List<RightsIssueApplication> rightApp);

    public void updateRightIssueSetup(int rightsIssueId);

    public org.greenpole.hibernate.entity.RightsIssue getRightsIssueById(int rightsIssueId, int clientCompanyId);

    public boolean checkRightsIssue(int rightsIssueId, int clientCompanyId);

    public List<org.greenpole.hibernate.entity.HolderCompanyAccount> getHolderCompanyAccountsByClientCompanyId(int clientCompanyId);

    public boolean updateHCA(int holderId, double sharesToadd);

    public org.greenpole.hibernate.entity.HolderCompanyAccount getOneHolderCompanyAccount(int holderId, int clientCompanyId);

    public org.greenpole.hibernate.entity.RightsIssueApplication getOneHolderRightApplication(int holderId, int clientCompanyId, int rightAppId);

    /**
     * retrieve the list of all applied rights under the specified client
     * company with the id of the rights issue
     *
     * @param clientCompanyId the client company rights to retrieve
     * @param rightAppId the rights issue to be retrieved
     * @return list of rights application from hibernate
     */
    public List<RightsIssueApplication> getAllRightsIssueApplications(int clientCompanyId, int rightAppId);

    /**
     * retrieves rights issue setup details using the search parameters
     *
     * @param clientCompanyId
     * @param descriptor
     * @param startDate
     * @param endDate
     * @param dateFormat
     * @return
     */
    public List<RightsIssue> getRightsIssue(int clientCompanyId, String descriptor, String startDate, String endDate, String dateFormat);

    /**
     * retrieves private placement records under the specified client company
     *
     * @param clientCompanyId
     * @param ppAppId
     * @return
     */
    public List<PrivatePlacementApplication> getPrivatePlacementApplication(int clientCompanyId, int ppAppId);

    /**
     *
     * @param clientCompanyId
     * @param descriptor
     * @param startDate
     * @param endDate
     * @param dateFormat
     * @return
     */
    public List<PrivatePlacement> getPrivatePlacement(int clientCompanyId, String descriptor, String startDate, String endDate, String dateFormat);

    /**
     * checks the existence of the holder company account of the specified
     * client company
     *
     * @param holderId the holder to check
     * @param clientCompanyId the client company to check against
     * @return true if the holder has an account with the client company
     */
    public boolean checkHolderCompanyAccount(int holderId, int clientCompanyId);

    /**
     * creates a right issue application for a holder
     *
     * @param rightsIssueApplication the right issue application to create
     */
    public void applyForRightIssue(RightsIssueApplication rightsIssueApplication);

    /**
     * updates canceled status of a holder rights application
     *
     * @param clientCompanyId the client company issuing the rights
     * @param holderId the holder application to be canceled
     * @param rightsIssueId the right issue application to be canceled
     * @return the status of the request
     */
    public boolean updateCancelleRightsApp(int clientCompanyId, int holderId, int rightsIssueId);

    /**
     * adds the subscribed shares to be canceled into the remaining shares of
     * the client company
     *
     * @param clientCompanyId the client company issuing the rights
     * @param rightsIssueId the right issue shares is to be added back into.
     */
    public void updateRightIssueTotalShares(int clientCompanyId, int rightsIssueId);

    /**
     *
     * @param holderId the holder to update rights application
     * @param clientCompanyId the issuer of the rights
     * @param rightsIssueId the rights issue Id
     */
    public void updateShareholderRightsIssueApplication(int holderId, int clientCompanyId, int rightsIssueId);

    public boolean uploadRightsApplicationEnmass(List<RightsIssueApplication> applicationList);

    public boolean checkHolderRightsApplication(int holderId, int clientCompanyId, int rightAppId);

    public void updateHCA(int holderId, int clientCompanyId);

    public boolean createNewHolder(org.greenpole.hibernate.entity.Holder holder);

    //public boolean createNewHCA()
    public boolean uploadTransaction(List<ProcessedTransaction> processedTransaction);

    public boolean checkCSCSTransactionExistence(int cscsTransactionId);

    public boolean checkCertExistince(String certNumber);

    public org.greenpole.hibernate.entity.ProcessedTransaction getProcessedTransaction(int processedTransactionId);

    public org.greenpole.hibernate.entity.TransactionType getTransactionType(int transactionId);

    public List<org.greenpole.hibernate.entity.ProcessedTransaction> queryTransaction(String descriptor, org.greenpole.hibernate.entity.ProcessedTransaction pth, String start_date, String end_date, Map<String, Integer> shareUnitSoldCriteria,
            Map<String, Integer> shareUnitBoughtCriteria, Map<String, Integer> bondUnitSoldCriteria, Map<String, Integer> bondUnitBoughtCriteria);

    public List<org.greenpole.hibernate.entity.ProcessedTransactionHolder> getProcessedTransactionHolder(int procesedTransactionId);

    public boolean configureDividendType(org.greenpole.hibernate.entity.DividendIssueType diviType);

    public boolean configureCouponType(org.greenpole.hibernate.entity.BondType couponType);

    public org.greenpole.hibernate.entity.Dividend getDividendAnnotation(int dividendId);

    public void createDividendAnnotation(org.greenpole.hibernate.entity.DividenAnnotation divi);

    public List<org.greenpole.hibernate.entity.Dividend> queryDividend(String descriptor, org.greenpole.hibernate.entity.Dividend divi, Map<String, Double> grossAmount, Map<String, Double> tax, Map<String, Double> payableAmount);

    public List<org.greenpole.hibernate.entity.DividenAnnotation> getDividendAnnotations(int dividendId);

    public List<org.greenpole.hibernate.entity.Coupon> getCoupon(String descriptor, org.greenpole.hibernate.entity.Coupon coupon, Map<String, Double> redemptionAmount, Map<String, Double> couponAmount);

    public List<org.greenpole.hibernate.entity.Stockbroker> getStockBrokers(int holderId);

    public List<org.greenpole.hibernate.entity.HolderBondAccount> getAllBondsAccountByClientCompanyId(int clientCompanyId);

    public boolean updateCaution(int holderId, int cautionId);

    public boolean checkCaution(int holderId, int cautionId);

    public boolean rightsIssueAppFromSetup(org.greenpole.hibernate.entity.RightsIssueApplication right);

    public List<org.greenpole.hibernate.entity.Dividend> getAllMarkedDividends(String descriptor, String startDate, String endDate, String dateformat);

    public List<org.greenpole.hibernate.entity.DividenAnnotation> getAllDividendsAnnotation(int dividendId);

    public List<org.greenpole.hibernate.entity.Dividend> getAllDividends();

    public void invalidatDividend(int dividendId);

    public org.greenpole.hibernate.entity.Dividend getOneDividendRecord(int dividendId, int holderCompanyAccountId);

    public boolean revalidateDividend(int dividendId, int holderCompanyAccountId);

    public boolean checkAgainstUploadingSameDivRecord(int clientCompanyId, int dividendDeclaredId, int warrantNumber);
}
