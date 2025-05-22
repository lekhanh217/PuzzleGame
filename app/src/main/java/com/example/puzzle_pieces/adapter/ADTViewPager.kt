package com.example.puzzle_pieces.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.puzzle_pieces.ui.CollectionFragment
import com.example.puzzle_pieces.ui.HomeFragment

class ADTViewPager(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> CollectionFragment()
            else -> Fragment()
        }
    }
}
