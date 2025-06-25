package com.greybox.projectmesh.user

import android.util.Log

class UserRepository(private val userDao: UserDao) {

    suspend fun insertOrUpdateUser(uuid: String, name: String, address: String?) {
        val existing = userDao.getUserByUuid(uuid)
        if (existing == null) {
            // Insert new user with address
            userDao.insertUser(
                UserEntity(
                    uuid = uuid,
                    name = name,
                    address = address
                )
            )
        } else {
            // Update existing user, copying over address
            userDao.updateUser(
                existing.copy(
                    name = name,
                    address = address
                )
            )
        }
    }

    suspend fun getUserByIp(ip: String): UserEntity? {
        return userDao.getUserByIp(ip)
    }
    suspend fun getUser(uuid: String): UserEntity? {
        return userDao.getUserByUuid(uuid)
    }
    suspend fun getAllConnectedUsers(): List<UserEntity> {
        return userDao.getAllConnectedUsers()
    }

    suspend fun getAllUsers(): List<UserEntity> {
        return userDao.getAllUsers()
    }

    suspend fun hasUser(uuid: String): Boolean {
        return userDao.hasWithID(uuid)
    }

    // Add more methods as needed
}