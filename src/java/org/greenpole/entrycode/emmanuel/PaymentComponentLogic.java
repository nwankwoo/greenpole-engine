/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.greenpole.entity.model.clientcompany.BondType;
import org.greenpole.entity.model.taguser.TagUser;
import org.greenpole.entity.notification.NotificationMessageTag;
import org.greenpole.entity.notification.NotificationWrapper;
import org.greenpole.entity.response.Response;
import org.greenpole.entity.security.Login;
import org.greenpole.entrycode.emmanuel.model.Coupon;
import org.greenpole.entrycode.emmanuel.model.DividenAnnotation;
import org.greenpole.entrycode.emmanuel.model.Dividend;
import org.greenpole.entrycode.emmanuel.model.DividendIssueType;
import org.greenpole.entrycode.emmanuel.model.QueryCoupon;
import org.greenpole.entrycode.emmanuel.model.QueryDividend;
import org.greenpole.hibernate.entity.HolderCompanyAccountId;
import org.greenpole.hibernate.query.ClientCompanyComponentQuery;
import org.greenpole.hibernate.query.GeneralComponentQuery;
import org.greenpole.hibernate.query.HolderComponentQuery;
import org.greenpole.hibernate.query.factory.ComponentQueryFactory;
import org.greenpole.notifier.sender.QueueSender;
import org.greenpole.util.Descriptor;
import org.greenpole.util.Notification;
import org.greenpole.util.properties.GreenpoleProperties;
import org.greenpole.util.properties.NotificationProperties;
import org.greenpole.util.properties.NotifierProperties;
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
            int counter;
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

    public Response viewMarkedDividends_Request(Login login, QueryDividend queryParams) {
        logger.info("request to query marked dividends , invoked by [{}] ", login.getUserId());
        Response resp = new Response();
        Descriptor descriptorUtil = new Descriptor();
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());

            Map<String, String> descriptors = descriptorUtil.decipherDescriptor(queryParams.getDescriptor());
            if (descriptors.size() == 1) {
                if (descriptors.get("date").equalsIgnoreCase("none")) {
                    try {
                        formatter.parse(queryParams.getStartDate());
                    } catch (ParseException ex) {
                        logger.info("an error was thrown while checking the start date. See error log invoked by [{}]", login.getUserId());
                        resp.setRetn(300);
                        resp.setDesc("Incorrect date format for start date");
                        logger.error("Incorrect date format for start date invoked by [{}]", login.getUserId(), ex);

                        return resp;
                    }
                }
                //check end date is properly formatted
                if (descriptors.get("date").equalsIgnoreCase("between")) {
                    try {
                        formatter.parse(queryParams.getEndDate());
                    } catch (ParseException ex) {
                        logger.info("an error was thrown while checking the start date. See error log - [{}]", login.getUserId());
                        resp.setRetn(300);
                        resp.setDesc("Incorrect date format for end date");
                        logger.error("Incorrect date format for end date - [{}]", login.getUserId(), ex);

                        return resp;
                    }
                }
                List<org.greenpole.hibernate.entity.Dividend> div_hib_list = hd.getAllMarkedDividends(queryParams.getDescriptor(), queryParams.getStartDate(),
                        queryParams.getEndDate(), greenProp.getDateFormat());
                TagUser tag = new TagUser();
                List<Dividend> div_model_list_out = new ArrayList<>();
                for (org.greenpole.hibernate.entity.Dividend div : div_hib_list) {
                    Dividend div_model = new Dividend();
                    div_model.setId(div.getId());
                    div_model.setClientCompName(null);
                    div_model.setCompAccHoldings(div.getCompAccHoldings());
                    div_model.setDivNumber(div.getDivNumber());
                    div_model.setDividendDeclaredId(div.getDividendDeclared().getId());
                    div_model.setDividendIssueTypeId(div.getDividendIssueType().getId());
                    div_model.setGrossAmount(div.getGrossAmount());
                    div_model.setIssueType(div.getIssueType());
                    div_model.setPaymentMethod(div.getPaymentMethod());
                    div_model.setYearEnding(formatter.format(div.getYearEnding()));
                    div_model.setYearType(div.getYearType());
                    div_model.setPayableAmount(div.getPayableAmount());
                    div_model.setRate(div.getRate());
                    div_model.setPayableDate(formatter.format(div.getPayableDate()));
                    div_model.setSHolderMailingAddr(div.getSHolderMailingAddr());
                    if (div.getIssueDate() != null) {
                        div_model.setIssueDate(formatter.format(div.getIssueDate()));
                    }
                    if (div.getPaymentMethod() != null && !"".equals(div.getPaymentMethod()) && !div.getPaid()) {//unpaid dividends for e-payment
                        div_model.setPayableDate(formatter.format(div.getPayableDate()));
                    }
                    if (!div.getPaid() && div.getIssued())//if issued but not paid
                    {
                        div_model.setIssueDate(formatter.format(div.getIssuedDate()));
                    }
                    if (div.getPaidDate() != null && div.getPaid())//paid dividend
                    {
                        div_model.setPaidDate(formatter.format(div.getPaidDate()));
                    }
                    if (div.getUnclaimed())//if not claimed
                    {
                        div_model.setUnclaimedDate(formatter.format(div.getUnclaimedDate()));
                    }
                    if (div.getReIssuedDate() != null && div.getReIssued()) {//re-issued dividend
                        div_model.setReIssuedDate(formatter.format(div.getReIssuedDate()));
                    }
                    if (div.getReIssuedDate() != null && !div.getPaid()) {//re-issued but not paid dividends
                        div_model.setReIssuedDate(formatter.format(div.getReIssuedDate()));
                    }
                    List<DividenAnnotation> dividend_ann_model_out = new ArrayList<>();
                    for (org.greenpole.hibernate.entity.DividenAnnotation dv : hd.getAllDividendsAnnotation(div.getId())) {
                        DividenAnnotation dv_model = new DividenAnnotation();
                        dv_model.setAnnotation(dv.getAnnotation());
                        dv_model.setId(dv.getId());
                        dividend_ann_model_out.add(dv_model);
                    }
                    div_model.setDividendAnnotation(dividend_ann_model_out);
                    div_model_list_out.add(div_model);
                }
                List<TagUser> tagList = new ArrayList<>();

                tag.setQueryParam(queryParams);
                tag.setResult(div_model_list_out);
                tagList.add(tag);

                resp.setBody(tagList);
                resp.setDesc("Query Successful");
                resp.setRetn(0);
                logger.info("Query successful - [{}]", login.getUserId());
                return resp;
            }
        } catch (Exception ex) {
            logger.info("error querying marked dividends. See error log - [{}]", login.getUserId());
            logger.error("error querying marked dividends - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to query marked dividends . Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
        return resp;
    }

    public static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInDays = date1.getTime() - date2.getTime();
        return timeUnit.convert(diffInDays, TimeUnit.MILLISECONDS);
    }

    public void monitorAndInvalidateDividend() {
        Response resp = new Response();
        try {
            long millis = System.currentTimeMillis();
            Date current_date = new java.sql.Date(millis);
            //boolean paidStatus = false;
            int month = 0, days = 0;
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            List<org.greenpole.hibernate.entity.Dividend> dividends_hib_list = hd.getAllDividends();
            if (!dividends_hib_list.isEmpty()) {
                for (org.greenpole.hibernate.entity.Dividend div : dividends_hib_list) {
                    org.greenpole.hibernate.entity.Dividend div_hib = new org.greenpole.hibernate.entity.Dividend();
                    if (div.getIssued() && !div.getPaid()) {//true if not paid else false if paid but am using false
                        Date payableDate = div.getPayableDate();
                        long noOfDaysDiff = getDateDiff(current_date, payableDate, TimeUnit.DAYS);
                        month = (int) noOfDaysDiff / 30;
                        days = (int) noOfDaysDiff % 30;
                        if (month >= 6 && days > 0) {
                            div_hib.setUnclaimed(true);
                            div_hib.setUnclaimedDate(current_date);
                            hd.invalidatDividend(div.getId());
                        }
                    }
                }
            }
        } catch (Exception ex) {
        }
    }

    public Response revalidateDividend_Request(Login login, String authenticator, Dividend dividend) {
        logger.info("request to revalidate dividend invoked by - [{}]", login.getUserId());
        Response resp = new Response();
        Notification notification = new Notification();
        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;
            SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
            long millis = System.currentTimeMillis();
            Date current_date = new java.sql.Date(millis);
            boolean status = false;
            if (dividend != null) {
                org.greenpole.hibernate.entity.Dividend div = hd.getOneDividendRecord(dividend.getId(), dividend.getHolderCompanyAccountId());
                if (div.getUnclaimed()) {//checks if unclaimed is true
                    if (div.getUnclaimedDate() != null && !formatter.format(div.getUnclaimedDate()).isEmpty()) {
                        if (dividend.getPayableAmount() <= 50000) {
                            div.setPayableDate(current_date);
                            div.setUnclaimed(false);
                            div.setReIssued(true);
                            div.setReIssuedDate(current_date);
                            status = hd.revalidateDividend(div.getId(), dividend.getHolderCompanyAccountId());
                            if (status) {
                                resp.setRetn(0);
                                resp.setDesc("Dividend revalidated successfully");
                                logger.info("Dividend revalidated successfully by ", login.getUserId());
                                return resp;
                            }
                            if (!status) {
                                resp.setRetn(300);
                                resp.setDesc("Dividend revalidation failed");
                                logger.info("Dividend revalidation failed by ", login.getUserId());
                                return resp;
                            }
                        } else {
                            wrapper = new NotificationWrapper();
                            prop = new NotifierProperties(PaymentComponentLogic.class);
                            qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                                    prop.getAuthoriserNotifierQueueName());
                            List<Dividend> divList = new ArrayList();
                            divList.add(dividend);
                            wrapper.setCode(notification.createCode(login));
                            wrapper.setDescription("Revalidation of invalidated dividend record ");
                            wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                            wrapper.setFrom(login.getUserId());
                            wrapper.setTo(authenticator);
                            wrapper.setModel(divList);
                            logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                            resp = qSender.sendAuthorisationRequest(wrapper);
                            return resp;
                        }
                    }
                    resp.setRetn(300);
                    resp.setDesc("Dividend unclaimed date should not be empty ");
                    logger.info("Dividend unclaimed date should not be empty ", login.getUserId());
                    return resp;
                }
                resp.setRetn(300);
                resp.setDesc("This dividend record does not reflect it is unclaimed ");
                logger.info("This dividend record does not reflect it is unclaimed ", login.getUserId());
                return resp;
            }
            resp.setRetn(300);
            resp.setDesc("Dividend to be revalidated cannot be empty ");
            logger.info("Dividend to be revalidated cannot be empty ", login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error revalidating dividend record. See error log - [{}]", login.getUserId());
            logger.error("error revalidating dividend record - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to revalidate dividend record . Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    public Response revalidateDividend_Authorise(Login login, String notificationCode) {
        Response resp = new Response();
        logger.info("authorise revalidation of dividend, invoked by - [{}] " + login.getUserId());
        Notification notification = new Notification();
        SimpleDateFormat formatter = new SimpleDateFormat(greenProp.getDateFormat());
        long millis = System.currentTimeMillis();
        Date current_date = new java.sql.Date(millis);
        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<Dividend> divList = (List<Dividend>) wrapper.getModel();
            Dividend divModel = divList.get(0);
            org.greenpole.hibernate.entity.Dividend div = hd.getOneDividendRecord(divModel.getId(), divModel.getHolderCompanyAccountId());
            boolean status = false;
            if (div.getUnclaimed()) {//checks if unclaimed is true
                if (div.getUnclaimedDate() != null && !formatter.format(div.getUnclaimedDate()).isEmpty()) {
                    if (div.getPayableAmount() > 50000) {
                        div.setPayableDate(current_date);
                        div.setUnclaimed(false);
                        div.setReIssued(true);
                        div.setReIssuedDate(current_date);
                        status = hd.revalidateDividend(div.getId(), divModel.getHolderCompanyAccountId());
                        if (status) {
                            resp.setRetn(0);
                            resp.setDesc("Dividend revalidated successfully");
                            logger.info("Dividend revalidated successfully by ", login.getUserId());
                            return resp;
                        }
                        if (!status) {
                            resp.setRetn(300);
                            resp.setDesc("Dividend revalidation failed");
                            logger.info("Dividend revalidation failed by ", login.getUserId());
                            return resp;
                        }
                    }
                    resp.setRetn(300);
                    resp.setDesc("Dividend unclaimed amount must be above #50, 000 before authorisation request is invoked ");
                    logger.info("Dividend unclaimed amount must be above #50, 000 before authorisation request is invoked ", login.getUserId());
                    return resp;
                }
                resp.setRetn(300);
                resp.setDesc("Dividend unclaimed date should not be empty ");
                logger.info("Dividend unclaimed date should not be empty ", login.getUserId());
                return resp;
            }
            resp.setRetn(300);
            resp.setDesc("This dividend record does not reflect it is unclaimed ");
            logger.info("This dividend record does not reflect it is unclaimed ", login.getUserId());
            return resp;

        } catch (Exception ex) {
            logger.info("error revalidating dividend record from authorisation . See error log - [{}]", login.getUserId());
            logger.error("error revalidating dividend record from authorisation - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to revalidate dividend record from authorisation. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    /**
     * Processes request to upload dividend details from
     *
     * @param login
     * @param authenticator
     * @param divList
     * @return
     */
    public Response uploadPaidDividends_Request(Login login, String authenticator, List<Dividend> divList) {
        Response resp = new Response();
        logger.info("request to upload paid dividends details from U - Dividend");
        Notification notification = new Notification();
        try {
            NotificationWrapper wrapper;
            QueueSender qSender;
            NotifierProperties prop;
            int counter;
            String msgDesc = "";
            boolean exists = false;
            for (counter = 0; counter < divList.size(); counter++) {
                exists = hd.checkAgainstUploadingSameDivRecord(divList.get(counter).getClientCompanyId(), divList.get(counter).getDividendDeclaredId(), divList.get(counter).getWarrantNumber());
                if (exists) {
                    logger.info("found an existing dividend record that matches one of the records uploaded ", login.getUserId());
                    break;
                } else if (divList.get(counter).getClientCompName() == null || divList.get(counter).getClientCompName().isEmpty()) {
                    resp.setRetn(300);
                    resp.setDesc("Client company name cannot be empty");
                    return resp;
                } else if (divList.get(counter).getPayableAmount() > 0) {
                    resp.setRetn(300);
                    resp.setDesc("Please specify the amount paid");
                    return resp;
                } else if (divList.get(counter).getPaidDate() != null && !"".equals(divList.get(counter).getPaidDate())) {
                    resp.setRetn(300);
                    resp.setDesc("Please specify the date paid");
                    return resp;
                }
            }
            if (!exists) {
                wrapper = new NotificationWrapper();
                prop = new NotifierProperties(PaymentComponentLogic.class);
                qSender = new QueueSender(prop.getAuthoriserNotifierQueueFactory(),
                        prop.getAuthoriserNotifierQueueName());

                logger.info("dividend record does not exist - [{}]", login.getUserId());
                wrapper.setCode(notification.createCode(login));
                wrapper.setDescription("Upload of paid dividends details from U - Dividend ");
                wrapper.setMessageTag(NotificationMessageTag.Authorisation_request.toString());
                wrapper.setFrom(login.getUserId());
                wrapper.setTo(authenticator);
                wrapper.setModel(divList);
                logger.info("notification fowarded to queue - notification code: [{}] - [{}]", wrapper.getCode(), login.getUserId());
                resp = qSender.sendAuthorisationRequest(wrapper);
                return resp;
            }
            resp.setRetn(205);
            resp.setDesc("The dividend record with dividend warrant number [" + divList.get(counter).getWarrantNumber() + "] already exist.");
            logger.info("The dividend record with dividend warrant number already exist so cannot be uploaded - [{}]: [{}]",
                    divList.get(counter).getWarrantNumber(), login.getUserId());
            return resp;
        } catch (Exception ex) {
            logger.info("error processing upload of dividend details from U - Dividend request. See error log - [{}]", login.getUserId());
            logger.error("error processing upload of dividend details from U - Dividend request - [" + login.getUserId() + "]", ex);

            resp.setRetn(99);
            resp.setDesc("General error. Unable to process upload of dividend details from U - Dividend records request. Contact system administrator."
                    + "\nMessage: " + ex.getMessage());
            return resp;
        }
    }

    public Response uploadPaidDividends_Request(Login login, String notificationCode) {
        Response resp = new Response();
        logger.info("authorisation for upload of paid dividends details from U - Dividend invoked by [{}] - notification code: [{}]", login.getUserId(), notificationCode);
        Notification notification = new Notification();
        try {
            NotificationWrapper wrapper = notification.loadNotificationFile(notificationProp.getNotificationLocation(), notificationCode);
            List<Dividend> divList = (List<Dividend>) wrapper.getModel();
            int counter;
            boolean exists = false;
            for (counter = 0; counter < divList.size(); counter++) {
                exists = hd.checkAgainstUploadingSameDivRecord(divList.get(counter).getClientCompanyId(), divList.get(counter).getDividendDeclaredId(), divList.get(counter).getWarrantNumber());
                if (exists) {
                    logger.info("found an existing dividend record that matches one of the records uploaded ", login.getUserId());
                    break;
                } else if (divList.get(counter).getClientCompName() == null || divList.get(counter).getClientCompName().isEmpty()) {
                    resp.setRetn(300);
                    resp.setDesc("Client company name cannot be empty");
                    return resp;
                } else if (divList.get(counter).getPayableAmount() > 0) {
                    resp.setRetn(300);
                    resp.setDesc("Please specify the amount paid");
                    return resp;
                } else if (divList.get(counter).getPaidDate() != null && !"".equals(divList.get(counter).getPaidDate())) {
                    resp.setRetn(300);
                    resp.setDesc("Please specify the date paid");
                    return resp;
                }
            }
            if (!exists) {

            }

        } catch (Exception ex) {
        }
        return resp;
    }

    private List<org.greenpole.hibernate.entity.Dividend> retrieveDividends(List<Dividend> divList) {
        List<org.greenpole.hibernate.entity.Dividend> div_hib_list = new ArrayList<>();
        org.greenpole.hibernate.entity.Dividend div_hib = new org.greenpole.hibernate.entity.Dividend();
        if (!divList.isEmpty()) {
            for (Dividend dv : divList) {
                Random ran = new Random();
                int randomOne = ran.nextInt() % 1000000;
                int randomTwo = ran.nextInt() % 1000000;
                if (randomOne < 0) {
                    randomOne = randomOne / (-1);
                }
                if (randomTwo < 0) {
                    randomTwo = randomTwo / (-1);
                }
                String sr = Integer.toString(randomOne).substring(0, 5);//generates five digits
                String ssl = Integer.toString(randomTwo).substring(0, 5);//generates five digits
                String warrantNo = " ";
                warrantNo = sr + ssl;
                long ac = Long.parseLong(warrantNo);
                div_hib.setClientCompName(null);
                div_hib.setClientCompany(null);
                div_hib.setCompAccHoldings(Integer.MAX_VALUE);
                div_hib.setDivNumber(Integer.MIN_VALUE);
                div_hib.setDividendDeclared(null);
                div_hib.setDividendIssueType(null);
                div_hib.setGrossAmount(Double.MAX_VALUE);
                div_hib.setIssueDate(null);
                div_hib.setIssueType(null);
                div_hib.setIssued(Boolean.TRUE);
                div_hib.setIssuedDate(null);
                div_hib.setPaid(Boolean.TRUE);
                div_hib.setPaidDate(null);
                div_hib.setPayableAmount(Double.NaN);
                div_hib.setPayableDate(null);
                div_hib.setPaymentMethod(null);
                div_hib.setRate(Double.NaN);
                div_hib.setReIssued(false);
                div_hib.setSHolderMailingAddr(null);
                div_hib.setTax(Double.NaN);
                div_hib.setUnclaimed(false);
                //div_hib.setWarrantNumber(randomTwo);
                //div_hib.setWarrantNumber(warrantNumber);//generated randomly
                div_hib.setYearEnding(null);
                div_hib.setYearType(null);
            }
        }
        return div_hib_list;
    }
    private static long randomNum() {
    List<Integer> numbers = new ArrayList<>();
    long fg = 0;
    for(int i = 0; i < 10; i++){
        numbers.add(i);
    }
    Collections.shuffle(numbers);
    String result = "";
    for(int i = 0; i < 10; i++){
        result += numbers.get(i).toString();
        fg = Long.parseLong(result);
    }
    return fg;
}
}
