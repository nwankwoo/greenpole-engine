/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.greenpole.entity.security.Login;
import org.greenpole.hibernate.entity.Holder;
import org.greenpole.entity.model.holder.PowerOfAttorney;
import org.greenpole.hibernate.entity.AccountConsolidation;
import org.greenpole.hibernate.entity.Administrator;
import org.greenpole.hibernate.entity.AdministratorEmailAddress;
import org.greenpole.hibernate.entity.AdministratorPhoneNumber;
import org.greenpole.hibernate.entity.AdministratorPostalAddress;
import org.greenpole.hibernate.entity.AdministratorResidentialAddress;
import org.greenpole.hibernate.entity.BondOffer;
import org.greenpole.hibernate.entity.BondType;
import org.greenpole.hibernate.entity.Caution;
import org.greenpole.hibernate.entity.ClearingHouse;
import org.greenpole.hibernate.entity.ClientCompany;
import org.greenpole.hibernate.entity.CompanyAccountConsolidation;
import org.greenpole.hibernate.entity.Coupon;
import org.greenpole.hibernate.entity.DividenAnnotation;
import org.greenpole.hibernate.entity.Dividend;
import org.greenpole.hibernate.entity.DividendIssueType;
import org.greenpole.hibernate.entity.HolderBondAccount;
import org.greenpole.hibernate.entity.HolderCompanyAccount;
import org.greenpole.hibernate.entity.HolderEmailAddress;
import org.greenpole.hibernate.entity.HolderPhoneNumber;
import org.greenpole.hibernate.entity.HolderPostalAddress;
import org.greenpole.hibernate.entity.HolderResidentialAddress;
import org.greenpole.hibernate.entity.HolderType;
import org.greenpole.hibernate.entity.InitialPublicOffer;
import org.greenpole.hibernate.entity.IpoApplication;
import org.greenpole.hibernate.entity.PrivatePlacement;
import org.greenpole.hibernate.entity.PrivatePlacementApplication;
import org.greenpole.hibernate.entity.ProcessedTransaction;
import org.greenpole.hibernate.entity.ProcessedTransactionHolder;
import org.greenpole.hibernate.entity.RightsIssue;
import org.greenpole.hibernate.entity.RightsIssueApplication;
import org.greenpole.hibernate.entity.Stockbroker;
import org.greenpole.hibernate.entity.TransactionType;
import org.greenpole.hibernate.query.GeneralisedAbstractDao;

/**
 *
 * @author user
 */
public class HibernateDummyQuery extends GeneralisedAbstractDao implements HibernatDummyQuerInterface {

    @Override
    public void createAdministratorForShareHolderAndBondHolder(Administrator admin, List<AdministratorEmailAddress> emailAddress, List<AdministratorPhoneNumber> phoneNumber, List<AdministratorResidentialAddress> residentialAddress, List<Holder> holder) {
    }

    @Override
    public void createEmail(AdministratorEmailAddress emailAddress) {
    }

    /**
     *
     * @param phoneNumber
     */
    @Override
    public void createPhoneNumber(AdministratorPhoneNumber phoneNumber) {
    }

    @Override
    public void createAdministratorResidentialAddress(AdministratorResidentialAddress adminResidentialAddress) {
    }

    @Override
    public void updateAdministrationHolderCompanyAccount(org.greenpole.hibernate.entity.Holder holder) {
    }

    @Override
    public Holder retrieveHolderObject(int holderId) {
        Holder holder = new Holder();
        return holder;
    }

    @Override
    public void uploadPowerOfAttorney(PowerOfAttorney power) {
    }

    @Override
    public boolean checkHolderNubanNumber(String nubanAccount) {
        boolean bool = false;
        return bool;
    }

    @Override
    public List<HolderCompanyAccount> getAllShareholderNubanAccounts() {
        List<org.greenpole.hibernate.entity.HolderCompanyAccount> holderAccount = new ArrayList();
        return holderAccount;
    }

    @Override
    public List<HolderBondAccount> getAllBondholderNubanAccounts() {
        List<HolderBondAccount> bondHolder = new ArrayList();
        return bondHolder;
    }

    @Override
    public void addShareholderNubanAccount() {
    }

    @Override
    public void createNubanAccount(HolderCompanyAccount holderAccount) {
    }

    @Override
    public void createBondNubanAccount(HolderBondAccount holderAccount) {
    }

    @Override
    public void changeShareholderNubanAccount(HolderCompanyAccount holderAccount) {

    }

    @Override
    public void changeBondholderNubanAccount(HolderBondAccount bondholderAccount) {

    }

    /**
     * @param holderId
     * @param clientCompanyId
     * @return
     * @Override public org.greenpole.hibernate.entity.HolderChanges
     * getHolderEditedDetails(int holderId){ return null; }
     * @Override public org.greenpole.hibernate.entity.HolderChanges
     * retrieveHolderChangesQueryOne(String changeType, String changeDate, int
     * holderId){ return null; }
     * @Override public org.greenpole.hibernate.entity.HolderChanges
     * retrieveHolderChangesQueryTwo(String changeType, String changeDate1,
     * String changeDate2, int holderId){ return null; }
     */
    @Override
    public HolderCompanyAccount retrieveHolderCompanyAccount(int holderId, int clientCompanyId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public HolderBondAccount retrieveHolderBondCompAccount(int holderId, int bondId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<CompanyAccountConsolidation> queryAccountConsolidation(String descriptor, CompanyAccountConsolidation compAccCon, String start_date, String end_date) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Login> getUserList(List<Login> login) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<AccountConsolidation> queryAccCon(int holderId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * finds power of attorney with the specified holder ID and updates the
     * isPrimaryPowerOfAttorney to false
     *
     * @param holderId the holder Id whose power of attorney is to be updated.
     * @return boolean object
     */
    @Override
    public boolean updatePowerOfAttorneyStatus(int holderId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * retrieves the power of attorney for the specified holder
     *
     * @param holderId the Id of the holder's power of attorney to be retrieved
     * @return the hibernate entity object.
     */
    @Override
    public org.greenpole.hibernate.entity.PowerOfAttorney retrieveCurrentPowerOfAttorney(int holderId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Returns the holder type
     *
     * @param holderTypeId the holder type Id
     * @return the hibernate entity of the type of holder
     */
    @Override
    public HolderType getHolderType(int holderTypeId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * saves cautioned details pertaining to a particular holder
     *
     * @param caution the details of the caution
     */
    @Override
    public void cautionShareHolderAndBondholder(Caution caution) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Caution> queryCautionedHolders(String descriptor, Caution searchParams, String startDate, String endDate) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Sets up of rights issue
     *
     * @param rightsIssue
     * @return
     */
    @Override
    public boolean setUp_RightIssue(RightsIssue rightsIssue) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * retrieves list of all holders of a particular client company
     *
     * @param clientCompanyId
     * @return the holders details to retrieved
     */
    @Override
    public List<org.greenpole.hibernate.entity.Holder> getHoldersByClientCompanyId(int clientCompanyId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * retrieves list of all holders company account details of a particular
     * client company
     *
     * @param clientCompanyId the holders details to retrieved
     * @return
     */
    @Override
    public List<org.greenpole.hibernate.entity.HolderCompanyAccount> getHoldersShareUnitsByClientCompanyId(int clientCompanyId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<RightsIssueApplication> getHoldersRightsIssueApplicationByClientCompany(int clientCompanyId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean removeClientCompany(int clientCompanyId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean updateClientCompanyValidStatus(int clientCompanyId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public HolderCompanyAccount getHolderCompanyAccount(int holderId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public BondOffer getBondOfferId(int clientCompanyId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Holder getHolderIdFromHolderBondAccount(int bondOfferId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean hasBondAccounts(int holderId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean hasCompanyAccounts(int holderId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<HolderBondAccount> getAllBondAccounts(int holderId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<HolderCompanyAccount> getAllCompAccounts(int holderId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Holder getHolder(int holderId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean mergeClientCompanyAccounts(ClientCompany pryClientComp, List<ClientCompany> secClientComps, List<HolderCompanyAccount> secHolderCompAccts, List<HolderBondAccount> secHolderBondAccts) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<HolderCompanyAccount> getAllHolderCompanyAccountsByClientCompanyId(int clientCompanyId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<InitialPublicOffer> getInitialPublicOfferByClientCompanyId(int clientCompanyId, String descriptor, String startDate, String endDate, String dateFormat) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<IpoApplication> getAllIpoApplication(int ipoId, int clientCompanyId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ClearingHouse getClearingHouse(int clearingHouseId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Holder> queryShareholders(String descriptor, int clientCompanyId, int startAge, int endAge) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    

    
    @Override
    public List<HolderCompanyAccount> getHolderCompanyAccounts(int holderId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<HolderEmailAddress> getHolderEmailAddresses(int holderId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<HolderPhoneNumber> getHolderPhoneNumbers(int holderId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<HolderResidentialAddress> getHolderResidentialAddresses(int holderId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<HolderPostalAddress> getHolderPostalAddresses(int holderId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Administrator> getHolderAdministrators(int holderId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<HolderBondAccount> getHolderBondAccounts(int holderId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<AdministratorEmailAddress> getAdministratorEmail(int administratorId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<AdministratorPhoneNumber> getAdministratorPhone(int administratorId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<AdministratorPostalAddress> getAdministratorPostalAddress(int administratorId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<AdministratorResidentialAddress> getAdministratorResidentialAddress(int administratorId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean RightIssueApplication(List<RightsIssueApplication> rightApp) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateRightIssueSetup(int rightsIssueId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RightsIssue getRightsIssueById(int rightsIssueId, int clientCompanyId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean checkRightsIssue(int rightsIssueId, int clientCompanyId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<HolderCompanyAccount> getHolderCompanyAccountsByClientCompanyId(int clientCompanyId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean updateHCA(int holderId, double sharesToadd) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


    @Override
    public HolderCompanyAccount getOneHolderCompanyAccount(int holderId, int clientCompanyId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public RightsIssueApplication getOneHolderRightApplication(int holderId, int clientCompanyId, int rightAppId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<RightsIssueApplication> getAllRightsIssueApplications(int clientCompanyId, int rightAppId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean checkHolderCompanyAccount(int holderId, int clientCompanyId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<RightsIssue> getRightsIssue(int clientCompanyId, String descriptor, String startDate, String endDate, String dateFormat) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<PrivatePlacementApplication> getPrivatePlacementApplication(int clientCompanyId, int ppAppId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<PrivatePlacement> getPrivatePlacement(int clientCompanyId, String descriptor, String startDate, String endDate, String dateFormat) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void applyForRightIssue(RightsIssueApplication rightsIssueApplication) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean updateCancelleRightsApp(int clientCompanyId, int holderId, int rightsIssueId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateRightIssueTotalShares(int clientCompanyId, int rightsIssueId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

   

    @Override
    public void updateShareholderRightsIssueApplication(int holderId, int clientCompanyId, int rightsIssueId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean uploadRightsApplicationEnmass(List<RightsIssueApplication> applicationList) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean checkHolderRightsApplication(int holderId, int clientCompanyId, int rightAppId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void updateHCA(int holderId, int clientCompanyId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


    @Override
    public boolean createNewHolder(Holder holder) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean uploadTransaction(List<ProcessedTransaction> processedTransaction) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean checkCSCSTransactionExistence(int cscsTransactionId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean checkCertExistince(String certNumber) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ProcessedTransaction getProcessedTransaction(int processedTransactionId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public TransactionType getTransactionType(int transactionId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<ProcessedTransaction> queryTransaction(String descriptor, ProcessedTransaction pth, String start_date, String end_date, Map<String, Integer> shareUnitSoldCriteria, Map<String, Integer> shareUnitBoughtCriteria, Map<String, Integer> bondUnitSoldCriteria, Map<String, Integer> bondUnitBoughtCriteria) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    @Override
    public List<ProcessedTransactionHolder> getProcessedTransactionHolder(int procesedTransactionId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean configureDividendType(DividendIssueType diviType) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean configureCouponType(BondType couponType) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Dividend getDividendAnnotation(int dividendId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void createDividendAnnotation(DividenAnnotation divi) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Dividend> queryDividend(String descriptor, Dividend divi, Map<String, Double> grossAmount, Map<String, Double> tax, Map<String, Double> payableAmount) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<DividenAnnotation> getDividendAnnotations(int dividendId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Coupon> getCoupon(String descriptor, Coupon coupon, Map<String, Double> redemptionAmount, Map<String, Double> couponAmount) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Stockbroker> getStockBrokers(int holderId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<HolderBondAccount> getAllBondsAccountByClientCompanyId(int clientCompanyId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean updateCaution(int holderId, int cautionId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean checkCaution(int holderId, int cautionId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean rightsIssueAppFromSetup(RightsIssueApplication right) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Dividend> getAllMarkedDividends(String descriptor, String startDate, String endDate, String dateformat) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<DividenAnnotation> getAllDividendsAnnotation(int dividendId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Dividend> getAllDividends() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void invalidatDividend(int dividendId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Dividend getOneDividendRecord(int dividendId, int holderCompanyAccountId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
/**
 * revalidates an invalidated dividend record
 * @param dividendId the dividend Id to be revalidated
 * @param holderCompanyAccountId the account to revalidate it dividend
 * @return true if revalidated else false
 */
    @Override
    public boolean revalidateDividend(int dividendId, int holderCompanyAccountId) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean checkAgainstUploadingSameDivRecord(int clientCompanyId, int dividendDeclaredId, long warrantNumber) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
}
