package com.princeraj.campustaxipooling;

import org.junit.Test;
import static org.junit.Assert.*;

import com.princeraj.campustaxipooling.repository.ChatRepository;

/**
 * Unit tests for Chat moderation business logic.
 * These ensure that PII (Phone/Email) and restricted keywords are correctly flagged.
 */
public class ChatModerationTest {

    private final ChatRepository chatRepo = ChatRepository.getInstance();

    @Test
    public void testCleanMessage() {
        ChatRepository.ModerationResult result = chatRepo.moderateMessage("Hello, I am at the main gate.");
        assertTrue("Message should be clean", result.isClean);
        assertNull("Flag reason should be null", result.flagReason);
    }

    @Test
    public void testPhoneDetection() {
        ChatRepository.ModerationResult result = chatRepo.moderateMessage("My number is 9876543210.");
        assertFalse("Message with phone should be flagged", result.isClean);
        assertEquals("PHONE_DETECTED", result.flagReason);
    }

    @Test
    public void testEmailDetection() {
        ChatRepository.ModerationResult result = chatRepo.moderateMessage("Email me at test@example.com");
        assertFalse("Message with email should be flagged", result.isClean);
        assertEquals("EMAIL_DETECTED", result.flagReason);
    }

    @Test
    public void testKeywordDetection() {
        ChatRepository.ModerationResult result = chatRepo.moderateMessage("Please whatsapp me directly.");
        assertFalse("Message with restricted keywords should be flagged", result.isClean);
        assertEquals("KEYWORD_DETECTED", result.flagReason);
    }

    @Test
    public void testCaseInsensitiveKeywords() {
        ChatRepository.ModerationResult result = chatRepo.moderateMessage("CALL ME NOW");
        assertFalse("Case-insensitive keywords should be flagged", result.isClean);
        assertEquals("KEYWORD_DETECTED", result.flagReason);
    }
}
