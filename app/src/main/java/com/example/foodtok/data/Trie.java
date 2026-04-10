package com.example.foodtok.data;

import java.util.ArrayList;
import java.util.List;

/**
 * A prefix tree (Trie) for fast string autocomplete and lookup.
 *
 * <p>All words are stored in lowercase. Insertion is O(L) where L is the
 * word length. Autocomplete returns up to {@code maxResults} words sharing
 * the given prefix, collected via depth-first traversal of the subtree.</p>
 *
 * <p>This is a custom implementation for the 50.004 data structures
 * requirement — it does not delegate to any library trie.</p>
 */
public class Trie {

  private final TrieNode root;
  private int size;

  /** Creates an empty trie. */
  public Trie() {
    this.root = new TrieNode();
    this.size = 0;
  }

  /**
   * Inserts a word into the trie. The word is converted to lowercase
   * before insertion. Null and blank strings are ignored.
   *
   * @param word the word to insert
   */
  public void insert(String word) {
    if (word == null || word.trim().isEmpty()) {
      return;
    }

    String lower = word.trim().toLowerCase();
    TrieNode current = root;

    for (int i = 0; i < lower.length(); i++) {
      current = current.addChild(lower.charAt(i));
    }

    if (!current.isEndOfWord()) {
      size++;
    }
    current.setEndOfWord(true);
    current.setFullWord(lower);
  }

  /**
   * Returns {@code true} if the trie contains an exact match for the
   * given word (case-insensitive).
   *
   * @param word the word to search for
   * @return {@code true} if the word exists in the trie
   */
  public boolean search(String word) {
    if (word == null || word.trim().isEmpty()) {
      return false;
    }

    TrieNode node = findNode(word.trim().toLowerCase());
    return node != null && node.isEndOfWord();
  }

  /**
   * Returns {@code true} if any word in the trie starts with the given
   * prefix (case-insensitive).
   *
   * @param prefix the prefix to check
   * @return {@code true} if at least one word starts with the prefix
   */
  public boolean startsWith(String prefix) {
    if (prefix == null || prefix.trim().isEmpty()) {
      return false;
    }

    return findNode(prefix.trim().toLowerCase()) != null;
  }

  /**
   * Returns up to {@code maxResults} words that start with the given
   * prefix, collected via depth-first search. Results are in insertion
   * order within each subtree branch.
   *
   * @param prefix     the prefix to autocomplete (case-insensitive)
   * @param maxResults maximum number of suggestions to return
   * @return list of matching words, never null
   */
  public List<String> autocomplete(String prefix, int maxResults) {
    List<String> results = new ArrayList<>();

    if (prefix == null || prefix.trim().isEmpty() || maxResults <= 0) {
      return results;
    }

    TrieNode node = findNode(prefix.trim().toLowerCase());
    if (node != null) {
      collectWords(node, results, maxResults);
    }

    return results;
  }

  /** Returns the number of distinct words stored in the trie. */
  public int size() {
    return size;
  }

  /** Returns {@code true} if the trie contains no words. */
  public boolean isEmpty() {
    return size == 0;
  }

  /**
   * Navigates from the root to the node corresponding to the last
   * character of the given key, or {@code null} if the path does not
   * exist.
   */
  private TrieNode findNode(String key) {
    TrieNode current = root;

    for (int i = 0; i < key.length(); i++) {
      current = current.getChild(key.charAt(i));
      if (current == null) {
        return null;
      }
    }

    return current;
  }

  /**
   * Depth-first traversal that collects terminal words into
   * {@code results}, stopping early once {@code maxResults} is reached.
   */
  private void collectWords(TrieNode node, List<String> results,
      int maxResults) {
    if (results.size() >= maxResults) {
      return;
    }

    if (node.isEndOfWord()) {
      results.add(node.getFullWord());
    }

    for (TrieNode child : node.getChildren().values()) {
      if (results.size() >= maxResults) {
        return;
      }
      collectWords(child, results, maxResults);
    }
  }
}