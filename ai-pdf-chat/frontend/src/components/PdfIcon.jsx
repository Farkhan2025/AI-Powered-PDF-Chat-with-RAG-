import React from 'react';

// Small PDF file icon (drawn with SVG, so it looks the same on every computer)
export default function PdfIcon({ size = 24 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        d="M6 2h8l5 5v13a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z"
        fill="#fee2e2" stroke="#dc2626" strokeWidth="1.4" strokeLinejoin="round"
      />
      <path d="M14 2v5h5" stroke="#dc2626" strokeWidth="1.4" strokeLinejoin="round" />
      <text x="12" y="17" textAnchor="middle" fontSize="6" fontWeight="700" fill="#dc2626"
            fontFamily="Arial, sans-serif">PDF</text>
    </svg>
  );
}
