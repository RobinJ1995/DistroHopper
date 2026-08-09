# Privacy Policy — DistroHopper

_Last updated: 2026-08-09_

DistroHopper (`be.robinj.distrohopper`) is a free, open-source Android home
screen. It contains **no advertising, no analytics, and no tracking SDKs**, and
does **no third-party data collection**. It is built on open-source libraries
(ACRA among them), but none of them collect or transmit your data.

## Who is responsible

The data controller is:

- **Name:** Robin Jacobs
- **Contact:** robin@robinj.be

I run the crash-report server (`acra.robinj.be`) myself, on infrastructure I
operate in Ireland. No analytics provider, advertising network or other company
receives your data, and it never leaves the EEA.

## Crash reports

If the app crashes or hits a handled error, it sends a crash report to that
server. This is **on by default**; you can turn it off at any time under
**Settings → Advanced → "Send crash reports"**. The separate `-paranoia` build
has crash reporting compiled out entirely.

A crash report contains:

- The app and version that crashed
- Your Android version, and your device's model, brand and variant code
- Screen specifications and approximate total/free memory
- The technical error details (stack trace) and the name of the failing thread
- DistroHopper's own recent log output (the last 100 lines its process wrote),
  which records what the app was doing before it failed
- Whether it was a full crash or a handled, non-fatal error
- Screen orientation and language at the moment of the crash
- Your DistroHopper settings, including which icon pack and which search sources
  you have enabled
- When the app started and when the error happened
- A random per-report identifier and a random installation identifier

The log is DistroHopper's own output only — not your device's system log, and not
any other app's. It records what the app was doing: loading and sorting your
apps, drawing the home screen, and errors it recovered from. It is written not to
name the apps you have installed.

Because a crash report is a technical snapshot, the error details can
occasionally mention a specific app, icon pack or file that was involved in the
failure. This is incidental to diagnosing the fault, not an attempt to record
what you use.

A crash report does **not** contain your files, messages, contacts, location,
your device's system log, accounts, advertising ID, any hardware or SIM
identifier, your searches, **the apps on your home screen, your home-screen
layout, or your app-usage history**.

**Installation identifier:** a random value created when you install the app. It
is used only to tell whether several reports came from the same installation
(one device crashing repeatedly vs. many people hitting one bug). It is not
linked to your identity and is reset if you reinstall.

**Legal basis:** legitimate interest (Art. 6(1)(f) GDPR) — diagnosing and fixing
crashes so the app works. No profiling, no sale, no sharing.

**Retention:** a crash report is deleted 180 days after it arrives. While a fault
is still being reported, a summary of it is kept — which error it is, in which
app version, and how many installations hit it — but that summary holds counts
and technical detail, never the installation identifier. Once the last report of
a fault has aged out, the summary is deleted with it.

## Search

Out of the box, search only looks at your installed apps, entirely on your
device. Searching the files on your device is optional: you turn it on yourself
and choose which folders it may look in, and it too runs entirely on your
device. Neither sends anything anywhere.

You can also enable remote search sources — currently DuckDuckGo, F-Droid,
GitHub and Google Play; the full list is in the app's search settings. If you
do, **what you type in the search box is sent to that provider** so it can
return results, and their own privacy policy applies
([DuckDuckGo](https://duckduckgo.com/privacy),
[F-Droid](https://f-droid.org/docs/Privacy_Policy/),
[GitHub](https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement),
[Google](https://policies.google.com/privacy)). DistroHopper never stores your
searches and never receives them itself.

## Accessibility Service

An optional gesture can open your notification tray. Because that is the only way
a home screen can do it, this uses an Accessibility Service that you must switch
on yourself in Android's settings. It is off unless you enable it, and it does
one thing: open the notification tray. It observes no events and reads no screen
content.

## What stays on your device

Your installed-app list, home-screen layout, folders, widgets and app launch
counts are stored only on your device.

DistroHopper asks for **no storage or media permissions at all**. If you use the
file search, you pick each folder yourself through Android's own folder picker,
and the app can read only the folders you chose — nothing else on your device.
You can withdraw a folder at any time in the Local files settings. Searching
those folders happens entirely on your device; nothing about your files is sent
anywhere.

## Children

DistroHopper is not directed at children and does not knowingly collect data
from them.

## Your rights

You can ask me to give you a copy of any data relating to you, correct it, delete
it, or restrict how it is used. Because crash reporting relies on legitimate
interest, you also have the **right to object** to it — and you can exercise that
yourself at any time, immediately, by turning off "Send crash reports" in
Settings → Advanced.

Email the contact address above for any of these. In practice the only identifier
that could link reports to you is the random installation identifier, so please
include it if you can — otherwise I may not be able to find your reports.

If you are unhappy with how I handle your data, you can complain to the Irish
Data Protection Commission ([dataprotection.ie](https://www.dataprotection.ie)),
or to the supervisory authority where you live.

## Changes

If this policy changes, the updated version will be published here with a new
date.
