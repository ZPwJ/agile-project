---
name: project-task
description: Obsługa taska z GitHub Project 13 (ZPwJ) — dodanie taska, wzięcie do realizacji (człowiek lub AI), branch, commity, PR, statusy. Użyj gdy user mówi „zrób taska”, „weź taska”, „dodaj taska”, podaje numer issue, pyta o swoje taski lub kończy pracę.
---

# Project task

Tablica: https://github.com/orgs/ZPwJ/projects/13 (owner `ZPwJ`, nr `13`). Repo: `ZPwJ/agile-project`. Base: `feature/I1S-7.7-g2`.

Task = issue w repo dodane do projektu. ID taska = numer issue.

## Dodanie taska

```bash
gh issue create -R ZPwJ/agile-project --title "<tytuł>" --body "<opis + kryteria akceptacji w BDD>" --assignee <gh-login>
gh project item-add 13 --owner ZPwJ --url <issue-url>
```

Kryteria akceptacji w body jako scenariusze:

```
Scenario: <nazwa>
  Given <stan>
  When <akcja>
  Then <wynik>
```

Ustaw `Executor` (`Human`/`AI`) i `Status = Todo` (patrz „Pola” niżej).

## Lista tasków

```bash
gh project item-list 13 --owner ZPwJ --format json -L 200
```

AI do realizacji bierze tylko item z `Status = Todo`, `Executor = AI` i assignee. Brak takich → zapytaj usera.

## Start pracy

1. `gh issue view <id> -R ZPwJ/agile-project` — przeczytaj opis i kryteria.
2. Ustal imię osoby odpowiedzialnej (`piotr|czarek|bartek|kacper|michal`). Nie wiesz → zapytaj.
3. Status → `In Progress`.
4. Branch:
   ```bash
   git fetch origin
   git switch -c <typ>/<imie>/<id>/<opis> origin/feature/I1S-7.7-g2
   ```
5. Implementacja wg skilla `java-clean-code` (test BDD najpierw).

## Commity

`<typ>(#<id>): <opis>`, bez atrybucji AI. Hooki sprawdzą format.

## Koniec pracy

1. `make verify` zielone.
2. Self-review skillem `code-review`, popraw blokujące.
3. Push: `git push -u origin HEAD` (pre-push odpali build + testy).
4. PR:
   ```bash
   gh pr create -R ZPwJ/agile-project --base feature/I1S-7.7-g2 \
     --title "[<imie>] <typ>(#<id>): <opis>" \
     --body-file <plik z wypełnionym .github/pull_request_template.md, zawiera "Closes #<id>">
   gh project item-add 13 --owner ZPwJ --url <pr-url>
   ```
5. Reviewer: @pr3civk (CODEOWNERS dodaje automatycznie).
6. **Nie merguj.** `Done` ustawia się po merge (automatyzacja projektu lub ręcznie).

## Pola projektu

ID pól i opcji pobierz raz:

```bash
gh project view 13 --owner ZPwJ --format json --jq .id
gh project field-list 13 --owner ZPwJ --format json
```

Zmiana pola single-select:

```bash
gh project item-edit --project-id <project-id> --id <item-id> \
  --field-id <field-id> --single-select-option-id <option-id>
```

`Status`: `Todo` | `In Progress` | `Done`. `Executor`: `Human` | `AI`.

## Zasady

- 1 task = 1 branch = 1 PR.
- Pushe i PR tylko gdy user o to prosi lub task tego wprost wymaga.
- Nie zamykaj issue, nie zmieniaj cudzych tasków, nie merguj.
