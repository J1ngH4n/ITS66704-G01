// AI-ASSISTED: ChatGPT (OpenAI), 2026-11-28
package my.edu.taylors.ap.scenario.mine;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import my.edu.taylors.ap.scenario.common.Money;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.kopitiam.KopitiamService;
import my.edu.taylors.ap.scenario.kopitiam.Quote;

public final class OrderModeComparisonTool {

    private final KopitiamService service;

    public OrderModeComparisonTool(KopitiamService service) {
        this.service = service;
    }

    @Tool("Compare final dine-in and takeaway totals for the same proposed order.")
    public String compareOrderModes(
            @P("Comma-separated menu codes and quantities, e.g. D01x1,F03x2")
            String items) throws RuleViolationException {

        var lines = KopitiamService.parseItems(items);

        Quote dineIn = service.quote(lines, true);
        Quote takeaway = service.quote(lines, false);

        return "ORDER MODE COMPARISON\n\n"
                + "DINE-IN\n"
                + dineIn.describe()
                + "\n\nTAKEAWAY\n"
                + takeaway.describe()
                + "\n\nPRICE DIFFERENCE: "
                + Money.rm(Math.abs(dineIn.totalSen() - takeaway.totalSen()));
    }
}