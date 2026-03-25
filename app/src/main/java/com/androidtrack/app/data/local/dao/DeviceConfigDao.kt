package com.androidtrack.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.androidtrack.app.data.local.entity.DeviceConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceConfigDao {

    @Query("SELECT * FROM device_config WHERE id = 1")
    fun observe(): Flow<DeviceConfigEntity?>

    @Query("SELECT * FROM device_config WHERE id = 1")
    suspend fun get(): DeviceConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(config: DeviceConfigEntity)
}
