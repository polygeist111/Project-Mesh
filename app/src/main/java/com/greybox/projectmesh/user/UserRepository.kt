package com.greybox.projectmesh.user

class UserRepository(private val userDao: UserDao) {

    suspend fun insertOrUpdateUser(uuid: String, name: String) {
        val existing = userDao.getUserByUuid(uuid)
        if (existing == null) {
            userDao.insertUser(UserEntity(uuid = uuid, name = name))
        } else {
            userDao.updateUser(existing.copy(name = name))
        }
    }
    suspend fun insertOrUpdateUserByIp(ip: String, name: String) {
        val existing = userDao.getUserByIp(ip)
        if (existing == null) {
            // Decide on a pseudo-uuid if you don't have a real one:
            val pseudoUuid = "temp-$ip"
            val newUser = UserEntity(
                uuid = pseudoUuid,
                name = name,
                address = ip
            )
            userDao.insertUser(newUser)
        } else {
            // just update the name
            val updated = existing.copy(name = name)
            userDao.updateUser(updated)
        }
    }
    suspend fun getUserByIp(ip: String): UserEntity? {
        return userDao.getUserByIp(ip)
    }
    suspend fun getUser(uuid: String): UserEntity? {
        return userDao.getUserByUuid(uuid)
    }

    suspend fun hasUser(uuid: String): Boolean {
        return userDao.hasWithID(uuid)
    }

    // Add more methods as needed
}