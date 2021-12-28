package com.samdroid.aifoodiefacts

import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.camera.camera2.internal.annotation.CameraExecutor
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_INDEFINITE
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Executors.*
import java.util.jar.Manifest

class CaptureActivity : AppCompatActivity() {
    private val TAG = "CaptureActivity"
    lateinit var previewView: PreviewView
    lateinit var flashFb:FloatingActionButton
    lateinit var cameerShutterFb:FloatingActionButton
    lateinit var barcodeScannerFb:FloatingActionButton
    private lateinit var cameraExecutor: ExecutorService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_capture)

        var mCameraManager:CameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        var mCameraId = mCameraManager.cameraIdList[0]
        previewView = findViewById(R.id.previewView)
        flashFb = findViewById(R.id.fb_flash_off);
        cameerShutterFb = findViewById(R.id.fb_camera_shutter)
        barcodeScannerFb = findViewById(R.id.fb_barcode_scanner)

        flashFb.setOnClickListener {
            if (checkFlashAvailable()) {
                mCameraManager.setTorchMode(mCameraId, true)
            } else {
                showNoFlashError()
            }

        }
        checkPermission()

        barcodeScannerFb.setOnClickListener{
            cameraExecutor = newSingleThreadExecutor()
            scanBarcode()
        }

    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))

    }

    fun bindPreview(cameraProvider: ProcessCameraProvider){
        val preview : Preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        var cameraSelector : CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//        preview.setSurfaceProvider(previewView.surfaceProvider)
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview)
        }catch (e : Exception){
            Log.e(TAG, "bindPreview: error", e)
        }

    }

    fun checkPermission(){
        when{
            ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ->{
                startCamera()
            }

            shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)->{
                val snackbar = Snackbar.make(findViewById(R.id.coordinatorLayout), "Need to use the camera to capture the image. Give permission to access camera.", LENGTH_INDEFINITE )
                snackbar.setAction("Okay", View.OnClickListener {
                    requestPermission()
                })
                snackbar.show()
            }
            else -> {
                requestPermission()
            }
        }

    }
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            100 -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                    startCamera()
                } else {
                    val snackbar = Snackbar.make(findViewById(R.id.coordinatorLayout), "Need to use the camera to capture the image. Give permission to access camera.", LENGTH_SHORT)
                    snackbar.setAction("Okay", View.OnClickListener {
                        requestPermission()
                    })
                    snackbar.show()
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun requestPermission(){
        requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 100)
    }

    private fun checkFlashAvailable():Boolean{
        var isFlashAvailable = applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        if(!isFlashAvailable){
            return false
        }
        return true
    }

    private fun showNoFlashError() {
        var builder = AlertDialog.Builder(this)
        builder.setTitle("Oops")
        builder.setMessage("Flash not available in this device ...")
        builder.setPositiveButton("Ok", null)
        var alertDialog = builder.create()
        alertDialog.show()
    }

    override fun onDestroy(){
        super.onDestroy()
        cameraExecutor.shutdown()

    }

    private fun scanBarcode(){
        val cameraProvideFuture = ProcessCameraProvider.getInstance(this)
        cameraProvideFuture.addListener({
            val cameraProvider = cameraProvideFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer(this))
                }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalyzer, preview)

            } catch (ex : java.lang.Exception){
                ex.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(this))
    }

}