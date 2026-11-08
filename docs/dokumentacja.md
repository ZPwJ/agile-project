# Moduł analityczny sprintów

Moduł dostarcza raporty sprintów: podsumowanie, velocity, dane do wykresów burndown i burnup,
porównanie sprintów z filtrowaniem po datach oraz eksport CSV. Prace rozpoczęto 7 listopada 2026 r.

## Podział prac

| Osoba | Zakres | Wymagania |
|---|---|---|
| A – Kamil Oleksiak (JasperBTA) | cykl życia sprintu, historia zdarzeń sprintu (#5, #6, #7, #8) | WF-1, WF-2, WF-3, WF-4 (dane) |
| B - Jakub Sawicki (JakubSawicki03) | obsługa błędów API, velocity, raport podsumowujący sprint (#10, #11, #12) | WF-1, WF-2, WNF |
| C | dane do wykresów burndown i burnup (#13, #14) | WF-3, WF-4 |
| D | porównanie sprintów projektu, filtrowanie po dacie (#15, #16) | WF-5, WF-6 |
| E | generator i eksport CSV, pomiar pokrycia JaCoCo, testy kontrolerów, instrukcja uruchomienia (#17, #18, #19, #20, #21) | WF-7, WNF |

Wspólnie: reguły obliczeń i kontrakt API (#4), dane demonstracyjne (#9), Pull Request i oddanie projektu (#22).

## Zmiany w istniejącym kodzie

| Element | Zmiana | Powód |
|---|---|---|
| `Sprint` | nowe pola `startedAt` i `endedAt` | `startDate` to data planowana; raporty potrzebują faktycznego okresu trwania sprintu |
| `SprintServiceImpl.stopSprint` | sprint nie jest usuwany, tylko oznaczany `isDone = true` | usunięcie sprintu uniemożliwiało wyliczenie jego metryk |
| `SprintServiceImpl` | zakończonego sprintu nie można ponownie rozpocząć, usunąć ani dodać do niego zadań; do sprintu nie można dodać zadania `DONE` | zamknięty sprint jest danymi historycznymi |
| `SprintServiceImpl`, `IssueServiceImpl` | zapis zdarzeń przy starcie sprintu, dodaniu i usunięciu zadania, zmianie estymacji i ukończeniu zadania | bez historii nie da się odtworzyć przebiegu sprintu |
| `ExceptionMessages` | komunikaty `SPRINT_ALREADY_FINISHED`, `ISSUE_ALREADY_DONE` | nowe blokady |

## Nowe elementy

- **Encja `SprintIssueEvent`** (tabela `sprint_issue_event`): `sprintID`, `issueID`, `type`, `estimation`
  (estymacja w chwili zdarzenia), `occurredAt`. Identyfikatory nie są kluczami obcymi, więc historia zostaje po usunięciu zadania.
- **Enum `SprintIssueEventType`**: `SCOPE_AT_START`, `ADDED`, `REMOVED`, `ESTIMATION_CHANGED`, `COMPLETED`.
  Zdarzenia są zapisywane tylko dla trwającego sprintu; zmiany sprzed startu obejmuje `SCOPE_AT_START`.
- **Repozytorium `SprintIssueEventRepository`**: zdarzenia sprintu w kolejności chronologicznej.
- **Migracje**: projekt nie używa narzędzia do migracji. Schemat aktualizuje Hibernate (`ddl-auto: update`),
  więc nowa tabela i kolumny powstają przy starcie aplikacji.
- **Endpointy** (w trakcie realizacji):

| Metoda | Ścieżka | Wymaganie |
|---|---|---|
| GET | `/api/analytics/sprints/{id}/report` | WF-1, WF-2 |
| GET | `/api/analytics/sprints/{id}/burndown` | WF-3 |
| GET | `/api/analytics/sprints/{id}/burnup` | WF-4 |
| GET | `/api/analytics/projects/{id}/sprints?from=&to=` | WF-5, WF-6 |
| GET | `/api/analytics/projects/{id}/sprints/export.csv?from=&to=` | WF-7 |

Zestawienie obejmuje zakończone sprinty, których okres trwania nachodzi na przedział `from`–`to`.
`from` późniejsze niż `to` zwraca 400, nieistniejący sprint lub projekt 404, zmiana zakończonego sprintu 409.

## Sposób liczenia metryk

Jednostką są story points (`Issue.estimation`). Zadanie jest ukończone, gdy ma status `DONE`.

- **Velocity**: suma estymacji ze zdarzeń `COMPLETED`, czyli z chwili ukończenia zadania.
  Zadanie usunięte ze sprintu po ukończeniu nie jest wliczane. Zmiana estymacji po ukończeniu
  zadania nie zmienia velocity, bo raport używa zapisanej historii, a nie aktualnej estymacji.
  Ograniczenie: dla sprintu bez historii (`historyAvailable = false`) używane są aktualne estymacje.
- **Raport sprintu** (WF-1): stan sprintu jest odtwarzany przez przejście zdarzeń w kolejności chronologicznej,
  bo zakończenie sprintu odpina od niego zadania. Liczba początkowa to liczba zdarzeń `SCOPE_AT_START`.
  Ukończone i nieukończone to zadania, które po ostatnim zdarzeniu nadal należą do sprintu.
  Suma nieukończonych używa ostatniej znanej estymacji. Sprint bez historii (`startedAt` puste:
  nierozpoczęty albo rozpoczęty przed wdrożeniem modułu) jest liczony z bieżących zadań i ich statusu.
- **Burndown**: dla każdego dnia sprintu pozostała praca, czyli zakres początkowy (`SCOPE_AT_START`)
  minus praca ukończona do końca dnia. Wartości nie spadają poniżej zera.
- **Burnup**: dla każdego dnia narastająca suma ukończonej pracy oraz całkowity zakres,
  zmieniany przez zdarzenia `ADDED`, `REMOVED` i `ESTIMATION_CHANGED`.

Dni liczone są w strefie `Europe/Warsaw`.

## Przypadki szczególne

- **Zadania bez estymacji** są liczone w liczbie zadań, ale nie zwiększają sumy punktów.
- **Zmiany zakresu sprintu** po starcie są zapisywane jako `ADDED`, `REMOVED` i `ESTIMATION_CHANGED`.
  Widać je na wykresie burnup jako zmianę linii zakresu; burndown liczony jest od zakresu początkowego.
- **Brak historii**: zdarzenia zapisywane są od wdrożenia modułu. Sprint rozpoczęty wcześniej nie ma `startedAt`
  ani zdarzeń, więc wykresy zwracają dla niego pustą listę punktów zamiast niepełnych danych.

## Raport sprintu i velocity — stan realizacji (osoba B)

Zrobione: WF-1 (#12) i WF-2 (#11). Endpoint `GET /api/analytics/sprints/{sprintID}/report`
w `AnalyticsRestController` zwraca 200, 404 dla nieistniejącego sprintu i 400 dla nienumerycznego id.

```json
{ "sprintID": 7, "sprintName": "Sprint 3", "projectID": 1, "state": "FINISHED",
  "plannedStartDate": "2026-11-02T08:00:00Z", "startedAt": "2026-11-02T08:05:00Z", "endedAt": "2026-11-09T16:00:00Z",
  "historyAvailable": true, "initialIssueCount": 4, "completedIssueCount": 2, "uncompletedIssueCount": 2,
  "velocity": 5, "uncompletedEstimation": 3, "estimationUnit": "STORY_POINTS" }
```

`state` to `PLANNED`, `ACTIVE` albo `FINISHED`. Daty są w ISO-8601 (UTC).

| Klasa | Pakiet | Rola |
|---|---|---|
| `SprintMetricsCalculator` | `resource.service.analytics` | jedyne miejsce liczenia metryk sprintu, w tym velocity |
| `SprintMetrics` | `resource.service.analytics` | wynik: `historyAvailable`, `initialIssueCount`, `completedIssueCount`, `uncompletedIssueCount`, `velocity`, `uncompletedEstimation` |
| `SprintIssueStates` | `resource.service.analytics` | odtworzenie końcowego stanu zadań sprintu ze zdarzeń |
| `SprintReportService(Impl)` | `resource.service` | raport WF-1 |
| `SprintReportDTO`, `SprintState` | `resource.dto.analytics` | odpowiedź API |
| `ResourceNotFoundException` | `exception.definition` | wyjątek mapowany na HTTP 404 |

### Wskazówki dla osoby D (WF-5, WF-6, WF-7)

- **Nie licz velocity ani liczby zadań samodzielnie.** Dla każdego sprintu wywołaj
  `sprintMetricsCalculator.calculate(sprint, events)` i weź pola `completedIssueCount`,
  `uncompletedIssueCount` i `velocity`. Dzięki temu porównanie, CSV i raport sprintu zawsze dają te same liczby.
- `calculate` nie odpytuje bazy. `events` to zdarzenia **tylko tego sprintu**, w kolejności chronologicznej.
  Dla sprintu bez historii (`hasHistory(sprint) == false`) przekaż `List.of()`, a wynik zostanie policzony z bieżących zadań.
- **WNF-5:** nie pobieraj zdarzeń osobno dla każdego sprintu. Dodaj do `SprintIssueEventRepository` metodę
  `findAllBySprintIDInOrderByOccurredAtAscIdAsc(Collection<Integer> sprintIDs)`, pobierz zdarzenia jednym zapytaniem
  i pogrupuj je `groupingBy(SprintIssueEvent::getSprintID)`. Grupowanie zachowuje kolejność.
- Nieistniejący projekt: rzuć `new ResourceNotFoundException(PROJECT_DOES_NOT_EXISTS)`, a endpoint zwróci 404.
  Spójną strukturę JSON błędów (w tym 400 dla złego przedziału dat) dodaje osoba B w ramach WNF (#10).
- Testy wzorcowe: `SprintMetricsCalculatorTest` (jednostkowe, bez Springa), `SprintReportServiceTest`
  (pełny cykl start → zmiany statusów → stop) i `AnalyticsRestControllerIntegrationTest` (MockMvc).
  Nowe endpointy dodawaj do `AnalyticsRestController`.

## Uruchomienie

Wymagany Docker. W katalogu repozytorium:

```bash
docker compose -f compose.yaml up --build
```

Aplikacja działa pod `http://localhost:8081`, dokumentacja API pod `http://localhost:8081/docs.html`.

Testy (Java 21, Maven, baza H2 w pamięci, bez Dockera):

```bash
mvn test
```

## Pokrycie testami

```bash
mvn verify
```

Raport JaCoCo powstaje w `target/site/jacoco/index.html`.
Aktualny wynik: 100 testów, pokrycie instrukcji 67,20%. Wymagane minimum to 75%.

## Zrealizowane GitHub Issues

| Issue | Tytuł | Osoba |
|---|---|---|
| #5 | Zachowanie zakończonego sprintu | A |
| #6 | Blokady zakończonego sprintu | A |
| #7 | Historia zdarzeń sprintu | A |
| #8 | Zapisywanie zdarzeń sprintu | A |
