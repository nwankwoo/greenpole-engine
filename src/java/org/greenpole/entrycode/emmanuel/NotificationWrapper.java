/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.greenpole.entrycode.emmanuel;

import java.io.Serializable;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import org.greenpole.entity.model.clientcompany.ClientCompany;
import org.greenpole.entity.model.clientcompany.ShareQuotation;
import org.greenpole.entity.model.user.Department;
import org.greenpole.entity.model.user.Unit;
import org.greenpole.entity.model.user.User;

/**
 *
 * @author user
 * The Notification wrapper to contain notifications to the stored and forwarded 
 * to the front-end.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"code","messageTag","from","to","description","model"})
@XmlSeeAlso({ClientCompany.class, User.class, Department.class, Unit.class,
                ShareQuotation.class})
public class NotificationWrapper implements Serializable {
    @XmlElement
    private String code;
    @XmlElement
    private String description;
    @XmlElement
    private String from;
    @XmlElementWrapper(name = "to")
    private List<?> to;
    @XmlElementWrapper(name = "wrapper")
    private List<?> model;
    @XmlElement
    private String messageTag;
    @XmlTransient
    private String fromType;
    @XmlTransient
    private String toType;
    @XmlTransient
    private boolean attendedTo;

    public NotificationWrapper() {
    }

    /**
     * Gets the notification code.
     * @return the notification code
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the notification code.
     * The notification code will serve as the notification's unique identifier,
     * as well as the notification's file name.
     * @param code the notification code
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Gets the notificaton's description.
     * @return the notification's description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the notification's description.
     * @param description the notification's description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the sender of the notification.
     * @return the notification's sender
     */
    public String getFrom() {
        return from;
    }

    /**
     * Sets the name of the notification's initiator.
     * @param from the notification's sender
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * Gets the receiver of the notification.
     * @return notification's receiver
     */
   

    /**
     * Gets the notification's tag.
     * @return the notification's tag
     */
    public String getMessageTag() {
        return messageTag;
    }

    /**
     * Sets the notification's tag.
     * The notification's tag help distinguishes the different types of notification
     * the system utilises.
     * @param messageTag
     */
    public void setMessageTag(String messageTag) {
        this.messageTag = messageTag;
    }

    /**
     * Gets the list of model(s) attached to the notification.
     * @return the list of model(s)
     */
    public List<?> getModel() {
        return model;
    }

    /**
     * Sets the list of model(s) attached to the notification.
     * Some notifications - like authorisation request notifications - may
     * come with entities to the added to the database. These entities are stored
     * in the model list.
     * @param model the list of model(s)
     */
    public void setModel(List<?> model) {
        this.model = model;
    }

    /**
     * Gets the notification's from-type.
     * @return the notification's from-type
     */
    public String getFromType() {
        return fromType;
    }

    /**
     * Sets the notification's from-type.
     * The from-type denotes the type of user the notification is coming from.
     * It could be an internal user (a user of the system) or an external user
     * (like a Holder).
     * @param fromType the notification's from-type
     */
    public void setFromType(String fromType) {
        this.fromType = fromType;
    }

    /**
     * Gets the notification's to-type.
     * @return the notification's to-type
     */
    public String getToType() {
        return toType;
    }

    /**
     * Sets the notification's to-type.
     * The to-type denotes the type of user the notification is going to.
     * It could be an internal user (a user of the system) or an external user
     * (like a Holder).
     * @param toType
     */
    public void setToType(String toType) {
        this.toType = toType;
    }

    /**
     * Gets the notification's attended-to status.
     * @return the notification's attended-to status
     */
    public boolean isAttendedTo() {
        return attendedTo;
    }

    /**
     * Sets the notification's attended-to status.
     * This denotes whether the notification has been attended to by the receiving
     * user. In some cases - especially if the to-type is external - the attended-to
     * is automatically set to true.
     * @param attendedTo the notification's attended-to status
     */
    public void setAttendedTo(boolean attendedTo) {
        this.attendedTo = attendedTo;
    }

    /**
     * @return the to
     */
    public List<?> getTo() {
        return to;
    }

    /**
     * @param to the to to set
     */
    public void setTo(List<?> to) {
        this.to = to;
    }
}
