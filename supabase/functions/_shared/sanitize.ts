/**
 * Request-body validation for the AI proxy functions.
 *
 * These functions are reachable by anyone holding the Supabase anon key, which
 * is extractable from the published APK. The defence is therefore the *shape*
 * of the request, not the caller's auth tier: a constrained, typed body means
 * the worst an abuser gets is a bounded cooking answer, not a general-purpose
 * LLM running on our Gemini quota.
 */

export const MAX_BODY_BYTES = 64 * 1024;

const MAX_TURNS = 20;
const MAX_TEXT = 2000;
const MAX_TITLE = 500;
const MAX_DESCRIPTION = 2000;
const MAX_INGREDIENTS = 60;
const MAX_INGREDIENT_NAME = 200;
const MAX_TAGS = 30;
const MAX_TAG = 100;

export interface CleanIngredient {
  name: string;
  calories: number;
}

export interface CleanRecipe {
  title: string;
  authorName: string;
  description: string;
  ingredients: CleanIngredient[];
  prepTimeMinutes: number;
  cookTimeMinutes: number;
  estimatedCalories: number;
  tags: string[];
}

export interface CleanTurn {
  role: "user" | "model";
  text: string;
}

/** Thrown for any malformed body so callers can map it to a 400. */
export class BadRequest extends Error {}

function str(value: unknown, max: number): string {
  if (typeof value !== "string") {
    return "";
  }
  return value.slice(0, max);
}

function num(value: unknown): number {
  return typeof value === "number" && Number.isFinite(value) ? value : 0;
}

export function sanitizeRecipe(raw: unknown): CleanRecipe {
  if (raw === null || typeof raw !== "object") {
    throw new BadRequest("recipe must be an object");
  }
  const r = raw as Record<string, unknown>;

  const title = str(r.title, MAX_TITLE);
  if (title.length === 0) {
    throw new BadRequest("recipe.title is required");
  }

  const rawIngredients = Array.isArray(r.ingredients) ? r.ingredients : [];
  const ingredients: CleanIngredient[] = rawIngredients
    .slice(0, MAX_INGREDIENTS)
    .map((i) => {
      const o = (i ?? {}) as Record<string, unknown>;
      return {
        name: str(o.name, MAX_INGREDIENT_NAME),
        calories: num(o.calories),
      };
    })
    .filter((i) => i.name.length > 0);

  const rawTags = Array.isArray(r.tags) ? r.tags : [];
  const tags = rawTags
    .slice(0, MAX_TAGS)
    .map((t) => str(t, MAX_TAG))
    .filter((t) => t.length > 0);

  return {
    title,
    authorName: str(r.authorName, MAX_TITLE),
    description: str(r.description, MAX_DESCRIPTION),
    ingredients,
    prepTimeMinutes: num(r.prepTimeMinutes),
    cookTimeMinutes: num(r.cookTimeMinutes),
    estimatedCalories: num(r.estimatedCalories),
    tags,
  };
}

export function sanitizeHistory(raw: unknown): CleanTurn[] {
  if (!Array.isArray(raw)) {
    throw new BadRequest("history must be an array");
  }
  // Keep the most recent turns. The client caps at MAX_HISTORY_SIZE = 6; this
  // is defence in depth for a caller that ignores it.
  const turns: CleanTurn[] = raw.slice(-MAX_TURNS).map((t): CleanTurn => {
    const o = (t ?? {}) as Record<string, unknown>;
    if (o.role !== "user" && o.role !== "model") {
      throw new BadRequest("history role must be 'user' or 'model'");
    }
    return { role: o.role, text: str(o.text, MAX_TEXT) };
  }).filter((t) => t.text.length > 0);

  if (turns.length === 0) {
    throw new BadRequest("history must contain at least one non-empty turn");
  }
  return turns;
}

/** Reads and size-checks the JSON body. Throws BadRequest on anything invalid. */
export async function readJsonBody(req: Request): Promise<Record<string, unknown>> {
  const raw = await req.text();
  if (raw.length > MAX_BODY_BYTES) {
    throw new BadRequest("request body too large");
  }
  try {
    const parsed = JSON.parse(raw);
    if (parsed === null || typeof parsed !== "object" || Array.isArray(parsed)) {
      throw new BadRequest("body must be a JSON object");
    }
    return parsed as Record<string, unknown>;
  } catch (e) {
    if (e instanceof BadRequest) {
      throw e;
    }
    throw new BadRequest("body is not valid JSON");
  }
}
