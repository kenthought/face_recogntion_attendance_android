package com.example.myapplication

import com.example.myapplication.ui.activity.FaceRecognitionActivity

// Logs message using log_textview present in activity_main.xml
class Logger {

    companion object {

        fun log( message : String ) {
            FaceRecognitionActivity.setMessage(  FaceRecognitionActivity.logTextView.text.toString() + "\n" + ">> $message" )
            // To scroll to the last message
            // See this SO answer -> https://stackoverflow.com/a/37806544/10878733
            while ( FaceRecognitionActivity.logTextView.canScrollVertically(1) ) {
                FaceRecognitionActivity.logTextView.scrollBy(0, 10);
            }
        }

    }

}