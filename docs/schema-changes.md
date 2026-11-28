# Zmiany schematu bazy danych

Projekt aktualizuje schemat mechanizmem Hibernate (`spring.jpa.hibernate.ddl-auto: update`). Nie ma osobnych skryptów migracji: wystarczy uruchomić nową wersję aplikacji na istniejącej bazie.

## Rzeczywiste daty sprintu (tabela `sprint`)

| Kolumna | Typ w PostgreSQL | `null` | Znaczenie |
|---|---|---|---|
| `started_at` | `timestamp(6) with time zone` | dozwolony | chwila uruchomienia sprintu |
| `ended_at` | `timestamp(6) with time zone` | dozwolony | chwila zakończenia sprintu |

Kolumny są niezależne od `start_date`, która może być datą planowaną.

### Powód

Zakończenie sprintu usuwało go z bazy, więc nie dało się przygotować raportów dla zakończonych sprintów. Teraz sprint zostaje w bazie z `is_done = true`, `active = false` i ustawionym `ended_at`. Zadania są od niego odpinane jak wcześniej.

### Istniejące dane

- Istniejące sprinty mają po aktualizacji `started_at` i `ended_at` równe `null`. Wartości nie są uzupełniane z `start_date`, bo nie wiadomo, czy to rzeczywista data rozpoczęcia.
- Sprint aktywny w chwili aktualizacji po zakończeniu ma `ended_at`, a `started_at` pozostaje `null`.
- Sprintów zakończonych przed aktualizacją nie da się odtworzyć, bo zostały usunięte.

### Sprawdzenie

Po uruchomieniu aplikacji:

```sql
select column_name, data_type, is_nullable, datetime_precision
from information_schema.columns
where table_name = 'sprint' and column_name in ('started_at', 'ended_at');
```

Oczekiwany wynik: dwa wiersze, typ `timestamp with time zone`, `is_nullable = YES`, precyzja 6.

### Wycofanie

Starsza wersja aplikacji ignoruje dodatkowe kolumny, więc nie trzeba ich usuwać. Zakończone sprinty zostaną w bazie i nie pojawią się na liście sprintów nieaktywowanych. Starsza wersja nie blokuje jednak dodawania zadań do zakończonego sprintu ani jego ponownego uruchomienia.

## Dziennik zdarzeń sprintu (tabela `sprint_issue_event`)

Nowa tabela. Istniejące tabele się nie zmieniają.

| Kolumna | Typ w PostgreSQL | `null` | Znaczenie |
|---|---|---|---|
| `sprint_issue_eventid` | `integer`, klucz główny | nie | identyfikator zdarzenia |
| `sprintid` | `integer` | nie | sprint |
| `issueid` | `integer` | nie | zadanie |
| `type` | `varchar` | nie | `COMMITTED`, `ADDED`, `REMOVED`, `COMPLETED` |
| `estimation` | `integer` | dozwolony | estymacja w story points |
| `occurred_at` | `timestamp(6) with time zone` | nie | chwila zdarzenia |

Dodatkowo powstają:

- sekwencja `sprint_issue_event_seq` (krok 50),
- indeks `sprint_issue_event_sprint_issue_idx` na `(sprintid, issueid)`, używany do szukania ostatniego wejścia zadania do zakresu i, przez pierwszą kolumnę, do odczytu historii sprintów,
- ograniczenie `sprint_issue_event_type_check` na wartości `type`. `ddl-auto: update` go nie zmienia, więc dodanie nowego typu wymaga ręcznej zmiany ograniczenia.

`sprintid` i `issueid` nie mają kluczy obcych, aby historia nie blokowała usuwania zadań, sprintów i projektów.

### Kiedy powstają zdarzenia

| Operacja | Zdarzenie |
|---|---|
| uruchomienie sprintu | `COMMITTED` dla każdego zadania, z czasem równym `started_at` |
| dodanie zadania do aktywnego sprintu | `ADDED` |
| usunięcie zadania z aktywnego sprintu | `REMOVED`, dla sprintu, w którym zadanie faktycznie było |
| usunięcie zadania z systemu, gdy jest w aktywnym sprincie | `REMOVED` |
| przejście zadania do `DONE` | `COMPLETED` |
| zakończenie sprintu | brak zdarzeń |

Zakończenie sprintu nie zapisuje zdarzeń, choć Issue #102 wymienia `stopSprint` wśród miejsc zapisu. Koniec historii wyznacza `sprint.ended_at`, a skład sprintu na koniec wynika ze zdarzeń. Zdarzenia `REMOVED` dla zadań nieukończonych zaniżyłyby zakres i wyzerowały ich liczbę.

### Jak czytać zdarzenia

- Zakres: suma `COMMITTED` i `ADDED` minus suma `REMOVED`.
- Praca ukończona: suma `COMPLETED`. Usunięcie ukończonego zadania odejmuje je także od pracy ukończonej.
- `REMOVED` i `COMPLETED` mają estymację z ostatniego `COMMITTED` lub `ADDED` tego zadania w tym sprincie, nie bieżącą. Dzięki temu praca ukończona nie przekracza zakresu.
- Historię sprintu uznajemy za dostępną, gdy ma co najmniej jedno zdarzenie `COMMITTED`.
- Odczyt: `SprintEventReader.findBySprintIDs`, jednym zapytaniem, rosnąco po czasie i identyfikatorze.

### Ograniczenia

- Brak historii sprzed wdrożenia. Sprint aktywny w chwili aktualizacji nie ma `COMMITTED`, a jego późniejsze zdarzenia nie wystarczają do odtworzenia przebiegu.
- Zmiana estymacji po wejściu zadania do zakresu nie jest zapisywana i nie wpływa na raport tego sprintu.
- Zdarzenia zostają po usunięciu zadania, sprintu lub projektu.
- Ujemna estymacja jest nadal przyjmowana przez aplikację i może dać ujemne sumy.
- Sprinty mogą zawierać zadania z innego projektu lub zadania `DONE` dodane przed tą zmianą. Takie dodania są teraz odrzucane, ale istniejące dane nie są poprawiane.

### Sprawdzenie

```sql
select indexname from pg_indexes where tablename = 'sprint_issue_event';
```

Oczekiwane: klucz główny i `sprint_issue_event_sprint_issue_idx`.

### Wycofanie

Starsza wersja aplikacji ignoruje nową tabelę. Nie zapisuje zdarzeń, więc historia sprintów trwających po wycofaniu będzie niepełna.
