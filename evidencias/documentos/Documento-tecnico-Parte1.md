# Práctica 3 — Parte 1: Actualización tecnológica

**Materia:** Desarrollo e Integración Continua
**Microservicios:** `Ejercicio5MyAPIGestor` (backend, API REST) y `Ejercicio5Cliente` (frontend, Thymeleaf)

## 1. Objetivo

Migrar ambos microservicios a **Java 21**, separarlos en dos repositorios independientes y prepararlos para
compilarse, probarse y analizarse en Jenkins con Maven, JaCoCo, Surefire y SonarQube.

## 2. Situación inicial

| | Ejercicio5MyAPIGestor | Ejercicio5Cliente |
|---|---|---|
| Spring Boot | 2.7.17 (sin soporte) | 3.2.5 |
| Java | 11 | 17 |
| Repositorio | Carpeta dentro de `admin-proyectos` | Carpeta dentro de `admin-proyectos` |
| Plugins de calidad | Ninguno | Ninguno |
| Maven Wrapper | `mvnw` sin carpeta `.mvn` (no funcionaba) | Igual |
| Pruebas | `contextLoads` (requería MariaDB encendida) | `contextLoads` + `GestorControllerTest` |

## 3. Separación en dos repositorios

Cada microservicio se copió del monorepo `admin-proyectos` a su propio repositorio Git
(sin `target/` ni `.idea/`). Cada repositorio tiene dos commits:

1. **Estado original**: el código tal como estaba (POM original).
2. **Migración a Java 21**: todos los cambios de esta práctica.

Así el historial de Git sirve como evidencia del antes y el después (`git diff HEAD~1 HEAD`).

## 4. Cambios en los POM

Archivos de evidencia en `poms/`:

| Microservicio | Original | Actualizado |
|---|---|---|
| Backend | `Ejercicio5MyAPIGestor-pom-ORIGINAL.xml` | `Ejercicio5MyAPIGestor-pom-ACTUALIZADO.xml` |
| Frontend | `Ejercicio5Cliente-pom-ORIGINAL.xml` | `Ejercicio5Cliente-pom-ACTUALIZADO.xml` |

| Elemento | Antes | Después | Justificación |
|---|---|---|---|
| `spring-boot-starter-parent` | 2.7.17 / 3.2.5 | **3.5.16** en ambos | Spring Boot 2.7 terminó su soporte y no está certificado para Java 21. La versión 3.5 soporta Java 21 y unifica ambos servicios en la misma pila. |
| `java.version` | 11 / 17 | **21** | Requisito de la práctica (Java 21 LTS). |
| `maven-surefire-plugin` | Versión implícita | **3.5.3** explícita | Ejecuta las pruebas JUnit 5 en Java 21. Su `argLine` incluye `@{argLine}` para que el agente de JaCoCo se inyecte, y `-XX:+EnableDynamicAgentLoading` para evitar la advertencia de Java 21 al cargar agentes (Mockito/Byte Buddy). |
| `jacoco-maven-plugin` | No existía | **0.8.13** | Compatible con el bytecode de Java 21. Con `prepare-agent` + `report` en la fase `test` genera `target/site/jacoco/jacoco.xml`, que es la ruta que lee SonarQube en el pipeline. |
| `sonar-maven-plugin` | No existía | **5.5.0.6356** | Plugin obligatorio de la práctica, usado en la etapa *SonarQube Analysis*. |
| `h2` (solo backend, `test`) | No existía | Gestionada por Spring Boot | El test `contextLoads` necesitaba MariaDB encendida. En Jenkins no hay base de datos, así que las pruebas usan H2 en memoria (modo MariaDB). |

Las versiones de los plugins se declaran como propiedades (`jacoco.version`, `surefire.version`,
`sonar.plugin.version`) para actualizarlas en un solo lugar.

## 5. Cambios en el código y configuración

### Backend (`Ejercicio5MyAPIGestor`)
- **`javax.persistence` → `jakarta.persistence`** en las 5 entidades (`Usuario`, `Proyectos`, `Tareas`,
  `Asignaciones`, `Comentarios`). Spring Boot 3 usa Jakarta EE 10; sin este cambio el proyecto no compila.
- **`application.properties`**:
  - La URL, el usuario y la contraseña se leen de variables de entorno (`DB_URL`, `DB_USER`, `DB_PASSWORD`)
    con valores por defecto, para no subir credenciales a GitHub y poder configurarlas en Jenkins, Docker o Kubernetes.
  - `hibernate.id.db_structure_naming_strategy=legacy`: Hibernate 6 cambió la estrategia de IDs
    (`<tabla>_seq`). Con `legacy` se sigue usando la tabla `hibernate_sequence` de la base de datos existente.
  - Se corrigió `spring.datasource.dbcp2.driver-class-name` (propiedad de otro pool) por `spring.datasource.driver-class-name`.
- **`src/test/resources/application.properties`**: configuración de H2 solo para pruebas.
- **`db/dump-dbgestor-202406261453.sql`**: esquema de la base de datos (se usará en el pipeline de base de datos de la Parte 2).

### Frontend (`Ejercicio5Cliente`)
- No requirió cambios de código: ya usaba Spring Boot 3 (Jakarta).

### Ambos
- **Maven Wrapper**: se agregó `.mvn/wrapper/maven-wrapper.properties` (Maven 3.9.9). El script `mvnw` existía,
  pero sin esa carpeta no funcionaba. Ahora se puede compilar sin instalar Maven.
- **`.gitignore`**: excluye `target/`, `.idea/` y `reportes/`.
- **`.gitattributes`**: fuerza saltos de línea LF en `mvnw` para que funcione en el agente Linux de Jenkins.
- **`Jenkinsfile`**: pipeline basado en el script de clase (ver sección 6).

## 6. Configuración de Jenkins

### Herramientas (Manage Jenkins → Tools)
| Herramienta | Nombre en Jenkins | Valor |
|---|---|---|
| JDK | `JDK21` | JDK 21 (Eclipse Temurin 21, instalación automática o `JAVA_HOME` del agente) |
| Maven | `Maven 3.9.0` | Maven 3.9.x (compatible con Java 21) |
| SonarQube Server | `SonnarServer` | `http://host.docker.internal:9000` con su token |

### Pipeline (`Jenkinsfile`)
Respecto al script de clase:
- Se agregó `jdk 'JDK21'` en `tools`, para que Maven compile con Java 21.
- Se eliminó `dir('Ejercicio5Cliente')`, porque cada repositorio ahora contiene solo su microservicio.
- La URL de *Checkout* apunta al repositorio propio de cada microservicio.
- *Ver Directorio* también muestra `mvn -version` (evidencia de que Maven usa Java 21).
- Semgrep excluye `target` (`--exclude=target`) y analiza el código fuente (`.`).

Etapas: Checkout → Ver Directorio → Build & Test → SonarQube Analysis → Security Scan (SAST) - Semgrep.

## 7. Verificación local

Se compiló con `mvnw clean package -Dsurefire.useFile=false` (el mismo comando del pipeline) usando
JDK 21.0.12. Logs completos en `build-logs/` y reportes HTML de JaCoCo en `jacoco/<servicio>/index.html`.

| | Ejercicio5MyAPIGestor | Ejercicio5Cliente |
|---|---|---|
| Compilación | `javac [release 21]` ✔ | `javac [release 21]` ✔ |
| Pruebas (Surefire) | 1 ejecutada, 0 fallos | 11 ejecutadas, 0 fallos |
| Reporte JaCoCo (`jacoco.xml`) | Generado | Generado |
| Cobertura de líneas | 16 / 271 (5.9 %) | 111 / 280 (39.6 %) |
| Resultado | **BUILD SUCCESS** | **BUILD SUCCESS** |

La cobertura del backend es baja porque solo existe la prueba `contextLoads`. Las pruebas de los
controladores, para llegar al 100 % de los métodos evaluables, corresponden a la Parte 2.

### Problemas encontrados y solución
| Problema | Solución |
|---|---|
| `mvnw` fallaba porque faltaba la carpeta `.mvn/wrapper` | Se agregó `maven-wrapper.properties` |
| El backend no compilaba con Spring Boot 3 (`javax.persistence` no existe) | Migración a `jakarta.persistence` |
| `contextLoads` del backend fallaba sin MariaDB | H2 en memoria solo para pruebas |
| Hibernate 6 buscaría secuencias `<tabla>_seq` que no existen en la BD | `db_structure_naming_strategy=legacy` |

## 8. Repositorios

| Microservicio | Repositorio |
|---|---|
| Backend | https://github.com/Diegue83/microserviciobackend |
| Frontend | https://github.com/Diegue83/microserviciofrontend |
