package com.example.foodtok.services;

/** Async callback for operations that return an integer count. */
public interface IntCallback {
  void onResult(int count);
  void onError(String message);
}
