package com.samdroid.aifoodiefacts

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.text.SpannableString
import android.text.util.Linkify
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.*
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {

    private val rotateOpen : Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_open_anim) }
    private val rotateClose : Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_close_anim) }
    private val frombottom : Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.from_bottom_anim) }
    private val toBottom : Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.to_bottom_anim) }

    private lateinit var recyclerview: RecyclerView
    private lateinit var fabAdd:FloatingActionButton
    private lateinit var fabTakePicture:FloatingActionButton
    private lateinit var fabScanBarcode:FloatingActionButton
    private lateinit var imageView: ImageView
    private var clicked = false

    private val CAMERA_PERMISSION = 100
    private lateinit var cameraLauncher:ActivityResultLauncher<Intent>
    private lateinit var inputImage: InputImage
    private val TAG = MainActivity::class.java.simpleName
    private lateinit var barcodeScanner: BarcodeScanner
    private var flag by Delegates.notNull<Int>()
    private lateinit var photo:Bitmap
    private lateinit var functions: FirebaseFunctions
    private var signinFlag = 0
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: Adapter
    private lateinit var list: MutableList<Data>
    private lateinit var menu: Menu
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerview = findViewById(R.id.recycler_view)
        fabAdd = findViewById(R.id.fab_add)
        fabTakePicture = findViewById(R.id.fab_take_picture)
        fabScanBarcode = findViewById(R.id.fab_scan_barcode)
        imageView = findViewById(R.id.iv_item)
        progressBar = findViewById(R.id.progress_bar)

        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult(), object : ActivityResultCallback<ActivityResult>{
            override fun onActivityResult(result: ActivityResult?) {
                val data = result?.data
                try{
                    photo = data?.extras?.get("data") as Bitmap
                    inputImage = InputImage.fromBitmap(photo,0)
                    if(flag == 1){
                        processBarcode()
                    } else{
                        imageView.setImageBitmap(photo)
                        labelImage()
                    }

                } catch (e : Exception){
                    Log.e(TAG, "onActivityResult: error -> ", e )
                }
//                clicked = true
//                setFabAnimation(clicked)
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
//            Toast.makeText(this, "take picture clicked", Toast.LENGTH_SHORT).show()
            if(auth.currentUser != null){
                flag = 0
                checkPermission()
            } else {
                showSnackbar("Please sign in to use this feature")
            }

        }
        fabScanBarcode.setOnClickListener{
            flag = 1
            checkPermission()
        }

        auth = FirebaseAuth.getInstance()!!
        functions = FirebaseFunctions.getInstance()
        list = mutableListOf()
        recyclerview.layoutManager = LinearLayoutManager(this)
        recyclerview.setHasFixedSize(true)
        adapter = Adapter(this, list)
        recyclerview.adapter = adapter

        if(auth.currentUser != null){
            val name = auth.currentUser?.displayName
            setTitle("Welcome, $name")
        } else{
            setTitle("Vision")
        }
        if (intent.getIntExtra("signedIn", 0) == 1){
            showSnackbar(resources.getString(R.string.signed_in))
        }

    }

    private fun showSnackbar(message: String){
        Snackbar.make(findViewById(R.id.coordinatorLayout), message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater:MenuInflater = getMenuInflater()
        menuInflater.inflate(R.menu.menu, menu)
        if (menu != null) {
            this.menu = menu
            if(auth.currentUser != null){
                menu.findItem(R.id.sign_in).setTitle("Sign out")
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.sign_in){
//            Toast.makeText(this, "sign in clicked", Toast.LENGTH_SHORT).show()
            if(auth.currentUser == null){
                startActivity(Intent(this, AuthenticationActivity::class.java))
            }else{
                val alertDialogBuilder = AlertDialog.Builder(this)
                alertDialogBuilder.setTitle("Sign Out")
                    .setMessage("Are you sure you want to sign out?")
                    .setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i ->
                        AuthUI.getInstance().signOut(this)
                            .addOnCompleteListener(OnCompleteListener {
                                showSnackbar("You have successfully signed out")
                                menu.findItem(R.id.sign_in).setTitle("Sign in")
                                setTitle("Vision")
                            })
                    })
                    .setNegativeButton("No", null)
                val alertDialog = alertDialogBuilder.create()
                alertDialog.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun labelImage(){
        progressBar.visibility = View.VISIBLE
        list.clear()
        photo = scaleDownImage(photo, 640)
        val imageEncodedInString  = convertImageToBase64EncodedString(photo)
        // Create json request to cloud vision
        val request = JsonObject()
        // Add image to request
        val image = JsonObject()
        image.add("content", JsonPrimitive(imageEncodedInString))
        request.add("image", image)
        //Add features to the request
        val feature = JsonObject()
        feature.add("maxResults", JsonPrimitive(5))
        feature.add("type", JsonPrimitive("LABEL_DETECTION"))
        val features = JsonArray()
        features.add(feature)
        request.add("features", features)
        annotateImage(request.toString())
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    // Task failed with an exception
                    // ...
                    showSnackbar(task.exception?.message as String)
                } else {
                    // Task completed successfully
                    // ...
                    for (label in task.result.asJsonArray[0].asJsonObject["labelAnnotations"].asJsonArray) {
                        val labelObj = label.asJsonObject
                        val text = labelObj["description"]
                        val entityId = labelObj["mid"]
                        val confidence = labelObj["score"].asFloat
                        val df = DecimalFormat("#.##")
                        val confidenceInPercent = df.format(confidence * 100)

                        list.add(Data(text.asString, "$confidenceInPercent%"))
                    }
                    adapter.notifyDataSetChanged()
                    progressBar.visibility = View.GONE
                }
            }

    }
    private fun scaleDownImage(image:Bitmap, maxDimension: Int) : Bitmap{
        val originalWidth = image.width
        val originalHeight = image.height
        var resizedWidth = maxDimension
        var resizedHeight = maxDimension
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension
            resizedWidth =
                (resizedHeight * originalWidth.toFloat() / originalHeight.toFloat()).toInt()
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension
            resizedHeight =
                (resizedWidth * originalHeight.toFloat() / originalWidth.toFloat()).toInt()
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension
            resizedWidth = maxDimension
        }
        return Bitmap.createScaledBitmap(image, resizedWidth, resizedHeight, false)
    }

    private fun convertImageToBase64EncodedString(image : Bitmap) : String{
        val byteArrayOutputStream = ByteArrayOutputStream()
        photo.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val imageBytes: ByteArray = byteArrayOutputStream.toByteArray()
        val base64encoded = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        return base64encoded
    }

    private fun annotateImage(requestJson: String): Task<JsonElement> {
        return functions
            .getHttpsCallable("annotateImage")
            .call(requestJson)
            .continueWith { task ->
                // This continuation runs on either success or failure, but if the task
                // has failed then result will throw an Exception which will be
                // propagated down.
                val result = task.result?.data
                JsonParser.parseString(Gson().toJson(result))
            }
    }

    private fun processBarcode(){
        list.clear()
        imageView.setImageBitmap(null)
        progressBar.visibility = View.VISIBLE
        val result = barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes){
                    val valueType = barcode.valueType
                    var message = ""
                    Log.d(TAG, "processBarcode: value type ${valueType}")
                    when (valueType) {
                        Barcode.TYPE_WIFI -> {
                            val ssid = barcode.wifi!!.ssid
                            val password = barcode.wifi!!.password
                            val type = barcode.wifi!!.encryptionType
                            message = "ssid - $ssid\npassword - $password\nencryption type - $type"
                            showAlertDialog(message)
                        }
                        Barcode.TYPE_URL -> {
                            val title = barcode.url!!.title
                            val url = barcode.url!!.url
                            message = "title - $title\nurl - $url"
                            showAlertDialog(message)
                        }
                        Barcode.TYPE_PRODUCT -> {
                            val value = barcode.displayValue
                            message = "result - $value"
                            showAlertDialog(message)
                        }
                        Barcode.TYPE_TEXT -> {
                            val value = barcode.displayValue
                            message = "result - $value"
                            showAlertDialog(message)
                        }
                        Barcode.TYPE_UNKNOWN -> {
                            val value = barcode.displayValue
                            message = "reult - $value"
                            showAlertDialog(message)
                        }
                        Barcode.TYPE_PHONE ->{
                            val phone = barcode.phone?.number
                            message = "phone number - $phone"
                            showAlertDialog(message)
                        }
                        Barcode.TYPE_EMAIL ->{
                            val email = barcode.email
                            message = "email - $email"
                            showAlertDialog(message)
                        }
                    }
                }
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener{
                Log.d(TAG, "processBarcode: failed to analyze image -> ${it.message}")
                progressBar.visibility = View.GONE
            }

    }

    private fun showAlertDialog(message: String){
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Info")
            .setMessage(message)
            .setPositiveButton("Ok", null)
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
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