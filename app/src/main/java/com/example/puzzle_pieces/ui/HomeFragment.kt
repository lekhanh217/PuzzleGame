package com.example.puzzle_pieces.ui

import android.content.Intent
import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.puzzle_pieces.R
import com.example.puzzle_pieces.adapter.ADTCoverCategory
import com.example.puzzle_pieces.databinding.FragmentHomeBinding
import com.example.puzzle_pieces.service.AppMusicService
import com.example.puzzle_pieces.ui.App
import com.example.puzzle_pieces.utils.HandleWithFile
import com.example.puzzle_pieces.utils.MeasureScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

class HomeFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var binding: FragmentHomeBinding;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate((context as App).layoutInflater);
        val screenSize = MeasureScreen().getScreenSizeExcludingStatusBar(context as App);
            val imgContainerLayout = binding.cslGroup.layoutParams;
            imgContainerLayout.height = screenSize.first*5/11;
            binding.cslGroup.layoutParams = imgContainerLayout;

            binding.rcvCategoryList.adapter = ADTCoverCategory(requireContext());
            val layoutManager = GridLayoutManager(requireContext(),2);
            binding.rcvCategoryList.layoutManager = layoutManager;
            binding.rcvCategoryList.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
            ){
                outRect.top = 9.dp;
                outRect.left = 3.dp;
                outRect.right = 3.dp;
            }
        })
        fetchImages();
        val handler = Handler(Looper.getMainLooper())
        var index = 1;
        val runnable = object : Runnable {
            override fun run() {
                handler.postDelayed(this, 6000)
                index++;
                binding.rivDot1.setImageResource(R.color.grey)
                binding.rivDot2.setImageResource(R.color.grey)
                binding.rivDot3.setImageResource(R.color.grey)
                if(index%3 == 0 ){
                    startSlideAnimation(binding.cslImgContainer2, binding.cslImgContainer3)
                    binding.rivDot3.setImageResource(R.color.grey_bold)
                }
                if(index%3 == 1 ){
                    startSlideAnimation(binding.cslImgContainer3, binding.cslImgContainer1)
                    binding.rivDot1.setImageResource(R.color.grey_bold)
                }
                if(index%3 == 2 ){
                    startSlideAnimation(binding.cslImgContainer1, binding.cslImgContainer2)
                    binding.rivDot2.setImageResource(R.color.grey_bold)
                }
            }
        }
        handler.postDelayed(runnable, 6000)
        return binding.root
    }
    private fun startSlideAnimation(currentConst: ConstraintLayout, nextConst: ConstraintLayout) {
        val width = currentConst.width.toFloat();
        nextConst.translationX = width
        nextConst.visibility = View.VISIBLE

        currentConst.animate()
            .translationX(-width)
            .setDuration(600)
            .start()

        nextConst.animate()
            .translationX(0f)
            .setDuration(600)
            .withEndAction {
                currentConst.visibility = View.INVISIBLE
                nextConst.translationX = 0f
            }
            .start()
    }
    private fun fetchImages(){
        lifecycleScope.launch(Dispatchers.IO){
               val newList = HandleWithFile().fetchCategoryCoverImages(requireContext());
            lifecycleScope.launch(Dispatchers.Main){
                binding.progressBar.visibility = View.GONE;
                (binding.rcvCategoryList.adapter as ADTCoverCategory).setCategoryList(newList);
                fetchMusic();
            }
        }
    }
    fun fetchMusic(){
        lifecycleScope.launch(Dispatchers.IO){
            val state = HandleWithFile().loadAppState(requireContext())
            if(state.sound){
                lifecycleScope.launch(Dispatchers.Main) {
                    requireContext().startService(Intent(requireContext(), AppMusicService::class.java))
                }
            }
        }
    }
}