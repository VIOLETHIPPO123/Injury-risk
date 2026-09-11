# Requirements

## User Personas

### Persona 1: Marcus Chen — The Competitive Dynasty Manager

**Background:** 34, software engineer, has run the same 10-team keeper/dynasty fantasy football league with the same friend group for 7 years. Treats it like a part-time job — spends hours weekly on waiver research and trade analysis.

**Goals:**

1. Wants to identify which of his roster players carry hidden long-term injury risk before deciding who to keep for next season, since a wrong keeper pick sets him back a full year.
2. Wants to compare injury-risk profiles across multiple players at once when evaluating a trade, so he doesn't trade for a player who looks good on paper but is injury-prone.
3. Wants risk data he can defend to his league-mates when making a controversial keeper/trade call — not just a black-box score.

### Persona 2: Jarvis Dent — The Rookie

**Background:** 20-year-old college junior who just found a close friend group and, though introverted, wants to get more involved in their group activities. He was invited to join their fantasy football league. He isn't a big football fan — most of his exposure comes from scrolling highlights on TikTok — and he barely understands how the app works or the players beyond a few big names.

**Goals:**

1. Wants to feel like a genuine part of the friend group by participating, not just be a name on the league roster.
2. Wants to avoid finishing last and facing the league's loser punishment, despite not caring about football itself.
3. Wants simple, plain-language guidance on which players are risky picks — he doesn't know the players or the analytics, so he needs the app to translate injury-risk data into a clear "safe or not" signal rather than raw numbers.

## User Stories

1. As a fantasy manager, I want to filter the roster list by position or team, so that I can narrow down players relevant to me.

   **Acceptance Criteria:**
   1. Given the roster list, when I select one or more positions (e.g., QB, RB, WR, TE) and/or one or more teams for filter controls, then the list updates to show only players matching all selected criteria.
   2. Given multiple filters are active, when I clear an individual filter or select "Reset," then the list returns to its unfiltered (or previous) state without a full page reload.
   3. Given no players match the selected filters, when the filter is applied, then an empty-state message is shown instead of a blank list.
   4. Given I apply filters, when I navigate away and return to the roster list within the same session, then my filter selections persist.

2. As a fantasy manager, I want to sort the full player list by risk score, so that I can see who's most at risk league-wide.

   **Acceptance Criteria:**
   1. Given the full player list, when I select "Sort by risk score," then players are ordered from highest to lowest risk score by default, with a control to reverse the order.
   2. Given players have identical risk scores, when the list is sorted, then a consistent secondary sort (e.g., alphabetical by name) is applied so ordering doesn't jump between refreshes.
   3. Given a player has no risk score available (e.g., insufficient data), when sorting is applied, then that player is grounded clearly (e.g., at the bottom, labeled "No data") rather than breaking the sort.
   4. Given I combine sorting with an active filter (position/team), when both are applied, then the sort order is perserved within the filtered subset.

3. As a mobile user, I want push notifications for risk-score alerts, so that I get warned even when I'm not in the app.

   **Acceptance Criteria:**
   1. Given I have not yet granted notification permissions, when I opt in to risk-score alerts, then the app prompts the OS-level push permission request.
   2. Given a rostered player's risk score crosses a defined alert threshold, when the change is detected, then a push notification is sent within an agreed time window (e.g., within 15 minutes) containing the player's name and new risk level.
   3. Given I tap a risk-score push notification, when the app opens, then I'm taken directly to that player's detail view, not just the app home screen.
   4. Given I want to control noise, when I go to notification settings, then I can turn risk-score alerts on/off independently of other notification types.
   5. Given a player's risk score fluctuates rapidly near the threshold, when multiple crossings occur in a short period, then notifications are throttled/deduplicated (e.g., max one alert per player per X hours) to avoid spamming the user.

4. As a user, I want a plain-language explainer of what "ACWR" means, so that I don't need a sports-science background to trust the score.

   **Acceptance Criteria:**
   1. Given I see "ACWR" or a risk score derived from it anywhere in the UI, when I tap/click an info icon or the term itself, then a plain-language explanation appears (tooltip or modal) without leaving the current screen.
   2. Given the explainer is shown, when I read it, then it defines ACWR in non-technical terms, explains what drives a player's score up or down, and avoids unexplained jargon or raw formulas.
   3. Given the explainer is displayed, when I view it, then it includes a simple visual or example (e.g., a labeled scale of low/moderate/high risk) to reinforce the text explanation.
   4. Given I'm a screen reader or accessibility-tool user, when I access the explainer, then its content is fully readable/navigable via assistive technology.

## Step-by-step Use Cases

1. User sorts the full player list by risk score

   **Actor:**
   Fantasy Manager

   **Trigger:**
   User wants to see which players league-wide are currently at the highest injury risk, regardless of who's on their roster.

   **Preconditions:**
   User is logged in and has an active roster.

   **Steps:**
   1. User navigates to the search bar from any screen in the app.
   2. User types the player's name (or a partial match) into the search field.
   3. System displays a list of matching players in real time as the user types, showing name, team, and position for disambiguation.
   4. User selects the intended player from the results list.
   5. System navigates to the player's detail page.
   6. System displays the player's current risk score, risk level (e.g., low/moderate/high), and the underlying ACWR value.
   7. User taps the info icon next to the risk score to view the plain-language explainer (per Story 4).
   8. System displays the explainer without navigating away from the player detail page.

   **Expected Outcome:**
   The user successfully identifies the correct player and views an up-to-date, understandable risk score for that player, with the option to learn what the score means.

   **Alternate/Exception Flows:**
   1. No matches found: If no players match the search term, the system displays a "No players found" message and suggests checking spelling.
   2. No risk data available: If the selected player has insufficient data for a risk score, the system displays "Risk data unavailable" instead of a blank or misleading score.

2. User filters the roster list by position and enables a risk-score alert.

   **Actor:**
   Fantasy manager (mobile user)

   **Trigger:**
   User wants to monitor only their running backs for risk-score changes and receive push notifications when risk increases.

   **Preconditions:**
   User is logged in, has a roster with players assigned to multiple positions, and has the app installed on a mobile device.

   **Steps:**
   1. User navigates to the roster list screen.
   2. User opens the filter controls and selects "RB" under the position filter.
   3. System updates the roster list to display only running backs on the user's roster.
   4. User selects a specific player from the filtered list to open their detail page.
   5. User taps "Enable risk alerts" for that player.
   6. If push permissions have not yet been granted, the system prompts the OS-level push notification permission request.
   7. User grants permission.
   8. System confirms that risk-score alerts are enabled for the selected player and returns the user to the player detail page.
   9. At a later time, the player's risk score crosses the defined alert threshold.
   10. System sends a push notification to the user's device containing the player's name and new risk level.
   11. User taps the notification.
   12. System opens the app directly to that player's detail page.

   **Expected Outcome:**
   The user views a roster list narrowed to their running backs, successfully enables push alerts for a specific player, and receives a timely, actionable notification when that player's risk score changes -- without needing to have the app open.

   **Alternate/Exception Flows:**
   1. Permission denied: If the user denies push permission, the system disables the alert toggle and displays a message explaining that alerts require notification access, with a link to device settings.
   2. No results after filtering: If the user has no players at the selected position, the system displays an empty-state message instead of a blank list.
