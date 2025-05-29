package app.vitune.android.ui.screens.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.vitune.android.Database
import app.vitune.android.DatabaseInitializer
import app.vitune.android.LocalPlayerServiceBinder
import app.vitune.android.R
import app.vitune.android.preferences.AppearancePreferences
import app.vitune.android.preferences.DataPreferences
import app.vitune.android.preferences.PlayerPreferences
import app.vitune.android.query
import app.vitune.android.service.PlayerMediaBrowserService
import app.vitune.android.service.PrecacheService
import app.vitune.android.ui.components.themed.SecondaryTextButton
import app.vitune.android.ui.components.themed.SliderDialog
import app.vitune.android.ui.components.themed.SliderDialogBody
import app.vitune.android.ui.screens.Route
import app.vitune.android.ui.screens.logsRoute
import app.vitune.android.utils.findActivity
import app.vitune.android.utils.intent
import app.vitune.android.utils.isIgnoringBatteryOptimizations
import app.vitune.android.utils.smoothScrollToBottom
import app.vitune.android.utils.toast
import app.vitune.core.ui.utils.isAtLeastAndroid12
import app.vitune.core.ui.utils.isAtLeastAndroid6
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.system.exitProcess
import com.google.android.gms.auth.GoogleAuthUtil
import androidx.compose.runtime.LaunchedEffect
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

@SuppressLint("BatteryLife")
@Route
@Composable
fun OtherSettings() {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val uriHandler = LocalUriHandler.current

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var isAndroidAutoEnabled by remember {
        val component = ComponentName(context, PlayerMediaBrowserService::class.java)
        val disabledFlag = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        val enabledFlag = PackageManager.COMPONENT_ENABLED_STATE_ENABLED

        mutableStateOf(
            value = context.packageManager.getComponentEnabledSetting(component) == enabledFlag,
            policy = object : SnapshotMutationPolicy<Boolean> {
                override fun equivalent(a: Boolean, b: Boolean): Boolean {
                    context.packageManager.setComponentEnabledSetting(
                        component,
                        if (b) enabledFlag else disabledFlag,
                        PackageManager.DONT_KILL_APP
                    )
                    return a == b
                }
            }
        )
    }

    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(context.isIgnoringBatteryOptimizations)
    }

    val activityResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { isIgnoringBatteryOptimizations = context.isIgnoringBatteryOptimizations }
    )

    val queriesCount by remember {
        Database.queriesCount().distinctUntilChanged()
    }.collectAsState(initial = 0)

    SettingsCategoryScreen(
        title = stringResource(R.string.other),
        scrollState = scrollState
    ) {
        SettingsGroup(title = stringResource(R.string.android_auto)) {
            SwitchSettingsEntry(
                title = stringResource(R.string.android_auto),
                text = stringResource(R.string.android_auto_description),
                isChecked = isAndroidAutoEnabled,
                onCheckedChange = { isAndroidAutoEnabled = it }
            )

            AnimatedVisibility(visible = isAndroidAutoEnabled) {
                SettingsDescription(text = stringResource(R.string.android_auto_warning))
            }
        }
        SettingsGroup(title = stringResource(R.string.search_history)) {
            SwitchSettingsEntry(
                title = stringResource(R.string.pause_search_history),
                text = stringResource(R.string.pause_search_history_description),
                isChecked = DataPreferences.pauseSearchHistory,
                onCheckedChange = { DataPreferences.pauseSearchHistory = it }
            )

            AnimatedVisibility(visible = !(DataPreferences.pauseSearchHistory && queriesCount == 0)) {
                SettingsEntry(
                    title = stringResource(R.string.clear_search_history),
                    text = if (queriesCount > 0) stringResource(
                        R.string.format_clear_search_history_amount,
                        queriesCount
                    )
                    else stringResource(R.string.empty_history),
                    onClick = { query(Database::clearQueries) },
                    isEnabled = queriesCount > 0
                )
            }
        }
        SettingsGroup(title = stringResource(R.string.playlists)) {
            SwitchSettingsEntry(
                title = stringResource(R.string.auto_sync_playlists),
                text = stringResource(R.string.auto_sync_playlists_description),
                isChecked = DataPreferences.autoSyncPlaylists,
                onCheckedChange = { DataPreferences.autoSyncPlaylists = it }
            )
        }
        SettingsGroup(title = stringResource(R.string.built_in_playlists)) {
            IntSettingsEntry(
                title = stringResource(R.string.top_list_length),
                text = stringResource(R.string.top_list_length_description),
                currentValue = DataPreferences.topListLength,
                setValue = { DataPreferences.topListLength = it },
                defaultValue = 10,
                range = 1..500
            )
        }
        SettingsGroup(title = stringResource(R.string.quick_picks)) {
            EnumValueSelectorSettingsEntry(
                title = stringResource(R.string.quick_picks_source),
                selectedValue = DataPreferences.quickPicksSource,
                onValueSelect = { DataPreferences.quickPicksSource = it },
                valueText = { it.displayName() }
            )

            SwitchSettingsEntry(
                title = stringResource(R.string.quick_picks_cache),
                text = stringResource(R.string.quick_picks_cache_description),
                isChecked = DataPreferences.shouldCacheQuickPicks,
                onCheckedChange = { DataPreferences.shouldCacheQuickPicks = it }
            )
        }
        SettingsGroup(title = stringResource(R.string.dynamic_thumbnails)) {
            var selectingThumbnailSize by remember { mutableStateOf(false) }
            SettingsEntry(
                title = stringResource(R.string.max_dynamic_thumbnail_size),
                text = stringResource(R.string.max_dynamic_thumbnail_size_description),
                onClick = { selectingThumbnailSize = true }
            )
            if (selectingThumbnailSize) SliderDialog(
                onDismiss = { selectingThumbnailSize = false },
                title = stringResource(R.string.max_dynamic_thumbnail_size)
            ) {
                SliderDialogBody(
                    provideState = {
                        remember(AppearancePreferences.maxThumbnailSize) {
                            mutableFloatStateOf(AppearancePreferences.maxThumbnailSize.toFloat())
                        }
                    },
                    onSlideComplete = { AppearancePreferences.maxThumbnailSize = it.roundToInt() },
                    min = 32f,
                    max = 1920f,
                    toDisplay = { stringResource(R.string.format_px, it.roundToInt()) },
                    steps = 58
                )
            }
        }
        SettingsGroup(title = stringResource(R.string.service_lifetime)) {
            AnimatedVisibility(visible = !isIgnoringBatteryOptimizations) {
                SettingsDescription(
                    text = stringResource(R.string.service_lifetime_warning),
                    important = true
                )
            }

            if (isAtLeastAndroid12) SettingsDescription(
                text = stringResource(R.string.service_lifetime_warning_android_12)
            )

            SettingsEntry(
                title = stringResource(R.string.ignore_battery_optimizations),
                text = if (isIgnoringBatteryOptimizations) stringResource(R.string.ignoring_battery_optimizations)
                else stringResource(R.string.ignore_battery_optimizations_action),
                onClick = {
                    if (!isAtLeastAndroid6) return@SettingsEntry

                    try {
                        activityResultLauncher.launch(
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                        )
                    } catch (e: ActivityNotFoundException) {
                        try {
                            activityResultLauncher.launch(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        } catch (e: ActivityNotFoundException) {
                            context.toast(context.getString(R.string.no_battery_optimization_settings_found))
                        }
                    }
                },
                isEnabled = !isIgnoringBatteryOptimizations
            )

            AnimatedVisibility(!isAtLeastAndroid12 || isIgnoringBatteryOptimizations) {
                SwitchSettingsEntry(
                    title = stringResource(R.string.invincible_service),
                    text = stringResource(R.string.invincible_service_description),
                    isChecked = PlayerPreferences.isInvincibilityEnabled,
                    onCheckedChange = { PlayerPreferences.isInvincibilityEnabled = it }
                )
            }

            SettingsEntry(
                title = stringResource(R.string.need_help),
                text = stringResource(R.string.need_help_description),
                onClick = {
                    uriHandler.openUri("https://dontkillmyapp.com/")
                }
            )

            SettingsDescription(text = stringResource(R.string.service_lifetime_report_issue))
        }

        var showTroubleshoot by rememberSaveable { mutableStateOf(false) }

        AnimatedContent(showTroubleshoot, label = "") { show ->
            if (show) SettingsGroup(
                title = stringResource(R.string.troubleshooting),
                description = stringResource(R.string.troubleshooting_warning),
                important = true
            ) {
                val troubleshootScope = rememberCoroutineScope()
                var reloading by rememberSaveable { mutableStateOf(false) }

                SecondaryTextButton(
                    text = stringResource(R.string.reload_app_internals),
                    onClick = {
                        if (!reloading) troubleshootScope.launch {
                            reloading = true
                            context.stopService(context.intent<PrecacheService>())
                            binder?.restartForegroundOrStop()
                            DatabaseInitializer.reload()
                            reloading = false
                        }
                    },
                    enabled = !reloading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                SecondaryTextButton(
                    text = stringResource(R.string.kill_app),
                    onClick = {
                        binder?.stopRadio()
                        binder?.invincible = false
                        context.findActivity().finishAndRemoveTask()
                        binder?.restartForegroundOrStop()
                        troubleshootScope.launch {
                            delay(500L)
                            Handler(Looper.getMainLooper()).postAtFrontOfQueue { exitProcess(0) }
                        }
                    },
                    enabled = !reloading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                SecondaryTextButton(
                    text = stringResource(R.string.show_logs),
                    onClick = {
                        logsRoute.global()
                    },
                    enabled = !reloading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                        .padding(horizontal = 16.dp)
                )
            } else SecondaryTextButton(
                text = stringResource(R.string.show_troubleshoot_section),
                onClick = {
                    coroutineScope.launch {
                        delay(500)
                        scrollState.smoothScrollToBottom()
                    }
                    showTroubleshoot = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 16.dp)
                    .padding(horizontal = 16.dp)
            )
        }

        SettingsGroup(title = stringResource(R.string.ytmusic)) {
            var isLoggedIn by rememberSaveable { mutableStateOf(DataPreferences.ytMusicIdToken.isNotBlank()) }
            var accountName by rememberSaveable { mutableStateOf(DataPreferences.ytMusicAccountEmail) }
            var idToken by rememberSaveable { mutableStateOf(DataPreferences.ytMusicIdToken) }

            // Configura GoogleSignInClient
            val gso = remember {
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestIdToken("YOUR_WEB_CLIENT_ID") // Sostituisci con il client ID OAuth 2.0
                    .build()
            }
            val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

            // Launcher per Google Sign-In
            val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                    isLoggedIn = true
                    accountName = account.email ?: account.displayName ?: ""
                    idToken = account.idToken ?: ""
                    DataPreferences.ytMusicAccountEmail = accountName
                    DataPreferences.ytMusicIdToken = idToken
                } catch (e: Exception) {
                    isLoggedIn = false
                    accountName = ""
                    idToken = ""
                    DataPreferences.ytMusicAccountEmail = ""
                    DataPreferences.ytMusicIdToken = ""
                }
            }

            if (isLoggedIn) {
                SettingsEntry(
                    title = stringResource(R.string.ytmusic_logged_in, accountName),
                    text = stringResource(R.string.ytmusic_logout),
                    onClick = {
                        googleSignInClient.signOut()
                        isLoggedIn = false
                        accountName = ""
                        idToken = ""
                        DataPreferences.ytMusicAccountEmail = ""
                        DataPreferences.ytMusicIdToken = ""
                    }
                )
                SettingsEntry(
                    title = stringResource(R.string.ytmusic_import),
                    text = null,
                    onClick = {
                        // TODO: Usa idToken per importare playlist da YouTube Music
                    }
                )
            } else {
                SettingsEntry(
                    title = stringResource(R.string.ytmusic_login),
                    text = stringResource(R.string.ytmusic_login_description),
                    onClick = {
                        signInLauncher.launch(googleSignInClient.signInIntent)
                    }
                )
            }
        }
    }
}

val client = OkHttpClient()

fun fetchYouTubeMusicPlaylists(accessToken: String, onResult: (List<YTMusicPlaylist>?) -> Unit) {
    val request = Request.Builder()
        .url("https://www.googleapis.com/youtube/v3/playlists?mine=true&part=snippet,contentDetails&maxResults=50")
        .addHeader("Authorization", "Bearer $accessToken")
        .build()
    Thread {
        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && body != null) {
                val json = JSONObject(body)
                val items = json.getJSONArray("items")
                val playlists = mutableListOf<YTMusicPlaylist>()
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val id = item.getString("id")
                    val snippet = item.getJSONObject("snippet")
                    val title = snippet.getString("title")
                    val thumbnails = snippet.optJSONObject("thumbnails")
                    val thumb = thumbnails?.optJSONObject("default")?.optString("url")
                    val contentDetails = item.optJSONObject("contentDetails")
                    val count = contentDetails?.optInt("itemCount") ?: 0
                    playlists.add(YTMusicPlaylist(id, title, count, thumb))
                }
                onResult(playlists)
            } else {
                onResult(null)
            }
        } catch (e: Exception) {
            onResult(null)
        }
    }.start()
}

// ... nel signInLauncher, dopo aver ottenuto l'access token ...
LaunchedEffect(account) {
    try {
        val token = withContext(Dispatchers.IO) {
            GoogleAuthUtil.getToken(
                context,
                account.account,
                "oauth2:https://www.googleapis.com/auth/youtube.readonly"
            )
        }
        accessToken = token
        error = null
        // Fetch playlists
        fetchYouTubeMusicPlaylists(token) { playlists ->
            playlists?.let {
                DataPreferences.ytMusicPlaylists = it
            }
        }
    } catch (e: Exception) {
        error = e.message
    }
}
