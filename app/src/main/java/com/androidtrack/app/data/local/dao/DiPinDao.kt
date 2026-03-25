package com.androidtrack.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.androidtrack.app.data.local.entity.DiPinEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiPinDao {

    @Query("SELECT * FROM di_pin ORDER BY pinNumber ASC")
    fun observeAll(): Flow<List<DiPinEntity>>

    @Query("SELECT * FROM di_pin ORDER BY pinNumber ASC")
    suspend fun getAll(): List<DiPinEntity>

    @Query("SELECT * FROM di_pin WHERE id = :id")
    suspend fun getById(id: Int): DiPinEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pin: DiPinEntity): Long

    @Update
    suspend fun update(pin: DiPinEntity)

    @Delete
    suspend fun delete(pin: DiPinEntity)

    @Query("DELETE FROM di_pin WHERE id = :id")
    suspend fun deleteById(id: Int)

    /**
     * Returns the number of pins with the given [pinNumber], excluding [excludeId].
     * Used to enforce uniqueness when adding or editing a pin.
     */
    @Query("SELECT COUNT(*) FROM di_pin WHERE pinNumber = :pinNumber AND id != :excludeId")
    suspend fun countByPinNumber(pinNumber: String, excludeId: Int = 0): Int

    @Query("UPDATE di_pin SET shootCount = :count WHERE id = :id")
    suspend fun updateShootCount(id: Int, count: Long)
}
