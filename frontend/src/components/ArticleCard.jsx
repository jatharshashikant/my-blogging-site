import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import Reactions from './Reactions';

const SNIPPET_LENGTH = 240;

export default function ArticleCard({ id, title, author, publishedAt, category, body }) {
  const [expanded, setExpanded] = useState(false);

  const formatDate = (date) =>
    new Date(date).toLocaleDateString('en-IN', {
      year: 'numeric', month: 'long', day: 'numeric',
    });

  const plainText = body ? body.replace(/\\n/g, ' ').replace(/\n+/g, ' ').trim() : '';
  const snippet   = plainText.slice(0, SNIPPET_LENGTH);
  const hasMore   = plainText.length > SNIPPET_LENGTH;

  return (
    <div className="article-card">
      <Link to={`/articles/${id}`} className="card-link">
        <h3 className="card-title">{title}</h3>
      </Link>

      <div className="card-meta">
        <span className="card-author">✍ {author || 'Admin'}</span>
        <span className="meta-dot">·</span>
        <span>{formatDate(publishedAt)}</span>
        {category && <span className="category-badge">{category}</span>}
      </div>

      {body && (
        <div className="card-body-wrap">
          {expanded ? (
            <ArticleBodyRenderer body={body} compact={false} />
          ) : (
            <p className="card-snippet">
              {snippet}{hasMore ? '…' : ''}
            </p>
          )}

          {hasMore && (
            <button className="read-more-btn" onClick={() => setExpanded(!expanded)}>
              {expanded ? '▲ Show Less' : '▼ Read More'}
            </button>
          )}
        </div>
      )}

      {/* Reactions */}
      <Reactions articleId={id} />
    </div>
  );
}

/* ─────────────────────────────────────────────────────────────────
   Shared rich-text renderer — used by ArticleCard (expanded) AND
   ArticleDetailPage.
   ───────────────────────────────────────────────────────────────── */
export function ArticleBodyRenderer({ body }) {
  if (!body) return null;

  // Handle both real newlines AND literal \n strings stored in DB
  const normalised = body.replace(/\\n/g, '\n');
  const lines   = normalised.split('\n');
  const out     = [];
  let   i       = 0;
  let   listBuf = [];   // accumulate consecutive list-item lines

  const flushList = () => {
    if (listBuf.length === 0) return;
    out.push(
      <ul key={`list-${i}`} className="article-bullets">
        {listBuf.map((text, idx) => (
          <li key={idx}>{renderInline(text)}</li>
        ))}
      </ul>
    );
    listBuf = [];
  };

  /* ── helpers ── */

  // Numbered Devanagari section heading: "१. Title text" or "११ Title text"
  const sectionRe = /^([१२३४५६७८९0-9]+[०-९]*)\s*[.)]\s*(.+)/u;

  // A line is a "list item" if it starts with ✦ • ✓ or
  // is a short line beginning with जिथे/जेव्हा (continuation list in the article)
  const isBullet = (line) =>
    /^[✦•✓*-]\s/.test(line) || /^जिथे\s/.test(line);

  // Strip bullet prefix chars
  const stripBullet = (line) => line.replace(/^[✦•✓*-]\s*/, '').trim();

  // Render inline bold: **text**
  const renderInline = (text) => {
    if (!text.includes('**')) return text;
    const parts = text.split(/(\*\*[^*]+\*\*)/g);
    return parts.map((p, j) =>
      p.startsWith('**') && p.endsWith('**')
        ? <strong key={j}>{p.slice(2, -2)}</strong>
        : p
    );
  };

  while (i < lines.length) {
    const raw  = lines[i];
    const line = raw.trim();

    /* blank line → flush list, skip */
    if (!line) {
      flushList();
      i++;
      continue;
    }

    /* section heading */
    const secM = line.match(sectionRe);
    if (secM) {
      flushList();
      out.push(
        <div key={i} className="article-section-heading">
          <span className="section-number">{secM[1]}</span>
          <span className="section-title">{secM[2]}</span>
        </div>
      );
      i++;
      continue;
    }

    /* block-quote: line wrapped in " … " */
    if (
      (line.startsWith('"') || line.startsWith('\u201c')) &&
      (line.endsWith('"') || line.endsWith('\u201d')) &&
      line.length < 400
    ) {
      flushList();
      out.push(
        <blockquote key={i} className="article-blockquote">
          {renderInline(line.replace(/^["\u201c]|["\u201d]$/g, ''))}
        </blockquote>
      );
      i++;
      continue;
    }

    /* pull-quote: short italic question */
    if (line.endsWith('?') && line.length < 180 && !line.includes('.')) {
      flushList();
      out.push(
        <div key={i} className="article-pullquote">
          {renderInline(line)}
        </div>
      );
      i++;
      continue;
    }

    /* bullet / list item */
    if (isBullet(line)) {
      listBuf.push(stripBullet(line));
      i++;
      continue;
    }

    /* paragraph ending with "—" introduces a list on the NEXT lines */
    if (line.endsWith('—') || line.endsWith('–')) {
      flushList();
      out.push(
        <p key={i} className="article-para list-intro">
          {renderInline(line)}
        </p>
      );
      i++;
      /* read consecutive "जिथे…" lines as list items */
      while (i < lines.length && lines[i].trim() && isBullet(lines[i].trim())) {
        listBuf.push(stripBullet(lines[i].trim()));
        i++;
      }
      continue;
    }

    /* subtitle / kicker line (short, no full stop, at top of article) */
    if (out.length === 0 && line.length < 80 && !line.endsWith('.')) {
      out.push(
        <p key={i} className="article-subtitle">{line}</p>
      );
      i++;
      continue;
    }

    /* normal paragraph */
    flushList();
    out.push(
      <p key={i} className="article-para">{renderInline(line)}</p>
    );
    i++;
  }

  flushList(); // flush any trailing list

  return <div className="article-body-rendered">{out}</div>;
}
