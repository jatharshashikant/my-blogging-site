import React from 'react';
import { Link } from 'react-router-dom';

export default function AboutPage() {
  return (
    <div className="container about-page">
      <h1>About My Blog</h1>
      <p>
        Welcome to my blogging platform! Here you'll find articles on a variety of topics,
        and you're invited to submit your own guest posts.
      </p>
      <h2>How It Works</h2>
      <ul>
        <li>Browse published articles on the home page</li>
        <li>Like articles to show appreciation</li>
        <li>Search for topics you're interested in</li>
        <li>Submit your own guest post for admin approval</li>
      </ul>
      <h2>Get In Touch</h2>
      <p>
        Have a story to share? <Link to="/submit">Submit a guest post</Link> and join our community of writers!
      </p>
    </div>
  );
}
