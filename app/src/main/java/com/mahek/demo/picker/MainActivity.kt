package com.mahek.demo.picker

import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.mahek.imagepicker.MahekMediaPicker


class MainActivity : AppCompatActivity() {

    private val mahekMediaPicker by lazy {
        MahekMediaPicker(registry = activityResultRegistry, this@MainActivity, requiresCrop = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(mahekMediaPicker)

        setContentView(R.layout.activity_main)
        initUI()
    }

    private fun initUI() {
        findViewById<Button>(R.id.selectImgBtn).clickWithDelay {
            mahekMediaPicker.startMediaPicker(mediaType = MahekMediaPicker.MediaType.IMAGE) { pathList, type ->
                if (type == MahekMediaPicker.MediaType.IMAGE) {
                    val path = pathList.firstOrNull()
                    findViewById<ImageView>(R.id.imageView).setImageURI(Uri.parse(path))
                }
            }
        }


        findViewById<Button>(R.id.selectVideoBtn).clickWithDelay {
            mahekMediaPicker.startMediaPicker(mediaType = MahekMediaPicker.MediaType.VIDEO) { pathList, type ->
                if (type == MahekMediaPicker.MediaType.VIDEO) {
                    val path = pathList.firstOrNull()

                    findViewById<VideoView>(R.id.videoView).also {
                        it.setMediaController(MediaController(this))
                        it.setVideoPath(path)
                        it.start()
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mahekMediaPicker.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.e("scopedMediaPicker----", "onRequestPermissionsResult: ${permissions.toString()}")
    }
}

fun View.clickWithDelay(delayTime: Long = 700L, action: () -> Unit) {
    val onClickListener = object : View.OnClickListener {
        private var lastClickTime: Long = 0
        override fun onClick(v: View?) {
            val currentTime = SystemClock.elapsedRealtime()
            if (currentTime - lastClickTime < delayTime) {
                return
            }
            lastClickTime = currentTime
            action()
        }
    }
    this.setOnClickListener(onClickListener)
}