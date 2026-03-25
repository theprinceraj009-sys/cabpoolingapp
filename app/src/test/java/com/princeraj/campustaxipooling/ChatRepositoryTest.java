package com.princeraj.campustaxipooling;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.princeraj.campustaxipooling.repository.ChatRepositoryImpl;
import com.princeraj.campustaxipooling.repository.IChatRepository;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for ChatRepositoryImpl moderation logic.
 *
 * Note: ChatRepositoryImpl constructor takes (FirebaseFirestore, CampusTaxiDatabase).
 * Both are passed as null here because moderateMessage() is pure logic
 * and never touches Firebase or the database.
 */
public class ChatRepositoryTest {

    private ChatRepositoryImpl chatRepo;

    @Before
    public void setup() {
        // 2-arg constructor: (FirebaseFirestore db, CampusTaxiDatabase database)
        // Passing null is safe — only moderateMessage() is tested here (pure logic, no I/O).
        chatRepo = new ChatRepositoryImpl(null, null);
    }

    @Test
    public void testModeration_CleanMessage() {
        IChatRepository.ModerationResult result = chatRepo.moderateMessage("Hello, how are you?");
        assertTrue("Message should be clean", result.isClean);
    }

    @Test
    public void testModeration_PhoneNumber() {
        IChatRepository.ModerationResult result = chatRepo.moderateMessage("Call me on 9876543210");
        assertFalse("Message with phone number should be flagged", result.isClean);
        assertEquals_flagReason("PHONE_DETECTED", result.flagReason);
    }

    @Test
    public void testModeration_SensitiveKeyword() {
        IChatRepository.ModerationResult result = chatRepo.moderateMessage("Give me your password");
        assertFalse("Message should be flagged", result.isClean);
        assertEquals_flagReason("KEYWORD_DETECTED", result.flagReason);
    }

    @Test
    public void testModeration_ToxicContent() {
        IChatRepository.ModerationResult result = chatRepo.moderateMessage("You are stupid");
        assertFalse("Toxicity should be caught by client-side filters", result.isClean);
    }

    @Test
    public void testModeration_OtpKeyword() {
        IChatRepository.ModerationResult result = chatRepo.moderateMessage("Share your otp with me");
        assertFalse("OTP keyword should be flagged", result.isClean);
    }

    /** Helper wrapper so tests remain readable when checking flagReason. */
    private void assertEquals_flagReason(String expected, String actual) {
        assertTrue("Expected flagReason to contain '" + expected + "' but got: " + actual,
                actual != null && actual.equals(expected));
    }
}
