package dev.thingscompanion.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Receives what another app shares and hands it to [MainActivity], as the widget does: brought to
 * the front if it is already running, it still gets the shared text (a share started in a new task
 * would otherwise only bring the app's task forward and drop it).
 */
class ShareActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val shared = intent
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_SEND
                type = shared.type
                // Only what a share carries: never the widget's extras another app could slip in.
                putExtra(Intent.EXTRA_SUBJECT, shared.getStringExtra(Intent.EXTRA_SUBJECT))
                putExtra(Intent.EXTRA_TEXT, shared.getCharSequenceExtra(Intent.EXTRA_TEXT))
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
        )
        finish()
    }
}
