# <img width="50" height="40" src="https://github.com/user-attachments/assets/591a9945-2dc0-46a7-93af-de31a893fa2b" /> CHWIMEET

<div align="center">
<div align="center">
  <img width="880" height="256" alt="chwimeet_logo" src="https://github.com/user-attachments/assets/d90bcd4a-ccdd-4d7b-8712-5694ff90a775" />
</div>

**“장비가 이어주는 취미의 시작”**

> 장비를 통해 **새로운 취미를 시작하고**,  
> 장비를 가진 사람과 **즐겁게 연결되는 플랫폼, 취밋**  
>  
> 시작하고 싶은 취미와  
> 사용되지 않던 장비가  
> 취밋에서 만나 새로운 즐거움을 만들어갑니다.

[🖥️ 서비스 바로가기](https://www.chwimeet.store/) ·
[🛠️ API 문서 (Swagger)](https://api.chwimeet.store/)

</div>


<br>

#  📖 목차

1. [🧑‍💻 팀 소개](#-팀-소개)
2. [💡 개발 배경](#-개발-배경)
3. [🧩 아키텍처](#-아키텍처)
4. [🚀 기능 소개](#-기능-소개)
5. [🛠️ 기술 스택](#️-기술-스택)
6. [⚙️ 기능 구현 방식](#️-기능-구현-방식)
7. [🔥 트러블슈팅](#-트러블-슈팅)

<br>

# 🧑‍💻 팀 소개 — 일단 진행해 팀

| [![](https://avatars.githubusercontent.com/u/110077966?v=4)](https://github.com/Yoepee) | [![](https://avatars.githubusercontent.com/u/129157326?v=4)](https://github.com/ehgk4245) | [![](https://avatars.githubusercontent.com/u/163832764?v=4)](https://github.com/jjuchan) | [![](https://avatars.githubusercontent.com/u/119219808?v=4)](https://github.com/kku1403) | [![](https://avatars.githubusercontent.com/u/138780449?v=4)](https://github.com/geun-00) | [![](https://avatars.githubusercontent.com/u/109943444?v=4)](https://github.com/1J-Choi) |
|--------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|
| <p align="center">김동엽<br/>BE/FE</p> | <p align="center">김도하<br/>BE</p> | <p align="center">윤주찬<br/>BE</p> | <p align="center">김은주<br/>BE</p> | <p align="center">장근영<br/>BE</p> | <p align="center">최원제<br/>BE</p> |

<br>

# 💡 개발 배경

우리 주변에는 한두 번만 사용하고 방치되는 물건들이 정말 많습니다.  
캠핑 장비, 공구, 촬영 장비처럼 **자주 쓰지 않지만 필요할 때는 꼭 필요한 물건들**이 대표적입니다.  
이런 물건들을 사기에는 부담되고, 보관도 번거롭고, 그렇다고 한 번 쓰자고 구매하기도 아깝습니다.

그래서 많은 사람들은 친구나 지인에게 빌리려고 해보지만,  
**누가 어떤 물건을 가지고 있는지 알기 어렵고**,  
빌리고 돌려주는 과정에서도 생각보다 많은 불편이 생깁니다.

> **“잠깐만 필요한 물건, 이웃에게 편하게 빌릴 수 있다면 어떨까?”**  
> **“서랍 속에 놀고 있는 물건들을, 필요한 사람들에게 자연스럽게 공유할 수는 없을까?”**

만약 그런 환경이 마련된다면, 물건을 둘러싼 소비는 훨씬 더 가볍고 효율적이 될 것입니다.  
구매 부담은 줄고, 집 안에 쌓여가는 물건도 줄어들며,  
이웃 간의 연결과 신뢰가 자연스럽게 생겨나는 경험도 가능해집니다.

저희는 많은 사람들이 일상 속에서 느끼는 이 **작지만 반복적인 불편함과 아쉬움**을 기술로 해결해보고자,  
이 P2P 물건 대여 플랫폼을 기획하게 되었습니다.


<br>

# 🧩 아키텍쳐
- 아키텍처 이미지
<div align="center">
<img width="720" height="347" alt="image" src="https://github.com/user-attachments/assets/fa9ea756-b7c3-4f40-b0f0-812108882554" />
</div>

- 주요 아키텍처 특징
  - 실시간 모니터링: Grafana 대시보드를 통한 실시간 시스템 메트릭 모니터링 및 알림
  - 무중단 배포: Nginx Proxy Manager를 활용한 Blue/Green 배포 전략으로 서비스 중단 없이 안전한 배포 구현
  - 이미지 최적화: AWS Lambda를 통한 서버리스 이미지 리사이징으로 메인 서버 부하 분산 및 비용 효율화
  - 분산 캐싱: Redis를 활용한 세션 관리 및 캐싱으로 서버 간 데이터 일관성 유지

<br>

# 🚀 기능 소개

- CRUD나 회원같은 모든 프로젝트에 있는 기능보다는 특별한 기술스택을 활용한 기능 위주로 설명하는게 좋아보임
- Ex) 쿼츠 기반 배치작업 처리, Redis 활용 캐싱, Spring AI 활용 기능들, 채팅 및 알림 등 실시간 통신

<!-- 게시글 -->
<details>
<summary>📝 게시글</summary>

### 게시글 조회
![게시글조회](images/게시글/게시글%20목록.gif)

- 게시글 목록 조회 기능
- 카테고리, 지역, 키워드 기준 검색 가능

### 게시글 상세 조회회

![게시글상세조회](images/게시글/게시글%20상세.gif)

- 게시글 상세 정보 조회
- 호스트(작성자) 정보, 게시글 이미지, 본문 내용 제공
- 추가 옵션 및 리뷰 정보 함께 조회

### 게시글 등록

![게시글등록](images/게시글/게시글%20등록.gif)

- 게시글 기본 정보 및 이미지 업로드 기능
- 이미지 업로드 시 AWS Lambda를 통해 리사이징 처리
- 처리된 이미지는 S3에 저장되며 CloudFront를 통해 캐싱

</details>

<!-- AI -->
<details>
<summary>🤖 AI</summary>

### AI 검색
![AI검색](images/AI기능/스마트검색.gif)

- 게시글 데이터를 임베딩하여 벡터 형태로 저장
- MariaDB Vector 기반 1차 유사도 검색 수행
- reranker를 통한 재정렬 
- LLM이 답변을 생성하는 2-stage RAG 구조 적용

### 게시글 자동 생성

![게시글 자동 생성](images/AI기능/게시글%20등록.gif)

### AI 후기 요약

![AI 후기 요약](images/AI기능/후기%20요약.gif)

</details>

<!-- 예약 -->
<details>
<summary>📅 예약</summary>

### 예약 등록/조회/취소

- 게시글 기반 예약 가능
- 예약 상태 변경(승인, 취소) 관리
- Quartz 기반 스케줄링으로 자동 취소 및 정산 재시도 기능 제공

</details>

<!-- 후기 -->
<details> <summary>⭐ 후기</summary>

### 후기 작성

![후기작성](images/리뷰/리뷰%20작성.gif)

- 일정 단계 이상 진행 된 예약에 대하여 후기 작성가능

### 후기 통계

![후기통계](images/리뷰/리뷰%20목록%20및%20통계.gif)

- 사용자가 등록한 게시글에 대한 사용자 후기 통계 기능
- 단일 게시글에 대한 후기 통계 기능

</details>


<!-- 채팅 -->
<details>
<summary>💬 채팅</summary>

<div align="center">
  <img 
    src="https://raw.githubusercontent.com/kku1403/AIBE3_FinalProject_Team1_BE/main/images/%EC%B1%84%ED%8C%85/%EC%B1%84%ED%8C%85.gif"
    width="700"
    alt="실시간 채팅 기능 시연"
  />
</div>

<br>

- 게시글을 통해 호스트와 채팅방 생성
- 실시간 통신

</details>

<!-- 알림 -->
<details>
<summary>🔔 알림</summary>

### 알림 기능

![알림](images/알림/알림.gif)

- 이벤트 알림 제공
- SSE 기반 실시간 알림

</details>

<!-- 신고 -->
<details>
<summary>🚨 신고</summary>

- 게시글, 댓글, 후기 신고 가능
- 관리자가 확인 후 처리
- 알림과 연동되어 신고 처리 결과 확인 가능

</details>

<!-- 관리자 -->
<details>
<summary>⚙️ 관리자</summary>

### 지역 관리 기능

![지역](images/지역/지역.gif)

- 등록된 지역 목록 조회
- 지역 등록, 수정, 삭제

### 카테고리 관리 기능

![카테고리](images/카테고리/카테고리.gif)

- 등록된 카테고리 목록 조회
- 카테고리 등록, 수정, 삭제

### 신고 목록 확인 및 제제 기능

![신고제제](images/신고/신고%20제제.gif)

- 사용자가 등록한 신고 목록 조회
- 제제 버튼으로 제제할 수 있는 기능

</details>

<br>

<br>

# 🛠️ 기술 스택

- 아이콘이나 이미지로 기술 스택 나열 후 특별히 선택한 이유가 명확하고 눈에 뛸만한 기술 스택에 대해 선택한 이유 적기

<img src="https://img.shields.io/badge/Mariadb-003545?style=for-the-badge&logo=mariadb&logoColor=white" />

프로젝트 초기에는 익숙한 관계형 데이터베이스인 MySQL을 사용했지만,
검색 정확도 향상과 임베딩 기반 분류 기능을 구현하기 위해 **벡터 검색(Vector Search)** 이 필요한 상황이 되었습니다.

외부 Vector Database(Pinecone, Qdrant, Weaviate 등)도 검토했으나,
별도의 솔루션을 도입할 경우 운영 복잡성 증가, 데이터 동기화 문제, 인프라 비용 상승이 발생할 가능성이 컸습니다.

MariaDB는 기존 MySQL 계열의 장점을 유지하면서도 벡터 타입과 벡터 인덱스 기능을 공식 지원해
관계형 데이터와 벡터 데이터를 하나의 DB에서 통합 관리할 수 있었습니다.
이로써 추가 인프라 없이 필요한 기능을 확장할 수 있었고, 데이터 일관성도 자연스럽게 유지할 수 있었습니다.

또한 Spring 기반의 프로젝트였기 때문에,
Spring AI에서 MariaDB Vector Store를 기본적으로 지원한다는 점이 큰 이점으로 작용했습니다.
즉, 복잡한 커넥터 구현 없이도 임베딩 저장·조회·검색 기능을 기존 Spring Data 스타일로 바로 사용할 수 있어
개발 속도와 유지보수성이 크게 향상되었습니다.

<img src="https://img.shields.io/badge/Spring_AI-6DB33F?style=for-the-badge&logoColor=white">

Spring AI는 다양한 AI 모델과 벡터 DB를 추상화된 인터페이스로 제공하여 **특정 벤더나 기술에 종속되지 않고 유연한 확장**이 가능합니다. 또한 **RAG 파이프라인**을 지원하여 임베딩 및 검색, 응답 생성 구조를 쉽고 빠르게 구축할 수 있습니다. 결과적으로 Spring 생태계와의 결합, 유지보수성과 확장성, 그리고 RAG를 효율적으로 개발하기 위해 Spring AI를 선택하였습니다.

<img src="https://img.shields.io/badge/Redis-FF4438?style=for-the-badge&logo=redis&logoColor=white" />

...STOMP Pub/Sub 설명...

또한 레디스를 캐시 저장소로 사용하였는데, 이는 이미 STOMP Pub/Sub 구조에서 레디스가 구축된 상황에서 도입 비용과 운영 복잡도를 줄이기 위함이었습니다. 그리고 레디스를 단순 캐시 용도가 아니라, **Redisson을 활용한 분산락**을 적용함으로써 다중 인스턴스에서 발생할 수 있는 **캐시 스탬피드 현상**을 제어할 수 있었습니다. 즉, 캐싱을 통한 성능 개선과 분산락을 통한 최적화 및 데이터 정합성 확보를 동시에 만족하기 위해 레디스를 선택하였습니다.

<img src="https://img.shields.io/badge/Quartz-3E4348?style=for-the-badge&logoColor=white" />


<br>


# ⚙️ 기능 구현 방식

- 어필할만한 기능 및 기술(RAG 기반 스마트 검색, Redis pub/sub 활용 채팅, AI 후기 캐싱, lambda 활용 이미지 리사이징, 쿼츠 기반 배치작업, 전략 패턴 기반 다형성 아키텍처)

<details>
<summary><strong>💬 Redis Pub/Sub 기반 실시간 채팅</strong></summary>

<br>

### 1. 도입 배경
초기에는 단일 서버 환경에서 WebSocket 기반 채팅만으로도 충분했지만,
서비스 특성상 **서버 확장 및 무중단 배포 환경**을 고려하면서
다음과 같은 한계를 확인했습니다.

- 다중 서버 환경에서  
  → 같은 채팅방 사용자라도 서로 다른 서버에 연결될 경우 메시지 전달 불가
- 서버별 WebSocket 세션 분리로 인한  
  → 메시지 동기화 및 일관성 문제
- 배포 또는 서버 재시작 시  
  → 연결 상태에 따라 채팅 흐름이 끊길 가능성

이를 해결하기 위해 **Redis Pub/Sub을 메시지 브로커로 사용하는 구조**를 도입했습니다.

---

### 2. 구조 및 동작 방식
채팅 메시지는 다음 흐름으로 처리됩니다.

1. 사용자가 채팅 메시지를 전송하면  
   → WebSocket을 통해 현재 연결된 서버로 전달됩니다.
2. 서버는 해당 메시지를 Redis Channel로 Publish 합니다.
3. Redis는 메시지를 해당 채널을 구독 중인 모든 서버 인스턴스에 브로드캐스트합니다.
4. 각 서버는 Redis로부터 전달받은 메시지를  
   → 자신에게 연결된 클라이언트들에게 WebSocket으로 다시 전송합니다.

이를 통해 **어느 서버에 연결되어 있든 동일한 채팅 메시지를 실시간으로 수신**할 수 있습니다.

---

### 3. 설계 시 고려한 점
채팅 메시지는 Redis Pub/Sub을 통해 전달되기 전에
이미 데이터베이스에 저장되는 구조입니다.

따라서 Redis는 메시지 영속성을 담당하지 않으며, 
실시간 전달 및 서버 간 동기화 역할에 집중하도록 설계했습니다.  

또한 Redis Publish 과정에서 예외가 발생할 경우를 대비해,
WebSocket을 통해 현재 서버에 연결된 클라이언트에게
직접 메시지를 전달하는 fallback 로직을 추가했습니다.

```java
public void publish(Long chatRoomId, ChatMessageDto dto) {
    try {
        //정상 동작 시 Redis Publish 동작
        ...

    } catch (Exception e) {
        //Redis Publish 실패 시 fallback 
        log.error("Failed to publish chat message: chatRoomId={}, messageId={}",
                chatRoomId, dto.id(), e);
        chatWebsocketService.broadcastMessage(chatRoomId, dto);
    }
}
```
이를 통해 일부 인프라 장애 상황에서도 
채팅 기능이 완전히 중단되지 않도록 구성했습니다.
</details>

<br>

<details>
<summary><strong>🔔 알림 타입 기준 Batch 로딩과 Mapper 기반 응답 조합을 적용한 알림 조회 설계 </strong></summary>

<br>

### 기능 개요

알림 목록 조회 API는 **페이징·정렬 기준에 따라 하나의 목록으로 조회되지만**,  
목록에 포함된 각 알림은 **알림 타입에 따라 참조해야 하는 엔티티와 응답 구조가 서로 다릅니다.**

이를 단순 조건 분기나 타입별로 분리된 API로 처리하지 않고,  
**목록 조회 성능을 유지하면서도 알림 타입 확장에 유연한 조회 구조**로 설계했습니다.

--- 


### 설계 목표

- **요청 쿼리 최소화**
  - 알림 개수에 비례해 쿼리가 증가하는 N+1 문제 방지
- **확장성 확보**
  - 알림 타입 추가 시 서비스 계층 로직 수정 없이 확장 가능
- **역할 분리**
  - 조회 / 로딩 / 응답 조합 책임을 명확히 분리
 
---


### 처리 흐름

```text
알림 엔티티 페이징 조회
(알림 타입, 연관 엔티티 ID)
        ↓
알림 타입 기준 그룹화
        ↓
그룹별 Batch 조회로 필요한 엔티티 로딩
        ↓
Mapper를 통한 타입별 응답 데이터 매핑
        ↓
  페이징 응답 반환
```
---

### 단계별 핵심 로직

##### 1. 알림 엔티티 페이징 조회

```java
Page<Notification> notificationsPage =
        notificationRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId, pageable);

List<Notification> notifications = notificationsPage.getContent();
```
- 알림 엔티티 페이징 조회
- 알림 ID, 알림 타입 등 기본 정보와 함께 연관 엔티티 ID 조회

---

##### 2. 알림 타입 기준 그룹화

```java
Map<NotificationType.GroupType, List<Long>> groupedTargetIds =
        notifications.stream()
                .collect(Collectors.groupingBy(
                        n -> n.getType().getGroupType(),
                        Collectors.mapping(Notification::getTargetId, Collectors.toList())
                ));
```
- batch 조회를 위한 사전 작업
- GroupType을 Key로, 조회해야 할 연관 엔티티 ID들을 그룹화

---

##### 3. 그룹별 Batch 조회로 연관 엔티티 로딩

```java
for (Map.Entry<NotificationType.GroupType, List<Long>> entry : groupedTargetIds.entrySet()) {
    NotificationType.GroupType groupType = entry.getKey();
    List<Long> targetIds = entry.getValue();

    Function<List<Long>, Map<Long, ?>> loader = batchLoaders.get(groupType);
    if (loader != null) {
      loadedEntities.put(groupType, loader.apply(targetIds));
    }
}
```

- 그룹화 한 Map 정보를 바탕으로 타입 별로 batch 조회
- 알림 개수와 무관하게 타입 수 만큼만 쿼리 발생

---

##### 4. Mapper를 통한 타입별 응답 데이터 매핑

```java
for (Notification notification : notifications) {
    NotificationDataMapper<? extends NotificationData> mapper =
            mapperRegistry.get(notification.getType());

    Map<Long, ?> entityMap =
            loadedEntities.get(notification.getType().getGroupType());
    Object entity =
            entityMap != null ? entityMap.get(notification.getTargetId()) : null;

    NotificationData data = mapper.map(entity, notification);
}

```

- Batch 조회 시 DTO로 직접 조회하지 않고 Entity를 로딩한 뒤 Mapper에서 응답 DTO로 변환
- 알림 타입별 응답 구조 변경 시 쿼리 수정 없이 Mapper만 변경 가능

##### 5. 응답 반환

```java
public record NotificationResBody<T extends NotificationData>(
        Long id,
        NotificationType notificationType,
        LocalDateTime createdAt,
        Boolean isRead,
        T data
)
```

- 위 형태의 응답 DTO를 페이징 으로 감싸서 반환
- 하나의 API 응답에서 알림 타입에 따라 서로 다른 데이터 구조를 반환

<br>

### 새로운 알림 타입 추가

새로운 알림 타입이 추가되더라도 **기존 조회 로직이나 서비스 계층 코드는 수정하지 않습니다.**  
아래 두 지점만 확장하도록 설계했습니다.

- **Batch 조회 로더 추가**
  - 알림 타입이 참조하는 연관 엔티티를 한 번에 조회하는 로직을 `GroupType` 기준으로 등록

- **알림 타입별 Mapper 추가**
  - 해당 알림 타입의 응답 데이터를 생성하는 `NotificationDataMapper` 구현체만 추가

이 구조를 통해,
- 알림 타입 증가에 따른 **조건 분기 코드 증가를 방지**
- **쿼리 구조와 조회 흐름은 그대로 유지**
- 알림 타입 확장 시 **로딩 로직과 응답 매핑만 분리해서 확장 가능**

결과적으로,  
**조회 성능과 응답 확장성을 동시에 유지할 수 있는 구조**로 알림 조회 API를 설계했습니다.

<br>

</details>

# 🔥 트러블 슈팅

- 적어놓은 트러블 슈팅들 중, 기술적으로 설명할 내용이 많거나, 어필할만한 내용을 구체적(문제 발생, 원인 파악, 원인, 해결 및 과정)으로 정리하면 좋을 것 같음.

<details>
  
<summary><strong>SSE 도입 후 프론트 API 요청 무한 Pending 발생 문제</strong></summary>

### 문제 상황

실시간 알림 기능을 구현하기 위해 **SSE(Server-Sent Events)** 기반 기능을 추가했습니다.  
SSE 연결 자체는 정상적으로 유지되는 것처럼 보였지만,  
이후 **프론트엔드에서 일반 API 요청을 보내면 응답이 오지 않고 무한 Pending 상태**에 빠지는 문제가 발생했습니다.

서버는 정상적으로 실행 중이었고,  
프론트에서는 요청이 전송되었지만 응답을 받지 못하는 상태였습니다.

<br>

### 원인 분석 과정

#### **1. 서버 로그 확인**

- 서버 로그를 확인한 결과  
  **DB 커넥션 풀 고갈(Connection Pool Exhausted)** 관련 로그가 발생하는 것을 확인했습니다.
- 새로운 API 요청이 들어와도  
  DB 커넥션을 할당받지 못해 요청이 대기 상태로 멈추고 있었습니다.

#### **2. 커넥션이 반환되지 않는 엔드포인트 확인**

- 일반적인 REST API들은 요청 종료 시 커넥션이 정상적으로 반환되고 있었습니다.
- 커넥션 점유가 지속되는 엔드포인트를 추적한 결과,  
  **SSE 연결을 담당하는 엔드포인트에서 커넥션이 반환되지 않고 있는 것**을 확인했습니다.

#### **3. SSE와 OSIV 설정의 관계 확인**

원인을 조사한 결과, 다음과 같은 구조에서 문제가 발생하고 있었습니다.

- SSE 엔드포인트는 **HTTP 요청을 종료하지 않고 연결을 유지**
- 서버 설정에서 **OSIV(Open Session In View)** 는 기본적으로 활성화 상태
- OSIV가 켜진 상태에서는:
  - HTTP 요청 생명주기 동안 영속성 컨텍스트 유지
  - **DB 커넥션을 반환하지 않고 계속 점유**

결과적으로,

> **SSE 연결이 유지되는 동안  
> OSIV로 인해 DB 커넥션이 함께 묶여 반환되지 않는 구조**였습니다.

<br>

### 해결 방안 및 구현

#### **1. OSIV 설정 비활성화**

해당 서버는 **REST API 전용 서버**로 사용 중이었고,  
컨트롤러 단에서 엔티티를 직접 사용하는 구조가 아니었기 때문에  
OSIV 설정이 필요하지 않았습니다.

이에 따라 OSIV를 명시적으로 비활성화했습니다.

```yaml
spring:
  jpa:
    open-in-view: false
```

</details>

<br>

---

<div align="center">

🧡 *Developed with passion by Team 일단 진행해* 🧡  
_2025 PRGRMS AI·BE DevCourse 3기_

</div>
