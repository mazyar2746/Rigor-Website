package com.example.i18n

enum class Language(
    val code: String,
    val nativeName: String,
    val flag: String,
    val isRtl: Boolean
) {
    ENGLISH("en", "English", "🇺🇸", false),
    PERSIAN("fa", "فارسی", "🇮🇷", true),
    ARABIC("ar", "العربية", "🇸🇦", true),
    TURKISH("tr", "Türkçe", "🇹🇷", false),
    RUSSIAN("ru", "Русский", "🇷🇺", false),
    GERMAN("de", "Deutsch", "🇩🇪", false),
    CHINESE("zh", "简体中文", "🇨🇳", false),
    FRENCH("fr", "Français", "🇫🇷", false),
    PORTUGUESE("pt", "Português", "🇵🇹", false);

    companion object {
        fun fromCode(code: String): Language {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}
