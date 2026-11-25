# prompt-history

Historia promptów do AI. Struktura: `<gh-login>/<YYYY-MM-DD>.md`, wpis: `## HH:MM · <agent> · <branch> · <sesja>`.

- Claude Code: zapis automatyczny (hook `UserPromptSubmit`, `.claude/settings.json`).
- Inne agenty: `printf '%s' "<prompt>" | node scripts/log-prompt.mjs <agent>`.
- Login: `GH_LOGIN` → `git config github.user` → `gh api user` (cache w `.git/gh-login`).
- Logger pomija komunikaty systemowe (`<system-reminder>`, `<task-notification>`) i maskuje sekrety (`[REDACTED]`): klucze, tokeny, JWT, `password=`, hasła w URL.
- Commitujemy razem z taskiem. Przejrzyj diff przed commitem — maskowanie nie łapie wszystkiego.
