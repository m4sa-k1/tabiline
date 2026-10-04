package io.github.m4sak1.tabiline

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.m4sak1.tabiline.core.model.TransportLeg
import io.github.m4sak1.tabiline.core.model.Trip
import io.github.m4sak1.tabiline.feature.editor.LegEditorScreen
import io.github.m4sak1.tabiline.feature.editor.TripEditorDialog
import io.github.m4sak1.tabiline.feature.home.HomeScreen
import io.github.m4sak1.tabiline.feature.home.PlansScreen
import io.github.m4sak1.tabiline.feature.settings.SettingsScreen
import io.github.m4sak1.tabiline.feature.timeline.TimelineScreen
import io.github.m4sak1.tabiline.ui.components.AppDestination
import io.github.m4sak1.tabiline.ui.components.AppBottomBar
import io.github.m4sak1.tabiline.ui.components.BubbleReveal
import io.github.m4sak1.tabiline.ui.theme.TabilineTheme

private val footerFaces = listOf(
    "(·_·)", "(≥o≤)", "(;-;)", "(^-^*)", "(o^^)o",
    "(•‿•)", "(･ω･)", "(≧▽≦)", "(¬‿¬)", "(•̀ᴗ•́)و",
    "(╹▽╹)", "(ᵕ—ᴗ—)", "(｡•́︿•̀｡)",
)

private const val SCREEN_TRANSITION_MILLIS = 320
private val emphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

private fun routePosition(route: String?): Int = when (route) {
    "home" -> 0
    "trip/{tripId}" -> 1
    "plans" -> 2
    "settings" -> 3
    "leg/{tripId}/{legId}" -> 4
    else -> 0
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory((application as TabilineApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        requestHighRefreshRate()
        setContent { TabilineRoot(viewModel) }
    }

    override fun onResume() {
        super.onResume()
        requestHighRefreshRate()
    }

    private fun requestHighRefreshRate() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        window.decorView.post {
            val display = window.decorView.display ?: return@post
            val currentMode = display.mode
            val refreshRate = display.supportedModes
                .asSequence()
                .filter {
                    it.physicalWidth == currentMode.physicalWidth &&
                        it.physicalHeight == currentMode.physicalHeight
                }
                .maxOfOrNull { it.refreshRate }
                ?: display.supportedModes.maxOfOrNull { it.refreshRate }
                ?: return@post
            window.attributes = window.attributes.apply {
                preferredDisplayModeId = 0
                preferredRefreshRate = refreshRate
            }
        }
    }
}

@Composable
private fun TabilineRoot(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    TabilineTheme(settings.theme, settings.accentPalette) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        val nav = rememberNavController()
        val currentEntry by nav.currentBackStackEntryAsState()
        val trips by viewModel.trips.collectAsStateWithLifecycle()
        var tripDialog by remember { mutableStateOf<Trip?>(null) }
        var showNewTrip by remember { mutableStateOf(false) }
        var footerFace by rememberSaveable { mutableStateOf(footerFaces.random()) }
        var selectedTripId by rememberSaveable { mutableStateOf<Long?>(null) }
        LaunchedEffect(trips) {
            if (trips.none { it.trip.id == selectedTripId }) {
                val today = java.time.LocalDate.now()
                selectedTripId = trips.firstOrNull { today in it.trip.startDate..it.trip.endDate }?.trip?.id
                    ?: trips.firstOrNull { it.trip.startDate > today }?.trip?.id
                    ?: trips.firstOrNull()?.trip?.id
            }
        }

        val route = currentEntry?.destination?.route
        val destination = when (route) {
            "home" -> AppDestination.TODAY
            "plans" -> AppDestination.PLANS
            "settings" -> AppDestination.SETTINGS
            "trip/{tripId}" -> AppDestination.TIMELINE
            else -> null
        }
        var retainedDestination by remember { mutableStateOf(AppDestination.TODAY) }
        LaunchedEffect(destination) { destination?.let { retainedDestination = it } }
        val isAddRoute = route == "leg/{tripId}/{legId}" && currentEntry?.arguments?.getLong("legId") == 0L

        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.zIndex(if (isAddRoute) 2f else 0f),
            enterTransition = {
                val opensFromAddButton = targetState.destination.route == "leg/{tripId}/{legId}" &&
                    targetState.arguments?.getLong("legId") == 0L
                if (opensFromAddButton) {
                    EnterTransition.None
                } else {
                    val direction = if (
                        routePosition(targetState.destination.route) >= routePosition(initialState.destination.route)
                    ) 1 else -1
                    slideInHorizontally(
                        initialOffsetX = { direction * it },
                        animationSpec = tween(SCREEN_TRANSITION_MILLIS, easing = emphasizedEasing),
                    )
                }
            },
            exitTransition = {
                val opensFromAddButton = targetState.destination.route == "leg/{tripId}/{legId}" &&
                    targetState.arguments?.getLong("legId") == 0L
                if (opensFromAddButton) ExitTransition.None
                else {
                    val direction = if (
                        routePosition(targetState.destination.route) >= routePosition(initialState.destination.route)
                    ) 1 else -1
                    slideOutHorizontally(
                        targetOffsetX = { -direction * it },
                        animationSpec = tween(SCREEN_TRANSITION_MILLIS, easing = emphasizedEasing),
                    )
                }
            },
            popEnterTransition = {
                val closesIntoAddButton = initialState.destination.route == "leg/{tripId}/{legId}" &&
                    initialState.arguments?.getLong("legId") == 0L
                if (closesIntoAddButton) EnterTransition.None
                else {
                    val direction = if (
                        routePosition(targetState.destination.route) >= routePosition(initialState.destination.route)
                    ) 1 else -1
                    slideInHorizontally(
                        initialOffsetX = { direction * it },
                        animationSpec = tween(SCREEN_TRANSITION_MILLIS, easing = emphasizedEasing),
                    )
                }
            },
            popExitTransition = {
                val closesIntoAddButton = initialState.destination.route == "leg/{tripId}/{legId}" &&
                    initialState.arguments?.getLong("legId") == 0L
                if (closesIntoAddButton) {
                    ExitTransition.None
                } else {
                    val direction = if (
                        routePosition(targetState.destination.route) >= routePosition(initialState.destination.route)
                    ) 1 else -1
                    slideOutHorizontally(
                        targetOffsetX = { -direction * it },
                        animationSpec = tween(SCREEN_TRANSITION_MILLIS, easing = emphasizedEasing),
                    )
                }
            },
        ) {
            composable("home") {
                HomeScreen(
                    trips = trips,
                    onCreateTrip = { showNewTrip = true },
                    onOpenTrip = { selectedTripId = it; nav.navigate("trip/$it") },
                    onEditLeg = { tripId, legId -> selectedTripId = tripId; nav.navigate("leg/$tripId/$legId") },
                    onAddLeg = { tripId ->
                        selectedTripId = tripId
                        nav.navigate("leg/$tripId/0") { launchSingleTop = true }
                    },
                )
            }
            composable("plans") {
                PlansScreen(
                    trips = trips,
                    selectedTripId = selectedTripId,
                    onCreateTrip = { showNewTrip = true },
                    onOpenTrip = { selectedTripId = it; nav.navigate("trip/$it") },
                )
            }
            composable(
                route = "trip/{tripId}",
                arguments = listOf(navArgument("tripId") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("tripId") ?: return@composable
                LaunchedEffect(id) { selectedTripId = id }
                val item by viewModel.observeTrip(id).collectAsState(initial = null)
                TimelineScreen(
                    item = item,
                    settings = settings,
                    onBack = { nav.popBackStack() },
                    onEditTrip = { tripDialog = item?.trip },
                    onDeleteTrip = { viewModel.deleteTrip(id) { nav.navigate("plans") { popUpTo("home") } } },
                    onAddLeg = { nav.navigate("leg/$id/0") { launchSingleTop = true } },
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
                var isSaving by remember(legId) { mutableStateOf(false) }
                var saveError by remember(legId) { mutableStateOf<String?>(null) }
                var addScreenVisible by remember(legId) { mutableStateOf(true) }
                var destinationAfterClose by remember(legId) { mutableStateOf<Long?>(null) }
                LaunchedEffect(legId) {
                    if (legId != 0L) existing = viewModel.getLeg(legId)
                    loaded = true
                }
                val trip = trips.firstOrNull { it.trip.id == tripId }
                val initialTripId = trip?.trip?.takeUnless { it.isAutomatic }?.id
                val isAdding = legId == 0L
                val closeEditor = {
                    if (!isSaving) {
                        if (isAdding) addScreenVisible = false else nav.popBackStack()
                    }
                }
                BackHandler(enabled = isAdding && addScreenVisible) { closeEditor() }

                @Composable
                fun EditorContent() {
                    LegEditorScreen(
                        initialTripId = existing?.let { leg ->
                            trips.firstOrNull { it.trip.id == leg.tripId }?.trip?.takeUnless { it.isAutomatic }?.id
                        } ?: initialTripId,
                        availableTrips = trips.map { it.trip },
                        existing = existing,
                        previous = trip?.legs?.lastOrNull(),
                        defaultZoneId = settings.defaultZoneId,
                        isLoading = !loaded,
                        isSaving = isSaving,
                        saveError = saveError,
                        onBack = closeEditor,
                        onSave = { leg, standalone ->
                            if (!isSaving) {
                                isSaving = true
                                saveError = null
                                viewModel.saveLeg(
                                    leg = leg,
                                    standalone = standalone,
                                    onSaved = { destinationTripId ->
                                        selectedTripId = destinationTripId
                                        if (isAdding) {
                                            destinationAfterClose = destinationTripId
                                            addScreenVisible = false
                                        } else {
                                            nav.popBackStack()
                                            nav.navigate("trip/$destinationTripId") { launchSingleTop = true }
                                        }
                                    },
                                    onError = {
                                        isSaving = false
                                        saveError = "保存できませんでした。入力内容を確認して、もう一度お試しください。"
                                    },
                                )
                            }
                        },
                        onDelete = if (isAdding) null else ({
                            viewModel.deleteLeg(it) { nav.popBackStack() }
                        }),
                    )
                }

                if (isAdding) {
                    BubbleReveal(
                        visible = addScreenVisible,
                        onHidden = {
                            nav.popBackStack()
                            destinationAfterClose?.let { destinationTripId ->
                                nav.navigate("trip/$destinationTripId") { launchSingleTop = true }
                            }
                        },
                    ) { EditorContent() }
                } else {
                    EditorContent()
                }
            }
            composable("settings") {
                SettingsScreen(
                    settings = settings,
                    onUpdate = viewModel::updateSettings,
                )
            }
        }

        if (destination != null || isAddRoute) {
            val barDestination = destination ?: retainedDestination
            val today = java.time.LocalDate.now()
            val homeTripId = trips.firstOrNull {
                !it.trip.isAutomatic && today in it.trip.startDate..it.trip.endDate
            }?.trip?.id
            val routeTripId = currentEntry?.arguments?.getLong("tripId")
            AppBottomBar(
                selected = barDestination,
                modifier = Modifier.align(Alignment.BottomCenter).zIndex(1f),
                onAdd = when (barDestination) {
                    AppDestination.TODAY -> ({
                        nav.navigate("leg/${homeTripId ?: 0L}/0") { launchSingleTop = true }
                    })
                    AppDestination.TIMELINE -> routeTripId?.let { tripId ->
                        { nav.navigate("leg/$tripId/0") { launchSingleTop = true } }
                    }
                    AppDestination.PLANS -> ({ showNewTrip = true })
                    AppDestination.SETTINGS -> ({
                        footerFace = footerFaces.filterNot { it == footerFace }.random()
                    })
                },
                addContentDescription = when (barDestination) {
                    AppDestination.TODAY -> "移動を追加"
                    AppDestination.TIMELINE -> "移動を追加"
                    AppDestination.PLANS -> "新しい旅行"
                    AppDestination.SETTINGS -> "表情を変える"
                },
                addText = footerFace.takeIf { barDestination == AppDestination.SETTINGS },
            ) { target ->
                when (target) {
                    AppDestination.TODAY -> nav.popBackStack("home", false)
                    AppDestination.TIMELINE -> selectedTripId?.let {
                        nav.navigate("trip/$it") { launchSingleTop = true }
                    }
                    AppDestination.PLANS -> nav.navigate("plans") { launchSingleTop = true }
                    AppDestination.SETTINGS -> nav.navigate("settings") { launchSingleTop = true }
                }
            }
        }

        if (showNewTrip) TripEditorDialog(null, { showNewTrip = false }) {
            viewModel.saveTrip(it) { id -> selectedTripId = id; showNewTrip = false; nav.navigate("trip/$id") }
        }
        tripDialog?.let { trip -> TripEditorDialog(trip, { tripDialog = null }) {
            viewModel.saveTrip(it) { tripDialog = null }
        } }
        }
    }
}
