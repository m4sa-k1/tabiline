package io.github.m4sak1.tabiline.notifications

import android.app.NotificationManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.m4sak1.tabiline.R
import io.github.m4sak1.tabiline.core.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class DepartureNotificationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun notificationHasTransparentIconAndOpensApp() {
        val manager = context.getSystemService(NotificationManager::class.java)
        assertTrue("Grant POST_NOTIFICATIONS to the test app before running", manager.areNotificationsEnabled())
        val reminders = DepartureReminders(context)
        reminders.sync(emptyList(), UserSettings())
        val leg = TransportLeg(id = 987654, tripId = 0, departure = Instant.now().plusSeconds(600),
            arrival = Instant.now().plusSeconds(3600), departureZoneId = "Asia/Tokyo", arrivalZoneId = "Asia/Tokyo",
            departurePlace = "テスト出発", arrivalPlace = "テスト到着", mode = TransportMode.TRAIN)
        try {
            reminders.show(leg)
            val posted = manager.activeNotifications.first { it.tag == "leg-987654" }.notification
            assertEquals(R.drawable.ic_notification, posted.smallIcon.resId)
            assertNotNull(posted.contentIntent)
            posted.contentIntent.send()
            val drawable = context.getDrawable(R.drawable.ic_notification)!!
            val bitmap = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, 108, 108)
            drawable.draw(Canvas(bitmap))
            assertEquals(0, Color.alpha(bitmap.getPixel(0, 0)))
            assertTrue(Color.alpha(bitmap.getPixel(30, 40)) > 0)
        } finally { manager.cancel("leg-987654", 0) }
    }
}
