package com.Reparaciones

import Reparacion
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText // Para el input de búsqueda
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar // Importar Toolbar
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Comparator
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var listViewReparaciones: ListView
    private lateinit var btnNuevaReparacion: android.widget.Button // Asumiendo un botón flotante o similar
    private lateinit var listaReparaciones: MutableList<Reparacion>
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var toolbar: Toolbar
    private lateinit var btnLimpiarBusqueda: Button
    private var isSortingAscending = true
    private var ultimaBusqueda: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar) // Configura la Toolbar como la ActionBar de la actividad
        supportActionBar?.title = "Reparaciones" // Título de la app en la Toolbar

        listViewReparaciones = findViewById(R.id.listViewReparaciones)
        btnNuevaReparacion = findViewById(R.id.btnNuevaReparacion)

        listaReparaciones = mutableListOf()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_2, android.R.id.text1, mutableListOf())
        listViewReparaciones.adapter = adapter

        btnNuevaReparacion.setOnClickListener {
            val intent = Intent(this, CargaActivity::class.java)
            startActivity(intent)
        }

        listViewReparaciones.setOnItemClickListener { parent, view, position, id ->
            // Asegúrate de usar la listaReparaciones (filtrada/ordenada) para obtener el item correcto
            mostrarDetallesReparacion(listaReparaciones[position])
        }

        btnLimpiarBusqueda = findViewById(R.id.btnLimpiarBusqueda)
        btnLimpiarBusqueda.setOnClickListener {
            ultimaBusqueda = null
            cargarReparaciones(false)
            btnLimpiarBusqueda.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        cargarReparaciones(false) // Carga las reparaciones inicialmente sin aplicar ordenamiento extra si ya hay uno
    }

    // --- Métodos para la Toolbar ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu) // Infla tu archivo de menú
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                mostrarDialogoBusqueda()
                true
            }
            R.id.action_sort -> {
                toggleSortOrder()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleSortOrder() {
        isSortingAscending = !isSortingAscending // Invierte el estado
        cargarReparaciones(true) // Recarga y reordena la lista con el nuevo criterio
        val orderText = if (isSortingAscending) "más antiguas primero" else "más recientes primero"
        Toast.makeText(this, "Ordenando por fecha: $orderText", Toast.LENGTH_SHORT).show()
    }

    private fun mostrarDialogoBusqueda() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Buscar Reparación")
        val input = EditText(this)
        input.hint = "Nombre o Apellido del cliente"
        input.setText(ultimaBusqueda ?: "")
        builder.setView(input)

        builder.setPositiveButton("Buscar") { dialog, _ ->
            val query = input.text.toString().trim()
            if (query.isNotEmpty()) {
                filtrarReparaciones(query)
            } else {
                Toast.makeText(this, "Ingresa un término de búsqueda.", Toast.LENGTH_SHORT).show()
                cargarReparaciones(false) // Si está vacío, vuelve a mostrar todo
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            cargarReparaciones(false) // Si cancela, vuelve a mostrar la lista completa
            dialog.cancel()
        }
        builder.show()
    }

    private fun filtrarReparaciones(query: String) {
        adapter.clear()
        val filteredList = listaReparaciones.filter {
            val texto = "${it.nombre} ${it.apellido}".lowercase()
            val partes = query.lowercase().split(" ")
            partes.all { texto.contains(it) }
        }

        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No se encontraron resultados para '$query'.", Toast.LENGTH_SHORT).show()
        }

        for (reparacion in filteredList) {
            val displayText = "${reparacion.nombre} ${reparacion.apellido}\nFecha: ${reparacion.fecha}"
            adapter.add(displayText)
        }
        adapter.notifyDataSetChanged()
        ultimaBusqueda = query
        btnLimpiarBusqueda.visibility = View.VISIBLE
    }


    private fun cargarReparaciones(applySort: Boolean) {
        listaReparaciones.clear()
        adapter.clear()

        try {
            val fileInputStream = openFileInput("reparaciones.txt")
            val inputStreamReader = InputStreamReader(fileInputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            val gson = Gson()
            var line: String?

            while (bufferedReader.readLine().also { line = it } != null) {
                val reparacion: Reparacion = gson.fromJson(line, Reparacion::class.java)
                listaReparaciones.add(reparacion)
            }
            bufferedReader.close()

            // Solo ordena si se pidió (para el toggle) o si es la carga inicial
            if (applySort) {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                listaReparaciones.sortWith(Comparator { r1, r2 ->
                    val date1 = dateFormat.parse(r1.fecha)
                    val date2 = dateFormat.parse(r2.fecha)
                    if (isSortingAscending) date1.compareTo(date2) else date2.compareTo(date1)
                })
            } else { // Si no se aplica ordenamiento, asegura un orden predeterminado (ej. el original de carga)
                // Opcional: si quieres que sin aplicar sort explícito, siempre se mantenga un orden base,
                // podrías ordenar por un campo predeterminado aquí (ej. por fecha ascendente por defecto)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                listaReparaciones.sortWith(Comparator { r1, r2 ->
                    val date1 = dateFormat.parse(r1.fecha)
                    val date2 = dateFormat.parse(r2.fecha)
                    date1.compareTo(date2)
                })
            }


            for (reparacion in listaReparaciones) {
                val displayText = "${reparacion.nombre} ${reparacion.apellido}\nFecha: ${reparacion.fecha}"
                adapter.add(displayText)
            }
            adapter.notifyDataSetChanged()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "No se encontraron reparaciones guardadas o hubo un error al cargar.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDetallesReparacion(reparacion: Reparacion) {
        val detalles = StringBuilder()
        detalles.append("Cliente: ${reparacion.nombre} ${reparacion.apellido}\n")
        detalles.append("Teléfono: ${reparacion.telefono}\n")
        detalles.append("Fecha: ${reparacion.fecha}\n")
        detalles.append("Auto: ${reparacion.auto}\n")
        detalles.append("Amortiguadores: ${reparacion.amortiguadores.joinToString(", ")}\n")
        detalles.append("Costo Final: $${String.format("%.2f", reparacion.costoFinal)}\n")

        AlertDialog.Builder(this)
            .setTitle("Detalles de la Reparación")
            .setMessage(detalles.toString())
            .setPositiveButton("Cerrar") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Editar") { _, _ ->
                val intent = Intent(this, CargaActivity::class.java)
                val gson = Gson()
                val reparacionJson = gson.toJson(reparacion)
                intent.putExtra("reparacion_json", reparacionJson)
                startActivity(intent)
            }
            .setNegativeButton("Eliminar") { _, _ -> // Agrega el botón Eliminar
                mostrarDialogoConfirmacionEliminar(reparacion)
            }
            .show()
    }

    // --- Nueva función para el diálogo de confirmación de eliminación ---
    private fun mostrarDialogoConfirmacionEliminar(reparacion: Reparacion) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que quieres eliminar la reparación de ${reparacion.nombre} ${reparacion.apellido} del ${reparacion.fecha}?")
            .setPositiveButton("Sí, Eliminar") { dialog, _ ->
                eliminarReparacion(reparacion)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    // --- Nueva función para eliminar la reparación ---
    private fun eliminarReparacion(reparacionAEliminar: Reparacion) {
        val gson = Gson()
        val reparacionesActualizadas = mutableListOf<Reparacion>()

        // Leer todas las reparaciones excepto la que se quiere eliminar
        try {
            val fileInputStream = openFileInput("reparaciones.txt")
            val inputStreamReader = InputStreamReader(fileInputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                val rep: Reparacion = gson.fromJson(line, Reparacion::class.java)
                if (rep.id != reparacionAEliminar.id) { // Solo añade las que NO son la que queremos eliminar
                    reparacionesActualizadas.add(rep)
                }
            }
            bufferedReader.close()

            // Escribir la lista actualizada de nuevo al archivo (sobrescribiendo)
            val fileOutputStream = openFileOutput("reparaciones.txt", MODE_PRIVATE)
            val outputStreamWriter = OutputStreamWriter(fileOutputStream)
            for (rep in reparacionesActualizadas) {
                outputStreamWriter.append(gson.toJson(rep)).append("\n")
            }
            outputStreamWriter.close()

            Toast.makeText(this, "Reparación eliminada correctamente.", Toast.LENGTH_SHORT).show()
            cargarReparaciones(false) // Recargar la lista para que refleje el cambio

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al eliminar la reparación: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


}