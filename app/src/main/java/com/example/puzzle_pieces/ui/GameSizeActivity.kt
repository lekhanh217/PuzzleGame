package com.example.puzzle_pieces.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import com.example.puzzle_pieces.R
import com.example.puzzle_pieces.databinding.ActivityGameSizeBinding
import com.example.puzzle_pieces.utils.MeasureScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GameSizeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameSizeBinding
    private var gameSize: String = "6*6";
    private lateinit var source: String;
    private lateinit var localImagePath: String;
    private lateinit var bitmap: Bitmap;
    private var lastX = 0f;
    private var allowMoveLL = false;
    private lateinit var llLeverGameContainer: LinearLayout;


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityGameSizeBinding.inflate(layoutInflater);
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val screenSize = MeasureScreen().getScreenSizeExcludingStatusBar(this);
        binding.imgBack.setOnClickListener { finish() }
        llLeverGameContainer = binding.llLeverGameContainer;
        source = this.intent.getStringExtra("source")!!
        localImagePath = this.intent.getStringExtra("localImagePath")!!
        CoroutineScope(Dispatchers.IO).launch {
            if (source.equals("assetFile")) {
                bitmap =
                    BitmapFactory.decodeStream(this@GameSizeActivity.assets.open(localImagePath!!));
            } else if (source.equals("croppedGalleryImage")) {  //croppedGalleryImage
                bitmap = BitmapFactory.decodeFile(localImagePath);
            }
            CoroutineScope(Dispatchers.Main).launch {
                binding.imgPicture.setImageBitmap(bitmap)
            }
        }

        llLeverGameContainer.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    allowMoveLL = true;
                    lastX = event.rawX;
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    allowMoveLL = false
                }

                MotionEvent.ACTION_MOVE -> {
                    if (allowMoveLL == true) {
                        val movX = event.rawX - lastX;
                        for (i in 0 until llLeverGameContainer.childCount){
                            if (i == 0) {
                                val centerViewX1 =
                                    binding.lllever44Container.x + (binding.lllever44Container.width / 2);
                                val centerViewX2 =
                                    binding.lllever1010Container.x + (binding.lllever1010Container.width / 2);
                                if (centerViewX1 > screenSize.first / 2 && movX > 0) break;
                                if (centerViewX2 < screenSize.first / 2 && movX < 0) break;
                            }
                            val child = llLeverGameContainer.getChildAt(i);
                            if (child is ConstraintLayout == false) continue;
                            child.x += movX
                            if (child is ConstraintLayout) {
                                val centerViewX = child.x + child.width / 2;
                                if (centerViewX > screenSize.first / 3 && centerViewX < screenSize.first / 3 * 2) {
                                    val scale =
                                        1 + ((screenSize.first / 6 - Math.abs(screenSize.first / 2 - centerViewX)) / (screenSize.first / 6)) / 2;
                                    child.scaleY = scale;
                                    child.scaleX = scale;
                                }
                            }
                        }
                        lastX = event.rawX;
                    }
                }

                MotionEvent.ACTION_UP -> {
                    allowMoveLL = false;
                    var minToScreenCenter = 99999f;
                    var elementToZoom: View? = null;
                    for (i in 0 until llLeverGameContainer.childCount) {
                        val child = llLeverGameContainer.getChildAt(i);
                        if (child is ConstraintLayout == false) continue;
                        val toScreenCenter =
                            Math.abs(child.x + child.width / 2 - screenSize.first / 2);
                        if (toScreenCenter < minToScreenCenter) {
                            minToScreenCenter = toScreenCenter;
                            elementToZoom = child;
                        }
                    }
                    val distanceX =
                        screenSize.first / 2 - elementToZoom!!.x - (elementToZoom.height / 2);
                    val animators = mutableListOf<Animator>()
                    for (element in llLeverGameContainer.children) {
                        val ElementAnimX =
                            ObjectAnimator.ofFloat(element, "x", element.x + distanceX)
                        animators.add(ElementAnimX)
                        if (element == elementToZoom) {
                            for (i in 0 until (element as ConstraintLayout).childCount) {
                                val child = element.getChildAt(i)
                                if (child is TextView) {
                                    when (child.id) {
                                        R.id.tvGame1010 -> gameSize = "10*10"
                                        R.id.tvGame88 -> gameSize = "8*8"
                                        R.id.tvGame66 -> gameSize = "6*6"
                                        R.id.tvGame44 -> gameSize = "4*4"
                                    }
                                    break;
                                }
                            }
                            val scaleX = ObjectAnimator.ofFloat(element, "scaleX", 1.5f)
                            val scaleY = ObjectAnimator.ofFloat(element, "scaleY", 1.5f)
                            animators.add(scaleX)
                            animators.add(scaleY)
                        } else {
                            val scaleX = ObjectAnimator.ofFloat(element, "scaleX", 1f)
                            val scaleY = ObjectAnimator.ofFloat(element, "scaleY", 1f)
                            animators.add(scaleX)
                            animators.add(scaleY)
                        }
                        AnimatorSet().apply {
                            playTogether(animators)
                            duration = 200
                            start()
                        }
                    }
                }
            }
            true
        }
        binding.btnPlayGame.setOnClickListener {
            if (gameSize.equals("") == false) {
                val intent = Intent(this, PrepareGame::class.java);
                intent.putExtra("gameSize", gameSize);
                intent.putExtra("localImagePath", this.intent.getStringExtra("localImagePath"));
                intent.putExtra("source", this.intent.getStringExtra("source"));
                startActivity(intent);
                finish();
            }
        }
    }
}