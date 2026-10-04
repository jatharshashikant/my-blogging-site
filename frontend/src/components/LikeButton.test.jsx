import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import LikeButton from './LikeButton';
import * as localStorage from '../utils/localStorage';
import apiClient from '../api/apiClient';

// Mock the dependencies
jest.mock('../api/apiClient');
jest.mock('../utils/localStorage');

describe('LikeButton Component', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Default mock implementations
    localStorage.isLiked.mockReturnValue(false);
  });

  test('renders with initial like count', () => {
    render(<LikeButton articleId={1} initialLikes={42} />);
    const button = screen.getByRole('button');
    expect(button).toHaveTextContent('42');
  });

  test('renders with heart emoji when not liked', () => {
    render(<LikeButton articleId={1} initialLikes={10} />);
    const button = screen.getByRole('button');
    expect(button).toHaveTextContent('🤍');
  });

  test('renders with filled heart emoji when liked', () => {
    localStorage.isLiked.mockReturnValue(true);
    render(<LikeButton articleId={1} initialLikes={10} />);
    const button = screen.getByRole('button');
    expect(button).toHaveTextContent('❤️');
  });

  test('sends POST request when clicking like button while not liked', async () => {
    const articleId = 1;
    apiClient.post.mockResolvedValue({ data: { likesCount: 11 } });
    
    render(<LikeButton articleId={articleId} initialLikes={10} />);
    const button = screen.getByRole('button');
    
    fireEvent.click(button);
    
    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith(`/api/articles/${articleId}/like`);
    });
  });

  test('sends DELETE request when clicking unlike button while liked', async () => {
    const articleId = 1;
    localStorage.isLiked.mockReturnValue(true);
    apiClient.delete.mockResolvedValue({ data: { likesCount: 9 } });
    
    render(<LikeButton articleId={articleId} initialLikes={10} />);
    const button = screen.getByRole('button');
    
    fireEvent.click(button);
    
    await waitFor(() => {
      expect(apiClient.delete).toHaveBeenCalledWith(`/api/articles/${articleId}/like`);
    });
  });

  test('updates like count after successful like', async () => {
    const articleId = 1;
    apiClient.post.mockResolvedValue({ data: { likesCount: 11 } });
    
    render(<LikeButton articleId={articleId} initialLikes={10} />);
    const button = screen.getByRole('button');
    
    fireEvent.click(button);
    
    await waitFor(() => {
      expect(button).toHaveTextContent('11');
    });
  });

  test('updates like count after successful unlike', async () => {
    const articleId = 1;
    localStorage.isLiked.mockReturnValue(true);
    apiClient.delete.mockResolvedValue({ data: { likesCount: 9 } });
    
    render(<LikeButton articleId={articleId} initialLikes={10} />);
    const button = screen.getByRole('button');
    
    fireEvent.click(button);
    
    await waitFor(() => {
      expect(button).toHaveTextContent('9');
    });
  });

  test('calls setLiked with true after successful like', async () => {
    const articleId = 1;
    apiClient.post.mockResolvedValue({ data: { likesCount: 11 } });
    
    render(<LikeButton articleId={articleId} initialLikes={10} />);
    const button = screen.getByRole('button');
    
    fireEvent.click(button);
    
    await waitFor(() => {
      expect(localStorage.setLiked).toHaveBeenCalledWith(articleId, true);
    });
  });

  test('calls setLiked with false after successful unlike', async () => {
    const articleId = 1;
    localStorage.isLiked.mockReturnValue(true);
    apiClient.delete.mockResolvedValue({ data: { likesCount: 9 } });
    
    render(<LikeButton articleId={articleId} initialLikes={10} />);
    const button = screen.getByRole('button');
    
    fireEvent.click(button);
    
    await waitFor(() => {
      expect(localStorage.setLiked).toHaveBeenCalledWith(articleId, false);
    });
  });

  test('disables button while request is in progress', async () => {
    const articleId = 1;
    apiClient.post.mockImplementation(
      () => new Promise(resolve => setTimeout(() => resolve({ data: { likesCount: 11 } }), 100))
    );
    
    render(<LikeButton articleId={articleId} initialLikes={10} />);
    const button = screen.getByRole('button');
    
    fireEvent.click(button);
    expect(button).toBeDisabled();
    
    await waitFor(() => {
      expect(button).not.toBeDisabled();
    });
  });

  test('handles API error gracefully', async () => {
    const articleId = 1;
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();
    apiClient.post.mockRejectedValue(new Error('Network error'));
    
    render(<LikeButton articleId={articleId} initialLikes={10} />);
    const button = screen.getByRole('button');
    
    fireEvent.click(button);
    
    await waitFor(() => {
      expect(consoleErrorSpy).toHaveBeenCalledWith('Error toggling like:', expect.any(Error));
      // Button should be re-enabled
      expect(button).not.toBeDisabled();
    });
    
    consoleErrorSpy.mockRestore();
  });

  test('toggles between liked and not liked states', async () => {
    const articleId = 1;
    let isLikedState = false;
    
    localStorage.isLiked.mockImplementation(() => isLikedState);
    
    apiClient.post.mockImplementation(async () => {
      isLikedState = true;
      return { data: { likesCount: 11 } };
    });
    
    apiClient.delete.mockImplementation(async () => {
      isLikedState = false;
      return { data: { likesCount: 10 } };
    });
    
    const { rerender } = render(<LikeButton articleId={articleId} initialLikes={10} />);
    let button = screen.getByRole('button');
    
    // Like it
    fireEvent.click(button);
    await waitFor(() => {
      expect(localStorage.setLiked).toHaveBeenCalledWith(articleId, true);
    });
    
    // Rerender to see updated state
    rerender(<LikeButton articleId={articleId} initialLikes={11} />);
    button = screen.getByRole('button');
    expect(button).toHaveTextContent('11');
  });

  test('handles default initialLikes value', () => {
    render(<LikeButton articleId={1} />);
    const button = screen.getByRole('button');
    expect(button).toHaveTextContent('0');
  });

  test('reads liked state from localStorage on mount', () => {
    const articleId = 42;
    localStorage.isLiked.mockReturnValue(false);
    
    render(<LikeButton articleId={articleId} initialLikes={5} />);
    
    expect(localStorage.isLiked).toHaveBeenCalledWith(articleId);
  });

  test('button has correct CSS classes when not liked', () => {
    localStorage.isLiked.mockReturnValue(false);
    render(<LikeButton articleId={1} initialLikes={10} />);
    const button = screen.getByRole('button');
    expect(button).toHaveClass('like-button');
    expect(button).not.toHaveClass('liked');
  });

  test('button has correct CSS classes when liked', () => {
    localStorage.isLiked.mockReturnValue(true);
    render(<LikeButton articleId={1} initialLikes={10} />);
    const button = screen.getByRole('button');
    expect(button).toHaveClass('like-button');
    expect(button).toHaveClass('liked');
  });
});
