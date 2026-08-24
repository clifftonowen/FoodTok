package com.example.foodtok.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import com.example.foodtok.models.ChatMessage;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the conversation window that {@link SupabaseChatService} sends to the model.
 *
 * <p>Regression cover for a crash: the chat history list is shared by reference with
 * {@code ChatMessageAdapter}, which uses it as its RecyclerView backing store. The old
 * implementation trimmed that shared list in place with no adapter notification, so
 * RecyclerView's item count desynced and the next layout pass threw
 * {@code IndexOutOfBoundsException: Inconsistency detected}. The window must therefore
 * bound what is *sent* without mutating what is *displayed*.
 */
public class ChatContextWindowTest {

  private static List<ChatMessage> conversation(int count) {
    List<ChatMessage> messages = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      messages.add(new ChatMessage(i % 2 == 0 ? "user" : "model", "message " + i));
    }
    return messages;
  }

  @Test
  public void doesNotMutateTheCallerList() {
    List<ChatMessage> history = conversation(9);
    SupabaseChatService.contextWindow(history, 6);
    assertEquals("history must not be trimmed in place — it is the adapter's backing list",
        9, history.size());
    assertEquals("message 0", history.get(0).getText());
  }

  @Test
  public void capsAtTheLimit() {
    assertEquals(6, SupabaseChatService.contextWindow(conversation(9), 6).size());
  }

  @Test
  public void keepsTheMostRecentMessagesInOrder() {
    List<ChatMessage> window = SupabaseChatService.contextWindow(conversation(9), 6);
    assertEquals("message 3", window.get(0).getText());
    assertEquals("message 8", window.get(5).getText());
  }

  @Test
  public void returnsACopyWhenTrimming() {
    List<ChatMessage> history = conversation(9);
    assertNotSame(history, SupabaseChatService.contextWindow(history, 6));
  }

  @Test
  public void passesShortConversationsThrough() {
    List<ChatMessage> history = conversation(4);
    assertSame(history, SupabaseChatService.contextWindow(history, 6));
  }

  @Test
  public void handlesExactlyTheLimit() {
    List<ChatMessage> history = conversation(6);
    assertSame(history, SupabaseChatService.contextWindow(history, 6));
  }

  @Test
  public void handlesEmptyHistory() {
    assertEquals(0, SupabaseChatService.contextWindow(new ArrayList<>(), 6).size());
  }
}
