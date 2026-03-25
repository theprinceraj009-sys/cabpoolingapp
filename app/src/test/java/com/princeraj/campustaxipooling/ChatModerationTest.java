package com.princeraj.campustaxipooling;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.princeraj.campustaxipooling.repository.ChatRepositoryImpl;
import com.princeraj.campustaxipooling.repository.IChatRepository;

/**
 * Unit tests for Chat moderation business logic.
 * These ensure that PII (Phone/Email) and restricted keywords are correctly flagged.
 *
 * ChatRepositoryImpl.moderateMessage() is pure logic (no Firebase/DB needed),
 * so we pass null for Firebase/DB — they are never touched in these tests.
 */
public class ChatModerationTest {

    private IChatRepository chatRepo;

    @Before
    public void setUp() {
        // moderateMessage() is pure logic — Firebase and DB are never used here.
        chatRepo = new ChatRepositoryImpl(null, null);
    }

    @Test
    public void testCleanMessage() {
        IChatRepository.ModerationResult result = chatRepo.moderateMessage("Hello, I am at the main gate.");
        assertTrue("Message should be clean", result.isClean);
        assertNull("Flag reason should be null", result.flagReason);
    }

    @Test
    public void testPhoneDetection() {
        IChatRepository.ModerationResult result = chatRepo.moderateMessage("My number is 9876543210.");
        assertFalse("Message with phone should be flagged", result.isClean);
        assertEquals("PHONE_DETECTED", result.flagReason);
    }

    @Test
    public void testEmailDetection() {
        IChatRepository.ModerationResult result = chatRepo.moderateMessage("Email me at test@example.com");
        assertFalse("Message with email should be flagged", result.isClean);
        assertEquals("EMAIL_DETECTED", result.flagReason);
    }

    @Test
    public void testKeywordDetection() {
        IChatRepository.ModerationResult result = chatRepo.moderateMessage("Please whatsapp me directly.");
        assertFalse("Message with restricted keywords should be flagged", result.isClean);
        assertEquals("KEYWORD_DETECTED", result.flagReason);
    }

    @Test
    public void testCaseInsensitiveKeywords() {
        IChatRepository.ModerationResult result = chatRepo.moderateMessage("CALL ME NOW");
        assertFalse("Case-insensitive keywords should be flagged", result.isClean);
        assertEquals("KEYWORD_DETECTED", result.flagReason);
    }
}
