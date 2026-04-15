package com.aira.health.presentation.nutrition.scanner

import javax.inject.Inject

/**
 * ML Kit implementation of BarcodeScannerGateway.
 * (Placeholder for actual ML Kit integration pending phase 5/6 dependencies)
 */
class MlKitBarcodeScannerGateway @Inject constructor() : BarcodeScannerGateway {
    override suspend fun scanBarcode(): ScannerResult? {
        // Placeholder implementation for Phase 4 UI contracts
        // In reality, this would launch the ML Kit scanner intent and parse the result
        return ScannerResult(
            barcode = "123456789012",
            foodName = "Sample Barcode Food",
            calories = 150f,
            proteinG = 5f,
            carbsG = 20f,
            fatG = 3f
        )
    }
}
