import React, { useState, useEffect } from 'react';
import apiClient from '../api/apiClient';
import { isLiked, setLiked } from '../utils/localStorage';

export default function LikeButton({ articleId, initialLikes }) {
  const [likeCount, setLikeCount] = useState(initialLikes || 0);
  const [liked, setLikedState] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLikedState(isLiked(articleId));
  }, [articleId]);

  const handleToggleLike = async () => {
    setLoading(true);
    try {
      if (liked) {
        // Unlike
        const response = await apiClient.delete(`/api/articles/${articleId}/like`);
        setLikeCount(response.data.likesCount);
        setLiked(articleId, false);
        setLikedState(false);
      } else {
        // Like
        const response = await apiClient.post(`/api/articles/${articleId}/like`);
        setLikeCount(response.data.likesCount);
        setLiked(articleId, true);
        setLikedState(true);
      }
    } catch (error) {
      console.error('Error toggling like:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <button
      onClick={handleToggleLike}
      disabled={loading}
      className={`like-button ${liked ? 'liked' : ''}`}
    >
      {liked ? '❤️' : '🤍'} {likeCount}
    </button>
  );
}
