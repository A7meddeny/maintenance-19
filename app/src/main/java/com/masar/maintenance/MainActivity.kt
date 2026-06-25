package com.masar.maintenance

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.messaging.FirebaseMessaging
import com.masar.maintenance.data.Net
import com.masar.maintenance.ui.screens.*
import com.masar.maintenance.ui.theme.MasarTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    private fun askPermissions() {
        val perms = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) perms += Manifest.permission.POST_NOTIFICATIONS
        val toAsk = perms.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (toAsk.isNotEmpty()) permLauncher.launch(toAsk.toTypedArray())
    }

    fun registerFcmToken() {
        if (!Net.isInitialized() || !Net.session.isLoggedIn) return
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result ?: return@addOnCompleteListener
                CoroutineScope(Dispatchers.IO).launch { runCatching { Net.repo.registerDevice(token) } }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askPermissions()
        registerFcmToken()
        setContent {
            MasarTheme {
                val nav = rememberNavController()
                val start = if (Net.session.isLoggedIn) "home" else "login"
                NavHost(navController = nav, startDestination = start) {

                    composable("login") {
                        LoginScreen(onLoggedIn = {
                            registerFcmToken()
                            nav.navigate("home") { popUpTo("login") { inclusive = true } }
                        })
                    }

                    composable("home") { HomeScreen(nav) }

                    composable(
                        "requests?scope={scope}",
                        arguments = listOf(navArgument("scope") { defaultValue = "" })
                    ) { back ->
                        RequestsScreen(nav, back.arguments?.getString("scope") ?: "")
                    }

                    composable(
                        "request/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType })
                    ) { back ->
                        RequestDetailScreen(nav, back.arguments?.getInt("id") ?: 0)
                    }

                    composable("newRequest") { NewRequestScreen(nav) }
                    composable("periodic") { PeriodicScreen(nav) }
                    composable("scan") { ScanScreen(nav) }
                    composable("inspections") { InspectionsListScreen(nav) }
                    composable(
                        "inspection/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType })
                    ) { back -> InspectionScreen(nav, back.arguments?.getInt("id") ?: 0) }
                    composable("periodic/{rid}") { e ->
                        PeriodicScreen(nav, e.arguments?.getString("rid")?.toIntOrNull() ?: 0)
                    }

                    composable("cars") { CarsScreen(nav) }
                    composable("carReport") { CarReportScreen(nav) }

                    composable(
                        "carForm?id={id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType; defaultValue = 0 })
                    ) { back ->
                        CarFormScreen(nav, back.arguments?.getInt("id") ?: 0)
                    }

                    composable(
                        "carHistory/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType })
                    ) { back ->
                        CarHistoryScreen(nav, back.arguments?.getInt("id") ?: 0)
                    }

                    composable("companies") { CompaniesScreen(nav) }
                    composable("employees") { EmployeesScreen(nav) }
                    composable("staff") { StaffScreen(nav) }
                    composable("dashboard") { DashboardScreen(nav) }

                    composable("rentCar") { RentCarScreen(nav) }
                    composable("dailyWorks") { DailyWorksScreen(nav) }
                    composable("tasks") { TasksScreen(nav) }
                    composable("dwTransfer") { DailyTransferScreen(nav) }
                    composable("dwDelivery") { DailyDeliveryScreen(nav) }
                    composable("dwPart") { DailyPartPickupScreen(nav) }
                    composable("dwNote") { DailyNoteScreen(nav) }
                    composable("rentalInbox") { RentalInboxScreen(nav) }
                    composable(
                        "rentalForm/{rentalId}?kind={kind}",
                        arguments = listOf(
                            navArgument("rentalId") { type = NavType.IntType },
                            navArgument("kind") { defaultValue = "handover" }
                        )
                    ) { back ->
                        RentalInspectionScreen(
                            nav,
                            back.arguments?.getInt("rentalId") ?: 0,
                            back.arguments?.getString("kind") ?: "handover"
                        )
                    }
                }
            }
        }
    }
}
