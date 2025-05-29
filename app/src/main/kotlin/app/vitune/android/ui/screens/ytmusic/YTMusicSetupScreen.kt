package app.vitune.android.ui.screens.ytmusic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.vitune.android.R
import app.vitune.android.preferences.DataPreferences
import app.vitune.android.ui.components.themed.HeaderWithIcon
import app.vitune.android.ui.components.themed.Scaffold
import app.vitune.android.ui.screens.GlobalRoutes
import app.vitune.android.ui.screens.Route
import app.vitune.android.utils.toast
import app.vitune.compose.routing.RouteHandler
import app.vitune.providers.ytmusic.YTMusic
import kotlinx.coroutines.launch

@Route
@Composable
fun YTMusicSetupScreen() {
    var headersText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    RouteHandler {
        GlobalRoutes()
        
        Content {
            Scaffold(
                key = "ytmusic_setup",
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = 0,
                onTabChange = { },
                tabColumnContent = {
                    tab(0, R.string.setup, R.drawable.settings)
                }
            ) { _ ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HeaderWithIcon(
                        title = stringResource(R.string.youtube_music_setup),
                        iconId = R.drawable.youtube_music
                    )
                    
                    Text(
                        text = stringResource(R.string.youtube_music_setup_description),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = headersText,
                        onValueChange = { headersText = it },
                        label = { Text(stringResource(R.string.youtube_music_headers)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 10
                    )
                    
                    Button(
                        onClick = {
                            if (headersText.isBlank()) {
                                context.toast(context.getString(R.string.youtube_music_headers_empty))
                                return@Button
                            }
                            
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    val headers = YTMusic.setupFromBrowser(headersText)
                                    
                                    // Store headers in preferences
                                    DataPreferences.ytMusicHeaders = headersText
                                    
                                    // Navigate to YTMusic screen
                                    ytMusicRoute(headers)
                                } catch (e: Exception) {
                                    context.toast("Error: ${e.message}")
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(4.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.connect))
                        }
                    }
                }
            }
        }
    }
} 