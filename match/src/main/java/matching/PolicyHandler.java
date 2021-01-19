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
    @Autowired MatchRepository MatchRepository;
    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverVisitAssigned_(@Payload VisitAssigned visitAssigned){

        if(visitAssigned.isMe()){
            System.out.println("##### listener wheneverVisitAssigned  : " + visitAssigned.toJson());

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

//            MyPage mypage = new MyPage();
            MatchRepository.findById(visitCanceled.getMatchId()).ifPresent(Match -> {
                System.out.println("##### wheneverVisitCanceled_MatchRepository.findById : exist");
                Match.setStatus(visitCanceled.getEventType());
                MatchRepository.save(Match);
            });
        }
    }

}
