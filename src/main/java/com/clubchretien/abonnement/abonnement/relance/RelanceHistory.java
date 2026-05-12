package com.clubchretien.abonnement.abonnement.relance;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDate;

@Entity
public class RelanceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDate getRelanceDate() {
        return relanceDate;
    }

    public void setRelanceDate(LocalDate relanceDate) {
        this.relanceDate = relanceDate;
    }

    public int getDaysBeforeExpiry() {
        return daysBeforeExpiry;
    }

    public void setDaysBeforeExpiry(int daysBeforeExpiry) {
        this.daysBeforeExpiry = daysBeforeExpiry;
    }

    private Long userId;
    private LocalDate relanceDate; // date à laquelle la relance est envoyée
    private int daysBeforeExpiry;  // J-x de la relance


}