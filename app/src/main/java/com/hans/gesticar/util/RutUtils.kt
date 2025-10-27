package com.hans.gesticar.util

private val rutRegex = Regex("""^[0-9]{7,}-[0-9K]$""", RegexOption.IGNORE_CASE)

/**
 * Normaliza un RUT eliminando puntos y espacios, asegurando el guion antes del dígito verificador
 * y dejando el dígito verificador en mayúscula.
 */
fun normalizeRut(input: String): String {
    val cleaned = input
        .trim()
        .replace(".", "")
        .replace(" ", "")
        .replace("-", "")
        .uppercase()
        .filter { it.isDigit() || it == 'K' }
    if (cleaned.length < 2) return cleaned
    val cuerpo = cleaned.dropLast(1)
    val dv = cleaned.last()
    return "$cuerpo-$dv"
}

/**
 * Formatea un RUT para mostrarse con el patrón "XXXXXXXX - Y".
 */
fun formatRutForDisplay(rut: String): String {
    val normalized = normalizeRut(rut)
    val parts = normalized.split("-")
    return if (parts.size == 2) {
        "${parts[0]} - ${parts[1]}"
    } else normalized
}

fun formatRutInput(raw: String): String {
    val cleaned = sanitizeRutInput(raw)
    if (cleaned.isEmpty()) return ""
    if (cleaned.length == 1) return cleaned
    val cuerpo = cleaned.dropLast(1).filter { it.isDigit() }
    val dv = cleaned.last()
    return if (cuerpo.isEmpty()) dv.toString() else "$cuerpo - $dv"
}

fun sanitizeRutInput(raw: String): String {
    if (raw.isBlank()) return ""
    val upper = raw.uppercase()
    val digits = StringBuilder()
    var dv: Char? = null
    for (char in upper) {
        when {
            char.isDigit() -> digits.append(char)
            char == 'K' -> dv = 'K'
        }
    }
    if (dv != null) {
        digits.append(dv)
    }
    return digits.toString()
}

private fun calcularDv(cuerpo: String): Char {
    var factor = 2
    var suma = 0
    for (char in cuerpo.reversed()) {
        suma += char.digitToInt() * factor
        factor = if (factor == 7) 2 else factor + 1
    }
    val resto = 11 - (suma % 11)
    return when (resto) {
        11 -> '0'
        10 -> 'K'
        else -> ('0' + resto)
    }
}

fun isRutValid(input: String): Boolean {
    val normalized = normalizeRut(input)
    if (!rutRegex.containsMatchIn(normalized)) return false
    val cuerpo = normalized.substringBefore('-')
    val dvIngresado = normalized.substringAfter('-')
    if (cuerpo.length < 7) return false
    val dvCalculado = calcularDv(cuerpo)
    return dvIngresado.equals(dvCalculado.toString(), ignoreCase = true)
}

fun rutDigits(input: String): String = normalizeRut(input).replace("-", "")
