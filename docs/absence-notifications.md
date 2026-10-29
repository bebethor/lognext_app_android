# Android absence notification pilot

Branch: `feature/android-absence-notifications`.

The backend admits only the authenticated Microsoft accounts configured for Android:
`jose.ruiz@lognext.com` and `enrique.martinez@lognext.com`. The app does not decide
eligibility from an editable email field. iOS remains a separate APNs pilot.

Firebase project: `nexter-4694c`, Android package `com.lognext.nexterandroid`.
`app/google-services.json` contains public client configuration, not the server credential.
Only Firebase Messaging is included; Analytics is not enabled. Automatic token creation is
disabled; the app explicitly obtains a token after the user opts in. No private service
account key belongs in this repository or APK.

## Testing

1. Build/install this branch (`./gradlew :app:assembleDebug`) on an Android device with
   Google Play services, then sign in with one of the pilot accounts.
2. Enter Ausencias. Below the vacation balance, enable Avisos de ausencias and grant
   the Android notification permission. The switch should remain on.
3. Agree a new test vacation request with the approver. Approve or reject it in Cezanne.
   Existing history is captured quietly on registration and does not generate alerts.
4. Check the exact approval/rejection wording, in foreground and with the app in background.
   Tapping the notification should open and refresh Ausencias.
5. Disable the switch and repeat a decision: this installation must not display it.
   Sign out and check that a late message from the previous session is ignored.

Safe Logcat tag: `AbsencePush`. It records registration, status, suppressed messages,
notification display and navigation, without tokens, account names or HR content.
Server API/worker logs use `nexter.absence`, with `absence.fcm` delivery results.

The app retries transient IO failures twice and renews its 30-day lease once per launch
and at most every 24 hours thereafter, or when the token/account changes. Logout suppresses
notifications locally, revokes before MSAL signs out, deletes the FCM token, and remembers
failed revocation for the same account's next session. Local opt-in/installation data is
excluded from backup and transfer. A matching subscription and opt-in are checked before
display and navigation; delivery IDs suppress duplicate displays.

FCM data messages are high-priority with a one-hour TTL. They contain only subscription,
delivery UUID, kind and outcome. The client localizes the text and posts a private
notification in the `absence_status` channel. Force-stop in Android settings prevents
FCM delivery until the app is opened again. System notification settings can also block
presentation. Cancellation and other absence categories do not produce alerts.

Checks: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
