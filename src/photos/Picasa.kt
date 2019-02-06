package photos

import com.auth0.jwt.JWT
import util.OAuth
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
  val imgSize = 1600 // this is max that Google allows as imgmax

  val urlPrefix: String
    get() = "/${user}"

  val urlSuffix: String
    get() = if (user != Config.defaultUser) "?by=${user}" else ""

  val gallery: Gallery
    get() = jsonLoader.loadAll("/v1/albums", AlbumsResponse::class).toGallery(user, 212)

  fun getAlbum(name: String): Album {
    val thumbSize = 144
    val id = gallery.albums[name]?.id ?: name
    val path = if (id.matches("\\d+".toRegex())) "/albumid/" + id else "/album/" + urlEncode(name)
    val url = path + "?kind=photo,comment&imgmax=${imgSize}&thumbsize=${thumbSize}c&max-results=500" +
      "&fields=id,updated,title,subtitle,icon,gphoto:*,georss:where(gml:Point),entry(title,summary,content,author,category,gphoto:id,gphoto:photoid,gphoto:width,gphoto:height,gphoto:commentCount,gphoto:timestamp,exif:*,media:*,georss:where(gml:Point))"
    val loader = AlbumLoader(content, thumbSize)
    val album = loader.load(url)
    while (album.size > album.photos.size)
      loader.load(url + "&start-index=${album.photos.size+1}")
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

  private fun <T: Entity> XMLListener<T>.load(query: String)
      = URLLoader.load(toFullUrl(query), this)

  private fun toFullUrl(query: String) = Config.apiBase + query

  private fun urlEncode(name: String): String {
    return URLEncoder.encode(name, "UTF-8")
  }
}

private fun List<JsonAlbum>.toGallery(user: String, thumbSize: Int): Gallery {
  return Gallery(thumbSize).apply {
    author = OAuth.userName()
    albums.putAll(filter { it.title != null }.map {
      it.title!! to Album(thumbSize, it.id, it.name, it.title, null, null, author).apply {
        thumbUrl = it.coverPhotoBaseUrl.crop(thumbSize)
        size = it.mediaItemsCount
      }
    }.toMap())
  }
}
