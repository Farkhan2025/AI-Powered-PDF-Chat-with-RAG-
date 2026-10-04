import React, { useEffect, useState } from 'react';
import UploadBox from '../components/UploadBox.jsx';
import PdfIcon from '../components/PdfIcon.jsx';
import { getDocuments, deleteDocument, getErrorMessage, formatFileSize } from '../api.js';
import './Home.css';

// First screen: upload a new PDF, or open one that was uploaded before.
//   onOpenDocument(document): called when the user picks a PDF to chat with
export default function Home({ onOpenDocument }) {
  const [documents, setDocuments] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  // Load the list of PDFs once, when the screen opens
  useEffect(() => {
    loadDocuments();
  }, []);

  const loadDocuments = async () => {
    try {
      const list = await getDocuments();
      setDocuments(list);
    } catch (err) {
      setError('Could not load your documents. Is the backend running?');
    } finally {
      setIsLoading(false);
    }
  };

  const removeDocument = async (event, document) => {
    event.stopPropagation(); // do not open the chat when Remove is clicked
    if (!window.confirm('Remove "' + document.fileName + '"?')) {
      return;
    }
    try {
      await deleteDocument(document.id);
      setDocuments(documents.filter((item) => item.id !== document.id));
    } catch (err) {
      setError(getErrorMessage(err, 'Could not remove the PDF. Please try again.'));
    }
  };

  return (
    <div className="home-page">
      <section className="home-hero">
        <h1>Chat with your PDF</h1>
        <p>Upload a PDF and ask questions about its content.</p>
      </section>

      <UploadBox onUploaded={onOpenDocument} />

      <section className="document-list-section">
        <h2>Your documents</h2>

        {error && <p className="home-error">{error}</p>}
        {isLoading && <p className="muted">Loading...</p>}
        {!isLoading && documents.length === 0 && !error && (
          <p className="muted">No documents yet. Upload a PDF to get started.</p>
        )}

        <ul className="document-list">
          {documents.map((document) => (
            <li
              key={document.id}
              className={document.status === 'READY' ? 'document-item' : 'document-item document-item-disabled'}
              onClick={() => { if (document.status === 'READY') onOpenDocument(document); }}
            >
              <div className="document-item-main">
                <PdfIcon size={28} />
                <div>
                  <p className="document-item-name">{document.fileName}</p>
                  <p className="document-item-meta">
                    {formatFileSize(document.fileSizeBytes)} · {new Date(document.uploadedAt).toLocaleDateString()}
                    {document.status !== 'READY' && ' · ' + document.status.toLowerCase()}
                  </p>
                </div>
              </div>
              <button className="document-remove-btn" onClick={(event) => removeDocument(event, document)}>
                Remove
              </button>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
