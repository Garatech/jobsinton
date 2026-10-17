# Revisión inicial de permisos del repositorio

Fecha: 17/10/2026. Repositorio: https://github.com/Garatech/jobsinton.

## Objetivo

Mantener el proyecto visible públicamente y reservar los cambios del repositorio original al propietario y sus herramientas autorizadas. Las contribuciones externas se proponen desde forks y se integran únicamente si el propietario lo decide.

## Configuración revisada

| Control | Estado observado o aplicado |
| --- | --- |
| Visibilidad | Pública |
| Propietario | Garatech, cuenta personal |
| Colaboradores | 0; GitHub indica que solo el propietario puede contribuir directamente |
| Claves de despliegue | Ninguna |
| GitHub Apps instaladas | ChatGPT Codex Connector; integración existente del propietario |
| Fusión automática de PR | Desactivada |
| Edición de la wiki | Restringida a colaboradores; con 0 colaboradores, al propietario |
| Permisos predeterminados de GITHUB_TOKEN | Lectura de contenidos y paquetes |
| Crear o aprobar PR mediante GitHub Actions | Desactivado |
| Ejecutar workflows de PR desde forks | Cambiado de aprobación para primeros contribuidores a aprobación para todos los contribuidores externos |

Las PR y las issues permanecen abiertas a propuestas externas. Esto no concede escritura sobre las ramas ni capacidad para integrar cambios.

## Alcance y límites

- Esta revisión se limita a los controles del repositorio indicados arriba; no audita sesiones, tokens personales, credenciales del equipo ni seguridad de la cuenta.
- El conector autorizado puede actuar con los permisos que el propietario le haya concedido. No se ha ampliado ni revocado su acceso.
- Los permisos predeterminados de Actions no son un límite absoluto: un workflow puede declarar permisos adicionales. Se revisará cualquier futuro archivo de automatización antes de integrarlo.
- No se ha añadido una regla de protección de ramas en esta revisión. La ausencia de colaboradores ya impide que desconocidos escriban o fusionen PR. Las protecciones contra errores del propio mantenedor y los checks obligatorios se diseñarán con el primer flujo de CI.
- No existen workflows ni pruebas automatizadas de la aplicación en este commit.

## Referencias

- [Permisos en repositorios de cuentas personales](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/repository-access-and-collaboration/permission-levels-for-a-personal-account-repository)
- [Forks y permisos](https://docs.github.com/en/pull-requests/reference/forks)
