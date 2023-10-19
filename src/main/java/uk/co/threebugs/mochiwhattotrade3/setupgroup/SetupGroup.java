package uk.co.threebugs.mochiwhattotrade3.setupgroup;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.co.threebugs.mochiwhattotrade3.setupgroups.SetupGroups;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "setup_group")
public final class SetupGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "setup_groups_id")
    private SetupGroups setupGroups;

    private String path;
    private String symbol;
    private Boolean enabled;
}

