<div align="center">
<img width="866" height="696" alt="Image" src="https://github.com/user-attachments/assets/8377836b-fe79-44f3-bde6-a9baf019bdac" />

# 🚀 파일 변환 서비스
**이미지(PNG, JPG, BMP)를 PDF로 간편하게 변환하세요.**

---

### 📊 실시간 서비스 현황
| 누적 변환 건수 | 누적 변환 용량 |
| :---: | :---: |
| **7건** | **37.1 KB** |

</div>

## 📖 서비스 소개
- 회원가입없이 사용자의 이미지 파일을 PDF 문서로 변환하여 실시간으로 신속하고 안전하게 전달합니다.
- 이미지 파일 변환 목적만을 가진 사용자를 위한 별도의 설정없는 직관적인 UI 를 통해 누구나 쉽게 이용 가능합니다.
- 변환 건에 대한 내역이 실시간으로 통계에 누적반영되어 서비스의 신뢰성을 보장합니다.
<br>

## 👤 멤버 구성

| 김주원 | 진승우 |
| :---: | :---: |
| <img src="" width="200" alt="김주원 프로필 사진"> | <img src="" width="200" alt="진승우 프로필 사진"> |
| [@goyois](https://github.com/goyois) | [@SeungWoo1222](https://github.com/SeungWoo1222) |

---
## 1.개발 환경
<div align="center">
<img width="1792" height="1063" alt="undefined (1)" src="https://github.com/user-attachments/assets/3df277bc-1de1-4769-806e-1f64b42f2d2e" />

</div>

[Backend]
- Language: Java 17
- Framework: Spring Boot
- Database: MySQL, Redis
- Event Data: Debezium (CDC)
- Message Broker: RabbitMQ, Redis
- Real-time: SSE (Server-Sent Events)
- Container: Docker
- Monitoring: Prometheus, Grafana
- Test: Artillery (Load Test)
- Alarm: Slack API


[Front]
- View Engine: Thymeleaf 
- Styling: Bootstrap 5 (CDN), Custom CSS (Dark Theme) 
- Scripting: Vanilla JavaScript (ES6+) 
- Real-time: Server-Sent Events (SSE)

---

## 2.개발 기술의 채택 이유와 사용사례

- #### RabbitMQ
  - 파일 변환은 CPU 와 시간이 많이 소요되므로 사용자가 브라우저를 붙잡고 있지않도록 비동기적으로 처리했습니다.
  - API Server,RabbitMQ,CDC 를 개별적으로 분리하여 한 곳에서 문제가 생겨도 데이터가 소실되는 경우를 방지했습니다.
  - 만약 워커 서버에서 변환에 실패하는 경우(변환 불가 포맷,네트워크 지연 등등) 3번까지 재시도하며 DeadLetterQueue 로 이관되며 설정된 Slack 알림을 통해 내용을 확인할 수 있습니다.

- #### Redis & SSE
  - 파일이 변환되는 과정에서 진행률, 변환 상태를 사용자가 직접 조회를 하지않고 실시간으로 신속하게 확인해야하므로 Redis 의 메세지 브로커를 통해 상태를 전송하고 구독된 유저 식별자를 대상으로 파일 이름을 매칭하여 실시간 상태 업데이트를 했습니다.
  - SSE 사용자가 페이지로 진입할 때 유저 식별자를 SSE 로 넘겨 정해진 시간동안 실시간으로 최신 데이터를 수신받을 수 있습니다.

- #### Debezium & Docker
  - 신규 변환 요청을 파악하기 위한 스케줄러 기반 방식은 주기적인 Polling으로 인해 데이터베이스에 지속적인 부하를 주고 실시간성이 떨어지는 단점이 있는 반면 Debezium은 DB의 트랜잭션 로그를 직접 읽어 변경 사항을 감지하므로 서비스 로직과 DB 간의 결합도를 낮추고 성능 저하 없이 이벤트를 발행할 수 있습니다.
  - 사용자가 변환 요청을 하면 별도의 log 감지용 전용 테이블에 요청이 생성되고 파싱된 데이터를 RabbitMQ 에 전송합니다.
  - Docker 를 통해 독립적으로 서버를 구성하여 장애 격리 및 유지보수 편의성 증대시켰습니다.

- #### Monitoring & Data Visualization (Prometheus + Grafana)
   - 변환 서비스의 CPU 집약적 작업은 서버 자원 사용량에 대한 모니터링이 필수적이므로 Prometheus의 Pull 방식의 수집 구조를 통해 추후 확장하더라도 워커 서버들의 통합 관리가 가능하며 범용적인 데이터 소스를 지원하는 그라파나로 시각화 대시보드를 구성했습니다.
   - 워커 서버가 RabbitMQ의 메세지를 변환하는 과정에서의 CPU 점유율 및 메모리 점유율을 확인하여 특정 임계치를 넘어서는 시점을 파악하고 인프라 확장의 근거 데이터로 활용했습니다.
 
- #### Artillery (Stress Test)
   - 실제 운영환경에서 여러 사용자가 동시에 파일을 업로드했을 때 API Server 가 무너지지않고 성공적으로 비동기 처리를 할 수 있는지 검증하기 위해 도입했습니다.
   - 임의로 총 4단계의 가상의 시나리오를 작성하여 동시 요청수를 높여가며 API Server 로 요청을 보내 Debezium 이 정상적으로 메세지를 Publishing(Trigger) 하는지 확인해볼 수 있었습니다.
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
<img width="1152" height="711" alt="image" src="https://github.com/user-attachments/assets/4b431b6e-be6b-49ee-8826-8de843bd8ed7" />


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
### 김주원
 - ### Backend
 - 서버 아키텍처 설계
   - Event-Driven Architecture(EDA) 기반 아키텍처 설계 및 구축
 - 이미지 변환 처리
   - iText 라이브러리를 통해 PDF 포맷 변환 구현
 - RabbitMQ(Producer,Consumer)
   - 변환 대상 파일의 메타데이터를 담는 Queue/관리자 알림을 전송하는 DeadLetterQueue 구성
   - 작업 완료 시점에만 Ack를 전송하는 신뢰성 있는 메시징 처리
 - CDC + Docker
   - Debezium 을 Docker 를 통해 개별 서버로 구축
   - Debezium 을 RabbitMQ 의 큐에 메세지를 보내는 Publisher 형태로 구현
 - Server-Sent-Event를 통한 단방향 통신 구축
   - 사용자별 SSE 구독 설정 및 실시간 변환 진행 상태 전송
   - Redis Pub/Sub과 SSE를 연동하여 분산 환경에서의 비동기 상태 동기화 구현
 - S3
   - SDK를 통한 S3 생명주기 관리 로직 구현
 - ### Front
 - CSS
   - Bootstrap 5와 Custom CSS를 활용하여 사용자 몰입감을 높이는 다크 모드 인터페이스 제작
 - SSE
   - Vanilla JS 기반의 SSE 를 통해 작업 상태 데이터를 실시간으로 반영
 - 동적 진행 상태 표시(Progress Bar) 및 UI 트리거 구현
   - 변환 작업 진행률을 실시간으로 업데이트하는 Progress Bar 구현

### 진승우
- ### Backend / Infra
- AWS 클라우드 아키텍처 설계 및 구축
  - Route 53, ACM, ALB를 활용하여 사용자 요청이 HTTPS로 안전하게 유입되도록 구성
  - API EC2, Worker EC2, RDS를 Private Subnet에 배치하고, NAT Gateway를 통해 필요한 아웃바운드 통신만 허용하도록 설계
  - Session Manager 기반으로 서버 운영 환경을 구성하여 퍼블릭 SSH 포트 개방 없이 인스턴스에 접근할 수 있도록 구축
  - S3를 파일 저장소로 연동하고, 업로드/변환 흐름에 맞는 네트워크 구조를 설계

- 파일 업로드 API 설계 및 구현
  - `upload-init` / `upload-complete` 2단계 업로드 API를 구현
  - 서버가 직접 S3 Key와 UUID 기반 파일명을 생성하여 클라이언트의 임의 경로 업로드를 방지
  - Presigned URL 기반 업로드를 적용하여, 클라이언트가 API 서버를 거치지 않고 S3에 직접 파일을 업로드하도록 구성했습니다.
  - API 서버는 파일 자체를 중계하지 않고 업로드 경로 생성, URL 발급, 업로드 완료 검증만 담당하도록 분리했습니다.

- 실시간 전역 통계 기능 구현
  - Redis Hash(`stats:global`) 기반으로 서비스 전체 누적 변환 건수와 용량을 관리
  - SSE 연결 시 최신 전역 통계 값을 즉시 내려주고, 변경 발생 시 전체 구독자에게 브로드캐스트하도록 구현
  - 서비스 전체 기준 통계를 별도로 분리하여 개별 파일 진행률과 전역 누적 통계가 섞이지 않도록 설계

- Redis / DB 정합성 보정 로직 구현
  - 1분 주기의 스케줄러를 통해 Redis 전역 통계를 DB 단일 집계 테이블에 flush 하도록 구현
  - 자정 기준 스케줄러를 통해 History 테이블 완료 이력을 다시 집계하고 Redis / DB 값을 overwrite 하여 정합성을 보정
  - 단일 PK(`global`) 기반의 집계 테이블 구조를 사용하여 Hot Row 문제를 최소화하면서 누적 통계를 안정적으로 유지
  - Redis 기반 실시간 조회를 중심으로 구현했으며, 장애 상황에서의 복구를 위해 DB 집계 테이블 fallback 구조를 설계했습니다.

- RMQ / CDC
  - 비동기 변환 요청의 신뢰성을 높이기 위해 Outbox 패턴을 적용했습니다.
  - 업로드 완료 시 Debezium CDC가 Outbox 테이블의 변경을 감지해 RabbitMQ로 이벤트를 발행하도록 구성했습니다.
  - 이를 통해 애플리케이션이 직접 브로커에 즉시 publish하는 구조보다 이벤트 유실 가능성을 줄였습니다.
 
- ci / cd
- s3 바이러스 검사
- 이미지 최적화
---
## 8.트러블 슈팅
### 김주원
 ### - 파일 변환 서비스에 따른 서버 아키텍처 설계(모놀리틱 -> EDA)
  - #### 문제:
    - 결합성이 강한 모놀리틱한 아키텍처로 서비스 구현 시 사용자 요청,파일 업로드,변환,결과 알림 로직등이 하나의 프로세스에서 동기적으로 실행되며,특정 파일의 변환 작업이 서버 자원을 과하게 점유하거나 오류를 발생시킬 경우 다른 서비스에 영향을 미치게됨
  - #### 원인 분석: 
    - 스레드 풀 고갈: 모놀리틱 서버 구조에서 무거운 변환 작업과 HTTP 요청을 같이 처리하는 경우 스레드 풀 고갈 현상이 발생할 수 있음
    - 확장성의 한계: 트래픽이 증가해 스케일 아웃해야하는 경우 변환 로직만 별도로 확장할 수 없으므로 비효율적인 인프라 비용 증가
    - 진행 상태 피드백 불가: 변환 완료까지 동기 방식으로 진행되기때문에 요청된 HTTP 연결을 유지하며 대기해야하므로 진행 과정을 확인 할 수 없음
  - #### 해결:
    - 관심사 분리: API 서버는 사용자의 요청/응답 및 데이터를 저장하는 역할만 수행,Worker 서버는 이벤트 수신,변환 작업,이벤트 발행으로 독립적인 처리를 수행
    - 이벤트 전파: 애플리케이션이 직접 Queue 로 메세지를 넣지않고 이벤트 발행 테이블을 별도로 구성한 뒤 해당 테이블을 감지하여 메세지를 발행하도록 CDC 도입
    - 진행 상태 피드백: 변환 단계별 진행 상태를 담은 메세지를 API 서버로 발행하고 사용자는 실시간으로 진행 상황을 확인할 수 있음

 ### - Debezium(CDC) 을 통한 Message Publishing 이슈
  - #### 문제:
    - Debezium 이 발행한 메세지가 RabbitMQ 의 Queue 로 라우팅되지 않고 유실되는 상황이 발생됨
  - #### 원인 분석:
    -  데이터 로그 분석:RabbitMQ Admin 설정을 부여하여 Tracing Log 생성하여 확인 결과, Debezium 이 발행하는 메세지의 routingKey 가 명시한대로 바인딩되지않고 빈값으로 확인됨, 이에 따라 Debezium 이 메세지를 발행하기전 명시한 routingKey 를 찾지못해 빈 값이 들어간 것으로 유추함
  - #### 해결:
    - 버전 특성 파악: Debezium 공식 문서 확인, Debezium 3.4 버전의 경우 지속적으로 업데이트되면서 내부적으로 routingKey Naming Convention 이 엄격해지며 기존의 소문자형식이 아닌 CamelCase 로 작성해야함을 확인하여 수정 후 정상적으로 바인딩되어 메세지를 발행할 수 있었음
   
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
- 리드미 실시간 변환 현황 
- api, worker, cdc 앱 모듈화
- 업로드 이미지 최적화
- s3 바이러스 검사













