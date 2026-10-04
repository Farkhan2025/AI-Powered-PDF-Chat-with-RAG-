# PDF Chat

Upload a PDF and ask questions about it. The answers come from the content of your PDF.
Everything runs on your own computer: no cloud AI service, no API keys.

This is a beginner-friendly version of the original project. The functionality is the same
(real RAG with Spring AI, Ollama, Llama 3.2, nomic-embed-text and ChromaDB), but the Spring Boot
code is much simpler to read and explain. See [INTERVIEW_GUIDE.md](INTERVIEW_GUIDE.md) for questions
and answers about the project.

---

## 1. What you need

| Software | Version | Check with |
|---|---|---|
| Java (JDK) | 17 or newer | `java -version` |
| Maven | 3.8 or newer | `mvn -version` |
| Node.js | 18 or newer (with npm) | `node -v` |
| Ollama | latest | `ollama --version` |
| Docker (for ChromaDB) | any recent version | `docker --version` |

If you do not want Docker, you can run ChromaDB with Python instead (see step 4).

---

## 2. Install Ollama and the two models

1. Install Ollama from <https://ollama.com/download> (Windows, macOS and Linux are supported).
2. Download the two models (only needed once, the files are a few GB):

```bash
ollama pull llama3.2
ollama pull nomic-embed-text
```

- `llama3.2` writes the answers.
- `nomic-embed-text` turns text into embeddings (numbers that represent the meaning of the text).

3. Make sure Ollama is running. On Windows and macOS the app starts it automatically.
   On Linux, or if it is not running, start it yourself:

```bash
ollama serve
```

Check it: open <http://localhost:11434>. You should see `Ollama is running`.

---

## 3. Start ChromaDB (vector database)

From the project folder (where `docker-compose.yml` is):

```bash
docker compose up -d
```

Check it:

```bash
curl http://localhost:8000/api/v1/heartbeat
```

You should get a JSON answer with a number (the heartbeat).

> The ChromaDB version is pinned to `0.5.20` in `docker-compose.yml` on purpose. This project uses
> Spring AI `1.0.0-M3`, which uses the ChromaDB v1 API, and newer ChromaDB versions (1.x) removed it.

## 4. (Optional) ChromaDB without Docker

```bash
pip install chromadb==0.5.20
chroma run --path ./chroma-data --port 8000
```

---

## 5. Start the backend (Spring Boot)

```bash
cd backend
mvn spring-boot:run
```

The first start downloads the dependencies, so it takes a few minutes.
The backend runs on <http://localhost:8080>.

In IntelliJ you can also open the `backend` folder as a Maven project and run
`AiPdfChatApplication`.

All settings are in `backend/src/main/resources/application.properties`
(port, Ollama URL, model names, ChromaDB address, database, upload size, chunk size).

## 6. Start the frontend (React)

Open a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open <http://localhost:5173>.

## 7. How to use it

1. Open <http://localhost:5173>.
2. Drag a PDF into the box (or click it and choose a file). Wait while the PDF is processed.
   A large PDF can take a minute because every chunk is turned into an embedding.
3. Type a question at the bottom and press Enter. Shift+Enter adds a new line.
4. Click **View sources** under an answer to see which parts of the PDF were used.
5. Keep asking questions.
6. Click **Remove** next to the PDF (above the input box) to delete it. This deletes the PDF
   from the database and its chunks from ChromaDB, and takes you back to the upload screen.
7. **Back** leaves the chat but keeps the PDF, so you can open it again from "Your documents".

Note: scanned PDFs (pictures of pages) have no text to read, so they cannot be used.

---

## 8. How it works (simple version)

```
React frontend  <--REST-->  Spring Boot  --Spring AI-->  Ollama (llama3.2, nomic-embed-text)
                                 |
                                 +--Spring AI-->  ChromaDB (chunks + embeddings)
                                 |
                                 +--JPA-->        H2 database (PDF name, size, status)
```

**When you upload a PDF**

```
DocumentController -> PdfService -> PDFBox (extract text)
                                 -> ChunkService (split into chunks)
                                 -> VectorService -> Spring AI -> nomic-embed-text -> ChromaDB
```

**When you ask a question**

```
ChatController -> ChatService -> VectorService -> Spring AI -> nomic-embed-text (embed the question)
                                              -> ChromaDB (find the most similar chunks)
               -> build prompt (chunks + question)
               -> Spring AI -> Ollama -> llama3.2 -> answer
```

This idea is called **RAG** (Retrieval-Augmented Generation): first *retrieve* the useful parts of
the PDF, then ask the model to *generate* an answer using only those parts.

## 9. Project structure

```
ai-pdf-chat/
├── docker-compose.yml          starts ChromaDB
├── README.md
├── INTERVIEW_GUIDE.md
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── resources/application.properties
│       └── java/com/aipdfchat/
│           ├── AiPdfChatApplication.java
│           ├── controller/
│           │   ├── DocumentController.java   upload / list / delete PDFs
│           │   └── ChatController.java       ask a question
│           ├── service/
│           │   ├── PdfService.java           validate, PDFBox, upload flow, delete
│           │   ├── ChunkService.java         split text into chunks
│           │   ├── VectorService.java        save / search / delete in ChromaDB
│           │   └── ChatService.java          the RAG flow + Llama prompt
│           ├── entity/Document.java          the "documents" table
│           └── repository/DocumentRepository.java
└── frontend/
    ├── package.json, vite.config.js, index.html
    └── src/
        ├── main.jsx, App.jsx, api.js         api.js = every call to the backend
        ├── pages/        Home.jsx, ChatPage.jsx
        └── components/   UploadBox, ChatWindow, MessageBubble, Loader, PdfIcon
```

## 10. REST API

| Method | URL | What it does |
|---|---|---|
| POST | `/api/documents/upload` | Upload a PDF (`multipart/form-data`, field `file`). Returns the saved document. |
| GET | `/api/documents` | List all uploaded PDFs. |
| DELETE | `/api/documents/{id}` | Remove a PDF (database row + ChromaDB chunks). |
| POST | `/api/chat` | Body: `{ "documentId": "...", "question": "..." }`. Returns `{ "answer": "...", "sources": [...] }`. |

Errors come back as `{ "message": "..." }` with status 400 (problem with the request), 404 (not found)
or 500 (usually Ollama or ChromaDB is not running).

## 11. Troubleshooting

| Problem | Solution |
|---|---|
| "Could not process the PDF. Please check that Ollama and ChromaDB are running." | Check `http://localhost:11434` (Ollama) and `http://localhost:8000/api/v1/heartbeat` (ChromaDB). Look at the backend console for the real error. |
| Ollama says the model was not found | Run `ollama pull llama3.2` and `ollama pull nomic-embed-text`. |
| ChromaDB errors about a missing or removed API | You are probably running ChromaDB 1.x. Use the pinned version in `docker-compose.yml`. |
| Frontend cannot reach the backend | Is Spring Boot running on port 8080? The Vite dev server forwards `/api` to it (`vite.config.js`). |
| Port 8080 or 5173 already used | Change `server.port` in `application.properties`, or the port in `vite.config.js` (then also update `@CrossOrigin` in the controllers). |
| Answers are slow | Llama 3.2 runs on your own computer, so speed depends on your hardware. The first question after starting Ollama is the slowest. |

## 12. What was simplified compared to the original project

Same features, simpler code:

- Removed the DTO classes, the custom exception classes, the global exception handler, the config classes
  and the utility class, and removed Lombok. Entities and services use plain constructors, getters and setters.
- 6 services became 4 (`PdfService`, `ChunkService`, `VectorService`, `ChatService`).
  The extra `EmbeddingService` and `RetrievalService` only passed calls through to Spring AI.
- Errors are handled with a simple try/catch in the controllers.
- The React app no longer uses a router: `App.jsx` keeps the selected PDF in state.
- The original saved a copy of each PDF on disk, but nothing ever read that copy (the text is
  extracted during upload), so it is no longer saved.
- New: the **Remove** button and the `DELETE /api/documents/{id}` endpoint.
- Settings moved to `application.properties`, and `num-ctx` was added so Ollama does not cut off the
  retrieved text (see the comment in the file).
