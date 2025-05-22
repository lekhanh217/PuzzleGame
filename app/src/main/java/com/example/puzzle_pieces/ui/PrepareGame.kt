package com.example.puzzle_pieces.ui

import android.app.ComponentCaller
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.puzzle_pieces.R
import com.example.puzzle_pieces.databinding.ActivityWaitingForGameBinding
import com.example.puzzle_pieces.model.PuzzleBitmapSource
import com.example.puzzle_pieces.model.PuzzlePiece
import com.example.puzzle_pieces.service.AppMusicService
import com.example.puzzle_pieces.utils.HandleWithFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.collections.hashSetOf

class PrepareGame : AppCompatActivity() {
    private var localImagePath : String? = null;
    private lateinit var source : String;
    private var gameSize = "";
    private lateinit var  binding : ActivityWaitingForGameBinding
    private val pieceList = mutableListOf<PuzzlePiece>();
    private lateinit var originalImgBitmap : Bitmap;
    private var finishActivity = false;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityWaitingForGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        localImagePath = intent.getStringExtra("localImagePath");
        gameSize  =intent.getStringExtra("gameSize")!!;
        source = intent.getStringExtra("source")!!;
        val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if(HandleWithFile().loadAppState(this).sound == true){
                startService(Intent(this,AppMusicService::class.java));
            }
            finish()
        }

        CoroutineScope(Dispatchers.IO).launch{
            var puzzleBitmapsForGame : List<PuzzleBitmapSource>? = null;
            if(source.equals("assetFile")){
                originalImgBitmap  = BitmapFactory.decodeStream(this@PrepareGame.assets.open(localImagePath!!));
            }else if(source.equals("croppedGalleryImage")) {  //croppedGalleryImage
                originalImgBitmap = BitmapFactory.decodeFile(localImagePath);
            }
            CoroutineScope(Dispatchers.Main).launch{
                binding.imgPicture.setImageBitmap(originalImgBitmap);
            }
            when(gameSize){
                "4*4"->{
                    if(originalImgBitmap.width!=1280 || originalImgBitmap.height !=960){
                        originalImgBitmap = HandleWithFile().resizeCropWithPreScale(originalImgBitmap,1280,960);
                    }
                    pieceList.add(PuzzlePiece("a",160,120,436,243,hashSetOf<Int>(1)))
                    pieceList.add(PuzzlePiece("b",164,120,328,327,hashSetOf<Int>(2)))
                    pieceList.add(PuzzlePiece("c",276,120,552,243,hashSetOf<Int>(3)))
                    pieceList.add(PuzzlePiece("d",164,120,324,327,hashSetOf<Int>(4)))
                    pieceList.add(PuzzlePiece("e",160,207,324,414,hashSetOf<Int>(5)))
                    pieceList.add(PuzzlePiece("f",276,123,552,246,hashSetOf<Int>(6,11)))
                    pieceList.add(PuzzlePiece("i",164,207,328,414,hashSetOf<Int>(7,10)))
                    pieceList.add(PuzzlePiece("j",276,123,436,246,hashSetOf<Int>(8)))
                    pieceList.add(PuzzlePiece("k",160,123,436,246,hashSetOf<Int>(9)))
                    pieceList.add(PuzzlePiece("l",164,207,324,414,hashSetOf<Int>(12)))
                    pieceList.add(PuzzlePiece("m",160,207,324,327,hashSetOf<Int>(13)))
                    pieceList.add(PuzzlePiece("n",276,123,552,243,hashSetOf<Int>(14)))
                    pieceList.add(PuzzlePiece("o",164,207,328,327,hashSetOf<Int>(15)))
                    pieceList.add(PuzzlePiece("s",276,123,436,243,hashSetOf<Int>(16)))
                    puzzleBitmapsForGame =  HandleWithFile().cropBitmapToPiecex(originalImgBitmap, pieceList, this@PrepareGame, "4*4",::updateProgressPercent);
                }
                "6*6"->{
                    if(originalImgBitmap.width!=1440 || originalImgBitmap.height !=1080){
                        originalImgBitmap = HandleWithFile().resizeCropWithPreScale(originalImgBitmap, 1440, 1080);
                    }
                    pieceList.add(PuzzlePiece("a",120,90,327,182,hashSetOf<Int>(1)))
                    pieceList.add(PuzzlePiece("b",122,90,244,245,hashSetOf<Int>(2,4)))
                    pieceList.add(PuzzlePiece("c",207,90,414,182,hashSetOf<Int>(3,5)))
                    pieceList.add(PuzzlePiece("d",122,90,242,245,hashSetOf<Int>(6)))
                    pieceList.add(PuzzlePiece("e",120,155,242,310,hashSetOf<Int>(7,19)))
                    pieceList.add(PuzzlePiece("f",207,92,414,184,hashSetOf<Int>(8,10,15,17,20,22,27,29)))
                    pieceList.add(PuzzlePiece("i",122,155,244,310,hashSetOf<Int>(9,11,14,16,21,23,26,28)))
                    pieceList.add(PuzzlePiece("j",207,92,327,184,hashSetOf<Int>(12,24)))
                    pieceList.add(PuzzlePiece("k",120,92,327,184,hashSetOf<Int>(13,25)))
                    pieceList.add(PuzzlePiece("l",122,155,242,310,hashSetOf<Int>(18,30)))
                    pieceList.add(PuzzlePiece("m",120,155,242,245,hashSetOf<Int>(31)))
                    pieceList.add(PuzzlePiece("n",207,92,414,182,hashSetOf<Int>(32,34)))
                    pieceList.add(PuzzlePiece("o",122,155,244,245,hashSetOf<Int>(33,35)))
                    pieceList.add(PuzzlePiece("s",207,92,327,182,hashSetOf<Int>(36)))
                    puzzleBitmapsForGame =  HandleWithFile().cropBitmapToPiecex(originalImgBitmap, pieceList, this@PrepareGame, "6*6",::updateProgressPercent);
                }
                "8*8"->{
                    if(originalImgBitmap.width!=1600 || originalImgBitmap.height !=1200){
                        originalImgBitmap = HandleWithFile().resizeCropWithPreScale(originalImgBitmap, 1600, 1200);
                    }
                    pieceList.add(PuzzlePiece("a",100,75,273,152,hashSetOf<Int>(1)))
                    pieceList.add(PuzzlePiece("b",103,75,206,205,hashSetOf<Int>(2,4,6)))
                    pieceList.add(PuzzlePiece("c",173,75,346,152,hashSetOf<Int>(3,5,7)))
                    pieceList.add(PuzzlePiece("d",103,75,203,205,hashSetOf<Int>(8)))
                    pieceList.add(PuzzlePiece("e",100,130,203,260,hashSetOf<Int>(9,25,41)))
                    pieceList.add(PuzzlePiece("f",173,77,346,154,hashSetOf<Int>(10,12,14,19,21,23,26,28,30,35,37,39,42,44,46,51,53,55)))
                    pieceList.add(PuzzlePiece("i",103,130,206,260,hashSetOf<Int>(11,13,15,18,20,22,27,29,31,34,36,38,43,45,47,50,52,54)))
                    pieceList.add(PuzzlePiece("j",173,77,273,154,hashSetOf<Int>(16,32,48)))
                    pieceList.add(PuzzlePiece("k",100,77,273,154,hashSetOf<Int>(17,33,49)))
                    pieceList.add(PuzzlePiece("l",103,130,203,260,hashSetOf<Int>(24,40,56)))
                    pieceList.add(PuzzlePiece("m",100,130,203,205,hashSetOf<Int>(57)))
                    pieceList.add(PuzzlePiece("n",173,77,346,152,hashSetOf<Int>(58,60,62)))
                    pieceList.add(PuzzlePiece("o",103,130,206,205,hashSetOf<Int>(59,61,63)))
                    pieceList.add(PuzzlePiece("s",173,77,273,152,hashSetOf<Int>(64)))
                    puzzleBitmapsForGame =  HandleWithFile().cropBitmapToPiecex(originalImgBitmap, pieceList, this@PrepareGame, "8*8",::updateProgressPercent);
                }
                "10*10"->{
                    if(originalImgBitmap.width!=2000 || originalImgBitmap.height !=1500){
                        originalImgBitmap = HandleWithFile().resizeCropWithPreScale(originalImgBitmap, 2000, 1500);
                    }
                    pieceList.add(PuzzlePiece("a",100,75,273,152,hashSetOf<Int>(1)))
                    pieceList.add(PuzzlePiece("b",103,75,206,205,hashSetOf<Int>(2,4,6,8)))
                    pieceList.add(PuzzlePiece("c",173,75,346,152,hashSetOf<Int>(3,5,7,9)))
                    pieceList.add(PuzzlePiece("d",103,75,203,205,hashSetOf<Int>(10)))
                    pieceList.add(PuzzlePiece("e",100,130,203,260,hashSetOf<Int>(11,31,51,71)))
                    pieceList.add(PuzzlePiece("f",173,77,346,154,hashSetOf<Int>(12,14,16,18,23,25,27,29,32,34,36,38,43,45,47,49,52,54,56,58,63,65,67,69,72,74,76,78,83,85,87,89)))
                    pieceList.add(PuzzlePiece("i",103,130,206,260 ,hashSetOf<Int>(13,15,17,19,22,24,26,28,33,35,37,39,42,44,46,48,53,55,57,59,62,64,66,68,73,75,77,79,82,84,86,88)))
                    pieceList.add(PuzzlePiece("j",173,77,273,154,hashSetOf<Int>(20,40,60,80)))
                    pieceList.add(PuzzlePiece("k",100,77,273,154,hashSetOf<Int>(21,41,61,81)))
                    pieceList.add(PuzzlePiece("l",103,130,203,260,hashSetOf<Int>(30,50,70,90)))
                    pieceList.add(PuzzlePiece("m",100,130,203,205,hashSetOf<Int>(91)))
                    pieceList.add(PuzzlePiece("n",173,77,346,152,hashSetOf<Int>(92,94,96,98)))
                    pieceList.add(PuzzlePiece("o",103,130,206,205,hashSetOf<Int>(93,95,97,99)))
                    pieceList.add(PuzzlePiece("s",173,77,273,152,hashSetOf<Int>(100)))
                    puzzleBitmapsForGame =  HandleWithFile().cropBitmapToPiecex(originalImgBitmap, pieceList, this@PrepareGame, "10*10",::updateProgressPercent);
                }
            }
            CoroutineScope(Dispatchers.Main).launch{
                if(finishActivity == true){
                    return@launch
                }else{
                    PuzzleGame.bitmapMap = HashMap<Int, PuzzleBitmapSource>();
                    for (element in puzzleBitmapsForGame!!) {
                        PuzzleGame.bitmapMap!!.put(element.index, element);
                    }
                    PuzzleGame.bitmapOriginalImg = originalImgBitmap;
                    val intent = Intent(this@PrepareGame, PuzzleGame::class.java)
                    intent.putExtra("gameSize", gameSize);
                    intent.putExtra("source", source);
                    intent.putExtra("localImagePath", localImagePath)
                    intent.putExtra("state", this@PrepareGame.intent.getStringExtra("state"))
                    startForResult.launch(intent)
                }
            }
        }
    }
    fun updateProgressPercent(percent : Int){
        lifecycleScope.launch(Dispatchers.Main){
            binding.progressBar.progress = percent
        }
    }

    override fun onDestroy() {
        finishActivity = true;
        super.onDestroy()
    }
}