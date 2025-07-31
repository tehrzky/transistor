package org.y20k.transistor.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.y20k.transistor.R
import org.y20k.transistor.core.Collection
import org.y20k.transistor.sync.GoogleDriveSyncService
import org.y20k.transistor.sync.GoogleSignInHelper

class SyncSettingsActivity : AppCompatActivity() {
    
    private lateinit var googleSignInHelper: GoogleSignInHelper
    private lateinit var syncService: GoogleDriveSyncService
    
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            initializeSyncService()
            updateSignInStatus()
        } else {
            Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create simple layout programmatically
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 50, 50, 50)
        
        val title = TextView(this)
        title.text = "Google Drive Sync"
        title.textSize = 24f
        layout.addView(title)
        
        val signInBtn = Button(this)
        signInBtn.text = "Sign in with Google"
        signInBtn.id = View.generateViewId()
        layout.addView(signInBtn)
        
        val backupBtn = Button(this)
        backupBtn.text = "Backup Now"
        backupBtn.id = View.generateViewId()
        layout.addView(backupBtn)
        
        val restoreBtn = Button(this)
        restoreBtn.text = "Restore Backup"
        restoreBtn.id = View.generateViewId()
        layout.addView(restoreBtn)
        
        val signOutBtn = Button(this)
        signOutBtn.text = "Sign Out"
        signOutBtn.id = View.generateViewId()
        layout.addView(signOutBtn)
        
        setContentView(layout)
        
        googleSignInHelper = GoogleSignInHelper(this)
        syncService = GoogleDriveSyncService(this)
        
        signInBtn.setOnClickListener { signInToGoogle() }
        backupBtn.setOnClickListener { performBackup() }
        restoreBtn.setOnClickListener { performRestore() }
        signOutBtn.setOnClickListener { signOutFromGoogle() }
        
        updateSignInStatus()
    }
    
    private fun updateSignInStatus() {
        // Update UI based on sign-in status
    }
    
    private fun signInToGoogle() {
        val signInIntent = googleSignInHelper.getSignInIntent()
        signInLauncher.launch(signInIntent)
    }
    
    private fun signOutFromGoogle() {
        googleSignInHelper.signOut {
            updateSignInStatus()
            Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun initializeSyncService() {
        lifecycleScope.launch {
            syncService.initialize()
        }
    }
    
    private fun performBackup() {
        lifecycleScope.launch {
            try {
                val collection = Collection.getInstance()
                val stations = collection.stations
                
                val success = syncService.uploadStations(stations)
                
                if (success) {
                    Toast.makeText(this@SyncSettingsActivity, "Backup completed!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SyncSettingsActivity, "Backup failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SyncSettingsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun performRestore() {
        lifecycleScope.launch {
            try {
                val stations = syncService.downloadStations()
                
                if (stations != null && stations.isNotEmpty()) {
                    Toast.makeText(this@SyncSettingsActivity, "Restored ${stations.size} stations", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SyncSettingsActivity, "No backup found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SyncSettingsActivity, "Restore error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
