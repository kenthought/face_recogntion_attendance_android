package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.classes.ClassItem
import com.example.myapplication.ui.activity.FaceRecognitionActivity
import com.example.myapplication.ui.dialog.AddStudentDialog


class ClassAdapter(private val classItem : ArrayList<ClassItem>) : RecyclerView.Adapter<ClassAdapter.ClassViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val viewItem = LayoutInflater.from(parent.context).inflate(R.layout.card_class, parent, false)
        val addStudentButton = viewItem.findViewById<ImageButton>(R.id.addStudentButton)

        viewItem.setOnClickListener {

            val intent = Intent(parent.context, FaceRecognitionActivity::class.java).apply {
                val className = viewItem.findViewById(R.id.className) as TextView
                val classID = viewItem.findViewById(R.id.classID) as TextView
                val subject = viewItem.findViewById(R.id.subject) as TextView
                putExtra("CLASS NAME", className.text.toString())
                putExtra("CLASS ID", classID.text.toString())
                putExtra("SUBJECT", subject.text.toString())
            }

            parent.context.startActivity(intent)

            Toast.makeText(
                parent.context, "Starting Face Recognition...", Toast.LENGTH_LONG
            ).show()
        }
        addStudentButton.setOnClickListener{
            val className = viewItem.findViewById(R.id.className) as TextView
            val classID = viewItem.findViewById(R.id.classID) as TextView
            val subject = viewItem.findViewById(R.id.subject) as TextView
            val students = viewItem.findViewById(R.id.studentSize) as TextView
            val activity: FragmentActivity = parent.context as FragmentActivity
            val fm: FragmentManager = activity.supportFragmentManager

            val bundle = Bundle()
            bundle.putString("CLASS NAME", className.text.toString())
            bundle.putString("CLASS ID", classID.text.toString())
            bundle.putString("SUBJECT", subject.text.toString())
            bundle.putString("STUDENT SIZE", students.text.toString())
            val addDialogFragment = AddStudentDialog()
            addDialogFragment.arguments = bundle;
            addDialogFragment.show(fm,"add")
        }
        return ClassViewHolder(viewItem)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        val currentItem = classItem[position]
        holder.className.text = currentItem.class_name
        holder.classID.text = currentItem.id
        holder.subject.text = currentItem.subject
        holder.studentSize.text = currentItem.students.toString()
    }

    override fun getItemCount(): Int {
        return classItem.size
    }

    class ClassViewHolder(viewItem: View): RecyclerView.ViewHolder(viewItem) {
        val className : TextView = viewItem.findViewById(R.id.className)
        val subject : TextView = viewItem.findViewById(R.id.subject)
        val classID : TextView = viewItem.findViewById(R.id.classID)
        val studentSize : TextView = viewItem.findViewById(R.id.studentSize)
    }

}