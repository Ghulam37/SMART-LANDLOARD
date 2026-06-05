package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ---------------------------------------------------------------------
// ROOM ENTITIES
// ---------------------------------------------------------------------

@Entity(tableName = "users")
data class UserAccount(
    @PrimaryKey val email: String,
    val name: String,
    val passwordHash: String,
    val subscriptionPlan: String = "Free", // Free, Basic, Pro
    val subStartDate: Long = System.currentTimeMillis(),
    val subEndDate: Long = System.currentTimeMillis() + 31536000000L, // 1 Year default
    val googleSignedIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "properties")
data class Property(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // Apartment, House, Commercial, Townhouse
    val address: String,
    val monthlyRent: Double,
    val status: String, // Occupied, Vacant
    val imageUri: String = "",
    val notes: String = "",
    val userId: String // Associated landlord email
)

@Entity(tableName = "tenants")
data class Tenant(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val idPassportNumber: String,
    val phoneNumber: String,
    val emailAddress: String,
    val leaseStartDate: String,
    val leaseEndDate: String,
    val emergencyContact: String,
    val propertyId: Long, // Linked property id (0 if none)
    val propertyName: String = "",
    val userId: String
)

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tenantId: Long,
    val tenantName: String,
    val propertyName: String,
    val amountDue: Double,
    val amountPaid: Double,
    val dueDate: String,
    val paidDate: String = "", // empty if unpaid
    val status: String, // Paid, Unpaid, Partial
    val balance: Double,
    val paymentMethod: String, // EFT, Cash, Card
    val receiptNo: String,
    val userId: String
)

@Entity(tableName = "maintenance_requests")
data class MaintenanceRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val propertyId: Long,
    val propertyName: String,
    val priority: String, // Low, Medium, High
    val status: String, // Open, In Progress, Completed
    val imageUri: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val userId: String
)

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val message: String,
    val type: String, // RentDue, LatePayment, LeaseExpiry, Maintenance
    val propertyId: Long,
    val date: String,
    val isRead: Boolean = false,
    val userId: String
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val action: String,
    val timestamp: Long = System.currentTimeMillis()
)

// ---------------------------------------------------------------------
// DATA ACCESS OBJECTS (DAOs)
// ---------------------------------------------------------------------

@Dao
interface SmartLandlordDao {

    // Users & Subscriptions
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserAccount?

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsersFlow(): Flow<List<UserAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount)

    @Update
    suspend fun updateUser(user: UserAccount)

    // Properties
    @Query("SELECT * FROM properties WHERE userId = :userId ORDER BY name ASC")
    fun getPropertiesForUser(userId: String): Flow<List<Property>>

    @Query("SELECT * FROM properties WHERE userId = :userId")
    suspend fun getPropertiesForUserList(userId: String): List<Property>

    @Query("SELECT * FROM properties WHERE id = :id LIMIT 1")
    suspend fun getPropertyById(id: Long): Property?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperty(property: Property): Long

    @Update
    suspend fun updateProperty(property: Property)

    @Query("DELETE FROM properties WHERE id = :id")
    suspend fun deletePropertyById(id: Long)

    // Tenants
    @Query("SELECT * FROM tenants WHERE userId = :userId ORDER BY fullName ASC")
    fun getTenantsForUser(userId: String): Flow<List<Tenant>>

    @Query("SELECT * FROM tenants WHERE userId = :userId")
    suspend fun getTenantsForUserList(userId: String): List<Tenant>

    @Query("SELECT * FROM tenants WHERE id = :id LIMIT 1")
    suspend fun getTenantById(id: Long): Tenant?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTenant(tenant: Tenant): Long

    @Update
    suspend fun updateTenant(tenant: Tenant)

    @Query("DELETE FROM tenants WHERE id = :id")
    suspend fun deleteTenantById(id: Long)

    // Payments
    @Query("SELECT * FROM payments WHERE userId = :userId ORDER BY dueDate DESC")
    fun getPaymentsForUser(userId: String): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE userId = :userId")
    suspend fun getPaymentsForUserList(userId: String): List<Payment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Update
    suspend fun updatePayment(payment: Payment)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePaymentById(id: Long)

    // Maintenance
    @Query("SELECT * FROM maintenance_requests WHERE userId = :userId ORDER BY createdAt DESC")
    fun getMaintenanceForUser(userId: String): Flow<List<MaintenanceRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaintenance(request: MaintenanceRequest): Long

    @Update
    suspend fun updateMaintenance(request: MaintenanceRequest)

    @Query("DELETE FROM maintenance_requests WHERE id = :id")
    suspend fun deleteMaintenanceById(id: Long)

    // Notifications
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY date DESC")
    fun getNotificationsForUser(userId: String): Flow<List<Notification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllNotificationsAsRead(userId: String)

    // Activity Logs
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 50")
    fun getActivityLogs(): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLog)
}

// ---------------------------------------------------------------------
// ROOM DATABASE
// ---------------------------------------------------------------------

@Database(
    entities = [
        UserAccount::class,
        Property::class,
        Tenant::class,
        Payment::class,
        MaintenanceRequest::class,
        Notification::class,
        ActivityLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SmartLandlordDatabase : RoomDatabase() {
    abstract fun dao(): SmartLandlordDao

    companion object {
        @Volatile
        private var INSTANCE: SmartLandlordDatabase? = null

        fun getDatabase(context: Context): SmartLandlordDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmartLandlordDatabase::class.java,
                    "smart_landlord_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
