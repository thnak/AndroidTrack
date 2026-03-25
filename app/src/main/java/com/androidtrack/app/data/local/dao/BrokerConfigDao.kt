package com.androidtrack.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.androidtrack.app.data.local.entity.BrokerConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BrokerConfigDao {

    @Query("SELECT * FROM broker_config WHERE id = 1")
    fun observe(): Flow<BrokerConfigEntity?>

    @Query("SELECT * FROM broker_config WHERE id = 1")
    suspend fun get(): BrokerConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(config: BrokerConfigEntity)
}
