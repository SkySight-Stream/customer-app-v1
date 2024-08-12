package com.parrot.hellodrone

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.parrot.drone.groundsdk.device.peripheral.stream.CameraLive
import com.parrot.drone.groundsdk.stream.GsdkStreamView

class StreamingFragment : Fragment() {

    private lateinit var streamView: GsdkStreamView
    private lateinit var takeOffLandBt: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_streaming, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize views using findViewById
        streamView = view.findViewById(R.id.stream_view)
        takeOffLandBt = view.findViewById(R.id.takeOffLandBt)

        takeOffLandBt.setOnClickListener {
            // Handle take-off/land button click here
        }
    }

    fun updateStream(stream: CameraLive?) {
        // Update the stream view with the new stream
        streamView.setStream(stream)
    }
}
