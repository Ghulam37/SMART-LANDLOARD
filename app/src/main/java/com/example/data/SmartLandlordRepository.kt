package com.example.data

import kotlinx.coroutines.flow.Flow
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmartLandlordRepository(private val dao: SmartLandlordDao) {

    // ---------------------------------------------------------------------
    // USERS, SESSIONS & SIGN-IN
    // ---------------------------------------------------------------------

    fun getAllUsersFlow(): Flow<List<UserAccount>> = dao.getAllUsersFlow()

    suspend fun getUserByEmail(email: String): UserAccount? = dao.getUserByEmail(email)

    suspend fun registerUser(name: String, email: String, passwordHash: String, googleSignedIn: Boolean = false): Boolean {
        val existing = dao.getUserByEmail(email)
        if (existing != null) return false

        // If it's mghulam2006@gmail.com, make it a developer/admin automatically!
        val isDev = email.lowercase() == "mghulam2006@gmail.com"
        val user = UserAccount(
            email = email,
            name = name,
            passwordHash = passwordHash,
            subscriptionPlan = if (isDev) "Pro" else "Free",
            subStartDate = System.currentTimeMillis(),
            subEndDate = System.currentTimeMillis() + 31536000000L, // 365 Days
            googleSignedIn = googleSignedIn
        )
        dao.insertUser(user)
        logActivity(email, "Registered User Account (${user.subscriptionPlan} Plan)")
        
        // Seed demo properties/tenants for a quick start!
        if (!isDev) {
            seedDemoData(email)
        }
        return true
    }

    suspend fun updateUser(user: UserAccount) {
        dao.updateUser(user)
        logActivity(user.email, "Updated subscription profile to ${user.subscriptionPlan}")
    }

    // ---------------------------------------------------------------------
    // LIMIT CHECKS & SUBSCRIPTIONS
    // ---------------------------------------------------------------------

    suspend fun getPropertyCount(userId: String): Int {
        return dao.getPropertiesForUserList(userId).size
    }

    suspend fun getTenantCount(userId: String): Int {
        return dao.getTenantsForUserList(userId).size
    }

    suspend fun canAddProperty(userId: String): Boolean {
        val user = dao.getUserByEmail(userId) ?: return false
        val count = getPropertyCount(userId)
        return when (user.subscriptionPlan) {
            "Free" -> count < 1
            "Basic" -> count < 20
            else -> true // Pro gets unlimited
        }
    }

    suspend fun canAddTenant(userId: String): Boolean {
        val user = dao.getUserByEmail(userId) ?: return false
        val count = getTenantCount(userId)
        return when (user.subscriptionPlan) {
            "Free" -> count < 5
            else -> true // Basic and Pro are unlimited
        }
    }

    // Upgrade user subscription (1 Year)
    suspend fun upgradeSubscription(email: String, plan: String) {
        val user = dao.getUserByEmail(email) ?: return
        val updated = user.copy(
            subscriptionPlan = plan,
            subStartDate = System.currentTimeMillis(),
            subEndDate = System.currentTimeMillis() + 31536000000L // 365 Days
        )
        dao.insertUser(updated)
        logActivity(email, "Upgraded to $plan Yearly Subscription")
    }

    // ---------------------------------------------------------------------
    // PROPERTIES
    // ---------------------------------------------------------------------

    fun getPropertiesFlow(userId: String): Flow<List<Property>> = dao.getPropertiesForUser(userId)

    suspend fun getPropertyById(id: Long): Property? = dao.getPropertyById(id)

    suspend fun insertProperty(property: Property): Boolean {
        if (!canAddProperty(property.userId)) {
            return false
        }
        val id = dao.insertProperty(property)
        logActivity(property.userId, "Added Property (ID: $id, Name: ${property.name})")
        return true
    }

    suspend fun updateProperty(property: Property) {
        dao.updateProperty(property)
        logActivity(property.userId, "Updated Property (ID: ${property.id}, Name: ${property.name})")
    }

    suspend fun deleteProperty(id: Long, userId: String) {
        val prop = dao.getPropertyById(id)
        if (prop != null) {
            dao.deletePropertyById(id)
            logActivity(userId, "Deleted Property: ${prop.name}")
        }
    }

    // ---------------------------------------------------------------------
    // TENANTS
    // ---------------------------------------------------------------------

    fun getTenantsFlow(userId: String): Flow<List<Tenant>> = dao.getTenantsForUser(userId)

    suspend fun getTenantById(id: Long): Tenant? = dao.getTenantById(id)

    suspend fun insertTenant(tenant: Tenant): Boolean {
        if (!canAddTenant(tenant.userId)) {
            return false
        }
        val id = dao.insertTenant(tenant)
        logActivity(tenant.userId, "Added Tenant: ${tenant.fullName}")
        
        // Setup automatic initial Rent Payment item
        if (tenant.propertyId != 0L) {
            val property = dao.getPropertyById(tenant.propertyId)
            if (property != null) {
                // Auto occupied status
                dao.updateProperty(property.copy(status = "Occupied"))
                
                // Create a rent schedule item due this month
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = dateFormat.format(Date())
                dao.insertPayment(
                    Payment(
                        tenantId = id,
                        tenantName = tenant.fullName,
                        propertyName = property.name,
                        amountDue = property.monthlyRent,
                        amountPaid = 0.0,
                        dueDate = today,
                        status = "Unpaid",
                        balance = property.monthlyRent,
                        paymentMethod = "EFT",
                        receiptNo = "SL-REC-${System.currentTimeMillis() % 1000000}",
                        userId = tenant.userId
                    )
                )
                // Add notification
                dao.insertNotification(
                    Notification(
                        message = "First Rent Schedule created for ${tenant.fullName} - R ${property.monthlyRent}",
                        type = "RentDue",
                        propertyId = property.id,
                        date = today,
                        userId = tenant.userId
                    )
                )
            }
        }
        return true
    }

    suspend fun updateTenant(tenant: Tenant) {
        dao.updateTenant(tenant)
        logActivity(tenant.userId, "Updated Tenant: ${tenant.fullName}")
        if (tenant.propertyId != 0L) {
            val property = dao.getPropertyById(tenant.propertyId)
            if (property != null) {
                dao.updateProperty(property.copy(status = "Occupied"))
            }
        }
    }

    suspend fun deleteTenant(id: Long, userId: String) {
        val tenant = dao.getTenantById(id)
        if (tenant != null) {
            dao.deleteTenantById(id)
            logActivity(userId, "Deleted Tenant: ${tenant.fullName}")
            // Optional: reset property status
            if (tenant.propertyId != 0L) {
                val prop = dao.getPropertyById(tenant.propertyId)
                if (prop != null) {
                    dao.updateProperty(prop.copy(status = "Vacant"))
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // PAYMENTS & RECONCILIATION
    // ---------------------------------------------------------------------

    fun getPaymentsFlow(userId: String): Flow<List<Payment>> = dao.getPaymentsForUser(userId)

    suspend fun insertPayment(payment: Payment) {
        dao.insertPayment(payment)
        logActivity(payment.userId, "Added Payment due ${payment.dueDate} for ${payment.tenantName}")
    }

    suspend fun recordRentPayment(paymentId: Long, amountPaid: Double, paymentMethod: String, date: String, userId: String): String? {
        val payments = dao.getPaymentsForUserList(userId)
        val p = payments.find { it.id == paymentId } ?: return null
        
        val newPaid = p.amountPaid + amountPaid
        val balance = (p.amountDue - newPaid).coerceAtLeast(0.0)
        val currentStatus = when {
            balance <= 0 -> "Paid"
            newPaid > 0 -> "Partial"
            else -> "Unpaid"
        }
        
        val receiptNumber = "REC-${100000 + (paymentId % 900000)}-SL"
        val updated = p.copy(
            amountPaid = newPaid,
            balance = balance,
            status = currentStatus,
            paymentMethod = paymentMethod,
            paidDate = date,
            receiptNo = receiptNumber
        )
        dao.updatePayment(updated)
        logActivity(userId, "Recorded R $amountPaid payment for ${p.tenantName} - Status: $currentStatus")

        return receiptNumber
    }

    suspend fun deletePayment(id: Long, userId: String) {
        dao.deletePaymentById(id)
        logActivity(userId, "Deleted Payment schedule item (ID: $id)")
    }

    // ---------------------------------------------------------------------
    // MAINTENANCE
    // ---------------------------------------------------------------------

    fun getMaintenanceFlow(userId: String): Flow<List<MaintenanceRequest>> = dao.getMaintenanceForUser(userId)

    suspend fun insertMaintenance(request: MaintenanceRequest) {
        dao.insertMaintenance(request)
        logActivity(request.userId, "Logged maintenance request: ${request.title}")
    }

    suspend fun updateMaintenance(request: MaintenanceRequest) {
        dao.updateMaintenance(request)
        logActivity(request.userId, "Updated maintenance status to ${request.status}")
    }

    suspend fun deleteMaintenance(id: Long, userId: String) {
        dao.deleteMaintenanceById(id)
        logActivity(userId, "Deleted maintenance item $id")
    }

    // ---------------------------------------------------------------------
    // NOTIFICATIONS
    // ---------------------------------------------------------------------

    fun getNotificationsFlow(userId: String): Flow<List<Notification>> = dao.getNotificationsForUser(userId)

    suspend fun markAllNotificationsRead(userId: String) {
        dao.markAllNotificationsAsRead(userId)
    }

    suspend fun insertNotification(notification: Notification) {
        dao.insertNotification(notification)
    }

    // ---------------------------------------------------------------------
    // ACTIVITY LOGGING & SYSTEM STATS (Admin developer level)
    // ---------------------------------------------------------------------

    fun getActivityLogsFlow(): Flow<List<ActivityLog>> = dao.getActivityLogs()

    suspend fun logActivity(email: String, action: String) {
        dao.insertActivityLog(ActivityLog(email = email, action = action))
    }

    // ---------------------------------------------------------------------
    // WHATSAPP COMMUNICATORS & LINK BUILDERS
    // ---------------------------------------------------------------------

    // Generates a proper click-to-chat WhatsApp link
    fun getWhatsAppReminderLink(tenant: Tenant, payment: Payment): String {
        val phoneNo = formatPhoneNumberForWhatsApp(tenant.phoneNumber)
        val message = "Hi *${tenant.fullName}*,\n\n" +
                "Hope you're doing well. This is a quick friendly reminder that your monthly rent " +
                "for *${payment.propertyName}* of *R ${payment.amountDue}* is due on *${payment.dueDate}*.\n\n" +
                "Outstanding balance: *R ${payment.balance}*.\n\n" +
                "Please make payment to our designated account using EFT or deposit reference.\n\n" +
                "Thanks,\n" +
                "Smart Landlord Admin"
        
        return "https://api.whatsapp.com/send?phone=$phoneNo&text=${URLEncoder.encode(message, "UTF-8")}"
    }

    fun getWhatsAppReceiptLink(tenant: Tenant, payment: Payment): String {
        val phoneNo = formatPhoneNumberForWhatsApp(tenant.phoneNumber)
        val dateStr = if (payment.paidDate.isNotBlank()) payment.paidDate else "Today"
        val message = "Hi *${tenant.fullName}*,\n\n" +
                "Thank you for your rent payment of *R ${payment.amountPaid}* for *${payment.propertyName}*.\n\n" +
                "🧾 *OFFICIAL PAYMENT RECEIPT*\n" +
                "• Receipt No: *${payment.receiptNo}*\n" +
                "• Date Paid: *$dateStr*\n" +
                "• Paid Method: *${payment.paymentMethod}*\n" +
                "• Due Amount: *R ${payment.amountDue}*\n" +
                "• Total Paid: *R ${payment.amountPaid}*\n" +
                "• Outstanding Balance: *R ${payment.balance}*\n\n" +
                "Thank you for being a valued tenant!\n\n" +
                "Regards,\n" +
                "Smart Landlord Admin\n" +
                "GHULAM TECH INFO"
        
        return "https://api.whatsapp.com/send?phone=$phoneNo&text=${URLEncoder.encode(message, "UTF-8")}"
    }

    private fun formatPhoneNumberForWhatsApp(phone: String): String {
        // Clean out spaces, dashes, parentheses
        var clean = phone.replace("[^0-9]".toRegex(), "")
        // South Africa mobile standard: 072... or 082... change to country code 27
        if (clean.startsWith("0") && clean.length == 10) {
            clean = "27" + clean.substring(1)
        }
        // If it does not have 27 at starting, and it is 9 digits (missing leading 0)
        else if (clean.length == 9 && !clean.startsWith("27")) {
            clean = "27" + clean
        }
        return clean
    }

    // Seed initial demo properties so that users see beautiful fully populated dashboards!
    private suspend fun seedDemoData(email: String) {
        val propId1 = dao.insertProperty(
            Property(
                name = "Rosebank Executive Court Apt 4B",
                type = "Apartment",
                address = "14 Tyrwhitt Ave, Rosebank, Johannesburg, 2196",
                monthlyRent = 12500.0,
                status = "Occupied",
                imageUri = "https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?auto=format&fit=crop&w=400&q=80",
                notes = "Fibre internet pre-installed. Secure access control and private parking.",
                userId = email
            )
        )

        val tenantId = dao.insertTenant(
            Tenant(
                fullName = "Sipho Khumalo",
                idPassportNumber = "9408245123087",
                phoneNumber = "0724567890",
                emailAddress = "sipho.khumalo@gmail.com",
                leaseStartDate = "2026-01-01",
                leaseEndDate = "2026-12-31",
                emergencyContact = "Lerato Khumalo (Wife) - 0831122334",
                propertyId = propId1,
                propertyName = "Rosebank Executive Court Apt 4B",
                userId = email
            )
        )

        // Seed initial payment history
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        dao.insertPayment(
            Payment(
                tenantId = tenantId,
                tenantName = "Sipho Khumalo",
                propertyName = "Rosebank Executive Court Apt 4B",
                amountDue = 12500.0,
                amountPaid = 12500.0,
                dueDate = today,
                paidDate = today,
                status = "Paid",
                balance = 0.0,
                paymentMethod = "EFT",
                receiptNo = "SL-REC-${102374 + (System.currentTimeMillis() % 10000)}",
                userId = email
            )
        )

        // Add typical maintenance request
        dao.insertMaintenance(
            MaintenanceRequest(
                title = "Geyser leaking in bathroom roof",
                description = "Constant dripping sound. Plumber should inspect pressure valve.",
                propertyId = propId1,
                propertyName = "Rosebank Executive Court Apt 4B",
                priority = "High",
                status = "In Progress",
                imageUri = "",
                userId = email
            )
        )
    }
}
