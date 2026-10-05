package com.nextyear.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ExifInterface
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

class CameraActivity : Activity(), TextureView.SurfaceTextureListener, CameraUi.Cb {
    private var d = 3f
    private lateinit var root: FrameLayout
    private lateinit var frame: FrameLayout
    private lateinit var tex: TextureView
    private lateinit var ui: CameraUi
    private val thread = HandlerThread("nycam")
    private lateinit var bg: Handler

    private var cam: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var reader: ImageReader? = null
    private var builder: CaptureRequest.Builder? = null
    private var chars: CameraCharacteristics? = null
    private var previewSize = Size(1280, 960)
    private var sensorOrientation = 90
    private var front = false
    private var flashOn = false
    private var zoomIdx = 0
    private val zooms = floatArrayOf(1f, 2f)
    private var surfaceReady = false
    private var opening = false
    @Volatile private var busy = false

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        d = resources.displayMetrics.density
        Haptics.init(this)
        goFullscreen()
        thread.start(); bg = Handler(thread.looper)

        root = FrameLayout(this).apply { setBackgroundColor(Pal.BG) }
        frame = FrameLayout(this).apply {
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, o: android.graphics.Outline) {
                    o.setRoundRect(0, 0, v.width, v.height, 26 * d)
                }
            }
        }
        tex = TextureView(this).apply { surfaceTextureListener = this@CameraActivity }
        frame.addView(tex, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        root.addView(frame, FrameLayout.LayoutParams(1, 1))
        ui = CameraUi(this).also { it.cb = this }
        root.addView(ui, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        root.addOnLayoutChangeListener { _, l, t, r, bm, _, _, _, _ -> layoutFrame(r - l, bm - t) }

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 7)
    }

    private fun layoutFrame(w: Int, h: Int) {
        if (w == 0 || h == 0) return
        val vf = CamGeo.vf(w, h)
        val lp = frame.layoutParams as FrameLayout.LayoutParams
        if (lp.width != vf.width().toInt() || lp.height != vf.height().toInt()) {
            lp.width = vf.width().toInt(); lp.height = vf.height().toInt()
            lp.leftMargin = vf.left.toInt(); lp.topMargin = vf.top.toInt()
            frame.layoutParams = lp
        }
        fitTexture()
    }

    /** Center-crop the preview so it fills the viewfinder. */
    private fun fitTexture() {
        val lp = frame.layoutParams
        val fw = lp.width.toFloat(); val fh = lp.height.toFloat()
        if (fw <= 1f || fh <= 1f) return
        val ar = previewSize.height.toFloat() / previewSize.width // portrait content aspect
        val tw: Float; val th: Float
        if (fw / fh > ar) { tw = fw; th = fw / ar } else { th = fh; tw = fh * ar }
        tex.layoutParams = FrameLayout.LayoutParams(tw.toInt(), th.toInt(), Gravity.CENTER)
        tex.scaleX = if (front) -1f else 1f
    }

    private fun pick(sizes: Array<Size>, maxArea: Int): Size {
        val ok = sizes.filter { it.width * it.height <= maxArea }
        val ratio = ok.filter { abs(it.width.toFloat() / it.height - 4f / 3f) < 0.02f }
        return (if (ratio.isEmpty()) ok else ratio).maxByOrNull { it.width * it.height } ?: sizes[0]
    }

    @SuppressLint("MissingPermission")
    private fun tryOpen() {
        if (!surfaceReady || cam != null || opening) return
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return
        try {
            val mgr = getSystemService(CAMERA_SERVICE) as CameraManager
            val want = if (front) CameraCharacteristics.LENS_FACING_FRONT else CameraCharacteristics.LENS_FACING_BACK
            val id = mgr.cameraIdList.firstOrNull {
                mgr.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == want
            } ?: mgr.cameraIdList.firstOrNull() ?: return
            val ch = mgr.getCameraCharacteristics(id)
            chars = ch
            sensorOrientation = ch.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90
            val map = ch.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return
            previewSize = pick(map.getOutputSizes(SurfaceTexture::class.java), 1_400_000)
            val js = pick(map.getOutputSizes(ImageFormat.JPEG), 4_000_000)
            reader?.close()
            reader = ImageReader.newInstance(js.width, js.height, ImageFormat.JPEG, 2).apply {
                setOnImageAvailableListener({ r -> onImage(r) }, bg)
            }
            ui.flashOk = ch.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            if (!ui.flashOk) { flashOn = false; ui.flash = false }
            fitTexture()
            opening = true
            mgr.openCamera(id, stateCb, bg)
        } catch (e: Exception) {
            opening = false
        }
    }

    private val stateCb = object : CameraDevice.StateCallback() {
        override fun onOpened(c: CameraDevice) { cam = c; opening = false; startPreview() }
        override fun onDisconnected(c: CameraDevice) { c.close(); cam = null; opening = false }
        override fun onError(c: CameraDevice, e: Int) { c.close(); cam = null; opening = false }
    }

    private fun applyZoom(b: CaptureRequest.Builder) {
        val ch = chars ?: return
        val r = ch.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE) ?: return
        val z = zooms[zoomIdx]
        val cw = (r.width() / z).toInt(); val chh = (r.height() / z).toInt()
        val x = r.left + (r.width() - cw) / 2; val y = r.top + (r.height() - chh) / 2
        b.set(CaptureRequest.SCALER_CROP_REGION, Rect(x, y, x + cw, y + chh))
    }

    private fun startPreview() {
        val c = cam ?: return
        val st = tex.surfaceTexture ?: return
        val rd = reader ?: return
        try {
            st.setDefaultBufferSize(previewSize.width, previewSize.height)
            val surf = Surface(st)
            val b = c.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            b.addTarget(surf)
            b.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            applyZoom(b)
            builder = b
            @Suppress("DEPRECATION")
            c.createCaptureSession(listOf(surf, rd.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(s: CameraCaptureSession) {
                    if (cam == null) return
                    session = s
                    try { s.setRepeatingRequest(b.build(), null, bg) } catch (e: Exception) { }
                }
                override fun onConfigureFailed(s: CameraCaptureSession) { }
            }, bg)
        } catch (e: Exception) { }
    }

    private fun closeCamera() {
        try { session?.close() } catch (e: Exception) { }
        try { cam?.close() } catch (e: Exception) { }
        try { reader?.close() } catch (e: Exception) { }
        session = null; cam = null; reader = null; opening = false
    }

    // ---- CameraUi.Cb ----
    override fun onClose() { Haptics.soft(); setResult(RESULT_CANCELED); finish() }

    override fun onFlash() {
        if (!ui.flashOk) return
        flashOn = !flashOn; ui.flash = flashOn; Haptics.tick(); ui.invalidate()
    }

    override fun onZoom() {
        val max = chars?.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1f
        if (max < 2f) return
        zoomIdx = (zoomIdx + 1) % zooms.size
        ui.zoomLabel = if (zoomIdx == 0) "1x" else "2x"
        Haptics.tick(); ui.invalidate()
        bg.post {
            val b = builder ?: return@post
            applyZoom(b)
            try { session?.setRepeatingRequest(b.build(), null, bg) } catch (e: Exception) { }
        }
    }

    override fun onFlip() {
        Haptics.click()
        front = !front; zoomIdx = 0; ui.zoomLabel = "1x"
        closeCamera(); tryOpen()
    }

    override fun onShutter() {
        if (busy) return
        val c = cam ?: return
        val s = session ?: return
        val r = reader ?: return
        busy = true
        Haptics.click()
        bg.post {
            try {
                val b = c.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                b.addTarget(r.surface)
                b.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                applyZoom(b)
                if (flashOn && ui.flashOk) b.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
                b.set(CaptureRequest.JPEG_ORIENTATION, sensorOrientation)
                b.set(CaptureRequest.JPEG_QUALITY, 92.toByte())
                s.capture(b.build(), null, bg)
            } catch (e: Exception) { busy = false }
        }
    }

    private fun onImage(r: ImageReader) {
        val img = r.acquireLatestImage() ?: return
        try {
            val buf = img.planes[0].buffer
            val bytes = ByteArray(buf.remaining()); buf.get(bytes)
            img.close()
            var bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: run { busy = false; return }
            val exif = try {
                ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } catch (e: Exception) { ExifInterface.ORIENTATION_NORMAL }
            val rot = when (exif) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
            val m = Matrix()
            if (rot != 0) m.postRotate(rot.toFloat())
            else if (bmp.width > bmp.height) m.postRotate(sensorOrientation.toFloat())
            if (front) m.postScale(-1f, 1f)
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)

            val ta = frame.width.toFloat() / frame.height.toFloat()
            val ba = bmp.width.toFloat() / bmp.height
            var cw = bmp.width; var chh = bmp.height
            if (ba > ta) cw = (bmp.height * ta).toInt() else chh = (bmp.width / ta).toInt()
            bmp = Bitmap.createBitmap(bmp, (bmp.width - cw) / 2, (bmp.height - chh) / 2, cw, chh)
            val longest = maxOf(bmp.width, bmp.height)
            if (longest > 1600) {
                val k = 1600f / longest
                bmp = Bitmap.createScaledBitmap(bmp, (bmp.width * k).toInt(), (bmp.height * k).toInt(), true)
            }
            FileOutputStream(File(filesDir, "pending.jpg")).use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            val out = bmp
            runOnUiThread { Haptics.soft(); ui.show(out) }
        } catch (e: Exception) {
            busy = false
        }
    }

    override fun onRetake() { Haptics.soft(); busy = false; ui.hide() }

    override fun onConfirm() {
        try {
            val dir = File(filesDir, "photos").apply { mkdirs() }
            File(filesDir, "pending.jpg").copyTo(File(dir, "${System.currentTimeMillis()}.jpg"), overwrite = true)
        } catch (e: Exception) { }
        Haptics.heavy()
        WidgetUtil.refreshAll(this)
        setResult(RESULT_OK)
        finish()
    }

    // ---- TextureView ----
    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) { surfaceReady = true; tryOpen() }
    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) { }
    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean { surfaceReady = false; return true }
    override fun onSurfaceTextureUpdated(st: SurfaceTexture) { }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, res: IntArray) {
        if (res.isNotEmpty() && res[0] == PackageManager.PERMISSION_GRANTED) tryOpen()
        else { Toast.makeText(this, "Camera permission needed", Toast.LENGTH_SHORT).show(); finish() }
    }

    override fun onResume() { super.onResume(); goFullscreen(); if (tex.isAvailable) surfaceReady = true; tryOpen() }
    override fun onPause() { closeCamera(); super.onPause() }
    override fun onDestroy() { thread.quitSafely(); super.onDestroy() }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { if (busy) onRetake() else super.onBackPressed() }

    @Suppress("DEPRECATION")
    private fun goFullscreen() {
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }
}
