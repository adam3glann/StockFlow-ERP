package com.adam.warehouse.model;

public class Customer {

    private int customerId;

    private String fullName;

    private String phone;

    private String email;

    private String address;

    private boolean active;

    public Customer() {
        this.active = true;
    }

    public Customer(int customerId,
                    String fullName,
                    String phone,
                    String email,
                    String address,
                    boolean active) {

        this.customerId = customerId;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.active = active;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName == null ? "" : fullName.trim();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone == null ? "" : phone.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? "" : email.trim().toLowerCase();
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address == null ? "" : address.trim();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean hasEmail() {
        return !email.isBlank();
    }

    public boolean hasPhone() {
        return !phone.isBlank();
    }

    @Override
    public String toString() {
        return fullName;
    }

}