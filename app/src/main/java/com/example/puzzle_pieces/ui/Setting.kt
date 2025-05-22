package com.example.puzzle_pieces.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.puzzle_pieces.R
import com.example.puzzle_pieces.databinding.ActivitySettingBinding
import com.example.puzzle_pieces.model.AppState
import com.example.puzzle_pieces.service.AppMusicService
import com.example.puzzle_pieces.utils.HandleWithFile
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Setting : AppCompatActivity(){
    private lateinit var binding : ActivitySettingBinding;
    private lateinit var initState : AppState;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingBinding.inflate(layoutInflater);
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.imbBack.setOnClickListener { finish() }
        val switchMaterialList = listOf<SwitchMaterial>(binding.smSound,binding.smDarkGameMode,binding.smVibrate)
        for(switchMaterial in switchMaterialList){
            switchMaterial.setOnCheckedChangeListener{ _, isChecked ->
                if (isChecked) {
                    switchMaterial.thumbTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.blue))
                } else {
                    switchMaterial.thumbTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.grey_bold))
                }
            }
        }
        lifecycleScope.launch(Dispatchers.IO){
            initState = HandleWithFile().loadAppState(this@Setting)
            lifecycleScope.launch(Dispatchers.Main){
               binding.smSound.isChecked = initState.sound
               binding.smDarkGameMode.isChecked = (initState.gameBackground.equals("dark"))
               binding.smVibrate.isChecked = initState.vibrate
            }
        }
        binding.btnSaveProgress.setOnClickListener { 
            val sound = binding.smSound.isChecked
            val vibrate = binding.smVibrate.isChecked
            val gameBackground = if(binding.smDarkGameMode.isChecked) "dark" else "light"
            if(sound == true && initState.sound == false){
               startService(Intent(this, AppMusicService::class.java));
            }
            if(sound == false && initState.sound == true){
                AppMusicService.stopMusic(this)
            }
            lifecycleScope.launch(Dispatchers.IO){
                HandleWithFile().saveAppState(this@Setting, AppState(sound,gameBackground,vibrate))
            }
            finish();
        }
    }
}