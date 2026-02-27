package com.example.proyecto

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import java.io.File
import com.example.proyecto.domain.FakeMushroomClassifier
import com.example.proyecto.domain.HistoryItem
import com.example.proyecto.domain.HistoryStore

class CameraFragment : Fragment(R.layout.fragment_camera) {

    private var imageCapture: ImageCapture? = null
    private val requestCode = 10

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (hasCameraPermission()) startCamera()
        else requestPermissions(arrayOf(Manifest.permission.CAMERA), requestCode)

        view.findViewById<ImageButton>(R.id.btnShutter).setOnClickListener {
            takePhotoAndAnalyze()
        }
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.requestCode &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also { p ->
                val pv = requireView().findViewById<androidx.camera.view.PreviewView>(R.id.previewView)
                p.surfaceProvider = pv.surfaceProvider
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e("CameraFragment", "Error iniciando cámara", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhotoAndAnalyze() {
        val capture = imageCapture ?: return
        val tv = requireView().findViewById<TextView>(R.id.tvResult)

        val photoFile = File(requireContext().cacheDir, "tmp_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                    // 1) "Clasificar" (fake por ahora)
                    val (label, confidence) = FakeMushroomClassifier.classify()

                    // 2) Mostrar resultado en pantalla
                    tv.text = "Resultado: $label (${(confidence * 100).toInt()}%)"

                    // 3) Guardar en historial (sin guardar la foto)
                    HistoryStore.items.add(
                        HistoryItem(
                            label = label,
                            confidence = confidence,
                            timestamp = System.currentTimeMillis()
                        )
                    )

                    // 4) Borrar archivo temporal
                    photoFile.delete()
                }


                override fun onError(exception: ImageCaptureException) {
                    tv.text = "Error al capturar ❌"
                    Log.e("CameraFragment", "Error tomando foto", exception)
                }
            }
        )
    }
}
