package com.example.data.camera

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.sqrt

enum class EmojiPalette(val title: String, val emojisWithColors: List<Pair<String, Int>>) {
    CLASSIC_COLORS(
        "Классические цвета",
        listOf(
            "⬛" to Color.rgb(15, 15, 18),
            "⬜" to Color.rgb(240, 240, 245),
            "🟥" to Color.rgb(220, 38, 38),
            "🟧" to Color.rgb(234, 88, 12),
            "🟨" to Color.rgb(234, 179, 8),
            "🟩" to Color.rgb(22, 163, 74),
            "🟦" to Color.rgb(37, 99, 235),
            "🟪" to Color.rgb(147, 51, 234),
            "🟫" to Color.rgb(120, 53, 15)
        )
    ),
    NATURE_ELEMENTS(
        "Природа и стихии",
        listOf(
            "🌑" to Color.rgb(20, 20, 25),
            "☁️" to Color.rgb(230, 235, 245),
            "🔥" to Color.rgb(240, 68, 20),
            "☀️" to Color.rgb(250, 204, 21),
            "🌲" to Color.rgb(20, 110, 40),
            "🌊" to Color.rgb(14, 116, 200),
            "🌸" to Color.rgb(244, 114, 182),
            "🍂" to Color.rgb(180, 83, 9),
            "❄️" to Color.rgb(186, 230, 253)
        )
    ),
    EMOTIONS(
        "Эмоции и лица",
        listOf(
            "💀" to Color.rgb(30, 30, 30),
            "👻" to Color.rgb(240, 240, 240),
            "😡" to Color.rgb(225, 29, 72),
            "🤠" to Color.rgb(217, 119, 6),
            "😀" to Color.rgb(250, 204, 21),
            "🤢" to Color.rgb(34, 197, 94),
            "🥶" to Color.rgb(56, 189, 248),
            "😈" to Color.rgb(168, 85, 247),
            "😎" to Color.rgb(100, 116, 139)
        )
    ),
    CYBERPUNK(
        "Киберпанк Неон",
        listOf(
            "🖤" to Color.rgb(5, 5, 10),
            "🤍" to Color.rgb(245, 245, 255),
            "⚡" to Color.rgb(255, 230, 0),
            "👾" to Color.rgb(192, 38, 211),
            "💎" to Color.rgb(6, 182, 212),
            "💚" to Color.rgb(16, 185, 129),
            "💜" to Color.rgb(139, 92, 246),
            "🦄" to Color.rgb(244, 63, 94)
        )
    )
}

enum class EmojiMask(val title: String, val icon: String, val overlayDescription: String) {
    NONE("Без маски", "🚫", "Чистая палитра"),
    COOL_CAT("Кот и усы", "🐱", "Кошачья мордочка и лапки"),
    CYBER_ALIEN("Кибер-Пришелец", "👽", "НЛО и неоновые антенны"),
    SUPER_STAR("Суперзвезда", "😎", "Тёмные очки и сияние звезд"),
    DEVIL_HORNS("Демон", "😈", "Огненные рожки и трезубец"),
    MATRIX_STREAM("Матрица", "🟢", "Символы цифрового дождя")
}

object EmojiMosaicProcessor {

    /**
     * Превращает Bitmap изображения в двумерную сетку эмодзи по цветовой палитре
     */
    fun processBitmapToEmojiGrid(
        bitmap: Bitmap,
        gridCols: Int = 24,
        gridRows: Int = 30,
        palette: EmojiPalette = EmojiPalette.CLASSIC_COLORS,
        mask: EmojiMask = EmojiMask.NONE
    ): List<List<String>> {
        val scaled = Bitmap.createScaledBitmap(bitmap, gridCols, gridRows, true)
        val result = mutableListOf<List<String>>()

        val midRow = gridRows / 3
        val midCol = gridCols / 2

        for (y in 0 until gridRows) {
            val row = mutableListOf<String>()
            for (x in 0 until gridCols) {
                // Check if mask overlay applies to this coordinate
                val maskEmoji = getMaskEmojiAt(x, y, midCol, midRow, mask)
                if (maskEmoji != null) {
                    row.add(maskEmoji)
                    continue
                }

                val pixel = scaled.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                val bestEmoji = findClosestEmoji(r, g, b, palette)
                row.add(bestEmoji)
            }
            result.add(row)
        }

        if (scaled != bitmap) {
            scaled.recycle()
        }

        return result
    }

    /**
     * Конвертирует сетку в строку для копирования в мессенджеры
     */
    fun gridToTextArt(grid: List<List<String>>): String {
        return grid.joinToString("\n") { row -> row.joinToString("") }
    }

    /**
     * Создает Bitmap с отрисованными эмодзи на холсте для сохранения как картинки
     */
    fun renderGridToBitmap(
        grid: List<List<String>>,
        cellPx: Int = 36,
        backgroundColor: Int = Color.BLACK
    ): Bitmap {
        val rows = grid.size
        val cols = if (rows > 0) grid[0].size else 0
        val width = cols * cellPx
        val height = rows * cellPx

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        canvas.drawColor(backgroundColor)

        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            textSize = cellPx * 0.75f
            textAlign = android.graphics.Paint.Align.CENTER
        }

        val yOffset = (cellPx / 2f) - ((paint.descent() + paint.ascent()) / 2f)

        for (r in 0 until rows) {
            val rowList = grid[r]
            for (c in 0 until rowList.size) {
                val emoji = rowList[c]
                val x = c * cellPx + (cellPx / 2f)
                val y = r * cellPx + yOffset
                canvas.drawText(emoji, x, y, paint)
            }
        }

        return output
    }

    private fun findClosestEmoji(r: Int, g: Int, b: Int, palette: EmojiPalette): String {
        var minDistance = Double.MAX_VALUE
        var bestEmoji = palette.emojisWithColors.first().first

        for ((emoji, color) in palette.emojisWithColors) {
            val er = Color.red(color)
            val eg = Color.green(color)
            val eb = Color.blue(color)

            // Weighted Euclidean distance for human perception
            val dr = (r - er).toDouble()
            val dg = (g - eg).toDouble()
            val db = (b - eb).toDouble()
            val distance = (dr * dr * 0.299) + (dg * dg * 0.587) + (db * db * 0.114)

            if (distance < minDistance) {
                minDistance = distance
                bestEmoji = emoji
            }
        }

        return bestEmoji
    }

    private fun getMaskEmojiAt(x: Int, y: Int, midCol: Int, midRow: Int, mask: EmojiMask): String? {
        return when (mask) {
            EmojiMask.NONE -> null
            EmojiMask.COOL_CAT -> {
                // Cat ears and paws
                if (y == midRow - 4 && (x == midCol - 4 || x == midCol + 4)) "🐱"
                else if (y == midRow && (x == midCol - 2 || x == midCol + 2)) "✨"
                else if (y == midRow + 4 && (x == midCol - 3 || x == midCol + 3)) "🐾"
                else null
            }
            EmojiMask.SUPER_STAR -> {
                // Sunglasses over face area
                if (y == midRow && (x in (midCol - 3)..(midCol + 3))) "🕶️"
                else if (y == midRow - 3 && (x == midCol - 4 || x == midCol + 4)) "⭐"
                else null
            }
            EmojiMask.CYBER_ALIEN -> {
                if (y == midRow - 4 && x == midCol) "🛸"
                else if (y == midRow && (x == midCol - 3 || x == midCol + 3)) "👾"
                else null
            }
            EmojiMask.DEVIL_HORNS -> {
                if (y == midRow - 4 && (x == midCol - 3 || x == midCol + 3)) "🔥"
                else if (y == midRow + 4 && x == midCol) "🔱"
                else null
            }
            EmojiMask.MATRIX_STREAM -> {
                if ((x + y * 3) % 11 == 0) "🟢"
                else null
            }
        }
    }
}
