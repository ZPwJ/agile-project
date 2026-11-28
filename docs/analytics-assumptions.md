# Moduł analityczny sprintów: założenia i kontrakt

## 1. Status

Koncepcja z Issue #100 została omówiona i uzgodniona na spotkaniu zespołu.

Kontrakt DTO, sygnatury serwisów, adresy endpointów i ustalenia pomocnicze to **propozycje, do potwierdzenia**.

Oznaczenia:

| Oznaczenie | Znaczenie |
|---|---|
| **[WYMAGANIE]** | wynika z wymagań projektu (`WF-x`, `WNF-x`) |
| **[UZGODNIONE]** | koncepcja z Issue #100 |
| **[PROPOZYCJA]** | propozycja, do potwierdzenia |
| **[DO USTALENIA]** | kwestia otwarta, opisana jako `D1`…`D21` w sekcji 9 |
| *przyjęte w implementacji* | rozwiązanie z PR #173 lub #174, do potwierdzenia przez zespół |

## 2. Pojęcia

- **[PROPOZYCJA]** Status końcowy: wyłącznie `DONE`. Zadanie w każdym innym statusie jest nieukończone. Zdarzenie ukończenia powstaje tylko przy przejściu do `DONE` — przyjęte w implementacji (PR #174), do potwierdzenia przez zespół.
- **[PROPOZYCJA]** Jednostka estymacji: story points.
- **[PROPOZYCJA]** Zadanie bez estymacji (`null`) wlicza się do liczby zadań, a do sumy punktów dodaje 0.
- **[UZGODNIONE]** Daty i czas: UTC, format ISO-8601. „Dzień” wykresu to data kalendarzowa UTC.
- **[UZGODNIONE]** `startedAt` i `endedAt` są rzeczywiste i odrębne od planowanej `startDate`.

## 3. Historia sprintu i zakończenie sprintu

- **[UZGODNIONE]** Historia dla burndown i burnup: dziennik zdarzeń. Tabela zdarzeń sprintu (`COMMITTED`, `ADDED`, `REMOVED`, `COMPLETED`) z estymacją i czasem zdarzenia.
- **[UZGODNIONE]** Wykresy są liczone z dziennika, a nie ze stanu bieżącego.
- **[UZGODNIONE]** Sprinty bez historii (sprzed wdrożenia): `historyAvailable=false`, tylko punkty wiarygodne (szczegóły: D11).
- **[UZGODNIONE]** Zakończony sprint pozostaje w bazie. `stopSprint` ustawia `isDone=true`, `active=false` i `endedAt`.
- **[UZGODNIONE]** Zadania nieukończone wracają do backlogu produktu (`TO_DO`), a skład sprintu jest odtwarzany z dziennika zdarzeń.

### Kiedy powstają zdarzenia

Zasady z tej części są przyjęte w implementacji (PR #174), do potwierdzenia przez zespół.

| Operacja | Zdarzenie |
|---|---|
| uruchomienie sprintu | `COMMITTED` dla każdego zadania, z czasem równym `startedAt` |
| dodanie zadania do aktywnego sprintu | `ADDED` |
| usunięcie zadania z aktywnego sprintu | `REMOVED`, dla sprintu, w którym zadanie faktycznie było |
| usunięcie zadania z systemu, gdy jest w aktywnym sprincie | `REMOVED` (D4) |
| przejście zadania do `DONE` | `COMPLETED` |
| zakończenie sprintu (`stopSprint`) | brak zdarzeń |

- Dla sprintu nieaktywnego zdarzenia nie powstają (D6).
- `stopSprint` nie zapisuje zdarzeń, inaczej niż w opisie Issue #102. Koniec historii wyznacza `endedAt`, a skład sprintu na koniec wynika ze zdarzeń.
- Historia sprintu jest dostępna, gdy ma co najmniej jedno zdarzenie `COMMITTED` (D11).

### Model danych

Zmiany modelu danych realizują zadania 05 (#101: pola `startedAt`, `endedAt`) i 06 (#102: dziennik zdarzeń). Schemat aktualizuje się mechanizmem używanym w projekcie (`ddl-auto: update`); nowe kolumny dopuszczają `null` (WNF-4). Szczegóły: `docs/schema-changes.md`, dodawany w PR #173 i #174.

## 4. Reguły obliczeń

### 4.1. Raport sprintu (WF-1)

- **[WYMAGANIE]** Raport dotyczy wyłącznie wskazanego sprintu i zawiera: identyfikator i nazwę sprintu, identyfikator projektu, daty rozpoczęcia i zakończenia, początkową liczbę zadań, liczbę ukończonych i nieukończonych, velocity oraz sumę estymacji zadań nieukończonych.
- **[WYMAGANIE]** Brak estymacji nie powoduje awarii. Nieistniejący sprint daje odpowiedź błędu (404).
- **[PROPOZYCJA]** Raport sprintu, który się jeszcze nie rozpoczął: 409 z komunikatem.
- **[DO USTALENIA]** Sposób liczenia zadań przy zmianach zakresu: D9.

### 4.2. Velocity (WF-2)

- **[WYMAGANIE]** Velocity to suma estymacji zadań ukończonych w danym sprincie. Zadania nieukończone jej nie zwiększają, a brak estymacji nie powoduje błędu.
- **[WYMAGANIE]** Testy obejmują: sprint bez zadań, sprint z zadaniami nieukończonymi i zadania bez estymacji.
- Velocity liczy się z dziennika zdarzeń, bo po zakończeniu sprintu zadania są od niego odpięte (D1, D8) — przyjęte w implementacji (PR #173 i #174), do potwierdzenia przez zespół. Samo obliczanie należy do zadania 08.
- `COMMITTED` i `ADDED` zapisują estymację z chwili zdarzenia. `REMOVED` i `COMPLETED` kopiują estymację z ostatniego `COMMITTED` lub `ADDED` tego zadania w tym sprincie, więc velocity liczy się z wartości, z którą zadanie weszło do zakresu (D2) — przyjęte w implementacji (PR #174), do potwierdzenia przez zespół.

### 4.3. Burndown (WF-3)

- **[WYMAGANIE]** Każdy punkt ma znacznik czasu i pozostałą pracę, a jednostka jest jednoznaczna. Punkty są chronologiczne, wartości nieujemne. Nie wolno przedstawiać wymyślonych danych jako rzeczywistych.
- **[UZGODNIONE]** Pozostała praca = zakres w chwili t minus praca ukończona do chwili t. Dodanie zadania podnosi linię, usunięcie ją obniża. Wartości są nieujemne.
- Zakres to suma `COMMITTED` i `ADDED` minus suma `REMOVED`, a praca ukończona to suma `COMPLETED`. Usunięcie ukończonego zadania odejmuje je także od pracy ukończonej (D2, D3) — przyjęte w implementacji (PR #174), do potwierdzenia przez zespół. Przy nieujemnych estymacjach praca ukończona nie przekracza zakresu. Obliczenia należą do zadań 08–10.
- **[UZGODNIONE]** Krok burndown: punkt początkowy plus jeden punkt na każdy dzień (stan na koniec dnia) od `startedAt` do `endedAt` (dla sprintu aktywnego do chwili bieżącej).
- **[PROPOZYCJA]** Sprint bez zadań z estymacją: punkty istnieją, a pozostała praca wynosi 0.
- **[DO USTALENIA]** Zgodność z regułą 2 z WF-3 (D10), format punktów (D14), sprint nieuruchomiony (D12).

### 4.4. Burnup (WF-4)

- **[WYMAGANIE]** Każdy punkt ma znacznik czasu i skumulowaną estymację ukończonej pracy, a także całkowity zakres, jeśli da się go wiarygodnie ustalić. Punkty są chronologiczne.
- **[WYMAGANIE]** Praca ukończona nie obejmuje zadań spoza wskazanego sprintu. Pola i jednostki są jednoznacznie opisane, a działanie pokryte testami.
- **[UZGODNIONE]** Zakres i praca ukończona wynikają z dziennika zdarzeń. Sprint bez historii ma `historyAvailable=false`.
- **[WYMAGANIE]** Bez historii zmian nie sugerujemy zmian zakresu, których aplikacja nie zapisała.
- **[DO USTALENIA]** Format punktów (D14), sprint nieuruchomiony (D12).

### 4.5. Przypadki brzegowe (WNF-4)

- **[WYMAGANIE]** Wyniki są deterministyczne dla tego samego stanu danych. Raporty nie łączą zadań z różnych sprintów ani projektów.
- **[PROPOZYCJA]** Pusty sprint: liczniki i sumy punktów wynoszą 0.
- **[PROPOZYCJA]** Brak sprintów spełniających warunki: pusta lista `sprints`.

## 5. Zestawienie, filtry i CSV

### 5.1. Zestawienie sprintów (WF-5)

- **[WYMAGANIE]** Dla każdego sprintu projektu: identyfikator i nazwa, daty rozpoczęcia i zakończenia, liczba ukończonych i nieukończonych zadań, velocity. Sprinty bez ukończonych zadań też występują. Kolejność jest stabilna i udokumentowana, a nieistniejący projekt obsłużony.
- **[UZGODNIONE]** Zestawienie obejmuje sprinty uruchomione (aktywne i zakończone) z polem `status`.
- **[UZGODNIONE]** Sortowanie rosnąco po `startedAt`, następnie po identyfikatorze.
- **[PROPOZYCJA]** Nieistniejący projekt: 404.

### 5.2. Filtry (WF-6)

- **[WYMAGANIE]** Filtrowanie co najmniej po projekcie i przedziale dat. Parametry są walidowane, błędny przedział daje czytelny błąd, a dane innych projektów nie są zwracane. Reguła jest udokumentowana i przetestowana.
- **[UZGODNIONE]** Filtr obejmuje sprinty nachodzące na przedział [`from`, `to`]. Sprint aktywny trwa do chwili bieżącej.
- **[PROPOZYCJA]** Zły format daty albo `from` późniejsze niż `to`: 400.
- **[DO USTALENIA]** Typ parametrów, granice, brak jednej z dat, sprinty bez `startedAt`: D16.

### 5.3. Eksport CSV (WF-7)

- **[WYMAGANIE]** Kolumny: identyfikator projektu, identyfikator sprintu, nazwa, daty rozpoczęcia i zakończenia, liczba ukończonych i nieukończonych zadań, velocity.
- **[WYMAGANIE]** Eksport respektuje te same filtry co zestawienie, ma wiersz nagłówka, poprawnie escapuje separator, cudzysłów i nową linię, ma właściwy typ treści i nazwę pliku. Puste zestawienie daje przewidywalny wynik.
- **[PROPOZYCJA]** Escapowanie według RFC 4180, `Content-Type: text/csv; charset=UTF-8`, `Content-Disposition: attachment`. Puste zestawienie: sam nagłówek.
- **[PROPOZYCJA]** Nazwy kolumn: `projectID`, `sprintID`, `sprintName`, `startedAt`, `endedAt`, `completedIssueCount`, `uncompletedIssueCount`, `velocity`.
- **[DO USTALENIA]** Separator, nazwy nagłówków, nazwa pliku, puste daty, BOM: D15.

## 6. Endpointy, kody odpowiedzi i uprawnienia

**[PROPOZYCJA]** Osobny kontroler `/api/report/...`.

| Zadanie | Metoda i adres | Odpowiedź 200 | Błędy |
|---|---|---|---|
| 07 | `GET /api/report/sprint/{sprintID}` | `SprintReportDTO` | 403, 404, 409 |
| 09 | `GET /api/report/sprint/{sprintID}/burndown` | `BurndownDTO` | 403, 404, 409 (D12) |
| 10 | `GET /api/report/sprint/{sprintID}/burnup` | `BurnupDTO` | 403, 404, 409 (D12) |
| 11, 12 | `GET /api/report/project/{projectID}/sprints?from=&to=` | `SprintComparisonDTO` | 400, 403, 404 |
| 13 | `GET /api/report/project/{projectID}/sprints/export?from=&to=` | plik CSV | 400, 403, 404 |

- **[PROPOZYCJA]** Kody: 400 niepoprawne parametry, 403 brak uprawnień, 404 brak zasobu, 409 sprint jeszcze nieuruchomiony.
- **[PROPOZYCJA]** Raporty są dostępne dla uczestników projektu; pozostali dostają 403 (D17).
- **[PROPOZYCJA]** Format błędów z zadania 03 (#99): czas, status, komunikat, ścieżka. Nazwy pól ustala zadanie 03 (D21).
- **[WYMAGANIE]** Błędy są czytelne, bez stosu wywołań i szczegółów wewnętrznych. Odpowiedzi JSON mają spójną strukturę (WNF-6).

## 7. Kontrakt DTO

**[PROPOZYCJA]** Typy: identyfikatory `Integer`; znaczniki czasu `Instant` (ISO-8601, UTC); daty `LocalDate`; punkty i liczniki `Integer`. Znak `?` oznacza, że pole może być `null`.

| DTO | Pola |
|---|---|
| `SprintReportDTO` | `sprintID`, `sprintName`, `projectID`, `status`, `startedAt?`, `endedAt?`, `initialIssueCount?`, `completedIssueCount?`, `uncompletedIssueCount?`, `velocity?`, `uncompletedPoints?`, `unit`, `historyAvailable` |
| `BurndownDTO` | `sprintID`, `unit`, `historyAvailable`, `points: List<BurndownPointDTO>` |
| `BurndownPointDTO` | `timestamp`, `remainingPoints` |
| `BurnupDTO` | `sprintID`, `unit`, `historyAvailable`, `points: List<BurnupPointDTO>` |
| `BurnupPointDTO` | `timestamp`, `completedPoints`, `scopePoints?` |
| `SprintComparisonDTO` | `projectID`, `unit`, `sprints: List<SprintSummaryDTO>` |
| `SprintSummaryDTO` | `sprintID`, `sprintName`, `status`, `startedAt?`, `endedAt?`, `completedIssueCount?`, `uncompletedIssueCount?`, `velocity?`, `historyAvailable` |
| `SprintFilter` | `from?` (`LocalDate`), `to?` (`LocalDate`) |

Znaczenie pól:

| Pole | Znaczenie |
|---|---|
| `status` | `ACTIVE` albo `COMPLETED` (D13) |
| `unit` | `STORY_POINTS` |
| `historyAvailable` | czy sprint ma zapisaną historię; gdy `false`, metryki mogą być `null` (D11) |
| `endedAt` | `null` dla sprintu aktywnego |
| `initialIssueCount` | liczba zadań w zakresie na starcie sprintu |
| `completedIssueCount`, `uncompletedIssueCount` | liczba zadań ukończonych i nieukończonych (D9) |
| `velocity`, `uncompletedPoints` | suma story points zadań ukończonych i nieukończonych |
| `timestamp` | chwila, której dotyczy punkt wykresu (D14) |
| `remainingPoints` | pozostała praca w story points |
| `completedPoints` | skumulowana praca ukończona w story points |
| `scopePoints` | całkowity zakres sprintu w danej chwili; `null`, gdy nie da się go wiarygodnie ustalić |

W CSV kolumna `projectID` pochodzi z `SprintComparisonDTO.projectID`.

Źródło estymacji w `velocity` i `uncompletedPoints` (D2) oraz warunek `historyAvailable` (D11) — przyjęte w implementacji (PR #174), do potwierdzenia przez zespół. Kontrakt DTO pozostaje propozycją, do potwierdzenia.

## 8. Sygnatury serwisów i wykonawcy

**[PROPOZYCJA]**

| Zadanie | Wykonawca | Interfejs i metoda |
|---|---|---|
| 06 (dane dla 07–11) | RomanFisher | `SprintEventReader.findBySprintIDs(Collection<Integer> sprintIDs): List<SprintIssueEvent>` |
| 07 (#103) | OleksandrPyrlyk | `SprintReportService.getSprintReport(Integer sprintID): SprintReportDTO` |
| 08 (#104) | OleksandrPyrlyk | `SprintMetricsService.calculate(List<SprintIssueEvent> events): SprintMetrics` |
| 09 (#105) | s101749-droid | `BurndownService.getBurndown(Integer sprintID): BurndownDTO` |
| 10 (#106) | s101749-droid | `BurnupService.getBurnup(Integer sprintID): BurnupDTO` |
| 11, 12 (#107, #108) | s101747-pl | `SprintComparisonService.compare(Integer projectID, SprintFilter filter): SprintComparisonDTO` |
| 13 (#109) | s101747-pl | `SprintCsvExportService.export(Integer projectID, SprintFilter filter): byte[]` |

- `SprintEventReader` zwraca zdarzenia rosnąco po czasie, jednym zapytaniem (WNF-5).
- `SprintIssueEvent`: `sprintID`, `issueID`, `type`, `estimation`, `occurredAt`. Dla `REMOVED` i `COMPLETED` estymacja pochodzi z wejścia do zakresu (D2).
- `SprintEventReader.findBySprintIDs` i pola `SprintIssueEvent` — przyjęte w implementacji (PR #174), do potwierdzenia przez zespół.
- `SprintMetrics`: `initialIssueCount`, `completedIssueCount`, `uncompletedIssueCount`, `velocity`, `uncompletedPoints`.
- Zestawienie pobiera zdarzenia wszystkich sprintów naraz, grupuje je i dopiero wtedy woła `SprintMetricsService`.
- Eksport CSV korzysta z wyniku `SprintComparisonService`, więc filtry są wspólne.
- Wyjątki dla 403, 404 i 409 pochodzą z zadania 03.
- Kontrolery nie zawierają obliczeń (WNF-2).
- Sygnatura `SprintMetricsService` zakłada liczenie ze zdarzeń (D1).

## 9. Ograniczenia i kwestie do ustalenia

### Ograniczenia

- Sprinty zakończone przed wdrożeniem zostały skasowane przez wcześniejsze `stopSprint`; nie da się ich odtworzyć.
- Sprzed wdrożenia nie ma historii zmian statusów ani zakresu. Sprint aktywny w chwili wdrożenia dziennika nie ma zdarzeń `COMMITTED`.
- Sprint zakończony po wdrożeniu PR #173, a przed wdrożeniem PR #174, nie zachowuje składu.
- Zmiana estymacji po wejściu zadania do zakresu nie wpływa na raport tego sprintu.
- Zadania z innego projektu lub o statusie `DONE`, dodane do sprintów przed PR #174, nie są poprawiane.
- Ujemna estymacja jest nadal przyjmowana i może dać ujemne sumy (D20).

### Kwestie do ustalenia i potwierdzenia

**[DO USTALENIA]** Przy kwestii otwartej podana jest propozycja, nie decyzja. Dopisek „przyjęte w implementacji” oznacza rozwiązanie z kodu, które zespół ma jeszcze potwierdzić.

| Nr | Kwestia | Propozycja lub przyjęte rozwiązanie |
|---|---|---|
| D1 | #104 liczy velocity z zadań przypisanych do sprintu w statusie `DONE`, a koncepcja z #100 opiera historię i skład sprintu na dzienniku. Wyniki mogą się różnić. | Liczyć z dziennika: po zakończeniu sprintu zadania są od niego odpięte. Samo obliczanie należy do zadania 08 (#104) — przyjęte w implementacji (PR #173 i #174), do potwierdzenia przez zespół. |
| D2 | Inna estymacja przy wejściu do zakresu i przy ukończeniu: pozostała praca nie spada do zera albo wychodzi ujemna. | Jedna wartość, z wejścia do zakresu: `REMOVED` i `COMPLETED` kopiują ją z ostatniego `COMMITTED` lub `ADDED`. Zmienia to ustalenie pomocnicze o wartości z chwili zdarzenia — przyjęte w implementacji (PR #174), do potwierdzenia przez zespół. |
| D3 | Usunięcie zadania `DONE` ze sprintu. | Zapisać `REMOVED`; przy liczeniu odjąć z zakresu i z pracy ukończonej — przyjęte w implementacji (PR #174), do potwierdzenia przez zespół. |
| D4 | Usunięcie zadania z systemu w trakcie sprintu (#102 nie wymienia `deleteIssue`). | `deleteIssue` zadania w aktywnym sprincie zapisuje `REMOVED`; dziennik przechowuje identyfikatory bez kluczy obcych — przyjęte w implementacji (PR #174), do potwierdzenia przez zespół. |
| D5 | Zmiany w zakończonym sprincie (zadania, edycja, kasowanie). | Ponowne uruchomienie i dodanie zadania są odrzucane, na razie z kodem 500 — przyjęte w implementacji (PR #173), do potwierdzenia przez zespół. Otwarte: edycja i usuwanie zakończonego sprintu oraz kod 409. |
| D6 | Zdarzenia przed startem sprintu. | Zdarzenia powstają tylko dla aktywnego sprintu; stan na starcie to `COMMITTED` — przyjęte w implementacji (PR #174), do potwierdzenia przez zespół. |
| D7 | Dodanie do sprintu zadania, które już jest `DONE` (API na to pozwala). | Do sprintu nie można dodać zadania o statusie `DONE` — przyjęte w implementacji (PR #174), do potwierdzenia przez zespół. |
| D8 | Czy zadania `DONE` zostają przypięte do zakończonego sprintu. | Zadania `DONE` są odpinane przy zakończeniu jak dotąd; skład sprintu wynika z dziennika — przyjęte w implementacji (PR #173 i #174), do potwierdzenia przez zespół. |
| D9 | Liczniki przy zmianach zakresu. | Początkowa = `COMMITTED`; ukończone i nieukończone z zakresu na końcu, bez usuniętych. |
| D10 | Reguła 2 w WF-3 mówi „początkowa minus ukończona”, a koncepcja „zakres w chwili t minus ukończona”. | Stosować koncepcję i to uzasadnić: przy stałym zakresie obie dają to samo. |
| D11 | Sprint bez historii (aktywny w chwili wdrożenia): które punkty są wiarygodne i co zwraca raport. | Historia jest dostępna, gdy sprint ma co najmniej jedno `COMMITTED` — przyjęte w implementacji (PR #174), do potwierdzenia przez zespół. Otwarte: co zwraca raport bez historii (propozycja: pusta lista punktów, metryki `null`). |
| D12 | Burndown i burnup sprintu nieuruchomionego. | 409, jak raport. |
| D13 | Wartości `status` i raport sprintu aktywnego. | `ACTIVE`, `COMPLETED`; raport aktywnego dozwolony. |
| D14 | Format punktów wykresu, bieżący dzień sprintu aktywnego, powtarzalność wyniku (WNF-4). | `timestamp`: start, końce dni, „teraz”; w testach wstrzykiwany zegar. |
| D15 | CSV: separator, nazwy nagłówków, nazwa pliku, puste daty, BOM. | Przecinek i CRLF (RFC 4180); nagłówki jak pola DTO; pusta wartość dla braku daty. Nazwa pliku i BOM bez propozycji. |
| D16 | Filtr dat: data czy data z godziną, granice, tylko `from` albo tylko `to`, sprinty bez `startedAt` i ich miejsce w kolejności. | Daty UTC, granice włącznie, oba parametry opcjonalne; sprinty bez `startedAt` poza filtrem i na końcu listy. |
| D17 | Uprawnienia do raportów. Członek dodany po e-mailu nie jest dziś rozpoznawany, bo liczy się login. | Uczestnicy projektu, inni 403. |
| D18 | Kto naprawia błędy bez przypisanego Issue: zadanie z innego projektu można dodać do sprintu (WNF-4); brak kontroli członkostwa w istniejących endpointach; usunięcie projektu, którego sprint ma zadania, kończy się błędem. | Zadanie z innego projektu: przejęte przez zadanie 06, dodanie jest odrzucane — przyjęte w implementacji (PR #174), do potwierdzenia przez zespół. Otwarte, bez propozycji: kontrola członkostwa i usuwanie projektu ze sprintem z zadaniami. |
| D19 | Nazwy pól: `sprintID` jak w kodzie czy `sprintId` jak w opisach #103–#109. | `sprintID`, `projectID`. |
| D20 | Ujemna estymacja jest dziś przyjmowana. | Odrzucać przy zapisie; wskazać zadanie. |
| D21 | Nazwy pól błędu i klasy wyjątków. | Uzgodnić z wykonawcą zadania 03. |

## 10. Powiązanie z wymaganiami

| Wymaganie | Sekcje | Do ustalenia lub potwierdzenia |
|---|---|---|
| WF-1 | 4.1, 6, 7, 8 | D9, D11, D13 |
| WF-2 | 2, 4.2, 8 | D1, D2, D7, D8 |
| WF-3 | 3, 4.3, 6, 7, 8 | D2, D3, D4, D10, D12, D14, D20 |
| WF-4 | 3, 4.4, 6, 7, 8 | D3, D4, D12, D14 |
| WF-5 | 5.1, 6, 7, 8 | D13, D19 |
| WF-6 | 5.2, 6, 7, 8 | D16 |
| WF-7 | 5.3, 6, 8 | D15 |
| WNF-4 | 3, 4.5, 9 | D5, D6, D14, D18, D20 |
| WNF-6 | 2, 5, 6, 7 | D17, D19, D21 |
