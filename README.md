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
---
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
- **InMemoryBlueprintPersistence()**: Constructor que inicializa tres *Blueprint* los cuales los guarda en el `Map`.
- **keyOf(Blueprint bp)**: método para juntar en un solo *String* el autor y el nombre de *Blueprint* a partir de un *Blueprint*.
- **keyOf(String author, String name)**: método para juntar en un solo *String* el autor y el nombre a partir de un *author* y *name*.
- **saveBlueprint(Blueprint bp)**: método para guarda un *Blueprint* en el *Map* siempre y cuando este no haya estado antes.
- **getBlueprint(String author, String name)**: método que busca dentro de *Map* un *Blueprint* a partir de *author* y *name*.
- **getBlueprintsByAuthor(String author)**: es un método que "agarra" todo los *Blueprint* que tienen el mismo *Author* y los devuelve en un conjunto.
- **getAllBlueprints()**: devuelve todos los *Blueprints* que están dentro del *Map*.
- **addPoint(String author, String name, int x, int y)**: método para añadirle a un *Blueprint* un punto.

### 1.3 Entendiendo `filters`:

**BlueprintsFilter**: Es una interface continene un único método para filtrar en general.

**IdentityFilter**: Es una clase que implementa a *BlueprintsFilters* que busca a partir de un objeto *Blueprint*.

**RedundancyFilter**:  Es una clase que implementa a *BlueprintsFilters* que busca solo los *Blueprint* que tenga puntos consecutivos diferentes.

**UndersamplingFilter**: Es una clase que implementa a *BlueprintsFilters* que filtra por posición par de la lista de puntos  (2,4,6,..) de un *Blueprints*.

### 1.4 Entendiendo `services`:

**BlueprintsServices**: Es una clase de service encarda de llevar toda la lógica.

- Tiene dos atributos (persistence y filter).
- **addNewBlueprint(Blueprint bp)**: guarda un *Blueprint* en la persistencia.
- **getAllBlueprints()**: le pide a la persistencia todos los *Blueprint*.
- **getBlueprintsByAuthor(String author)**: le pide a la persistencia los *Blueprints* por *author*.
- **getBlueprint(String author, String name)**: le pide a la persistencia los *Blueprints* a través de un filtro.
- **addPoint(String author, String name, int x, int y)**: le pide a la persistencia darle un punto a un *Blueprint*.

### 1.5 Entendiendo `controllers`:

**BlueprintsAPIController**: recibe peticiones HTTP

- Tiene un atributo (service).
- **getAll()**: tiene el  endpoint `@GetMapping` y le pide al servicio todos los `Blueprint`.
- **byAuthor(@PathVariable String author)**: tiene el endpoint `@GetMapping("/{author}")` y le pide buscar un `Blueprint`por `author`al servicio.
- **byAuthorAndName(@PathVariable String author, @PathVariable String bpname)**: tiene el endpoint `@GetMapping("/{author}/{bpname}")` y le pide buscar un `Blueprint`por `author` y `name`al servicio.
- **NewBlueprintRequest**: Es un record que sirve para que definir cómo debe verse el JSON que llega cuando alguien quiere crear un Blueprint.
- **add(@Valid @RequestBody NewBlueprintRequest req)**: tiene el endpoint `@PostMapping` y añade un nuevo `Blueprint` con ayuda del servicio.
- **addPoint(@PathVariable String author, @PathVariable String bpname, @RequestBody Point p)**: tiene el endpoint `@PutMapping("/{author}/{bpname}/points")` y le pide al servicio añadir un punto a un `Blueprint`.

---
## 2. Migración a persistencia en PostgreSQL.

### 2.1 Configuración de PostgreSQL:

Lo primero que se hizo fue instalar PostgreSQL en nuestro equipo y allí crear la base
de datos para la API. Una vez creada fue asignada a un usuario de la base de datos.

### 2.2 Configuración del `pom.xml`

Se agregaron estas dos dependencias nuevas:

```
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    
```

La primera dependencia incluye Spring Data JPA junto con Hibernate como proveedor JPA por defecto.
La segunda proporciona el driver JDBC específico para PostgreSQL, marcado como runtime porque solo se necesita durante la ejecución.

### 2.3 Configuración de `application.properties`

Aquí se agregaron los parámetros de conexión con nuestra base de datos PostgreSQL.

```
    # Configuración de la base de datos
    spring.datasource.url=jdbc:postgresql://localhost:5432/BlueprintDB
    spring.datasource.username=jacobo
    spring.datasource.password=jacobo123
    spring.datasource.driver-class-name=org.postgresql.Driver
    
    # Configuración de JPA/Hibernate
    spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
    spring.jpa.hibernate.ddl-auto=update
    spring.jpa.show-sql=true
    spring.jpa.properties.hibernate.format_sql=true
    
```
**Configuración de la base de datos**

- **URL de conexión**: Aquí se coloca el puerto donde trabaja nuestra base de datos *5432* y el nombre de nuestra base *BlueprinDB*.

- **USERNAME**: aquí se coloca el nombre del propietario de la base de datos, este caso *jacobo*.

- **PASSWORD**: aquí se coloca la contraseña del propietario de la base de datos, en este caso, *jacobo123*.

**Configuración de JPA/Hibernate**

- **spring.jpa.hibernate.ddl-auto**: controla cómo Hibernate gestiona el esquema de la base de datos.

**Configuración de logging SQL**

- Estas configuraciones permiten visualizar las consultas SQL generadas por Hibernate en la consola, facilitando la comprensión y optimización de las operaciones de base de datos.

### 2.4 Migración a la base de datos PostgreSQL

**2.4.1 tranformación de `model`**

La implementación de `JPA` permite poner anotaciones para hacer referencia a tablas en nuestra base de datos.

- **@Entity**: esta anotación le dice al programa *"esta clase representa una tabla en la base de datos"*.
- **@Table**: permite decirle al programa con qué tabla exacta de la base de datos se quiere conectar tu clase.
- **@Id**: Se utiliza para marcar un campo como clave primaria de una entidad JPA.
- **@GeneratedValue**: genera valores únicos para el Id de la tabla.
- **@ElementCollection**: le dice al programa qué tipo de datos va a contener una lista o colección.
- **@Embeddable**:anotación para declarar que una clase será incrustada por otras entidades.

Estas anotaciones se utlizadorn en la clase *Blueprint* generando los siguientes cambios:

- **Nuevo atributo**: *Blueprint* tiene un nuevo atributo llamado *id*.
- **Modificación de atributo**: la *points* dejó de ser un atributo *final*.
- **Constructor sin argumentos**: *JPA* necesita el constructor vacío porque cuando recupera datos de la base de datos, necesita crear el objeto primero vacío y luego ir rellenando sus campos uno por uno.
- **Nuevo getter**: se generó el *getter* del atributo *id*.

Cambios en la clase *Point*:

- Point dejó de ser un *Record* y se convirtió en una clase normal porque *JPA* necestia un **constructor sin argumentos**.

**2.4.2 Creación de `BlueprintRepository`**

```java
public interface BlueprintRepository extends JpaRepository<Blueprint,Long> {
    Optional<Blueprint> findByAuthorAndName(String author, String name);
    Set<Blueprint> findByAuthor(String author);
}
```

Esta es una interface que exiende a *JpaRepository*. Esta permite comunicarse con la base de datos sin tener que escribir las consultas SQL.


**2.4.3 Creación de `PostgresBlueprintPersistence`**

```java
@Repository
@Primary
public class PostgresBlueprintPersistence implements BlueprintPersistence{
    private BlueprintRepository blueprintRepository;

    public PostgresBlueprintPersistence(BlueprintRepository blueprintRepository) {
        this.blueprintRepository = blueprintRepository;
    }

    @Override
    public void saveBlueprint(Blueprint bp) throws BlueprintPersistenceException {
        Optional<Blueprint> existing = blueprintRepository.findByAuthorAndName(bp.getAuthor(), bp.getName());
        if (existing.isPresent()) throw new BlueprintPersistenceException("Blueprint ya exíste");
        blueprintRepository.save(bp);
    }

    @Override
    public Blueprint getBlueprint(String author, String name) throws BlueprintNotFoundException {
        Optional<Blueprint> existing = blueprintRepository.findByAuthorAndName(author,name);
        if(existing.isEmpty()) throw new BlueprintNotFoundException("Blueprint no encontrado");
        return existing.get();
    }

    @Override
    public Set<Blueprint> getBlueprintsByAuthor(String author) throws BlueprintNotFoundException {
        Set<Blueprint> existing = blueprintRepository.findByAuthor(author);
        if (existing.isEmpty()) throw new BlueprintNotFoundException("Blueprint no encontrado");
        return existing;
    }

    @Override
    public Set<Blueprint> getAllBlueprints() {
        return new HashSet<>(blueprintRepository.findAll());
    }

    @Override
    public void addPoint(String author, String name, int x, int y) throws BlueprintNotFoundException {
        Optional<Blueprint> existing = blueprintRepository.findByAuthorAndName(author,name);
        if (existing.isEmpty()) throw new BlueprintNotFoundException("Blueprint no encontrado");
        Blueprint bd = existing.get();
        bd.addPoint(new Point(x,y));
        blueprintRepository.save(bd);

    }
}
```
Esta clase funciona igual que *InMemoryBlueprintPersistence*, con la diferencia de que en vez de guardar los datos en un *Map* (que se borra al apagar la aplicación), los guarda en una base de datos PostgreSQL de forma permanente usando un *BlueprintRepository*.


---
## 3. Buenas prácticas de API REST

### 3.1 Cambio de path:

Lo que se hizo fue poner el path `/api/v1/blueprints` en el `@RequestMapping(...)` de nuestro *controller*.

### 3.2 Códigos HTTP:

Se usaron los siguientes métodos estáticos para controlar el código HTTP de la respuesta.

- **.ok()**: Se usa para dar la respuesta `200 OK`. Fue implementado en los métodos con el endpont `GET`.
- **.notFound()**: Se usa para dar la respuesta `404 Not Found`. Fue implementado en los métodos del *controller* donde se buscaba por un atributo particular.
- **.status(HttpStatus.CREATED)**: Se usa para dar la respuestra `201 Created`. Fue implementado en el método del *controller* que tenia el verbo `POST`.
- **.status(HttpStatus.ACCEPTED)**: Se usa para dar la respuesta `202 Accepted`. Fue implementado en el método **addPoint()** del *controller*.
- **.status(HttpStatus.CONFLICT)**: Se usa para la respuesta `409 Conflict`. Fue implementado en **.add()** de *controller* que usa el verbo `POST`.

### 3.3 Implementación de clase genérica para dar respuesta:

**3.3.1 Creación de la clase genérica**:

Se creó este *Record* para hacer manualmente nuestras respuestas a solicitudes.
```java
public record ApiResponse<T>(int code, String message, T data) { }
```

**3.3.2 Implementación en el *Controller***:

Se modificó el tipo de retorno de todos los endpoints para que envuelvan la data en un `ApiResponse<T>`, estandarizando así todas las respuestas de la API con el mismo formato:
```json
{
  "code": 200,
  "message": "execute ok",
  "data": {}
}
```

Antes, los endpoints retornaban el objeto directamente:
```java
return ResponseEntity.ok(services.getAllBlueprints());
```

Después, toda respuesta pasa por `ApiResponse`:
```java
ApiResponse<Set<Blueprint>> apiResponse = new ApiResponse<>(200, "execute ok", services.getAllBlueprints());
return ResponseEntity.ok(apiResponse);
```

Esto aplica tanto para respuestas exitosas como para errores, donde `data` es `null` y el `code` y `message` reflejan el error correspondiente.

---
## 4. OpenAPI / Swagger

### 4.1 Dependencia en `pom.xml`

Se agregó la dependencia de `springdoc-openapi` para Spring Boot 3.x:

```xml
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.6.0</version>
</dependency>
```

Esta librería genera automáticamente la documentación de la API leyendo las anotaciones de Spring MVC y expone una interfaz visual interactiva (Swagger UI) sin configuración adicional.

### 4.2 Configuración de `OpenApiConfig`

Se creó la clase `OpenApiConfig` en el paquete `config` para personalizar los metadatos que aparecen en la cabecera de Swagger UI:

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI api() {
        return new OpenAPI().info(new Info()
                .title("ARSW Blueprints API")
                .version("v1")
                .description("Blueprints Laboratory (Java 21 / Spring Boot 3.3.x)"));
    }
}
```

### 4.3 Configuración de `application.properties`

Se agregaron las siguientes propiedades para definir las rutas de acceso a la documentación:

```properties
# Swagger / OpenAPI (springdoc)
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
```

- **`springdoc.api-docs.path`**: ruta donde se expone el JSON con la especificación OpenAPI 3.
- **`springdoc.swagger-ui.path`**: ruta de la interfaz visual interactiva.
- **`springdoc.swagger-ui.enabled`**: activa/desactiva la UI.

Una vez levantada la aplicación, se puede acceder a:

| Recurso | URL |
|---|---|
| Swagger UI | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |
| OpenAPI JSON | [http://localhost:8080/api-docs](http://localhost:8080/api-docs) |

### 4.4 Anotaciones en el controlador

Se enriqueció `BlueprintsAPIController` con anotaciones de documentación para describir cada endpoint:

- **`@Tag`**: agrupa todos los endpoints bajo la etiqueta *Blueprints* en la UI.
- **`@Operation`**: describe el propósito de cada endpoint (resumen y descripción larga).
- **`@ApiResponses` / `@ApiResponse`**: documenta los posibles códigos HTTP de retorno y su significado.
- **`@Parameter`**: describe cada `@PathVariable` con texto legible.

Ejemplo aplicado al endpoint `GET /api/v1/blueprints/{author}`:

```java
@Operation(summary = "Obtener planos por autor",
           description = "Retorna todos los blueprints que pertenecen al autor indicado.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Blueprints del autor encontrados"),
    @ApiResponse(responseCode = "404", description = "Autor no encontrado")
})
@GetMapping("/{author}")
public ResponseEntity<ApiResponse<Set<Blueprint>>> byAuthor(
        @Parameter(description = "Nombre del autor") @PathVariable String author) { ... }
```

Esta combinación permite que Swagger UI muestre, para cada operación, su descripción, los parámetros esperados y los posibles resultados, facilitando el consumo y la prueba de la API directamente desde el navegador.

