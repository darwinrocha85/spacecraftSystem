# Proyecto Spacecraft

Este proyecto es una API REST para gestionar naves espaciales. La aplicación está construida utilizando **Spring Boot 3** y está diseñada para interactuar con una base de datos **H2** en memoria. También implementa **caché en memoria** con **Caffeine** para optimizar las consultas.

## Tecnologías Utilizadas

- **Framework**: Spring Boot 3
- **Base de Datos**: H2 (base de datos en memoria)
- **Cache**: Caffeine
- **Lombok**: Para simplificar la creación de entidades con getters, setters y otros métodos
- **Swagger**: Documentación interactiva de API (opcional)

## Configuración del Proyecto

### Base de Datos H2

Este proyecto usa una base de datos H2 en memoria que se inicializa al inicio de la aplicación. Puedes acceder a la consola H2 en `http://localhost:8080/h2-console` para inspeccionar la base de datos en tiempo de ejecución.

### Cache en Memoria (Caffeine)

La configuración de cache está centralizada para almacenar las consultas de naves espaciales por su ID, reduciendo el tiempo de respuesta en búsquedas repetidas.

## Endpoints

La API cuenta con los siguientes endpoints:

| Método HTTP | Endpoint                         | Descripción                                                                                                                                   |
|-------------|----------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| `GET`       | `/api/spacecrafts`               | Obtener todas las naves espaciales.                                                                                                           |
| `GET`       | `/api/spacecrafts/page`          | Obtener todas las naves espaciales con paginación. Acepta parámetros `page`, `size`, `sortBy`, `sortDirection`.                              |
| `GET`       | `/api/spacecrafts/{id}`          | Obtener una nave espacial por su ID. Este endpoint utiliza caché.                                                                             |
| `POST`      | `/api/spacecrafts`               | Crear una nueva nave espacial.                                                                                                               |
| `PUT`       | `/api/spacecrafts/{id}`          | Actualizar una nave espacial existente por su ID.                                                                                            |
| `DELETE`    | `/api/spacecrafts/{id}`          | Eliminar una nave espacial por su ID.                                                                                                        |
| `GET`       | `/api/spacecrafts/search`        | Obtener todas las naves espaciales que contengan en su nombre el texto proporcionado en el parámetro `name`, con paginación opcional.        |

## Configuración de la Colección Postman

Una colección Postman está disponible para probar cada uno de los endpoints con diferentes escenarios. Esta colección incluye casos de prueba para:

- **Crear una nave espacial**
- **Obtener todas las naves espaciales**
- **Obtener todas las naves con paginación**
- **Buscar naves por ID** (probando el cache)
- **Buscar naves que contengan un nombre específico**
- **Actualizar una nave**
- **Eliminar una nave**

### Importar la Colección Postman

1. Descarga el archivo `spacecraft-collection.json` y guárdalo localmente.
2. Abre Postman y selecciona "Importar".
3. Carga el archivo `spacecraft-collection.json` para importar la colección en tu entorno de Postman.
4. Asegúrate de que la aplicación esté ejecutándose en `http://localhost:8080` para probar correctamente la colección.

## Ejecución

1. Clona este repositorio en tu máquina local.
2. Ejecuta la aplicación con `./mvnw spring-boot:run` (Linux/macOS) o `mvnw.cmd spring-boot:run` (Windows).
3. Accede a `http://localhost:8080` en tu navegador o desde Postman para realizar pruebas con la API.

## Contribuir

Para contribuir a este proyecto, por favor sigue estos pasos:

1. Haz un fork del repositorio.
2. Crea una nueva rama (`git checkout -b feature-nueva-funcionalidad`).
3. Realiza tus cambios y haz un commit (`git commit -am 'Añadir nueva funcionalidad'`).
4. Haz push a la rama (`git push origin feature-nueva-funcionalidad`).
5. Crea un nuevo Pull Request.

## Licencia

Este proyecto está bajo la Licencia MIT. Consulta el archivo `LICENSE` para obtener más detalles.
