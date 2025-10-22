package com.droidnova.notificationhistory.presentation.screens.setting

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(navController: NavHostController, packageName: String) {

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(" ${packageName}  Filter") })
            Log.e("MyTag","packagename  ${packageName}  ")
        }

    ) {innerPadding->
        SettingScreenContent(
            innerPaddingValues = innerPadding
        )
    }
}

@Composable
fun SettingScreenContent(innerPaddingValues: PaddingValues) {
    Log.e("MyTag","packagename ")

}