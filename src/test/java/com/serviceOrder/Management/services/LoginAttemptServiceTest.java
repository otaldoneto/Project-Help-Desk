package com.serviceOrder.Management.services;

import com.serviceOrder.Management.controllers.exceptions.TooManyRequestsException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptServiceTest {

    private final LoginAttemptService service = new LoginAttemptService();

    @Test
    @DisplayName("checkAllowed should not throw before the limit is reached")
    void checkAllowedShouldNotThrowBeforeTheLimit() {
        String ip = "203.0.113.1";

        for (int i = 0; i < 4; i++) {
            assertDoesNotThrow(() -> service.checkAllowed(ip));
            service.recordFailure(ip);
        }

        // 4 failures recorded so far, still under the limit of 5
        assertDoesNotThrow(() -> service.checkAllowed(ip));
    }

    @Test
    @DisplayName("checkAllowed should throw once 5 failures have been recorded, with a positive retry time")
    void checkAllowedShouldThrowAfterFiveFailures() {
        String ip = "203.0.113.2";

        for (int i = 0; i < 5; i++) {
            service.recordFailure(ip);
        }

        TooManyRequestsException exception = assertThrows(TooManyRequestsException.class,
                () -> service.checkAllowed(ip));
        assertTrue(exception.getRetryAfterSeconds() > 0);
    }

    @Test
    @DisplayName("a successful login should reset the counter, allowing attempts again")
    void recordSuccessShouldResetTheCounter() {
        String ip = "203.0.113.3";

        for (int i = 0; i < 5; i++) {
            service.recordFailure(ip);
        }
        assertThrows(TooManyRequestsException.class, () -> service.checkAllowed(ip));

        service.recordSuccess(ip);

        assertDoesNotThrow(() -> service.checkAllowed(ip));
    }

    @Test
    @DisplayName("different IP addresses should be tracked independently")
    void differentIpsShouldBeTrackedIndependently() {
        String blockedIp = "203.0.113.4";
        String otherIp = "203.0.113.5";

        for (int i = 0; i < 5; i++) {
            service.recordFailure(blockedIp);
        }
        assertThrows(TooManyRequestsException.class, () -> service.checkAllowed(blockedIp));

        assertDoesNotThrow(() -> service.checkAllowed(otherIp));
    }
}