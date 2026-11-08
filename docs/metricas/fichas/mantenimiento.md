# Anexo B: esfuerzo y costo de mantenimiento por tipo

## Nombre y atributo
Distribucion del esfuerzo real registrado en mantenimiento correctivo, adaptativo, perfectivo y preventivo. El costo monetario es opcional y requiere tarifas/currency declaradas por el equipo; no se deduce de commits ni de lineas.

## Formula
Horas_tipo = suma de horas de registros validos de ese tipo.
Porcentaje_tipo = 100 * Horas_tipo / total_horas_registradas.
Costo_registro = horas * tarifa_hora, solo cuando existe tarifa documentada.
Sin tarifa no se informa costo monetario total. Sin registros la medicion esta pendiente: NO es costo cero. Las distribuciones describen solo el esfuerzo registrado; issues sin registro son cobertura pendiente.

## Umbral y referencia
No hay umbral universal de aprobacion. Comparar criticamente con la distribucion clasica del curso (21% correctivo, 25% adaptativo, 50% perfectivo, 4% preventivo), que es contexto de literatura y NO meta obligatoria del grupo. Un proyecto corto y academico no tiene por que replicarla.

## Herramienta, version y comando
Medidor propio gym-track-maintenance/1.0; biblioteca estandar Python 3.10 o posterior, Git y snapshot publico de GitHub REST. No requiere token ni lo imprime. Si el servicio limita peticiones o no hay acceso, detener y conservar el error; no asumir lista vacia.

Despues de preparar registros honestos y fusionar los tres PR:

```powershell
py metricas/mantenimiento/measure.py --export-issues docs/metricas/resultados/baseline/issues.json
py metricas/mantenimiento/measure.py --issues docs/metricas/resultados/baseline/issues.json --out docs/metricas/resultados/baseline/mantenimiento
```

## Alcance y reglas de registro
Solo mantenimiento real de esta materia sobre el repositorio unico, posterior a tomar la aplicacion base. No contar importacion, merges, aprobaciones o esperas como horas de mantenimiento. La documentacion de medicion no se etiqueta automaticamente como mantenimiento del producto: justificar cada clasificacion.

Cristian mantiene registro.csv con la informacion proporcionada por cada integrante. Una fila es una sesion de trabajo de una persona sobre un issue real. record_id es unico; issue_number es el numero real, no una URL ni PR; type coincide con UNA etiqueta correctivo/adaptativo/perfectivo/preventivo del issue. person es Juan, Laura o Cristian. date va AAAA-MM-DD. hours es un decimal positivo con punto. basis es recorded (registro contemporaneo) o retrospective_estimate (estimacion retrospectiva explicada). Nunca fingir que una estimacion fue cronometrada.

hourly_rate se deja vacio si no hay tarifa; si existe, todos usan la misma moneda documentada y un criterio comun. commit es opcional si el trabajo sigue abierto; si se pone, debe ser un SHA real del autor y alcanzable desde HEAD. description explica que hizo, de donde salen las horas y por que es mantenimiento. No poner ejemplos ficticios como datos del equipo.

Los issues pueden crearse ahora para documentar trabajo ya hecho, indicando honestamente que el registro es retrospectivo y enlazando PR/evidencia. No cambiar fechas ni inventar un issue previo. Para pruebas nuevas que anticipan regresiones puede justificarse preventivo, si corresponde al trabajo real. No clasificar por el prefijo del commit de forma automatica.

## Salida, justificacion y accion
maintenance.json registra horas, porcentajes sobre lo registrado, costo solo si procede, presencia de estimaciones, issues etiquetados sin registros, hashes de entrada, fecha UTC y revision Git. El snapshot conserva las etiquetas que se usaron al medir. Estado inicial: pendiente, sin datos prellenados.

Permite discutir donde se invirtio esfuerzo y como la complejidad, falta de pruebas o cambios del entorno influyen en el mantenimiento. Ante concentracion de correctivos investigar causas y cobertura; no modificar etiquetas para parecerse a la distribucion clasica. Reconciliar registros con el equipo antes de publicar cifras.

## Referencias
- Guia del docente y contenido de clase aportados: tipos/costos y Anexo B.
- https://docs.github.com/en/rest/issues/issues#list-repository-issues
- https://docs.github.com/en/issues/using-labels-and-milestones-to-track-work/managing-labels
- https://docs.python.org/3/library/decimal.html
