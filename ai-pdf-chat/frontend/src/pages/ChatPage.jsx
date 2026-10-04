import React from 'react';
import ChatWindow from '../components/ChatWindow.jsx';
import './ChatPage.css';

// Screen for chatting with one PDF.
//   onBack:    go back to Home (the PDF stays saved)
//   onRemoved: the PDF was deleted, go back to Home
export default function ChatPage({ document, onBack, onRemoved }) {
  return (
    <div className="chat-page">
      <div className="chat-page-header">
        <button className="back-btn" onClick={onBack}>Back</button>
        <h2 className="chat-page-title">{document.fileName}</h2>
      </div>

      <div className="chat-page-body">
        <ChatWindow document={document} onRemoved={onRemoved} />
      </div>
    </div>
  );
}
