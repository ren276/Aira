package com.aira.health.presentation.nutrition.scanner

import javax.inject.Inject

/**
 * ML Kit implementation of BarcodeScannerGateway.
 * Returns null until camera-backed scanner integration is wired.
 */
class MlKitBarcodeScannerGateway @Inject constructor() : BarcodeScannerGateway {
    override suspend fun scanBarcode(): ScannerResult? {
        return null
    }
}
