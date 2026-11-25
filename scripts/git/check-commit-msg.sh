#!/usr/bin/env sh
. "$(dirname "$0")/rules.sh"

message="$(grep -v '^#' "$1")"
branch="${2:-$(git rev-parse --abbrev-ref HEAD)}"
subject="$(printf '%s\n' "$message" | head -n 1)"

printf '%s' "$subject" | grep -Eq '^(Merge|Revert) ' && exit 0

printf '%s' "$message" | grep -Eiq "$AI_ATTRIBUTION_REGEX" && fail "Commit zawiera atrybucję AI (Co-Authored-By / Generated with / 🤖). Usuń ją."

printf '%s' "$subject" | grep -Eq "$COMMIT_REGEX" || fail "Zły format commita: '$subject'
  Format: <typ>(#<id>): <opis>   (małą literą, bez kropki, max 72 znaki)
  np.     feat(#12): add sprint velocity endpoint"

commit_id="$(printf '%s' "$subject" | sed -E 's/^[a-z]+\(#([0-9]+)\).*/\1/')"
branch_id="$(task_id_of_branch "$branch")"
[ "$commit_id" = "$branch_id" ] || fail "ID w commicie (#$commit_id) ≠ ID z brancha (#$branch_id)."
