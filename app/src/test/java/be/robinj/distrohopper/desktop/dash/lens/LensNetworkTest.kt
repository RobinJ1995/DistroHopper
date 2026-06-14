package be.robinj.distrohopper.desktop.dash.lens

import android.app.Application
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.net.URLConnection

@RunWith(RobolectricTestRunner::class)
class LensNetworkTest {
    private lateinit var application: Application
    private lateinit var lens: FakeConnectionLens

    @Before fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        lens = FakeConnectionLens(application)
    }

    @Test fun downloadStrReadsAllContent() {
        lens.stream = TrackingInputStream("noot\nnoot".byteInputStream())
        assertEquals("nootnoot", lens.download("http://example.com"))
    }

    @Test fun downloadStrClosesStreamOnSuccess() {
        val stream = TrackingInputStream("noot noot".byteInputStream())
        lens.stream = stream
        lens.download("http://example.com")
        assertTrue(stream.closed)
    }

    @Test fun downloadStrClosesStreamWhenReadFails() {
        val stream = TrackingInputStream(FailingInputStream())
        lens.stream = stream
        try {
            lens.download("http://example.com")
            fail("Expected IOException")
        } catch (expected: IOException) {
        }
        assertTrue("stream must be closed even when the download fails", stream.closed)
    }

    @Test fun downloadImageDecodesImageBytes() {
        val pngBytes = ByteArrayOutputStream().also {
            Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }.toByteArray()
        lens.stream = TrackingInputStream(ByteArrayInputStream(pngBytes))
        assertNotNull(lens.downloadDrawable("http://example.com/image.png"))
    }

    @Test fun downloadImageClosesStreamOnSuccess() {
        val stream = TrackingInputStream(ByteArrayInputStream(ByteArray(8)))
        lens.stream = stream
        lens.downloadDrawable("http://example.com/image.png")
        assertTrue(stream.closed)
    }

    @Test fun downloadImageClosesStreamWhenReadFails() {
        val stream = TrackingInputStream(FailingInputStream())
        lens.stream = stream
        try {
            lens.downloadDrawable("http://example.com/image.png")
            fail("Expected IOException")
        } catch (expected: IOException) {
        }
        assertTrue("stream must be closed even when the download fails", stream.closed)
    }

    @Test fun connectionsAreConfiguredWithTimeouts() {
        val connection = lens.realConnection("http://example.com")
        assertTrue("a connect timeout must be set", connection.connectTimeout > 0)
        assertTrue("a read timeout must be set", connection.readTimeout > 0)
    }

    /** Wraps a stream and records whether it has been closed. */
    private class TrackingInputStream(private val delegate: InputStream) : InputStream() {
        var closed = false

        override fun read() = delegate.read()
        override fun read(b: ByteArray, off: Int, len: Int) = delegate.read(b, off, len)
        override fun close() {
            closed = true
            delegate.close()
        }
    }

    /** Yields a few bytes, then fails mid-download. */
    private class FailingInputStream : InputStream() {
        private var reads = 0

        override fun read(): Int {
            if (++reads > 4) {
                throw IOException("connection reset")
            }
            return 'x'.code
        }
    }

    private class FakeConnectionLens(context: android.content.Context) : Lens(context) {
        var stream: InputStream? = null

        fun download(url: String): String = downloadStr(url)
        fun downloadDrawable(url: String) = downloadImage(url)
        fun realConnection(url: String): URLConnection = super.openConnection(url)

        override fun openConnection(url: String): URLConnection =
            object : URLConnection(URL(url)) {
                override fun connect() {}
                override fun getInputStream(): InputStream = stream!!
            }

        override val type = LensType.NETWORK
        override suspend fun search(query: String, maxResults: Int, emitter: LensResultEmitter) {}
        override fun getName() = "FakeConnection"
        override fun getDescription() = "Test lens"
    }
}
