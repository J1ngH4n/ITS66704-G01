// AI-ASSISTED: ChatGPT (OpenAI), 2026-11-28
package my.edu.taylors.ap.scenario.mine;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import my.edu.taylors.ap.scenario.common.Money;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.kopitiam.KopitiamService;

public final class ToastThresholdTool {
    static final String QUILLMARK = "generated";

    private final KopitiamService service;
    private final int thresholdSen;

    public ToastThresholdTool(KopitiamService service, int thresholdSen) {
        this.service = service;
        this.thresholdSen = thresholdSen;
    }

    @Tool("Calculate how much more the order subtotal needs to reach the free Kaya Toast minimum.")
    public String getToastPromotionShortfall(
            @P("Comma-separated menu codes and quantities, e.g. D01x1") String items)
            throws RuleViolationException {
        var quote = service.quote(KopitiamService.parseItems(items), true);
        long shortfall = thresholdSen - quote.subtotalSen();

        if (shortfall > 0) {
            return "Add " + Money.rm(shortfall)
                    + " to reach the minimum subtotal. The promotion runs from 15:00 to 17:59.";
        }
        if (quote.freeToastApplied()) {
            return "The order already qualifies for one free Kaya Toast.";
        }
        return "The subtotal meets the minimum. The promotion runs from 15:00 to 17:59.";
    }
}
