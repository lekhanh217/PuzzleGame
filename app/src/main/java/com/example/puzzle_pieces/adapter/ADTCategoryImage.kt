package com.example.puzzle_pieces.adapter

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.puzzle_pieces.R
import com.example.puzzle_pieces.databinding.ImageByCategoryBinding
import com.example.puzzle_pieces.model.ImageInfo
import com.example.puzzle_pieces.ui.CategoryWithImages
import com.example.puzzle_pieces.ui.GameSizeActivity
import com.example.puzzle_pieces.ui.PrepareGame
import com.example.puzzle_pieces.utils.HandleWithFile
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class ADTCategoryImage(val context: Context) : RecyclerView.Adapter<ADTCategoryImage.ViewHold>(){
    private var imageInfoList  = mutableListOf<ImageInfo>();
    private var category : String = ""
    var gameStartPosition : Int? = null;
    fun setImageList(imageList : List<ImageInfo>,category : String){
        this.imageInfoList.addAll(imageList);
        this.category = category;
        notifyDataSetChanged();
    }

    override fun onBindViewHolder(holder: ADTCategoryImage.ViewHold, position: Int) {
        Log.d("onbind dc goi","");
        val image = imageInfoList!!.get(position);
        holder.binging.imgCategoryImg.setImageBitmap(image.bitmap);
        holder.binging.tvCategoryName.text = category.first().uppercase();
        var finishedGame = 0;
        if(image.gameState == null){
            holder.binging.tvState.visibility = View.INVISIBLE;
        }else{
            var exactlyBlockElement = 0;
            for(block in image.gameState.blockList){
                if(block.size >= 2) exactlyBlockElement += block.size
            };
            val achievedNumber = image.gameState.correctElement.size + exactlyBlockElement/2;
            when(image.gameState!!.gameSize){
                "4*4" ->{
                    finishedGame = (achievedNumber.toFloat()/16 * 100).toInt()
                }
                "6*6" ->{
                    finishedGame = (achievedNumber.toFloat()/36 * 100).toInt()
                }
                "8*8" ->{
                    finishedGame = (achievedNumber.toFloat()/64 * 100).toInt()
                }
                "10*10" ->{
                    finishedGame = achievedNumber
                }
            }
            holder.binging.tvState.visibility = View.VISIBLE;
            holder.binging.tvState.text = "$finishedGame %";
        }
        holder.binging.root.setOnClickListener {
            // if có state. hỏi user có muốn tiếp tục không !
            if(image.gameState != null){
                val dialog = Dialog(context)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                dialog.setCancelable(true)
                dialog.setContentView(R.layout.game_state_dialog)
                dialog.findViewById<TextView>(R.id.tvProgress).text = "In progress : $finishedGame %"
                val btnContinue = dialog.findViewById(R.id.btnContinue) as AppCompatButton
                val btnNewGame = dialog.findViewById(R.id.btnNewGame) as AppCompatButton

                btnContinue.setOnClickListener{
                    val intent = Intent(context, PrepareGame::class.java);
                    intent.putExtra("localImagePath",image.pathFull);
                    intent.putExtra("source","assetFile");
                    intent.putExtra("gameSize",image.gameState!!.gameSize);
                    intent.putExtra("state", Gson().toJson(image.gameState));
                    context.startActivity(intent);
                    gameStartPosition = position;
                    dialog.dismiss()
                }
                btnNewGame.setOnClickListener{
                    (context as CategoryWithImages).lifecycleScope.launch(Dispatchers.IO){
                        HandleWithFile().deleteGameState(context,image.pathFull);
                    }
                    val intent = Intent(context, GameSizeActivity::class.java);
                    intent.putExtra("localImagePath",image.pathFull);
                    intent.putExtra("source","assetFile");
                    context.startActivity(intent);
                    gameStartPosition = position;
                    dialog.dismiss();
                }
                dialog.window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                dialog.show()
                return@setOnClickListener
            }
            val intent = Intent(context, GameSizeActivity::class.java);
            intent.putExtra("localImagePath",image.pathFull);
            intent.putExtra("source","assetFile");
            context.startActivity(intent);
            gameStartPosition = position;
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ADTCategoryImage.ViewHold {
        val binging = ImageByCategoryBinding.inflate((context as CategoryWithImages).layoutInflater);
        return ViewHold(binging);
    }


    override fun getItemCount(): Int {
        if(imageInfoList == null) return 0;
        return imageInfoList!!.size;
    }

    fun updateStateGame(){
        (context as CategoryWithImages).lifecycleScope.launch(Dispatchers.Main){
            Log.d("states update","")
            val newGameStates = HandleWithFile().loadGameState(context,imageInfoList.get(gameStartPosition!!).pathFull);
            val imageInfo = imageInfoList.get(gameStartPosition!!);
            val newInfoGame = ImageInfo(newGameStates,imageInfo.name,imageInfo.width,imageInfo.height,imageInfo.pathFull,imageInfo.bitmap);
            imageInfoList[gameStartPosition!!] = newInfoGame;
            notifyItemChanged(gameStartPosition!!)
        }
    }

    class ViewHold(val binging : ImageByCategoryBinding) : RecyclerView.ViewHolder(binging.root)
}