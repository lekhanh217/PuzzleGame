package com.example.puzzle_pieces.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.puzzle_pieces.R
import com.example.puzzle_pieces.databinding.ActivityPuzzleGameBinding
import com.example.puzzle_pieces.model.GameState
import com.example.puzzle_pieces.model.PuzzleBitmapSource
import com.example.puzzle_pieces.onclick.OnPuzzleGameElement
import com.example.puzzle_pieces.service.AppMusicService
import com.example.puzzle_pieces.service.GameMusicService
import com.example.puzzle_pieces.ui.PrepareGame
import com.example.puzzle_pieces.utils.HandleWithFile
import com.example.puzzle_pieces.utils.MeasureScreen
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PuzzleGame : AppCompatActivity() {
    private var isVibrate: Boolean = true;
    private var screenToBitmapWidthRatio = -1f;
    private val imgCroppedMap = HashMap<Int, Pair<PuzzleBitmapSource, ImageView>>();
    private lateinit var binding: ActivityPuzzleGameBinding
    private val startTime = System.currentTimeMillis();
    private val correctlyPlacedElements = HashSet<ImageView>();
    private val lockedMatrixElements = HashSet<ImageView>();
    private lateinit var scaleGestureDetector: ScaleGestureDetector;
    private lateinit var originalImageView: ImageView;
    private var zoomDividerLine = 0
    private var allowMoveMainImg = false;
    private var allowMoveViewCropped = HashMap<ImageView, Boolean>();
    private var proposedPivotX: Float? = null;
    private var proposedPivotY: Float? = null;
    private lateinit var gameSize: String;
    private lateinit var locationRootView : IntArray;
    private lateinit var screenSize: Pair<Int, Int>;
    private var elementBlocks = HashSet<HashSet<ImageView>>();
    private var isInAnimation = false;

    companion object {
        var bitmapMap: HashMap<Int, PuzzleBitmapSource>? = null;
        var bitmapOriginalImg: Bitmap? = null;
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPuzzleGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        screenSize = MeasureScreen().getScreenSizeExcludingStatusBar(this);
        gameSize = intent.getStringExtra("gameSize")!!
        screenToBitmapWidthRatio = screenSize.first.toFloat() / bitmapOriginalImg!!.width
        originalImageView = binding.imgMainImage;

        val drawable = BitmapDrawable(resources, bitmapOriginalImg)
        originalImageView.setBackgroundDrawable(drawable)
        binding.imgBack.setOnClickListener {
            (binding.imgBack.context as PuzzleGame).finish();
        }
        binding.imgUpdateMusic.setOnClickListener {
            updateMusic()
        }
        binding.imgUpdatePosition.setOnClickListener {
            returnToStartPosition(false);
        }
        binding.imgIconReset.setOnClickListener {
            newDisplayPieces(12, 1);
        }
        binding.ibnHideElement.setOnClickListener {
            hideIncorrectElement()
        }
        binding.imbChangeBackground.setOnClickListener {
            changeBackGround();
        }
        initPiecesPuzzle();
        registerCropViewEvents()
        inittializeGestureDetector();
        setEventOriginalImage();
        binding.viewZoomDividerLine.setOnTouchListener { view, event -> true }
        binding.llBarAction.setOnTouchListener { view, event -> true }
        updateLayout();
        updateGameStates();
        AppMusicService.stopMusic(this)
    }

    fun initPiecesPuzzle() {
        for (key in bitmapMap!!.keys) { // key la 1-36 neu game 6^6 , 1-64 neu 8*8
            val element = bitmapMap!![key]!!
            val imageView = ImageView(this).apply {
                layoutParams =
                    ConstraintLayout.LayoutParams(element.bitmap.width, element.bitmap.height)
                        .apply {
                            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                            topMargin = 0
                            marginStart = 0
                        }
                elevation = 0f;
            }
            imageView.visibility = View.INVISIBLE;
            imageView.setImageBitmap(element.bitmap);
            binding.main.addView(imageView)
            imgCroppedMap.put(key, Pair(element, imageView));
            elementBlocks.add(HashSet<ImageView>().apply { add(imageView) });
        }
    }

    fun inittializeGestureDetector() {
        scaleGestureDetector =
            ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    allowMoveMainImg = false
                    val allMatrixDynamicView = getAllMatrixDynamicView();
                    if (proposedPivotX != null) {
                        // lần đầu k phóng.
                        if (allMatrixDynamicView != null) {
                            updateMainImgDynamicElements(allMatrixDynamicView)
                        };
                        updateNewViewPosition(originalImageView);
                        lockedMatrixElements.forEach { element ->
                            updateNewViewPosition(element);
                        }
                        proposedPivotX = null;
                        proposedPivotX = null;
                        return true;
                    }

                    ////////////////////////////
                    var newScale = detector.scaleFactor;
                    if (originalImageView.width * originalImageView.scaleX > screenSize.first * 2.5 && newScale > 1) return true;
                    if (originalImageView.width * originalImageView.scaleX <= screenSize.first * 0.75 && newScale < 1) return true;
                    if (newScale > 1.03) newScale = 1.03f;
                    if (newScale < 0.97) newScale = 0.97f;
                    originalImageView.scaleX += newScale - 1;
                    originalImageView.scaleY += newScale - 1;
                    // gan lai dung vi tri cac phan tu dong vao ma tran truoc khi zoom
                    if (allMatrixDynamicView != null) updateMainImgDynamicElements(allMatrixDynamicView);
                    for (element in lockedMatrixElements) {
                        element.scaleX = originalImageView.scaleX;
                        element.scaleY = originalImageView.scaleY;
                    }
                    return super.onScale(detector)
                }
            })

    }

    fun setEventOriginalImage() {
        var lastX = 0f
        var lastY = 0f
        originalImageView.setOnTouchListener { view, event ->
            scaleGestureDetector.onTouchEvent(event);
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    allowMoveMainImg = true;
                    for (block in elementBlocks) {
                        for (blockElement in block) {
                            allowMoveViewCropped[blockElement] = false;
                        }
                    }
                    lastX = event.rawX;
                    lastY = event.rawY;
                    proposedPivotX = event.rawX - locationRootView[0];
                    proposedPivotY = event.rawY - locationRootView[0];
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    allowMoveMainImg = false
                }

                MotionEvent.ACTION_MOVE -> {
                    if (allowMoveMainImg == true) {
                        view.x = event.rawX - lastX + view.x
                        view.y = event.rawY - lastY + view.y
                        proposedPivotX = event.rawX - locationRootView[0];
                        proposedPivotY = event.rawY - locationRootView[0];
                        val allDynamicMatrixElements = getAllMatrixDynamicView();
                        if (allDynamicMatrixElements != null) {
                            updateMainImgDynamicElements(allDynamicMatrixElements)
                        };
                        for (element in lockedMatrixElements) {
                            element.x = event.rawX - lastX + element.x
                            element.y = event.rawY - lastY + element.y
                        }
                        lastX = event.rawX;
                        lastY = event.rawY;
                    }
                }

                MotionEvent.ACTION_UP -> {
                    allowMoveMainImg = false;
                }
            }
            true
        }
    }

    fun registerCropViewEvents() {
        for (key in imgCroppedMap.keys) { // key la 1-36 neu la 6*6, 1-16 neu 4*4
            val element = imgCroppedMap[key]!!.second as ImageView
            var lastX = 0f
            var lastY = 0f
            allowMoveViewCropped.put(element, false);
            element.setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastX = event.rawX;
                        lastY = event.rawY;
                        allowMoveMainImg = false;
                        for (block in elementBlocks) {
                            for (blockElement in block) {
                                allowMoveViewCropped[blockElement] = false;
                                if (block.contains(element)) blockElement.bringToFront();
                            }
                        }
                        allowMoveViewCropped[element] = true
                    }

                    MotionEvent.ACTION_POINTER_UP -> {
                        allowMoveViewCropped[element] = false;
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (allowMoveViewCropped[element] == true) {
                            val blockContainElement = getBlockContainsElement(element);
                            if (event.rawY > zoomDividerLine) {
                                    updateUnderlinePosition(element, event.rawX - lastX, event.rawY - lastY, event.rawX, event.rawY);
                            } else {
                                // check có bao nhieu view đang hiển thị bên dưới, nếu <=3 thì cho thêm 9
                                var countUnderDivider = 0;
                                imgCroppedMap.forEach { (_, value) ->
                                    if (value.second.visibility == View.VISIBLE && value.second.elevation == 100f && getBlockContainsElement(value.second)!!.size == 1) countUnderDivider++;
                                }
                                if (countUnderDivider <= 3) newDisplayPieces(9, 0);//
                                val isLineUnderBlock = (element.elevation == 100f);
                                if (lockedMatrixElements.contains(element) || isLineUnderBlock == true) {
                                    // chỉ xay ra khi keo 1 phan tu ra khoi matran sau phong, hoặc đi dưới underline lên
                                    for (blockElement in blockContainElement){
                                        if (isLineUnderBlock){
                                            blockElement.scaleX = originalImageView.scaleX;
                                            blockElement.scaleY = originalImageView.scaleY
                                            // dịch chuyen tam den dung vi tri truoc do
                                        }
                                        releaseLockedElement(blockElement)//
                                        element.elevation = 10f;
                                        // hàm này chi tra ve kich thuoc và vi tri zoom là 0f,0f, neu truoc do vi tri da dung
                                    }
                                    if (isLineUnderBlock && (gameSize.equals("8*8") || gameSize.equals("10*10"))){
                                        val changeXDistance = event.rawX - locationRootView[0] - ( element.x + (element.width*element.scaleX/2));
                                        val changeYDistance = event.rawY - locationRootView[1] - ( element.y + (element.height*element.scaleY/2));
                                        for (blockElement in blockContainElement){
                                            blockElement.x += changeXDistance
                                            blockElement.y += changeYDistance
                                        }
                                    }
                                }
                                val moveX = event.rawX - lastX;
                                val moveY = event.rawY - lastY;
                                for (blockElement in blockContainElement) {
                                    blockElement.x = moveX + blockElement.x
                                    blockElement.y = moveY + blockElement.y
                                    zoomViewAtPivot(blockElement, 0f, 0f, originalImageView.scaleX)
                                    blockElement.elevation = 10f;
                                }
                            }
                            lastX = event.rawX;
                            lastY = event.rawY;
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        allowMoveViewCropped[element] = false
                        // 1-> if element below underline
                        val blockContainsElement = getBlockContainsElement(element);
                        if (blockContainsElement.size > 1 && element.elevation == 100f) {
                            val moveY = checkDisplayPosition(element).second - screenSize.second / 2
                            for (blockElement in blockContainsElement) {
                                blockElement.scaleX = originalImageView.scaleX;
                                blockElement.scaleY = originalImageView.scaleY
                                blockElement.y -= moveY;
                                releaseLockedElement(blockElement);
                                blockElement.elevation = 10f;
                            }
                            return@setOnTouchListener true
                        }
                        if (blockContainsElement.size == 1 && element.elevation == 100f) {
                            releaseLockedElement(element)
                            return@setOnTouchListener true;
                        }
                        // 1-> if element is correct
                        val screenMainImgPivotX = originalImageView.x + originalImageView.pivotX;
                        val screenMainImgPivotY = originalImageView.y + originalImageView.pivotY;
                        // tính xem vị trí hiện tại của tâm điểm chính xác đó là bao nhiêu độ hiện tại trên màn ảnh
                        // so voi view goc
                        val exactlyViewXY =
                            getExaclyViewPosition(element); //// vi tri chinh xac so voi gốc 0f,0f ảnh gốc
                        var exactlyViewX = exactlyViewXY.first //
                        var exactlyViewY = exactlyViewXY.second
                        val snapX =
                            screenMainImgPivotX - (originalImageView.pivotX - exactlyViewX) * originalImageView.scaleX;
                        val snapY =
                            screenMainImgPivotY - (originalImageView.pivotY - exactlyViewY) * originalImageView.scaleY;
                        if (Math.abs(element.x - snapX) < element.width * element.scaleX * 0.3 && Math.abs(element.y - snapY) < element.height * element.scaleY * 0.3) {
                            // gọi hàm move block, di chuyển cả khối;
                            newBlockPlace(blockContainsElement, snapX - view.x, snapY - view.y, true);
                            elementBlocks.remove(blockContainsElement);
                            return@setOnTouchListener true;
                        } else {
                            CheckToMergeBlock(blockContainsElement)
                        }
                    }
                }
                true
            }
        }
    }

    fun updateLayout() {
        val layout = binding.viewZoomDividerLine.layoutParams
        layout.height = (screenSize.second / 4).toInt();
        binding.viewZoomDividerLine.layoutParams = layout;
        binding.viewZoomDividerLine.post{
            locationRootView = IntArray(2)
            binding.main.getLocationInWindow(locationRootView);
            val location = IntArray(2)
            binding.viewZoomDividerLine.getLocationOnScreen(location)
            this.zoomDividerLine = location[1];
            zoomViewAtPivot(originalImageView, 0f, 0f, screenToBitmapWidthRatio)//
            var originalViewLayout = originalImageView.layoutParams;
            originalViewLayout.width = bitmapOriginalImg!!.width;
            originalViewLayout.height = bitmapOriginalImg!!.height;
            val originalImageLayout = originalImageView.layoutParams as ViewGroup.MarginLayoutParams
            originalImageLayout.topMargin =
                ((zoomDividerLine - locationRootView[1] - (bitmapOriginalImg!!.height * screenToBitmapWidthRatio) - binding.llBarAction.height).toInt() / 2) + binding.llBarAction.height;
            originalImageView.layoutParams = originalImageLayout;
            originalImageView.post {
                val json = this.intent.getStringExtra("state")
                val gameState = json?.let {
                    Gson().fromJson(it, GameState::class.java)
                }
                if (gameState != null) {
              //      Log.d("state game", "" + gameState)
                    for (index in gameState!!.correctElement) {
                        val view = getImageViewByIndex(index)
                        correctlyPlacedElements.add(view);
                        val exaclyPosition = getExaclyViewPosition(view);
                        view.x =
                            originalImageView.x + (exaclyPosition.first * originalImageView.scaleX);
                        view.y =
                            originalImageView.y + (exaclyPosition.second * originalImageView.scaleY);
                        zoomViewAtPivot(view, 0f, 0f, originalImageView.scaleX);
                        // truong hop tren la dynamic
                        lockElementsAroundPivot(view, originalImageView.x, originalImageView.y, true);
                        view.elevation = 10f;
                        view.visibility = View.VISIBLE
                        elementBlocks.remove(getBlockContainsElement(view));
                        view.setOnTouchListener(null);
                    }
                    for (map in gameState!!.inCorrectElement) {
                        val view = getImageViewByIndex(map.key);
                        view.x = originalImageView.x + (map.value.first * originalImageView.scaleX);
                        view.y =
                            originalImageView.y + (map.value.second * originalImageView.scaleY);
                        view.elevation = 10f;
                        view.visibility = View.VISIBLE
                        view.bringToFront()
                        zoomViewAtPivot(view, 0f, 0f, originalImageView.scaleX);
                      //  Log.d("state view hien thi", "${view.x} -- ${view.y} --${view.visibility}");
                    }
                    for (block in gameState!!.blockList) {
                        val newBlock = HashSet<ImageView>();
                        for (index in block) {
                            val view = getImageViewByIndex(index);
                            newBlock.add(view);
                            elementBlocks.remove(getBlockContainsElement(view));
                        }
                        elementBlocks.add(newBlock);
                    }
                }
                newDisplayPieces(11, 0);
            }
        }
    }
    fun updateGameStates(){
        lifecycleScope.launch(Dispatchers.IO){
            val states = HandleWithFile().loadAppState(this@PuzzleGame);
            val isDark = states.gameBackground.equals("dark");
            lifecycleScope.launch(Dispatchers.Main){
                this@PuzzleGame.isVibrate = states.vibrate;
                binding.imbChangeBackground.imageTintMode = PorterDuff.Mode.SRC_IN
                setBackground(isDark);
            }
        }
    }
    fun setBackground(isDark : Boolean){
        val greyColor = ContextCompat.getColor(this, R.color.grey)
        val blackColor = ContextCompat.getColor(this, R.color.black)
        if (isDark){
            binding.main.setBackgroundColor(ContextCompat.getColor(this, R.color.mainBlackMode))
            binding.llBarAction.setBackgroundColor(blackColor)
            binding.viewZoomDividerLine.setBackgroundColor(ContextCompat.getColor(this, R.color.drakBlue))
            binding.imbChangeBackground.imageTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue_bold))
            originalImageView.alpha = 0.25f
        } else {
            binding.main.setBackgroundColor(ContextCompat.getColor(this, R.color.mainLightMode))
            binding.llBarAction.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
            binding.viewZoomDividerLine.setBackgroundColor(ContextCompat.getColor(this, R.color.lightGrey))
            binding.imbChangeBackground.imageTintList = ColorStateList.valueOf(greyColor)
            originalImageView.alpha = 0.3f
        }
    }

    fun updateMainImgDynamicElements(viewSet: Set<ImageView>) {
        val screenPivotX = originalImageView.x + originalImageView.pivotX;
        val screenPivotY = originalImageView.y + originalImageView.pivotY
        for (element in viewSet) {
            lockElementsAroundPivot(element, screenPivotX, screenPivotY, true);
        }
    }

    fun lockElementsAroundPivot(element: ImageView, screenPivotX: Float, screenPivotY: Float, addMatrix: Boolean) { // ham nay la de set lai vi tri view.x,y khi dua vao ma tran
        // tinh khoang cach hien tại trên màn ảnh so với điểm pivot
        val currentDistanceX = (element.x - screenPivotX);
        val currentDistanceY = (element.y - screenPivotY);
        // tính khoảng cách mới từ pivotX đến view.x
        val newDistanceX = currentDistanceX / element.scaleX;
        val newDistanceY = currentDistanceY / element.scaleY;
        element.x = screenPivotX + newDistanceX;
        element.y = screenPivotY + newDistanceY;
        zoomViewAtPivot(element, -newDistanceX, -newDistanceY, element.scaleX);
        if (addMatrix == true) lockedMatrixElements.add(element);
    }

    fun updateMusic() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.music_dialog)
        val llMusicGroup = dialog.findViewById<LinearLayout>(R.id.llMusicGroup);
        val llUncheckMusic = dialog.findViewById<LinearLayout>(R.id.llUncheckMusic)
        val llMusic1 = dialog.findViewById<LinearLayout>(R.id.llMusic1)
        val llMusic2 = dialog.findViewById<LinearLayout>(R.id.llMusic2)
        val llMusic3 = dialog.findViewById<LinearLayout>(R.id.llMusic3)
        val cbAddVibrate = dialog.findViewById<CheckBox>(R.id.cbAddVibrate)

        val btnCancelDialog = dialog.findViewById(R.id.btnCancelDialog) as AppCompatButton
        val btnUpdateMusic = dialog.findViewById(R.id.btnUpdateMusic) as AppCompatButton
        val colorBlueId = ContextCompat.getColor(this, R.color.blue);
        if (GameMusicService.currentSourceId == null) {
            llUncheckMusic.setBackgroundColor(colorBlueId)
        } else {
            if (GameMusicService.currentSourceId == R.raw.music) {
                llMusic1.setBackgroundColor(colorBlueId)
            }
            if (GameMusicService.currentSourceId == R.raw.music2) {
                llMusic2.setBackgroundColor(colorBlueId)
            }
            if (GameMusicService.currentSourceId == R.raw.music3) {
                llMusic3.setBackgroundColor(colorBlueId)
            }
        }
        if(isVibrate == true) cbAddVibrate.isChecked = true
        else cbAddVibrate.isChecked = false;
        var checkedMusicId = GameMusicService.currentSourceId;
        val remoreMusicBackground ={
            for (i in 0 until llMusicGroup.childCount) {
                val child = llMusicGroup.getChildAt(i)
                child.setBackgroundColor(ContextCompat.getColor(this,R.color.dialogLightBackground))
            }
        }
        llUncheckMusic.setOnClickListener {
            remoreMusicBackground();
            checkedMusicId = null;
            llUncheckMusic.setBackgroundColor(colorBlueId)
        }
        llMusic1.setOnClickListener{
            remoreMusicBackground()
            checkedMusicId = R.raw.music
            llMusic1.setBackgroundColor(colorBlueId)
        }
        llMusic2.setOnClickListener {
            remoreMusicBackground()
            checkedMusicId = R.raw.music2
            llMusic2.setBackgroundColor(colorBlueId)
        }
        llMusic3.setOnClickListener {
            remoreMusicBackground();
            checkedMusicId = R.raw.music3
            llMusic3.setBackgroundColor(colorBlueId)
        }

        btnUpdateMusic.setOnClickListener {
            if(cbAddVibrate.isChecked == true) isVibrate = true
            else isVibrate = false;
            if(checkedMusicId == null){
                GameMusicService.currentSourceId = null;
                stopService(Intent(this,GameMusicService ::class.java))
            }else{
                if(GameMusicService.currentSourceId != checkedMusicId){
                    GameMusicService.stopMusic(this)
                    GameMusicService.currentSourceId = checkedMusicId;
                    startService(Intent(this,GameMusicService ::class.java))
                }
            }
            dialog.dismiss()
        }
        btnCancelDialog.setOnClickListener {
            dialog.dismiss();
        }
        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    fun returnToStartPosition(finishGame: Boolean) {
        if (finishGame == true) Thread.sleep(300);
        releaseLockedElement(originalImageView); // dua anh goc zoom tai 0f,0f;
        val allMatrixElement = getAllMatrixView();
        if (allMatrixElement != null) {
            for (element in allMatrixElement) {
                if (lockedMatrixElements.contains(element)) releaseLockedElement(element)
                lockElementsAroundPivot(element, originalImageView.x, originalImageView.y, true);
            }
        }

        val animationReadyViews = mutableListOf<ImageView>().apply {
            add(originalImageView)
            if (allMatrixElement != null) addAll(allMatrixElement)
        }
        val locationRootView = IntArray(2);
        binding.main.getLocationInWindow(locationRootView);
        val distanceX =
            0 - originalImageView.x; // x,y khi mới bat dau game gio ta chuyen no ve lai toa do
        val distanceY =
            ((zoomDividerLine - locationRootView[1] - (bitmapOriginalImg!!.height * screenToBitmapWidthRatio) - binding.llBarAction.height).toInt() / 2) + binding.llBarAction.height - originalImageView.y;

        val animators = mutableListOf<Animator>()
        for (element in animationReadyViews) {
            val ElementAnimX = ObjectAnimator.ofFloat(element, "x", element.x + distanceX)
            val ElementAnimY = ObjectAnimator.ofFloat(element, "y", element.y + distanceY)
            animators.add(ElementAnimX)
            animators.add(ElementAnimY)
            val scaleX = ObjectAnimator.ofFloat(element, "scaleX", screenToBitmapWidthRatio)
            val scaleY = ObjectAnimator.ofFloat(element, "scaleY", screenToBitmapWidthRatio)
            animators.add(scaleX)
            animators.add(scaleY)
        }
        AnimatorSet().apply {
            playTogether(animators)
            duration = 400
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isInAnimation = false;
                    if (finishGame == true) {
                        originalImageView.setOnTouchListener(null)
                        val currentTime = System.currentTimeMillis()
                        val elapsed = currentTime - startTime
                        val seconds = (elapsed / 1000) % 60
                        val minutes = (elapsed / 1000 / 60) % 60
                        val hours = (elapsed / 1000 / 60 / 60)
                        var timeStr = "";
                        if (hours.toInt() == 0) {
                            timeStr = String.format("%02d:%02d", minutes, seconds)
                        } else {
                            timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                        }
                        binding.llBarAction.visibility = View.INVISIBLE;
                        binding.viewZoomDividerLine.visibility = View.INVISIBLE
                        binding.main.setBackgroundColor(ContextCompat.getColor(this@PuzzleGame, R.color.black))
                        binding.tvShowWinner.visibility = View.VISIBLE
                        val params =
                            binding.tvShowWinner.layoutParams as ConstraintLayout.LayoutParams;
                        params.topMargin =
                            originalImageView.y.toInt() + (originalImageView.height * originalImageView.scaleY).toInt() + 50.dp;
                        binding.tvShowWinner.layoutParams = params;
                        binding.tvGameTime.visibility = View.VISIBLE;
                        binding.tvGameTime.text = "you completed the game in " + timeStr
                        binding.imbFinishGame.visibility = View.VISIBLE;
                        binding.imbFinishGame.setOnClickListener { finish() }
                        lifecycleScope.launch {
                            while (true) {
                                delay(1000)
                                for (element in correctlyPlacedElements) {
                                    element.visibility = View.INVISIBLE;
                                }
                                val shuffledList = correctlyPlacedElements.shuffled()
                                for (element in shuffledList) {
                                    delay(70);
                                    element.visibility = View.VISIBLE;
                                }
                            }
                        }
                    }
                }
            })
            isInAnimation = true;
            start()
        }


    }

    fun showWinnerPlayer() {
        returnToStartPosition(true)
        saveGameFinish()
    }
    fun saveGameFinish(){
        val elapsed = System.currentTimeMillis() - startTime
        val seconds = (elapsed / 1000) % 60
        val minutes = (elapsed / 1000 / 60) % 60
        val hours = (elapsed / 1000 / 60 / 60)
        val timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        var pieceAmount = 16;
        if(gameSize.equals("6*6")) pieceAmount = 36;
        if(gameSize.equals("8*8")) pieceAmount = 64;
        if(gameSize.equals("10*10")) pieceAmount = 100;
        lifecycleScope.launch(Dispatchers.IO){
            HandleWithFile().saveGameFinished(timeStr,pieceAmount,intent.getStringExtra("localImagePath")!!,this@PuzzleGame);
        }
    }

    fun updateNewViewPosition(view: ImageView) {
        val posiDisplay = checkDisplayPosition(view);
        view.x = proposedPivotX!! - ((proposedPivotX!! - posiDisplay.first) / view.scaleX);
        view.y = proposedPivotY!! - ((proposedPivotY!! - posiDisplay.second) / view.scaleY);
        // tính xem điểm chạm đó tương đương với x,y bao nhiêu trên view hiện tại
        //(snapX - relutivePositionX)/originalImageView.scaleX // khoang cach tinh tu view.x
        view.pivotX = proposedPivotX!! - view.x;
        view.pivotY = proposedPivotY!! - view.y;
    }

    fun newDisplayPieces(amount: Int, flag: Int) {
        if (flag == 1) {
            // giấu tat ca cac phan tu hien dang hien thi và hien thi moi. vi dụ khi nguoi dung nhan vao
            // bieu tuong reset các piece
            // con truong hop chi la user lay gan het phan tu hoac lan dau render
            // thi khong can goi doan ma nây, flag = 0
            imgCroppedMap.forEach { (_, value) ->
                if (value.second.visibility == View.VISIBLE && value.second.elevation == 100f && getBlockContainsElement(value.second)!!.size == 1) {
                    // check phong luc user dang keo phan tu xuong duoi underline và người nhấn reset;
                    value.second.visibility = View.INVISIBLE;
                }
            }
        }
        imgCroppedMap.entries.filter { (_, value) ->
            value.second.visibility == View.INVISIBLE
        }.shuffled().take(amount).forEach { (_, value) ->
            randomDisplayPieces(value.second)
        }
    }

    fun hideIncorrectElement() {
        val allMatrixView = getAllMatrixView();
        if (allMatrixView != null) {
            allMatrixView.forEach { element ->
                if (correctlyPlacedElements.contains(element)) return@forEach;
                if (getBlockContainsElement(element)!!.size < 2) {
                    element.visibility = View.INVISIBLE;
                    element.elevation = 0f;
                    if (lockedMatrixElements.contains(element)) {
                        lockedMatrixElements.remove(element);
                    }
                }
            }
        }
    }


    fun randomDisplayPieces(view: ImageView) {
        zoomViewAtPivot(view, 0f, 0f, getCroppedViewScaleUnderLine());
        view.x =
            ((screenSize.first / 8).toInt()..(screenSize.first * 2 / 3).toInt()).random().toFloat();
        view.y =
            (binding.viewZoomDividerLine.y.toInt() + (binding.viewZoomDividerLine.height / 5f).toInt()..binding.viewZoomDividerLine.y.toInt() + (binding.viewZoomDividerLine.height / 2).toInt()).random()
                .toFloat();
        view.elevation = 100f;
        view.visibility = View.VISIBLE;
    }

    fun updateUnderlinePosition(element: ImageView, movX: Float, movY: Float, rawX: Float, rawY: Float) {
        val block = getBlockContainsElement(element);
        val scale = getCroppedViewScaleUnderLine();
        val inMatrix = (element.elevation == 10f);
        for(blockElement in block){
            if (inMatrix){ // vừa đi từ tren underline xuống
                lockElementsAroundPivot(blockElement, rawX, rawY, false) // van giu scale = imgMain.scale; /
                blockElement.scaleX = scale; //
                blockElement.scaleY = scale;
                blockElement.elevation = 100f;
            };
            blockElement.x = movX + blockElement.x
            blockElement.y = movY + blockElement.y
            blockElement.bringToFront()
            lockedMatrixElements.remove(blockElement);
        }
        if(inMatrix){
            val elementXY = checkDisplayPosition(element);
            val changeX = rawX - locationRootView[0] - (elementXY.first + (element.width * element.scaleX/2))
            val changeY = rawY - locationRootView[1] - (elementXY.second + (element.height * element.scaleY/2))
            for(blockElement in block){
                blockElement.x += changeX;
                blockElement.y += changeY;
            }
        }
    }

    fun releaseLockedElement(element: ImageView) {
        if (lockedMatrixElements.contains(element)) lockedMatrixElements.remove(element);
        val posiXYDisplay = checkDisplayPosition(element)
        element.x = posiXYDisplay.first
        element.y = posiXYDisplay.second
        element.pivotY = 0f;
        element.pivotX = 0f;
    }

    fun CheckToMergeBlock(block: HashSet<ImageView>) {
        // tra ve toa do x,y ma moi phan tu phai di chuyen cho su hop nhat khoi
        for (element in block) {
            // lay ra key cua no
            val elementsToCheck = mutableListOf<Int>();
            imgCroppedMap.forEach { (key, value) ->
                if (value.second == element) {
                    when (gameSize) {
                        "4*4" -> {
                            if (key % 4 != 1) elementsToCheck.add(key - 1);
                            if (key % 4 != 0) elementsToCheck.add(key + 1);
                            if (key - 4 > 0) elementsToCheck.add(key - 4);
                            if (key + 4 <= 16) elementsToCheck.add(key + 4);
                        }
                        "6*6" -> {
                            if (key % 6 != 1) elementsToCheck.add(key - 1);
                            if (key % 6 != 0) elementsToCheck.add(key + 1);
                            if (key - 6 > 0) elementsToCheck.add(key - 6);
                            if (key + 6 <= 36) elementsToCheck.add(key + 6);
                        }

                        "8*8" -> {
                            if (key % 8 != 1) elementsToCheck.add(key - 1);
                            if (key % 8 != 0) elementsToCheck.add(key + 1);
                            if (key - 8 > 0) elementsToCheck.add(key - 8);
                            if (key + 8 <= 64) elementsToCheck.add(key + 8);
                        }
                        "10*10" -> {
                            if (key % 10 != 1) elementsToCheck.add(key - 1);
                            if (key % 10 != 0) elementsToCheck.add(key + 1);
                            if (key - 10 > 0) elementsToCheck.add(key - 10);
                            if (key + 10 <= 100) elementsToCheck.add(key + 10);
                        }
                    }
                    val orginViewXY =
                        checkDisplayPosition(element); // tọa độ trên màn ảnh , tính cả zoom( người dùng thấy)
                    val exactOriginViewXY =
                        getExaclyViewPosition(element); // vi tri chinh xac, ko zoom với gốc 0f,0f MainImg
                    Log.d("check 4 phan tu", "" + elementsToCheck)
                    for (index in elementsToCheck) {
                        // tính xem tam cua chung tren man ảnh, tinh và so sanh khoang cach tam
                        val targetView = getImageViewByIndex(index);
                        if (targetView.elevation != 10f || targetView.visibility == View.INVISIBLE) continue; // nằm ngoài ma trận, các phan tu tron ma tran luon hien thí và do cao 10f
                        if (correctlyPlacedElements.contains(element)) continue;
                        if (block.contains(targetView)) continue;

                        val targetViewXy = checkDisplayPosition(targetView);
                        val exactTargetViewXY =
                            getExaclyViewPosition(targetView); // khoang cach khi dat chinh xac , khong zoom so với goc 0f,0f MainImg

                        val distanceX =
                            exactTargetViewXY.first - exactOriginViewXY.first;  // khoang cach X chinh xac khi dat dung , khong zoom giữa 2 view
                        val distanceY =
                            exactTargetViewXY.second - exactOriginViewXY.second; // khoang cach Y chinh xac khi dat dung khong zoom , giữa 2 view
                        val currentExactlyX = targetViewXy.first - (distanceX * targetView.scaleX)
                        val currentExactlyY = targetViewXy.second - (distanceY * targetView.scaleY)

                        // viewCenterpositionX la vi that tren man anh , px ma nguoi dung that
                        if (Math.abs(orginViewXY.first - currentExactlyX) < element.width * element.scaleX * 0.3 && Math.abs(orginViewXY.second - currentExactlyY) < element.height * element.scaleY * 0.3) {

                            // hợp nhất khối
                            // biến tât ca phan tu kia thanh dong va hop nhat
                            newBlockPlace(block, currentExactlyX - orginViewXY.first, currentExactlyY - orginViewXY.second, false);
                            // hop nhat phan tu
                            for (mergedBlock in elementBlocks) {
                                if (mergedBlock.contains(targetView)) {
                                    mergedBlock.forEach { element ->
                                        releaseLockedElement(element)
                                        element.bringToFront();
                                    }
                                    val newBlock = HashSet<ImageView>();
                                    newBlock.addAll(block);
                                    newBlock.addAll(mergedBlock);
                                    elementBlocks.remove(mergedBlock);
                                    elementBlocks.remove(block);
                                    elementBlocks.add(newBlock);
                                    newBlock.forEach { element ->
                                        allowMoveViewCropped[element] = false
                                    }
                                    return;
                                }
                            }
                        }
                    }
                    return@forEach
                }
            }
        }
    }

    fun checkDisplayPosition(view: ImageView): Pair<Float, Float> {
        val posiX = (view.x + view.pivotX) - (view.pivotX * view.scaleX)
        val posiY = (view.y + view.pivotY) - (view.pivotY * view.scaleY);
        return Pair(posiX, posiY);
    }

    fun newBlockPlace(block: Set<ImageView>, distanceX: Float, distanceY: Float, finishBlock: Boolean) {
        val animators = mutableListOf<Animator>()
        for (blockElement in block) {
            val blockElementAnimX =
                ObjectAnimator.ofFloat(blockElement, "x", blockElement.x + distanceX)
            val blockElementAnimY =
                ObjectAnimator.ofFloat(blockElement, "y", blockElement.y + distanceY)
            animators.add(blockElementAnimX)
            animators.add(blockElementAnimY)
        }
        isInAnimation = true
        if(isVibrate) vibrateOnCorrectMove()
        AnimatorSet().apply {
            playTogether(animators)
            duration = 100
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isInAnimation = false;
                    if (finishBlock == true) {
                        super.onAnimationEnd(animation)
                        for (element in block) {
                            element.setOnTouchListener(null); // //
                            lockElementsAroundPivot(element, originalImageView.x + originalImageView.pivotX, originalImageView.y + originalImageView.pivotY, true);
                            correctlyPlacedElements.add(element);
                            if (correctlyPlacedElements.size == imgCroppedMap.size) showWinnerPlayer();
                        }
                    }
                    Log.d("so block - current ", ":" + elementBlocks.size);
                }
            })
            showEffect(block);
            start()
        }
    }

    fun getImageViewByIndex(key: Int): ImageView {
        for (map in imgCroppedMap) {
            if (map.key == key) return map.value.second;
        }
        throw IllegalArgumentException("Error while getIndex")
    }

    fun getIndexByImageView(img: ImageView): Int {
        for (map in imgCroppedMap) {
            if (map.value.second == img) return map.key;
        }
        throw IllegalArgumentException("Error while getIndex")
    }

    fun vibrateOnCorrectMove() {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Android 12 trở lên dùng VibratorManager
            val vibratorManager = getSystemService(VibratorManager::class.java)
            vibratorManager.defaultVibrator
        } else {
            // Dưới Android 12 vẫn dùng cách cũ
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }


    fun zoomViewAtPivot(view: ImageView, pivotX: Float, pivotY: Float, scale: Float) {
        view.pivotX = pivotX;
        view.pivotY = pivotY;
        view.scaleX = scale;
        view.scaleY = scale
    }

    fun getBlockContainsElement(view: ImageView): HashSet<ImageView> {
        for (block in elementBlocks) {
            if (block.contains(view)) {
                return block
            };
        }
        throw IllegalArgumentException("Error while getIndex")
    }


    fun getCroppedViewScaleUnderLine(): Float {
        var scale = 0f;
        when (gameSize) {
            "4*4" -> {
                scale = screenSize.first.toFloat() / 1280 * 0.85f;
            }

            "6*6" -> {
                scale = screenSize.first.toFloat() / 1440 * 1.15f;
            }

            "8*8" -> {
                scale = screenSize.first.toFloat() / 1600 * 1.4f;
            }
            "10*10" -> {
                scale = screenSize.first.toFloat() / 1600 * 1.8f;
            }
        }
        return scale;
    }

    fun changeBackGround() {
        val tintColor = binding.imbChangeBackground.imageTintList?.defaultColor
        val greyColor = ContextCompat.getColor(this, R.color.grey)
        val isDark = tintColor != greyColor; // hiện tại đang màu tối
        setBackground(!isDark);
    }

    fun getExaclyViewPosition(view: ImageView): Pair<Int, Int> {
        // complete -> func
        val key = getIndexByImageView(view);
        var exactlyViewX = 0;
        var exactlyViewY = 0;
        when(gameSize){
            "4*4"->{
                exactlyViewX = 160 + ((key - 1) % 4) * 320 - imgCroppedMap[key]!!.first.offsetXFromCenter;
                exactlyViewY = 120 + ((key - 1) / 4 * 240) - imgCroppedMap[key]!!.first.offsetYFromCenter;
            }
            "6*6"->{
                exactlyViewX = 120 + ((key - 1) % 6) * 240 - imgCroppedMap[key]!!.first.offsetXFromCenter;
                exactlyViewY = 90 + ((key - 1) / 6 * 180) - imgCroppedMap[key]!!.first.offsetYFromCenter;
            }
            "8*8"->{
                exactlyViewX = 100 + ((key - 1) % 8) * 200 - imgCroppedMap[key]!!.first.offsetXFromCenter;
                exactlyViewY = 75 + ((key - 1) / 8 * 150) - imgCroppedMap[key]!!.first.offsetYFromCenter;
            }
            "10*10"->{
                exactlyViewX = 100 + ((key - 1) % 10) * 200 - imgCroppedMap[key]!!.first.offsetXFromCenter;
                exactlyViewY = 75 + ((key - 1) / 10 * 150) - imgCroppedMap[key]!!.first.offsetYFromCenter;
            }
        }
        return Pair(exactlyViewX, exactlyViewY);
    }

    fun getAllMatrixView(): Set<ImageView>? {
        val result = mutableSetOf<ImageView>();
        imgCroppedMap.forEach { (_, value) ->
            val element = value.second;
            if (element.elevation == 10f && element.visibility == View.VISIBLE) result.add(element);
        }
        return if (result.size > 0) result else null;
    }

    fun getAllMatrixDynamicView(): Set<ImageView>? {
        val allMatrixView = getAllMatrixView();
        if (allMatrixView == null) return null;
        val result = mutableSetOf<ImageView>();
        allMatrixView.forEach { element ->
            if (lockedMatrixElements.contains(element) == false) result.add(element)
        }
        return result;
    }

    override fun onBackPressed() {
        finish();
        super.onBackPressed()
    }

    override fun finish() {
        saveState();
        super.finish()
    }

    override fun onStop() {
        saveState();
        super.onStop()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (isInAnimation == true) return true;
        return super.dispatchTouchEvent(ev)
    }

    override fun onDestroy(){
        stopService(Intent(this,GameMusicService ::class.java))
        bitmapOriginalImg = null; //
        bitmapMap = null;
        super.onDestroy()
    }

    fun saveState() {
        val filePath = intent.getStringExtra("localImagePath")!!;
        val source = intent.getStringExtra("source")!!;
        val correctElements = mutableListOf<Int>();
        val inCorrectMatrixElements = HashMap<Int, Pair<Float, Float>>();
        val blockInMatrixList = mutableListOf<HashSet<Int>>();
        val allMatrixElement = getAllMatrixView();
        if (allMatrixElement != null && correctlyPlacedElements.size != imgCroppedMap.size) {
            val originalImgPositionXY = checkDisplayPosition(originalImageView);
            val visitedElementsInBlock = HashSet<ImageView>();
            for (element in allMatrixElement) {
                val index = getIndexByImageView(element);
                if (correctlyPlacedElements.contains(element)) {
                    correctElements.add(index);
                } else {
                    val positionXY = checkDisplayPosition(element);
                    val distanceX =
                        (positionXY.first - originalImgPositionXY.first) / originalImageView.scaleX;
                    val distanceY =
                        (positionXY.second - originalImgPositionXY.second) / originalImageView.scaleY;
                    inCorrectMatrixElements.put(index, Pair(distanceX, distanceY));
                    val block = getBlockContainsElement(element)!!;
                    val stateBlock = HashSet<Int>();
                    for (elementInBlock in block) {
                        if (visitedElementsInBlock.contains(elementInBlock) == true) break;
                        stateBlock.add(getIndexByImageView(elementInBlock));
                        visitedElementsInBlock.add(elementInBlock);
                    }
                    if (stateBlock.size > 0) blockInMatrixList.add(stateBlock);
                }
            }
            val gameState =
                GameState(filePath, source, gameSize, correctElements, inCorrectMatrixElements, blockInMatrixList);
            HandleWithFile().saveGameState(this, gameState);
        } else {
            HandleWithFile().deleteGameState(this, filePath);
        }
    }


    fun showEffect(block: Set<ImageView>) {
        // tính tọa độ x, y trung bình rồi tạo hieu ung
        var sumX = 0f;
        var sumY = 0f;
        for (element in block) {
            val displayElementXY = checkDisplayPosition(element);
            sumX += displayElementXY.first;
            sumY += displayElementXY.second;
        }
        val firstElement = block.first();
        val centerX = sumX / block.size + (firstElement.width * firstElement.scaleX / 2);
        val centerY = sumY / block.size + (firstElement.height * firstElement.scaleY);
        for (i in 1..3) {
            val imageResId = this.resources.getIdentifier("effect$i", "drawable", this.packageName)
            val srcDrawable = ContextCompat.getDrawable(applicationContext, imageResId)
            val bitmap = (srcDrawable as BitmapDrawable).bitmap
            val imageView = ImageView(this).apply {
                layoutParams =
                    ConstraintLayout.LayoutParams(bitmap.width.dp / 11, bitmap.height.dp / 11)
                        .apply {
                            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                            topMargin = centerY.toInt();
                            marginStart =
                                centerX.toInt() + ((i - 2) * firstElement.width * firstElement.scaleX / 2).toInt()
                        }
            }
            imageView.setImageBitmap(bitmap)
            imageView.elevation = 100f;
            val color = ContextCompat.getColor(this, R.color.blue)
            imageView.imageTintList = ColorStateList.valueOf(color)
            imageView.imageTintMode = PorterDuff.Mode.SRC_IN
            imageView.scaleType = ImageView.ScaleType.FIT_CENTER;
            binding.main.addView(imageView)
            imageView.translationY = 0f
            imageView.alpha = 1f
            // Bay lên 0.1s
            imageView.animate().translationYBy(-80.dp.toFloat()).setDuration(300).rotationBy(360f)
                .withEndAction {
                    // Rơi xuống 0.1s
                    imageView.animate().translationYBy(80.dp.toFloat()).rotationBy(360f).alpha(0f)
                        .setDuration(300).withEndAction {
                            binding.main.removeView(imageView);
                        }.start()
                }.start()
        }
    }
}
