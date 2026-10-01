package com.atulpandey.clearwrite

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.atulpandey.clearwrite.data.ClearWriteLocalStore
import com.atulpandey.clearwrite.ai.AiRewriteService
import com.atulpandey.clearwrite.billing.PlayBillingManager
import com.atulpandey.clearwrite.ui.main.MainScreen
import com.atulpandey.clearwrite.ui.main.MainScreenViewModel

@Composable
fun MainNavigation(
  localStore: ClearWriteLocalStore,
  billingManager: PlayBillingManager,
  aiRewriteRepository: AiRewriteService,
) {
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          val mainViewModel =
            viewModel<MainScreenViewModel> {
              MainScreenViewModel(localStore = localStore, aiRewriteService = aiRewriteRepository)
            }
          MainScreen(viewModel = mainViewModel, billingManager = billingManager)
        }
      },
  )
}
