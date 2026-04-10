package com.example.foodtok.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

/** Unit tests for {@link Trie} and {@link TrieNode}. */
public class TrieTest {

  private Trie trie;

  @Before
  public void setUp() {
    trie = new Trie();
  }

  @Test
  public void emptyTrieHasSizeZero() {
    assertTrue(trie.isEmpty());
    assertEquals(0, trie.size());
  }

  @Test
  public void insertAndSearchSingleWord() {
    trie.insert("chicken");
    assertTrue(trie.search("chicken"));
    assertFalse(trie.search("chick"));
    assertEquals(1, trie.size());
  }

  @Test
  public void searchIsCaseInsensitive() {
    trie.insert("Chicken");
    assertTrue(trie.search("chicken"));
    assertTrue(trie.search("CHICKEN"));
    assertTrue(trie.search("Chicken"));
  }

  @Test
  public void insertDuplicateDoesNotIncreaseSize() {
    trie.insert("salt");
    trie.insert("salt");
    trie.insert("Salt");
    assertEquals(1, trie.size());
  }

  @Test
  public void searchReturnsFalseForMissingWord() {
    trie.insert("pepper");
    assertFalse(trie.search("pep"));
    assertFalse(trie.search("peppers"));
    assertFalse(trie.search("salt"));
  }

  @Test
  public void startsWithFindsPrefix() {
    trie.insert("chicken");
    trie.insert("chickpea");
    assertTrue(trie.startsWith("chi"));
    assertTrue(trie.startsWith("chicken"));
    assertFalse(trie.startsWith("cheese"));
  }

  @Test
  public void autocompleteReturnsMatchingWords() {
    trie.insert("chicken");
    trie.insert("chickpea");
    trie.insert("chili");
    trie.insert("cheese");
    trie.insert("salt");

    List<String> results = trie.autocomplete("chi", 10);
    assertEquals(3, results.size());
    assertTrue(results.contains("chicken"));
    assertTrue(results.contains("chickpea"));
    assertTrue(results.contains("chili"));
  }

  @Test
  public void autocompleteRespectsMaxResults() {
    trie.insert("apple");
    trie.insert("apricot");
    trie.insert("avocado");
    trie.insert("artichoke");

    List<String> results = trie.autocomplete("a", 2);
    assertEquals(2, results.size());
  }

  @Test
  public void autocompleteWithExactMatch() {
    trie.insert("salt");
    List<String> results = trie.autocomplete("salt", 10);
    assertEquals(1, results.size());
    assertEquals("salt", results.get(0));
  }

  @Test
  public void autocompleteWithNoMatch() {
    trie.insert("chicken");
    List<String> results = trie.autocomplete("xyz", 10);
    assertTrue(results.isEmpty());
  }

  @Test
  public void insertNullAndBlankAreIgnored() {
    trie.insert(null);
    trie.insert("");
    trie.insert("   ");
    assertEquals(0, trie.size());
    assertFalse(trie.search(null));
    assertFalse(trie.search(""));
  }

  @Test
  public void autocompleteWithNullPrefixReturnsEmpty() {
    trie.insert("chicken");
    assertTrue(trie.autocomplete(null, 10).isEmpty());
    assertTrue(trie.autocomplete("", 10).isEmpty());
  }

  @Test
  public void multipleWordsWithSharedPrefix() {
    String[] ingredients = {
        "chicken", "chickpea", "chili", "cheese", "cherry",
        "garlic", "ginger", "green onion",
        "salt", "sugar", "soy sauce"
    };
    for (String ingredient : ingredients) {
      trie.insert(ingredient);
    }

    assertEquals(11, trie.size());

    List<String> chResults = trie.autocomplete("ch", 10);
    assertEquals(5, chResults.size());

    List<String> gResults = trie.autocomplete("g", 10);
    assertEquals(3, gResults.size());

    List<String> sResults = trie.autocomplete("s", 10);
    assertEquals(3, sResults.size());
  }
}