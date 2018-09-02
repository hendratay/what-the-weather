package com.example.hendratay.whatheweather.presentation.view.activity

import android.accounts.AccountManager
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import com.example.hendratay.whatheweather.BuildConfig
import com.example.hendratay.whatheweather.R
import kotlinx.android.synthetic.main.activity_sendfeedback.*
import com.example.hendratay.whatheweather.presentation.view.utils.Gmail
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.common.AccountPicker
import kotlin.concurrent.thread

class SendFeedbackActivity: AppCompatActivity() {

    companion object {
        const val REQUEST_CODE_EMAIL = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sendfeedback)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setColorFilter(resources.getColor(android.R.color.black), PorterDuff.Mode.SRC_ATOP);
        setupSpinnerFrom()
        setupSpinnerAbout()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_sendfeedback, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?) = when (item?.itemId) {
        android.R.id.home -> {
            onBackPressed()
            true
        }
        R.id.send -> {
            sendFeedback()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == REQUEST_CODE_EMAIL && resultCode == Activity.RESULT_OK) {
            val account = data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOf(account))
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner_from.adapter = adapter
        }
    }

    private fun setupSpinnerFrom() {
        try {
            val intent = AccountPicker.newChooseAccountIntent(null, null,
                    arrayOf(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE), false, null, null, null, null)
            startActivityForResult(intent, REQUEST_CODE_EMAIL)
        } catch (e: ActivityNotFoundException) {
        }
    }

    private fun setupSpinnerAbout() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item,
                resources.getStringArray(R.array.feedback_about))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_about.adapter = adapter
    }

    private fun sendFeedback() {
        thread {
            try {
                try {
                    val sender = Gmail(BuildConfig.APP_GMAIL, BuildConfig.APP_GMAIL_PASS)
                    sender.sendMail(spinner_about.selectedItem.toString(),
                            input_feedback.text.toString(),
                            spinner_from.selectedItem.toString(),
                            BuildConfig.CUSTOMER_SERVICE_GMAIL)
                } catch (e: Exception) {
                }
            } catch (e: Exception) {
            }
        }
    }

}