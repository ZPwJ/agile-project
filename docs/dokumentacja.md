# Moduł analityczny sprintów

Moduł rozszerza istniejącą aplikację Scrum o raporty sprintów: podsumowanie sprintu z velocity,
dane do wykresów burndown i burnup, porównanie sprintów projektu z filtrowaniem po datach oraz eksport porównania do CSV.

| Metoda | Ścieżka | Wymaganie | Błędy |
|---|---|---|---|
| GET | `/api/analytics/sprints/{sprintID}/report` | WF-1, WF-2 | 404 nieistniejący sprint, 400 nienumeryczne id |
| GET | `/api/analytics/sprints/{sprintID}/burndown` | WF-3 | 404 nieistniejący sprint, 400 nienumeryczne id |
| GET | `/api/analytics/sprints/{sprintID}/burnup` | WF-4 | 404 nieistniejący sprint, 400 nienumeryczne id |
| GET | `/api/analytics/projects/{projectID}/sprints?from=&to=` | WF-5, WF-6 | 404 nieistniejący projekt, 400 niepoprawne id, data lub przedział |
| GET | `/api/analytics/projects/{projectID}/sprints/export.csv?from=&to=` | WF-7 | jak wyżej |

## Uruchomienie

Wymagany Docker. W katalogu repozytorium:

```bash
cp .env.example .env
docker compose -f compose.yaml up --build
```

Aplikacja działa pod `http://localhost:8081`, dokumentacja API (Swagger UI) pod `http://localhost:8081/swagger-ui/index.html`.
Endpointy wymagają tokena JWT z Keycloaka — sposób jego pobrania i konta testowe opisuje README, sekcja „Authentication”.
Aby uruchomić aplikację z przykładowymi danymi, zobacz sekcję „Dane demonstracyjne”.

Testy (Java 21, baza H2 w pamięci, bez Dockera) i raport pokrycia:

```bash
./mvnw test            # same testy
./mvnw clean verify   # testy + raport JaCoCo + kontrola progu 75%
```

## Konwencje API

- **Jednostka**: story points (`Issue.estimation`), w odpowiedziach pole `estimationUnit = STORY_POINTS`.
- **Daty i czas**: znaczniki czasu w ISO-8601 w UTC (`2026-10-01T07:00:00Z`), daty dzienne i parametry `from`/`to`
  w formacie `YYYY-MM-DD`. Dni sprintu i granice filtra liczone są w strefie `Europe/Warsaw` (pole `timeZone`).
- **Błędy**: zawsze JSON w jednej strukturze (`ApiErrorDTO`), także dla endpointu CSV. Stos wywołań nie trafia do klienta.

```json
{ "timestamp": "2026-11-08T08:59:17.163Z", "status": 404, "error": "Not Found",
  "message": "Sprint with given ID does not exists", "path": "/api/analytics/sprints/-1/report" }
```

| Przyczyna (wyjątek z `exception.definition`) | HTTP | Przykład |
|---|---|---|
| `ResourceNotFoundException` | 404 | nieistniejący sprint, projekt, zadanie, komentarz, status |
| `BusinessRuleException` | 409 | zmiana zakończonego sprintu, drugi aktywny sprint, niedozwolona zmiana statusu, nieunikalna nazwa |
| `ForbiddenOperationException`, brak roli w `@PreAuthorize` | 403 | edycja cudzego komentarza |
| `InvalidRequestException`, `IllegalArgumentException` (w tym `NullIdentifierException`) | 400 | `from` późniejsze niż `to`, brak identyfikatora |
| nieprawidłowy typ parametru, niepoprawny JSON, brak wymaganego parametru | 400 | `/sprints/abc/report` |
| nieobsługiwana metoda HTTP | 405 | `POST` na endpoint `GET` |
| każdy inny wyjątek | 500 | komunikat `Unexpected server error`, szczegóły tylko w logu serwera |

Obsługę błędów realizuje `resource.controller.ApiExceptionHandler`.

## Reguły obliczeń

### Historia sprintu

Zakończenie sprintu odpina od niego zadania, więc bieżące dane zadań nie opisują zakończonego sprintu.
Dlatego moduł zapisuje historię w tabeli `sprint_issue_event` i odtwarza z niej stan sprintu, przechodząc zdarzenia chronologicznie:

| Zdarzenie | Kiedy |
|---|---|
| `SCOPE_AT_START` | start sprintu — po jednym dla każdego zadania w sprincie |
| `ADDED`, `REMOVED` | dodanie / usunięcie zadania w trakcie sprintu (także skasowanie zadania) |
| `ESTIMATION_CHANGED` | zmiana estymacji zadania w trakcie sprintu |
| `COMPLETED` | zmiana statusu zadania na `DONE` |

Każde zdarzenie przechowuje estymację z chwili zdarzenia. Zdarzenia zapisywane są tylko dla trwającego sprintu.

### Metryki

- **Status końcowy**: zadanie jest ukończone, gdy ma status `DONE`. To jedyny status końcowy:
  `TO_DO`, `IN_PROGRESS`, `CODE_REVIEW` i `IN_TESTS` oznaczają zadanie nieukończone.
- **Velocity** (WF-2): suma estymacji zadań ukończonych w sprincie, z estymacją z chwili ukończenia (zdarzenie `COMPLETED`).
  Zmiana estymacji po ukończeniu nie zmienia velocity, bo raport używa zapisanej historii, a nie aktualnej estymacji.
- **Raport sprintu** (WF-1): liczba początkowa to liczba zdarzeń `SCOPE_AT_START`. Ukończone i nieukończone to zadania,
  które po ostatnim zdarzeniu nadal należą do sprintu. Suma nieukończonych używa ostatniej znanej estymacji.
- **Burndown** (WF-3): dla każdego dnia dwie wartości:
  `remainingWork` = zakres początkowy (suma estymacji `SCOPE_AT_START`) minus praca ukończona do końca dnia, nie mniej niż 0
  (klasyczny burndown wg reguły 2 z WF-3), oraz `remainingScope` = aktualny zakres sprintu minus praca ukończona,
  czyli praca faktycznie pozostała razem ze zmianami zakresu po starcie.
- **Burnup** (WF-4): dla każdego dnia narastająca suma ukończonej pracy oraz całkowity zakres sprintu w tym dniu.
- Raport, wykresy, porównanie i CSV liczą ukończoną pracę tym samym kodem (`SprintIssueStates`, `SprintMetricsCalculator`),
  więc dają spójne liczby: ostatni punkt burnup ma `completedWork` równe velocity, a `totalScope` równe
  `velocity + uncompletedEstimation`.

### Przypadki szczególne

- **Zadanie bez estymacji** jest liczone w liczbie zadań, ale nie zwiększa żadnej sumy punktów ani wartości na wykresach.
  Sprint, w którym żadne zadanie nie ma estymacji, ma velocity 0 i pełną listę punktów wykresu z zerami.
- **Ujemna estymacja** jest odrzucana przy tworzeniu i edycji zadania (400, `Issue estimation cannot be negative`),
  dzięki czemu wszystkie wartości są nieujemne.
- **Zadanie dodane w trakcie sprintu** zwiększa `totalScope` na burnup od dnia dodania. Burndown liczony jest od zakresu
  początkowego, więc ukończenie dodanego zadania także obniża `remainingWork` — dlatego wartość jest ograniczona do 0
  i może wynosić 0, choć w sprincie zostały nieukończone zadania. Faktycznie pozostałą pracę pokazuje `remainingScope`.
- **Zadanie usunięte ze sprintu** zmniejsza `totalScope` od dnia usunięcia i nie jest wliczane do metryk sprintu,
  nawet jeśli wcześniej zostało ukończone.
- **Zmiana estymacji** zmienia `totalScope` od dnia zmiany; po ukończeniu zadania nie zmienia już żadnej wartości.
- **Brak historii**: zdarzenia zapisywane są od wdrożenia modułu. Sprint nierozpoczęty albo rozpoczęty wcześniej ma
  `historyAvailable = false` i puste `startedAt`. Raport i porównanie liczą go z bieżących zadań i ich statusu
  (z aktualnymi estymacjami), a wykresy zwracają pustą listę punktów — przebiegu nie da się wiarygodnie odtworzyć,
  więc moduł nie zwraca przybliżonych danych.

## Endpointy

### Raport sprintu (WF-1, WF-2)

`GET /api/analytics/sprints/{sprintID}/report`

```json
{ "sprintID": 7, "sprintName": "Sprint 3", "projectID": 1, "state": "FINISHED",
  "plannedStartDate": "2026-11-02T08:00:00Z", "startedAt": "2026-11-02T08:05:00Z", "endedAt": "2026-11-09T16:00:00Z",
  "historyAvailable": true, "initialIssueCount": 4, "completedIssueCount": 2, "uncompletedIssueCount": 2,
  "velocity": 5, "uncompletedEstimation": 3, "estimationUnit": "STORY_POINTS" }
```

`state` to `PLANNED`, `ACTIVE` albo `FINISHED`. `plannedStartDate` to data planowana, `startedAt` i `endedAt` — faktyczne.

### Wykresy burndown i burnup (WF-3, WF-4)

`GET /api/analytics/sprints/{sprintID}/burndown` i `GET /api/analytics/sprints/{sprintID}/burnup`

```json
{ "sprintID": 7, "sprintName": "Sprint 3", "projectID": 1, "state": "FINISHED",
  "startedAt": "2026-11-02T08:05:00Z", "endedAt": "2026-11-04T16:00:00Z", "historyAvailable": true,
  "initialScope": 16, "estimationUnit": "STORY_POINTS", "timeZone": "Europe/Warsaw",
  "points": [ { "day": 0, "date": "2026-11-02", "remainingWork": 16, "remainingScope": 16 },
              { "day": 1, "date": "2026-11-02", "remainingWork": 11, "remainingScope": 11 },
              { "day": 2, "date": "2026-11-03", "remainingWork": 11, "remainingScope": 16 },
              { "day": 3, "date": "2026-11-04", "remainingWork": 3, "remainingScope": 8 } ] }
```

Burnup ma ten sam nagłówek (bez `initialScope`), a punkty postaci
`{ "day": 3, "date": "2026-11-04", "completedWork": 13, "totalScope": 21 }`.

- `day = 0` to chwila startu sprintu: zakres początkowy, nic nie jest jeszcze ukończone.
- `day = n` (n ≥ 1) to stan na koniec n-tego dnia kalendarzowego sprintu, ze wszystkimi zdarzeniami do tego dnia włącznie.
- Ostatni dzień to dzień zakończenia sprintu, a dla trwającego sprintu dzień dzisiejszy.
- Punkty są posortowane rosnąco po `day`, dni są kolejne, bez przerw.

| Pole punktu | Wykres | Znaczenie |
|---|---|---|
| `remainingWork` | burndown | `initialScope` − praca ukończona do tego punktu, minimum 0 |
| `remainingScope` | burndown | `totalScope` z burnup − praca ukończona do tego punktu: praca faktycznie pozostała, łącznie z dodaną po starcie |
| `completedWork` | burnup | narastająca suma estymacji zadań ukończonych w tym sprincie (estymacja z chwili ukończenia) |
| `totalScope` | burnup | suma estymacji zadań w sprincie w tym punkcie: ukończone z estymacją z chwili ukończenia, pozostałe z ostatnią znaną |

### Porównanie sprintów i filtrowanie (WF-5, WF-6)

`GET /api/analytics/projects/{projectID}/sprints?from=&to=`

```json
{ "projectID": 1, "from": "2026-10-14", "to": "2026-10-28", "timeZone": "Europe/Warsaw", "estimationUnit": "STORY_POINTS",
  "sprints": [ { "sprintID": 4, "sprintName": "Sprint 2", "startedAt": "2026-10-01T07:00:00Z", "endedAt": "2026-10-14T15:00:00Z",
                 "historyAvailable": true, "completedIssueCount": 5, "uncompletedIssueCount": 1, "velocity": 18 },
               { "sprintID": 5, "sprintName": "Sprint 3", "startedAt": "2026-10-28T08:00:00Z", "endedAt": "2026-11-10T16:00:00Z",
                 "historyAvailable": true, "completedIssueCount": 0, "uncompletedIssueCount": 3, "velocity": 0 } ] }
```

**Zestawienie:**

- Obejmuje **zakończone** sprinty wskazanego projektu; sprint trwający lub planowany nie ma jeszcze końcowych wyników.
  Sprinty innych projektów nigdy nie trafiają do wyniku.
- Sprinty bez ukończonych zadań również są w zestawieniu (`completedIssueCount = 0`, `velocity = 0`).
  Projekt bez zakończonych sprintów zwraca 200 i pustą listę `sprints`.
- **Kolejność: od najstarszego do najnowszego**, rosnąco po `startedAt`, przy równym `startedAt` rosnąco po `sprintID`.
  Sprinty bez historii (puste `startedAt`) są na początku, bo rozpoczęły się przed wdrożeniem modułu.

**Filtr dat:**

- `from` i `to` są opcjonalne, obie granice są **włączne**. Odpowiedź zwraca zastosowany filtr (`null`, gdy granicy nie podano);
  pusty parametr (`?from=`) oznacza brak granicy.
- **Reguła: sprint nachodzi na przedział**, czyli dzień `endedAt` ≥ `from` i dzień `startedAt` ≤ `to`.
  Przykład dla przedziału 14–28 października: sprinty 1–14 X i 28 X–10 XI są w wyniku, sprinty 1–14 IX i 16–29 XI nie.
- Tylko `from`: sprinty zakończone tego dnia lub później. Tylko `to`: sprinty rozpoczęte tego dnia lub wcześniej.
- Sprint bez historii nie spełnia filtra dat, bo nie da się stwierdzić, czy nachodzi na przedział. Bez filtra jest w zestawieniu.
- Parametry są walidowane przed sprawdzeniem projektu, więc zły przedział daje 400 również dla nieistniejącego projektu.

| Błąd | HTTP | `message` |
|---|---|---|
| `from` późniejsze niż `to` | 400 | `Parameter 'from' must not be later than 'to'` |
| niepoprawna data, np. `from=2026-13-01` | 400 | `Invalid value '2026-13-01' for parameter 'from'` |
| nienumeryczne `projectID` | 400 | `Invalid value 'abc' for parameter 'projectID'` |
| nieistniejący projekt | 404 | `Project with given ID does not exists` |

### Eksport CSV (WF-7)

`GET /api/analytics/projects/{projectID}/sprints/export.csv?from=&to=`

```text
projectID,sprintID,sprintName,startedAt,endedAt,historyAvailable,completedIssueCount,uncompletedIssueCount,velocity
1,4,Sprint 2,2026-10-01T07:00:00Z,2026-10-14T15:00:00Z,true,5,1,18
```

- Zawiera te same sprinty, w tej samej kolejności i z tymi samymi liczbami co porównanie sprintów, z tymi samymi filtrami i błędami.
- Odpowiedź ma typ `text/csv;charset=UTF-8` i nazwę pliku `project-{id}-sprints.csv`.
- Separator `,`, końce linii `CRLF`. Nagłówek jest zawsze; puste zestawienie to plik z samym nagłówkiem.
- Sprint bez historii ma puste `startedAt` i `endedAt`.
- Wartość z przecinkiem, cudzysłowem lub znakiem nowej linii jest ujmowana w cudzysłów, a cudzysłowy są podwajane
  (`Sprint "Q4", part 1` → `"Sprint ""Q4"", part 1"`).

## Zmiany w istniejącym kodzie

| Element | Zmiana | Powód |
|---|---|---|
| `Sprint` | nowe pola `startedAt` i `endedAt` | `startDate` to data planowana; raporty potrzebują faktycznego okresu trwania sprintu |
| `SprintServiceImpl.stopSprint` | sprint nie jest usuwany, tylko oznaczany `isDone = true` | usunięcie sprintu uniemożliwiało wyliczenie jego metryk |
| `SprintServiceImpl` | zakończonego sprintu nie można ponownie rozpocząć, usunąć ani dodać do niego zadań; do sprintu nie można dodać zadania `DONE` | zamknięty sprint jest danymi historycznymi |
| `SprintServiceImpl`, `IssueServiceImpl` | zapis zdarzeń przy starcie sprintu, dodaniu i usunięciu zadania, zmianie estymacji i ukończeniu zadania | bez historii nie da się odtworzyć przebiegu sprintu |
| `IssueServiceImpl` | tworzenie i edycja zadania odrzucają ujemną estymację (`NEGATIVE_ESTIMATION`, HTTP 400) | ujemne story points dawałyby ujemne velocity i wartości wykresów |
| `SprintRepository` | metoda `findAllByProjectIdAndIsDoneTrue` | zakończone sprinty projektu jednym zapytaniem (WF-5) |
| `ExceptionMessages` | komunikaty `SPRINT_ALREADY_FINISHED`, `ISSUE_ALREADY_DONE`, `INVALID_DATE_RANGE` | nowe blokady i czytelny błąd 400 dla złego przedziału dat |
| serwisy `*ServiceImpl` | `new RuntimeException(...)` zastąpione typowanymi wyjątkami z `exception.definition`; komunikaty bez zmian | wcześniej każdy błąd biznesowy kończył się HTTP 500 |
| `ApiExceptionHandler` | odpowiedź błędu ma zawsze typ `application/json` | przy `Accept: text/csv` eksport zwracał 500 zamiast 404/400 |
| `CommitmentRuleServiceImpl` | `@Transactional`, tak jak w innych serwisach | usuwanie wszystkich reguł DoD kończyło się 500 (`LazyInitializationException`) |
| `application.yml` | hasło bazy brane z `SPRING_DATASOURCE_PASSWORD`, `data-prod.sql` wczytywany przy starcie | hasło nie powinno być w repo, a bez słowników nie dało się dodać zadania |
| `compose.yaml` | profil Springa można nadpisać zmienną `SPRING_PROFILES_ACTIVE` (domyślnie `prod`) | uruchomienie z danymi demonstracyjnymi |

## Nowe elementy

- **Encja `SprintIssueEvent`** (tabela `sprint_issue_event`): `sprintID`, `issueID`, `type`, `estimation`, `occurredAt`.
  Identyfikatory nie są kluczami obcymi, więc historia zostaje po usunięciu zadania.
- **Enum `SprintIssueEventType`**: typy zdarzeń opisane w sekcji „Historia sprintu”.
- **Repozytorium `SprintIssueEventRepository`**: zdarzenia jednego sprintu albo wielu sprintów naraz, w kolejności chronologicznej.
- **Migracje**: projekt nie używa narzędzia do migracji. Schemat aktualizuje Hibernate (`ddl-auto: update`),
  więc nowa tabela i kolumny powstają przy starcie aplikacji.

| Klasa | Pakiet | Rola |
|---|---|---|
| `AnalyticsRestController` | `resource.controller` | wszystkie endpointy modułu |
| `SprintReportService(Impl)` | `resource.service` | raport sprintu (WF-1, WF-2) |
| `SprintChartService(Impl)` | `resource.service` | burndown i burnup (WF-3, WF-4) |
| `SprintComparisonService(Impl)` | `resource.service` | porównanie zakończonych sprintów z filtrem dat (WF-5, WF-6) |
| `SprintIssueStates` | `resource.service.analytics` | odtworzenie stanu zadań sprintu ze zdarzeń |
| `SprintMetricsCalculator`, `SprintMetrics` | `resource.service.analytics` | jedyne miejsce liczenia metryk sprintu, w tym velocity |
| `SprintProgressCalculator`, `SprintProgress` | `resource.service.analytics` | stan sprintu dzień po dniu dla wykresów |
| `SprintDateRange` | `resource.service.analytics` | walidacja przedziału dat i reguła nachodzenia sprintu na przedział |
| `SprintComparisonCsvWriter` | `resource.service.analytics` | zamiana porównania sprintów na CSV (WF-7) |
| `SprintReportDTO`, `SprintBurndownDTO`, `SprintBurnupDTO`, `SprintComparisonDTO`, `SprintState` | `resource.dto.analytics` | odpowiedzi API |
| `ResourceNotFoundException`, `BusinessRuleException`, `InvalidRequestException`, `ForbiddenOperationException` | `exception.definition` | typowane wyjątki mapowane na kody HTTP |

## Wydajność (WNF-5)

- **Raport i wykresy** pobierają zdarzenia wskazanego sprintu jednym zapytaniem. Wykres odtwarza stan każdego dnia
  od początku listy zdarzeń, więc koszt to O(dni × zdarzenia) — dla typowego sprintu (kilkanaście dni, kilkaset zdarzeń) pomijalny.
- **Porównanie i CSV** wykonują trzy zapytania niezależnie od liczby sprintów: istnienie projektu, zakończone sprinty projektu
  i zdarzenia wszystkich sprintów, które przeszły filtr dat, pobrane naraz i pogrupowane po sprincie.
- Ograniczenie: sprint bez historii jest liczony z bieżących zadań, co oznacza jedno dodatkowe zapytanie na taki sprint.
  Dotyczy to wyłącznie sprintów sprzed wdrożenia modułu.

## Dane demonstracyjne

Profil `demo` wczytuje przy starcie, oprócz słowników (`data-prod.sql`), plik `data-demo.sql`.
Bez profilu baza zawiera tylko słowniki.

```bash
SPRING_PROFILES_ACTIVE=prod,demo docker compose -f compose.yaml up --build
```

Dane odpowiadają stanowi, jaki aplikacja zostawia po przeprowadzeniu dwóch sprintów przez API:
sprinty są zakończone, zadania są od nich odpięte, a przebieg sprintów opisują zdarzenia w `sprint_issue_event`.
Identyfikatory zaczynają się od 9001, więc nie kolidują z rekordami tworzonymi przez aplikację.
Każdy `INSERT` ma `ON CONFLICT DO NOTHING`, więc ponowny start nie duplikuje danych.

Projekt `9001` „Sklep internetowy (demo)”:

| Sprint | Okres | Co pokazuje | Zakres początkowy | Ukończone / velocity | Nieukończone |
|---|---|---|---|---|---|
| `9001` Sprint 1 | 5–9 X 2026 | zadanie bez estymacji (9004), zadanie dodane po starcie (9006), zmiana estymacji 5 → 8 (9003) | 5 zadań, 18 SP | 5 zadań, 18 SP | 1 zadanie (9005), 5 SP |
| `9002` Sprint 2 | 12–16 X 2026 | zadanie przeniesione ze sprintu 1 (9005), zadanie usunięte ze sprintu (9009), dodany błąd (9010), zadanie bez estymacji (9008) | 4 zadania, 16 SP | 3 zadania, 7 SP | 1 zadanie (9007), 8 SP |

Sprint 1 dzień po dniu (`/sprints/9001/burndown` i `/burnup`):

| `day` | 0 | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|---|
| `remainingWork` | 18 | 18 | 13 | 13 | 8 | 0 |
| `remainingScope` | 18 | 18 | 15 | 18 | 13 | 5 |
| `completedWork` | 0 | 0 | 5 | 5 | 10 | 18 |
| `totalScope` | 18 | 18 | 20 | 23 | 23 | 23 |

Burndown spada do 0, mimo że w sprincie zostało 5 SP: liczy się od zakresu początkowego (18 SP), a praca dodana po starcie (9006)
i zwiększona estymacja (9003) zostały ukończone, więc ich punkty też obniżają `remainingWork`.
Pozostałe 5 SP (zadanie 9005) widać w `remainingScope`.
Zmianę zakresu widać na burnup (`totalScope` 18 → 20 → 23). Filtr `?from=2026-10-12` zwraca tylko sprint 2, bo sprint 1 zakończył się 9 X.

## Testy i pokrycie

```bash
./mvnw clean verify
```

Wynik: 298 testów, wszystkie przechodzą; pokrycie instrukcji 89,37% (4104 z 4592). Raport JaCoCo powstaje
w `target/site/jacoco/index.html`. Pomiar obejmuje cały kod produkcyjny bez ręcznych wykluczeń; JaCoCo pomija jedynie
kod generowany przez Lomboka (gettery, settery, konstruktory, oznaczone przez Lomboka adnotacją `@Generated`), więc wynik
dotyczy kodu napisanego przez zespół. Wymagane minimum to 75% pokrycia instrukcji; przy niższym `verify` kończy się błędem
(`Rule violated ... expected minimum is 0.75`). `clean` usuwa wyniki poprzednich uruchomień, które JaCoCo inaczej by doliczył.

| Test | Rodzaj | Co sprawdza |
|---|---|---|
| `SprintMetricsCalculatorTest` | jednostkowe | velocity (WF-2): sprint bez zadań, tylko zadania nieukończone, zadania bez estymacji, estymacja z chwili ukończenia, zadanie usunięte po ukończeniu, sprint bez historii |
| `SprintIssueStatesTest` | jednostkowe | odtworzenie stanu zadań sprintu ze zdarzeń |
| `SprintProgressCalculatorTest` | jednostkowe | punkty wykresów: zmiany zakresu, strefa czasowa, brak estymacji |
| `SprintDateRangeTest` | jednostkowe | granice przedziału, jedna granica, strefa czasowa, sprinty bez dat |
| `SprintComparisonServiceImplTest` | jednostkowe (Mockito) | jedno zapytanie o zdarzenia, brak mieszania zdarzeń między sprintami |
| `SprintComparisonCsvWriterTest` | jednostkowe | nagłówek, kolejność, puste daty, escapowanie |
| `SprintReportServiceTest`, `SprintChartServiceTest`, `SprintComparisonServiceTest` | integracyjne | pełny cykl sprintu przez serwisy, zgodność liczb między raportami, izolacja projektów, filtry |
| `AnalyticsRestControllerIntegrationTest` | integracyjne (MockMvc) | endpointy modułu: odpowiedzi, kody błędów, CSV |
| `DemoDataIntegrationTest` | integracyjne | wczytanie danych demonstracyjnych przez profil `demo` i wartości z tabel powyżej |
| testy pozostałych kontrolerów (`*RestControllerIntegrationTest`) | integracyjne (MockMvc) | istniejące API; każdy kontroler ma 100% pokrycia instrukcji |

## Podział prac i zrealizowane zadania

| Osoba | Login GitHub | Zakres | Wymagania |
|---|---|---|---|
| A – Kamil Oleksiak | JasperBTA | cykl życia sprintu, historia zdarzeń sprintu, dane demonstracyjne | WF-1–WF-4 (historia danych), WNF-4 |
| B – Jakub Sawicki | JakubSawicki03 | obsługa błędów API, velocity, raport sprintu | WF-1, WF-2, WNF-6 |
| C – Nikodem Wełna | shoxxie01 | dane do wykresów burndown i burnup | WF-3, WF-4 |
| D – Mateusz Słupski | SlupskiMateusz02 | porównanie sprintów, filtrowanie po dacie | WF-5, WF-6 |
| E – Antoni Widelski | antoniwidelski | eksport CSV, pomiar pokrycia, testy kontrolerów, instrukcja uruchomienia | WF-7, WNF-3, WNF-7 |

| Issue | Tytuł | Osoba |
|---|---|---|
| #4 | Reguły obliczeń i kontrakt API (ten dokument) | wspólnie |
| #5 | Zachowanie zakończonego sprintu | A |
| #6 | Blokady zakończonego sprintu | A |
| #7 | Historia zdarzeń sprintu | A |
| #8 | Zapisywanie zdarzeń sprintu | A |
| #9 | Dane demonstracyjne | A |
| #10 | Obsługa błędów API | B |
| #11 | Obliczanie velocity | B |
| #12 | Raport podsumowujący sprint | B |
| #13 | Dane do wykresu burndown | C |
| #14 | Dane do wykresu burnup | C |
| #15 | Porównanie sprintów projektu | D |
| #16 | Filtrowanie zestawienia sprintów po dacie | D |
| #17 | Generator CSV | E |
| #18 | Eksport zestawienia do CSV | E |
| #19 | Pomiar pokrycia JaCoCo | E |
| #20 | Testy kontrolerów | E |
| #21 | Instrukcja uruchomienia i konfiguracja | E |
| #22 | Pull Request i oddanie projektu | wspólnie |
