#!/usr/bin/env sh
. "$(dirname "$0")/rules.sh"

title="$1"
branch="$2"
body="$3"

"$(dirname "$0")/check-branch.sh" "$branch" || exit 1

printf '%s' "$title" | grep -Eq "$PR_TITLE_REGEX" || fail "Zły tytuł PR: '$title'
  Format: [<imie>] <typ>(#<id>): <opis>
  np.     [piotr] feat(#12): add sprint velocity endpoint"

title_name="$(printf '%s' "$title" | sed -E 's/^\[([a-z]+)\].*/\1/')"
title_id="$(printf '%s' "$title" | sed -E 's/^\[[a-z]+\] [a-z]+\(#([0-9]+)\).*/\1/')"
[ "$title_name" = "$(name_of_branch "$branch")" ] || fail "Imię w tytule PR ≠ imię z brancha."
[ "$title_id" = "$(task_id_of_branch "$branch")" ] || fail "ID w tytule PR ≠ ID z brancha."

printf '%s' "$body" | grep -Eiq "(closes|fixes|resolves) #$title_id([^0-9]|$)" || fail "Body PR musi zawierać 'Closes #$title_id'."
printf '%s\n%s' "$title" "$body" | grep -Eiq "$AI_ATTRIBUTION_REGEX" && fail "PR zawiera atrybucję AI. Usuń ją."

exit 0
