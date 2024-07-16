package com.mahek.imagepicker.utils

import android.content.Context
import android.os.Environment
import java.io.File

enum class TYPES {
    LOCAL_FILE_DIRECTORY,
    LOCAL_CACHE_DIRECTORY,

    PUBLIC_OBB_DIRECTORY,
    PUBLIC_CACHE_DIRECTORY,
    PUBLIC_MEDIA_DIRECTORY,
    PUBLIC_IMAGE_DIRECTORY,
    PUBLIC_VIDEO_DIRECTORY,
    PUBLIC_AUDIO_DIRECTORY,
    PUBLIC_PDF_DIRECTORY,
    PUBLIC_OTHER_DIRECTORY,

    GENERAL_PUBLIC_DIRECTORY,
    GENERAL_PUBLIC_DOWNLOAD_DIRECTORY
}

class FilePaths(private val context: Context) {

    fun getLocalDirectory(type: TYPES = TYPES.LOCAL_FILE_DIRECTORY): File? {
        val folder = when (type) {

            TYPES.LOCAL_FILE_DIRECTORY -> {
//                    /data/user/0/com.hb.imagepicker.utils/files
                context.filesDir
            }
            TYPES.LOCAL_CACHE_DIRECTORY -> {
//                    /data/user/0/com.hb.imagepicker.utils/cache
                context.cacheDir
            }

            TYPES.PUBLIC_OBB_DIRECTORY -> {
//                    /storage/emulated/0/Android/obb/com.hb.imagepicker.utils
                context.obbDir
            }
            TYPES.PUBLIC_CACHE_DIRECTORY -> {
//                    /storage/emulated/0/Android/data/com.hb.imagepicker.utils/cache
                context.externalCacheDir
            }
            TYPES.PUBLIC_MEDIA_DIRECTORY -> {
//                    /storage/emulated/0/Android/media/com.hb.imagepicker.utils
                if (context.externalMediaDirs?.isEmpty() == false) context.filesDir else context.externalMediaDirs.first()
            }

            TYPES.PUBLIC_IMAGE_DIRECTORY -> {
                //                    /storage/emulated/0/Android/data/com.hb.imagepicker.utils/files/Image
                context.getExternalFilesDir("Image")
            }
            TYPES.PUBLIC_VIDEO_DIRECTORY -> {
                //                    /storage/emulated/0/Android/data/com.hb.imagepicker.utils/files/Video
                context.getExternalFilesDir("Video")
            }
            TYPES.PUBLIC_AUDIO_DIRECTORY -> {
                //                    /storage/emulated/0/Android/data/com.hb.imagepicker.utils/files/Audio
                context.getExternalFilesDir("Audio")
            }
            TYPES.PUBLIC_PDF_DIRECTORY -> {
                //                    /storage/emulated/0/Android/data/com.hb.imagepicker.utils/files/Pdf
                context.getExternalFilesDir("Pdf")
            }
            TYPES.PUBLIC_OTHER_DIRECTORY -> {
                //                    /storage/emulated/0/Android/data/com.hb.imagepicker.utils/files/Other
                context.getExternalFilesDir("Other")
            }

            TYPES.GENERAL_PUBLIC_DIRECTORY -> {
                //                    /storage/emulated/0
                Environment.getExternalStorageDirectory()
            }
            TYPES.GENERAL_PUBLIC_DOWNLOAD_DIRECTORY -> {
                //                    /storage/emulated/0/Download
                Environment.getExternalStoragePublicDirectory("Download")
            }
            else -> {
                Environment.getExternalStoragePublicDirectory("Download")
            }
        }
        if (folder != null && !folder.exists()) {
            folder.mkdirs()
        }
        return folder
    }
}