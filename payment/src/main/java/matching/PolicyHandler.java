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

    @Autowired PaymentRepository PaymentRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverMatchCanceled_(@Payload MatchCanceled matchCanceled){

        if(matchCanceled.isMe()){
            System.out.println("################ 매칭 취소요청으로 인한 주문 취소 ################ ");
            System.out.println("##### listener PaymentCancel : " + matchCanceled.toJson());

            PaymentRepository.findById(matchCanceled.getId()).ifPresent(Payment ->{
                Payment.setPaymentAction("Cancel");
                PaymentRepository.save(Payment);
//                PaymentRepository.delete(payment);  //매칭 취소요청이 오면 payment삭제
            });

        }
    }

}
