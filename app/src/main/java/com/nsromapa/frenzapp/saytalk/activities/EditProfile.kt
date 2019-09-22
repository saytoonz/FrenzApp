package com.nsromapa.frenzapp.saytalk.activities

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import com.nsromapa.frenzapp.saytalk.utils.FirebaseUtils
import com.nsromapa.frenzapp.saytalk.utils.utils
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.nsromapa.frenzapp.R
import com.nsromapa.frenzapp.newfy.ui.activities.MainActivity
import com.nsromapa.frenzapp.newfy.utils.AnimationUtil
import com.nsromapa.frenzapp.newfy.utils.Config
import com.nsromapa.frenzapp.newfy.utils.database.UserHelper
import com.nsromapa.frenzapp.saytalk.models.Models
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_edit_profile.*
import kotlinx.android.synthetic.main.layout_profile_image_picker.*
import me.shaohui.advancedluban.Luban
import me.shaohui.advancedluban.OnCompressListener
import org.jetbrains.anko.selector
import org.jetbrains.anko.toast
import java.io.File
import java.lang.Exception
import java.util.HashMap

class EditProfile : AppCompatActivity() {

    var myUID = FirebaseUtils.getUid()
    val context = this
    var isProfileChanged = false
    lateinit var bitmap:Bitmap
    lateinit var imageFile:File
    var isForAccountCreation = false
    private var userHelper: UserHelper? = null
    var profileURL = ""
    var name = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)


        isForAccountCreation = intent.getBooleanExtra(utils.constants.KEY_IS_ON_ACCOUNT_CREATION, false)

        if(supportActionBar!=null && !isForAccountCreation)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        title = "My Profile"

        myUID = FirebaseUtils.getUid()
        userHelper = UserHelper(this)

        FirebaseUtils.loadProfilePic(this, myUID, profile_circleimageview)


        ///Pick Profile Image Button
        profile_pick_btn.setOnClickListener {
//            ImagePicker.pickImage(context)

            selector("Edit profile picture", listOf("Change picture", "Remove picture")) { _, pos ->

                if(pos == 0){
                    CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setCropShape(CropImageView.CropShape.RECTANGLE)
                    .setAspectRatio(1,1)
                    .start(this)
                }
                else{
                    //delete pic
                     FirebaseUtils.ref.user(myUID)
                        .child(FirebaseUtils.KEY_PROFILE_PIC_URL).setValue("").addOnSuccessListener { toast("Profile pic removed") }
                }

            }


        }



        ///Fill TextEdits with user info if user
        // already logged in
        if(FirebaseUtils.isLoggedIn()) {
            FirebaseUtils.ref.user(myUID)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) { }

                    override fun onDataChange(p0: DataSnapshot) {
                        var full_name = ""
                        var city = ""
                        var bio = "Hey there, I am a FrenzApp User"
                        if(p0.exists()){
                            try {
                                profileURL=p0.getValue(Models.User::class.java)?.profile_pic_url!!
                                name = p0.getValue(Models.User::class.java)?.name!!
                                full_name= p0.getValue(Models.User::class.java)?.full_name!!
                                bio = p0.getValue(Models.User::class.java)?.bio!!
                                city = p0.getValue(Models.User::class.java)?.city!!
                            } catch (e: Exception) { }
                        }

                        profile_name.setText(name)
                        profile_bio.setText(bio)
                        profile_location.setText(city)
                        profile_full_name.setText(full_name)
                    }
                })
        }



        ///Update user Info
        updateProfileBtn.setOnClickListener {

            /// Check if username is empty
            if(profile_name.text.isEmpty()){
                profile_name.error = "Username cannot be empty"
                AnimationUtil.shakeView(profile_name, this@EditProfile)
                return@setOnClickListener
            }
            /// Check if full name is empty
            if(profile_full_name.text.isEmpty()){
                profile_full_name.error = "Enter your full name"
                AnimationUtil.shakeView(profile_full_name, this@EditProfile)
                return@setOnClickListener
            }
            /// Check if city is empty
            if(profile_location.text.isEmpty()){
                profile_location.error = "Tell us your current city"
                AnimationUtil.shakeView(profile_location, this@EditProfile)
                return@setOnClickListener
            }
            /// Check if bio is empty
            if(profile_bio.text.isEmpty()){
                profile_bio.error = "Write your current status"
                AnimationUtil.shakeView(profile_bio, this@EditProfile)
                return@setOnClickListener
            }




            //Check if username is free for usage....
            FirebaseFirestore.getInstance().collection("Usernames")
                    .document(profile_name.text.toString())
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        if (!documentSnapshot.exists()) {

                            registerUser()
                        } else {
                            if (profile_name.text.toString().equals(name)) {
                                registerUser()
                            } else {
                                Toasty.error(this@EditProfile, "Username alreay exists", Toasty.LENGTH_LONG, true).show()
                                AnimationUtil.shakeView(profile_name, this@EditProfile)
                            }

                        }
                    }
                    .addOnFailureListener { e -> Log.e("Error", e.message) }

        }

        //load profile name
//        FirebaseUtils.ref.user(FirebaseUtils.getUid())
//            .child(FirebaseUtils.KEY_NAME)
//            .addListenerForSingleValueEvent(object : ValueEventListener {
//                override fun onCancelled(p0: DatabaseError) {
//                }
//
//                override fun onDataChange(p0: DataSnapshot) {
//                    profile_name.setText( p0.value.toString().trim() )
//                }
//            })

    }

    private fun registerUser() {
        val usernameMap = HashMap<String, Any>()
        usernameMap["username"] = profile_name.text.toString()

        //Create Username
        FirebaseFirestore.getInstance().collection("Usernames")
                .document(profile_name.text.toString())
                .set(usernameMap)
                .addOnSuccessListener {

                    //Update Username
                    FirebaseUtils.ref.user(myUID)
                            .child(FirebaseUtils.KEY_NAME)
                            .setValue(profile_name.text.toString())

                    //Update full name
                    FirebaseUtils.ref.user(myUID)
                            .child(FirebaseUtils.KEY_FULL_NAME)
                            .setValue(profile_full_name.text.toString())
                    //Update current City
                    FirebaseUtils.ref.user(myUID)
                            .child(FirebaseUtils.KEY_CITY)
                            .setValue(profile_location.text.toString())
                    //Update current BIO
                    FirebaseUtils.ref.user(myUID)
                            .child(FirebaseUtils.KEY_BIO)
                            .setValue(profile_bio.text.toString())


                    if(profile_name.text.isNotEmpty()){
                        if(FirebaseUtils.isLoggedIn()) {
                            FirebaseAuth.getInstance().currentUser!!.updateProfile(
                                    UserProfileChangeRequest.Builder()
                                            .setDisplayName(profile_name.text.toString().trim()).build())
                        }
                    }



                    //Check if profile image is changed and
                    // upload if changed...
                    if(isProfileChanged) {

                        val storageRef = FirebaseUtils.ref.profilePicStorageRef(myUID)

                        val dbRef = FirebaseUtils.ref.user(myUID)
                                .child(FirebaseUtils.KEY_PROFILE_PIC_URL)

                        uploadProfilePic(context, imageFile, storageRef, dbRef, "Profile updated")

                        // uploadImage(imageFile)
                        isProfileChanged = false
                    }




                    userHelper?.updateContactUserName(1,profile_name.text.toString())
                    userHelper?.updateContactName(1,profile_full_name.text.toString())
                    userHelper?.updateContactLocation(1,profile_location.text.toString())
                    userHelper?.updateContactBio(1,profile_bio.text.toString())
                    userHelper?.updateContactImage(1,profileURL)
                    userHelper?.updateContactEmail(1,FirebaseUtils.getPhoneNumber())



                    //Check if its a new account
                    // If so, Close this activity and open main activity
                    if (!isProfileChanged) {
                        if (intent.getBooleanExtra(utils.constants.KEY_IS_ON_ACCOUNT_CREATION, false)) {
                            newAccountIntoFireStore(profileURL)
                        } else {
                            userHelper?.updateContactLocation(1,profile_location.text.toString())
                            userHelper?.updateContactBio(1,profile_bio.text.toString())
                        }
                    }




                }
                .addOnFailureListener { e -> Log.e("Error", e.message) }
    }


    private fun uploadProfilePic(context: Context, file: File, storageRef: StorageReference,
                                 dbRef: DatabaseReference,
                                 toastAfterUploadIfAny: String){


        val dialog = ProgressDialog(context)
        dialog.setMessage("Wait a moment...")
        dialog.setCancelable(false)
        dialog.show()



        Log.d("EditProfile", "uploadImage: File size = "+file.length()/1024)




        val uploadTask = storageRef.putFile(utils.getUriFromFile(context, file))

        uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            Log.d("FirebaseUtils", "uploadedImage: size = "+task.result!!.bytesTransferred/1024)
            return@Continuation storageRef.downloadUrl
        })
            .addOnCompleteListener { task ->

                try {
                    dialog.dismiss()
                }catch (e:Exception){}
                if(task.isSuccessful) {
                    val link = task.result

                    dbRef.setValue(link.toString())
                        .addOnSuccessListener {
                            //   isProfileChanged = false
                            if(toastAfterUploadIfAny.isNotEmpty())
                                utils.toast(context, toastAfterUploadIfAny) }


                    if(FirebaseUtils.isLoggedIn()) {
                        FirebaseAuth.getInstance().currentUser!!
                            .updateProfile(
                                UserProfileChangeRequest.Builder()
                                    .setPhotoUri(link).build()
                            )
                    }





                    if(intent.getBooleanExtra(utils.constants.KEY_IS_ON_ACCOUNT_CREATION, false)){
                        newAccountIntoFireStore(link.toString())
                    }else{
                        userHelper?.updateContactUserName(1,profile_name.text.toString())
                        userHelper?.updateContactName(1,profile_full_name.text.toString())
                        userHelper?.updateContactLocation(1,profile_location.text.toString())
                        userHelper?.updateContactBio(1,profile_bio.text.toString())
                        userHelper?.updateContactImage(1,link.toString())
                    }




                }
                else
                    utils.toast(context, task.exception!!.message.toString())
            }

            .addOnSuccessListener {
                dialog.dismiss()


            }
            .addOnFailureListener{
                dialog.dismiss()
            }



    }



    private fun newAccountIntoFireStore(imageLink: String) {

        val userMap = HashMap<String, Any>()
        userMap["id"] = myUID
        userMap["name"] = profile_full_name.text.toString()
        userMap["image"] = imageLink
        userMap["email"] = FirebaseUtils.getPhoneNumber()
        userMap["bio"] = profile_bio.text.toString()
        userMap["username"] = profile_name.text.toString()
        userMap["location"] = profile_location.text.toString()

        FirebaseFirestore.getInstance().collection("Users")
                .document(myUID).set(userMap)
                .addOnSuccessListener {


                    FirebaseInstanceId.getInstance()
                            .instanceId
                            .addOnCompleteListener {
                                if (!it.isSuccessful)
                                    return@addOnCompleteListener

                                val token_id = it.result!!.token

                                FirebaseFirestore.getInstance().collection("Users").document(myUID).get()
                                        .addOnSuccessListener { documentSnapshot ->

                                            val tokenMap = HashMap<String, Any>()
                                            tokenMap["token_ids"] = FieldValue.arrayUnion(token_id)

                                            FirebaseFirestore.getInstance().collection("Users")
                                                    .document(myUID)
                                                    .update(tokenMap)
                                                    .addOnSuccessListener {

                                                        FirebaseFirestore.getInstance().collection("Users")
                                                                .document(myUID).get().addOnSuccessListener { documentSnapshot1 ->

                                                                    val pref = applicationContext.getSharedPreferences(Config.SHARED_PREF, Context.MODE_PRIVATE)
                                                                    val editor = pref.edit()
                                                                    editor.putString("regId", token_id)
                                                                    editor.apply()

                                                                    val username = documentSnapshot1.getString("username")
                                                                    val name = documentSnapshot1.getString("name")
                                                                    val email = documentSnapshot1.getString("email")
                                                                    val image = documentSnapshot1.getString("image")
                                                                    val location = documentSnapshot1.getString("location")
                                                                    val bio = documentSnapshot1.getString("bio")

                                                                    userHelper?.updateContactUserName(1,username)
                                                                    userHelper?.updateContactName(1,name)
                                                                    userHelper?.updateContactLocation(1,location)
                                                                    userHelper?.updateContactBio(1,bio)
                                                                    userHelper?.updateContactImage(1,image)
                                                                    userHelper?.updateContactEmail(1,email)

                                                                    MainActivity.startActivity(this@EditProfile)
                                                                    finish()

                                                                }
                                                                .addOnFailureListener { e ->
                                                                    Log.e("Error", ".." + e.message)
                                                                }


                                                    }.addOnFailureListener { e ->
                                                        Toasty.error(this@EditProfile, "Error: "
                                                                + e.message, Toasty.LENGTH_SHORT, true).show()
                                                    }

                                        }


                            }

                }.addOnFailureListener { e ->
                    Toasty.error(this@EditProfile, "Error: "
                            + e.message, Toasty.LENGTH_SHORT, true).show()
                }

    }




    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when(resultCode){
            Activity.RESULT_OK -> {


                utils.printIntentKeyValues(data!!)

                 val result = CropImage.getActivityResult(data)
                val filePath = result.uri.path

                Log.d("EditProfile", "onActivityResult: $filePath")
                    //utils.getRealPathFromURI(context, result.uri)
                     //ImagePicker.getImagePathFromResult(context, requestCode, resultCode, data)

                Luban.compress(context, File(filePath))
                    .putGear(Luban.THIRD_GEAR)
                    .launch(object : OnCompressListener {
                        override fun onStart() {

                        }

                        override fun onSuccess(file: File?) {

                            imageFile = file!!

                            bitmap = BitmapFactory.decodeFile(file.path)

                            profile_circleimageview.setImageBitmap(bitmap)

                            isProfileChanged = true



                        }

                        override fun onError(e: Throwable?) {
                            utils.toast(context, e!!.message.toString())
                        }

                    })


            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }





    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if(item!!.itemId == android.R.id.home)
            finish()
        return super.onOptionsItemSelected(item)
    }

}
