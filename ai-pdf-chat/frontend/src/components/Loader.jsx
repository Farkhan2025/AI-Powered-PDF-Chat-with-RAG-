import React from 'react';
import './Loader.css';

// Three bouncing dots, shown while we wait for the answer
export default function Loader() {
  return (
    <div className="loader-dots" aria-label="Loading">
      <span></span>
      <span></span>
      <span></span>
    </div>
  );
}
