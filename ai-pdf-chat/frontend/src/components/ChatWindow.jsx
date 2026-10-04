import React, { useEffect, useRef, useState } from 'react';
import MessageBubble from './MessageBubble.jsx';
import PdfIcon from './PdfIcon.jsx';
import { askQuestion, deleteDocument, getErrorMessage, formatFileSize } from '../api.js';
import './ChatWindow.css';

// The chat for one PDF: messages on top, the uploaded PDF (with Remove button)
// above the input box, and the input box at the bottom.
//   document:  the selected PDF { id, fileName, fileSizeBytes, ... }
//   onRemoved: called after the PDF was deleted, so the parent can leave the chat
export default function ChatWindow({ document, onRemoved }) {
  const [messages, setMessages] = useState([
    { role: 'assistant', content: 'Ask me anything about "' + document.fileName + '".' },
  ]);
  const [input, setInput] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [isRemoving, setIsRemoving] = useState(false);
  const [error, setError] = useState('');
  const bottomRef = useRef(null);

  // Scroll to the newest message
  useEffect(() => {
    bottomRef.current.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isSending]);

  const sendMessage = async () => {
    const question = input.trim();
    if (question === '' || isSending) return;

    setError('');
    setMessages((old) => [...old, { role: 'user', content: question }]);
    setInput('');
    setIsSending(true);

    try {
      const result = await askQuestion(document.id, question);
      setMessages((old) => [
        ...old,
        { role: 'assistant', content: result.answer, sources: result.sources },
      ]);
    } catch (err) {
      const message = getErrorMessage(err, 'Something went wrong. Please try again.');
      setMessages((old) => [...old, { role: 'assistant', content: message }]);
    } finally {
      setIsSending(false);
    }
  };

  // Remove button: asks the backend to delete the PDF (database + ChromaDB),
  // then tells the parent, which closes the chat.
  const removePdf = async () => {
    if (!window.confirm('Remove "' + document.fileName + '"? You will have to upload it again to ask more questions.')) {
      return;
    }

    setError('');
    setIsRemoving(true);
    try {
      await deleteDocument(document.id);
      onRemoved();
    } catch (err) {
      setError(getErrorMessage(err, 'Could not remove the PDF. Please try again.'));
      setIsRemoving(false);
    }
  };

  // Enter sends the message, Shift+Enter adds a new line
  const handleKeyDown = (event) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      sendMessage();
    }
  };

  return (
    <div className="chat-window">
      <div className="chat-messages">
        {messages.map((message, index) => (
          <MessageBubble
            key={index}
            role={message.role}
            content={message.content}
            sources={message.sources}
          />
        ))}
        {isSending && <MessageBubble role="assistant" isLoading={true} />}
        <div ref={bottomRef} />
      </div>

      {error && <p className="chat-error">{error}</p>}

      {/* The uploaded PDF */}
      <div className="file-bar">
        <PdfIcon size={28} />
        <div className="file-info">
          <span className="file-name">{document.fileName}</span>
          <span className="file-meta">{formatFileSize(document.fileSizeBytes)}</span>
        </div>
        <button
          className="file-remove-btn"
          onClick={removePdf}
          disabled={isRemoving || isSending}
        >
          {isRemoving ? 'Removing...' : 'Remove'}
        </button>
      </div>

      <div className="chat-input-bar">
        <textarea
          className="chat-input"
          placeholder="Ask a question about this PDF..."
          value={input}
          onChange={(event) => setInput(event.target.value)}
          onKeyDown={handleKeyDown}
          rows={1}
        />
        <button
          className="chat-send-btn"
          onClick={sendMessage}
          disabled={isSending || isRemoving || input.trim() === ''}
        >
          Send
        </button>
      </div>
    </div>
  );
}
