CREATE TABLE "articles" (
	"id" serial PRIMARY KEY,
	"title" varchar(500) NOT NULL,
	"body" text DEFAULT '' NOT NULL,
	"author" varchar(255) DEFAULT 'Admin' NOT NULL,
	"category" varchar(255),
	"likes_count" integer DEFAULT 0 NOT NULL,
	"source" varchar(20) DEFAULT 'ADMIN' NOT NULL,
	"document_url" varchar(1000),
	"published_at" timestamp DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "comments" (
	"id" serial PRIMARY KEY,
	"article_id" integer NOT NULL,
	"body" text NOT NULL,
	"author_name" varchar(255) NOT NULL,
	"created_at" timestamp DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "guest_posts" (
	"id" serial PRIMARY KEY,
	"title" varchar(500) NOT NULL,
	"body" text NOT NULL,
	"author_name" varchar(255),
	"category" varchar(255),
	"status" varchar(20) DEFAULT 'PENDING' NOT NULL,
	"document_url" varchar(1000),
	"submitted_at" timestamp DEFAULT now() NOT NULL
);
--> statement-breakpoint
CREATE TABLE "reactions" (
	"id" serial PRIMARY KEY,
	"article_id" integer NOT NULL,
	"session_id" varchar(255) NOT NULL,
	"reaction_type" varchar(20) NOT NULL
);
--> statement-breakpoint
CREATE INDEX "articles_published_at_idx" ON "articles" ("published_at");--> statement-breakpoint
CREATE INDEX "comments_article_id_idx" ON "comments" ("article_id");--> statement-breakpoint
CREATE UNIQUE INDEX "reactions_article_session_type_idx" ON "reactions" ("article_id","session_id","reaction_type");--> statement-breakpoint
ALTER TABLE "comments" ADD CONSTRAINT "comments_article_id_articles_id_fkey" FOREIGN KEY ("article_id") REFERENCES "articles"("id") ON DELETE CASCADE;--> statement-breakpoint
ALTER TABLE "reactions" ADD CONSTRAINT "reactions_article_id_articles_id_fkey" FOREIGN KEY ("article_id") REFERENCES "articles"("id") ON DELETE CASCADE;