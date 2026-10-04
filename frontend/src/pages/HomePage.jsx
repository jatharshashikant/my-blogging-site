import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import apiClient from '../api/apiClient';
import ArticleCard from '../components/ArticleCard';
import Pagination from '../components/Pagination';
import ErrorMessage from '../components/ErrorMessage';

export default function HomePage() {
  const [searchParams] = useSearchParams();
  const [articles, setArticles] = useState([]);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const query = searchParams.get('q');

  useEffect(() => {
    setCurrentPage(0);
  }, [query]);

  useEffect(() => {
    const fetchArticles = async () => {
      setLoading(true);
      setError('');
      try {
        if (query) {
          const response = await apiClient.get(`/api/articles/search?q=${encodeURIComponent(query)}`);
          setArticles(response.data);
          setTotalPages(1);
        } else {
          const response = await apiClient.get(`/api/articles?page=${currentPage}&size=10`);
          setArticles(response.data.content);
          setTotalPages(response.data.totalPages);
        }
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load articles');
        setArticles([]);
      } finally {
        setLoading(false);
      }
    };

    fetchArticles();
  }, [query, currentPage]);

  if (loading) return <div className="container"><p>Loading...</p></div>;

  return (
    <div className="container">
      <ErrorMessage message={error} />
      
      {articles.length === 0 ? (
        <p className="no-articles">
          {query ? 'No results found' : 'No articles yet'}
        </p>
      ) : (
        <>
          <div className="articles-list">
            {articles.map((article) => (
              <ArticleCard
                key={article.id}
                id={article.id}
                title={article.title}
                author={article.author}
                publishedAt={article.publishedAt}
                category={article.category}
                body={article.body}
              />
            ))}
          </div>
          {!query && <Pagination currentPage={currentPage} totalPages={totalPages} onPageChange={setCurrentPage} />}
        </>
      )}
    </div>
  );
}
