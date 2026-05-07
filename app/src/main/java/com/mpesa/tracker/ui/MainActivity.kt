package com.mpesa.tracker.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.mpesa.tracker.MpesaTrackerApp
import com.mpesa.tracker.R
import com.mpesa.tracker.databinding.ActivityMainBinding
import com.mpesa.tracker.sms.SmsScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Required permissions
    private val requiredPermissions = mutableListOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val smsGranted = results[Manifest.permission.READ_SMS] == true
        if (smsGranted) {
            scanHistoricalSms()
        } else {
            Toast.makeText(this, "SMS permission required for auto-tracking", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Navigation
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)

        // Request permissions
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            scanHistoricalSms()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    private fun scanHistoricalSms() {
        val prefs = getSharedPreferences("mpesa_prefs", MODE_PRIVATE)
        // Check for v2 scan to ensure the new expanded logic runs even if they already scanned with v1
        val alreadyScanned = prefs.getBoolean("inbox_scanned_v2", false)
        if (alreadyScanned) return

        CoroutineScope(Dispatchers.IO).launch {
            val count = SmsScanner.scanInbox(this@MainActivity)
            withContext(Dispatchers.Main) {
                if (count > 0) {
                    Toast.makeText(
                        this@MainActivity,
                        "Imported $count historical M-Pesa transactions!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            // Mark v2 as scanned
            prefs.edit().putBoolean("inbox_scanned_v2", true).apply()
        }
    }
}
