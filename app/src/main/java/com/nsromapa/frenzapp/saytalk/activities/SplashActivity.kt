package com.nsromapa.frenzapp.saytalk.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.nsromapa.frenzapp.R
import com.nsromapa.frenzapp.newfy.ui.activities.MainActivity
import com.nsromapa.frenzapp.saytalk.utils.FirebaseUtils

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_)


        if (FirebaseUtils.isLoggedIn()) {
            MainActivity.startActivity(this, true)
            finish()
        }

    }


    fun onGetStartedClick(v: View) {
        startActivity(Intent(this, MobileLoginActivity::class.java))
        finish()
    }


}
