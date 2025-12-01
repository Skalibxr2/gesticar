# Gesticar

**Autor:** Hans Marmolejo

Gesticar es una aplicación Android desarrollada con Jetpack Compose para administrar órdenes de trabajo (OT) de un taller mecánico. Permite que administradores y mecánicos gestionen clientes, vehículos y el estado de las OTs de forma sencilla.

## Backend de microservicios (Spring Boot)

El proyecto incluye un backend en `backend/` creado con Spring Boot 3.4, que expone recursos REST para clientes, vehículos, órdenes de trabajo (OT), tareas y presupuestos, con persistencia en PostgreSQL y migraciones Flyway.

### Prerrequisitos

- Docker/Docker Compose para levantar PostgreSQL.
- JDK 17.
- Maven (ya viene con `./backend/mvnw`).

### Levantar la base de datos

1. Ubícate en el directorio `backend/`.
2. Ejecuta `docker compose up -d` para iniciar PostgreSQL con la base `gesticar`, usuario `gesticar` y contraseña `gesticar` en el puerto 5432.

Las migraciones Flyway (`src/main/resources/db/migration`) crean las tablas de clientes, vehículos, OTs, tareas y presupuestos, y cargan datos de ejemplo.

### Ejecutar el backend

1. Desde `backend/`, inicia el servidor con:

   ```bash
   ./mvnw spring-boot:run
   ```

2. El backend quedará disponible en `http://localhost:8080` (configurable con la variable `PORT`). Las variables `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` permiten apuntar a otra base de datos si lo necesitas.

### Poblar y validar datos

Al primer arranque, Flyway ejecuta `V1__init.sql` y carga datos de ejemplo:

- Clientes: Ana Carvallo y Luis Farias.
- Vehículos: placas `AA-BB-11` y `CC-DD-22`.
- OTs: `OT-001` (BORRADOR) y `OT-002` (INICIADA) con sus tareas y presupuestos.

### Endpoints principales

Todos los endpoints comienzan en `/api` y envían/reciben JSON.

- **Clientes** `/api/clientes`
  - `GET /api/clientes` lista todos; `GET /api/clientes?rut=12.345.678-9` filtra por RUT.
  - `POST /api/clientes` crea un cliente nuevo.
- **Vehículos** `/api/vehiculos`
  - `GET /api/vehiculos` lista todos; `GET /api/vehiculos?customerId=1` lista por cliente.
  - `POST /api/vehiculos` crea un vehículo asociado a un cliente.
- **Órdenes de trabajo (OT)** `/api/ots`
  - `GET /api/ots` lista todas o por `customerId`.
  - `POST /api/ots` crea una OT (con `status` opcional; por defecto `BORRADOR`).
  - `PATCH /api/ots/{id}/estado` actualiza el estado (`BORRADOR`, `INICIADA`, `FINALIZADA`).
- **Tareas** `/api/tareas`
  - `GET /api/tareas` lista todas o por `workOrderId`.
  - `POST /api/tareas` crea una tarea ligada a una OT.
- **Presupuestos** `/api/presupuestos`
  - `GET /api/presupuestos` lista todos o por `workOrderId`.
  - `POST /api/presupuestos` crea un presupuesto para una OT.

Ejemplo de prueba rápida (con datos iniciales):

```bash
curl http://localhost:8080/api/ots
curl -X POST http://localhost:8080/api/clientes \
  -H "Content-Type: application/json" \
  -d '{"rut":"11.111.111-1","firstName":"Nuevo","lastName":"Cliente","phone":"+56912345678","email":"nuevo@correo.com"}'
```

Puedes apuntar la app Android a este backend configurando la URL base `http://10.0.2.2:8080` si usas el emulador de Android Studio.

## Funcionalidades principales

Autenticación y roles: acceso con perfiles de Administrador y Mecánico.

Creación de Órdenes de Trabajo (OT): ingresa el RUT del cliente.

Si ya existe en la base de datos, los campos del formulario se completan automáticamente.

Si no existe, podrás registrar un nuevo cliente.

Es posible agregar vehículos a cada cliente; en la OT solo se puede seleccionar un vehículo.

Se pueden adjuntar fotos desde la cámara o la galería.

Permite asignar uno o más mecánicos por OT.

La OT se crea en estado Borrador; puede guardarse y seguir editando.

Una vez aprobado el presupuesto, el estado puede cambiar a Iniciada.


Búsqueda y consulta: localiza OTs existentes para visualizar su información detallada y, según permisos, editarlas.

Gestión asociada a la OT: administración de tareas, presupuestos y estados.


Requisitos previos

Android Studio Giraffe o posterior.

JDK 17 instalado y configurado.

Emulador o dispositivo con Android 8.0 (API 26) o superior.


Pasos para iniciar la aplicación

1. Inicia sesión con las credenciales de ejemplo mostradas en la pantalla de acceso.


2. En el menú principal, elige entre:

Crear nueva OT: completa o verifica los datos del cliente (vía RUT), selecciona el vehículo, adjunta fotos, asigna mecánicos y guarda en Borrador. Cuando el presupuesto sea aprobado, cambia el estado a Iniciada.

Buscar OT existente: usa el buscador para consultar o editar la orden según tus permisos. o superior.
- JDK 17 instalado y configurado en el sistema.
- Emulador Android o dispositivo físico con Android 8.0 (API 26) o superior.

## Pasos para iniciar la aplicación.

ingresa al login con las credenciales mostradas en el inicio de sesión.

una vez en el menú principal podrás crear una nueva OT o buscar una ya creada para editarla.
