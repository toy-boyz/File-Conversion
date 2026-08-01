<div align="center">
<img width="866" height="696" alt="Image" src="https://github.com/user-attachments/assets/8377836b-fe79-44f3-bde6-a9baf019bdac" />

# 🚀 파일 변환 서비스
**이미지(PNG, JPG, BMP)를 PDF로 간편하게 변환하세요.**


</div>

## 📖 서비스 소개
- 회원가입없이 사용자의 이미지 파일을 PDF 문서로 변환하여 실시간으로 신속하고 안전하게 전달합니다.
- 이미지 파일 변환 목적만을 가진 사용자를 위한 별도의 설정없는 직관적인 UI 를 통해 누구나 쉽게 이용 가능합니다.
- 변환 건에 대한 내역이 실시간으로 통계에 누적반영되어 서비스의 신뢰성을 보장합니다.
<br>

## 👤 멤버 구성

| 김주원 | 진승우 |
| :---: | :---: |
| <img src="" width="200" alt="김주원 프로필 사진"> | <img width="300" height="300" alt="image" src="https://github.com/user-attachments/assets/940719ea-ab91-45d1-9cb6-7415b6777f02" /> |
| [@goyois](https://github.com/goyois) | [@SeungWoo1222](https://github.com/SeungWoo1222) |

---
## 1.개발 환경
<div align="center">
<img width="1792" height="1063" alt="undefined (1)" src="https://github.com/user-attachments/assets/1bdf15e4-afac-4277-ad67-a2f02c9df7e4" />
</div>

[Backend]
- Language: Java 17
- Framework: Spring Boot 3.4
- Database: MySQL, Redis
- Event Data: Debezium (CDC)
- Message Broker: RabbitMQ, Redis
- Real-time: SSE (Server-Sent Events)
- Container: Docker
- Monitoring: Prometheus, Grafana
- Test: Artillery (Stress Test)
- Alarm: Slack API


[Front]
- View Engine: Thymeleaf 
- Styling: Bootstrap 5 (CDN), Custom CSS (Dark Theme) 
- Scripting: Vanilla JavaScript (ES6+) 
- Real-time: Server-Sent Events (SSE)

---

## 2.기술 채택 이유와 사용 사례

- #### Event Driven Architecture
  - #### 채택 이유
    - 기존 Monolithic 구조의 강결합으로 인해 발생되는 응답 지연 증가, 시스템 병목 발생, 서비스 장애 전파, 확장성 문제가 발생.
  - #### 사용 사례 및 효율성
    - 사용자의 요청과 응답을 처리하는 API 서버, 무거운 변환 작업을 수행하는 Worker 서버, 이벤트 트리거 역할의 CDC, 메시지 큐로 나누어 서비스 간 관심사 분리 및 시스템 확장성을 확보.
    - 개별적인 서버 구조를 통해 변환을 담당하는 특정 모듈 서버가 죽어도 비즈니스 연속성을 보장.
    - 비동기 워크플로우를 통해 워커 서버의 변환작업 효율 최적화 및 무거운 변환중에도 API 서버의 스레드 차단 방지 및 쾌적한 UX 를 제공.
- #### RabbitMQ(메시지 큐)
  - #### 채택 이유
    - 파일 변환 서비스의 경우 순서 보장이 불필요한 특징을 고려하여 Kafka의 파티션 기반 순서 보장 메커니즘보다는 RabbitMQ의 유연한 메시지 배분과 병렬 확장성이 더 적합하다고 판단하여 채택.      
    - 파일 변환은 CPU와 메모리 사용량이 높아 사용자가 동시에 파일을 변환하는 경우 HTTP 연결을 유지하며 변환을 같이 진행하면 스레드 고갈을 유발해 전체 서비스의 마비가 올 수 있다고 판단.
    - UX 개선을 위해 사용자에게 즉시 응답을 전달, 이후 Worker 서버가 변환 프로세스를 진행하여 부하를 분산하는 아키텍처를 채택함으로써 Worker 서버의 트리거 역할을 해줄 미들웨어 필요.
    - 추후 트래픽 급증 시 변환 작업에 불필요한 로직이 포함된 API 서버를 확장하지않고 Consumer Thread 를 늘리거나, Worker 서버를 스케일아웃하여 대응 가능.
  - #### 사용 사례 및 효율성
    - 사용자의 변환 요청이 들어올 경우 Debezium 을 통해 전송된 메타 데이터가 exchange 를 통해 큐로 삽입되어 Worker 서버가 해당 메시지를 수신하여 변환을 시작하는 파이프 라인 구축하여 빠른 응답성을 유지.
    - Acknowledgment 를 개발자가 직접 제어하는 환경을 구성힌 뒤 변환 작업이 완료되는 경우에만 Ack 를 보내 소비시켜 원자성 보장.
    - 일시적인 오류로 인해 3번 이상 변환 실패 시 DeadLetterQueue 로 전송시켜 특정 메시지의 무한 재시도 현상 방어.
    - 주기적으로 DB를 조회하는 폴링(Polling) 방식의 스케줄링 대신, 이벤트 기반의 비동기 방식을 채택하여 불필요한 DB I/O 누적 부하를 제거하고, 실제 작업이 있을 때만 워커가 동작하게 하여 서버 리소스를 효율적으로 활용.
- #### Redis
  - #### 채택 이유
    - 파일 변환은 서버 리소스 상황과 파일 크기에 따라 소요 시간이 가변적이며, 사용자가 매번 새로고침을 하거나 수동으로 조회(Polling)하는 불편함을 해소하고, 변환 진행률과 성공 여부를 즉각적으로 전달하기 위해 실시간 푸시(Push) 메커니즘이 필수적이라고 판단
    - 디스크 기반의 메시지 브로커보다 훨씬 빠른 지연 시간(Low Latency)을 보장합니다. 짧은 간격으로 발생하는 수많은 상태 업데이트 메시지를 처리하기에 가장 적합하다고 판단하여 채택.
  - #### 사용 사례 및 효율성
    - Worker 서버가 파일 변환 과정의 각 단계(시작, 진행 중, 완료, 실패)에서 상태 메시지를 발행(Publish)하면, API 서버가 이를 구독(Subscribe)하여 클라이언트에게 즉시 전달하는 파이프라인을 구축.
    - 메모리상에서 즉시 전파 형태로 리소스 소모가 적으며, 대량의 변환 요청이 동시에 발생하더라도 상태 업데이트 지연을 최소화.
- #### SSE
  - #### 채택 이유
    - 양방향 통신이 불필요한 진행 상태 알림 서비스 특성상 서버 리소스를 가장 적게 소모하는 SSE가 최적이라고 판단.
    - HTML5 표준 기술로서 대부분의 브라우저에서 기본적으로 지원하며, 네트워크 연결이 일시적으로 끊겼을 때 클라이언트가 자동으로 재연결을 시도하는 기능이 내장되어 있어 관리가 용이하다고 판단.
  - #### 사용 사례 및 효율성
    - 사용자가 변환 결과 페이지로 진입하는 시점에 UUID를 통해 SSE 연결을 맺고, 이후 정해진 연결 시간(30분) 동안 별도의 요청 없이 서버가 밀어주는 최신 변환 상태(진행률, 상태, 변환 파일 등)를 즉시 수신.
    - Polling 방식 대비 불필요한 HTTP 요청 횟수를 줄여 서버의 트래픽 최소화.
    - 변환 과정을 실시간으로 확인 가능하므로 변환 프로세스 투명성을 확보.  
- #### Debezium(CDC)
  - #### 채택 이유
    - HTTP Polling 를 통한 스케줄러 기반의 DB 조회 방식은 데이터의 변화가 없더라도 주기적으로 쿼리를 실행해야 하므로, DB 커넥션 점유와 같은 문제를 인덱스 스캔에 따른 지속적인 부하가 발생.
    - API 서버 직접 전송 방식의 데이터 불일치 위험과 응답 지연 오버헤드를 제거하고, 로그 기반의 이벤트 발행을 통해 시스템 간 결합도를 낮추면서도 높은 신뢰성을 확보하기 위해 Debezium을 채택.
  - #### 사용 사례 및 효율성
    - 신규 변환 요청을 파악하기 위한 스케줄러 기반 방식은 주기적인 Polling 으로 인해 데이터베이스에 지속적인 부하를 주고 실시간성이 떨어지는 단점이 있는 반면 Debezium은 DB의 트랜잭션 로그를 직접 읽어 변경 사항을 감지하므로 서비스 로직과 DB 간의 결합도를 낮추고 성능 저하 없이 이벤트를 발행.
    - 사용자가 변환 요청을 하면 별도의 log 감지용 전용 테이블에 요청이 생성되고 파싱된 데이터를 RabbitMQ 에 전송하는 파이프라인 구성.
    - Docker 를 통해 독립적으로 서버를 구성하여 API 서버와의 런타임 의존성 제거하여 장애 격리 및 유지보수 편의성 증대.
- #### iText(변환 라이브러리)
  - #### 채택 이유
    - 이미지 변환 서비스의 특성상 빠른 처리 속도와 메모리 효율을 가장 중요하다고 판단하여,비즈니스 확장(최소 변환가능 용량)시에도 스트림 기반 처리에 최적화되어있어 제한된 리소스 자원을 아껴야하는 백엔드 환경에 적합하다고 판단.
  - #### 사용 사례 및 효율성
    - 메시지 큐로 전달받은 파일 메타 데이터를 기반으로 S3 에서 파일을 읽어와 iText 엔진을 통해 PDF 로 변환.
    - PdfWriter,Document 객체를 활용한 파이프라인 구성을 통해 데이터 읽기->변환->쓰기 과정의 처리 시간을 단축.
- #### Monitoring & Data Visualization (Prometheus + Grafana)
  - #### 채택 이유
    - 변환 서비스의 CPU 집약적 작업은 서버 자원 사용량에 대한 모니터링이 필수적이므로 Prometheus의 Pull 방식의 수집 구조를 통해 추후 확장하더라도 워커 서버들의 통합 관리가 가능하며 범용적인 데이터 소스를 지원하는 그라파나로 시각화 대시보드를 구성. 
  - #### 사용 사례 및 효율성
     - 워커 서버가 RabbitMQ의 메세지를 변환하는 과정에서의 CPU 점유율 및 메모리 점유율을 확인하여 특정 임계치를 넘어서는 시점을 파악하고 인프라 확장의 근거 데이터로 활용.
- #### Artillery (Stress Test)
  - #### 채택 이유
    - 단계별 고부하 상황을 기획하여 시나리오를 작성할 수 있으며 API 서버의 생존성과 메시지 발행 신뢰성 검증 및 유저 플로우에 따른 통합 테스트를 진행 가능.
  - #### 사용 사례 및 효율성
     - 실제 운영환경과 유사하게 총 5단계(Warm Up ~ Traffic Spike0) 로 구성하여 진행.
     - 디비지움이 메시지 큐로 정상적으로 메시지를 전송하는지 검증.
---

## 3.시퀀스 다이어그램(UML)
<div align="center">
<img width="1121" height="522" alt="file-conversion-uml drawio" src="https://github.com/user-attachments/assets/e383735f-df02-4038-9e9c-4ab19cee81a9" />
</div>

---
## 4.데이터베이스 테이블(ERD)
<div align="center">
<img width="1201" height="471" alt="스크린샷 2026-03-31 오후 11 18 26" src="https://github.com/user-attachments/assets/a46c7651-d62c-467c-a207-d2c15fa4d213" />
</div>

---
## 5.클라우드 아키텍처
<img width="1061" height="758" alt="image" src="https://github.com/user-attachments/assets/e8e6e923-415d-488f-8503-88118167c195" />



---
## 6.기능 시연 

### 파일 추가 및 삭제
![ezgif com-video-to-gif-converter](https://github.com/user-attachments/assets/95a57da8-b52c-4906-87ea-75acb54dbb5e)

 - 업로드 대상 파일을 추가하거나 삭제할 수 있습니다.
<br>
<br>

### 파일 업로드 및 누적 현황
![ezgif com-cut](https://github.com/user-attachments/assets/626ccd87-62de-466a-9bf7-b646dc9de4e8)
 - 총 3개까지 파일을 스테이징을 할 수 있습니다.
 - 추가한 순번대로 기다릴 필요없이 동시에 여러 파일들이 동시에 변환 시작됩니다.
 - 완료 시점에 계산된 파일의 용량과 횟수가 누적됩니다.


---
## 7.역할 분담
## 김주원
 - ### Backend
 - #### 서버 아키텍처 설계(Event Driven Architecture)
   - 기존 모놀리식에서 강결합된 관심사를 분리 및 메시지 큐 & CDC 를 도입하여 EDA 구조 설계.
   - 관심사 분리를 통해 성능 향상을 위한 확장이 용이한 구조로 개선
   - 각각의 독립적인 서버 구조로 인해 장애 격리에 특화
 - #### 이미지 변환 처리(iText)
   - 파일 메타 데이터를 기반으로 S3 에서 PNG,JPG(JPEG),GIF,BMP 의 파일을 내려받아 스트림 기반의 변환 처리 구현.
 - #### RabbitMQ(Producer,Consumer,DeadLetterQueue + Slack)
   - 변환 대상 파일의 메타데이터를 담는 메인 Queue 구성과 작업 실패 시 관리자 알림을 전송하는 DeadLetterQueue 구성한 분리 설계.
   - Exchange Topic 전략을 통해 패턴 매칭으로 구성하여 다수의 워커 서버를 가동해도 무리없는 확장을 설계.
   - Throughput 을 모니터링하고 서버 사양에 맞는 리스너의 Concurrency 를 조정(기본: 3, 최대: 8).
   - Acknowledgment 를 개발자가 직접 제어하여 작업 완료시점에만 Ack를 전송하여 안정성을 보장하며 일시적 장애에도 최대 3회까지 재처리
   - 변환이 실패하여 DLQ 로 이관 시 관리자의 Slack 메세지로 알림 전송되도록 구현.
 - #### CDC 를 통한 이벤트 트리거 최적화(Debezium)
   - Docker 컨테이너를 활용하여 독립적인 Debezium 서버를 구성.
   - 대상 테이블의 Bin-Log를 감지하여 메시지큐 Publisher 역할을 하도록 구현.
 - #### Redis + Server-Sent-Event를 통한 실시간 상태 동기화
     - SSE 구현 및 생명 주기 설정(30분).
     - Redis 의 Pub/Sub 구조를 통해 Worker 서버에서 API 서버로 진행 상태를 비동기 전송.
     - DB의 업데이트 트랜잭션 발생 시 해당 이벤트를 SSE 를 통해 프론트로 전송 로직 및 프론트단과의 통신 API 구현.
 - #### S3
   - 개발 편의성을 위해 AWS SDK를 통한 S3 버킷의 이미지 생명주기 관리 로직 구현.
   - Worker 서버에서 변환 작업 시 직접적인 S3 간의 upload/download 가 필요하여 구현.
   - VPC Endpoint 를 적용하여 서버와 S3 간의 데이터 전송 경로를 내부망으로 돌려 S3 Network Latency 10% 개선 
 - #### Prometheus + Grafana
   - Worker 서버, RabbitMQ 의 커스텀 모니터링 구축.
   - Worker 서버의 동시 처리수에 따른 처리량,S3 Network Latency 등의 패널 구성
   - RabbitMQ 를 기준으로 Ack 시점에 따른 분당 Throughput 을 확인하여 Worker 서버의 처리량 산출.
 - ## Front
 - #### CSS
   - Bootstrap 5와 Custom CSS를 활용하여 사용자 몰입감을 높이는 다크 모드 인터페이스 제작.
 - #### SSE + 프로그래스바 구현
   - Vanilla JS 기반의 SSE 를 통해 작업 상태 데이터를 실시간으로 반영.
   - 동적 진행 상태 표시(Progress Bar) 및 UI 트리거 구현.
   - 변환 작업 진행률을 실시간으로 업데이트하는 Progress Bar 구현.
   </br>
   </br>
   </br>

---

## 진승우
- ## Backend
- #### 파일 업로드 API 설계 및 구현
  - `upload-init` / `upload-complete` 2단계 업로드 API를 구현
  - 서버가 UUID 기반 S3 Key를 직접 생성하여 클라이언트의 임의 경로 업로드 방지
  - Presigned URL로 클라이언트가 S3에 직접 업로드, API 서버는 경로 생성·URL 발급·완료 검증만 담당

- #### 실시간 전역 통계 기능 구현
  - Redis Hash(`stats:global`)로 누적 변환 건수·용량 관리
  - SSE 연결 시 최신 통계 즉시 전달, 변경 발생 시 전체 구독자 브로드캐스트
  - 개별 파일 진행률 채널과 전역 통계 채널을 분리하여 데이터 혼선 방지
  - `@PostConstruct` Cache Warm-up으로 서버 기동 직후 다수 SSE 요청의 Thundering Herd 방지

- #### Redis / DB 정합성 보정 로직 구현
  - Redis atomic increment 처리 + 60초 주기 DB flush로 단일 집계 행 Hot Row 방지
  - 자정 스케줄러로 History 테이블 전체 재집계 후 Redis·DB 동시 overwrite
  - Redis 장애 시 DB 집계 테이블에서 복구하는 fallback 구조 설계

- #### RMQ / Outbox / CDC
  - Outbox 패턴 적용으로 애플리케이션이 브로커에 직접 publish하는 구조 대비 이벤트 유실 가능성 감소
  - 업로드 완료 시 Debezium CDC가 Outbox 테이블 변경을 감지해 RabbitMQ로 이벤트 발행
  - 이를 통해 이벤트 유실 가능성을 줄이고, 불필요한 DB 부하를 제거
<br>

- ## Infra
- #### AWS 클라우드 아키텍처 설계 및 구축
  - Route 53, ACM, ALB로 HTTPS 트래픽 유입 구성
  - API EC2·Worker EC2·RDS를 Private Subnet에 배치, NAT Gateway로 아웃바운드 통신 제한
  - Session Manager 기반 접근으로 퍼블릭 SSH 포트 미개방
  - S3 파일 저장소 연동 및 업로드·변환 흐름에 맞는 네트워크 구조 설계
 
- #### AutoScaling
  - RabbitMQ가 비관리형 서비스라 CloudWatch 자동 수집 불가 → Lambda + EventBridge로 커스텀 메트릭 파이프라인 직접 구성
  - 1분마다 Lambda가 RabbitMQ Management API를 폴링하여 `RabbitMQQueueDepth`를 CloudWatch에 발행
  - Step Scaling: 큐 ≥ 100이 2분 지속 시 스케일 아웃, 큐 ≤ 10이 5분 지속 시 스케일 인
  - Graceful Shutdown 120초 적용으로 스케일 인 시 처리 중인 변환 작업 보호
  - Lambda·EventBridge·CloudWatch·Step Scaling·Route53 전체를 Terraform IaC로 관리
    
- #### CI / CD
  - **CI**: Gradle 테스트 자동 실행, Gradle 캐시로 빌드 시간 단축
  - **CD**: GitHub OIDC 기반 AWS 인증으로 IAM Access Key 없이 배포
  - API·Worker 이미지를 ECR에 푸시, 태그 형식 `YYYYMMDD-HHMM-{run_number}`으로 배포 이력 추적
  - 이미지 태그를 SSM Parameter Store에 기록하여 스케일 아웃 후 새 인스턴스도 동일 버전으로 기동 보장

---
## 8.트러블 슈팅
### 김주원
 ### - 파일 변환 서비스에 따른 서버 아키텍처 설계(모놀리틱 -> EDA)
  - #### 문제:
    - 결합성이 강한 모놀리틱한 아키텍처로 서비스 구현 시 사용자 요청,파일 업로드,변환,결과 알림 로직등이 하나의 프로세스에서 동기적으로 실행되며,특정 파일의 변환 작업이 서버 자원을 과하게 점유하거나 오류를 발생시킬 경우 다른 서비스에 영향을 미치게됨
  - #### 원인 분석: 
    - 스레드 풀 고갈: 모놀리틱 서버 구조에서 무거운 변환 작업과 HTTP 요청을 같이 처리하는 경우 스레드 풀 고갈 현상이 발생할 수 있음
    - 확장성의 한계: 트래픽이 증가해 스케일아웃해야하는 경우 변환 로직만 별도로 확장할 수 없으므로 비효율적인 인프라 비용 증가
    - 진행 상태 피드백 불가: 변환 완료까지 동기 방식으로 진행되기때문에 요청된 HTTP 연결을 유지하며 대기해야하므로 진행 과정을 확인 할 수 없음
  - #### 해결:
    - 관심사 분리: API 서버는 사용자의 요청/응답 및 데이터를 저장하는 역할만 수행,Worker 서버는 이벤트 수신,변환 작업,이벤트 발행으로 독립적인 처리를 수행
    - 이벤트 전파: 애플리케이션이 직접 Queue 로 메세지를 넣지않고 이벤트 발행 테이블을 별도로 구성한 뒤 해당 테이블을 감지하여 메세지를 발행하도록 CDC 도입
    - 진행 상태 피드백: 변환 단계별 진행 상태를 담은 메세지를 API 서버로 발행하고 사용자는 실시간으로 진행 상황을 확인할 수 있음

 ### - Debezium(CDC) 도입
  - #### 문제:
    - HTTP Polling Schedulling 으로 인한 비효율적인 리소스 점유
    - API 서버가 죽으면 메시지큐로 발행하는 역할도 수행이 불가
    - Debezium 이 발행한 메세지가 RabbitMQ 의 Queue 로 라우팅되지 않고 유실되는 상황이 발생됨
  - #### 원인 분석:
    - 커넥션 점유 현상: 테이블에 데이터 유무와 상관없이 주기적으로 테이블을 스캔하기위해 커넥션을 지속적으로 점유하게 됨
    - 관심사 분리: API 서버가 테이블을 직접 확인하고 메시지큐로 전달하는 방식으로 인해 메시지큐는 API 서버에게 의존성을 가지는 현상이 발생함 
    - 데이터 로그 분석: RabbitMQ Admin 설정을 부여하여 Tracing Log 생성하여 확인 결과, Debezium 이 발행하는 메세지의 routingKey 가 명시한대로 바인딩되지않고 빈값으로 확인됨, 이에 따라 Debezium 이 메세지를 발행하기전 명시한 routingKey 를 찾지못해 빈 값이 들어간 것으로 유추함
  - #### 해결: 
    - Debezium 도입: Bin-Log 감지 기반의 Change Data Capture(CDC) 를 도입하여 비효율적인 HTTP Polling Schedulling 방식을 제거하고 데이터베이스 I/O 부하 감소
    - 명확해진 EDA 구조: API 서버와 메시지 발행 역할을 분리하고 이벤트 기반으로 동작하는 아키텍처를 구현하여 강결합을 해소    
    - 버전 특성 파악: Debezium 공식 문서 확인, Debezium 3.4 버전의 경우 지속적으로 업데이트되면서 내부적으로 routingKey Naming Convention이 엄격해지며 기존의 소문자 형식이 아닌 CamelCase로 작성해야 함을 확인하여 수정 후 정상적으로 바인딩되어 메세지를 발행할 수 있었음

---

### 진승우
  ### - 부하 테스트 중 Redis 재기동 시 발견된 Thundering Herd → Hikari 풀 고갈
  - #### 문제:
    - 부하 테스트 도중 Redis 서버를 재기동하는 과정에서 일시적으로 Redis 연결이 단절됨. SSE를 구독 중이던 다수의 클라이언트가 동시에 DB fallback을 시도 → Hikari 풀 10개 즉시 고갈 → Connection timed out으로 Redis도 DB도 응답 불가한 이중 장애 발생
  
  - #### 원인 분석:
    - fallback 구조 문제: Redis 장애 시 DB로 fallback하는 로직에 동시성 제어가 없어, 모든 스레드가 일제히 DB로 몰리는 Thundering Herd 발생
    - 풀 한계 초과: 순간적으로 수백 개의 스레드가 동시에 DB 커넥션을 요청하면서 Hikari 풀 10개가 즉시 고갈, Connection timed out으로 DB 조회까지 실패
      
  - #### 해결:
    - ReentrantLock.tryLock()으로 단 하나의 스레드만 DB 조회를 실행
    - 결과를 Caffeine 로컬 캐시(TTL 60s)에 저장, 나머지 스레드는 캐시에서 서빙
    - Redis 복구 후 TTL 만료 시 자동으로 Redis 경로로 복귀
      
<br>

  ### - Debezium 크래시 루프 (Worker Auto Scaling)
  - #### 문제:
    - Auto Scaling 테스트 중 ASG가 새 Worker 인스턴스를 생성할 때마다 Debezium이 약 10~13초 간격으로 크래시 루프를 반복하며 정상 기동되지 않음

  - #### 원인 분석:
    - **binlog 유실**: AMI에 포함된 offsets.dat가 참조하는 binlog를 RDS가 이미 삭제한 상태 → 존재하지 않는 binlog 위치를 참조하며 ERROR → STOPPING 반복
    - **server_id 충돌**: offsets.dat 삭제 후 재시도했으나 Worker 2대의 Debezium이 동일 server_id로 MySQL에 동시 접속 → 서로를 연결에서 밀어내는 핑퐁 크래시로 번짐
    - **근본 원인**: Debezium이 스케일 아웃 대상인 Worker EC2에 함께 배포된 구조. Worker가 N대로 늘어나면 Debezium도 N개가 떠서 중복 발행과 충돌이 필연적으로 발생

  - #### 해결:
    - Debezium은 Worker 수와 무관하게 단 1개만 있으면 충분하므로, Worker EC2에서 분리하여 스케일링 범위 밖인 RMQ EC2로 이전
    - **[Before]** Worker EC2 × N대 — Worker App + Debezium
    - **[After]** RMQ EC2(고정) — RabbitMQ + Redis + Debezium / Worker EC2 × N대 — Worker App만

<br>

--- 
## 9.추후 도입예정 기능

- PDF 합치기
- PDF 분할
- PDF 회전
- PDF 압축
- 관리자 기능
  - 변환 내역 조회
  - 변환 실패 로그 조회
  - 일별 변환 횟수 통계 조회
- api, worker, cdc 앱 모듈화 v 
- s3 바이러스 검사
- History 테이블 완료 이력 조회 방식 변경
  - 현재: 자정 기준 스케줄러로 History 테이블 전체를 재집계하여 Redis / DB 값을 overwrite
  - 문제: 전체 테이블 스캔으로 데이터 누적 시 부하가 증가하고, 즉시 정합성이 필요한 시점(Redis 장애 복구 직후)에 자정까지 대기해야 하는 구조
  - 개선 방향: 마지막 집계 시점(timestamp) 이후 변경된 이력만 증분 집계하는 방식으로 전환하여 전체 테이블 스캔 부하를 제거하고, 보정 주기를 단축하거나 이벤트 기반으로 트리거
- CD 방식 변경 (Auto Scaling 대비)
  - 현재 SSM Run Command 방식은 CD 시점에 실행 중인 인스턴스에만 배포되어, 이후 스케일 아웃으로 추가된 인스턴스는 구버전으로 기동될 위험이 있음
  - Launch Template 기반의 Instance Refresh 방식으로 전환하여 스케일 아웃 시에도 항상 최신 버전이 보장되는 구조로 개선 예정












