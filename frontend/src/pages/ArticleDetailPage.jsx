import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import apiClient from '../api/apiClient';
import LikeButton from '../components/LikeButton';
import Reactions from '../components/Reactions';
import Comments from '../components/Comments';
import ErrorMessage from '../components/ErrorMessage';
import { ArticleBodyRenderer } from '../components/ArticleCard';

export default function ArticleDetailPage() {
  const { id } = useParams();
  const [article, setArticle] = useState(null);
  const [error, setError]     = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchArticle = async () => {
      setLoading(true);
      setError('');
      try {
        const response = await apiClient.get(`/api/articles/${id}`);
        setArticle(response.data);
      } catch (err) {
        setError(err.response?.data?.message || 'Article not found');
      } finally {
        setLoading(false);
      }
    };
    fetchArticle();
  }, [id]);

  if (loading) return <div className="container"><p className="loading-text">Loading…</p></div>;
  if (error || !article) return <div className="container"><ErrorMessage message={error} /></div>;

  const formatDate = (date) =>
    new Date(date).toLocaleDateString('en-IN', {
      year: 'numeric', month: 'long', day: 'numeric',
    });

  return (
    <div className="container">
      <Link to="/" className="back-link">← Back to articles</Link>

      <article className="article-detail">
        {/* Title */}
        <h1 className="article-detail-title">{article.title}</h1>

        {/* Meta */}
        <div className="article-detail-meta">
          <span>✍ {article.author}</span>
          <span className="meta-dot">·</span>
          <span>📅 {formatDate(article.publishedAt)}</span>
          {article.category && (
            <span className="category-badge">{article.category}</span>
          )}
        </div>

        <hr className="article-divider" />

        {/* Body */}
        <div className="article-body">
          <ArticleBodyRenderer body={article.body || '(No content)'} />
        </div>

        {/* Document download */}
        {article.documentUrl && (
          <p className="document-link">
            <a
              href={`${process.env.REACT_APP_API_URL || ''}/uploads/${article.documentUrl}`}
              target="_blank"
              rel="noreferrer"
            >
              📎 Download Document
            </a>
          </p>
        )}

        {/* Reactions */}
        <Reactions articleId={article.id} />

        {/* Comments */}
        <Comments articleId={article.id} />

        {/* Footer */}
        <div className="article-footer">
          <LikeButton articleId={article.id} initialLikes={article.likesCount} />
        </div>
      </article>
    </div>
  );
}
