#!/usr/bin/env sh

BASE_BRANCH="feature/I1S-7.7-g2"
PROTECTED_BRANCHES="main $BASE_BRANCH"
TYPES="feat|fix|refactor|test|docs|chore|ci|perf|build"
NAMES="piotr|czarek|bartek|kacper|michal"

BRANCH_REGEX="^($TYPES)/($NAMES)/[0-9]+/[a-z0-9]+(-[a-z0-9]+)*$"
COMMIT_REGEX="^($TYPES)\(#[0-9]+\): [a-z0-9].{0,70}[^.]$"
PR_TITLE_REGEX="^\[($NAMES)\] ($TYPES)\(#[0-9]+\): .+[^.]$"
AI_ATTRIBUTION_REGEX="co-authored-by:.*(claude|anthropic|codex|openai|chatgpt|gpt|copilot|cursor|gemini|noreply@anthropic)|generated (with|by) .*(claude|codex|ai|copilot|chatgpt)|🤖"
SECRET_REGEX="-----BEGIN [A-Z ]*PRIVATE KEY-----|AKIA[0-9A-Z]{16}|gh[pousr]_[A-Za-z0-9]{36}|github_pat_[A-Za-z0-9_]{40,}|sk-[A-Za-z0-9_-]{20,}|xox[baprs]-[A-Za-z0-9-]{10,}"

fail() {
  printf '\033[31m✖ %s\033[0m\n' "$1" >&2
  exit 1
}

is_protected() {
  for protected in $PROTECTED_BRANCHES; do
    [ "$1" = "$protected" ] && return 0
  done
  return 1
}

task_id_of_branch() {
  printf '%s' "$1" | cut -d/ -f3
}

name_of_branch() {
  printf '%s' "$1" | cut -d/ -f2
}
