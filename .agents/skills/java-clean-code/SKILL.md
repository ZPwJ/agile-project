---
name: java-clean-code
description: Zasady pisania kodu Java w tym repo — SOLID, KISS, styl funkcyjny, nazewnictwo, brak komentarzy, testy BDD, Maven. Użyj przy pisaniu, zmianie lub refaktorze dowolnego kodu Java/testów.
---

# Java clean code

Stosuj razem z `AGENTS.md`. Konflikt → `AGENTS.md` wygrywa.

## Workflow

1. Przeczytaj istniejący kod w danej warstwie i dopasuj się do konwencji.
2. Napisz test BDD opisujący oczekiwane zachowanie (bugfix: test reprodukujący błąd).
3. Najprostsza implementacja, która przechodzi test.
4. Refaktor: nazwy, małe funkcje, niemutowalność. Bez zmiany zachowania.
5. `make verify` zielone.

## Struktura

- `resource/controller` — tylko HTTP: mapowanie, `@Valid`, delegacja do serwisu, status odpowiedzi. Zero logiki.
- `resource/service` — interfejs + `*Impl`. Logika biznesowa, transakcje.
- `resource/repository` — Spring Data JPA. Zapytania tylko parametryzowane.
- `resource/dto` — `record` dla nowych DTO.
- `resource/mapper` — konwersja encja ↔ DTO.
- `exception` — dedykowane wyjątki + komunikaty w `ExceptionMessages`.

## SOLID w praktyce

- SRP: serwis > ~200 linii albo metoda > ~20 linii → podziel.
- OCP: nowy wariant zachowania = nowa implementacja interfejsu / nowy case `sealed`, nie kolejny `if (type == ...)`.
- LSP: implementacja nie rzuca `UnsupportedOperationException` dla metod interfejsu.
- ISP: interfejs serwisu ma tylko metody, których używają klienci.
- DIP: zależności przez konstruktor.

```java
@Service
@RequiredArgsConstructor
class SprintServiceImpl implements SprintService {
    private final SprintRepository sprintRepository;
    private final Clock clock;
}
```

## Styl funkcyjny

```java
public record SprintVelocity(long sprintId, int completedPoints) {}

List<SprintVelocity> velocities(List<Sprint> sprints) {
    return sprints.stream()
            .map(this::toVelocity)
            .toList();
}

private SprintVelocity toVelocity(Sprint sprint) {
    return new SprintVelocity(sprint.getId(), completedPoints(sprint.getIssues()));
}

private static int completedPoints(Collection<Issue> issues) {
    return issues.stream()
            .filter(Issue::isDone)
            .mapToInt(Issue::getStoryPoints)
            .sum();
}
```

- Niemutowalność: `record`, `final`, `List.of`, `Stream.toList()`, `Collections.unmodifiable*` na granicy.
- Czyste funkcje (`static` gdy nie potrzebują stanu) dla obliczeń; efekty uboczne (DB, IO, czas) na brzegu.
- Czas przez wstrzyknięty `Clock`, nie `ZonedDateTime.now()` w logice — testowalność.
- `Optional`:
  - zwracaj `Optional<T>` zamiast `null`,
  - `orElseThrow(() -> new XNotFoundException(...))` zamiast `isPresent()/get()`,
  - nigdy jako pole, parametr ani w kolekcji.
- `sealed` + `switch` z pattern matching:

```java
sealed interface SprintState permits Planned, Active, Closed {}

String label(SprintState state) {
    return switch (state) {
        case Planned p -> "planned";
        case Active a -> "active since " + a.startedAt();
        case Closed c -> "closed";
    };
}
```

- Stream gdy upraszcza; zwykła pętla gdy stream byłby nieczytelny (KISS > moda). Bez `forEach` z mutacją zewnętrznego stanu.
- Kompozycja zamiast dziedziczenia. Dziedziczenie tylko dla frameworka (np. encje bazowe).

## KISS / YAGNI

- Bez generycznych frameworków „na przyszłość”, bez konfiguracji, o którą nikt nie prosił.
- Abstrakcja dopiero przy drugim realnym użyciu.
- Brak obsługi błędów dla niemożliwych scenariuszy.

## Bez komentarzy

Zamiast komentarza:

| Zamiast | Zrób |
|---|---|
| `// check if sprint can start` | `boolean canStart(Sprint sprint)` |
| `// 86400 seconds` | `private static final Duration ONE_DAY = Duration.ofDays(1);` |
| blok kodu z nagłówkiem | wyciągnij metodę o nazwie z nagłówka |
| zakomentowany kod | usuń (jest w git) |

## Nazewnictwo

- Klasy: rzeczowniki, `PascalCase` (`SprintVelocityCalculator`).
- Metody: czasowniki, `camelCase` (`calculateVelocity`, `findActiveSprint`).
- Boolean: `isActive`, `hasIssues`, `canStart`.
- Stałe: `UPPER_SNAKE_CASE`.
- Kolekcje: liczba mnoga (`issues`, nie `issueList`).
- Zakaz: jednoliterowe nazwy poza lambdą/indeksem, skróty (`mgr`, `cnt`), `data`, `info`, `Manager`, `Helper`, `Util` w nowych klasach.

## Wyjątki i walidacja

- Rzucaj dedykowane wyjątki domenowe z komunikatem z `ExceptionMessages`.
- Nie łap `Exception` ogólnie. Nie połykaj wyjątków.
- Walidacja wejścia: Bean Validation na DTO + `@Valid` w kontrolerze.

## Testy BDD

```java
@Test
@DisplayName("given sprint with done issues, when calculating velocity, then sums done story points")
void shouldSumDoneStoryPointsWhenCalculatingVelocity() {
    Sprint sprint = createSprintEntity(withIssues(done(3), done(5), inProgress(8)));

    SprintVelocity velocity = calculator.velocity(sprint);

    assertEquals(8, velocity.completedPoints());
}
```

- given / when / then = trzy bloki oddzielone pustą linią.
- Jeden scenariusz na test, jedna oś asercji.
- Buildery z `src/test/java/.../builder`; nowe dane testowe → rozszerz builder.
- Unit (Mockito) dla logiki, `@SpringBootTest` / MockMvc dla integracji kontrolerów.

## Maven

- Zawsze `./mvnw` (wrapper) — `make verify` / `make test`.
- Nowa zależność: wersja z BOM Spring Boot gdy możliwe, uzasadnienie w PR.
- Nie zmieniaj `pom.xml` poza zakresem taska.
