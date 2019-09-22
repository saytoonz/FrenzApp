package com.nsromapa.frenzapp.saytalk.activities

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.nsromapa.frenzapp.R
import com.nsromapa.frenzapp.saytalk.fragments.FragmentOTP
import es.dmoral.toasty.Toasty
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump
import kotlinx.android.synthetic.main.input_phone.*
import org.jetbrains.anko.alert

class MobileLoginActivity : AppCompatActivity() {


    private var fragmentOTP: FragmentOTP? = null

    object KEY {
        const val PHONE = "phone"
        const val COUNTRY = "country"
        const val COUNTRY_CODE = "countryCode"
        const val COUNTRY_LOCALE_CODE = "locale"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewPump.init(ViewPump.builder()
                .addInterceptor(CalligraphyInterceptor(
                        CalligraphyConfig.Builder()
                                .setDefaultFontPath("fonts/bold.ttf")
                                .setFontAttrId(R.attr.fontPath)
                                .build()))
                .build())

        setContentView(R.layout.input_phone)

        country_picker.registerCarrierNumberEditText(mobile_number)
        askPermission()
        generate_otp.setOnClickListener {


            if(!country_picker.isValidFullNumber) {
                mobile_number.error = "Input valid number"
                return@setOnClickListener
            }


            fragmentOTP = FragmentOTP()
            val bundle = Bundle()
            val countryCode = country_picker.selectedCountryCode
            val countryName = country_picker.selectedCountryName
            val countryLocale = country_picker.selectedCountryNameCode

            bundle.putString(KEY.PHONE, country_picker.fullNumberWithPlus)
            bundle.putString(KEY.COUNTRY, countryName)
            bundle.putString(KEY.COUNTRY_LOCALE_CODE, countryLocale)
            bundle.putString(KEY.COUNTRY_CODE, countryCode)




            fragmentOTP?.arguments = bundle

            Log.d("MobileLoginActivity", "onCreate: bundle = $bundle")


            alert {
                message = "Is ${country_picker.fullNumberWithPlus} your phone number?"
                positiveButton("Yes"){
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, fragmentOTP!!)
                        .commit()
            }
            negativeButton("No"){

            }

            }.show()


        }
    }

    private fun askPermission() {

        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS)
                .withListener(object : MultiplePermissionsListener {

                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toasty.info(this@MobileLoginActivity, "You have denied some permissions permanently, if the app force close try granting permission from settings.", Toasty.LENGTH_LONG, true).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(permissions: List<PermissionRequest>, token: PermissionToken) {
                        token.continuePermissionRequest()
                    }
                })
                .check()

    }

    override fun onBackPressed() {

        if(!supportFragmentManager.fragments.contains(fragmentOTP))
            super.onBackPressed()
    }

}
