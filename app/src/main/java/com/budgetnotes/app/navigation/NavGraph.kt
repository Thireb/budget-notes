package com.budgetnotes.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.budgetnotes.app.BudgetNotesApplication
import com.budgetnotes.app.ui.editor.NoteEditorScreen
import com.budgetnotes.app.ui.editor.NoteEditorViewModel
import com.budgetnotes.app.ui.home.HomeScreen
import com.budgetnotes.app.ui.home.HomeViewModel

object Routes {
    const val HOME = "home"
    const val EDITOR = "editor/{noteId}"
    fun editor(noteId: Long) = "editor/$noteId"
}

@Composable
fun BudgetNotesNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    val app = LocalContext.current.applicationContext as BudgetNotesApplication
    val repository = remember { app.container.repository }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(repository),
            )
            HomeScreen(
                viewModel = vm,
                onOpenNote = { noteId ->
                    navController.navigate(Routes.editor(noteId))
                },
            )
        }
        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType }),
        ) { entry ->
            val noteId = entry.arguments?.getLong("noteId") ?: return@composable
            val vm: NoteEditorViewModel = viewModel(
                factory = NoteEditorViewModel.Factory(noteId, repository),
            )
            NoteEditorScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
