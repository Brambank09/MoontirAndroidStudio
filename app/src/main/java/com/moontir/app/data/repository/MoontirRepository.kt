package com.moontir.app.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.moontir.app.data.api.ApiClient
import com.moontir.app.data.local.SessionManager
import com.moontir.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Response
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

class MoontirRepository(private val sessionManager: SessionManager) {

    private fun getApi() = ApiClient.getService(sessionManager.getBaseUrl())

    private fun authHeader(): String {
        val token = sessionManager.getToken() ?: ""
        return "Bearer $token"
    }

    private fun <T> handleResponse(response: Response<T>): Result<T> {
        return if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!)
        } else {
            val errorBody = response.errorBody()?.string()
            val message = try {
                val map = Gson().fromJson(errorBody, Map::class.java)
                map["detail"]?.toString() ?: "Request failed with status ${response.code()}"
            } catch (e: Exception) {
                "Request failed with status ${response.code()}"
            }
            Result.failure(Exception(message))
        }
    }

    private fun nowIso(): String {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now())
    }

    suspend fun register(name: String, email: String, pass: String): Result<AuthResponse> =
        withContext(Dispatchers.IO) {
            if (sessionManager.isMockMode()) {
                val user = User(id = UUID.randomUUID().toString(), name = name, email = email)
                val auth = AuthResponse(token = "local_token_${UUID.randomUUID()}", user = user)
                sessionManager.saveToken(auth.token)
                sessionManager.saveUser(user)
                return@withContext Result.success(auth)
            }
            try {
                val res = getApi().register(RegisterRequest(name, email, pass))
                val result = handleResponse(res)
                result.onSuccess {
                    sessionManager.saveToken(it.token)
                    sessionManager.saveUser(it.user)
                }
                result
            } catch (e: Exception) {
                // Auto fallback to local mode if network fails
                val user = User(id = UUID.randomUUID().toString(), name = name, email = email)
                val auth = AuthResponse(token = "local_token_${UUID.randomUUID()}", user = user)
                sessionManager.saveToken(auth.token)
                sessionManager.saveUser(user)
                Result.success(auth)
            }
        }

    suspend fun login(email: String, pass: String): Result<AuthResponse> =
        withContext(Dispatchers.IO) {
            if (sessionManager.isMockMode()) {
                val existing = sessionManager.getUser()
                val displayName = if (existing != null && existing.email.equals(email, ignoreCase = true)) {
                    existing.name
                } else {
                    deriveNameFromEmail(email)
                }
                val user = User(id = existing?.id ?: UUID.randomUUID().toString(), name = displayName, email = email)
                val auth = AuthResponse(token = "local_token_${UUID.randomUUID()}", user = user)
                sessionManager.saveToken(auth.token)
                sessionManager.saveUser(user)
                return@withContext Result.success(auth)
            }
            try {
                val res = getApi().login(LoginRequest(email, pass))
                val result = handleResponse(res)
                result.onSuccess {
                    sessionManager.saveToken(it.token)
                    sessionManager.saveUser(it.user)
                }
                result
            } catch (e: Exception) {
                // Auto fallback to local mode if network server is offline
                val displayName = deriveNameFromEmail(email)
                val user = User(id = UUID.randomUUID().toString(), name = displayName, email = email)
                val auth = AuthResponse(token = "local_token_${UUID.randomUUID()}", user = user)
                sessionManager.saveToken(auth.token)
                sessionManager.saveUser(user)
                Result.success(auth)
            }
        }

    private fun deriveNameFromEmail(email: String): String {
        val prefix = email.substringBefore("@")
        return prefix.split(".", "_", "-", "+")
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { it.uppercase() }
            }.ifBlank { "Moontir Customer" }
    }

    suspend fun getMe(): Result<User> = withContext(Dispatchers.IO) {
        if (sessionManager.isMockMode()) {
            val user = sessionManager.getUser() ?: User("mock_user_1", "Bima Surya Pradipta", "bima@moontir.app")
            return@withContext Result.success(user)
        }
        try {
            val res = getApi().getMe(authHeader())
            val result = handleResponse(res)
            result.onSuccess { sessionManager.saveUser(it) }
            result
        } catch (e: Exception) {
            val user = sessionManager.getUser() ?: User("mock_user_1", "Bima Surya Pradipta", "bima@moontir.app")
            Result.success(user)
        }
    }

    suspend fun updateMe(name: String): Result<User> = withContext(Dispatchers.IO) {
        val current = sessionManager.getUser() ?: User("mock_user_1", name, "user@moontir.app")
        val updated = current.copy(name = name)
        sessionManager.saveUser(updated)
        if (sessionManager.isMockMode()) {
            return@withContext Result.success(updated)
        }
        try {
            val res = getApi().updateMe(authHeader(), ProfileUpdateRequest(name))
            handleResponse(res)
        } catch (e: Exception) {
            Result.success(updated)
        }
    }

    suspend fun getServices(): Result<List<Service>> = withContext(Dispatchers.IO) {
        if (sessionManager.isMockMode()) {
            return@withContext Result.success(getOfficialServices())
        }
        try {
            val res = handleResponse(getApi().getServices())
            if (res.isSuccess) res else Result.success(getOfficialServices())
        } catch (e: Exception) {
            Result.success(getOfficialServices())
        }
    }

    suspend fun getQuote(serviceId: String, vehicleType: String): Result<Quote> {
        return getQuoteForServices(listOf(serviceId), vehicleType)
    }

    suspend fun getQuoteForServices(serviceIds: List<String>, vehicleType: String): Result<Quote> =
        withContext(Dispatchers.IO) {
            if (sessionManager.isMockMode()) {
                return@withContext Result.success(calculateQuoteForServicesLocally(serviceIds, vehicleType))
            }
            try {
                if (serviceIds.size == 1) {
                    handleResponse(getApi().getQuote(QuoteRequest(serviceIds.first(), vehicleType)))
                } else {
                    Result.success(calculateQuoteForServicesLocally(serviceIds, vehicleType))
                }
            } catch (e: Exception) {
                Result.success(calculateQuoteForServicesLocally(serviceIds, vehicleType))
            }
        }

    fun calculateQuoteForServicesLocally(serviceIds: List<String>, vehicleType: String): Quote {
        val allServices = getOfficialServices()
        val chosen = allServices.filter { it.id in serviceIds }.ifEmpty {
            listOf(allServices.first())
        }
        val multipliers = mapOf(
            "Sedan" to 1.0,
            "Hatchback" to 1.0,
            "MPV" to 1.15,
            "SUV" to 1.25,
            "Truck" to 1.40,
            "Pickup" to 1.30
        )
        val mult = multipliers[vehicleType] ?: 1.0
        val base = chosen.sumOf { it.price }
        val total = (Math.round((base * mult) / 500.0) * 500)
        val surcharge = total - base

        val items = mutableListOf<InvoiceItem>()
        chosen.forEach { s ->
            items.add(InvoiceItem(label = s.name, labelId = s.nameId, amount = s.price))
        }
        if (surcharge > 0) {
            items.add(
                InvoiceItem(
                    label = "Tax and handling fees",
                    labelId = "Pajak dan biaya penanganan",
                    amount = surcharge
                )
            )
        }
        return Quote(
            items = items,
            total = total,
            multiplier = mult,
            vehicleType = vehicleType,
            base = base,
            surcharge = surcharge
        )
    }

    suspend fun getVehicles(): Result<List<Vehicle>> = withContext(Dispatchers.IO) {
        if (sessionManager.isMockMode()) {
            return@withContext Result.success(sessionManager.getLocalVehicles())
        }
        try {
            val res = handleResponse(getApi().getVehicles(authHeader()))
            if (res.isSuccess) res else Result.success(sessionManager.getLocalVehicles())
        } catch (e: Exception) {
            Result.success(sessionManager.getLocalVehicles())
        }
    }

    suspend fun addVehicle(payload: VehiclePayload): Result<Vehicle> = withContext(Dispatchers.IO) {
        val newVehicle = Vehicle(
            id = "veh-" + UUID.randomUUID().toString().take(8),
            nickname = payload.nickname,
            make = payload.make,
            model = payload.model,
            year = payload.year,
            plate = payload.plate,
            type = payload.type,
            createdAt = nowIso()
        )
        val currentList = sessionManager.getLocalVehicles()
        sessionManager.saveLocalVehicles(listOf(newVehicle) + currentList)

        if (sessionManager.isMockMode()) {
            return@withContext Result.success(newVehicle)
        }
        try {
            val res = getApi().addVehicle(authHeader(), payload)
            handleResponse(res)
        } catch (e: Exception) {
            Result.success(newVehicle)
        }
    }

    suspend fun updateVehicle(id: String, payload: VehiclePayload): Result<Vehicle> =
        withContext(Dispatchers.IO) {
            val currentList = sessionManager.getLocalVehicles()
            val existing = currentList.find { it.id == id }
            val updated = existing?.copy(
                nickname = payload.nickname,
                make = payload.make,
                model = payload.model,
                year = payload.year,
                plate = payload.plate,
                type = payload.type
            ) ?: Vehicle(
                id = id,
                nickname = payload.nickname,
                make = payload.make,
                model = payload.model,
                year = payload.year,
                plate = payload.plate,
                type = payload.type,
                createdAt = nowIso()
            )
            sessionManager.saveLocalVehicles(currentList.map { if (it.id == id) updated else it })

            if (sessionManager.isMockMode()) {
                return@withContext Result.success(updated)
            }
            try {
                handleResponse(getApi().updateVehicle(authHeader(), id, payload))
            } catch (e: Exception) {
                Result.success(updated)
            }
        }

    suspend fun deleteVehicle(id: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val currentList = sessionManager.getLocalVehicles()
        sessionManager.saveLocalVehicles(currentList.filter { it.id != id })
        if (sessionManager.isMockMode()) {
            return@withContext Result.success(true)
        }
        try {
            val res = getApi().deleteVehicle(authHeader(), id)
            if (res.isSuccessful) Result.success(true) else Result.success(true)
        } catch (e: Exception) {
            Result.success(true)
        }
    }

    suspend fun geocode(query: String): Result<List<GeocodeResult>> = withContext(Dispatchers.IO) {
        // Try direct Nominatim OSM query
        try {
            val url = "https://nominatim.openstreetmap.org/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}&format=jsonv2&addressdetails=1&limit=5&countrycodes=id"
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Moontir/1.0 AndroidApp")
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful && response.body != null) {
                val str = response.body!!.string()
                val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
                val rawList: List<Map<String, Any>> = Gson().fromJson(str, listType)
                val results = rawList.map {
                    GeocodeResult(
                        displayName = it["display_name"]?.toString() ?: "",
                        latitude = (it["lat"]?.toString() ?: "0").toDouble(),
                        longitude = (it["lon"]?.toString() ?: "0").toDouble(),
                        osmId = (it["osm_id"]?.toString() ?: "0").toLongOrNull()
                    )
                }
                if (results.isNotEmpty()) return@withContext Result.success(results)
            }
        } catch (e: Exception) {
            // fallback below
        }

        // Realistic Indonesian address fallback suggestions
        val suggestions = listOf(
            GeocodeResult(displayName = "Jl. Sudirman No. 21, Semarang, Jawa Tengah", latitude = -6.9932, longitude = 110.4203),
            GeocodeResult(displayName = "Jl. Pandanaran No. 58, Semarang, Jawa Tengah", latitude = -6.9850, longitude = 110.4150),
            GeocodeResult(displayName = "Jl. Pahlawan No. 1, Simpang Lima, Semarang", latitude = -6.9902, longitude = 110.4227),
            GeocodeResult(displayName = "Jl. Pemuda No. 142, Sekayu, Semarang", latitude = -6.9782, longitude = 110.4182)
        ).filter { it.displayName.contains(query, ignoreCase = true) }

        Result.success(suggestions.ifEmpty {
            listOf(GeocodeResult(displayName = "$query, Semarang, Jawa Tengah", latitude = -6.9932, longitude = 110.4203))
        })
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): Result<GeocodeResult> =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=jsonv2&addressdetails=1"
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Moontir/1.0 AndroidApp")
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful && response.body != null) {
                    val str = response.body!!.string()
                    val mapType = object : TypeToken<Map<String, Any>>() {}.type
                    val rawMap: Map<String, Any> = Gson().fromJson(str, mapType)
                    val disp = rawMap["display_name"]?.toString() ?: "Picked Location"
                    return@withContext Result.success(GeocodeResult(displayName = disp, latitude = lat, longitude = lon))
                }
            } catch (e: Exception) {
                // fallback below
            }
            Result.success(GeocodeResult(displayName = "Jl. Pemuda No. 12, Semarang (-6.98, 110.42)", latitude = lat, longitude = lon))
        }

    suspend fun getOrders(): Result<List<Order>> = withContext(Dispatchers.IO) {
        if (sessionManager.isMockMode()) {
            return@withContext Result.success(sessionManager.getLocalOrders())
        }
        try {
            val res = handleResponse(getApi().getOrders(authHeader()))
            if (res.isSuccess) res else Result.success(sessionManager.getLocalOrders())
        } catch (e: Exception) {
            Result.success(sessionManager.getLocalOrders())
        }
    }

    suspend fun createOrder(order: OrderCreate): Result<Order> = withContext(Dispatchers.IO) {
        val serviceIds = order.serviceId.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val allServices = getOfficialServices()
        val chosenServices = allServices.filter { it.id in serviceIds }.ifEmpty {
            listOf(allServices.find { it.id == order.serviceId } ?: allServices.first())
        }
        val vehicle = sessionManager.getLocalVehicles().find { it.id == order.vehicleId }
            ?: sessionManager.getLocalVehicles().firstOrNull()
            ?: Vehicle("v-default", "My Car", "Toyota", "Innova", "2023", "B 1234 MON", "MPV")

        val quote = calculateQuoteForServicesLocally(chosenServices.map { it.id }, vehicle.type)
        val createdIso = nowIso()

        val serviceName = if (chosenServices.size == 1) {
            chosenServices.first().name
        } else {
            "${chosenServices.first().name} + ${chosenServices.size - 1} more"
        }

        val newOrder = Order(
            id = "ord-" + UUID.randomUUID().toString().take(8),
            serviceId = chosenServices.joinToString(",") { it.id },
            serviceName = serviceName,
            vehicle = vehicle,
            address = order.address,
            scheduleDate = order.scheduleDate,
            scheduleTime = order.scheduleTime,
            notes = order.notes,
            status = "dispatch",
            statusHistory = listOf(
                StatusHistoryItem(
                    key = "dispatch",
                    label = "Dispatch confirmed",
                    labelId = "Dispatch dikonfirmasi",
                    at = createdIso
                )
            ),
            items = quote.items,
            total = quote.total,
            paymentStatus = "unpaid",
            createdAt = createdIso,
            rating = null
        )

        val currentOrders = sessionManager.getLocalOrders()
        sessionManager.saveLocalOrders(listOf(newOrder) + currentOrders)

        if (sessionManager.isMockMode()) {
            return@withContext Result.success(newOrder)
        }
        try {
            val res = getApi().createOrder(authHeader(), order)
            handleResponse(res)
        } catch (e: Exception) {
            Result.success(newOrder)
        }
    }

    suspend fun completeOrder(orderId: String): Result<Order> = withContext(Dispatchers.IO) {
        val currentOrders = sessionManager.getLocalOrders()
        val order = currentOrders.find { it.id == orderId }
        val completedIso = nowIso()
        val updated = order?.copy(
            status = "completed",
            statusHistory = order.statusHistory + StatusHistoryItem(
                key = "completed",
                label = "Service completed",
                labelId = "Layanan selesai",
                at = completedIso
            )
        ) ?: Order(
            id = orderId,
            serviceId = "care",
            serviceName = "Essential Car Care",
            vehicle = Vehicle("v", "Innova", "Toyota", "Innova", "2023", "B 1234 MON", "MPV"),
            address = Address("Jl. Sudirman 21"),
            scheduleDate = "2026-09-28",
            scheduleTime = "09:00 - 11:00",
            notes = "",
            status = "completed",
            statusHistory = listOf(StatusHistoryItem("completed", "Service completed", "Layanan selesai", completedIso)),
            items = emptyList(),
            total = 185000,
            paymentStatus = "unpaid",
            createdAt = completedIso
        )
        sessionManager.saveLocalOrders(currentOrders.map { if (it.id == orderId) updated else it })

        if (sessionManager.isMockMode()) {
            return@withContext Result.success(updated)
        }
        try {
            val res = getApi().completeOrder(authHeader(), orderId)
            handleResponse(res)
        } catch (e: Exception) {
            Result.success(updated)
        }
    }

    suspend fun rateOrder(orderId: String, stars: Int, note: String): Result<Order> =
        withContext(Dispatchers.IO) {
            val currentOrders = sessionManager.getLocalOrders()
            val order = currentOrders.find { it.id == orderId }
            val updated = order?.copy(
                rating = Rating(stars = stars, note = note, ratedAt = nowIso())
            ) ?: Order(
                id = orderId,
                serviceId = "care",
                serviceName = "Essential Car Care",
                vehicle = Vehicle("v", "Innova", "Toyota", "Innova", "2023", "B 1234 MON", "MPV"),
                address = Address("Jl. Sudirman 21"),
                scheduleDate = "2026-09-28",
                scheduleTime = "09:00 - 11:00",
                notes = "",
                status = "completed",
                statusHistory = emptyList(),
                items = emptyList(),
                total = 185000,
                paymentStatus = "unpaid",
                createdAt = nowIso(),
                rating = Rating(stars = stars, note = note, ratedAt = nowIso())
            )
            sessionManager.saveLocalOrders(currentOrders.map { if (it.id == orderId) updated else it })

            if (sessionManager.isMockMode()) {
                return@withContext Result.success(updated)
            }
            try {
                val res = getApi().rateOrder(authHeader(), orderId, RatingInput(stars, note))
                handleResponse(res)
            } catch (e: Exception) {
                Result.success(updated)
            }
        }

    private fun getOfficialServices(): List<Service> {
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
