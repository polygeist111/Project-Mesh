package com.greybox.projectmesh.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE uuid = :uuid LIMIT 1")
    suspend fun getUserByUuid(uuid: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE uuid = :uuid)")
    suspend fun hasWithID(uuid: String): Boolean

    @Query("SELECT * FROM users WHERE address = :ip LIMIT 1")
    suspend fun getUserByIp(ip: String): UserEntity?
    @Query("SELECT * FROM users WHERE address IS NOT NULL")
    suspend fun getAllConnectedUsers(): List<UserEntity>

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>
}