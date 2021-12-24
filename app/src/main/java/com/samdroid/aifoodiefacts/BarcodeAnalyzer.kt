package com.samdroid.aifoodiefacts

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.OnFailureListener
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeAnalyzer(var context: Context) : ImageAnalysis.Analyzer {

    var TAG = BarcodeAnalyzer::class.java.simpleName

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        val img = image.image
        if(img != null){
            val inputImage = InputImage.fromMediaImage(img, image.imageInfo.rotationDegrees)
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE,
                    Barcode.FORMAT_AZTEC)
                .build()

            val scanner = BarcodeScanning.getClient(options)
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes){
                        val valueType = barcode.valueType
                        // See API reference for complete list of supported types
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
                                val info = barcode.displayValue
                                Toast.makeText(context, info, Toast.LENGTH_SHORT).show()

                            }

                        }
//                        Toast.makeText(context, barcode.rawValue, Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener{ Log.d(TAG, "analyze: nothing found")}

        }
        image.close()
    }
}