package com.example.sihscrap.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface MaterialCatalogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MaterialCatalogEntity item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<MaterialCatalogEntity> items);

    @Update
    void update(MaterialCatalogEntity item);

    @Query("SELECT * FROM material_catalog ORDER BY categoryCode ASC, name ASC")
    List<MaterialCatalogEntity> getAll();

    @Query("SELECT * FROM material_catalog WHERE code = :code LIMIT 1")
    MaterialCatalogEntity getByCode(String code);

    @Query("SELECT * FROM material_catalog WHERE categoryCode = :categoryCode")
    List<MaterialCatalogEntity> getByCategory(String categoryCode);

    @Query("UPDATE material_catalog SET spotRatePerKg = :newRate WHERE code = :code")
    void updateSpotRate(String code, double newRate);

    @Query("SELECT COUNT(*) FROM material_catalog")
    int getCount();

    @Query("DELETE FROM material_catalog")
    void clearAll();
}
