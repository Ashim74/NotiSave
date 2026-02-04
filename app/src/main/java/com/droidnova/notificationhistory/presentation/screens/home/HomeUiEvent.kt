package com.droidnova.notificationhistory.presentation.screens.home

sealed class HomeUiEvent {
    object ShowPermissionRequiredMessage : HomeUiEvent()
    object NavigateToSelectApps : HomeUiEvent()
}
