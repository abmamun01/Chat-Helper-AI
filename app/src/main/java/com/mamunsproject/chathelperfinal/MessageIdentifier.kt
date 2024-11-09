package com.mamunsproject.chathelperfinal

import android.view.accessibility.AccessibilityNodeInfo

class MessageIdentifier {
    companion object {
        private val USER_MESSAGE_IDENTIFIERS = setOf(
            "user_message", "outgoing_message", "sent_message",
            "message_outgoing", "right_message", "self_message",
            "message_out", "msg_out", "bubble_out"
        )

        private val OTHER_MESSAGE_IDENTIFIERS = setOf(
            "other_message", "incoming_message", "received_message",
            "message_incoming", "left_message", "friend_message",
            "message_in", "msg_in", "bubble_in"
        )
    }

    fun identifyMessageType(node: AccessibilityNodeInfo?): Boolean? {
        if (node == null) return null
        return when {
            isUserMessage(node) -> true
            isOtherMessage(node) -> false
            else -> null
        }
    }

    private fun isUserMessage(node: AccessibilityNodeInfo): Boolean {
        return matchesIdentifiers(node, USER_MESSAGE_IDENTIFIERS)
    }

    private fun isOtherMessage(node: AccessibilityNodeInfo): Boolean {
        return matchesIdentifiers(node, OTHER_MESSAGE_IDENTIFIERS)
    }

    private fun matchesIdentifiers(node: AccessibilityNodeInfo, identifiers: Set<String>): Boolean {
        val viewId = node.viewIdResourceName ?: ""
        val className = node.className?.toString() ?: ""
        val description = node.contentDescription?.toString() ?: ""

        return identifiers.any { identifier ->
            viewId.contains(identifier, ignoreCase = true) ||
                    className.contains(identifier, ignoreCase = true) ||
                    description.contains(identifier, ignoreCase = true)
        }
    }
}