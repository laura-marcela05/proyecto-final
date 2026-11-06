# Pruebas

Las pruebas automatizadas viven dentro de cada proyecto, porque así lo exigen Maven y Angular:

| Tipo | Ubicación | Cómo se ejecuta |
|---|---|---|
| Unitarias y de controlador (JUnit 5, Mockito) | `src/g-t-api/src/test/java` | `cd src/g-t-api && ./mvnw test` |
| Unitarias del portal (Jasmine, Karma) | `src/g-t-portal/src/app/**/*.spec.ts` | `cd src/g-t-portal && npm test` |
| Pruebas de la API de punta a punta (Postman) | `src/g-t-api/postman/gym-tracker.postman_collection.json` | Importar en Postman con la app levantada y ejecutar la colección |
