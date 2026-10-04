package io.github.m4sak1.tabiline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.m4sak1.tabiline.core.model.TransportLeg
import io.github.m4sak1.tabiline.core.model.Trip
import io.github.m4sak1.tabiline.feature.editor.LegEditorScreen
import io.github.m4sak1.tabiline.feature.editor.TripEditorDialog
import io.github.m4sak1.tabiline.feature.home.HomeScreen
import io.github.m4sak1.tabiline.feature.settings.SettingsScreen
import io.github.m4sak1.tabiline.feature.timeline.TimelineScreen
import io.github.m4sak1.tabiline.ui.theme.TabilineTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory((application as TabilineApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TabilineRoot(viewModel) }
    }
}

@Composable
private fun TabilineRoot(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    TabilineTheme(settings.theme) {
        val nav = rememberNavController()
        val trips by viewModel.trips.collectAsStateWithLifecycle()
        var tripDialog by remember { mutableStateOf<Trip?>(null) }
        var showNewTrip by remember { mutableStateOf(false) }

        NavHost(navController = nav, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    trips = trips,
                    onCreateTrip = { showNewTrip = true },
                    onOpenTrip = { nav.navigate("trip/$it") },
                    onSettings = { nav.navigate("settings") },
                )
            }
            composable(
                route = "trip/{tripId}",
                arguments = listOf(navArgument("tripId") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("tripId") ?: return@composable
                val item by viewModel.observeTrip(id).collectAsState(initial = null)
                TimelineScreen(
                    item = item,
                    settings = settings,
                    onBack = { nav.popBackStack() },
                    onEditTrip = { tripDialog = item?.trip },
                    onDeleteTrip = { viewModel.deleteTrip(id) { nav.popBackStack("home", false) } },
                    onAddLeg = { nav.navigate("leg/$id/0") },
                    onEditLeg = { nav.navigate("leg/$id/$it") },
                    onMoveLeg = { legId, direction ->
                        val legs = item?.legs.orEmpty().toMutableList()
                        val from = legs.indexOfFirst { it.id == legId }
                        val to = (from + direction).coerceIn(0, legs.lastIndex)
                        if (from >= 0 && from != to) {
                            val moved = legs.removeAt(from); legs.add(to, moved)
                            viewModel.reorder(id, legs.map { it.id })
                        }
                    },
                )
            }
            composable(
                route = "leg/{tripId}/{legId}",
                arguments = listOf(
                    navArgument("tripId") { type = NavType.LongType },
                    navArgument("legId") { type = NavType.LongType },
                ),
            ) { entry ->
                val tripId = entry.arguments?.getLong("tripId") ?: return@composable
                val legId = entry.arguments?.getLong("legId") ?: 0
                var existing by remember(legId) { mutableStateOf<TransportLeg?>(null) }
                var loaded by remember(legId) { mutableStateOf(legId == 0L) }
                LaunchedEffect(legId) {
                    if (legId != 0L) existing = viewModel.getLeg(legId)
                    loaded = true
                }
                val trip = trips.firstOrNull { it.trip.id == tripId }
                LegEditorScreen(
                    tripId = tripId,
                    existing = existing,
                    previous = trip?.legs?.lastOrNull(),
                    isLoading = !loaded,
                    onBack = { nav.popBackStack() },
                    onSave = { viewModel.saveLeg(it) { nav.popBackStack() } },
                    onDelete = if (legId == 0L) null else ({ viewModel.deleteLeg(it) { nav.popBackStack() } }),
                )
            }
            composable("settings") {
                SettingsScreen(settings, { nav.popBackStack() }, viewModel::updateSettings)
            }
        }

        if (showNewTrip) TripEditorDialog(null, { showNewTrip = false }) {
            viewModel.saveTrip(it) { id -> showNewTrip = false; nav.navigate("trip/$id") }
        }
        tripDialog?.let { trip -> TripEditorDialog(trip, { tripDialog = null }) {
            viewModel.saveTrip(it) { tripDialog = null }
        } }
    }
}
