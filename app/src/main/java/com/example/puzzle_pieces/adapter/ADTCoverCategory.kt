package com.example.puzzle_pieces.adapter

import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.puzzle_pieces.databinding.ImageHomeBinding
import com.example.puzzle_pieces.model.Category
import com.example.puzzle_pieces.ui.App
import com.example.puzzle_pieces.ui.CategoryWithImages
import kotlin.jvm.java

class ADTCoverCategory(val context: Context) : RecyclerView.Adapter<ADTCoverCategory.ViewHold>(){
    private var categoryList : MutableList<Category> = mutableListOf();
    fun setCategoryList(newCategoryList : List<Category>){
        this.categoryList.addAll(newCategoryList);
        notifyDataSetChanged();
    }

    override fun onBindViewHolder(holder: ADTCoverCategory.ViewHold, position: Int) {
        val element = categoryList!!.get(position);
        holder.binging.imgCategoryImg.setImageBitmap(element.bitmap);
        val category = element.categoryName.replaceFirstChar { it.uppercaseChar() }
        holder.binging.tvPictureAmount.text = "$category (${element.pictureAmount} picture)";
        holder.binging.root.setOnClickListener {
            val intent = Intent(context, CategoryWithImages::class.java);
            intent.putExtra("category",categoryList!!.get(position).categoryName);
            context.startActivity(intent);
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ADTCoverCategory.ViewHold {
        val binging = ImageHomeBinding.inflate((context as App).layoutInflater);
        return ViewHold(binging);
    }


    override fun getItemCount(): Int {
        return categoryList.size;
    }
    class ViewHold(val binging : ImageHomeBinding) : RecyclerView.ViewHolder(binging.root)
}