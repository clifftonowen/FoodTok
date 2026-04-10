package com.example.foodtok.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Node in a prefix tree (Trie). Each node stores a map of child nodes
 * keyed by character and a flag indicating whether the node marks the
 * end of a complete word.
 */
public class TrieNode {

  private final Map<Character, TrieNode> children;
  private boolean isEndOfWord;
  private String fullWord;

  /** Creates an empty trie node with no children. */
  public TrieNode() {
    this.children = new HashMap<>();
    this.isEndOfWord = false;
    this.fullWord = null;
  }

  /** Returns the map of child nodes keyed by character. */
  public Map<Character, TrieNode> getChildren() {
    return children;
  }

  /** Returns {@code true} if this node marks the end of a stored word. */
  public boolean isEndOfWord() {
    return isEndOfWord;
  }

  /**
   * Sets whether this node marks the end of a stored word.
   *
   * @param endOfWord {@code true} to mark this node as a word boundary
   */
  public void setEndOfWord(boolean endOfWord) {
    this.isEndOfWord = endOfWord;
  }

  /**
   * Returns the full word stored at this terminal node, or {@code null}
   * if this node is not a word boundary.
   */
  public String getFullWord() {
    return fullWord;
  }

  /**
   * Stores the full word at this terminal node for easy retrieval.
   *
   * @param fullWord the complete word ending at this node
   */
  public void setFullWord(String fullWord) {
    this.fullWord = fullWord;
  }

  /**
   * Returns whether this node has a child for the given character.
   *
   * @param c the character to check
   * @return {@code true} if a child exists for {@code c}
   */
  public boolean hasChild(char c) {
    return children.containsKey(c);
  }

  /**
   * Returns the child node for the given character, or {@code null}
   * if no such child exists.
   *
   * @param c the character to look up
   * @return the child node, or {@code null}
   */
  public TrieNode getChild(char c) {
    return children.get(c);
  }

  /**
   * Adds a child node for the given character and returns it.
   * If a child already exists for this character, the existing child
   * is returned without modification.
   *
   * @param c the character to add a child for
   * @return the child node (newly created or existing)
   */
  public TrieNode addChild(char c) {
    return children.computeIfAbsent(c, k -> new TrieNode());
  }
}