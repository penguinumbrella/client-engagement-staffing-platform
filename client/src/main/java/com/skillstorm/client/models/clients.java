package com.skillstorm.client.models;

import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

import com.skillstorm.client.models.enums.RelationshipStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

@Entity
@Table(name = "clients")
public class clients {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false, unique = true)
    @NotBlank(message = "Company name is required")
    @Size(max = 100, message = "Company name must be less than 100 characters")
    private String companyName;

    @Column(name = "industry")
    @Size(max = 50, message = "Industry must be less than 50 characters")
    private String industry;

    @Column(name = "primary_contact_name")
    @Size(max = 100, message = "Primary contact name must be less than 100 characters")
    private String primaryContactName;

    @Column(name = "primary_contact_email")
    @Size(max = 100, message = "Primary contact email must be less than 100 characters")
    private String primaryContactEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_status")
    @NotNull(message = "Relationship status is required")
    private RelationshipStatus relationshipStatus;

    public clients() {
    }

    public clients(String companyName, String industry, String primaryContactName, String primaryContactEmail,
            RelationshipStatus relationshipStatus) {
        this.companyName = companyName;
        this.industry = industry;
        this.primaryContactName = primaryContactName;
        this.primaryContactEmail = primaryContactEmail;
        this.relationshipStatus = relationshipStatus;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getCompanyName() {
        return companyName;
    }
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getIndustry() {
        return industry;
    }
    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getPrimaryContactName() {
        return primaryContactName;
    }
    public void setPrimaryContactName(String primaryContactName) {
        this.primaryContactName = primaryContactName;
    }

    public String getPrimaryContactEmail() {
        return primaryContactEmail;
    }
    public void setPrimaryContactEmail(String primaryContactEmail) {
        this.primaryContactEmail = primaryContactEmail;
    }

    public RelationshipStatus getRelationshipStatus() {
        return relationshipStatus;
    }
    public void setRelationshipStatus(RelationshipStatus relationshipStatus) {
        this.relationshipStatus = relationshipStatus;
    }

}
