# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Proyecto

GMV ("Gestión Móvil de Ventas") — app Android de fuerza de ventas de un solo módulo para **UNIMARK S.A.** (Nicaragua). Kotlin, sin Compose: layouts XML + ViewBinding, AppCompat, Material 3. Cubre login, catálogo/carrito/pedidos, gestión de clientes, facturación/cobranza (recibos), solicitudes de incremento de crédito, y un módulo aparte de "Visitador Médico".

Ya existe un `AGENTS.md` en la raíz del repo — sus secciones de Stack/Dependencias/Build son correctas, pero "Project structure" y "UI Patterns" describen una versión temprana de la app (solo 3 fragments) y están desactualizadas (p. ej. dice que el bottom nav lo maneja Navigation Component; eso solo es cierto en el módulo médico — ver abajo). Toma este archivo como la fuente de verdad actual sobre arquitectura.

## Build & Test

```bash
./gradlew assembleDebug              # compilar APK debug
./gradlew testDebugUnitTest          # tests unitarios JUnit4 (app/src/test)
./gradlew connectedDebugAndroidTest  # tests instrumentados (requiere device/emulador)
./gradlew testDebugUnitTest --tests "com.unimarksa.gmv.ExampleUnitTest"   # una sola clase de test
```

`local.properties` (gitignored) debe existir con `sdk.dir=...`. También se lee ahí una propiedad `gmv.app_key` en tiempo de build (`app/build.gradle.kts`), inyectada como `BuildConfig.APP_KEY` y enviada como `APP_KEY` en cada llamada a la API legacy — sin eso en `local.properties` la app compila normal pero el backend rechaza las peticiones (`APP_KEY = "MISSING_KEY"`).

No hay lint/format configurado (ktlint/detekt/spotless) — no hay nada más que correr aparte de las tareas de Gradle de arriba.

## Arquitectura

### Dos estilos de navegación en paralelo — hay que saber en cuál pantalla se está

- **App principal (flujo de ventas)**: `DashboardActivity` aloja un `ViewPager` (NO Navigation Component) con un `ViewPagerAdapter` de 3 tabs (Artículos / Clientes / Perfil) más un bottom nav hecho a mano con `LinearLayout`s planos (no `BottomNavigationView`). Cualquier otra pantalla (carrito, pedidos, facturas, recibos, solicitudes, reportes) es una `Activity` separada lanzada con `Intent`, no un destino de fragment.
- **Módulo médico**: `MedicalDashboardActivity` aloja un `NavHostFragment` real + `nav_graph_medical.xml`; sus fragments navegan con `findNavController().navigate(...)`. Es el único lugar donde realmente se usan destinos de fragment de Navigation Component. `nav_graph.xml` (el otro grafo, casi vacío) está en la práctica sin uso/legacy.

### Networking — un endpoint PHP legacy + una API REST moderna

- `tools/ApiInterface.kt` + `tools/ApiService.kt`: casi todas las llamadas pegan a `api/api.php` sobre `Config.BASE_URL` (o la variante AWS/SAC `Config.BASE_AWS`), diferenciadas solo por qué query param está presente (`get_recent`, `clients_id`, `post_order_recibo`, `del_recibo_colector`, etc. — despacho estilo endpoint-único antiguo, no REST por recurso). Las respuestas llegan como `JsonElement`/string crudo y se parsean a mano en `ApiService` con `org.json` (`extractArray`, funciones `parseXxx`) en vez de mapeo Gson a data class, porque la forma de la respuesta del backend y hasta el casing de los nombres de campo varían (`opt("NOMBRE") ?: opt("nombre") ?: opt("name") ...` en todos lados). Algunos payloads son strings pseudo-CSV delimitados por punto y coma/corchetes (`parseOrderList`, `parseFacturasPendientes`) en vez de JSON, y los pedidos/recibos se *envían* de la misma forma (`formatearOrderList`, `formatearOrderListRecibo`).
- `tools/MedicalApiInterface.kt` + `tools/MedicalApiService.kt`: un cliente Retrofit realmente separado contra `Config.VisitaMedica`, autenticado por bearer token vía un interceptor de OkHttp que lee `SessionManager.getMedicalToken()`. Llamar `ensureAuth(...)` antes de pegarle a endpoints médicos protegidos — re-loguea con el usuario/contraseña guardados si todavía no hay token.
- Trabajo nuevo de integración con el backend debería extender `ApiInterface`/`ApiService` siguiendo el patrón existente (despacho por query param sobre `api.php`, parseo defensivo `opt(...) ?:`), salvo que se esté construyendo explícitamente contra el estilo REST de la API médica.
- `Config.kt` tiene las tres URLs base (`BASE_URL`, `BASE_AWS`, `VisitaMedica`) — se cambian seguido entre IPs de LAN local y el host de producción `apps.gumacorp.com` durante desarrollo; revisar cuál está activa antes de asumir que las llamadas van a llegar a producción.

### Persistencia local — Room, usado solo para estado offline de carrito/pedido/recibo

`data/local/AppDatabase.kt` (Room, `version = 6`, `fallbackToDestructiveMigration()` — sin migraciones reales, aumentar el set de entidades simplemente borra los datos locales) sostiene tres managers singleton independientes en `tools/`:
- `CartManager` — carrito en memoria `LiveData<List<ItemCarrito>>`, persistido en Room por cliente (`switchToClient` vuelca el carrito actual a `CartEntity`/`CartItemEntity` antes de cargar el carrito del nuevo cliente) y se refleja en `OrderEntity`/`OrderItemEntity` al hacer checkout.
- `PagoManager` / el flujo de recibo usa `ReciboEntity`/`ReciboDetalleEntity`/`ReciboCartEntity` de forma similar.
- Estos managers son singletons de todo el proceso (`object`), no están vinculados a una Activity/ViewModel — se inicializan una vez (`.init(database, ...)`) y se leen `.currentItems`/`.items` (LiveData) desde donde se necesite.

### Sesión & auth

`tools/SessionManager.kt` es un singleton respaldado por `SharedPreferences` (`init(context)` debe correr antes de usarlo — se hace en `DashboardActivity.onCreate` y en `LoginActivity`). Guarda la sesión de la app de ventas (usuario/contraseña/perfil) *y* el bearer token del módulo médico por separado (`saveMedicalToken`/`getMedicalToken`/`clearMedicalToken`). El login (`LoginActivity`) autentica contra `ApiService.fetchLogin`, que espera un array `result` de una sola fila con `success == "1"`.

### Pantallas como Activities, no Fragments (fuera de los dos nav hosts de arriba)

La mayoría de features (`CartActivity`, `OrderSummaryActivity`, `FacturaDetailActivity`, `HistoricoFacturaActivity`, `RecibosActivity`, `ReciboColectorActivity`, `SolicitudesActivity`, `SolicitarIncrementoActivity`, `SellerMetricsActivity`, `HistorialLotesActivity`, `OrderPrintActivity`, etc.) son `AppCompatActivity` independientes lanzadas por `Intent` con extras, cada una con su propio RecyclerView + adapter bajo `adapters/`. Al agregar una pantalla nueva en el flujo de ventas, seguir este patrón de Activity-por-pantalla en vez de agregar un destino de fragment/nav — los 3 tabs del ViewPager son los únicos destinos basados en fragment en ese flujo.

### Los modelos son DTOs uno-por-pantalla, no un modelo de dominio compartido

`models/` tiene data classes angostas y específicas de cada pantalla (`ItemProducto`, `ItemCliente`, `ItemDetalleFactura`, `ItemReporteFactura`, `ItemPagoFactura`, `ItemLoteHistorial`, `SolicitudItem`, etc.) que mapean 1:1 con lo que `ApiService` parsea para esa pantalla, más un set paralelo más chico para el módulo médico (`Doctor`, `DoctorStatus`, `MedicalVisit`, `Product`). No asumir que un modelo se reutiliza entre pantallas — revisar si la pantalla que se está tocando tiene su propio `ItemX`.

## Convenciones

- Nomenclatura en español en todo el proyecto (clases, variables, string resources) — mantenerla en código nuevo (`fetchClientes`, `enviarRecibo`, `codCliente`, etc.).
- `edge-to-edge` está habilitado por Activity (`enableEdgeToEdge()`); los insets/color de la status bar se manejan a mano en el `onCreate` de cada Activity (ver `DashboardActivity`), no de forma centralizada.
- `compileSdk` usa la sintaxis de bloque del version catalog de AGP 8.13+ (`compileSdk { version = release(36) }`), no la forma plana `compileSdk = 36`.
- ProGuard/R8 está deshabilitado en builds de release (`isMinifyEnabled = false`).
- **Línea gráfica Unimark 20 años** (compartida con MonitorLoteria): tokens `umk_*` al inicio de `values/colors.xml` (navy `#1B2766`, royal `#2B3B9E`, cyan `#29B6E8`, ink, mist, line, danger). Los nombres viejos (`primary`, `brand_primary`, `accent_primary`, `perfil_accent`, etc.) son alias a esos tokens — en código nuevo usar `@color/umk_*` directo, no hex sueltos. Fuente Archivo (`@font/archivo`, `@font/archivo_medio`) puesta en el tema; cifras con `TextAppearance.GMV.Numero` (Barlow Condensed). Logo: `@drawable/logo_unimark_20`; franja con ondas: `@drawable/bg_band_waves` sobre un bloque `umk_royal`. App solo en modo claro (no hay `values-night`).
