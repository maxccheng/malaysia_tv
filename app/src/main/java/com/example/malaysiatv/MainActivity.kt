package com.example.malaysiatv

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.MotionEvent
import android.view.View
import android.view.WindowMetrics
import android.webkit.*
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintSet
import java.io.*
import java.lang.IllegalArgumentException
import java.lang.StringBuilder

var adList: StringBuilder = StringBuilder()
var desktopModeActual: Boolean = false
var desktopMode: Boolean = false
var fullscreenView: View? = null
var fullscreenCallback: WebChromeClient.CustomViewCallback? = null
var screenHeight: Int = 0
var screenWidth: Int = 0

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        desktopModeActual = isDexEnabled()

        adList = loadAdList()
        Log.i("MainActivity", "adlist length " + adList.length)

        var mainWebView: WebView = findViewById(R.id.mainWebView)
        var fullscreenLayout: FrameLayout = findViewById(R.id.fullscreenView)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = this.windowManager.currentWindowMetrics
            screenHeight = metrics.bounds.width()
            screenWidth = metrics.bounds.height()
        } else {
            val metrics = DisplayMetrics()
            this.getWindowManager().getDefaultDisplay().getMetrics(metrics)
            screenHeight = metrics.heightPixels
            screenWidth = metrics.widthPixels
        }

        Log.i("MainActivity", "screenWidth=$screenWidth")
        Log.i("MainActivity", "screenHeight=$screenHeight")

        mainWebView.settings.javaScriptEnabled = true
        mainWebView.settings.builtInZoomControls = true
        mainWebView.settings.domStorageEnabled = true
        mainWebView.settings.mediaPlaybackRequiresUserGesture = false

        mainWebView.webViewClient = object: WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                mainWebView.evaluateJavascript("var vid = 'kdFzSYy1bHxrForBrar';" +
                        "\$('.embed-responsive').html('<iframe frameborder=\"0\" class=\"embed-responsive-item\" ng-src=\"//www.dailymotion.com/embed/video/'+vid+'?syndication=273888&amp;queue-enable=false&amp;ui-start-screen-info=false&amp;autoPlay=1&amp;mute=0\" allowfullscreen allow=\"autoplay;fullscreen\" src=\"//www.dailymotion.com/embed/video/'+vid+'?syndication=273888&amp;queue-enable=false&amp;ui-start-screen-info=false&amp;autoplay=1&amp;mute=0&amp;fullscreen=1&amp;quality=720\"></iframe>');", null)

                mainWebView.evaluateJavascript("var isFS = false; window.onclick = function(e) { if (!isFS) {\$('.embed-responsive').css({\"position\":\"fixed\", \"top\":\"0\", \"bottom\":\"0\", \"left\":\"0\", \"right\":\"0\", \"width\":\"100%\", \"height\":\"100%\", \"margin\":\"0\", \"padding\":\"0\", \"overflow\":\"hidden\", \"z-index\":\"99999\"}); isFS = true;} e.preventDefault(); e.stopPropagation(); return false; }", null)

                val handler = Handler()
                handler.postDelayed({
                    val x = (screenWidth * 0.5).toFloat()
                    val y = (screenHeight * 0.85).toFloat()
                    simulateClick(x, y)
                    Log.i("MainActivity", "simulateClick($x, $y)")
                }, 18000)

                super.onPageFinished(view, url)
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {

                val EMPTY: ByteArrayInputStream = ByteArrayInputStream("".toByteArray())
                val url: String = ":::::" + request?.url?.host
                if (adList.contains(url)) {
                    Log.i("MainActivity", "blocked $url")
                    return WebResourceResponse("text/plain", "utf-8", EMPTY)
                }
                return super.shouldInterceptRequest(view, request)
            }
        }

        mainWebView.webChromeClient = object: WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                mainWebView.visibility = View.GONE
                if (fullscreenView != null) {
                    callback?.onCustomViewHidden()
                    return
                }

                fullscreenLayout.addView(view)
                fullscreenView = view
                fullscreenCallback = callback
                fullscreenLayout.visibility = View.VISIBLE
                //super.onShowCustomView(view, callback)
            }

            override fun onHideCustomView() {
                if (fullscreenView == null) {
                    return
                }

                fullscreenView!!.visibility = View.GONE
                fullscreenLayout.removeView(fullscreenView)
                fullscreenView = null
                fullscreenCallback?.onCustomViewHidden()
                mainWebView.visibility = View.VISIBLE
                //super.onHideCustomView()
            }
        }

        mainWebView.loadUrl("https://www.xtra.com.my/live-tv/")

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        desktopMode = isDexEnabled()
        if (desktopMode != desktopModeActual) {
            desktopModeActual = desktopMode
            this.onCreate(null)
        }
    }

    fun loadAdList(): StringBuilder {
        var sb: StringBuilder = StringBuilder()
        val fis: InputStream = resources.openRawResource(R.raw.adblockserverlist)
        val br: BufferedReader = fis.bufferedReader()

        br.useLines {
            it.forEach { x -> sb.append(x + "\n") }
        }

        return sb
    }

    fun isDexEnabled(): Boolean {
        val config = resources.configuration

        try {
            val configClass = config.javaClass
            return configClass.getField("SEM_DESKTOP_MODE_ENABLED").getInt(configClass) ==
                    configClass.getField("semDesktopModeEnabled").getInt(config)
        }
        catch (e: NoSuchFieldException) {
        }
        catch (e: IllegalAccessException) {
        }
        catch (e: IllegalArgumentException) {
        }

        return false
    }

    fun simulateClick(x: Float, y: Float) {
        val pressedDownTime: Long = SystemClock.uptimeMillis()
        val eventTime: Long = SystemClock.uptimeMillis()

        var properties: Array<MotionEvent.PointerProperties> = arrayOf(MotionEvent.PointerProperties())
        var pp1: MotionEvent.PointerProperties = MotionEvent.PointerProperties()
        pp1.id = 0
        pp1.toolType = MotionEvent.TOOL_TYPE_FINGER

        var ptrCoords: Array<MotionEvent.PointerCoords> = arrayOf(MotionEvent.PointerCoords())
        var pc1: MotionEvent.PointerCoords = MotionEvent.PointerCoords()
        pc1.x = x
        pc1.y = y
        pc1.pressure = 1f
        pc1.size = 5f
        ptrCoords[0] = pc1

        var mov: MotionEvent = MotionEvent.obtain(
            pressedDownTime,
            eventTime,
            MotionEvent.ACTION_DOWN,
            1,
            properties,
            ptrCoords,
            0,0,1f,1f,0,0,0,0)

        dispatchTouchEvent(mov)

        mov = MotionEvent.obtain(
            pressedDownTime,
            eventTime,
            MotionEvent.ACTION_UP,
            1,
            properties,
            ptrCoords,
            0,0,1f,1f,0,0,0,0)

        dispatchTouchEvent(mov)
    }
}
