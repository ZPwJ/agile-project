#!/usr/bin/env sh
. "$(dirname "$0")/rules.sh"

leaks="$(git diff --cached -U0 --no-color | grep -E '^\+[^+]' | grep -Eo -- "$SECRET_REGEX")"

[ -z "$leaks" ] || fail "Wykryto możliwy sekret w staged zmianach:
$leaks
Usuń go (też z prompt-history) i użyj zmiennej środowiskowej."
