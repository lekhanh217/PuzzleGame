package com.example.puzzle_pieces.ui
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.graphics.*
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.puzzle_pieces.R
import com.example.puzzle_pieces.adapter.ADTCategoryImage
import com.example.puzzle_pieces.databinding.ActivityPictureByCategoryBinding
import com.example.puzzle_pieces.service.AppMusicService
import com.example.puzzle_pieces.utils.HandleWithFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CategoryWithImages : AppCompatActivity() {
    private lateinit var binding: ActivityPictureByCategoryBinding
    private lateinit var category : String;
    val Int.dp: Int
        get() = (this * Resources.getSystem().displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPictureByCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        this.category = intent.getStringExtra("category")!!;
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.imgIconBack.setOnClickListener { finish() }
        binding.tvCategoryName.text = this@CategoryWithImages.category.replaceFirstChar { it.uppercase() }
        binding.rcvImage.adapter = ADTCategoryImage(this);
        binding.rcvImage.layoutManager = GridLayoutManager(this,2);
        binding.rcvImage.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
            ){
                outRect.bottom = 5.dp;
                outRect.left =
                    3.dp;
                outRect.right = 3.dp;
            }
        })
        updateImageList()
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home){
            finish();
        }
        return super.onContextItemSelected(item)
    }
    fun updateImageList(){
        lifecycleScope.launch(Dispatchers.IO){
            val images = HandleWithFile().getImageListByCategory(this@CategoryWithImages, this@CategoryWithImages.category);
            lifecycleScope.launch(Dispatchers.Main) {
                (binding.rcvImage.adapter as ADTCategoryImage).setImageList(images, this@CategoryWithImages.category)
            }
        }
    }
    override fun onRestart() {
        (binding.rcvImage.adapter as ADTCategoryImage).updateStateGame();
        super.onRestart()
    }

}