/**
 * Thin wrapper around the Gemini generateContent REST endpoint.
 *
 * This is the only place the Gemini API key is read. The key is supplied as a
 * Supabase secret (`supabase secrets set GEMINI_API_KEY=...`) and is never sent
 * to, or returned to, the client.
 */

// Matches the model the Android app used before this migration.
const MODEL = "gemini-2.5-flash";
const ENDPOINT =
  `https://generativelanguage.googleapis.com/v1beta/models/${MODEL}:generateContent`;

export interface Turn {
  role: "user" | "model";
  text: string;
}

export type GeminiResult =
  | { ok: true; text: string }
  | { ok: false; status: number; error: string };

/**
 * Calls Gemini with a system instruction and a list of conversation turns.
 *
 * The upstream status code is propagated on failure so callers can forward a
 * real 429 rather than burying it in a 200 body.
 */
export async function callGemini(
  systemPrompt: string,
  turns: Turn[],
  opts: { jsonOutput?: boolean } = {},
): Promise<GeminiResult> {
  const apiKey = Deno.env.get("GEMINI_API_KEY");
  if (!apiKey) {
    console.error("GEMINI_API_KEY secret is not set");
    return { ok: false, status: 500, error: "server_misconfigured" };
  }

  const body: Record<string, unknown> = {
    system_instruction: { parts: [{ text: systemPrompt }] },
    contents: turns.map((t) => ({ role: t.role, parts: [{ text: t.text }] })),
  };
  if (opts.jsonOutput) {
    // Suppresses markdown code fences at the source. The client's parser
    // tolerates both, so this is a safe belt-and-braces addition.
    body.generationConfig = { responseMimeType: "application/json" };
  }

  let res: Response;
  try {
    res = await fetch(ENDPOINT, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        // Header rather than ?key= so the key never lands in a URL or proxy log.
        "x-goog-api-key": apiKey,
      },
      body: JSON.stringify(body),
    });
  } catch (e) {
    console.error("gemini fetch failed:", e instanceof Error ? e.message : e);
    return { ok: false, status: 502, error: "upstream_unreachable" };
  }

  if (!res.ok) {
    // Log status only. The response body can echo request content; the key is
    // never in it, but there is no reason to write user text to the logs.
    console.error("gemini returned", res.status);
    if (res.status === 429) {
      return { ok: false, status: 429, error: "rate_limited" };
    }
    return { ok: false, status: 502, error: "upstream_error" };
  }

  const data = await res.json();
  const text = data?.candidates?.[0]?.content?.parts?.[0]?.text ?? null;
  if (typeof text !== "string" || text.length === 0) {
    return { ok: false, status: 502, error: "empty_response" };
  }
  return { ok: true, text };
}
