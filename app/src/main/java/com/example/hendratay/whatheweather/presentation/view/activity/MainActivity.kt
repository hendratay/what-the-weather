package com.example.hendratay.whatheweather.presentation.view.activity

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import com.example.hendratay.whatheweather.R
import com.example.hendratay.whatheweather.presentation.view.fragment.TodayFragment
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_main.*

const val TAG = "MainActivity"
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AndroidInjection.inject(this)

        setupToolbar()

    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun loadFragment(fragment: Fragment) {
        if(fragment != null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit()
        }
    }

}
