# Cómo construimos JobSinton

## Objetivo

Avanzar con cambios pequeños, comprensibles y verificables. El primer commit establece la base documental; los siguientes incrementos seguirán el flujo de issues y pull requests.

## Contribuciones externas

El repositorio es público y su propietario es `Garatech`. Si no eres el propietario, trabaja en un fork y abre una pull request para proponer cambios. La creación de una PR no concede permisos de escritura ni garantiza que se integre. El propietario decide qué incorporar; no hay fusión automática habilitada.

Las herramientas autorizadas por el propietario pueden actuar en su nombre. No se añaden colaboradores externos automáticamente por aceptar una contribución.

## Ciclo de una actualización

1. Definir una tarea con problema, alcance y criterios observables de aceptación.
2. Crear una rama con un propósito, por ejemplo `feat/profile-preferences`, `fix/missing-salary` o `docs/local-setup`.
3. Explicar el diseño y construir conjuntamente la solución.
4. Ejecutar las comprobaciones pertinentes y registrar sus resultados reales.
5. Abrir una PR con motivo, cambios, evidencia y limitaciones.
6. Revisar el cambio y resolver observaciones bloqueantes.
7. Integrar y comprobar el estado resultante de `main`.

Se propone usar commits descriptivos con prefijos `feat`, `fix`, `docs`, `test` y `chore`. Las reglas de protección de ramas y comprobaciones automáticas todavía no están configuradas.

## Evaluación por riesgo

| Tipo de cambio | Evidencia mínima prevista |
| --- | --- |
| Documentación | Exactitud, enlaces internos y ausencia de instrucciones obsoletas |
| Interfaz | Flujo afectado, teclado, tamaños relevantes y estados de error o vacío |
| Afinidad | Casos de referencia, restricciones, datos ausentes, pesos y explicación |
| Persistencia | Integración con base de datos y efecto de la migración |
| Conector | Contrato, paginación, error, timeout y límites sin depender de credenciales en CI |
| Cuentas | Autenticación, autorización y aislamiento entre dos usuarios |
| Recorrido crítico | Prueba de extremo a extremo del comportamiento afectado |

No añadimos pruebas que solo repitan la implementación. No exigimos un porcentaje arbitrario de cobertura. Las pruebas deben proteger comportamientos y riesgos concretos. Los cambios de reglas de afinidad se compararán contra un conjunto estable de ejemplos revisados.

## Criterios de integración

- Se cumplen los criterios de aceptación de la tarea.
- Compilación, análisis estático y pruebas aplicables pasan cuando estén configurados.
- No hay secretos, datos personales de prueba ni regresiones conocidas sin resolver.
- La lógica de dominio es comprensible y está separada del formato de cada proveedor.
- La experiencia contempla entradas inválidas, fallos y datos incompletos relevantes.
- README y documentación reflejan el estado real.
- Los cambios de datos o configuración delicados incluyen un plan de recuperación.
- Se documenta quién revisó y qué fue asistido por IA; una autoevaluación no se presenta como revisión humana independiente.

## Cierre de sesión

Registrar objetivo, decisiones, archivos relevantes, comprobaciones realizadas, limitaciones y siguiente paso. Explicar con palabras propias una decisión del cambio y una alternativa descartada. Si algo no se ha entendido, revisarlo antes de ampliar el alcance.

## Datos y servicios externos

Usar datos ficticios en pruebas y demos. Mantener claves fuera del repositorio. Revisar acceso, cuotas, atribución y retención de cada fuente antes de incorporarla. El presupuesto acordado es 0 €: cualquier cambio que requiera un servicio de pago debe replantearse con el propietario del proyecto.
