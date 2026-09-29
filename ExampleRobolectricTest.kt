package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.emoji.CipherMode
import com.example.data.emoji.EmojiCipherAlgorithm
import com.example.data.emoji.EmojiDictionary
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TransMutate", appName)
  }

  @Test
  fun `emoji dictionary has entries`() {
    assertTrue(EmojiDictionary.entries.isNotEmpty())
    assertNotNull(EmojiDictionary.wordToEmoji["кофе"])
    assertNotNull(EmojiDictionary.emojiToWordRu["☕"])
  }

  @Test
  fun `emoji cipher semantic encoding and decoding`() {
    val input = "Я люблю пить кофе"
    val encoded = EmojiCipherAlgorithm.encode(input, CipherMode.SEMANTIC)
    assertFalse(encoded.isBlank())
    assertTrue(encoded.contains("☕"))

    val decoded = EmojiCipherAlgorithm.decode(encoded)
    assertFalse(decoded.isBlank())
  }

  @Test
  fun `emoji cryptic cipher reversible encoding`() {
    val input = "код"
    val encoded = EmojiCipherAlgorithm.encode(input, CipherMode.CRYPTIC)
    assertFalse(encoded.isBlank())
    val decoded = EmojiCipherAlgorithm.decode(encoded)
    assertEquals("Код", decoded)
  }
}
