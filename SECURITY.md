# Security policy

## Reporting a vulnerability

Please report security or privacy issues privately to the project maintainers rather than opening a public issue with exploit details. Include:

- affected version/commit;
- Android version and device/OEM;
- reproduction steps;
- impact, especially any way to weaken the privacy invariants below.

## Privacy invariants

Changes must not introduce:

- the `INTERNET` or `ACCESS_NETWORK_STATE` permission;
- cloud inference or model downloads;
- persistence or transmission of camera frames, face templates, or detection history;
- `SYSTEM_ALERT_WINDOW`, accessibility-service screen access, or MediaProjection screen capture;
- implicit camera activation after process/service restart.

## Scope

This app is a heuristic deterrent, not a guarantee against shoulder surfing. False negatives and false positives are expected limitations, not vulnerabilities by themselves. Report vulnerabilities involving permission bypass, unauthorized camera use, notification spoofing, unintended data retention, or fragile service behavior.

## Supported versions

Security fixes target the latest release line and the `main` branch.
