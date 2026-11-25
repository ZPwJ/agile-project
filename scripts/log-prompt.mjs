#!/usr/bin/env node
import { execFileSync } from "node:child_process";
import { appendFileSync, existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const run = (...command) => {
  try {
    return execFileSync(command[0], command.slice(1), { encoding: "utf8", stdio: ["ignore", "pipe", "ignore"], timeout: 5000 }).trim();
  } catch {
    return "";
  }
};

const repoRoot = run("git", "rev-parse", "--show-toplevel") || process.cwd();
const loginCache = join(run("git", "rev-parse", "--git-dir") || ".git", "gh-login");

const cachedLogin = () => (existsSync(loginCache) ? readFileSync(loginCache, "utf8").trim() : "");

const fetchLogin = () => {
  const login = run("gh", "api", "user", "--jq", ".login");
  if (login) writeFileSync(loginCache, login);
  return login;
};

const sanitize = (value) => value.toLowerCase().replace(/[^a-z0-9-]/g, "-");

const githubLogin = () =>
  sanitize(process.env.GH_LOGIN || run("git", "config", "--get", "github.user") || cachedLogin() || fetchLogin() || run("git", "config", "--get", "user.name") || "unknown");

const parseInput = (raw, agent) => {
  try {
    const json = JSON.parse(raw);
    return { prompt: json.prompt ?? "", agent, session: json.session_id ?? "" };
  } catch {
    return { prompt: raw, agent, session: "" };
  }
};

const SYSTEM_BLOCKS = /<(system-reminder|task-notification)>[\s\S]*?<\/\1>/g;

const SECRET_PATTERNS = [
  [/-----BEGIN [A-Z ]*PRIVATE KEY-----[\s\S]*?-----END [A-Z ]*PRIVATE KEY-----/g, "[REDACTED]"],
  [/eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}/g, "[REDACTED]"],
  [/\b(AKIA[0-9A-Z]{16}|gh[pousr]_[A-Za-z0-9]{36}|github_pat_[A-Za-z0-9_]{40,}|sk-[A-Za-z0-9_-]{20,}|xox[baprs]-[A-Za-z0-9-]{10,})\b/g, "[REDACTED]"],
  [/(:\/\/[^\s:/@]+:)[^\s@]+@/g, "$1[REDACTED]@"],
  [/\b(password|passwd|pwd|secret|token|api[_-]?key|client[_-]?secret)(\s*[:=]\s*)\S+/gi, "$1$2[REDACTED]"],
];

const stripSystemBlocks = (prompt) => prompt.replace(SYSTEM_BLOCKS, "").trim();

const redact = (prompt) => SECRET_PATTERNS.reduce((text, [pattern, replacement]) => text.replace(pattern, replacement), prompt);

const pad = (number) => String(number).padStart(2, "0");

const entry = ({ prompt, agent, session }, now) => {
  const time = `${pad(now.getHours())}:${pad(now.getMinutes())}`;
  const sessionTag = session ? ` · ${session.slice(0, 8)}` : "";
  return `## ${time} · ${agent} · ${run("git", "rev-parse", "--abbrev-ref", "HEAD")}${sessionTag}\n\n${prompt}\n\n`;
};

const save = (rawInput) => {
  const input = { ...rawInput, prompt: redact(stripSystemBlocks(rawInput.prompt)) };
  if (!input.prompt) return;
  const now = new Date();
  const directory = join(repoRoot, "prompt-history", githubLogin());
  const date = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
  mkdirSync(directory, { recursive: true });
  appendFileSync(join(directory, `${date}.md`), entry(input, now));
};

try {
  save(parseInput(readFileSync(0, "utf8"), process.argv[2] || "claude"));
} catch {
  process.exit(0);
}
