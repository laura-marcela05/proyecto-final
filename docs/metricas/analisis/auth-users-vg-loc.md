# Analisis inicial de auth/users, V(G) y LOC

Autor responsable: Juan. Trabajo nuevo de Metricas y Estandares de Calidad, con apoyo de IA declarado en el informe. Estado: analisis previo a refactorizacion.

## Evidencia y alcance

Entradas: [files.csv](../resultados/baseline/vg_loc/files.csv), [methods.csv](../resultados/baseline/vg_loc/methods.csv) y [metadata.json](../resultados/baseline/vg_loc/metadata.json), incorporadas por el PR 22. La medicion incluye Java y TypeScript de produccion de las rutas de la ficha, no todo el repositorio. Las cifras detalladas se conservan en las salidas y en el Anexo C generado, sin transcribirlas manualmente.

## Interpretacion

Las unidades registradas permanecen en el rango bajo de V(G) de la ficha. EditUser.submit es la unidad con mayor V(G) en la salida TypeScript; estar dentro del rango no garantiza ausencia de errores. Ningun cuerpo registrado supera la referencia orientativa de longitud de la ficha. Se incluye cada lambda por separado, por lo que sumar LOC de todos los cuerpos puede contar regiones anidadas mas de una vez: el tamano agregado se obtiene de files.csv.

Los antiguos maximos del METRICS.md del generador no son la linea base de esta materia. Se utilizan exclusivamente las salidas del PR 22. La proporcion de comentarios es descriptiva: aumentarla no demuestra mejor mantenibilidad. Complejidad baja tampoco verifica por si sola rendimiento, seguridad ni cumplimiento de todos los RF/RNF.

## Revision del area y mejoras propuestas

UserService.create y update repiten la respuesta al nombre de usuario duplicado, con consultas distintas segun la operacion. Es candidato a un refactor pequeno para expresar esa regla con claridad, conservando la consulta correcta y el orden de validacion. No se propone alterar la API ni los requisitos. Tambien conviene revisar submit en el formulario de edicion cuando se amplie esa pantalla; su posicion en el ranking no exige cambiarlo hoy.

| Prioridad | Propuesta | Riesgo si se cambia mal | Esfuerzo estimado | Comprobacion prevista |
|---|---|---|---|---|
| Alta | Mantener y ejecutar pruebas de duplicados y permisos antes de tocar usuarios | Aceptar duplicados o alterar respuestas HTTP | Bajo | UserServiceTest y UserControllerTest, mas suite completa |
| Media | Refactor pequeno de la regla de duplicado en UserService | Cambiar validacion de create/update | Bajo | Mismas respuestas y consultas; re-medir unidad original, auxiliares y clase |
| Baja | Revisar separacion de validacion y envio en EditUser.submit en una evolucion posterior | Cambiar validacion del formulario | Medio | Pruebas del portal antes de modificarlo |

La tarea posterior seleccionara el cambio concreto tras revisar las pruebas. No se declara implementado ni se promete que V(G), LOC y CK mejoren a la vez. Extraer un metodo puede desplazar complejidad y aumentar el numero de metodos: el antes/despues debe incluir el conjunto afectado.

## Referencias

- [Ficha V(G)/LOC](../fichas/vg-loc.md), basada en el contenido del curso.
- [PR 17, pruebas auth/users](https://github.com/laura-marcela05/proyecto-final/pull/17).
- [PR 22, medicion inicial](https://github.com/laura-marcela05/proyecto-final/pull/22).
