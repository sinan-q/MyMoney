package com.sinxn.mymoney

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
        super.onCreate(savedInstanceState)
        setContent {
            MyMoneyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                onNavigateToBackup = {
                                    navController.navigate("backup")
                                },
                                onNavigateToWallet = { walletId ->
                                    navController.navigate("wallet_details/$walletId")
                                }
                            )
                        }
                        composable("backup") {
                            BackupScreen()
                        }
                        composable("wallet_details/{walletId}") {
                            com.sinxn.mymoney.feature.wallet.WalletDetailsScreen(
                                onNavigateUp = { navController.navigateUp() }
                            )
                        }
                    }
                }
            }
        }
    }
}