package com.example.quietly

import GalleryAdapter
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class GalleryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GalleryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery) // Ensure activity_gallery.xml is correct and exists

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2) // Grid layout with 2 columns

        // Initialize RecyclerView adapter
        adapter = GalleryAdapter(this)
        recyclerView.adapter = adapter

        // Fetch processed pictures from server
        fetchProcessedPictures()
    }

    private fun fetchProcessedPictures() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://your-node-server-ip:3000") // Replace with your server URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ApiService::class.java)
        service.getProcessedPictures().enqueue(object : Callback<List<String>> {
            override fun onResponse(call: Call<List<String>>, response: Response<List<String>>) {
                if (response.isSuccessful) {
                    val pictures = response.body()
                    pictures?.let {
                        adapter.setPictures(it)
                    }
                } else {
                    Toast.makeText(this@GalleryActivity, "Failed to fetch pictures", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<String>>, t: Throwable) {
                t.printStackTrace()
                Toast.makeText(this@GalleryActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
