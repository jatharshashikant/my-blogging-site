import { pgTable, serial, text, varchar, integer, timestamp, index, uniqueIndex } from "drizzle-orm/pg-core";

export const articles = pgTable(
  "articles",
  {
    id: serial().primaryKey(),
    title: varchar({ length: 500 }).notNull(),
    body: text().notNull().default(""),
    author: varchar({ length: 255 }).notNull().default("Admin"),
    category: varchar({ length: 255 }),
    likesCount: integer("likes_count").notNull().default(0),
    source: varchar({ length: 20 }).notNull().default("ADMIN"),
    documentUrl: varchar("document_url", { length: 1000 }),
    publishedAt: timestamp("published_at").notNull().defaultNow(),
  },
  (t) => [index("articles_published_at_idx").on(t.publishedAt)],
);

export const comments = pgTable(
  "comments",
  {
    id: serial().primaryKey(),
    articleId: integer("article_id")
      .notNull()
      .references(() => articles.id, { onDelete: "cascade" }),
    body: text().notNull(),
    authorName: varchar("author_name", { length: 255 }).notNull(),
    createdAt: timestamp("created_at").notNull().defaultNow(),
  },
  (t) => [index("comments_article_id_idx").on(t.articleId)],
);

export const guestPosts = pgTable("guest_posts", {
  id: serial().primaryKey(),
  title: varchar({ length: 500 }).notNull(),
  body: text().notNull(),
  authorName: varchar("author_name", { length: 255 }),
  category: varchar({ length: 255 }),
  status: varchar({ length: 20 }).notNull().default("PENDING"),
  documentUrl: varchar("document_url", { length: 1000 }),
  submittedAt: timestamp("submitted_at").notNull().defaultNow(),
});

export const reactions = pgTable(
  "reactions",
  {
    id: serial().primaryKey(),
    articleId: integer("article_id")
      .notNull()
      .references(() => articles.id, { onDelete: "cascade" }),
    sessionId: varchar("session_id", { length: 255 }).notNull(),
    reactionType: varchar("reaction_type", { length: 20 }).notNull(),
  },
  (t) => [uniqueIndex("reactions_article_session_type_idx").on(t.articleId, t.sessionId, t.reactionType)],
);
