# Evidencias: microserviciofrontend (Ejercicio5Cliente)

Práctica 3 · Desarrollo e Integración Continua. Los documentos técnicos completos están en `documentos/`. Las evidencias del pipeline de base de datos y de la infraestructura están en el repo `microserviciobackend`, en su carpeta `evidencias/`.

## parte1/: Actualización a Java 21

| Archivo | Qué demuestra |
|---|---|
| `pom-ORIGINAL.xml` / `pom-ACTUALIZADO.xml` | POM antes (Spring Boot 3.2.5, Java 17) y después (3.5.16, Java 21, Surefire 3.5.3, JaCoCo 0.8.13, Sonar 5.5.0.6356) |
| `cambios-migracion-java21.diff` | Cambios de la migración |
| `build-local-java21.log` | Compilación y pruebas locales con JDK 21 |
| `jenkins-java-version-21.txt` | Jenkins ejecutando con `openjdk 21.0.13` y Maven 3.9.0 |

## parte2/: Pruebas, seguridad y pipeline

| Carpeta | Contenido |
|---|---|
| `pruebas-y-cobertura/` | 31 pruebas (`surefire/`), `jacoco.xml`, reporte HTML de JaCoCo (`reporte-jacoco-html.zip`) y resumen: **100 %** de líneas, ramas y métodos |
| `jenkins/` | Logs de todos los builds de `MicroservicioFront`: #1 y #2 fallaron en Checkout por el tamaño del repo, #3 a #5 en SUCCESS. Configuración del job y de las herramientas |
| `sonarqube/` | Análisis inicial y análisis con las pruebas: 100 %, 0 incidencias |
| `semgrep/` | Antes: 20 hallazgos (18 `spring-http-request` y 2 `missing-integrity`). Después: **0** |
| `incidencias/` | Clasificación y atención de cada hallazgo, y el resultado |
| `capturas/` | Panel de Jenkins, error del checkout, Tools, pipeline en verde y SonarQube después |

## Commits

| Commit | Cambio |
|---|---|
| `ba748cd` | Estado original |
| `37a3d44` | Migración a Java 21 y Spring Boot 3.5.16 |
| `188925b` | Pruebas unitarias (cobertura 100 %) |
| `c285784` | Atención de hallazgos de Semgrep (URL configurable y SRI) |
