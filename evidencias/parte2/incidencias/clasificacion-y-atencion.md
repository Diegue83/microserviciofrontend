# Clasificación y atención de incidencias

Fuentes: **SonarQube** (análisis de código del backend y del frontend), **Semgrep** (SAST, excluye `target`) y el **análisis del esquema** del pipeline de base de datos.

Cada incidencia se clasificó como **real** (se corrige), **falso positivo** (se justifica) o **riesgo aceptado** (se documenta). No se ocultó ninguna con supresiones ni excluyendo código fuente.

## 1. Backend (Ejercicio5MyAPIGestor), SonarQube: 38 incidencias

| # | Regla | Tipo / severidad | Cant. | Clasificación | Problema | Atención (commit `6ec50c0`) |
|---|---|---|---|---|---|---|
| 1 | `java:S2159` | Bug / MAJOR | 15 | **Real** | `if(!x.equals(null))` nunca puede ser `false`: si `x` es `null`, `equals` lanza `NullPointerException` antes de comparar. La validación no servía, y además dejaba 15 ramas de código muerto (JaCoCo: 27/42 ramas). | Cambiado por `if (x != null)` en los 15 métodos de alta, actualización y baja. `ValidacionNulosTest` prueba el caso nulo: devuelve `0` y no toca el repositorio. Ramas: **42/42**. |
| 2 | `java:S6813` | Code smell / MAJOR | 5 | **Real** | Inyección con `@Autowired` en un campo: impide que el campo sea `final` y obliga a usar reflexión para probar el controlador. | Inyección por constructor con campo `private final` en los 5 controladores. |
| 3 | `java:S4684` | Vulnerabilidad / CRITICAL | 18 | **Real** | Los endpoints reciben la **entidad JPA** como `@RequestBody`. Esto permite *mass assignment*: el cliente controla cualquier campo persistente. | Nuevo paquete `dto` con 5 `record` (`UsuarioDTO`, `ProyectoDTO`, `TareaDTO`, `AsignacionDTO`, `ComentarioDTO`) que definen qué campos acepta la API, más `toEntity()`. Los nombres JSON se mantienen con `@JsonProperty`, así que el frontend no cambia. `DtoTest` verifica la conversión. |

Además, en `ProyectosController` se eliminó un bloque de código comentado, y en `TareasController` y `AsignacionesController` los `import` sin uso.

## 2. Frontend (Ejercicio5Cliente), Semgrep: 20 hallazgos

| # | Regla | Severidad | Cant. | Clasificación | Problema | Atención (commit `c285784`) |
|---|---|---|---|---|---|---|
| 4 | `spring-http-request` (insecure-transport) | WARNING | 18 | **Real (configuración)** | La URL de la API (`http://localhost:8081`) estaba escrita 19 veces en `GestorController`. No se podía cambiar a HTTPS ni a otra dirección sin recompilar. | La URL base se lee de `api.url` en `application.properties` (`${API_URL:http://localhost:8081}`) y se inyecta por constructor. En local se sigue usando `http://localhost`, que es el mismo equipo. En Docker o Kubernetes se define `API_URL`, por ejemplo con `https://`. |
| 5 | `missing-integrity` | WARNING | 2 | **Real** | Hojas de estilo de CDN sin *Subresource Integrity*: si el CDN sirviera un archivo alterado, el navegador lo aceptaría. | Se agregó `integrity="sha384-…"` (hash calculado del archivo publicado) y `crossorigin="anonymous"` a Font Awesome 5.13.0 (`home.html`) y Bootstrap Icons 1.11.3 (`proyecto.html`). |

En SonarQube, el frontend tenía **0 incidencias** desde el primer análisis.

## 3. Base de datos (dbgestor), pipeline `MicroservicioBD`

| # | Hallazgo | Fuente | Clasificación | Justificación |
|---|---|---|---|---|
| 6 | 0 llaves foráneas; los índices `asignaciones_*_FK` no imponen la relación | Análisis del esquema | **Real, documentado** | Corregirlo exige migrar datos existentes. Se deja documentado como mejora: `ALTER TABLE ... ADD CONSTRAINT ... FOREIGN KEY`. |
| 7 | `hibernate_sequence` sin llave primaria | Análisis del esquema | **Falso positivo** | Es la tabla de secuencia que genera Hibernate: una sola fila con `next_val`. Así la define Hibernate. |
| 8 | `usuario.contrasena varchar(100)` en texto plano | Análisis del esquema | **Real, riesgo aceptado** | Hay que guardar un *hash* (BCrypt) y cambiar el login, en el backend y en los datos. Queda fuera del alcance de la práctica. |
| 9 | 6 tablas en `latin1` | Análisis del esquema | **Real, documentado** | La migración sugerida es a `utf8mb4`. Se mantiene `latin1` para no alterar los datos existentes. |
| 10 | 89 avisos de estilo (LT01, LT02, LT05, CP01) | SQLFluff | **Estilo, no bloqueante** | El formato lo genera `mysqldump`, no es código escrito a mano. El pipeline solo se detiene si el esquema no se puede analizar. |
| — | 0 hallazgos | Semgrep (`db/`) | — | — |

## 4. Resultado

Ver `resultado-despues.md` en esta misma carpeta.
