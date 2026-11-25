#!/usr/bin/env sh
. "$(dirname "$0")/rules.sh"

branch="${1:-$(git rev-parse --abbrev-ref HEAD)}"

is_protected "$branch" && fail "Bezpośrednia praca na '$branch' zabroniona. Zrób branch i PR (AGENTS.md)."

printf '%s' "$branch" | grep -Eq "$BRANCH_REGEX" || fail "Zła nazwa brancha '$branch'.
  Format: <typ>/<imie>/<id-taska>/<krotki-opis>
  typ:  $TYPES
  imie: $NAMES
  np.   feat/piotr/12/sprint-velocity-endpoint"
