package com.budgetnotes.app.ui.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.budgetnotes.app.BudgetNotesApplication
import com.budgetnotes.app.navigation.Routes
import com.budgetnotes.app.ui.cards.CardEditorScreen
import com.budgetnotes.app.ui.cards.CardEditorViewModel
import com.budgetnotes.app.ui.cards.CardsHomeScreen
import com.budgetnotes.app.ui.cards.CardsHomeViewModel
import com.budgetnotes.app.ui.editor.NoteEditorScreen
import com.budgetnotes.app.ui.editor.NoteEditorViewModel
import com.budgetnotes.app.ui.home.HomeScreen
import com.budgetnotes.app.ui.home.HomeViewModel

private enum class RootTab(
    val route: String,
    val label: String,
) {
    Notes(Routes.NOTES_HOME, "Notes"),
    Cards(Routes.CARDS_HOME, "Cards"),
}

/**
 * Editors live as siblings of the tab shell so showing/hiding the bottom bar
 * does not resize an already-visible NavHost during transitions.
 */
@Composable
fun RootScaffold(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.TABS,
    ) {
        navigation(
            startDestination = Routes.NOTES_HOME,
            route = Routes.TABS,
        ) {
            composable(Routes.NOTES_HOME) {
                TabScaffold(
                    navController = navController,
                    selected = RootTab.Notes,
                ) {
                    val app = LocalContext.current.applicationContext as BudgetNotesApplication
                    val repository = remember { app.container.repository }
                    val vm: HomeViewModel = viewModel(
                        factory = HomeViewModel.Factory(repository),
                    )
                    HomeScreen(
                        viewModel = vm,
                        onOpenNote = { noteId ->
                            navController.navigate(Routes.noteEditor(noteId))
                        },
                    )
                }
            }
            composable(Routes.CARDS_HOME) {
                TabScaffold(
                    navController = navController,
                    selected = RootTab.Cards,
                ) {
                    val app = LocalContext.current.applicationContext as BudgetNotesApplication
                    val cardRepository = remember { app.container.cardRepository }
                    val imageStore = remember { app.container.imageStore }
                    val vm: CardsHomeViewModel = viewModel(
                        factory = CardsHomeViewModel.Factory(cardRepository),
                    )
                    CardsHomeScreen(
                        viewModel = vm,
                        imageStore = imageStore,
                        onOpenCard = { cardId ->
                            navController.navigate(Routes.cardEditor(cardId))
                        },
                    )
                }
            }
        }

        composable(
            route = Routes.NOTE_EDITOR,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType }),
        ) { entry ->
            val noteId = entry.arguments?.getLong("noteId") ?: return@composable
            val app = LocalContext.current.applicationContext as BudgetNotesApplication
            val repository = remember { app.container.repository }
            val vm: NoteEditorViewModel = viewModel(
                factory = NoteEditorViewModel.Factory(noteId, repository),
            )
            NoteEditorScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.CARD_EDITOR,
            arguments = listOf(navArgument("cardId") { type = NavType.LongType }),
        ) { entry ->
            val cardId = entry.arguments?.getLong("cardId") ?: return@composable
            val app = LocalContext.current.applicationContext as BudgetNotesApplication
            val cardRepository = remember { app.container.cardRepository }
            val imageStore = remember { app.container.imageStore }
            val vm: CardEditorViewModel = viewModel(
                factory = CardEditorViewModel.Factory(
                    cardId = cardId,
                    repository = cardRepository,
                    imageStore = imageStore,
                ),
            )
            CardEditorScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun TabScaffold(
    navController: NavHostController,
    selected: RootTab,
    content: @Composable () -> Unit,
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                RootTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selected == tab,
                        onClick = {
                            if (selected == tab) return@NavigationBarItem
                            navController.navigate(tab.route) {
                                popUpTo(Routes.TABS) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    RootTab.Notes -> Icons.AutoMirrored.Filled.Note
                                    RootTab.Cards -> Icons.Default.CreditCard
                                },
                                contentDescription = tab.label,
                            )
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}
