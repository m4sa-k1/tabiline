package io.github.m4sak1.tabiline

import android.app.Application
import io.github.m4sak1.tabiline.di.AppContainer

class TabilineApplication : Application() {
    val container by lazy { AppContainer(this) }
}
