package com.moontir.app.data.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: String,
    val name: String,
    val email: String
)

data class AuthResponse(
    val token: String,
    val user: User
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class ProfileUpdateRequest(
    val name: String
)

data class Service(
    val id: String,
    val group: String,
    val name: String,
    @SerializedName("name_id") val nameId: String,
    val category: String,
    @SerializedName("category_id") val categoryId: String,
    val description: String,
    @SerializedName("description_id") val descriptionId: String,
    val duration: String,
    @SerializedName("duration_id") val durationId: String,
    val price: Long,
    val featured: Boolean,
    val features: List<String> = emptyList()
)

data class Vehicle(
    val id: String,
    val nickname: String,
    val make: String,
    val model: String,
    val year: String,
    val plate: String,
    val type: String,
    @SerializedName("created_at") val createdAt: String? = null
)

data class VehiclePayload(
    val nickname: String,
    val make: String,
    val model: String,
    val year: String,
    val plate: String,
    val type: String
)

data class Address(
    val label: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class InvoiceItem(
    val label: String,
    @SerializedName("label_id") val labelId: String? = null,
    val amount: Long
)

data class Rating(
    val stars: Int,
    val note: String = "",
    @SerializedName("rated_at") val ratedAt: String? = null
)

data class RatingInput(
    val stars: Int,
    val note: String = ""
)

data class StatusHistoryItem(
    val key: String,
    val label: String,
    @SerializedName("label_id") val labelId: String? = null,
    val at: String
)

data class Order(
    val id: String,
    @SerializedName("service_id") val serviceId: String,
    @SerializedName("service_name") val serviceName: String,
    val vehicle: Vehicle,
    val address: Address,
    @SerializedName("schedule_date") val scheduleDate: String,
    @SerializedName("schedule_time") val scheduleTime: String,
    val notes: String = "",
    val status: String,
    @SerializedName("status_history") val statusHistory: List<StatusHistoryItem> = emptyList(),
    val items: List<InvoiceItem> = emptyList(),
    val total: Long,
    @SerializedName("payment_status") val paymentStatus: String,
    @SerializedName("created_at") val createdAt: String,
    val rating: Rating? = null
)

data class OrderCreate(
    @SerializedName("service_id") val serviceId: String,
    @SerializedName("vehicle_id") val vehicleId: String,
    val address: Address,
    @SerializedName("schedule_date") val scheduleDate: String,
    @SerializedName("schedule_time") val scheduleTime: String,
    val notes: String = ""
)

data class Quote(
    val items: List<InvoiceItem>,
    val total: Long,
    val multiplier: Double,
    @SerializedName("vehicle_type") val vehicleType: String,
    val base: Long,
    val surcharge: Long
)

data class QuoteRequest(
    @SerializedName("service_id") val serviceId: String,
    @SerializedName("vehicle_type") val vehicleType: String = "Sedan"
)

data class GeocodeResult(
    @SerializedName("displayName") val displayName: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("osmId") val osmId: Long? = null
)
