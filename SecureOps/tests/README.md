# Tests

Unit tests live next to the code they test:

- Backend: `backend/src/test/java` (JUnit 5, Mockito, AssertJ) - run `mvn test` in `backend/`. They need **no database and no RHEL**: SSH is a fake `RemoteSession`, repositories are mocks.
- Frontend: `frontend/src/__tests__` (Vitest + Testing Library) - run `npm test` in `frontend/`.

## PostgreSQL integration test (needs a running PostgreSQL)
`MachineRepositoryIT` runs the real repository SQL against PostgreSQL (generated ids, last_seen preservation, append-only metrics,
`ON CONFLICT` de-duplication of auth events, JSONB round-trip, cascade delete). It is **skipped unless** `SECUREOPS_IT_DB_URL` is set.

```powershell
# one-time: a separate test database owned by the application role
psql -U postgres -c "CREATE DATABASE secureops_test OWNER secureops"
$env:PGPASSWORD = "<DB_PASSWORD>"; psql -U secureops -d secureops_test -f database\schema.sql

$env:SECUREOPS_IT_DB_URL      = "jdbc:postgresql://localhost:5432/secureops_test"
$env:SECUREOPS_IT_DB_USER     = "secureops"
$env:SECUREOPS_IT_DB_PASSWORD = "<DB_PASSWORD>"
cd backend; mvn test
```
The test deletes only the machines it creates. Never point it at your real `secureops` database if you want to keep its data tidy.
