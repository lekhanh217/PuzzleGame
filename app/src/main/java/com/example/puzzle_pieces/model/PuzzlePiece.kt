package com.example.puzzle_pieces.model

data class PuzzlePiece(val symbol : String,
                       val offsetXFromCenter : Int,
                       val offsetYFromCenter : Int,
                       val width : Int,
                       val height : Int,
                       val elementList: HashSet<Int>
                       )