# Resultado después de atender las incidencias

- **Fecha:** 2026-11-05
- **Builds:** `MicroservicioBack` #4 (commit `6ec50c0`) SUCCESS · `MicroservicioFront` #5 (commit `c285784`) SUCCESS · `MicroservicioBD` #1 SUCCESS
- **Fuente:** API de SonarQube y reportes de Semgrep archivados por Jenkins

| Métrica | Backend antes | **Backend después** | Frontend antes | **Frontend después** |
|---|---|---|---|---|
| Quality Gate | OK | **OK** | OK | **OK** |
| Cobertura total | 5.1 % (luego 95.2 %) | **100.0 %** | 39.6 % (local) | **100.0 %** |
| Cobertura de ramas | 0 % (luego 64.3 %) | **100.0 %** | — | **100.0 %** |
| Pruebas | 1 | **41** | 11 | **31** |
| Bugs | 15 (C) | **0 (A)** | 0 | **0 (A)** |
| Vulnerabilidades | 18 (D) | **0 (A)** | 0 | **0 (A)** |
| Code smells | 5 | **0 (A)** | 0 | **0 (A)** |
| Incidencias abiertas (SonarQube) | 38 | **0** | 0 | **0** |
| Semgrep (excluye `target`) | 0 de 166 reglas | **0** (166 reglas, 31 archivos) | 20 | **0** (332 reglas, 75 archivos) |

Base de datos: Semgrep 0 hallazgos. Los hallazgos de diseño del esquema están clasificados en `clasificacion-y-atencion.md` (puntos 6 a 10).

**Capturas:** `06-capturas/16` a `19` (SonarQube después), `11` y `12` (builds con correcciones), `14` (diff del commit).

**Cómo se verificó:** cada corrección se probó primero en local (`mvnw clean test` con JDK 21 y Semgrep en Docker) y después en Jenkins con el pipeline completo. Las cifras de la tabla salen de esos pipelines, no de la ejecución local.
