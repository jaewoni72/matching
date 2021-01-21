# matching
- 선생님-학생 매칭 서비스

- 본 예제는 MSA/DDD/Event Storming/EDA 를 포괄하는 분석/설계/구현/운영 전단계를 커버하도록 구성한 예제입니다.
이는 클라우드 네이티브 애플리케이션의 개발에 요구되는 체크포인트들을 통과하기 위한 예시 답안을 포함합니다.
- 체크포인트 : https://workflowy.com/s/assessment-check-po/T5YrzcMewfo4J6LW


### Table of contents

- [예제 ](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계)
  - [구현:](#구현-)
    - [DDD 의 적용](#DDD-의-적용)
    - [동기식 호출 과 Fallback 처리](#동기식-호출과-Fallback-처리)
    - [이벤트드리븐 아키텍쳐의 구현](#이벤트드리븐-아키텍쳐의-구현)
    - [Poliglot](#폴리글랏-퍼시스턴스)
    - [Gateway](#Gateway)
  - [운영](#운영)
    - [CI/CD 설정](#cicd설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-/-서킷-브레이킹-/-장애격리)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [Persistence Volume](#Persistence-Volume)
    - [Self_healing (liveness probe)](#Self_healing-(liveness-probe))
    - [무정지 재배포](#무정지-재배포)


### 서비스 시나리오

기능적 요구사항
1. 학생이 금액을 제시하여 매칭 요청을 한다. (선생님을 선택할 수 없다)
1. 학생이 결제한다.
1. 금액이 결제되면 방문요청내역이 목록에 조회된다.
1. 선생님은 방문요청을 조회한 후 선생님 이름과 만남시간을 입력하여 방문을 확정한다.
1. 학생이 매칭을 취소할 수 있다.
1. 매칭요청이 취소되면 방문 및 쿠폰발행이 취소된다.
1. 학생은 myPage에서 매칭 상태를 중간중간 조회할 수 있다.
1. 매칭요청 화면에서 상태를 조회할 수 있다. 
1. 매칭요청/결제요청/방문확정/쿠폰발행/결제취소/방문취소/쿠폰취소 시 상태가 변경된다. 

비기능적 요구사항
1. 트랜잭션
    1. 결제가 되지 않은 매칭건은 아예 매칭이 성립되지 않아야 한다. Sync 호출
1. 장애격리
    1. 방문관리 기능이 수행되지 않더라도 매칭요청은 365/24 받을 수 있어야 한다. Async(event-driven) Eventual Consistency
1. 성능
    1. 학생이 매칭시스템에서 확인할 수 있는 상태를 마이페이지(프론트엔드)에서 확인할 수 있어야 한다 CQRS
    1. 상태가 바뀔때마다 myPage에서는 변경된 상태를 조회할 수 있어야 한다. Event driven
    1. 방문 상태가 변경될 때 마다 매칭관리에서 상태가 변경되어야 한다. corelation


### 체크포인트
```
- 분석 설계


  - 이벤트스토밍: 
    - 스티커 색상별 객체의 의미를 제대로 이해하여 헥사고날 아키텍처와의 연계 설계에 적절히 반영하고 있는가?
    - 각 도메인 이벤트가 의미있는 수준으로 정의되었는가?
    - 어그리게잇: Command와 Event 들을 ACID 트랜잭션 단위의 Aggregate 로 제대로 묶었는가?
    - 기능적 요구사항과 비기능적 요구사항을 누락 없이 반영하였는가?    

  - 서브 도메인, 바운디드 컨텍스트 분리
    - 팀별 KPI 와 관심사, 상이한 배포주기 등에 따른  Sub-domain 이나 Bounded Context 를 적절히 분리하였고 그 분리 기준의 합리성이 충분히 설명되는가?
      - 적어도 3개 이상 서비스 분리
    - 폴리글랏 설계: 각 마이크로 서비스들의 구현 목표와 기능 특성에 따른 각자의 기술 Stack 과 저장소 구조를 다양하게 채택하여 설계하였는가?
    - 서비스 시나리오 중 ACID 트랜잭션이 크리티컬한 Use 케이스에 대하여 무리하게 서비스가 과다하게 조밀히 분리되지 않았는가?
  - 컨텍스트 매핑 / 이벤트 드리븐 아키텍처 
    - 업무 중요성과  도메인간 서열을 구분할 수 있는가? (Core, Supporting, General Domain)
    - Request-Response 방식과 이벤트 드리븐 방식을 구분하여 설계할 수 있는가?
    - 장애격리: 서포팅 서비스를 제거 하여도 기존 서비스에 영향이 없도록 설계하였는가?
    - 신규 서비스를 추가 하였을때 기존 서비스의 데이터베이스에 영향이 없도록 설계(열려있는 아키택처)할 수 있는가?
    - 이벤트와 폴리시를 연결하기 위한 Correlation-key 연결을 제대로 설계하였는가?

  - 헥사고날 아키텍처
    - 설계 결과에 따른 헥사고날 아키텍처 다이어그램을 제대로 그렸는가?
    
- 구현
  - [DDD] 분석단계에서의 스티커별 색상과 헥사고날 아키텍처에 따라 구현체가 매핑되게 개발되었는가?
    - Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 데이터 접근 어댑터를 개발하였는가
    - [헥사고날 아키텍처] REST Inbound adaptor 이외에 gRPC 등의 Inbound Adaptor 를 추가함에 있어서 도메인 모델의 손상을 주지 않고 새로운 프로토콜에 기존 구현체를 적응시킬 수 있는가?
    - 분석단계에서의 유비쿼터스 랭귀지 (업무현장에서 쓰는 용어) 를 사용하여 소스코드가 서술되었는가?
  - Request-Response 방식의 서비스 중심 아키텍처 구현
    - 마이크로 서비스간 Request-Response 호출에 있어 대상 서비스를 어떠한 방식으로 찾아서 호출 하였는가? (Service Discovery, REST, FeignClient)
    - 서킷브레이커를 통하여  장애를 격리시킬 수 있는가?
  - 이벤트 드리븐 아키텍처의 구현
    - 카프카를 이용하여 PubSub 으로 하나 이상의 서비스가 연동되었는가?
    - Correlation-key:  각 이벤트 건 (메시지)가 어떠한 폴리시를 처리할때 어떤 건에 연결된 처리건인지를 구별하기 위한 Correlation-key 연결을 제대로 구현 하였는가?
    - Message Consumer 마이크로서비스가 장애상황에서 수신받지 못했던 기존 이벤트들을 다시 수신받아 처리하는가?
    - Scaling-out: Message Consumer 마이크로서비스의 Replica 를 추가했을때 중복없이 이벤트를 수신할 수 있는가
    - CQRS: Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능한가?

  - 폴리글랏 플로그래밍
    - 각 마이크로 서비스들이 하나이상의 각자의 기술 Stack 으로 구성되었는가?
    - 각 마이크로 서비스들이 각자의 저장소 구조를 자율적으로 채택하고 각자의 저장소 유형 (RDB, NoSQL, File System 등)을 선택하여 구현하였는가?
  - API 게이트웨이
    - API GW를 통하여 마이크로 서비스들의 집입점을 통일할 수 있는가?
    - 게이트웨이와 인증서버(OAuth), JWT 토큰 인증을 통하여 마이크로서비스들을 보호할 수 있는가?
- 운영
  - SLA 준수
    - 셀프힐링: Liveness Probe 를 통하여 어떠한 서비스의 health 상태가 지속적으로 저하됨에 따라 어떠한 임계치에서 pod 가 재생되는 것을 증명할 수 있는가?
    - 서킷브레이커, 레이트리밋 등을 통한 장애격리와 성능효율을 높힐 수 있는가?
    - 오토스케일러 (HPA) 를 설정하여 확장적 운영이 가능한가?
    - 모니터링, 앨럿팅: 
  - 무정지 운영 CI/CD (10)
    - Readiness Probe 의 설정과 Rolling update을 통하여 신규 버전이 완전히 서비스를 받을 수 있는 상태일때 신규버전의 서비스로 전환됨을 siege 등으로 증명 
    - Contract Test :  자동화된 경계 테스트를 통하여 구현 오류나 API 계약위반를 미리 차단 가능한가?
```


# 분석/설계

## Event Storming 결과
- MSAEz 로 모델링한 이벤트스토밍 결과:  http://www.msaez.io/#/storming/OmlGD6ICVSRtR6dYFF7jEteSnoS2/mine/3e0ad62233bdff4bdd9d03458ee27b40
![이벤트스토밍결과](https://user-images.githubusercontent.com/66051393/105207597-0c4eba80-5b8b-11eb-9efe-7892f4985479.png)
```
- 도메인 서열 분리
    - Core Domain:  match, visit, coupon  : 핵심 서비스이며, 연간 Up-time SLA 수준을 99.999% 목표, 배포주기는 match의 경우 1주일 1회 미만, visit의 경우 1개월 1회 미만
    - Supporting Domain:  visitReqLists , myPages : 경쟁력을 내기위한 서비스이며, SLA 수준은 연간 60% 이상 uptime 목표, 배포주기는 각 팀의 자율이나 표준 스프린트 주기가 1주일 이므로 1주일 1회 이상을 기준으로 함.
    - General Domain:   payment(결제) : 결제서비스로 3rd Party 외부 서비스를 사용하는 것이 경쟁력이 높음
```

## 헥사고날 아키텍처 다이어그램 도출
![핵사고날아키텍쳐](https://user-images.githubusercontent.com/66051393/105129090-aa109e00-5b27-11eb-84e6-124571b6a187.png)
```
  - Chris Richardson, MSA Patterns 참고하여 Inbound adaptor와 Outbound adaptor를 구분함
  - 호출관계에서 PubSub 과 Req/Res 를 구분함
  - 서브 도메인과 바운디드 컨텍스트의 분리:  각 팀의 KPI 별로 아래와 같이 관심 구현 스토리를 나눠가짐
```

# 구현
분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 8085 이다)
```
cd match    // port:8081
mvn spring-boot:run

cd visit    // port:8082
mvn spring-boot:run  

cd payment  // port:8083
mvn spring-boot:run 

cd mypage  // port:8084
mvn spring-boot:run  

cd coupon  // port:8085
mvn spring-boot:run
```

## DDD 의 적용
- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다: (예시는 match 마이크로 서비스). 이때 가능한 현업에서 사용하는 언어 (유비쿼터스 랭귀지)를 그대로 사용하였고, 모든 구현에 있어서 영문으로 사용하여 별다른  오류없이 구현하였다.
```
package matching;

import javax.persistence.*;

import matching.external.Payment;
import matching.external.PaymentService;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Match_table")
public class Match {

    @Id
    private Long id;
    private Integer price;
    private String status;

    @PostPersist
    public void onPostPersist(){
        MatchRequested matchRequested = new MatchRequested();
        BeanUtils.copyProperties(this, matchRequested);
        matchRequested.publishAfterCommit();

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.
        Payment payment = new Payment();
        // mappings goes here

        //변수 setting
        payment.setMatchId(Long.valueOf(this.getId()));
        payment.setPrice(Integer.valueOf(this.getPrice()));
        payment.setPaymentAction("Approved");

        MatchApplication.applicationContext.getBean(PaymentService.class)
                .paymentRequest(payment);
    }

    @PreUpdate
    public void onPreUpdate(){
        if("cancel".equals(status)) {
            MatchCanceled matchCanceled = new MatchCanceled();
            BeanUtils.copyProperties(this, matchCanceled);
            matchCanceled.publishAfterCommit();
        }
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Integer getPrice() {
        return price;
    }
    public void setPrice(Integer price) {
        this.price = price;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
    }
}

```
- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```
package matching;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface MatchRepository extends PagingAndSortingRepository<Match, Long>{ }
```


- 적용 후 REST API 의 테스트
```
# match 서비스의 접수처리
http POST http://localhost:8081/matches id=1000 price=10000 status=matchRequest
```
![1 match에서명령어날림](https://user-images.githubusercontent.com/66051393/105128218-efcc6700-5b25-11eb-8771-757bd2a43c91.png)
```
# match 서비스의 접수상태확인
http http://localhost:8081/matches
```
![2 match테이블에쌓임](https://user-images.githubusercontent.com/66051393/105128422-5baecf80-5b26-11eb-8303-ce5165a0c5c5.png)
```
# payment 서비스의 상태확인
http http://localhost:8083/payments
```
![3 payment에서match에서날린데이터확인](https://user-images.githubusercontent.com/66051393/105128518-86008d00-5b26-11eb-821e-122c5ebd8d8b.png)
```
# match 서비스에 대한 visit 응답
http POST http://localhost:8082/visits matchId=1000 teacher=kim visitDate=20210101
```
![4 visit에서선생님방문계획작성](https://user-images.githubusercontent.com/66051393/105128586-a6304c00-5b26-11eb-81de-9348f27d110b.png)
```
# coupon 서비스의 상태확인
http http://localhost:8085/coupons
```
![5 coupon테이블에쌓임](https://user-images.githubusercontent.com/66051393/105257447-27411f00-5bcb-11eb-9932-8a7325737aff.png)
```
# 진행상태에 대한 확인
http http://localhost:8084/myPages
```
![5 mypage테이블에쌓임](https://user-images.githubusercontent.com/66051393/105257718-b3534680-5bcb-11eb-9b04-fc63123f5359.png)


## 동기식 호출과 Fallback 처리

- 분석단계에서의 조건 중 하나로 접수(match)->결제(payment) 간의 호출은 동기식으로 호출하고자  동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient를 이용하여 호출하도록 한다.

- 결제서비스를 호출하기 위하여 FeignClient 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현

```
# (payment) PaymentService.java

package matching.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@FeignClient(name="payment", url="${api.payment.url}")
public interface PaymentService {

    @RequestMapping(method= RequestMethod.POST, path="/payments")
    public void paymentRequest(@RequestBody Payment payment);

}
```

- match접수를 받은 직후(@PostPersist) 결제를 요청하도록 처리
```
# match.java (Entity)
  @PostPersist
    public void onPostPersist(){
        MatchRequested matchRequested = new MatchRequested();
        BeanUtils.copyProperties(this, matchRequested);
        matchRequested.publishAfterCommit();

        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.
        Payment payment = new Payment();
        // mappings goes here

        //변수 setting
        payment.setMatchId(Long.valueOf(this.getId()));
        payment.setPrice(Integer.valueOf(this.getPrice()));
        payment.setPaymentAction("Approved");

        MatchApplication.applicationContext.getBean(PaymentService.class)
                .paymentRequest(payment);
    }
```

- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 결제 시스템이 장애가 나면 접수도 못받는다는 것을 확인

```
# 결제 (payment) 서비스를 잠시 내려놓음

# 접수처리
http POST http://localhost:8081/matches id=2000 price=20000 status=matchRequest   #Fail
```
![11 payment내리면match안됨](https://user-images.githubusercontent.com/66051393/105129385-4aff5900-5b28-11eb-9b0a-9cead946ab22.png)
```

# payment서비스 재기동
cd payment
mvn spring-boot:run

# match 처리
http localhost:8088/matches id=2006 price=20000 status=matchRequest  #Success
```
![11 payment올리면match됨](https://user-images.githubusercontent.com/66051393/105129536-9f0a3d80-5b28-11eb-95ed-14f9cde24a44.png)

```
또한 과도한 요청시에 서비스 장애가 도미노 처럼 벌어질 수 있다. (서킷브레이커, 폴백 처리는 운영단계에서 설명한다.)
```

## 이벤트드리븐 아키텍쳐의 구현

### 비동기식 호출 

- 결제가 완료 된 후에 방문(visit) 시스템으로 이를 알려주는 행위는 동기식이 아닌 비동기식으로 처리하며, 방문시스템의 처리를 위하여 매칭요청/결제가 블로킹 되지 않도록 처리한다.
  이를 위하여 결제요청이력에 기록을 남긴 후에 곧바로 결제 완료 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
 
```
package matching;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;

@Entity
@Table(name="Payment_table")
public class Payment {

    @Id
    private Long matchId;
    private Integer price;
    private String paymentAction;

    @PostPersist
    public void onPostPersist(){
        PaymentApproved paymentApproved = new PaymentApproved();
        BeanUtils.copyProperties(this, paymentApproved);
        paymentApproved.publishAfterCommit();


    }
```

- 방문 서비스에서는 결제완료 이벤트를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다
```
package matching;

import matching.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired VisitReqListRepository VisitReqListRepository;
    @Autowired VisitRepository VisitRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentApproved_(@Payload PaymentApproved paymentApproved){

        if(paymentApproved.isMe()){
            System.out.println("##### listener  : " + paymentApproved.toJson());


            //승인완료 시 승인완료된 리스트를 visitReqList에 받아서 보여줄 수 있도록 id setting

            VisitReqList visitReqList = new VisitReqList();

            visitReqList.setId(paymentApproved.getMatchId());
            VisitReqListRepository.save(visitReqList);

        }
    }
```


### 시간적 디커플링 / 장애격리 
- 방문(visit) 시스템은 결제(payment) 시스템과 완전히 분리되어있으며 이벤트 수신에 따라 처리되기 때문에, 방문 시스템이 유지보수로 인해 잠시 내려간 상태라도 방문요청(match) 및 결제(payment)하는데에 문제가 없다
```
# 방문 서비스(visit)를 잠시 놓은 후 매칭 요청 처리
```
![11 시간적티커플링_visit서비스내린_화면](https://user-images.githubusercontent.com/66051393/105129722-07591f00-5b29-11eb-9517-4165255121c1.png)

```
# 방문 서비스 다시 가동
cd visit
mvn spring-boot:run
```
```
# 가동 전/후의 방문상태 확인
```
```
# 신규 접수된 매칭요청건에 대해 선생님과 방문일자 매칭
http POST http://localhost:8082/visits matchId=3000 teacher=Smith visitDate=20210101 
http localhost:8082/visits     
```
![11 시간적티커플링_visit구현과실행_2](https://user-images.githubusercontent.com/66051393/105130890-31abdc00-5b2b-11eb-8273-167c4f72c599.png)


### SAGA / Corelation
- 방문(visit) 시스템에서 상태가 방문확정 또는 방문취소로 변경되면 매치(match) 시스템 원천데이터의 상태(status) 정보가 update된다.  

```
# mypage > PolicyHandler.java

  @StreamListener(KafkaProcessor.INPUT)
  public void wheneverVisitAssigned_(@Payload VisitAssigned visitAssigned){

      if(visitAssigned.isMe()){
          System.out.println("##### listener wheneverVisitAssigned  : " + visitAssigned.toJson());

          //방문 assign 이벤트를 수신하였을 때 해당 ID를 찾아서 상태값을 visitAssigned로 변경
          MatchRepository.findById(visitAssigned.getMatchId()).ifPresent(Match ->{
              System.out.println("##### wheneverVisitAssigned_MatchRepository.findById : exist" );

              Match.setStatus(visitAssigned.getEventType()); //상태값은 모두 이벤트타입으로 셋팅함
              MatchRepository.save(Match);
          });

      }
  }


  @StreamListener(KafkaProcessor.INPUT)
  public void wheneverVisitCanceled_(@Payload VisitCanceled visitCanceled) {

      if (visitCanceled.isMe()) {
          System.out.println("##### listener  : " + visitCanceled.toJson());

          //방문취소 이벤트를 수신하였을 때 해당 ID를 찾아서 상태값을 visitCanceled로 변경
          MatchRepository.findById(visitCanceled.getMatchId()).ifPresent(Match -> {
              System.out.println("##### wheneverVisitCanceled_MatchRepository.findById : exist");
              Match.setStatus(visitCanceled.getEventType());
              MatchRepository.save(Match);
          });
      }
  }
```


### CQRS
- 매칭 상태가 변경될 때 마다 mypage에서 event를 수신하여 mypage의 매칭상태를 조회하도록 view를 구현하였다.   

```
# mypage > PolicyHandler.java

@StreamListener(KafkaProcessor.INPUT)
public void wheneverVisitCanceled_(@Payload VisitCanceled visitCanceled){

    if(visitCanceled.isMe()){
        System.out.println("##### listener  : " + visitCanceled.toJson());

        MyPageRepository.findById(visitCanceled.getMatchId()).ifPresent(MyPage ->{
            System.out.println("##### wheneverVisitCanceled_MyPageRepository.findById : exist" );
            MyPage.setStatus(visitCanceled.getEventType());
            MyPageRepository.save(MyPage);
        });
    }
}
@StreamListener(KafkaProcessor.INPUT)
public void wheneverVisitAssigned_(@Payload VisitAssigned visitAssigned){

    if(visitAssigned.isMe()){
        System.out.println("##### listener wheneverVisitAssigned  : " + visitAssigned.toJson());

        MyPageRepository.findById(visitAssigned.getMatchId()).ifPresent(MyPage ->{
            System.out.println("##### wheneverVisitAssigned_MyPageRepository.findById : exist" );

            MyPage.setStatus(visitAssigned.getEventType()); //상태값은 모두 이벤트타입으로 셋팅함
            MyPage.setTeacher(visitAssigned.getTeacher());
            MyPage.setVisitDate(visitAssigned.getVisitDate());
            MyPageRepository.save(MyPage);
        });

    }
}
@StreamListener(KafkaProcessor.INPUT)
public void wheneverPaymentApproved_(@Payload PaymentApproved paymentApproved){

    if(paymentApproved.isMe()){
        System.out.println("##### listener  : " + paymentApproved.toJson());

        MyPage mypage = new MyPage();
        mypage.setId(paymentApproved.getMatchId());
        mypage.setPrice(paymentApproved.getPrice());
        mypage.setStatus(paymentApproved.getEventType());
        MyPageRepository.save(mypage);
    }
}
@StreamListener(KafkaProcessor.INPUT)
public void wheneverPaymentCanceled_(@Payload PaymentCanceled paymentCanceled){

    if(paymentCanceled.isMe()){
        System.out.println("##### listener  : " + paymentCanceled.toJson());


        MyPageRepository.findById(paymentCanceled.getMatchId()).ifPresent(MyPage ->{
            System.out.println("##### wheneverPaymentCanceled_MyPageRepository.findById : exist" );

            MyPage.setStatus(paymentCanceled.getEventType()); //상태값은 모두 이벤트타입으로 셋팅함
            MyPageRepository.save(MyPage);
        });
    }
}
@StreamListener(KafkaProcessor.INPUT)
public void wheneverMatchCanceled_(@Payload MatchCanceled matchCanceled){

    if(matchCanceled.isMe()){
        System.out.println("##### listener  : " + matchCanceled.toJson());

        MyPageRepository.findById(matchCanceled.getId()).ifPresent(MyPage ->{
            System.out.println("##### wheneverMatchCanceled_MyPageRepository.findById : exist" );

            MyPage.setStatus(matchCanceled.getEventType()); //상태값은 모두 이벤트타입으로 셋팅함
            MyPageRepository.save(MyPage);
        });

    }
}
    
```
```
# mypage의 view로 조회
```
![CQRS_mypage화면](https://user-images.githubusercontent.com/66051393/105131926-44271500-5b2d-11eb-8d5f-f522ee20e8a8.png)


### 폴리글랏 퍼시스턴스
- match 구현체는 다른 서비스와 구별을 위해 별도 hsqldb를 사용 하였다. 이를 위해 match내 pom.xml에 dependency를 h2database에서 hsqldb로 변경함

```
# match의 pom.xml dependency를 수정하여 DB변경
```
![폴리글랏퍼시스턴스](https://user-images.githubusercontent.com/66051393/105132458-4b9aee00-5b2e-11eb-9c9d-32161822fb43.png)

```
# match 구현체 정상적으로 처리되는 것을 확인
```
![폴리글랏퍼시스턴스_정상화면](https://user-images.githubusercontent.com/66051393/105132515-666d6280-5b2e-11eb-8dc5-07692f14a9ea.png)


### Gateway
- Gateway 구현체의 Application.yml 설정
```
server:
  port: 8088

---

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: match
          uri: http://localhost:8081
          predicates:
            - Path=/matches/** 
        - id: visit
          uri: http://localhost:8082
          predicates:
            - Path=/visits/**,/visitReqLists/**
        - id: payment
          uri: http://localhost:8083
          predicates:
            - Path=/payments/**
        - id: mypage
          uri: http://localhost:8084
          predicates:
            - Path=/myPages/**
        - id: coupon
            uri: http://localhost:8085
            predicates:
              - Path=/coupons/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true


---

spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: match
          uri: http://match:8080
          predicates:
            - Path=/matches/** 
        - id: visit
          uri: http://visit:8080
          predicates:
            - Path=/visits/**,/visitReqLists/**
        - id: payment
          uri: http://payment:8080
          predicates:
            - Path=/payments/** 
        - id: mypage
          uri: http://mypage:8080
          predicates:
            - Path=/myPages/**
        - id: coupon
          uri: http://coupon:8080
          predicates:
            - Path=/coupons/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080
```

- Gateway 구현체의 Application.yml 설정  
  Gateway 서비스 실행 상태에서 match와 visit 서비스가 정상적으로 실행됨
```
http POST http://match:8080/matches id=9000 price=1000 status=matchRequest
http POST http://visit:8081/matches id=9000 price=1000 status=matchRequest
```
![매칭요청/방문확정](https://user-images.githubusercontent.com/66051393/105153182-7cd5e700-5b4b-11eb-9c21-246045d62f92.png)


## 운영

### CI/CD Pipeline설정
- 서비스들은 각자의 source repository에 구성되었고, 사용한 CI/CD 플랫폼은 Azure를 사용하였으며, pipeline build script 는 각 프로젝트 폴더 이하에 deployment.yml, service.yml에 포함함

```
# CI 설정
```
![CI 설정](https://user-images.githubusercontent.com/66051393/105149096-827cfe00-5b46-11eb-8890-d7d5e13b8f54.png)


```
# CD 설정
```
![CD 설정](https://user-images.githubusercontent.com/66051393/105149254-b0fad900-5b46-11eb-9d55-6c3074ddd1ab.png)

```
# 신규 구현체(coupon) 추가 설정
```
![coupon 설정](https://user-images.githubusercontent.com/66051393/105149284-b821e700-5b46-11eb-984c-f532eb8de37b.png)

```
서비스정상기동 확인
```
![서비스기동 확인](https://user-images.githubusercontent.com/66051393/105149346-ca038a00-5b46-11eb-88a7-0a173e92deba.png)


### 동기식 호출 / 서킷 브레이킹 / 장애격리
- 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현함
시나리오는 매칭요청(match)-->결제(payment) 시의 연결을 RESTful Request/Response 로 연동하여 구현이 되어있고, 결제 요청이 과도할 경우 CB 를 통하여 장애격리.

```
# Hystrix 를 설정:  요청처리 쓰레드에서 처리시간이 680 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정
 match서비스의 application.yml을 hystrix 라이브러리 추가
```
![서비스기동 확인](https://user-images.githubusercontent.com/66051393/105186079-abb48300-5b74-11eb-88a8-676ee8676c10.png)

```
# 피호출 서비스(결제:payment) 의 임의 부하 처리 : 400 밀리에서 증감 300 밀리 증감이 발생하도록 설정
  payment서비스 Payment.java (Entity)
```
![피호출화면증적](https://user-images.githubusercontent.com/66051393/105187317-fd114200-5b75-11eb-8cb8-76d545906df0.png)

```
# 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인
  동시사용자 100명 / 60초 동안 부하 발생

 siege -c100 -t60S -r5 -v --content-type "application/json" 'http://match:8080/matches POST {"id": 600, "price":1000, "status": "matchRequest"}' 
``` 
![03 화면증적](https://user-images.githubusercontent.com/66051393/105108628-ffd05080-5afd-11eb-826d-66a7b252c09a.png)

```
# 서킷브레이크가 발생하지 않아 아래와 같이 여러 조건으로 추가 부하테스트를 진행하였으나, 500 에러를 발견할 수 없었음

 - siege -c255 -t1M -r5 -v --content-type "application/json" 'http://match:8080/matches POST {"id": 600, "price":1000, "status": "matchRequest"}'  
 - siege -c255 -t2M -r5 -v --content-type "application/json" 'http://match:8080/matches POST {"id": 600, "price":1000, "status": "matchRequest"}' 
```


### 오토스케일 아웃
- 앞서 CB는 시스템을 안정되게 운영할 수 있게 해줬지만 사용자의 요청을 100% 받아들여주지 못했기 때문에 이에 대한 보완책으로 자동화된 확장 기능을 적용함.
  visit 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정함. 설정은 CPU 사용량이 10프로를 넘어서면 replica 를 10개까지 늘려줌

```
kubectl autoscale deploy visit --min=1 --max=10 --cpu-percent=10
```
![오토스케일화면증적](https://user-images.githubusercontent.com/66051393/105187684-685b1400-5b76-11eb-9e77-31167c4d0521.png)

```
kubectl exec -it pod siege -- /bin/bash
siege -c20 -t120S -v http://visit:8080/visits/600
```
![siege부하발생_화면증적](https://user-images.githubusercontent.com/66051393/105187844-96d8ef00-5b76-11eb-996e-04236a1677f6.png)

```
부하에 따라 visit pod의 cpu 사용률이 증가했고, Pod Replica 수가 증가하는 것을 확인할 수 있었음
```
![화면증적_스케일아웃적용](https://user-images.githubusercontent.com/66051393/105187925-ace6af80-5b76-11eb-926d-9f71a328bd4f.png)


### Persistence Volume
- visit 서비스를 마이크로서비스로 배포하면서 영속성 있는 저장장치(Persistent Volume)를 적용함

```
# PVC 설정 확인
  kubectl describe pvc azure-pvc
```
![PVC설정후_describe](https://user-images.githubusercontent.com/66051393/105187979-bf60e900-5b76-11eb-91ed-e7385ecc6c43.png)

```
# PVC Volume설정 확인
  mypage 구현체에서 해당 pvc를 volumeMount 하여 사용 (kubectl get deployment mypage -o yaml)
```
![PVC Volume](https://user-images.githubusercontent.com/66051393/105042760-f87e5800-5aa7-11eb-9447-2ecb7d427623.png)

```
# mypage pod에 PV Volume설정 확인
```
![mypagepod에 PV Volume설정 확인](https://user-images.githubusercontent.com/66051393/105188212-ff27d080-5b76-11eb-8bfe-d9009f0df48f.png)

```
# mypage pod에 접속하여 mount 용량 확인
```
![mount_설정확인](https://user-images.githubusercontent.com/66051393/105188251-0a7afc00-5b77-11eb-9ee4-38e5ec20e874.png)


### Self_healing (liveness probe)
- mypage 서비스의 deployment.yaml 소스 서비스포트를 8080이 아닌 고의로 8081로 변경하여 재배포한 후 pod 상태 확인

```
# 정상 서비스포트 확인
```
![증적자료_pod yaml확인](https://user-images.githubusercontent.com/66051393/105188376-31393280-5b77-11eb-9e94-220936a2dc41.png)

```
# 비정상 상태의 pod 정보 확인
```
![증적자료_POD비정상으로재기동](https://user-images.githubusercontent.com/66051393/105188430-3e562180-5b77-11eb-9c21-5680544bc6e3.png)


### 무정지 재배포
- 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 CB 설정을 제거함
seige 로 배포작업 직전에 워크로드를 모니터링 함.

```
# match 서비스의 Deployment.yml에서 Readiness 설정 삭제 후 CI/CD를 통해 재배포
```
![readiness제외후 배포](https://user-images.githubusercontent.com/66051393/105188942-c6d4c200-5b77-11eb-8383-7f5da4150144.png)

```
# 부하측정을 seige로 진입하여 Availability  확인
```
![readiness빠진상태에서재배포시_부하](https://user-images.githubusercontent.com/66051393/105189152-03082280-5b78-11eb-89fb-98a759470f34.png)

```
# 부하측정을 seige로 진입하여 Availability 확인
  배포기간중 Availability가 평소 100%에서 70%대로 떨어지는 것을 확인. 원인은 쿠버네티스가 성급하게 새로 올려진 서비스를 READY상태로 인식하여 서비스 유입을 진행한 것이기 때문. 
  이를 막기위해 Readiness Probe 를 설정함
```
```
# match 서비스의 Deployment.yml에 Readiness설정 추가 후 CI/CD를 통해 재배포
```
![readiness설정후재배포시02-1](https://user-images.githubusercontent.com/66051393/105206017-369f7880-5b89-11eb-9775-ddbb121b30d0.png)
```
# 신규 pod 생성
```
![readiness설정후_재배포시_01](https://user-images.githubusercontent.com/66051393/105190112-07810b00-5b79-11eb-9e31-5e55c222dc6e.png)
```
# 기존 pod 삭제
```
![readiness설정후_재배포시_02](https://user-images.githubusercontent.com/66051393/105190148-11a30980-5b79-11eb-8cfc-470830a3955f.png)

```
# 신규 pod 활성화
```
![readiness설정후_재배포시_03](https://user-images.githubusercontent.com/66051393/105190396-4d3dd380-5b79-11eb-8a98-6a1fdb8f2cb8.png)
 
```
# 동일한 시나리오로 Availability 확인
```
![siege부하발행시_100퍼센트가용성확보_01](https://user-images.githubusercontent.com/66051393/105190490-647cc100-5b79-11eb-80c1-4c32f0db95ef.png)

```
# 배포기간 동안 Availability 가 변화없기 때문에 무정지 재배포가 성공한 것으로 확인됨.
```
