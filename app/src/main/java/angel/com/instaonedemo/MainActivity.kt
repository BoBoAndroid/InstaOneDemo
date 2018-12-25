package angel.com.instaonedemo

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.Surface
import android.widget.CompoundButton
import android.widget.Toast
import com.arashivision.arplayer.GlTarget
import com.arashivision.insta360.arutils.exception.SourceException
import com.arashivision.insta360.arutils.source.ISource
import com.arashivision.insta360.arutils.source.SourceFactory
import com.arashivision.insta360.sdk.render.controller.GestureController
import com.arashivision.insta360.sdk.render.controller.gyro.GyroMatrixProvider
import com.arashivision.insta360.sdk.render.controller.gyro.GyroMatrixType
import com.arashivision.insta360.sdk.render.controller.gyro.VideoAntiShakeController
import com.arashivision.insta360.sdk.render.player.IPlayerFactory
import com.arashivision.insta360.sdk.render.player.PlayerCallback
import com.arashivision.insta360.sdk.render.renderer.Insta360PanoRenderer
import com.arashivision.insta360.sdk.render.renderer.model.SphericalStitchModel
import com.arashivision.insta360.sdk.render.renderer.strategy.FishEyeStrategy
import com.arashivision.insta360.sdk.render.source.OnLoadSourceListener
import com.arashivision.insta360.sdk.render.view.PanoramaView
import com.arashivision.nativeutils.Log
import com.arashivision.onecamera.AudioSource
import com.arashivision.onecamera.OneDriverInfo
import com.arashivision.onecamera.camerarequest.TakePicture
import com.arashivision.onecamera.render.RenderMethod
import com.arashivision.onecamera.render.RenderMode
import org.greenrobot.eventbus.EventBus
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.rajawali3d.materials.textures.ISurfacePlayer
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val MY_PERMISSIONS_REQUEST_ACCESS_EXTERNAL_STORAGE = 1
    private lateinit var mCameraService: CameraService
    private var mCameraStreamStarted: Boolean = false
    private val mCameraStreamWidth = 3840
    private val mCameraStreamHeight = 1920
    private val mCameraStreamFps = 30
    private var mRenderer: Insta360PanoRenderer? = null
    private var mSurface: Object? = null
    private lateinit var videoAntiShakeController: VideoAntiShakeController
    private val mExtra = Bundle()
    private var filename:String=""
    private enum class OpenState {
        Idle, Opening, Opened
    }
    private var mOpenState = OpenState.Idle


    private enum class LivePushingState {
        Idle, Pushing, Stopping
    }

    private var mLivePushingState = LivePushingState.Idle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        EventBus.getDefault().register(this)
        mCameraService = CameraService.instance(this.applicationContext)
        initEvent()
        initPanoView()
        updateUI()
    }

    private fun initEvent(){
        mCameraSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
                onOpenCamera()
            else
                onCloseCamera()
        }
        mStreamingSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
                onStartCameraStream()
            else
                onStopCameraStream()
        }

        bt_takephoto.setOnClickListener {
            filename= File(
                Environment.getExternalStorageDirectory(), "one_demo/photo_" +
                        SimpleDateFormat("MM_dd_HH_mm_ss_SSS", Locale.getDefault()).format(Date()) + ".jpg"
            ).absolutePath
            testCaptureStillImageWithouStorage(filename, false)
        }
    }

    private fun testCaptureStillImageWithouStorage(mPath: String, bStitch: Boolean) {
        val obj = TakePicture()
        obj.mode = OneDriverInfo.Request.ImageMode.NORMAL
        obj.extra_metadata = null
        obj.aeb_ev_bias = IntArray(3)
        obj.aeb_ev_bias[0] = 0
        obj.aeb_ev_bias[1] = 1
        obj.aeb_ev_bias[2] = 2
        mCameraService.captureStillImageWithouStorage(mPath, bStitch, obj)
    }

    private fun onOpenCamera() {
        mCameraService!!.open(null)
        mOpenState = OpenState.Opening
        updateUI()
    }

    private fun onCloseCamera() {
        if (mLivePushingState != LivePushingState.Idle) {
            mCameraService!!.stopRecord(null)
        }
        mCameraService!!.close()
        mOpenState = OpenState.Idle
        mCameraStreamStarted = false
        mLivePushingState = LivePushingState.Idle
        updateUI()
    }

    private fun onStartCameraStream() {
        // 3840x1920@30fps H.264

        mCameraService?.setVideoParam(
            mCameraStreamWidth, mCameraStreamHeight, mCameraStreamFps,
            40, true
        )
        mCameraService?.setAudioSource(AudioSource.AUDIO_CAMERA)
        mCameraService?.fixSomething()
        mCameraService?.switchRenderMode(RenderMode.withGlRenderer(RenderMethod.PlanarKeep))
        mCameraService?.startStreaming()

        val source = SourceFactory.create("usb://.mp4")
        source.updateOffset(getOffset())
        mRenderer!!.getSourceManager().start(source)

        mCameraStreamStarted = true
        updateUI()
    }

    private fun onStopCameraStream() {
        if (!mCameraStreamStarted)
            return
        mCameraService?.stopStreaming()
        mCameraStreamStarted = false
        updateUI()
    }

    private fun getOffset(): String {
        val mKeys = ArrayList<String>()
        mKeys.add(OneDriverInfo.Options.MEDIA_OFFSET)

        val mOptions = mCameraService?.getOptions(mKeys)

        return try {
            if(mOptions!=null){
                mOptions.mediaOffset
            }else{
                " "
            }
        }catch (e:Exception){
            ""
        }
    }

    private fun updateUI(){
        if(mCameraStreamStarted){
            mStreamingSwitch.isEnabled = true
            mStreamingSwitch.isChecked = true
            bt_takephoto.isClickable=true
            setSurfaceViewSize(mCameraStreamWidth, mCameraStreamHeight)
        }else{
            if (mOpenState != OpenState.Opened) {
                mStreamingSwitch.isEnabled = false
                mStreamingSwitch.isChecked = false
                bt_takephoto.isClickable=false
            } else {
                mStreamingSwitch.isEnabled = true
                mStreamingSwitch.isChecked = false
                bt_takephoto.isClickable=true
            }
        }
    }

    private fun setSurfaceViewSize(videoWidth: Int, videoHeight: Int) {
        var w = mPanoramaView.width
        var h = mPanoramaView.height
        if (w * videoHeight > h * videoWidth)
            w = h * videoWidth / videoHeight
        else
            h = w * videoHeight / videoWidth
        val lp = mPanoramaView.layoutParams
        lp.width = w
        lp.height = h
        mPanoramaView.layoutParams = lp
    }

    private fun initPanoView() {
        mPanoramaView.setFrameRate(60.0)
        mPanoramaView.setRenderMode(PanoramaView.RENDERMODE_WHEN_DIRTY)
        mRenderer = Insta360PanoRenderer(applicationContext)

        var playerFactory = object : IPlayerFactory {
            override fun makePlayer(p0: PlayerCallback?): ISurfacePlayer {
                return object : TestSurfacePlayer() {

                    override fun useGLTarget(): Boolean {
                        return true
                    }

                    override fun onCreateGLTarget(glTarget: GlTarget?) {
                        mSurface = glTarget as Object
                        mCameraService.surface = glTarget
                    }

                    override fun onReleaseGLTarget(glTarget: GlTarget?) {
                        super.onReleaseGLTarget(glTarget)
                        if (mCameraService.surface === glTarget)
                            mCameraService.surface = null
                        mSurface = null
                    }

                    override fun onCreateSurface(surface: Surface?) {
                        super.onCreateSurface(surface)
                        mSurface = surface as Object
                        mCameraService.surface = surface
                    }

                    override fun onReleaseSurface(surface: Surface?) {
                        super.onReleaseSurface(surface)
                        mCameraService?.surface = null
                        mSurface = null
                    }

                    override fun isPlaying(): Boolean {
                        return mCameraStreamStarted
                    }

                    override fun readExtras(): Boolean {
                        return false
                    }

                    override fun getExtra(): Bundle {
                        return mExtra
                    }

                    override fun getVolume(): Float {
                        return 0F
                    }

                    override fun getGyro(): String? {
                        return null
                    }

                }
            }

        }

        mRenderer!!.initWithReducedMode(
            FishEyeStrategy(),
            playerFactory, SphericalStitchModel(mRenderer!!.id)
        )
        mPanoramaView.renderer = mRenderer

        //手势
        val controller = GestureController(applicationContext, mRenderer!!.renderModel.camera)
        controller.setHolders(mRenderer!!.renderModel.getLayerAt(0))
        controller.isEnabled = true
        mRenderer!!.controllerManager.addController(
            GestureController::class.java.simpleName,
            controller
        )
        //防抖
        videoAntiShakeController = VideoAntiShakeController(object : GyroMatrixProvider {
            override fun getMatrix(v: Double): FloatArray {
                if (mCameraService!!.currentGyroMatrix == null) {
                    return FloatArray(16)
                } else {
                    return mCameraService!!.currentGyroMatrix
                }
            }

            override fun getGyroMatrixType(): GyroMatrixType {
                return GyroMatrixType.ONE
            }
        })

        videoAntiShakeController.setHolders(mRenderer!!.renderModel.getLayerAt(1))
        videoAntiShakeController.isEnabled = true
        mRenderer!!.controllerManager.addController(
            VideoAntiShakeController::class.java.simpleName,
            videoAntiShakeController
        )



        mRenderer!!.onPlayerPrepareOK()
    }

    private fun requestPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO
                ),
                MY_PERMISSIONS_REQUEST_ACCESS_EXTERNAL_STORAGE
            )
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != MY_PERMISSIONS_REQUEST_ACCESS_EXTERNAL_STORAGE)
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var permissionNotGrant = false
        if (permissions.size == 0)
            permissionNotGrant = true
        else {
            for (i in permissions.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    permissionNotGrant = true
                    break
                }
            }
        }
        if (permissionNotGrant) {
            AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle("权限请求")
                .setMessage("操作相机需要一下权限")
                .setPositiveButton(android.R.string.yes
                ) { dialogInterface, i ->  }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        }
    }

    override fun onStart() {
        super.onStart()
        requestPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        //destroy
        if (mRenderer != null) {
            mRenderer!!.onDestroy()
            mRenderer = null
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOpenComplete(event: CameraService.OpenEvent){
        if (mOpenState != OpenState.Opening)
            return
        mOpenState = OpenState.Opened
        val offset = getOffset()
        mCameraService.updatePanoOffset(offset)
        updateUI()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDetachEvent(event: CameraService.DetachEvent) {
        AlertDialog.Builder(this).setTitle("提示").setMessage("相机已断开")
            .setCancelable(false).setPositiveButton(android.R.string.yes
            ) { dialogInterface, i -> mCameraSwitch.isChecked = false }
            .setIcon(android.R.drawable.ic_dialog_alert).create().show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onErrorEvent(event: CameraService.ErrorEvent) {
        if (mOpenState == OpenState.Idle)
            return

        if (event.error === Error.ERR_NOCAMERA)
            showCameraError("camera_not_attached")
        else
            showCameraError("Camera met error: " + event.error)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onStillImageCapturedEvent(event: CameraService.StillImageCapturedEvent) {
        if (event.error === 0) {
            Toast.makeText(
                this, "take picture success",
                Toast.LENGTH_LONG
            ).show()
            var intent=Intent(this,showPhotoActivity().javaClass)
            intent.putExtra("imgPath",filename)
            startActivity(intent)
        } else {
            Toast.makeText(
                this, "take picture fail",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showCameraError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("提示")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(android.R.string.yes
            ) { dialogInterface, i -> mCameraSwitch.isChecked = false }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .create().show()
    }
}
