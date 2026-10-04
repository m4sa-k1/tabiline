package io.github.m4sak1.tabiline.core.model

enum class TransportMode(val label: String) {
    TRAIN("電車"),
    FLIGHT("飛行機"),
    FERRY("船"),
    BUS("バス"),
    WALK("徒歩"),
    OTHER("その他"),
}

enum class TrainType(val label: String) {
    SHINKANSEN("新幹線"),
    LIMITED_EXPRESS("特急"),
    EXPRESS("急行"),
    RAPID("快速"),
    LOCAL("普通"),
    OTHER("その他"),
}
