package com.example.sihscrap.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SyncQueueEntity queueItem);

    @Update
    void update(SyncQueueEntity queueItem);

    @Delete
    void delete(SyncQueueEntity queueItem);

    @Query("SELECT * FROM sync_queue WHERE status = 'QUEUED' ORDER BY id ASC")
    List<SyncQueueEntity> getPendingQueue();

    @Query("SELECT * FROM sync_queue WHERE transactionId = :transactionId LIMIT 1")
    SyncQueueEntity getByTransactionId(long transactionId);

    @Query("DELETE FROM sync_queue WHERE transactionId = :transactionId")
    void deleteByTransactionId(long transactionId);

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'QUEUED'")
    int getPendingCount();

    @Query("DELETE FROM sync_queue")
    void clearAll();
}
