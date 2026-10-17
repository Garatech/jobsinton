# JobSinton

**Empleo en sintonía contigo.** Un proyecto de Gara's Workshop.

JobSinton será una aplicación para descubrir y comparar ofertas de empleo tecnológico según habilidades, experiencia y preferencias personales, con una puntuación de afinidad explicable.

> Estado: definición y preparación del repositorio. Todavía no existe una aplicación ejecutable, una integración real con portales ni un despliegue público.

## Índice

- [Objetivo](#objetivo)
- [Decisiones acordadas](#decisiones-acordadas)
- [Primera versión](#primera-versión)
- [Perfil y afinidad](#perfil-y-afinidad)
- [Fuentes de ofertas](#fuentes-de-ofertas)
- [Arquitectura propuesta](#arquitectura-propuesta)
- [Hoja de ruta](#hoja-de-ruta)
- [Desarrollo local](#desarrollo-local)
- [Cómo trabajamos](#cómo-trabajamos)
- [Registro de sesiones](#registro-de-sesiones)

## Objetivo

Ayudar a desarrolladores a priorizar ofertas que encajen con su perfil y sus condiciones de trabajo. El proyecto también forma parte de un portafolio profesional: sus decisiones, limitaciones y comprobaciones deben poder explicarse y reproducirse.

## Decisiones acordadas

| Aspecto | Decisión | Estado |
| --- | --- | --- |
| Nombre | JobSinton | Acordado el 17/10/2026 |
| Portafolio | Gara's Workshop | Acordado |
| Orientación | Backend / full stack Java | Acordado |
| Backend | Java y Spring Boot | Acordado; versiones pendientes |
| Frontend | React y TypeScript | Acordado; versiones pendientes |
| Base de datos | PostgreSQL | Propuesta técnica pendiente de cerrar |
| Editor | Visual Studio Code | Acordado |
| Dedicación | 6–10 horas semanales | Acordado |
| Método | Construcción conjunta paso a paso y documentación continua | Acordado |
| Presupuesto | 0 €; desarrollo local | Acordado |
| Mercado | España inicialmente, preparado para otros países europeos | Acordado |
| Alojamiento | Evaluar una opción gratuita cuando exista una demo | Pendiente |

El nombre se eligió tras una búsqueda preliminar sin coincidencias exactas localizadas. No se han verificado marcas registradas, dominios ni identificadores en servicios de alojamiento.

## Primera versión

Recorrido objetivo: **configurar perfil → consultar ofertas → entender afinidad → guardar oferta**.

- Perfil editable con prioridades y restricciones.
- Catálogo ficticio claramente identificado para desarrollar y demostrar el producto.
- Un proveedor real, sujeto a disponibilidad y condiciones de acceso.
- Búsqueda, filtros, detalle y puntuación explicada.
- Ofertas guardadas; seguimiento de candidaturas en un hito posterior.

Fuera del alcance inicial: envío automático de candidaturas, extracción de CV mediante IA, múltiples proveedores simultáneos, microservicios y servicios de pago. Las cuentas públicas se incorporarán con aislamiento de datos probado; antes, la demo usará datos ficticios.

## Perfil y afinidad

Datos propuestos:

| Dimensión | Ejemplos |
| --- | --- |
| Objetivo | Backend, frontend, full stack, desarrollo SAP o consultoría |
| Competencias | Tecnología, nivel declarado, uso reciente y contexto: trabajo, prácticas o proyecto |
| Experiencia | Responsabilidades y experiencia relevante; requisitos obligatorios y deseables |
| Ubicación | País de residencia, países aceptados por la oferta, ciudades y traslado |
| Modalidad | Remoto, híbrido, presencial y días presenciales máximos |
| Condiciones | Salario mínimo y objetivo, moneda, periodo salarial, jornada, contrato, viajes y guardias |
| Idiomas y formación | Nivel de idioma y formación relevante |
| Prioridades | Obligatorio, preferido o indiferente; pesos configurables |

Principios del motor:

1. Comprobar restricciones obligatorias antes de ordenar por preferencias.
2. Calcular afinidad con reglas deterministas y pesos documentados.
3. Diferenciar dato ausente, incompatibilidad y coincidencia.
4. Mostrar la información disponible y los motivos de la puntuación.
5. Versionar las reglas para poder reproducir resultados.

**La afinidad no representa probabilidad de contratación.** No se inferirán aptitudes a partir de características personales ajenas al puesto.

Pendiente: definir fórmula, tratamiento de datos desconocidos, equivalencias de competencias, desempates y un conjunto pequeño de casos revisados manualmente. No se incorporará IA al cálculo inicial.

Para Europa, separaremos ubicación de la empresa, ubicación del puesto y países admitidos para teletrabajar. Conservaremos moneda, periodicidad y carácter bruto/neto del salario; no compararemos cantidades incompatibles directamente. El diseño permitirá traducciones, sin prometer cobertura de países que los proveedores no ofrezcan.

## Fuentes de ofertas

Revisión preliminar: 17/10/2026. Documentación consultada; ningún acceso autenticado está validado todavía.

| Proveedor | Evidencia | Próximo paso |
| --- | --- | --- |
| InfoJobs | API documentada y credenciales obligatorias | Comprobar alta, búsqueda, cuotas y condiciones |
| LinkedIn | API de publicación restringida; no acredita búsqueda general | No comprometer integración inicial |
| AIApply | Documentación de buscador interno; API pública para este uso no localizada | Confirmar con el proveedor si fuera prioritario |
| Adzuna | API de búsqueda con cuotas y condiciones | Evaluar cobertura y uso permitido |

Referencias:

- [InfoJobs: operaciones](https://developer.infojobs.net/documentation/operation-list/index.xhtml)
- [LinkedIn: Job Posting API](https://learn.microsoft.com/en-us/linkedin/talent/job-postings/api/overview)
- [AIApply: Job Search](https://support.aiapply.co/en/articles/16889656-how-to-use-the-job-search-section)
- [Adzuna: API](https://developer.adzuna.com/overview)
- [Adzuna: condiciones](https://developer.adzuna.com/docs/terms_of_service)

Cada conector deberá contemplar paginación, normalización, atribución, límites de uso, tiempos de espera, errores, deduplicación y caducidad. La demo deberá funcionar con datos ficticios aunque falle el proveedor.

## Arquitectura propuesta

Un repositorio con frontend, backend y documentación. Backend como monolito modular: perfiles, ofertas, conectores, afinidad y candidaturas. Los conectores traducirán sus respuestas a un modelo común, sin introducir detalles del proveedor en el motor de afinidad.

Propuestas pendientes de concretar: PostgreSQL, migraciones versionadas, API REST documentada, Docker Compose y GitHub Actions. Las versiones se fijarán tras comprobar el entorno y la compatibilidad.

## Hoja de ruta

Los hitos tienen criterios de salida, no fechas cerradas. Se estimarán al descomponerlos en tareas pequeñas.

| Hito | Entregable | Criterio de salida | Estado |
| --- | --- | --- | --- |
| 0. Definición | Nombre, alcance, stack y criterios de calidad | Decisiones principales registradas; pendientes identificados | En curso |
| 1. Base | Repositorio remoto, entorno y comprobaciones automáticas | Un clon limpio arranca siguiendo instrucciones verificadas | Pendiente |
| 2. Recorrido inicial | Perfil, catálogo ficticio, detalle y guardado | Funciona el recorrido y sus estados vacíos, de carga y error | Pendiente |
| 3. Afinidad v1 | Restricciones, pesos y explicaciones | Casos revisados cubren incompatibilidades, ausencias y cambios de pesos | Pendiente |
| 4. Fuente real | Un conector y normalización | Acceso autorizado; límites, duplicados y fallos controlados | Pendiente |
| 5. Uso personal | Persistencia, candidaturas y cuentas | Aislamiento entre usuarios y eliminación de datos verificados | Pendiente |
| 6. Demo | Interfaz cuidada y despliegue viable a coste cero | Recorrido principal accesible en móvil y escritorio con datos ficticios | Pendiente |
| 7. Portafolio | Capturas, documentación y explicación técnica | Demo reproducible y decisiones defendibles en entrevista | Pendiente |

## Desarrollo local

Esta entrega contiene únicamente documentación y configuración del espacio de trabajo. No hay comandos de arranque, dependencias ni pruebas de aplicación que ejecutar todavía.

Abre `JobSinton.code-workspace` en VS Code, o abre directamente esta carpeta. `Ctrl+Shift+V` permite previsualizar este README.

Antes del primer código comprobaremos versiones de Java, Node, Git y Docker, elegiremos versiones compatibles y documentaremos los pasos exactos de instalación y arranque. No se requieren claves API para esta fase.

## Cómo trabajamos

Consulta [CONTRIBUTING.md](CONTRIBUTING.md) y la [plantilla de pull request](.github/pull_request_template.md).

Cada sesión: objetivo pequeño → explicación → implementación conjunta → verificación → documentación. Cada actualización debe indicar qué cambió, por qué, cómo se comprobó y qué queda pendiente.

El README será la entrada principal. Cuando una explicación crezca, se trasladará a `docs/` y quedará enlazada aquí. La asistencia de IA se documentará con honestidad; no se atribuirán revisiones independientes ni pruebas que no se hayan realizado.

No se publicarán credenciales, CV reales, perfiles personales de demostración ni copias de ofertas que no podamos redistribuir. La elección de licencia queda pendiente; no se declara una licencia de código abierto en esta entrega.

## Acceso y contribuciones externas

Repositorio público: [Garatech/jobsinton](https://github.com/Garatech/jobsinton).

La escritura y la integración de cambios están reservadas al propietario, `Garatech`, y a las herramientas que autorice. Cualquier otra persona puede trabajar en su propio fork y proponer una pull request; proponer un cambio no lo incorpora al repositorio. No se concederá acceso de colaborador sin una decisión expresa del propietario.

Consulta el [registro de revisión de permisos](docs/repository-access.md). Los ajustes de GitHub se gestionan en el servidor; este documento registra la revisión, pero no sustituye esos controles.

## Registro de sesiones

### 17/10/2026 — Inicio documental

- Elegido JobSinton como nombre.
- Registradas las decisiones de tecnología, dedicación, presupuesto y mercado.
- Preparados el README, los criterios de contribución, una plantilla de PR y el espacio de VS Code.
- Sin aplicación ejecutable, CI, despliegue ni integraciones reales.
- Preparada la vinculación con `Garatech/jobsinton` y autorizada la publicación del primer commit documental.
- Revisados los permisos del repositorio y reforzada la aprobación de workflows de forks externos.
- Siguiente sesión: revisar el entorno y preparar el primer incremento técnico juntos.
