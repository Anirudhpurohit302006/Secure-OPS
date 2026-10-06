# Analytics integration

## Status
The original visualization module (React/Vite + FastAPI) was **not available** when SecureOps V1 was assembled,
so it could not be inspected. Analytics was therefore implemented natively, following the same stack and ideas:

| Existing module (per spec)  | SecureOps V1                                                   |
|-----------------------------|----------------------------------------------------------------|
| React + Vite + Recharts     | Same: `frontend/src/charts/*`, `frontend/src/pages/AnalyticsPage.tsx` |
| FastAPI + Pandas data prep  | Replaced by `AnalyticsService` (Java) reading **PostgreSQL history** |
| Natural-language dataset upload | Not part of V1 (as specified)                              |

## Integration boundary
```
Analytics page -> /api/analytics/*  (Java AnalyticsService) -> PostgreSQL history tables
```
Analytics never opens SSH connections. No Python service is required to run SecureOps.

## If you want to merge the original module later
Drop it into this folder (without `node_modules` / `.venv`) and compare its chart components with
`frontend/src/charts/`. Reusable pieces are the Recharts wrappers; the FastAPI/Pandas part is not needed
because aggregation happens in `AnalyticsService`. Keep chart components presentational (props in, JSX out)
so they can be swapped.
