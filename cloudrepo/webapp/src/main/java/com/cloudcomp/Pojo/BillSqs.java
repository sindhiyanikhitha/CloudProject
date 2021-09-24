package com.cloudcomp.Pojo;

import java.util.List;
import java.util.UUID;

public class BillSqs {
    private String email;
    private List<UUID> billList;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<UUID> getBillList() {
        return billList;
    }

    public void setBillList(List<UUID> billList) {
        this.billList = billList;
    }
}
