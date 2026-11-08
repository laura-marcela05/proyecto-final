# Colaboración para Métricas y Estándares de Calidad

Repositorio único: https://github.com/laura-marcela05/proyecto-final

Cada integrante trabaja en su propia rama y crea sus commits con su identidad y cuenta de GitHub. Cada commit corresponde a un cambio lógico terminado; no se fracciona artificialmente la importación ni se modifica su fecha.

Flujo: actualizar main, crear rama, realizar el cambio, verificarlo, crear commits, publicar la rama y abrir PR hacia main. El autor incorpora correcciones y fusiona después de la aprobación. Conservar la autoría de los commits; no hacer squash de aportes de personas distintas.

Revisión cruzada: Juan revisa a Laura; Laura revisa a Cristian; Cristian revisa a Juan. La revisión debe comprobar archivos, comportamiento y evidencia; no se declara una ejecución que nadie realizó.

Commits y títulos de PR: tipo(scope): descripción en inglés, verbo imperativo, minúsculas, sin punto final y máximo 72 caracteres. La justificación y los RF relacionados van en el cuerpo. Código, pruebas y documentación de cambios lógicos diferentes se separan.

Etiquetas de mantenimiento:

- correctivo: corrige un defecto observado.
- adaptativo: responde a un cambio del entorno.
- perfectivo: mejora una capacidad o atributo del producto.
- preventivo: reduce el riesgo de fallos futuros.

Cada issue de mantenimiento registra evidencia, responsable, tipo, horas reales, criterio de cierre y enlaces a PR/commits. No se deduce costo monetario del número de commits o líneas: se requiere una tarifa declarada. Las importaciones no cuentan como mantenimiento.

Antes de cada commit se revisan la rama, el diff y los archivos preparados. No subir secretos, `.env`, `node_modules`, `target`, `dist`, dependencias descargadas ni compilados. Una regla de .gitignore no elimina archivos ya versionados.

Se mantienen intactos los RF/RNF de `docs/requisitos.md` y la carpeta `g-t-docs`. No se fuerza el push ni se reescribe el historial compartido para corregir contadores.
