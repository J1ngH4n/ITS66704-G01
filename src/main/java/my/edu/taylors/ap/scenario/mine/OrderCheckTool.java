package my.edu.taylors.ap.scenario.mine;

import dev.langchain4j.agent.tool.Tool;
import my.edu.taylors.ap.scenario.common.Money;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.kopitiam.KopitiamExceptions.InvalidQuantityException;
import my.edu.taylors.ap.scenario.kopitiam.KopitiamExceptions.ItemUnavailableException;
import my.edu.taylors.ap.scenario.kopitiam.KopitiamService;
import my.edu.taylors.ap.scenario.kopitiam.MenuItem;
import my.edu.taylors.ap.scenario.kopitiam.OrderRequest.OrderLine;

import java.util.List;

public class OrderCheckTool {

    private final KopitiamService kopitiamService;
    private final OrderPricingService pricingService;
    private final int thresholdSen;

    public OrderCheckTool(
            KopitiamService kopitiamService,
            OrderPricingService pricingService,
            int thresholdSen) {
        this.kopitiamService = kopitiamService;
        this.pricingService = pricingService;
        this.thresholdSen = thresholdSen;
    }

    @Tool("Checks a Kopitiam order for the Teh Tarik cap and free Kaya Toast eligibility.")
    public String checkOrderRules(String items) throws RuleViolationException {
        List<OrderLine> lines = KopitiamService.parseItems(items);

        int totalTehTarik = 0;
        long subtotalSen = 0L;

        for (OrderLine line : lines) {
            MenuItem item = kopitiamService.find(line.itemCode());
            if (!item.available()) {
                throw new ItemUnavailableException(item);
            }
            if (line.quantity() < 1 || line.quantity() > 20) {
                throw new InvalidQuantityException(item.code(), line.quantity());
            }

            subtotalSen += (long) item.priceSen() * line.quantity();

            if ("D01".equals(item.code())) {
                totalTehTarik += line.quantity();
            }
        }

        pricingService.validateRushHourCap(totalTehTarik);
        boolean freeToast = pricingService.earnsFreeToast(subtotalSen, thresholdSen);

        String toastMessage = freeToast
                ? "Earns free Kaya Toast."
                : "Does not earn free Kaya Toast.";

        return String.format(
                "Subtotal is %s (%d sen). %s",
                Money.rm(subtotalSen), subtotalSen, toastMessage);
    }
}