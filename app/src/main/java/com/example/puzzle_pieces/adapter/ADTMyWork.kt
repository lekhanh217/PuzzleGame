package com.example.puzzle_pieces.adapter

import android.app.Dialog
import android.graphics.Bitmap
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.puzzle_pieces.R
import com.example.puzzle_pieces.databinding.MyWorkItemBinding
import com.example.puzzle_pieces.model.MyWorkInfo
import com.example.puzzle_pieces.ui.MyWork
import com.example.puzzle_pieces.utils.HandleWithFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ADTMyWork(val activity: MyWork,val fetchData :()-> Unit,val showFinishedGameDetail : (MyWorkInfo) -> Unit) : RecyclerView.Adapter<ADTMyWork.ViewHold>(){
    private val myWorkList = mutableListOf<MyWorkInfo>()
    fun setMyworkList(newList : List<MyWorkInfo>){
        this.myWorkList.clear()
        this.myWorkList.addAll(newList);
        notifyDataSetChanged();
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ADTMyWork.ViewHold {
        val binding = MyWorkItemBinding.inflate(activity.layoutInflater);
        return ViewHold(binding);
    }

    override fun onBindViewHolder(holder: ADTMyWork.ViewHold, position: Int) {
           val element = myWorkList.get(position);
           val binding = holder.binding;
           binding.imgMywork.setImageBitmap(element.bitmap);
           binding.imgMywork.setOnClickListener{
               showFinishedGameDetail(element);
           }
           binding.tvGameTime.text = element.gameTime
           binding.tvPieceAmount.text = ""+element.pieceAmount;
    }

    override fun getItemCount(): Int {
        return myWorkList.size;
    }
    class ViewHold(val binding : MyWorkItemBinding) : RecyclerView.ViewHolder(binding.root);
}