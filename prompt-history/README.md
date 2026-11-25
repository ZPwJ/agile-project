# prompt-history

Historia promptów do AI. Struktura: `<gh-login>/<YYYY-MM-DD>.md`, wpis: `## HH:MM · <agent> · <branch> · <sesja>`.

- Claude Code: zapis automatyczny (hook `UserPromptSubmit`, `.claude/settings.json`).
- Inne agenty: `printf '%s' "<prompt>" | node scripts/log-prompt.mjs <agent>`.
- Login: `GH_LOGIN` → `git config github.user` → `gh api user` (cache w `.git/gh-login`).
- Commitujemy razem z taskiem. Bez sekretów w promptach.
