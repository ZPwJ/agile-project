---
name: code-review
description: Code review PR-a lub diffu w tym repo — poprawność, czystość, SOLID, KISS, styl funkcyjny, brak komentarzy, testy BDD, konwencje brancha/commitów/PR, security. Użyj gdy ktoś prosi o review, przed otwarciem PR (self-review) lub przy sprawdzaniu cudzego PR.
---

# Code review

Reviewer końcowy: **@pr3civk**. AI robi pre-review; decyzja o merge należy do człowieka. AI nigdy nie merguje ani nie approve’uje PR.

## Wejście

- PR: `gh pr view <nr> --json title,body,headRefName,baseRefName,files,commits` + `gh pr diff <nr>`
- Lokalnie: `git diff origin/feature/I1S-7.7-g2...HEAD`

Czytaj pełne pliki wokół zmian, nie tylko diff.

## Checklista

### 1. Proces (blokujące)
- [ ] base = `feature/I1S-7.7-g2`
- [ ] branch `<typ>/<imie>/<id>/<opis>`, imię z listy `piotr|czarek|bartek|kacper|michal`
- [ ] tytuł `[<imie>] <typ>(#<id>): <opis>`, imię i ID = z brancha
- [ ] commity `<typ>(#<id>): <opis>`, ID = z brancha
- [ ] body zawiera `Closes #<id>`
- [ ] zero atrybucji AI (Co-Authored-By AI, „Generated with”, 🤖)
- [ ] PR dotyczy jednego taska; zmiany niezwiązane → do wydzielenia
- [ ] CI zielone

### 2. Poprawność (blokujące)
- logika zgodna z opisem taska / issue
- edge case’y: puste kolekcje, brak encji, duplikaty, granice dat, strefy czasowe
- transakcje (`@Transactional`) tam, gdzie wiele zapisów
- brak N+1 i zapytań w pętli
- `null` nie zwracany; `Optional` poprawnie użyty

### 3. Czystość / zasady (blokujące gdy rażące)
- SOLID: SRP (klasa/metoda robi jedno), OCP (brak `if` po typach), DIP (konstruktor, `private final`, brak `@Autowired` na polach)
- KISS / YAGNI: brak spekulatywnych abstrakcji, konfiguracji, martwego kodu
- styl funkcyjny: `record`, niemutowalność, Stream gdy czytelny, czyste funkcje, efekty na brzegu, `Clock` zamiast `now()`
- **brak komentarzy** w kodzie (każdy komentarz = finding z propozycją nazwy/metody)
- nazewnictwo wg `java-clean-code`
- warstwy: kontroler bez logiki, encje nie wyciekają do API
- duplikacja logiki

### 4. Testy (blokujące)
- nowa logika ma testy; bugfix ma test reprodukujący
- BDD: `should..When..` + `@DisplayName("given.., when.., then..")`, bloki given/when/then
- brak usuniętych / `@Disabled` testów bez uzasadnienia
- testy sprawdzają zachowanie, nie implementację

### 5. Security (blokujące)
- sekrety / tokeny / hasła w kodzie, configu, prompt-history
- brak autoryzacji na nowym endpoincie lub zbyt szerokie `permitAll`
- brak walidacji wejścia (`@Valid`)
- SQL/JPQL sklejany ze stringów
- logowanie danych wrażliwych
- wyciek stack trace / szczegółów w odpowiedzi błędu
- IDOR: dostęp do zasobu innego projektu/użytkownika bez sprawdzenia
- CSV/formula injection w eksportach
- nowe zależności: uzasadnione, aktualne, bez znanych CVE

## Raport

Po polsku, zwięźle. Każdy finding:

```
[BLOKUJĄCE|SUGESTIA] plik:linia — problem
  scenariusz: konkretne wejście → zły wynik
  fix: konkretna propozycja
```

Kolejność: blokujące → sugestie. Na końcu werdykt: `REQUEST CHANGES` / `OK DO REVIEW @pr3civk`.

Nie zgłaszaj: preferencji stylu bez reguły, zmian poza diffem (chyba że diff je psuje), rzeczy wyłapywanych przez kompilator/CI.

Publikacja komentarzy na PR (`gh pr review --comment`) tylko gdy użytkownik o to poprosi.
