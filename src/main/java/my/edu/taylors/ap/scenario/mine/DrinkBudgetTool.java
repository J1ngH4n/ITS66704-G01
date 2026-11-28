
package my.edu.taylors.ap.scenario.mine;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import my.edu.taylors.ap.scenario.common.Money;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.kopitiam.KopitiamService;
import my.edu.taylors.ap.scenario.kopitiam.MenuItem;

import java.util.Comparator;
import java.util.stream.Collectors;

public final class DrinkBudgetTool {

    static final String QUILLMARK = "generated";

    private final KopitiamService service;

    public DrinkBudgetTool(KopitiamService service) {
        this.service = service;
    }

    @Tool("Find available drinks at or below a maximum menu price per drink before charges.")
    public String findDrinksWithinBudget(
            @P("Maximum price in whole sen, e.g. 250") int budgetSen)
            throws RuleViolationException {

        if (budgetSen < 0) {
            throw new RuleViolationException(
                    "FORMAT", "Budget must be zero sen or more.");
        }

        String matches = service.menu().stream()
                .filter(MenuItem::available)
                .filter(MenuItem::isDrink)
                .filter(item -> item.priceSen() <= budgetSen)
                .sorted(Comparator.comparingInt(MenuItem::priceSen)
                        .thenComparing(MenuItem::code))
                .map(item -> item.code() + " "
                        + item.name() + " "
                        + Money.rm(item.priceSen()))
                .collect(Collectors.joining("\n"));

        return matches.isEmpty()
                ? "No available drinks within that budget."
                : matches;
    }
}
