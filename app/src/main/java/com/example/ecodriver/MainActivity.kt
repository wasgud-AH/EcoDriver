package com.example.ecodriver


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.ecodriver.ui.components.EcoDriverTopBar
import com.example.ecodriver.ui.navigation.EcoDriverNavHost
import com.example.ecodriver.ui.theme.EcoDriverTheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.ecodriver.data.runBluetoothObdSession
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    private val btPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d("MainActivity", "BLUETOOTH_CONNECT granted=$granted")
        if (granted) startObdSession()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EcoDriverTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.primary,
                    topBar = { EcoDriverTopBar() }
                ) { innerPadding ->
                    EcoDriverNavHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }

        if (Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            startObdSession()
        }
    }

    private fun startObdSession() {
        lifecycleScope.launch {
            val ok = runBluetoothObdSession()
            Log.d("MainActivity", if (ok) "OBD session OK" else "OBD session failed")
        }
    }
}
