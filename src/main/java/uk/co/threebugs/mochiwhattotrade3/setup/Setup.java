package uk.co.threebugs.mochiwhattotrade3.setup;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.co.threebugs.mochiwhattotrade3.setupgroup.SetupGroup;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "setup")
public class Setup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "setup_group_id")
    private SetupGroup setupGroup;

    private String symbol;

    @Column(name = "rank_column")
    private Integer rank;

    private Integer dayOfWeek;

    private Integer hourOfDay;

    @Column(name = "stop_column")
    private Integer stop;
    @Column(name = "limit_column")
    private Integer limit;

    private Integer tickOffset;

    private Integer tradeDuration;

    private Integer outOfTime;

    public boolean isLong() {
        return stop < limit;
    }

    public String concatenateFields() {

        return "id: %d- setupGroup: %d- symbol: %s- rank: %d- dayOfWeek: %d- hourOfDay: %d- stop: %d- limit: %d- tickOffset: %d- tradeDuration: %d- outOfTime: %d- isLong: %s".formatted(id, setupGroup.getId(), symbol, rank, dayOfWeek, hourOfDay, stop, limit, tickOffset, tradeDuration, outOfTime, getDirection())
                                                                                                                                                                              .replace(",", "-");
    }

    public String getDirection() {
        return stop < limit ? "LONG" : "SHORT";
    }
}
