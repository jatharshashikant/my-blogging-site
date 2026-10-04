import React, { useState, useEffect } from 'react';
import apiClient from '../api/apiClient';
import './Comments.css';

export default function Comments({ articleId, isAdmin = false, adminKey = '' }) {
  const [comments, setComments] = useState([]);
  const [expanded, setExpanded] = useState(false);
  const [loading, setLoading] = useState(false);
  const [newComment, setNewComment] = useState({ body: '', authorName: '' });
  const [error, setError] = useState('');

  useEffect(() => {
    if (expanded && comments.length === 0) {
      fetchComments();
    }
  }, [expanded]);

  const fetchComments = async () => {
    setLoading(true);
    try {
      const response = await apiClient.get(`/api/articles/${articleId}/comments`);
      setComments(response.data);
      setError('');
    } catch (err) {
      setError('Failed to load comments');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmitComment = async (e) => {
    e.preventDefault();
    if (!newComment.body.trim() || !newComment.authorName.trim()) {
      setError('Please fill in all fields');
      return;
    }

    try {
      const response = await apiClient.post(
        `/api/articles/${articleId}/comments`,
        newComment
      );
      setComments([response.data, ...comments]);
      setNewComment({ body: '', authorName: '' });
      setError('');
    } catch (err) {
      setError('Failed to post comment');
      console.error(err);
    }
  };

  const handleDeleteComment = async (commentId) => {
    try {
      await apiClient.delete(
        `/api/articles/${articleId}/comments/${commentId}`,
        { headers: isAdmin && adminKey ? { 'X-Admin-Key': adminKey } : {} }
      );
      setComments(comments.filter(c => c.id !== commentId));
    } catch (err) {
      setError('Failed to delete comment');
      console.error(err);
    }
  };

  return (
    <div className="comments-section">
      <button
        className="comments-toggle"
        onClick={() => setExpanded(!expanded)}
      >
        💬 Comments ({comments.length}) {expanded ? '▼' : '▶'}
      </button>

      {expanded && (
        <div className="comments-container">
          {error && <div className="error-message">{error}</div>}

          <form onSubmit={handleSubmitComment} className="comment-form">
            <input
              type="text"
              placeholder="Your name"
              value={newComment.authorName}
              onChange={(e) => setNewComment({ ...newComment, authorName: e.target.value })}
              required
            />
            <textarea
              placeholder="Add a comment..."
              value={newComment.body}
              onChange={(e) => setNewComment({ ...newComment, body: e.target.value })}
              rows="3"
              required
            />
            <button type="submit" className="submit-btn">Post Comment</button>
          </form>

          {loading && <div className="loading">Loading comments...</div>}

          <div className="comments-list">
            {comments.map((comment) => (
              <div key={comment.id} className="comment-item">
                <div className="comment-header">
                  <strong>{comment.authorName}</strong>
                  <span className="comment-date">
                    {new Date(comment.createdAt).toLocaleString()}
                  </span>
                  {isAdmin && (
                    <button
                      className="delete-btn"
                      onClick={() => handleDeleteComment(comment.id)}
                      title="Delete comment (admin)"
                    >
                      ✕
                    </button>
                  )}
                </div>
                <p className="comment-body">{comment.body}</p>
              </div>
            ))}
            {comments.length === 0 && !loading && (
              <p className="no-comments">No comments yet. Be the first to comment!</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
