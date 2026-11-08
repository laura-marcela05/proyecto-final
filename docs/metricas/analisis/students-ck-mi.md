# Analisis inicial de students, suite CK e indice de mantenibilidad

Autora responsable: Laura. Trabajo nuevo de Metricas y Estandares de Calidad, con apoyo de IA declarado en el informe. Estado: analisis previo a refactorizacion.

## Evidencia y alcance

Entradas del PR 23: [CK filtrado](../resultados/baseline/ck/ck.csv), [CK original](../resultados/baseline/ck/class.csv), [log](../resultados/baseline/ck/ck.log), [MI](../resultados/baseline/mi/mi.csv) y sus metadata.json. El Anexo C generado contiene los valores por clase y unidad. CK interpreta las clases publicas de las capas declaradas; no representa todo Java ni el portal. MI se calcula solamente para las unidades Java del alcance.

## Interpretacion CK

Los resultados muestran alertas orientativas de CBO en StudentService y StudentController, junto con otras clases del sistema. No se concluye automaticamente que sean clases mal disenadas: CK cuenta tipos de Spring/JPA y otras dependencias, sin resolver todos los JAR externos. Se revisan responsabilidades y dependencias antes de decidir un cambio. WMC, RFC y DIT deben compararse con sus propios umbrales; no se reemplaza WMC con la suma del V(G) de otro medidor.

Student y otras entidades tienen LCOM* por encima de la referencia orientativa normalizada. Getters, setters y mapeo de persistencia pueden influir en esta observacion. No se propone partir entidades ni modificar el esquema solo para mejorar una cifra. El campo lcom bruto no admite el umbral normalizado de LCOM*. Valores indefinidos o fuera de escala requieren revision manual.

## Interpretacion MI

Las unidades Java con MI bruto definido no activan la referencia de alerta bruta de la ficha en esta corrida. Esto no demuestra calidad global: el lexer Halstead es propio y experimental. La escala bruta y la normalizada se presentan separadas; no se usa el umbral bruto sobre la escala normalizada ni se llama a este resultado MI de Visual Studio. La configuracion de seguridad aparece entre las unidades con menor MI bruto; su rol de configuracion exige contexto antes de refactorizar.

Los avisos de configuracion del logger de CK aparecen en el log conservado. No se ocultan ni se confunden con un resultado de pruebas de la aplicacion. El log informa que se extrajeron metricas.

## Revision del area y mejoras propuestas

StudentService.create y update construyen la misma excepcion por documento duplicado, aunque usan consultas diferentes. Es candidato a expresar esa regla con un auxiliar pequeno, sin alterar acceso por propietario, consulta de duplicados ni restricciones de eliminacion. No se promete reducir CBO con esta extraccion; puede aumentar WMC por introducir un metodo.

| Prioridad | Propuesta | Riesgo si se cambia mal | Esfuerzo estimado | Comprobacion prevista |
|---|---|---|---|---|
| Alta | Conservar pruebas de propiedad, duplicados y borrado antes de cambiar students | Exponer estudiantes ajenos o aceptar documentos repetidos | Bajo | StudentServiceTest y StudentControllerTest, mas suite completa |
| Media | Refactor pequeno de la regla de documento duplicado | Confundir create con update | Bajo | Consultas y errores equivalentes; re-medir metodos, auxiliares y clase |
| Baja | Revisar acoplamiento estructural de servicios en una evolucion posterior | Cambiar transacciones o contrato de persistencia | Medio | Justificar dependencia concreta antes de introducir interfaces o clases |

No hay refactor implementado ni medicion posterior en este documento. La tarea siguiente comparara el codigo cambiado con su linea base y explicara tanto mejoras como metricas iguales o mayores.

## Referencias

- [Ficha CK y MI](../fichas/ck-mi.md).
- [CK 0.7.0](https://github.com/mauricioaniche/ck/tree/ck-0.7.0).
- [Formula y escalas de MI](https://learn.microsoft.com/en-us/visualstudio/code-quality/code-metrics-maintainability-index-range-and-meaning).
- [PR 16, pruebas students](https://github.com/laura-marcela05/proyecto-final/pull/16) y [PR 23, medicion](https://github.com/laura-marcela05/proyecto-final/pull/23).
