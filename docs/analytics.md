# Moduł analityczny sprintów — plan i reguły

**Data rozpoczęcia projektu:** 2026-11-07

Dokument jest wspólnym punktem startowym zespołu. Reguły poniżej to propozycja do zatwierdzenia;
zmiany wprowadzamy w tym pliku, żeby kod, testy i dokumentacja API opierały się na tych samych ustaleniach.

## Ograniczenia obecnego kodu

- `SprintServiceImpl.stopSprint` odpina zadania i usuwa sprint, więc zakończone sprinty nie istnieją w bazie.
- `Sprint` ma tylko edytowalne pole `startDate`; brakuje faktycznych dat startu i zakończenia.
- Aplikacja nie zapisuje daty ukończenia zadania ani historii estymacji i zakresu sprintu.
- Brak globalnej obsługi błędów, więc nieistniejący zasób kończy się odpowiedzią HTTP 500.

## Proponowany model danych

- `Sprint`: nowe pola `startedAt` i `endedAt`; `startDate` pozostaje datą planowaną.
- Zakończony sprint nie jest usuwany. Nie można go ponownie uruchomić, edytować ani dodawać do niego zadań.
- Tabela `sprint_issue_event`: `sprint_id`, `issue_id`, `type`, `estimation`, `occurred_at`.
- Typy zdarzeń: `IN_SCOPE_AT_START`, `ADDED`, `REMOVED`, `ESTIMATION_CHANGED`, `COMPLETED`.
- `issue_id` nie ma klucza obcego, ponieważ `deleteIssue` usuwa zadania fizycznie.
- Schemat aktualizuje `ddl-auto: update`, czyli mechanizm już stosowany w projekcie.

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
