package com.example.gamehub

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.gamehub.fragment.CommunityFragment
import com.example.gamehub.fragment.FriendsFragment
import com.example.gamehub.fragment.GamesFragment
import com.example.gamehub.fragment.ProfileFragment
import com.example.gamehub.fragment.SearchFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.stripe.android.PaymentConfiguration


class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PaymentConfiguration.init(applicationContext, BuildConfig.STRIPE_PUBLISHABLE_KEY)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        if (savedInstanceState == null) {
            switchFragment(GamesFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_games -> {
                    switchFragment(GamesFragment())
                    true
                }
                R.id.nav_friends -> {
                    switchFragment(FriendsFragment())
                    true
                }
                R.id.nav_community -> {
                    switchFragment(CommunityFragment())
                    true
                }
                R.id.nav_profile -> {
                    switchFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun openSearch() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, SearchFragment())
            .addToBackStack(null)
            .commit()
    }
}