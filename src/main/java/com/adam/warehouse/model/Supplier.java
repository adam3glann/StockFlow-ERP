package com.adam.warehouse.model;

public class Supplier {

    private int supplierId;

    private String name;

    private String phone;

    private String email;

    private String address;

    private boolean active;

    public Supplier() {
        this.active = true;
    }

    public Supplier(int supplierId,
                    String name,
                    String phone,
                    String email,
                    String address,
                    boolean active) {

        this.supplierId = supplierId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.active = active;
    }

    public int getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(int supplierId) {
        this.supplierId = supplierId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name.trim();
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
        return name;
    }

}