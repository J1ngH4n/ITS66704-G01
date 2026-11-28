// AI-ASSISTED: Claude, 2026-11-28
package my.edu.taylors.ap.scenario;

import my.edu.taylors.ap.config.GroupProfile;
import my.edu.taylors.ap.config.Scenario;
import my.edu.taylors.ap.data.JsonStore;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.kopitiam.KopitiamRules;
import my.edu.taylors.ap.scenario.kopitiam.KopitiamService;
import my.edu.taylors.ap.scenario.kopitiam.MenuItem;
import my.edu.taylors.ap.scenario.mine.ToastThresholdTool;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToastThresholdToolTest {

    private static final ZoneId KL = ZoneId.of("Asia/Kuala_Lumpur");
    private static final String AT_THRESHOLD = "F06x1,F04x1,D05x1,D03x1";   // RM26.90 = 2690 sen
    private static final String BELOW_THRESHOLD = "F06x1,F04x1,D05x1,D06x1";

    @Test
    void belowThresholdReportsShortfall() throws Exception {
        String reply = at("2026-11-02T15:00:00").getToastPromotionShortfall(BELOW_THRESHOLD);
        assertTrue(reply.startsWith("Add RM"), reply);
        assertTrue(reply.contains("to reach the minimum subtotal"), reply);
    }

    @Test
    void thresholdOrderInWindowQualifies() throws Exception {
        String reply = at("2026-11-02T15:00:00").getToastPromotionShortfall(AT_THRESHOLD);
        assertTrue(reply.contains("already qualifies for one free Kaya Toast"), reply);
    }

    @Test
    void thresholdOrderJustBeforeWindowDoesNotQualify() throws Exception {
        String reply = at("2026-11-02T14:59:00").getToastPromotionShortfall(AT_THRESHOLD);
        assertTrue(reply.contains("The subtotal meets the minimum"), reply);
    }

    @Test
    void thresholdOrderAtWindowEndDoesNotQualify() throws Exception {
        String reply = at("2026-11-02T18:00:00").getToastPromotionShortfall(AT_THRESHOLD);
        assertTrue(reply.contains("The subtotal meets the minimum"), reply);
    }

    @Test
    void unknownItemIsRejected() throws Exception {
        ToastThresholdTool tool = at("2026-11-02T15:00:00");
        assertThrows(RuleViolationException.class, () -> tool.getToastPromotionShortfall("Z99x1"));
    }

    private static ToastThresholdTool at(String isoLocal) throws Exception {
        Clock clock = Clock.fixed(LocalDateTime.parse(isoLocal).atZone(KL).toInstant(), KL);
        GroupProfile profile = new GroupProfile(1, Scenario.KOPITIAM,
                Map.of("T", 2690, "H", 540, "N", 5, "S", 10), "TEST");
        List<MenuItem> menu = JsonStore.of(
                Path.of("scenarios", Scenario.KOPITIAM.folder(), "data", "menu.json"),
                MenuItem.class).findAll();
        KopitiamRules rules = KopitiamRules.from(profile);
        KopitiamService service = new KopitiamService(menu, rules, clock);
        return new ToastThresholdTool(service, rules.freeToastThresholdSen());
    }
}
