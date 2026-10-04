import axios from 'axios';

// All calls to the Spring Boot backend are in this file.
// The paths start with /api and are forwarded to http://localhost:8080 by vite.config.js.

// POST /api/documents/upload  (the PDF is sent as multipart/form-data, field name "file")
export async function uploadPdf(file, onProgress) {
  const formData = new FormData();
  formData.append('file', file);

  const response = await axios.post('/api/documents/upload', formData, {
    onUploadProgress: (event) => {
      if (event.total) {
        onProgress(Math.round((event.loaded * 100) / event.total));
      }
    },
  });
  return response.data; // the saved Document: { id, fileName, fileSizeBytes, totalChunks, status, uploadedAt }
}

// GET /api/documents
export async function getDocuments() {
  const response = await axios.get('/api/documents');
  return response.data;
}

// DELETE /api/documents/{id}
export async function deleteDocument(documentId) {
  await axios.delete('/api/documents/' + documentId);
}

// POST /api/chat   body: { documentId, question }
export async function askQuestion(documentId, question) {
  const response = await axios.post('/api/chat', { documentId, question });
  return response.data; // { answer, sources }
}

// Turns an axios error into a message we can show to the user
export function getErrorMessage(error, fallback) {
  if (error.response && error.response.status === 413) {
    return 'This PDF is too large. The maximum size is 20 MB.';
  }
  if (error.response && error.response.data && error.response.data.message) {
    return error.response.data.message;
  }
  return fallback;
}

// 1536 bytes -> "1.5 KB"
export function formatFileSize(bytes) {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}
