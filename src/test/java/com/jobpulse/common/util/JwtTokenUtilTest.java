package com.jobpulse.common.util;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
class JwtTokenUtilTest {
    private JwtTokenUtil tokens(long lifetime) {
        var tokens=new JwtTokenUtil();
        ReflectionTestUtils.setField(tokens,"jwtSecret","test-only-hs512-secret-material-for-unit-tests-abcdefghijklmnopqrstuvwxyz0123456789");
        ReflectionTestUtils.setField(tokens,"jwtExpirationMs",lifetime);return tokens;
    }
    @Test void rejectsExpiredTokensAndCarriesRevocationVersion() {
        var valid=tokens(60000);var expired=tokens(-1000);
        assertTrue(valid.parseIdentity(expired.generateToken("alice","USER",2)).isEmpty());
        assertEquals(2,valid.parseIdentity(valid.generateToken("alice","USER",2)).orElseThrow().version());
        assertTrue(valid.parseIdentity("not-a-token").isEmpty());
    }
}
