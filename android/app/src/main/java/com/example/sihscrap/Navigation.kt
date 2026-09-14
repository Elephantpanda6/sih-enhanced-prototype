package com.example.sihscrap

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.sihscrap.ui.main.MainScreen
import com.example.sihscrap.ui.screens.RoleSelectionScreen
import com.example.sihscrap.ui.screens.RecyclerDashboardScreen

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(RoleSelection)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<RoleSelection> {
          RoleSelectionScreen(
              onCollectorClick = { 
                  backStack.removeLastOrNull()
                  backStack.add(Main) 
              },
              onRecyclerClick = { 
                  backStack.removeLastOrNull()
                  backStack.add(RecyclerDashboard) 
              },
              voiceEngine = com.example.sihscrap.voice.VoiceEngine(androidx.compose.ui.platform.LocalContext.current)
          )
        }
        entry<Main> {
          MainScreen(onItemClick = { navKey -> backStack.add(navKey) }, modifier = Modifier.safeDrawingPadding().padding(16.dp))
        }
        entry<RecyclerDashboard> {
          RecyclerDashboardScreen(onBack = { 
              backStack.removeLastOrNull()
              backStack.add(RoleSelection)
          })
        }
      },
  )
}
