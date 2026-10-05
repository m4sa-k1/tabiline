package io.github.m4sak1.tabiline.ui.components

import org.junit.Assert.*
import org.junit.Test

class PopupScrimTest {
    @Test fun scrimFadesWithPopupAndNeverExceedsLightDim() {
        assertEquals(0f, popupScrimAlpha(0f), 0.0001f)
        assertEquals(0.07f, popupScrimAlpha(0.5f), 0.0001f)
        assertEquals(0.14f, popupScrimAlpha(1f), 0.0001f)
        assertEquals(0f, popupScrimAlpha(-1f), 0.0001f)
        assertEquals(0.14f, popupScrimAlpha(2f), 0.0001f)
    }
}
