package com.example.shopdemo

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ProductItem(
    val id: String,
    val name: String,
    val category: String,
    val price: String,
    val rating: String
)

class MainActivity : ComponentActivity() {

    private val sampleProducts = listOf(
        ProductItem("P-101", "Wireless Noise-Cancelling Headphones", "Audio", "$199.99", "4.8 ★"),
        ProductItem("P-102", "Smart Fitness Watch Ultra", "Wearables", "$299.00", "4.9 ★"),
        ProductItem("P-103", "Ergonomic Mechanical Keyboard", "Accessories", "$149.50", "4.7 ★"),
        ProductItem("P-104", "4K Ultra HD Streaming Webcam", "Video", "$119.00", "4.6 ★"),
        ProductItem("P-105", "Compact Wireless Charger Dock", "Power", "$39.99", "4.5 ★")
    )

    companion object {
        fun isDeviceCharging(context: Context): Boolean {
            return try {
                val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                val batteryStatus: Intent? = context.registerReceiver(null, intentFilter)
                val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            } catch (e: Exception) {
                true // Fallback for testing environment
            }
        }

        fun isWifiConnected(context: Context): Boolean {
            return try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return true
                val activeNet = cm.activeNetwork ?: return true
                val caps = cm.getNetworkCapabilities(activeNet) ?: return true
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            } catch (e: Exception) {
                true // Fallback for testing environment
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register uncaught exception handler to send crash details to BugTrace collector
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                val intent = Intent("com.bugtrace.app.ACTION_APP_CRASH").apply {
                    setPackage("com.bugtrace.app")
                    putExtra("target_package", packageName)
                    putExtra("exit_reason", "UNHANDLED_EXCEPTION")
                    putExtra("exit_description", throwable.message ?: throwable.toString())
                    putExtra("exception_class", throwable.javaClass.name)
                    putExtra("is_charging", isDeviceCharging(this@MainActivity))
                    putExtra("network_state", if (isWifiConnected(this@MainActivity)) "Wi-Fi" else "Other")
                    putExtra("last_orientation", if (isLandscape) "LANDSCAPE" else "PORTRAIT")
                }
                sendBroadcast(intent)
            } catch (e: Exception) {
                // Ignore failure sending broadcast
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        setContent {
            val configuration = LocalConfiguration.current
            val context = LocalContext.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val isCharging = remember(isLandscape) { isDeviceCharging(context) }
            val isWifi = remember(isLandscape) { isWifiConnected(context) }

            // Function to handle product item click
            val handleProductClick: (ProductItem) -> Unit = { product ->
                if (VersionConfig.IS_BUGGY) {
                    val currentCharging = isDeviceCharging(context)
                    val currentWifi = isWifiConnected(context)
                    // Multi-condition defect in V1: Crashes ONLY when LANDSCAPE + CHARGING + WI-FI conditions are satisfied
                    if (isLandscape && currentCharging && currentWifi) {
                        throw IllegalStateException("ShopDemo V1 Defect: Unhandled landscape charging buffer exception during Wi-Fi data sync for ${product.name}")
                    }
                }
            }

            // Also check on configuration layout change if all 3 conditions are active
            if (isLandscape && isCharging && isWifi && VersionConfig.IS_BUGGY) {
                throw IllegalStateException("ShopDemo V1 Defect: Unhandled landscape charging configuration buffer exception during Wi-Fi data sync")
            }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Header Banner
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (VersionConfig.IS_BUGGY) Color(0xFF7F1D1D) else Color(0xFF065F46))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = "ShopDemo App",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SHOPDEMO",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Text(
                                text = VersionConfig.VERSION_NAME.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (VersionConfig.IS_BUGGY) Color(0xFFFCA5A5) else Color(0xFFA7F3D0)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Live Condition Indicators Banner
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ConditionBadge("WI-FI: ${if (isWifi) "ON" else "OFF"}", isWifi)
                            ConditionBadge("CHARGING: ${if (isCharging) "YES" else "NO"}", isCharging)
                            ConditionBadge("ORIENT: ${if (isLandscape) "LANDSCAPE" else "PORTRAIT"}", isLandscape)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Sample E-Commerce App Under Test • Tap product to trigger test action",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Product List Representation
                        if (isLandscape) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(sampleProducts) { product ->
                                    ProductCard(product = product, onClick = { handleProductClick(product) })
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(sampleProducts) { product ->
                                    ProductCard(product = product, onClick = { handleProductClick(product) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConditionBadge(label: String, isActive: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) Color(0xFF1E293B) else Color(0xFF0F172A))
            .border(1.dp, if (isActive) Color(0xFF38BDF8) else Color(0xFF334155), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = if (isActive) Color(0xFF38BDF8) else Color(0xFF64748B),
            fontSize = 9.sp
        )
    }
}

@Composable
fun ProductCard(product: ProductItem, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = product.category.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF38BDF8),
                    fontSize = 10.sp
                )

                Text(
                    text = product.rating,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFFBBF24),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = product.price,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF34D399),
                fontSize = 13.sp
            )
        }
    }
}
