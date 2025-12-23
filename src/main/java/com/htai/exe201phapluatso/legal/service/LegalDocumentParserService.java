package com.htai.exe201phapluatso.legal.service;

import com.htai.exe201phapluatso.ai.service.DocumentParserService;
import com.htai.exe201phapluatso.common.exception.BadRequestException;
import com.htai.exe201phapluatso.legal.entity.LegalArticle;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LegalDocumentParserService {

    private final DocumentParserService documentParser;

    public LegalDocumentParserService(DocumentParserService documentParser) {
        this.documentParser = documentParser;
    }

    /**
     * Parse PDF file and extract legal articles
     * Pattern: "Điều 1.", "Điều 2.", etc.
     */
    public List<LegalArticle> parseDocument(MultipartFile file) {
        // Extract full text from PDF
        String fullText = documentParser.extractText(file);
        
        if (fullText == null || fullText.trim().isEmpty()) {
            throw new BadRequestException("Không thể đọc nội dung file PDF");
        }

        // Parse articles
        return extractArticles(fullText);
    }

    /**
     * Extract articles from full text using regex pattern
     * Improved version with better pattern matching and debugging
     */
    private List<LegalArticle> extractArticles(String fullText) {
        List<LegalArticle> articles = new ArrayList<>();
        
        // Primary pattern: "Điều X. Title" (most common in Vietnamese legal documents)
        // This pattern captures both the number and title on the same line
        Pattern pattern = Pattern.compile(
            "Điều\\s+(\\d+)\\.\\s*([^\\n]+)",
            Pattern.UNICODE_CHARACTER_CLASS
        );
        
        Matcher matcher = pattern.matcher(fullText);
        
        int lastEnd = 0;
        int currentArticleNum = 0;
        String currentTitle = "";
        int matchCount = 0;
        List<Integer> foundNumbers = new ArrayList<>();
        
        while (matcher.find()) {
            matchCount++;
            int articleNum = Integer.parseInt(matcher.group(1));
            foundNumbers.add(articleNum);
            
            // Save previous article (if exists)
            if (currentArticleNum > 0) {
                String content = fullText.substring(lastEnd, matcher.start()).trim();
                
                // Clean up content: remove chapter headers, etc.
                content = cleanContent(content);
                
                // Only save if content is meaningful (not just whitespace or headers)
                if (content.length() > 10) {
                    LegalArticle article = new LegalArticle();
                    article.setArticleNumber(currentArticleNum);
                    article.setArticleTitle(currentTitle);
                    article.setContent(content);
                    articles.add(article);
                } else {
                    System.out.println("⚠ Skipped Điều " + currentArticleNum + " (content too short: " + content.length() + " chars)");
                }
            }
            
            // Start new article
            currentArticleNum = articleNum;
            currentTitle = matcher.group(2).trim();
            lastEnd = matcher.end();
        }
        
        // Save last article
        if (currentArticleNum > 0) {
            String content = fullText.substring(lastEnd).trim();
            content = cleanContent(content);
            
            if (content.length() > 10) {
                LegalArticle article = new LegalArticle();
                article.setArticleNumber(currentArticleNum);
                article.setArticleTitle(currentTitle);
                article.setContent(content);
                articles.add(article);
            } else {
                System.out.println("⚠ Skipped last article Điều " + currentArticleNum + " (content too short: " + content.length() + " chars)");
            }
        }
        
        // Detailed logging
        System.out.println("========== PARSING RESULTS ==========");
        System.out.println("✓ Pattern used: Điều \\s+(\\d+)\\.\\s*([^\\n]+)");
        System.out.println("✓ Total matches found: " + matchCount);
        System.out.println("✓ Articles saved: " + articles.size());
        System.out.println("✓ Articles skipped: " + (matchCount - articles.size()));
        
        if (!foundNumbers.isEmpty()) {
            System.out.println("✓ Article number range: " + foundNumbers.get(0) + " to " + foundNumbers.get(foundNumbers.size() - 1));
        }
        
        if (articles.size() > 0) {
            System.out.println("✓ First article: Điều " + articles.get(0).getArticleNumber() + " - " + articles.get(0).getArticleTitle());
            if (articles.size() > 1) {
                System.out.println("✓ Last article: Điều " + articles.get(articles.size() - 1).getArticleNumber() + " - " + articles.get(articles.size() - 1).getArticleTitle());
            }
        }
        System.out.println("====================================");
        
        if (articles.isEmpty()) {
            throw new BadRequestException(
                "Không tìm thấy điều luật nào trong file.\n" +
                "Vui lòng kiểm tra:\n" +
                "- File PDF có text layer (không phải ảnh scan)\n" +
                "- Có pattern 'Điều 1.', 'Điều 2.' trong file\n" +
                "- Encoding đúng (UTF-8)\n" +
                "Total matches found: " + matchCount
            );
        }
        
        return articles;
    }

    /**
     * Clean content: remove chapter headers, page numbers, section titles, etc.
     * More aggressive cleaning to handle various document formats
     */
    private String cleanContent(String content) {
        // Remove chapter headers (Chương I, Chương XX, CHƯƠNG I, etc.)
        // Match both with and without additional text after
        content = content.replaceAll("(?mi)^\\s*Chương\\s+[IVX]+[^\\n]*$", "");
        content = content.replaceAll("(?mi)^\\s*Chương\\s+\\d+[^\\n]*$", "");
        
        // Remove part headers (Phần thứ nhất, Phần I, PHẦN THỨ NHẤT, etc.)
        content = content.replaceAll("(?mi)^\\s*Phần\\s+(thứ\\s+)?[a-zàáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]+[^\\n]*$", "");
        content = content.replaceAll("(?mi)^\\s*Phần\\s+[IVX]+[^\\n]*$", "");
        
        // Remove section headers (Mục 1, MỤC 1, etc.)
        content = content.replaceAll("(?mi)^\\s*Mục\\s+\\d+[^\\n]*$", "");
        
        // Remove all-caps titles (likely section/chapter titles)
        // Must be at least 10 chars and all uppercase Vietnamese
        content = content.replaceAll("(?m)^\\s*[A-ZÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ\\s]{10,}\\s*$", "");
        
        // Remove page numbers (various formats)
        content = content.replaceAll("(?m)^\\s*\\d+\\s*$", "");
        content = content.replaceAll("(?m)^\\s*Trang\\s+\\d+[^\\n]*$", "");
        content = content.replaceAll("(?m)^\\s*-\\s*\\d+\\s*-\\s*$", "");
        content = content.replaceAll("(?m)^\\s*\\[\\s*\\d+\\s*\\]\\s*$", "");
        
        // Remove document headers that might appear at top
        content = content.replaceAll("(?mi)^\\s*VĂN BẢN HỢP NHẤT[^\\n]*$", "");
        content = content.replaceAll("(?mi)^\\s*BỘ LUẬT[^\\n]*NĂM \\d{4}[^\\n]*$", "");
        
        // Remove lines that are just dashes or underscores (separators)
        content = content.replaceAll("(?m)^\\s*[-_=]{3,}\\s*$", "");
        
        // Remove multiple blank lines (3+ newlines -> 2 newlines)
        content = content.replaceAll("\\n{3,}", "\n\n");
        
        // Remove leading/trailing whitespace
        content = content.trim();
        
        return content;
    }

    /**
     * Auto-detect document name from text
     * Looks for common patterns like "Bộ luật...", "Luật...", "Nghị định..."
     */
    public String detectDocumentName(String fullText) {
        // Pattern: "Bộ luật Hình sự", "Luật Dân sự", etc.
        Pattern pattern = Pattern.compile(
            "(Bộ luật|Luật|Nghị định|Thông tư|Quyết định)\\s+([^\\n]{10,100})",
            Pattern.UNICODE_CHARACTER_CLASS
        );
        
        Matcher matcher = pattern.matcher(fullText);
        if (matcher.find()) {
            return matcher.group(0).trim();
        }
        
        return null;
    }
}
