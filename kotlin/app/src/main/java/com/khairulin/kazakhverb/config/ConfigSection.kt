package com.khairulin.kazakhverb.config

data class ConfigSection(
    val settings: List<Setting>
) {
    companion object {
        val kDefaultTenseSettings = listOf(
            Setting("Present transitive", true),
            Setting("Present continuous", true),
            Setting("Past", true),
            Setting("Remote past", false),
            Setting("Conditional mood", true),
            Setting("Imperative mood", true),
            Setting("Optative mood", true),
        )
        val kDefaultFormSettings = listOf(
            Setting("мен", true),
            Setting("біз", true),
            Setting("сен", true),
            Setting("сендер", false),
            Setting("Cіз", true),
            Setting("Cіздер", false),
            Setting("ол", true),
            Setting("олар", true),
        )

        fun makeTenseConfigDefault() = ConfigSection(kDefaultTenseSettings)

        fun makeFormConfigDefault() = ConfigSection(kDefaultFormSettings)

        private fun decodeFromString(s: String, defaultSettings: List<Setting>): ConfigSection {
            require(s.length == defaultSettings.size) {
                "failed to decode TenseConfig: invalid string size ${s.length}"
            }
            val settings = mutableListOf<Setting>()
            defaultSettings.forEachIndexed { index, setting ->
                when (s[index]) {
                    '1' -> settings.add(setting.copy(on = true))
                    else -> settings.add(setting.copy(on = false))
                }
            }
            return ConfigSection(settings.toList())
        }

        fun decodeTenseConfigFromString(s: String) = decodeFromString(s, kDefaultTenseSettings)
        fun decodeFormConfigFromString(s: String) = decodeFromString(s, kDefaultFormSettings)
    }

    fun encodeToString(): String {
        val sb = StringBuilder()
        for (setting in settings) {
            sb.append(if(setting.on) '1' else '0')
        }
        return sb.toString()
    }

    fun toggleAt(index: Int): ConfigSection {
        val cur = settings.toMutableList()
        val setting = cur[index]
        cur[index] = setting.copy(on = !setting.on)
        return ConfigSection(cur.toList())
    }
}
