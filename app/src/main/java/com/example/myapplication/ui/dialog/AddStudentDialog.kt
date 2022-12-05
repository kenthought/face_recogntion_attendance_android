package com.example.myapplication.ui.dialog

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.DialogFragment
import com.example.myapplication.R
import com.example.myapplication.classes.StudentItem
import com.example.myapplication.classes.StudentPicture
import com.example.myapplication.classes.YearLevel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.component1
import com.google.firebase.storage.ktx.component2
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.ktx.storageMetadata
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*
import kotlin.collections.ArrayList


class AddStudentDialog  : DialogFragment()  {
    private lateinit var database: DatabaseReference
    private var storage = Firebase.storage
    private lateinit var studentName: EditText
    private lateinit var yearLevels: Spinner
    private var imageUri: Uri? = null
    private lateinit var file_name: TextView
    private lateinit var imageView: ImageView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        database = Firebase.database.reference
        val user = Firebase.auth.currentUser
        getYearLevels(database.child("year_level"))
        val bundle = arguments

        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater;
            // Inflate and set the layout for the dialog

            val dialogView = inflater.inflate(R.layout.add_student_dialog, null)
            val idNumber = dialogView.findViewById(R.id.idNumber) as EditText
            studentName = dialogView.findViewById(R.id.studentName) as EditText
            yearLevels = dialogView.findViewById(R.id.yearLevels) as Spinner
            imageView = dialogView.findViewById(R.id.imageView2) as ImageView
            val className = bundle!!.getString("CLASS NAME", "")
            val classID = bundle!!.getString("CLASS ID", "")
            val subject = bundle!!.getString("SUBJECT", "")
            val students = bundle!!.getString("STUDENT SIZE", "")
            val takePhoto = dialogView.findViewById(R.id.add_photo) as ImageButton
            file_name = dialogView.findViewById(R.id.file_name) as TextView

            takePhoto.setOnClickListener {
                startChooseImageIntentForResult()
//                startCameraIntentForResult()
            }

            // Pass null as the parent view because its going in the dialog layout
            builder.setView(dialogView)
                .setPositiveButton("Add",
                    DialogInterface.OnClickListener { dialog, id ->
                        val studentPicture = StudentPicture(idNumber.text.toString(),
                                studentName.text.toString(), yearLevels.selectedItem.toString(),
                                className, subject, file_name.text.toString())
                        val studentItem = StudentItem(idNumber.text.toString(),
                            studentName.text.toString(), yearLevels.selectedItem.toString(),
                            className, subject)

                        database.child("student_picture").child(studentName.text.toString()).push().setValue(studentPicture)
                        database.child("students").child(classID).push().setValue(studentItem)
                        database.child("classes/" + user!!.uid + "/" + classID + "/" + "students/").setValue(Integer.parseInt(students) + 1)
                        saveToFaceRecognition();
                        uploadImage();
                    })
                .setNegativeButton("Cancel",
                    DialogInterface.OnClickListener { dialog, id ->
                        getDialog()?.cancel()
                    })
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun startChooseImageIntentForResult() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(intent, "Select Picture"),
            1002
        )
    }

    private fun startCameraIntentForResult() { // Clean up last time's image
//        appAlbumDirectory()
//        imageUri = null
//        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//        val packageManager = requireActivity().packageManager
//        val contentResolver = requireActivity().contentResolver
//        if (takePictureIntent.resolveActivity(packageManager) != null) {
//            val values = ContentValues()
//            values.put(MediaStore.Images.Media.TITLE, "New Picture")
//            values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
//            imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
//            Log.d("image", imageUri.toString())
//            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
//            startActivityForResult(
//                takePictureIntent,
//                1001
//            )
//        }
    }

    fun saveToFaceRecognition() {
        var outputStream: OutputStream;
        var bitmapDrawable = imageView.drawable;
        var bitmap = bitmapDrawable.toBitmap()
        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                var resolver = requireActivity().contentResolver;
                var contentValues = ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "FRIMAGE.jpg")
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES+File.separator+"SPCT"+File.separator+studentName.text.toString())
                var imageUri2 = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                outputStream = resolver.openOutputStream(Objects.requireNonNull(imageUri2!!))!!;
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                Objects.requireNonNull(outputStream)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Cannot execute copy image\n"+e.message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1002 && resultCode == Activity.RESULT_OK) {
            // In this case, imageUri is returned by the chooser, save it.
            imageUri = data!!.data!!
            file_name.text = imageUri!!.lastPathSegment.toString()
                .substring(imageUri!!.lastPathSegment.toString().lastIndexOf("/") + 1)
            imageView.setImageURI(imageUri);
        } else if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            // In this case, imageUri is returned by the chooser, save it.
            imageUri = data!!.data!!
            file_name.text = imageUri!!.lastPathSegment.toString()
                .substring(imageUri!!.lastPathSegment.toString().lastIndexOf("/") + 1)
        } else
            super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getYearLevels(yearLevelRef: DatabaseReference) {
        val yearListener = object : ValueEventListener {
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
        yearLevelRef.addValueEventListener(yearListener)
    }


    fun uploadImage() {
        val storageRef = storage.reference

        val studentPicture = storageRef.child(studentName.text.toString() + "/"+file_name.text+".jpg")
        val inputStream = requireActivity().contentResolver.openInputStream(imageUri!!)

        val metadata = storageMetadata {
            contentType = "image/jpeg"
        }

        var uploadTask = studentPicture.putStream(inputStream!!, metadata)

        uploadTask.addOnProgressListener { (bytesTransferred, totalByteCount) ->
            val progress = (100.0 * bytesTransferred) / totalByteCount
            Log.d("PROGRESS", "Upload is $progress% done")
        }.addOnPausedListener {
            Log.d("PAUSED", "Upload is paused")
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed uploading picture", Toast.LENGTH_SHORT)
        }.addOnSuccessListener {
            Toast.makeText(requireContext(), "Picture uploaded", Toast.LENGTH_SHORT)
        }

    }

//    private fun downloadImage() {
//        Log.d("we", "www")
//        val storageRef = storage.reference
////        val picRef = storageRef.child(studentName.text.toString() + "/"+file_name.text)
//
//        val picRef = storageRef.child("Student Y/image:1000000934.jpg")
//        val localFile = File.createTempFile("Student Y", "jpg")
//
//        picRef.getFile(localFile).addOnSuccessListener {
//            Log.d("we", "www2")
//            val fileOutputStream = FileOutputStream(File( requireContext().filesDir.absolutePath + "/$name.png"))
//            image.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
//        }.addOnFailureListener {
//            // Handle any errors
//        }
//    }

    fun copyStreamToFile(localFile : File) {

    }
}