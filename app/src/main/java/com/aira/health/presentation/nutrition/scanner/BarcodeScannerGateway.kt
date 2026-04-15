package com.aira.health.presentation.nutrition.scanner

/**
 * Result of a barcode scanning operation mapped to a nutrition draft (D-13).
 */
data class ScannerResult(
    val barcode: String,
    val foodName: String?,
    val calories: Float?,
    val proteinG: Float?,
    val carbsG: Float?,
    val fatG: Float?
)

/**
 * Adapter that converts barcode scan outputs into nutrition drafts.
 */
interface BarcodeScannerGateway {
    suspend fun scanBarcode(): ScannerResult?
}
