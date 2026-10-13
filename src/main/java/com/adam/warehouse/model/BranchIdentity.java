package com.adam.warehouse.model;

/** Identity of the branch served by this installation's database. */
public record BranchIdentity(String code, String name, String location, double taxRate) { }
