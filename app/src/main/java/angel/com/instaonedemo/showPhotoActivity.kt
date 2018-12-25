package angel.com.instaonedemo

import android.app.Dialog
import android.os.Bundle
import android.os.Trace
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import com.arashivision.extradata.ARObject
import com.arashivision.insta360.arutils.source.SourceFactory
import com.arashivision.insta360.export.exporter.ARComposerConfig
import com.arashivision.insta360.export.services.ExportManager
import com.arashivision.insta360.export.services.OnExportListener
import com.arashivision.insta360.export.services.Request
import com.arashivision.insta360.sdk.render.controller.GestureController
import com.arashivision.insta360.sdk.render.controller.gyro.CImageGyroController
import com.arashivision.insta360.sdk.render.player.IPlayerFactory
import com.arashivision.insta360.sdk.render.player.PlayerDelegate
import com.arashivision.insta360.sdk.render.renderer.Insta360PanoRenderer
import com.arashivision.insta360.sdk.render.renderer.model.RenderModel
import com.arashivision.insta360.sdk.render.renderer.model.SphericalStitchModel
import com.arashivision.insta360.sdk.render.renderer.screen.SingleScreen
import com.arashivision.insta360.sdk.render.renderer.strategy.FishEyeStrategy
import com.arashivision.insta360.sdk.render.view.PanoramaView
import kotlinx.android.synthetic.main.activity_insta_pic_detail.*
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.*

class showPhotoActivity: AppCompatActivity(),View.OnClickListener {

    private var mRenderer: Insta360PanoRenderer? = null
    private var mRenderModel: RenderModel? = null
    private var mPanoramaView: PanoramaView? = null
    private var mPlayerDelegate: PlayerDelegate? = null
    private var mGestureController: GestureController? = null
    private lateinit var mExportManager: ExportManager
    private var mDialog: Dialog? = null

    private var imgUrl: String?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)// 隐藏标题
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)// 设置全屏
        setContentView(R.layout.activity_insta_pic_detail)
        mExportManager = ExportManager(this.applicationContext)
        initIntent()
        initView()
    }

    override fun onResume() {
        super.onResume()
        mExportManager.register()
    }

    override fun onPause() {
        super.onPause()
        mExportManager.unregister()
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyResources()
    }

    private fun initView(){
        tv_back.setOnClickListener(this)
        iv_ball.setOnClickListener(this)
    }

    private fun initIntent(){
        var intent= intent
        imgUrl=intent.getStringExtra("imgPath")
        startTrace()
        if(!TextUtils.isEmpty(imgUrl)){
            createPicResources()
            val source = SourceFactory.create(imgUrl)
            mRenderer!!.sourceManager.start(source)
            endTrace()
        }
    }

    private fun startTrace() {
        Trace.beginSection("TestARPlayer")
    }

    private fun endTrace() {
        Trace.endSection()
    }
    private fun createPicResources() {
        destroyResources()
        mRenderer = Insta360PanoRenderer(this)
        //render model
        mRenderModel = SphericalStitchModel(mRenderer?.getId())

        //
        mRenderer?.init(
            FishEyeStrategy(), IPlayerFactory.DefaultPlayerFactory(),
            mRenderModel, SingleScreen()
        )

        mRenderer?.renderModelScene!!.backgroundColor = 100
        //surface view
        mPanoramaView = PanoramaView(this)
        mPanoramaView?.setFrameRate(60.0)
        mPanoramaView?.setRenderMode(PanoramaView.RENDERMODE_WHEN_DIRTY)
        mPanoramaView?.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        mPanoramaView?.renderer = mRenderer

        //player
        mPlayerDelegate = mRenderer?.textureHolder?.playerDelegate
        mPlayerDelegate?.setOnPreparedListener {

        }

        //gesture
        mGestureController = GestureController(this, mRenderModel?.getCamera())
        mGestureController?.setHolders(mRenderModel?.getLayerAt(0))
        mGestureController?.isEnabled = true
        mRenderer?.controllerManager?.addController(GestureController::class.java.simpleName, mGestureController)

        val mArObject = ARObject.create(imgUrl)

        val mCImage = CImageGyroController(mArObject.gyro)
        mCImage.gyroQuaternion.toRotationMatrix()

        val postMatrix = mCImage.gyroQuaternion.toRotationMatrix()
        mRenderModel?.postMatrix = postMatrix

        //apply
        val layout = findViewById<FrameLayout>(R.id.layout_loader_container)
        layout.addView(mPanoramaView)
    }

    private fun destroyResources() {
        if (mPlayerDelegate != null) {
            if (mRenderer != null) {
                mRenderer!!.onDestroy()
                mRenderer = null
            }
            val layout = findViewById<FrameLayout>(R.id.layout_loader_container)
            layout.removeView(mPanoramaView)

            mPanoramaView = null
            mPlayerDelegate = null
        }
    }

    override fun onClick(v: View) {
        when(v.id){
            R.id.tv_back->{
                finish()
            }
            R.id.iv_ball->{
                mDialog = CustomProgressDialog.createLoadingDialog(this, "")
                mDialog!!.setCancelable(false)
                mDialog!!.show()
                exportBallPic(this!!.imgUrl!!)
            }
        }
    }

    private fun exportBallPic(mPicUrl: String) {

        if (!TextUtils.isEmpty(mPicUrl)) {
            var outputPath =
                mPicUrl.substring(0, mPicUrl.lastIndexOf("/") + 1) + "球形_" + SimpleDateFormat(
                    "yyyy_MM_dd_HH_mm",
                    Locale.getDefault()
                ).format(Date()) + ".jpg"

            try {
                outputPath = URLDecoder.decode(outputPath, "utf-8")


                var request = Request(ARComposerConfig.TYPE_THUMBNAIL_STITCH)
                request.isBatchProcesses = true
                request.input = mPicUrl
                request.output = outputPath
                request.quality = 100

                val arObject = ARObject.create(request.input)

                if (arObject != null) {
                    val cImageGyroController = CImageGyroController(arObject.gyro)
                    //陀螺仪校准
                    val postMatrix = cImageGyroController.gyroQuaternion.toRotationMatrix()
                    request.postMatrix = postMatrix
                }

                mExportManager.setOnExportListener(object : OnExportListener {
                    override fun onComplete(p0: String?) {

                        if (mDialog!!.isShowing) {
                            mDialog!!.dismiss()
                        }

                        Toast.makeText(
                            this@showPhotoActivity, "导出成功$outputPath",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    override fun onCancel(p0: String?) {
                    }

                    override fun onProgress(p0: String?, p1: Int) {

                    }

                    override fun onFileSizeChanged(p0: String?, p1: String?, p2: Long) {

                    }

                    override fun onError(p0: String?, p1: Int) {
                        if (mDialog!!.isShowing) {
                            mDialog!!.dismiss()
                        }
                        Toast.makeText(
                            this@showPhotoActivity, "出错了$p0",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                })
                mExportManager.enqueue(request)

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "相机出错了", Toast.LENGTH_LONG).show()
        }
    }
}