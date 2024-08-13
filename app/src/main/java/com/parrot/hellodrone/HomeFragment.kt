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
import com.parrot.drone.groundsdk.device.instrument.BatteryInfo
import com.parrot.drone.groundsdk.device.pilotingitf.ManualCopterPilotingItf
import com.parrot.drone.groundsdk.facility.AutoConnection

/**
 * Home Fragment to handle drone status, battery info, and flying time.
 */
class HomeFragment : Fragment() {

    /** GroundSdk instance. */
    private lateinit var groundSdk: GroundSdk

    // Drone:
    private var drone: Drone? = null
    private var droneStateRef: Ref<DeviceState>? = null
    private var droneBatteryInfoRef: Ref<BatteryInfo>? = null
    private var pilotingItfRef: Ref<ManualCopterPilotingItf>? = null

    // User Interface:
    private lateinit var droneStatusLabel: TextView
    private lateinit var droneStateTxt: TextView
    private lateinit var droneBatteryTxt: TextView
    private lateinit var flyingTimeLeftTxt: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize user interface components.
        droneStatusLabel = view.findViewById(R.id.droneStatusLabel)
        droneStateTxt = view.findViewById(R.id.droneStateTxt)
        droneBatteryTxt = view.findViewById(R.id.droneBatteryTxt)
        flyingTimeLeftTxt = view.findViewById(R.id.flyingTimeLeftTxt)

        // Initialize default values.
        droneStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
        droneBatteryTxt.text = "Battery: Unknown"
        flyingTimeLeftTxt.text = "Flying Time Left: Unknown"

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
            }
        }
    }

    private fun resetDroneUi() {
        droneStateTxt.text = DeviceState.ConnectionState.DISCONNECTED.toString()
        droneBatteryTxt.text = "Battery: Unknown"
        flyingTimeLeftTxt.text = "Flying Time Left: Unknown"
    }

    private fun startDroneMonitors() {
        monitorDroneState()
        monitorDroneBatteryChargeLevel()
        estimateFlyingTime()
    }

    private fun stopDroneMonitors() {
        droneStateRef?.close()
        droneStateRef = null

        droneBatteryInfoRef?.close()
        droneBatteryInfoRef = null

        pilotingItfRef?.close()
        pilotingItfRef = null
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
                estimateFlyingTime(it.charge)
            }
        }
    }

    private fun estimateFlyingTime(batteryCharge: Int? = null) {
        // For demonstration, assuming battery provides around 20 minutes of flight time at 100% charge.
        val fullBatteryFlightTime = 20 // minutes
        val batteryLevel = batteryCharge ?: 100 // Assume 100% if battery info is not available

        val remainingTime = (batteryLevel / 100.0 * fullBatteryFlightTime).toInt()
        val minutes = remainingTime
        val seconds = 0 // For simplicity, not considering seconds here

        flyingTimeLeftTxt.text = String.format("Flying Time Left: %02d:%02d", minutes, seconds)
    }

    override fun onStop() {
        super.onStop()
        stopDroneMonitors()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        droneStateRef?.close()
        droneBatteryInfoRef?.close()
        pilotingItfRef?.close()
    }
}
