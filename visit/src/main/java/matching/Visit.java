package matching;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Entity
@Table(name="Visit_table")
public class Visit {
   // @Autowired
    //VisitReqListRepository VisitReqListRepository;

    @Id
    private Long matchId;
    private String teacher;
    private String visitDate;

    @PostPersist
    public void onPostPersist(){
        VisitAssigned visitAssigned = new VisitAssigned();
        BeanUtils.copyProperties(this, visitAssigned);
        visitAssigned.publishAfterCommit();

/**
 //선생님 assign 처리가 완료된 건은 visitReqList에서 삭제한다. --> 이 주석 풀면 오류남
 VisitReqListRepository.findById(visitAssigned.getMatchId()).ifPresent(VisitReqList->{
 VisitReqListRepository.delete(VisitReqList);
 });
 **/

    }

    @PostRemove
    public void onPostRemove(){
        VisitCanceled visitCanceled = new VisitCanceled();
        BeanUtils.copyProperties(this, visitCanceled);
        visitCanceled.publishAfterCommit();
    }



    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }
    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }
    public String getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(String visitDate) {
        this.visitDate = visitDate;
    }




}



