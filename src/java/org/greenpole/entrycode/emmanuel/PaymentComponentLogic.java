/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.greenpole.entity.model.clientcompany.BondType;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.entrycode.emmanuel.model.Coupon;
import org.greenpole.entrycode.emmanuel.model.DividenAnnotation;
import org.greenpole.entrycode.emmanuel.model.Dividend;
import org.greenpole.entrycode.emmanuel.model.DividendIssueType;
import org.greenpole.entrycode.emmanuel.model.QueryCoupon;
import org.greenpole.entrycode.emmanuel.model.QueryDividend;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.GeneralComponentQuery;
import org.greenpole.hibernate.query.HolderComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.util.Descriptor;
import org.greenpole.util.properties.GreenpoleProperties;
import org.greenpole.util.properties.NotificationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author user
 */
public class PaymentComponentLogic {

    private static final Logger logger = LoggerFactory.getLogger(HolderComponentLogic.class);
    private final ClientCompanyComponentQuery cq = ComponentQueryFactory.getClientCompanyQuery();
    private final GeneralComponentQuery gq = ComponentQueryFactory.getGeneralComponentQuery();
    private final NotificationProperties notificationProp = new NotificationProperties(ClientCompanyLogic.class);
    private final HolderComponentQuery hq = ComponentQueryFactory.getHolderComponentQuery();
    private final GreenpoleProperties greenProp = new GreenpoleProperties(org.greenpole.logic.HolderComponentLogic.class);
    private final HibernatDummyQuerInterface hd = HibernateDummyQueryFactory.getHibernateDummyQuery();//not needed

    /**
     *
     * @param login the user login details
     * @param dividendType the dividend issue type to configure
     * @return response to the configuring of dividend issue type
     */
    public Response configureDividendType_Request(Login login, DividendIssueType dividendType) {
        Response resp = new Response();
        logger.info("request to configure dividend type invoked by ", login.getUserId());
        try {
            boolean created = false;
            if (dividendType.getDividendType() != null && !dividendType.getDividendType().isEmpty()) {
                if (dividendType.getDescription() != null && !dividendType.getDescription().isEmpty()) {
                    org.greenpole.hibernate.entity.DividendIssueType diviType_hib = new org.greenpole.hibernate.entity.DividendIssueType();
                    diviType_hib.setDividendType(dividendType.getDividendType());
                    diviType_hib.setDescription(dividendType.getDescription());
                    created = hd.configureDividendType(diviType_hib);
                    if (created) {
                        resp.setRetn(0);
                        resp.setDesc("Successful");
                        logger.info("Successfully created dividend issue type ", login.getUserId());
                        return resp;
                    }
                    if (!created) {
                        resp.setRetn(300);
                        resp.setDesc("Unable to configure dividend issue type ");
                        logger.info("Unable to configure dividend issue type ", login.getUserId());
                        return resp;
                    }
                }
                resp.setRetn(300);
                resp.setDesc("Dividend issue description should not be empty ");
                logger.info("Dividend issue description should not be empty", login.getUserId());
                return resp;
            }
            resp.setRetn(300);
            resp.setDesc("Dividend issue type should not be empty ");
            logger.info("Dividend issue type should not be empty", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("Error configuring dividend issue type. See error log - [{}]", login.getUserId());
            logger.error("Error configuring dividend issue type - [" + login.getUserId() + "] - ", ex);
            resp.setRetn(99);
            resp.setDesc("General error: Unable to configure dividend issue type, please contact the system administrator." + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     *
     * @param login the user login details
     * @param bondType
     * @return response to the configuring of dividend issue type
     */
    public Response configureCouponType_Request(Login login, BondType bondType) {
        Response resp = new Response();
        logger.info("request to configure dividend type invoked by ", login.getUserId());
        try {
            boolean created = false;
            if (bondType.getType() != null && !bondType.getType().isEmpty()) {
                if (bondType.getDescription() != null && !bondType.getDescription().isEmpty()) {
                    org.greenpole.hibernate.entity.BondType bondType_hib = new org.greenpole.hibernate.entity.BondType();
                    bondType_hib.setType(bondType.getType());
                    bondType_hib.setDescription(bondType.getDescription());
                    created = hd.configureCouponType(bondType_hib);
                    if (created) {
                        resp.setRetn(0);
                        resp.setDesc("Successful");
                        logger.info("Successfully configured coupon type ", login.getUserId());
                        return resp;
                    }
                    if (!created) {
                        resp.setRetn(300);
                        resp.setDesc("Unable to configure coupon type ");
                        logger.info("Unable to configure coupon type ", login.getUserId());
                        return resp;
                    }
                }
                resp.setRetn(300);
                resp.setDesc("Coupon description should not be empty ");
                logger.info("Coupon description should not be empty", login.getUserId());
                return resp;
            }
            resp.setRetn(300);
            resp.setDesc("Coupon type should not be empty ");
            logger.info("Coupon issue type should not be empty", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("Error configuring coupon type. See error log - [{}]", login.getUserId());
            logger.error("Error configuring coupon type - [" + login.getUserId() + "] - ", ex);
            resp.setRetn(99);
            resp.setDesc("General error: Unable to configure coupon type, please contact the system administrator." + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    public Response AnnotateDividend_Request(Login login, DividenAnnotation dividendAnnotation) {
        Response resp = new Response();
        logger.info("annotation of dividend invoked by ", login.getUserId());
        try {
            org.greenpole.hibernate.entity.DividenAnnotation divi_hib = new org.greenpole.hibernate.entity.DividenAnnotation();
            org.greenpole.hibernate.entity.Dividend da = hd.getDividendAnnotation(dividendAnnotation.getDividendId());
            if (dividendAnnotation.getDividendId() > 0) {
                if (dividendAnnotation.getAnnotation() != null && !dividendAnnotation.getAnnotation().isEmpty()) {
                    divi_hib.setAnnotation(dividendAnnotation.getAnnotation());
                    divi_hib.setDividend(da);
                    hd.createDividendAnnotation(divi_hib);
                    resp.setRetn(0);
                    resp.setDesc("Successful annotation of dividend ");
                    logger.info("Successful annotation of dividend ", login.getUserId());
                    return resp;
                }
                resp.setRetn(300);
                resp.setDesc("Unable to annotate dividend ");
                logger.info("Unable to annotate dividend ", login.getUserId());
                return resp;
            }
            resp.setRetn(300);
            resp.setDesc("Annotation should not be empty ");
            logger.info("Annotation should not be empty ", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("Error annotating dividend. See error log - [{}]", login.getUserId());
            logger.error("Error annotating dividend - [" + login.getUserId() + "] - ", ex);
            resp.setRetn(99);
            resp.setDesc("General error: Unable to annotate dividend, please contact the system administrator." + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    public Response queryDividend_Request(Login login, QueryDividend queryParams) {
        Response resp = new Response();
        logger.info("querying of dividend details invoked by ", login.getUserId());
        Descriptor descriptorUtil = new Descriptor();
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        org.greenpole.hibernate.entity.Dividend div_hib_search = new org.greenpole.hibernate.entity.Dividend();
        try {
            if (queryParams.getDescriptor() == null || "".equals(queryParams.getDescriptor())) {
                logger.info("Uploade transaction query unsuccessful. Empty descriptor - [{}]", login.getUserId());
                resp.setRetn(300);
                resp.setDesc("Unsuccessful transaction query, due to empty descriptor. Contact system administrator");
                return resp;
            }

            Map<String, String> descriptors = descriptorUtil.decipherDescriptor(queryParams.getDescriptor());
            if (descriptors.size() == 6) {
                Dividend div_model_search = new Dividend();
                if (queryParams.getDividend() != null) {
                    div_model_search = queryParams.getDividend();
                    org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(div_model_search.getClientCompanyId());
                    div_hib_search.setClientCompName(div_model_search.getClientCompName());
                    div_hib_search.setIssueType(div_model_search.getIssueType());
                    div_hib_search.setIssued(true);
                    div_hib_search.setPaid(true);
                    try {
                        div_hib_search.setIssueDate(formatter.parse(div_model_search.getIssueDate()));
                    } catch (Exception ex) {
                        logger.info("an error was thrown while checking the dividend issue date. See error log - [{}]", login.getUserId());
                        resp.setRetn(308);
                        resp.setDesc("Incorrect date format for issue date");
                        logger.error("Incorrect date format for issue date [" + login.getUserId() + "]", ex);
                    }
                    div_hib_search.setDivNumber(div_model_search.getDivNumber());
                    div_hib_search.setYearType(div_model_search.getYearType());//for Interim/final search
                    div_hib_search.setWarrantNumber(div_model_search.getWarrantNumber());
                    div_hib_search.setSHolderMailingAddr(div_model_search.getSHolderMailingAddr());
                    try {
                        div_hib_search.setPayableDate(formatter.parse(div_model_search.getPayableDate()));
                    } catch (Exception ex) {
                        logger.info("an error was thrown while checking the dividend payable date. See error log - [{}]", login.getUserId());
                        resp.setRetn(308);
                        resp.setDesc("Incorrect date format for dividend payable date");
                        logger.error("Incorrect date format for dividend payable date [" + login.getUserId() + "]", ex);
                    }

                }
                Map<String, Double> grossAmount_search;
                if (queryParams.getGrossAmount() != null && !queryParams.getGrossAmount().isEmpty()) {
                    grossAmount_search = queryParams.getGrossAmount();
                } else {
                    grossAmount_search = new HashMap<>();
                }
                Map<String, Double> tax_search;
                if (queryParams.getTax() != null && !queryParams.getTax().isEmpty()) {
                    tax_search = queryParams.getTax();
                } else {
                    tax_search = new HashMap<>();
                }
                Map<String, Double> payableAmount_search;
                if (queryParams.getPayableAmount() != null && !queryParams.getPayableAmount().isEmpty()) {
                    payableAmount_search = queryParams.getPayableAmount();
                } else {
                    payableAmount_search = new HashMap<>();
                }
                List<Dividend> dividend_model_list_out = new ArrayList<>();
                List<org.greenpole.hibernate.entity.Dividend> dividend_hib_list = hd.queryDividend(queryParams.getDescriptor(), div_hib_search, grossAmount_search, tax_search, payableAmount_search);
                for (org.greenpole.hibernate.entity.Dividend div : dividend_hib_list) {
                    Dividend di = new Dividend();
                    org.greenpole.hibernate.entity.HolderCompanyAccount hca = hq.getHolderCompanyAccount(div.getHolderCompanyAccount().getHolder().getId(), div.getClientCompany().getId());
                    di.setClientCompName(div.getClientCompName());
                    di.setIssueType(div.getIssueType());
                    di.setIssued(div.getIssued());
                    di.setPaid(div.getPaid());
                    di.setDivNumber(div.getDivNumber());
                    di.setYearType(div.getYearType());
                    di.setWarrantNumber(div.getWarrantNumber());
                    di.setSHolderMailingAddr(div.getSHolderMailingAddr());
                    di.setPayableDate(formatter.format(div.getPayableDate()));
                    di.setCompAccHoldings(div.getCompAccHoldings());
                    di.setCancelled(div.getCancelled());
                    di.setCanelledDate(formatter.format(div.getCanelledDate()));
                    di.setDividendDeclaredId(div.getDividendDeclared().getId());
                    di.setClientCompanyId(div.getClientCompany().getId());
                    List<DividenAnnotation> div_ann_list_out = new ArrayList<>();
                    List<org.greenpole.hibernate.entity.DividenAnnotation> divAnn_list_hib = hd.getDividendAnnotations(div.getId());
                    for (org.greenpole.hibernate.entity.DividenAnnotation di_ann : divAnn_list_hib) {
                        DividenAnnotation di_ann_model = new DividenAnnotation();
                        di_ann_model.setAnnotation(di_ann.getAnnotation());
                        div_ann_list_out.add(di_ann_model);
                    }
                    di.setDividendAnnotation(div_ann_list_out);
                    dividend_model_list_out.add(di);

                }
                logger.info("Dividend query successful - [{}]", login.getUserId());
                resp.setRetn(0);
                resp.setDesc("Successful");
                resp.setBody(dividend_model_list_out);
                return resp;
            }
        } catch (Exception ex) {
            logger.info("error querying dividend. See error log - [{}]", login.getUserId());
            logger.error("error querying dividend - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to query dividend. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
        return resp;
    }
     public Response queryCoupon_Request(Login login, QueryCoupon queryParams) {
        Response resp = new Response();
        logger.info("querying of coupon details invoked by ", login.getUserId());
        Descriptor descriptorUtil = new Descriptor();
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        org.greenpole.hibernate.entity.Dividend div_hib_search = new org.greenpole.hibernate.entity.Dividend();
        org.greenpole.hibernate.entity.Coupon coup_hib_search = new org.greenpole.hibernate.entity.Coupon();
        try {
            if (queryParams.getDescriptor() == null || "".equals(queryParams.getDescriptor())) {
                logger.info("Uploade transaction query unsuccessful. Empty descriptor - [{}]", login.getUserId());
                resp.setRetn(300);
                resp.setDesc("Unsuccessful transaction query, due to empty descriptor. Contact system administrator");
                return resp;
            }

            Map<String, String> descriptors = descriptorUtil.decipherDescriptor(queryParams.getDescriptor());
            if (descriptors.size() == 6) {
                Coupon coup_model_search = new Coupon();
                if (queryParams.getCoupon() != null) {
                    coup_model_search = queryParams.getCoupon();
                    //org.greenpole.hibernate.entity.ClientCompany cc = cq.getClientCompany(div_model_search.getClientCompanyId());
                    coup_hib_search.setClientCompanyName(coup_model_search.getClientCompanyName());
                    coup_hib_search.setCouponNumber(coup_model_search.getCouponNumber());
                    coup_hib_search.setBondType(coup_model_search.getBondType());
                    coup_hib_search.setBondholderMailingAddress(coup_model_search.getBondholderMailingAddress());
                    
                    try {
                        coup_hib_search.setIssueDate(formatter.parse(coup_model_search.getIssueDate()));
                    } catch (Exception ex) {
                        logger.info("an error was thrown while checking the dividend issue date. See error log - [{}]", login.getUserId());
                        resp.setRetn(308);
                        resp.setDesc("Incorrect date format for issue date");
                        logger.error("Incorrect date format for issue date [" + login.getUserId() + "]", ex);
                    }
                    try {
                        coup_hib_search.setRedemptnDate(formatter.parse(coup_model_search.getRedemptnDate()));
                    } catch (Exception ex) {
                        logger.info("an error was thrown while checking the redemption date. See error log - [{}]", login.getUserId());
                        resp.setRetn(308);
                        resp.setDesc("Incorrect date format for redemption date");
                        logger.error("Incorrect date format for redemption date [" + login.getUserId() + "]", ex);
                    }

                }
                Map<String, Double> redemptionAmount_search;
                if (queryParams.getRedemptionAmount() != null && !queryParams.getRedemptionAmount().isEmpty()) {
                    redemptionAmount_search = queryParams.getRedemptionAmount();
                } else {
                    redemptionAmount_search = new HashMap<>();
                }
                Map<String, Double> couponAmount_search;
                if (queryParams.getCouponAmount() != null && !queryParams.getCouponAmount().isEmpty()) {
                    couponAmount_search = queryParams.getCouponAmount();
                } else {
                    couponAmount_search = new HashMap<>();
                }
                List<Coupon> coupon_model_list_out = new ArrayList<>();
                List<org.greenpole.hibernate.entity.Coupon> coupon_hib_list = hd.getCoupon(queryParams.getDescriptor(), coup_hib_search, redemptionAmount_search, couponAmount_search);
                for (org.greenpole.hibernate.entity.Coupon cou : coupon_hib_list) {
                    Coupon co = new Coupon();
                    //org.greenpole.hibernate.entity.HolderCompanyAccount hca = hq.getHolderCompanyAccount(div.getHolderCompanyAccount().getHolder().getId(), div.getClientCompany().getId());
                    co.setClientCompanyName(cou.getClientCompanyName());
                    co.setIssueDate(formatter.format(cou.getIssueDate()));
                    co.setCouponNumber(cou.getCouponNumber());
                    co.setBondType(cou.getBondType());
                    co.setBondholderMailingAddress(cou.getBondholderMailingAddress());
                    co.setRedemptnDate(formatter.format(cou.getRedemptnDate()));
                    co.setCouponAmt(cou.getCouponAmt());
                    co.setRedemptionAmt(cou.getRedemptionAmt());
                    coupon_model_list_out.add(co);
                }
                logger.info("Coupon query successful - [{}]", login.getUserId());
                resp.setRetn(0);
                resp.setDesc("Successful");
                resp.setBody(coupon_model_list_out);
                return resp;
            }
        } catch (Exception ex) {
            logger.info("error querying coupon. See error log - [{}]", login.getUserId());
            logger.error("error querying coupon - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to query coupon. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
        return resp;
    }
}
