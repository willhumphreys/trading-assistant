package uk.co.threebugs.mochiwhattotrade3.setupgroups;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "setup_groups")
public class SetupGroups {

    private String name;

    private String scriptsDirectory;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
}
