# Anexo B: suite CK e indice de mantenibilidad

## Nombre y atributos
La suite CK estudia complejidad de clase, herencia, acoplamiento, respuesta y cohesion. El MI combina volumen lexical Halstead, complejidad y LOC. Se registran por separado; no son un unico indicador.

## CK: calculo y convenciones
Se usa CK 0.7.0, artefacto publicado en Maven Central y fijado por SHA-256 en ck.py. Ejecuta analisis estatico Java con useJars=false; las dependencias externas no se resuelven con los jars del proyecto. Esto puede limitar CBO/RFC/DIT y debe declararse al interpretar, sin afirmar precision semantica completa.

Campos: wmc, dit, noc, cbo, rfc, lcom y lcom*. CK define cada contador en su implementacion. No sustituir WMC de CK por la suma de V(G) de otro medidor: las convenciones pueden diferir. Se conserva el CSV original para auditarlo.

LCOM bruto y LCOM* normalizado son variantes diferentes. El umbral de 0.7 del curso es solo una heuristica para una escala normalizada compatible; NO se aplica al campo lcom bruto. Los casos degenerados, no finitos o fuera de escala se revisan manualmente, no se convierten en cero.

Referencias de alerta del curso: WMC >20, DIT >5, CBO >10, RFC >50; no hay umbral numerico universal para NOC. No basta superar un umbral para demostrar un defecto.

## MI: formula y convenciones
Medidor propio gym-track-mi-lexical/1.0. V = (N1+N2) * log2(n1+n2), con N1/N2 conteos totales y n1/n2 vocabularios distintos de operadores/operandos. En esta convencion lexical, identificadores y literales atomicos son operandos; los demas tokens, incluidos delimitadores y palabras reservadas, son operadores. Comentarios se excluyen. Las unidades ejecutables anidadas se miden por separado. La tabla de tokens es una decision del equipo: no se afirma equivalencia con Visual Studio ni con otro lexer Halstead.

MI bruto = 171 - 5.2*ln(V) - 0.23*CC - 16.2*ln(LOC).
MI normalizado = max(0, min(100, MI bruto*100/171)).
Con V<=0 o LOC<=0 el resultado se marca indefinido; no se inventa V=1.

CC y LOC provienen de methods.csv de Juan. Se comprueba SHA-256 de la fuente antes de combinar datos. Halstead usa exactamente el cuerpo de la misma unidad. No se debe mezclar LOC de clase con CC de metodo ni resultados de revisiones distintas.

## Umbrales e interpretacion del MI
La guia del curso menciona MI bajo 65 como alerta, pero su formula publicada es la bruta: NO trasladar 65 a la escala 0-100. Microsoft documenta otra referencia para su escala normalizada: 0-9 baja, 10-19 moderada y 20-100 buena. Como este medidor tiene convencion Halstead propia, ambas referencias son orientativas y no una calibracion validada de esta implementacion. Priorizar comparaciones antes/despues dentro del mismo archivo/unidad y formula; explicar la limitacion en el informe.

## Herramientas y comandos
CK 0.7.0, Java 17 o posterior como entorno del proyecto; Python 3.10-3.13. MI usa tree-sitter 0.25.2 y Java 0.23.5. Versiones fijas en requirements y metadata, sin modificar el pom de la aplicacion.

Despues de fusionar los tres PR y de obtener V(G)/LOC:

```powershell
.\metricas\ck_mi\.venv\Scripts\python.exe metricas/ck_mi/ck.py --out docs/metricas/resultados/baseline/ck
.\metricas\ck_mi\.venv\Scripts\python.exe metricas/ck_mi/mi.py --vg docs/metricas/resultados/baseline/vg_loc --out docs/metricas/resultados/baseline/mi
```

## Alcance y justificacion
CK interpreta clases publicas explicitas en service, security, controller y entity: modulos principales del backend donde se concentran reglas, seguridad y persistencia. El analizador recibe todo Java de produccion para disponer de su contexto, pero records, enums y codigo generado no forman parte de la tabla CK interpretada. Se exige que todas las clases esperadas del alcance aparezcan. No se presenta CK como medicion del portal ni de todo Java sin exclusiones.

MI interpreta unidades Java con cuerpo registradas por Juan, incluidos records y lambdas. No se infiere MI de TypeScript: queda fuera de esta ficha para evitar mezclar convenciones lexicas. Se justifica priorizar el nucleo de negocio del backend, no se afirma cobertura global.

## Salidas y accion asociada
CK conserva class.csv, method.csv, ck.log, ck.csv filtrado y metadata.json. MI produce mi.csv y metadata.json con hashes y procedencia de entradas. Valores: pendientes de ejecucion del equipo.

Revisar clases con responsabilidades/acoplamiento altos y cohesion baja. Proponer Extract Class o extraer colaboracion concreta sin asumir que toda clase extraida mejora CBO global. Para MI bajo investigar CC, LOC y volumen; proteger cambios con pruebas y re-medir con el mismo criterio.

## Referencias
- Guia del docente y contenido del curso aportados: CK, MI y Anexo B.
- https://github.com/mauricioaniche/ck/tree/ck-0.7.0
- https://central.sonatype.com/artifact/com.github.mauricioaniche/ck/0.7.0
- https://learn.microsoft.com/en-us/visualstudio/code-quality/code-metrics-maintainability-index-range-and-meaning
- https://pypi.org/project/tree-sitter/0.25.2/
- https://pypi.org/project/tree-sitter-java/0.23.5/
