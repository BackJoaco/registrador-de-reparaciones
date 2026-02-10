package com.Reparaciones

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var listViewReparaciones: ListView
    private lateinit var btnNuevaReparacion: Button
    private lateinit var listaReparaciones: MutableList<Reparacion>
    private var listaVisualizada: List<Reparacion> = ArrayList()
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var toolbar: Toolbar
    private lateinit var btnLimpiarBusqueda: Button
    private var isSortingAscending = true
    private var ultimaBusqueda: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Reparaciones"

        listViewReparaciones = findViewById(R.id.listViewReparaciones)
        btnNuevaReparacion = findViewById(R.id.btnNuevaReparacion)
        btnLimpiarBusqueda = findViewById(R.id.btnLimpiarBusqueda)

        listaReparaciones = mutableListOf()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_2, android.R.id.text1, mutableListOf())
        listViewReparaciones.adapter = adapter

        btnNuevaReparacion.setOnClickListener {
            val intent = Intent(this, CargaActivity::class.java)
            startActivity(intent)
        }

        listViewReparaciones.setOnItemClickListener { parent, view, position, id ->
            if (position < listaVisualizada.size) {
                mostrarDetallesReparacion(listaVisualizada[position])
            }
        }

        btnLimpiarBusqueda.setOnClickListener {
            ultimaBusqueda = null
            cargarReparaciones(false)
            btnLimpiarBusqueda.visibility = View.GONE
        }
        ejecutarSincronizacion()
    }

    override fun onResume() {
        super.onResume()
        // Cada vez que volvemos a la pantalla, recargamos desde la Base de Datos local
        cargarReparaciones(false)
    }

    // --- Métodos Toolbar ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
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

    private fun ejecutarSincronizacion() {
        lifecycleScope.launch {
            val sincronizador = Sincronizador(applicationContext)
            val resultado = sincronizador.sincronizar()

            // Solo mostramos mensaje si realmente pasó algo interesante (no "Todo al día")
            if (resultado != "Todo está al día.") {
                Toast.makeText(this@MainActivity, resultado, Toast.LENGTH_LONG).show()
                cargarReparaciones(false) // Refrescamos la lista
            }
        }
    }

    private fun toggleSortOrder() {
        isSortingAscending = !isSortingAscending
        ordenarLista()
        actualizarAdapter()
        val orderText = if (isSortingAscending) "más antiguas primero" else "más recientes primero"
        Toast.makeText(this, "Ordenando por fecha: $orderText", Toast.LENGTH_SHORT).show()
    }

    // --- LÓGICA DE BASE DE DATOS (ROOM) ---

    private fun cargarReparaciones(applySort: Boolean) {
        lifecycleScope.launch {
            // 1. Obtenemos la instancia de la base de datos
            val database = AppDatabase.getDatabase(applicationContext)

            // 2. Ejecutamos la consulta en segundo plano (suspend function)
            val reparacionesLocales = database.reparacionDao().obtenerTodas()

            // 3. Actualizamos la lista en memoria
            listaReparaciones.clear()
            listaReparaciones.addAll(reparacionesLocales)

            // 4. Aplicamos ordenamiento y actualizamos la UI
            if (applySort || isSortingAscending) {
                ordenarLista()
            }

            if (!ultimaBusqueda.isNullOrEmpty()) {
                filtrarReparaciones(ultimaBusqueda!!)
            } else {
                actualizarAdapter()
            }
        }
    }

    private fun ordenarLista() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        listaReparaciones.sortWith(Comparator { r1, r2 ->
            try {
                val date1 = dateFormat.parse(r1.fecha)
                val date2 = dateFormat.parse(r2.fecha)
                if (date1 != null && date2 != null) {
                    if (isSortingAscending) date1.compareTo(date2) else date2.compareTo(date1)
                } else 0
            } catch (e: Exception) {
                0
            }
        })
    }

    private fun actualizarAdapter(listaAVisualizar: List<Reparacion> = listaReparaciones) {
        listaVisualizada = listaAVisualizar
        adapter.clear()
        for (reparacion in listaAVisualizar) {
            val displayText = "${reparacion.nombre} ${reparacion.apellido}\nFecha: ${reparacion.fecha}"
            adapter.add(displayText)
        }
        adapter.notifyDataSetChanged()
    }

    // --- Búsqueda ---
    // Mantenemos la lógica de búsqueda en memoria para aprovechar que ya cargamos los datos,
    // aunque también podríamos usar el método DAO 'buscar(query)' si la lista fuera enorme.

    private fun mostrarDialogoBusqueda() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Buscar Reparación")
        val input = EditText(this)
        input.hint = "Nombre o Apellido"
        input.setText(ultimaBusqueda ?: "")
        builder.setView(input)

        builder.setPositiveButton("Buscar") { dialog, _ ->
            val query = input.text.toString().trim()
            if (query.isNotEmpty()) {
                filtrarReparaciones(query)
            } else {
                cargarReparaciones(false)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            cargarReparaciones(false)
            dialog.cancel()
        }
        builder.show()
    }

    private fun filtrarReparaciones(query: String) {
        val filteredList = listaReparaciones.filter {
            val texto = "${it.nombre} ${it.apellido}".lowercase()
            val partes = query.lowercase().split(" ")
            partes.all { texto.contains(it) }
        }

        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No se encontraron resultados.", Toast.LENGTH_SHORT).show()
        }

        actualizarAdapter(filteredList)
        ultimaBusqueda = query
        btnLimpiarBusqueda.visibility = View.VISIBLE
    }

    // --- Detalles y Eliminación ---

    private fun mostrarDetallesReparacion(reparacion: Reparacion) {
        val detalles = StringBuilder()
        detalles.append("Cliente: ${reparacion.nombre} ${reparacion.apellido}\n")
        detalles.append("Teléfono: ${reparacion.telefono}\n")
        detalles.append("Fecha: ${reparacion.fecha}\n")
        detalles.append("Auto: ${reparacion.auto}\n")

        // CAMBIO: Ahora 'amortiguadores' es un String, no una lista. Lo mostramos directo.
        detalles.append("Amortiguadores: ${reparacion.amortiguadores}\n")

        detalles.append("Costo: $${String.format("%.2f", reparacion.costoFinal)}\n")

        // Debug: Mostrar estado de sincronización (opcional)
        val estadoSync = if (reparacion.esSincronizado) "Sí" else "Pendiente"
        detalles.append("Sincronizado: $estadoSync\n")

        AlertDialog.Builder(this)
            .setTitle("Detalles")
            .setMessage(detalles.toString())
            .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
            .setNeutralButton("Editar") { _, _ ->
                val intent = Intent(this, CargaActivity::class.java)
                intent.putExtra("reparacion_obj", reparacion)
                startActivity(intent)
            }
            .setNegativeButton("Eliminar") { _, _ ->
                mostrarDialogoConfirmacionEliminar(reparacion)
            }
            .show()
    }

    private fun mostrarDialogoConfirmacionEliminar(reparacion: Reparacion) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Eliminar reparación de ${reparacion.nombre}?")
            .setPositiveButton("Sí, Eliminar") { dialog, _ ->
                eliminarReparacion(reparacion)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
            .show()
    }

    private fun eliminarReparacion(reparacion: Reparacion) {
        val id = reparacion.id ?: return

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            db.reparacionDao().marcarComoEliminado(id)

            Toast.makeText(this@MainActivity, "Eliminada.", Toast.LENGTH_SHORT).show()
            cargarReparaciones(false) // Desaparecerá de la lista visual

            ejecutarSincronizacion()
        }
    }
}