package com.example.twitterdemo

class Ticket {

    var tweetId:String? = null
    var tweetText:String? = null
    var tweetImageURL:String? = null
    var tweetPersonUID:String? = null
    constructor(tweetId:String,tweetText:String,tweetImageURL:String,tweetPersonUID:String){
        this.tweetId = tweetId
        this.tweetText = tweetText
        this.tweetImageURL = tweetImageURL
        this.tweetPersonUID = tweetPersonUID

    }
}