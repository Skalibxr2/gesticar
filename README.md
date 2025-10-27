# Gesticar

**Autor:** Hans Marmolejo

Gesticar es una aplicación Android desarrollada con Jetpack Compose para administrar órdenes de trabajo (OT) de un taller mecánico. Permite que administradores y mecánicos gestionen clientes, vehículos y el estado de las OTs de forma sencilla.

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
