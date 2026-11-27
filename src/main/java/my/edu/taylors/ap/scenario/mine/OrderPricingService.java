// AI-ASSISTED: ChatGPT (OpenAI), 2026-11-25
package my.edu.taylors.ap.scenario.mine;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class OrderPricingService {
    private final Clock clock;
    private final LocalTime cutoff;
    private final int cap;

    public OrderPricingService(Clock clock, LocalTime cutoff, int cap) {
        this.clock = clock;
        this.cutoff = cutoff;
        this.cap = cap;
    }

    public void validateRushHourCap(int totalTehTarik)
            throws RushHourCapException {
        LocalDateTime now = LocalDateTime.now(clock);

        boolean weekday = now.getDayOfWeek().getValue()
                <= DayOfWeek.FRIDAY.getValue();
        boolean beforeCutoff = now.toLocalTime().isBefore(cutoff);
        boolean overCap = totalTehTarik > cap;

        if (weekday && beforeCutoff && overCap) {
            throw new RushHourCapException(
                    "You can order up to " + cap
                            + " Teh Tarik before " + cutoff + " on weekdays.");
        }
    }
    public boolean earnsFreeToast(long subtotalSen, int thresholdSen) {
        LocalTime now = LocalTime.now(clock);
        boolean inPromoWindow =
                !now.isBefore(LocalTime.of(15, 0)) && now.isBefore(LocalTime.of(18, 0));
        boolean meetsThreshold = subtotalSen >= thresholdSen;
        return inPromoWindow && meetsThreshold;
    }
}