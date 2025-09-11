// Define esta clase en un archivo Kotlin separado (Reparacion.kt)
import java.util.UUID // Importa UUID

data class Reparacion(
    val id: String = UUID.randomUUID().toString(), // Nuevo campo ID único
    val nombre: String,
    val apellido: String,
    val fecha: String,
    val auto: String,
    val amortiguadores: List<String>,
    val costoFinal: Double,
    val telefono: String
)