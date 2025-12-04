# CI/CD Setup Guide for Yole

This guide walks you through setting up the complete CI/CD infrastructure for Yole, including GitHub Actions, code coverage, and automated deployments.

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [GitHub Actions Setup](#github-actions-setup)
- [Codecov Setup](#codecov-setup)
- [GitHub Pages Setup](#github-pages-setup)
- [Testing CI Workflows](#testing-ci-workflows)
- [Troubleshooting](#troubleshooting)

---

## Overview

Yole uses a comprehensive CI/CD pipeline with four main workflows:

1. **Main Build** - Builds Android APK on every push
2. **Tests & Coverage** - Runs full test suite with coverage reporting
3. **Lint & Docs** - Code quality checks and API documentation
4. **PR Validation** - Comprehensive validation for pull requests

**Automation Benefits**:
- Automated testing on every commit
- Code coverage tracking and enforcement
- API documentation auto-generation
- Pull request validation before merge
- APK size tracking
- Dependency security scanning

---

## Prerequisites

Before setting up CI/CD, ensure you have:

- [x] GitHub repository with admin access
- [x] Yole project pushed to GitHub
- [x] Gradle build working locally
- [ ] Codecov account (free for open source)
- [ ] Basic understanding of GitHub Actions

---

## GitHub Actions Setup

### Step 1: Verify Workflow Files

All workflow files are already in place in `.github/workflows/`:

```bash
.github/workflows/
├── build-android-project.yml    # Main build (legacy)
├── test-and-coverage.yml        # Test execution & coverage
├── lint-and-docs.yml            # Lint checks & documentation
├── pr-validation.yml            # Pull request validation
└── README.md                    # CI documentation
```

**Verification**:
```bash
ls -la .github/workflows/
```

### Step 2: Enable GitHub Actions

1. Go to your GitHub repository
2. Click **Settings** tab
3. Navigate to **Actions** → **General**
4. Under **Actions permissions**, select:
   - ✅ **Allow all actions and reusable workflows**
5. Under **Workflow permissions**, select:
   - ✅ **Read and write permissions**
   - ✅ **Allow GitHub Actions to create and approve pull requests**
6. Click **Save**

### Step 3: Verify First Workflow Run

Push your code to trigger workflows:

```bash
git add .github/workflows/
git commit -m "Add CI/CD workflows"
git push origin master
```

**Check Status**:
1. Go to **Actions** tab in GitHub
2. You should see workflows running
3. Wait for completion (first run may take longer due to cache initialization)

---

## Codecov Setup

Codecov provides code coverage visualization and enforcement.

### Step 1: Create Codecov Account

1. Go to [codecov.io](https://codecov.io/)
2. Click **Sign up with GitHub**
3. Authorize Codecov to access your GitHub account

### Step 2: Add Yole Repository

1. In Codecov dashboard, click **Add new repository**
2. Find and select **Yole** repository
3. Codecov will show your repository token

### Step 3: Add Codecov Token to GitHub

1. Copy the **repository upload token** from Codecov
2. Go to your GitHub repository
3. Navigate to **Settings** → **Secrets and variables** → **Actions**
4. Click **New repository secret**
5. Name: `CODECOV_TOKEN`
6. Value: Paste your Codecov token
7. Click **Add secret**

### Step 4: Verify codecov.yml Configuration

The `codecov.yml` file is already configured with:

```yaml
coverage:
  status:
    project:
      target: 80%      # Project coverage goal
    patch:
      target: 70%      # New code coverage requirement
```

**Location**: `codecov.yml` in repository root

### Step 5: Trigger Coverage Upload

Push a commit to trigger test workflow:

```bash
git commit --allow-empty -m "Trigger coverage upload"
git push origin master
```

**Verify**:
1. Go to **Actions** tab
2. Wait for **Tests & Coverage** workflow to complete
3. Check Codecov dashboard for uploaded report
4. Add Codecov badge to README (optional):

```markdown
[![codecov](https://codecov.io/gh/vasic-digital/Yole/branch/master/graph/badge.svg)](https://codecov.io/gh/vasic-digital/Yole)
```

---

## GitHub Pages Setup

GitHub Pages hosts the auto-generated API documentation.

### Step 1: Enable GitHub Pages

1. Go to **Settings** → **Pages**
2. Under **Source**, select:
   - Branch: `gh-pages`
   - Folder: `/ (root)`
3. Click **Save**

**Note**: The `gh-pages` branch will be created automatically by the workflow on first documentation generation.

### Step 2: Configure Custom Domain (Optional)

If you have a custom domain:

1. In Pages settings, enter your domain in **Custom domain**
2. Add DNS records as instructed by GitHub
3. Wait for DNS propagation
4. Enable **Enforce HTTPS**

### Step 3: Trigger Documentation Generation

Documentation is auto-generated on pushes to master:

```bash
git commit --allow-empty -m "Generate API documentation"
git push origin master
```

**Access Documentation**:
- Default: `https://vasic-digital.github.io/Yole/api/`
- Custom domain: `https://yourdomain.com/api/`

### Step 4: Link Documentation in README

Update README.md with documentation link:

```markdown
📚 **[API Documentation](https://vasic-digital.github.io/Yole/api/)** - Comprehensive API reference
```

---

## Testing CI Workflows

### Test Main Build Workflow

```bash
# Make a simple change
echo "# Test" >> TEST.md
git add TEST.md
git commit -m "Test CI build"
git push origin master
```

**Expected**:
- ✅ `CI` workflow runs
- ✅ Android APK is built
- ✅ Artifacts are uploaded

### Test Test & Coverage Workflow

```bash
# Trigger test workflow
git commit --allow-empty -m "Test coverage workflow"
git push origin master
```

**Expected**:
- ✅ `Tests & Coverage` workflow runs
- ✅ All test jobs complete (shared, android, desktop)
- ✅ Coverage report is generated
- ✅ Report is uploaded to Codecov

### Test Lint & Docs Workflow

```bash
# Trigger lint workflow
git commit --allow-empty -m "Test lint workflow"
git push origin master
```

**Expected**:
- ✅ `Lint & Docs` workflow runs
- ✅ Detekt, Android Lint, KtLint execute
- ✅ API documentation is generated
- ✅ Documentation is published to GitHub Pages

### Test PR Validation Workflow

1. Create a new branch:
   ```bash
   git checkout -b test-pr-validation
   echo "# Test PR" >> TESTPR.md
   git add TESTPR.md
   git commit -m "Test PR validation"
   git push origin test-pr-validation
   ```

2. Create pull request on GitHub

**Expected**:
- ✅ `PR Validation` workflow runs
- ✅ Build, test, lint, size, and security checks execute
- ✅ Summary comment is posted to PR
- ✅ Status checks appear on PR

---

## Workflow Configuration

### Adjust Coverage Thresholds

Edit `codecov.yml`:

```yaml
coverage:
  status:
    project:
      target: 80%       # Change project target
      threshold: 1%     # Allow 1% decrease
    patch:
      target: 70%       # Change patch target
      threshold: 5%     # Allow 5% decrease
```

### Customize Workflow Triggers

Edit workflow files to change triggers:

**Example: Run tests only on main branches**

```yaml
on:
  push:
    branches:
      - master
      - main
    paths-ignore:
      - 'docs/**'
      - '**.md'
```

**Example: Skip CI on specific commits**

Add `[ci skip]` or `[skip ci]` to commit message:

```bash
git commit -m "Update README [ci skip]"
```

### Adjust Workflow Timeouts

Edit workflow files:

```yaml
jobs:
  test:
    timeout-minutes: 30    # Change from default 360
```

---

## Monitoring CI Health

### View Workflow Status

**GitHub Actions Dashboard**:
1. Go to **Actions** tab
2. View all workflow runs
3. Click on run to see detailed logs
4. Download artifacts if needed

**GitHub Checks API**:
- Status checks appear on commits and PRs
- Green checkmark = passed
- Red X = failed
- Yellow dot = running

### Coverage Trends

**Codecov Dashboard**:
1. Go to [codecov.io](https://codecov.io/)
2. Select Yole repository
3. View coverage trends over time
4. See coverage by module/file
5. Identify uncovered lines

### Workflow Performance

**Optimize for Speed**:
- Enable Gradle caching (already configured)
- Use concurrency groups to cancel old runs
- Run independent jobs in parallel
- Skip unnecessary steps with path filters

**Current Performance**:
- Main Build: ~10-15 min
- Tests & Coverage: ~20-30 min
- Lint & Docs: ~15-20 min
- PR Validation: ~25-35 min

---

## Troubleshooting

### Workflow Fails Immediately

**Problem**: Workflow fails with "Invalid workflow file"

**Solution**:
```bash
# Validate YAML syntax
yamllint .github/workflows/*.yml

# Check GitHub Actions tab for specific error
```

### Coverage Upload Fails

**Problem**: "Error: Codecov token not found"

**Solution**:
1. Verify `CODECOV_TOKEN` secret is set in GitHub
2. Check secret name matches workflow file
3. Ensure token is valid in Codecov dashboard

### Documentation Not Publishing

**Problem**: API docs not appearing on GitHub Pages

**Solution**:
1. Check GitHub Pages is enabled in Settings
2. Verify `gh-pages` branch exists
3. Check workflow logs for `peaceiris/actions-gh-pages` step
4. Ensure `GITHUB_TOKEN` has write permissions

### Tests Failing in CI But Pass Locally

**Problem**: Tests pass locally but fail in CI

**Solution**:
1. Check Java version match (CI uses Java 21)
2. Verify Gradle wrapper version
3. Check for timezone/locale dependencies
4. Review test isolation (no shared state)
5. Add detailed logging:
   ```bash
   ./gradlew test --info --stacktrace
   ```

### APK Size Check Fails

**Problem**: "Could not compare APK sizes"

**Solution**:
1. Verify base branch builds successfully
2. Check APK output path in workflow
3. Ensure both base and PR APKs exist

### Lint Failures

**Problem**: Lint checks fail with many warnings

**Solution**:
1. Run locally: `./gradlew lint detekt ktlintCheck`
2. Fix critical issues
3. Add suppressions for false positives:
   ```kotlin
   @Suppress("UnusedPrivateMember")
   ```
4. Update baseline files for Detekt

---

## Maintenance

### Update Workflow Actions

Regularly update actions to latest versions:

```yaml
# Before
- uses: actions/checkout@v4

# After (check for newer version)
- uses: actions/checkout@v5
```

**Check for Updates**:
1. Go to Actions tab in GitHub
2. GitHub shows warnings for outdated actions
3. Update versions in workflow files

### Rotate Secrets

Periodically rotate sensitive tokens:

1. Generate new Codecov token
2. Update GitHub secret
3. Verify workflows still function

### Clean Up Old Artifacts

GitHub retains artifacts based on retention days:

- Test reports: 14 days
- Coverage reports: 30 days
- Lint reports: 7 days

**Manual Cleanup**:
1. Go to **Actions** tab
2. Click on workflow run
3. Delete artifacts if needed

---

## Advanced Configuration

### Matrix Testing

Test across multiple Java versions:

```yaml
jobs:
  test:
    strategy:
      matrix:
        java: [17, 21]
    steps:
      - uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java }}
```

### Conditional Workflows

Run workflows only on specific conditions:

```yaml
jobs:
  deploy:
    if: github.ref == 'refs/heads/master' && github.event_name == 'push'
```

### Workflow Dispatch Inputs

Allow manual parameters:

```yaml
on:
  workflow_dispatch:
    inputs:
      coverage-threshold:
        description: 'Minimum coverage percentage'
        required: false
        default: '80'
```

---

## Security Best Practices

1. **Never commit secrets** - Use GitHub Secrets
2. **Limit secret access** - Only required workflows
3. **Use dependabot** - Auto-update dependencies
4. **Pin action versions** - Use specific tags, not `@main`
5. **Review third-party actions** - Verify source and permissions

---

## Support

For CI/CD issues:

- **Workflow Logs**: Check Actions tab in GitHub
- **Documentation**: `.github/workflows/README.md`
- **Issues**: [GitHub Issues](https://github.com/vasic-digital/Yole/issues)
- **Discussions**: [GitHub Discussions](https://github.com/vasic-digital/Yole/discussions)

---

## Summary Checklist

Setup completion checklist:

- [ ] GitHub Actions enabled
- [ ] Workflow permissions configured
- [ ] Codecov account created
- [ ] `CODECOV_TOKEN` secret added
- [ ] GitHub Pages enabled
- [ ] All workflows triggered and passed
- [ ] Coverage uploaded to Codecov
- [ ] API documentation published
- [ ] Badges added to README
- [ ] PR validation tested

---

*Last Updated: November 2025*
*Yole CI/CD Infrastructure v1.0*
