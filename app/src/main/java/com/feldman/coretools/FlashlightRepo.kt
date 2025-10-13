// FlashlightRepo.kt
package com.feldman.coretools

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

object FlashlightRepo {
    @Volatile private var inited = false
    @Volatile private var cm: CameraManager? = null
    @Volatile private var cameraId: String? = null
    @Volatile private var maxLevelCached: Int = 1

    // hot, in-memory state (no disk read during tile)
    private val _isOn = AtomicBoolean(false)
    private val _level = AtomicInteger(0)

    val isOn: Boolean get() = _isOn.get()
    val level: Int get() = _level.get()
    val maxLevel: Int get() = maxLevelCached

    fun init(context: Context) {
        if (inited) return
        synchronized(this) {
            if (inited) return
            val mgr = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cm = mgr

            // Resolve camera once (slow on some OEMs → do it here, not in tile)
            val id = mgr.cameraIdList.firstOrNull {
                val ch = mgr.getCameraCharacteristics(it)
                ch.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                        && ch.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: mgr.cameraIdList.firstOrNull {
                mgr.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            cameraId = id

            // Cache max strength once
            maxLevelCached = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && id != null) {
                mgr.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
            } else 1

            // Single process-wide callback keeps hot state fresh
            mgr.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(camId: String, enabled: Boolean) {
                    if (camId == id || id == null) {
                        _isOn.set(enabled)
                        if (!enabled) _level.set(0)
                        else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && _level.get() == 0) {
                            _level.set(1)
                        }
                    }
                }
                override fun onTorchStrengthLevelChanged(camId: String, torchStrength: Int) {
                    if (camId == id || id == null) {
                        _isOn.set(torchStrength > 0)
                        _level.set(torchStrength.coerceAtLeast(0))
                    }
                }
            }, null)

            inited = true
        }
    }

    fun toggle(context: Context) {
        val id = cameraId ?: return
        val mgr = cm ?: return
        val turningOn = !_isOn.get()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (turningOn) {
                val target = (if (level > 0) level else maxOf(1, (maxLevel * 0.5f).toInt()))
                mgr.turnOnTorchWithStrengthLevel(id, target)
            } else {
                mgr.setTorchMode(id, false)
            }
        } else {
            mgr.setTorchMode(id, turningOn)
        }
        // Optimistic hot state (callback will confirm)
        _isOn.set(turningOn)
        _level.set(if (turningOn) (if (level > 0) level else 1) else 0)
    }

    fun setLevel(context: Context, newLevel: Int) {
        val id = cameraId ?: return
        val mgr = cm ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (newLevel <= 0) mgr.setTorchMode(id, false)
            else mgr.turnOnTorchWithStrengthLevel(id, newLevel)
        } else {
            mgr.setTorchMode(id, newLevel > 0)
        }
        _isOn.set(newLevel > 0)
        _level.set(newLevel.coerceAtLeast(0))
    }
}
