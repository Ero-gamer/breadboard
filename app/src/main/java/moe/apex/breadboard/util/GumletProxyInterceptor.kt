package moe.apex.breadboard.util

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.net.URLEncoder


class GumletProxyInterceptor : Interceptor {

    companion object {
        private val PROXY_HOST = "ero2.gumlet.io"
        private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mov", "avi", "mkv")

        /** Set this to true/false as the user toggles the preference. */
        @Volatile
        var isEnabled: Boolean = false
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (!isEnabled) return chain.proceed(original)

        val url = original.url

        // Skip if already going through Gumlet (prevents double-wrapping)
        if (url.host == PROXY_HOST) return chain.proceed(original)

        // Skip video files — Gumlet is an image CDN
        val path = url.encodedPath.lowercase()
        val extension = path.substringAfterLast('.', "")
        if (extension in VIDEO_EXTENSIONS) return chain.proceed(original)

        val proxiedUrl = buildProxyUrl(url) ?: return chain.proceed(original)
        val newRequest = original.newBuilder().url(proxiedUrl).build()
        return chain.proceed(newRequest)
    }

    private fun buildProxyUrl(originalUrl: HttpUrl): HttpUrl? {
        return try {
            val encoded = URLEncoder.encode(originalUrl.toString(), "UTF-8")
            HttpUrl.Builder()
                .scheme("https")
                .host(PROXY_HOST)
                // addEncodedPathSegments preserves existing percent-encoding so
                // https%3A%2F%2F… is never double-encoded to %253A%252F…
                .addEncodedPathSegments("fetch/$encoded")
                .addQueryParameter("format", "webp")
                .build()
        } catch (_: Exception) {
            null
        }
    }
}
