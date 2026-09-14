package com.example.sihscrap.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(TransactionEntity transaction);

    @Update
    void update(TransactionEntity transaction);

    @Delete
    void delete(TransactionEntity transaction);

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    TransactionEntity getById(long id);

    @Query("SELECT * FROM transactions WHERE receiptHash = :receiptHash LIMIT 1")
    TransactionEntity getByReceiptHash(String receiptHash);

    @Query("SELECT * FROM transactions ORDER BY epochMillis DESC")
    List<TransactionEntity> getAllTransactions();

    @Query("SELECT * FROM transactions WHERE status = :status ORDER BY epochMillis ASC")
    List<TransactionEntity> getTransactionsByStatus(String status);

    @Query("UPDATE transactions SET status = :status, serverTxId = :serverId, syncErrorMessage = :errorMessage WHERE id = :id")
    void updateSyncStatus(long id, String status, Long serverId, String errorMessage);

    @Query("SELECT COUNT(*) FROM transactions")
    int getCount();

    @Query("DELETE FROM transactions")
    void clearAll();
}
