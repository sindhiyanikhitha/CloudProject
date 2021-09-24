package com.cloudcomp.Pojo;

import com.google.gson.annotations.Expose;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Date;
import java.util.UUID;

@Entity(name="User")
public class User {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
        @Column(columnDefinition="BINARY(16)",updatable = false, nullable = false)
        UUID id;
        String first_name;
        String last_name;
        String password;
        String account_created;
        String account_updated;
        String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getFirst_name() {
            return first_name;
        }

        public void setFirst_name(String first_name) {
            this.first_name = first_name;
        }

        public String getLast_name() {
            return last_name;
        }

        public void setLast_name(String last_name) {
            this.last_name = last_name;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

    public String getAccount_created() {
        return account_created;
    }

    public void setAccount_created(String account_created) {
        this.account_created = account_created;
    }

    public String getAccount_updated() {
        return account_updated;
    }

    public void setAccount_updated(String account_updated) {
        this.account_updated = account_updated;
    }

}

