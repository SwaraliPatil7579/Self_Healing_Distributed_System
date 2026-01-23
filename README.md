# 🛠️ Self_Healing_Distributed_System
![Java](https://img.shields.io/badge/Java-17+-blue?logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-2.7+-green?logo=spring&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-20.10-blue?logo=docker&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-3.9-red?logo=apachemaven&logoColor=white)
![Git](https://img.shields.io/badge/Git-2.40-orange?logo=git&logoColor=white)

A self-healing distributed system built using Java and Spring Boot that automatically detects service failures and recovers without human intervention.
This project demonstrates fault tolerance 🛡️, health monitoring ❤️‍🩹, and automatic recovery 🔄.

## 📌 Overview

Failures are inevitable in distributed systems due to service crashes, instance unresponsiveness, or network issues 🌐.
The real challenge is not avoiding failures, but detecting and recovering from them automatically.

This project implements a mini yet realistic distributed system that demonstrates how self-healing mechanisms ⚙️ work in practice.

## 🚀 Features

- Multiple independent backend services (nodes)
- Heartbeat-based health monitoring ❤️
- Centralized failure detection 🚨
- Automatic service restart on failure 🔁
- Fault-tolerant request handling 🛡️
- Detailed logging for observability 📊
- Live failure simulation and recovery demo 🎥

## 🧱 System Architecture

`Client`<br>
`⬇️`<br>
`Service A — Service B — Service C`<br>
`⬇️           ⬇️         ⬇️`<br>
`Heartbeat  Heartbeat  Heartbeat`<br>
`      ⬇️     `<br>
`Health Monitor 🩺`<br>
`⬇️`<br>
`Failure Detection 🚨`<br>
`⬇️`<br>
`Automatic Recovery 🔄`


**How it works:**
- Each service sends periodic heartbeat signals
- Health Monitor tracks service availability
- Missing heartbeats indicate a failure
- Failed services are automatically restarted
- Requests are routed to healthy instances

## 🧑‍💻 Technology Stack

- ☕ Java 17+
- 🌱 Spring Boot
- 🌐 REST (HTTP)
- ❤️ Custom heartbeat mechanism
- 📜 Logback logging
- 🐳 Docker
- 🧩 Docker Compose
- 🔧 Git

## 🔄 Failure Detection and Recovery Flow

1. A service crashes or becomes unresponsive ❌
2. Heartbeat signals stop ❤️‍🩹
3. Health Monitor detects the failure 🩺
4. Failure is logged 📜
5. A replacement service instance is started 🔁
6. Requests are routed to healthy services ✅
7. The system continues functioning 🟢

## 🧪 Failure Simulation

The system supports controlled failure testing 🧯:

- Manual service shutdown
- Crash simulation
- Observation of automatic recovery through logs 📊

Ideal for demonstrations and interviews 🎤.

## 📂 Project Structure

self-healing-distributed-system/<br>
├── service-a/<br>
├── service-b/<br>
├── service-c/<br>
├── health-monitor/<br>
├── docker-compose.yml<br>
└── README.md


## 🛠️ Setup and Run Instructions

📦 Prerequisites

- Java 17+
- Maven
- Docker
- Docker Compose

▶️ Steps to Run
git clone <repository-url>
cd self-healing-distributed-system
mvn clean package
docker-compose up


System logs will display heartbeat activity ❤️, failure detection 🚨, and automatic recovery 🔄.

## 📊 Observability

Logs provide visibility into:

- Service health status 🟢
- Failure detection events 🚨
- Recovery actions 🔁

## ⚠️ Limitations

- Designed for learning and demonstration purposes 🎓
- Not intended for large-scale production workloads
- No distributed system is completely bug-free 🐞

The system is fault-tolerant 🛡️ and tested under defined failure scenarios.

## 🎯 Learning Outcomes

- Understanding distributed system failures 🧠
- Implementing self-healing mechanisms 🔄
- Building resilient backend services 🏗️
- Practical experience with Spring Boot microservices 🌱
- Exposure to DevOps fundamentals ⚙️

## 🔮 Future Enhancements

- Leader election 🗳️
- Dynamic service discovery 🧭
- Circuit breakers ⚡
- Metrics using Prometheus and Grafana 📈
- Kubernetes-based orchestration ☸️

## 👩‍💻 Author

Swarali Patil<br>
Computer Engineering Student at VIT Pune🎓
