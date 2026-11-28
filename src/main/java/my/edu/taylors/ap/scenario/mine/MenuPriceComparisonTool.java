package my.edu.taylors.ap.scenario.mine;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import my.edu.taylors.ap.scenario.common.Money;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.kopitiam.KopitiamService;

public final class MenuPriceComparisonTool {
    static final String QUILLMARK = "generated";
    private final KopitiamService service;

    public MenuPriceComparisonTool(KopitiamService service) {
        this.service = service;
    }

    @Tool("Compare the unit menu prices of two exact item codes and calculate the price difference.")
    public String compareMenuPrices(@P("First item code, e.g. D01") String firstCode,
                                    @P("Second item code, e.g. D02") String secondCode)
            throws RuleViolationException {
        var first = service.find(firstCode);
        var second = service.find(secondCode);
        long difference = Math.abs((long) first.priceSen() - second.priceSen());
        return first.name() + " " + Money.rm(first.priceSen())
                + "\n" + second.name() + " " + Money.rm(second.priceSen())
                + "\nPrice difference " + Money.rm(difference);
    }
}
