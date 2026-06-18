package io.trtc.uikit.videochat.common.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImageResourceCache(private val context: Context) {

    private var defaultAvatarPath: String? = null
    
    companion object {
        private const val CACHE_DIR_AVATARS = "avatars"
        private const val DEFAULT_AVATAR_FILE_NAME = "default_avatar.png"
    }

    fun getDefaultAvatarPath(defaultAvatarResId: Int): String? {
        if (defaultAvatarPath != null) {
            return defaultAvatarPath
        }
        
        return try {
            val cacheDir = getAvatarsCacheDir()
            val defaultAvatarFile = File(cacheDir, DEFAULT_AVATAR_FILE_NAME)
            
            if (defaultAvatarFile.exists()) {
                defaultAvatarPath = defaultAvatarFile.absolutePath
                return defaultAvatarPath
            }
            
            val bitmap = BitmapFactory.decodeResource(context.resources, defaultAvatarResId)
                ?: return null
            
            val absolutePath = saveBitmapToFile(bitmap, defaultAvatarFile)
            if (absolutePath != null) {
                defaultAvatarPath = absolutePath
            }
            absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun cacheNetworkImage(imageUrl: String?, callback: (String?) -> Unit) {
        if (imageUrl.isNullOrEmpty()) {
            callback(null)
            return
        }
        
        Glide.with(context)
            .downloadOnly()
            .load(imageUrl)
            .listener(object : RequestListener<File> {
                override fun onResourceReady(
                    resource: File?,
                    model: Any?,
                    target: Target<File>?,
                    dataSource: com.bumptech.glide.load.DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    callback(resource?.absolutePath)
                    return false
                }
                
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<File>?,
                    isFirstResource: Boolean
                ): Boolean {
                    callback(null)
                    return false
                }
            })
            .preload()
    }

    private fun saveBitmapToFile(bitmap: Bitmap, targetFile: File): String? {
        return try {
            FileOutputStream(targetFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
            }
            targetFile.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } finally {
            bitmap.recycle()
        }
    }

    private fun getAvatarsCacheDir(): File {
        val cacheDir = context.cacheDir
        val avatarCacheDir = File(cacheDir, CACHE_DIR_AVATARS)
        if (!avatarCacheDir.exists()) {
            avatarCacheDir.mkdirs()
        }
        return avatarCacheDir
    }
}

