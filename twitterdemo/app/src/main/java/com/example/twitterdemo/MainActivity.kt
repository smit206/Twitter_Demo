package com.example.twitterdemo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {
//    private var mAuth: FirebaseAuth?=null

    private var mAuth:FirebaseAuth? = null
    private var database= FirebaseDatabase.getInstance()
    private var myRef=database.reference
    var ListTweets = ArrayList<Ticket>()
    var lvlist:ListView? = null
    var adapter:MyTweetAdapter? = null
    var myemail:String? = null
    var UserUID:String? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mAuth = FirebaseAuth.getInstance()
        lvlist = findViewById<ListView>(R.id.lvTweets)

        var b:Bundle = intent.extras!!
        myemail=b.getString("email")
        UserUID=b.getString("uid")

        //Dummy data
        ListTweets.add(Ticket("0","him","url","add"))

        adapter = MyTweetAdapter(this,ListTweets)
        lvlist!!.adapter = adapter

        LoadPost()

    }

    open inner class MyTweetAdapter: BaseAdapter {
//        var currentUser = mAuth!!.currentUser

        var etText:EditText? = null
        var context: Context? = null
        var listofnoteAdapter = ArrayList<Ticket>()
        constructor(context: Context, listofnote:ArrayList<Ticket>):super(){
            this.listofnoteAdapter = listofnote
            this.context = context
        }
        override fun getCount(): Int {
            return listofnoteAdapter.size
        }

        override fun getItem(p0: Int): Any {
            return listofnoteAdapter[p0]
        }

        override fun getItemId(p0: Int): Long {
            return p0.toLong()
        }


        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
            var myTweet = ((listofnoteAdapter[p0]))
            val storage= FirebaseStorage.getInstance()
            val storageRef = storage.getReferenceFromUrl("gs://twitterdemo-46f62.appspot.com")
            val df= SimpleDateFormat("ddMMyyHHmmss")
            val dataobj = Date()
            val imagePAth = SplitString(myemail!!) + "." +df.format(dataobj)+ ".jpg"
            if (myTweet.tweetPersonUID.equals("add")){
                var myView = layoutInflater.inflate(R.layout.add_ticket,null)
                myView.findViewById<ImageView>(R.id.iv_attach).setOnClickListener(View.OnClickListener {
                    loadImage()
                })
                val texts = myView.findViewById<EditText>(R.id.etPost)

                myView.findViewById<ImageView>(R.id.iv_post).setOnClickListener(View.OnClickListener {
                    val postData = hashMapOf(
                        "UserUID" to UserUID,
                        "Text" to texts.text.toString(),
                        "Posted Image" to DownloadURL
                    )
                    //upload to server
                    myRef.child("posts").push().setValue(postData)
                    myView.findViewById<EditText>(R.id.etPost).setText("")
                })
                return myView
            }
            else if(myTweet.tweetPersonUID.equals("loading")){
                var myView = layoutInflater.inflate(R.layout.loading_ticket,null)
                return myView
            }
            else{
                var myView = layoutInflater.inflate(R.layout.tweets_ticket,null)
                var ivptimp:ImageView = myView.findViewById(R.id.tweet_picture)
                var imgpstusr:ImageView = myView.findViewById(R.id.picture_path)
                myView.findViewById<TextView>(R.id.txt_tweet).setText(myTweet.tweetText)
//                myView.findViewById<ImageView>(R.id.tweet_picture).setImageResource(myTweet.tweetImageURL)

                Log.d("ImageUrl", "URL: " + myTweet.tweetImageURL)
//                Picasso.get().load(myTweet.tweetImageURL).into(ivptimp)
//                if (myTweet.tweetImageURL != null) {
//                    Picasso.get().load(myTweet.tweetImageURL).into(ivptimp);
//                }
                if(myTweet.tweetImageURL !=null){
                    Glide.with(this@MainActivity)
                        .load(myTweet.tweetImageURL)
                        .placeholder(R.drawable.tweets)
                        .into(ivptimp)
                }



                myRef.child("Users").child(myTweet.tweetPersonUID!!)
                    .addValueEventListener(object  : ValueEventListener{
                        override fun onDataChange(datasnapshot: DataSnapshot) {

                            try {
                                var td = datasnapshot!!.value as HashMap<String,Any>

                                for(key in td.keys){
                                    var userInfo = td[key] as String
                                    if(key == "Email"){
                                        myView.findViewById<TextView>(R.id.txtUserName).text = userInfo

                                    }else{
//                                        myView.findViewById<TextView>(R.id.txtUserName).text = userInfo
                                        Glide.with(this@MainActivity)
                                            .load(userInfo)
                                            .placeholder(R.drawable.tweets)
                                            .into(imgpstusr)
                                    }
                                }
                            }catch (ex:Exception){
                                Log.e("TAG", "onDataChange: ${ex.localizedMessage}")
                                ex.message
                            }


                        }

                        override fun onCancelled(error: DatabaseError) {
                            TODO("Not yet implemented")
                        }
                    })

                return myView
            }

        }
    }


    // Load Image

    val PICK_IMAGE_CODE = 123
    fun loadImage(){
        var intent = Intent(Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
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
            UploadImage(BitmapFactory.decodeFile(picturePath))
        }
    }


var DownloadURL:String? = null
    @SuppressLint("SimpleDateFormat")
    fun UploadImage(bitmap: Bitmap){
        ListTweets.add(0, Ticket("0","him","url","loading"))
        adapter!!.notifyDataSetChanged()

        var currentUser = mAuth!!.currentUser
        val email:String = currentUser!!.email.toString()
        val storage= FirebaseStorage.getInstance()
        val storageRef = storage.getReferenceFromUrl("gs://twitterdemo-46f62.appspot.com")
        val df= SimpleDateFormat("ddMMyyHHmmss")
        val dataobj = Date()
        val imagePAth = SplitString(email) + "." +df.format(dataobj)+ ".jpg"
        val ImageRef = storageRef.child("imagepost/" + imagePAth)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,baos)
        val data = baos.toByteArray()
        val uploadTask = ImageRef.putBytes(data)
        uploadTask.addOnFailureListener{
            Toast.makeText(applicationContext,"Failed To Upload", Toast.LENGTH_SHORT).show()
        }.addOnSuccessListener {taskSnapshot ->
//            DownloadURL = taskSnapshot.storage.downloadUrl.toString()
                ImageRef.downloadUrl.addOnSuccessListener { uri ->
                    DownloadURL = uri.toString()
                    ListTweets.removeAt(0)
                    adapter!!.notifyDataSetChanged()
                }
        }
    }
    fun SplitString(email:String):String{
        val split = email.split("@")
        return split[0]
    }

    fun LoadPost(){
        myRef.child("posts")
            .addValueEventListener(object  : ValueEventListener{
                override fun onDataChange(datasnapshot: DataSnapshot) {

                    try {
                        ListTweets.clear()
                        ListTweets.add(Ticket("0","him","url","add"))
                        var td = datasnapshot.value as HashMap<String,Any>

                        for(key in td.keys){
                            var post = td[key] as HashMap<String,Any>

                            ListTweets.add(Ticket(key,
                                post["Text"] as String,
                                post["Posted Image"].toString(),
                                post["UserUID"] as String))

                        }
                        adapter!!.notifyDataSetChanged()
                    }catch (ex:Exception){
                        ex.message
                    }


                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TAG", "onCancelled:${error.message}" )
                }
            })
    }

}