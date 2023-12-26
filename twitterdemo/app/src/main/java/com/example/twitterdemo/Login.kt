package com.example.twitterdemo

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date

@Suppress("DEPRECATION")
class Login : AppCompatActivity() {
    private var mAuth:FirebaseAuth?=null
    private var database= FirebaseDatabase.getInstance()
    private var myRef=database.reference
    var ivp:ImageView? = null
    var btnlgn:Button? = null
    var inemail:EditText? = null
    var inpass:EditText? = null
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var isReadPermissionGranted = false

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        mAuth = FirebaseAuth.getInstance()



        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permissions ->

            isReadPermissionGranted = permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE] ?: isReadPermissionGranted
            isReadPermissionGranted = permissions[android.Manifest.permission.READ_MEDIA_IMAGES]?: isReadPermissionGranted

        }

        requestPermission()
        mAuth = FirebaseAuth.getInstance()

        inemail = findViewById<EditText>(R.id.etEmail)
        inpass = findViewById<EditText>(R.id.etPassword)
        ivp = findViewById<ImageView>(R.id.ivimagePerson)
        ivp!!.setOnClickListener(View.OnClickListener {
            checkPermission()
        })

        btnlgn = findViewById<Button>(R.id.btnLogin)
        btnlgn!!.setOnClickListener {
            LoginToFirebase(inemail!!.text.toString(),inpass!!.text.toString())
        }
    }

    fun LoginToFirebase(email:String,password:String){

        mAuth!!.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener(this){task ->

                if (task.isSuccessful){
                    Toast.makeText(applicationContext,"Successful Login",Toast.LENGTH_SHORT).show()

                    SaveImageInFirebase()
                }
                else{
                    Toast.makeText(applicationContext,"Failed Login",Toast.LENGTH_SHORT).show()
                }

            }
    }

    @SuppressLint("SimpleDateFormat")
    fun SaveImageInFirebase(){
        var currentUser = mAuth!!.currentUser
        val email:String = currentUser!!.email.toString()
        val storage= FirebaseStorage.getInstance()
        val storageRef = storage.getReferenceFromUrl("gs://twitterdemo-46f62.appspot.com")
        val df=SimpleDateFormat("ddMMyyHHmmSS")
        val dataobj = Date()
        val imagePAth = SplitString(email) + "." +df.format(dataobj)+ ".jpg"
        val ImageRef = storageRef.child("images/" + imagePAth)
        ivp!!.isDrawingCacheEnabled = true
        ivp!!.buildDrawingCache()

        val drawable = ivp!!.drawable as BitmapDrawable
        val bitmap = drawable.bitmap
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,baos)
        val data = baos.toByteArray()
        val uploadTask = ImageRef.putBytes(data)
        uploadTask.addOnFailureListener{
            Toast.makeText(applicationContext,"Failed To Upload",Toast.LENGTH_SHORT).show()
        }.addOnSuccessListener { taskSnapshot ->

            ImageRef.downloadUrl.addOnSuccessListener { uri ->
                val downloadURL = uri.toString()
                myRef.child("Users").child(currentUser.uid).child("Email").setValue(currentUser.email)
                myRef.child("Users").child(currentUser.uid).child("ProfileImage").setValue(downloadURL)
                LoadTweets()
            }
//            var DownloadURL= taskSnapshot.storage.downloadUrl.toString()

        }
    }

    override fun onStart() {
        super.onStart()
        LoadTweets()
    }


    fun LoadTweets(){
        var currentUser = mAuth!!.currentUser
        if(currentUser!= null){
            var intent = Intent(this,MainActivity::class.java)
            intent.putExtra("email",currentUser.email)
            intent.putExtra("uid",currentUser.uid)
            startActivity(intent)
        }
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private  fun requestPermission(){

        isReadPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES, android.Manifest.permission.READ_EXTERNAL_STORAGE).toString()
        ) == PackageManager.PERMISSION_GRANTED

        val permissionRequest: MutableList<String> = ArrayList()

        if(!isReadPermissionGranted){
            permissionRequest.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            permissionRequest.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if(permissionRequest.isNotEmpty()){
            permissionLauncher.launch(permissionRequest.toTypedArray())
        }
    }

    fun SplitString(email:String):String{
        val split = email.split("@")
        return split[0]
    }

    val READIMAGE:Int = 253
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun checkPermission(){
        if (Build.VERSION.SDK_INT>= 32){
            if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.READ_MEDIA_IMAGES)!=PackageManager.PERMISSION_GRANTED){
                requestPermissions(arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES),READIMAGE)
                if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.READ_MEDIA_IMAGES)!=PackageManager.PERMISSION_GRANTED){
                    showPermissionDialog()
                }
                return
            }
            else if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.READ_MEDIA_IMAGES)==PackageManager.PERMISSION_GRANTED){
                loadImage()
                return
            }
        }

        else{
            if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),READIMAGE)
                if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
                    showPermissionDialog()
                }
                return
            }
            else if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED){
                loadImage()
                return
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode){
            READIMAGE->
            {if (grantResults[0]!=PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this,"Cannot access your images",Toast.LENGTH_SHORT).show()
                }
            else{
                loadImage()
            }}
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
val PICK_IMAGE_CODE = 123
    fun loadImage(){
        var intent = Intent(Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent,PICK_IMAGE_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==PICK_IMAGE_CODE && data != null){
            val selectedImage = data.data
            val filePathColum = arrayOf(MediaStore.Images.Media.DATA)
            val cursor = contentResolver.query(selectedImage!!,filePathColum,null,null,null)
            cursor!!.moveToFirst()
            val coulmImdex=cursor.getColumnIndex(filePathColum[0])
            val picturePath=cursor.getString(coulmImdex)
            cursor.close()
            ivp!!.setImageBitmap(BitmapFactory.decodeFile(picturePath))
        }
    }


    fun showPermissionDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Permission Required")
        builder.setMessage("Some permissions are needed to be allowed to use this feature.")
        builder.setPositiveButton("Grant"){d,_ ->
            d.cancel()
            startActivity(Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")))
        }
        builder.setNegativeButton("Cancel"){d,_ ->
            d.dismiss()
        }
        builder.show()
    }
}