package com.example.puzzle_pieces.model

data class GameState(val filePath : String ,val source : String, val gameSize : String,val correctElement: List<Int>,
                     val inCorrectElement : Map<Int, Pair<Float, Float>>,val blockList : List<Set<Int>>)