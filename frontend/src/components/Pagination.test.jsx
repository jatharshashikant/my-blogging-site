import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import Pagination from './Pagination';

describe('Pagination Component', () => {
  describe('Rendering', () => {
    it('should render Prev and Next buttons', () => {
      const mockOnPageChange = jest.fn();
      render(
        <Pagination currentPage={0} totalPages={5} onPageChange={mockOnPageChange} />
      );
      
      expect(screen.getByText(/← Prev/)).toBeInTheDocument();
      expect(screen.getByText(/Next →/)).toBeInTheDocument();
    });

    it('should display current page info correctly', () => {
      const mockOnPageChange = jest.fn();
      render(
        <Pagination currentPage={0} totalPages={5} onPageChange={mockOnPageChange} />
      );
      
      expect(screen.getByText('Page 1 of 5')).toBeInTheDocument();
    });

    it('should display correct page number for non-zero pages', () => {
      const mockOnPageChange = jest.fn();
      render(
        <Pagination currentPage={2} totalPages={5} onPageChange={mockOnPageChange} />
      );
      
      expect(screen.getByText('Page 3 of 5')).toBeInTheDocument();
    });
  });

  describe('Button Disabling', () => {
    it('should disable Prev button on first page', () => {
      const mockOnPageChange = jest.fn();
      render(
        <Pagination currentPage={0} totalPages={5} onPageChange={mockOnPageChange} />
      );
      
      const prevBtn = screen.getByText(/← Prev/).closest('button');
      expect(prevBtn).toBeDisabled();
    });

    it('should disable Next button on last page', () => {
      const mockOnPageChange = jest.fn();
      render(
        <Pagination currentPage={4} totalPages={5} onPageChange={mockOnPageChange} />
      );
      
      const nextBtn = screen.getByText(/Next →/).closest('button');
      expect(nextBtn).toBeDisabled();
    });

    it('should enable both buttons on middle page', () => {
      const mockOnPageChange = jest.fn();
      render(
        <Pagination currentPage={2} totalPages={5} onPageChange={mockOnPageChange} />
      );
      
      const prevBtn = screen.getByText(/← Prev/).closest('button');
      const nextBtn = screen.getByText(/Next →/).closest('button');
      
      expect(prevBtn).not.toBeDisabled();
      expect(nextBtn).not.toBeDisabled();
    });

    it('should disable both buttons when there is only one page', () => {
      const mockOnPageChange = jest.fn();
      render(
        <Pagination currentPage={0} totalPages={1} onPageChange={mockOnPageChange} />
      );
      
      const prevBtn = screen.getByText(/← Prev/).closest('button');
      const nextBtn = screen.getByText(/Next →/).closest('button');
      
      expect(prevBtn).toBeDisabled();
      expect(nextBtn).toBeDisabled();
    });

    it('should enable Prev button when not on first page', () => {
      const mockOnPageChange = jest.fn();
      render(
        <Pagination currentPage={1} totalPages={5} onPageChange={mockOnPageChange} />
      );
      
      const prevBtn = screen.getByText(/← Prev/).closest('button');
      expect(prevBtn).not.toBeDisabled();
    });

    it('should enable Next button when not on last page', () => {
      const mockOnPageChange = jest.fn();
      render(
        <Pagination currentPage={3} totalPages={5} onPageChange={mockOnPageChange} />
      );
      
      const nextBtn = screen.getByText(/Next →/).closest('button');
      expect(nextBtn).not.toBeDisabled();
    });
  });

  describe('Callback Handling', () => {
    it('should call onPageChange with previous page when Prev clicked', () => {
      const mockOnPageChange = jest.fn();
      render(
        <Pagination currentPage={2} totalPages={5} onPageChange={mockOnPageChange} />
      );
      
      const prevBtn = screen.getByText(/← Prev/).closest('button');
      fireEvent.click(prevBtn);
      
      expect(mockOnPageChange).toHaveBeenCalledWith(1);
    });

    it('should call onPageChange with next page when Next clicked', () => {
      const mockOnPageChange = jest.fn();
      render(
        <Pagination currentPage={2} totalPages={5} onPageChange={mockOnPageChange} />
      );
      
      const nextBtn = screen.getByText(/Next →/).closest('button');
      fireEvent.click(nextBtn);
      
      expect(mockOnPageChange).toHaveBeenCalledWith(3);
    });

    it('should not call onPageChange when disabled Prev button is clicked', () => {
      const mockOnPageChange = jest.fn();
      render(
        <Pagination currentPage={0} totalPages={5} onPageChange={mockOnPageChange} />
      );
      
      const prevBtn = screen.getByText(/← Prev/).closest('button');
      fireEvent.click(prevBtn);
      
      expect(mockOnPageChange).not.toHaveBeenCalled();
    });

    it('should not call onPageChange when disabled Next button is clicked', () => {
      const mockOnPageChange = jest.fn();
      render(
        <Pagination currentPage={4} totalPages={5} onPageChange={mockOnPageChange} />
      );
      
      const nextBtn = screen.getByText(/Next →/).closest('button');
      fireEvent.click(nextBtn);
      
      expect(mockOnPageChange).not.toHaveBeenCalled();
    });
  });

  describe('Edge Cases', () => {
    it('should handle zero-based page indexing correctly', () => {
      const mockOnPageChange = jest.fn();
      render(
        <Pagination currentPage={0} totalPages={1} onPageChange={mockOnPageChange} />
      );
      
      expect(screen.getByText('Page 1 of 1')).toBeInTheDocument();
    });

    it('should handle large page numbers', () => {
      const mockOnPageChange = jest.fn();
      render(
        <Pagination currentPage={99} totalPages={100} onPageChange={mockOnPageChange} />
      );
      
      expect(screen.getByText('Page 100 of 100')).toBeInTheDocument();
      const nextBtn = screen.getByText(/Next →/).closest('button');
      expect(nextBtn).toBeDisabled();
    });
  });
});
