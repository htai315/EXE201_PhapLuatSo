package com.htai.exe201phapluatso.legal.controller;

import com.htai.exe201phapluatso.ai.service.DocumentParserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/legal/analyze")
public class LegalAnalyzeController {

    private final DocumentParserService documentParser;

    public LegalAnalyzeController(DocumentParserService documentParser) {
        this.documentParser = documentParser;
    }

    /**
     * Analyze PDF file to see all "Điều" patterns
     */
    @PostMapping("/pdf")
    public ResponseEntity<Map<String, Object>> analyzePdf(@RequestParam("file") MultipartFile file) {
        String fullText = documentParser.extractText(file);
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalLength", fullText.length());
        result.put("patterns", analyzePatterns(fullText));
        result.put("preview", fullText.substring(0, Math.min(1000, fullText.length())));
        
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> analyzePatterns(String text) {
        Map<String, Object> patterns = new HashMap<>();
        
        // Pattern 1: "Điều 1."
        Pattern p1 = Pattern.compile("Điều\\s+(\\d+)\\.", Pattern.UNICODE_CHARACTER_CLASS);
        patterns.put("pattern1_count", countMatches(p1, text));
        patterns.put("pattern1_samples", getSamples(p1, text, 5));
        
        // Pattern 2: "Điều 1" (no dot)
        Pattern p2 = Pattern.compile("Điều\\s+(\\d+)(?!\\.)", Pattern.UNICODE_CHARACTER_CLASS);
        patterns.put("pattern2_count", countMatches(p2, text));
        patterns.put("pattern2_samples", getSamples(p2, text, 5));
        
        // Pattern 3: "Điều1" (no space)
        Pattern p3 = Pattern.compile("Điều(\\d+)", Pattern.UNICODE_CHARACTER_CLASS);
        patterns.put("pattern3_count", countMatches(p3, text));
        patterns.put("pattern3_samples", getSamples(p3, text, 5));
        
        // All article numbers found
        Set<Integer> allNumbers = new TreeSet<>();
        for (Pattern p : Arrays.asList(p1, p2, p3)) {
            Matcher m = p.matcher(text);
            while (m.find()) {
                allNumbers.add(Integer.parseInt(m.group(1)));
            }
        }
        patterns.put("unique_articles", allNumbers.size());
        patterns.put("article_numbers", allNumbers);
        
        return patterns;
    }

    private int countMatches(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    private List<String> getSamples(Pattern pattern, String text, int limit) {
        List<String> samples = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find() && count < limit) {
            int start = Math.max(0, matcher.start() - 20);
            int end = Math.min(text.length(), matcher.end() + 50);
            samples.add(text.substring(start, end));
            count++;
        }
        return samples;
    }
}
