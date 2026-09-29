package com.moontir.app.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.moontir.app.ui.theme.MoontirTheme

private class MapBridge(private val onPick: (Double, Double) -> Unit) {
    private val handler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onPick(lat: Double, lng: Double) {
        handler.post {
            try {
                onPick.invoke(lat, lng)
            } catch (_: Throwable) {}
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LeafletMapView(
    latitude: Double?,
    longitude: Double?,
    onPickPin: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
    heightDp: Int = 240
) {
    val context = LocalContext.current
    val isLight = MaterialTheme.colorScheme.background.red > 0.5f

    val lat = latitude ?: -6.9932
    val lon = longitude ?: 110.4203

    val bgColor = if (isLight) "#EBF0D5" else "#26221F"
    val pinColor = if (isLight) "#403A35" else "#D8E2AE"
    val pinBorder = if (isLight) "#D8E2AE" else "#191715"
    val attrColor = if (isLight) "#5C544D" else "#C7C2B8"
    val tileFilterStyle = if (isLight) {
        ""
    } else {
        ".leaflet-tile { filter: brightness(0.8) contrast(1.1) invert(0.92) hue-rotate(180deg); }"
    }

    val leafletJs = remember {
        try {
            context.assets.open("leaflet/leaflet.js").bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            ""
        }
    }
    val leafletCss = remember {
        try {
            context.assets.open("leaflet/leaflet.css").bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            ""
        }
    }

    val initialHtml = remember(isLight) {
        val cssTag = if (leafletCss.isNotBlank()) {
            "<style>$leafletCss</style>"
        } else {
            "<link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />"
        }

        val jsTag = if (leafletJs.isNotBlank()) {
            "<script>$leafletJs</script>"
        } else {
            "<script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>"
        }

        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            $cssTag
            <style>
                html, body, #map {
                    margin: 0; padding: 0; height: 100%; width: 100%;
                    background: $bgColor;
                }
                .moon-pin {
                    width: 24px; height: 24px; border-radius: 50%;
                    background: $pinColor;
                    border: 3px solid $pinBorder;
                    box-shadow: 0 2px 8px rgba(0,0,0,0.35);
                    transform: translate(-12px, -12px);
                }
                .leaflet-container { background: $bgColor; }
                $tileFilterStyle
                .leaflet-control-attribution {
                    background: rgba(0,0,0,0.5) !important;
                    color: $attrColor !important;
                    font-size: 9px;
                    border-radius: 4px;
                    padding: 2px 6px;
                }
                .leaflet-control-attribution a { color: $attrColor !important; }
            </style>
        </head>
        <body>
        <div id="map"></div>
        $jsTag
        <script>
            var map = L.map('map', {
                zoomControl: false,
                attributionControl: true
            }).setView([$lat, $lon], 15);

            var osmLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                maxZoom: 19,
                subdomains: ['a', 'b', 'c'],
                attribution: '© OpenStreetMap'
            });

            var cartoLayer = L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                maxZoom: 19,
                subdomains: ['a', 'b', 'c', 'd'],
                attribution: '© CartoDB'
            });

            osmLayer.on('tileerror', function() {
                if (!map.hasLayer(cartoLayer)) {
                    map.removeLayer(osmLayer);
                    cartoLayer.addTo(map);
                }
            });

            osmLayer.addTo(map);

            var pinIcon = L.divIcon({ className: 'moon-pin', iconSize: [24, 24] });
            var marker = L.marker([$lat, $lon], { draggable: true, icon: pinIcon }).addTo(map);

            marker.on('dragend', function(e) {
                var p = e.target.getLatLng();
                if (window.AndroidBridge && window.AndroidBridge.onPick) {
                    window.AndroidBridge.onPick(p.lat, p.lng);
                }
            });

            map.on('click', function(e) {
                marker.setLatLng(e.latlng);
                if (window.AndroidBridge && window.AndroidBridge.onPick) {
                    window.AndroidBridge.onPick(e.latlng.lat, e.latlng.lng);
                }
            });

            window.updatePin = function(newLat, newLon) {
                if (window.map && window.marker) {
                    var current = window.marker.getLatLng();
                    if (Math.abs(current.lat - newLat) > 0.00001 || Math.abs(current.lng - newLon) > 0.00001) {
                        marker.setLatLng([newLat, newLon]);
                        window.map.panTo([newLat, newLon], { animate: true, duration: 0.4 });
                    }
                }
            };
        </script>
        </body>
        </html>
        """.trimIndent()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MoontirTheme.colors.borderStrong, RoundedCornerShape(16.dp))
            .background(MoontirTheme.colors.surfaceTertiary)
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 MoontirApp/1.0"
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()
                    addJavascriptInterface(MapBridge(onPickPin), "AndroidBridge")
                    loadDataWithBaseURL("https://tile.openstreetmap.org/", initialHtml, "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                try {
                    if (webView.isAttachedToWindow) {
                        webView.evaluateJavascript("if (window.updatePin) { window.updatePin($lat, $lon); }", null)
                    }
                } catch (_: Throwable) {}
            },
            onRelease = { webView ->
                try {
                    webView.stopLoading()
                    webView.removeJavascriptInterface("AndroidBridge")
                    webView.webChromeClient = null
                    webView.webViewClient = WebViewClient()
                    webView.clearFocus()
                    webView.post {
                        try {
                            (webView.parent as? android.view.ViewGroup)?.removeView(webView)
                            webView.destroy()
                        } catch (_: Throwable) {}
                    }
                } catch (_: Throwable) {}
            },
            modifier = Modifier.matchParentSize()
        )
    }
}
