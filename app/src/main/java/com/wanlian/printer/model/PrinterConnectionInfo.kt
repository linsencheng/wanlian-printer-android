package com.wanlian.printer.model

/** Details captured from the transport object that is actually used for printer writes. */
data class PrinterConnectionInfo(
    val transport: BluetoothTransport,
    val serviceUuid: String? = null,
    val characteristicUuid: String? = null,
    val supportsWrite: Boolean = false,
    val supportsWriteWithoutResponse: Boolean = false,
) {
    val characteristicPropertiesLabel: String
        get() = buildList {
            if (supportsWrite) add("WRITE")
            if (supportsWriteWithoutResponse) add("WRITE_WITHOUT_RESPONSE")
        }.joinToString(", ").ifBlank { "N/A" }
}
