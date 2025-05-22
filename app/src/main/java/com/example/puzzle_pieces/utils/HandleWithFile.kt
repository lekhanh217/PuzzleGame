package com.example.puzzle_pieces.utils

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.puzzle_pieces.model.AppState
import com.example.puzzle_pieces.model.Category
import com.example.puzzle_pieces.model.GameState
import com.example.puzzle_pieces.model.ImageInfo
import com.example.puzzle_pieces.model.MyWorkInfo
import com.example.puzzle_pieces.model.PuzzleBitmapSource
import com.example.puzzle_pieces.model.PuzzlePiece
import com.google.gson.Gson
import java.io.File
import java.io.IOException
import kotlin.collections.mutableListOf
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

class HandleWithFile {

    fun getImageListByCategory(context: Context, folder: String): List<ImageInfo> {

        val assetManager = context.assets
        val imageList = mutableListOf<ImageInfo>()
        try {
            val fullFolderPath = "imagecategory/" + folder
            val files = assetManager.list(fullFolderPath) ?: return emptyList()
            for (fileName in files) {
                val fullPath = "$fullFolderPath/$fileName"
                val inputStream = assetManager.open(fullPath)
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()
                val width = options.outWidth
                val height = options.outHeight

                val bitmap = assetManager.open(fullPath).use { bitmapStream ->
                    BitmapFactory.decodeStream(bitmapStream)
                }
                val gameState = loadGameState(context, fullPath);

                imageList.add(
                    ImageInfo(
                        gameState = gameState, name = fileName, width = width, height = height, fullPath, bitmap
                    )
                )
            }
        } catch (e: IOException) {
            Log.d("ngoai le doc file", "" + e.printStackTrace());
        }
        return imageList
    }

    fun getCompletedGameList(context: Context): List<MyWorkInfo> {
        val result = mutableListOf<MyWorkInfo>()
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("completedGameKey", Context.MODE_PRIVATE)
        val keys = sharedPreferences.getString("fileKey",null)
        if (keys == null) return listOf();
        else {
            val keyList = keys.split("<--->");
            val sharedPreferences: SharedPreferences =
                context.getSharedPreferences("completedGame", Context.MODE_PRIVATE)
            for (key in keyList) {
                Log.d("key:"+key,"");
                val completedGameInfos = sharedPreferences.getString(key, null)!!
                val completedGameList = completedGameInfos.split("<--->");
                val gameTime = completedGameList[0];
                val pieceAmount = completedGameList[1].toInt();
                var bitmap: Bitmap;
                val pathFull = completedGameList[2];
                if (assetExists(context, pathFull)) {
                    val inputStream = context.assets.open(pathFull);
                    bitmap = inputStream.use { bitmapStream ->
                        BitmapFactory.decodeStream(bitmapStream)
                    }
                } else {
                    bitmap = BitmapFactory.decodeFile(pathFull)
                }
                result.add(MyWorkInfo(key,gameTime,pieceAmount, bitmap));
            }
        }
        return result;
    }
    fun removeGameFinished(fileKey : String,context: Context){
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("completedGameKey", Context.MODE_PRIVATE)
        val keys = sharedPreferences.getString("fileKey",null);
        if(keys == null) throw IllegalArgumentException("Error")
        val keyList = mutableListOf<String>().apply { addAll(keys.split("<--->"))}
        keyList.remove(fileKey);
        var newKeys  = "";
        for(key in keyList){
            if(newKeys.equals("")) newKeys = key;
            else newKeys = key +"<--->"+ newKeys;
        }
        val fileKeyEditor = sharedPreferences.edit();
        if(newKeys.equals("") == false){
            fileKeyEditor.putString("fileKey",newKeys);
        }else fileKeyEditor.remove("fileKey");
        fileKeyEditor.apply();

        val finishedGame  = context.getSharedPreferences("completedGame", Context.MODE_PRIVATE)
        val finishedGameEditor = sharedPreferences.edit();
        val finishedGameInfo  = finishedGame.getString(fileKey,null)!!;
        val pathFull = finishedGameInfo.split("<--->")[2];
        finishedGameEditor.remove(fileKey)
        finishedGameEditor.apply()
        // check xem pathFull là ?
        if(assetExists(context,pathFull)) return;
        val dirList = context.cacheDir.listFiles()
        for(file in dirList){
            if(file.isFile && file.absolutePath.equals(pathFull)) return;
        }
        // check xem còn key nào trung pathFull khong
        for(key in keyList){
            val finishedGameInfo  = finishedGame.getString(key,null)!!;
            val otherFullPath = finishedGameInfo.split("<--->")[2];
            if(otherFullPath.equals(pathFull)) return;
        }
        File(pathFull).delete();
    }

    fun fetchCategoryCoverImages(context: Context): List<Category> {
        val result = mutableListOf<Category>()
        val assetManager = context.assets
        val categoryNames = assetManager.list("imagecategory")?.toList() ?: emptyList()
        for (categoryName in categoryNames) {
            val fullFolderPath = "imagecategory/" + categoryName
            val files = assetManager.list(fullFolderPath) ?: return emptyList()
            for (file in files) {
                if (file.startsWith("cover_category")) {
                    val fullFilePath = fullFolderPath + "/" + file;
                    try {
                        val inputStream = assetManager.open(fullFilePath);
                        val bitmap = inputStream.use { bitmapStream ->
                            BitmapFactory.decodeStream(bitmapStream)
                        }
                        result.add(Category(categoryName, bitmap, files.size));
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    break;
                }
            }
        }
        return result ?: emptyList()
    }

    fun resizeCropWithPreScale(original: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val targetRatio = targetWidth.toFloat() / targetHeight
        val scaleFactor = maxOf(
            targetWidth.toFloat() / original.width, targetHeight.toFloat() / original.height
        )
        val scaledWidth = (original.width * scaleFactor).roundToInt()
        val scaledHeight = (original.height * scaleFactor).roundToInt()
        val scaledBitmap = Bitmap.createScaledBitmap(original, scaledWidth, scaledHeight, true)
        val croppedBitmap = if (scaledWidth.toFloat() / scaledHeight > targetRatio) {
            val newWidth = (scaledHeight * targetRatio).roundToInt()
            val xOffset = (scaledWidth - newWidth) / 2
            Bitmap.createBitmap(scaledBitmap, xOffset, 0, newWidth, scaledHeight)
        } else {
            val newHeight = (scaledWidth / targetRatio).toInt()
            val yOffset = (scaledHeight - newHeight) / 2
            Bitmap.createBitmap(scaledBitmap, 0, yOffset, scaledWidth, newHeight)
        }
        return Bitmap.createScaledBitmap(croppedBitmap, targetWidth, targetHeight, true)
    }

    fun cropBitmapToPiecex(bitmapAssetFile: Bitmap, pieceInfoList: List<PuzzlePiece>, context: Context, size: String,updateProgress : (Int) -> Unit): List<PuzzleBitmapSource> {
        val result = mutableListOf<PuzzleBitmapSource>();
        var centerX = 0
        var centerY = 0
        var originalX = 0
        var stepX = 0
        var stepY = 0
        var amountElement = 0
        when (size) {
            "4*4" -> {
                centerX = 0
                centerY = -120
                amountElement = 16
                stepX = 320
                stepY = 240
                originalX = 160
            }

            "6*6" -> {
                centerX = 0
                centerY = -90
                amountElement = 36
                stepX = 240
                stepY = 180
                originalX = 120
            }

            "8*8" -> {
                centerX = 0
                centerY = -75
                amountElement = 64
                stepX = 200
                stepY = 150
                originalX = 100
            }

            "10*10" -> {
                centerX = 0
                centerY = -75
                amountElement = 100
                stepX = 200
                stepY = 150
                originalX = 100
            }
        }
        for (i in 1..amountElement) {
            if (i % sqrt(amountElement.toDouble()).roundToInt() == 1) {
                centerY += stepY;
                centerX = originalX;
            } else {
                centerX += stepX
            }
            var dirContainsPiece = "";
            if (size.equals("4*4")) dirContainsPiece = "piece4_4"
            if (size.equals("6*6")) dirContainsPiece = "piece6_6"
            if (size.equals("8*8")) dirContainsPiece = "piece8_8"
            if (size.equals("10*10")) dirContainsPiece = "piece10_10"

            for (piece in pieceInfoList) {
                if (piece.elementList.contains(i)) {
                    var resourceName = "";
                    if (size.equals("4*4")) resourceName = "${piece.symbol}44.png"
                    if (size.equals("6*6")) resourceName = "${piece.symbol}66.png"
                    if (size.equals("8*8")) resourceName = "${piece.symbol}88.png"
                    if (size.equals("10*10")) resourceName = "${piece.symbol}1010.png"

                    val croppedDrawBitmap =
                        context.assets.open("pieces/$dirContainsPiece/$resourceName")
                            .use { bitmapStream ->
                                BitmapFactory.decodeStream(bitmapStream)
                            }
                    val resultBitmap =
                        Bitmap.createBitmap(bitmapAssetFile, centerX - piece.offsetXFromCenter, centerY - piece.offsetYFromCenter, piece.width, piece.height)
                    for (y in 0 until croppedDrawBitmap.height) {
                        var x = 0;
                        while (x < croppedDrawBitmap.width) {
                            val srcPixel = croppedDrawBitmap.getPixel(x, y)
                            val r = Color.red(srcPixel)
                            if (r <= 190) {
                                resultBitmap.setPixel(x, y, srcPixel)
                            }
                            x++;
                        }
                    }

                    result.add(PuzzleBitmapSource(i, piece.offsetXFromCenter, piece.offsetYFromCenter, resultBitmap));
                    Log.d("bitmap create : " + i, "");
                    updateProgress(((i.toFloat()/amountElement)*100).toInt())
                }
            }
        }
        return result;
    }

    fun getLibraryImage(context: Context): List<ImageInfo> {
        val imageList = mutableListOf<ImageInfo>()
        val cacheFiles = context.cacheDir.listFiles() ?: return imageList;
        for (file in cacheFiles) {
            if (file.isFile) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                val gameState = loadGameState(context, file.absolutePath);
                imageList.add(ImageInfo(gameState, file.name, bitmap.width, bitmap.height, file.absolutePath, bitmap));
            }
        }
        return imageList
    }

    fun deleteLibraryImage(context: Context, filePath: String): Unit {
        val cacheFiles = context.cacheDir.listFiles() ?: return;
        for (file in cacheFiles) {
            if (file.isFile && file.absolutePath.equals(filePath)) {
                // check xem hiện tại game hoàn thành có đường dẫn file này không
                val sharedPreferences: SharedPreferences =
                    context.getSharedPreferences("completedGameKey", Context.MODE_PRIVATE)
                val completedGame: SharedPreferences =
                    context.getSharedPreferences("completedGame", Context.MODE_PRIVATE)


                val keys = sharedPreferences.getString("fileKey", null)
                if (keys != null) {
                    val keyList = keys.split("<--->");
                    var gameFinished = false;
                    for (key in keyList){
                    //    Log.d("key la:"+key,"")
                        val completedGameInfos = completedGame.getString(key,null)!!
                        val completedGameInfo = completedGameInfos.split("<--->");
                        val pathFull = completedGameInfo[2];
                        if (file.absolutePath.equals(pathFull)) {
                            gameFinished = true;
                            break;
                        }
                    }
                    if (gameFinished == true) {
                        val editor = completedGame.edit();
                        val targetDir = File(context.filesDir, "library_game_finished")
                        if (!targetDir.exists()) {
                            targetDir.mkdirs()
                        }
                        val targetFile = File(targetDir, file.name);
                        file.copyTo(targetFile, overwrite = true)
                        for (key in keyList) {
                            val completedGameInfos = completedGame.getString(key, null)!!
                            val completedGameInfo = completedGameInfos.split("<--->");
                            val pathFull = completedGameInfo[2];
                            if (file.absolutePath.equals(pathFull)) {
                                val timeGame = completedGameInfo[0];
                                val pieceAmount = completedGameInfo[1];
                                editor.putString(key, timeGame + "<--->" + pieceAmount + "<--->" + targetFile.absolutePath)
                            }
                        }
                        editor.apply()
                    }
                }
                file.delete();
                deleteGameState(context, filePath);
                return;
            }
        }
        return;
    }

    fun assetExists(context: Context, path: String): Boolean {
        return try {
            context.assets.open(path).close()
            true
        } catch (e: IOException) {
            false
        }
    }

    fun saveGameFinished(gameTime: String, pieceAmount: Int, pathFull: String, context: Context) {
        val newKey = "file" + System.currentTimeMillis();
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("completedGameKey", Context.MODE_PRIVATE)
        var keys = sharedPreferences.getString("fileKey",null)
        var newKeys = "";
        if(keys == null) newKeys = newKey;
        else newKeys = newKey + "<--->" + keys
        val editor = sharedPreferences.edit()
        editor.putString("fileKey", newKeys);
        editor.apply();
        val saveGame: SharedPreferences =
            context.getSharedPreferences("completedGame", Context.MODE_PRIVATE)
        val saveGameEditor = saveGame.edit();
        saveGameEditor.putString(newKey, gameTime + "<--->" + pieceAmount + "<--->" + pathFull);
        saveGameEditor.apply()
    }


    fun saveGameState(context: Context, gameState: GameState) {
        val gson = Gson()
        val json = gson.toJson(gameState)
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("game_state", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(gameState.filePath, json);
        editor.commit()
    }
    fun loadGameState(context: Context, filePath: String): GameState? {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("game_state", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString(filePath, null)
        return if (json != null) {
            val gson = Gson()
            gson.fromJson(json, GameState::class.java)
        } else {
            null // Trả về null nếu không tìm thấy giá trị với key tương ứng
        }
    }
    fun deleteGameState(context: Context, filePath: String) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("game_state", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove(filePath)
        editor.commit()
    }
    fun saveAppState(context: Context, appState: AppState) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("app_state", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("sound", appState.sound);
        editor.putBoolean("vibrate", appState.vibrate);
        editor.putString("gameBackground", appState.gameBackground);
        editor.commit()
    }
    fun loadAppState(context: Context): AppState {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("app_state", Context.MODE_PRIVATE)
        val sound = sharedPreferences.getBoolean("sound", false)
        val vibrate = sharedPreferences.getBoolean("vibrate", true)
        val gameBackground = sharedPreferences.getString("gameBackground", "dark")
        //cac value thu2 khi ta tải mở app lần đầu
        return AppState(sound, gameBackground!!, vibrate);
    }
}
