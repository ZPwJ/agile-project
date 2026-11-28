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
