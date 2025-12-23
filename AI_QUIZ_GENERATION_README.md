# AI Quiz Generation - Backend Documentation

## ✅ Đã hoàn thành

### 1. Dependencies
- ✅ Apache POI (DOCX parsing)
- ✅ Apache PDFBox (PDF parsing)
- ✅ Spring WebFlux (HTTP client)

### 2. Configuration
- ✅ Gemini API key: `AIzaSyD8DA_B8y5xaC6GzCcAnMSaTUAnE3HlKpw`
- ✅ Max file size: 10MB
- ✅ Max text length: 50K characters

### 3. Services
- ✅ `DocumentParserService` - Parse PDF/DOCX/TXT
- ✅ `GeminiAIService` - Call Gemini API
- ✅ `AIQuizService` - Orchestrate the flow

### 4. API Endpoint
```
POST /api/ai/quiz/generate-from-document
```

## 📋 API Usage

### Request
```http
POST /api/ai/quiz/generate-from-document
Content-Type: multipart/form-data
Authorization: Bearer {JWT_TOKEN}

Parameters:
- file: MultipartFile (PDF/DOCX/TXT, max 10MB)
- quizSetName: String (required)
- description: String (optional)
- questionCount: int (default: 15, range: 5-50)
```

### Response
```json
{
  "quizSetId": 123,
  "quizSetName": "Bộ đề Luật Dân sự",
  "totalQuestions": 15,
  "questions": [
    {
      "question": "Theo Bộ luật Dân sự 2015, độ tuổi thành niên là?",
      "optionA": "16 tuổi",
      "optionB": "17 tuổi",
      "optionC": "18 tuổi",
      "optionD": "21 tuổi",
      "correctAnswer": "C",
      "explanation": "Theo Điều 21 Bộ luật Dân sự 2015..."
    }
  ]
}
```

## 🔧 How It Works

### Flow:
1. User uploads document (PDF/DOCX/TXT)
2. `DocumentParserService` extracts text
3. `GeminiAIService` calls Gemini API with prompt
4. AI generates 15 questions in JSON format
5. `AIQuizService` creates QuizSet and saves questions to database
6. Returns response with quiz set ID and questions

### Supported File Types:
- ✅ PDF (`.pdf`)
- ✅ DOCX (`.docx`)
- ✅ TXT (`.txt`)

### Validation:
- File size: max 10MB
- Text length: max 50K characters
- Question count: 5-50
- Each question must have 4 options (A, B, C, D)
- Correct answer must be A, B, C, or D

## 🎯 AI Prompt Strategy

The prompt instructs Gemini to:
- Create multiple-choice questions about Vietnamese law
- Each question has 4 options
- Only 1 correct answer
- Provide detailed explanation
- Return strict JSON format
- Questions must be based on document content

## 🚀 Testing

### Using Postman/cURL:
```bash
curl -X POST http://localhost:8080/api/ai/quiz/generate-from-document \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@document.pdf" \
  -F "quizSetName=Bộ đề test" \
  -F "description=Mô tả" \
  -F "questionCount=15"
```

### Using Frontend (Next Step):
Create `quiz-generate-ai.html` with:
- File upload input
- Quiz set name input
- Question count selector
- Submit button
- Loading indicator
- Results display

## 📊 Performance

- Average processing time: 10-30 seconds
- Depends on:
  - Document size
  - Number of questions
  - Gemini API response time

## 🔐 Security

- ✅ Requires JWT authentication
- ✅ File size validation
- ✅ File type validation
- ✅ Text length limitation
- ✅ Input sanitization

## 🎓 Next Steps

1. Create frontend UI (`quiz-generate-ai.html`)
2. Add loading progress indicator
3. Add error handling UI
4. Add question preview before saving
5. Add ability to edit AI-generated questions
6. Add rate limiting (prevent abuse)

## 💡 Tips

- Use clear, well-structured documents for best results
- Longer documents = better context = better questions
- Test with different document types
- Review AI-generated questions before using in production

## 🔄 Future Enhancements

- [ ] Support more file types (PPT, Excel)
- [ ] Batch processing (multiple files)
- [ ] Custom prompt templates
- [ ] Question difficulty levels
- [ ] Multi-language support
- [ ] Switch to OpenAI when ready
