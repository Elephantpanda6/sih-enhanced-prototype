package com.example.sihscrap.data.local;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "transactions",
    indices = {@Index(value = {"receiptHash"}, unique = true)}
)
public class TransactionEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String receiptHash;
    public String materialCode;
    public String materialName;
    public double weightKg;
    public double spotRatePerKg;
    public double eprBonusPerKg;
    public double purityFactor;
    public double rustDeductionPct;
    public double grossAmount;
    public double finalPayout;
    public double gpsLatitude;
    public double gpsLongitude;
    public String nonce;
    public long epochMillis;
    public String status; // PENDING_SYNC, SYNCED, FAILED
    public String notes;
    public String voiceTranscript;
    public Long serverTxId;
    public String syncErrorMessage;

    public static final String STATUS_PENDING_SYNC = "PENDING_SYNC";
    public static final String STATUS_SYNCED = "SYNCED";
    public static final String STATUS_FAILED = "FAILED";

    public TransactionEntity() {
        this.status = STATUS_PENDING_SYNC;
        this.epochMillis = System.currentTimeMillis();
    }
}
