package com.droidnova.notificationhistory.presentation.screens.home

sealed interface HomeUiEvent {
    data object OpenNotificationAccessSettings : HomeUiEvent
    data object DoWorkAfterEnabled : HomeUiEvent     // navigate/start tracking/etc.
}