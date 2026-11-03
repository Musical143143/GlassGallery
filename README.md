# Glass Gallery

A Material 3 photo and video gallery with an iOS-style glass interface, built with Jetpack Compose.

## Features
- Photos and videos grouped by day (Today, Yesterday, dates)
- Albums with cover thumbnails, and album detail screens
- Filters (All, Favorites, Videos) and search by file name
- Multi-select with long press: favorite or share several items at once
- Full-screen viewer: swipe, pinch to zoom, double-tap to zoom, tap to hide controls
- Favorites saved on the device
- Share sheet support, and videos open in your default player
- Smooth scrolling: lazy grid with stable keys, thumbnail downsampling, and Coil caching

## Build on your phone with Termux (no PC needed)
The Android SDK does not run in Termux, so GitHub builds the APK for you:

1. Unzip the project in Termux: `pkg install unzip` then `unzip GlassGallery.zip`
2. Create a GitHub repository and push the folder:
   ```bash
   cd GlassGallery
   git init && git add . && git commit -m "Glass Gallery"
   git branch -M main
   git remote add origin https://github.com/YOUR_NAME/glass-gallery.git
   git push -u origin main
   ```
3. Open the repo on GitHub, go to **Actions → Build APK**, and wait for the green check.
4. Download the **GlassGallery-debug** artifact, unzip it, and install the APK. Allow "Install unknown apps" for your browser or file manager when Android asks.

## Notes
- The glass effect uses translucent gradients, rims, and shadows. It is not a live backdrop blur, which keeps scrolling fast on every device.
- Built and tested by Gradle on GitHub Actions. If a build fails, copy the first error from the log and send it back.
