# NotificationHub - Сервис работы с уведомлениями

# Назначение сервиса

NotificationHub - это микросервис, который отвечает за работу с уведомлениями:

- Получает уведомления из кафки и сохраняет их в БД
- Отправляет email при регистрации пользователям
- Выдаёт список уведомлений пользователю по запросу 
- Изменяет статус уведомления

# Ключевые функции

1. Получение данных из топика кафки
    - Используется Avro (Schema Registry) для конвертации в DTO
    - Если во входящем сообщении нет userId, считаем что это новое уведомление
    - Если во входящем сообщении есть userId, выполняем поиск и обновление уведомления

# Инструкция по запуску:
 - Запуск инфраструктуры: docker-compose up
 - Доступ к сервисам: 
   - Kafka UI: http://localhost:8082   
   - Schema Registry: http://localhost:8081
   - Внутри Docker: kafka:9092
   - Снаружи Docker: localhost:9093

# Технологический стек
   - Язык: Java 21
   - Фреймворки: Spring Boot 3.2.4, Spring Web MVC, Spring Data JPA, Spring Kafka, Spring Mail
   - База данных: PostgreSQL (+ Flyway для миграций)
   - Работа с сообщениями: Apache Kafka, Apache Avro 1.12.0, Confluent Kafka Avro Serializer 7.8.1
   - Сериализация: Jackson (JSON), Avro (для Kafka сообщений)
   - Утилиты: Lombok 1.18.30, Apache Commons Lang 3.14.0
   - Тестирование: Testcontainers (PostgreSQL, Kafka), GreenMail 2.1.3 (для тестирования email), REST Assured
   - Инфраструктура: Spring Testcontainers, JUnit 5