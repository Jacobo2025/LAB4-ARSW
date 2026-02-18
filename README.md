## Laboratorio #4 – REST API Blueprints (Java 21 / Spring Boot 3.3.x)
# Escuela Colombiana de Ingeniería – Arquitecturas de Software  

---

## 📋 Requisitos
- Java 21
- Maven 3.9+

## ▶️ Ejecución del proyecto
```bash
mvn clean install
mvn spring-boot:run
```
Probar con `curl`:
```bash
curl -s http://localhost:8080/blueprints | jq
curl -s http://localhost:8080/blueprints/john | jq
curl -s http://localhost:8080/blueprints/john/house | jq
curl -i -X POST http://localhost:8080/blueprints -H 'Content-Type: application/json' -d '{ "author":"john","name":"kitchen","points":[{"x":1,"y":1},{"x":2,"y":2}] }'
curl -i -X PUT  http://localhost:8080/blueprints/john/kitchen/points -H 'Content-Type: application/json' -d '{ "x":3,"y":3 }'
```

> Si deseas activar filtros de puntos (reducción de redundancia, *undersampling*, etc.), implementa nuevas clases que implementen `BlueprintsFilter` y cámbialas por `IdentityFilter` con `@Primary` o usando configuración de Spring.
---

Abrir en navegador:  
- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)  
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)  

---

## 🗂️ Estructura de carpetas (arquitectura)

```
src/main/java/edu/eci/arsw/blueprints
  ├── model/         # Entidades de dominio: Blueprint, Point
  ├── persistence/   # Interfaz + repositorios (InMemory, Postgres)
  │    └── impl/     # Implementaciones concretas
  ├── services/      # Lógica de negocio y orquestación
  ├── filters/       # Filtros de procesamiento (Identity, Redundancy, Undersampling)
  ├── controllers/   # REST Controllers (BlueprintsAPIController)
  └── config/        # Configuración (Swagger/OpenAPI, etc.)
```

> Esta separación sigue el patrón **capas lógicas** (modelo, persistencia, servicios, controladores), facilitando la extensión hacia nuevas tecnologías o fuentes de datos.

---

## 📖 Actividades del laboratorio

### 1. Familiarización con el código base
- Revisa el paquete `model` con las clases `Blueprint` y `Point`.  
- Entiende la capa `persistence` con `InMemoryBlueprintPersistence`.  
- Analiza la capa `services` (`BlueprintsServices`) y el controlador `BlueprintsAPIController`.

### 2. Migración a persistencia en PostgreSQL
- Configura una base de datos PostgreSQL (puedes usar Docker).  
- Implementa un nuevo repositorio `PostgresBlueprintPersistence` que reemplace la versión en memoria.  
- Mantén el contrato de la interfaz `BlueprintPersistence`.  

### 3. Buenas prácticas de API REST
- Cambia el path base de los controladores a `/api/v1/blueprints`.  
- Usa **códigos HTTP** correctos:  
  - `200 OK` (consultas exitosas).  
  - `201 Created` (creación).  
  - `202 Accepted` (actualizaciones).  
  - `400 Bad Request` (datos inválidos).  
  - `404 Not Found` (recurso inexistente).  
- Implementa una clase genérica de respuesta uniforme:
  ```java
  public record ApiResponse<T>(int code, String message, T data) {}
  ```
  Ejemplo JSON:
  ```json
  {
    "code": 200,
    "message": "execute ok",
    "data": { "author": "john", "name": "house", "points": [...] }
  }
  ```

### 4. OpenAPI / Swagger
- Configura `springdoc-openapi` en el proyecto.  
- Expón documentación automática en `/swagger-ui.html`.  
- Anota endpoints con `@Operation` y `@ApiResponse`.

### 5. Filtros de *Blueprints*
- Implementa filtros:
  - **RedundancyFilter**: elimina puntos duplicados consecutivos.  
  - **UndersamplingFilter**: conserva 1 de cada 2 puntos.  
- Activa los filtros mediante perfiles de Spring (`redundancy`, `undersampling`).  

---

## ✅ Entregables

1. Repositorio en GitHub con:  
   - Código fuente actualizado.  
   - Configuración PostgreSQL (`application.yml` o script SQL).  
   - Swagger/OpenAPI habilitado.  
   - Clase `ApiResponse<T>` implementada.  

2. Documentación:  
   - Informe de laboratorio con instrucciones claras.  
   - Evidencia de consultas en Swagger UI y evidencia de mensajes en la base de datos.  
   - Breve explicación de buenas prácticas aplicadas.  

---

## 📊 Criterios de evaluación

| Criterio | Peso |
|----------|------|
| Diseño de API (versionamiento, DTOs, ApiResponse) | 25% |
| Migración a PostgreSQL (repositorio y persistencia correcta) | 25% |
| Uso correcto de códigos HTTP y control de errores | 20% |
| Documentación con OpenAPI/Swagger + README | 15% |
| Pruebas básicas (unitarias o de integración) | 15% |

**Bonus**:  

- Imagen de contenedor (`spring-boot:build-image`).  
- Métricas con Actuator.

---
# **INFORME DE LABORATORIO**
#### **AUTORES**
- *Jacobo Diaz Alvarado*

- *Santiago Carmona Pineda*

## 1. Familiarización con el código base

### 1.1 Entendiendo `model`:

En la carpeta *model* se encuntran las siguientes clase:

**Point**: es un record con dos campos *x* y *y*. Al ser un record hay métodos que se generan automáticamente. 

**Blueprint**: 
- Esta clase tiene tres atributos (author, name, una lista de Point inicializada).
- Tiene sus respectivos `getters`.
- Tiene un método añadir punto.
- Realiza una sobreescritura al método `equals()` que tiene como objetivo decir que dos `Objetos`son iguales si tienen el mismo nombre y el mismo autor, sin importar el espacio de memorio.
- Realiza una sobreescritura al método `hashCode()` ya que *hashCode()* y *equals()* van de la mano. Su funcion garantizar que esa igualdad funcione bien en colecciones.

### 1.2 Entendiendo `persistence`:

En la carpeta *persistence* se encuentrar las siguientes clases:

**BlueprintNotFoundException**: es una extensión de Exception. Es una excepción personalizada para el caso en el que no se encuentre un *Blueprint*.

**BlueprintPersistenceException**: Es una excepción personalizada para el caso en el que ya hay un *Blueprint* en una colección.

**BlueprintPersistence**: Es una interfaz que contiene los métodos posibles para manejar el almacenamiento.

**InMemoryBlueprintPersistence**: Es una clase que implementa a *BlueprintPersistence*. 

- Posee un `Map` que  guarda por nombre y objeto.
- InMemoryBlueprintPersistence(): Constructor que inicializa tres *Blueprint* los cuales los guarda en el `Map`.
- keyOf(Blueprint bp): método para juntar en un solo *String* el autor y el nombre de *Blueprint* a partir de un *Blueprint*.
- keyOf(String author, String name): método para juntar en un solo *String* el autor y el nombre a partir de un *author* y *name*.
- saveBlueprint(Blueprint bp): método para guarda un *Blueprint* en el *Map* siempre y cuando este no haya estado antes.
- getBlueprint(String author, String name): método que busca dentro de *Map* un *Blueprint* a partir de *author* y *name*.
- getBlueprintsByAuthor(String author): es un método que "agarra" todo los *Blueprint* que tienen el mismo *Author* y los devuelve en un conjunto.
- getAllBlueprints(): devuelve todos los *Blueprints* que están dentro del *Map*.
- addPoint(String author, String name, int x, int y): método para añadirle a un *Blueprint* un punto.

### 1.3 Entendiendo `filters`:

**BlueprintsFilter**: Es una interface continene un único método para filtrar en general.

**IdentityFilter**: Es una clase que implementa a *BlueprintsFilters* que busca a partir de un objeto *Blueprint*.

**RedundancyFilter**:  Es una clase que implementa a *BlueprintsFilters* que busca solo los *Blueprint* que tenga puntos consecutivos diferentes.

**UndersamplingFilter**: Es una clase que implementa a *BlueprintsFilters* que filtra por posición par de la lista de puntos  (2,4,6,..) de un *Blueprints*.

### 1.4 Entendiendo `services`:

**BlueprintsServices**: Es una clase de service encarda de llevar toda la lógica.

- Tiene dos atributos (persistence y filter).
- addNewBlueprint(Blueprint bp): guarda un *Blueprint* en la persistencia.
- getAllBlueprints(): le pide a la persistencia todos los *Blueprint*.
- getBlueprintsByAuthor(String author): le pide a la persistencia los *Blueprints* por *author*.
- getBlueprint(String author, String name): le pide a la persistencia los *Blueprints* a través de un filtro.
- addPoint(String author, String name, int x, int y): le pide a la persistencia darle un punto a un *Blueprint*.

### 1.5 Entendiendo `controllers`:

**BlueprintsAPIController**: recibe peticiones HTTP 

- Tiene un atributo (service).
- getAll(): tiene el  endpoint `@GetMapping` y le pide al servicio todos los `Blueprint`.
- byAuthor(@PathVariable String author): tiene el endpoint `@GetMapping("/{author}")` y le pide buscar un `Blueprint`por `author`al servicio.
- byAuthorAndName(@PathVariable String author, @PathVariable String bpname): tiene el endpoint `@GetMapping("/{author}/{bpname}")` y le pide buscar un `Blueprint`por `author` y `name`al servicio.
- NewBlueprintRequest: Es un record que sirve para que definir cómo debe verse el JSON que llega cuando alguien quiere crear un Blueprint.
- add(@Valid @RequestBody NewBlueprintRequest req): tiene el endpoint `@PostMapping` y añade un nuevo `Blueprint` con ayuda del servicio.
- addPoint(@PathVariable String author, @PathVariable String bpname, @RequestBody Point p): tiene el endpoint `@PutMapping("/{author}/{bpname}/points")` y le pide al servicio añadir un punto a un `Blueprint`.



