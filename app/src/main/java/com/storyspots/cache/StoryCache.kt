package com.storyspots.cache

import android.content.Context
import android.util.Log
import android.util.LruCache
import com.storyspots.caption.StoryData
import com.storyspots.caption.CachedStoryData
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
                    val cachedStoriesData = input.readObject() as List<CachedStoryData>
                    return@withContext cachedStoriesData.map { it.toStoryData() }
                }
            }
        } catch (e: Exception) {
            Log.e("StoryCache", "Failed to read cache", e)
            try {
                cacheFile.delete()
            } catch (deleteException: Exception) {
                Log.e("StoryCache", "Failed to delete corrupted cache", deleteException)
            }
        }
        emptyList()
    }

    suspend fun cacheStories(stories: List<StoryData>) = withContext(Dispatchers.IO) {
        try {
            val cachedStoriesData = stories.map { it.toCached() }

            ObjectOutputStream(cacheFile.outputStream()).use { output ->
                output.writeObject(cachedStoriesData)
            }

            stories.forEach { story ->
                memoryCache.put(story.id, story)
            }

            Log.d("StoryCache", "Successfully cached ${stories.size} stories")
        } catch (e: Exception) {
            Log.e("StoryCache", "Failed to write cache", e)
        }
    }

    fun clearCache() {
        try {
            cacheFile.delete()
            memoryCache.evictAll()
            Log.d("StoryCache", "Cache cleared")
        } catch (e: Exception) {
            Log.e("StoryCache", "Failed to clear cache", e)
        }
    }
}