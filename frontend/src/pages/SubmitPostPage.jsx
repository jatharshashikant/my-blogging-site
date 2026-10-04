import React, { useState } from 'react';
import apiClient from '../api/apiClient';
import ErrorMessage from '../components/ErrorMessage';

export default function SubmitPostPage() {
  const [formData, setFormData] = useState({
    title: '',
    body: '',
    authorName: '',
    category: ''
  });
  const [document, setDocument] = useState(null);
  const [errors, setErrors] = useState({});
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
    if (errors[name]) {
      setErrors({ ...errors, [name]: '' });
    }
  };

  const handleFileChange = (e) => {
    setDocument(e.target.files[0] || null);
  };

  const validate = () => {
    const newErrors = {};
    if (!formData.title.trim()) newErrors.title = 'Title is required';
    if (!formData.body.trim()) newErrors.body = 'Body is required';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;

    setLoading(true);
    setSuccess('');
    try {
      const form = new FormData();
      form.append('request', new Blob([JSON.stringify({
        title: formData.title,
        body: formData.body,
        authorName: formData.authorName || null,
        category: formData.category || null
      })], { type: 'application/json' }));
      
      if (document) {
        form.append('document', document);
      }

      await apiClient.post('/api/guest-posts', form);

      setSuccess('Post submitted successfully! Pending admin approval.');
      setFormData({ title: '', body: '', authorName: '', category: '' });
      setDocument(null);
    } catch (error) {
      setErrors({ submit: error.response?.data?.message || 'Failed to submit post' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container form-container">
      <h1>Submit a Guest Post</h1>
      <ErrorMessage message={errors.submit} />
      {success && <div className="success-message">{success}</div>}

      <form onSubmit={handleSubmit} className="guest-form">
        <div className="form-group">
          <label>Title *</label>
          <input
            type="text"
            name="title"
            value={formData.title}
            onChange={handleChange}
            placeholder="Article title"
          />
          {errors.title && <span className="error-text">{errors.title}</span>}
        </div>

        <div className="form-group">
          <label>Body *</label>
          <textarea
            name="body"
            value={formData.body}
            onChange={handleChange}
            placeholder="Article content"
            rows="8"
          />
          {errors.body && <span className="error-text">{errors.body}</span>}
        </div>

        <div className="form-group">
          <label>Author Name</label>
          <input
            type="text"
            name="authorName"
            value={formData.authorName}
            onChange={handleChange}
            placeholder="Your name (optional)"
          />
        </div>

        <div className="form-group">
          <label>Category</label>
          <input
            type="text"
            name="category"
            value={formData.category}
            onChange={handleChange}
            placeholder="Category (optional)"
          />
        </div>

        <div className="form-group">
          <label>Document (PDF, DOC, DOCX - max 10 MB)</label>
          <input
            type="file"
            onChange={handleFileChange}
            accept=".pdf,.doc,.docx"
          />
        </div>

        <button type="submit" disabled={loading} className="submit-btn">
          {loading ? 'Submitting...' : 'Submit Post'}
        </button>
      </form>
    </div>
  );
}
