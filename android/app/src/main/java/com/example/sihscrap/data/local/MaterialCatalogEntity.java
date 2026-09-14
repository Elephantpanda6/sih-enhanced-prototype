package com.example.sihscrap.data.local;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "material_catalog",
    indices = {@Index(value = {"code"}, unique = true)}
)
public class MaterialCatalogEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String code;
    public String name;
    public String nameHi;
    public String nameMr;
    public String categoryCode; // non_ferrous, ferrous, e_waste, battery_hazmat, plastics, paper_cardboard
    public double spotRatePerKg;
    public double eprSubsidyBonusPerKg; // Extra ₹3 to ₹7 / kg incentive
    public double defaultPurity;
    public double co2SavingPerKg;
    public String tierColor; // EMERALD_GREEN, AMBER, SLATE, CRIMSON
    public String unit;

    public MaterialCatalogEntity() {
        this.unit = "kg";
        this.defaultPurity = 1.0;
        this.tierColor = "EMERALD_GREEN";
    }

    public MaterialCatalogEntity(String code, String name, String nameHi, String nameMr,
                                 String categoryCode, double spotRatePerKg, double eprSubsidyBonusPerKg,
                                 double defaultPurity, double co2SavingPerKg, String tierColor) {
        this.code = code;
        this.name = name;
        this.nameHi = nameHi;
        this.nameMr = nameMr;
        this.categoryCode = categoryCode;
        this.spotRatePerKg = spotRatePerKg;
        this.eprSubsidyBonusPerKg = eprSubsidyBonusPerKg;
        this.defaultPurity = defaultPurity;
        this.co2SavingPerKg = co2SavingPerKg;
        this.tierColor = tierColor;
        this.unit = "kg";
    }
}
