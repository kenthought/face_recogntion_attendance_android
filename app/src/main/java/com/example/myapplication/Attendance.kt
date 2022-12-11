package com.example.myapplication

import androidx.fragment.app.FragmentManager
import com.example.myapplication.ui.activity.FaceRecognitionActivity

class Attendance {

    companion object {

        fun attendance( studentName : String, time: String, className: String, classID: String, subject: String, fragmentManager: FragmentManager ) {
            FaceRecognitionActivity.listAttendance(studentName, time, className, classID, subject, fragmentManager)
        }

    }

}