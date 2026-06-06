# GitHub Cleanup Steps

Do these before changing the repository to public.

## 1. Delete Old Workflow Runs And Artifacts

Use the GitHub web UI:

1. Open the repository on GitHub.
2. Go to **Actions**.
3. Open old workflow runs, especially runs before the workflow stopped decoding `APPLICATION_YML`.
4. Delete artifacts from each run.
5. Delete the workflow run if GitHub shows the delete option.

If GitHub CLI is available and authenticated, use:

```powershell
gh run list --limit 100
gh run delete <run-id>
```

Do not paste GitHub tokens into chat or commit them into the repository.

## 2. Rewrite Sensitive Git History

Dry run:

```powershell
.\scripts\security\rewrite-sensitive-history.ps1
```

Force-push cleaned history only after collaborators are ready:

```powershell
.\scripts\security\rewrite-sensitive-history.ps1 -Push
```

This removes:

- `ubuntu@www.sku-graduation.p-e.kr`
- `src/main/resources/application.properties`
- `teamPost`

## 3. Ask GitHub Support To Purge Pull Request References

After the force-push, contact GitHub Support and ask them to purge cached PR refs/views for the exposed credential.

Use the ready-to-submit request body in:

```text
scripts/security/github-support-request.md
```

Include:

- Repository: `sku-com-graduation/sku-graduation-exhibition-backend`
- Exposed path: `ubuntu@www.sku-graduation.p-e.kr`
- Exposed commit: `cc9ff92f2d60a15c8cd6b07c69432ab3dd44ddb1`
- Affected PR refs observed locally: PR `92`, PR `93`
- Cleanup paths removed by history rewrite:
  - `ubuntu@www.sku-graduation.p-e.kr`
  - `src/main/resources/application.properties`
  - `teamPost`

## 4. Re-scan

```powershell
.\scripts\security\scan-sensitive-history.ps1
```
