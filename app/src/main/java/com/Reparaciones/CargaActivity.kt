package com.Reparaciones

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID // Necesario para generar IDs offline

class CargaActivity : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etAuto: EditText
    private lateinit var etFecha: EditText
    private lateinit var etTelefono: EditText
    private lateinit var cbDelanteroDerecho: CheckBox
    private lateinit var cbDelanteroIzquierdo: CheckBox
    private lateinit var cbTraseroDerecho: CheckBox
    private lateinit var cbTraseroIzquierdo: CheckBox
    private lateinit var etCosto: EditText
    private lateinit var btnGuardar: Button
    private lateinit var btnCerrar: Button

    private var reparacionEditando: Reparacion? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carga)

        // Inicializar vistas
        etNombre = findViewById(R.id.nombre)
        etApellido = findViewById(R.id.apellido)
        etAuto = findViewById(R.id.auto)
        etFecha = findViewById(R.id.etFecha)
        etTelefono = findViewById(R.id.etTelefono)
        cbDelanteroDerecho = findViewById(R.id.delanteroDerecho)
        cbDelanteroIzquierdo = findViewById(R.id.delanteroIzquierdo)
        cbTraseroDerecho = findViewById(R.id.traseroDerecho)
        cbTraseroIzquierdo = findViewById(R.id.traseroIzquierdo)
        etCosto = findViewById(R.id.costo)
        btnGuardar = findViewById(R.id.btnGuardar)
        btnCerrar = findViewById(R.id.btnCerrar)

        // Configuración de Fecha
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        etFecha.setText(dateFormat.format(calendar.time))

        etFecha.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    calendar.set(selectedYear, selectedMonth, selectedDay)
                    etFecha.setText(dateFormat.format(calendar.time))
                },
                year, month, day
            )
            datePickerDialog.show()
        }

        // --- Cargar datos si es edición ---
        @Suppress("DEPRECATION")
        reparacionEditando = intent.getSerializableExtra("reparacion_obj") as? Reparacion

        reparacionEditando?.let { rep ->
            etNombre.setText(rep.nombre)
            etApellido.setText(rep.apellido)
            etAuto.setText(rep.auto)
            etFecha.setText(rep.fecha)
            etTelefono.setText(rep.telefono)

            // Lógica para marcar los checkboxes desde un String (separado por comas)
            // Ejemplo de string en DB: "Delantero Derecho,Trasero Izquierdo"
            val listaAmortiguadores = rep.amortiguadores.split(",")

            cbDelanteroDerecho.isChecked = listaAmortiguadores.any { it.trim() == "Delantero Derecho" }
            cbDelanteroIzquierdo.isChecked = listaAmortiguadores.any { it.trim() == "Delantero Izquierdo" }
            cbTraseroDerecho.isChecked = listaAmortiguadores.any { it.trim() == "Trasero Derecho" }
            cbTraseroIzquierdo.isChecked = listaAmortiguadores.any { it.trim() == "Trasero Izquierdo" }

            etCosto.setText(String.format(Locale.US, "%.2f", rep.costoFinal))
        }

        // Listeners
        btnGuardar.setOnClickListener {
            validarYGuardar()
        }

        btnCerrar.setOnClickListener {
            finish()
        }
    }

    private fun validarYGuardar() {
        val nombre = etNombre.text.toString().trim()
        val apellido = etApellido.text.toString().trim()
        val auto = etAuto.text.toString().trim()
        val fecha = etFecha.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()
        val costoFinalStr = etCosto.text.toString().trim()

        if (nombre.isEmpty() || apellido.isEmpty() || auto.isEmpty() || fecha.isEmpty() || telefono.isEmpty() || costoFinalStr.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show()
            return
        }

        val costoFinal = costoFinalStr.toDoubleOrNull()
        if (costoFinal == null) {
            Toast.makeText(this, "El costo debe ser un número válido.", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Recopilar Checkboxes en una lista
        val amortiguadoresLista = mutableListOf<String>()
        if (cbDelanteroDerecho.isChecked) amortiguadoresLista.add("Delantero Derecho")
        if (cbDelanteroIzquierdo.isChecked) amortiguadoresLista.add("Delantero Izquierdo")
        if (cbTraseroDerecho.isChecked) amortiguadoresLista.add("Trasero Derecho")
        if (cbTraseroIzquierdo.isChecked) amortiguadoresLista.add("Trasero Izquierdo")

        // 2. Convertir lista a String para Room (separado por comas)
        val amortiguadoresString = amortiguadoresLista.joinToString(",")

        // 3. Definir el ID
        // Si estamos editando, usamos el ID que ya tenía. Si es nuevo, generamos uno.
        val idFinal = reparacionEditando?.id ?: UUID.randomUUID().toString()

        val reparacionAGuardar = Reparacion(
            id = idFinal,
            nombre = nombre,
            apellido = apellido,
            fecha = fecha,
            auto = auto,
            amortiguadores = amortiguadoresString, // Pasamos el String, no la lista
            costoFinal = costoFinal,
            telefono = telefono,
            esSincronizado = false // IMPORTANTE: Marcamos que aún no se ha subido a la nube
        )

        guardarEnBaseDeDatosLocal(reparacionAGuardar)
    }

    private fun guardarEnBaseDeDatosLocal(reparacion: Reparacion) {
        lifecycleScope.launch {
            try {
                btnGuardar.isEnabled = false // Evitar doble click

                // Llamada a Room (sincrona dentro de la corrutina)
                val database = AppDatabase.getDatabase(applicationContext)
                database.reparacionDao().guardar(reparacion)

                Toast.makeText(this@CargaActivity, "Guardado localmente.", Toast.LENGTH_SHORT).show()
                finish() // Volver a MainActivity

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@CargaActivity, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                btnGuardar.isEnabled = true
            }
        }
    }
}