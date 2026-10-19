# Bienestar (Android)

Entry: Más → Bienestar → Manicura y pedicura / Fisioterapia. Each drawer row opens its booking page directly in a Custom Tab, without an intermediate screen. Returning from the browser keeps the drawer available. Existing drawer gestures are preserved.

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

The committed list contains manicure/pedicure and physiotherapy, using the links
published under Servicios Corporativos on the Lognext intranet. Both pages were
opened in Safari on 2026-10-19 and showed their service/date/time controls. No
appointment was submitted. Availability, prices, duration and custom questions
remain in Bookings. Use the final bookings.cloud.microsoft/book service links
observed after redirection in Safari, preserving the physiotherapy service path.
The legacy Outlook links add a redirect; the general Bookings homepage is not
a booking link.

Service entries require an ID, a name and an HTTPS URL without embedded
credentials. Duplicate IDs and malformed entries are ignored. The two current services
are translated by service ID through Android resources (es/en/fr); future service
IDs fall back to configured text until their localized resources are added.

The provider page opens in Custom Tabs. The provider handles login, availability,
confirmation and any rescheduling/cancellation it supports. No new login scopes,
appointment records, medical information or browser credentials are introduced.
The native Graph slot picker needs a backend with application permissions for
getStaffAvailability; it is not implemented in either mobile app.

The global stale-cache banner is removed; cache updates remain active. The menu
uses Material's interactive drawer gestures for opening and closing, with a
transparent scrim. Android's native gesture can open from the content area,
whereas the iOS opening gesture begins at the left edge.

Verification does not include submitting, changing or cancelling a real booking.
Microsoft may require login on a device; no new Graph permissions or login
behavior were introduced.
