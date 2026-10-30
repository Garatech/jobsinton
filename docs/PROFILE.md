# Professional profile

The first profile increment provides an editable professional profile and a summary of the last successful save. It runs in the browser and remains usable when the Java service is unavailable.

## Data and validation

| Field | Rule |
| --- | --- |
| Target role | Backend, frontend, full stack, SAP, data or platform |
| Technologies | 1–20 unique entries, each 1–40 characters after whitespace normalization |
| Relevant experience | Whole years from 0 to 60; zero is valid |
| Country of residence | Required text, 1–80 characters after normalization |
| City of residence | Required text, 1–80 characters after normalization |
| Preferred work mode | One of remote, hybrid, or on-site |
| Maximum on-site days | Required integer from 1 to 5 for hybrid/on-site; stored as null for remote |

Technologies are compared without case sensitivity, but their display spelling is retained. Leading/trailing whitespace is removed and repeated whitespace is collapsed. Country and city are free text in this increment; they are not geocoded or evidence of work authorization. The experience bound is an input limit, not a suitability score.

Switching work mode clears the previous day limit. Returning from remote to hybrid/on-site requires choosing a limit again. Only one preferred mode is supported in this version.

## Editing and storage

- **Add technology** or Enter adds an entry; each entry has a named removal button.
- **Save profile** validates every field and saves explicitly. If a technology is still typed in the input, it is validated and included rather than silently discarded.
- Field errors are associated with their controls, and submission focuses the first invalid field.
- The summary shows the last successful save. Unsaved edits are marked, remain available when navigating within the app, and are lost on reload. **Discard changes** restores the last saved values.
- **Clear saved profile** asks for confirmation and clears both the stored profile and the current draft. Cancelling keeps both.
- Storage errors preserve the draft and never display a successful-save message. The previous saved summary remains visible.
- Malformed or unsupported saved data produces a recovery notice. It is not changed automatically: a successful save replaces it, or Clear removes it.

Storage key: `jobsinton.professional-profile`. Format: `{ "version": 1, "profile": { ... } }`. Saved input is checked for shape, version, field validity, and a 32 KiB size limit before rendering.

The profile is stored in `localStorage`, scoped to the browser profile and origin (scheme, host, and port). Use the same URL consistently: `localhost:5173` and `127.0.0.1:5173` have separate storage. Git, switching devices, and opening a different browser do not transfer this profile. Clearing site data removes it; private browsing may restrict storage or discard it when closed.

The profile is not encrypted, is not a user account, and is not sent to the backend. Use fictional data for demonstrations on shared devices. Multiple tabs do not synchronize live; the last successful save wins.

## Verify after merging the pull request

### 1. Update the checkout

Stop the frontend, backend, and any test runners with Ctrl+C in their terminals. From the repository root:

```sh
git status --short --branch
git remote -v
```

Confirm the remote is `Garatech/jobsinton`. If local changes exist, preserve them on their work branch before switching; do not discard them. Once the checkout is clean:

```sh
git switch main
git pull --ff-only
```

If the pull cannot fast-forward, inspect the history before proceeding.

### 2. Check tools and run automated verification

```sh
java --version
javac --version
mvn --version
node --version
npm --version
```

Java, javac, and Maven's Java runtime must use version 21; Node must use version 24. Maven has its own version number. If VS Code shows an old runtime, close all VS Code windows and reopen the project from a terminal with the correct environment.

With the development servers stopped, run each command in order and continue only when it succeeds:

```sh
mvn -B -ntp -f backend/pom.xml verify
npm --prefix frontend ci
npm --prefix frontend run build
cd frontend
npx playwright install chromium
npm run test:e2e
cd ..
```

The browser suite starts both services automatically and covers desktop and mobile Chromium. Stop existing services first so the tests use the current code. On Linux, browser system dependencies may require `npx playwright install --with-deps chromium`.

### 3. Start the app

From the repository root, terminal 1:

```sh
mvn -f backend/pom.xml spring-boot:run
```

From the repository root, terminal 2:

```sh
npm --prefix frontend run dev
```

Open [http://localhost:5173](http://localhost:5173), then select **Profile** in the navigation.

### 4. Manual acceptance checklist

Use fictional details such as Backend developer, Java, React, 0 years, Spain, Madrid, Hybrid, and 2 on-site days.

- [ ] Save an empty form: clear errors appear and focus moves to Target role.
- [ ] Add Java, then try ` java `: the duplicate is rejected. Remove and re-add a technology.
- [ ] Enter zero years: it is accepted. Negative, fractional, and greater-than-60 values are rejected.
- [ ] Choose Hybrid without a day limit: saving requests a limit.
- [ ] Save the sample profile: the success message and summary reflect the entered values.
- [ ] Reload the same URL: the form and summary retain the saved profile.
- [ ] Edit the city without saving: the summary retains the saved city and an unsaved-changes notice appears.
- [ ] Reload without saving: the previously saved city returns. Edit again and save: the summary updates.
- [ ] Type a new technology and select Save directly: it is included in the saved profile.
- [ ] Change Hybrid to Remote and save: no day selector remains and the summary says remote only.
- [ ] Switch back to Hybrid: choose the limit again. Check On-site with 5 days as well.
- [ ] Navigate with Tab, Shift+Tab, Enter, and the select controls' arrow keys: focus is visible and all actions are reachable.
- [ ] Check a narrow mobile-sized window: controls and summary remain readable without horizontal scrolling.
- [ ] Open **Service status** in the footer, stop the Java service and check its connection: the API shows unavailable, but the profile can still be saved.
- [ ] Select Clear saved profile and cancel: data remains. Repeat and confirm, then reload: the profile is empty.

Storage denial, failed writes/removals, malformed records, unknown versions, and long-value layout checks are covered by automated tests. To manually inspect recovery, use a separate browser profile with fictional data and change this application's storage key to malformed JSON in browser developer tools, then reload. A notice should appear and a new valid save should recover the profile.

## Automated verification record

[GitHub Actions run 39445633841](https://github.com/Garatech/jobsinton/actions/runs/39445633841) passed for implementation commit `25d971b`: backend build and 2 HTTP tests, locked frontend installation, strict TypeScript checking, production build, and 40 Playwright executions. These comprise 30 browser journeys across desktop/mobile Chromium and 5 pure validation cases repeated in both project configurations.

The run also retains desktop/mobile screenshots in the `profile-previews` artifact for seven days. These are review aids, not evidence of human visual acceptance. See [PR #10](https://github.com/Garatech/jobsinton/pull/10) for the final checked revision.

## Implementation map

- `frontend/src/profile/model.ts`: data types, normalization, validation, and saved-data parsing.
- `frontend/src/profile/storage.ts`: guarded browser storage operations.
- `frontend/src/profile/ProfileEditor.tsx`: draft, errors, controls, summary, and explicit save/clear actions.
- `frontend/tests/profile-model.spec.ts`: validation boundaries and saved-data contracts.
- `frontend/tests/profile.spec.ts`: user journeys, recovery, keyboard operation, and viewport checks.

No backend profile endpoint, account, database persistence or matching score is included. The current interface uses the saved profile alongside the fictional catalog described in [DESIGN.md](DESIGN.md). Backend validation and user isolation will be required when profiles are stored on the server.

See [development](DEVELOPMENT.md), [architecture](ARCHITECTURE.md), and [changelog](../CHANGELOG.md).
