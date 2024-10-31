package com.risyan.quickshutdownphone.feature.widget

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import com.risyan.quickshutdownphone.R
import com.risyan.quickshutdownphone.databinding.CustomBonkDialogBinding


class CustomBonkDialog(
    context: Context,
    private val title: String,
    private val message: String,
    private val imageResource: Int = R.drawable.lock_meme,
    val onDismiss: () -> Unit = {}
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the view using view binding
        CustomBonkDialogBinding.inflate(
            LayoutInflater.from(context)
        ).apply {
            setContentView(root)
            dialogTitle.text = title
            dialogMessage.text = message
            dialogImage.setImageResource(imageResource)
            dialogButton.setOnClickListener {
                onDismiss()
                dismiss()
            }
        }

        // Set dialog background to be fully transparent
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Ensure layout params are applied
        val params = window?.attributes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params?.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            params?.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        window?.attributes = params
    }
}
