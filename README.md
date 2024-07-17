# Media Picker for Android

Media Picker is an easy-to-use library for Android that simplifies the process of selecting images and videos from the device's gallery or capturing them using the camera. It provides options for cropping images and handles permissions seamlessly.

## Features

- **Image and Video Selection**: Choose between images, videos, or both.
- **Image Cropping**: Crop selected images with customizable aspect ratios.
- **Camera Integration**: Capture images and videos directly from the device's camera.
- **Permission Handling**: Automatically handles runtime permissions for accessing media.
- **Callback Support**: Provides callback mechanisms to receive selected media paths.

## Usage

### Initialization

```kotlin
// Initialize MahekMediaPicker in your activity or fragment
val mahekMediaPicker = MahekMediaPicker(activityResultRegistry, context)
```

### Starting Media Picker
```kotlin
// Start media picker to choose images and/or videos
mahekMediaPicker.startMediaPicker(MediaType.IMAGE_AND_VIDEO, SourceType.CAMERA_AND_GALLERY) { pathList, mediaType ->
    // Handle selected media paths here
    // pathList contains paths of selected media files
    // mediaType specifies whether image, video, or both were selected
}
```

### Permissions

Ensure that you request necessary permissions before using the media picker:
```kotlin

// Handle permissions in your activity or fragment
override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    mahekMediaPicker.onRequestPermissionsResult(requestCode, permissions, grantResults)
}
```


### Customization

You can customize aspects like cropping behavior and aspect ratio by modifying the MahekMediaPicker instance.



