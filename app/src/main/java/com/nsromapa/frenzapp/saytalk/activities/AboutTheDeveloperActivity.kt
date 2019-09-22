package com.nsromapa.frenzapp.saytalk.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import com.nsromapa.frenzapp.BuildConfig
import com.nsromapa.frenzapp.R
import com.nsromapa.frenzapp.saytalk.models.Models
import com.nsromapa.frenzapp.saytalk.utils.FirebaseUtils
import com.nsromapa.frenzapp.saytalk.utils.utils
import com.yarolegovich.lovelydialog.LovelyTextInputDialog
import mehdi.sakout.aboutpage.AboutPage
import mehdi.sakout.aboutpage.Element
import org.jetbrains.anko.toast

class AboutTheDeveloperActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        val aboutPage = AboutPage(this)
            .isRTL(false)
            .setImage(R.mipmap.ic_launcher)
            .setDescription("FrenzApp\n\nAn Open Source Chat Project for Android")
            .addItem(Element("Version "+ BuildConfig.VERSION_NAME,
                R.mipmap.ic_launcher
            ))
            .addInstagram("nanayawdedrick")

            .addTwitter("Saytoonz")

            .addGroup("Connect with us")

            .addEmail("saytoonz05@gmail.com")

            .addWebsite("https://frenzapp.nsromapa.com/", "Visit my website")
//            .addFacebook("shanu.siddiqui.568","Connect on Facebook")
//            .addPlayStore(BuildConfig.APPLICATION_ID)

            .addGitHub("saytoonz")

            .addItem(Element("Check for update", R.mipmap.ic_launcher).setOnClickListener
                    { FirebaseUtils.checkForUpdate(this@AboutTheDeveloperActivity, true) })

            .addItem(Element("Feedback to the developer",android.R.drawable.ic_dialog_info).setOnClickListener
                    { showFeedbackDialog(FirebaseUtils.getUid()) })

        Log.d("about", "onCreate: uid = ${FirebaseUtils.getUid()}")

        if(FirebaseUtils.getUid() == "GEgg2dmPTmTFDVH93gcVgBOrKC33"){

            // if it falls under one of my uids or debug APK
            Log.d("AboutTheDeveloper", "onCreate: show feedbacks")

            aboutPage.addItem(Element("Feedbacks",android.R.drawable.ic_dialog_info).setOnClickListener
            { startActivity(Intent(this@AboutTheDeveloperActivity, FeedbackActivity::class.java)) })
        }

        val aboutView = aboutPage.create()

        setContentView(aboutView)


        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }


    private fun showFeedbackDialog(uid:String){


        LovelyTextInputDialog(this)
            .setTopColorRes(R.color.colorAccent)
            .setTopTitleColor(Color.WHITE)
            .setTopTitle("Feedback")
            .setTitle("Type your feedback here...")
            .setInputFilter("Too short") {
                return@setInputFilter it.isNotBlank() && it.length > 5
            }
            .setConfirmButton("Submit") {

                FirebaseUtils.ref.feedback()
                    .push()
                    .setValue(Models.Feedback(uid, feedback = it))
                    .addOnSuccessListener { toast("Feedback submitted. Thanks!") }


            }
            .setNegativeButton("Cancel"){}
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        //just finishing this activity
        finish()
        return super.onOptionsItemSelected(item)
    }


}