package com.Reparaciones // Asegúrate de que este sea tu paquete real

import Reparacion
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID // Asegúrate de importar UUID

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

    private var reparacionEditando: Reparacion? = null // Para guardar la reparación que se está editando

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

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // --- Manejo de la fecha ---
        etFecha.setText(dateFormat.format(calendar.time)) // Precarga con fecha actual por defecto

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
                year,
                month,
                day
            )
            datePickerDialog.show()
        }
        // --- Fin manejo de la fecha ---

        // --- Recibir datos de la reparación si se está editando ---
        val reparacionJson = intent.getStringExtra("reparacion_json")
        if (reparacionJson != null) {
            val gson = Gson()
            reparacionEditando = gson.fromJson(reparacionJson, Reparacion::class.java)
            // Precargar campos con los datos de la reparación existente
            reparacionEditando?.let { rep ->
                etNombre.setText(rep.nombre)
                etApellido.setText(rep.apellido)
                etAuto.setText(rep.auto)
                etFecha.setText(rep.fecha) // Usa la fecha de la reparación
                etTelefono.setText(rep.telefono)
                cbDelanteroDerecho.isChecked = rep.amortiguadores.contains("Delantero Derecho")
                cbDelanteroIzquierdo.isChecked = rep.amortiguadores.contains("Delantero Izquierdo")
                cbTraseroDerecho.isChecked = rep.amortiguadores.contains("Trasero Derecho")
                cbTraseroIzquierdo.isChecked = rep.amortiguadores.contains("Trasero Izquierdo")
                etCosto.setText(String.format(Locale.getDefault(), "%.2f", rep.costoFinal))
            }
        }

        // Asignar listeners
        btnGuardar.setOnClickListener {
            guardarReparacion()
        }

        btnCerrar.setOnClickListener {
            finish()
        }
    }

    private fun guardarReparacion() {
        val nombre = etNombre.text.toString().trim()
        val apellido = etApellido.text.toString().trim()
        val auto = etAuto.text.toString().trim()
        val fecha = etFecha.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()
        val costoFinalStr = etCosto.text.toString().trim()

        if (nombre.isEmpty() || apellido.isEmpty() || auto.isEmpty() || fecha.isEmpty() || telefono.isEmpty() || costoFinalStr.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos obligatorios.", Toast.LENGTH_SHORT).show()
            return
        }

        val costoFinal = costoFinalStr.toDoubleOrNull()
        if (costoFinal == null) {
            Toast.makeText(this, "El costo final debe ser un número válido.", Toast.LENGTH_SHORT).show()
            return
        }

        val amortiguadoresReparados = mutableListOf<String>()
        if (cbDelanteroDerecho.isChecked) amortiguadoresReparados.add("Delantero Derecho")
        if (cbDelanteroIzquierdo.isChecked) amortiguadoresReparados.add("Delantero Izquierdo")
        if (cbTraseroDerecho.isChecked) amortiguadoresReparados.add("Trasero Derecho")
        if (cbTraseroIzquierdo.isChecked) amortiguadoresReparados.add("Trasero Izquierdo")

        // Crea el nuevo objeto Reparacion (o actualizado)
        val nuevaReparacion = Reparacion(
            id = reparacionEditando?.id ?: UUID.randomUUID().toString(), // Mantiene el ID si edita, crea uno nuevo si es nueva
            nombre = nombre,
            apellido = apellido,
            fecha = fecha,
            auto = auto,
            amortiguadores = amortiguadoresReparados,
            costoFinal = costoFinal,
            telefono = telefono
        )

        val gson = Gson()
        val reparacionesEnArchivo = mutableListOf<Reparacion>()

        // 1. Leer todas las reparaciones existentes
        try {
            val fileInputStream = openFileInput("reparaciones.txt")
            val inputStreamReader = InputStreamReader(fileInputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                val rep: Reparacion = gson.fromJson(line, Reparacion::class.java)
                reparacionesEnArchivo.add(rep)
            }
            bufferedReader.close()
        } catch (e: Exception) {
            // No pasa nada si el archivo no existe o está vacío, simplemente la lista estará vacía
        }

        // 2. Modificar la lista según si es edición o nueva
        if (reparacionEditando != null) { // Es una edición
            val index = reparacionesEnArchivo.indexOfFirst { it.id == reparacionEditando?.id }
            if (index != -1) {
                reparacionesEnArchivo[index] = nuevaReparacion // Reemplaza la reparación existente
                Toast.makeText(this, "Reparación actualizada correctamente", Toast.LENGTH_SHORT).show()
            } else {
                // Esto no debería pasar si el ID funciona bien, pero por seguridad
                reparacionesEnArchivo.add(nuevaReparacion) // Si no se encuentra, la añade como nueva
                Toast.makeText(this, "Reparación no encontrada, se añadió como nueva.", Toast.LENGTH_SHORT).show()
            }
        } else { // Es una nueva reparación
            reparacionesEnArchivo.add(nuevaReparacion)
            Toast.makeText(this, "Reparación guardada correctamente", Toast.LENGTH_SHORT).show()
        }

        // 3. Volver a escribir todo el archivo con la lista actualizada
        try {
            val fileOutputStream = openFileOutput("reparaciones.txt", MODE_PRIVATE) // Usamos MODE_PRIVATE para SOBRESCRIBIR
            val outputStreamWriter = OutputStreamWriter(fileOutputStream)
            for (rep in reparacionesEnArchivo) {
                outputStreamWriter.append(gson.toJson(rep)).append("\n")
            }
            outputStreamWriter.close()
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al guardar/actualizar la reparación: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}