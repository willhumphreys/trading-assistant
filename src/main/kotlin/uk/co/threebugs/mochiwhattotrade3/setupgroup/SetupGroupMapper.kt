package uk.co.threebugs.mochiwhattotrade3.setupgroup

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.co.threebugs.mochiwhattotrade3.setupgroups.SetupGroups
import java.nio.file.Path

@Mapper(componentModel = "spring")
abstract class SetupGroupMapper {
    @Mapping(source = "path", target = "path")
    @Mapping(target = "setupGroups", source = "setupGroups")
    abstract fun toDto(setup: SetupGroup): SetupGroupDto

    @Mapping(source = "setupGroups", target = "setupGroups")
    @Mapping(source = "setupDto.id", target = "id")
    abstract fun toEntity(setupDto: SetupGroupDto, setupGroups: SetupGroups): SetupGroup
    fun toString(path: Path?): String? {
        return path?.toString()
    }

    fun toPath(path: String?): Path? {
        return path?.let { Path.of(path) }
    }
}
