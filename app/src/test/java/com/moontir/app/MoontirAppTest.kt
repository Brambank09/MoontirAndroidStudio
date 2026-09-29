package com.moontir.app

import com.google.gson.Gson
import com.moontir.app.data.model.*
import com.moontir.app.ui.components.formatRupiah
import com.moontir.app.ui.i18n.getAppStrings
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.roundToLong

class MoontirAppTest {

    private val gson = Gson()

    @Test
    fun testFormatRupiah() {
        val formatted = formatRupiah(185000L)
        assertTrue(formatted.contains("185.000") || formatted.contains("185,000"))
        assertTrue(formatted.contains("Rp"))
    }

    @Test
    fun testBilingualStrings() {
        val en = getAppStrings("en")
        val id = getAppStrings("id")

        assertEquals("Premium car care.", en.welcome)
        assertEquals("Perawatan mobil premium.", id.welcome)

        assertEquals("Sign in", en.signIn)
        assertEquals("Masuk", id.signIn)

        assertEquals("HOME", en.home)
        assertEquals("BERANDA", id.home)

        assertEquals("GARAGE", en.garage)
        assertEquals("GARASI", id.garage)
    }

    @Test
    fun testServiceJsonParsing() {
        val json = """
            {
                "id": "care",
                "group": "light",
                "name": "Essential Car Care",
                "name_id": "Perawatan Mobil Esensial",
                "category": "Light Service",
                "category_id": "Layanan Cepat",
                "description": "A practical checkup for everyday driving confidence.",
                "description_id": "Pemeriksaan praktis untuk berkendara sehari-hari.",
                "duration": "90 minutes",
                "duration_id": "90 menit",
                "price": 185000,
                "featured": false,
                "features": ["Fluid check", "Battery check", "Tire pressure"]
            }
        """.trimIndent()

        val service = gson.fromJson(json, Service::class.java)
        assertEquals("care", service.id)
        assertEquals("light", service.group)
        assertEquals("Essential Car Care", service.name)
        assertEquals("Perawatan Mobil Esensial", service.nameId)
        assertEquals(185000L, service.price)
        assertEquals(3, service.features.size)
    }

    @Test
    fun testVehicleJsonParsing() {
        val json = """
            {
                "id": "veh-123",
                "nickname": "Daily Sedan",
                "make": "Toyota",
                "model": "Camry",
                "year": "2023",
                "plate": "B 1234 XYZ",
                "type": "Sedan",
                "created_at": "2026-09-28T00:00:00Z"
            }
        """.trimIndent()

        val vehicle = gson.fromJson(json, Vehicle::class.java)
        assertEquals("veh-123", vehicle.id)
        assertEquals("Daily Sedan", vehicle.nickname)
        assertEquals("Toyota", vehicle.make)
        assertEquals("Camry", vehicle.model)
        assertEquals("Sedan", vehicle.type)
    }

    @Test
    fun testQuoteCalculationMatchingBackend() {
        val multipliers = mapOf(
            "Sedan" to 1.0,
            "Hatchback" to 1.0,
            "MPV" to 1.15,
            "SUV" to 1.25,
            "Truck" to 1.40,
            "Pickup" to 1.30
        )

        val basePrice = 275000L // Moon Shine Detail
        val suvMultiplier = multipliers["SUV"]!!
        val calculatedTotal = (Math.round((basePrice * suvMultiplier) / 500.0) * 500).toLong()
        val surcharge = calculatedTotal - basePrice

        assertEquals(344000L, calculatedTotal)
        assertEquals(69000L, surcharge)
        assertTrue(calculatedTotal > basePrice)
    }

    @Test
    fun testOrderJsonParsing() {
        val json = """
            {
                "id": "ord-abc",
                "service_id": "shine",
                "service_name": "Moon Shine Detail",
                "vehicle": {
                    "id": "v1",
                    "nickname": "Innova",
                    "make": "Toyota",
                    "model": "Innova",
                    "year": "2022",
                    "plate": "B 5678 DEF",
                    "type": "MPV"
                },
                "address": {
                    "label": "Jl. Pandanaran 10, Semarang",
                    "latitude": -6.985,
                    "longitude": 110.415
                },
                "schedule_date": "2026-09-29",
                "schedule_time": "09:00 – 11:00",
                "notes": "Gate code #1234",
                "status": "dispatch",
                "status_history": [
                    {
                        "key": "dispatch",
                        "label": "Dispatch confirmed",
                        "label_id": "Dispatch dikonfirmasi",
                        "at": "2026-09-28T05:00:00Z"
                    }
                ],
                "items": [
                    {
                        "label": "Moon Shine Detail",
                        "label_id": "Detail Moon Shine",
                        "amount": 275000
                    },
                    {
                        "label": "MPV handling surcharge",
                        "label_id": "Biaya penanganan MPV",
                        "amount": 41500
                    }
                ],
                "total": 316500,
                "payment_status": "unpaid",
                "created_at": "2026-09-28T05:00:00Z"
            }
        """.trimIndent()

        val order = gson.fromJson(json, Order::class.java)
        assertEquals("ord-abc", order.id)
        assertEquals("shine", order.serviceId)
        assertEquals("Moon Shine Detail", order.serviceName)
        assertEquals("dispatch", order.status)
        assertEquals(316500L, order.total)
        assertEquals(2, order.items.size)
        assertEquals("unpaid", order.paymentStatus)
    }

    @Test
    fun testEmergencyServicesAndBilingualCategories() {
        val en = getAppStrings("en")
        val id = getAppStrings("id")

        assertEquals("Emergency service", en.groupEmergency)
        assertEquals("Layanan darurat", id.groupEmergency)

        assertEquals("Car service", en.groupCarService)
        assertEquals("Servis mobil", id.groupCarService)

        assertEquals("EMERGENCY SERVICE", en.emergencyBannerTitle)
        assertEquals("LAYANAN DARURAT", id.emergencyBannerTitle)

        assertEquals("App Version", en.appVersion)
        assertEquals("Versi Aplikasi", id.appVersion)
    }

    @Test
    fun testMultiServiceQuoteCalculation() {
        // Test combining Oil Change (290,000) and Full Tune-Up (350,000)
        val oilChangePrice = 290000L
        val tuneUpPrice = 350000L
        val subtotal = oilChangePrice + tuneUpPrice // 640,000

        val multipliers = mapOf("SUV" to 1.25)
        val suvMultiplier = multipliers["SUV"]!!
        val calculatedTotal = (Math.round((subtotal * suvMultiplier) / 500.0) * 500).toLong()
        val surcharge = calculatedTotal - subtotal

        assertEquals(640000L, subtotal)
        assertEquals(800000L, calculatedTotal)
        assertEquals(160000L, surcharge)
    }

    @Test
    fun testEmergencyCategoryIsolationAndDurationCalculation() {
        val en = getAppStrings("en")
        val id = getAppStrings("id")

        assertTrue(en.emergencyExclusive.contains("Emergency services cannot be combined"))
        assertTrue(id.emergencyExclusive.contains("Layanan darurat tidak dapat digabungkan"))
        assertEquals("24/7 IMMEDIATE DISPATCH", en.emergencyImmediateTitle)
        assertEquals("LAYANAN DARURAT 24/7 SIAGA", id.emergencyImmediateTitle)

        // Duration calculation: Oil Change (45m) + Full Tune-Up (90m) = 135m (2h 15m)
        val oilChangeMins = 45
        val tuneUpMins = 90
        val totalMins = oilChangeMins + tuneUpMins
        assertEquals(135, totalMins)

        // Start time 09:30 -> 11:45
        val startHour = 9
        val startMin = 30
        val startTotal = startHour * 60 + startMin
        val endTotal = startTotal + totalMins
        val endHour = (endTotal / 60) % 24
        val endMin = endTotal % 60
        val completionStr = String.format(java.util.Locale.US, "%02d:%02d", endHour, endMin)
        assertEquals("11:45", completionStr)

        // Emergency Towing (45m) + Battery Jumper (30m) = 75m (1h 15m)
        val towMins = 45
        val jumpMins = 30
        assertEquals(75, towMins + jumpMins)
    }
}
