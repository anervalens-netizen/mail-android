# Mail 1.3.0 — search and POP3 download correction

## Root causes

- Mail 1.2 deliberately searched only the sender. Queries found in the subject,
  message body or recipients could not match. Restore broad local search while
  retaining targeted sender-only syntax via `from:`.
- The inherited Thunderbird default auto-download threshold was 128 KiB. Larger
  POP3 messages remained partial, so attachments required a second download.
  When another client deleted them from the POP3 server first, retry failed.

## New behavior

- General local search: From, To, Cc, Bcc, Subject, downloaded body content.
- Explicit `from:` search: only sender names and addresses.
- New accounts: 10 MiB auto-download threshold.
- Existing POP3 accounts at the former 128 KiB default: migrate once to 10 MiB
  and persist. Leave custom limits and non-POP3 accounts intact.
- No deletion, local-message rewrite, account reset, secret change or IMAP
  behavior change intended.
- Historic partial messages already removed from POP3 cannot be recovered by
  this change. The original RCA document remains in the separate work PST
  archive, not in the current POP3 mailbox.

## Verification

Build, test and on-device evidence pending.
