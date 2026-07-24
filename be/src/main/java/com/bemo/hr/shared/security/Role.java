package com.bemo.hr.shared.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class Role {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private RoleCode code;

    @Column(name = "name_ar", nullable = false, length = 100)
    private String nameAr;

    protected Role() {
    }

    public Role(RoleCode code, String nameAr) {
        this.code = code;
        this.nameAr = nameAr;
    }

    public RoleCode getCode() { return code; }
    public String getNameAr() { return nameAr; }
}
