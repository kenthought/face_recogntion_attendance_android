package com.example.myapplication.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.myapplication.R
import com.example.myapplication.classes.CreateClass
import com.example.myapplication.classes.SubjectsItem
import com.example.myapplication.classes.YearLevel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class AddClassDialog  : DialogFragment() {
    private lateinit var database: DatabaseReference
    private lateinit var yearLevels: Spinner
    private lateinit var subjects: Spinner

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        database = Firebase.database.reference
        val user = Firebase.auth.currentUser
        getSubject(database.child("subjects/" + user!!.uid))
        getYearLevels(database.child("year_level"))

        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater;
            // Inflate and set the layout for the dialog

            val dialogView = inflater.inflate(R.layout.add_class_dialog, null)

            val className = dialogView.findViewById(R.id.classNameInput) as EditText
            yearLevels = dialogView.findViewById(R.id.yearLevels) as Spinner
            subjects = dialogView.findViewById(R.id.subjects) as Spinner

            // Pass null as the parent view because its going in the dialog layout
            builder.setView(dialogView)
                .setPositiveButton("Add",
                    DialogInterface.OnClickListener { dialog, id ->
                        if(className.text.toString() != "") {
                            val key : String = database.child("classes").push().key.toString()
                            database.child("classes/" + user!!.uid + "/" + key).setValue(
                                CreateClass(
                                    key,
                                    className.text.toString(),
                                    yearLevels.selectedItem.toString(),
                                    subjects.selectedItem.toString()
                                )
                            )
                        }else
                            Toast.makeText(context, "Please enter class name!", Toast.LENGTH_LONG).show()
                    })
                .setNegativeButton("Cancel",
                    DialogInterface.OnClickListener { dialog, id ->
                        getDialog()?.cancel()
                    })
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun getSubject(subjectRef: DatabaseReference) {
        val subjectListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get Post object and use the values to update the UI
                var subject = ArrayList<SubjectsItem>();
                var options: ArrayList<String> = ArrayList()
                var count = 0;
                for (childSnapshot in dataSnapshot.children) {
                    childSnapshot.getValue<SubjectsItem>()?.let { subject.add(count, it) }
                    count++;
                }

                for (item in subject) {
                    options.add(item.subject_name);
                }

                val dataAdapter = context?.let {
                    ArrayAdapter(
                        it, android.R.layout.simple_spinner_dropdown_item, options
                    )
                }
                dataAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                subjects.adapter = dataAdapter

                Log.d("GET", subject.toString())
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("ERROR", "loadPost:onCancelled", databaseError.toException())
            }
        }
        subjectRef.addValueEventListener(subjectListener)
    }

    private fun getYearLevels(yearLevelRef: DatabaseReference) {
        val subjectListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get Post object and use the values to update the UI
                var yearLevel = ArrayList<YearLevel>();
                var options: ArrayList<String> = ArrayList()
                var count = 0;
                for (childSnapshot in dataSnapshot.children) {
                    childSnapshot.getValue<YearLevel>()?.let { yearLevel.add(count, it) }
                    count++;
                }

                for (item in yearLevel) {
                    options.add(item.yearLevel);
                }

                val dataAdapter = context?.let {
                    ArrayAdapter(
                        it, android.R.layout.simple_spinner_dropdown_item, options
                    )
                }
                dataAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                yearLevels.adapter = dataAdapter

                Log.d("GET", yearLevel.toString())
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("ERROR", "loadPost:onCancelled", databaseError.toException())
            }
        }
        yearLevelRef.addValueEventListener(subjectListener)
    }
}