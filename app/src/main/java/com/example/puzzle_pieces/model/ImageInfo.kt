package com.example.puzzle_pieces.model

import android.graphics.Bitmap

data class ImageInfo(
    val gameState: GameState?,
    val name: String,
    val width: Int,
    val height: Int,
    val pathFull : String,
    val bitmap : Bitmap
)
