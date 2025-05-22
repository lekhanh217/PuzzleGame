package com.example.puzzle_pieces.model

import android.graphics.Bitmap

data class PuzzleBitmapSource(val index : Int,
                              val offsetXFromCenter : Int,
                              val offsetYFromCenter : Int,
                              val bitmap : Bitmap)