package app.vitune.providers.ytmusic.models

import app.vitune.providers.innertube.Innertube
import app.vitune.providers.innertube.models.Thumbnail
import kotlinx.serialization.Serializable

/**
 * Represents a YouTube Music playlist
 */
@Serializable
data class YTMusicPlaylist(
    val id: String,
    val title: String,
    val description: String? = null,
    val thumbnail: Thumbnail? = null,
    val author: String? = null,
    val songCount: Int = 0,
    val isLiked: Boolean = false,
    val isOfficial: Boolean = false
) {
    /**
     * Convert to the app's playlist model
     */
    fun toPlaylistItem(): Innertube.PlaylistItem {
        return Innertube.PlaylistItem(
            info = Innertube.Info(
                name = title,
                endpoint = null // Will be set by the UI
            ),
            channel = author?.let {
                Innertube.Info(
                    name = it,
                    endpoint = null
                )
            },
            songCount = songCount,
            thumbnail = thumbnail
        )
    }
}

/**
 * Represents a list of YouTube Music playlists
 */
@Serializable
data class YTMusicPlaylists(
    val playlists: List<YTMusicPlaylist>,
    val continuation: String? = null
) 