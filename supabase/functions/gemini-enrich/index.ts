/**
 * gemini-enrich — one-shot recipe analysis.
 *
 * Returns the model's raw text verbatim rather than a parsed object. That is
 * deliberate: the Android client's parseEnrichment(), its markdown-fence
 * stripping, and its per-recipe cache all keep working unchanged. Parsing
 * server-side would force a rewrite of exactly the code this migration is
 * meant to leave alone.
 *
 * Request:  { recipe: {...} }
 * Response: { text: "<raw model JSON as a string>" }  on 200
 *           { error: "<code>" }                       on 400 / 429 / 502
 */

import { handleOptions, json } from "../_shared/cors.ts";
import { callGemini } from "../_shared/gemini.ts";
import { buildEnrichSystemPrompt, buildEnrichUserPrompt } from "../_shared/prompts.ts";
import { BadRequest, readJsonBody, sanitizeRecipe } from "../_shared/sanitize.ts";

Deno.serve(async (req: Request) => {
  const preflight = handleOptions(req);
  if (preflight) {
    return preflight;
  }
  if (req.method !== "POST") {
    return json({ error: "method_not_allowed" }, 405);
  }

  let systemPrompt: string;
  let userPrompt: string;
  try {
    const body = await readJsonBody(req);
    const recipe = sanitizeRecipe(body.recipe);
    systemPrompt = buildEnrichSystemPrompt();
    userPrompt = buildEnrichUserPrompt(recipe);
  } catch (e) {
    if (e instanceof BadRequest) {
      return json({ error: "bad_request", detail: e.message }, 400);
    }
    console.error("unexpected error parsing request:", e);
    return json({ error: "bad_request" }, 400);
  }

  const result = await callGemini(
    systemPrompt,
    [{ role: "user", text: userPrompt }],
    { jsonOutput: true },
  );
  if (!result.ok) {
    return json({ error: result.error }, result.status);
  }
  return json({ text: result.text });
});
