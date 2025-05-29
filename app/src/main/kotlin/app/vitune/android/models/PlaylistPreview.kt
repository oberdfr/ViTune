package app.vitune.android.models

import androidx.compose.runtime.Immutable
import androidx.room.Ignore

@Immutable
data class PlaylistPreview(
    val id: Long,
    val name: String,
    val songCount: Int,
    val thumbnail: String?
) {
    @Ignore
    var isYouTubeMusic: Boolean? = null
    @Ignore
    var ytMusicId: String? = null

    val playlist by lazy {
        Playlist(
            id = id,
            name = name,
            browseId = null,
            thumbnail = thumbnail,
            isYouTubeMusic = isYouTubeMusic == true,
            ytMusicId = ytMusicId
        )
    }
}
