package photos

import util.URLLoader
import util.XMLListener
import java.lang.Math.min
import java.net.URLEncoder
import java.security.SecureRandom
import java.util.*

class Picasa(
  private val content: LocalContent,
  user: String? = null,
  private val jsonLoader: JsonLoader = JsonLoader()
) {
  companion object {
    internal var random: Random = SecureRandom()
  }

  val user: String = user ?: Config.defaultUser
  val profile = jsonLoader.load("https://www.googleapis.com/oauth2/v1/userinfo", Profile::class, mapOf("alt" to "json"))
  val imgSize = 1600 // this is max that Google allows as imgmax

  val urlPrefix get() = "/${user}"

  val urlSuffix get() = if (user != Config.defaultUser) "?by=$user" else ""

  val gallery get() = jsonLoader.loadAll("/v1/albums", AlbumsResponse::class).toGallery()

  fun getAlbum(name: String): Album {
    val album = gallery.albums[name]!!
    album.photos += jsonLoader.loadAll("/v1/mediaItems:search", PhotosResponse::class, mapOf("albumId" to album.id)).toPhotos()
    return album
  }

  fun getRandomPhotos(numNext: Int): RandomPhotos {
    val album = weightedRandom(gallery.albums.values)
    val photos = getAlbum(album.name!!).photos
    val index = random(photos.size)
    return RandomPhotos(photos.subList(index, min(index + numNext, photos.size)), album.author, album.title)
  }

  fun weightedRandom(albums: Collection<Album>): Album {
    var sum = 0
    for (album in albums) sum += transform(album.size().toDouble())
    val index = random(sum)

    sum = 0
    for (album in albums) {
      sum += transform(album.size().toDouble())
      if (sum > index) return album
    }
    return albums.first()
  }

  private fun transform(n: Double): Int {
    return (100.0 * Math.log10(1 + n / 50.0)).toInt()
  }

  internal fun random(max: Int): Int {
    return if (max == 0) 0 else random.nextInt(max)
  }

  fun search(query: String): Album {
    val thumbSize = 144
    return SearchLoader(content, thumbSize, gallery.albums.values).load("?kind=photo&q=" + urlEncode(query) + "&imgmax=${imgSize}&thumbsize=${thumbSize}c")
  }

  private fun <T: Entity> XMLListener<T>.load(query: String) = URLLoader.load(toFullUrl(query), this)

  private fun toFullUrl(query: String) = Config.apiBase + query

  private fun urlEncode(name: String): String {
    return URLEncoder.encode(name, "UTF-8")
  }

  private fun List<JsonAlbum>.toGallery() = Gallery(212).apply {
    author = profile.name
    albums.putAll(filter { content.contains(it.name) }.map {
      val albumContent = content.forAlbum(it.name)
      it.name!! to Album(144, it.id, it.name, it.title, null, albumContent?.content, author).apply {
        geo = albumContent?.geo
        thumbUrl = it.coverPhotoBaseUrl.crop(212)
        size = it.mediaItemsCount
      }
    }.toMap())
  }

  private fun List<JsonMediaItem>.toPhotos() = map {
    Photo().apply {
      id = it.id
      width = it.mediaMetadata?.width
      height = it.mediaMetadata?.height
      thumbUrl = it.baseUrl.crop(144)
      url = it.baseUrl.fit(1920, 1080)
      description = it.description
      timestampISO = it.mediaMetadata?.creationTime
      it.mediaMetadata?.photo?.let {
        exif = Exif().apply {
          camera = it.cameraModel
          exposure = it.exposureTime
          focal = it.focalLength
          iso = it.isoEquivalent
          fstop = it.apertureFNumber
        }
      }
    }
  }
}
