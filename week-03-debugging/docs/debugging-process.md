# Week 3 Debugging Record

## Scope

The Week 2 blog API was used as the application under investigation because the supplied Week 3 brief describes a simulated debugging exercise rather than including a separate faulty codebase. Five controlled issues were introduced in this Week 3 copy, then regression checks were run to capture the behavior before and after repair. The Week 2 project itself was left unchanged.

## Findings and fixes

| Area | Reproduced behavior | Cause | Correction |
| --- | --- | --- | --- |
| Post ownership | The owner received `403`; a different signed-in user could update the post. | The owner comparison in `PostService` was inverted. | Deny the update only when the authenticated user does not match the post author. |
| Comment ownership | The owner received `403`; a different signed-in user could update the comment. | The owner comparison in `CommentService` was inverted. | Apply the same ownership guard used for posts. |
| Email uniqueness | Registering an existing email with different capitalization created a second account. | Registration trimmed but did not lowercase email before checking and saving it. | Normalize email with `Locale.ROOT` before lookup and persistence. |
| Missing post comments | Requesting comments for an unknown post returned `200` with an empty list. | The list method queried comments without first checking that the post existed. | Verify the parent post and return the API's `404` error when it is missing. |
| Post-list query count | Listing five posts by five authors issued eight SQL statements. | Each post's author was loaded separately while mapping the response. | Add a JPA entity graph to fetch the author with the post list. |

## Test sequence and result

Seven focused regression tests were run against the controlled defects. All seven exposed the expected behavior: two owner authorization checks failed in each direction, case-insensitive duplicate email registration was accepted, a missing post returned a successful empty comment response, and the five-author list needed eight statements instead of one. The defects were then corrected and the same checks were rerun.

The final Week 3 Maven run completed with 11 tests, 0 failures, 0 errors, and 0 skipped tests. This is 4 existing API integration tests plus 7 new regression tests. Run it from this directory with `./mvnw test` (Windows: `mvnw.cmd test`).

## Regression coverage

`Week3RegressionTests` verifies email normalization, missing-parent behavior, owner updates, rejection of non-owner updates, and a single-query post listing when five authors are present. The query-count check uses Hibernate statistics so the test checks database behavior as well as the HTTP response. The corrected files are `user/UserService.java`, `comment/CommentService.java`, `post/PostService.java`, and `post/PostRepository.java`.

These checks address the reproduced issues in this local exercise. They do not represent a production load test or an external security audit.
