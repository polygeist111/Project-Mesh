package com.greybox.projectmesh.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.greybox.projectmesh.db.entities.User
import kotlinx.coroutines.flow.Flow


@Dao
interface UserDao {
    @Query("SELECT * FROM user")
    fun getAll(): List<User>

    @Query("SELECT * FROM user")
    fun getAllFlow(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun update(user: User)

    @Query("SELECT EXISTS(SELECT * FROM user WHERE uuid = :uuid)")
    fun hasWithID(uuid: String): Boolean
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun initSelf(user: User)

    //@Update(entity = User::class)
    @Query("UPDATE user SET name = :newName WHERE uuid = :uuid")
    fun updateName(uuid: String,newName: String)
}
