package com.parrot.hellodrone

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.parrot.drone.groundsdk.GroundSdk
import com.parrot.drone.groundsdk.ManagedGroundSdk
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.DeviceState
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.drone.groundsdk.device.RemoteControl
import com.parrot.drone.groundsdk.device.peripheral.StreamServer
import com.parrot.drone.groundsdk.device.peripheral.stream.CameraLive
import com.parrot.drone.groundsdk.facility.AutoConnection
import com.parrot.drone.groundsdk.stream.GsdkStreamView
import com.parrot.drone.groundsdk.stream.GsdkStreamView.PADDING_FILL_BLUR_CROP

class VideoFragment : Fragment() {

    /** GroundSdk instance. */
    private lateinit var groundSdk: GroundSdk

    // Drone:
    /** Current drone instance. */
    private var drone: Drone? = null
    /** Reference to the current drone state. */
    private var droneStateRef: Ref<DeviceState>? = null
    /** Reference to the current drone stream server Peripheral. */
    private var streamServerRef: Ref<StreamServer>? = null
    /** Reference to the current drone live stream. */
    private var liveStreamRef: Ref<CameraLive>? = null
    /** Current drone live stream. */
    private var liveStream: CameraLive? = null

    // Remote control:
    /** Current remote control instance. */
    private var rc: RemoteControl? = null
    /** Reference to the current remote control state. */
    private var rcStateRef: Ref<DeviceState>? = null

    // User interface:
    /** Video stream view. */
    private lateinit var streamView: GsdkStreamView
    /** Drone state text view. */
    private lateinit var droneStateTxt: TextView
    /** Remote state text view. */
    private lateinit var rcStateTxt: TextView

    // Delegates to manage camera user interface:
    /** Delegate to display camera active state. */
    private lateinit var activeState: ActiveState
    /** Delegate to display and change camera mode. */
    private lateinit var cameraMode: CameraMode
    /** Delegate to manage start and stop photo capture and video recording button. */
    private lateinit var startStop: StartStop
    /** Delegate to display and change custom white balance temperature. */
    private lateinit var whiteBalanceTemperature: WhiteBalanceTemperature

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_video, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize user interface components.
        streamView = view.findViewById(R.id.stream_view)
        droneStateTxt = view.findViewById(R.id.droneStateTxt)
        rcStateTxt = view.findViewById(R.id.rcStateTxt)

        // Initialize delegates with their respective views.
        activeState = ActiveState(view.findViewById(R.id.activeTxt))
        cameraMode = CameraMode(view.findViewById(R.id.photoMode), view.findViewById(R.id.recordingMode))
        startStop = StartStop(view.findViewById(R.id.startStopBtn))
        whiteBalanceTemperature = WhiteBalanceTemperature(view.findViewById(R.id.whiteBalanceSpinner))

        // Initialize default values.
        droneStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
        rcStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
        streamView.paddingFill = PADDING_FILL_BLUR_CROP

        // Get a GroundSdk session.
        groundSdk = ManagedGroundSdk.obtainSession(requireContext() as Activity)
    }

    override fun onStart() {
        super.onStart()

        // Monitor the auto connection facility.
        groundSdk.getFacility(AutoConnection::class.java) {
            it?.let {
                // Start auto connection.
                if (it.status != AutoConnection.Status.STARTED) {
                    it.start()
                }

                // If the drone has changed.
                if (drone?.uid != it.drone?.uid) {
                    if (drone != null) {
                        // Stop monitoring the previous drone.
                        stopDroneMonitors()
                        // Reset user interface drone part.
                        resetDroneUi()
                    }

                    // Monitor the new drone.
                    drone = it.drone
                    if (drone != null) {
                        startDroneMonitors()
                    }
                }

                // If the remote control has changed.
                if (rc?.uid != it.remoteControl?.uid) {
                    if (rc != null) {
                        // Stop monitoring the previous remote.
                        stopRcMonitors()
                        // Reset user interface Remote part.
                        resetRcUi()
                    }

                    // Monitor the new remote.
                    rc = it.remoteControl
                    if (rc != null) {
                        startRcMonitors()
                    }
                }
            }
        }
    }

    /**
     * Resets drone user interface part.
     */
    private fun resetDroneUi() {
        // Reset drone user interface views.
        droneStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
        // Stop rendering the stream.
        streamView.setStream(null)
    }

    /**
     * Starts drone monitors.
     */
    private fun startDroneMonitors() {
        // Monitor drone state.
        monitorDroneState()
        // Start video stream.
        startVideoStream()
        // Start monitoring by camera user interface delegates.
        drone?.let { drone ->
            activeState.startMonitoring(drone)
            cameraMode.startMonitoring(drone)
            startStop.startMonitoring(drone)
            whiteBalanceTemperature.startMonitoring(drone)
        }
    }

    /**
     * Stops drone monitors.
     */
    private fun stopDroneMonitors() {
        // Close all references linked to the current drone to stop their monitoring.
        droneStateRef?.close()
        droneStateRef = null
        liveStreamRef?.close()
        liveStreamRef = null
        streamServerRef?.close()
        streamServerRef = null
        liveStream = null
        // Stop monitoring by camera user interface delegates.
        activeState.stopMonitoring()
        cameraMode.stopMonitoring()
        startStop.stopMonitoring()
        whiteBalanceTemperature.stopMonitoring()
    }

    /**
     * Monitor current drone state.
     */
    private fun monitorDroneState() {
        // Monitor current drone state.
        droneStateRef = drone?.getState {
            it?.let {
                // Update drone connection state view.
                droneStateTxt.text = it.connectionState.toString()
            }
        }
    }

    /**
     * Resets remote user interface part.
     */
    private fun resetRcUi() {
        // Reset remote control user interface views.
        rcStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
    }

    /**
     * Starts remote control monitors.
     */
    private fun startRcMonitors() {
        // Monitor remote state.
        monitorRcState()
    }

    /**
     * Stops remote control monitors.
     */
    private fun stopRcMonitors() {
        // Close all references linked to the current remote to stop their monitoring.
        rcStateRef?.close()
        rcStateRef = null
    }

    /**
     * Monitor current remote control state.
     */
    private fun monitorRcState() {
        // Monitor current drone state.
        rcStateRef = rc?.getState {
            it?.let {
                // Update remote connection state view.
                rcStateTxt.text = it.connectionState.toString()
            }
        }
    }

    /**
     * Starts the video stream.
     */
    private fun startVideoStream() {
        // Monitor the stream server.
        streamServerRef = drone?.getPeripheral(StreamServer::class.java) { streamServer ->
            streamServer?.run {
                // Enable Streaming.
                if (!streamingEnabled()) {
                    enableStreaming(true)
                }
                // Monitor the live stream.
                if (liveStreamRef == null) {
                    liveStreamRef = live { stream ->
                        if (stream != null) {
                            if (liveStream == null) {
                                // It is a new live stream.
                                streamView.setStream(stream)
                            }
                            // Play the live stream.
                            if (stream.playState() != CameraLive.PlayState.PLAYING) {
                                stream.play()
                            }
                        } else {
                            // Stop rendering the stream.
                            streamView.setStream(null)
                        }
                        // Keep the live stream to know if it is a new one or not.
                        liveStream = stream                    }
                }
            } ?: run {
                // Stop monitoring the live stream.
                liveStreamRef?.close()
                liveStreamRef = null
                // Stop rendering the stream.
                streamView.setStream(null)
            }
        }
    }

    override fun onStop() {
        super.onStop()

        // Stop monitoring when fragment is not visible.
        stopDroneMonitors()
        stopRcMonitors()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Clean up resources when the view is destroyed.
        liveStreamRef?.close()
        streamServerRef?.close()
        droneStateRef?.close()
        rcStateRef?.close()
    }
}

