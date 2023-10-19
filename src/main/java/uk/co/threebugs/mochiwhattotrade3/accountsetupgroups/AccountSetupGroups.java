package uk.co.threebugs.mochiwhattotrade3.accountsetupgroups;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.co.threebugs.mochiwhattotrade3.account.Account;
import uk.co.threebugs.mochiwhattotrade3.setupgroups.SetupGroups;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "account_setup_groups")
public class AccountSetupGroups {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id")
    private Account account;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "setup_groups_id")
    private SetupGroups setupGroups;
}
