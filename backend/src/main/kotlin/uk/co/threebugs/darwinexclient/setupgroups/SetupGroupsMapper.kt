package uk.co.threebugs.darwinexclient.setupgroups

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import java.nio.file.Path

@Mapper(componentModel = "spring")
abstract class SetupGroupsMapper {
    abstract fun toDto(setupGroups: SetupGroups): SetupGroupsDto
    abstract fun toEntity(setupGroupsDto: SetupGroupsDto): SetupGroups

    @Mapping(target = "id", ignore = true)
    abstract fun toEntity(setupGroupsDto: SetupGroupsFile): SetupGroups
    fun toString(path: Path?): String? {
        return path?.toString()
    }

    fun toPath(path: String?): Path? {
        return path?.let { Path.of(it) }
    }
}
