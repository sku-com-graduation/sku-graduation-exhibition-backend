# GitHub Support Request

Use this at https://support.github.com/contact.

## Subject

Remove cached PR refs and views for sensitive AWS credential exposure

## Body

Hello GitHub Support,

I am an owner/maintainer of this repository:

`sku-com-graduation/sku-graduation-exhibition-backend`

We accidentally committed a Spring Boot jar that contained an application configuration file with an AWS access key ID pattern. The exposed AWS credential has to be treated as compromised. We have rewritten and force-pushed the affected branches to remove the sensitive file and related historical configuration/artifact paths, but GitHub pull request refs and cached PR views still reference the sensitive commit.

Please remove cached views and references to the sensitive data in the affected pull requests.

Sensitive commit:

`cc9ff92f2d60a15c8cd6b07c69432ab3dd44ddb1`

Sensitive path:

`ubuntu@www.sku-graduation.p-e.kr`

The sensitive path is a Spring Boot jar. Inside that jar, the sensitive file is:

`BOOT-INF/classes/application.properties`

The branch history has been cleaned and force-pushed. Current branch checks show:

```text
remote_bad_jar_path_refs=0
remote_application_properties_refs=0
remote_teamPost_refs=0
remote_aws_access_key_pattern_refs=0
```

The remaining GitHub pull request refs observed via `git ls-remote` are:

```text
4852015d1edced8fc05d2c4ee3e51e9d57729940 refs/pull/92/head
f1e303fba8d25e4d227409937b9d817f4c39fdcf refs/pull/92/merge
cc9ff92f2d60a15c8cd6b07c69432ab3dd44ddb1 refs/pull/93/head
```

The cleanup removed these paths from all branch history:

```text
ubuntu@www.sku-graduation.p-e.kr
src/main/resources/application.properties
teamPost
```

Please purge any cached PR views, PR refs, and internal references that still expose or reference the sensitive commit/path, especially PR 92 and PR 93.

Thank you.

## Evidence Commands

After branch cleanup:

```powershell
git ls-remote origin 'refs/pull/92/head' 'refs/pull/92/merge' 'refs/pull/93/head'
.\scripts\security\scan-sensitive-history.ps1
```
