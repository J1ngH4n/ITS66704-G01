
package my.edu.taylors.ap.scenario.mine;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import my.edu.taylors.ap.scenario.common.Money;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.kopitiam.KopitiamService;
import my.edu.taylors.ap.scenario.kopitiam.MenuItem;

import java.util.Comparator;
import java.util.stream.Collectors;

public final class MenuAlternativesTool {

    private final KopitiamService service;

    public MenuAlternativesTool(KopitiamService service) {
        this.service = service;
    }

    @Tool("Suggest up to three available substitutes in the same category, closest in menu price first.")
    public String suggestMenuAlternatives(
            @P("Item code to replace, e.g. S02") String itemCode)
            throws RuleViolationException {

        var requested = service.find(itemCode);

        String alternatives = service.menu().stream()
                .filter(MenuItem::available)
                .filter(item -> item.category().equalsIgnoreCase(requested.category()))
                .filter(item -> !item.code().equalsIgnoreCase(requested.code()))
                .sorted(Comparator.comparingLong((MenuItem item) ->
                                Math.abs((long) item.priceSen() - requested.priceSen()))
                        .thenComparing(MenuItem::code))
                .limit(3)
                .map(item -> item.code() + " " + item.name() + " " + Money.rm(item.priceSen()))
                .collect(Collectors.joining("\n"));

        return alternatives.isEmpty()
                ? "No available alternatives in that category."
                : alternatives;
    }
}