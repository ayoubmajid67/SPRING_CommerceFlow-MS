![1767212063574](image/part9_circuit_breaker/1767212063574.png)
![1767212079211](image/part9_circuit_breaker/1767212079211.png)

we will use resiliency4j 
- rate limiter
- circuit breaker
- bulkhead pattern 
- we will use spring cloud circuit breaker
![1767212327711](image/part9_circuit_breaker/1767212327711.png)


1- add this dependency in the pom.xml of the order service
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
</dependency>
```