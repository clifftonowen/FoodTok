/**
 * gemini-chat — per-recipe cooking assistant.
 *
 * Accepts a typed recipe context plus the conversation history the client is
 * holding, builds the system prompt server-side, and proxies the call to Gemini
 * with the API key supplied as a Supabase secret.
 *
 * Conversation memory stays on the device: Edge Functions are stateless, and
 * the Android UI shares a live List<ChatMessage> between the service and the
 * adapter, so history must remain client-owned.
 *
 * Request:  { recipe: {...}, history: [{ role, text }, ...] }
 * Response: { text: "<raw model text>" }  on 200
 *           { error: "<code>" }           on 400 / 429 / 502
 *
 * The 429 is returned as a real HTTP status, not a 200 carrying an error body,
 * because the Android client branches on response.code() == 429 to show its
 * friendly rate-limit message.
 */

import { handleOptions, json } from "../_shared/cors.ts";
import { callGemini } from "../_shared/gemini.ts";
import { buildChatSystemPrompt } from "../_shared/prompts.ts";
import { BadRequest, readJsonBody, sanitizeHistory, sanitizeRecipe } from "../_shared/sanitize.ts";

Deno.serve(async (req: Request) => {
  const preflight = handleOptions(req);
  if (preflight) {
    return preflight;
  }
  if (req.method !== "POST") {
    return json({ error: "method_not_allowed" }, 405);
  }

  let systemPrompt: string;
  let turns: ReturnType<typeof sanitizeHistory>;
  try {
    const body = await readJsonBody(req);
    const recipe = sanitizeRecipe(body.recipe);
    turns = sanitizeHistory(body.history);
    systemPrompt = buildChatSystemPrompt(recipe);
  } catch (e) {
    if (e instanceof BadRequest) {
      return json({ error: "bad_request", detail: e.message }, 400);
    }
    console.error("unexpected error parsing request:", e);
    return json({ error: "bad_request" }, 400);
  }

  // Observability only. anon callers are allowed on purpose: the feed and chat
  // page are reachable without logging in, and the public demo user will never
  // sign up. verify_jwt = true already requires at least the anon key.
  console.log("gemini-chat role:", callerRole(req), "turns:", turns.length);

  const result = await callGemini(systemPrompt, turns);
  if (!result.ok) {
    return json({ error: result.error }, result.status);
  }
  return json({ text: result.text });
});

/** Decodes (does not verify) the JWT role claim. The platform gate already verified it. */
function callerRole(req: Request): string {
  const auth = req.headers.get("Authorization") ?? "";
  const token = auth.startsWith("Bearer ") ? auth.slice(7) : "";
  const payload = token.split(".")[1];
  if (!payload) {
    return "unknown";
  }
  try {
    const decoded = JSON.parse(atob(payload.replace(/-/g, "+").replace(/_/g, "/")));
    return typeof decoded.role === "string" ? decoded.role : "unknown";
  } catch {
    return "unknown";
  }
}
