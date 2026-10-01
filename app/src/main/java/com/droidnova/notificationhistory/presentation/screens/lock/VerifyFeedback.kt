package com.droidnova.notificationhistory.presentation.screens.lock

import com.droidnova.notificationhistory.core.lock.AppLockRepository
import com.droidnova.notificationhistory.core.lock.VerifyResult
import com.droidnova.notificationhistory.utils.Analytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What to show under a PIN / password / recovery-code field after a wrong entry. */
data class VerifyFeedback(
    val attemptsLeft: Int? = null,
    val lockoutRemainingMs: Long = 0L,
    val isChecking: Boolean = false,
    /** Bumped on every rejected entry; the PIN pad shakes and clears when it changes. */
    val rejections: Int = 0
) {
    val isLockedOut: Boolean get() = lockoutRemainingMs > 0L

    /** Key for [PinEntry]'s errorKey: null until the first rejection. */
    val errorKey: Int? get() = rejections.takeIf { it > 0 }
}

/** Runs verifications one at a time and counts a lockout down to zero. */
class VerifyFeedbackHolder(
    private val scope: CoroutineScope,
    private val repository: AppLockRepository
) {
    private val _feedback = MutableStateFlow(VerifyFeedback())
    val feedback: StateFlow<VerifyFeedback> = _feedback.asStateFlow()
    private var ticker: Job? = null

    /** Picks up a lockout that is still running (e.g. the app was reopened mid-lockout). */
    fun refreshLockout() {
        scope.launch {
            val remaining = repository.lockoutRemaining(repository.current())
            if (remaining > 0L) startTicker()
        }
    }

    fun verify(check: suspend () -> VerifyResult, onSuccess: suspend () -> Unit) {
        if (_feedback.value.isChecking || _feedback.value.isLockedOut) return
        _feedback.update { it.copy(isChecking = true) }
        scope.launch {
            val rejections = _feedback.value.rejections + 1
            when (val result = check()) {
                VerifyResult.Success -> {
                    _feedback.value = VerifyFeedback()
                    onSuccess()
                }
                is VerifyResult.Wrong -> _feedback.value = VerifyFeedback(
                    attemptsLeft = result.attemptsLeft,
                    rejections = rejections
                )
                is VerifyResult.LockedOut -> {
                    _feedback.value = VerifyFeedback(
                        lockoutRemainingMs = result.remainingMs,
                        rejections = rejections
                    )
                    Analytics.log(Analytics.APP_LOCK_LOCKOUT)
                    startTicker()
                }
            }
        }
    }

    fun clear() {
        ticker?.cancel()
        _feedback.value = VerifyFeedback()
        refreshLockout()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                val remaining = repository.lockoutRemaining(repository.current())
                _feedback.update { it.copy(lockoutRemainingMs = remaining, isChecking = false) }
                if (remaining <= 0L) break
                delay(TICK_MS)
            }
        }
    }

    private companion object {
        const val TICK_MS = 1_000L
    }
}
