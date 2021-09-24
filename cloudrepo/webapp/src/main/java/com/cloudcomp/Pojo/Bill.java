package com.cloudcomp.Pojo;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity(name="Bill")
public class Bill {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(columnDefinition="BINARY(16)",updatable = false, nullable = false)
    private UUID id;
    private String created_ts;
    private String updated_ts;
    @Column(columnDefinition="BINARY(16)",updatable = false, nullable = false)
    private UUID ownerId;
    private String vendor;
    private String bill_date;
    private String due_date;
    private double amount_due;
    @ElementCollection
    private List<String> categories;
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
    @OneToOne(optional = true)
    private File attachment;

    public File getAttachment() {
        return attachment;
    }

    public void setAttachment(File attachment) {
        this.attachment = attachment;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCreated_ts() {
        return created_ts;
    }

    public void setCreated_ts(String created_ts) {
        this.created_ts = created_ts;
    }

    public String getUpdated_ts() {
        return updated_ts;
    }

    public void setUpdated_ts(String updated_ts) {
        this.updated_ts = updated_ts;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getBill_date() {
        return bill_date;
    }

    public void setBill_date(String bill_date) {
        this.bill_date = bill_date;
    }

    public String getDue_date() {
        return due_date;
    }

    public void setDue_date(String due_date) {
        this.due_date = due_date;
    }

    public double getAmount_due() {
        return amount_due;
    }

    public void setAmount_due(double amount_due) {
        this.amount_due = amount_due;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }



    @Override
    public String toString() {
        return "id"+ id+
                "paymentstatus"+paymentStatus+
                "amount due" +amount_due;
    }
}
