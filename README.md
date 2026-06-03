# GMV — Gestión Móvil de Ventas

Aplicación Android para la gestión de ventas, pedidos, clientes y facturación de **UNIMARK S.A.**, desarrollada en Kotlin con Material Design 3.

## ✨ Funcionalidades

- **Autenticación** — Inicio de sesión con usuario/contraseña + soporte biométrico
- **Dashboard** — Pantalla principal con navegación inferior y búsqueda global
- **Artículos / Catálogo** — Exploración de productos con imágenes, SKU, precios, tallas y bonificaciones
- **Carrito de compras** — Agregar/quitar productos, cálculo de subtotal, IVA y total
- **Pedidos** — Creación, confirmación, resumen y detalle de pedidos
- **Clientes** — Listado, detalle, historial de pedidos por cliente
- **Facturación** — Facturas pendientes, detalle, registro de pagos/abonos
- **Recibos** — Gestión de recibos colector con detalle de pagos
- **Reportes** — Reporte de facturación por fechas, histórico de facturas
- **Módulo Visitador Médico** — Gestión de médicos, registro de visitas, catálogo de productos médicos
- **Métricas de Vendedor** — Estadísticas de ventas, recuperación y artículos
- **Solicitudes** — Solicitudes de incremento de crédito (pendientes/autorizadas)
- **Historial de Lotes** — Seguimiento de lotes de productos
- **Impresión de Pedidos** — Vista optimizada para impresión

## 🛠 Stack

| Componente | Versión |
|---|---|
| Kotlin | 2.0.21 |
| Android Gradle Plugin | 8.13.2 |
| Gradle | 8.13 |
| Min SDK / Target SDK | 28 / 36 |
| Java | 11 |
| ViewBinding | ✅ |
| Material Design 3 | ✅ |

### Librerías principales

- **AndroidX** — AppCompat, Core KTX, Activity, Fragment, ConstraintLayout, Navigation, ViewPager2, SwipeRefreshLayout, RecyclerView, CardView
- **Material 3** — `com.google.android.material:material`
- **Retrofit2 + OkHttp** — Consumo de API REST
- **Room** — Base de datos local (con KSP)
- **Picasso** — Carga de imágenes
- **CircleImageView** — Avatares circulares
- **MaterialDateTimePicker** — Selector de fechas
- **Biometric** — Autenticación biométrica

## 📁 Estructura del proyecto

```
app/src/main/java/com/unimarksa/gmv/
├── activities/         # Activities (Login, Dashboard, Detalle, Carrito, etc.)
├── adapters/           # RecyclerView adapters
├── data/
│   └── local/          # Room database, DAOs, entities
├── fragments/          # Fragmentos (Articulos, Clientes, Perfil, Médico, etc.)
├── models/             # Data classes
├── tools/              # Utilidades (Config, SessionManager, ApiService, etc.)
└── views/              # Custom views (ArcProgressView, MetricPillView, etc.)
```

## 🚀 Cómo empezar

### Requisitos

- Android Studio Hedgehog o posterior
- JDK 11
- SDK de Android 36

### Clonar y compilar

```bash
git clone https://github.com/tu-usuario/gmv-kotlin.git
cd gmv-kotlin
```

Crea un archivo `local.properties` con la ruta de tu SDK:
```properties
sdk.dir=C:\\Users\\tu-usuario\\AppData\\Local\\Android\\Sdk
```

Compila e instala:
```bash
./gradlew assembleDebug
```

### Pruebas

```bash
# Pruebas unitarias (JUnit 4)
./gradlew testDebugUnitTest

# Pruebas instrumentadas (requiere dispositivo/emulador)
./gradlew connectedDebugAndroidTest
```

## 🔌 API

La app consume una API REST interna configurada en `Config.kt`:
- `BASE_URL` — Endpoint principal del backend GMV
- `BASE_AWS` — Endpoint del módulo SAC

## 🏗 Arquitectura

- **Patrón**: Activities + Fragments con ViewBinding
- **Navegación**: Navigation Component con `BottomNavigationView`
- **UI**: XML layouts con ConstraintLayout y Material Design 3
- **Persistencia local**: Room (SQLite) para carrito, órdenes y recibos off-line
- **Red**: Retrofit2 + OkHttp con interfaz REST definida en `ApiInterface.kt`

## 📄 Licencia

Propietario — UNIMARK S.A.
