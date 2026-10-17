# Bienestar (Android)

Entry: Más → Empleado → Bienestar. Matches the iOS entry, horizontal navigation,
return-to-menu behavior, empty state, and Spanish/English/French interface text.

Configure approved services in `app/src/main/assets/wellbeing-services.json`:

```json
[
  {
    "id": "stable-service-id",
    "name": "Company-approved service name",
    "summary": "Optional description",
    "bookingURL": "https://company-approved-provider.example/book"
  }
]
```

The committed list is empty pending a real published booking URL. The general
Bookings homepage is not an employee booking link. Safari investigation found
LOGNEXT, but the current account could not open its management page; no service
or public booking URL was verified. Do not substitute personal meeting links.

Service entries require an ID, a name and an HTTPS URL without embedded
credentials. Duplicate IDs and malformed entries are ignored. Service content
is displayed verbatim; supply localized provider content as needed.

The provider page opens in Custom Tabs. The provider handles login, availability,
confirmation and any rescheduling/cancellation it supports. No new login scopes,
appointment records, medical information or browser credentials are introduced.
The native Graph slot picker needs a backend with application permissions for
getStaffAvailability; it is not implemented in either mobile app.

The global stale-cache banner is removed; cache updates remain active. The menu
uses Material's interactive drawer gestures for opening and closing, with a
transparent scrim. Android's native gesture can open from the content area,
whereas the iOS opening gesture begins at the left edge.
