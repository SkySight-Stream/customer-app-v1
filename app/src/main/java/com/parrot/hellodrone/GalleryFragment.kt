package com.parrot.hellodrone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.parrot.hellodrone.R

class GalleryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var galleryAdapter: GalleryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_gallery, container, false)

        // Set up RecyclerView
        recyclerView = view.findViewById(R.id.gallery_recycler_view)
        recyclerView.layoutManager = GridLayoutManager(context, 3) // 3 columns in the grid
        galleryAdapter = GalleryAdapter()
        recyclerView.adapter = galleryAdapter

        // Load data into the adapter (for demo purposes, using empty data)
        loadGalleryData()

        return view
    }

    private fun loadGalleryData() {
        // Replace with actual data loading logic
        val demoData = listOf(
            MediaItem("Sample Image 1", "image", R.drawable.sample_image_1),
            MediaItem("Sample Video 1", "video", R.drawable.sample_video_thumb_1),
            MediaItem("Sample Image 1", "image", R.drawable.sample_image_2),
            MediaItem("Sample Video 1", "video", R.drawable.sample_video_thumb_2)
        )
        galleryAdapter.submitList(demoData)
    }
}