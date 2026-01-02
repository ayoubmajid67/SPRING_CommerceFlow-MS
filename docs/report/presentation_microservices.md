---
marp: true
theme: default
paginate: true
backgroundColor: #fff
backgroundImage: url('https://marp.app/assets/hero-background.svg')
---

<!-- _class: lead -->
<!-- _paginate: false -->

# SPRING_CommerceFlow-MS

## Conception et Développement d'une Plateforme E-Commerce Basée sur une Architecture Microservices

**Réalisé par :** MAJJID Ayoub & EL HILALI Ayman
**Encadré par :** Prof. Hatim JAADOUNI
**Année Universitaire :** 2025-2026

---

<!-- _class: lead -->

# Plan de Présentation

1. **Contexte & Problématique**
2. **Architecture & Conception**
3. **Implémentation Technique**
4. **Démonstration Live**
5. **Conclusion & Perspectives**

---

# Problématique

## Limites des Architectures Monolithiques

| Problème | Impact |
|----------|--------|
| ❌ **Couplage fort** | Redéploiement complet pour chaque modification |
| ❌ **Scalabilité limitée** | Impossible de scaler un composant indépendamment |
| ❌ **Technologie unique** | Obligation d'utiliser le même langage |
| ❌ **Déploiement risqué** | Un bug affecte l'ensemble du système |

---

# Objectifs du Projet

✅ **Architecture microservices** scalable et résiliente

✅ **Sécurité OAuth2/OIDC** avec Keycloak

✅ **Service Discovery** avec Eureka

✅ **Résilience** avec Circuit Breaker (Resilience4j)

✅ **Persistance polyglotte** (MongoDB + MySQL)

✅ **Tests automatisés** (> 70% coverage)

---

<!-- _class: lead -->

# Architecture Globale

---

# Architecture du Système

```
┌─────────────────────────────────────────────────┐
│              API GATEWAY (Port 9000)            │
│         • Routage  • Sécurité  • Load Balancing │
└────────────────────┬────────────────────────────┘
                     │
    ┌────────────────┼────────────────┐
    │                │                │
┌───▼────┐      ┌───▼────┐      ┌───▼────┐
│PRODUCT │      │ ORDER  │      │INVENTORY│
│SERVICE │      │SERVICE │      │SERVICE  │
│:8080   │      │:8081   │      │:8082    │
└───┬────┘      └───┬────┘      └───┬─────┘
    │               │                │
┌───▼────┐      ┌───▼────┐      ┌───▼─────┐
│MongoDB │      │ MySQL  │      │ MySQL   │
└────────┘      └────────┘      └─────────┘
```

**Eureka Server (8761)** | **Keycloak (8181)**

---

# Composants Principaux

| Service | Port | Base de Données | Rôle |
|---------|------|-----------------|------|
| **Product Service** | 8080 | MongoDB | Gestion du catalogue |
| **Order Service** | 8081 | MySQL | Traitement des commandes |
| **Inventory Service** | 8082 | MySQL | Gestion des stocks |
| **API Gateway** | 9000 | - | Point d'entrée unique |
| **Eureka Server** | 8761 | - | Service Discovery |
| **Keycloak** | 8181 | - | Authentification IAM |

---

# Service Discovery avec Eureka

## Problème
Comment router les requêtes quand les services changent d'adresse ?

## Solution : Eureka + LoadBalancer

1. **Enregistrement** : Chaque service s'enregistre auprès d'Eureka
2. **Discovery** : Le Gateway consulte Eureka pour trouver les instances
3. **Load Balancing** : Sélection Round-Robin entre instances disponibles

**Avantage** : Scaling dynamique sans redémarrage du Gateway

---

# Flux de Service Discovery

```
1. Client → GET /api/product/1

2. Gateway → ServiceResolver.resolve("product-service")

3. LoadBalancerClient.choose("product-service")
   ├─► Consulte cache Eureka local
   ├─► Instances: [localhost:8080, localhost:8090]
   └─► Sélectionne: localhost:8080 (Round-Robin)

4. MvcUtils.setRequestUrl(resolvedUri)

5. HTTP Request → http://localhost:8080/api/product/1
```

---

# Sécurité OAuth2 avec Keycloak

## Flux d'Authentification

1. **Login** : Utilisateur → Keycloak
2. **Token JWT** : Keycloak → Client (Access Token + Refresh Token)
3. **API Request** : Client → Gateway (avec Bearer Token)
4. **Validation** : Gateway valide le JWT
5. **Forward** : Gateway → Microservice (avec token)

**Access Token** : 5 minutes
**Refresh Token** : 30 minutes

---

# Types de Tokens

| Caractéristique | Access Token | Refresh Token |
|-----------------|--------------|---------------|
| **Objectif** | Autoriser les requêtes API | Obtenir de nouveaux tokens |
| **Durée** | Courte (5 min) | Longue (30 min) |
| **Envoyé à** | Gateway + APIs | Keycloak uniquement |
| **Contenu** | User info + rôles | Identifiant de référence |
| **Si volé** | Dommages limités | Peut obtenir nouveaux tokens |

---

# Résilience avec Resilience4j

## Circuit Breaker Pattern

```
CLOSED (Normal)
   │ (50% échecs)
   ▼
OPEN (Rejet immédiat)
   │ (après 5s)
   ▼
HALF_OPEN (3 tests)
   ├─ Succès → CLOSED
   └─ Échec → OPEN
```

**Configuration** :
- Fenêtre glissante : 10 appels
- Seuil d'échec : 50%
- Durée OPEN : 5 secondes

---

# Patterns de Résilience

| Pattern | Rôle | Configuration |
|---------|------|---------------|
| **Circuit Breaker** | Protège contre défaillances | 50% échecs → OPEN |
| **TimeLimiter** | Limite temps d'attente | Timeout 3s |
| **Retry** | Réessaie en cas d'échec | 3 tentatives max |
| **Rate Limiter** | Contrôle le trafic | Anti-abus |

**Flux** : RateLimiter → TimeLimiter → Retry → CircuitBreaker → Service

---

<!-- _class: lead -->

# Implémentation Technique

---

# Stack Technologique

## Backend
- **Spring Boot** 4.0.0
- **Spring Cloud** 2025.1.0
- **Java** 21

## Bases de Données
- **MongoDB** (Product Service)
- **MySQL 8.x** (Order & Inventory)

## Infrastructure
- **Eureka** (Service Discovery)
- **Keycloak** (IAM)
- **Resilience4j** (Circuit Breaker)
- **Docker** (Conteneurisation)

---

# Communication Inter-Services

## OpenFeign Client

```java
@FeignClient(name = "inventory-service")
public interface InventoryClient {
    
    @GetMapping("/api/inventory/{skuCode}")
    InventoryResponse checkInventory(
        @PathVariable String skuCode
    );
}
```

**Avantages** :
- Déclaratif et type-safe
- Facilement testable (WireMock)
- Intégration native avec Eureka

---

# ServiceResolver Custom

## Gateway MVC nécessite une résolution manuelle

```java
@Component
public class ServiceResolver {
    
    private final LoadBalancerClient loadBalancerClient;
    
    public URI resolve(String serviceName, String path) {
        ServiceInstance instance = 
            loadBalancerClient.choose(serviceName);
        
        return URI.create(
            instance.getUri().toString() + path
        );
    }
}
```

---

# Routes.java - Gateway Configuration

```java
private RouterFunction<ServerResponse> createServiceRoute(
    String serviceName, String pathPrefix
) {
    return GatewayRouterFunctions.route(serviceName)
        .route(RequestPredicates.path(pathPrefix), request -> {
            String path = request.requestPath()
                .pathWithinApplication().value();
            
            URI resolvedUri = serviceResolver.resolve(
                serviceName, path
            );
            
            MvcUtils.setRequestUrl(newRequest, resolvedUri);
            return HandlerFunctions.http().handle(newRequest);
        })
        .filter(CircuitBreakerFilterFunctions.circuitBreaker(
            serviceName + "CircuitBreaker",
            URI.create("forward:/fallbackRoute")
        ))
        .build();
}
```

---

# Tests & Qualité

## Pyramide des Tests

| Type | Outil | Couverture |
|------|-------|------------|
| **Tests Unitaires** | Mockito | 100% |
| **Tests d'Intégration** | MockMvc | 70% |
| **Tests E2E** | RestAssured | Flux principaux |
| **Tests Services Externes** | WireMock | Feign Clients |

**Couverture globale** : > 70%

---

# Persistance Polyglotte

## Pourquoi MongoDB pour Product ?
- ✅ Schéma flexible (attributs variables)
- ✅ Performance de lecture optimisée
- ✅ Scalabilité horizontale facile

## Pourquoi MySQL pour Order & Inventory ?
- ✅ Conformité ACID (transactions)
- ✅ Intégrité référentielle
- ✅ Requêtes complexes (JOIN)

---

<!-- _class: lead -->

# Démonstration Live

---

# Scénario de Démo

## 1. Dashboard Eureka
Visualisation des services enregistrés

## 2. Authentification Keycloak
Login → JWT Token

## 3. Flux E-Commerce Complet
Catalogue → Stock → Commande

## 4. Circuit Breaker en Action
Arrêt service → Fallback

---

# Résultats du Projet

## Réalisations ✅

- Architecture microservices complète
- 4 services métier fonctionnels
- Sécurité OAuth2/OIDC opérationnelle
- Service Discovery & Load Balancing
- Mécanismes de résilience (Circuit Breaker, Retry, TimeLimiter)
- Tests automatisés (> 70% coverage)

**Progression globale : ~78%**

---

# État d'Avancement

| Composant | Statut | Progression |
|-----------|--------|-------------|
| Product Service | ✅ Terminé | 100% |
| Order Service | ✅ Terminé | 100% |
| Inventory Service | ✅ Terminé | 100% |
| API Gateway | ✅ Terminé | 100% |
| Eureka Service Discovery | ✅ Terminé | 100% |
| Sécurité Keycloak | ✅ Terminé | 100% |
| Circuit Breaker | ✅ Terminé | 100% |
| Notification Service | 🟡 En cours | 30% |
| Frontend | 🟡 En cours | 50% |

---

# Perspectives

## Travaux Futurs 🔄

1. **Notification Service** avec Apache Kafka
2. **Frontend React** complet
3. **Pipeline CI/CD** (Jenkins/GitLab)
4. **Distributed Tracing** (Zipkin/Tempo)
5. **Centralized Logging** (Loki/ELK)
6. **Déploiement Kubernetes**
7. **Monitoring avancé** (Prometheus + Grafana)

---

# Défis Rencontrés & Solutions

| Défi | Solution |
|------|----------|
| Configuration JWT | Utilisation de `issuer-uri` au lieu de `jwk-set-uri` |
| Communication inter-services | Migration vers OpenFeign |
| Gateway MVC + Eureka | ServiceResolver custom avec LoadBalancerClient |
| Tests Feign Clients | WireMock pour simuler les APIs |

---

# Leçons Apprises

## Bonnes Pratiques Adoptées

✅ **Architecture Layered** : Séparation Controller → Service → Repository

✅ **DTO Pattern** : Isolation des entités de base de données

✅ **Configuration externalisée** : Profils Spring

✅ **Tests automatisés** : Pyramide complète

✅ **Documentation** : Swagger/OpenAPI

---

<!-- _class: lead -->

# Conclusion

---

# Conclusion

## Ce projet démontre :

- ✅ Faisabilité d'une architecture microservices moderne
- ✅ Avantages du découplage et de la scalabilité
- ✅ Importance de la sécurité centralisée
- ✅ Nécessité des mécanismes de résilience
- ✅ Valeur des tests automatisés

**Spring Boot + Spring Cloud** = Solution production-ready pour applications e-commerce

---

<!-- _class: lead -->

# Merci pour votre attention !

## Questions ?

**MAJJID Ayoub & EL HILALI Ayman**

---

# Annexe : Endpoints API

## Product Service (8080)
- `GET /api/product` - Liste des produits
- `POST /api/product` - Créer un produit
- `GET /api/product/{id}` - Détails d'un produit

## Order Service (8081)
- `GET /api/order` - Liste des commandes
- `POST /api/order` - Passer une commande
- `POST /api/order/{id}/cancel` - Annuler

## Inventory Service (8082)
- `GET /api/inventory/{sku}` - Vérifier stock
- `POST /api/inventory/{sku}/sell` - Vendre
- `POST /api/inventory/{sku}/purchase` - Acheter

---

# Annexe : Configuration Eureka

## Eureka Server
```properties
server.port=8761
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
```

## Eureka Client (Services)
```properties
spring.application.name=product-service
eureka.client.service-url.defaultZone=
    http://localhost:8761/eureka/
```

---

# Annexe : Circuit Breaker Config

```properties
# Circuit Breaker
resilience4j.circuitbreaker.configs.default.sliding-window-size=10
resilience4j.circuitbreaker.configs.default.minimum-number-of-calls=5
resilience4j.circuitbreaker.configs.default.failure-rate-threshold=50
resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state=5s

# TimeLimiter
resilience4j.timelimiter.configs.default.timeout-duration=3s

# Retry
resilience4j.retry.configs.default.max-attempts=3
resilience4j.retry.configs.default.wait-duration=2s
```
