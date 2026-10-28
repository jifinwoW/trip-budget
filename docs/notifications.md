# Chilavu notifications

Who receives each alert, and when it is sent.

A person is “on the trip” when they have signed in and joined with the invite code, or they own the trip. Budget names on the Members tab are not accounts, so they do not get phone alerts by themselves.

The person who made a change does not get that change’s push. Custom messages and “Friend joined” are the exceptions below. Morning reminders stay on the phone that has the trip. They are not sent to other people.

Alerts show while Chilavu is closed, as long as the phone has opened the app once while signed in and notifications are allowed. Force-stopping Chilavu in Android settings blocks every alert until the app is opened again. While the app is on screen, only custom messages and “Friend joined” pop up. The rest wait until the app is in the background or closed.

---

## Custom message

| | |
| --- | --- |
| **When** | The owner sends one from Notify members. |
| **Title** | The title they typed. |
| **Body** | The message they typed. The trip name is not added. |
| **Who** | Every signed-in person on that trip, including the owner. |
| **On screen** | Yes. The owner sees it immediately. |

---

## Someone joins with the invite code

| | |
| --- | --- |
| **When** | A signed-in person joins a trip. Joining again with the same account does not send another alert. Creating the trip does not count. |
| **Title** | Friend joined |
| **Body** | The trip name. The person’s name and email are not included. |
| **Who** | The owner. The person who joined does not get it. |
| **On screen** | Yes. |

---

## Expenses

| | |
| --- | --- |
| **When** | An expense is added, edited, or deleted. |
| **Title** | New expense, Expense updated, or Expense removed |
| **Body** | Trip name, expense title, and amount. |
| **Who** | Everyone else on the trip. |
| **On screen** | No. It appears when the app is in the background or closed. |

---

## Contributions

| | |
| --- | --- |
| **When** | A payment toward someone’s share is added, edited, or deleted. |
| **Title** | New contribution, Contribution updated, or Contribution removed |
| **Body** | Trip name, note, and amount. |
| **Who** | Everyone else on the trip. |
| **On screen** | No. |

---

## Budget names

| | |
| --- | --- |
| **When** | A name is added on the Members tab. Editing or removing a name does not send an alert. This is not the invite-code join. |
| **Title** | New member |
| **Body** | “{name} joined {trip}”, or just the trip name if the name is blank. |
| **Who** | Everyone else on the trip. |
| **On screen** | No. |

---

## Over budget

| | |
| --- | --- |
| **When** | Spending crosses the planned budget. Later spending does not repeat it until the trip is back under budget and crosses again. |
| **Title** | Over budget |
| **Body** | “{trip} is {amount} over plan” |
| **Who** | Everyone else on the trip. It can arrive together with the expense alert that caused it. |
| **On screen** | No. |

---

## Money still owed

| | |
| --- | --- |
| **When** | A budget name’s unpaid amount goes up after a contribution or member change. |
| **Title** | Money still owed |
| **Body** | “{trip}: {name} owes {amount}”. Several names can be listed together. |
| **Who** | Everyone else on the trip. |
| **On screen** | No. |

---

## Stop reached

| | |
| --- | --- |
| **When** | Road-trip tracking is on, and a stop changes to reached. Adding, moving, or editing a stop does not send this. |
| **Title** | Stop reached |
| **Body** | “{trip}: {stop title}” |
| **Who** | Everyone else on the trip. |
| **On screen** | No. |

---

## Trip details changed

| | |
| --- | --- |
| **When** | The trip name or other trip details are saved, and another person’s app is already running. |
| **Title** | Trip updated |
| **Body** | The trip name. |
| **Who** | Other people on the trip whose app is open in the background. This one is not sent after the app has been closed. |
| **On screen** | No. |

---

## Weather

| | |
| --- | --- |
| **When** | The weather kind at the trip’s place changes. The first reading is saved quietly. A trip with no place is skipped. The check runs on the hour at minute 15, and also when a phone loads the trip header. |
| **Title** | Weather changed |
| **Body** | “{trip}: {previous} to {new label}” |
| **Who** | Everyone on the trip, including the owner. The phone copy is skipped while that phone has the app on screen. |
| **On screen** | No. |

---

## Morning reminders

These stay on each phone. They are not pushed to the rest of the trip.

| | |
| --- | --- |
| **When** | 8:00 local time. Once on the start date, then each later morning through the end date while a stop is still ahead. |
| **Start-day title** | Trip starts today |
| **Start-day body** | The trip name, plus “First stop: {title}” when a stop exists. |
| **Later title** | Next stop |
| **Later body** | “{trip}: {next stop}” |
| **Who** | That phone only. |
