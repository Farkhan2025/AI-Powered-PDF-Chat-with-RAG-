import React, { useState } from 'react';
import Home from './pages/Home.jsx';
import ChatPage from './pages/ChatPage.jsx';
import './App.css';

// The app has two screens:
//   - Home:     upload a PDF or pick an old one
//   - ChatPage: ask questions about the selected PDF
// "selectedDocument" is the PDF the user is currently chatting with.
// null means no PDF is selected, so we show the Home screen.
export default function App() {
  const [selectedDocument, setSelectedDocument] = useState(null);

  return (
    <div className="app-shell">
      <header className="app-header">
        <span className="brand-name">PDF Chat</span>
      </header>

      <main className="app-main">
        {selectedDocument === null ? (
          <Home onOpenDocument={setSelectedDocument} />
        ) : (
          <ChatPage
            document={selectedDocument}
            onBack={() => setSelectedDocument(null)}
            onRemoved={() => setSelectedDocument(null)}
          />
        )}
      </main>
    </div>
  );
}
