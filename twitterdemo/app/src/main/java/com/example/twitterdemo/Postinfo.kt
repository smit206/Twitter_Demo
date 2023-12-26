package com.example.twitterdemo

class Postinfo {

    var UserUID:String? = null
    var text:String? = null
    var postImage:String? = null

    constructor(UserUID:String,text:String,postImage:String){
        this.UserUID = UserUID
        this.text = text
        this.postImage = postImage
    }
}