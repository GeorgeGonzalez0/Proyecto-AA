package com.example.proyecto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.proyecto.domain.ClassificationInput
import com.example.proyecto.domain.MushroomClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * SearchFragment - Pantalla de clasificación manual por variables.
 *
 * El usuario ingresa las 4 categorías de variables y presiona
 * "Clasificar" para obtener la familia biológica predicha.
 */
class SearchFragment : Fragment() {

    private val classifier = MushroomClassifier()

    // ── Referencias a los campos del formulario ────────────────
    // Variables morfológicas
    private lateinit var etSporeSize:       EditText
    private lateinit var spinnerSporeShape: Spinner
    private lateinit var spinnerWallType:   Spinner
    private lateinit var spinnerOrnam:      Spinner

    // Variables genéticas
    private lateinit var spinnerGeneITS:        Spinner
    private lateinit var spinnerGeneticCluster: Spinner
    private lateinit var etGcContent:           EditText

    // Variables ecológicas
    private lateinit var spinnerHabitat:    Spinner
    private lateinit var etElevation:       EditText
    private lateinit var etMeanTemp:        EditText

    // Variables fisicoquímicas
    private lateinit var etPH:              EditText
    private lateinit var etConductividad:   EditText
    private lateinit var etNitrogeno:       EditText
    private lateinit var radioTextura:      RadioGroup

    // Resultados
    private lateinit var btnClasificar:     Button
    private lateinit var tvResultado:       TextView
    private lateinit var progressBar:       ProgressBar
    private lateinit var cardResultado:     View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // NOTA: Debes crear el layout fragment_search.xml con estos campos.
        // Ver guía de integración para el XML completo.
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupSpinners()
        setupListeners()
    }

    private fun initViews(view: View) {
        etSporeSize         = view.findViewById(R.id.et_spore_size)
        spinnerSporeShape   = view.findViewById(R.id.spinner_spore_shape)
        spinnerWallType     = view.findViewById(R.id.spinner_wall_type)
        spinnerOrnam        = view.findViewById(R.id.spinner_ornamentation)
        spinnerGeneITS      = view.findViewById(R.id.spinner_gene_its)
        spinnerGeneticCluster = view.findViewById(R.id.spinner_genetic_cluster)
        etGcContent         = view.findViewById(R.id.et_gc_content)
        spinnerHabitat      = view.findViewById(R.id.spinner_habitat)
        etElevation         = view.findViewById(R.id.et_elevation)
        etMeanTemp          = view.findViewById(R.id.et_mean_temp)
        etPH                = view.findViewById(R.id.et_ph)
        etConductividad     = view.findViewById(R.id.et_conductividad)
        etNitrogeno         = view.findViewById(R.id.et_nitrogeno)
        radioTextura        = view.findViewById(R.id.rg_textura)
        btnClasificar       = view.findViewById(R.id.btn_clasificar)
        tvResultado         = view.findViewById(R.id.tv_resultado)
        progressBar         = view.findViewById(R.id.progress_bar)
        cardResultado       = view.findViewById(R.id.card_resultado)
    }

    private fun setupSpinners() {
        // Forma del esporo
        setupSpinner(spinnerSporeShape, listOf("Redondo (0)", "Ovalado (1)", "Elongado (2)"))
        // Tipo de pared
        setupSpinner(spinnerWallType, listOf("Simple (0)", "Doble (1)", "Triple (2)"))
        // Ornamentación
        setupSpinner(spinnerOrnam, listOf("Lisa (0)", "Granulosa (1)", "Espinosa (2)", "Reticulada (3)"))
        // Gen ITS
        setupSpinner(spinnerGeneITS, listOf("Ausente (0)", "Presente (1)"))
        // Clúster genético
        setupSpinner(spinnerGeneticCluster, listOf("Clúster 0", "Clúster 1", "Clúster 2", "Clúster 3"))
        // Tipo de hábitat
        setupSpinner(spinnerHabitat, listOf("Bosque (0)", "Páramo (1)", "Matorral (2)", "Pastizal (3)"))
    }

    private fun setupSpinner(spinner: Spinner, items: List<String>) {
        spinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            items
        )
    }

    private fun setupListeners() {
        btnClasificar.setOnClickListener {
            if (validarCampos()) {
                clasificar()
            }
        }
    }

    private fun validarCampos(): Boolean {
        val campos = listOf(
            etSporeSize to "Tamaño del esporo",
            etGcContent to "Contenido GC",
            etElevation to "Altitud",
            etMeanTemp to "Temperatura",
            etPH to "pH",
            etConductividad to "Conductividad",
            etNitrogeno to "Nitrógeno"
        )
        for ((campo, nombre) in campos) {
            if (campo.text.toString().isBlank()) {
                campo.error = "Campo requerido"
                Toast.makeText(context, "Ingresa: $nombre", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        if (radioTextura.checkedRadioButtonId == -1) {
            Toast.makeText(context, "Selecciona el tipo de textura del suelo", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun clasificar() {
        btnClasificar.isEnabled = false
        progressBar.visibility = View.VISIBLE
        cardResultado.visibility = View.GONE

        // Leer textura seleccionada
        val (tArc, tAren, tFranc, tLimo) = when (radioTextura.checkedRadioButtonId) {
            R.id.rb_arcillosa -> listOf(1.0, 0.0, 0.0, 0.0)
            R.id.rb_arenosa   -> listOf(0.0, 1.0, 0.0, 0.0)
            R.id.rb_franca    -> listOf(0.0, 0.0, 1.0, 0.0)
            R.id.rb_limosa    -> listOf(0.0, 0.0, 0.0, 1.0)
            else              -> listOf(1.0, 0.0, 0.0, 0.0)
        }

        val input = ClassificationInput(
            sporeSizeUm        = etSporeSize.text.toString().toDouble(),
            sporeShape         = spinnerSporeShape.selectedItemPosition,
            wallType           = spinnerWallType.selectedItemPosition,
            ornamentation      = spinnerOrnam.selectedItemPosition,
            geneITS            = spinnerGeneITS.selectedItemPosition.toDouble(),
            geneticCluster     = spinnerGeneticCluster.selectedItemPosition,
            gcContent          = etGcContent.text.toString().toDouble(),
            habitatType        = spinnerHabitat.selectedItemPosition,
            elevationM         = etElevation.text.toString().toDouble(),
            meanTempC          = etMeanTemp.text.toString().toDouble(),
            pH                 = etPH.text.toString().toDouble(),
            conductividadDsM   = etConductividad.text.toString().toDouble(),
            nitrogenoTotalPct  = etNitrogeno.text.toString().toDouble(),
            texturaArcillosa   = tArc,
            texturaArenosa     = tAren,
            texturaFranca      = tFranc,
            texturaLimosa      = tLimo,
        )

        lifecycleScope.launch {
            // Llamar al servidor en hilo de red
            val result = withContext(Dispatchers.IO) {
                classifier.classify(input)
            }

            // Actualizar UI en hilo principal
            progressBar.visibility = View.GONE
            btnClasificar.isEnabled = true

            if (result.success) {
                cardResultado.visibility = View.VISIBLE
                val top3Text = result.top3?.joinToString("\n") {
                    "  ${it.first}: ${(it.second * 100).toInt()}%"
                } ?: ""

                tvResultado.text = buildString {
                    appendLine("🔬 FAMILIA PREDICHA:")
                    appendLine("   ${result.familia}")
                    appendLine()
                    appendLine("📊 Confianza: ${result.confianzaPct}")
                    appendLine()
                    appendLine("📋 Top 3 familias:")
                    appendLine(top3Text)
                }
            } else {
                Toast.makeText(
                    context,
                    "❌ Error: ${result.error}\n\n¿El servidor está corriendo?",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
