package com.droidnova.notificationhistory.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

import com.droidnova.notificationhistory.presentation.screens.home.HomeScreen

@Composable
fun AppNavGraph(){
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route)
    {
        composable(Screen.Home.route) {
            HomeScreen()
        }
    }
}