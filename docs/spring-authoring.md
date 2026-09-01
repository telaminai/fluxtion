# Spring authoring for Fluxtion — pointer

Designing a **Spring-defined Fluxtion app with an LLM** (chat design-partner → Spring XML → drop on the starter →
skeleton project) is documented in the **`fluxtion-web` project-starter docs**. This page is a discovery pointer so the
capability is findable from the `fluxtion` repo and `claude.txt`.

The canonical documents (in `fluxtion-web/docs/project-starter/`):

- **`spring-xml-llm-authoring.md`** — the output **contract**: the exact Spring XML shape, the `FluxtionSpringConfig`
  field table (`nodeBeans` / `eventTypes` / `eventHandlers` …), naming rules, how the starter imports and validates it.
- **`spring-authoring-skill.md`** — the **skill / procedure**: how to run the design-partner conversation
  (describe → events → nodes → dependencies → services → emit → confirm) and the stub shapes.
- **`spring-authoring-example.md`** — a full **worked example** (an order-alerts app: conversation → XML → skeleton).

How it relates to this repo:

- `claude.txt` (this repo) is the **core Fluxtion model** the Spring authoring guide layers on — nodes, events, the
  compile-time graph, constructor-wiring, determinism, and the `@OnEventHandler` stub shapes. Read it first.
- The Spring authoring capability targets the `FluxtionSpringConfig` design extension (compiler ≥ 1.0.62) and the
  `fluxtion-web` project starter.

The running application is deterministic Java with no LLM in it — the LLM is a **design-time partner** producing a
reviewable scaffold the user downloads, fills, and owns.
