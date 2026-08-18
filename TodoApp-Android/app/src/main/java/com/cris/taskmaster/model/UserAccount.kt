package com.cris.taskmaster.model

data class UserAccount(
    val id: String,
    val name: String,
    val email: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)
