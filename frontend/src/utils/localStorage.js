export function isLiked(articleId) {
  const likedStr = localStorage.getItem('likedArticles');
  if (!likedStr) return false;
  try {
    const liked = JSON.parse(likedStr);
    return liked.has ? liked.has(articleId) : Array.isArray(liked) && liked.includes(articleId);
  } catch {
    return false;
  }
}

export function setLiked(articleId, liked) {
  let likedSet;
  const likedStr = localStorage.getItem('likedArticles');
  
  if (likedStr) {
    try {
      const parsed = JSON.parse(likedStr);
      likedSet = Array.isArray(parsed) ? new Set(parsed) : parsed instanceof Set ? parsed : new Set();
    } catch {
      likedSet = new Set();
    }
  } else {
    likedSet = new Set();
  }

  if (liked) {
    likedSet.add(articleId);
  } else {
    likedSet.delete(articleId);
  }

  localStorage.setItem('likedArticles', JSON.stringify(Array.from(likedSet)));
}
