# Raporty sprintów - założenia

Zasady, według których liczymy raporty sprintów (WF-1 do WF-7). Same endpointy raportowe dojdą w kolejnych zadaniach.

## Estymacje i statusy

- Estymację podajemy w story points (liczba całkowita).
- Za ukończone uznajemy tylko zadania ze statusem `DONE`. Pozostałe statusy (`TO_DO`, `IN_PROGRESS`, `CODE_REVIEW`, `IN_TESTS`) oznaczają zadanie nieukończone.
- Zadanie bez estymacji wlicza się do liczby zadań, ale ma 0 punktów.
- Ujemnej estymacji nie da się zapisać (400). Jeśli w bazie zostały stare ujemne wartości, liczymy je jako 0, żeby pozostała praca nigdy nie wyszła ujemna.

## Velocity i liczniki

- Velocity to suma punktów zadań `DONE` w chwili zamknięcia sprintu. Późniejsze zmiany zadań nie zmieniają już wyniku.
- Początkowa liczba zadań to stan z chwili startu sprintu. Liczba ukończonych i nieukończonych zadań to stan z chwili zamknięcia, więc ich suma może się różnić od liczby początkowej, jeśli w trakcie dodano albo usunięto zadania.
- Pusty sprint ma wszystkie wartości równe 0.

## Historia sprintu

Wcześniej aplikacja nie zapisywała żadnej historii, a zamknięty sprint był usuwany z bazy. Teraz:

- zamknięty sprint zostaje w bazie razem z datą zakończenia, a nieukończone zadania wracają do backlogu jako `TO_DO`,
- w tabeli `sprint_progress` zapisujemy stan sprintu: liczbę zadań, liczbę ukończonych, sumę punktów i sumę ukończonych punktów. Wpis powstaje przy starcie, przy każdej zmianie w trwającym sprincie (dodanie lub usunięcie zadania, zmiana estymacji albo statusu) i przy zamknięciu.

Dodanie zadania zwiększa zakres, usunięcie go zmniejsza. Zmiana estymacji zmienia punkty od momentu zapisu, również dla zadań już ukończonych. Starych wpisów nie poprawiamy.

## Burndown i burnup

Wartości są w story points, a czas punktu to moment zapisu wpisu.

- burndown: pozostała praca = suma punktów - ukończone punkty,
- burnup: ukończone punkty oraz całkowity zakres (suma punktów).

Punkty wykresu to kolejne wpisy z `sprint_progress` posortowane po czasie, a przy tym samym czasie po ID. Nie dorabiamy punktów dla dni, w których nic się nie zmieniło. Sprint bez estymacji ma na wykresie same zera.

## Brak historii

Nie uzupełniamy brakujących danych. Jeśli sprint nie ma wpisu z chwili startu, początkowa liczba zadań jest nieznana, a wykres pokazuje tylko te wpisy, które są. Sprintów usuniętych z bazy przed tą zmianą nie da się już odtworzyć.

## Daty i filtrowanie

- Czas w odpowiedziach jest w formacie ISO 8601 w UTC, np. `2026-11-11T21:15:54Z`. Daty w filtrach: `YYYY-MM-DD`.
- Filtr działa po dacie zakończenia sprintu (w UTC). `from` i `to` są opcjonalne, obie granice są włączne. Jeśli `from` jest późniejsze niż `to`, API zwraca 400.
- Porównanie sprintów i CSV obejmują tylko zakończone sprinty danego projektu, od najstarszego (po dacie zakończenia, potem po ID).
- Nieistniejący projekt: 404. Brak wyników: pusta lista, a w CSV sam nagłówek.
- Raport dla trwającego sprintu albo sprintu bez zapisanego wyniku: 409.
