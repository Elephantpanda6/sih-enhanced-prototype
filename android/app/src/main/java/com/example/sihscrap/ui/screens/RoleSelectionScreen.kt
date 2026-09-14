package com.example.sihscrap.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import com.example.sihscrap.voice.VoiceEngine
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sihscrap.R

@Composable
fun RoleSelectionScreen(
    onCollectorClick: () -> Unit,
    onRecyclerClick: () -> Unit,
    voiceEngine: VoiceEngine,
    modifier: Modifier = Modifier
) {
    var currentLanguage by remember { mutableStateOf(voiceEngine.currentLanguage) }
    
    // Green gradient background
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2E7D32), // Dark Green
            Color(0xFF81C784)  // Light Green
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "Welcome to",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light
                )
                Text(
                    text = "EcoSetu",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Language Button
            Surface(
                color = Color(0xFFC5E1A5), 
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable {
                        val langs = VoiceEngine.AppLanguage.values()
                        val nextIndex = (currentLanguage.ordinal + 1) % langs.size
                        currentLanguage = langs[nextIndex]
                        voiceEngine.currentLanguage = currentLanguage
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = "Language",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentLanguage.displayName,
                        color = Color(0xFF2E7D32),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))

        // Cropped Image Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Collector Image Button
            Image(
                painter = painterResource(id = R.drawable.btn_collector),
                contentDescription = "Collector Role",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onCollectorClick)
            )

            // Recycler Image Button
            Image(
                painter = painterResource(id = R.drawable.btn_recycler),
                contentDescription = "Recycler Role",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onRecyclerClick)
            )
        }
    }
}
