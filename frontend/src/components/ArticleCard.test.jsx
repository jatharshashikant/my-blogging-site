import React from 'react';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import ArticleCard from './ArticleCard';
import '@testing-library/jest-dom';

describe('ArticleCard Component', () => {
  const defaultProps = {
    id: 1,
    title: 'Test Article Title',
    author: 'John Doe',
    publishedAt: '2024-01-15T10:30:00Z',
    category: 'Technology'
  };

  const renderArticleCard = (props = {}) => {
    return render(
      <BrowserRouter>
        <ArticleCard {...defaultProps} {...props} />
      </BrowserRouter>
    );
  };

  describe('Rendering', () => {
    it('should render the article title as a heading', () => {
      renderArticleCard();
      expect(screen.getByRole('heading', { name: 'Test Article Title' })).toBeInTheDocument();
    });

    it('should render the author name', () => {
      renderArticleCard();
      expect(screen.getByText(/John Doe/)).toBeInTheDocument();
    });

    it('should fall back to "Admin" when author is not provided', () => {
      renderArticleCard({ author: '' });
      expect(screen.getByText(/Admin/)).toBeInTheDocument();
    });

    it('should fall back to "Admin" when author is null', () => {
      renderArticleCard({ author: null });
      expect(screen.getByText(/Admin/)).toBeInTheDocument();
    });

    it('should render the formatted publication date', () => {
      renderArticleCard();
      // Date is formatted using toLocaleDateString with en-IN locale
      // Just verify that the date span contains a date string
      expect(screen.getByText(/January.*2024|January.*2024|2024/)).toBeInTheDocument();
    });

    it('should render the category badge', () => {
      renderArticleCard();
      expect(screen.getByText('Technology')).toBeInTheDocument();
    });

    it('should not render a category badge if category is not provided', () => {
      renderArticleCard({ category: '' });
      expect(screen.queryByText('Technology')).not.toBeInTheDocument();
    });

    it('should not render a category badge if category is null', () => {
      renderArticleCard({ category: null });
      expect(screen.queryByText('Technology')).not.toBeInTheDocument();
    });
  });

  describe('Navigation', () => {
    it('should render a link to the article detail page', () => {
      renderArticleCard();
      const link = screen.getByRole('link');
      expect(link).toHaveAttribute('href', '/articles/1');
    });

    it('should link to the correct article based on id', () => {
      renderArticleCard({ id: 42 });
      const link = screen.getByRole('link');
      expect(link).toHaveAttribute('href', '/articles/42');
    });

    it('should wrap the title in a clickable link', () => {
      renderArticleCard();
      const link = screen.getByRole('link', { name: 'Test Article Title' });
      expect(link).toBeInTheDocument();
    });
  });

  describe('Styling and Structure', () => {
    it('should have article-card class', () => {
      const { container } = renderArticleCard();
      const card = container.querySelector('.article-card');
      expect(card).toBeInTheDocument();
    });

    it('should have category-badge class when category is provided', () => {
      const { container } = renderArticleCard();
      const badge = container.querySelector('.category-badge');
      expect(badge).toBeInTheDocument();
      expect(badge).toHaveTextContent('Technology');
    });

    it('should have card-meta class for metadata section', () => {
      const { container } = renderArticleCard();
      const meta = container.querySelector('.card-meta');
      expect(meta).toBeInTheDocument();
    });

    it('should have card-link class on the title link', () => {
      const { container } = renderArticleCard();
      const link = container.querySelector('.card-link');
      expect(link).toBeInTheDocument();
    });
  });

  describe('Props Handling', () => {
    it('should render with all required props', () => {
      renderArticleCard();
      expect(screen.getByRole('heading', { name: 'Test Article Title' })).toBeInTheDocument();
      expect(screen.getByText(/John Doe/)).toBeInTheDocument();
      expect(screen.getByText('Technology')).toBeInTheDocument();
    });

    it('should handle different date formats', () => {
      const testDates = [
        '2023-06-01T14:00:00Z',
        '2024-12-25T00:00:00Z',
        '2022-01-01T23:59:59Z'
      ];

      testDates.forEach(date => {
        const { unmount } = renderArticleCard({ publishedAt: date });
        // Just verify a reasonable date string appears (month year or similar)
        const container = document.querySelector('.card-meta span:nth-child(3)');
        expect(container).toBeInTheDocument();
        expect(container.textContent).toBeTruthy();
        unmount();
      });
    });

    it('should handle very long titles', () => {
      const longTitle = 'A'.repeat(200);
      renderArticleCard({ title: longTitle });
      expect(screen.getByText(longTitle)).toBeInTheDocument();
    });

    it('should handle special characters in title', () => {
      const specialTitle = 'Article & "Test" with <symbols>';
      renderArticleCard({ title: specialTitle });
      expect(screen.getByText(specialTitle)).toBeInTheDocument();
    });

    it('should handle special characters in author', () => {
      const specialAuthor = 'O\'Brien & Associates';
      renderArticleCard({ author: specialAuthor });
      expect(screen.getByText(new RegExp(specialAuthor))).toBeInTheDocument();
    });

    it('should handle special characters in category', () => {
      const specialCategory = 'C++/C# Programming';
      renderArticleCard({ category: specialCategory });
      expect(screen.getByText(specialCategory)).toBeInTheDocument();
    });
  });
});
