package com.mamunsproject.chathelperfinal

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class TextExtractionService : AccessibilityService() {

    companion object {
        private var instance: TextExtractionService? = null
        fun getInstance(): TextExtractionService? = instance

        // Constants for message alignment detection
        private const val ALIGNMENT_THRESHOLD = 0.45 // 45% of screen width
        private const val TIMESTAMP_PATTERN = "\\d{1,2}:\\d{2}( ?[AP]M)?"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    fun extractMessengerChat(): String {
        try {
            Thread.sleep(100) // Ensure UI is stable
            val rootNode = rootInActiveWindow ?: return "No active window found"

            return try {
                val screenWidth = resources.displayMetrics.widthPixels
                val messages = extractChatMessages(rootNode, screenWidth)
                formatMessengerChat(messages)
            } catch (e: Exception) {
                "Error extracting chat: ${e.message}"
            } finally {
                rootNode.recycle()
            }
        } catch (e: Exception) {
            return "Error accessing screen content: ${e.message}"
        }
    }

    private fun extractChatMessages(node: AccessibilityNodeInfo, screenWidth: Int): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()
        val bounds = Rect()

        try {
            node.getBoundsInScreen(bounds)

            // Skip nodes that are too small or too large
            if (bounds.width() in 20..screenWidth && bounds.height() in 20..200) {
                val nodeText = node.text?.toString() ?: ""
                val nodeDesc = node.contentDescription?.toString() ?: ""

                if (nodeText.isNotBlank() && !isTimestamp(nodeText)) {
                    val isRightAligned = isMessageRightAligned(bounds, screenWidth)
                    val timestamp = findTimestamp(node) ?: ""

                    val sender = if (isRightAligned) "Person 2" else "Person 1"

                    messages.add(ChatMessage(
                        text = nodeText,
                        sender = sender,
                        timestamp = timestamp,
                        isRightAligned = isRightAligned,
                        bounds = Rect(bounds)
                    ))
                }
            }

            // Process child nodes
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { childNode ->
                    messages.addAll(extractChatMessages(childNode, screenWidth))
                    childNode.recycle()
                }
            }
        } catch (e: Exception) {
            // Skip problematic nodes
        }

        return messages.sortedBy { it.bounds.top }
    }

    private fun isMessageRightAligned(bounds: Rect, screenWidth: Int): Boolean {
        val messageCenter = bounds.centerX()
        val screenThreshold = screenWidth * ALIGNMENT_THRESHOLD
        return messageCenter > screenThreshold
    }

    private fun findTimestamp(node: AccessibilityNodeInfo): String? {
        // Search for timestamp in nearby nodes
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { childNode ->
                val text = childNode.text?.toString() ?: ""
                if (isTimestamp(text)) {
                    return text
                }
                childNode.recycle()
            }
        }
        return null
    }

    private fun isTimestamp(text: String): Boolean {
        return text.matches(Regex(TIMESTAMP_PATTERN))
    }

    private fun formatMessengerChat(messages: List<ChatMessage>): String {
        if (messages.isEmpty()) return "No messages found"

        val stringBuilder = StringBuilder()
        var currentSender = ""

        messages.forEach { message ->
            // Add line break between different senders
            if (currentSender != message.sender && currentSender.isNotEmpty()) {
                stringBuilder.append("\n")
            }

            // Update current sender
            currentSender = message.sender

            // Format the message
            val timestamp = if (message.timestamp.isNotEmpty()) " [${message.timestamp}]" else ""

            // Add sender name only when it changes
            if (stringBuilder.isEmpty() || messages.getOrNull(messages.indexOf(message) - 1)?.sender != message.sender) {
                stringBuilder.append("${message.sender}:\n")
            }

            // Indent message and add timestamp
            stringBuilder.append("  ${message.text}$timestamp\n")
        }

        return stringBuilder.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}
