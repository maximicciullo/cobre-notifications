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

---

_(Este archivo se sigue actualizando a medida que avanza el desarrollo: implementación del
código Task 2, y análisis de seguridad Task 3.)_
