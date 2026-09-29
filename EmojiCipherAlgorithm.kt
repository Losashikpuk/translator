package com.example.data.emoji

enum class CipherMode(val title: String, val description: String) {
    SEMANTIC("Смысловой перевод", "Превращает слова в концептуальные эмодзи-иероглифы"),
    CRYPTIC("Символьный шифр (100% обратимый)", "Точное посимвольное кодирование в эмодзи-шифр с возможностью точного восстановления")
}

object EmojiCipherAlgorithm {

    /**
     * Превращает текст в последовательность смайликов
     */
    fun encode(text: String, mode: CipherMode): String {
        if (text.isBlank()) return ""

        return when (mode) {
            CipherMode.SEMANTIC -> encodeSemantic(text)
            CipherMode.CRYPTIC -> encodeCryptic(text)
        }
    }

    /**
     * Превращает смайлики обратно в осмысленный текст
     */
    fun decode(emojiString: String): String {
        if (emojiString.isBlank()) return ""

        // Check if string is predominantly cryptic alphabet emojis (e.g. contains ▫️ or alphabet fruits)
        val hasAlphabetSeparator = emojiString.contains("▫️")
        val emojiTokens = extractEmojiTokens(emojiString)

        if (hasAlphabetSeparator || isMostlyAlphabetCipher(emojiTokens)) {
            val decodedCryptic = decodeCryptic(emojiTokens)
            if (decodedCryptic.isNotBlank()) return decodedCryptic
        }

        // Otherwise decode semantically
        return decodeSemantic(emojiTokens)
    }

    private fun encodeSemantic(text: String): String {
        val result = mutableListOf<String>()
        val wordsAndSeparators = text.split(Regex("(?<=\\s|[,.!?])|(?=\\s|[,.!?])"))

        for (token in wordsAndSeparators) {
            val trimmed = token.trim()
            if (trimmed.isEmpty()) continue

            if (trimmed in listOf(",", ".", "!", "?", ":", ";")) {
                result.add(trimmed)
                continue
            }

            val cleanWord = trimmed.lowercase().filter { it.isLetter() }
            val matchedEmoji = EmojiDictionary.wordToEmoji[cleanWord]

            if (matchedEmoji != null) {
                result.add(matchedEmoji)
            } else {
                // Try sub-stem or fallback to alphabet cipher
                val stemMatch = findStemMatch(cleanWord)
                if (stemMatch != null) {
                    result.add(stemMatch)
                } else if (cleanWord.length <= 4) {
                    // Spell short unknown words using alphabet cipher
                    val spelled = cleanWord.mapNotNull { EmojiDictionary.ruCharToEmoji[it] }.joinToString("")
                    if (spelled.isNotEmpty()) result.add(spelled) else result.add(token)
                } else {
                    result.add("✨$token✨")
                }
            }
        }

        return result.joinToString(" ")
    }

    private fun encodeCryptic(text: String): String {
        val sb = StringBuilder()
        for (char in text.lowercase()) {
            val emoji = EmojiDictionary.ruCharToEmoji[char]
            if (emoji != null) {
                sb.append(emoji)
            } else {
                sb.append(char)
            }
        }
        return sb.toString()
    }

    private fun decodeCryptic(tokens: List<String>): String {
        val sb = StringBuilder()
        for (token in tokens) {
            val char = EmojiDictionary.emojiToRuChar[token]
            if (char != null) {
                sb.append(char)
            } else {
                sb.append(token)
            }
        }
        val result = sb.toString().replace("▫️", " ").trim()
        return if (result.isNotEmpty()) {
            result.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else ""
    }

    private fun decodeSemantic(tokens: List<String>): String {
        val words = mutableListOf<String>()

        for (token in tokens) {
            // Check direct dictionary
            val word = EmojiDictionary.emojiToWordRu[token]
            if (word != null) {
                words.add(word)
                continue
            }

            // Check if it's a single alphabet char
            val char = EmojiDictionary.emojiToRuChar[token]
            if (char != null) {
                if (char == ' ') {
                    words.add(" ")
                } else {
                    words.add(char.toString())
                }
                continue
            }

            // Punctuation or raw text
            words.add(token)
        }

        // Assemble words into natural sentence
        var assembled = words.joinToString(" ")
            .replace(" ,", ",")
            .replace(" .", ".")
            .replace(" !", "!")
            .replace(" ?", "?")
            .trim()

        return if (assembled.isNotEmpty()) {
            assembled.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        } else ""
    }

    private fun findStemMatch(word: String): String? {
        if (word.length < 3) return null
        val prefix = word.take(4)
        for ((k, v) in EmojiDictionary.wordToEmoji) {
            if (k.startsWith(prefix) || (k.length >= 4 && prefix.startsWith(k.take(4)))) {
                return v
            }
        }
        return null
    }

    private fun isMostlyAlphabetCipher(tokens: List<String>): Boolean {
        if (tokens.isEmpty()) return false
        var alphabetCount = 0
        for (t in tokens) {
            if (EmojiDictionary.emojiToRuChar.containsKey(t)) {
                alphabetCount++
            }
        }
        return (alphabetCount.toDouble() / tokens.size) > 0.6
    }

    private fun extractEmojiTokens(input: String): List<String> {
        val list = mutableListOf<String>()
        var i = 0
        val len = input.length
        while (i < len) {
            val codePoint = input.codePointAt(i)
            val charCount = Character.charCount(codePoint)

            // Look ahead for zero-width joiner or variation selector (e.g. ✍️, 🗣️)
            var end = i + charCount
            while (end < len) {
                val nextCp = input.codePointAt(end)
                if (nextCp == 0xFE0F || nextCp == 0x200D || (nextCp in 0x1F3FB..0x1F3FF)) {
                    end += Character.charCount(nextCp)
                    // If ZWJ, also consume the next emoji
                    if (nextCp == 0x200D && end < len) {
                        val followingCp = input.codePointAt(end)
                        end += Character.charCount(followingCp)
                    }
                } else {
                    break
                }
            }

            val token = input.substring(i, end)
            if (token.isNotBlank()) {
                list.add(token)
            }
            i = end
        }
        return list
    }
}
