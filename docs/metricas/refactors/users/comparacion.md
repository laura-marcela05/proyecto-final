# Antes y despues - users

Antes: `f0669d6f2898906a565fb179e2359f1bcd06fbca`. Despues medido: `f1aa46598fc0c836099c14993e45aadc7b85627b`.

Las pruebas reforzadas se ejecutaron antes y despues del cambio de produccion. La igualdad de resultados respalda los casos cubiertos; no demuestra equivalencia para todas las entradas.

| Elemento | Metrica | Antes | Despues | Cambio | Lectura |

|---|---|---:|---:|---|---|

| UserService | LOC fisicas archivo | 81 | 83 | 2 | Tamano del archivo, incluye auxiliares |

| UserService | LOC codigo archivo | 69 | 70 | 1 | Tamano del archivo, incluye auxiliares |

| UserService | WMC | 12 | 12 | 0 | No asumir que extraer un metodo reduce esta metrica |

| UserService | DIT | 1 | 1 | 0 | No asumir que extraer un metodo reduce esta metrica |

| UserService | NOC | 0 | 0 | 0 | No asumir que extraer un metodo reduce esta metrica |

| UserService | CBO | 11 | 11 | 0 | No asumir que extraer un metodo reduce esta metrica |

| UserService | RFC | 23 | 24 | 1 | No asumir que extraer un metodo reduce esta metrica |

| UserService | LCOM | 0 | 0 | 0 | No asumir que extraer un metodo reduce esta metrica |

| UserService | LCOM* | 0.4762 | 0.5417 | 0.0655 | No asumir que extraer un metodo reduce esta metrica |

| UserService.create | V(G) | 2 | 1 | -1 | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| UserService.create | LOC cuerpo | 8 | 6 | -2 | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| UserService.create | MI bruto lexical | 106.1829 | 112.0518 | 5.8690 | Misma convencion lexical; no mezclar escalas |

| UserService.create | MI normalizado lexical | 62.0952 | 65.5274 | 3.4322 | Misma convencion lexical; no mezclar escalas |

| UserService.delete | V(G) | 3 | 3 | 0 | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| UserService.delete | LOC cuerpo | 10 | 10 | 0 | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| UserService.delete | MI bruto lexical | 104.0658 | 104.0658 | 0 | Misma convencion lexical; no mezclar escalas |

| UserService.delete | MI normalizado lexical | 60.8572 | 60.8572 | 0 | Misma convencion lexical; no mezclar escalas |

| UserService.find | V(G) | 1 | 1 | 0 | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| UserService.find | LOC cuerpo | 3 | 3 | 0 | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| UserService.find | MI bruto lexical | 132.7956 | 132.7956 | 0 | Misma convencion lexical; no mezclar escalas |

| UserService.find | MI normalizado lexical | 77.6583 | 77.6583 | 0 | Misma convencion lexical; no mezclar escalas |

| UserService.get | V(G) | 1 | 1 | 0 | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| UserService.get | LOC cuerpo | 3 | 3 | 0 | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| UserService.get | MI bruto lexical | 133.1810 | 133.1810 | 0 | Misma convencion lexical; no mezclar escalas |

| UserService.get | MI normalizado lexical | 77.8836 | 77.8836 | 0 | Misma convencion lexical; no mezclar escalas |

| UserService.list | V(G) | 1 | 1 | 0 | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| UserService.list | LOC cuerpo | 3 | 3 | 0 | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| UserService.list | MI bruto lexical | 129.3603 | 129.3603 | 0 | Misma convencion lexical; no mezclar escalas |

| UserService.list | MI normalizado lexical | 75.6493 | 75.6493 | 0 | Misma convencion lexical; no mezclar escalas |

| UserService.requireAvailableUsername | V(G) | Sin dato | 2 | Unidad nueva; no comparable | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| UserService.requireAvailableUsername | LOC cuerpo | Sin dato | 5 | Unidad nueva; no comparable | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| UserService.requireAvailableUsername | MI bruto lexical | Sin dato | 122.9304 | Unidad nueva; no comparable | Misma convencion lexical; no mezclar escalas |

| UserService.requireAvailableUsername | MI normalizado lexical | Sin dato | 71.8892 | Unidad nueva; no comparable | Misma convencion lexical; no mezclar escalas |

| UserService.update | V(G) | 3 | 2 | -1 | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| UserService.update | LOC cuerpo | 12 | 10 | -2 | Comparar tambien auxiliares y clase; menor valor local no prueba mejora global |

| UserService.update | MI bruto lexical | 98.1262 | 102.1039 | 3.9778 | Misma convencion lexical; no mezclar escalas |

| UserService.update | MI normalizado lexical | 57.3837 | 59.7099 | 2.3262 | Misma convencion lexical; no mezclar escalas |

| UserService | Unidades ejecutables del archivo | 8 | 9 | 1 | Incluye constructores y lambdas; esta suma NO es WMC de CK |

| UserService | Suma V(G) de unidades del archivo | 13 | 13 | 0 | Incluye constructores y lambdas; esta suma NO es WMC de CK |


## Interpretacion

La extraccion agrupa una regla repetida y conserva el orden observable. No se exige reducir todos los indicadores. Revisar metodos originales, auxiliar nuevo, LOC del archivo y CK de clase: si WMC o LOC aumentan se informa explicitamente.

Las filas nuevas no tienen un antes numerico comparable; no se inventa cero. MI mantiene la misma convencion experimental y sus escalas separadas. Las lambdas y constructores se incluyen en el conteo y suma de unidades del archivo, pero no se alinean individualmente por numeros de linea que cambiaron.


## Limites

Corrida individual: solo cambia el servicio de esta persona respecto a baseline. Las otras ramas de refactor se integran por separado. Tras los tres merges se realizara una verificacion conjunta; no presentar estas tres corridas como una corrida unica del main final.

Fuentes: salidas originales de baseline y refactor-users, fichas del equipo y resumenes Surefire conservados.
