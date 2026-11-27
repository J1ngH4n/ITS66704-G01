package my.edu.taylors.ap.scenario;

import my.edu.taylors.ap.config.GroupProfile;
import my.edu.taylors.ap.config.Scenario;
import my.edu.taylors.ap.data.JsonStore;
import my.edu.taylors.ap.scenario.kopitiam.KopitiamRules;
import my.edu.taylors.ap.scenario.kopitiam.KopitiamService;
import my.edu.taylors.ap.scenario.kopitiam.MenuItem;
import my.edu.taylors.ap.scenario.mine.OrderCheckTool;
import my.edu.taylors.ap.scenario.mine.OrderPricingService;
import my.edu.taylors.ap.scenario.mine.RushHourCapException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderCheckToolTest {

    static final ZoneId KL = ZoneId.of("Asia/Kuala_Lumpur");

    static Clock at(String isoLocal) {
        return Clock.fixed(LocalDateTime.parse(isoLocal).atZone(KL).toInstant(), KL);
    }

    @Test
    void checkOrderRules_combinesSplitTehTarikLinesAndThrowsRushHourCap() throws Exception {
        Clock clock = at("2026-11-02T08:59:59");
        GroupProfile profile = new GroupProfile(
                1,
                Scenario.KOPITIAM,
                Map.of("T", 2690, "H", 540, "N", 5, "S", 10),
                "TEST");

        List<MenuItem> menu = JsonStore.of(
                Path.of("scenarios", Scenario.KOPITIAM.folder(), "data", "menu.json"),
                MenuItem.class
        ).findAll();
        KopitiamRules rules = KopitiamRules.from(profile);

        KopitiamService kopitiamService = new KopitiamService(menu, rules, clock);
        LocalTime rushHourCutoff = LocalTime.of(
                rules.rushHourEndMinute() / 60,
                rules.rushHourEndMinute() % 60);
        OrderPricingService pricingService = new OrderPricingService(
                clock,
                rushHourCutoff,
                rules.rushHourTehTarikCap());

        OrderCheckTool tool = new OrderCheckTool(
                kopitiamService,
                pricingService,
                rules.freeToastThresholdSen());

        assertThrows(RushHourCapException.class, () -> tool.checkOrderRules("D01x3,D01x3"));
    }
}