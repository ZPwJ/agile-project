-- Dane demonstracyjne modułu analitycznego (profil `demo`, #9).
-- Stan bazy jest taki, jaki zostawiłaby aplikacja po przeprowadzeniu dwóch sprintów przez API:
-- zakończony sprint ma isDone = true, a jego zadania są od niego odpięte (sprintid = NULL),
-- więc raporty liczone są z historii w sprint_issue_event.
-- Identyfikatory od 9001, żeby nie kolidowały z rekordami tworzonymi przez aplikację z sekwencji.
-- Każdy INSERT ma ON CONFLICT DO NOTHING, bo skrypt wykonuje się przy każdym starcie aplikacji.
-- Oczekiwane wyniki są opisane w docs/dokumentacja.md, sekcja „Dane demonstracyjne”.

INSERT INTO backlog(backlogid) VALUES (9001) ON CONFLICT DO NOTHING;

INSERT INTO project(projectid, name, description, project_owner, project_goal, backlogid)
VALUES (9001, 'Sklep internetowy (demo)', 'Projekt z danymi demonstracyjnymi raportów sprintów', 'demo',
        'Pierwsza wersja sklepu', 9001) ON CONFLICT DO NOTHING;

INSERT INTO sprint(sprintid, name, sprint_goal, projectid, start_date, started_at, ended_at, active, is_done)
VALUES (9001, 'Sprint 1', 'Konta użytkowników i koszyk', 9001,
        '2026-10-05 08:00:00+00', '2026-10-05 08:00:00+00', '2026-10-09 15:00:00+00', FALSE, TRUE) ON CONFLICT DO NOTHING;
INSERT INTO sprint(sprintid, name, sprint_goal, projectid, start_date, started_at, ended_at, active, is_done)
VALUES (9002, 'Sprint 2', 'Płatności i zamówienia', 9001,
        '2026-10-12 08:00:00+00', '2026-10-12 08:00:00+00', '2026-10-16 15:00:00+00', FALSE, TRUE) ON CONFLICT DO NOTHING;

-- Statusy: 1 TO_DO, 5 DONE. Typy: 1 USER_STORY, 2 BUG. Priorytet: 3 MEDIUM.
-- Zadania 9004 i 9008 nie mają estymacji.
INSERT INTO issue(issueid, name, estimation, projectid, sprintid, issue_statusid, issue_typeid, issue_priorityid) VALUES
    (9001, 'Logowanie',                 5,    9001, NULL, 5, 1, 3),
    (9002, 'Rejestracja konta',         3,    9001, NULL, 5, 1, 3),
    (9003, 'Koszyk',                    8,    9001, NULL, 5, 1, 3),
    (9004, 'Literówka na stronie głównej', NULL, 9001, NULL, 5, 2, 3),
    (9005, 'Płatności online',          5,    9001, NULL, 5, 1, 3),
    (9006, 'Filtr produktów',           2,    9001, NULL, 5, 1, 3),
    (9007, 'Historia zamówień',         8,    9001, NULL, 1, 1, 3),
    (9008, 'Eksport faktur',            NULL, 9001, NULL, 5, 1, 3),
    (9009, 'Powiadomienia e-mail',      3,    9001, NULL, 1, 1, 3),
    (9010, 'Błąd przeliczania koszyka', 2,    9001, NULL, 5, 2, 3)
ON CONFLICT DO NOTHING;

-- Sprint 1: zakres początkowy 5 zadań, 18 SP (9004 bez estymacji).
-- Zmiana zakresu: 9006 dodane 6 X, estymacja 9003 zmieniona z 5 na 8 dnia 7 X.
-- 9005 nieukończone — wraca do TO_DO i przechodzi do sprintu 2.
INSERT INTO sprint_issue_event(id, sprintid, issueid, type, estimation, occurred_at) VALUES
    (9001, 9001, 9001, 'SCOPE_AT_START',     5,    '2026-10-05 08:00:00+00'),
    (9002, 9001, 9002, 'SCOPE_AT_START',     3,    '2026-10-05 08:00:00+00'),
    (9003, 9001, 9003, 'SCOPE_AT_START',     5,    '2026-10-05 08:00:00+00'),
    (9004, 9001, 9004, 'SCOPE_AT_START',     NULL, '2026-10-05 08:00:00+00'),
    (9005, 9001, 9005, 'SCOPE_AT_START',     5,    '2026-10-05 08:00:00+00'),
    (9006, 9001, 9006, 'ADDED',              2,    '2026-10-06 09:00:00+00'),
    (9007, 9001, 9001, 'COMPLETED',          5,    '2026-10-06 14:00:00+00'),
    (9008, 9001, 9003, 'ESTIMATION_CHANGED', 8,    '2026-10-07 10:00:00+00'),
    (9009, 9001, 9004, 'COMPLETED',          NULL, '2026-10-07 12:00:00+00'),
    (9010, 9001, 9002, 'COMPLETED',          3,    '2026-10-08 11:00:00+00'),
    (9011, 9001, 9006, 'COMPLETED',          2,    '2026-10-08 15:00:00+00'),
    (9012, 9001, 9003, 'COMPLETED',          8,    '2026-10-09 13:00:00+00')
ON CONFLICT DO NOTHING;

-- Sprint 2: zakres początkowy 4 zadania, 16 SP (9008 bez estymacji).
-- Zmiana zakresu: 9009 usunięte 13 X, 9010 (błąd) dodane 14 X. 9007 nieukończone.
INSERT INTO sprint_issue_event(id, sprintid, issueid, type, estimation, occurred_at) VALUES
    (9013, 9002, 9005, 'SCOPE_AT_START', 5,    '2026-10-12 08:00:00+00'),
    (9014, 9002, 9007, 'SCOPE_AT_START', 8,    '2026-10-12 08:00:00+00'),
    (9015, 9002, 9008, 'SCOPE_AT_START', NULL, '2026-10-12 08:00:00+00'),
    (9016, 9002, 9009, 'SCOPE_AT_START', 3,    '2026-10-12 08:00:00+00'),
    (9017, 9002, 9009, 'REMOVED',        3,    '2026-10-13 09:00:00+00'),
    (9018, 9002, 9010, 'ADDED',          2,    '2026-10-14 08:30:00+00'),
    (9019, 9002, 9005, 'COMPLETED',      5,    '2026-10-14 16:00:00+00'),
    (9020, 9002, 9010, 'COMPLETED',      2,    '2026-10-15 10:00:00+00'),
    (9021, 9002, 9008, 'COMPLETED',      NULL, '2026-10-16 12:00:00+00')
ON CONFLICT DO NOTHING;

COMMIT;
