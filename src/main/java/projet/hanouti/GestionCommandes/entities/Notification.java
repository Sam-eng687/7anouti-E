package projet.hanouti.GestionCommandes.entities;

import projet.hanouti.GestionCommandes.enums.EventNotification;
import projet.hanouti.GestionCommandes.enums.TypeNotification;

import java.time.LocalDateTime;

public class Notification {

    private int notificationId;

    private int userId;

    private TypeNotification type;

    private EventNotification event;

    private String titre;

    private String message;

    private Integer referenceId;

    private boolean isRead;

    private java.time.LocalDateTime dateCreation;


    public Notification() {
    }

    public Notification(int notificationId, int userId, TypeNotification type, EventNotification event, String titre, String message, Integer referenceId, boolean isRead, LocalDateTime dateCreation) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.type = type;
        this.event = event;
        this.titre = titre;
        this.message = message;
        this.referenceId = referenceId;
        this.isRead = isRead;
        this.dateCreation = dateCreation;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public TypeNotification getType() {
        return type;
    }

    public void setType(TypeNotification type) {
        this.type = type;
    }

    public EventNotification getEvent() {
        return event;
    }

    public void setEvent(EventNotification event) {
        this.event = event;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Integer referenceId) {
        this.referenceId = referenceId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "notificationId=" + notificationId +
                ", userId=" + userId +
                ", type=" + type +
                ", event=" + event +
                ", titre='" + titre + '\'' +
                ", message='" + message + '\'' +
                ", referenceId=" + referenceId +
                ", isRead=" + isRead +
                ", dateCreation=" + dateCreation +
                '}';
    }
}
