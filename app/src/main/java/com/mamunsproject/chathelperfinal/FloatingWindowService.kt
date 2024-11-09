package com.mamunsproject.chathelperfinal


import android.app.Service
import android.content.ClipData
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast


class FloatingWindowService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var editText: EditText
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupFloatingWindow()
    }


    private fun setupFloatingWindow() {
        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window, null)
        editText = floatingView.findViewById(R.id.editText)

        // Set EditText properties for better chat display
        editText.apply {
            setTextIsSelectable(true)
            setHorizontallyScrolling(false)
            maxLines = Int.MAX_VALUE
            background = getDrawable(R.drawable.chat_edit_text_background)
        }

        setupButtons()
        setupTouchListener()
        setupEditTextFocus()

        windowManager.addView(floatingView, layoutParams)
    }

    private fun setupButtons() {
        floatingView.findViewById<ImageButton>(R.id.extractButton).setOnClickListener {
            TextExtractionService.getInstance()?.let { service ->
                layoutParams?.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                layoutParams?.let { windowManager.updateViewLayout(floatingView, it) }

                Handler(Looper.getMainLooper()).postDelayed({
                    val extractedText = service.extractMessengerChat()  // Using the new method
                    editText.setText(extractedText)

                    layoutParams?.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    layoutParams?.let { windowManager.updateViewLayout(floatingView, it) }
                }, 100)
            } ?: run {
                Toast.makeText(this, "Please enable accessibility service", Toast.LENGTH_LONG)
                    .show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
        }
    }

    private fun setupTouchListener() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMoving = false

        floatingView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isMoving = true
                    initialX = layoutParams?.x ?: 0
                    initialY = layoutParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isMoving && layoutParams != null) {
                        layoutParams?.x = initialX + (event.rawX - initialTouchX).toInt()
                        layoutParams?.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, layoutParams)
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    isMoving = false
                    true
                }

                else -> false
            }
        }
    }

    private fun setupEditTextFocus() {
        editText.setOnClickListener {
            layoutParams?.flags = layoutParams?.flags?.and(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            ) ?: 0
            layoutParams?.let { params -> windowManager.updateViewLayout(floatingView, params) }
            editText.requestFocus()
        }

        editText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                layoutParams?.flags = layoutParams?.flags?.or(
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                ) ?: 0
                layoutParams?.let { params -> windowManager.updateViewLayout(floatingView, params) }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }
}

