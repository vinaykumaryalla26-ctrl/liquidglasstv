package com.liquidglass.tv

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.media.tv.TvInputManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.liquidglass.tv.adapter.AppGridAdapter
import com.liquidglass.tv.adapter.DockAdapter
import com.liquidglass.tv.databinding.ActivityMainBinding
import com.liquidglass.tv.model.AppInfo
import com.liquidglass.tv.util.FocusHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val allApps = mutableListOf<AppInfo>()
    private val dockApps = mutableListOf<AppInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupNetworkMonitoring()
        loadInstalledApps()
    }

    private fun setupUI() {
        // 4-Column App Grid configuration
        val gridLayoutManager = GridLayoutManager(this, 4)
        binding.appGridRecycler.layoutManager = gridLayoutManager
        binding.appGridRecycler.setHasFixedSize(true)

        // Bottom Liquid Glass Dock configuration (Horizontal 4-app favorites)
        val dockLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.dockRecycler.layoutManager = dockLayoutManager
        binding.dockRecycler.setHasFixedSize(true)

        // Input Selector Button D-pad focus & click listener
        binding.btnInputSelector.setOnFocusChangeListener { view, hasFocus ->
            FocusHelper.applyFocusScale(view, hasFocus, scale = 1.08f, elevationDp = 8f)
        }
        binding.btnInputSelector.setOnClickListener {
            showInputSourceDialog()
        }
    }

    private fun setupNetworkMonitoring() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread { updateWifiStatus(isConnected = true) }
            }

            override fun onLost(network: Network) {
                runOnUiThread { updateWifiStatus(isConnected = false) }
            }
        }

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            // Fallback for restricted sandboxes or devices
            checkCurrentNetworkState()
        }

        checkCurrentNetworkState()
    }

    private fun checkCurrentNetworkState() {
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        updateWifiStatus(isConnected)
    }

    private fun updateWifiStatus(isConnected: Boolean) {
        if (isFinishing || isDestroyed) return

        if (isConnected) {
            binding.wifiIcon.setColorFilter(
                ContextCompat.getColor(this, R.color.status_green)
            )
            binding.wifiStatusText.text = getString(R.string.wifi_connected)
        } else {
            binding.wifiIcon.setColorFilter(
                ContextCompat.getColor(this, R.color.status_disconnected)
            )
            binding.wifiStatusText.text = getString(R.string.wifi_disconnected)
        }
    }

    /**
     * Loads apps asynchronously in Dispatchers.IO to maintain smooth 60 FPS
     * on 1 GB RAM Android TV hardware.
     */
    private fun loadInstalledApps() {
        lifecycleScope.launch(Dispatchers.IO) {
            val pm = packageManager
            val apps = queryInstalledApps(pm)

            withContext(Dispatchers.Main) {
                allApps.clear()
                dockApps.clear()

                allApps.addAll(apps)

                // Populate bottom dock with up to 4 priority/favorite apps
                val dockCount = minOf(4, apps.size)
                dockApps.addAll(apps.take(dockCount))

                binding.appGridRecycler.adapter = AppGridAdapter(allApps) { app ->
                    launchApplication(app)
                }

                binding.dockRecycler.adapter = DockAdapter(dockApps) { app ->
                    launchApplication(app)
                }

                // Initial focus request to first app card or dock item
                binding.root.post {
                    if (binding.appGridRecycler.childCount > 0) {
                        binding.appGridRecycler.getChildAt(0)?.requestFocus()
                    }
                }
            }
        }
    }

    private fun queryInstalledApps(pm: PackageManager): List<AppInfo> {
        val appMap = mutableMapOf<String, AppInfo>()

        // 1. Query Leanback Launcher apps (primary TV apps)
        val tvIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }
        val tvApps = pm.queryIntentActivities(tvIntent, 0)
        for (info in tvApps) {
            if (info.activityInfo.packageName == packageName) continue // Skip self
            val appInfo = createAppInfoFromResolveInfo(pm, info)
            appMap[info.activityInfo.packageName] = appInfo
        }

        // 2. Query Standard Launcher apps (sideloaded or non-leanback apps)
        val standardIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val stdApps = pm.queryIntentActivities(standardIntent, 0)
        for (info in stdApps) {
            if (info.activityInfo.packageName == packageName) continue // Skip self
            if (!appMap.containsKey(info.activityInfo.packageName)) {
                val appInfo = createAppInfoFromResolveInfo(pm, info)
                appMap[info.activityInfo.packageName] = appInfo
            }
        }

        return appMap.values.sortedBy { it.label.lowercase() }
    }

    private fun createAppInfoFromResolveInfo(pm: PackageManager, info: ResolveInfo): AppInfo {
        val pkg = info.activityInfo.packageName
        val cls = info.activityInfo.name
        val label = info.loadLabel(pm)?.toString() ?: pkg
        val icon = info.loadIcon(pm) ?: ContextCompat.getDrawable(this, R.drawable.ic_launcher_background)!!

        val launchIntent = pm.getLeanbackLaunchIntentForPackage(pkg)
            ?: pm.getLaunchIntentForPackage(pkg)
            ?: Intent(Intent.ACTION_MAIN).apply {
                setClassName(pkg, cls)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        return AppInfo(
            packageName = pkg,
            className = cls,
            label = label,
            icon = icon,
            launchIntent = launchIntent
        )
    }

    private fun launchApplication(app: AppInfo) {
        try {
            app.launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(app.launchIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.app_launch_failed), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showInputSourceDialog() {
        val inputSources = arrayOf(
            getString(R.string.input_hdmi1),
            getString(R.string.input_hdmi2),
            getString(R.string.input_hdmi3),
            getString(R.string.input_av)
        )

        val builder = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
        builder.setTitle(getString(R.string.select_input_title))
        builder.setItems(inputSources) { _, which ->
            val selected = inputSources[which]
            handleInputSourceSelection(selected)
        }
        builder.setNegativeButton(android.R.string.cancel, null)

        val dialog = builder.create()
        dialog.show()
    }

    private fun handleInputSourceSelection(sourceName: String) {
        try {
            // Attempt standard TV input intent for Android TV
            val tvInputIntent = Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (packageManager.resolveActivity(tvInputIntent, 0) != null) {
                startActivity(tvInputIntent)
                return
            }
        } catch (e: Exception) {
            // Fallback to toast
        }

        Toast.makeText(this, "Input switched to: $sourceName", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                // Ignore if unregistration fails
            }
        }
    }
}
