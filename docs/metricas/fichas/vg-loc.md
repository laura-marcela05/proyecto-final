# Anexo B: V(G) y LOC

## Nombre y atributo
V(G) estima caminos independientes por unidad ejecutable; LOC describe tamano fisico y documentacion. Son dos metricas distintas, presentadas juntas porque comparten parser y alcance.

## Formula y convenciones
V(G) = 1 + decisiones sintacticas: if, bucles, catch, ternario, cada etiqueta case no default y cada && / ||. No se cuentan else, default, finally, return, try ni operadores dentro de comentarios/literales. El switch con varias constantes en una misma etiqueta cuenta una etiqueta. No equivale a un grafo de flujo construido formalmente ni incorpora todos los caminos excepcionales. Las reglas se fijan antes de medir y no se cambian para mejorar cifras.

Se incluye cada metodo con cuerpo, constructor explicito, constructor compacto de record, lambda y funcion/metodo TypeScript. Las firmas abstractas y metodos sintetizados no se cuentan. Las lambdas se miden por separado y no se suman dos veces al metodo exterior. Cada unidad se identifica por archivo, clase si existe, nombre, linea y columna.

LOC fisicas = lineas de codigo + lineas solo comentario + lineas en blanco. Una linea con codigo y comentario cuenta como codigo. Las lineas efectivas del script son lineas fisicas con tokens de codigo, incluyendo declaraciones y llaves: NO equivalen a numero de instrucciones ejecutables. Los literales multilinea son codigo. Porcentaje de comentarios = 100 * lineas solo comentario / LOC fisicas. No se cuenta una linea vacia extra por el salto final.

## Umbral e interpretacion
Referencia del curso: V(G) 1-10 bajo, 11-20 moderado, 21-50 alto y >50 muy alto. Mas de 50 lineas efectivas por metodo es una senal de revision, no un defecto demostrado. No usar el porcentaje de comentarios como medida suficiente de calidad.

## Herramienta, version y comando
Medidor propio gym-track-vg-loc/1.0, parser tree-sitter 0.25.2, gramatica Java 0.23.5 y TypeScript 0.23.2; Python 3.10-3.13. La implementacion es propia del equipo con apoyo de IA declarado.

Desde la raiz del repositorio, despues de fusionar los tres PR:

```powershell
.\metricas\vg_loc\.venv\Scripts\python.exe metricas/vg_loc/measure.py --out docs/metricas/resultados/baseline/vg_loc
```

No sobreescribe resultados previos. Si el directorio existe, usar otra identificacion de corrida de forma coherente para todas las metricas.

## Alcance y justificacion
Java de produccion en src/g-t-api/src/main/java y TypeScript en src/g-t-portal/src; excluye tests, .spec.ts, .d.ts, dependencias, compilados y fuentes generadas fuera de esas rutas. SQL, HTML, CSS e infraestructura no se incluyen en esta medicion estructural: se priorizan implementacion de negocio, seguridad, controladores y logica de interfaz. Por ello no se presenta como LOC total de todo el repositorio.

El script falla ante errores del parser o fuentes no versionadas/modificadas y conserva SHA-256 por archivo. Revisar la cobertura de unidades antes de interpretar resultados; no reciclar las cifras del generador. Esto respalda el estudio de mantenibilidad sin modificar RF/RNF.

## Salida y accion asociada
files.csv, methods.csv y metadata.json registran unidades, hashes, versiones, fecha UTC y revision Git. Conservarlos completos. Resultado: pendiente de ejecucion del equipo.

Si hay valores altos, inspeccionar responsabilidades, ramas y duplicacion; proponer Extract Method o simplificar condiciones, proteger con pruebas y comparar antes/despues con el MISMO criterio. No eliminar funciones del alcance para bajar el promedio.

## Referencias
- Guia del proyecto y contenido del curso aportados por el equipo: Anexo B, V(G) y LOC.
- https://tree-sitter.github.io/py-tree-sitter/
- https://pypi.org/project/tree-sitter/0.25.2/
- https://pypi.org/project/tree-sitter-java/0.23.5/
- https://pypi.org/project/tree-sitter-typescript/0.23.2/
