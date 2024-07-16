package com.mahek.imagepicker


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mahek.imagepicker.utils.FilePaths
import com.mahek.imagepicker.utils.FilesUtilities
import com.mahek.imagepicker.utils.TYPES
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class MahekMediaPicker(
    private val registry: ActivityResultRegistry,
    private val context: Context,
    private val requiresCrop: Boolean = false,
    private val aspectRatioX: Float = 1f,
    private val aspectRatioY: Float = 1f,
) : DefaultLifecycleObserver {

    companion object {
        private const val IMAGE_PICKER_LAUNCHER = "ImagePickerLauncher"
        private const val IMAGE_CAPTURE_LAUNCHER = "ImageCaptureLauncher"
        private const val VIDEO_PICKER_LAUNCHER = "VideoPickerLauncher"
        private const val VIDEO_CAPTURE_LAUNCHER = "VideoCaptureLauncher"
        private const val CROP_LAUNCHER = "CropLauncher"

        private const val PERMISSION_REQUEST = 1101
        private const val IMAGE_PREFIX = "img_"
        private const val VIDEO_PREFIX = "video_"

        private const val TAG = "MahekMediaPicker"
    }

    private lateinit var imagePickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var imageCaptureLauncher: ActivityResultLauncher<Uri>
    private lateinit var videoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var videRecordLauncher: ActivityResultLauncher<Uri>
    private lateinit var cropActivityLauncher: ActivityResultLauncher<Intent>


    private var imageUri: Uri? = null
    private var cameraPickerUri: Uri? = null
    private var videoRecordUri: Uri? = null
    private var mediaType: MediaType = MediaType.IMAGE_AND_VIDEO
    private var actionType: SourceType = SourceType.CAMERA_AND_GALLERY

    lateinit var onMediaChoose: (pathList: ArrayList<String>, type: MediaType) -> Unit

    private val filePaths by lazy {
        FilePaths(context)
    }

    private val permissions by lazy {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.CAMERA)
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
            }

            else -> {
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }


    enum class SourceType(i: Int) {
        CAMERA(1), GALLERY(2), CAMERA_AND_GALLERY(3)
    }

    enum class MediaType(i: Int) {
        IMAGE(1), VIDEO(2), IMAGE_AND_VIDEO(3)
    }

    override fun onCreate(owner: LifecycleOwner) {
        imagePickerLauncher = registry.register(IMAGE_PICKER_LAUNCHER, owner, ActivityResultContracts.PickVisualMedia()) {
            it?.let { uri ->
                handleImageRequest(uri)
            }
        }
        imageCaptureLauncher = registry.register(IMAGE_CAPTURE_LAUNCHER, owner, ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                handleImageRequest(cameraPickerUri)
            }
        }
        videoPickerLauncher = registry.register(VIDEO_PICKER_LAUNCHER, owner, ActivityResultContracts.PickVisualMedia()) {
            it?.let { uri ->
                val videoPath = FilesUtilities.getFilePath(uri, context) ?: ""
                if (videoPath.isNotEmpty()) {
                    onMediaChoose(arrayListOf(videoPath), MediaType.VIDEO)
                }
            }
        }
        videRecordLauncher = registry.register(VIDEO_CAPTURE_LAUNCHER, owner, ActivityResultContracts.CaptureVideo()) { success: Boolean ->
            if (success) {
                videoRecordUri?.let { uri ->
                    val videoPath = FilesUtilities.getFilePath(uri, context) ?: ""
                    if (videoPath.isNotEmpty()) {
                        onMediaChoose(arrayListOf(videoPath), MediaType.VIDEO)
                    }
                }
            }
        }

        cropActivityLauncher = registry.register(CROP_LAUNCHER, owner, ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                if (data != null) {
                    val resultUri = UCrop.getOutput(data)
                    val imagePath = resultUri?.path ?: ""

                    onMediaChoose(arrayListOf(imagePath), MediaType.IMAGE)
                }
            }
        }
    }

    /**
     * source type
     *  - Camera
     *  - Gallery
     *  - Camera & Gallery
     *
     *  Media Type
     *  - Image
     *  - Video
     *  - Both
     *
     * */

    fun startMediaPicker(
        mediaType: MediaType = MediaType.IMAGE_AND_VIDEO,
        sourceType: SourceType = SourceType.CAMERA_AND_GALLERY,
        onMediaChooseMultiple: (pathList: ArrayList<String>, type: MediaType) -> Unit,
    ) {
        this.mediaType = mediaType
        this.actionType = sourceType
        this.onMediaChoose = onMediaChooseMultiple

        if (isPermissionsAllowed(permissions)) {
            when (sourceType) {
                SourceType.CAMERA -> {
                    when (mediaType) {
                        MediaType.IMAGE -> { // image capture
                            chooseImage(SourceType.CAMERA)
                        }

                        MediaType.VIDEO -> { //video capture
                            chooseVideo(SourceType.CAMERA)
                        }

                        MediaType.IMAGE_AND_VIDEO -> { // image & video capture
                            selectMediaDialog()
                        }
                    }
                }

                SourceType.GALLERY -> {
                    when (mediaType) {
                        MediaType.IMAGE -> { // select image from gallery
                            chooseImage(SourceType.GALLERY)
                        }

                        MediaType.VIDEO -> {// select video from gallery
                            chooseVideo(SourceType.GALLERY)
                        }

                        MediaType.IMAGE_AND_VIDEO -> {// select image & video from gallery
                            selectMediaDialog()
                        }
                    }
                }

                SourceType.CAMERA_AND_GALLERY -> {
                    when (mediaType) {
                        MediaType.IMAGE -> {
                            selectSourceTypeDialog(MediaType.IMAGE)
                        }

                        MediaType.VIDEO -> {
                            selectSourceTypeDialog(MediaType.VIDEO)
                        }

                        MediaType.IMAGE_AND_VIDEO -> {
                            selectMediaDialog()
                        }
                    }
                }
            }
        }
    }

    /**
     * select media type
     * - Image
     * - Video
     * */
    private fun selectMediaDialog() {
        val context = this.context ?: return
        val bottomSheetDialog = BottomSheetDialog(context)

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = inflater.inflate(R.layout.dialog_media_picker, null)
        bottomSheetDialog.setContentView(view)
        val llImageCamera = view.findViewById<LinearLayout>(R.id.llImageCamera)
        val llVideoCamera = view.findViewById<LinearLayout>(R.id.llVideoCamera)

        llImageCamera.setOnClickListener {//image
            bottomSheetDialog.dismiss()
            selectSourceTypeDialog(MediaType.IMAGE)
        }
        llVideoCamera.setOnClickListener {//video
            selectSourceTypeDialog(MediaType.VIDEO)
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.show()
    }

    /**
     * Select source type
     * - Camera
     * - Gallery
     * */
    private fun selectSourceTypeDialog(mediaType: MediaType) {

        val bottomSheetDialog = BottomSheetDialog(context)

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = inflater.inflate(R.layout.dialog_gallery_picker, null)
        bottomSheetDialog.setContentView(view)
        val llCamera = view.findViewById<LinearLayout>(R.id.llCamera)
        val llGallery = view.findViewById<LinearLayout>(R.id.llGallery)

        llCamera.setOnClickListener {
            if (mediaType == MediaType.IMAGE) {
                chooseImage(SourceType.CAMERA)
            } else if (mediaType == MediaType.VIDEO) {
                chooseVideo(SourceType.CAMERA)
            }
            bottomSheetDialog.dismiss()
        }
        llGallery.setOnClickListener {
            if (mediaType == MediaType.IMAGE) {
                chooseImage(SourceType.GALLERY)
            } else if (mediaType == MediaType.VIDEO) {
                chooseVideo(SourceType.GALLERY)
            }
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.show()
    }

    private fun chooseImage(sourceType: SourceType) {
        if (sourceType == SourceType.CAMERA) {
            val name = IMAGE_PREFIX + System.currentTimeMillis() + ".jpg"
            val sdImageMainDirectory = File(context.externalCacheDir, name)
            cameraPickerUri = FileProvider.getUriForFile(context, context.packageName + context.getString(R.string.provider), sdImageMainDirectory)
            if (cameraPickerUri != null) {
                imageCaptureLauncher.launch(cameraPickerUri!!)
            }
        } else {
            imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    private fun chooseVideo(sourceType: SourceType) {
        if (sourceType == SourceType.CAMERA) {
            val name = VIDEO_PREFIX + System.currentTimeMillis() + ".mp4"
            val externalFilesDir = context.getExternalFilesDir(null)
            if (externalFilesDir != null && externalFilesDir.exists()) {
                val sdVideoMainDirectory = File(externalFilesDir, name)
                videoRecordUri = FileProvider.getUriForFile(context, context.packageName + context.getString(R.string.provider), sdVideoMainDirectory)
                if (videoRecordUri != null) {
                    grantUriPermissions(videoRecordUri!!)
                    videRecordLauncher.launch(videoRecordUri!!)
                }
            } else {
                Log.e(TAG, "the external files directory does not exist")
            }
        } else {
            videoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        }
    }

    private fun grantUriPermissions(uri: Uri) {
        val intentFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.packageManager.queryIntentActivities(Intent(MediaStore.ACTION_VIDEO_CAPTURE), PackageManager.MATCH_DEFAULT_ONLY).forEach { resolveInfo ->
            context.grantUriPermission(resolveInfo.activityInfo.packageName, uri, intentFlags)
        }
    }

    /**
     * Check if scoped permission is allowed or not
     */
    private fun isPermissionsAllowed(permissions: Array<String>): Boolean {

        val pendingPermissions = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_DENIED
        }

        return if (pendingPermissions.isNotEmpty()) {
            requestRequiredPermissions(pendingPermissions.toTypedArray())
            false
        } else {
            true
        }
    }


    private fun requestRequiredPermissions(permissions: Array<String>) {
        val pendingPermissions: ArrayList<String> = ArrayList()
        permissions.forEachIndexed { _, permission ->
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_DENIED) pendingPermissions.add(permission)
        }
        val array = arrayOfNulls<String>(pendingPermissions.size)
        pendingPermissions.toArray(array)
        (context as Activity).requestPermissions(array, PERMISSION_REQUEST)
    }

    private fun handleImageRequest(uri: Uri?) {
        val imagePath = FilesUtilities.getFilePath(uri, context) ?: ""
        if (imagePath.isNotEmpty()) imageUri = Uri.fromFile(File(imagePath))
        imageUri?.let { imageUri ->
            val exceptionHandler = CoroutineExceptionHandler { _, t ->
                t.printStackTrace()
                Log.e(TAG, "${t.printStackTrace()} \n ${t.message}")
            }
            GlobalScope.launch(Dispatchers.Main + exceptionHandler) {
                val compressedPath = FilesUtilities.from(context.applicationContext, imageUri).absolutePath
                if (requiresCrop) {
                    val destinationFile = File(filePaths.getLocalDirectory(type = TYPES.LOCAL_CACHE_DIRECTORY)?.path + "/" + File(compressedPath).name)
                    withContext(Dispatchers.IO) {
                        destinationFile.createNewFile()
                    }

                    //Cropping
                    val options = UCrop.Options()
                    options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.NONE, UCropActivity.SCALE)
                    options.setToolbarColor(ContextCompat.getColor(context, R.color.color_golden))
                    options.setStatusBarColor(ContextCompat.getColor(context, R.color.crop_statusbar_color))
                    options.setToolbarWidgetColor(ContextCompat.getColor(context, R.color.crop_toolbar_color))
                    options.setToolbarTitle(context.getString(R.string.crop_image))
                    options.setHideBottomControls(true)

                    val uCropIntent = UCrop.of(imageUri, Uri.fromFile(destinationFile)).withOptions(options).withAspectRatio(aspectRatioX, aspectRatioY).getIntent(context)
                    cropActivityLauncher.launch(uCropIntent)
                } else {
                    onMediaChoose(arrayListOf(compressedPath), MediaType.IMAGE)
                }
            }
        }
    }

    fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray,
        sourceType: SourceType = SourceType.CAMERA_AND_GALLERY,
    ) {
        when (requestCode) {
            PERMISSION_REQUEST -> {
                if (isAllPermissionsGranted(grantResults)) {
                    when (sourceType) {
                        SourceType.CAMERA -> {
                            when (mediaType) {
                                MediaType.IMAGE -> { // image capture
                                    chooseImage(SourceType.CAMERA)
                                }

                                MediaType.VIDEO -> { //video capture
                                    chooseVideo(SourceType.CAMERA)
                                }

                                MediaType.IMAGE_AND_VIDEO -> { // image & video capture
                                    selectMediaDialog()
                                }
                            }
                        }

                        SourceType.GALLERY -> {
                            when (mediaType) {
                                MediaType.IMAGE -> { // select image from gallery
                                    chooseImage(SourceType.GALLERY)
                                }

                                MediaType.VIDEO -> {// select video from gallery
                                    chooseVideo(SourceType.GALLERY)
                                }

                                MediaType.IMAGE_AND_VIDEO -> {// select image & video from gallery
                                    selectMediaDialog()
                                }
                            }
                        }

                        SourceType.CAMERA_AND_GALLERY -> {
                            when (mediaType) {
                                MediaType.IMAGE -> {
                                    selectSourceTypeDialog(MediaType.IMAGE)
                                }

                                MediaType.VIDEO -> {
                                    selectSourceTypeDialog(MediaType.VIDEO)
                                }

                                MediaType.IMAGE_AND_VIDEO -> {
                                    selectMediaDialog()
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(context, "Permission not granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun isAllPermissionsGranted(grantResults: IntArray): Boolean {
        var isGranted = true
        for (grantResult in grantResults) {
            isGranted = grantResult == PackageManager.PERMISSION_GRANTED
            if (!isGranted) break
        }
        return isGranted
    }

}