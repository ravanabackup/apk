package com.example.network

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.NetworkUrlInfo
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {

  fun getReachableUrls(port: Int): List<NetworkUrlInfo> {
    val results = mutableListOf<NetworkUrlInfo>()
    try {
      val interfaces = NetworkInterface.getNetworkInterfaces() ?: return emptyList()
      while (interfaces.hasMoreElements()) {
        val nif = interfaces.nextElement()
        if (!nif.isUp || nif.isLoopback) continue

        val ifName = nif.name ?: "net"
        val addresses = nif.inetAddresses
        while (addresses.hasMoreElements()) {
          val addr = addresses.nextElement()
          if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
            val ip = addr.hostAddress ?: continue
            val isTailscale = isTailscaleIp(ip) || ifName.contains("tailscale", ignoreCase = true)
            val isWifi = !isTailscale && (ifName.startsWith("wlan") || ip.startsWith("192.168.") || ip.startsWith("10."))
            val label =
              when {
                isTailscale -> "Tailscale / VPN"
                ifName.startsWith("wlan") -> "Wi-Fi LAN"
                ifName.startsWith("ap") || ifName.startsWith("swlan") -> "Hotspot"
                ifName.startsWith("tun") || ifName.startsWith("wg") -> "VPN Tunnel"
                else -> "Local Network"
              }
            results.add(
              NetworkUrlInfo(
                url = "http://$ip:$port",
                ipAddress = ip,
                interfaceName = ifName,
                label = label,
                isTailscale = isTailscale,
                isWifi = isWifi,
              )
            )
          }
        }
      }
    } catch (_: Exception) {
    }

    return results.sortedWith(
      compareByDescending<NetworkUrlInfo> { it.isWifi }
        .thenByDescending { it.isTailscale }
        .thenBy { it.ipAddress }
    )
  }

  private fun isTailscaleIp(ip: String): Boolean {
    val parts = ip.split(".")
    if (parts.size != 4) return false
    val first = parts[0].toIntOrNull() ?: return false
    val second = parts[1].toIntOrNull() ?: return false
    // Tailscale uses CGNAT 100.64.0.0/10 (100.64.0.0 - 100.127.255.255)
    return first == 100 && second in 64..127
  }
}

object QrCodeHelper {
  fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
    return try {
      val hints = mapOf(EncodeHintType.MARGIN to 1)
      val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
      val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
      for (x in 0 until sizePx) {
        for (y in 0 until sizePx) {
          bitmap.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
        }
      }
      bitmap
    } catch (_: Exception) {
      null
    }
  }
}
