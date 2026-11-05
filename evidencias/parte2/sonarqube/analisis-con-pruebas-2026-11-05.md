# SonarQube: análisis con las pruebas nuevas

- **Fecha:** 2026-11-05
- **Builds:** `MicroservicioBack` #2 (commit `9354efa`) y `MicroservicioFront` #4 (commit `188925b`), ambos en SUCCESS
- **Fuente:** API de SonarQube `/api/measures/component` y `/api/issues/search`

| Métrica | Backend (Ejercicio5MyAPIGestor) | Frontend (Ejercicio5Cliente) |
|---|---|---|
| Quality Gate | OK | OK |
| **Cobertura total** | **95.2 %** (antes 5.1 %) | **100.0 %** |
| Cobertura de líneas | 100.0 % | 100.0 % |
| Cobertura de ramas | 64.3 % | 100.0 % |
| Pruebas | 31 | 31 |
| Bugs | 15 (Reliability C) | 0 (A) |
| Vulnerabilidades | 18 (Security D) | 0 (A) |
| Code smells | 5 (A) | 0 (A) |
| Security hotspots | 0 | 0 |
| Duplicación | 0.0 % | 0.0 % |
| Líneas de código | 791 | 621 |

**Incidencias abiertas en el backend: 38.** Son las mismas del análisis inicial: `java:S4684` ×18, `java:S2159` ×15 y `java:S6813` ×5. Las pruebas suben la cobertura pero no cambian el código, así que las incidencias siguen. Se atienden en el paso de corrección.

**Frontend:** 0 incidencias.

La cobertura de ramas del backend (64.3 %) está limitada por las 15 ramas inalcanzables de `if(!x.equals(null))`, que son exactamente los 15 bugs `java:S2159`. Al corregirlos, ese porcentaje debería subir.
