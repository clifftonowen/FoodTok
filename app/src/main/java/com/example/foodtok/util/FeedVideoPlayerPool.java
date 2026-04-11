package com.example.foodtok.util;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import com.example.foodtok.models.Recipe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Three-slot ExoPlayer pool (prev / current / next) that keeps the
 * currently visible feed item playing while the neighbours stay
 * pre-buffered and paused. Rotating the pool on swipe makes the next
 * video start instantly — its buffer is already filled.
 *
 * <p>All players share the disk-backed {@link VideoCache}, so any
 * item outside the 3-slot window still benefits from cached bytes on
 * a scroll-back.
 */
@OptIn(markerClass = UnstableApi.class)
public final class FeedVideoPlayerPool {

  /**
   * Half-window size: we hold players for positions in
   * [current - WINDOW, current + WINDOW], so the live pool caps at
   * {@code 2 * WINDOW + 1}. Keeping neighbours pre-buffered is what
   * makes the next swipe start instantly.
   */
  private static final int WINDOW = 2;
  private static final int MIN_BUFFER_MS = 2_000;
  private static final int MAX_BUFFER_MS = 10_000;
  private static final int BUFFER_FOR_PLAYBACK_MS = 1_000;
  private static final int BUFFER_FOR_REBUFFER_MS = 2_000;

  private final Context appContext;
  private final DefaultMediaSourceFactory mediaSourceFactory;

  /** position -> player, for the (up to) 3 positions currently held. */
  private final Map<Integer, ExoPlayer> playersByPosition = new HashMap<>();

  private List<Recipe> recipes;
  private int currentPosition = -1;

  public FeedVideoPlayerPool(Context context) {
    this.appContext = context.getApplicationContext();
    this.mediaSourceFactory = new DefaultMediaSourceFactory(appContext)
        .setDataSourceFactory(VideoCache.getCacheFactory(appContext));
  }

  public void setRecipes(List<Recipe> recipes) {
    this.recipes = recipes;
  }

  /**
   * Moves the "current" slot to {@code position}. Rotates the pool so
   * the prev/next neighbours stay pre-buffered, evicts players whose
   * position fell out of the window, and plays the current one.
   */
  public void setCurrentPosition(int position) {
    if (recipes == null || position < 0 || position >= recipes.size()) {
      return;
    }
    currentPosition = position;

    // Evict any player whose position is no longer in the window.
    HashMap<Integer, ExoPlayer> next = new HashMap<>();
    for (int k = position - WINDOW; k <= position + WINDOW; k++) {
      if (k >= 0 && k < recipes.size()) {
        ExoPlayer existing = playersByPosition.remove(k);
        if (existing == null) {
          existing = buildPlayerFor(k);
        }
        if (existing != null) {
          next.put(k, existing);
        }
      }
    }
    for (ExoPlayer stale : playersByPosition.values()) {
      stale.release();
    }
    playersByPosition.clear();
    playersByPosition.putAll(next);

    // Play current, pause neighbours at start.
    for (Map.Entry<Integer, ExoPlayer> e : playersByPosition.entrySet()) {
      ExoPlayer p = e.getValue();
      if (e.getKey() == position) {
        p.setPlayWhenReady(true);
      } else {
        p.setPlayWhenReady(false);
        p.seekTo(0);
      }
    }
  }

  /** Attaches the player for {@code position} to a PlayerView. */
  public void attach(int position, PlayerView view) {
    ExoPlayer player = playersByPosition.get(position);
    if (player != null) {
      view.setPlayer(player);
    }
  }

  /** Detaches the player for {@code position} from a PlayerView. */
  public void detach(int position, PlayerView view) {
    if (view.getPlayer() != null
        && playersByPosition.get(position) == view.getPlayer()) {
      view.setPlayer(null);
    }
  }

  /** Pauses the currently playing video (e.g. leaving Home tab). */
  public void pauseCurrent() {
    ExoPlayer player = playersByPosition.get(currentPosition);
    if (player != null) {
      player.setPlayWhenReady(false);
    }
  }

  /** Resumes the currently playing video (e.g. returning to Home). */
  public void resumeCurrent() {
    ExoPlayer player = playersByPosition.get(currentPosition);
    if (player != null) {
      player.setPlayWhenReady(true);
    }
  }

  /** Releases every player in the pool. Call from onDestroyView. */
  public void release() {
    for (ExoPlayer p : playersByPosition.values()) {
      p.release();
    }
    playersByPosition.clear();
    currentPosition = -1;
  }

  @Nullable
  private ExoPlayer buildPlayerFor(int position) {
    Recipe recipe = recipes.get(position);
    String url = recipe.getVideoUrl();
    if (TextUtils.isEmpty(url)) {
      return null;
    }
    LoadControl loadControl = new DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            MIN_BUFFER_MS,
            MAX_BUFFER_MS,
            BUFFER_FOR_PLAYBACK_MS,
            BUFFER_FOR_REBUFFER_MS)
        .build();
    ExoPlayer player = new ExoPlayer.Builder(appContext)
        .setMediaSourceFactory(mediaSourceFactory)
        .setLoadControl(loadControl)
        .build();
    player.setRepeatMode(Player.REPEAT_MODE_ONE);
    // Self-heal transient playback failures — network blips, HTTP
    // disconnects mid-stream, etc. Without this the feed item stays
    // silently broken with no refresh UI, which is how fast scrolls
    // ended up in an inconsistent "some loaded, some not" state.
    player.addListener(new Player.Listener() {
      @Override
      public void onPlayerError(PlaybackException error) {
        player.seekToDefaultPosition();
        player.prepare();
      }
    });
    player.setMediaItem(MediaItem.fromUri(url));
    player.prepare();
    player.setPlayWhenReady(false);
    return player;
  }
}
