package matching.external;

public class Payment {

    private Long matchId;
    private Integer price;
    private String paymentAction;

    public Long getMatchId() {
        return matchId;
    }
    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }
    public Integer getPrice() {
        return price;
    }
    public void setPrice(Integer price) {
        this.price = price;
    }
    public String getPaymentAction() {
        return paymentAction;
    }
    public void setPaymentAction(String paymentAction) {
        this.paymentAction = paymentAction;
    }

}
