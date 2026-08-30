# Historial de uso de IA

Registro del uso de IA (Claude Code, modelo Claude Sonnet 5) durante el desarrollo del
challenge de Cobre, tal como lo pide el enunciado ("please document in detail — prompts,
screenshots, etc. — in what use cases you have used [AI]").

Cada entrada resume: contexto/tarea, prompt (resumido), qué generó la IA, y cómo se usó el
resultado (aceptado tal cual / modificado / descartado).

---

## Sesión 1 — 2026-08-29

### 1. Extracción de requisitos del challenge
- **Tarea**: entender el PDF del challenge y dejar un checklist claro para armar el plan de trabajo.
- **Prompt (resumen)**: "Creá un md file NOTES a partir del PDF del challenge donde identifiques
  claramente los puntos a realizar para armar un plan de trabajo paso a paso."
- **Resultado de la IA**: lectura del PDF (`challenge/Sr_Software_Engineer_Case_-_Notifications...pdf`)
  y generación de `NOTES.md` con checklist de requisitos (delivery de notificaciones, API
  self-service, Task 1/2/3, entregables).
- **Uso dado**: aceptado tal cual como base para todo el planeamiento posterior.

### 2. Propuesta de opciones de diseño y stack tecnológico
- **Tarea**: definir arquitectura y tecnologías dado un timebox muy ajustado (2 días, entrega
  al día siguiente).
- **Prompt (resumen)**: "Definime 3 posibles diseños y tecnologías explicando pros y contras
  para poder definir alguno, considerando que solo queda hoy y mañana."
- **Resultado de la IA**: 3 opciones comparadas (A: monolito pragmático, B: diseño cloud-native
  ambicioso + implementación simple, C: event-driven real con Kafka), con pros/contras y
  recomendación razonada por riesgo/tiempo disponible.
- **Uso dado**: el usuario eligió la **Opción B** (híbrido) como base de la solución.

### 3. Refinamiento punto por punto de la Opción B
- **Tarea**: validar y ajustar cada decisión de la Opción B antes de empezar a implementar.
- **Prompt (resumen)**: iteración de varias rondas ajustando: selección final de vulnerabilidades
  OWASP (simplicidad de implementación), confirmación de Postgres como storage, definición de
  dónde queda configurada la URL del webhook, alcance de testing (unitarios en services y
  controllers + adapters), pedido de ejemplo narrado paso a paso para el diagrama de secuencia,
  elección de versión de Java (17 vs 21 vs 25) y pedido de un índice de toda la documentación.
- **Resultado de la IA**: selección final de OWASP (A01 Broken Access Control, A03 Injection,
  A10 SSRF), confirmación de Java 21 con justificación (LTS + virtual threads, relevante para
  el worker de delivery con múltiples llamadas HTTP concurrentes), definición de estructura de
  documentación (`NOTES.md`, `DESIGN.md`, `SECURITY.md`, `AI_USAGE.md`, `README.md`).
- **Uso dado**: aceptado; sirve de base para `DESIGN.md` y `SECURITY.md`, que se están
  construyendo de forma iterativa junto con el usuario.

### 4. Construcción iterativa de `DESIGN.md` (Task 1) con visor de diagramas
- **Tarea**: redactar las 5 secciones de `DESIGN.md` (Contexto, Contenedores, secuencia de
  delivery + retry, modelo de datos, decisiones/trade-offs) validando cada diagrama antes de
  avanzar a la siguiente, según lo pedido por el enunciado ("be ready to discuss any
  architectural decision").
- **Prompt (resumen)**: iteración sección por sección — pedido de mostrar los diagramas en un
  lugar visual para validarlos (no solo como código Mermaid en el archivo), corrección de un
  diagrama `C4Context` que renderizaba con texto superpuesto/ilegible, separación del sequence
  diagram combinado en dos diagramas independientes (Scenario A / Scenario B) por ser
  demasiado denso, pedido de una narrativa tipo "cuento" explicando cada escenario en lenguaje
  simple, y luego pedido de que esa narrativa se correspondiera número a número con las
  flechas del diagrama y con los nombres reales de los componentes.
- **Resultado de la IA**:
  - Se armó un pipeline local (`@mermaid-js/mermaid-cli`, sin servicios externos) que extrae
    los bloques `mermaid` de `DESIGN.md`, los renderiza en modo claro/oscuro, y los publica como
    Artifact (página HTML) para revisión visual iterativa con el mismo link estable.
  - Se detectó y corrigió que el tipo de diagrama `C4Context` y `erDiagram` de Mermaid
    renderizan mal en este entorno (labels superpuestos en el primero, texto invisible en
    filas alternadas en el segundo) — se reemplazaron por `graph`/`sequenceDiagram` estilizados
    manualmente, que sí renderizan de forma confiable, y se dejó esa regla fija para el resto
    del documento.
  - Se escribió una tabla de "elenco" (personaje de la historia ↔ componente real del sistema)
    y se numeraron los pasos de cada narrativa para que coincidan exactamente con la
    numeración automática (`autonumber`) de cada sequence diagram.
- **Uso dado**: aceptado con ida y vuelta iterativo (varias correcciones visuales pedidas
  explícitamente por el usuario antes de aprobar cada sección); el archivo `DESIGN.md`
  resultante es el entregable final de Task 1.

### 5. Repo de git + exportación a PDF
- **Tarea**: dejar un repo de git local listo (sin publicar) y generar una versión exportable
  en PDF de la documentación de diseño, reutilizando lo ya construido.
- **Prompt (resumen)**: "andá viendo cómo exportamos esto como entregable"; el usuario eligió
  GitHub (repo con los `.md`, sin exportar nada extra) + un PDF standalone como bonus; pidió
  explícitamente usar su perfil personal de GitHub (no el de Invenco, que comparte la misma
  máquina) y aclaró más de una vez que nunca se debe hacer `git push` sin que él lo pida.
- **Resultado de la IA**: se validó el `.gitconfig`/`.ssh/config` para confirmar que el
  directorio del challenge no cae bajo el `includeIf` del perfil de Invenco antes de hacer
  cualquier commit; se hizo `git init` + primer commit local (sin remoto, sin push). Se armó un
  pipeline (`marked` + los SVGs ya renderizados de Mermaid + Chrome headless
  `--print-to-pdf`) que convierte `DESIGN.md` + `SECURITY.md` a un PDF con los diagramas
  embebidos, regenerable en cualquier momento.
- **Uso dado**: aceptado. El PDF se comparte por fuera del chat (vía archivo local) porque el
  link de descarga inicial no era accesible para el usuario.

### 6. Implementación completa de Task 2 (Spring Boot, hexagonal)
- **Tarea**: implementar el mecanismo de delivery vía webhook (con retry) y los 3 endpoints
  self-service (`GET /notification_events`, `GET /{id}`, `POST /{id}/replay`) en arquitectura
  hexagonal, usando `notification_events.json` como semilla, más las 3 mitigaciones OWASP de
  `SECURITY.md` implementadas en código real (no solo documentadas).
- **Prompt (resumen)**: "arranca con el scaffold" — a partir de ahí, generación end-to-end del
  proyecto (dominio, puertos, servicios, adapters de persistencia/web/webhook/scheduler,
  migraciones Flyway, seeding, Docker) con verificación real en cada paso (compilar, levantar
  la app contra Postgres, probar los endpoints con curl, correr el flujo completo de replay,
  correr el stack entero con `docker compose up --build`).
- **Resultado de la IA / problemas reales encontrados y corregidos**:
  - Spring Initializr ya no ofrece Spring Boot 3 (línea actual 4.1.1) — se armó el `pom.xml` a
    mano fijando **Spring Boot 3.5.3** (última versión 3.x real en Maven Central), decisión
    consultada y confirmada con el usuario por el riesgo de usar una versión más nueva que mi
    conocimiento sólido.
  - Lombok no generaba los getters/setters en compilación: faltaba declarar
    `annotationProcessorPaths` explícito en el `maven-compiler-plugin` — corregido.
  - La query nativa de Postgres para el listado fallaba (`could not determine data type of
    parameter $2`) cuando un filtro venía `null` — Postgres no puede inferir el tipo de un
    parámetro solo comparado contra `NULL`; se agregaron casts explícitos (`CAST(:x AS
    timestamptz)`, etc.).
  - Se estandarizó toda la API en `snake_case` (vía `spring.jackson.property-naming-strategy`)
    para ser consistente con el `notification_events.json` provisto y con el nombre del
    endpoint (`/notification_events`).
  - El validador SSRF (A10) se probó primero manualmente con curl contra el placeholder
    `example.com` (falló por DNS, no por el guard) y después con tests unitarios usando IPs
    literales (127.0.0.1, 169.254.169.254, rangos RFC1918) para no depender de red real.
  - `docker compose up --build` chocó con el puerto 8080 ya ocupado por otro proceso local
    (ajeno al challenge) — se remapeó a `8082:8080` en `docker-compose.yml`.
- **Uso dado**: aceptado. Los 31 tests unitarios pasan, el stack completo levanta con
  `docker compose up --build`, y se verificó manualmente el flujo completo (delivery exitoso,
  timeout → retry → dead-letter → replay → 202/409/404) contra la base de datos real.

### 7. Documentación Swagger/OpenAPI
- **Tarea**: agregar documentación interactiva de los 3 endpoints.
- **Prompt (resumen)**: "hagamos documentación swagger para los endpoints que tenemos, agregalo
  también en el readme".
- **Resultado de la IA**: se agregó `springdoc-openapi` (genera el spec directo del código,
  nunca se desactualiza), se anotó el controller con `@Operation`/`@ApiResponses`, se configuró
  el esquema de seguridad `X-Api-Key`, y se validó con `docker compose up --build` real
  (Swagger UI y `/v3/api-docs` respondiendo 200 con los 3 paths esperados).
- **Uso dado**: aceptado. Sección nueva en `README.md` con los links y cómo autorizar.

### 8. Revisión de código: dependencias, comentarios, SOLID y cobertura
- **Tarea**: revisión pedida explícitamente por el usuario con 4 puntos: (1) solo Java/Spring
  Boot, sin libs externas innecesarias, (2) eliminar comentarios que se notaran generados por
  IA, (3) seguir lineamientos SOLID/buenas prácticas, (4) reportar la cobertura real de tests.
- **Prompt (resumen)**: "hagamos una revisión del código" con los 4 puntos listados arriba.
- **Resultado de la IA**:
  - Se auditaron las dependencias con grep de uso real: **Lombok**, **H2** y
    **spring-boot-starter-validation** no se usaban en ningún lado (H2 y validation ni
    siquiera estaban referenciados; Lombok solo en 3 entidades) — se sacaron. **WireMock**
    se reemplazó por `com.sun.net.httpserver.HttpServer` (viene con el JDK) en el test del
    adapter de webhook. Se dejó `springdoc-openapi` como única dependencia no-Spring, por ser
    la que el usuario pidió explícitamente en el paso anterior.
  - Se revisaron todos los comentarios de `src/main`: casi todos referenciaban `SECURITY.md`,
    `DESIGN.md §N` o códigos OWASP (A01/A03/A10) — un patrón típico de comentario "generado"
    en vez de un comentario orgánico de código. Se reescribieron conservando el "por qué"
    quedaba (ej: "404 y no 403 para no confirmar que el evento de otro cliente existe") pero
    sacando las referencias a documentos externos, incluyendo las descripciones de Swagger
    (que además no deberían apuntar a archivos internos del repo para consumidores reales
    de la API).
  - Se encontró y corrigió un code smell real en `DeliveryAttempt.restore()`: llamaba al
    constructor público (que fija `status=PENDING`) y después pisaba esos valores — se
    reemplazó por un constructor privado dedicado. Se sacó un `@ExceptionHandler` de
    `MethodArgumentNotValidException` que había quedado como código muerto tras sacar la
    dependencia de validation. Se simplificó un converter a lambda.
  - Se agregó el plugin `jacoco-maven-plugin` y se leyó el reporte real (no estimado):
    cobertura total 52% instrucciones / 48% líneas / 62% branches. Se detectó que
    `application.service` estaba en 12% (solo `ReplayService` tenía test) y se agregaron
    `DeliveryProcessingServiceTest` y `NotificationEventQueryServiceTest`, subiendo esa capa
    a 89%.
- **Uso dado**: aceptado. Se validó con `./mvnw clean test` (39/39 tests) y
  `docker compose up --build` real después de cada tanda de cambios, no solo compilación.

---

_(Este archivo se sigue actualizando a medida que avanza el desarrollo.)_
