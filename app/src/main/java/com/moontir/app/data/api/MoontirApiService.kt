package com.moontir.app.data.api

import com.moontir.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface MoontirApiService {

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("api/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<User>

    @PATCH("api/me")
    suspend fun updateMe(
        @Header("Authorization") token: String,
        @Body request: ProfileUpdateRequest
    ): Response<User>

    @GET("api/services")
    suspend fun getServices(): Response<List<Service>>

    @POST("api/quote")
    suspend fun getQuote(@Body request: QuoteRequest): Response<Quote>

    @GET("api/vehicles")
    suspend fun getVehicles(@Header("Authorization") token: String): Response<List<Vehicle>>

    @POST("api/vehicles")
    suspend fun addVehicle(
        @Header("Authorization") token: String,
        @Body payload: VehiclePayload
    ): Response<Vehicle>

    @PATCH("api/vehicles/{id}")
    suspend fun updateVehicle(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body payload: VehiclePayload
    ): Response<Vehicle>

    @DELETE("api/vehicles/{id}")
    suspend fun deleteVehicle(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Map<String, Any>>

    @GET("api/geocode")
    suspend fun geocode(@Query("q") query: String): Response<List<GeocodeResult>>

    @GET("api/reverse-geocode")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): Response<GeocodeResult>

    @GET("api/orders")
    suspend fun getOrders(@Header("Authorization") token: String): Response<List<Order>>

    @POST("api/orders")
    suspend fun createOrder(
        @Header("Authorization") token: String,
        @Body order: OrderCreate
    ): Response<Order>

    @POST("api/orders/{id}/complete")
    suspend fun completeOrder(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Order>

    @POST("api/orders/{id}/rating")
    suspend fun rateOrder(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body rating: RatingInput
    ): Response<Order>
}
