package com.example.puzzle_pieces.ui
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.puzzle_pieces.R
import com.example.puzzle_pieces.adapter.ADTViewPager
import com.example.puzzle_pieces.databinding.ActivityHomeBinding
import com.example.puzzle_pieces.service.AppMusicService
import com.example.puzzle_pieces.service.GameMusicService
import com.example.puzzle_pieces.ui.Setting
import com.example.puzzle_pieces.utils.HandleWithFile
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class App : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var tabLayout : TabLayout;
    private lateinit var viewPager2: ViewPager2;


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater);
        setContentView(binding.root)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        binding.imbGoMyWork.setOnClickListener {
            startActivity(Intent(this, MyWork ::class.java));
        }
        binding.imbSetting.setOnClickListener {
            startActivity(Intent(this, Setting::class.java));
        }

        tabLayout = binding.tabLayout
        viewPager2 = binding.viewPager;
        viewPager2.adapter = ADTViewPager(this)
        TabLayoutMediator(tabLayout, viewPager2) { tab, position -> true}.attach()

        for (i in 0..1) {
            val tab = tabLayout.getTabAt(i)
            val layoutResId = when (i) {
                0 -> R.layout.tab1
                1 -> R.layout.tab2
                else -> R.layout.tab1
            }
            tab?.setCustomView(layoutResId)
            val customView = tab?.customView
            val imageView = customView?.findViewById<ImageView>(R.id.image_view_icontab)
            val textView = customView?.findViewById<TextView>(R.id.content_tab)
            if (i == 0) {
                imageView?.setColorFilter(ContextCompat.getColor(this, R.color.blue))
                textView?.setTextColor(ContextCompat.getColor(this, R.color.blue))
            }
        }
        checkTabSelect(tabLayout);

    }
    fun checkTabSelect(tabLayout: TabLayout) {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 0){
                    binding.toolbar.visibility = View.VISIBLE
                }
                val imageView = tab?.customView?.findViewById<ImageView>(R.id.image_view_icontab)
                imageView?.setColorFilter(ContextCompat.getColor(this@App, R.color.blue))
                val textView = tab?.customView?.findViewById<TextView>(R.id.content_tab)
                textView?.setTextColor(ContextCompat.getColor(this@App, R.color.blue))
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                if (tab?.position == 0){
                    binding.toolbar.visibility = View.GONE
                }
                val textView = tab?.customView?.findViewById<TextView>(R.id.content_tab)
                textView?.setTextColor(ContextCompat.getColor(this@App, R.color.grey_tab))
                val imageView = tab?.customView?.findViewById<ImageView>(R.id.image_view_icontab)
                imageView?.setColorFilter(ContextCompat.getColor(this@App, R.color.grey_tab))

            }
            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
    }

    override fun onDestroy() {
        stopService(Intent(this,GameMusicService ::class.java))
        super.onDestroy()
    }
}