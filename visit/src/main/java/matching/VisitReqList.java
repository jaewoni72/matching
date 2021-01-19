package matching;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="VisitReqList_table")
public class VisitReqList {

        @Id
        private Long id;


        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

}
