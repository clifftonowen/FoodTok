package com.example.foodtok.util;

import com.example.foodtok.BuildConfig;

/** Single source of truth for the connection-free UI preview. */
public final class PreviewMode {

  private PreviewMode() {}

  public static boolean isEnabled() {
    return BuildConfig.USE_STUB_DATA;
  }
}
