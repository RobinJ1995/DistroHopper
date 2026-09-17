---
name: self-review
description: Review your own changes before handing them over by fanning out one independent reviewer per dimension — bugs, robustness and failure paths, engineering/design, cleanliness, scope creep, comments, Android API usage and version guards, test coverage — then merging their findings and putting scope-creep decisions back to the user. Broader than the built-in code-review, and the only one that checks the diff against what was actually asked for. Use when the user asks for a self-review, or for the current diff/branch/PR to be reviewed before pushing.
---

# Self-review

Fan out narrow, independent reviewers over a diff, then merge their findings
into one report.

## 1. Establish the diff

Write down the **stated goal** first, verbatim: the user's request in their own
words, the task description, or the PR/issue body. Reviewers 2 and 5 cannot
work without it — if none is available, ask.

Unless the user names a target, review the branch against `master`. `$D` is a
scratch file (use the session scratchpad directory if there is one):

```
git fetch origin master
git add -N .                            # untracked files, so their content diffs
git diff origin/master...HEAD  >  "$D"  # committed work
git diff HEAD                  >> "$D"  # staged + unstaged work
```

Named targets: a PR number — `git fetch origin refs/pull/<n>/head` and diff
against its merge-base (or `gh pr diff <n>` where `gh` is available); a branch
or ref — `git diff $(git merge-base origin/master <ref>)..<ref>`; a path —
append `-- <path>` to the range.

If `$D` is empty, say so and stop. Above ~1500 lines, split it per file or per
area and fan out one reviewer per dimension per chunk — a reviewer that reads
only what its file reader returns before truncating will review a prefix and
report as if it reviewed the change.

## 2. Fan out

Run every reviewer whose dimension the diff can contain, and skip the rest:
reviewer 7 only when the diff touches `app/src/`, Gradle files or the
manifest; reviewer 8 only when behaviour changes; on a docs-only diff, 4, 5
and 6 alone. Name the skipped ones in the final report.

Launch the rest in parallel, in one message. Each gets:

- the path to the diff file, its length in lines, and the git range,
- the stated goal, verbatim,
- the instruction to read the surrounding code in full (a diff hides context),
- its own brief from the list below,
- this output contract:

> Begin with one line: your dimension, the files you actually examined, and
> anything you could not review and why. An empty finding list is a valid
> result, but only once that line accounts for every file in the diff.
>
> Then the findings, ranked by severity. Each: `file:line` — line numbers as
> they are in the working tree, not as they appear in the diff — one sentence
> on what is wrong, one on why it matters, and the concrete fix. Only report
> what you would defend in review; no style-guide padding, no "consider
> possibly". Do not change any code.

### Reviewers

1. **Bugs** — defects in the changed code: wrong logic, off-by-one, null/index
   handling, inverted conditions, resource and listener leaks, lifecycle
   mistakes, state that can go stale, threading and re-entrancy. Trace each
   changed path with concrete inputs rather than reading for plausibility.

2. **Robustness** — the edges and what happens when they are hit: empty/huge
   inputs, missing packages, revoked permissions, absent optional components,
   configuration changes, process death. Follow every failure path to what the
   user ends up seeing. Flag silent catch blocks and error handling that
   leaves the UI lying.

3. **Engineering & design** — is this the right shape? Responsibilities in the
   right class, sensible boundaries, no reinvention of something the codebase
   or the platform already provides, no abstraction invented for a single
   caller. Judge it against the patterns already in this repo (see AGENTS.md),
   not against a textbook. Over-engineering is a finding too.

4. **Cleanliness** — naming, dead code, duplication, leftover debug logging,
   commented-out code, inconsistent formatting with the surrounding file,
   things left half-renamed. Small findings, but name them precisely.

5. **Scope creep** — group the diff into coherent sets of changes and check
   each against the stated goal you were given, never against the diff's own
   apparent purpose. Anything the goal does not need — drive-by refactors,
   unrelated fixes, opportunistic renames, new options nobody asked for — is a
   finding. Report each set as: what it is, which files and hunks it covers,
   why it is out of scope, and whether it could stand alone as its own change.
   This brief replaces the output contract above.

6. **Comments** — docstrings are exempt from the burden of proof. Every other
   comment must earn its place: it stays only if it explains something the
   code cannot say itself (a non-obvious reason, a constraint, a trap, a
   workaround for external behaviour). Flag comments that restate the code,
   narrate the diff ("now we also…"), are stale relative to the code they sit
   above, or are three sentences where one would do. Propose the shortened
   wording, or deletion.

7. **Android APIs & version guards** — read `minSdk`, `compileSdk` and
   `targetSdk` from `app/build.gradle` and review against those (31 and 36 at
   the time of writing). Every API newer than `minSdk` needs a real guard
   (`Build.VERSION.SDK_INT` check, or `@RequiresApi` on a call site that is
   itself guarded), and the guard must name the version the API *actually*
   appeared in — not the one that seems about right. Check deprecations, the
   correct overload for the target level, `PendingIntent` mutability flags,
   intent/package-visibility requirements, permission declarations, and
   whether an equivalent `ContextCompat`/`AndroidX` helper should be used
   instead. Check that new lint errors are fixed rather than baselined
   (`app/lint-baseline.xml` is for pre-existing issues only). Confirm every
   API level against the docs, or offline against `@RequiresApi`/`@since` in
   the local SDK and AndroidX sources; a level you could not confirm is
   reported as unverified, never asserted — a wrong version number here ships
   a crash.

8. **Test coverage** — are the changes covered by tests that would actually
   fail if the change regressed? JVM unit tests are preferred over
   instrumented ones; AGENTS.md's Build & test section is authoritative on
   where they live and how they run. Look for: untested new branches, edge
   cases asserted nowhere, tests that assert the mock rather than the
   behaviour, and tests that would pass with the implementation deleted. You
   are reviewing, not running, so say so rather than implying you executed
   anything. Missing coverage for a pure-refactor diff is not a finding;
   missing coverage for new behaviour is.

## 3. Merge

Collect every report, then:

- Drop findings that are wrong — verify each one against the code before you
  pass it on, scope-creep sets included.
- Deduplicate: several reviewers will find the same thing from different
  angles. Report it once, under the angle that explains it best.
- Check for drift: compare `git status --short` against what step 1 captured.
  A reviewer that edited the tree despite the contract has invalidated
  everyone else's line numbers — say so.
- Rank: bugs and API/version mistakes first, then robustness, then design,
  then test coverage, then cleanliness and comments. Scope creep is not ranked
  here; it goes to step 4.

Present in that order, each item with `file:line`, the problem in one
sentence, and the fix. No preamble, no score, no summary of how many reviewers
ran. Close with one line naming any skipped reviewers and stating that nothing
was built, tested or linted.

## 4. Scope-creep questions

For each set that survived the verification above, put the decision to the
user — `AskUserQuestion` with one question per set (keep / split out into its
own change / drop), or plainly in text if that tool is unavailable. Record the
answer and stop there: step 5 governs whether anything is carried out.

## 5. Fixing

Apply fixes only when the user asks, step 4's answers included. Then
regenerate the diff file and re-run the relevant reviewer against it.

Uncommitted work is never reverted by this skill. If dropping a change means
discarding work that exists nowhere else, commit or stash it first.
