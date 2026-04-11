package com.example.foodtok.util;

import android.content.Context;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;

import java.io.File;

/**
 * Process-wide video cache. A single {@link SimpleCache} backs every
 * ExoPlayer instance in the feed so that re-watching a video during
 * the same session is served from disk instead of the network. Sized
 * at 200 MB with an LRU evictor — tuned for a ~20-item feed where
 * clips average 5–10 MB.
 */
@OptIn(markerClass = UnstableApi.class)
public final class VideoCache {

  private static final long MAX_CACHE_BYTES = 200L * 1024L * 1024L;
  private static final String CACHE_DIR = "feed_video_cache";

  private static SimpleCache cache;
  private static CacheDataSource.Factory cacheFactory;

  private VideoCache() {
  }

  /**
   * Returns a {@link CacheDataSource.Factory} wrapping the shared disk
   * cache. Pass this to {@code DefaultMediaSourceFactory} when building
   * an ExoPlayer so reads/writes go through the cache.
   */
  public static synchronized CacheDataSource.Factory getCacheFactory(
      Context context) {
    if (cacheFactory == null) {
      SimpleCache simpleCache = getCache(context);
      DefaultHttpDataSource.Factory upstream =
          new DefaultHttpDataSource.Factory()
              .setAllowCrossProtocolRedirects(true);
      cacheFactory = new CacheDataSource.Factory()
          .setCache(simpleCache)
          .setUpstreamDataSourceFactory(upstream)
          .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
    }
    return cacheFactory;
  }

  private static synchronized SimpleCache getCache(Context context) {
    if (cache == null) {
      File cacheDir = new File(
          context.getApplicationContext().getCacheDir(), CACHE_DIR);
      cache = new SimpleCache(
          cacheDir,
          new LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
          new StandaloneDatabaseProvider(
              context.getApplicationContext()));
    }
    return cache;
  }
}