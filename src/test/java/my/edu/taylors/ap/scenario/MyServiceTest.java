package my.edu.taylors.ap.scenario;

import my.edu.taylors.ap.scenario.mine.OrderPricingService;
import my.edu.taylors.ap.scenario.mine.RushHourCapException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TODO [B1] Unit tests for YOUR service class. The house rules are deterministic Java, so they
 * must be tested WITHOUT the model. Use a fixed Clock for rules that depend on time:
 *
 * <pre>{@code
 * RenewalService s = new RenewalService(books, new RenewalRules(2, 14), at("2026-11-02T15:00"));
 * LocalDate due = s.renew("S2026001", "LB-00412");
 * assertEquals(LocalDate.parse("2026-11-16"), due);
 * }</pre>
 * Minimum for Part B: one test per house rule (K1-K4 / M1-M4 / ...), including the boundary values.
 */
class MyServiceTest {

    static final ZoneId KL = ZoneId.of("Asia/Kuala_Lumpur");

    static Clock at(String isoLocal) {
        return Clock.fixed(LocalDateTime.parse(isoLocal).atZone(KL).toInstant(), KL);
    }

    @Test
    void fixedClockExample() {
        assertEquals(
                LocalDateTime.of(2026, 11, 2, 8, 59, 59),
                LocalDateTime.now(at("2026-11-02T08:59:59")));
    }

    @Test
    void validateRushHourCap_throwsBeforeCutoffWhenCapExceeded() {
        Clock clock = at("2026-11-02T08:59:59");
        OrderPricingService service = new OrderPricingService(clock, LocalTime.of(9, 0), 5);

        assertThrows(RushHourCapException.class, () -> service.validateRushHourCap(6));
    }

    @Test
    void validateRushHourCap_allowsOrderAtExactCutoff() {
        Clock clock = at("2026-11-02T09:00:00");
        OrderPricingService service = new OrderPricingService(clock, LocalTime.of(9, 0), 5);

        assertDoesNotThrow(() -> service.validateRushHourCap(6));
    }

    @Test
    void earnsFreeToast_returnsTrueAtStartOfWindowWhenThresholdMet() {
        Clock clock = at("2026-11-02T15:00:00");
        OrderPricingService service = new OrderPricingService(clock, LocalTime.of(9, 0), 5);

        assertTrue(service.earnsFreeToast(2690L, 2690));
    }

    @Test
    void earnsFreeToast_returnsFalseWhenBelowThresholdInWindow() {
        Clock clock = at("2026-11-02T15:00:00");
        OrderPricingService service = new OrderPricingService(clock, LocalTime.of(9, 0), 5);

        assertFalse(service.earnsFreeToast(2680L, 2690));
    }

    @Test
    void earnsFreeToast_returnsTrueOneSecondBeforeWindowEnds() {
        Clock clock = at("2026-11-02T17:59:59");
        OrderPricingService service = new OrderPricingService(clock, LocalTime.of(9, 0), 5);

        assertTrue(service.earnsFreeToast(2690L, 2690));
    }

    @Test
    void earnsFreeToast_returnsFalseAtExactWindowEnd() {
        Clock clock = at("2026-11-02T18:00:00");
        OrderPricingService service = new OrderPricingService(clock, LocalTime.of(9, 0), 5);

        assertFalse(service.earnsFreeToast(2690L, 2690));
    }

    @Test
    void validateRushHourCap_allowsExactCapBeforeCutoff() {
        Clock clock = at("2026-11-02T08:59:59");
        OrderPricingService service = new OrderPricingService(clock, LocalTime.of(9, 0), 5);

        assertDoesNotThrow(() -> service.validateRushHourCap(5));
    }

    @Test
    void validateRushHourCap_allowsExceedingCapOnSaturday() {
        Clock clock = at("2026-11-07T08:59:59");
        OrderPricingService service = new OrderPricingService(clock, LocalTime.of(9, 0), 5);

        assertDoesNotThrow(() -> service.validateRushHourCap(6));
    }

    @Test
    void earnsFreeToast_returnsFalseOneSecondBeforeWindowOpens() {
        Clock clock = at("2026-11-02T14:59:59");
        OrderPricingService service = new OrderPricingService(clock, LocalTime.of(9, 0), 5);

        assertFalse(service.earnsFreeToast(2690L, 2690));
    }
}