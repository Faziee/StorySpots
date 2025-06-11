package com.storyspots.cache

import android.content.Context
import android.util.Log
import android.util.LruCache
import com.storyspots.caption.StoryData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class StoryCache(private val context: Context) {
    private val cacheFile = File(context.cacheDir, "stories_cache.dat")
    private val memoryCacheSize = 50
    private val memoryCache = LruCache<String, StoryData>(memoryCacheSize)

    suspend fun getCachedStories(): List<StoryData> = withContext(Dispatchers.IO) {
        try {
            if (cacheFile.exists()) {
                ObjectInputStream(cacheFile.inputStream()).use { input ->
                    @Suppress("UNCHECKED_CAST")
                    return@withContext input.readObject() as List<StoryData>
                }
            }
        } catch (e: Exception) {
            Log.e("StoryCache", "Failed to read cache", e)
        }
        emptyList()
    }

    suspend fun cacheStories(stories: List<StoryData>) = withContext(Dispatchers.IO) {
        try {
            ObjectOutputStream(cacheFile.outputStream()).use { output ->
                output.writeObject(stories)
            }

            // Also update memory cache
            stories.forEach { story ->
                memoryCache.put(story.id, story)
            }
        } catch (e: Exception) {
            Log.e("StoryCache", "Failed to write cache", e)
        }
    }

    fun getStoryFromMemory(id: String): StoryData? = memoryCache.get(id)
}