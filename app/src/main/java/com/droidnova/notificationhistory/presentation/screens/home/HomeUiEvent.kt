package com.droidnova.notificationhistory.presentation.screens.home

sealed class HomeUiEvent {
    object OpenNotificationAccessSettings : HomeUiEvent()
    object DoWorkAfterEnabled : HomeUiEvent()
    object NavigateToSelectApps : HomeUiEvent()
}
