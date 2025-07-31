package org.y20k.transistor.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.y20k.transistor.core.Station
import java.io.ByteArrayOutputStream
import java.util.*

class GoogleDriveSyncService(private val context: Context) {
    
    companion object {
        private const val TAG = "GoogleDriveSync"
        private const val TRANSISTOR_FOLDER = "Transistor_Backup"
        private const val STATIONS_FILE = "stations.json"
        private const val APPLICATION_NAME = "Transistor Radio App"
    }
    
    private var driveService: Drive? = null
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account == null) {
                Log.e(TAG, "No signed-in Google account found")
                return@withContext false
            }
            
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = account.account
            
            driveService = Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName(APPLICATION_NAME).build()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Drive service", e)
            false
        }
    }
    
    private suspend fun getOrCreateBackupFolder(): String? = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext null
            
            val result = service.files().list()
                .setQ("name='$TRANSISTOR_FOLDER' and mimeType='application/vnd.google-apps.folder' and trashed=false")
                .setSpaces("drive")
                .execute()
            
            if (result.files.isNotEmpty()) {
                return@withContext result.files[0].id
            }
            
            val folderMetadata = File()
            folderMetadata.name = TRANSISTOR_FOLDER
            folderMetadata.mimeType = "application/vnd.google-apps.folder"
            
            val folder = service.files().create(folderMetadata)
                .setFields("id")
                .execute()
            
            folder.id
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create/get backup folder", e)
            null
        }
    }
    
    suspend fun uploadStations(stations: List<Station>): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext false
            val folderId = getOrCreateBackupFolder() ?: return@withContext false
            
            val gson = Gson()
            val stationsJson = gson.toJson(stations)
            
            val existingFiles = service.files().list()
                .setQ("name='$STATIONS_FILE' and parents in '$folderId' and trashed=false")
                .execute()
            
            val fileMetadata = File()
            fileMetadata.name = STATIONS_FILE
            fileMetadata.parents = listOf(folderId)
            
            val mediaContent = com.google.api.client.http.ByteArrayContent(
                "application/json",
                stationsJson.toByteArray()
            )
            
            if (existingFiles.files.isNotEmpty()) {
                val fileId = existingFiles.files[0].id
                service.files().update(fileId, fileMetadata, mediaContent).execute()
            } else {
                service.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()
            }
            
            Log.i(TAG, "Stations uploaded successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload stations", e)
            false
        }
    }
    
    suspend fun downloadStations(): List<Station>? = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext null
            val folderId = getOrCreateBackupFolder() ?: return@withContext null
            
            val result = service.files().list()
                .setQ("name='$STATIONS_FILE' and parents in '$folderId' and trashed=false")
                .execute()
            
            if (result.files.isEmpty()) {
                Log.i(TAG, "No backup file found")
                return@withContext emptyList()
            }
            
            val fileId = result.files[0].id
            val outputStream = ByteArrayOutputStream()
            
            service.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            
            val jsonContent = outputStream.toString("UTF-8")
            val gson = Gson()
            val listType = object : TypeToken<List<Station>>() {}.type
            val stations: List<Station> = gson.fromJson(jsonContent, listType)
            
            Log.i(TAG, "Downloaded ${stations.size} stations")
            stations
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download stations", e)
            null
        }
    }
}
