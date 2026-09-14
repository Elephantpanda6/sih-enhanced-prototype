package com.example.sihscrap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecyclerDashboardScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recycler Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Market Rates Section
            item {
                SectionHeader("Live Market Rates", Icons.Rounded.TrendingUp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RateCard(modifier = Modifier.weight(1f), title = "Copper", price = "₹450/kg", trend = "+₹12")
                    RateCard(modifier = Modifier.weight(1f), title = "Aluminium", price = "₹120/kg", trend = "-₹3")
                    RateCard(modifier = Modifier.weight(1f), title = "E-Waste", price = "₹80/kg", trend = "+₹5")
                }
            }

            // Pending Orders Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("Pending Inbound Orders", Icons.Rounded.ListAlt)
                OrderCard(id = "ORD-9912", collector = "Ramesh Kumar", weight = "14.5 kg", status = "In Transit")
                OrderCard(id = "ORD-9913", collector = "Sunita Devi", weight = "8.2 kg", status = "Scheduled")
            }

            // GPS Tracking / Vans Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("GPS Van Tracking", Icons.Rounded.LocalShipping)
                VanCard(vanId = "MH-12-AB-1234", driver = "Suresh", eta = "10 mins", load = "85%")
                VanCard(vanId = "MH-14-XY-9876", driver = "Anil", eta = "45 mins", load = "40%")
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF2E7D32))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
    }
}

@Composable
fun RateCard(modifier: Modifier, title: String, price: String, trend: String) {
    val isUp = trend.startsWith("+")
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontSize = 12.sp, color = Color.Gray)
            Text(text = price, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(
                text = trend, 
                fontSize = 12.sp, 
                color = if (isUp) Color(0xFF388E3C) else Color(0xFFD32F2F)
            )
        }
    }
}

@Composable
fun OrderCard(id: String, collector: String, weight: String, status: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = id, fontWeight = FontWeight.Bold)
                Text(text = "$collector • $weight", color = Color.Gray, fontSize = 14.sp)
            }
            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = status, 
                    color = Color(0xFF2E7D32), 
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun VanCard(vanId: String, driver: String, eta: String, load: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = Color(0xFF1976D2))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = vanId, fontWeight = FontWeight.Bold)
                Text(text = "Driver: $driver • ETA: $eta", color = Color.Gray, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "Load", fontSize = 10.sp, color = Color.Gray)
                Text(text = load, fontWeight = FontWeight.Bold, color = Color(0xFFF57C00))
            }
        }
    }
}
