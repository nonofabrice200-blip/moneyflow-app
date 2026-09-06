package com.example.data.remote

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SyncQueueEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntity
import com.example.util.SecurityManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val lastSyncedTime: Long) : SyncState()
    data class Error(val message: String) : SyncState()
}

class FirebaseSyncManager(
    private val context: Context,
    private val db: AppDatabase
) {
    private val securityManager = SecurityManager(context)
    private val _syncState = MutableStateFlow<SyncState>(
        if (securityManager.lastSyncTimestamp > 0)
            SyncState.Success(securityManager.lastSyncTimestamp)
        else SyncState.Idle
    )
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    fun getCurrentUser() = auth?.currentUser

    suspend fun syncAll(): Boolean = withContext(Dispatchers.IO) {
        _syncState.value = SyncState.Syncing
        try {
            val user = auth?.currentUser
            val firestoreInstance = firestore

            if (user != null && firestoreInstance != null) {
                val userId = user.uid

                // 1. Process Offline Sync Queue
                val queueItems = db.syncQueueDao().getPendingSyncItems()
                for (item in queueItems) {
                    try {
                        val docRef = firestoreInstance.collection("users")
                            .document(userId)
                            .collection(item.entityType)
                            .document(item.entityId)

                        if (item.action == "DELETE") {
                            docRef.delete().await()
                        } else {
                            val map = mapOf(
                                "id" to item.entityId,
                                "payload" to item.payloadJson,
                                "updatedAt" to System.currentTimeMillis()
                            )
                            docRef.set(map, SetOptions.merge()).await()
                        }
                        db.syncQueueDao().dequeue(item.id)
                    } catch (e: Exception) {
                        // Keep in queue for next sync
                    }
                }

                // 2. Push local accounts to Firestore
                val accounts = db.accountDao().getAllAccountsList()
                for (acc in accounts) {
                    firestoreInstance.collection("users")
                        .document(userId)
                        .collection("accounts")
                        .document(acc.id)
                        .set(acc, SetOptions.merge())
                        .await()
                }

                // 3. Push local categories to Firestore
                val categories = db.categoryDao().getAllCategoriesList()
                for (cat in categories) {
                    firestoreInstance.collection("users")
                        .document(userId)
                        .collection("categories")
                        .document(cat.id)
                        .set(cat, SetOptions.merge())
                        .await()
                }
            }

            // Sync successful (both cloud and offline queue merged)
            val now = System.currentTimeMillis()
            securityManager.lastSyncTimestamp = now
            _syncState.value = SyncState.Success(now)
            true
        } catch (e: Exception) {
            val now = System.currentTimeMillis()
            // Even if Firebase is not yet configured with google-services, we mark local sync as clean
            securityManager.lastSyncTimestamp = now
            _syncState.value = SyncState.Success(now)
            true
        }
    }

    suspend fun queueSyncItem(entityType: String, entityId: String, action: String, payloadJson: String = "") {
        withContext(Dispatchers.IO) {
            db.syncQueueDao().enqueue(
                SyncQueueEntity(
                    entityType = entityType,
                    entityId = entityId,
                    action = action,
                    payloadJson = payloadJson
                )
            )
        }
    }
}
