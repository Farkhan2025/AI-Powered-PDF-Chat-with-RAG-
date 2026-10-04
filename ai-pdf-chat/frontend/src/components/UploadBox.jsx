import React, { useRef, useState } from 'react';
import { uploadPdf, getErrorMessage } from '../api.js';
import PdfIcon from './PdfIcon.jsx';
import './UploadBox.css';

const MAX_SIZE_MB = 20; // same limit as spring.servlet.multipart.max-file-size in the backend

// Box where the user drops a PDF or clicks to choose one.
// When the upload is finished, onUploaded(document) is called.
export default function UploadBox({ onUploaded }) {
  const fileInputRef = useRef(null);
  const [isDragging, setIsDragging] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState('');

  const handleFile = async (file) => {
    if (!file) return;

    if (!file.name.toLowerCase().endsWith('.pdf')) {
      setError('Only PDF files are supported.');
      return;
    }
    if (file.size > MAX_SIZE_MB * 1024 * 1024) {
      setError('This PDF is too large. The maximum size is ' + MAX_SIZE_MB + ' MB.');
      return;
    }

    setError('');
    setIsUploading(true);
    setProgress(0);

    try {
      const document = await uploadPdf(file, setProgress);
      onUploaded(document);
    } catch (err) {
      setError(getErrorMessage(err, 'Could not upload the PDF. Please try again.'));
    } finally {
      setIsUploading(false);
      // lets the user choose the same file again after an error
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const handleDrop = (event) => {
    event.preventDefault();
    setIsDragging(false);
    if (isUploading) return;
    handleFile(event.dataTransfer.files[0]);
  };

  return (
    <div
      className={isDragging ? 'upload-box upload-box-dragging' : 'upload-box'}
      onDragOver={(event) => { event.preventDefault(); setIsDragging(true); }}
      onDragLeave={() => setIsDragging(false)}
      onDrop={handleDrop}
      onClick={() => { if (!isUploading) fileInputRef.current.click(); }}
      role="button"
      tabIndex={0}
    >
      <input
        ref={fileInputRef}
        type="file"
        accept="application/pdf"
        hidden
        onChange={(event) => handleFile(event.target.files[0])}
      />

      <div className="upload-icon"><PdfIcon size={44} /></div>

      {isUploading ? (
        <div className="upload-progress-wrap">
          <p className="upload-title">
            {progress < 100 ? 'Uploading...' : 'Processing your PDF. This can take a minute...'}
          </p>
          <div className="upload-progress-bar">
            <div className="upload-progress-fill" style={{ width: progress + '%' }} />
          </div>
          <span className="upload-progress-label">{progress}%</span>
        </div>
      ) : (
        <>
          <p className="upload-title">Drag and drop a PDF here</p>
          <p className="upload-subtitle">or click to choose a file from your computer</p>
        </>
      )}

      {error && <p className="upload-error">{error}</p>}
    </div>
  );
}
