package app.vitune.android.ui.screens.ytmusic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.vitune.android.R
import app.vitune.android.ui.components.themed.HeaderWithIcon
import app.vitune.android.ui.components.themed.Scaffold
import app.vitune.android.ui.items.PlaylistItem
import app.vitune.android.ui.screens.GlobalRoutes
import app.vitune.android.ui.screens.Route
import app.vitune.android.ui.screens.playlistRoute
import app.vitune.android.utils.toast
import app.vitune.compose.routing.RouteHandler
import app.vitune.providers.innertube.models.NavigationEndpoint
import app.vitune.providers.ytmusic.YTMusic
import app.vitune.providers.ytmusic.models.YTMusicPlaylist

@Route
@Composable
fun YTMusicScreen(
    authHeaders: Map<String, String>
) {
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var playlists by remember { mutableStateOf<List<YTMusicPlaylist>>(emptyList()) }
    var likedSongsPlaylist by remember { mutableStateOf<YTMusicPlaylist?>(null) }
    
    val context = LocalContext.current
    
    LaunchedEffect(authHeaders) {
        isLoading = true
        error = null
        
        val ytMusic = YTMusic(authHeaders)
        
        // Get liked songs
        ytMusic.getLikedSongs()
            .onSuccess { response ->
                // In a real implementation, we would parse the response
                // For now, create a placeholder
                likedSongsPlaylist = YTMusicPlaylist(
                    id = "LM",
                    title = "Liked Songs",
                    songCount = 0,
                    isLiked = true,
                    isOfficial = true
                )
            }
            .onFailure {
                error = it.message
                context.toast("Failed to load liked songs: ${it.message}")
            }
        
        // Get playlists
        ytMusic.getLibraryPlaylists()
            .onSuccess { response ->
                // In a real implementation, we would parse the response
                // For now, create placeholders
                playlists = listOf(
                    YTMusicPlaylist(
                        id = "PLAYLIST1",
                        title = "My Playlist 1",
                        songCount = 10,
                        isOfficial = false
                    ),
                    YTMusicPlaylist(
                        id = "PLAYLIST2",
                        title = "My Playlist 2",
                        songCount = 5,
                        isOfficial = false
                    )
                )
            }
            .onFailure {
                error = it.message
                context.toast("Failed to load playlists: ${it.message}")
            }
        
        isLoading = false
    }
    
    RouteHandler {
        GlobalRoutes()
        
        Content {
            Scaffold(
                key = "ytmusic",
                topIconButtonId = R.drawable.chevron_back,
                onTopIconButtonClick = pop,
                tabIndex = 0,
                onTabChange = { },
                tabColumnContent = {
                    tab(0, R.string.playlists, R.drawable.playlist)
                }
            ) { _ ->
                if (isLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (error != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Error: $error")
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            HeaderWithIcon(
                                title = stringResource(R.string.official_playlists),
                                iconId = R.drawable.youtube_music
                            )
                        }
                        
                        // Liked Songs playlist
                        likedSongsPlaylist?.let { playlist ->
                            item {
                                PlaylistItem(
                                    title = playlist.title,
                                    channelName = "YouTube Music",
                                    songCount = playlist.songCount,
                                    thumbnailUrl = null,
                                    onClick = {
                                        playlistRoute(
                                            browseId = "VL${playlist.id}",
                                            params = null,
                                            maxDepth = null,
                                            shouldDedup = true
                                        )
                                    }
                                )
                            }
                        }
                        
                        // User's playlists
                        if (playlists.isNotEmpty()) {
                            item {
                                HeaderWithIcon(
                                    title = stringResource(R.string.your_playlists),
                                    iconId = R.drawable.playlist
                                )
                            }
                            
                            items(playlists) { playlist ->
                                PlaylistItem(
                                    title = playlist.title,
                                    channelName = playlist.author ?: "You",
                                    songCount = playlist.songCount,
                                    thumbnailUrl = playlist.thumbnail?.url,
                                    onClick = {
                                        playlistRoute(
                                            browseId = "VL${playlist.id}",
                                            params = null,
                                            maxDepth = null,
                                            shouldDedup = true
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 