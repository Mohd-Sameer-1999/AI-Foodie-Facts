package com.samdroid.aifoodiefacts

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    lateinit var recyclerview:RecyclerView
    lateinit var fabAdd:FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_main)

        recyclerview = findViewById(R.id.recycler_view)
        fabAdd = findViewById(R.id.fab_add)
        fabAdd.setOnClickListener {
//            Toast.makeText(this, "clicked", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, CaptureActivity::class.java)
            startActivity(intent)
        }

    }

}