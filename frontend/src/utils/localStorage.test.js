import { isLiked, setLiked } from './localStorage';

describe('localStorage utility', () => {
  beforeEach(() => {
    // Clear localStorage before each test
    localStorage.clear();
  });

  describe('isLiked', () => {
    it('should return false when no articles are liked', () => {
      expect(isLiked(1)).toBe(false);
    });

    it('should return true when an article is liked', () => {
      setLiked(1, true);
      expect(isLiked(1)).toBe(true);
    });

    it('should return false when an article is unliked', () => {
      setLiked(1, true);
      setLiked(1, false);
      expect(isLiked(1)).toBe(false);
    });

    it('should handle multiple liked articles', () => {
      setLiked(1, true);
      setLiked(2, true);
      setLiked(3, true);
      expect(isLiked(1)).toBe(true);
      expect(isLiked(2)).toBe(true);
      expect(isLiked(3)).toBe(true);
    });

    it('should handle invalid localStorage data gracefully', () => {
      localStorage.setItem('likedArticles', 'invalid json');
      expect(isLiked(1)).toBe(false);
    });

    it('should handle empty localStorage gracefully', () => {
      localStorage.removeItem('likedArticles');
      expect(isLiked(1)).toBe(false);
    });
  });

  describe('setLiked', () => {
    it('should add an article to liked set', () => {
      setLiked(1, true);
      expect(isLiked(1)).toBe(true);
    });

    it('should remove an article from liked set', () => {
      setLiked(1, true);
      setLiked(1, false);
      expect(isLiked(1)).toBe(false);
    });

    it('should persist multiple liked articles', () => {
      setLiked(1, true);
      setLiked(2, true);
      setLiked(3, true);
      
      const stored = JSON.parse(localStorage.getItem('likedArticles'));
      expect(stored).toHaveLength(3);
      expect(stored).toContain(1);
      expect(stored).toContain(2);
      expect(stored).toContain(3);
    });

    it('should correctly toggle like state', () => {
      setLiked(1, true);
      expect(isLiked(1)).toBe(true);
      
      setLiked(1, false);
      expect(isLiked(1)).toBe(false);
      
      setLiked(1, true);
      expect(isLiked(1)).toBe(true);
    });

    it('should preserve other liked articles when adding/removing', () => {
      setLiked(1, true);
      setLiked(2, true);
      setLiked(3, true);
      
      setLiked(2, false);
      
      expect(isLiked(1)).toBe(true);
      expect(isLiked(2)).toBe(false);
      expect(isLiked(3)).toBe(true);
    });

    it('should handle article IDs as numbers', () => {
      setLiked(123456789, true);
      expect(isLiked(123456789)).toBe(true);
    });
  });
});
