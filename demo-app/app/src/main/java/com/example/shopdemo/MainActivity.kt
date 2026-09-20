package com.example.shopdemo

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            // Defect in Version 1: Rotating to Landscape orientation throws unhandled IllegalStateException
            if (isLandscape && VersionConfig.IS_BUGGY) {
                throw IllegalStateException("ShopDemo V1 Defect: Unhandled landscape configuration buffer exception")
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

                        Spacer(modifier = Modifier.height(12.dp))

                        // App Subtitle / Context Info
                        Text(
                            text = "Sample E-Commerce App Under Test",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Product List Representation
                        if (isLandscape) {
                            // Version 2 Landscape Grid view
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(sampleProducts) { product ->
                                    ProductCard(product = product)
                                }
                            }
                        } else {
                            // Portrait List View
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(sampleProducts) { product ->
                                    ProductCard(product = product)
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
fun ProductCard(product: ProductItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
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
