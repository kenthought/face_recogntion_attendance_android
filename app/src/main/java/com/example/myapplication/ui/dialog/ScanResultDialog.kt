package com.example.myapplication.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.example.myapplication.R
import com.example.myapplication.classes.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

var isShowing = false;

class ScanResultDialog : DialogFragment() {
    private lateinit var database: DatabaseReference
    var dialogIsShown = isShowing;

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        database = Firebase.database.reference
        val user = Firebase.auth.currentUser
        val bundle = arguments

        return activity?.let {
                val builder = AlertDialog.Builder(it)
                // Get the layout inflater
                val inflater = requireActivity().layoutInflater;
                // Inflate and set the layout for the dialog

                builder.setTitle("Result")
                val dialogView = inflater.inflate(R.layout.scan_result_dialog, null)
                isShowing = true;
                val scanResult = bundle!!.getString("SCAN RESULT", "")
                if (scanResult == "successful") {
                    var checkStudent = false;
                    val className = bundle!!.getString("CLASS NAME", "")
                    val classId = bundle!!.getString("CLASS ID", "")
                    val subject = bundle!!.getString("SUBJECT", "")
                    val studentName = bundle!!.getString("STUDENT NAME", "")
                    val time = bundle!!.getString("TIME", "")
                    database.child("students").child(classId).get().addOnSuccessListener {
                        for (childSnapshot in it.children) {
                            childSnapshot.getValue<StudentItem>()?.let {
                                Log.d("NAME", it.student_name)
                                if (studentName == it.student_name) {
                                    checkStudent = true
                                    Log.d("NAME1", checkStudent.toString())
                                }
                            }
                            if (checkStudent)
                                break
                        }

                        if (checkStudent) {
                            val key: String = database.child("attendance").push().key.toString()

                            database.child("attendance/" + user!!.uid + "/" + key)
                                .setValue(
                                    AttendanceItem(
                                        key,
                                        className,
                                        subject,
                                        studentName,
                                        time
                                    )
                                )

                            val date = time.substring(0, time.indexOf(" "))
                            val calendar = Calendar.getInstance()
                            val simpleDateFormat = SimpleDateFormat("dd/M/yyyy")
                            calendar.time = simpleDateFormat.parse(date)

                            database.child(
                                "report/" + classId + "/" + calendar.get(Calendar.YEAR)
                                    .toString() + "/"
                                        + calendar.get(Calendar.MONTH)
                                    .toString() + "/" + calendar.get(Calendar.DATE).toString()
                            )
                                .push().setValue(AttendanceReporting(studentName, time))
                            val scanResultText =
                                dialogView.findViewById(R.id.scan_result_text) as TextView
                            scanResultText.text = "Scan successful!\n\nWelcome " + studentName
                            val showSuccess =
                                dialogView.findViewById(R.id.scan_result_success) as ImageView
                            showSuccess.visibility = View.VISIBLE
                        } else {
                            val scanResultText =
                                dialogView.findViewById(R.id.scan_result_text) as TextView
                            scanResultText.text = "Scan failed\n\n" + studentName + " is not in this class!"
                            val showFailed =
                                dialogView.findViewById(R.id.scan_result_failed) as ImageView
                            showFailed.visibility = View.VISIBLE
                        }
                    }.addOnFailureListener {
                        val scanResultText =
                            dialogView.findViewById(R.id.scan_result_text) as TextView
                        scanResultText.text = "Scan failed"
                        val showFailed =
                            dialogView.findViewById(R.id.scan_result_failed) as ImageView
                        showFailed.visibility = View.VISIBLE
                    }
                } else {
                    val scanResultText = dialogView.findViewById(R.id.scan_result_text) as TextView
                    scanResultText.text = "Scan failed"
                    val showFailed = dialogView.findViewById(R.id.scan_result_failed) as ImageView
                    showFailed.visibility = View.VISIBLE
                }


                isCancelable = false
                // Pass null as the parent view because its going in the dialog layout
                builder.setView(dialogView)
                    .setPositiveButton("OK", DialogInterface.OnClickListener { dialog, id ->
                        isShowing = false
                    })
                builder.create()

        } ?: throw IllegalStateException("Activity cannot be null")
    }
}