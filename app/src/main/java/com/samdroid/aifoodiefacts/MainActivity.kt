package com.samdroid.aifoodiefacts

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class MainActivity : AppCompatActivity() {

    private val rotateOpen : Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_open_anim) }
    private val rotateClose : Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_close_anim) }
    private val frombottom : Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.from_bottom_anim) }
    private val toBottom : Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.to_bottom_anim) }

    lateinit var recyclerview:RecyclerView
    lateinit var fabAdd:FloatingActionButton
    lateinit var fabTakePicture:FloatingActionButton
    lateinit var fabScanBarcode:FloatingActionButton
    private var clicked = false

    private val CAMERA_PERMISSION = 100
    private lateinit var cameraLauncher:ActivityResultLauncher<Intent>
    private lateinit var inputImage: InputImage
    private val TAG = MainActivity::class.java.simpleName
    private lateinit var barcodeScanner: BarcodeScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_main)
        recyclerview = findViewById(R.id.recycler_view)
        fabAdd = findViewById(R.id.fab_add)
        fabTakePicture = findViewById(R.id.fab_take_picture)
        fabScanBarcode = findViewById(R.id.fab_scan_barcode)

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), object : ActivityResultCallback<ActivityResult>{
            override fun onActivityResult(result: ActivityResult?) {
                val data = result?.data
                try{
                    val photo = data?.extras?.get("data") as Bitmap
                    inputImage = InputImage.fromBitmap(photo,0)
                    processBarcode()
                } catch (e : Exception){
                    Log.e(TAG, "onActivityResult: error -> ", e )
                }
            }

        })

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_ALL_FORMATS)
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)


        fabAdd.setOnClickListener {
//            Toast.makeText(this, "clicked", Toast.LENGTH_SHORT).show()
//            val intent = Intent(this, CaptureActivity::class.java)
//            startActivity(intent)
            onAddButtonClicked()
        }
        fabTakePicture.setOnClickListener{
            Toast.makeText(this, "take picture clicked", Toast.LENGTH_SHORT).show()
            checkPermission()
        }
        fabScanBarcode.setOnClickListener{
            checkPermission()
            Toast.makeText(this, "scan barcode clicked", Toast.LENGTH_SHORT).show()

        }


    }

    private fun processBarcode(){
        val result = barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes){
                    val valueType = barcode.valueType
                    Log.d(TAG, "processBarcode: value type ${valueType}")
                    when (valueType) {
                        Barcode.TYPE_WIFI -> {
                            val ssid = barcode.wifi!!.ssid
                            val password = barcode.wifi!!.password
                            val type = barcode.wifi!!.encryptionType

                        }
                        Barcode.TYPE_URL -> {
                            val title = barcode.url!!.title
                            val url = barcode.url!!.url
                        }
                        Barcode.TYPE_PRODUCT -> {
                            val value = barcode.displayValue
                            Log.d(TAG, "processBarcode: value = ${value}")
                        }
                        Barcode.TYPE_TEXT -> {
                            val value = barcode.displayValue
                            Log.d(TAG, "processBarcode: value = ${value}")
                        }
                        Barcode.TYPE_UNKNOWN -> {
                            val value = barcode.displayValue
                            Log.d(TAG, "processBarcode: value = ${value}")
                        }
                    }
                }
            }
            .addOnFailureListener{
                Log.d(TAG, "processBarcode: failed to analyze image -> ${it.message}")
            }

    }

    private fun onAddButtonClicked() {
        setFabVisibility(clicked)
        setFabAnimation(clicked)
        setFabClickable(clicked)
        clicked =!clicked
    }

    private fun setFabAnimation(clicked : Boolean) {
        if(!clicked){
            fabScanBarcode.startAnimation(frombottom)
            fabTakePicture.startAnimation(frombottom)
            fabAdd.startAnimation(rotateOpen)
        } else {
            fabScanBarcode.startAnimation(toBottom)
            fabTakePicture.startAnimation(toBottom)
            fabAdd.startAnimation(rotateClose)
        }

    }

    private fun setFabVisibility(clicked: Boolean) {
        if(!clicked){
            fabTakePicture.visibility = View.VISIBLE
            fabScanBarcode.visibility = View.VISIBLE
        } else {
            fabTakePicture.visibility = View.INVISIBLE
            fabScanBarcode.visibility = View.INVISIBLE
        }
    }

    private fun setFabClickable(clicked: Boolean){
        if(!clicked){
            fabTakePicture.isClickable = true
            fabScanBarcode.isClickable = true
        } else {
            fabTakePicture.isClickable = false
            fabScanBarcode.isClickable = false
        }
    }

    fun checkPermission(){
        when{
            ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ->{
                openCameraIntent()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)->{
                val snackbar = Snackbar.make(findViewById(R.id.coordinatorLayout), "Need to use the camera to do the given task. Give permission to access camera.",
                    BaseTransientBottomBar.LENGTH_INDEFINITE
                )
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
            CAMERA_PERMISSION-> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                    openCameraIntent()

                } else {
                    val snackbar = Snackbar.make(findViewById(R.id.coordinatorLayout), "Need to use the camera to capture the image. Give permission to access camera.",
                        BaseTransientBottomBar.LENGTH_SHORT
                    )
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
        requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION)

    }

    private fun openCameraIntent(){
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(cameraIntent)
    }


}