package com.moontir.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moontir.app.MoontirApplication
import com.moontir.app.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val sessionManager = MoontirApplication.instance.sessionManager
    private val repository = MoontirApplication.instance.repository

    val user: StateFlow<User?> = sessionManager.userFlow
    val token: StateFlow<String?> = sessionManager.tokenFlow
    val language: StateFlow<String> = sessionManager.languageFlow
    val themeMode: StateFlow<String> = sessionManager.themeFlow
    val baseUrl: StateFlow<String> = sessionManager.baseUrlFlow
    val isMockMode: StateFlow<Boolean> = sessionManager.isMockModeFlow

    private val _services = MutableStateFlow<List<Service>>(emptyList())
    val services: StateFlow<List<Service>> = _services.asStateFlow()

    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _currentScreen = MutableStateFlow("home")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Active bottom sheets & dialog states
    val isBookingOpen = MutableStateFlow(false)
    val preselectedService = MutableStateFlow<Service?>(null)
    val selectedServicesFilter = MutableStateFlow("all")

    val vehicleSheetState = MutableStateFlow<Vehicle?>(null) // null = closed, if non-null or mode set
    val isAddVehicleOpen = MutableStateFlow(false)
    val vehicleToEdit = MutableStateFlow<Vehicle?>(null)
    val vehicleToDelete = MutableStateFlow<Vehicle?>(null)

    val orderForInvoice = MutableStateFlow<Order?>(null)
    val orderForRating = MutableStateFlow<Order?>(null)

    init {
        loadInitialData()
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setScreen(screen: String) {
        _currentScreen.value = screen
    }

    fun toggleLanguage() {
        val next = if (language.value == "en") "id" else "en"
        sessionManager.setLanguage(next)
        _toastMessage.value = if (next == "id") "Bahasa diganti ke Bahasa Indonesia" else "Language switched to English"
    }

    fun cycleTheme() {
        val next = when (themeMode.value) {
            "system" -> "dark"
            "dark" -> "light"
            else -> "system"
        }
        sessionManager.setTheme(next)
        _toastMessage.value = "Theme: ${next.replaceFirstChar { it.uppercase() }}"
    }

    fun updateBaseUrl(newUrl: String) {
        sessionManager.setBaseUrl(newUrl)
        _toastMessage.value = "API Server URL updated"
        refreshData()
    }

    fun toggleMockMode(enabled: Boolean) {
        sessionManager.setMockMode(enabled)
        _toastMessage.value = if (enabled) "Standalone Mock Mode Active (Offline)" else "Remote API Server Mode Active"
        refreshData()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // Always load services
            repository.getServices().onSuccess {
                _services.value = it
            }.onFailure {
                // If network fails on initial load, fallback to default offline services so app works smoothly!
                _services.value = fallbackServices()
            }

            // If token exists, load user info, vehicles, and orders
            if (sessionManager.getToken() != null) {
                refreshData()
            }

            _isLoading.value = false
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _isBusy.value = true
            repository.getMe().onSuccess { user ->
                sessionManager.saveUser(user)
            }
            repository.getServices().onSuccess { _services.value = it }
            repository.getVehicles().onSuccess { _vehicles.value = it }
            repository.getOrders().onSuccess { _orders.value = it }
            _isBusy.value = false
        }
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _errorMessage.value = "Please complete all fields"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            _errorMessage.value = null
            repository.login(email.trim(), pass).onSuccess {
                _toastMessage.value = "Welcome back, ${it.user.name}!"
                refreshData()
            }.onFailure {
                _errorMessage.value = it.message ?: "Login failed"
            }
            _isBusy.value = false
        }
    }

    fun register(name: String, email: String, pass: String) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            _errorMessage.value = "Please complete all fields"
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            _errorMessage.value = null
            repository.register(name.trim(), email.trim(), pass).onSuccess {
                _toastMessage.value = "Account created! Welcome, ${it.user.name}!"
                refreshData()
            }.onFailure {
                _errorMessage.value = it.message ?: "Registration failed"
            }
            _isBusy.value = false
        }
    }

    fun logout() {
        sessionManager.clearSession()
        _vehicles.value = emptyList()
        _orders.value = emptyList()
        _currentScreen.value = "home"
        _toastMessage.value = "Signed out"
    }

    fun updateProfile(newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            _isBusy.value = true
            repository.updateMe(newName.trim()).onSuccess {
                _toastMessage.value = "Profile updated"
            }.onFailure {
                _toastMessage.value = it.message ?: "Profile update failed"
            }
            _isBusy.value = false
        }
    }

    fun saveVehicle(
        nickname: String,
        make: String,
        model: String,
        year: String,
        plate: String,
        type: String,
        editId: String? = null
    ) {
        val payload = VehiclePayload(
            nickname = nickname.trim(),
            make = make.trim(),
            model = model.trim(),
            year = year.trim(),
            plate = plate.trim().uppercase(),
            type = type
        )

        viewModelScope.launch {
            _isBusy.value = true
            _errorMessage.value = null
            if (editId != null) {
                repository.updateVehicle(editId, payload).onSuccess { updated ->
                    _vehicles.value = _vehicles.value.map { if (it.id == updated.id) updated else it }
                    isAddVehicleOpen.value = false
                    vehicleToEdit.value = null
                    _toastMessage.value = "Vehicle updated"
                }.onFailure {
                    _errorMessage.value = it.message ?: "Failed to update vehicle"
                }
            } else {
                repository.addVehicle(payload).onSuccess { created ->
                    _vehicles.value = listOf(created) + _vehicles.value
                    isAddVehicleOpen.value = false
                    _toastMessage.value = "${created.nickname} added to garage"
                }.onFailure {
                    _errorMessage.value = it.message ?: "Failed to add vehicle"
                }
            }
            _isBusy.value = false
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.deleteVehicle(vehicle.id).onSuccess {
                _vehicles.value = _vehicles.value.filter { it.id != vehicle.id }
                vehicleToDelete.value = null
                _toastMessage.value = "${vehicle.nickname} removed"
            }.onFailure {
                _toastMessage.value = it.message ?: "Failed to delete vehicle"
            }
            _isBusy.value = false
        }
    }

    fun createOrder(
        serviceId: String,
        vehicleId: String,
        address: Address,
        date: String,
        time: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            _errorMessage.value = null
            val orderCreate = OrderCreate(
                serviceId = serviceId,
                vehicleId = vehicleId,
                address = address,
                scheduleDate = date,
                scheduleTime = time,
                notes = notes
            )
            repository.createOrder(orderCreate).onSuccess { created ->
                _orders.value = listOf(created) + _orders.value
                isBookingOpen.value = false
                _currentScreen.value = "orders"
                _toastMessage.value = "Dispatch confirmed! Specialist preparing."
                onSuccess()
            }.onFailure {
                _errorMessage.value = it.message ?: "Could not confirm order"
            }
            _isBusy.value = false
        }
    }

    fun completeOrder(order: Order) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.completeOrder(order.id).onSuccess { updated ->
                _orders.value = _orders.value.map { if (it.id == updated.id) updated else it }
                orderForInvoice.value = updated
                _toastMessage.value = "Service completed"
            }.onFailure {
                _toastMessage.value = it.message ?: "Failed to complete service"
            }
            _isBusy.value = false
        }
    }

    fun submitRating(order: Order, stars: Int, note: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.rateOrder(order.id, stars, note).onSuccess { updated ->
                _orders.value = _orders.value.map { if (it.id == updated.id) updated else it }
                orderForInvoice.value = updated
                orderForRating.value = null
                _toastMessage.value = "Rating saved · $stars stars"
            }.onFailure {
                _toastMessage.value = it.message ?: "Failed to submit rating"
            }
            _isBusy.value = false
        }
    }

    suspend fun getQuote(serviceId: String, vehicleType: String): Quote? {
        return repository.getQuote(serviceId, vehicleType).getOrNull()
    }

    suspend fun getQuoteForServices(serviceIds: List<String>, vehicleType: String): Quote? {
        return repository.getQuoteForServices(serviceIds, vehicleType).getOrNull()
    }

    suspend fun searchAddress(q: String): List<GeocodeResult> {
        return repository.geocode(q).getOrDefault(emptyList())
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): GeocodeResult? {
        return repository.reverseGeocode(lat, lon).getOrNull()
    }

    private fun fallbackServices(): List<Service> {
        return listOf(
            // EMERGENCY SERVICE (24/7 Roadside Assistance)
            Service(
                id = "emg_tow",
                group = "emergency",
                name = "Emergency Towing",
                nameId = "Derek Mobil Darurat",
                category = "Emergency Service",
                categoryId = "Layanan Darurat",
                description = "24/7 flatbed emergency towing to your preferred workshop or home safely.",
                descriptionId = "Layanan derek gendong darurat 24/7 ke bengkel pilihan atau rumah dengan aman.",
                duration = "30–45 mins arrival",
                durationId = "30–45 mnt tiba",
                price = 350000,
                featured = true,
                features = listOf("24/7 rapid dispatch", "Flatbed towing", "Up to 15 km included", "Safe harness locking")
            ),
            Service(
                id = "emg_jump",
                group = "emergency",
                name = "Emergency Battery Jumper",
                nameId = "Jumper Aki Darurat",
                category = "Emergency Service",
                categoryId = "Layanan Darurat",
                description = "On-site battery jumpstart service and alternator output test for stalled cars.",
                descriptionId = "Layanan jumpstart aki di lokasi dan tes pengisian alternator untuk mobil mogok.",
                duration = "20–30 mins arrival",
                durationId = "20–30 mnt tiba",
                price = 120000,
                featured = true,
                features = listOf("Rapid roadside arrival", "Heavy-duty jumper pack", "Alternator check", "Terminal cleaning")
            ),
            Service(
                id = "emg_tire",
                group = "emergency",
                name = "Emergency Spare Tire Change",
                nameId = "Ganti Ban Serep Darurat",
                category = "Emergency Service",
                categoryId = "Layanan Darurat",
                description = "Rapid on-road spare tire swap, torque tightening, and tire pressure check.",
                descriptionId = "Pemasangan ban serep darurat di jalan, pengencangan baut torsi, dan cek tekanan angin.",
                duration = "25–35 mins arrival",
                durationId = "25–35 mnt tiba",
                price = 150000,
                featured = true,
                features = listOf("Hydraulic jack lift", "Lug bolt torque check", "Spare tire inflation", "Punctured tire secure")
            ),

            // CAR SERVICE (Comprehensive Maintenance & Care)
            Service(
                id = "car_check",
                group = "car_service",
                name = "Essential Car Checkup",
                nameId = "Pemeriksaan Mobil Esensial",
                category = "Car Service",
                categoryId = "Servis Mobil",
                description = "Comprehensive 35-point safety inspection covering brakes, suspension, belts, and engine vitals.",
                descriptionId = "Inspeksi keselamatan 35 titik komprehensif mencakup rem, suspensi, belt, dan kondisi mesin.",
                duration = "60 minutes",
                durationId = "60 menit",
                price = 175000,
                featured = false,
                features = listOf("35-point safety check", "Brake & pad wear test", "Fluid condition test", "Digital health report")
            ),
            Service(
                id = "oil_change",
                group = "car_service",
                name = "Oil Change",
                nameId = "Ganti Oli Mesin",
                category = "Car Service",
                categoryId = "Servis Mobil",
                description = "Full engine oil replacement with premium synthetic oil, OEM oil filter, and drain plug seal.",
                descriptionId = "Penggantian oli mesin menyeluruh dengan oli sintetis premium, filter oli OEM, dan ring karter.",
                duration = "45 minutes",
                durationId = "45 menit",
                price = 290000,
                featured = true,
                features = listOf("Fully synthetic motor oil", "Genuine OEM oil filter", "Crush washer renewal", "Eco-friendly oil recycling")
            ),
            Service(
                id = "fluid_refresh",
                group = "car_service",
                name = "Full Fluid Refresh",
                nameId = "Segarkan Semua Fluida",
                category = "Car Service",
                categoryId = "Servis Mobil",
                description = "Drain, flush, and refill coolant, brake fluid, transmission fluid check, and windshield washer.",
                descriptionId = "Kuras dan isi ulang coolant radiator, minyak rem, cek oli transmisi, dan air wiper.",
                duration = "60 minutes",
                durationId = "60 menit",
                price = 240000,
                featured = false,
                features = listOf("Radiator coolant flush", "DOT4 brake fluid flush", "Power steering check", "Windshield washer top-up")
            ),
            Service(
                id = "full_tune",
                group = "car_service",
                name = "Full Tune-Up",
                nameId = "Tune-Up Menyeluruh",
                category = "Car Service",
                categoryId = "Servis Mobil",
                description = "Complete engine optimization: throttle body cleaning, spark plug renewal, air and cabin filter renewal.",
                descriptionId = "Optimalisasi performa mesin: bersihkan throttle body, ganti busi, dan ganti filter udara serta kabin.",
                duration = "90 minutes",
                durationId = "90 menit",
                price = 350000,
                featured = true,
                features = listOf("Throttle body clean", "Spark plug renewal", "Engine air filter", "OBD2 computer diagnostics")
            ),
            Service(
                id = "battery_change",
                group = "car_service",
                name = "Battery Change",
                nameId = "Ganti Aki Mobil",
                category = "Car Service",
                categoryId = "Servis Mobil",
                description = "Home delivery and installation of maintenance-free car battery with old battery trade-in included.",
                descriptionId = "Pengantaran dan pemasangan aki bebas perawatan (MF) di rumah dengan tukar tambah aki lama.",
                duration = "30 minutes",
                durationId = "30 menit",
                price = 850000,
                featured = false,
                features = listOf("High-capacity MF battery", "Alternator test", "Terminal corrosion clean", "12-month replacement warranty")
            ),
            Service(
                id = "ac_refresh",
                group = "car_service",
                name = "Air Conditioner Refresh",
                nameId = "Segarkan AC Mobil",
                category = "Car Service",
                categoryId = "Servis Mobil",
                description = "AC cabin ozone sterilization, evaporator foam cleaning, cabin filter renewal, and refrigerant pressure test.",
                descriptionId = "Sterilisasi ozon kabin, busa pembersih evaporator, pembersihan filter kabin, dan tes freon.",
                duration = "60 minutes",
                durationId = "60 menit",
                price = 280000,
                featured = false,
                features = listOf("Evaporator foam wash", "Freon pressure check", "Cabin ozone mist", "Blower fan clean")
            ),

            // CAR DETAILING (Premium Exterior & Interior Rejuvenation)
            Service(
                id = "shine",
                group = "detail",
                name = "Exterior Detailing",
                nameId = "Detailing Eksterior",
                category = "Car Detailing",
                categoryId = "Detailing Mobil",
                description = "Multi-stage foam wash, clay decontamination, single-step swirl polish, and hydrophobic ceramic wax seal.",
                descriptionId = "Cuci busa bertahap, dekontaminasi clay, poles hilangkan baret halus, dan segel wax keramik.",
                duration = "2–3 hours",
                durationId = "2–3 jam",
                price = 320000,
                featured = true,
                features = listOf("pH-neutral snow foam", "Clay bar decontamination", "Machine paint polish", "Hydrophobic ceramic wax")
            ),
            Service(
                id = "interior",
                group = "detail",
                name = "Interior Detailing",
                nameId = "Detailing Interior",
                category = "Car Detailing",
                categoryId = "Detailing Mobil",
                description = "Deep cabin hot-water extraction, leather and fabric conditioning, stain removal, and odor elimination.",
                descriptionId = "Vakum ekstraksi uap panas jok, perawatan bahan kulit & kain, pembersihan noda, dan penghilang bau.",
                duration = "2 hours",
                durationId = "2 jam",
                price = 260000,
                featured = true,
                features = listOf("Hot extraction shampoo", "Leather & dash dressing", "Headliner dry clean", "Anti-bacterial mist")
            ),
            Service(
                id = "glass",
                group = "detail",
                name = "Crystal Glass Coating",
                nameId = "Lapisan Kaca Kristal",
                category = "Car Detailing",
                categoryId = "Detailing Mobil",
                description = "Hydrophobic glass coating on all windshields and windows for clarity and safety in heavy rain.",
                descriptionId = "Lapisan kaca hidrofobik untuk seluruh kaca mobil agar pandangan jernih saat hujan deras.",
                duration = "90 minutes",
                durationId = "90 menit",
                price = 195000,
                featured = false,
                features = listOf("Water-spot removal", "Hydrophobic coating", "Wiper blade conditioning", "All windows treated")
            ),

            // MOONTIR SPECIAL CARE (All-Inclusive Signature Packages)
            Service(
                id = "full",
                group = "special",
                name = "Full Moon Package",
                nameId = "Paket Full Moon",
                category = "Moontir Special Care",
                categoryId = "Perawatan Spesial",
                description = "Signature all-in-one bundle: Exterior Detailing + Interior Detailing with 35-point mechanical inspection.",
                descriptionId = "Paket andalan lengkap: Detailing Eksterior + Detailing Interior dengan inspeksi mekanis 35 titik.",
                duration = "4 hours",
                durationId = "4 jam",
                price = 495000,
                featured = true,
                features = listOf("Exterior detail", "Interior detail", "35-point safety check", "Engine bay dressing")
            ),
            Service(
                id = "eclipse",
                group = "special",
                name = "Total Eclipse Care",
                nameId = "Perawatan Total Eclipse",
                category = "Moontir Special Care",
                categoryId = "Perawatan Spesial",
                description = "The ultimate pinnacle treatment: Full Detailing, Crystal Glass Coating, and Complete Engine Tune-Up.",
                descriptionId = "Perawatan termewah: Detailing Penuh, Lapisan Kaca Kristal, dan Tune-Up Mesin Lengkap.",
                duration = "5 hours",
                durationId = "5 jam",
                price = 685000,
                featured = false,
                features = listOf("Pinnacle detailing", "Crystal glass coating", "Full tune-up", "Synthetic oil replacement")
            )
        )
    }
}
