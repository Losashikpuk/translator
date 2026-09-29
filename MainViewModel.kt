package com.example.ui.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.camera.EmojiMask
import com.example.data.camera.EmojiMosaicProcessor
import com.example.data.camera.EmojiPalette
import com.example.data.db.HistoryEntity
import com.example.data.emoji.CipherMode
import com.example.data.emoji.EmojiCipherAlgorithm
import com.example.data.engine.DistortionIntensity
import com.example.data.engine.TextMutatorEngine
import com.example.data.engine.TransformationHop
import com.example.data.network.TranslationProvider
import com.example.data.repository.HistoryRepository
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LanguageOption(val code: String, val name: String, val flag: String)

class MainViewModel(private val repository: HistoryRepository) : ViewModel() {

    private val mutatorEngine = TextMutatorEngine()
    private var transformationJob: Job? = null

    // Theme Mode
    private val _themeMode = MutableStateFlow(AppThemeMode.OLED)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
    }

    // Supported Languages
    val availableLanguages = listOf(
        LanguageOption("ru", "Русский", "🇷🇺"),
        LanguageOption("en", "English", "🇬🇧"),
        LanguageOption("de", "Deutsch", "🇩🇪"),
        LanguageOption("fr", "Français", "🇫🇷"),
        LanguageOption("es", "Español", "🇪🇸"),
        LanguageOption("it", "Italiano", "🇮🇹"),
        LanguageOption("ja", "日本語", "🇯🇵"),
        LanguageOption("zh", "中文", "🇨🇳"),
        LanguageOption("ar", "العربية", "🇸🇦"),
        LanguageOption("ko", "한국어", "🇰🇷")
    )

    // Translate & Mutator State
    private val _inputText = MutableStateFlow("Привет! Это тестовое сообщение для проверки искажённого перевода через разные языки.")
    val inputText = _inputText.asStateFlow()

    private val _sourceLang = MutableStateFlow("ru")
    val sourceLang = _sourceLang.asStateFlow()

    private val _targetLang = MutableStateFlow("ru")
    val targetLang = _targetLang.asStateFlow()

    private val _hopsCount = MutableStateFlow(6)
    val hopsCount = _hopsCount.asStateFlow()

    private val _intensity = MutableStateFlow(DistortionIntensity.HIGH)
    val intensity = _intensity.asStateFlow()

    private val _provider = MutableStateFlow(TranslationProvider.AUTO_CHAIN)
    val provider = _provider.asStateFlow()

    private val _isTransforming = MutableStateFlow(false)
    val isTransforming = _isTransforming.asStateFlow()

    private val _currentHopIndex = MutableStateFlow(0)
    val currentHopIndex = _currentHopIndex.asStateFlow()

    private val _intermediateHops = MutableStateFlow<List<TransformationHop>>(emptyList())
    val intermediateHops = _intermediateHops.asStateFlow()

    private val _finalResultText = MutableStateFlow("")
    val finalResultText = _finalResultText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun setInputText(text: String) { _inputText.value = text }
    fun setSourceLang(lang: String) { _sourceLang.value = lang }
    fun setTargetLang(lang: String) { _targetLang.value = lang }
    fun setHopsCount(count: Int) { _hopsCount.value = count }
    fun setIntensity(lvl: DistortionIntensity) { _intensity.value = lvl }
    fun setProvider(p: TranslationProvider) { _provider.value = p }

    fun swapLanguages() {
        val temp = _sourceLang.value
        _sourceLang.value = _targetLang.value
        _targetLang.value = temp
    }

    fun resetAllParameters() {
        _inputText.value = ""
        _sourceLang.value = "ru"
        _targetLang.value = "ru"
        _hopsCount.value = 5
        _intensity.value = DistortionIntensity.MEDIUM
        _provider.value = TranslationProvider.AUTO_CHAIN
        _finalResultText.value = ""
        _intermediateHops.value = emptyList()
        _errorMessage.value = null
    }

    fun applyPreset(presetText: String) {
        _inputText.value = presetText
    }

    fun startTransformation() {
        val textToTransform = _inputText.value.trim()
        if (textToTransform.isBlank()) {
            _errorMessage.value = "Введите текст для трансформации"
            return
        }

        transformationJob?.cancel()
        _isTransforming.value = true
        _errorMessage.value = null
        _intermediateHops.value = emptyList()
        _finalResultText.value = ""
        _currentHopIndex.value = 0

        transformationJob = viewModelScope.launch {
            try {
                val liveHops = mutableListOf<TransformationHop>()
                val result = mutatorEngine.mutateText(
                    inputText = textToTransform,
                    sourceLang = _sourceLang.value,
                    targetLang = _targetLang.value,
                    hopsCount = _hopsCount.value,
                    intensity = _intensity.value,
                    provider = _provider.value,
                    onHopCompleted = { hop ->
                        liveHops.add(hop)
                        _intermediateHops.value = liveHops.toList()
                        _currentHopIndex.value = hop.stepIndex
                    }
                )

                _finalResultText.value = result.finalText

                // Save to Room Database
                val stepsJson = mutatorEngine.serializeHopsToJson(result.hops)
                repository.saveTransformation(
                    originalText = result.originalText,
                    finalText = result.finalText,
                    sourceLang = _sourceLang.value,
                    targetLang = _targetLang.value,
                    hopsCount = _hopsCount.value,
                    intensity = _intensity.value.name,
                    stepsJson = stepsJson
                )
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка мутации: ${e.localizedMessage ?: "неизвестный сбой"}"
            } finally {
                _isTransforming.value = false
            }
        }
    }

    fun cancelTransformation() {
        transformationJob?.cancel()
        _isTransforming.value = false
    }

    // History State
    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery = _historySearchQuery.asStateFlow()

    private val _historyOnlyFavorites = MutableStateFlow(false)
    val historyOnlyFavorites = _historyOnlyFavorites.asStateFlow()

    val historyList: StateFlow<List<HistoryEntity>> = combine(
        repository.allHistory,
        _historySearchQuery,
        _historyOnlyFavorites
    ) { all, query, onlyFav ->
        all.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.originalText.contains(query, ignoreCase = true) ||
                    item.finalText.contains(query, ignoreCase = true)
            val matchesFav = !onlyFav || item.isFavorite
            matchesQuery && matchesFav
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setHistorySearch(query: String) { _historySearchQuery.value = query }
    fun toggleHistoryFavoritesOnly() { _historyOnlyFavorites.value = !_historyOnlyFavorites.value }

    fun toggleFavorite(item: HistoryEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(item.id, item.isFavorite)
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun loadFromHistory(item: HistoryEntity) {
        _inputText.value = item.originalText
        _finalResultText.value = item.finalText
        _sourceLang.value = item.sourceLang
        _targetLang.value = item.targetLang
        _hopsCount.value = item.hopsCount
        _intermediateHops.value = mutatorEngine.deserializeHopsFromJson(item.intermediateStepsJson)
    }

    // Emoji Cipher State
    private val _cipherInput = MutableStateFlow("Привет мир! Я люблю пить кофе, слушать музыку и программировать.")
    val cipherInput = _cipherInput.asStateFlow()

    private val _cipherOutput = MutableStateFlow("")
    val cipherOutput = _cipherOutput.asStateFlow()

    private val _cipherMode = MutableStateFlow(CipherMode.SEMANTIC)
    val cipherMode = _cipherMode.asStateFlow()

    private val _isEmojiToText = MutableStateFlow(false)
    val isEmojiToText = _isEmojiToText.asStateFlow()

    init {
        recomputeCipher()
    }

    fun setCipherInput(text: String) {
        _cipherInput.value = text
        recomputeCipher()
    }

    fun setCipherMode(mode: CipherMode) {
        _cipherMode.value = mode
        recomputeCipher()
    }

    fun toggleCipherDirection() {
        val currentInput = _cipherInput.value
        val currentOutput = _cipherOutput.value
        _isEmojiToText.value = !_isEmojiToText.value
        _cipherInput.value = currentOutput
        recomputeCipher()
    }

    fun clearCipher() {
        _cipherInput.value = ""
        _cipherOutput.value = ""
    }

    private fun recomputeCipher() {
        val text = _cipherInput.value
        if (text.isBlank()) {
            _cipherOutput.value = ""
            return
        }

        if (_isEmojiToText.value) {
            _cipherOutput.value = EmojiCipherAlgorithm.decode(text)
        } else {
            _cipherOutput.value = EmojiCipherAlgorithm.encode(text, _cipherMode.value)
        }
    }

    // Emoji Camera State
    private val _cameraPalette = MutableStateFlow(EmojiPalette.CLASSIC_COLORS)
    val cameraPalette = _cameraPalette.asStateFlow()

    private val _cameraMask = MutableStateFlow(EmojiMask.NONE)
    val cameraMask = _cameraMask.asStateFlow()

    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap = _capturedBitmap.asStateFlow()

    private val _capturedEmojiArt = MutableStateFlow<String?>(null)
    val capturedEmojiArt = _capturedEmojiArt.asStateFlow()

    private val _showCaptureDialog = MutableStateFlow(false)
    val showCaptureDialog = _showCaptureDialog.asStateFlow()

    fun setCameraPalette(palette: EmojiPalette) { _cameraPalette.value = palette }
    fun setCameraMask(mask: EmojiMask) { _cameraMask.value = mask }

    fun onPhotoCaptured(bitmap: Bitmap) {
        val grid = EmojiMosaicProcessor.processBitmapToEmojiGrid(
            bitmap = bitmap,
            gridCols = 24,
            gridRows = 30,
            palette = _cameraPalette.value,
            mask = _cameraMask.value
        )
        val textArt = EmojiMosaicProcessor.gridToTextArt(grid)
        val renderedBitmap = EmojiMosaicProcessor.renderGridToBitmap(grid)

        _capturedEmojiArt.value = textArt
        _capturedBitmap.value = renderedBitmap
        _showCaptureDialog.value = true
    }

    fun dismissCaptureDialog() {
        _showCaptureDialog.value = false
    }

    fun copyToClipboard(context: Context, text: String, message: String = "Скопировано в буфер обмена") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("TransMutate", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

class MainViewModelFactory(private val repository: HistoryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
