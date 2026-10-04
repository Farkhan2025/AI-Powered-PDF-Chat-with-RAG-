
# AI PDF Chat

AI PDF Chat is a full-stack application that lets you upload a PDF and ask questions about its content.

The application uses **Retrieval-Augmented Generation (RAG)** to retrieve the most relevant parts of the selected PDF before generating an answer. The AI workflow runs locally using **Ollama**, so no cloud AI API key is required.

---

## 1. What you need

| Software | Version | Check with |
|---|---|---|
| Java (JDK) | 17 or newer | `java -version` |
| Maven | 3.8 or newer | `mvn -version` |
| Node.js | 18 or newer | `node -v` |
| npm | Included with Node.js | `npm -v` |
| Ollama | Latest | `ollama --version` |
| Docker | Recent version | `docker --version` |

---

## 2. Technology Stack

### Backend

- Java 17
- Spring Boot 3.5.16
- Spring Web
- Spring Data JPA
- Spring AI 1.0.9
- Lombok
- Apache PDFBox 3.0.3
- Maven

### AI and RAG

- Ollama
- Llama 3.2
- `nomic-embed-text`
- Retrieval-Augmented Generation (RAG)
- Semantic similarity search

### Databases

- H2 Database
- ChromaDB

### Frontend

- React 18
- Vite
- JavaScript
- Axios
- HTML
- CSS

### Development and Deployment

- Docker
- Docker Compose
- Git

---

## 3. Features

- Upload PDF documents through the web interface.
- Validate uploaded PDF files before processing.
- Extract text from PDFs using Apache PDFBox.
- Clean extracted text before processing.
- Split PDF text into overlapping chunks.
- Generate embeddings using `nomic-embed-text`.
- Store chunks, embeddings, and metadata in ChromaDB.
- Ask questions about a selected PDF using natural language.
- Retrieve the most relevant chunks using semantic search.
- Generate answers using Llama 3.2 through Ollama.
- Return source excerpts used for the answer.
- Store PDF information in an H2 database.
- View all uploaded documents.
- Remove a document and its associated vector data.
- Process the AI workflow locally without an external AI API key.

---

## 4. How It Works

The application follows a Retrieval-Augmented Generation (RAG) workflow.

```text
                    PDF Upload
                         |
                         v
                PDF Text Extraction
                     (PDFBox)
                         |
                         v
                   Text Cleaning
                         |
                         v
                  Text Chunking
                         |
                         v
              Generate Embeddings
              (nomic-embed-text)
                         |
                         v
                     ChromaDB
               Chunks + Embeddings
                         |
                         |
                  User Question
                         |
                         v
                 Semantic Search
                     ChromaDB
                         |
                         v
              Relevant PDF Chunks
                         |
                         v
              Context + Question
                         |
                         v
                   Llama 3.2
                    (Ollama)
                         |
                         v
                      Answer
                         |
                         v
                  React Frontend
```

The important idea is that the complete PDF is not sent to the language model for every question. The application first searches the selected PDF for the most relevant chunks and sends those chunks as context to the model.

---

## 5. RAG Pipeline

### Step 1: Upload the PDF

The React frontend sends the PDF to:

```text
POST /api/documents/upload
```

The backend validates that:

- A file was selected.
- The file is not empty.
- The file has a `.pdf` extension.

The document is initially stored with the status:

```text
PROCESSING
```

---

### Step 2: Extract Text

Apache PDFBox reads the uploaded PDF and extracts text from its pages.

The application also cleans the extracted text by removing unnecessary spaces and excessive blank lines.

If no readable text is found, the upload fails with a message indicating that the PDF may be scanned or contain images only.

---

### Step 3: Split Text into Chunks

The extracted text is divided into smaller chunks.

Current configuration:

```text
Chunk size    : 800 words
Chunk overlap : 100 words
```

The overlap allows neighboring chunks to share some content so that information near a chunk boundary is less likely to lose context.

---

### Step 4: Generate Embeddings

Each text chunk is converted into a numerical representation called an embedding.

The application uses:

```text
nomic-embed-text
```

through Ollama.

The embedding represents the semantic meaning of the text and allows the application to compare the meaning of a question with the meaning of the document chunks.

---

### Step 5: Store Data in ChromaDB

The chunks and their embeddings are stored in ChromaDB.

Each stored chunk also contains metadata:

```text
documentId
fileName
chunkNumber
```

The `documentId` is used to ensure that questions are searched only against the selected PDF.

---

### Step 6: Ask a Question

When a user asks a question, the question is sent to the backend.

The application performs semantic similarity search in ChromaDB and retrieves the most relevant chunks.

The current configuration retrieves:

```text
Top K = 5
```

relevant chunks.

---

### Step 7: Generate the Answer

The retrieved chunks are combined with the user's question.

The backend sends this context to Llama 3.2 through Ollama.

The system instruction tells the model to:

```text
Answer only using the provided document context.
If the answer is not available in the document, say you don't know.
```

The generated answer and short source excerpts are then returned to the React frontend.

---

## 6. Architecture

```text
                         React Frontend
                              |
                              | REST API
                              v
                       Spring Boot Backend
                              |
              +---------------+---------------+
              |               |               |
              v               v               v
          PDFBox         Spring AI          Spring Data JPA
              |               |               |
              |               |               v
              |               |          H2 Database
              |               |
              |       +-------+-------+
              |       |               |
              |       v               v
              |    Ollama          ChromaDB
              |       |
              |   +---+---+
              |   |       |
              |   v       v
              | Llama 3.2  nomic-embed-text
              |
              v
         PDF Text Extraction
```

---

## 7. Project Structure

```text
ai-pdf-chat/
├── docker-compose.yml
├── README.md
│
├── backend/
│   ├── pom.xml
│   │
│   └── src/main/
│       ├── resources/
│       │   └── application.properties
│       │
│       └── java/com/aipdfchat/
│           ├── AiPdfChatApplication.java
│           │
│           ├── controller/
│           │   ├── DocumentController.java
│           │   └── ChatController.java
│           │
│           ├── service/
│           │   ├── PdfService.java
│           │   ├── ChunkService.java
│           │   ├── VectorService.java
│           │   └── ChatService.java
│           │
│           ├── entity/
│           │   └── Document.java
│           │
│           └── repository/
│               └── DocumentRepository.java
│
└── frontend/
    ├── package.json
    ├── vite.config.js
    ├── index.html
    │
    └── src/
        ├── main.jsx
        ├── App.jsx
        ├── api.js
        │
        ├── pages/
        │   ├── Home.jsx
        │   └── ChatPage.jsx
        │
        └── components/
            ├── UploadBox.jsx
            ├── ChatWindow.jsx
            ├── MessageBubble.jsx
            ├── Loader.jsx
            └── PdfIcon.jsx
```

---

## 8. Backend Components

### DocumentController

Provides the REST APIs for PDF management.

```text
POST   /api/documents/upload
GET    /api/documents
DELETE /api/documents/{id}
```

It handles validation errors and processing errors and returns appropriate HTTP responses.

### ChatController

Provides the API for asking questions about a selected document.

```text
POST /api/chat
```

The request contains:

```json
{
  "documentId": "document-id",
  "question": "What is this document about?"
}
```

### PdfService

Responsible for the complete PDF processing workflow:

```text
Validate PDF
     |
     v
Save document details
     |
     v
Extract text using PDFBox
     |
     v
Split text into chunks
     |
     v
Generate embeddings and store chunks
     |
     v
Update document status to READY
```

If processing fails, the document status is changed to:

```text
FAILED
```

### ChunkService

Splits extracted PDF text into smaller overlapping chunks.

The values are loaded from `application.properties`:

```text
app.chunk-size=800
app.chunk-overlap=100
```

### VectorService

Handles interaction with the vector store through Spring AI.

It is responsible for:

- Creating chunk documents with metadata.
- Adding chunks to ChromaDB.
- Performing semantic similarity search.
- Filtering search results by `documentId`.
- Deleting all chunks belonging to a document.

Spring AI communicates with Ollama to generate embeddings when documents are added and when questions are searched.

### ChatService

Handles the question-answering part of the RAG pipeline.

```text
Question
   |
   v
Semantic Search
   |
   v
Relevant Chunks
   |
   v
Context + Question
   |
   v
Llama 3.2
   |
   v
Answer + Sources
```

The service also verifies that the selected document exists and has a `READY` status before processing a question.

### Document Entity

The `Document` entity represents the `documents` table in the H2 database.

It contains:

```text
id
fileName
fileSizeBytes
totalChunks
status
uploadedAt
```

The entity uses Lombok's `@Data` annotation for common methods such as getters and setters.

### DocumentRepository

The repository extends:

```text
JpaRepository<Document, String>
```

This provides database operations such as saving, finding, listing, and deleting documents without manually writing SQL queries.

---

## 9. Database Design

The application uses H2 and ChromaDB for different purposes.

### H2 Database

H2 stores information about the uploaded PDF documents.

```text
documents
├── id
├── fileName
├── fileSizeBytes
├── totalChunks
├── status
└── uploadedAt
```

The H2 database is configured as a file-based database:

```text
./data/pdfchat
```

### ChromaDB

ChromaDB stores the document chunks, embeddings, and metadata used for semantic search.

```text
Chunk
   +
Embedding
   +
Metadata
```

Example metadata:

```text
documentId
fileName
chunkNumber
```

---

## 10. REST API

| Method | URL | Description |
|---|---|---|
| POST | `/api/documents/upload` | Upload and process a PDF using `multipart/form-data` with the field name `file`. |
| GET | `/api/documents` | Get all uploaded documents. |
| DELETE | `/api/documents/{id}` | Delete a document and its ChromaDB chunks. |
| POST | `/api/chat` | Ask a question about a selected document. |

### Upload PDF

```http
POST /api/documents/upload
```

Request:

```text
multipart/form-data
file=<PDF>
```

### Get Documents

```http
GET /api/documents
```

### Delete Document

```http
DELETE /api/documents/{id}
```

### Ask a Question

```http
POST /api/chat
```

Request:

```json
{
  "documentId": "document-id",
  "question": "What is this document about?"
}
```

Response:

```json
{
  "answer": "The document is about ...",
  "sources": [
    "Relevant text from the document..."
  ]
}
```

---

## 11. Configuration

The backend configuration is located at:

```text
backend/src/main/resources/application.properties
```

### Server

```properties
server.port=8080
```

### H2 Database

```properties
spring.datasource.url=jdbc:h2:file:./data/pdfchat;AUTO_SERVER=TRUE
```

The H2 console is enabled for local development.

### File Upload

The maximum file size is:

```text
20 MB
```

### Ollama

The application connects to:

```text
http://localhost:11434
```

The chat model is:

```text
llama3.2
```

The embedding model is:

```text
nomic-embed-text
```

The chat configuration uses:

```text
Temperature : 0.3
Context     : 8192
```

### ChromaDB

ChromaDB runs on:

```text
http://localhost:8000
```

The collection name is:

```text
pdf_chat_documents
```

### RAG Settings

```text
Chunk size    : 800 words
Chunk overlap : 100 words
Top K         : 5
```

---

## 12. Setup and Installation

### Step 1: Install Ollama

Install Ollama for your operating system.

Download the required models:

```bash
ollama pull llama3.2
ollama pull nomic-embed-text
```

Start Ollama if it is not already running:

```bash
ollama serve
```

Check that Ollama is available at:

```text
http://localhost:11434
```

---

### Step 2: Start ChromaDB

The project includes a Docker Compose configuration for ChromaDB.

From the project root, run:

```bash
docker compose up -d
```

ChromaDB will be available at:

```text
http://localhost:8000
```

Check the ChromaDB service:

```bash
curl http://localhost:8000/api/v1/heartbeat
```

The project uses ChromaDB `0.5.20`.

---

### Step 3: Start the Backend

Open a terminal in the backend directory:

```bash
cd backend
```

Run:

```bash
mvn spring-boot:run
```

The backend will start at:

```text
http://localhost:8080
```

You can also open the `backend` directory in IntelliJ IDEA as a Maven project and run:

```text
AiPdfChatApplication
```

---

### Step 4: Start the Frontend

Open another terminal:

```bash
cd frontend
```

Install the dependencies:

```bash
npm install
```

Start the development server:

```bash
npm run dev
```

The frontend will be available at:

```text
http://localhost:5173
```

---

## 13. Using the Application

### Upload a PDF

1. Open `http://localhost:5173`.
2. Select or drag a PDF into the upload area.
3. Wait while the PDF is processed.
4. The document becomes available once its status is `READY`.

During processing, the application:

```text
PDF
 ↓
Text Extraction
 ↓
Text Chunking
 ↓
Embedding Generation
 ↓
ChromaDB Storage
```

### Ask Questions

1. Open a processed document.
2. Enter your question.
3. Press Enter to submit the question.
4. The application searches the selected PDF.
5. The relevant chunks are sent to Llama 3.2.
6. The generated answer is displayed.
7. The source excerpts can be viewed below the answer.

### Remove a Document

The **Remove** action deletes:

```text
Document information → H2 Database
Document chunks      → ChromaDB
```

### Return to Documents

The **Back** action leaves the current chat while keeping the document available in the document list.

---

## 14. Error Handling

The backend returns clear error messages for common problems.

| Status | Meaning |
|---|---|
| 400 | Invalid request, invalid PDF, missing question, or missing document selection |
| 404 | Requested document was not found |
| 500 | Server-side processing failure, often caused by Ollama or ChromaDB |

Common messages include:

```text
Please choose a PDF file.
Only PDF files are supported.
No text found in this PDF.
Document not found.
This document is not ready.
Please type a question.
No document selected.
```

---

## 15. Troubleshooting

| Problem | Solution |
|---|---|
| PDF upload fails | Make sure the file is a valid PDF and contains selectable text. |
| Ollama model not found | Run `ollama pull llama3.2` and `ollama pull nomic-embed-text`. |
| Ollama connection error | Make sure Ollama is running at `http://localhost:11434`. |
| ChromaDB connection error | Start ChromaDB with `docker compose up -d` and check port `8000`. |
| Frontend cannot reach backend | Make sure Spring Boot is running on port `8080`. |
| Port 8080 is already in use | Change `server.port` in `application.properties`. |
| Port 5173 is already in use | Start Vite on another available port and update the backend CORS setting if necessary. |
| Answers are slow | Llama 3.2 runs locally, so response time depends on your computer's hardware. |
| Scanned PDF cannot be processed | The current application extracts text with PDFBox and does not include OCR. |

---

## 16. Local AI Architecture

The complete AI workflow can run locally:

```text
                         Local Computer
                              |
        +---------------------+---------------------+
        |                     |                     |
        v                     v                     v
   Spring Boot             Ollama               ChromaDB
        |                     |                     |
        |              +------+-------+             |
        |              |              |             |
        |              v              v             |
        |          Llama 3.2    nomic-embed-text   |
        |              |              |             |
        +--------------+--------------+-------------+
                       |
                       v
                  RAG Pipeline
```

This approach keeps the PDF processing and AI requests on the local machine and does not require an external AI API key.

---

## 17. Future Improvements

Possible improvements for future versions include:

- OCR support for scanned PDFs.
- Streaming responses from the language model.
- Conversation history.
- User authentication.
- Support for additional document formats.
- Improved source highlighting.
- More advanced document management.
- Cloud deployment.
- Additional validation and monitoring.

---

## License

This project is intended for learning, development, and experimentation with Spring Boot, Spring AI, RAG, vector databases, and local LLMs.
