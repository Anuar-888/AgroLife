package kz.spelost.agroapp.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CropDao {
    @Query("SELECT * FROM crops")
    List<CropEntity> getAll();

    @Query("SELECT * FROM crops WHERE id = :id LIMIT 1")
    CropEntity getById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CropEntity> crops);
}
