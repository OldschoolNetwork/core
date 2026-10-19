# ON Core Services

The account and shared service layer for Oldschool Network on classic Android devices.

## Download

Current build: **alpha 0.55**, version code **10**.

- [Download ON Core Services](https://github.com/OldschoolNetwork/core/releases/download/alpha-0.55/ON-Core-Services-0.55.apk)
- [Release notes and checksums](https://github.com/OldschoolNetwork/core/releases/tag/alpha-0.55)
- [All ON downloads](https://oldschoolnetwork.online/downloads/)

Android **4.0 / API 14 or newer**. This is a universal APK. It uses the persistent Oldschool Network signing key.

## What it does

Core manages ON Account sign-in, onboarding and Android system accounts. It supplies the account interface used by ON Store and checks published updates for Core and Store. Scheduled update checks can be turned off. Full system installations can download and install service updates automatically; this has a separate switch.

If you install Core on its own, use its Updates screen to get Store once a compatible Store release is published in the ON catalog.

## Alpha status

These builds are under development. Android 5.1 checks have passed, but that does not establish compatibility with every device or ROM. Full root installation, Google component removal, Magisk overlays and silent updates still need testing on physical devices. Keep a backup and recovery access before using system installation.

The ON release key differs from the old Android debug key. Android cannot update those old development packages in place; plan a one-time migration and keep any data you need.

## Help and problems

- [Oldschool Network](https://oldschoolnetwork.online/)
- [Documentation](https://oldschoolnetwork.online/downloads/docs/)
- [Changelog](CHANGELOG.md)

Include your device model, Android version, application version and the steps that led to a problem. Remove account tokens and private information from logs before posting them.
