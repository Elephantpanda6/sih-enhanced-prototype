package com.example.sihscrap.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sync_queue")
public class SyncQueueEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long transactionId;
    public String payloadJson; // Compact JSON (< 310 bytes)
    public int retryCount;
    public long lastAttemptMillis;
    public long backoffDelayMillis;
    public String status; // QUEUED, IN_PROGRESS, FAILED
    public String lastError;

    public static final String STATUS_QUEUED = "QUEUED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_FAILED = "FAILED";

    public SyncQueueEntity() {
        this.retryCount = 0;
        this.status = STATUS_QUEUED;
        this.backoffDelayMillis = 2000L; // initial 2s backoff
        this.lastAttemptMillis = 0L;
    }

    public SyncQueueEntity(long transactionId, String payloadJson) {
        this();
        this.transactionId = transactionId;
        this.payloadJson = payloadJson;
    }
}
