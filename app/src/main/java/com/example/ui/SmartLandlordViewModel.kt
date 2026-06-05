package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class SmartLandlordViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SmartLandlordDatabase.getDatabase(application)
    private val repository = SmartLandlordRepository(db.dao())

    // ---------------------------------------------------------------------
    // AUTHENTICATION & APP ROUTING STATE
    // ---------------------------------------------------------------------

    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private val _currentRoute = MutableStateFlow("login") // login, register, dashboard, properties, tenants, payments, maintenance, reports, admin, subscriptions
    val currentRoute: StateFlow<String> = _currentRoute.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _registerError = MutableStateFlow<String?>(null)
    val registerError: StateFlow<String?> = _registerError.asStateFlow()

    // ---------------------------------------------------------------------
    // RECOVERY/RESET PASSWORD FLOW SIMULATION
    // ---------------------------------------------------------------------
    private val _resetMessage = MutableStateFlow<String?>(null)
    val resetMessage: StateFlow<String?> = _resetMessage.asStateFlow()

    // ---------------------------------------------------------------------
    // ACTIVE CORE ENTITIES DATA FLOWS
    // ---------------------------------------------------------------------

    val properties: StateFlow<List<Property>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getPropertiesFlow(user.email) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tenants: StateFlow<List<Tenant>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getTenantsFlow(user.email) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payments: StateFlow<List<Payment>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getPaymentsFlow(user.email) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val maintenanceRequests: StateFlow<List<MaintenanceRequest>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getMaintenanceFlow(user.email) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<Notification>> = currentUser
        .flatMapLatest { user ->
            if (user != null) repository.getNotificationsFlow(user.email) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---------------------------------------------------------------------
    // ADMIN PORTAL - ONLY LOAD FOR DEVELOPER mghulam2006@gmail.com
    // ---------------------------------------------------------------------
    
    val allUsers: StateFlow<List<UserAccount>> = repository.getAllUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val systemLogs: StateFlow<List<ActivityLog>> = repository.getActivityLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---------------------------------------------------------------------
    // LANDLORD SUB-MEMBERS CONTROL ERRORS & LIMITS
    // ---------------------------------------------------------------------
    private val _dbOperationMessage = MutableStateFlow<String?>(null)
    val dbOperationMessage: StateFlow<String?> = _dbOperationMessage.asStateFlow()

    // Selected items for detailing / editing
    private val _selectedTenantId = MutableStateFlow<Long?>(null)
    val selectedTenantId: StateFlow<Long?> = _selectedTenantId.asStateFlow()

    // ---------------------------------------------------------------------
    // AUTHENTICATION LOGIC IMPLEMENTATIONS
    // ---------------------------------------------------------------------

    fun login(email: String, passwordText: String) {
        viewModelScope.launch {
            _loginError.value = null
            if (email.isBlank() || passwordText.isBlank()) {
                _loginError.value = "Email and Password cannot be empty."
                return@launch
            }
            val user = repository.getUserByEmail(email.lowercase().trim())
            if (user == null) {
                _loginError.value = "User not found. Please register."
                return@launch
            }
            if (user.passwordHash != passwordText) {
                _loginError.value = "Incorrect password. Try again."
                return@launch
            }
            
            // Set user session
            _currentUser.value = user
            repository.logActivity(user.email, "Logged in securely")
            _currentRoute.value = "dashboard"
        }
    }

    fun loginWithGoogleSimulation(email: String, name: String) {
        viewModelScope.launch {
            _loginError.value = null
            val cleanEmail = email.lowercase().trim()
            var user = repository.getUserByEmail(cleanEmail)
            if (user == null) {
                // Register instantly via google sign-in with default Free plan
                repository.registerUser(name, cleanEmail, "google_oauth_simulated", googleSignedIn = true)
                user = repository.getUserByEmail(cleanEmail)
            }
            _currentUser.value = user
            repository.logActivity(cleanEmail, "Logged in via Google Authentication")
            _currentRoute.value = "dashboard"
        }
    }

    fun register(name: String, email: String, passwordText: String) {
        viewModelScope.launch {
            _registerError.value = null
            if (name.isBlank() || email.isBlank() || passwordText.isBlank()) {
                _registerError.value = "All fields are required."
                return@launch
            }
            val success = repository.registerUser(name, email.lowercase().trim(), passwordText)
            if (success) {
                // Log in automatic after signup
                val user = repository.getUserByEmail(email.lowercase().trim())
                _currentUser.value = user
                _currentRoute.value = "dashboard"
            } else {
                _registerError.value = "Email already registered. Try logging in."
            }
        }
    }

    fun requestPasswordReset(email: String) {
        viewModelScope.launch {
            _resetMessage.value = null
            if (email.isBlank()) {
                _resetMessage.value = "Please enter an email address first."
                return@launch
            }
            val user = repository.getUserByEmail(email.lowercase().trim())
            if (user != null) {
                _resetMessage.value = "Reset link simulation successfully sent to ${email.lowercase()}. Please check your inbox."
                repository.logActivity(email, "Requested simulated password reset")
            } else {
                _resetMessage.value = "No registered user found with that email address."
            }
        }
    }

    fun logout() {
        _currentUser.value?.let {
            viewModelScope.launch {
                repository.logActivity(it.email, "Logged out securely")
            }
        }
        _currentUser.value = null
        _currentRoute.value = "login"
    }

    // ---------------------------------------------------------------------
    // APP NAVIGATION MANAGER
    // ---------------------------------------------------------------------

    fun navigateTo(route: String) {
        _currentRoute.value = route
        _dbOperationMessage.value = null
    }

    fun selectTenantAndNavigate(id: Long) {
        _selectedTenantId.value = id
        _currentRoute.value = "tenant_profile"
    }

    fun clearSelectedTenant() {
        _selectedTenantId.value = null
    }

    // ---------------------------------------------------------------------
    // PROPERTY MANAGEMENT OPERATIONS
    // ---------------------------------------------------------------------

    fun createProperty(name: String, type: String, address: String, monthlyRent: Double, imageUri: String, notes: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user == null) {
                _dbOperationMessage.value = "Must be logged in to create properties"
                onComplete(false)
                return@launch
            }
            
            val p = Property(
                name = name,
                type = type,
                address = address,
                monthlyRent = monthlyRent,
                status = "Vacant",
                imageUri = imageUri.ifBlank { getRandomPropertyImageUrl(type) },
                notes = notes,
                userId = user.email
            )
            
            val success = repository.insertProperty(p)
            if (success) {
                _dbOperationMessage.value = "Successfully added property!"
                onComplete(true)
            } else {
                val currentCount = repository.getPropertyCount(user.email)
                _dbOperationMessage.value = "Plan limit reached! Your current '${user.subscriptionPlan}' plan only allows up to ${if (user.subscriptionPlan == "Free") "1" else "20"} properties. Please upgrade in Subscriptions tab."
                onComplete(false)
            }
        }
    }

    fun updatePropertyDetails(property: Property, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.updateProperty(property)
            _dbOperationMessage.value = "Property details updated successfully"
            onComplete()
        }
    }

    fun deletePropertyDetails(id: Long) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.deleteProperty(id, user.email)
            _dbOperationMessage.value = "Property deleted successfully"
        }
    }

    // ---------------------------------------------------------------------
    // TENANT MANAGEMENT OPERATIONS
    // ---------------------------------------------------------------------

    fun createTenant(fullName: String, idPassport: String, phone: String, email: String, start: String, end: String, emergency: String, propertyId: Long, propertyName: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user == null) {
                onComplete(false)
                return@launch
            }

            val t = Tenant(
                fullName = fullName,
                idPassportNumber = idPassport,
                phoneNumber = phone,
                emailAddress = email,
                leaseStartDate = start,
                leaseEndDate = end,
                emergencyContact = emergency,
                propertyId = propertyId,
                propertyName = propertyName,
                userId = user.email
            )

            val success = repository.insertTenant(t)
            if (success) {
                _dbOperationMessage.value = "Successfully signed lease with new tenant!"
                onComplete(true)
            } else {
                _dbOperationMessage.value = "Plan limit reached! Your current '${user.subscriptionPlan}' plan only allows up to 5 tenants. Please upgrade in Subscriptions tab to manage more."
                onComplete(false)
            }
        }
    }

    fun updateTenantDetails(tenant: Tenant, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.updateTenant(tenant)
            _dbOperationMessage.value = "Tenant files updated"
            onComplete()
        }
    }

    fun deleteTenantRecord(id: Long) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.deleteTenant(id, user.email)
            _dbOperationMessage.value = "Tenant lease cancelled and record removed"
        }
    }

    // ---------------------------------------------------------------------
    // PAYMENTS AND RENT CONTROLLER
    // ---------------------------------------------------------------------

    fun addManualRentSchedule(tenantId: Long, tenantName: String, propertyName: String, amountDue: Double, dueDate: String) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val rNo = "SL-REC-${System.currentTimeMillis() % 1000000}"
            val pay = Payment(
                tenantId = tenantId,
                tenantName = tenantName,
                propertyName = propertyName,
                amountDue = amountDue,
                amountPaid = 0.0,
                dueDate = dueDate,
                status = "Unpaid",
                balance = amountDue,
                paymentMethod = "EFT",
                receiptNo = rNo,
                userId = user.email
            )
            repository.insertPayment(pay)
            _dbOperationMessage.value = "Created direct rental statement for $tenantName"
        }
    }

    fun logTenantRentReceipt(paymentId: Long, amountPaid: Double, method: String, date: String, onCompleted: (String) -> Unit) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val receiptNo = repository.recordRentPayment(paymentId, amountPaid, method, date, user.email)
            if (receiptNo != null) {
                _dbOperationMessage.value = "Rent received and receipt '$receiptNo' created!"
                onCompleted(receiptNo)
            }
        }
    }

    fun deletePaymentRecord(id: Long) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.deletePayment(id, user.email)
            _dbOperationMessage.value = "Payment ledger entry deleted"
        }
    }

    // ---------------------------------------------------------------------
    // MAINTENANCE SCHEDULING
    // ---------------------------------------------------------------------

    fun createMaintenanceRequest(title: String, desc: String, propertyId: Long, propertyName: String, priority: String, imageUri: String) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            val req = MaintenanceRequest(
                title = title,
                description = desc,
                propertyId = propertyId,
                propertyName = propertyName,
                priority = priority,
                status = "Open",
                imageUri = imageUri,
                userId = user.email
            )
            repository.insertMaintenance(req)
            _dbOperationMessage.value = "Maintenance request submitted to dashboard"

            // Auto-notify
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = dateFormat.format(Date())
            repository.insertNotification(
                Notification(
                    message = "New $priority priority request: '$title' at $propertyName",
                    type = "Maintenance",
                    propertyId = propertyId,
                    date = today,
                    userId = user.email
                )
            )
        }
    }

    fun updateMaintenanceDetails(req: MaintenanceRequest) {
        viewModelScope.launch {
            repository.updateMaintenance(req)
            _dbOperationMessage.value = "Maintenance ticket details modernized"
        }
    }

    fun deleteMaintenanceRecord(id: Long) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.deleteMaintenance(id, user.email)
            _dbOperationMessage.value = "Maintenance log deleted"
        }
    }

    // ---------------------------------------------------------------------
    // WHATSAPP URL EXPORTER BUILDERS
    // ---------------------------------------------------------------------

    fun getWhatsAppReminderUrl(tenant: Tenant, payment: Payment): String {
        return repository.getWhatsAppReminderLink(tenant, payment)
    }

    fun getWhatsAppReceiptUrl(tenant: Tenant, payment: Payment): String {
        return repository.getWhatsAppReceiptLink(tenant, payment)
    }

    // ---------------------------------------------------------------------
    // ADMIN SUBSCRIPTION AND USER CONTROLLER (mghulam2006@gmail.com ADMIN LEVEL)
    // ---------------------------------------------------------------------

    fun adminControlUserSubscription(email: String, newPlan: String, totalYears: Int) {
        viewModelScope.launch {
            val targetUser = repository.getUserByEmail(email) ?: return@launch
            val updatedEndDate = System.currentTimeMillis() + (totalYears * 31536000000L) // 1 Year = 31.5B ms
            val updatedUser = targetUser.copy(
                subscriptionPlan = newPlan,
                subStartDate = System.currentTimeMillis(),
                subEndDate = updatedEndDate
            )
            repository.updateUser(updatedUser)
            repository.logActivity("mghulam2006@gmail.com", "Admin manually upgraded developer email subscriber ($email) to $newPlan plan for $totalYears Years")
            _dbOperationMessage.value = "Successfully updated $email subscription to $newPlan for $totalYears Year(s)!"
            
            // If the updated user is currently the logged-in user, live refresh their session too!
            if (_currentUser.value?.email == email) {
                _currentUser.value = updatedUser
            }
        }
    }

    fun purchaseSelfSaaSPlan(plan: String) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.upgradeSubscription(user.email, plan)
            val updated = repository.getUserByEmail(user.email)
            _currentUser.value = updated
            _dbOperationMessage.value = "Congratulations! You have updated your business subscription to SaaS $plan Plan."
        }
    }

    // ---------------------------------------------------------------------
    // NUMERICAL UTILITIES & FORMATTING
    // ---------------------------------------------------------------------

    fun formatZAR(amount: Double): String {
        val formatter = DecimalFormat("###,###,##0.00")
        return "R " + formatter.format(amount)
    }

    private fun getRandomPropertyImageUrl(type: String): String {
        return when (type.lowercase()) {
            "apartment" -> "https://images.unsplash.com/photo-1545324418-cc1a3fa10c00?auto=format&fit=crop&w=400&q=80"
            "house" -> "https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=400&q=80"
            "commercial" -> "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=400&q=80"
            else -> "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=400&q=80"
        }
    }

    fun formatDate(timestampMs: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }
}
