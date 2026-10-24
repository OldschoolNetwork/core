# Changelog

## 0.56 — 2026-10-24

Core 0.56 improves account management on classic Android.

- Saved accounts are matched by ON ID. Legacy email labels and duplicate nicknames are merged without discarding saved credentials.
- The system authenticator uses Android's account-manager permission. Built-in Settings handlers can see the account while unrelated apps do not receive blanket access.
- Active sessions, account preferences and support requests are available through native screens backed by the account API.
- Profile and avatar changes report success after the server accepts them. Password changes are supported in Manage Profile.
- Signing out removes the local account and requests termination of its server session.
- Onboarding Help opens the Account documentation category.

Android 4.0 / API 14 and later. Universal APK, signed with the persistent ON certificate. Eight account, onboarding and dashboard checks passed on an isolated Android 5.1 emulator. Xiaomi firmware behavior still needs physical-device verification.

## 0.55 — 2026-10-19

Core 0.55 fixes a crash after password entry. Both sign-in and registration now focus the native verification button instead of the removed verification WebView. Built successfully and signed with the persistent ON key. Physical Mi 5 retesting is still needed.

## 0.54 — 2026-10-19

Core 0.54 adds automatic service installation after full setup.

- Full system installations can download and silently install updates for Core and Store. Store is updated first, Core last.
- Installation state survives replacement of Core's own process. Android confirms the installed version and ON certificate before the update is marked complete.
- Automatic checks and automatic download/install have separate switches. Disabling automatic updates stops the background download and prevents the next installation; manual downloads remain available.
- Ordinary installations keep the Download and Install buttons and Android confirmation. If privileged installation needs confirmation, the verified APK stays available for manual installation.
- The Services screen now reports the actual full or ordinary installation status.

Android 4.0 / API 14 or newer; universal APK. Signed with the persistent ON key. On an isolated Android 5.1 emulator, privileged Core downloaded and updated Store without Android's installer UI, then successfully replaced itself and recovered the saved result. Root fallback and physical ROM behaviour still need device testing.

## 0.53 — 2026-10-19

Core 0.53 fixes the Store download and onboarding flows.

- Download progress and errors are visible inside Core as well as in notifications. A verified download leaves an Install button on screen, including after reopening the page.
- Fixed APK staging before package verification; downloads keep the device awake while running.
- Added a shortcut to notification settings when download notifications are disabled.
- Opening the Store entry when Store is missing now opens the download screen.
- Onboarding keeps its current step when the screen rotates. A delayed initial network check no longer returns it to the first screen.
- Replaced the remote verification WebView with a native screen. This alpha does not send email codes; the screen explicitly explains that Send continues the flow.
- Saved account selection uses server-issued tokens and private account data, without recreating email-labelled public system accounts.

Android 4.0 / API 14 or newer; universal APK. Signed with the same persistent ON key as 0.52. Core download, notification, account and rotation scenarios were checked on Android 5.1. Root installation still needs physical-device testing.

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
