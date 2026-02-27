package com.example.proyecto.domain

import android.util.Log
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * MushroomClassifier - Conecta con el servidor Flask para clasificar familias biológicas.
 *
 * CONFIGURACIÓN:
 *   1. Cambia BASE_URL con la IP de tu computadora (donde corre api_servidor.py)
 *   2. Asegúrate de que tu PC y tu celular estén en la misma red WiFi
 *   3. El servidor debe estar corriendo: python api_servidor.py
 */
class MushroomClassifier {

    companion object {
        // ⚠️ CAMBIA ESTA IP POR LA DE TU COMPUTADORA
        // Windows: cmd → ipconfig → IPv4 Address
        // Mac/Linux: terminal → ifconfig | grep inet
        private const val BASE_URL = "http://10.0.2.2:5000"
        private const val TAG = "MushroomClassifier"
    }

    /**
     * Realiza la predicción llamando al servidor Flask.
     * IMPORTANTE: Llama esto desde una corrutina o hilo secundario, NO desde el hilo principal.
     */
    fun classify(input: ClassificationInput): ClassificationResult {
        return try {
            val url = URL("$BASE_URL/predict")
            val conn = url.openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            // Construir JSON con los valores de entrada
            val body = input.toJson().toString()
            OutputStreamWriter(conn.outputStream).use { it.write(body) }

            val responseCode = conn.responseCode
            if (responseCode != 200) {
                Log.e(TAG, "Error HTTP: $responseCode")
                return ClassificationResult.error("Error del servidor: $responseCode")
            }

            val response = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(response)

            ClassificationResult.success(
                familia       = json.getString("familia_predicha"),
                confianza     = json.getDouble("confianza"),
                confianzaPct  = json.getString("confianza_pct"),
                top3          = parseTop3(json),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al clasificar: ${e.message}")
            ClassificationResult.error(e.message ?: "Error desconocido")
        }
    }

    private fun parseTop3(json: JSONObject): List<Pair<String, Double>> {
        val arr = json.optJSONArray("top3") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val item = arr.getJSONObject(i)
            item.getString("familia") to item.getDouble("probabilidad")
        }
    }

    /**
     * Verifica si el servidor está disponible.
     * Llama esto antes de clasificar para mostrar un mensaje claro al usuario.
     */
    fun isServerAvailable(): Boolean {
        return try {
            val url = URL("$BASE_URL/health")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3_000
            conn.readTimeout = 3_000
            conn.responseCode == 200
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Datos de entrada para la clasificación.
 * Corresponde exactamente a las columnas del dataset.
 */
data class ClassificationInput(
    // 🔬 Variables morfológicas del esporo
    val sporeSizeUm:        Double,  // Tamaño del esporo en µm (ej: 50.0 - 300.0)
    val sporeShape:         Int,     // Forma: 0=redondo, 1=ovalado, 2=elongado
    val wallType:           Int,     // Tipo de pared: 0=simple, 1=doble, 2=triple
    val ornamentation:      Int,     // Ornamentación: 0=lisa, 1=granulosa, 2=espinosa, 3=reticulada

    // 🧬 Variables genéticas
    val geneITS:            Double,  // Gen ITS: 0.0=ausente, 1.0=presente
    val geneticCluster:     Int,     // Clúster genético: 0-3
    val gcContent:          Double,  // Contenido GC en % (ej: 35.0 - 60.0)

    // 🌿 Variables ecológicas
    val habitatType:        Int,     // Hábitat: 0=bosque, 1=páramo, 2=matorral, 3=pastizal
    val elevationM:         Double,  // Altitud en metros (ej: 500.0 - 4200.0)
    val meanTempC:          Double,  // Temperatura media °C (ej: 3.0 - 25.0)

    // 🪨 Variables fisicoquímicas del suelo
    val pH:                 Double,  // pH normalizado (valor de StandardScaler)
    val conductividadDsM:   Double,  // Conductividad normalizada
    val nitrogenoTotalPct:  Double,  // Nitrógeno total normalizado

    // Textura del suelo (one-hot encoding: solo uno puede ser 1.0)
    val texturaArcillosa:   Double,  // 1.0 si es arcillosa, 0.0 si no
    val texturaArenosa:     Double,  // 1.0 si es arenosa
    val texturaFranca:      Double,  // 1.0 si es franca
    val texturaLimosa:      Double,  // 1.0 si es limosa
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("spore_size_um",        sporeSizeUm)
        put("spore_shape",          sporeShape)
        put("wall_type",            wallType)
        put("ornamentation",        ornamentation)
        put("gene_ITS",             geneITS)
        put("genetic_cluster",      geneticCluster)
        put("gc_content",           gcContent)
        put("habitat_type",         habitatType)
        put("elevation_m",          elevationM)
        put("mean_temp_c",          meanTempC)
        put("pH",                   pH)
        put("conductividad_ds_m",   conductividadDsM)
        put("nitrogeno_total_pct",  nitrogenoTotalPct)
        put("textura_Arcillosa",    texturaArcillosa)
        put("textura_Arenosa",      texturaArenosa)
        put("textura_Franca",       texturaFranca)
        put("textura_Limosa",       texturaLimosa)
    }
}

/**
 * Resultado de la clasificación.
 */
data class ClassificationResult(
    val success:      Boolean,
    val familia:      String?,
    val confianza:    Double?,
    val confianzaPct: String?,
    val top3:         List<Pair<String, Double>>?,
    val error:        String?,
) {
    companion object {
        fun success(
            familia: String,
            confianza: Double,
            confianzaPct: String,
            top3: List<Pair<String, Double>>,
        ) = ClassificationResult(true, familia, confianza, confianzaPct, top3, null)

        fun error(msg: String) =
            ClassificationResult(false, null, null, null, null, msg)
    }
}
