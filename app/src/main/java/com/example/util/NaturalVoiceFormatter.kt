package com.example.util

import java.util.regex.Pattern

/**
 * NaturalVoiceFormatter converts diagnostic telemetry, OBD-II DTC codes,
 * and GPS navigation instructions into natural, human-sounding automotive speech.
 */
object NaturalVoiceFormatter {

    private val EMOJI_REGEX = Pattern.compile("[\\p{So}\\p{Cn}\\p{Cs}\\p{Co}]")
    private val MARKDOWN_REGEX = Pattern.compile("[*_~`#>\\[\\]()|]")
    private val MULTI_SPACE_REGEX = Pattern.compile("\\s+")

    fun formatForNaturalSpeech(rawText: String): String {
        if (rawText.isBlank()) return ""

        var text = rawText

        // 1. Remove URLs
        text = text.replace(Regex("https?://\\S+"), "online link")

        // 2. Expand Automotive DTC Fault Codes (e.g., P0300 -> "P zero 3 0 0", P0420 -> "P zero 4 2 0")
        text = text.replace(Regex("(?i)\\b(P|C|B|U)0(\\d)(\\d{2})\\b")) { match ->
            val system = match.groupValues[1].uppercase()
            val zero = "zero"
            val digit1 = match.groupValues[2]
            val digits2 = match.groupValues[3]
            "$system $zero $digit1 $digits2"
        }

        // 3. Expand Automotive Acronyms & Units to conversational words
        text = text
            .replace(Regex("(?i)\\bOBD-?II\\b"), "O B D two")
            .replace(Regex("(?i)\\bOBD-?2\\b"), "O B D two")
            .replace(Regex("(?i)\\bOBD\\b"), "O B D")
            .replace(Regex("(?i)\\bECU\\b"), "E C U")
            .replace(Regex("(?i)\\bCAN Bus\\b"), "CAN bus")
            .replace(Regex("(?i)\\bMAF\\b"), "mass air flow")
            .replace(Regex("(?i)\\bDTCs\\b"), "diagnostic trouble codes")
            .replace(Regex("(?i)\\bDTC\\b"), "diagnostic code")
            .replace(Regex("(?i)\\bO2\\b"), "oxygen")
            .replace(Regex("(?i)\\bPID\\b"), "parameter I D")
            .replace(Regex("(?i)\\bRPM\\b"), "R P M")
            .replace(Regex("(?i)\\bPSI\\b"), "P S I")
            .replace(Regex("(?i)\\bMPH\\b"), "miles per hour")
            .replace(Regex("(?i)\\bMPG\\b"), "miles per gallon")
            .replace(Regex("(?i)\\bEV\\b"), "E V")
            .replace(Regex("(?i)\\bkWh\\b"), "kilowatt hours")
            .replace(Regex("(?i)\\bkW\\b"), "kilowatts")
            .replace(Regex("(?i)\\bNACS\\b"), "N A C S")
            .replace(Regex("(?i)\\bCCS\\b"), "C C S")
            .replace(Regex("(?i)\\bDC Fast\\b"), "D C fast")

        // 4. Expand Temperatures and Units
        text = text
            .replace(Regex("(?i)(\\d+)\\s*°\\s*F"), "$1 degrees Fahrenheit")
            .replace(Regex("(?i)(\\d+)\\s*°\\s*C"), "$1 degrees Celsius")
            .replace(Regex("(?i)(\\d+)\\s*g/s"), "$1 grams per second")
            .replace(Regex("(?i)(\\d+)\\s*mi\\b"), "$1 miles")
            .replace(Regex("(?i)(\\d+)\\s*ft\\b"), "$1 feet")
            .replace(Regex("(?i)(\\d+)\\s*min\\b"), "$1 minutes")
            .replace(Regex("(?i)(\\d+)\\s*sec\\b"), "$1 seconds")
            .replace(Regex("(?i)(\\d+)\\s*hr\\b"), "$1 hours")

        // 5. Expand Highway & Road designations
        text = text
            .replace(Regex("(?i)\\bHwy\\.?\\s*(\\d+)"), "Highway $1")
            .replace(Regex("(?i)\\bI-(\\d+)"), "Interstate $1")
            .replace(Regex("(?i)\\bUS-(\\d+)"), "U S $1")
            .replace(Regex("(?i)\\bSt\\.\\b"), "Street")
            .replace(Regex("(?i)\\bBlvd\\.\\b"), "Boulevard")
            .replace(Regex("(?i)\\bAve\\.\\b"), "Avenue")

        // 6. Formatting & Symbols
        text = text
            .replace("•", ", ")
            .replace("|", ", ")
            .replace("/", " slash ")
            .replace("&", " and ")
            .replace("+", " plus ")
            .replace("✅", " safe ")
            .replace("⚠️", " caution ")
            .replace("🛑", " stop ")

        // 7. Strip Markdown syntax and remaining emojis
        text = MARKDOWN_REGEX.matcher(text).replaceAll(" ")
        text = EMOJI_REGEX.matcher(text).replaceAll(" ")

        // 8. Normalize spacing and punctuation pauses
        text = MULTI_SPACE_REGEX.matcher(text).replaceAll(" ").trim()
        text = text
            .replace(" ,", ",")
            .replace(" .", ".")
            .replace(" !", "!")
            .replace(" ?", "?")
            .replace(", ,", ",")

        return text
    }
}
