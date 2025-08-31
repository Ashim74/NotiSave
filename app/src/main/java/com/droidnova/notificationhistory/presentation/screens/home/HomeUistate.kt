package com.droidnova.notificationhistory.presentation.screens.home

data class HomeUiState(
    var userToggleTracking: Boolean = false,//switch on or off track
    //val listenerGranted: Boolean = false,
) {
  //  val isTrackingEnabled: Boolean get() = userToggleTracking && listenerGranted
}

