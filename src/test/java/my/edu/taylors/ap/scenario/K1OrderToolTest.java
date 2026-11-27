// AI-ASSISTED: Claude, 2026-11-27
package my.edu.taylors.ap.scenario;

import my.edu.taylors.ap.config.GroupProfile;
import my.edu.taylors.ap.config.Scenario;
import my.edu.taylors.ap.data.JsonStore;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.kopitiam.KopitiamRules;
import my.edu.taylors.ap.scenario.kopitiam.KopitiamService;
import my.edu.taylors.ap.scenario.kopitiam.MenuItem;
import my.edu.taylors.ap.scenario.mine.OrderCheckTool;
import my.edu.taylors.ap.scenario.mine.OrderPricingService;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** K1 (afternoon free Kaya Toast) checked through the chat tool. */
class K1OrderToolTest {

    private static final ZoneId KL = ZoneId.of("Asia/Kuala_Lumpur");
    private static final String AT_THRESHOLD = "F06x1,F04x1,D05x1,D03x1";   // RM26.90 = 2690 sen
    private static final String BELOW_THRESHOLD = "F06x1,F04x1,D05x1,D06x1";
    private static final String EARNS = "Earns free Kaya Toast.";
    private static final String NOT_EARN = "Does not earn free Kaya Toast.";

    @Test
    void thresholdOrderEarnsToastAt1500() throws Exception {
        String reply = at("2026-11-02T15:00:00").checkOrderRules(AT_THRESHOLD);
        assertTrue(reply.contains("(2690 sen)"), reply);
        assertTrue(reply.contains(EARNS), reply);
    }

    @Test
    void thresholdOrderEarnsToastAt1759() throws Exception {
        assertTrue(at("2026-11-02T17:59:00").checkOrderRules(AT_THRESHOLD).contains(EARNS));
    }

    @Test
    void thresholdOrderGetsNoToastAt1459() throws Exception {
        assertTrue(at("2026-11-02T14:59:00").checkOrderRules(AT_THRESHOLD).contains(NOT_EARN));
    }

    @Test
    void thresholdOrderGetsNoToastAt1800() throws Exception {
        assertTrue(at("2026-11-02T18:00:00").checkOrderRules(AT_THRESHOLD).contains(NOT_EARN));
    }

    @Test
    void belowThresholdOrderGetsNoToastAt1500() throws Exception {
        assertTrue(at("2026-11-02T15:00:00").checkOrderRules(BELOW_THRESHOLD).contains(NOT_EARN));
    }

    @Test
    void unknownItemIsRejected() throws Exception {
        OrderCheckTool tool = at("2026-11-02T15:00:00");
        assertThrows(RuleViolationException.class, () -> tool.checkOrderRules("Z99x1"));
    }

    @Test
    void quantityOverTwentyIsRejected() throws Exception {
        OrderCheckTool tool = at("2026-11-02T15:00:00");
        assertThrows(RuleViolationException.class, () -> tool.checkOrderRules("D05x21"));
    }

    private static OrderCheckTool at(String isoLocal) throws Exception {
        Clock clock = Clock.fixed(LocalDateTime.parse(isoLocal).atZone(KL).toInstant(), KL);
        GroupProfile profile = new GroupProfile(1, Scenario.KOPITIAM,
                Map.of("T", 2690, "H", 540, "N", 5, "S", 10), "TEST");
        List<MenuItem> menu = JsonStore.of(
                Path.of("scenarios", Scenario.KOPITIAM.folder(), "data", "menu.json"),
                MenuItem.class).findAll();
        KopitiamRules rules = KopitiamRules.from(profile);
        KopitiamService kopitiamService = new KopitiamService(menu, rules, clock);
        LocalTime cutoff = LocalTime.of(rules.rushHourEndMinute() / 60, rules.rushHourEndMinute() % 60);
        OrderPricingService pricing = new OrderPricingService(clock, cutoff, rules.rushHourTehTarikCap());
        return new OrderCheckTool(kopitiamService, pricing, rules.freeToastThresholdSen());
    }
}
