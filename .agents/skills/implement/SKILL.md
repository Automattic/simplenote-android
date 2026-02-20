---
name: implement
description: "End-to-end implementation workflow: plan, implement, verify, commit, and open a draft PR. Use when asked to implement, fix, build, or work on something."
---

# Implement Workflow

This skill handles the full lifecycle: plan -> implement -> verify -> commit -> draft PR.

## Phase 1: Plan (requires approval)

1. **Understand the request** — Read the issue description, linked context, or user instructions.
2. **Explore the codebase** — Find relevant files, understand existing patterns, read tests.
3. **Create an implementation plan**:
   - What files will be modified/created
   - What the changes will do
   - What edge cases to consider
   - What tests to add or update
4. **Present the plan and STOP** — Wait for explicit user approval before proceeding.

Do NOT start coding until the plan is approved. If requirements are ambiguous, ask.

## Phase 2: Implement

Execute the approved plan:
- Follow existing code patterns and conventions in the repo
- Keep changes minimal and focused — don't refactor unrelated code
- Max line length: 120 characters (enforced by `.editorconfig`)
- Use `com.automattic.simplenote` package conventions
- **Unit tests**: Add or update tests covering new/changed logic. Follow existing test patterns (JUnit 4, Mockito, coroutine test utilities). If writing meaningful tests would require disproportionate effort (e.g., complex setup, heavy mocking of framework internals), skip but notify the developer explaining why.

## Phase 3: Verify

Run verification checks and fix any failures. Iterate until all checks pass.

### 3a: Compile

```bash
./gradlew Simplenote:assembleDebug
```

If other modules were changed, compile those too:
```bash
./gradlew assembleDebug
```

### 3b: Lint

```bash
./gradlew :Simplenote:lintDebug
```

- Fix violations — prefer real fixes over suppression
- Only suppress when it's a false positive and document why

### 3c: Unit Tests

```bash
./gradlew :Simplenote:testDebugUnitTest
```

Or run specific tests related to the changes:
```bash
./gradlew :Simplenote:testDebugUnitTest --tests "com.automattic.simplenote.SpecificTest" --info
```

**Test rules:**
- NEVER weaken or remove assertions to make tests pass
- NEVER modify production code just to pass a test without permission
- Tests that pass only by not crashing are invalid — every test needs meaningful assertions
- If a test won't pass after reasonable attempts: stop and ask

### 3d: Fix Errors

If any step fails:
1. Analyze the error
2. Fix the issue
3. Re-run the failing check
4. Repeat until green

## Phase 4: Present Changes (requires approval)

Show a summary of all changes made:
- Files modified/created
- Key behavioral changes
- Test coverage

**STOP and wait for user approval** before committing.

## Phase 5: Commit and Draft PR

Only proceed after explicit approval.

### 5a: Run Final Lint

```bash
./gradlew :Simplenote:lintDebug
```

Fix any remaining issues.

### 5b: Inspect Changes

```bash
git status
git diff --stat
git diff
```

### 5c: Plan Commits

Review the changes and determine if they should be split into multiple commits:
- Independent logical units = separate commits
- Bug fix + feature = separate commits
- Formatting + logic = separate commits

For **each commit**, prepare:
1. **Commit message**: Imperative summary + brief body
2. **Files**: Paths to stage
3. **Summary**: What and why

**Commit message format** — use direct multi-line strings:
```bash
git commit -m "Imperative summary

- Detail one
- Detail two
"
```

**Rules:**
- NO co-author lines — NEVER add "Co-Authored-By" or AI attribution
- Each commit should be cohesive and buildable
- Use `git add -p` to split mixed concerns if needed

### 5d: Stage and Commit

```bash
git add <specific files>
git commit -m "message"
```

### 5e: Push and Create Draft PR

```bash
# Get the correct remote owner/repo
git remote get-url origin

# Push
git push -u origin HEAD

# Create draft PR
PAGER=cat gh pr create --draft --title "PR title" --body "$(cat <<'EOF'
### Fix
<description>

### Test
1. Step 1
2. Step 2

### Review
<review instructions>

### Release
<release notes statement>
EOF
)"
```

**PR rules:**
- Title: short, under 70 characters
- Body: follows the repo's PR template format (### Fix, ### Test, ### Review, ### Release)
- Always create as **draft**
- Target branch: `trunk`
- Return the PR URL when done
