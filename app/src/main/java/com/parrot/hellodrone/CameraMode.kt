package com.parrot.hellodrone

import android.view.View
import android.widget.RadioButton
import com.parrot.drone.groundsdk.Ref
import com.parrot.drone.groundsdk.device.Drone  
import com.parrot.drone.groundsdk.device.peripheral.MainCamera
import com.parrot.drone.groundsdk.device.peripheral.camera2.Camera

/**
 * Sample code to display and change camera mode, using `MainCamera` and `camera2.MainCamera`
 * peripherals (respectively Camera1 API and Camera2 API).
 *
 * Camera mode indicates if the camera is configured either to take photos or to record videos.
 */
class CameraMode(
    /** Photo mode button. */
    private val photoModeBt: RadioButton,
    /** Recording mode button. */
    private val recordingModeBt: RadioButton
) {
    /** Reference to `MainCamera` peripheral. */
    private var mainCameraRef: Ref<MainCamera>? = null
    /** Reference to `camera2.MainCamera` peripheral. */
    private var mainCamera2Ref: Ref<com.parrot.drone.groundsdk.device.peripheral.camera2.MainCamera>? = null

    init {
        // Setup buttons listeners.
        photoModeBt.setOnClickListener { view -> onModeButtonClicked(view) }
        recordingModeBt.setOnClickListener { view -> onModeButtonClicked(view) }
    }

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
     * Resets camera mode display.
     */
    private fun resetView() {
        photoModeBt.isEnabled = false
        recordingModeBt.isEnabled = false
    }

    /**
     * Updates camera mode display with `MainCamera` peripheral (Camera1 API).
     *
     * @param camera camera peripheral
     */
    private fun updateViewCamera1(camera: MainCamera) {
        // Enable camera mode buttons if mode is not currently changing.
        photoModeBt.isEnabled = !camera.mode().isUpdating
        recordingModeBt.isEnabled = !camera.mode().isUpdating
        // Update photo/recording mode radio button based on current camera mode setting.
        photoModeBt.isChecked = camera.mode().value == com.parrot.drone.groundsdk.device.peripheral.camera.Camera.Mode.PHOTO
        recordingModeBt.isChecked = camera.mode().value == com.parrot.drone.groundsdk.device.peripheral.camera.Camera.Mode.RECORDING
    }

    /**
     * Updates camera mode display with `camera2.MainCamera` peripheral (Camera2 API).
     *
     * @param camera camera peripheral
     */
    private fun updateViewCamera2(camera: Camera) {
        // Enable camera mode buttons if configuration is not currently changing.
        photoModeBt.isEnabled = !camera.config.updating
        recordingModeBt.isEnabled = !camera.config.updating
        // Update photo/recording mode radio button based on current camera configuration.
        photoModeBt.isChecked = camera.config[Camera.Config.MODE].value == Camera.Mode.PHOTO
        recordingModeBt.isChecked = camera.config[Camera.Config.MODE].value == Camera.Mode.RECORDING
    }

    /**
     * Sets camera mode with `MainCamera` peripheral (Camera1 API).
     *
     * @param camera camera peripheral
     * @param mode new camera mode
     */
    private fun setModeCamera1(camera: MainCamera,
                               mode: com.parrot.drone.groundsdk.device.peripheral.camera.Camera.Mode) {
        // To change camera mode with `MainCamera` peripheral, we first get the `mode` setting.
        // Then we set the new setting value.
        // If the drone is connected, this will immediately send this new setting value to the
        // drone.
        camera.mode().value = mode
    }

    /**
     * Sets camera mode with `camera2.MainCamera` peripheral (Camera2 API).
     *
     * @param camera camera peripheral
     * @param mode new camera mode
     */
    private fun setModeCamera2(camera: Camera, mode: Camera.Mode) {
        // To change camera mode with `camera2.MainCamera` peripheral, we use the configuration editor.
        // Create a configuration editor, starting from current configuration.
        val configEditor = camera.config.edit(fromScratch = false)
        // Set the value of the camera mode parameter.
        // Note: In case of conflicts with other parameters, the editor may automatically unset the
        // other conflicting parameters, so that the configuration remains consistent.
        configEditor[Camera.Config.MODE].value = mode
        // Automatically complete the edited configuration, to ensure that all parameters are set.
        configEditor.autoComplete()
        // Apply and send the new configuration to the drone, if the drone is connected.
        configEditor.commit()
    }


    /**
     * Called when camera mode button is clicked.
     */
    private fun onModeButtonClicked(view: View) {
        if (view is RadioButton && view.isChecked) {

            // Get `MainCamera` peripheral from its reference, if available.
            mainCameraRef?.get()?.let { camera ->
                // Set camera mode.
                setModeCamera1(camera, when (view.id) {
                    R.id.photoMode -> com.parrot.drone.groundsdk.device.peripheral.camera.Camera.Mode.PHOTO
                    else -> com.parrot.drone.groundsdk.device.peripheral.camera.Camera.Mode.RECORDING
                })
            } ?:

            // Otherwise, get `camera2.MainCamera` peripheral from its reference, if available.
            mainCamera2Ref?.get()?.let { camera ->
                // Set camera mode.
                setModeCamera2(camera, when (view.id) {
                    R.id.photoMode -> Camera.Mode.PHOTO
                    else -> Camera.Mode.RECORDING
                })
            }
        }
    }
}