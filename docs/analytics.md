# Moduł analityczny sprintów — plan i reguły

**Data rozpoczęcia projektu:** 2026-11-07

Dokument jest wspólnym punktem startowym zespołu. Reguły poniżej to propozycja do zatwierdzenia;
zmiany wprowadzamy w tym pliku, żeby kod, testy i dokumentacja API opierały się na tych samych ustaleniach.

## Ograniczenia obecnego kodu

- `SprintServiceImpl.stopSprint` odpina zadania i usuwa sprint, więc zakończone sprinty nie istnieją w bazie.
- `Sprint` ma tylko edytowalne pole `startDate`; brakuje faktycznych dat startu i zakończenia.
- Aplikacja nie zapisuje daty ukończenia zadania ani historii estymacji i zakresu sprintu.
- Brak globalnej obsługi błędów, więc nieistniejący zasób kończy się odpowiedzią HTTP 500.

## Model danych (zaimplementowany)

- `Sprint` ma nowe pola `startedAt` (ustawiane w `startSprint`) i `endedAt` (ustawiane w `stopSprint`);
  `startDate` pozostaje edytowalną datą planowaną.
- `stopSprint` nie usuwa już sprintu: ustawia `isDone = true` i `endedAt`. Zadania nadal wracają do backlogu
  (`sprint = null`), a nieukończone dostają status `TO_DO`, tak jak wcześniej.
- Zakończonego sprintu nie można uruchomić, usunąć ani dodać do niego zadań
  (komunikat `SPRINT_ALREADY_FINISHED`). Nazwę i cel można nadal edytować.
- Nazwa zakończonego sprintu pozostaje zajęta w projekcie, bo sprint już nie znika.
- Encja `SprintIssueEvent` (tabela `sprint_issue_event`): `id`, `sprintID`, `issueID`, `type`, `estimation`, `occurredAt`.
  `sprintID` i `issueID` to zwykłe wartości bez kluczy obcych, więc historia przetrwa usunięcie zadania.
- Odczyt historii: `SprintIssueEventRepository.findAllBySprintIDOrderByOccurredAtAscIdAsc(sprintID)`,
  posortowane chronologicznie, a przy równym czasie po `id`.
- Schemat aktualizuje `ddl-auto: update`, czyli mechanizm już stosowany w projekcie. Nowe kolumny są puste
  dla starszych sprintów.

### Kiedy powstaje zdarzenie

Zdarzenia powstają tylko dla sprintu, który wystartował. Zmiany przed startem obejmuje `SCOPE_AT_START`.

| Typ | Kiedy | `estimation` |
|---|---|---|
| `SCOPE_AT_START` | `startSprint`, po jednym dla każdego zadania w sprincie | estymacja w chwili startu |
| `ADDED` | `addIssueToSprint` do aktywnego sprintu | estymacja w chwili dodania |
| `REMOVED` | `deleteIssueFromSprint` albo `deleteIssue` dla zadania w aktywnym sprincie | estymacja w chwili usunięcia |
| `ESTIMATION_CHANGED` | `updateIssue` zmienia estymację zadania w aktywnym sprincie | nowa estymacja |
| `COMPLETED` | `changeIssueStatus` na `DONE` | estymacja w chwili ukończenia |

- `estimation` może być `null`, gdy zadanie nie ma estymacji.
- `DONE` jest statusem nieodwracalnym, więc zadanie ma co najwyżej jedno `COMPLETED` w sprincie.
- Nieukończone zadania nie mają osobnego zdarzenia przy `stopSprint`: to zakres sprintu
  (`SCOPE_AT_START` + `ADDED` − `REMOVED`) bez zadań z `COMPLETED`.
- `REMOVED` po `COMPLETED` oznacza zadanie usunięte po ukończeniu; zgodnie z regułami nie liczy się do velocity.
- `occurredAt` zapisuje się w strefie czasowej serwera; raporty przeliczają je na dni w `Europe/Warsaw`.
- Sprint z `startedAt == null` (nie wystartował albo trwał już przed wdrożeniem historii) nie ma wiarygodnej
  historii: wykresy zwracają dla niego pustą listę punktów.
- Zadania `DONE` nie można dodać do sprintu (komunikat `ISSUE_ALREADY_DONE`), więc zadanie ukończone
  w jednym sprincie nie wraca jako otwarta praca w kolejnym.

## Reguły obliczeń

- Statusem końcowym jest wyłącznie `DONE`.
- Velocity to suma estymacji zadań `DONE` w sprincie, z wartością estymacji z chwili ukończenia.
- Zadanie bez estymacji liczy się do liczby zadań, ale dodaje 0 punktów.
- Zadanie usunięte ze sprintu po ukończeniu nie liczy się do velocity.
- Początkowy zakres to zadania przypisane do sprintu w chwili jego startu.
- Burnup pokazuje zmiany zakresu w linii całkowitego zakresu.
- Sprint, który nie wystartował, ma pustą listę punktów na wykresach.
- Dni wykresów to dni kalendarzowe w strefie `Europe/Warsaw`.
- Do ustalenia: sposób pokazywania w burndownie pracy dodanej po starcie sprintu.

## Reguły API

- Zestawienia i CSV obejmują tylko sprinty zamknięte, od najstarszego `startedAt`.
- Filtr dat zwraca sprinty nachodzące na przedział `from`–`to`; `from > to` daje HTTP 400.
- Daty i znaczniki czasu mają format ISO-8601.
- Nieistniejący sprint lub projekt daje HTTP 404.
- Usunięcie zamkniętego sprintu jest blokowane odpowiedzią HTTP 409.
- Nazwa zamkniętego sprintu pozostaje zajęta w projekcie.

## Proponowane endpointy

| Metoda | Ścieżka | Wymaganie |
|---|---|---|
| GET | `/api/analytics/sprints/{id}/report` | WF-1, WF-2 |
| GET | `/api/analytics/sprints/{id}/burndown` | WF-3 |
| GET | `/api/analytics/sprints/{id}/burnup` | WF-4 |
| GET | `/api/analytics/projects/{id}/sprints?from=&to=` | WF-5, WF-6 |
| GET | `/api/analytics/projects/{id}/sprints/export.csv?from=&to=` | WF-7 |

## Podział obowiązków

| Osoba | Obszar | Zakres |
|---|---|---|
| A | Cykl życia sprintu i historia | Pola sprintu, tabela zdarzeń, zapisywanie zdarzeń, blokady zamkniętych sprintów, dane demo |
| B | Raport i velocity | WF-1, WF-2, globalna obsługa błędów |
| C | Wykresy | WF-3, WF-4 |
| D | Porównanie i filtry | WF-5, WF-6, zapytania zbiorcze bez N+1 |
| E | CSV i jakość | WF-7, JaCoCo, testy kontrolerów, README, uruchamianie projektu |

Każda osoba pisze testy do swojego kodu i pracuje na własnym branchu, który trafia do `feature/sprint-analytics`.
