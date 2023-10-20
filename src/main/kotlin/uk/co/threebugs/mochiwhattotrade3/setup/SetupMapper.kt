package uk.co.threebugs.mochiwhattotrade3.setup

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.co.threebugs.mochiwhattotrade3.setupgroup.SetupGroup
import java.nio.file.Path
import java.nio.file.Paths

@Mapper(componentModel = "spring")
abstract class SetupMapper {

    abstract fun toDto(setup: Setup): SetupDto

    @Mapping(target = "id", source = "setupDto.id")
    @Mapping(target = "symbol", source = "setupDto.symbol")
    abstract fun toEntity(setupDto: SetupDto, setupGroup: SetupGroup): Setup

    fun pathToString(path: Path?): String? {
        return path?.toString()
    }

    fun stringToPath(path: String?): Path? {
        return if (path == null) null else Paths.get(path)
    }
}
