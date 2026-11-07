# Moduł analityczny sprintów

Moduł dostarcza raporty sprintów: podsumowanie, velocity, dane do wykresów burndown i burnup,
porównanie sprintów z filtrowaniem po datach oraz eksport CSV. Prace rozpoczęto 7 listopada 2026 r.

## Podział prac

| Osoba | Zakres | Wymagania |
|---|---|---|
| A – Kamil Oleksiak (JasperBTA) | cykl życia sprintu, historia zdarzeń sprintu (#5, #6, #7, #8) | WF-1, WF-2, WF-3, WF-4 (dane) |
| B | obsługa błędów API, velocity, raport podsumowujący sprint (#10, #11, #12) | WF-1, WF-2, WNF |
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
  Zadanie usunięte ze sprintu po ukończeniu nie jest wliczane.
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
