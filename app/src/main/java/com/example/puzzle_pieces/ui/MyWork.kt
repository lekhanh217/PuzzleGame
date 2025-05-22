package com.example.puzzle_pieces.ui

import android.app.Dialog
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.puzzle_pieces.R
import com.example.puzzle_pieces.adapter.ADTCoverCategory
import com.example.puzzle_pieces.adapter.ADTMyWork
import com.example.puzzle_pieces.databinding.ActivityMyWorkBinding
import com.example.puzzle_pieces.model.MyWorkInfo
import com.example.puzzle_pieces.utils.HandleWithFile
import com.example.puzzle_pieces.utils.MeasureScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyWork : AppCompatActivity() {
    var showKeyFile = "";
    fun dpToPx(dp: Int): Int {
        val density = Resources.getSystem().displayMetrics.density
        return (dp * density).toInt()
    }

    private lateinit var binding :ActivityMyWorkBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMyWorkBinding.inflate(layoutInflater);
        setContentView(binding.root);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.imbBack.setOnClickListener { finish() }
        binding.rcvMywork.adapter = ADTMyWork(this,::fetchData,::showFinishedGameDetail);
        binding.rcvMywork.layoutManager = GridLayoutManager(this,2)
        binding.rcvMywork.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
            ){
                outRect.bottom = 9.dp;
                outRect.left = 3.dp;
                outRect.right = 3.dp;
            }
        })
        binding.clShowFinishedGameDetail.setOnClickListener {
            binding.clShowFinishedGameDetail.visibility = View.GONE;
            binding.clShowGameFinishedList.alpha = 1f
        }
        binding.imbCloseGameDetail.setOnClickListener {
            binding.clShowFinishedGameDetail.visibility = View.GONE;
            binding.clShowGameFinishedList.alpha = 1f
        }
        binding.imgDeleteFinishedGame.setOnClickListener {
            binding.clShowGameFinishedList.alpha = 1f
            binding.clShowFinishedGameDetail.visibility = View.GONE;
            deleteGameFinish()
        }
        val screenSize = MeasureScreen().getScreenSizeExcludingStatusBar(this);
        val cardViewWidth = screenSize.first - dpToPx(32);
        val cardViewLayout = binding.cvShowDetailGame.layoutParams;
        cardViewLayout.height = cardViewWidth/38*39;
        binding.cvShowDetailGame.layoutParams = cardViewLayout;

        val layoutImageGameFinished = binding.imgGameFinishedDetail.layoutParams;
        layoutImageGameFinished.height = cardViewWidth/4*3;
        binding.imgGameFinishedDetail.layoutParams = layoutImageGameFinished;
        fetchData();
    }
    fun fetchData(){
        lifecycleScope.launch(Dispatchers.IO){
            val newList = HandleWithFile().getCompletedGameList(this@MyWork)
            lifecycleScope.launch(Dispatchers.Main){
                if(newList.size == 0){
                    binding.clShowEmptyList.visibility = View.VISIBLE
                }else{
                    binding.clShowEmptyList.visibility = View.GONE
                }
                (binding.rcvMywork.adapter as ADTMyWork).setMyworkList(newList);
            }
        }
    }
    fun showFinishedGameDetail(myWorkInfo: MyWorkInfo){
        binding.clShowFinishedGameDetail.visibility = View.VISIBLE;
        binding.clShowGameFinishedList.alpha = 0.7f
        this.showKeyFile = myWorkInfo.fileKey;
        val times = myWorkInfo.gameTime.split(":");
        if(times[0].equals("00")){
            binding.tvFinishedGameTime.text = "You completed the game in "+times[1]+":"+times[2];
        }else {
            binding.tvFinishedGameTime.text = "You completed the game in "+myWorkInfo.gameTime
        }
        binding.imgGameFinishedDetail.setImageBitmap(myWorkInfo.bitmap);
    }
    fun deleteGameFinish(){
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.remove_picture_dialog)
        val btnCancel = dialog.findViewById(R.id.btnCancel) as AppCompatButton
        val btnYes = dialog.findViewById(R.id.btnYes) as AppCompatButton
        btnCancel.setOnClickListener{
            dialog.dismiss()
        }
        btnYes.setOnClickListener{
             lifecycleScope.launch(Dispatchers.IO){
                HandleWithFile().removeGameFinished(showKeyFile,this@MyWork);
                fetchData();
            }
            dialog.dismiss();
            fetchData()
        }
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.show()
    }
}