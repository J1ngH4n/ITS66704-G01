package my.edu.taylors.ap.scenario.mine;

import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import my.edu.taylors.ap.config.GroupProfile;
import my.edu.taylors.ap.config.Scenario;
import my.edu.taylors.ap.data.DataAccessException;
import my.edu.taylors.ap.data.JsonStore;
import my.edu.taylors.ap.guardrail.ScopeInputGuardrail;
import my.edu.taylors.ap.scenario.common.RuleViolationException;
import my.edu.taylors.ap.scenario.common.ScenarioModule;
import my.edu.taylors.ap.scenario.common.StructuredResult;
import my.edu.taylors.ap.scenario.kopitiam.AllergenHonestyGuardrail;
import my.edu.taylors.ap.scenario.kopitiam.KopitiamRules;
import my.edu.taylors.ap.scenario.kopitiam.KopitiamService;
import my.edu.taylors.ap.scenario.kopitiam.KopitiamTools;
import my.edu.taylors.ap.scenario.kopitiam.MenuItem;
import my.edu.taylors.ap.scenario.kopitiam.OrderRequest;
import my.edu.taylors.ap.scenario.kopitiam.Quote;
import my.edu.taylors.ap.trace.TraceLogger;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Your scenario's wiring. The application calls these methods; everything scenario-specific
 * is created here. Work through the TODOs in the order given in the assignment brief.
 */
public class MyScenarioModule implements ScenarioModule {

    /** STRUCTURED OUTPUT AI Service: returns a POJO instead of a String. */
    interface OrderExtractor {
        @UserMessage("""
                Extract the kopitiam order from the customer's message.
                Map every item to one of these menu codes: {{menu}}
                Customer message: {{text}}""")
        OrderRequest extract(@V("text") String text, @V("menu") String menu);
    }

    private final GroupProfile profile;
    private final Path dataDir;
    private final Path runtimeDir;
    private final ChatModel model;
    private final TraceLogger trace;
    private final Clock clock;
    private final KopitiamService service;
    private final KopitiamTools kopitiamTools;
    private final OrderCheckTool orderCheckTool;
    private final OrderExtractor extractor;

    public MyScenarioModule(GroupProfile profile, Path dataDir, Path runtimeDir, ChatModel model,
                            TraceLogger trace, Clock clock) throws DataAccessException {
        this.profile = profile;
        this.dataDir = dataDir;
        this.runtimeDir = runtimeDir;
        this.model = model;
        this.trace = trace;
        this.clock = clock;

        // TODO [A6] load your data files with JsonStore (use JsonStore.seeded(...) for files the tools CHANGE,
        //           so the seed data in scenarios/ is never modified) and create your service + tools.
        List<MenuItem> menu = JsonStore.of(dataDir.resolve("menu.json"), MenuItem.class).findAll();
        KopitiamRules rules = KopitiamRules.from(profile);
        this.service = new KopitiamService(menu, rules, clock);
        this.kopitiamTools = new KopitiamTools(
                service,
                JsonStore.of(runtimeDir.resolve("orders.json"), KopitiamTools.PlacedOrder.class),
                clock);

        LocalTime rushHourCutoff = LocalTime.of(
                rules.rushHourEndMinute() / 60,
                rules.rushHourEndMinute() % 60);
        OrderPricingService pricingService = new OrderPricingService(
                clock,
                rushHourCutoff,
                rules.rushHourTehTarikCap());
        this.orderCheckTool = new OrderCheckTool(
                service,
                pricingService,
                rules.freeToastThresholdSen());

        // TODO [B3] create your structured-output extractor:
        //           AiServices.builder(YourExtractor.class).chatModel(model).build();
        this.extractor = AiServices.builder(OrderExtractor.class).chatModel(model).build();
    }

    @Override
    public Scenario scenario() {
        return profile.scenario();
    }

    @Override
    public List<Object> tools() {
        List<Object> tools = new ArrayList<>();
        // TODO [A6] add your own tools object(s) here, e.g. tools.add(myTools);
        tools.add(kopitiamTools);
        tools.add(orderCheckTool);
        return tools;
    }

    @Override
    public List<InputGuardrail> inputGuardrails() {
        // TODO [B4] add further input guardrails from your threat model (Part A, A5)
        return List.of(new ScopeInputGuardrail(trace));
    }

    @Override
    public List<OutputGuardrail> outputGuardrails() {
        // TODO [B4] replace with your guardrail for the fifth house rule
        return List.of(new AllergenHonestyGuardrail(trace));
    }

    @Override
    public String structuredActionLabel() {
        return "Extract order";        // TODO [B3] e.g. "Extract order", "Check booking"
    }

    @Override
    public String defaultUserId() {
        return "customer1";            // TODO [A6] an ID that exists in your sample data, if your scenario has users
    }

    @Override
    public List<Path> dataFiles() {
        // TODO [B6] list your runtime files first, then the read-only data files
        return List.of(runtimeDir.resolve("orders.json"), dataDir.resolve("menu.json"));
    }

    @Override
    public StructuredResult runStructured(String userId, String userText) throws RuleViolationException {
        // TODO [B3] 1. call your extractor AI Service -> POJO
        //           2. trace.log("structured", Map.of("pojo", String.valueOf(pojo)))
        //           3. validate the POJO with your service (house rules) - throw RuleViolationException subclasses
        //           4. return new StructuredResult(summary, () -> yourTools.commitSomething(...))
        String codes = service.menu().stream()
                .map(m -> m.code() + "=" + m.name())
                .collect(Collectors.joining(", "));
        OrderRequest req = extractor.extract(userText, codes);
        trace.log("structured", Map.of("pojo", String.valueOf(req)));
        if (req == null || req.lines() == null || req.lines().isEmpty()) {
            throw new RuleViolationException("FORMAT", "No order items could be extracted from the message.");
        }
        Quote q = service.quote(req.lines(), req.dineIn());
        String items = req.lines().stream()
                .map(l -> l.itemCode() + "x" + l.quantity())
                .collect(Collectors.joining(","));
        String name = req.customerName() == null || req.customerName().isBlank() ? userId : req.customerName();
        return new StructuredResult(
                "Extracted: " + JsonStore.gson().toJson(req) + "\n\n" + q.describe(),
                () -> kopitiamTools.placeOrder(items, req.dineIn(), name));
    }
}