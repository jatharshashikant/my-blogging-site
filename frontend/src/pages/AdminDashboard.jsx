import React, { useState, useEffect } from 'react';
import apiClient from '../api/apiClient';
import ErrorMessage from '../components/ErrorMessage';
import './AdminDashboard.css';

export default function AdminDashboard() {
  const [guestPosts, setGuestPosts] = useState([]);
  const [articles, setArticles] = useState([]);
  const [activeTab, setActiveTab] = useState('pending'); // 'pending' or 'published'
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [adminKey, setAdminKey] = useState('');
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [approving, setApproving] = useState(null);
  const [deleting, setDeleting] = useState(null);

  useEffect(() => {
    const stored = localStorage.getItem('adminKey');
    if (stored) {
      setAdminKey(stored);
      setIsAuthenticated(true);
      fetchData(stored);
    } else {
      setLoading(false);
    }
  }, []);

  const handleLogin = (e) => {
    e.preventDefault();
    if (adminKey.trim()) {
      localStorage.setItem('adminKey', adminKey);
      setIsAuthenticated(true);
      fetchData(adminKey);
    }
  };

  const fetchData = async (key) => {
    setLoading(true);
    setError('');
    try {
      const [guestResponse, articlesResponse] = await Promise.all([
        apiClient.get('/api/admin/guest-posts', {
          headers: { 'X-Admin-Key': key },
        }),
        apiClient.get('/api/articles?page=0&size=100', {
          headers: { 'X-Admin-Key': key },
        }),
      ]);
      setGuestPosts(guestResponse.data);
      setArticles(articlesResponse.data.content || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load data. Check your admin key.');
      setGuestPosts([]);
      setArticles([]);
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (id) => {
    setApproving(id);
    try {
      await apiClient.post(`/api/admin/guest-posts/${id}/approve`, {}, {
        headers: { 'X-Admin-Key': adminKey },
      });
      setGuestPosts(guestPosts.filter((post) => post.id !== id));
      alert('Guest post approved and published!');
      fetchData(adminKey);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to approve post');
    } finally {
      setApproving(null);
    }
  };

  const handleDeleteArticle = async (id) => {
    if (!window.confirm('Are you sure you want to delete this article? This cannot be undone.')) {
      return;
    }

    setDeleting(id);
    try {
      await apiClient.delete(`/api/admin/articles/${id}`, {
        headers: { 'X-Admin-Key': adminKey },
      });
      setArticles(articles.filter((article) => article.id !== id));
      alert('Article deleted successfully!');
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete article');
    } finally {
      setDeleting(null);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('adminKey');
    setAdminKey('');
    setIsAuthenticated(false);
    setGuestPosts([]);
    setArticles([]);
  };

  if (!isAuthenticated) {
    return (
      <div className="admin-container">
        <div className="admin-login">
          <h2>Admin Panel</h2>
          <p>Enter your admin key to manage guest post submissions</p>
          <form onSubmit={handleLogin}>
            <input
              type="password"
              placeholder="Admin Key"
              value={adminKey}
              onChange={(e) => setAdminKey(e.target.value)}
              required
            />
            <button type="submit">Login</button>
          </form>
          <p className="hint">
            For development: use <code>dev-admin-key</code>
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="admin-container">
      <div className="admin-header">
        <h2>Admin Dashboard</h2>
        <button onClick={handleLogout} className="logout-btn">
          Logout
        </button>
      </div>

      <ErrorMessage message={error} />

      <div className="admin-tabs">
        <button
          className={`tab-btn ${activeTab === 'pending' ? 'active' : ''}`}
          onClick={() => setActiveTab('pending')}
        >
          Pending Posts ({guestPosts.length})
        </button>
        <button
          className={`tab-btn ${activeTab === 'published' ? 'active' : ''}`}
          onClick={() => setActiveTab('published')}
        >
          Published Articles ({articles.length})
        </button>
      </div>

      {activeTab === 'pending' && (
        <div className="admin-section">
          <h3>Pending Guest Posts</h3>

          {loading && <p>Loading pending posts...</p>}

          {guestPosts.length === 0 && !loading && (
            <p className="no-posts">No pending guest posts</p>
          )}

          <div className="guest-posts-list">
            {guestPosts.map((post) => (
              <div key={post.id} className="guest-post-card">
                <div className="post-content">
                  <h4>{post.title}</h4>
                  <p className="post-meta">
                    <strong>Author:</strong> {post.authorName || 'Anonymous'} |{' '}
                    <strong>Category:</strong> {post.category || 'Uncategorized'} |{' '}
                    <strong>Submitted:</strong> {new Date(post.submittedAt).toLocaleString()}
                  </p>
                  <div className="post-body">
                    <p>{post.body.substring(0, 300)}...</p>
                  </div>
                  {post.documentUrl && (
                    <p className="post-document">
                      📎 Document attached: <code>{post.documentUrl}</code>
                    </p>
                  )}
                </div>
                <div className="post-actions">
                  <button
                    onClick={() => handleApprove(post.id)}
                    disabled={approving === post.id}
                    className="approve-btn"
                  >
                    {approving === post.id ? 'Approving...' : 'Approve & Publish'}
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {activeTab === 'published' && (
        <div className="admin-section">
          <h3>Published Articles</h3>

          {loading && <p>Loading articles...</p>}

          {articles.length === 0 && !loading && (
            <p className="no-posts">No articles published yet</p>
          )}

          <div className="articles-list">
            {articles.map((article) => (
              <div key={article.id} className="article-card-admin">
                <div className="article-content">
                  <h4>{article.title}</h4>
                  <p className="article-meta">
                    <strong>Author:</strong> {article.author} |{' '}
                    <strong>Category:</strong> {article.category || 'Uncategorized'} |{' '}
                    <strong>Published:</strong> {new Date(article.publishedAt).toLocaleString()}
                  </p>
                  <div className="article-body">
                    <p>{article.body.substring(0, 300)}...</p>
                  </div>
                </div>
                <div className="article-actions">
                  <button
                    onClick={() => handleDeleteArticle(article.id)}
                    disabled={deleting === article.id}
                    className="delete-btn"
                  >
                    {deleting === article.id ? 'Deleting...' : '🗑️ Delete'}
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
