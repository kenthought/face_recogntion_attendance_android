package com.example.myapplication.ui.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.util.Size
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.example.myapplication.*
import com.example.myapplication.FileReader
import com.example.myapplication.model.FaceNetModel
import com.example.myapplication.model.Models
import com.example.myapplication.ui.dialog.AddClassDialog
import com.example.myapplication.ui.dialog.AddStudentDialog
import com.example.myapplication.ui.dialog.ScanResultDialog
import com.google.common.util.concurrent.ListenableFuture
import java.io.*
import java.util.concurrent.Executors


class FaceRecognitionActivity : AppCompatActivity() {

    private var isSerializedDataStored = false

    // Serialized data will be stored ( in app's private storage ) with this filename.
    private val SERIALIZED_DATA_FILENAME = "image_data"

    // Shared Pref key to check if the data was stored.
    private val SHARED_PREF_IS_DATA_STORED_KEY = "is_data_stored"

    private lateinit var previewView: PreviewView
    private lateinit var frameAnalyser: FrameAnalyser
    private lateinit var faceNetModel: FaceNetModel
    private lateinit var fileReader: FileReader
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var capture: Button;
    private lateinit var className: String;
    private lateinit var classID: String;
    private lateinit var subject: String;
//    private lateinit var captureStudent: ArrayList<CaptureStudent>;

    // <----------------------- User controls --------------------------->

    // Use the device's GPU to perform faster computations.
    // Refer https://www.tensorflow.org/lite/performance/gpu
    private val useGpu = true

    // Use XNNPack to accelerate inference.
    // Refer https://blog.tensorflow.org/2020/07/accelerating-tensorflow-lite-xnnpack-integration.html
    private val useXNNPack = true

    // You may the change the models here.
    // Use the model configs in Models.kt
    // Default is Models.FACENET ; Quantized models are faster
    private val modelInfo = Models.FACENET

    // <---------------------------------------------------------------->

    companion object {

        lateinit var logTextView: TextView
        val faceRecognitionActivity = FaceRecognitionActivity()
        fun scanRecognized() = faceRecognitionActivity.scanRecognized()
        var studentName = "";
        var getTime = "";
        var class_name = "";
        var class_id = "";
        var subject_name = "";
        var fragment_manager: FragmentManager? = null;

        fun setMessage(message: String) {
            logTextView.text = message
        }

        fun listAttendance(
            student_name: String,
            time: String,
            className: String,
            classID: String,
            subjectName: String,
            fragmentManager: FragmentManager
        ) {
            //check kung naa na ang name sa student sa array.
            //ibutang sa mutable list ang attendance tapos i access sa baba unya
            //himoog list para ma distinct by. kung dili proceed ta atong o click
            //ang button para ibutang sa list!
            studentName = "";
            getTime = "";
            class_name = "";
            class_id = "";
            subject_name = "";
            fragment_manager = null;

            if (student_name != "") {
                studentName = student_name;
                getTime = time
                class_name = className
                class_id = classID
                subject_name = subjectName
                fragment_manager = fragmentManager
                scanRecognized()
            } else {
                studentName = "";
                getTime = "";
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Remove the status bar to have a full screen experience
        // See this answer on SO -> https://stackoverflow.com/a/68152688/10878733
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController!!
                .hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        }
        setContentView(R.layout.face_recogntion)

        // Implementation of CameraX preview

        capture = findViewById(R.id.button_capture)
        previewView = findViewById(R.id.preview_view)
        logTextView = findViewById(R.id.log_textview)
        logTextView.movementMethod = ScrollingMovementMethod()
        // Necessary to keep the Overlay above the Preview View so that the boxes are visible.
        val boundingBoxOverlay = findViewById<BoundingBoxOverlay>(R.id.bbox_overlay)
        boundingBoxOverlay.setWillNotDraw(false)
        boundingBoxOverlay.setZOrderOnTop(true)

        val bundle = intent.extras
        className = bundle!!.getString("CLASS NAME")!!
        classID = bundle!!.getString("CLASS ID")!!
        subject = bundle!!.getString("SUBJECT")!!
        faceNetModel = FaceNetModel(this, modelInfo, useGpu, useXNNPack)
        frameAnalyser = FrameAnalyser(
            this,
            boundingBoxOverlay,
            faceNetModel,
            className,
            classID,
            subject,
            supportFragmentManager
        )
        fileReader = FileReader(faceNetModel)

        // We'll only require the CAMERA permission from the user.
        // For scoped storage, particularly for accessing documents, we won't require WRITE_EXTERNAL_STORAGE or
        // READ_EXTERNAL_STORAGE permissions. See https://developer.android.com/training/data-storage
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermission()
        } else {
            startCameraPreview()
        }

        sharedPreferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE)
        isSerializedDataStored = sharedPreferences.getBoolean(SHARED_PREF_IS_DATA_STORED_KEY, false)
        if (!isSerializedDataStored) {
            Logger.log("No serialized data was found. Select the images directory.")
            showSelectDirectoryDialog()
        } else {
            val alertDialog = AlertDialog.Builder(this).apply {
                setTitle("Serialized Data")
                setMessage("Existing image data was found on this device. Would you like to load it?")
                setCancelable(false)
                setNegativeButton("LOAD") { dialog, which ->
                    dialog.dismiss()
                    frameAnalyser.faceList = loadSerializedImageData()
                    Logger.log("Serialized data loaded.")
                }
                setPositiveButton("RESCAN") { dialog, which ->
                    dialog.dismiss()
                    launchChooseDirectoryIntent()
                }
                create()
            }
            alertDialog.show()
        }

//        captureStudent = ArrayList<CaptureStudent>();

        capture.setOnClickListener {

            if (FaceRecognitionActivity.studentName != "" && FaceRecognitionActivity.studentName != "Unknown") {
                val bundleForScan = Bundle()
                bundleForScan.putString("SCAN RESULT", "successful")
                bundleForScan.putString("CLASS NAME", bundle!!.getString("CLASS NAME"))
                bundleForScan.putString("CLASS ID", bundle!!.getString("CLASS ID"))
                bundleForScan.putString("SUBJECT", bundle!!.getString("SUBJECT"))
                bundleForScan.putString("STUDENT NAME", FaceRecognitionActivity.studentName)
                bundleForScan.putString("TIME", FaceRecognitionActivity.getTime)
                val scanResultDialog = ScanResultDialog()
                scanResultDialog.arguments = bundleForScan;
                scanResultDialog.show(supportFragmentManager, "add")
            } else if (FaceRecognitionActivity.studentName == "Unknown") {
                val bundleForScan = Bundle()
                bundleForScan.putString("SCAN RESULT", "failed")
                val scanResultDialog = ScanResultDialog()
                scanResultDialog.arguments = bundleForScan;
                scanResultDialog.show(supportFragmentManager, "add")
            } else {
                Toast.makeText(applicationContext, "No Face Detected!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    // ---------------------------------------------- //

    // Attach the camera stream to the PreviewView.
    private fun startCameraPreview() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                bindPreview(cameraProvider)
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview: Preview = Preview.Builder().build()
        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        val imageFrameAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(480, 640))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageFrameAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), frameAnalyser)
        cameraProvider.bindToLifecycle(
            this as LifecycleOwner,
            cameraSelector,
            preview,
            imageFrameAnalysis
        )
    }

    // We let the system handle the requestCode. This doesn't require onRequestPermissionsResult and
    // hence makes the code cleaner.
    // See the official docs -> https://developer.android.com/training/permissions/requesting#request-permission
    private fun requestCameraPermission() {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCameraPreview()
            } else {
                val alertDialog = AlertDialog.Builder(this).apply {
                    setTitle("Camera Permission")
                    setMessage("The app couldn't function without the camera permission.")
                    setCancelable(false)
                    setPositiveButton("ALLOW") { dialog, which ->
                        dialog.dismiss()
                        requestCameraPermission()
                    }
                    setNegativeButton("CLOSE") { dialog, which ->
                        dialog.dismiss()
                        finish()
                    }
                    create()
                }
                alertDialog.show()
            }

        }


    // ---------------------------------------------- //


    // Open File chooser to choose the images directory.
    private fun showSelectDirectoryDialog() {
        val alertDialog = AlertDialog.Builder(this).apply {
            setTitle("Select Images Directory")
            setMessage("Please select a directory which contains the images.")
            setCancelable(false)
            setPositiveButton("SELECT") { dialog, which ->
                dialog.dismiss()
                launchChooseDirectoryIntent()
            }
            create()
        }
        alertDialog.show()
    }


    private fun launchChooseDirectoryIntent() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        // startForActivityResult is deprecated.
        // See this SO thread -> https://stackoverflow.com/questions/62671106/onactivityresult-method-is-deprecated-what-is-the-alternative
        directoryAccessLauncher.launch(intent)
    }


    // Read the contents of the select directory here.
    // The system handles the request code here as well.
    // See this SO question -> https://stackoverflow.com/questions/47941357/how-to-access-files-in-a-directory-given-a-content-uri
    private val directoryAccessLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val dirUri = it.data?.data ?: return@registerForActivityResult
            val childrenUri =
                DocumentsContract.buildChildDocumentsUriUsingTree(
                    dirUri,
                    DocumentsContract.getTreeDocumentId(dirUri)
                )
            val tree = DocumentFile.fromTreeUri(this, childrenUri)
            val images = ArrayList<Pair<String, Bitmap>>()
            var errorFound = false
            if (tree!!.listFiles().isNotEmpty()) {
                for (doc in tree.listFiles()) {
                    if (doc.isDirectory && !errorFound) {
                        val name = doc.name!!
                        for (imageDocFile in doc.listFiles()) {
                            try {
                                images.add(Pair(name, getFixedBitmap(imageDocFile.uri)))
                            } catch (e: Exception) {
                                errorFound = true
                                Logger.log(
                                    "Could not parse an image in $name directory. Make sure that the file structure is " +
                                            "as described in the README of the project and then restart the app."
                                )
                                break
                            }
                        }
                        Logger.log("Found ${doc.listFiles().size} images in $name directory")
                    } else {
                        errorFound = true
                        Logger.log(
                            "The selected folder should contain only directories. Make sure that the file structure is " +
                                    "as described in the README of the project and then restart the app."
                        )
                    }
                }
            } else {
                errorFound = true
                Logger.log(
                    "The selected folder doesn't contain any directories. Make sure that the file structure is " +
                            "as described in the README of the project and then restart the app."
                )
            }
            if (!errorFound) {
                fileReader.run(images, fileReaderCallback)
                Logger.log("Detecting faces in ${images.size} images ...")
            } else {
                val alertDialog = AlertDialog.Builder(this).apply {
                    setTitle("Error while parsing directory")
                    setMessage(
                        "There were some errors while parsing the directory. Please see the log below. Make sure that the file structure is " +
                                "as described in the README of the project and then tap RESELECT"
                    )
                    setCancelable(false)
                    setPositiveButton("RESELECT") { dialog, which ->
                        dialog.dismiss()
                        launchChooseDirectoryIntent()
                    }
                    setNegativeButton("CANCEL") { dialog, which ->
                        dialog.dismiss()
                        finish()
                    }
                    create()
                }
                alertDialog.show()
            }
        }


    // Get the image as a Bitmap from given Uri and fix the rotation using the Exif interface
    // Source -> https://stackoverflow.com/questions/14066038/why-does-an-image-captured-using-camera-intent-gets-rotated-on-some-devices-on-a
    private fun getFixedBitmap(imageFileUri: Uri): Bitmap {
        var imageBitmap = BitmapUtils.getBitmapFromUri(contentResolver, imageFileUri)
        val exifInterface = ExifInterface(contentResolver.openInputStream(imageFileUri)!!)
        imageBitmap =
            when (exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> BitmapUtils.rotateBitmap(imageBitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> BitmapUtils.rotateBitmap(imageBitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> BitmapUtils.rotateBitmap(imageBitmap, 270f)
                else -> imageBitmap
            }
        return imageBitmap
    }


    // ---------------------------------------------- //


    private val fileReaderCallback = object : FileReader.ProcessCallback {
        override fun onProcessCompleted(
            data: ArrayList<Pair<String, FloatArray>>,
            numImagesWithNoFaces: Int
        ) {
            frameAnalyser.faceList = data
            saveSerializedImageData(data)
            Logger.log("Images parsed. Found $numImagesWithNoFaces images with no faces.")
        }
    }


    private fun saveSerializedImageData(data: ArrayList<Pair<String, FloatArray>>) {
        val serializedDataFile = File(filesDir, SERIALIZED_DATA_FILENAME)
        ObjectOutputStream(FileOutputStream(serializedDataFile)).apply {
            writeObject(data)
            flush()
            close()
        }
        sharedPreferences.edit().putBoolean(SHARED_PREF_IS_DATA_STORED_KEY, true).apply()
    }


    private fun loadSerializedImageData(): ArrayList<Pair<String, FloatArray>> {
        val serializedDataFile = File(filesDir, SERIALIZED_DATA_FILENAME)
        val objectInputStream = ObjectInputStream(FileInputStream(serializedDataFile))
        val data = objectInputStream.readObject() as ArrayList<Pair<String, FloatArray>>
        objectInputStream.close()
        return data
    }

    fun scanRecognized() {
        val scanResultDialog = ScanResultDialog()
        if (!scanResultDialog.dialogIsShown)
            if (FaceRecognitionActivity.studentName != "" && FaceRecognitionActivity.studentName != "Unknown") {
                Log.d(
                    "scanRecognized",
                    FaceRecognitionActivity.studentName + " " + FaceRecognitionActivity.getTime
                )
                val bundleForScan = Bundle()
                bundleForScan.putString("SCAN RESULT", "successful")
                bundleForScan.putString("CLASS NAME", FaceRecognitionActivity.class_name)
                bundleForScan.putString("CLASS ID", FaceRecognitionActivity.class_id)
                bundleForScan.putString("SUBJECT", FaceRecognitionActivity.subject_name)
                bundleForScan.putString("STUDENT NAME", FaceRecognitionActivity.studentName)
                bundleForScan.putString("TIME", FaceRecognitionActivity.getTime)
                scanResultDialog.arguments = bundleForScan;
                FaceRecognitionActivity.fragment_manager?.let { scanResultDialog.show(it, "add") }
            }
    }
}