package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.classifier.LlmModelManager
import com.example.ui.screens.DocumentResultScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ObligationDetailScreen
import com.example.ui.screens.ProcessingScreen
import com.example.ui.screens.ScanScreen
import com.example.ui.screens.SyncScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.KaagazViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: KaagazViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LlmModelManager.init(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        // 1. Home Screen
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToScan = { memberId ->
                                    navController.navigate("scan/$memberId")
                                },
                                onNavigateToDetail = { obligationId ->
                                    navController.navigate("obligation_detail/$obligationId")
                                },
                                onNavigateToSync = {
                                    navController.navigate("sync")
                                }
                            )
                        }

                        // 2. Scan Screen
                        composable(
                            route = "scan/{memberId}",
                            arguments = listOf(
                                navArgument("memberId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val memberId = backStackEntry.arguments?.getLong("memberId") ?: 1L
                            ScanScreen(
                                memberId = memberId,
                                onNavigateBack = { navController.popBackStack() },
                                onPhotoConfirmed = { imagePath, mId, backPath ->
                                    val encodedPath = Uri.encode(imagePath)
                                    val encodedBack = if (backPath != null) Uri.encode(backPath) else ""
                                    navController.navigate("processing/$mId?imagePath=$encodedPath&backPath=$encodedBack")
                                }
                            )
                        }

                        // 3. Processing Screen
                        composable(
                            route = "processing/{memberId}?imagePath={imagePath}&backPath={backPath}",
                            arguments = listOf(
                                navArgument("memberId") { type = NavType.LongType },
                                navArgument("imagePath") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                },
                                navArgument("backPath") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                }
                            )
                        ) { backStackEntry ->
                            val memberId = backStackEntry.arguments?.getLong("memberId") ?: 1L
                            val encodedPath = backStackEntry.arguments?.getString("imagePath") ?: ""
                            val encodedBack = backStackEntry.arguments?.getString("backPath") ?: ""
                            val imagePath = Uri.decode(encodedPath)
                            val backPath = if (encodedBack.isNotBlank()) Uri.decode(encodedBack) else null

                            ProcessingScreen(
                                viewModel = viewModel,
                                imagePath = imagePath,
                                memberId = memberId,
                                backImagePath = backPath,
                                onProcessingFinished = {
                                    navController.navigate("document_result") {
                                        popUpTo("scan/$memberId") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 4. Document Result Screen
                        composable("document_result") {
                            DocumentResultScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onSavedSuccessfully = {
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 5. Obligation Detail Screen
                        composable(
                            route = "obligation_detail/{obligationId}",
                            arguments = listOf(
                                navArgument("obligationId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val obligationId = backStackEntry.arguments?.getLong("obligationId") ?: 0L
                            ObligationDetailScreen(
                                viewModel = viewModel,
                                obligationId = obligationId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // 6. Sync Screen
                        composable("sync") {
                            SyncScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
