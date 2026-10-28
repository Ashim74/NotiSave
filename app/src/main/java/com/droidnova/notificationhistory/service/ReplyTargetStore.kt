package com.droidnova.notificationhistory.service

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.os.Build
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** The live "Reply" action of a chat notification; valid only while the notification is shown. */
class ReplyTarget(
    val packageName: String,
    val notificationKey: String,
    val postTime: Long,
    val conversationName: String,
    val actionIntent: PendingIntent,
    val remoteInputs: Array<RemoteInput>
)

/**
 * Latest reply target per conversationKey. PendingIntents can't be persisted, so this lives in
 * memory only and the listener rebuilds it from the shown notifications after a restart.
 */
object ReplyTargetStore {

    private val registry = ReplyTargetRegistry<ReplyTarget>(
        notificationKeyOf = { it.notificationKey },
        postTimeOf = { it.postTime }
    )

    fun put(conversationKey: String, target: ReplyTarget) = registry.put(conversationKey, target)

    fun get(conversationKey: String): ReplyTarget? = registry.get(conversationKey)

    fun observe(conversationKey: String): Flow<ReplyTarget?> = registry.observe(conversationKey)

    /** Drops targets created by the removed notification; a newer post's target is kept. */
    fun forgetNotification(notificationKey: String) = registry.forgetNotification(notificationKey)

    /** Drops [conversationKey]'s target if it still came from [notificationKey]. */
    fun forget(conversationKey: String, notificationKey: String) =
        registry.forget(conversationKey, notificationKey)

    fun clear() = registry.clear()

    /** Builds the reply target of [sbn], or null when it has no free-form reply action. */
    internal fun targetFor(sbn: StatusBarNotification, conversationName: String): ReplyTarget? {
        val key = sbn.key ?: return null
        val actions = sbn.notification.actions ?: return null
        val index = chooseReplyActionIndex(actions.map { it.toCandidate() }) ?: return null
        val action = actions[index]
        val intent = action.actionIntent ?: return null
        val inputs = action.remoteInputs ?: return null
        return ReplyTarget(
            packageName = sbn.packageName,
            notificationKey = key,
            postTime = sbn.postTime,
            conversationName = conversationName,
            actionIntent = intent,
            remoteInputs = inputs
        )
    }

    /** Cheap pre-check so conversation detection only runs for notifications that can be replied to. */
    internal fun hasReplyAction(notification: Notification): Boolean {
        val actions = notification.actions ?: return false
        return chooseReplyActionIndex(actions.map { it.toCandidate() }) != null
    }

    private fun Notification.Action.toCandidate() = ReplyActionCandidate(
        hasFreeFormInput = remoteInputs?.any { it.allowFreeFormInput } == true && actionIntent != null,
        isSemanticReply = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            semanticAction == Notification.Action.SEMANTIC_ACTION_REPLY
    )
}

/** What [chooseReplyActionIndex] needs to know about one notification action. */
internal data class ReplyActionCandidate(
    val hasFreeFormInput: Boolean,
    val isSemanticReply: Boolean
)

/**
 * The action a typed reply goes to: one that accepts free-form text, preferring the one the
 * app marked as its reply action. Null when no action takes free-form text.
 */
internal fun chooseReplyActionIndex(candidates: List<ReplyActionCandidate>): Int? {
    val freeForm = candidates.withIndex().filter { it.value.hasFreeFormInput }
    return (freeForm.firstOrNull { it.value.isSemanticReply } ?: freeForm.firstOrNull())?.index
}

/** Android-free bookkeeping behind [ReplyTargetStore], so its rules are JVM-testable. */
internal class ReplyTargetRegistry<T : Any>(
    private val notificationKeyOf: (T) -> String,
    private val postTimeOf: (T) -> Long
) {
    private val targets = MutableStateFlow<Map<String, T>>(emptyMap())

    /** Stores [target] unless the conversation already holds one from a newer post. */
    fun put(conversationKey: String, target: T) {
        targets.update { current ->
            val existing = current[conversationKey]
            if (existing != null && postTimeOf(existing) > postTimeOf(target)) current
            else current + (conversationKey to target)
        }
    }

    fun get(conversationKey: String): T? = targets.value[conversationKey]

    fun observe(conversationKey: String): Flow<T?> =
        targets.map { it[conversationKey] }.distinctUntilChanged()

    fun forgetNotification(notificationKey: String) {
        targets.update { current ->
            current.filterValues { notificationKeyOf(it) != notificationKey }
        }
    }

    fun forget(conversationKey: String, notificationKey: String) {
        targets.update { current ->
            val existing = current[conversationKey]
            if (existing != null && notificationKeyOf(existing) == notificationKey) {
                current - conversationKey
            } else {
                current
            }
        }
    }

    fun clear() {
        targets.value = emptyMap()
    }
}
