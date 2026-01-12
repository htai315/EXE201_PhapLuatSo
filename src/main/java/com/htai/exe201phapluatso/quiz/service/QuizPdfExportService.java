package com.htai.exe201phapluatso.quiz.service;

import com.htai.exe201phapluatso.quiz.entity.QuizQuestion;
import com.htai.exe201phapluatso.quiz.entity.QuizQuestionOption;
import com.htai.exe201phapluatso.quiz.entity.QuizSet;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class QuizPdfExportService {

    private static final Logger log = LoggerFactory.getLogger(QuizPdfExportService.class);
    
    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(26, 75, 132);
    private static final DeviceRgb CORRECT_COLOR = new DeviceRgb(34, 197, 94);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    // Cross-platform fonts that support Vietnamese
    private static final String[] VIETNAMESE_FONTS = {
        // Windows - common paths
        "C:\\Windows\\Fonts\\arial.ttf",
        "C:\\Windows\\Fonts\\times.ttf",
        "C:\\Windows\\Fonts\\tahoma.ttf",
        "C:\\Windows\\Fonts\\segoeui.ttf",
        "C:/Windows/Fonts/arial.ttf",
        "C:/Windows/Fonts/times.ttf",
        "C:/Windows/Fonts/tahoma.ttf",
        "C:/Windows/Fonts/segoeui.ttf",
        // Linux (common paths)
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
        "/usr/share/fonts/truetype/freefont/FreeSans.ttf",
        "/usr/share/fonts/TTF/DejaVuSans.ttf",
        // macOS
        "/System/Library/Fonts/Supplemental/Arial.ttf",
        "/System/Library/Fonts/Helvetica.ttc",
        "/Library/Fonts/Arial.ttf"
    };

    /**
     * Create font with Vietnamese support (cross-platform)
     */
    private PdfFont createVietnameseFont() {
        // Try system fonts first
        for (String fontPath : VIETNAMESE_FONTS) {
            try {
                java.io.File fontFile = new java.io.File(fontPath);
                if (fontFile.exists()) {
                    PdfFont font = PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H, 
                            PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                    log.info("Using Vietnamese font: {}", fontPath);
                    return font;
                }
            } catch (Exception e) {
                log.debug("Could not load font {}: {}", fontPath, e.getMessage());
            }
        }
        
        // Fallback to default font with warning
        try {
            log.warn("No Vietnamese font found on this system. Vietnamese characters may not display correctly. " +
                    "Consider installing DejaVu or Liberation fonts.");
            return PdfFontFactory.createFont();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create font", e);
        }
    }

    /**
     * Export quiz to PDF - Questions only (for exam)
     */
    public byte[] exportQuizToPdf(QuizSet quizSet, List<QuizQuestion> questions) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Use Vietnamese-compatible font
            PdfFont font = createVietnameseFont();
            document.setFont(font);

            // Title
            addTitle(document, quizSet);
            
            // Questions
            int questionNum = 1;
            for (QuizQuestion question : questions) {
                addQuestion(document, question, questionNum++, false);
            }

            // Footer
            addFooter(document, questions.size());

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Export quiz to PDF - With answers (for review/study)
     */
    public byte[] exportQuizWithAnswersToPdf(QuizSet quizSet, List<QuizQuestion> questions) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            PdfFont font = createVietnameseFont();
            document.setFont(font);

            // Title
            addTitle(document, quizSet);
            addSubtitle(document, "(Co dap an)");

            // Questions with answers
            int questionNum = 1;
            for (QuizQuestion question : questions) {
                addQuestion(document, question, questionNum++, true);
            }

            // Answer key section
            addAnswerKey(document, questions);

            // Footer
            addFooter(document, questions.size());

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    private void addTitle(Document document, QuizSet quizSet) {
        Paragraph title = new Paragraph(quizSet.getTitle())
                .setFontSize(20)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(title);

        if (quizSet.getDescription() != null && !quizSet.getDescription().isEmpty()) {
            Paragraph desc = new Paragraph(quizSet.getDescription())
                    .setFontSize(11)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5);
            document.add(desc);
        }

        Paragraph date = new Paragraph("Ngay tao: " + quizSet.getCreatedAt().format(DATE_FORMAT))
                .setFontSize(9)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(date);

        // Separator line
        Paragraph separator = new Paragraph("_".repeat(80))
                .setFontColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15);
        document.add(separator);
    }

    private void addSubtitle(Document document, String subtitle) {
        Paragraph sub = new Paragraph(subtitle)
                .setFontSize(12)
                .setItalic()
                .setFontColor(CORRECT_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(-15)
                .setMarginBottom(20);
        document.add(sub);
    }

    private void addQuestion(Document document, QuizQuestion question, int questionNum, boolean showAnswer) {
        // Question text
        Paragraph questionPara = new Paragraph()
                .setMarginBottom(8);
        
        Text numText = new Text("Cau " + questionNum + ": ")
                .setBold()
                .setFontColor(PRIMARY_COLOR);
        questionPara.add(numText);
        questionPara.add(new Text(question.getQuestionText()));
        document.add(questionPara);

        // Options
        for (QuizQuestionOption option : question.getOptions()) {
            Paragraph optionPara = new Paragraph()
                    .setMarginLeft(20)
                    .setMarginBottom(3);

            String optionLabel = option.getOptionKey() + ". " + option.getOptionText();
            
            if (showAnswer && option.isCorrect()) {
                optionPara.add(new Text(optionLabel)
                        .setFontColor(CORRECT_COLOR)
                        .setBold());
                optionPara.add(new Text(" [DUNG]").setFontColor(CORRECT_COLOR));
            } else {
                optionPara.add(new Text(optionLabel));
            }
            
            document.add(optionPara);
        }

        // Explanation (if showing answers and has explanation)
        if (showAnswer && question.getExplanation() != null && !question.getExplanation().isEmpty()) {
            Paragraph explPara = new Paragraph()
                    .setMarginLeft(20)
                    .setMarginTop(5)
                    .setMarginBottom(15)
                    .setFontSize(10)
                    .setItalic()
                    .setFontColor(ColorConstants.DARK_GRAY);
            explPara.add(new Text("Giai thich: ").setBold());
            explPara.add(new Text(question.getExplanation()));
            document.add(explPara);
        } else {
            document.add(new Paragraph().setMarginBottom(10));
        }
    }

    private void addAnswerKey(Document document, List<QuizQuestion> questions) {
        // Page break before answer key
        document.add(new Paragraph("\n"));
        
        Paragraph keyTitle = new Paragraph("BANG DAP AN")
                .setFontSize(14)
                .setBold()
                .setFontColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20)
                .setMarginBottom(15);
        document.add(keyTitle);

        StringBuilder answers = new StringBuilder();
        int count = 0;
        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion q = questions.get(i);
            String correctKey = q.getOptions().stream()
                    .filter(QuizQuestionOption::isCorrect)
                    .map(QuizQuestionOption::getOptionKey)
                    .findFirst()
                    .orElse("?");
            
            answers.append(String.format("%2d. %s", i + 1, correctKey));
            count++;
            
            if (count % 5 == 0) {
                answers.append("\n");
            } else if (i < questions.size() - 1) {
                answers.append("     ");
            }
        }

        Paragraph answersPara = new Paragraph(answers.toString())
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(answersPara);
    }

    private void addFooter(Document document, int totalQuestions) {
        Paragraph footer = new Paragraph()
                .setMarginTop(30)
                .setFontSize(9)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER);
        
        footer.add(new Text("_".repeat(40) + "\n"));
        footer.add(new Text("Tong so cau hoi: " + totalQuestions + " | "));
        footer.add(new Text("Xuat tu AI Phap Luat So"));
        
        document.add(footer);
    }
}
