package com.example.puzzle_pieces.adapter

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.puzzle_pieces.R
import com.example.puzzle_pieces.databinding.LibraryImageBinding
import com.example.puzzle_pieces.model.ImageInfo
import com.example.puzzle_pieces.ui.App
import com.example.puzzle_pieces.ui.CategoryWithImages
import com.example.puzzle_pieces.ui.CollectionFragment
import com.example.puzzle_pieces.ui.GameSizeActivity
import com.example.puzzle_pieces.ui.PrepareGame
import com.example.puzzle_pieces.utils.HandleWithFile
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ADTLibraryImage() : RecyclerView.Adapter<ADTLibraryImage.ViewHold>(){
    lateinit var context: Context;
    lateinit var fragment: CollectionFragment;
    var libraryImageList = mutableListOf<ImageInfo>();
    constructor(fragment: CollectionFragment) : this(){
        this.fragment = fragment
        this.context = fragment.requireContext();
    }

    fun setNewImageList(newImageList : List<ImageInfo>){
        this.libraryImageList.clear()
        this.libraryImageList.addAll(newImageList);
        notifyDataSetChanged();
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHold {
        val binding = LibraryImageBinding.inflate((context as App).layoutInflater);
        return ViewHold(binding);
    }

    override fun onBindViewHolder(holder: ViewHold, position: Int) {
        val image = libraryImageList!!.get(position);
        holder.binding.imgLibraryImg.setImageBitmap(image.bitmap);
        var finishedGame = 0;
        if(image.gameState == null){
            holder.binding.tvState.visibility = View.INVISIBLE;
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
            holder.binding.tvState.visibility = View.VISIBLE;
            holder.binding.tvState.text = "$finishedGame %";
        }
        holder.binding.imbOptionDelete.setOnClickListener {
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setCancelable(true)
            dialog.setContentView(R.layout.remove_picture_dialog)
            val btnCancel = dialog.findViewById(R.id.btnCancel) as AppCompatButton
            val btnYes = dialog.findViewById(R.id.btnYes) as AppCompatButton
            btnCancel.setOnClickListener{
                dialog.dismiss()
            }
            btnYes.setOnClickListener{
                (context as App).lifecycleScope.launch(Dispatchers.IO){
                    HandleWithFile().deleteLibraryImage(context,image.pathFull);
                    fragment.fetchImages();
                }
                dialog.dismiss();
            }
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.show()
        }

        holder.binding.root.setOnClickListener{
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
                    intent.putExtra("source","croppedGalleryImage");
                    intent.putExtra("gameSize",image.gameState!!.gameSize);
                    intent.putExtra("state", Gson().toJson(image.gameState));
                    context.startActivity(intent);
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
            intent.putExtra("source","croppedGalleryImage");
            context.startActivity(intent);
        }
    }

    override fun getItemCount(): Int {
        return libraryImageList.size;
    }
    class ViewHold(val binding : LibraryImageBinding) : RecyclerView.ViewHolder(binding.root);

}