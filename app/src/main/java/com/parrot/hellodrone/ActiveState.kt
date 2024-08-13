package com.parrot.hellodrone

import android.widget.TextView
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.Drone
import com.parrot.drone.groundsdk.device.peripheral.MainCamera
import com.parrot.drone.groundsdk.device.peripheral.camera2.Camera

/**
 * Sample code to display the active state of a camera, using `MainCamera` and `camera2.MainCamera`
 * peripherals (respectively Camera1 API and Camera2 API).
 *
 * When the camera is inactive, most features are unavailable, like taking pictures,
 * video recording, zoom control. However, it is possible to configure camera parameters.
 */
class ActiveState(
    /** Active state text view. */
    private val activeTxt: TextView
) {
    /** Reference to `MainCamera` peripheral. */
    private var mainCameraRef: Ref<MainCamera>? = null
    /** Reference to `camera2.MainCamera` peripheral. */
    private var mainCamera2Ref: Ref<com.parrot.drone.groundsdk.device.peripheral.camera2.MainCamera>? = null

    /**
     * Starts camera peripherals monitoring.
     *
     * @param drone drone to monitor
     */
    fun startMonitoring(drone: Drone) {

        // Drones: ANAFI_4K, ANAFI_THERMAL, ANAFI_USA
        // Monitor `MainCamera` peripheral, for drones supporting Camera1 API.
        // We keep camera reference as a class property, otherwise change notifications would stop.
        mainCameraRef = drone.getPeripheral(MainCamera::class.java) { camera ->
            // Called when the camera changes, on main thread.
            camera?.let {
                updateViewCamera1(camera)
            } ?: run {
                resetView()
            }
        }

        // Drones: ANAFI_2
        // Monitor `camera2.MainCamera` peripheral, for drones supporting Camera2 API.
        // We keep camera reference as a class property, otherwise change notifications would stop.
        mainCamera2Ref = drone.getPeripheral(com.parrot.drone.groundsdk.device.peripheral.camera2.MainCamera::class.java) { camera ->
            // Called when the camera changes, on main thread.
            camera?.let {
                updateViewCamera2(camera)
            } ?: run {
                resetView()
            }
        }
    }

    /**
     * Stops camera peripherals monitoring.
     */
    fun stopMonitoring() {
        // Release `MainCamera` peripheral reference.
        mainCameraRef?.close()
        mainCameraRef = null

        // Release `camera2.MainCamera` peripheral reference.
        mainCamera2Ref?.close()
        mainCamera2Ref = null

        resetView()
    }

    /**
     * Resets active state display.
     */
    private fun resetView() {
        activeTxt.text = ""
    }

    /**
     * Updates active state display with `MainCamera` peripheral (Camera1 API).
     *
     * @param camera camera peripheral
     */
    private fun updateViewCamera1(camera: MainCamera) {
        // Display whether the camera is active.
        activeTxt.text = camera.isActive.toString()
    }

    /**
     * Updates active state display with `camera2.MainCamera` peripheral (Camera2 API).
     *
     * @param camera camera peripheral
     */
    private fun updateViewCamera2(camera: Camera) {
        // Display whether the camera is active.
        activeTxt.text = camera.active.toString()
    }
}