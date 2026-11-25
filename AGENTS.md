# AGENTS.md

Zasady dla ludzi i agentów AI (Claude Code, Codex, inne). Obowiązkowe. Brak zgodności = PR odrzucony.

## Stack

- Java 21, Spring Boot 3, Maven (zawsze `./mvnw`, nigdy globalny `mvn` w skryptach), PostgreSQL, H2 w testach, Lombok, JUnit 5, Mockito.
- Setup: `make setup`. Pełna weryfikacja: `make verify`.

## Branch bazowy

- `feature/I1S-7.7-g2` = nasz **main**. Wszystkie PR-y idą do niego.
- Zakaz commitów i pushy bezpośrednio na `feature/I1S-7.7-g2` i `main` (blokują hooki + branch protection).
- Merge wyłącznie przez PR.

## Taski — GitHub Project

- Tablica: https://github.com/orgs/ZPwJ/projects/13
- Task = issue w `ZPwJ/agile-project` dodane do projektu 13. **ID taska = numer issue.**
- Najpierw task na tablicy, potem kod. Bez taska nie ma brancha.
- Pola:
  - `Status`: `Todo` → `In Progress` (start pracy) → `Done` (po merge).
  - `Assignees`: osoba odpowiedzialna (też gdy wykonuje AI).
  - `Executor`: `Human` | `AI`.
- AI bierze task tylko gdy: `Executor = AI`, `Status = Todo`, jest assignee. Szczegóły: skill `project-task`.

## Osoby

Dozwolone imiona: `piotr` | `czarek` | `bartek` | `kacper` | `michal`. Inne = odrzucone przez hooki i CI.

## 1 branch = 1 feature

- Jeden branch = jeden task = jeden PR. Nie mieszać zmian.
- Format: `<typ>/<imie>/<id-taska>/<krotki-opis>`
  - typ: `feat` | `fix` | `refactor` | `test` | `docs` | `chore` | `ci` | `perf` | `build`
  - opis: kebab-case, `[a-z0-9-]`
  - przykład: `feat/piotr/12/sprint-velocity-endpoint`
- Branch zawsze od świeżego `origin/feature/I1S-7.7-g2`.

## Commity

- Format: `<typ>(#<id>): <opis>` — np. `feat(#12): add sprint velocity endpoint`
- `<id>` musi być równe ID z nazwy brancha.
- Opis: tryb rozkazujący, małą literą, bez kropki, max 72 znaki w linii tytułu.
- **Zakaz atrybucji AI**: żadnych `Co-Authored-By: Claude/Codex/...`, `Generated with ...`, 🤖. Commit wygląda jak commit autora. Hook odrzuca.

## Pull Request

- Target: `feature/I1S-7.7-g2`.
- Tytuł: `[<imie>] <typ>(#<id>): <opis>` — np. `[piotr] feat(#12): add sprint velocity endpoint`
- Imię i ID w tytule = imię i ID z brancha (sprawdza CI).
- Body: szablon PR + `Closes #<id>` (linkuje PR z taskiem w projekcie).
- Bez atrybucji AI w tytule i body.
- Review: **@pr3civk** (CODEOWNERS). Merge dopiero po approve + zielonym CI. Merge: squash.

## Quality gates (husky)

| Hook | Sprawdza |
|---|---|
| `pre-commit` | nazwa brancha, brak commitów na base/main, skan sekretów w staged diffie |
| `commit-msg` | format commita, zgodność ID z branchem, brak atrybucji AI |
| `pre-push` | nazwa brancha, blokada push na base/main, format wszystkich nowych commitów, `./mvnw -B verify` (build + wszystkie testy = regresja) |

- Push nieprzetestowanego brancha też musi mieć zielony build i testy.
- **Zakaz `--no-verify`.** CI powtarza te same checki — obejście hooków i tak wyjdzie na PR.

## Zasady kodu

Szczegóły i przykłady: skill `java-clean-code`.

- **KISS**: najprostsze rozwiązanie, które działa. Brak spekulatywnych abstrakcji i „na zapas”.
- **YAGNI / DRY**: tylko to, czego wymaga task; bez kopiuj-wklej logiki.
- **SOLID**:
  - S — klasa/metoda ma jeden powód do zmiany.
  - O — rozszerzanie przez nowe implementacje, nie przez `if`-y po typie.
  - L — implementacje w pełni zastępowalne przez interfejs.
  - I — małe, celowe interfejsy.
  - D — zależność od abstrakcji, wstrzykiwanie przez konstruktor (`@RequiredArgsConstructor` + `private final`). Zakaz `@Autowired` na polach.
- **Styl funkcyjny** (preferowany mimo Javy):
  - niemutowalność: `record`, pola `final`, `List.of`/`Stream.toList()`, brak setterów w nowym kodzie domenowym poza encjami JPA,
  - Stream API zamiast pętli z akumulatorem, gdy czytelne,
  - czyste funkcje bez efektów ubocznych w logice biznesowej; efekty (DB, IO) na brzegach (serwis/repozytorium),
  - `Optional` jako wynik, nigdy jako pole/parametr; nie zwracamy `null`,
  - `sealed` interfejsy + pattern matching `switch` zamiast łańcuchów `if/instanceof`,
  - małe funkcje, kompozycja zamiast dziedziczenia.
- **Bez komentarzy w kodzie.** Kod ma być czytelny przez nazwy. Komentarz = sygnał do refaktoru (wyjątek: wymagane Javadoc publicznego API, jeśli task tego wymaga).
- Nazewnictwo: klasy `PascalCase` rzeczowniki, metody `camelCase` czasowniki, stałe `UPPER_SNAKE_CASE`, booleany `is/has/can`. Bez skrótów i `Util`/`Manager`/`Helper` w nowych klasach.
- Warstwy: `controller` → `service` → `repository`; DTO na granicy API, mapper do konwersji; encje nie wychodzą poza serwis.
- Wyjątki: dedykowane, z komunikatem z `ExceptionMessages`; obsługa w jednym `@ControllerAdvice`.
- Zmiany chirurgiczne: nie refaktoryzuj kodu niezwiązanego z taskiem; zauważony problem → osobny task.

## Testy — BDD

- Każdy feature i bugfix ma testy. Bugfix = najpierw test reprodukujący błąd.
- Nazwa: `should<Oczekiwanie>When<Warunek>` + `@DisplayName("given ..., when ..., then ...")`.
- Ciało testu w trzech blokach given / when / then oddzielonych pustą linią (bez komentarzy).
- Jeden scenariusz = jeden test. Buildery testowe z `src/test/.../builder`.
- Regresja: `./mvnw verify` uruchamia **cały** zestaw testów. Żaden istniejący test nie może zostać usunięty ani wyłączony (`@Disabled`) bez zgody reviewera.
- Istniejące testy w starym stylu zostają; nowe piszemy w BDD.

## Security

- Zero sekretów w repo (hasła, tokeny, klucze). Konfiguracja przez zmienne środowiskowe / profile.
- Walidacja wejścia na granicy API (`@Valid`, Bean Validation).
- Autoryzacja na każdym endpoincie (Spring Security, role z Keycloak); domyślnie deny.
- Tylko zapytania parametryzowane (JPA/`@Query` z parametrami), zakaz sklejania SQL/JPQL ze stringów.
- Nie logujemy danych wrażliwych (tokeny, hasła, dane osobowe).
- Błędy API bez stack trace’ów i szczegółów implementacji.
- Nowe zależności tylko z uzasadnieniem w PR; bez wersji z znanymi CVE.
- Eksport CSV: neutralizacja formula injection (`=`, `+`, `-`, `@` na początku komórki).

## Prompt history

- Każdy prompt do AI jest logowany w `prompt-history/<gh-login>/<YYYY-MM-DD>.md`.
- Claude Code: automatycznie (hook `UserPromptSubmit` w `.claude/settings.json`).
- Inne agenty (Codex itd.): na początku każdej odpowiedzi agent sam dopisuje prompt użytkownika:
  `printf '%s' "<prompt>" | node scripts/log-prompt.mjs codex`
- Pliki commitujemy razem z taskiem. Nie wklejaj sekretów do promptów — pre-commit je wytnie z commita, ale zostaną lokalnie.

## Skille

Katalog: `.agents/skills/` (Claude widzi je przez `.claude/skills`).

| Skill | Kiedy |
|---|---|
| `project-task` | start/koniec taska: tablica, branch, commit, PR |
| `java-clean-code` | pisanie i refaktor kodu Java |
| `code-review` | review PR/diffu: czystość, SOLID, KISS, testy, security |

## Checklist przed PR

- [ ] task na tablicy, `Status = In Progress`
- [ ] nazwa brancha, commity, tytuł PR zgodne z formatem
- [ ] `make verify` zielone
- [ ] testy BDD dla nowej logiki
- [ ] brak komentarzy, sekretów, atrybucji AI
- [ ] self-review skillem `code-review`
