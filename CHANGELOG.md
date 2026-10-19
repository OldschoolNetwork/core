# Changelog

## 0.52 — 2026-10-19

ON Core Services 0.52 adds system account integration and service updates.

- ON Account appears in Android Settings → Accounts → Add account and opens the onboarding screen.
- Existing email-labelled system accounts migrate to public nicknames while retaining their account credentials.
- Core checks published Core and Store updates. Scheduled checks can be disabled; the Updates screen also offers Store when it is missing.
- An offline notice identifies when Core is using saved account data.
- ON services use server-issued account tokens. Private account fields are restricted in the Android service interface.
- Registration uses the revised platform rules and consent version.

Requires Android 4.0 (API 14) or newer. This APK is universal and signed with the persistent ON key. System account integration was tested on Android 5.1. Full root installation and ROM-specific behaviour still need device testing.

An installation signed with the old Android debug key cannot be updated in place with this key. Back up the data you need before migrating.
