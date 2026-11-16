# Moduł analityczny sprintów

Moduł dostarcza raporty sprintów: podsumowanie, velocity, dane do wykresów burndown i burnup,
porównanie sprintów z filtrowaniem po datach oraz eksport CSV. Prace rozpoczęto 7 listopada 2026 r.

## Podział prac

| Osoba                                  | Zakres | Wymagania |
|----------------------------------------|---|---|
| A – Kamil Oleksiak (JasperBTA)         | cykl życia sprintu, historia zdarzeń sprintu (#5, #6, #7, #8) | WF-1, WF-2, WF-3, WF-4 (dane) |
| B - Jakub Sawicki (JakubSawicki03)     | obsługa błędów API, velocity, raport podsumowujący sprint (#10, #11, #12) | WF-1, WF-2, WNF |
| C - Nikodem Wełna (shoxxie01)          | dane do wykresów burndown i burnup (#13, #14) | WF-3, WF-4 |
| D - Mateusz Słupski (SlupskiMateusz02) | porównanie sprintów projektu, filtrowanie po dacie (#15, #16) | WF-5, WF-6 |
| E                                      | generator i eksport CSV, pomiar pokrycia JaCoCo, testy kontrolerów, instrukcja uruchomienia (#17, #18, #19, #20, #21) | WF-7, WNF |

Wspólnie: reguły obliczeń i kontrakt API (#4), dane demonstracyjne (#9), Pull Request i oddanie projektu (#22).

## Zmiany w istniejącym kodzie

| Element | Zmiana | Powód |
|---|---|---|
| `Sprint` | nowe pola `startedAt` i `endedAt` | `startDate` to data planowana; raporty potrzebują faktycznego okresu trwania sprintu |
| `SprintServiceImpl.stopSprint` | sprint nie jest usuwany, tylko oznaczany `isDone = true` | usunięcie sprintu uniemożliwiało wyliczenie jego metryk |
| `SprintServiceImpl` | zakończonego sprintu nie można ponownie rozpocząć, usunąć ani dodać do niego zadań; do sprintu nie można dodać zadania `DONE` | zamknięty sprint jest danymi historycznymi |
| `SprintServiceImpl`, `IssueServiceImpl` | zapis zdarzeń przy starcie sprintu, dodaniu i usunięciu zadania, zmianie estymacji i ukończeniu zadania | bez historii nie da się odtworzyć przebiegu sprintu |
| `ExceptionMessages` | komunikaty `SPRINT_ALREADY_FINISHED`, `ISSUE_ALREADY_DONE` | nowe blokady |
| `IssueServiceImpl` | tworzenie i edycja zadania odrzucają ujemną estymację (`NEGATIVE_ESTIMATION`, HTTP 400) | ujemne story points dawałyby ujemne velocity i wartości wykresów (WF-3) |
| `SprintRepository` | metoda `findAllByProjectIdAndIsDoneTrue` | zakończone sprinty projektu jednym zapytaniem (WF-5) |
| `ExceptionMessages` | komunikat `INVALID_DATE_RANGE` | czytelny błąd 400 dla `from` późniejszego niż `to` (WF-6) |
| serwisy `*ServiceImpl` | `new RuntimeException(...)` zastąpione typowanymi wyjątkami z `exception.definition`; komunikaty bez zmian | wcześniej każdy błąd biznesowy kończył się HTTP 500; typowane wyjątki dziedziczą po `RuntimeException`, więc istniejące testy działają bez zmian |

## Nowe elementy

- **Encja `SprintIssueEvent`** (tabela `sprint_issue_event`): `sprintID`, `issueID`, `type`, `estimation`
  (estymacja w chwili zdarzenia), `occurredAt`. Identyfikatory nie są kluczami obcymi, więc historia zostaje po usunięciu zadania.
- **Enum `SprintIssueEventType`**: `SCOPE_AT_START`, `ADDED`, `REMOVED`, `ESTIMATION_CHANGED`, `COMPLETED`.
  Zdarzenia są zapisywane tylko dla trwającego sprintu; zmiany sprzed startu obejmuje `SCOPE_AT_START`.
- **Repozytorium `SprintIssueEventRepository`**: zdarzenia sprintu w kolejności chronologicznej; dla zestawienia
  sprintów zdarzenia wielu sprintów naraz (`findAllBySprintIDInOrderByOccurredAtAscIdAsc`).
- **Migracje**: projekt nie używa narzędzia do migracji. Schemat aktualizuje Hibernate (`ddl-auto: update`),
  więc nowa tabela i kolumny powstają przy starcie aplikacji.
- **Endpointy** (`AnalyticsRestController`):

| Metoda | Ścieżka | Wymaganie | Status |
|---|---|---|---|
| GET | `/api/analytics/sprints/{id}/report` | WF-1, WF-2 | gotowe |
| GET | `/api/analytics/sprints/{id}/burndown` | WF-3 | gotowe |
| GET | `/api/analytics/sprints/{id}/burnup` | WF-4 | gotowe |
| GET | `/api/analytics/projects/{id}/sprints?from=&to=` | WF-5, WF-6 | gotowe |
| GET | `/api/analytics/projects/{id}/sprints/export.csv?from=&to=` | WF-7 | w realizacji |

Zestawienie obejmuje zakończone sprinty projektu, których okres trwania nachodzi na przedział `from`–`to`
(szczegóły w sekcji „Porównanie sprintów i filtrowanie”).
`from` późniejsze niż `to` zwraca 400, nieistniejący sprint lub projekt 404, zmiana zakończonego sprintu 409.

## Obsługa błędów API

Wszystkie endpointy zwracają błędy w jednej strukturze JSON (`ApiErrorDTO`). Nazwy pól są zgodne
z domyślną odpowiedzią Spring Boota, więc błędy spoza kontrolerów (np. 401 z filtra bezpieczeństwa) wyglądają tak samo.
Stos wywołań nigdy nie trafia do klienta.

```json
{ "timestamp": "2026-11-08T08:59:17.163Z", "status": 404, "error": "Not Found",
  "message": "Sprint with given ID does not exists", "path": "/api/analytics/sprints/-1/report" }
```

| Wyjątek (`exception.definition`) / przyczyna | HTTP | Przykład |
|---|---|---|
| `ResourceNotFoundException` | 404 | nieistniejący sprint, projekt, zadanie, komentarz, status |
| `BusinessRuleException` | 409 | zmiana zakończonego sprintu, drugi aktywny sprint, niedozwolona zmiana statusu, nieunikalna nazwa |
| `ForbiddenOperationException`, brak roli w `@PreAuthorize` | 403 | edycja cudzego komentarza |
| `InvalidRequestException`, `IllegalArgumentException` (w tym `NullIdentifierException`) | 400 | `from` późniejsze niż `to`, brak identyfikatora |
| nieprawidłowy typ parametru, niepoprawny JSON, brak wymaganego parametru | 400 | `/sprints/abc/report` |
| nieobsługiwana metoda HTTP | 405 | `POST` na endpoint `GET` |
| każdy inny wyjątek | 500 | komunikat `Unexpected server error`, szczegóły tylko w logu serwera |

Handler: `resource.controller.ApiExceptionHandler`. Nowy kod powinien rzucać jeden z powyższych wyjątków,
a nie `RuntimeException`, która zwróciłaby 500.

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
- „Praca ukończona do końca dnia” liczona jest tymi samymi regułami co velocity (`SprintIssueStates`),
  więc ostatni punkt burnup ma `completedWork` równe velocity z raportu sprintu,
  a `totalScope` równe `velocity + uncompletedEstimation`.

Dni liczone są w strefie `Europe/Warsaw`.

## Przypadki szczególne

- **Zadania bez estymacji** są liczone w liczbie zadań, ale nie zwiększają sumy punktów.
- **Ujemna estymacja** jest odrzucana przy tworzeniu i edycji zadania (400, `Issue estimation cannot be negative`),
  dzięki czemu velocity i wartości na wykresach są zawsze nieujemne.
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
- Nieistniejący projekt: rzuć `new ResourceNotFoundException(PROJECT_DOES_NOT_EXISTS)` (404).
  Zły przedział dat: rzuć `new InvalidRequestException(...)` (400). Treść błędu zbuduje `ApiExceptionHandler`
  (patrz sekcja „Obsługa błędów API”).
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
Aktualny wynik (16 listopada 2026 r., po WF-1–WF-6 i obsłudze błędów): 221 testów, wszystkie przechodzą,
pokrycie instrukcji 75,98% (3347 z 4405). Wymagane minimum to 75%.
Raport obejmuje cały kod produkcyjny, bez wykluczeń. Nowy kod raportu sprintu, velocity i obsługi błędów ma pokrycie 98,9%,
kod wykresów burndown i burnup — 100%, kod porównania i filtrowania sprintów — 100% instrukcji i gałęzi.

## Wykresy burndown i burnup — stan realizacji (osoba C)

Zrobione: WF-3 (#13) i WF-4 (#14). Endpointy `GET /api/analytics/sprints/{sprintID}/burndown`
i `GET /api/analytics/sprints/{sprintID}/burnup` zwracają 200, 404 dla nieistniejącego sprintu
i 400 dla nienumerycznego id.

### Punkty wykresu

- `day = 0` to chwila startu sprintu (`startedAt`): zakres początkowy, nic nie jest jeszcze ukończone.
- `day = n` (n ≥ 1) to stan na **koniec n-tego dnia kalendarzowego** sprintu w strefie `Europe/Warsaw`
  (pole `timeZone`). Uwzględnia wszystkie zdarzenia, które wystąpiły tego dnia lub wcześniej.
- Ostatni dzień to dzień zakończenia sprintu (`endedAt`), a dla trwającego sprintu dzień dzisiejszy.
- `date` to data w formacie ISO-8601 (`YYYY-MM-DD`). Punkty są zawsze posortowane rosnąco po `day`, dni są kolejne, bez przerw.
- Wszystkie wartości są w story points (`estimationUnit = STORY_POINTS`) i są nieujemne.

```json
{ "sprintID": 7, "sprintName": "Sprint 3", "projectID": 1, "state": "FINISHED",
  "startedAt": "2026-11-02T08:05:00Z", "endedAt": "2026-11-04T16:00:00Z", "historyAvailable": true,
  "initialScope": 16, "estimationUnit": "STORY_POINTS", "timeZone": "Europe/Warsaw",
  "points": [ { "day": 0, "date": "2026-11-02", "remainingWork": 16 },
              { "day": 1, "date": "2026-11-02", "remainingWork": 11 },
              { "day": 2, "date": "2026-11-03", "remainingWork": 11 },
              { "day": 3, "date": "2026-11-04", "remainingWork": 3 } ] }
```

Burnup ma ten sam nagłówek (bez `initialScope`), a punkty postaci
`{ "day": 3, "date": "2026-11-04", "completedWork": 13, "totalScope": 21 }`.

| Pole punktu | Wykres | Znaczenie |
|---|---|---|
| `remainingWork` | burndown | `initialScope` − praca ukończona do tego punktu, minimum 0 |
| `completedWork` | burnup | narastająca suma estymacji zadań ukończonych w tym sprincie (estymacja z chwili ukończenia) |
| `totalScope` | burnup | suma estymacji wszystkich zadań w sprincie w tym punkcie: ukończone z estymacją z chwili ukończenia, pozostałe z ostatnią znaną |

### Reguły i przypadki szczególne

- **Zadanie dodane w trakcie sprintu** zwiększa `totalScope` od dnia dodania. Burndown startuje od zakresu początkowego,
  więc ukończenie dodanego zadania także obniża `remainingWork` — dlatego wartość jest ograniczona do 0.
- **Zadanie usunięte ze sprintu (lub skasowane)** zmniejsza `totalScope` od dnia usunięcia. Jeśli było ukończone,
  przestaje być liczone w `completedWork` (zgodnie z velocity). Burndown nadal liczy się od zakresu początkowego.
- **Zmiana estymacji** zmienia `totalScope` od dnia zmiany; po ukończeniu zadania nie zmienia już ani `completedWork`, ani `totalScope`.
- **Zadania bez estymacji** nie zmieniają żadnej wartości. Sprint, w którym żadne zadanie nie ma estymacji,
  zwraca pełną listę punktów z zerami.
- **Sprint bez historii** (nierozpoczęty albo rozpoczęty przed wdrożeniem modułu) zwraca `historyAvailable = false`
  i pustą listę `points` — nie da się wiarygodnie odtworzyć jego przebiegu, więc nie zwracamy przybliżonych danych.
- Wykresy dotyczą tylko wskazanego sprintu: pobierane są wyłącznie jego zdarzenia (jedno zapytanie).

| Klasa | Pakiet | Rola |
|---|---|---|
| `SprintProgressCalculator` | `resource.service.analytics` | odtworzenie stanu sprintu dzień po dniu ze zdarzeń |
| `SprintProgress` | `resource.service.analytics` | wynik: `initialScope` i lista `DailyProgress` (`completedWork`, `totalScope`) |
| `SprintChartService(Impl)` | `resource.service` | burndown (WF-3) i burnup (WF-4) |
| `SprintBurndownDTO`, `SprintBurnupDTO` | `resource.dto.analytics` | odpowiedzi API |

Wydajność: stan każdego dnia jest odtwarzany od początku listy zdarzeń, więc koszt to O(dni × zdarzenia).
Dla typowego sprintu (kilkanaście dni, kilkaset zdarzeń) jest to pomijalne; obliczenia nie wykonują dodatkowych zapytań.

Testy: `SprintProgressCalculatorTest` (jednostkowe, m.in. zmiany zakresu, strefa czasowa, brak estymacji),
`SprintChartServiceTest` (pełny cykl sprintu, zgodność z raportem sprintu) i nowe przypadki w `AnalyticsRestControllerIntegrationTest`.

## Porównanie sprintów i filtrowanie — stan realizacji (osoba D)

Zrobione: WF-5 (#15) i WF-6 (#16). Endpoint `GET /api/analytics/projects/{projectID}/sprints?from=&to=`
w `AnalyticsRestController` zwraca 200, 404 dla nieistniejącego projektu oraz 400 dla nienumerycznego id,
niepoprawnej daty i `from` późniejszego niż `to`.

```json
{ "projectID": 1, "from": "2026-10-14", "to": "2026-10-28", "timeZone": "Europe/Warsaw", "estimationUnit": "STORY_POINTS",
  "sprints": [ { "sprintID": 4, "sprintName": "Sprint 2", "startedAt": "2026-10-01T07:00:00Z", "endedAt": "2026-10-14T15:00:00Z",
                 "historyAvailable": true, "completedIssueCount": 5, "uncompletedIssueCount": 1, "velocity": 18 },
               { "sprintID": 5, "sprintName": "Sprint 3", "startedAt": "2026-10-28T08:00:00Z", "endedAt": "2026-11-10T16:00:00Z",
                 "historyAvailable": true, "completedIssueCount": 0, "uncompletedIssueCount": 3, "velocity": 0 } ] }
```

Daty sprintu są w ISO-8601 (UTC), granice filtra w formacie `YYYY-MM-DD`.

### Zestawienie (WF-5)

- Obejmuje **zakończone** sprinty (`isDone = true`) wskazanego projektu. Sprint trwający lub planowany nie ma jeszcze
  końcowych wyników, więc nie jest porównywany.
- Sprinty bez ukończonych zadań również są w zestawieniu (`completedIssueCount = 0`, `velocity = 0`).
- `completedIssueCount`, `uncompletedIssueCount` i `velocity` liczy `SprintMetricsCalculator`, więc są równe wartościom
  z raportu sprintu (WF-1), łącznie z regułami dla zadań bez estymacji, zmian zakresu i zmian estymacji po ukończeniu.
- **Kolejność: od najstarszego do najnowszego**, rosnąco po `startedAt`. Sprinty bez `startedAt` (zakończone bez zapisanej
  historii) są na początku, bo rozpoczęły się przed wdrożeniem modułu, czyli przed każdym sprintem z zapisanym startem.
  Przy równym `startedAt` decyduje rosnące `sprintID`. Sortowanie odbywa się w Javie, bo PostgreSQL i H2 różnie
  ustawiają `NULL` w `ORDER BY`.
- Sprint bez historii ma `historyAvailable = false`, a `startedAt` i `endedAt` mogą być `null`; metryki liczone są
  z bieżących zadań, tak jak w raporcie sprintu.
- Projekt bez zakończonych sprintów: 200 i pusta lista `sprints`.

### Filtrowanie (WF-6)

- Filtr projektu wynika ze ścieżki: zestawienie nigdy nie zawiera sprintów innych projektów.
- `from` i `to` są opcjonalne. Obie granice są **włączne** i oznaczają dni kalendarzowe w strefie `Europe/Warsaw`
  (pole `timeZone`). Odpowiedź zwraca zastosowany filtr w polach `from` i `to` (`null`, gdy granicy nie podano).
- **Reguła: sprint nachodzi na przedział**, czyli dzień `endedAt` ≥ `from` i dzień `startedAt` ≤ `to`.
  Przykład dla przedziału 14–28 października: sprinty 1–14 X i 28 X–10 XI są w wyniku, sprinty 1–14 IX i 16–29 XI nie.
- Tylko `from`: sprinty zakończone tego dnia lub później. Tylko `to`: sprinty rozpoczęte tego dnia lub wcześniej.
  Pusty parametr (`?from=`) oznacza brak granicy.
- Sprint bez daty potrzebnej do sprawdzenia granicy (sprint bez historii) nie spełnia filtra, bo nie da się stwierdzić,
  czy nachodzi na przedział. Bez filtra dat jest w zestawieniu.

| Błąd | HTTP | `message` |
|---|---|---|
| `from` późniejsze niż `to` | 400 | `Parameter 'from' must not be later than 'to'` |
| niepoprawna data, np. `from=2026-13-01` | 400 | `Invalid value '2026-13-01' for parameter 'from'` |
| nienumeryczne `projectID` | 400 | `Invalid value 'abc' for parameter 'projectID'` |
| nieistniejący projekt | 404 | `Project with given ID does not exists` |

Parametry są walidowane przed sprawdzeniem projektu, więc zły przedział daje 400 również dla nieistniejącego projektu.

| Klasa | Pakiet | Rola |
|---|---|---|
| `SprintDateRange` | `resource.service.analytics` | walidacja przedziału dat i reguła nachodzenia sprintu na przedział (WF-6) |
| `SprintComparisonService(Impl)` | `resource.service` | zestawienie zakończonych sprintów projektu (WF-5) z filtrem dat (WF-6) |
| `SprintComparisonDTO` | `resource.dto.analytics` | odpowiedź API; wiersz sprintu to rekord `SprintResult` |

Wydajność (WNF-5): zestawienie wykonuje trzy zapytania niezależnie od liczby sprintów — istnienie projektu,
zakończone sprinty projektu i zdarzenia wszystkich pasujących sprintów naraz, grupowane po `sprintID`.
Filtr dat działa w pamięci na sprintach projektu (jest ich niewiele), a zdarzenia, czyli większość danych,
pobierane są tylko dla sprintów, które przeszły filtr. Gdy żaden sprint nie ma historii, zapytanie o zdarzenia jest pomijane.
Ograniczenie: dla sprintu bez historii kalkulator czyta jego bieżące zadania, co oznacza jedno dodatkowe zapytanie
na taki sprint; dotyczy to wyłącznie sprintów sprzed wdrożenia modułu.

Eksport CSV (WF-7) powinien korzystać z `SprintComparisonService.compareSprints(projectID, from, to)`, dzięki czemu
respektuje te same filtry, kolejność i błędy co zestawienie JSON.

Testy: `SprintDateRangeTest` (jednostkowe: granice przedziału, jedna granica, strefa czasowa, sprinty bez dat),
`SprintComparisonServiceImplTest` (jednostkowe z Mockito: jedno zapytanie o zdarzenia, brak mieszania zdarzeń między sprintami,
brak zapytań przy złym przedziale), `SprintComparisonServiceTest` (pełny cykl sprintu, zgodność z raportem sprintu, kolejność,
izolacja projektów, filtry) i nowe przypadki w `AnalyticsRestControllerIntegrationTest`.

## Zrealizowane GitHub Issues

| Issue | Tytuł | Osoba |
|---|---|---|
| #5 | Zachowanie zakończonego sprintu | A |
| #6 | Blokady zakończonego sprintu | A |
| #7 | Historia zdarzeń sprintu | A |
| #8 | Zapisywanie zdarzeń sprintu | A |
| #10 | Obsługa błędów API | B |
| #11 | Obliczanie velocity | B |
| #12 | Raport podsumowujący sprint | B |
| #13 | Dane do wykresu burndown | C |
| #14 | Dane do wykresu burnup | C |
| #15 | Porównanie sprintów projektu | D |
| #16 | Filtrowanie zestawienia sprintów po dacie | D |
