# Antes y despues - students

Antes: `f0669d6f2898906a565fb179e2359f1bcd06fbca`. Despues medido: `1208b494c4b55c6f002f711fbcecb5e40d131a94`.

Las pruebas reforzadas se ejecutaron antes y despues del cambio de produccion. La igualdad de resultados respalda los casos cubiertos; no demuestra equivalencia para todas las entradas.

| Elemento | Metrica | Antes | Despues | Cambio | Lectura |

|---|---|---:|---:|---|---|

| StudentService | LOC fisicas archivo | 87 | 89 | 2 | Tamano del archivo, incluye auxiliares |

| StudentService | LOC codigo archivo | 72 | 73 | 1 | Tamano del archivo, incluye auxiliares |

| StudentService | WMC | 13 | 13 | 0 | No asumir que extraer un metodo reduce esta metrica |

| StudentService | DIT | 1 | 1 | 0 | No asumir que extraer un metodo reduce esta metrica |

| StudentService | NOC | 0 | 0 | 0 | No asumir que extraer un metodo reduce esta metrica |

| StudentService | CBO | 12 | 12 | 0 | No asumir que extraer un metodo reduce esta metrica |

| StudentService | RFC | 21 | 22 | 1 | No asumir que extraer un metodo reduce esta metrica |

| StudentService | LCOM | 0 | 0 | 0 | No asumir que extraer un metodo reduce esta metrica |

| StudentService | LCOM* | 0.5238 | 0.5833 | 0.0595 | No asumir que extraer un metodo reduce esta metrica |

| StudentService.create | V(G) | 2 | 1 | -1 | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| StudentService.create | LOC cuerpo | 7 | 5 | -2 | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| StudentService.create | MI bruto lexical | 108.7276 | 115.4461 | 6.7185 | Misma convencion lexical; no mezclar escalas |

| StudentService.create | MI normalizado lexical | 63.5834 | 67.5123 | 3.9290 | Misma convencion lexical; no mezclar escalas |

| StudentService.delete | V(G) | 2 | 2 | 0 | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| StudentService.delete | LOC cuerpo | 7 | 7 | 0 | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| StudentService.delete | MI bruto lexical | 111.7872 | 111.7872 | 0 | Misma convencion lexical; no mezclar escalas |

| StudentService.delete | MI normalizado lexical | 65.3726 | 65.3726 | 0 | Misma convencion lexical; no mezclar escalas |

| StudentService.get | V(G) | 1 | 1 | 0 | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| StudentService.get | LOC cuerpo | 3 | 3 | 0 | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| StudentService.get | MI bruto lexical | 131.2877 | 131.2877 | 0 | Misma convencion lexical; no mezclar escalas |

| StudentService.get | MI normalizado lexical | 76.7764 | 76.7764 | 0 | Misma convencion lexical; no mezclar escalas |

| StudentService.list | V(G) | 2 | 2 | 0 | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| StudentService.list | LOC cuerpo | 6 | 6 | 0 | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| StudentService.list | MI bruto lexical | 113.4996 | 113.4996 | 0 | Misma convencion lexical; no mezclar escalas |

| StudentService.list | MI normalizado lexical | 66.3740 | 66.3740 | 0 | Misma convencion lexical; no mezclar escalas |

| StudentService.requireAccessible | V(G) | 3 | 3 | 0 | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| StudentService.requireAccessible | LOC cuerpo | 8 | 8 | 0 | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| StudentService.requireAccessible | MI bruto lexical | 107.6090 | 107.6090 | 0 | Misma convencion lexical; no mezclar escalas |

| StudentService.requireAccessible | MI normalizado lexical | 62.9292 | 62.9292 | 0 | Misma convencion lexical; no mezclar escalas |

| StudentService.requireAvailableDocument | V(G) | Sin dato | 2 | Unidad nueva; no comparable | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| StudentService.requireAvailableDocument | LOC cuerpo | Sin dato | 5 | Unidad nueva; no comparable | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| StudentService.requireAvailableDocument | MI bruto lexical | Sin dato | 122.9304 | Unidad nueva; no comparable | Misma convencion lexical; no mezclar escalas |

| StudentService.requireAvailableDocument | MI normalizado lexical | Sin dato | 71.8892 | Unidad nueva; no comparable | Misma convencion lexical; no mezclar escalas |

| StudentService.update | V(G) | 2 | 1 | -1 | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| StudentService.update | LOC cuerpo | 9 | 7 | -2 | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| StudentService.update | MI bruto lexical | 104.5815 | 109.9708 | 5.3894 | Misma convencion lexical; no mezclar escalas |

| StudentService.update | MI normalizado lexical | 61.1587 | 64.3104 | 3.1517 | Misma convencion lexical; no mezclar escalas |

| StudentService | Unidades ejecutables del archivo | 9 | 10 | 1 | Incluye constructores y lambdas; esta suma NO es WMC de CK |

| StudentService | Suma V(G) de unidades del archivo | 15 | 15 | 0 | Incluye constructores y lambdas; esta suma NO es WMC de CK |


## Interpretacion

La extraccion agrupa una regla repetida y conserva el orden observable. No se exige reducir todos los indicadores. Revisar metodos originales, auxiliar nuevo, LOC del archivo y CK de clase: si WMC o LOC aumentan se informa explicitamente.

Las filas nuevas no tienen un antes numerico comparable; no se inventa cero. MI mantiene la misma convencion experimental y sus escalas separadas. Las lambdas y constructores se incluyen en el conteo y suma de unidades del archivo, pero no se alinean individualmente por numeros de linea que cambiaron.


## Limites

Corrida individual: solo cambia el servicio de esta persona respecto a baseline. Las otras ramas de refactor se integran por separado. Tras los tres merges se realizara una verificacion conjunta; no presentar estas tres corridas como una corrida unica del main final.

Fuentes: salidas originales de baseline y refactor-students, fichas del equipo y resumenes Surefire conservados.
