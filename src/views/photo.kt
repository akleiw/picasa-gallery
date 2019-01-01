package views

import photos.Album
import photos.Photo

//language=HTML
fun photo(photo: Photo, album: Album, redirectUrl: String?) = """
<!DOCTYPE html>
<html lang="en">
<head>
  <title>${+album.title} - ${+photo.description} by ${+album.author}</title>
  <meta name="medium" content="image">
  <meta property="og:title" content="${+(photo.description ?: album.title)} by ${+album.author}">
  <meta property="og:image" content="${+photo.thumbUrlLarge}">
  <link rel="image_src" href="${+photo.thumbUrlLarge}">
  <style>
    html, body { background: black; color: gray; font-family: sans-serif }
    a { color: white }
    img { padding: 1em 0; max-height: 90vh; max-width: 95vw; cursor: pointer }
  </style>
</head>
<body>
  <div itemscope itemtype="http://schema.org/Photograph">
    <meta itemprop="datePublished" content="${photo.date}">
    <a href="/${album.name}">${+album.title} by <span itemprop="author">${+album.author}</span></a>
    <span itemprop="description">${+photo.description}</span>
    <div>
      <img itemprop="image" src="${photo.url}" alt="${+(photo.description ?: album.title)}">
    </div>
  </div>
  ${redirectUrl?.let { """
    <script>location.href = '$it'</script>
  """ } ?: ""}
</body>
</html>
"""
