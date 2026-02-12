# 🏦 delayed-transfer-service 💸

본 프로젝트는 카카오페이 지연이체 서비스 구조를 단순화하여,
Java 멀티스레드 환경에서 발생할 수 있는 동기화 문제와
그에 대한 해결 전략을 구현한 실습 프로젝트입니다.

🔗 참고 링크:
[KakaoPay Tech Blog: 지연이체 서비스 개발기: 은행 점검 시간 끝나면 송금해 드릴게요! (feat. 발표 후기)](https://tech.kakaopay.com/post/ifkakao2024-delayed-transfer)

---

## 1. 전체 아키텍처 흐름

```
Main (사용자)
    ↓
TransferCreator (이체 요청 생성)
    ↓
TransferRepository (DB 역할)
    ↓
TransferScheduler (주기적 실행 대상 스캔)
    ↓
TransferQueue (BlockingQueue)
    ↓
TransferConsumer (멀티 스레드 실행)
    ↓
UserLockManager (유저 단위 락 제어)
```

---

## 2. 멀티스레드 구조

본 프로젝트는 Producer–Consumer 패턴을 기반으로 구성되었다.

### 스레드 구성

* **Scheduler Thread 1개**

  * 일정 주기(200ms)로 실행 가능한 이체(`지금시간 >= bankOpenAt`)를 조회
  * 상태 변경 후 Queue에 등록

* **Consumer Thread 2개**

  * Queue에서 작업을 가져와 실행
  * 동시에 여러 이체를 처리
  * 동일 userId는 Lock으로 동시 실행 방지

### 역할 분리 구조

| 스레드        | 역할                  |
| ---------- | ------------------- |
| Scheduler  | 실행 대상 탐색 및 Queue 등록 |
| Consumer-1 | Queue에서 작업 실행       |
| Consumer-2 | Queue에서 작업 실행       |

---

## 3. 시나리오

### 설정

* 유저 A: 3건
* 유저 B: 1건
* 유저 C: 1건
* 은행 점검 종료: 2초 후

### 동작 과정

은행 점검 종료 시간(`bankOpenAt`)은 요청 생성 시점 + 2초로 설정된다.

1. 사용자들이 지연 이체 요청을 등록한다.
2. 모든 요청은 DELAYED 상태로 TransferRepository에 저장된다.
3. 스케줄러는 0.2초마다 반복 실행되며, `now >= bankOpenAt`인 이체를 찾는다.
4. 실행 가능한 이체를 찾으면, 해당 이체의 상태를 DELAYED → PREPARING으로 바꾼 뒤 Queue에 넣는다.
5. Consumer 2개가 동시에 실행을 시작한다.
6. 동일 userId의 이체는 Lock을 통해 동시에 실행되지 않도록 제어한다.
7. 실행이 완료되면 상태를 `DONE`으로 변경하고 Lock을 해제한다.

---

## 4. 상태 흐름

```
DELAYED
  ↓ (스케줄러가 실행 대상으로 선택)
PREPARING
  ↓ (컨슈머 실행 완료)
DONE
```

| 상태        | 의미                  |
| --------- | ------------------- |
| DELAYED   | 은행 점검이 끝나기를 기다리는 상태 |
| PREPARING | 실행 대기열에 등록된 상태      |
| DONE      | 이체 완료 상태            |

---

## 5. 동기화 이슈 시나리오

### 문제 상황 1 – 같은 거래를 두 Consumer가 동시에 실행

Consumer는 여러 스레드로 실행되며 동시에 Queue에서 이체 요청을 가져온다.
이때 Queue가 thread-safe하지 않은 자료구조(예: ArrayList, LinkedList)였다면, 다음과 같은 문제가 발생할 수 있다.

```
Consumer-1: userB의 거래 조회 → status == PREPARING 확인
Consumer-2: userB의 거래 조회 → status == PREPARING 확인
```

두 스레드가 동시에 같은 거래를 확인하고 실행하면
같은 이체가 두 번 실행될 수 있다. (중복 송금 위험)

### 문제 상황 2 – 같은 유저의 여러 거래가 동시에 실행
이 시스템은 Consumer를 2개 이상 실행하여 Queue에 들어온 이체 요청을 병렬 처리한다.

이때 Queue에는 서로 다른 유저 요청뿐 아니라,
같은 유저(userId)의 요청이 여러 개 들어올 수 있다.

```
userA: 3건
userB: 1건
userC: 1건
```

userA의 거래가 다음과 같이 3건 있다고 가정하자.
```
'가', '나', '다' (모두 PREPARING)
```

이 경우 다음과 같은 상황이 발생할 수 있다.
- Consumer-1이 userA의 '가' 이체를 처리 중
- Consumer-2가 동시에 userA의 '나' 이체를 꺼내 처리 시작

즉, 같은 유저의 이체가 동시에 처리될 수 있으며, 이 경우 데이터 정합성이 깨질 수 있다.

---

## 6. 동기화 해결 전략
### 해결 방법 1 - BlockingQueue

```java
LinkedBlockingQueue
```

* `take()`는 하나의 Consumer만 가져갈 수 있음
* 하나의 Queue 원소는 하나의 스레드만 처리

### 해결 방법 2 - User 단위 Lock 적용

```java
lockManager.tryLock(userId);
```

* userId를 기준으로 Lock을 획득
* 동일 userId의 이체는 동시에 실행되지 않도록 제어
* `ReentrantLock` 기반
* 한 유저의 이체는 순차적으로 처리

<img width="572" height="482" alt="image" src="https://github.com/user-attachments/assets/7beb3b69-dec2-4fe3-98d9-2a7b2c85ab6e" />


### User Lock 적용하지 않는 경우
<img width="593" height="413" alt="image" src="https://github.com/user-attachments/assets/6a2662b9-6c31-462d-b2ed-f23a2eb0a3ef" />


---

## 7. 동기화 제어 구조 요약

| 제어 단계         | 목적              |
| ------------- | --------------- |
| synchronized  | 동일 이체 상태 변경 보호  |
| BlockingQueue | 작업 분배 보호        |
| ReentrantLock | 동일 사용자 동시 실행 방지 |

---

## 8. 실행 방법

```
java -jar delayed-transfer-service.jar
```

또는 IDE에서 `Main` 클래스 실행

---

## 9. 실제 서비스 구조와 비교

| 실제 카카오페이      | 본 프로젝트            |
| ------------- | ----------------- |
| Kafka         | BlockingQueue     |
| Consumer 3대 | Consumer 2대       |
| User Lock     | ReentrantLock     |
| DB            | ConcurrentHashMap |
| 스케줄러          | Thread 기반 반복 실행   |

---

## 10. 브랜치 전략

### 브랜치 명명 규칙

```
feat/[기능명]
```

예시:

```
feat/user-lock
feat/delayed-transfer
```

---

## 11. 커밋 컨벤션

### feat
새로운 기능 추가</br>
ex) `feat: 지연 이체 스케줄러 구현`

### refactor
코드 리팩토링</br>
ex) `refactor: Transfer 상태 변경 로직 개선`

### bug
버그 수정</br>
ex) `bug: interrupt 처리 누락으로 인한 스레드 종료 문제 수정`

### docs
문서 수정</br>
ex) `docs: README 업데이트`

### test
테스트 코드 추가/수정</br>
ex) `test: UserLockManager 동시성 테스트 추가`

### build
빌드 설정 변경</br>
ex) `build: jar 실행 설정 추가`

### ci
CI 설정 변경</br>
ex) `ci: GitHub Actions 설정 추가`

### chore
기타 변경</br>
ex) `chore: 불필요한 로그 제거`

### style

코드 스타일 변경</br>
ex) `style: 코드 포맷팅 적용`
