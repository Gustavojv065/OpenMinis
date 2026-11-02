package com.openminis.app.ui.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalPreviewAddressTest {
    @Test fun acceptsCommonDevelopmentPorts() {
        assertEquals("http://127.0.0.1:3000/", LocalPreviewAddress.fromPortInput("3000"))
        assertEquals("http://127.0.0.1:5173/", LocalPreviewAddress.fromPortInput(" 5173 "))
        assertEquals("http://127.0.0.1:8080/", LocalPreviewAddress.fromPortInput("8080"))
        assertEquals("http://127.0.0.1:65535/", LocalPreviewAddress.fromPortInput("65535"))
    }

    @Test fun rejectsInvalidOrExternalTargets() {
        listOf("", "0", "65536", "-1", "70000", "http://localhost:3000",
            "3000/path", "3000;rm -rf", "1.2", "abc", "999999999999")
            .forEach { assertNull(it, LocalPreviewAddress.fromPortInput(it)) }
    }
}
