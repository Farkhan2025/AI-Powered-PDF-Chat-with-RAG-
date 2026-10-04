import React from 'react';
import Loader from './Loader.jsx';
import './MessageBubble.css';

// One chat message.
//   role:      'user' or 'assistant'
//   content:   the text
//   isLoading: show the dots instead of text
//   sources:   short pieces of the PDF that were used for the answer
export default function MessageBubble({ role, content, isLoading, sources }) {
  const isUser = role === 'user';

  return (
    <div className={isUser ? 'message-row message-row-user' : 'message-row message-row-ai'}>
      <div className={isUser ? 'message-bubble bubble-user' : 'message-bubble bubble-ai'}>
        {isLoading ? <Loader /> : <p className="message-text">{content}</p>}

        {!isLoading && sources && sources.length > 0 && (
          <details className="message-sources">
            <summary>View sources ({sources.length})</summary>
            <ul>
              {sources.map((source, index) => (
                <li key={index}>{source}</li>
              ))}
            </ul>
          </details>
        )}
      </div>
    </div>
  );
}
