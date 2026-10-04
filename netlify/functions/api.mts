import type { Config } from "@netlify/functions";
import { getStore } from "@netlify/blobs";
import { and, count, desc, eq, ilike, or, sql } from "drizzle-orm";
import { createHash, randomUUID, timingSafeEqual } from "node:crypto";
import { db } from "../../db/index.js";
import { articles, comments, guestPosts, reactions } from "../../db/schema.js";

// Port of the Spring Boot API (backend/) to a single Netlify Function.
// Endpoints and response shapes match the original controllers so the
// React frontend works unchanged.

class HttpError extends Error {
  constructor(public status: number, public error: string, message: string) {
    super(message);
  }
}

const notFound = (message: string) => new HttpError(404, "NOT_FOUND", message);
const invalid = (message: string) => new HttpError(400, "VALIDATION_ERROR", message);

const REACTION_TYPES = new Set(["LOVE", "LIKE", "DISLIKE"]);
const ALLOWED_DOCUMENT_TYPES = new Set([
  "application/pdf",
  "application/msword",
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
]);
const MAX_DOCUMENT_SIZE = 10 * 1024 * 1024;

const isBlank = (value: unknown) => typeof value !== "string" || value.trim() === "";

function toId(value: string) {
  const id = Number(value);
  if (!Number.isSafeInteger(id) || id <= 0 || id > 2147483647) throw notFound(`Resource not found with id: ${value}`);
  return id;
}

function requireAdmin(req: Request) {
  const expected = Netlify.env.get("ADMIN_KEY");
  const provided = req.headers.get("x-admin-key");
  const digest = (v: string) => createHash("sha256").update(v).digest();
  if (!expected || !provided || !timingSafeEqual(digest(expected), digest(provided))) {
    throw new HttpError(403, "Forbidden", "Missing or invalid X-Admin-Key header");
  }
}

async function findArticle(id: number) {
  const [article] = await db.select().from(articles).where(eq(articles.id, id));
  if (!article) throw notFound(`Article not found with id: ${id}`);
  return article;
}

function articleResponse(a: typeof articles.$inferSelect) {
  const { source, ...rest } = a;
  return rest;
}

// --- Articles ---------------------------------------------------------------

async function listArticles(url: URL) {
  const page = Math.max(0, Number.parseInt(url.searchParams.get("page") ?? "0", 10) || 0);
  const size = Math.min(100, Math.max(1, Number.parseInt(url.searchParams.get("size") ?? "10", 10) || 10));
  const [rows, [{ total }]] = await Promise.all([
    db.select().from(articles).orderBy(desc(articles.publishedAt), desc(articles.id)).limit(size).offset(page * size),
    db.select({ total: count() }).from(articles),
  ]);
  const totalPages = Math.ceil(total / size);
  return Response.json({
    content: rows.map(articleResponse),
    totalElements: total,
    totalPages,
    number: page,
    size,
    numberOfElements: rows.length,
    first: page === 0,
    last: page >= totalPages - 1,
    empty: rows.length === 0,
  });
}

async function searchArticles(url: URL) {
  const q = url.searchParams.get("q");
  if (q === null || isBlank(q)) throw invalid("Search query must not be blank");
  const pattern = `%${q.replace(/[\\%_]/g, (c) => `\\${c}`)}%`;
  const rows = await db
    .select()
    .from(articles)
    .where(or(ilike(articles.title, pattern), ilike(articles.body, pattern)))
    .orderBy(desc(articles.publishedAt));
  return Response.json(rows.map(articleResponse));
}

async function createArticle(req: Request) {
  const body = await req.json().catch(() => ({}));
  if (isBlank(body.title)) throw invalid("Article title must not be blank");
  const [created] = await db
    .insert(articles)
    .values({
      title: body.title.trim(),
      body: typeof body.body === "string" ? body.body : "",
      category: body.category ?? null,
      source: "ADMIN",
    })
    .returning();
  return Response.json(articleResponse(created), { status: 201 });
}

async function deleteArticle(id: number) {
  const deleted = await db.delete(articles).where(eq(articles.id, id)).returning({ id: articles.id });
  if (deleted.length === 0) throw notFound(`Article not found with id: ${id}`);
  return new Response(null, { status: 204 });
}

// --- Likes ------------------------------------------------------------------

async function like(id: number) {
  const [row] = await db
    .update(articles)
    .set({ likesCount: sql`${articles.likesCount} + 1` })
    .where(eq(articles.id, id))
    .returning({ likesCount: articles.likesCount });
  if (!row) throw notFound(`Article not found with id: ${id}`);
  return Response.json(row);
}

async function unlike(id: number) {
  const [row] = await db
    .update(articles)
    .set({ likesCount: sql`${articles.likesCount} - 1` })
    .where(and(eq(articles.id, id), sql`${articles.likesCount} > 0`))
    .returning({ likesCount: articles.likesCount });
  if (!row) {
    await findArticle(id);
    throw invalid("Like count is already 0");
  }
  return Response.json(row);
}

// --- Comments ---------------------------------------------------------------

async function listComments(articleId: number) {
  await findArticle(articleId);
  const rows = await db
    .select({ id: comments.id, body: comments.body, authorName: comments.authorName, createdAt: comments.createdAt })
    .from(comments)
    .where(eq(comments.articleId, articleId))
    .orderBy(desc(comments.createdAt));
  return Response.json(rows);
}

async function addComment(req: Request, articleId: number) {
  const body = await req.json().catch(() => ({}));
  if (isBlank(body.body)) throw invalid("Comment body cannot be empty");
  if (isBlank(body.authorName)) throw invalid("Author name cannot be empty");
  await findArticle(articleId);
  const [created] = await db
    .insert(comments)
    .values({ articleId, body: body.body, authorName: body.authorName })
    .returning({ id: comments.id, body: comments.body, authorName: comments.authorName, createdAt: comments.createdAt });
  return Response.json(created, { status: 201 });
}

async function deleteComment(articleId: number, commentId: number) {
  const deleted = await db
    .delete(comments)
    .where(and(eq(comments.id, commentId), eq(comments.articleId, articleId)))
    .returning({ id: comments.id });
  if (deleted.length === 0) throw notFound(`Comment not found with id: ${commentId}`);
  return new Response(null, { status: 204 });
}

// --- Reactions --------------------------------------------------------------

function sessionIdFrom(req: Request) {
  const match = /(?:^|;\s*)sessionId=([^;]+)/.exec(req.headers.get("cookie") ?? "");
  return match ? { id: decodeURIComponent(match[1]), isNew: false } : { id: randomUUID(), isNew: true };
}

async function reactionCounts(articleId: number, sessionId: string) {
  const rows = await db
    .select({ reactionType: reactions.reactionType, sessionId: reactions.sessionId })
    .from(reactions)
    .where(eq(reactions.articleId, articleId));
  const tally = (type: string) => rows.filter((r) => r.reactionType === type).length;
  return {
    love: tally("LOVE"),
    like: tally("LIKE"),
    dislike: tally("DISLIKE"),
    userReaction: rows.find((r) => r.sessionId === sessionId)?.reactionType ?? null,
  };
}

async function toggleReaction(articleId: number, sessionId: string, reactionType: string) {
  if (!REACTION_TYPES.has(reactionType)) throw invalid("Invalid reaction type. Allowed: LOVE, LIKE, DISLIKE");
  await findArticle(articleId);
  const mine = and(eq(reactions.articleId, articleId), eq(reactions.sessionId, sessionId));
  const removed = await db
    .delete(reactions)
    .where(and(mine, eq(reactions.reactionType, reactionType)))
    .returning({ id: reactions.id });
  if (removed.length === 0) {
    // A visitor holds at most one reaction per article: replace any other one.
    await db.delete(reactions).where(mine);
    await db.insert(reactions).values({ articleId, sessionId, reactionType }).onConflictDoNothing();
  }
}

async function handleReactions(req: Request, articleId: number, reactionType?: string) {
  const session = sessionIdFrom(req);
  if (reactionType !== undefined) {
    await toggleReaction(articleId, session.id, reactionType);
  } else {
    await findArticle(articleId);
  }
  const res = Response.json(await reactionCounts(articleId, session.id));
  if (session.isNew) {
    res.headers.set("Set-Cookie", `sessionId=${session.id}; Path=/; Max-Age=31536000; SameSite=Lax; HttpOnly; Secure`);
  }
  return res;
}

// --- Guest posts ------------------------------------------------------------

async function submitGuestPost(req: Request) {
  const form = await req.formData().catch(() => {
    throw invalid("Expected multipart/form-data");
  });
  const part = form.get("request");
  let data: Record<string, unknown> = {};
  try {
    data = JSON.parse(typeof part === "string" ? part : part ? await part.text() : "{}");
  } catch {
    throw invalid("Invalid request payload");
  }
  if (isBlank(data.title)) throw invalid("Title must not be blank");
  if (isBlank(data.body)) throw invalid("Body must not be blank");

  let documentUrl: string | null = null;
  const document = form.get("document");
  if (document && typeof document !== "string" && document.size > 0) {
    if (!ALLOWED_DOCUMENT_TYPES.has(document.type)) throw invalid("Invalid document type. Accepted types: PDF, DOC, DOCX");
    if (document.size > MAX_DOCUMENT_SIZE) throw invalid("Document size must not exceed 10 MB");
    const ext = /\.[A-Za-z0-9]{1,8}$/.exec(document.name)?.[0] ?? "";
    documentUrl = `${randomUUID()}${ext}`;
    await getStore("guest-documents").set(documentUrl, await document.arrayBuffer(), {
      metadata: { contentType: document.type, originalName: document.name },
    });
  }

  const str = (v: unknown) => (typeof v === "string" && v !== "" ? v : null);
  const [created] = await db
    .insert(guestPosts)
    .values({
      title: data.title as string,
      body: data.body as string,
      authorName: str(data.authorName),
      category: str(data.category),
      status: "PENDING",
      documentUrl,
    })
    .returning();
  return Response.json(created, { status: 201 });
}

async function listPendingGuestPosts() {
  const rows = await db
    .select()
    .from(guestPosts)
    .where(eq(guestPosts.status, "PENDING"))
    .orderBy(desc(guestPosts.submittedAt));
  return Response.json(rows);
}

async function approveGuestPost(id: number) {
  const [post] = await db
    .update(guestPosts)
    .set({ status: "APPROVED" })
    .where(and(eq(guestPosts.id, id), eq(guestPosts.status, "PENDING")))
    .returning();
  if (!post) throw notFound(`GuestPost not found with id: ${id}`);
  const [article] = await db
    .insert(articles)
    .values({
      title: post.title,
      body: post.body,
      author: isBlank(post.authorName) ? "Anonymous" : post.authorName!,
      category: post.category,
      source: "GUEST",
      documentUrl: post.documentUrl,
    })
    .returning();
  return Response.json(articleResponse(article));
}

async function serveDocument(name: string) {
  const result = await getStore("guest-documents").getWithMetadata(name, { type: "stream" });
  if (!result) throw notFound("Document not found");
  const meta = result.metadata as { contentType?: string; originalName?: string };
  const filename = (meta.originalName ?? name).replace(/["\\\r\n]/g, "");
  return new Response(result.data, {
    headers: {
      "Content-Type": meta.contentType ?? "application/octet-stream",
      "Content-Disposition": `inline; filename="${filename}"`,
      "X-Content-Type-Options": "nosniff",
    },
  });
}

// --- Router -----------------------------------------------------------------

async function route(req: Request): Promise<Response> {
  const url = new URL(req.url);
  const path = url.pathname.replace(/\/+$/, "");
  const method = req.method;
  let m: RegExpExecArray | null;

  if ((m = /^\/uploads\/([A-Za-z0-9.-]+)$/.exec(path)) && method === "GET") return serveDocument(m[1]);

  if (path === "/api/articles" && method === "GET") return listArticles(url);
  if (path === "/api/articles/search" && method === "GET") return searchArticles(url);
  if ((m = /^\/api\/articles\/([^/]+)$/.exec(path)) && method === "GET") {
    return Response.json(articleResponse(await findArticle(toId(m[1]))));
  }

  if ((m = /^\/api\/articles\/([^/]+)\/like$/.exec(path))) {
    if (method === "POST") return like(toId(m[1]));
    if (method === "DELETE") return unlike(toId(m[1]));
  }

  if ((m = /^\/api\/articles\/([^/]+)\/comments$/.exec(path))) {
    if (method === "GET") return listComments(toId(m[1]));
    if (method === "POST") return addComment(req, toId(m[1]));
  }
  if ((m = /^\/api\/articles\/([^/]+)\/comments\/([^/]+)$/.exec(path)) && method === "DELETE") {
    requireAdmin(req);
    return deleteComment(toId(m[1]), toId(m[2]));
  }

  if ((m = /^\/api\/articles\/([^/]+)\/reactions$/.exec(path)) && method === "GET") {
    return handleReactions(req, toId(m[1]));
  }
  if ((m = /^\/api\/articles\/([^/]+)\/reactions\/([^/]+)$/.exec(path)) && method === "POST") {
    return handleReactions(req, toId(m[1]), m[2]);
  }

  if (path === "/api/guest-posts" && method === "POST") return submitGuestPost(req);

  if (path.startsWith("/api/admin/")) {
    requireAdmin(req);
    if (path === "/api/admin/articles" && method === "POST") return createArticle(req);
    if (path === "/api/admin/guest-posts" && method === "GET") return listPendingGuestPosts();
    if ((m = /^\/api\/admin\/guest-posts\/([^/]+)\/approve$/.exec(path)) && method === "POST") {
      return approveGuestPost(toId(m[1]));
    }
    if ((m = /^\/api\/admin\/articles\/([^/]+)$/.exec(path)) && method === "DELETE") return deleteArticle(toId(m[1]));
  }

  throw notFound(`No route for ${method} ${url.pathname}`);
}

export default async (req: Request) => {
  try {
    return await route(req);
  } catch (err) {
    if (err instanceof HttpError) {
      return Response.json({ error: err.error, message: err.message }, { status: err.status });
    }
    console.error(err);
    return Response.json({ error: "INTERNAL_ERROR", message: "An unexpected error occurred" }, { status: 500 });
  }
};

export const config: Config = {
  path: ["/api/*", "/uploads/*"],
};
