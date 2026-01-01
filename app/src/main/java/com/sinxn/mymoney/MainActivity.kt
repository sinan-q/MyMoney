package com.sinxn.mymoney

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sinxn.mymoney.feature.home.HomeScreen
import com.sinxn.mymoney.feature.settings.BackupScreen
import com.sinxn.mymoney.ui.theme.MyMoneyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MyMoneyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val viewModel: MainViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                    val startDest by viewModel.startDestination.collectAsState()
                    
                    if (startDest != "loading") {
                        NavHost(navController = navController, startDestination = startDest) {
                            composable("home") {
                                HomeScreen(
                                    onNavigateToBackup = {
                                        navController.navigate("backup")
                                    },
                                    onNavigateToWallet = { walletId ->
                                        navController.navigate("wallet_details/$walletId")
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate("settings")
                                    },
                                    onAddTransaction = {
                                        navController.navigate("transaction_details/new")
                                    }
                                )
                            }
                            composable("backup") {
                                BackupScreen()
                            }
                            composable("settings") {
                                com.sinxn.mymoney.feature.settings.SettingsScreen(
                                    onNavigateUp = { navController.navigateUp() }
                                )
                            }
                            composable("wallet_details/{walletId}") {
                                com.sinxn.mymoney.feature.wallet.WalletDetailsScreen(
                                    onNavigateUp = { 
                                        if (navController.previousBackStackEntry != null) {
                                            navController.navigateUp()
                                        } else {
                                            // If launched directly here, back should go home
                                            navController.navigate("home") {
                                                popUpTo("wallet_details/{walletId}") { inclusive = true }
                                            }
                                        }
                                    },
                                    onTransactionClick = { transactionId ->
                                        navController.navigate("transaction_details/$transactionId")
                                    },
                                    onNavigateToRecap = {
                                        navController.navigate("recap")
                                    }
                                )
                            }
                            composable(
                                "transaction_details/{transactionId}",
                                deepLinks = listOf(
                                    androidx.navigation.navDeepLink { uriPattern = "mymoney://transaction/{transactionId}" }
                                )
                            ) {
                                com.sinxn.mymoney.feature.transaction.TransactionDetailsScreen(
                                    onNavigateBack = { navController.navigateUp() }
                                )
                            }
                            composable("recap") {
                                com.sinxn.mymoney.feature.recap.YearRecapScreen(
                                    onClose = { navController.popBackStack() }
                                )
                            }
                        }
                    } else {
                        // Show Loading Screen or Splash
                        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                             androidx.compose.material3.CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}