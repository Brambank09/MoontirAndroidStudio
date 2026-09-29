package com.moontir.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moontir.app.ui.components.MoontirBottomBar
import com.moontir.app.ui.components.MoontirLogoView
import com.moontir.app.ui.components.MoontirTopBar
import com.moontir.app.ui.i18n.LocalStrings
import com.moontir.app.ui.i18n.getAppStrings
import com.moontir.app.ui.screens.*
import com.moontir.app.ui.theme.MoontirAppTheme
import com.moontir.app.ui.theme.MoontirTheme
import com.moontir.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val user by viewModel.user.collectAsState()
            val token by viewModel.token.collectAsState()
            val language by viewModel.language.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()
            val baseUrl by viewModel.baseUrl.collectAsState()
            val isMockMode by viewModel.isMockMode.collectAsState()

            val services by viewModel.services.collectAsState()
            val vehicles by viewModel.vehicles.collectAsState()
            val orders by viewModel.orders.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()

            val isLoading by viewModel.isLoading.collectAsState()
            val isBusy by viewModel.isBusy.collectAsState()
            val errorMessage by viewModel.errorMessage.collectAsState()
            val toastMessage by viewModel.toastMessage.collectAsState()

            val isBookingOpen by viewModel.isBookingOpen.collectAsState()
            val preselectedService by viewModel.preselectedService.collectAsState()
            val isAddVehicleOpen by viewModel.isAddVehicleOpen.collectAsState()
            val vehicleToEdit by viewModel.vehicleToEdit.collectAsState()
            val vehicleToDelete by viewModel.vehicleToDelete.collectAsState()
            val orderForInvoice by viewModel.orderForInvoice.collectAsState()
            val orderForRating by viewModel.orderForRating.collectAsState()

            val strings = remember(language) { getAppStrings(language) }
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(toastMessage) {
                toastMessage?.let {
                    snackbarHostState.showSnackbar(it)
                    viewModel.clearToast()
                }
            }

            CompositionLocalProvider(LocalStrings provides strings) {
                MoontirAppTheme(themeMode = themeMode) {
                    val colors = MoontirTheme.colors

                    Scaffold(
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        containerColor = colors.surface,
                        bottomBar = {
                            if (token != null && user != null && !isLoading) {
                                MoontirBottomBar(
                                    currentScreen = currentScreen,
                                    onNavigate = { viewModel.setScreen(it) }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(colors.surface)
                                .padding(innerPadding)
                        ) {
                            when {
                                isLoading -> {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        MoontirLogoView(height = 44.dp)
                                        Spacer(modifier = Modifier.height(24.dp))
                                        CircularProgressIndicator(color = colors.brand)
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = strings.welcome.uppercase(),
                                            color = colors.muted,
                                            fontSize = 11.sp,
                                            letterSpacing = 2.sp
                                        )
                                    }
                                }

                                token == null || user == null -> {
                                    AuthScreen(
                                        onLogin = { email, pass -> viewModel.login(email, pass) },
                                        onRegister = { name, email, pass -> viewModel.register(name, email, pass) },
                                        onToggleLanguage = { viewModel.toggleLanguage() },
                                        currentLanguage = language,
                                        busy = isBusy,
                                        errorMessage = errorMessage,
                                        isMockMode = isMockMode
                                    )
                                }

                                else -> {
                                    val currentUser = user!!
                                    val activeCount = orders.count { it.status != "completed" }

                                    Column(modifier = Modifier.fillMaxSize()) {
                                        MoontirTopBar(
                                            activeOrderCount = activeCount,
                                            currentLanguage = language,
                                            onToggleLanguage = { viewModel.toggleLanguage() },
                                            onOrdersClick = { viewModel.setScreen("orders") }
                                        )

                                        Box(modifier = Modifier.weight(1f)) {
                                            when (currentScreen) {
                                                "home" -> {
                                                    HomeScreen(
                                                        user = currentUser,
                                                        services = services,
                                                        orders = orders,
                                                        isIndonesian = language == "id",
                                                        onBookClick = { s ->
                                                            viewModel.preselectedService.value = s
                                                            viewModel.isBookingOpen.value = true
                                                        },
                                                        onNavigateServices = { filter ->
                                                            viewModel.selectedServicesFilter.value = filter
                                                            viewModel.setScreen("services")
                                                        },
                                                        onNavigateOrders = { viewModel.setScreen("orders") },
                                                        onOpenOrderInvoice = { viewModel.orderForInvoice.value = it }
                                                    )
                                                }

                                                "services" -> {
                                                    val servicesFilter by viewModel.selectedServicesFilter.collectAsState()
                                                    ServicesScreen(
                                                        services = services,
                                                        isIndonesian = language == "id",
                                                        initialFilter = servicesFilter,
                                                        onBookClick = { s ->
                                                            viewModel.preselectedService.value = s
                                                            viewModel.isBookingOpen.value = true
                                                        }
                                                    )
                                                }

                                                "garage" -> {
                                                    GarageScreen(
                                                        vehicles = vehicles,
                                                        onAddVehicle = {
                                                            viewModel.vehicleToEdit.value = null
                                                            viewModel.isAddVehicleOpen.value = true
                                                        },
                                                        onEditVehicle = {
                                                            viewModel.vehicleToEdit.value = it
                                                            viewModel.isAddVehicleOpen.value = true
                                                        },
                                                        onDeleteVehicle = {
                                                            viewModel.vehicleToDelete.value = it
                                                        }
                                                    )
                                                }

                                                "orders" -> {
                                                    OrdersScreen(
                                                        orders = orders,
                                                        onOpenInvoice = { viewModel.orderForInvoice.value = it },
                                                        onCompleteOrder = { viewModel.completeOrder(it) },
                                                        onRateOrder = { viewModel.orderForRating.value = it }
                                                    )
                                                }

                                                "profile" -> {
                                                    ProfileScreen(
                                                        user = currentUser,
                                                        themeMode = themeMode,
                                                        currentLanguage = language,
                                                        baseUrl = baseUrl,
                                                        isMockMode = isMockMode,
                                                        busy = isBusy,
                                                        onUpdateName = { viewModel.updateProfile(it) },
                                                        onCycleTheme = { viewModel.cycleTheme() },
                                                        onToggleLanguage = { viewModel.toggleLanguage() },
                                                        onUpdateBaseUrl = { viewModel.updateBaseUrl(it) },
                                                        onToggleMockMode = { viewModel.toggleMockMode(it) },
                                                        onLogout = { viewModel.logout() }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Modals & Bottom Sheets
                            BookingDialog(
                                isOpen = isBookingOpen,
                                initialService = preselectedService,
                                services = services,
                                vehicles = vehicles,
                                isIndonesian = language == "id",
                                busy = isBusy,
                                errorMessage = errorMessage,
                                onDismiss = {
                                    viewModel.isBookingOpen.value = false
                                    viewModel.preselectedService.value = null
                                    viewModel.clearError()
                                },
                                onAddVehicle = {
                                    viewModel.vehicleToEdit.value = null
                                    viewModel.isAddVehicleOpen.value = true
                                },
                                onGetQuote = { sId, vType -> viewModel.getQuote(sId, vType) },
                                onGetMultiQuote = { sIds, vType -> viewModel.getQuoteForServices(sIds, vType) },
                                onSearchAddress = { q -> viewModel.searchAddress(q) },
                                onReverseGeocode = { lat, lon -> viewModel.reverseGeocode(lat, lon) },
                                onConfirmOrder = { serviceId, vehicleId, address, date, time, note ->
                                    viewModel.createOrder(serviceId, vehicleId, address, date, time, note) {
                                        viewModel.isBookingOpen.value = false
                                        viewModel.preselectedService.value = null
                                    }
                                }
                            )

                            VehicleBottomSheet(
                                isOpen = isAddVehicleOpen,
                                vehicleToEdit = vehicleToEdit,
                                busy = isBusy,
                                errorMessage = errorMessage,
                                onDismiss = {
                                    viewModel.isAddVehicleOpen.value = false
                                    viewModel.vehicleToEdit.value = null
                                    viewModel.clearError()
                                },
                                onSave = { nickname, make, model, year, plate, type, editId ->
                                    viewModel.saveVehicle(nickname, make, model, year, plate, type, editId)
                                }
                            )

                            InvoiceBottomSheet(
                                order = orderForInvoice,
                                isIndonesian = language == "id",
                                onDismiss = { viewModel.orderForInvoice.value = null },
                                onCompleteOrder = { viewModel.completeOrder(it) },
                                onRateOrder = { viewModel.orderForRating.value = it }
                            )

                            RatingBottomSheet(
                                order = orderForRating,
                                busy = isBusy,
                                onDismiss = { viewModel.orderForRating.value = null },
                                onSubmitRating = { order, stars, note ->
                                    viewModel.submitRating(order, stars, note)
                                }
                            )

                            ConfirmDeleteDialog(
                                vehicle = vehicleToDelete,
                                busy = isBusy,
                                onDismiss = { viewModel.vehicleToDelete.value = null },
                                onConfirm = { viewModel.deleteVehicle(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
