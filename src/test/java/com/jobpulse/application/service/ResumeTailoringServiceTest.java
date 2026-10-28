package com.jobpulse.application.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResumeTailoringServiceTest {
    @Test void emptyResumeRejectedBeforeConnectingToOllama() {
        ResumeTailoringService service = new ResumeTailoringService();
        assertThrows(IllegalArgumentException.class, () -> service.tailor("","Software Engineer"));
        assertThrows(IllegalArgumentException.class, () -> service.tailor("Java",""));
    }
}
