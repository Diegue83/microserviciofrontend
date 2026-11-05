# Práctica 3, Parte 2: Pruebas, seguridad y pipelines

**Materia:** Desarrollo e Integración Continua
**Microservicios:** `Ejercicio5MyAPIGestor` (backend, API REST), `Ejercicio5Cliente` (frontend, Thymeleaf) y la base de datos `dbgestor` (MariaDB)
**Repositorios:**
- Backend: https://github.com/Diegue83/microserviciobackend
- Frontend: https://github.com/Diegue83/microserviciofrontend

**Evidencias:** carpeta `Practica3-Parte2-Evidencias`. Cada sección indica la subcarpeta correspondiente, y `README.md` es el índice.

---

## 1. Objetivo

Sobre los microservicios migrados a Java 21 en la Parte 1:

- escribir pruebas automatizadas completas y medir su cobertura con JaCoCo;
- analizar el código con SonarQube y Semgrep, excluyendo `target`;
- clasificar y atender las incidencias reales;
- dejar funcionando **tres pipelines** en Jenkins (frontend, backend y base de datos), priorizando la calidad y el análisis de código.

## 2. Entorno de integración continua

Jenkins y SonarQube corren en Docker con `docker-compose.yml` (`05-infraestructura`).

| Servicio | Imagen | Puerto | Detalles |
|---|---|---|---|
| Jenkins 2.580.2 | `jenkins-jdk21-docker`, propia, basada en `jenkins/jenkins:lts-jdk21` | 8090 | Trae el cliente de Docker para ejecutar Semgrep, SQLFluff y MariaDB, y los plugins de Pipeline, Git y SonarQube |
| SonarQube Community 26.10 | `sonarqube:community` | 9000 | Base embebida (uso de evaluación) |

**Configuración en Jenkins** (`02-jenkins`, capturas `04` a `06`):

| Elemento | Valor |
|---|---|
| JDK | `JDK21`, con `/opt/java/openjdk` (OpenJDK 21.0.13) |
| Maven | `Maven 3.9.0`, con instalación automática |
| Servidor SonarQube | `SonnarServer` en `http://host.docker.internal:9000`, con el token guardado como credencial *Secret text* `sonar-token` (nunca en el código) |
| Jobs | `MicroservicioBack`, `MicroservicioFront` y `MicroservicioBD`, definidos como **Pipeline script en línea** |

**Ajustes de infraestructura necesarios:**
- **Acceso de Jenkins al socket de Docker.** El usuario `jenkins` se agregó al grupo del socket (`group_add: "0"`), porque Docker Desktop lo crea como `root:root 660`.
- **Semgrep montaba una carpeta vacía.** El script de clase monta `-v "${WORKSPACE}":/src`, pero esa ruta existe dentro de Jenkins, no donde Docker la busca. Se resolvió haciendo que `JENKINS_HOME` sea la misma ruta que el volumen tiene para Docker (`/var/lib/docker/volumes/jenkins_home/_data`). Así el script de clase funciona sin cambios.

## 3. Pruebas automatizadas

Ejecutadas por **maven-surefire-plugin 3.5.3** con JUnit 5. Evidencia en `01-pruebas-y-cobertura/surefire-*`.

### 3.1 Backend: 41 pruebas

| Clase de prueba | N.º | Qué valida |
|---|---|---|
| `ModelosTest` | 5 | Constructores, getters y setters de las 5 entidades JPA |
| `UsuarioControllerTest` | 6 | Login correcto; login incorrecto con la marca `"000"`; búsqueda existente e inexistente; alta, actualización y baja; listado |
| `ProyectosControllerTest` | 4 | Búsqueda existente e inexistente; alta, actualización y baja; listado |
| `TareasControllerTest` | 5 | Tareas de un proyecto; búsqueda existente e inexistente; alta, actualización y baja; listado |
| `AsignacionesControllerTest` | 4 | Igual que proyectos |
| `ComentariosControllerTest` | 5 | Comentarios de una tarea; búsqueda; alta, actualización y baja; listado |
| `ValidacionNulosTest` | 5 | Alta, actualización y baja con cuerpo nulo: devuelven `0` y no tocan el repositorio |
| `DtoTest` | 5 | Cada DTO lee el JSON con los nombres originales (`id_usuario`, etc.) y produce la entidad correcta |
| `Ejercicio5MyApiGestorApplicationTests` | 2 | Arranque del contexto (H2 en memoria) y `main()` |

**Técnica:**
- Los controladores se prueban con `@WebMvcTest` y `MockMvc`. Las peticiones HTTP son reales y se verifican el JSON de respuesta (`jsonPath`) y el código de estado.
- Los repositorios se simulan con `@MockitoBean` y se comprueba con `verify` que se llamó a `save` o `delete`. Así las pruebas no dependen de MariaDB y corren en Jenkins.
- Para el arranque del contexto se usa **H2 en memoria** (`src/test/resources/application.properties`, modo MariaDB).

### 3.2 Frontend: 31 pruebas

| Clase de prueba | N.º | Qué valida |
|---|---|---|
| `GestorControllerTest` | 24 | Cada endpoint de `GestorController` contra una **API simulada** (`HttpServer` en el puerto 8081) que responde como el backend: casos de éxito, error 500 y 404, usuario inexistente, campos vacíos del formulario de tarea enviados como `null`, vistas y redirecciones. También comprueba el cuerpo que el front envía a la API |
| `ModelosTest` | 5 | Los 5 modelos |
| `Ejercicio5ClienteApplicationTests` | 2 | Arranque del contexto y `main()` |

### 3.3 Cobertura no es lo mismo que calidad de las aserciones

Cada prueba verifica un resultado observable: el valor devuelto, la vista, el JSON, el cuerpo enviado a la API o la llamada al repositorio. No basta con ejecutar la línea. Por ejemplo, en el formulario de tarea se comprueba que una fecha vacía llega a la API como `"fecha_vencimiento":null`, y en el login fallido que el nombre es `"000"` y el id es nulo.

## 4. Cobertura con JaCoCo

**jacoco-maven-plugin 0.8.13**: `prepare-agent` junto con `report` en la fase `test`. Genera `target/site/jacoco/jacoco.xml`, que el pipeline le pasa a SonarQube (`-Dsonar.coverage.jacoco.xmlReportPaths`).

| Servicio | Líneas | Ramas | Métodos | Clases |
|---|---|---|---|---|
| Backend | **100 %** (289/289) | **100 %** (42/42) | **100 %** (113/113) | 16/16 |
| Frontend | **100 %** (282/282) | **100 %** (14/14) | **100 %** (96/96) | 7/7 |

- **Exclusiones:** ninguna. Se mide todo el código de `src/main`, incluida la clase con `main()`.
- **Evolución del backend:** 5.9 % (solo `contextLoads`), luego 100 % de líneas con 64 % de ramas, y finalmente 100 % en todo. Las 15 ramas restantes eran el código muerto de `if(!x.equals(null))`. Al corregirlo (sección 6), su rama "falsa" se volvió alcanzable y la cubre `ValidacionNulosTest`.
- **Evidencia:** `01-pruebas-y-cobertura` (HTML navegable, XML y CSV) y en SonarQube las capturas `16` a `18`.

## 5. Escaneo de vulnerabilidades

| Herramienta | Alcance | Exclusión de `target` |
|---|---|---|
| **Semgrep** (`--config=auto`), en contenedor | Código fuente y plantillas de cada repo; `db/` en el pipeline de BD | `--exclude=target`. El código fuente no se excluye |
| **SonarQube** | Java, XML y plantillas | SonarQube analiza las fuentes; `target` solo se usa para leer los reportes de pruebas y JaCoCo |

**Resultados iniciales:**
- **Backend:** Semgrep 0 hallazgos; SonarQube 38 incidencias.
- **Frontend:** Semgrep **20** hallazgos; SonarQube 0 incidencias.
- **Base de datos:** Semgrep 0 hallazgos. El análisis del esquema dio hallazgos de diseño.

Reportes en `04-semgrep` y `03-sonarqube`.

## 6. Clasificación y atención de incidencias

El detalle completo está en `09-incidencias/clasificacion-y-atencion.md`.

| Origen | Incidencia | Cant. | Clasificación | Atención |
|---|---|---|---|---|
| SonarQube backend | `java:S2159`: `if(!x.equals(null))` | 15 | Real (bug) | Cambiado por `if (x != null)` y probado con `ValidacionNulosTest` |
| SonarQube backend | `java:S6813`: `@Autowired` en campo | 5 | Real | Inyección por constructor con campo `final` |
| SonarQube backend | `java:S4684`: entidad JPA en `@RequestBody` (*mass assignment*) | 18 | Real (vulnerabilidad) | DTOs (`record`) con `@JsonProperty`; el contrato JSON no cambia |
| Semgrep frontend | `spring-http-request`: URL de la API fija en el código | 18 | Real (configuración) | La URL se lee de `api.url` / `API_URL`; en Docker o Kubernetes puede ser HTTPS |
| Semgrep frontend | `missing-integrity`: CSS de CDN sin SRI | 2 | Real | `integrity="sha384-…"` (hash calculado del archivo publicado) y `crossorigin` |
| Esquema BD | Sin llaves foráneas; contraseña en texto plano; tablas en `latin1` | — | Real, documentado | Requiere migrar datos; queda como mejora con la solución propuesta |
| Esquema BD | `hibernate_sequence` sin llave primaria | 1 | Falso positivo | Es la tabla de secuencia de Hibernate, con una sola fila |
| SQLFluff | 89 avisos de estilo | 89 | Estilo, no bloqueante | El formato lo genera `mysqldump`; solo un error de análisis detiene el pipeline |

**Commits:**
- Backend `6ec50c0`: https://github.com/Diegue83/microserviciobackend/commit/6ec50c0
- Frontend `c285784`: https://github.com/Diegue83/microserviciofrontend/commit/c285784

**Resultado** (`09-incidencias/resultado-despues.md`, capturas `16` a `19`):

| | Antes | Después |
|---|---|---|
| Backend: incidencias de SonarQube | 38 (Reliability C, Security D) | **0** (A / A / A) |
| Frontend: hallazgos de Semgrep | 20 | **0** |
| Quality Gate | OK | **OK** en ambos |

## 7. Los tres pipelines

### 7.1 Backend y frontend

Usan el **mismo script**, basado en el de clase, y solo cambian las rutas: el repositorio de *Checkout* y la clave del proyecto en SonarQube.

| Etapa | Qué hace |
|---|---|
| Tool Install | Prepara `JDK21` y `Maven 3.9.0` |
| Checkout | `git branch: 'main'` del repo del microservicio |
| Ver Directorio | `ls -la`, `java -version` y `mvn -version` (evidencia de Java 21) |
| Build & Test | `mvn clean package -Dsurefire.useFile=false`: compila, ejecuta las pruebas y genera JaCoCo |
| SonarQube Analysis | `sonar-maven-plugin:5.5.0.6356:sonar` dentro de `withSonarQubeEnv('SonnarServer')`, con el reporte de JaCoCo |
| Security Scan (SAST): Semgrep | `docker run semgrep/semgrep semgrep scan --config=auto --exclude=target --json`; el reporte se archiva |

Respecto al script de clase se agregaron `jdk 'JDK21'`, `mvn -version` y `--exclude=target`, y se quitó `dir('Ejercicio5Cliente')` porque cada repo contiene solo su microservicio.

### 7.2 Base de datos (`MicroservicioBD`)

Usa el script `db/dump-dbgestor-202406261453.sql` del repo del backend. Evidencia en `08-base-de-datos` y en las capturas `09` y `13`.

| Etapa | Qué hace | Criterio de fallo |
|---|---|---|
| Checkout | Repo del backend | — |
| Validar script en MariaDB | Contenedor temporal `mariadb:11.4` que carga el script | Falla si no se crean las **6 tablas** |
| Análisis del esquema | Consultas a `information_schema` → `analisis-esquema.txt` (tablas sin PK, llaves foráneas, índices `*FK` sin restricción, columnas de contraseña, charset) | Informativo |
| Calidad SQL: SQLFluff | Lint (dialecto MariaDB) del esquema exportado → `sqlfluff.json` / `.txt` | Falla si el esquema no se puede analizar (`PRS`) |
| Security Scan: Semgrep | `semgrep scan --config=auto` sobre `db/` | — |
| Post | Elimina el contenedor temporal y archiva los reportes | — |

El dump original contiene sentencias propias de `mysqldump` (`/*!40101 … */`, `LOCK TABLES`) que SQLFluff no puede leer. Por eso se analiza el **esquema exportado** del MariaDB temporal (`mariadb-dump --no-data --compact`), que además confirma que el script es válido.

### 7.3 Resultados

| Pipeline | Build | Resultado |
|---|---|---|
| MicroservicioBack | #4 (commit `6ec50c0`) | SUCCESS: 41 pruebas, SonarQube 100 % y 0 incidencias, Semgrep 0 |
| MicroservicioFront | #5 (commit `c285784`) | SUCCESS: 31 pruebas, SonarQube 100 %, Semgrep 0 (332 reglas, 75 archivos) |
| MicroservicioBD | #1 | SUCCESS: 6 tablas, esquema analizado, SQLFluff sin errores de análisis, Semgrep 0 |

Los logs de todos los builds, incluidos los fallidos, están en `02-jenkins`. La captura `10` muestra los tres en verde.

## 8. Problemas encontrados y cómo se verificó la solución

| Problema | Causa | Solución | Verificación |
|---|---|---|---|
| El frontend fallaba en *Checkout* ("Timeout after 10 minutes") | El repo pesa ~400 MB (videos y GIF) con una conexión de ~78 KB/s | Se reintentó con mejor conexión. Queda pendiente reducir el peso de los recursos | Front #3, #4 y #5 en SUCCESS |
| Docker Desktop no arrancaba | Un *socket* atascado en `%LOCALAPPDATA%\Docker\run`. Se eligió "Reset to factory defaults" y se perdieron los volúmenes | Se renombró la carpeta `run` y se reconstruyó todo con `docker compose` | Contenedores arriba y reconfigurados |
| Semgrep reportaba 0 archivos | `-v ${WORKSPACE}` montaba una ruta inexistente para Docker | `JENKINS_HOME` igual a la ruta real del volumen | Prueba con un archivo de control; Semgrep escanea 26 y 75 archivos |
| Back #3 falló en SonarQube | Dos pipelines a la vez saturaron la RAM (7.8 GB en el equipo, 3.7 GB para Docker) y SonarQube se reinició | Los pipelines se ejecutan de uno en uno | Back #4 en SUCCESS |
| SQLFluff no analizaba el dump | Sintaxis propia de `mysqldump` | Análisis del esquema exportado | Pipeline BD #1: 0 errores `PRS` |
| `mvnw` no funcionaba (Parte 1) | Faltaba `.mvn/wrapper` | Se agregó `maven-wrapper.properties` | Builds locales con `mvnw` |

## 9. Cumplimiento del instrumento (Parte 2)

| Criterio | Evidencia |
|---|---|
| Pruebas automatizadas completas de métodos y casos relevantes | Sección 3; `01-pruebas-y-cobertura/surefire-*`; 41 + 31 pruebas |
| Reporte JaCoCo y evidencias verificables de cobertura | Sección 4; `01-pruebas-y-cobertura`; capturas `16` a `18`; 100 % de métodos evaluables, sin exclusiones |
| Escaneo de vulnerabilidades excluyendo carpetas `target` | Sección 5; `04-semgrep` (`--exclude=target`) |
| Clasificación y atención de incidencias reales, con evidencia | Sección 6; `09-incidencias`; commits `6ec50c0` y `c285784`; capturas `14` y `19` |
| Tres pipelines funcionales: frontend, backend y base de datos | Sección 7; `02-jenkins`, `08-base-de-datos`; captura `10` |
| Video de ejecución de pipelines y explicación del aprendizaje | `10-video/practica3-pipelines.mp4` y `guion-video.md` (la narración la agrega el alumno) |
