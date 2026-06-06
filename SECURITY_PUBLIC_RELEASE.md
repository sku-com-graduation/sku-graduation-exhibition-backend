# Public Release Security Checklist

Use this checklist before changing the repository visibility to public.

## Findings

- Current source files do not contain AWS access key IDs or secret access keys.
- Gitleaks 8.30.1 found no leaks in the current working tree or reachable Git history scanned locally.
- TruffleHog 3.95.5 found no verified or unknown secrets in the current working tree or reachable Git history scanned locally.
- Commit `cc9ff92` on `origin/develop/main` / PR refs added a Spring Boot jar named `ubuntu@www.sku-graduation.p-e.kr`.
- That jar contains `BOOT-INF/classes/application.properties` with an AWS access key ID pattern. Treat the related AWS credentials as exposed.
- Older history also contains `src/main/resources/application.properties` blobs with direct database configuration values.
- Previous workflow versions decoded GitHub secrets into `src/main/resources` before building the jar. That can bake runtime secrets into `build/libs/*.jar` and uploaded workflow artifacts.

## Required Before Going Public

1. Rotate or delete any AWS IAM access keys that were ever used by this project.
2. Delete old GitHub Actions workflow artifacts and old workflow runs that may contain jars built with decoded secrets.
3. Put runtime configuration on the EC2 host in `.env`, not in the repository or build artifact.
4. Prefer an EC2 IAM role with least-privilege S3 permissions. If local credentials are needed, use the AWS SDK standard environment variables:
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`
   - `AWS_SESSION_TOKEN` if using temporary credentials
5. Keep GitHub repository secrets for deployment-only data such as SSH key, server IP, and SSH user. Do not store application config as a base64-encoded file secret that is copied into the jar.

## Required History Cleanup

Before making the repository public, remove these paths from all Git history and force-push the rewritten affected branches:

- `ubuntu@www.sku-graduation.p-e.kr`
- `src/main/resources/application.properties`
- `teamPost`

Validated cleanup command for a fresh mirror clone:

```bash
git clone --mirror https://github.com/sku-com-graduation/sku-graduation-exhibition-backend.git repo-clean.git
cd repo-clean.git
git filter-repo \
  --path 'ubuntu@www.sku-graduation.p-e.kr' \
  --path 'src/main/resources/application.properties' \
  --path 'teamPost' \
  --invert-paths
git push origin --force --all
git push origin --force --tags
```

The local validation clone showed these checks after filtering:

- bad jar path refs: `0`
- bad jar blob refs: `0`
- `src/main/resources/application.properties` refs: `0`
- `teamPost` refs: `0`
- AWS access key ID patterns in reachable text files: `0`

Because GitHub retains pull request refs and cached PR views, contact GitHub Support after force-pushing and ask them to remove cached views and references to the sensitive data in PRs.

References:

- https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository
- https://docs.github.com/en/code-security/concepts/secret-security/secret-scanning

## EC2 Runtime Env Files

Create one of these files on the server before deploy:

- `/home/ubuntu/sku-graduation/.env`
- `/home/ubuntu/linuxMaster/.env`

Use `.env.example` as the template. Never commit the real `.env`.

## If GitHub Still Shows a Secret Alert

Open the GitHub secret scanning alert and check the reported location.

- If it points to a workflow artifact or run, delete the artifact/run and rotate the credential.
- If it points to a commit, rewrite history with `git filter-repo` or create a new clean public repository, then force-push only after every collaborator has coordinated.
- If it points to a fork or pull request ref, close/delete the source branch or ask the owner to remove the leaked content, then rotate the credential.
