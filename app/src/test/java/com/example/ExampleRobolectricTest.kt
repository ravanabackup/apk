package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.network.DashboardHtml
import com.example.network.QrCodeHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("WaveBridge", appName)
  }

  @Test
  fun `dashboard html renders core controls and endpoints`() {
    val html = DashboardHtml.render()
    assertTrue(html.contains("/ws/control"))
    assertTrue(html.contains("/ws/audio"))
    assertTrue(html.contains("/api/history/export?format=json"))
    assertTrue(html.contains("/api/history/export?format=csv"))
  }

  @Test
  fun `qr code helper generates valid bitmap`() {
    val bmp = QrCodeHelper.generateQrBitmap("http://192.168.1.50:8765", 256)
    assertNotNull(bmp)
    assertEquals(256, bmp?.width)
    assertEquals(256, bmp?.height)
  }
}
