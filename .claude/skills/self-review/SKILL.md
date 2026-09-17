---
name: self-review
description: Review your own in-progress or finished changes before handing them over, by fanning out one independent review agent per dimension (bugs, correctness/robustness, engineering/design, cleanliness, scope creep, comments, Android API usage and version guards, test coverage). Use when the user asks for a self-review, a review of the current diff/branch/PR before pushing, or when you have just finished a non-trivial change and want it checked.
---

# Self-review

Fan out a set of narrow, independent reviewers over a diff, then merge their
findings into one report. Narrow reviewers beat one generalist: each agent
looks for one class of problem and is not distracted by the others.

## 1. Establish the diff

Unless the user names a target, review the branch against `master`:

```
git fetch origin master
git diff origin/master...HEAD --stat
git diff origin/master...HEAD
```

Include uncommitted work (`git diff` / `git status --short`) if there is any.
If the diff is empty, say so and stop.

Write the diff to a scratch file and give every agent its path — it is cheaper
than each of them regenerating it, and they all see exactly the same thing.

## 2. Fan out

Launch **all** of the agents below in parallel, in one message. Each gets:

- the path to the diff file, and the git range it came from,
- the instruction to read the surrounding code in full (a diff hides context),
- its own brief from the list below,
- this output contract:

> Report findings as a list. Each: `file:line` — one sentence on what is wrong,
> one on why it matters, and the concrete fix. Rank by severity. Only report
> what you would defend in review; no style-guide padding, no "consider
> possibly". If you find nothing, say so — an empty report is a valid result.
> Do not change any code.

### Reviewers

1. **Bugs** — defects in the changed code: wrong logic, off-by-one, null/index
   handling, inverted conditions, resource and listener leaks, lifecycle
   mistakes, state that can go stale, threading and re-entrancy. Trace each
   changed path with concrete inputs rather than reading for plausibility.

2. **Correctness & robustness** — does the change actually do what it set out
   to do, and does it hold up at the edges? Empty/huge inputs, missing
   packages, revoked permissions, absent optional components, configuration
   changes, process death, failure paths and what the user sees when they hit
   one. Flag silent catch blocks and error paths that leave the UI lying.

3. **Engineering & design** — is this the right shape? Responsibilities in the
   right class, sensible boundaries, no reinvention of something the codebase
   or the platform already provides, no abstraction invented for a single
   caller. Judge it against the patterns already in this repo (see AGENTS.md),
   not against a textbook. Over-engineering is a finding too.

4. **Cleanliness** — naming, dead code, duplication, leftover debug logging,
   commented-out code, inconsistent formatting with the surrounding file,
   things left half-renamed. Small findings, but name them precisely.

5. **Scope creep** — group the diff into coherent sets of changes and check
   each against what the task actually asked for. Anything that is not needed
   for the stated goal — drive-by refactors, unrelated fixes, opportunistic
   renames, new options nobody asked for — is a finding. Report each such set
   as: what it is, which files/hunks it covers, why it is out of scope, and
   whether it could stand alone as its own change. **This reviewer's findings
   become questions for the user** (see step 4).

6. **Comments** — docstrings are exempt from the burden of proof. Every other
   comment must earn its place: it stays only if it explains something the
   code cannot say itself (a non-obvious reason, a constraint, a trap, a
   workaround for external behaviour). Flag comments that restate the code,
   narrate the diff ("now we also…"), are stale relative to the code they sit
   above, or are three sentences where one would do. Verbosity is cognitive
   load, not thoroughness — propose the shortened wording, or deletion.

7. **Android APIs & version guards** — `minSdk` 31, `compileSdk`/`targetSdk`
   36. Every API newer than 31 needs a real guard (`Build.VERSION.SDK_INT`
   check or `@RequiresApi` on a call site that is itself guarded), and the
   guard must name the version the API *actually* appeared in — not the one
   that seems about right. Check deprecations, the correct overload for the
   target level, `PendingIntent` mutability flags, intent/package-visibility
   requirements, permission declarations, and whether an equivalent
   `ContextCompat`/`AndroidX` helper should be used instead. Check that new
   lint errors are fixed rather than baselined (`app/lint-baseline.xml` is for
   pre-existing issues only). Verify claims against the real API docs where
   you are unsure — a wrong version number here ships a crash.

8. **Test coverage** — are the changes covered by tests that would actually
   fail if the change regressed? Unit tests live in `app/src/test/` (Kotlin +
   Robolectric); prefer those over instrumented tests. Look for: untested new
   branches, edge cases asserted nowhere, tests that assert the mock rather
   than the behaviour, and tests that would pass with the implementation
   deleted. Missing coverage for a pure-refactor diff is not a finding; missing
   coverage for new behaviour is.

## 3. Merge

Collect every report, then:

- Drop findings that are wrong — verify each one against the code before you
  pass it on. Reviewers hallucinate; you are the filter.
- Deduplicate: several reviewers will find the same thing from different
  angles. Report it once, under the angle that explains it best.
- Rank: bugs and API/version mistakes first, then correctness, then design,
  then cleanliness and comments.

Present as a short list grouped by severity, each item with `file:line`, the
problem in one sentence, and the fix. No preamble, no score, no summary of how
many agents ran.

## 4. Scope-creep questions

Never silently discard or keep out-of-scope changes. For each set the scope
reviewer flagged, put the decision to the user — use `AskUserQuestion` with one
question per set (keep / discard / split out into its own change), or ask
plainly in text if that tool is unavailable. Wait for the answer; act on it
only afterwards.

## 5. Fixing

Default to reporting, not fixing. Apply fixes only when the user asks — then
fix and re-run the relevant reviewer over the new diff.
