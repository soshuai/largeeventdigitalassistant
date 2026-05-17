# Large Event Digital Assistant

This is a feature-complete Android project that includes the following core modules:

## Features

### 1. Network Request Module
- Implemented with Retrofit and OkHttp
- Supports GET/POST HTTP requests
- Supports request interceptors and logging

### 2. Image Loading Module
- Implemented with Glide
- Supports loading normal images, circular images, and rounded corner images
- Supports image cache management

### 3. File Download Module
- Supports file download with progress bar
- Background download service
- Progress display in notification bar

### 4. Camera Module
- Call system camera to take photos
- Select images from album
- Image path management

### 5. File Upload Module
- Supports file upload with progress bar
- Supports multiple file upload
- Upload status callback

### 6. NFC Recognition Module
- Automatic NFC tag detection
- Read NFC chip ID
- Supports multiple NFC technology standards

## Project Structure

```
app/src/main/java/com/largeevent/management/
├── MainActivity.java              # Main interface
├── network/                      # Network request module
│   ├── NetworkManager.java       # Network manager
│   └── NetworkCallback.java      # Network callback interface
├── image/                        # Image loading module
│   └── ImageLoader.java          # Image loading utility class
├── download/                     # File download module
│   ├── DownloadManager.java      # Download manager
│   └── DownloadListener.java     # Download callback interface
├── camera/                       # Camera module
│   ├── CameraHelper.java         # Camera utility class
│   └── CameraCallback.java       # Camera callback interface
├── upload/                       # File upload module
│   ├── UploadManager.java        # Upload manager
│   ├── ProgressRequestBody.java  # Request body with progress
│   └── UploadListener.java       # Upload callback interface
└── nfc/                          # NFC recognition module
    ├── NfcManager.java           # NFC manager
    └── NfcCallback.java          # NFC callback interface
```

## Permissions

The project has configured the following permissions:
- Network permissions (INTERNET, ACCESS_NETWORK_STATE)
- Storage permissions (READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE)
- Camera permission (CAMERA)
- NFC permission (NFC)

## Usage Instructions

1. Clone the project to Android Studio
2. Sync Gradle dependencies
3. Compile and run the project

## Notes

1. In actual use, you need to replace the sample URLs with real server addresses
2. NFC functionality needs to be tested on real devices
3. Some features require network connectivity to work properly# largeeventdigitalassistant
