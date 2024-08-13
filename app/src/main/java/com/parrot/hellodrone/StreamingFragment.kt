package com.parrot.hellodrone

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.parrot.drone.groundsdk.GroundSdk
import com.parrot.drone.groundsdk.ManagedGroundSdk
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.DeviceState
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.drone.groundsdk.device.RemoteControl
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo
import com.parrot.drone.groundsdk.device.peripheral.StreamServer
import com.parrot.drone.groundsdk.device.peripheral.stream.CameraLive
import com.parrot.drone.groundsdk.device.pilotingitf.Activable
import com.parrot.drone.groundsdk.device.pilotingitf.ManualCopterPilotingItf
import com.parrot.drone.groundsdk.facility.AutoConnection
import com.parrot.drone.groundsdk.stream.GsdkStreamView

/**
 * Streaming Fragment to handle drone video streaming, battery info, and piloting interface.
 */
class StreamingFragment : Fragment() {

    /** GroundSdk instance. */
    private lateinit var groundSdk: GroundSdk

    // Drone:
    private var drone: Drone? = null
    private var droneStateRef: Ref<DeviceState>? = null
    private var droneBatteryInfoRef: Ref<BatteryInfo>? = null
    private var pilotingItfRef: Ref<ManualCopterPilotingItf>? = null
    private var streamServerRef: Ref<StreamServer>? = null
    private var liveStreamRef: Ref<CameraLive>? = null
    private var liveStream: CameraLive? = null

    // Remote control:
    private var rc: RemoteControl? = null
    private var rcStateRef: Ref<DeviceState>? = null
    private var rcBatteryInfoRef: Ref<BatteryInfo>? = null

    // User Interface:
    private lateinit var streamView: GsdkStreamView
    private lateinit var droneStateTxt: TextView
    private lateinit var droneBatteryTxt: TextView
    private lateinit var rcStateTxt: TextView
    private lateinit var rcBatteryTxt: TextView
    private lateinit var takeOffLandBt: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_streaming, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize user interface components.
        streamView = view.findViewById(R.id.stream_view)
        droneStateTxt = view.findViewById(R.id.droneStateTxt)
        droneBatteryTxt = view.findViewById(R.id.droneBatteryTxt)
        rcStateTxt = view.findViewById(R.id.rcStateTxt)
        rcBatteryTxt = view.findViewById(R.id.rcBatteryTxt)
        takeOffLandBt = view.findViewById(R.id.takeOffLandBt)
        takeOffLandBt.setOnClickListener { onTakeOffLandClick() }

        // Initialize default values.
        droneStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
        rcStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()

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
                        stopDroneMonitors()
                        resetDroneUi()
                    }

                    drone = it.drone
                    if (drone != null) {
                        startDroneMonitors()
                    }
                }

                // If the remote control has changed.
                if (rc?.uid != it.remoteControl?.uid) {
                    if (rc != null) {
                        stopRcMonitors()
                        resetRcUi()
                    }

                    rc = it.remoteControl
                    if (rc != null) {
                        startRcMonitors()
                    }
                }
            }
        }
    }

    private fun resetDroneUi() {
        droneStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
        droneBatteryTxt.text = ""
        takeOffLandBt.isEnabled = false
        streamView.setStream(null)
    }

    private fun startDroneMonitors() {
        monitorDroneState()
        monitorDroneBatteryChargeLevel()
        monitorPilotingInterface()
        startVideoStream()
    }

    private fun stopDroneMonitors() {
        droneStateRef?.close()
        droneStateRef = null

        droneBatteryInfoRef?.close()
        droneBatteryInfoRef = null

        pilotingItfRef?.close()
        pilotingItfRef = null

        liveStreamRef?.close()
        liveStreamRef = null

        streamServerRef?.close()
        streamServerRef = null

        liveStream = null
    }

    private fun startVideoStream() {
        streamServerRef = drone?.getPeripheral(StreamServer::class.java) { streamServer ->
            if (streamServer != null) {
                if (!streamServer.streamingEnabled()) {
                    streamServer.enableStreaming(true)
                }

                if (liveStreamRef == null) {
                    liveStreamRef = streamServer.live { liveStream ->
                        if (liveStream != null) {
                            if (this.liveStream == null) {
                                streamView.setStream(liveStream)
                            }

                            if (liveStream.playState() != CameraLive.PlayState.PLAYING) {
                                liveStream.play()
                            }
                        } else {
                            streamView.setStream(null)
                        }
                        this.liveStream = liveStream
                    }
                }
            } else {
                liveStreamRef?.close()
                liveStreamRef = null
                streamView.setStream(null)
            }
        }
    }

    private fun monitorDroneState() {
        droneStateRef = drone?.getState {
            it?.let {
                droneStateTxt.text = it.connectionState.toString()
            }
        }
    }

    private fun monitorDroneBatteryChargeLevel() {
        droneBatteryInfoRef = drone?.getInstrument(BatteryInfo::class.java) {
            it?.let {
                droneBatteryTxt.text = getString(R.string.percentage, it.charge)
            }
        }
    }

    private fun monitorPilotingInterface() {
        pilotingItfRef = drone?.getPilotingItf(ManualCopterPilotingItf::class.java) {
            if (it == null) {
                takeOffLandBt.isEnabled = false
            } else {
                managePilotingItfState(it)
            }
        }
    }

    private fun managePilotingItfState(itf: ManualCopterPilotingItf) {
        when (itf.state) {
            Activable.State.UNAVAILABLE -> takeOffLandBt.isEnabled = false
            Activable.State.IDLE -> {
                takeOffLandBt.isEnabled = false
                itf.activate()
            }
            Activable.State.ACTIVE -> {
                takeOffLandBt.isEnabled = itf.canTakeOff() || itf.canLand()
                takeOffLandBt.text = if (itf.canTakeOff()) {
                    getString(R.string.take_off)
                } else if (itf.canLand()) {
                    getString(R.string.land)
                } else {
                    takeOffLandBt.isEnabled = false
                    ""
                }
            }
        }
    }

    private fun onTakeOffLandClick() {
        pilotingItfRef?.get()?.let { itf ->
            when {
                itf.canTakeOff() -> itf.takeOff()
                itf.canLand() -> itf.land()
            }
        }
    }

    private fun resetRcUi() {
        rcStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
        rcBatteryTxt.text = ""
    }

    private fun startRcMonitors() {
        monitorRcState()
        monitorRcBatteryChargeLevel()
    }

    private fun stopRcMonitors() {
        rcStateRef?.close()
        rcStateRef = null

        rcBatteryInfoRef?.close()
        rcBatteryInfoRef = null
    }

    private fun monitorRcState() {
        rcStateRef = rc?.getState {
            it?.let {
                rcStateTxt.text = it.connectionState.toString()
            }
        }
    }

    private fun monitorRcBatteryChargeLevel() {
        rcBatteryInfoRef = rc?.getInstrument(BatteryInfo::class.java) {
            it?.let {
                rcBatteryTxt.text = getString(R.string.percentage, it.charge)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        stopDroneMonitors()
        stopRcMonitors()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        liveStreamRef?.close()
        streamServerRef?.close()
        droneStateRef?.close()
        droneBatteryInfoRef?.close()
        pilotingItfRef?.close()
        rcStateRef?.close()
        rcBatteryInfoRef?.close()
    }
}
