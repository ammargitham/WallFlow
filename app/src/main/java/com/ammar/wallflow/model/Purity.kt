package com.ammar.wallflow.model

enum class Purity(val purityName: String) {
    SFW("sfw"),
    SKETCHY("sketchy"),
    NSFW("nsfw"),
    ;

    companion object {
        fun fromName(name: String) = when (name) {
            "nsfw" -> NSFW
            "sketchy" -> SKETCHY
            else -> SFW
        }
    }
}

private val WALLHAVEN_PURITY_INT_MAP = mapOf(
    Purity.SFW to 100,
    Purity.SKETCHY to 10,
    Purity.NSFW to 1,
)

fun Set<Purity>.toWallhavenPurityInt() = this.fold(0) { prev, purity ->
    prev + WALLHAVEN_PURITY_INT_MAP.getOrDefault(purity, 100)
}
