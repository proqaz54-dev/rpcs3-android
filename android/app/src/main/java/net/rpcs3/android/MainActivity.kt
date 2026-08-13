package net.rpcs3.android

import android.opengl.EGL14
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity(), SurfaceHolder.Callback {

    private lateinit var surfaceView: SurfaceView

    private external fun nativeBoot(bootPath: String?): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        surfaceView = SurfaceView(this)
        surfaceView.holder.addCallback(this)
        setContentView(FrameLayout(this).apply { addView(surfaceView) })
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.i(TAG, "surfaceCreated")
        nativeProvideSurface(holder.surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.i(TAG, "surfaceChanged: ${width}x$height")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.i(TAG, "surfaceDestroyed")
        nativeProvideSurface(null)
    }

    private fun nativeProvideSurface(surface: Surface?) {
        // setNativeWindow / releaseNativeWindow on the JNI side
        NativeBridge.setSurface(surface)
        surface?.let { startEmulation() }
    }

    private fun startEmulation() {
        val bootPath = null // TODO: file picker -> game path
        Log.i(TAG, "nativeBoot")
        nativeBoot(bootPath)
    }

    companion object {
        private const val TAG = "RPCS3"
    }
}

object NativeBridge {
    @Volatile
    var surface: Surface? = null
        private set

    fun setSurface(s: Surface?) {
        surface = s
        EGL14.eglGetCurrentDisplay() // touch EGL to ensure surface validity
    }
}