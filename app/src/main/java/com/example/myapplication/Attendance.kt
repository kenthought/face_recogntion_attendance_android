package com.example.myapplication

import com.example.myapplication.ui.activity.FaceRecognitionActivity

class Attendance {

    companion object {

        fun attendance( studentName : String, time: String ) {
            FaceRecognitionActivity.listAttendance(studentName, time)
        }

    }

}