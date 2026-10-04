import React, { useState, useEffect } from 'react';
import apiClient from '../api/apiClient';
import './Reactions.css';

export default function Reactions({ articleId }) {
  const [reactions, setReactions] = useState({
    love: 0,
    like: 0,
    dislike: 0,
    userReaction: null
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchReactions();
  }, [articleId]);

  const fetchReactions = async () => {
    try {
      const response = await apiClient.get(`/api/articles/${articleId}/reactions`);
      setReactions({
        love: response.data.love,
        like: response.data.like,
        dislike: response.data.dislike,
        userReaction: response.data.userReaction
      });
    } catch (error) {
      console.error('Failed to fetch reactions:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleReaction = async (reactionType) => {
    try {
      const response = await apiClient.post(
        `/api/articles/${articleId}/reactions/${reactionType}`
      );
      setReactions({
        love: response.data.love,
        like: response.data.like,
        dislike: response.data.dislike,
        userReaction: response.data.userReaction
      });
    } catch (error) {
      console.error('Failed to add reaction:', error);
    }
  };

  if (loading) return <div className="reactions">Loading...</div>;

  return (
    <div className="reactions">
      <button
        className={`reaction-btn love ${reactions.userReaction === 'LOVE' ? 'active' : ''}`}
        onClick={() => handleReaction('LOVE')}
        title="Love"
      >
        ❤️ {reactions.love}
      </button>
      <button
        className={`reaction-btn like ${reactions.userReaction === 'LIKE' ? 'active' : ''}`}
        onClick={() => handleReaction('LIKE')}
        title="Like"
      >
        👍 {reactions.like}
      </button>
      <button
        className={`reaction-btn dislike ${reactions.userReaction === 'DISLIKE' ? 'active' : ''}`}
        onClick={() => handleReaction('DISLIKE')}
        title="Dislike"
      >
        👎 {reactions.dislike}
      </button>
    </div>
  );
}
